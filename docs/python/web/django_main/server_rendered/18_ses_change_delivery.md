# 第18章 日本 SES 改修实战

## 本章成果

按现场流程完成“员工列表增加入职日期范围筛选”改修，并提交可 Review、可测试、可交接的成果。

## 本章开始状态

从第17章可重建、测试通过的版本创建改修分支。现有列表已经支持关键字和分页，本章不得破坏这些行为；本次不修改 Model，因此不应生成数据库迁移。

## 模拟规格

> 员工列表增加“入职日期 From / To”。两项都可为空；From 大于 To 时显示错误；搜索、翻页后条件必须保留；现有关键字搜索和权限不得受影响。

## 1. 先把规格变成可确认的问题

- **规格确认是什么**：把需求文字转换为输入、行为、边界和完成条件。
- **影响调查是什么**：在修改前找出可能受影响的代码、数据、权限、测试和运维项。
- **为什么需要**：现场改修的风险往往来自遗漏既有行为，而不只是新代码本身。
- **什么时候执行**：收到票据后、开始编码前执行；发现不明确条件时记录问题和暂定假设。

```text
规格与票据
→ 确认问题和完成条件
→ 调查影响范围
→ 小步修改
→ 单体测试与回归测试
→ diff 自查
→ Review
→ 指摘修正与再测试
→ 交付和交接
```

开始编码前确认日期格式、边界是否包含当天、错误显示位置、默认排序、目标角色、数据量和兼容浏览器。无法确认时记录假设，不把猜测伪装成确定规格。

## 2. 调查影响范围

从 URL 进入 View，再追踪 Form、QuerySet、Template、测试和样式。搜索已有分页参数拼接方式，确认是否存在共通组件。把调查结果写成短表：文件、当前作用、预定修改、风险。

## 3. 小步实现

建议创建只负责 GET 参数校验的搜索 Form；校验通过后给 QuerySet 追加 `joined_on__gte` 和 `joined_on__lte`；分页链接使用编码后的完整查询参数。不要把日期字符串直接拼进 SQL。

在 `employees/forms.py` 新增：

```python
class EmployeeSearchForm(forms.Form):
    q = forms.CharField(label="关键字", required=False)
    joined_from = forms.DateField(
        label="入职日期 From",
        required=False,
        widget=forms.DateInput(attrs={"type": "date"}),
    )
    joined_to = forms.DateField(
        label="入职日期 To",
        required=False,
        widget=forms.DateInput(attrs={"type": "date"}),
    )

    def clean(self):
        cleaned_data = super().clean()
        joined_from = cleaned_data.get("joined_from")
        joined_to = cleaned_data.get("joined_to")
        if joined_from and joined_to and joined_from > joined_to:
            raise forms.ValidationError("From 不能晚于 To。")
        return cleaned_data
```

`q`、`joined_from` 和 `joined_to` 对应 URL 中的三个 GET 参数，`required=False` 表示可以为空。`DateField` 把合法日期字符串转换为 Python 日期；`clean()` 在单字段校验后比较 From 和 To，并把跨字段错误放入非字段错误。返回 `cleaned_data` 后，View 才能继续使用校验结果。

`CharField(label, required, ...)` 和 `DateField(label, required, widget, ...)` 都返回Form字段对象。`label` 是页面标签，`required` 默认 `True`，设为 `False` 后允许空输入；`widget` 只改变HTML控件。字段校验成功后，`CharField` 提供字符串，`DateField` 提供 `datetime.date`；失败时错误进入Form，不返回不可信原值供查询使用。

`clean()` 是跨字段校验入口，不接额外参数并必须返回清理后的字典。`cleaned_data.get(key, default=None)` 在字段有效且存在时返回转换后的值，否则返回默认值；读取前必须先让Form执行校验。

在 `employees/views.py` 导入 `EmployeeSearchForm`，并用下面的完整函数替换第11章列表 View：

```python
@login_required
@permission_required("employees.view_employee", raise_exception=True)
def employee_list(request: HttpRequest) -> HttpResponse:
    form = EmployeeSearchForm(request.GET or None)
    employees = Employee.objects.filter(is_active=True).select_related("department")

    if form.is_valid():
        keyword = form.cleaned_data["q"].strip()
        if keyword:
            employees = employees.filter(
                Q(employee_number__icontains=keyword)
                | Q(name__icontains=keyword)
                | Q(department__name__icontains=keyword)
            )
        if form.cleaned_data["joined_from"]:
            employees = employees.filter(joined_on__gte=form.cleaned_data["joined_from"])
        if form.cleaned_data["joined_to"]:
            employees = employees.filter(joined_on__lte=form.cleaned_data["joined_to"])

    page_obj = Paginator(employees, 10).get_page(request.GET.get("page"))
query = request.GET.copy()
query.pop("page", None)
    return render(
        request,
        "employees/list.html",
        {"form": form, "page_obj": page_obj, "query_string": query.urlencode()},
    )
```

View 的处理顺序保持不变：先用 `request.GET` 绑定搜索 Form，再建立包含权限范围和在职条件的基础 QuerySet。只有 Form 合法时才读取 `cleaned_data` 并追加关键字、From、To 条件。分页完成后复制查询参数并移除旧页码，使模板生成的新翻页链接保留全部筛选条件。

`request.GET.copy()` 不接参数，返回可修改的QueryDict副本；原始 `request.GET` 不直接修改。`pop(key, default)` 删除指定键并返回对应值列表，不存在时返回默认值；这里删除旧 `page`。`urlencode()` 把剩余键值编码为查询字符串并返回字符串，模板可安全地继续拼接新页码。

所需的 `Q`、`Paginator`、权限装饰器等导入已经在第11–13章出现；如果编辑器提示名称未定义，逐项核对文件顶部导入。模板显示 `form.q`、`form.joined_from`、`form.joined_to` 和 `form.non_field_errors`。分页链接在 `query_string` 非空时追加 `&page=...`，不要只保留 `q`。

## 4. 测试观点

| 分类 | 代表场景 |
|---|---|
| 正常 | 只填 From、只填 To、两者都填、边界当天 |
| 异常 | 格式错误、From 大于 To |
| 回归 | 关键字搜索、空结果、分页、权限、逻辑删除 |

先写能复现规格的测试，再修改实现。手工确认时记录 URL、账号角色、输入、预期和实际结果。

至少增加两个自动测试：一个验证关键字与日期范围能组合，一个验证 From 晚于 To 时显示错误。再执行第16章已有全部测试，证明不是只测试新增功能。

## 5. Git 与 Review

提交只包含本改修所需文件，提交信息说明“做了什么和为什么”。Review 说明至少包含：

- 需求或票号。
- 修改概要与影响文件。
- 设计选择和未采用方案。
- 自动测试、手工测试及结果。
- 数据库迁移、配置、部署和回滚影响。
- 已知限制与希望 Reviewer 重点确认之处。

收到意见后先判断是缺陷、规格理解差异还是改进建议；修改代码时同步更新测试和说明。

## 6. 交接演练

用三分钟向下一位开发者说明：需求、现状、修改点、验证证据、剩余风险、下一步。交接的目标不是复述代码，而是让对方能够继续工作和判断风险。

## 7. Git 操作形成可 Review 的差异

Git 完整原理与冲突处理请学习站内 Git 课程；本任务至少按以下顺序操作，并在每一步确认状态：

```powershell
git clone <仓库地址> company-portal-change
Set-Location company-portal-change
git switch -c feature/employee-joined-date-filter
git status
git diff
git add employees/forms.py employees/views.py templates/employees/list.html employees/tests.py
git diff --staged
git commit -m "Add joined date filter to employee list"
git push -u origin feature/employee-joined-date-filter
```

| 命令 | 参数 | 可接受的值与必填性 | 执行后状态 | 验证重点 |
|---|---|---|---|---|
| `git clone <url> <directory>` | 仓库地址、目标目录 | 地址必填；目标目录可选，本例显式指定 | 创建 `company-portal-change` 工作目录 | 确认克隆成功，无认证或网络错误 |
| `Set-Location <path>` | 本地目录 | 已存在目录路径，必填 | PowerShell进入克隆后的仓库 | 当前路径中能看到 `.git` 和项目文件 |
| `git switch -c <branch>` | `-c`、分支名 | `-c` 表示创建；新分支名必填 | 创建并切换改修分支 | `git status` 显示目标分支 |
| `git status` | 无 | 不需要参数 | 只读取当前工作区状态 | 确认分支、已修改和未跟踪文件 |
| `git diff` | 无 | 默认查看未暂存差异 | 只读取差异 | 确认修改内容和无关变化 |
| `git add <paths...>` | 文件路径 | 一个或多个仓库内路径，必填 | 指定内容进入暂存区 | 不使用宽泛范围误收秘密或生成物 |
| `git diff --staged` | `--staged` | 可选开关，本步骤必须使用 | 只读取暂存区差异 | 提交前再次确认最终范围 |
| `git commit -m <message>` | `-m`、提交信息 | 提交信息必填 | 成功后创建本地提交 | 检查提交摘要和测试证据 |
| `git push -u origin <branch>` | `-u`、远程名、分支名 | 远程和分支必填；`-u`设置上游关系 | 提交推送到远程分支 | 确认推送成功及PR目标分支 |

这些命令依次取得仓库、进入克隆后的目录、创建改修分支、确认工作区、查看差异、只暂存本票文件、复查暂存差异、创建提交并推送远程。`git clone` 只创建目录，不会替当前PowerShell自动切换目录，因此 `Set-Location` 不能省略。每一步都保留确认点，避免无关文件或敏感信息混入PR。命令会改变本地或远程状态时，必须先完成前一步验证；示例中的仓库地址、目录和分支名需要替换为实际项目值。

若团队仍使用 `checkout`，先确认语义再遵守项目手顺。提交前检查基线分支是否最新、差异是否只有本票内容、生成文件与秘密是否混入。PR 中附自动测试命令、手工验证条件和必要截图；不要只写“已测试”。

Review 指摘修正后再次查看差异并复测。是否追加提交、整理提交或同步基线由团队规则决定；共享分支上不要擅自改写历史或强制推送。

## 8. 数据与障害视角

本改修不改变表结构，但仍需用 ORM 或只读 SQL 确认边界日期、离职数据和部门关联。查询生产数据必须遵守权限与脱敏要求，禁止为调查直接修改数据。

若测试环境出现“筛选后第二页为空”，按请求参数 → Form 校验 → QuerySet 条件 → 排序 → 分页参数 → 模板链接调查。记录现象、条件、日志和 SQL，再提出原因；不要边猜边改多个位置。

## 9. 交付用检查表

- [ ] 分支来源和目标环境明确，工作区无无关文件。
- [ ] 规格、假设、影响文件和不修改范围已记录。
- [ ] 正常、异常、边界、权限和回归证据可复现。
- [ ] 无迁移/有迁移、配置、部署顺序和回滚影响写清楚。
- [ ] Review 指摘、对应修改和再测试结果可追踪。
- [ ] 交接者能说明日志、SQL和代码的后续调查入口。

## 最终验收

- [ ] 规格、假设和完成条件清楚
- [ ] 影响调查覆盖代码、数据、权限、测试和运维
- [ ] 正常、异常、边界与回归测试有证据
- [ ] 差异中没有无关格式化、调试代码或敏感信息
- [ ] Reviewer 能复现，接收方能继续维护

参考实现和通过测试的第18章状态见[服务端渲染参考项目](reference_project.md)，提交用表达见[SES现场日语](ses_japanese.md)，练习判断见[章节练习参考答案](practice_answers.md)。

完成本章后，学员应具备在指导和 Review 下承担小型 Django 改修的基本闭环能力。真正进入现场后，仍需继续学习项目自己的架构、编码规范、业务术语和部署流程。

## 日本项目中的实际使用

日本 SES 现场通常以票据、设计书或指摘单为工作入口。担当者需要留下规格理解、影响范围、修改差异、测试证据和未解决事项。Review 不是只检查语法，而是确认规格、既有功能、数据、权限、发布与回滚风险。

## 新人常见错误

- 规格不明确时自行决定，却没有记录假设和确认事项。
- 只修改最先找到的 View，没有调查 Form、Template、测试和分页参数。
- 只验证新增日期条件，没有回归关键字、权限、逻辑删除和分页。
- `git add .` 把无关格式化、生成文件或秘密一起提交。
- 收到 Review 指摘后只改代码，没有更新测试和再次确认差异。

## 本章知识将在后续章节继续使用

```text
第1～17章技术能力
→ 第18章服务端渲染改修闭环
→ 第19章 REST 请求响应
→ 第20～28章 API、认证、联调、测试与部署
→ 第29章前后端分离 SES 改修闭环
```
