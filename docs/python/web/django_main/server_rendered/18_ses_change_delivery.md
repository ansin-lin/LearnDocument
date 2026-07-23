# 第18章 日本 SES 改修实战

## 本章成果

按现场流程完成“员工列表增加入职日期范围筛选”改修，并提交可 Review、可测试、可交接的成果。

## 本章开始状态

从第17章可重建、测试通过的版本创建改修分支。现有列表已经支持关键字和分页，本章不得破坏这些行为；本次不修改 Model，因此不应生成数据库迁移。

## 模拟规格

> 员工列表增加“入职日期 From / To”。两项都可为空；From 大于 To 时显示错误；搜索、翻页后条件必须保留；现有关键字搜索和权限不得受影响。

## 1. 先把规格变成可确认的问题

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

## 最终验收

- [ ] 规格、假设和完成条件清楚
- [ ] 影响调查覆盖代码、数据、权限、测试和运维
- [ ] 正常、异常、边界与回归测试有证据
- [ ] 差异中没有无关格式化、调试代码或敏感信息
- [ ] Reviewer 能复现，接收方能继续维护

参考实现和通过测试的最终状态见[最终参考项目](reference_project.md)，提交用表达见[SES现场日语](ses_japanese.md)，练习判断见[章节练习参考答案](practice_answers.md)。

完成本章后，学员应具备在指导和 Review 下承担小型 Django 改修的基本闭环能力。真正进入现场后，仍需继续学习项目自己的架构、编码规范、业务术语和部署流程。

## 7. Git 操作形成可 Review 的差异

Git 完整原理与冲突处理请学习站内 Git 课程；本任务至少按以下顺序操作，并在每一步确认状态：

```powershell
git clone <仓库地址>
git switch -c feature/employee-joined-date-filter
git status
git diff
git add employees/forms.py employees/views.py templates/employees/list.html employees/tests.py
git diff --staged
git commit -m "Add joined date filter to employee list"
git push -u origin feature/employee-joined-date-filter
```

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
