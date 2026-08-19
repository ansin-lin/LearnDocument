# 第10章 编辑、删除与消息提示

## 本章成果

补齐员工编辑、删除确认和操作结果提示。删除采用逻辑删除：记录仍保留，只把 `is_active` 改为 `False`。

## 本章开始状态与修改清单

复用第9章的 `EmployeeForm` 和表单模板。追加两个 View、两个命名路由和一个确认模板，并在 `base.html` 显示 messages；不要创建第二套编辑 Form。

## 编辑员工

本章第一次集中使用消息框架和 PRG：

- **messages 是什么**：跨一次重定向向用户显示操作结果的临时消息。
- **为什么需要**：POST 成功后页面跳转，仍需要告诉用户新增、更新或删除是否完成。
- **什么时候使用**：写操作成功或需要给出一次性反馈时使用；它不替代审计日志。

```python
from django.contrib import messages


def employee_update(request: HttpRequest, employee_id: int) -> HttpResponse:
    employee = get_object_or_404(Employee, pk=employee_id, is_active=True)
    if request.method == "POST":
        form = EmployeeForm(request.POST, instance=employee)
        if form.is_valid():
            employee = form.save()
            messages.success(request, "员工信息已更新。")
            return redirect("employees:detail", employee_id=employee.pk)
    else:
        form = EmployeeForm(instance=employee)
    return render(request, "employees/form.html", {"form": form, "employee": employee})
```

`instance=employee` 是新增与编辑的关键差异。遗漏它会新增一条记录。

`messages.success(request, message)` 的 `request` 是当前请求，`message` 是要在下一次页面中显示的文本；它把消息加入消息存储，业务代码不使用其返回值。`warning()` 和 `error()` 参数形式相同，只改变消息级别。

代码先查询当前员工，再根据请求方法创建表单。POST 时 `instance=employee` 表示更新现有对象；GET 时同一参数把原值显示在表单中。校验成功后保存、写入成功消息并重定向，校验失败则继续渲染带错误的表单。

## 删除必须先确认

```python
def employee_delete(request: HttpRequest, employee_id: int) -> HttpResponse:
    employee = get_object_or_404(Employee, pk=employee_id, is_active=True)
    if request.method == "POST":
        employee.is_active = False
        employee.save(update_fields=["is_active"])
        messages.success(request, "员工已设为离职。")
        return redirect("employees:list")
    return render(request, "employees/confirm_delete.html", {"employee": employee})
```

删除不能使用 GET。GET 应只显示确认页，真正修改数据必须由 POST 完成。

`employee.save(update_fields=["is_active"])` 只让Django生成该字段的更新SQL；`update_fields` 必须是当前Model字段名组成的可迭代对象。`save()` 返回 `None`，执行后数据库记录仍存在但状态已改变。

```html
{% extends "base.html" %}
{% block content %}
  <h1>确认将 {{ employee.name }} 设为离职吗？</h1>
  <form method="post">
    {% csrf_token %}
    <button type="submit">确认</button>
    <a href="{% url 'employees:detail' employee.pk %}">取消</a>
  </form>
{% endblock %}
```

## 路由

```python
path("<int:employee_id>/edit/", views.employee_update, name="update"),
path("<int:employee_id>/delete/", views.employee_delete, name="delete"),
```

## 显示消息

在 `base.html` 的 `<main>` 开头加入：

```html
{% if messages %}
  <ul class="messages">
    {% for message in messages %}<li>{{ message }}</li>{% endfor %}
  </ul>
{% endif %}
```

Django 新项目默认已经启用 messages；若无显示，再检查 `INSTALLED_APPS`、中间件和模板 context processor。

## 必测场景

- 编辑后主键不变、总记录数不增加。
- 删除确认页使用 GET 不改变数据。
- POST 后员工不再出现在列表，但数据库记录仍存在。
- 重复提交旧删除请求返回 404，不产生 500。

## 课堂任务

1. 编辑员工姓名，提交前后分别记录主键和员工总数，证明没有误新增。
2. 对同一员工执行一次 GET 删除页和一次 POST 删除，比较数据库状态。
3. 故意遗漏 `instance` 复现重复记录问题，说明原因后恢复；不要在共享数据库执行此实验。

现场报告：`削除は物理削除ではなく、在職フラグをFalseに更新しています。`

参考方向见[章节练习参考答案](practice_answers.md)。

## CRUD 运行检查

- [ ] 已形成新增、详情、编辑、逻辑删除的 CRUD 主流程
- [ ] 所有改写数据操作都使用 POST
- [ ] 成功结果有明确消息，失败结果有表单错误

## CRUD、PRG 与保存边界

CRUD 分别是 Create、Read、Update、Delete。服务端渲染页面的写操作通常采用 PRG（Post/Redirect/Get）：POST 成功后返回重定向，浏览器再 GET 结果页。这样刷新结果页不会重复提交；校验失败仍返回当前表单的200响应。

`form.save(commit=False)` 先生成尚未保存的实例，适合补充不应信任用户输入的字段：

```python
employee = form.save(commit=False)
employee.updated_by = request.user
employee.save()
```

只有确实需要补字段时才使用；忘记最后 `save()` 会导致页面看似成功但数据库未更新。

`messages.success()`、`warning()`、`error()` 表达不同结果。消息是用户反馈，不是审计日志。失败原因属于表单时优先显示字段/非字段错误；系统异常写日志并显示安全错误页。

物理删除真正移除记录，逻辑删除保留记录并改变状态。选择取决于规格、关联数据、审计和法规要求。逻辑删除后，所有列表、详情、唯一性、报表和 API 都要明确是否包含已删除数据；不能只在一个列表加过滤就认为完成。

## 事务与并发的基础

Django 默认使用自动提交：没有显式事务时，每次数据库写入通常独立提交。一个业务操作需要同时修改多条记录时，用 `transaction.atomic()` 保证全部成功或全部回滚。

`transaction.atomic()` 不传参数时返回事务上下文管理器；正常退出时提交，块内异常向外传播时回滚。它也可作为装饰器使用。本章只在Shell实验中使用，事务范围应尽量短。

下面是独立的 Shell 实验，不修改业务 View。先准备 `E001`、`E002` 两名员工，再执行：

```python
from django.db import transaction

from employees.models import Department, Employee


with transaction.atomic():
    target = Department.objects.get(name="开发部")
    Employee.objects.filter(employee_number="E001").update(department=target)
    Employee.objects.filter(employee_number="E002").update(department=target)
```

如果第二次更新前抛出异常，退出 `atomic()` 时第一次更新也会回滚。事务块应尽量短，不在其中等待用户输入、调用慢速外部 API 或传输大文件。

既有项目处理库存、审批或编号分配时，还可能使用 `select_for_update()` 锁定即将修改的记录。它必须结合事务使用，具体锁行为取决于 PostgreSQL、MySQL 等数据库；SQLite 学习环境不能代表生产并发行为。现场改修写操作时至少确认：

`select_for_update()` 返回带行锁要求的新QuerySet，查询真正执行时才尝试加锁；常用可选参数如 `nowait`、`skip_locked` 的支持情况取决于数据库。本课程不实际加入业务View，进入既有项目时应先确认事务范围、锁顺序和超时策略。

- 是否一次修改多张表或多条记录。
- 中途失败时哪些变化必须一起回滚。
- 是否可能有两个请求同时修改同一数据。
- 数据库约束、唯一性和锁等待如何处理。
- 重试是否可能造成重复写入。

## 本章总结

新增和编辑复用 ModelForm，写操作通过 POST 提交，并在成功后遵循 PRG 重定向。删除必须先确认业务含义；本项目使用逻辑删除保留记录。下一章在现有列表上增加搜索、排序和分页。

## 日本项目中的实际使用

企业系统通常要求 GET 不产生数据变更，删除或状态变更必须通过 POST 并显示确认画面。是否采用逻辑删除由规格、审计和关联数据决定。多记录更新还要确认事务范围、并发冲突和失败后的恢复方法。

## 新人常见错误

- 编辑时遗漏 `instance`，提交一次却新增一条员工记录。
- 使用 GET 执行删除，链接预览、爬虫或误点击都可能改变数据。
- POST 成功后直接返回页面，刷新时可能重复提交；应使用 PRG。
- 逻辑删除只过滤列表，没有同步检查详情、报表和关联功能。
- 把 messages 当成日志，导致调查时缺少操作者和请求信息。

## 本章知识将在后续章节继续使用

```text
ModelForm + instance
→ Update
POST 确认 → 逻辑删除
写操作 → messages → redirect → GET
→ 第13章增加权限
→ 第16章增加回归测试
```
