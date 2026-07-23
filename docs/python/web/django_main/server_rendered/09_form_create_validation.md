# 第9章 Form 与输入校验

## 本章成果

完成员工新增页面。学员会经历浏览器提交 POST、Django 校验、保存数据库和重定向的完整流程。

## 本章开始状态与修改清单

第8章列表和详情已经读取数据库。本章新建 `employees/forms.py` 和 `employees/form.html`，在 View 与 App 路由中追加新增功能；不要复制一套新的 Employee Model。

## 定义 ModelForm

创建 `employees/forms.py`：

```python
from django import forms

from .models import Employee


class EmployeeForm(forms.ModelForm):
    class Meta:
        model = Employee
        fields = ["employee_number", "name", "department", "email", "joined_on"]
        widgets = {"joined_on": forms.DateInput(attrs={"type": "date"})}

    def clean_employee_number(self) -> str:
        value = self.cleaned_data["employee_number"].strip().upper()
        if not value.startswith("E"):
            raise forms.ValidationError("员工编号必须以 E 开头。")
        return value
```

Model 负责全系统都要遵守的约束，Form 负责本输入场景的格式和提示。浏览器校验只是辅助，服务端仍必须校验。

## 编写新增 View

```python
from django.shortcuts import redirect

from .forms import EmployeeForm


def employee_create(request: HttpRequest) -> HttpResponse:
    if request.method == "POST":
        form = EmployeeForm(request.POST)
        if form.is_valid():
            employee = form.save()
            return redirect("employees:detail", employee_id=employee.pk)
    else:
        form = EmployeeForm()
    return render(request, "employees/form.html", {"form": form})
```

只有 `is_valid()` 成功后才能使用 `cleaned_data` 或保存。成功后重定向可防止刷新页面时重复提交。

## 增加路由和模板

```python
path("new/", views.employee_create, name="create"),
```

把固定路径 `new/` 放在 `<int:employee_id>/` 前面，路由意图更容易阅读。

创建 `employees/templates/employees/form.html`：

```html
{% extends "base.html" %}

{% block content %}
  <h1>新增员工</h1>
  <form method="post" novalidate>
    {% csrf_token %}
    {{ form.non_field_errors }}
    {{ form.as_p }}
    <button type="submit">保存</button>
    <a href="{% url 'employees:list' %}">取消</a>
  </form>
{% endblock %}
```

`{% csrf_token %}` 是 Django 对跨站请求伪造的基本防护，不应为了消除 403 而关闭它。

## 必测场景

1. 正常新增后只产生一条数据。
2. 员工编号不以 E 开头时显示错误且不保存。
3. 重复编号显示错误且不产生 500。
4. 必填项为空时保留用户已输入的其他内容。

使用浏览器 Network 确认：首次打开表单是 GET 200；提交成功是 POST 后302，再 GET 详情页；校验失败是 POST 200，并且数据库没有新增记录。

## 课堂任务

新增员工 `e004` 并确认保存为 `E004`；随后分别制造“编号不以E开头”和“编号重复”。提交调查结果时写清输入、页面错误和数据库记录数。

现场确认：`入力エラーの場合は同一画面にエラーを表示し、DBには登録されません。`

参考方向见[章节练习参考答案](practice_answers.md)。

## 完成检查

- [ ] 能说明 GET 显示表单、POST 处理表单
- [ ] 失败时原页面显示错误，成功时重定向
- [ ] POST 表单包含 CSRF token

下一章复用同一个 Form 完成编辑，并增加逻辑删除和消息提示。

## `Form`、`ModelForm` 与字段对象

普通 `Form` 适合搜索、登录、确认等不直接对应一张表的输入；`ModelForm` 根据 Model 字段生成表单并可保存实例。两者都包含 Field、Widget、label、errors、`cleaned_data` 和 `is_valid()`，Widget 只控制输入控件表现，不承担可信的后端校验。

模板中的 `form.employee_number` 是 `BoundField`。生产页面常逐字段渲染，以便准确放置标签和错误：

```html
<div>
  {{ form.employee_number.label_tag }}
  {{ form.employee_number }}
  {{ form.employee_number.errors }}
</div>
{{ form.non_field_errors }}
```

`clean_employee_number()` 校验单字段；`clean()` 适合比较多个字段。校验顺序中任何阶段失败都不应保存数据。错误信息应帮助用户修正输入，但不要暴露堆栈、SQL 或内部对象信息。

## CSRF 的工作过程

Django 在页面中写入 token，浏览器提交 POST 时一并发送，中间件验证该请求是否来自预期站点会话。出现403时检查 token、Cookie、请求来源和中间件，不关闭保护。CSRF 防止借用已登录浏览器发起跨站写请求，它不能替代登录、权限和业务校验。

## 表单处理固定流程

GET 创建未绑定表单；POST 使用 `request.POST`（含文件时再传 `request.FILES`）创建绑定表单；`is_valid()` 失败时原样渲染绑定表单；成功时保存并重定向。这个流程应能从既有 View 中一眼识别。
