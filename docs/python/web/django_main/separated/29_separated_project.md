# 第29章 REST API SES 改修实战

## 本章成果

在指导与 Review 下完成一次既有员工 API 改修：从日文规格和前端请求调查影响范围，实现筛选与响应字段，保持分页、权限和兼容性，补自动测试与 OpenAPI，并提交可发布、可交接的成果。

## 本章开始状态

从第28章已经通过本地测试、schema 验证和发布前检查的版本创建改修分支。现有筛选、搜索、排序、分页、JWT、部门数据范围和附件功能必须继续工作；本次需求不修改 Model，因此不应生成数据库迁移。尚未在真实生产环境验证的项目必须明确标注。

## 本章在整体架构中的位置

```text
Ticket → 规格确认 → 影响调查 → API 改修 → Test / OpenAPI
   → Review → 发布判断 → 监控与交接
```

本章整合前面所有组件，不引入新的接口设计。完成后，需要用代码差异、测试、契约和调查记录证明改修可交付，而不是只展示功能画面。

| 概念 | 是什么（What） | 为什么需要（Why） | 什么时候使用（When） |
|---|---|---|---|
| 影响调查 | 修改前确认调用方、代码、数据和发布影响的活动 | 防止只改局部而破坏既有系统 | 接到既有 API 改修票据后 |
| 兼容性 | 新旧客户端与接口版本能否继续协作的性质 | 前后端通常不能保证同时发布 | 增删字段、改变查询或响应时 |
| 交接证据 | 规格、差异、测试、部署和风险的可追踪记录 | 让 Review、发布和后续维护可判断 | 完成改修并移交成果时 |

## 1. 模拟票据

> 社員一覧APIに所属部署、入社日From/To、在職区分の絞り込みを追加する。検索・ソート・ページングとの併用を可能とする。互換用の読み取り専用項目 `department_name` を追加する。閲覧権限および部署単位の参照範囲は変更しない。既存フロントへの互換性を維持し、OpenAPIと単体テストを更新すること。

验收条件：

- 条件均可为空，可组合；日期边界包含当天。
- From 晚于 To 返回400和明确字段/非字段错误。
- 既有 `search`、`ordering`、`page` 同时有效。
- 只返回用户数据范围内员工，不能借筛选绕过权限。
- 新增只读 `department_name`，既有 `department` 和 `department_detail` 不删除或改类型。
- SQL 查询无明显 N+1；测试与 schema 验证通过。

## 2. 开始前确认

向负责人确认：在职区分是否允许查看离职数据、无权限资源返回403还是404、日期格式、时区、默认排序、最大页大小、API版本策略、前端发布时间和目标环境。无法即时确认的事项写为假设并标注风险，不擅自把个人理解变成规格。

在既有仓库根目录、激活项目虚拟环境且确认当前连接不是生产数据库后建立基线：

```powershell
git status
git branch --show-current
git log --oneline -1
git remote -v
python manage.py check
python manage.py test employees
python manage.py spectacular --file schema-before.yml --validate
```

`git branch --show-current` 返回当前分支名，工作区处于 detached HEAD 时可能没有输出；`git log --oneline -1` 用单行格式显示最近1条提交，应看到从项目创建阶段延续下来的基线。`git remote -v` 列出远程名称及抓取、推送地址；没有输出时仍可完成本地分支、提交和 Review 资料。需要推送时，先在代码托管平台创建空的练习仓库，再在项目根目录执行：

```powershell
$repositoryUrl = Read-Host "输入练习仓库 URL"
git remote add origin $repositoryUrl
git remote -v
```

`Read-Host` 从终端读取地址，避免把个人仓库地址写入共享脚本。`git remote add origin` 为当前仓库登记远程地址；若团队仓库已经存在 `origin`，不得重复添加或擅自改向。记录当前测试结果和一个既有前端请求/响应，后续用于证明兼容。

## 3. 影响调查

沿真实调用链调查：

```text
前端列表组件/API封装
→ /api/employees/
→ Router
→ EmployeeViewSet
→ EmployeePermission + get_queryset
→ EmployeeFilter
→ EmployeeSerializer
→ Employee/Department Model
→ 数据库
→ API测试与OpenAPI
```

形成影响表：

| 位置 | 当前职责 | 计划修改 | 风险 |
|---|---|---|---|
| `filters.py` | 部门与日期筛选 | 增加区间交叉校验 | 非法条件是否400 |
| `api_views.py` | 数据范围、搜索、分页 | 保持范围并优化关联 | 越权、N+1 |
| `serializers.py` | 员工响应 | 增加只读 `department_name` | 契约兼容 |
| API tests | 回归保护 | 条件组合/权限/查询 | 漏测边界 |
| OpenAPI | 调用契约 | 参数与响应更新 | 文档和实现不一致 |
| 前端 | 构造查询、显示部门 | 读取新增字段 | 旧响应兼容 |

本需求不修改 Model 时不应生成迁移。若 `git status` 出现迁移文件，先调查原因。

## 4. 实现策略

### 4.1 FilterSet 做输入与查询条件

在 `employees/filters.py` 中修改 `EmployeeFilter`；导入放在文件顶部，方法放在类内：

```python
import django_filters


class EmployeeFilter(django_filters.FilterSet):
    joined_from = django_filters.DateFilter(field_name="joined_on", lookup_expr="gte")
    joined_to = django_filters.DateFilter(field_name="joined_on", lookup_expr="lte")

    class Meta:
        model = Employee
        fields = ["department", "is_active"]

    def is_valid(self):
        valid = super().is_valid()
        if not valid:
            return False
        joined_from = self.form.cleaned_data.get("joined_from")
        joined_to = self.form.cleaned_data.get("joined_to")
        if joined_from and joined_to and joined_from > joined_to:
            self.form.add_error(None, "From 不能晚于 To。")
            return False
        return True
```

应用这段代码前，先确认当前 `django-filter` 与 DRF 如何把 FilterSet 错误转换为400。若现有错误结构不稳定，可改用自定义 FilterSet form clean 或 filter backend。非法范围不能静默返回空列表，也不能变成500。

`FilterSet.is_valid()` 返回当前查询参数是否通过表单校验；`super().is_valid()` 必须先执行父类解析。成功后才能读取 `self.form.cleaned_data`，其 `get(name)` 在参数缺失时返回 `None`。`self.form.add_error(None, message)` 添加非字段错误；本实现随后返回 `False`。这是对既有类的定向改修，提交前必须用真实 API 请求确认错误被 DRF 转换为约定的400结构。

### 4.2 QuerySet 先限制数据范围

在 `employees/api_views.py` 的 `EmployeeViewSet` 中替换 `get_queryset()`，导入放在文件顶部：

```python
from .access import scope_employee_queryset


def get_queryset(self):
    queryset = (
        Employee.objects
        .select_related("department")
        .order_by("employee_number", "pk")
    )
    if not self.request.user.has_perm("employees.view_inactive_employee"):
        queryset = queryset.filter(is_active=True)
    return scope_employee_queryset(queryset, self.request.user)
```

`scope_employee_queryset()` 是第22章已经实现并测试的共通函数。客户端筛选在这个QuerySet之后执行，只能缩小授权集合，不能扩大数据范围。

### 4.3 Serializer只增加兼容字段

在 `employees/serializers.py` 的 `EmployeeSerializer` 类中增加：

```python
department_name = serializers.CharField(
    source="department.name",
    read_only=True,
)
```

并把 `"department_name"` 加入 `Meta.fields`。保持 `department` ID、`department_detail` 和既有字段不变。不要为了“更整洁”在同一改修中重命名 `employee_number` 或改变日期格式。

## 5. 测试设计

| 分类 | 用例 |
|---|---|
| 正常 | 部门、From、To、在职状态分别筛选 |
| 组合 | 部门 + 日期 + search + ordering + page |
| 边界 | 入职日等于From/To；最后一页 |
| 异常 | 非法日期；From晚于To；未知排序字段 |
| 权限 | 匿名401；查看用户200；部门外数据不可见 |
| 兼容 | 既有字段、类型、分页结构不变 |
| 性能 | 部门摘要不产生每员工一次查询 |

测试要断言返回与未返回对象、顺序、count、状态码和数据库未变化。OpenAPI 生成验证与前端最小联调也属于完成条件。

## 6. Git 与 Review

以下命令在仓库根目录的 PowerShell 中执行。开始前必须确认工作区没有混入其他任务改动，并将分支名、远程名称和提交信息按团队规范调整；只有 `git remote -v` 已确认目标正确时才执行 push：

```powershell
git switch -c feature/employee-api-filters
git diff
git add employees/filters.py employees/api_views.py employees/serializers.py employees/tests_api.py
git diff --staged
git commit -m "Add employee API filters and department summary"
git push -u origin feature/employee-api-filters
```

没有远程仓库时，到本地 commit 为止即完成代码基线；PR 与 push 标记为“未执行：缺少远程练习仓库”，不能伪造已推送证据。

实际路径按仓库为准，schema 是否提交按项目规则。PR 内容：

`git switch -c <分支>` 从当前提交创建并切换分支；`git diff` 查看未暂存差异，`git add <明确路径>` 只暂存本票据文件，`git diff --staged` 复核待提交内容，`git commit -m <消息>` 创建本地提交，`git push -u origin <分支>` 首次推送并设置上游。推送前必须确认测试通过、远程仓库正确且没有秘密或无关文件。

- 票据/规格、确认事项和假设。
- 调用链与影响文件。
- API 契约变化和兼容判断。
- 权限与数据范围说明。
- 自动测试、schema 验证、前端联调和性能证据。
- 迁移、配置、部署顺序、回滚与已知限制。

Review 指摘逐条回答“理解、修改位置、验证结果”。若不同意，使用代码、规格和测试证据讨论，不只写“按指摘修改”。

## 7. 发布与交接

本改修没有迁移和新配置时也要明确写“无”。前后端若能独立发布，确认新增响应字段对旧前端安全，并先发布兼容后端。发布后检查健康、代表请求、401/403、筛选组合、错误率、延迟和日志请求 ID。

三分钟交接：需求与完成条件、修改链路、权限边界、验证证据、发布顺序、监控点、剩余风险和发生问题时的回滚/调查入口。

## 日本企业项目中的实际使用

SES 改修的核心不是按票据机械改代码，而是确认规格、调查影响、控制差异、留下验证证据并完成交接。无法确认的事项必须记录为假设和风险，交由负责人判断。

## 新人常见错误

- 未建立改修前基线，完成后无法证明兼容性。
- 只调查后端文件，忽略前端调用、OpenAPI 和发布顺序。
- 为满足筛选需求改变数据范围，造成越权。
- PR 只写“已修改”，没有规格、影响、测试和风险。
- 收到 Review 指摘后只改代码，不补回归证据。

## 企业项目调查路径

```text
Ticket / 规格 → 既有请求 → Router → ViewSet → Permission / QuerySet
→ Filter → Serializer → SQL → Test / OpenAPI → Git diff → 发布手顺
```

调查结果应能回答“为什么改这些位置、为什么没有改其他位置、如何证明没有破坏既有调用方”。

## 8. 最终验收

- [ ] 能从前端请求追到 Router、ViewSet、权限、Filter、Serializer、Model 和 SQL。
- [ ] 能修改并验证 JSON 契约，不破坏既有调用方。
- [ ] 认证、授权、数据范围和 CORS 各自正确。
- [ ] 正常、异常、边界、权限、性能和回归测试有证据。
- [ ] OpenAPI、依赖、配置和部署说明与代码一致。
- [ ] Git 差异聚焦，Review 与交接可追踪。

完成改修后，应能够在指导和 Review 下承担小型 Django/DRF 任务。进入实际项目后，还要继续确认业务、架构、前端框架、数据库、云环境、沟通方式和团队规范。

这次改修需要形成完整闭环：REST 契约、ViewSet、Serializer、JWT、权限、数据范围、查询、OpenAPI、测试、日志和部署证据必须保持一致。
