# 第9章 Form 与输入校验

## 本章成果

完成员工新增页面。学员会经历浏览器提交 POST、Django 校验、保存数据库和重定向的完整流程。

## 本章开始状态与修改清单

第8章列表和详情已经读取数据库。本章新建 `employees/forms.py` 和 `employees/form.html`，在 View 与 App 路由中追加新增功能；不要复制一套新的 Employee Model。

## 定义 ModelForm

- **Form 是什么**：Django 用来接收、转换并校验用户输入的对象。
- **ModelForm 是什么**：根据 Model 字段生成表单，并提供创建或更新 Model 的能力。
- **为什么需要**：浏览器提交的内容不能直接写入数据库，需要统一转换类型、检查错误并向页面返回提示。
- **什么时候使用**：输入不直接对应 Model 时使用 Form；员工新增、编辑这类 Model 操作使用 ModelForm。

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

`Meta.model` 指定表单对应 `Employee`；`fields` 是允许用户提交的字段白名单；`widgets` 只改变 HTML 控件。`clean_employee_number()` 读取已经完成基础转换的 `cleaned_data`，返回的值会成为最终员工编号；抛出 `ValidationError` 时，表单不保存并显示错误。

`forms.DateInput(attrs={"type": "date"})` 创建日期输入控件；`attrs` 是要写入HTML元素的属性字典，返回一个Widget对象，不负责后端日期校验。`EmployeeForm(data=None, instance=None)` 在不传 `data` 时是未绑定表单，传入 `request.POST` 后是绑定表单；`instance` 省略时用于新增，传入Model对象时用于编辑。

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

`is_valid()` 不接参数，执行字段转换和校验，返回布尔值；失败信息写入 `form.errors`。`save(commit=True)` 默认立即保存并返回Model对象；传 `commit=False` 时只返回尚未保存的对象。只有 `is_valid()` 成功后才能使用 `cleaned_data` 或保存。成功后重定向可防止刷新页面时重复提交。

## 增加路由和模板

```python
path("new/", views.employee_create, name="create"),
```

把固定路径 `new/` 放在 `<int:employee_id>/` 前面，路由意图更容易阅读。

创建 `templates/employees/form.html`：

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

- **CSRF 是什么**：攻击者诱导已登录浏览器向目标系统发送非本人意图的写请求。
- **为什么需要 token**：服务端通过请求中的随机值判断表单是否来自预期页面和会话。
- **什么时候使用**：Django 模板中的 POST 表单都应包含 `{% csrf_token %}`。

```text
GET → 未绑定 Form → 显示页面
POST → 绑定 request.POST → is_valid()
                    ├─ 失败 → errors → 原页面
                    └─ 成功 → cleaned_data → save() → redirect()
```

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

## 新增功能运行检查

- [ ] 能说明 GET 显示表单、POST 处理表单
- [ ] 失败时原页面显示错误，成功时重定向
- [ ] POST 表单包含 CSRF token

## 现场识读：`Form`、`ModelForm` 与字段对象

普通 `Form` 适合搜索、登录、确认等不直接对应一张表的输入；`ModelForm` 根据 Model 字段生成表单并可保存实例。两者都包含 Field、Widget、label、errors、`cleaned_data` 和 `is_valid()`，Widget 只控制输入控件表现，不承担可信的后端校验。

模板中的 `form.employee_number` 是 `BoundField`。生产页面常逐字段渲染，以便准确放置标签和错误：

- **BoundField是什么**：Form字段定义与当前绑定数据、错误和Widget组合后的模板对象。
- **为什么需要**：模板需要从同一个对象取得标签、输入控件、当前值和字段错误。
- **什么时候使用**：需要逐字段控制表单HTML和错误位置时使用。

```html
<div>
  {{ form.employee_number.label_tag }}
  {{ form.employee_number }}
  {{ form.employee_number.errors }}
</div>
{{ form.non_field_errors }}
```

`clean_employee_number()` 校验单字段；`clean()` 适合比较多个字段。校验顺序中任何阶段失败都不应保存数据。错误信息应帮助用户修正输入，但不要暴露堆栈、SQL 或内部对象信息。

`label_tag()` 不传参数时返回使用字段label生成的 `<label>` HTML，`errors` 提供该字段错误列表。`ValidationError(message, code=None, params=None)` 的错误消息必填，错误代码和格式化参数可选；抛出后由Form转换为可显示错误。单字段 `clean_<field>()` 不接额外参数并返回清理后的字段值；跨字段 `clean()` 返回完整 `cleaned_data` 字典。

## CSRF 的工作过程

Django 在页面中写入 token，浏览器提交 POST 时一并发送，中间件验证该请求是否来自预期站点会话。出现403时检查 token、Cookie、请求来源和中间件，不关闭保护。CSRF 防止借用已登录浏览器发起跨站写请求，它不能替代登录、权限和业务校验。

## 表单处理固定流程

GET 创建未绑定表单；POST 使用 `request.POST`（含文件时再传 `request.FILES`）创建绑定表单；`is_valid()` 失败时原样渲染绑定表单；成功时保存并重定向。这个流程应能从既有 View 中一眼识别。

## 本章总结

Form 负责接收和验证输入，ModelForm 进一步连接 Model 和保存操作。GET 显示表单，POST 校验并保存；失败时保留错误，成功时重定向。下一章复用同一个 Form 完成编辑，并增加逻辑删除和消息提示。

## 日本项目中的实际使用

企业项目常用 ModelForm 处理标准 CRUD，因为字段、类型和基础校验可以与 Model 保持一致。团队通常使用字段白名单，不直接暴露全部 Model 字段；操作者、状态等可信字段由后端填写，不能相信隐藏输入框。

## 新人常见错误

- POST 时仍创建 `EmployeeForm()`，表单没有绑定提交数据，应传入 `request.POST`。
- 没有调用 `is_valid()` 就读取 `cleaned_data` 或保存。
- 只依赖浏览器 `required` 校验，攻击者仍可绕过前端直接提交请求。
- POST 表单遗漏 `{% csrf_token %}`，正常请求返回403。
- 使用 `fields = "__all__"` 暴露不应由用户修改的字段。

## 本章知识将在后续章节继续使用

```text
Model
→ ModelForm 字段与校验
→ POST + CSRF
→ save()
→ 第10章使用 instance 完成编辑
→ 第14章结合 request.FILES 上传文件
```
