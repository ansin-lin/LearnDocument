# 第10章 编辑、删除与消息提示

## 本章成果

补齐员工编辑、删除确认和操作结果提示。删除采用逻辑删除：记录仍保留，只把 `is_active` 改为 `False`。

## 本章开始状态与修改清单

复用第9章的 `EmployeeForm` 和表单模板。追加两个 View、两个命名路由和一个确认模板，并在 `base.html` 显示 messages；不要创建第二套编辑 Form。

## 编辑员工

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

## 完成检查

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
