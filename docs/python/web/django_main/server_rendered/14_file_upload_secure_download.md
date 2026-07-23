# 第14章 文件上传与安全下载

## 本章成果

维护者可上传员工附件；有查看权限的用户才能下载。系统限制文件大小和类型，不直接信任用户提交的文件名或响应头。

## 本章开始状态与修改清单

第13章权限矩阵已经生效。本章依次修改 Model、生成迁移、新增 `AttachmentForm`、上传/下载 View、命名路由和模板。上传目录是运行时数据，不提交 Git。

## Media 配置

在 `settings.py` 中加入：

```python
MEDIA_URL = "/media/"
MEDIA_ROOT = BASE_DIR / "media"
```

Media 是用户上传内容，Static 是开发者发布的 CSS/JS/图片，两者不能混放。开发阶段也不要用公开 Media URL 暴露私密员工附件。

## 附件模型与校验

```python
from pathlib import Path

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
    uploaded_by = models.ForeignKey("auth.User", on_delete=models.PROTECT)
    uploaded_at = models.DateTimeField(auto_now_add=True)
```

扩展名校验不能证明文件内容一定安全。生产系统还应按风险加入 MIME/文件签名检查、恶意软件扫描、随机存储名和独立存储服务。

## 上传要点

HTML 表单必须包含 `enctype="multipart/form-data"`，View 必须把 `request.FILES` 传给 Form。保存前用服务端登录用户写入 `uploaded_by`，不要相信隐藏字段传来的用户 ID。

```python
form = AttachmentForm(request.POST, request.FILES)
if form.is_valid():
    attachment = form.save(commit=False)
    attachment.employee = employee
    attachment.uploaded_by = request.user
    attachment.original_name = Path(attachment.file.name.replace("\\", "/")).name
attachment.save()
```

`AttachmentForm` 的最小定义：

```python
class AttachmentForm(forms.ModelForm):
    class Meta:
        model = EmployeeAttachment
        fields = ["file"]
```

上传表单必须写：

```html
<form method="post" enctype="multipart/form-data" novalidate>
  {% csrf_token %}
  {{ form.as_p }}
  <button type="submit">上传</button>
</form>
```

## 受控下载

```python
from pathlib import Path

from django.http import FileResponse


@permission_required("employees.view_employee", raise_exception=True)
def attachment_download(request, attachment_id):
    attachment = get_object_or_404(EmployeeAttachment, pk=attachment_id)
    safe_name = Path(attachment.original_name.replace("\\", "/")).name
    return FileResponse(
        attachment.file.open("rb"),
        as_attachment=True,
        filename=safe_name,
    )
```

为上传 View 使用 `employees.add_employeeattachment`，为下载 View 至少检查员工查看权限。生产项目还要结合部门或对象级业务规则判断“能查看哪些员工”，Model 全局权限并不能自动完成数据范围控制。

下载通过受保护的 View，不把服务器绝对路径拼入 URL，不接收用户提供的任意文件路径。

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

课堂故障任务：故意删除 `enctype`，观察 `request.FILES` 为空和表单错误，再恢复。不要用公开 `MEDIA_URL + 文件名` 验证私密下载。

现场报告：`添付ファイルは公開URLではなく、権限確認付きのViewからダウンロードします。`

参考方向见[章节练习参考答案](practice_answers.md)。

## 完成检查

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
