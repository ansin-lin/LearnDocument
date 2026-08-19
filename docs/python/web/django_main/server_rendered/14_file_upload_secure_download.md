# 第14章 文件上传与安全下载

## 本章成果

维护者可上传员工附件；有查看权限的用户才能下载。系统限制文件大小和类型，不直接信任用户提交的文件名或响应头。

## 本章开始状态与修改清单

第13章权限矩阵已经生效。本章依次修改 Model、生成迁移、新增 `AttachmentForm`、上传/下载 View、命名路由和模板。上传目录是运行时数据，不提交 Git。

## Media 配置

- **Media 是什么**：运行期间由用户上传、内容会持续变化的文件。
- **FileField 是什么**：在 Model 中保存文件存储位置和相关元数据的字段，不是把整个文件塞进普通字符串字段。
- **为什么需要**：文件内容、数据库记录和访问权限需要统一管理，不能把用户文件当作公开 Static。
- **什么时候使用**：上传员工附件、头像或业务文档时使用，并同时设计校验、存储和下载权限。

在 `settings.py` 中加入：

```python
MEDIA_URL = "/media/"
MEDIA_ROOT = BASE_DIR / "media"
```

Media 是用户上传内容，Static 是开发者发布的 CSS/JS/图片，两者不能混放。开发阶段也不要用公开 Media URL 暴露私密员工附件。

## 附件模型与校验

```python
from pathlib import Path

from django.conf import settings
from django.core.exceptions import ValidationError
from django.core.validators import FileExtensionValidator


def validate_file_size(file) -> None:
    if file.size > 5 * 1024 * 1024:
        raise ValidationError("文件不能超过 5 MB。")


class EmployeeAttachment(models.Model):
    employee = models.ForeignKey(Employee, on_delete=models.CASCADE, related_name="attachments")
    file = models.FileField(
        upload_to="employee_attachments/%Y/%m/",
        validators=[FileExtensionValidator(["pdf"]), validate_file_size],
    )
    original_name = models.CharField(max_length=255)
    uploaded_by = models.ForeignKey(settings.AUTH_USER_MODEL, on_delete=models.PROTECT)
    uploaded_at = models.DateTimeField(auto_now_add=True)
```

`FileField(upload_to, validators, ...)` 中，`upload_to` 是存储目录前缀或生成路径的可调用对象；`validators` 是依次执行的校验器序列。字段在Model实例上表现为文件字段对象，可取得存储名称并调用 `open()`，数据库通常只保存存储引用。

`FileExtensionValidator(allowed_extensions)` 的参数是允许扩展名序列，返回可调用校验器；不匹配时抛出 `ValidationError`。自定义 `validate_file_size(value)` 接收上传文件对象，校验成功返回 `None`，失败抛出 `ValidationError`。扩展名和浏览器Content-Type都不能单独证明真实文件格式。

扩展名校验不能证明文件内容一定安全。生产系统还应按风险加入 MIME/文件签名检查、恶意软件扫描、随机存储名和独立存储服务。

## 上传要点

HTML 表单必须包含 `enctype="multipart/form-data"`，View 必须把 `request.FILES` 传给 Form。保存前用服务端登录用户写入 `uploaded_by`，不要相信隐藏字段传来的用户 ID。

在 `employees/views.py` 增加需要的导入和完整上传 View：

```python
from pathlib import Path

from django.contrib import messages
from django.contrib.auth.decorators import login_required, permission_required
from django.http import HttpRequest, HttpResponse
from django.shortcuts import get_object_or_404, redirect, render

from .forms import AttachmentForm
from .models import Employee


@login_required
@permission_required("employees.add_employeeattachment", raise_exception=True)
def attachment_upload(request: HttpRequest, employee_id: int) -> HttpResponse:
    employee = get_object_or_404(Employee, pk=employee_id, is_active=True)

    if request.method == "POST":
        form = AttachmentForm(request.POST, request.FILES)
        if form.is_valid():
            attachment = form.save(commit=False)
            attachment.employee = employee
            attachment.uploaded_by = request.user
            attachment.original_name = Path(
                attachment.file.name.replace("\\", "/")
            ).name
            attachment.save()
            messages.success(request, "附件已上传。")
            return redirect("employees:detail", employee_id=employee.pk)
    else:
        form = AttachmentForm()

    return render(
        request,
        "employees/attachment_form.html",
        {"form": form, "employee": employee},
    )
```

View 先确认目标员工存在且仍在职。GET 创建空表单；POST 同时绑定 `request.POST` 和 `request.FILES`。校验成功后，`commit=False` 先建立未保存对象，使 View 能补上当前员工、登录用户和安全化后的原文件名，最后一次 `save()` 写入数据库和文件存储。保存成功后沿用第10章的 messages 提示并重定向详情页；校验失败时继续渲染同一绑定表单，因此错误信息不会丢失。

`Path(path_string)` 接收字符串或路径类对象并返回 `Path` 对象；这里先把反斜杠统一为 `/`，再通过只读属性 `.name` 取得最后一段文件名。这样不会把客户端传来的目录部分继续保存为显示名称；它不替代文件类型、权限和存储层的安全检查。

`AttachmentForm` 的最小定义：

```python
class AttachmentForm(forms.ModelForm):
    class Meta:
        model = EmployeeAttachment
        fields = ["file"]
```

创建 `templates/employees/attachment_form.html`：

```html
{% extends "base.html" %}

{% block title %}上传附件 | 员工管理系统{% endblock %}

{% block content %}
<h1>为 {{ employee.name }} 上传附件</h1>
<form method="post" enctype="multipart/form-data" novalidate>
  {% csrf_token %}
  {{ form.non_field_errors }}
  {{ form.as_p }}
  <button type="submit">上传</button>
  <a href="{% url 'employees:detail' employee.pk %}">取消</a>
</form>
{% endblock %}
```

## 受控下载

```python
from pathlib import Path

from django.contrib.auth.decorators import login_required, permission_required
from django.http import FileResponse, HttpRequest
from django.shortcuts import get_object_or_404

from .models import EmployeeAttachment


@login_required
@permission_required("employees.view_employee", raise_exception=True)
def attachment_download(request: HttpRequest, attachment_id: int) -> FileResponse:
    attachment = get_object_or_404(
        EmployeeAttachment,
        pk=attachment_id,
        employee__is_active=True,
    )
    safe_name = Path(attachment.original_name.replace("\\", "/")).name
    return FileResponse(
        attachment.file.open("rb"),
        as_attachment=True,
        filename=safe_name,
    )
```

`FileResponse(open_file, as_attachment=False, filename="")` 的 `open_file` 必须是以二进制模式打开的文件对象；`as_attachment=True` 要求浏览器按下载处理；`filename` 设置对用户显示的安全文件名。返回值是流式HTTP响应，Django在发送完成后负责关闭文件；找不到数据库记录时前面的 `get_object_or_404()` 会先返回404。

为上传 View 使用 `employees.add_employeeattachment`，为下载 View 至少检查员工查看权限。生产项目还要结合部门或对象级业务规则判断“能查看哪些员工”，Model 全局权限并不能自动完成数据范围控制。

下载通过受保护的 View，不把服务器绝对路径拼入 URL，不接收用户提供的任意文件路径。

## 增加命名路由和详情页入口

在 `employees/urls.py` 的 `urlpatterns` 中追加：

```python
path(
    "<int:employee_id>/attachments/new/",
    views.attachment_upload,
    name="attachment_upload",
),
path(
    "attachments/<int:attachment_id>/download/",
    views.attachment_download,
    name="attachment_download",
),
```

第一条路由使用员工主键，因为上传动作必须先确定附件属于哪名员工；第二条路由使用附件主键，因为下载时要取得一条具体附件记录。两个 View 都在后端执行权限检查。

在 `templates/employees/detail.html` 的员工信息后追加上传入口和附件列表：

```html
{% if perms.employees.add_employeeattachment %}
  <a href="{% url 'employees:attachment_upload' employee.pk %}">上传附件</a>
{% endif %}

<h2>附件</h2>
<ul>
  {% for attachment in employee.attachments.all %}
    <li>
      <a href="{% url 'employees:attachment_download' attachment.pk %}">
        {{ attachment.original_name }}
      </a>
    </li>
  {% empty %}
    <li>尚未上传附件。</li>
  {% endfor %}
</ul>
```

模板权限判断只负责隐藏无权操作的上传入口；即使用户手工构造 URL，上传和下载 View 的装饰器仍会执行真正的后端检查。

```text
浏览器 multipart/form-data
→ request.FILES
→ Form / Model 校验
→ 文件存储 + 数据库元数据
→ 下载请求
→ 登录与权限检查
→ FileResponse
```

## 必测场景

- 正常 PDF 上传和下载。
- 超过 5 MB、非 PDF、空文件、同名文件。
- 未登录和无权限用户下载。
- 文件名含路径符号、空格、日文或中文。

执行 Model 修改后的固定检查：

```powershell
python manage.py makemigrations employees
python manage.py migrate
python manage.py check
```

这里给 `makemigrations` 传入App标签 `employees`，只为该App生成迁移；`migrate` 再把尚未应用的迁移执行到当前数据库。先阅读迁移文件和预计影响，再执行数据库变更。

课堂故障任务：故意删除 `enctype`，观察 `request.FILES` 为空和表单错误，再恢复。不要用公开 `MEDIA_URL + 文件名` 验证私密下载。

现场报告：`添付ファイルは公開URLではなく、権限確認付きのViewからダウンロードします。`

参考方向见[章节练习参考答案](practice_answers.md)。

## 文件功能运行检查

- [ ] Static 与 Media 分离
- [ ] 大小、类型、权限和文件名都在服务端处理
- [ ] 私密附件没有通过公开 Media URL 绕过权限

## FileField、ImageField 与真实文件类型

`FileField` 保存文件存储引用和元数据，不把整个文件内容塞进普通数据库字段。`ImageField` 会增加图片有效性检查并依赖 Pillow，但仍需大小、尺寸、格式和安全策略。`upload_to` 决定存储键前缀，不应包含用户可控制的绝对路径。

扩展名、浏览器上传的 `Content-Type` 和文件签名分别提供不同证据，任何单一证据都不足以覆盖高风险上传。课程项目只接受小型 PDF；真实项目按风险加入内容检测、病毒扫描、隔离区、随机存储名、对象存储和生命周期管理。

## 文件功能的完整数据流

```text
multipart/form-data
→ request.FILES
→ Form/Model校验
→ 私有存储
→ 附件元数据与上传者
→ 权限检查
→ FileResponse流式下载
```

数据库回滚不会自动删除已写入的外部文件，文件删除也不一定随 Model 删除自动完成。设计更新、失败恢复和定期清理时，必须同时考虑数据库与存储的一致性。文件响应测试除状态码外，还应确认权限、文件名、内容类型、流关闭和不存在文件。

## 本章总结

Media 保存用户上传内容，不能与 Static 混用。上传必须同时校验大小、类型、文件名和权限；私密文件只能通过后端权限检查后的 FileResponse 下载。下一章为系统增加可关联请求的日志、异常页和共通中间件。

## 日本项目中的实际使用

企业附件通常不通过公开 Media URL 直接访问，而由带权限检查的下载入口返回。设计书需要明确允许格式、大小、保存期限、病毒扫描、文件名显示和删除方式。测试证据不能使用真实员工资料。

## 新人常见错误

- 表单遗漏 `enctype="multipart/form-data"`，导致 `request.FILES` 为空。
- 只检查扩展名并完全相信浏览器的 Content-Type；两者都可能伪造。
- 直接使用用户文件名拼接服务器路径，产生路径穿越风险。
- 上传入口有权限，下载 URL 却公开，造成资料泄露。
- 认为数据库回滚会自动删除已经写入的外部文件。

## 本章知识将在后续章节继续使用

```text
Media 配置
→ request.FILES
→ AttachmentForm / FileField
→ 文件与元数据保存
→ Permission
→ FileResponse
→ 第16章文件测试
→ 第23章 API 文件端点
```
