# 第29章 前后端分离 SES 改修实战

## 本章成果

在指导与 Review 下完成一次既有员工 API 改修：从日文规格和前端请求调查影响范围，实现筛选与响应字段，保持分页、权限和兼容性，补自动测试与 OpenAPI，并提交可发布、可交接的成果。

这不是“再从零写一个项目”。开始状态是第28章已经可部署的仓库，学员必须先阅读现有代码、配置、测试和团队规范。

## 1. 模拟票据

> 社員一覧APIに所属部署、入社日From/To、在職区分の絞り込みを追加する。検索・ソート・ページングとの併用を可能とする。レスポンスに所属部署名を追加する。閲覧権限および部署単位の参照範囲は変更しない。既存フロントへの互換性を維持し、OpenAPIと単体テストを更新すること。

验收条件：

- 条件均可为空，可组合；日期边界包含当天。
- From 晚于 To 返回400和明确字段/非字段错误。
- 既有 `search`、`ordering`、`page` 同时有效。
- 只返回用户数据范围内员工，不能借筛选绕过权限。
- `department_detail.name` 增加后，既有字段不删除或改类型。
- SQL 查询无明显 N+1；测试与 schema 验证通过。

## 2. 开始前确认

向负责人确认：在职区分是否允许查看离职数据、无权限资源返回403还是404、日期格式、时区、默认排序、最大页大小、API版本策略、前端发布时间和目标环境。无法即时确认的事项写为假设并标注风险，不擅自把个人理解变成规格。

建立当前基线：

```powershell
git status
git branch --show-current
python manage.py check
python manage.py test employees
python manage.py spectacular --file schema-before.yml --validate
```

记录当前测试结果和一个既有前端请求/响应，后续用于证明兼容。

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
| `serializers.py` | 员工响应 | 增加部门摘要 | 契约兼容 |
| API tests | 回归保护 | 条件组合/权限/查询 | 漏测边界 |
| OpenAPI | 调用契约 | 参数与响应更新 | 文档和实现不一致 |
| 前端 | 构造查询、显示部门 | 读取新增字段 | 旧响应兼容 |

本需求不修改 Model 时不应生成迁移。若 `git status` 出现迁移文件，先调查原因。

## 4. 实现策略

### 4.1 FilterSet 做输入与查询条件

```python
from django.core.exceptions import ValidationError
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

这是教学方向，实际使用前应确认 `django-filter`/DRF 如何把 FilterSet 错误转换为400；更稳定的项目实现可自定义 FilterSet form clean 或 filter backend。关键是非法范围不能静默返回空列表，也不能变成500。

### 4.2 QuerySet 先限制数据范围

```python
def get_queryset(self):
    queryset = (
        Employee.objects
        .select_related("department")
        .order_by("employee_number", "pk")
    )
    if not self.request.user.has_perm("employees.view_inactive_employee"):
        queryset = queryset.filter(is_active=True)
    return apply_department_scope(queryset, self.request.user)
```

`apply_department_scope()` 代表项目既有共通函数；若仓库没有，不要凭空调用，应调查并在合适层实现。无论具体写法如何，客户端筛选只能缩小授权后的集合，不能扩大数据范围。

### 4.3 Serializer 只增加兼容字段

复用第20章 `department_detail`，保持 `department` ID 和既有字段。不要为了“更整洁”在同一改修中重命名 `employee_number` 或改变日期格式。

## 5. 测试设计

| 分类 | 用例 |
|---|---|
| 正常 | 部门、From、To、在职状态分别筛选 |
| 组合 | 部门 + 日期 + search + ordering + page |
| 边界 | 入职日等于From/To；最后一页 |
| 异常 | 非法日期；From晚于To；未知排序字段 |
| 权限 | 匿名401；查看组200；部门外数据不可见 |
| 兼容 | 既有字段、类型、分页结构不变 |
| 性能 | 部门摘要不产生每员工一次查询 |

测试要断言返回与未返回对象、顺序、count、状态码和数据库未变化。OpenAPI 生成验证与前端最小联调也属于完成条件。

## 6. Git 与 Review

```powershell
git switch -c feature/employee-api-filters
git diff
git add employees/filters.py employees/api_views.py employees/serializers.py employees/tests_api.py
git diff --staged
git commit -m "Add employee API filters and department summary"
git push -u origin feature/employee-api-filters
```

实际路径按仓库为准，schema 是否提交按项目规则。PR 内容：

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

## 8. 最终验收

- [ ] 能从前端请求追到 Router、ViewSet、权限、Filter、Serializer、Model 和 SQL。
- [ ] 能修改并验证 JSON 契约，不破坏既有调用方。
- [ ] 认证、授权、数据范围和 CORS 各自正确。
- [ ] 正常、异常、边界、权限、性能和回归测试有证据。
- [ ] OpenAPI、依赖、配置和部署说明与代码一致。
- [ ] Git 差异聚焦，Review 与交接可追踪。

完成第29章后，目标是能够在日本 SES 现场的指导和 Review 下读懂并承担小型 Django/DRF 改修。仍需继续学习所在项目的业务、架构、前端框架、数据库、云环境、日语沟通与团队规范；课程提供的是可靠起点，不替代现场经验。
