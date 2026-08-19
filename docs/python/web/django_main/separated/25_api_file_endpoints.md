# 第25章 API 文件上传与安全下载

## 本章成果

为员工项目增加附件 Model，并把附件功能提供为受 JWT、操作权限和部门数据范围保护的 API；随后在第24章前端中完成上传、列表和下载联调。完成后能排查415、400、401、403和404，能说明数据库事务与文件存储并非同一事务。

## 本章开始状态与修改清单

- 第22章已经实现 `scope_employee_queryset()` 和 `EmployeePermission`。
- 第24章已经有可运行的 `frontend/` 和带 token 刷新能力的 API 封装。
- 本章修改 `models.py` 并生成附件迁移，然后修改 `serializers.py`、`permissions.py`、`api_views.py`、项目路由和前端页面；数据库连接保持不变。

## 本章在整体架构中的位置

```text
Browser FormData → multipart Request → ViewSet action → Serializer / Storage
                                                        ↓
Browser download ← FileResponse ← Permission / 数据范围
```

完成后，员工系统将能在不公开私有文件路径的前提下，通过受控 API 上传、查询和下载附件。

| 概念 | 是什么（What） | 为什么需要（Why） | 什么时候使用（When） |
|---|---|---|---|
| multipart | 在一个 HTTP 请求中传递字段和文件的编码格式 | JSON 不适合直接承载普通二进制文件 | 浏览器上传附件时 |
| ViewSet action | 添加在资源标准 CRUD 之外的自定义操作 | 让附件上传等资源动作仍进入统一权限和路由 | 操作属于某个资源但不是标准 CRUD 时 |
| `FileResponse` | 面向文件流式输出的 Django 响应 | 避免一次性把完整文件读入内存 | 经过权限检查后提供私有文件下载时 |
| `FileField` | 记录文件存储名称并管理上传文件的 Model 字段 | 把文件元数据与业务对象关联 | Model 需要保存上传文件时 |

## 先理解文件 API 保存了什么

上传文件后，系统同时产生两类数据：数据库保存员工、上传者、原文件名和存储名称等元数据；文件存储保存 PDF 的实际字节。`FileField` 连接这两部分，但不会把整个文件内容直接放进普通数据库文本字段。

```text
Browser FormData
→ multipart/form-data Request
→ DRF Parser / Serializer 校验
→ Database：附件记录
→ Storage：PDF 文件内容

Download Request
→ JWT / Permission
→ 员工数据范围
→ 附件记录与存储文件
→ FileResponse
```

- **multipart 是什么**：在同一个 HTTP 请求中分段传递普通字段和二进制文件的编码格式。
- **为什么不能只返回 Media URL**：员工附件属于私有业务数据，公开 URL 会绕过 View、JWT、Permission 和部门数据范围。
- **什么时候使用 `FileResponse`**：权限检查通过后，以流式响应返回存储中的文件，避免把整个文件一次性读入内存。

文件安全需要多层检查。扩展名方便初步限制，大小限制控制资源消耗，文件签名检查阻止最明显的伪装；高风险项目还需要恶意软件扫描、隔离存储和下载审计。任何一层都不能单独证明文件安全。

数据库事务与文件存储也不是同一个事务。数据库回滚时，已经写入存储的文件不一定自动删除；删除数据库记录时，物理文件也不一定自动消失。因此上传失败补偿、孤立文件清理、备份与恢复必须分别设计。

## 1. 建立附件 Model

在 `employees/models.py` 顶部补充导入：

```python
from django.contrib.auth import get_user_model
from django.core.exceptions import ValidationError
from django.core.validators import FileExtensionValidator
```

在同一文件的 `Employee` 类之后追加：

```python
def validate_file_size(file) -> None:
    if file.size > 5 * 1024 * 1024:
        raise ValidationError("文件不能超过 5 MB。")


class EmployeeAttachment(models.Model):
    employee = models.ForeignKey(
        Employee,
        on_delete=models.CASCADE,
        related_name="attachments",
    )
    file = models.FileField(
        upload_to="employee_attachments/%Y/%m/",
        validators=[FileExtensionValidator(["pdf"]), validate_file_size],
    )
    original_name = models.CharField(max_length=255)
    uploaded_by = models.ForeignKey(get_user_model(), on_delete=models.PROTECT)
    uploaded_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        ordering = ["-uploaded_at"]

    def __str__(self) -> str:
        return self.original_name
```

`get_user_model()` 不接收参数，返回项目当前启用的用户 Model，避免把外键固定到某个具体用户类。`FileField(upload_to=..., validators=...)` 定义文件字段并返回字段配置：数据库只保存文件名称，内容由存储后端管理；`upload_to` 接受相对目录格式字符串或可调用对象，省略时保存到存储根目录，本例中的 `%Y/%m/` 按上传日期形成年度和月份目录；`validators` 接受校验函数或可调用对象列表，本例用它加入扩展名和容量两项校验。

`FileExtensionValidator(allowed_extensions=None, message=None, code=None)` 创建扩展名校验器；`allowed_extensions` 接受扩展名序列或默认的 `None`，本例明确传入 `["pdf"]`，`message` 和 `code` 可用于定制错误。执行校验时，通过则没有需要使用的返回值，失败时抛出 `ValidationError`。它只检查文件名扩展名，不能证明内容安全。`validate_file_size(file)` 接收上传对象，超过5 MB时抛出同类异常。`DateTimeField(auto_now_add=True)` 的该参数接受布尔值，默认是 `False`；设为 `True` 后在记录第一次创建时自动写入当前时间，之后再次保存不会自动更新。如果需要每次保存都更新时间才使用 `auto_now=True`。`models.CASCADE` 表示删除员工时一并删除附件记录，`models.PROTECT` 阻止删除仍被附件引用的上传者。删除数据库记录不保证物理文件自动删除，清理策略必须单独设计。

在 `company_portal/settings.py` 末尾加入本地文件存储位置：

```python
MEDIA_URL = "/media/"
MEDIA_ROOT = BASE_DIR / "media"
```

`MEDIA_ROOT` 是服务端实际保存上传文件的目录，`MEDIA_URL` 是媒体 URL 前缀。本项目的员工附件属于私有文件，因此不会把 `MEDIA_URL` 直接加入公开路由；下载必须经过本章后面的权限检查。

在项目根目录、已激活虚拟环境时执行：

```powershell
python manage.py makemigrations employees
python manage.py migrate
python manage.py showmigrations employees
```

迁移文件把新增表纳入版本管理，`migrate` 将其应用到当前数据库。确认新迁移显示 `[X]` 后再继续编写 Serializer。

迁移会同时生成附件 Model 的默认权限。使用超级用户进入 `/admin/`，给第22章的 `api-maintainer` 增加 `employees | employee attachment | Can add employee attachment` 权限；原有员工查看和修改权限保持不变。否则附件 POST 会按本章权限规则返回403。

### 阶段检查：先确认数据层

继续实现接口前，确认 `showmigrations employees` 中附件迁移显示 `[X]`，`media/` 没有加入 Git 暂存，并且 `api-maintainer` 已获得 `add_employeeattachment` 权限。第22章的 `api-viewer` 不增加该权限，用于验证查看和上传权限的差异。

## 2. 接口契约

| 操作 | 方法与URL | 权限 | 成功 |
|---|---|---|---:|
| 上传附件 | `POST /api/employees/{id}/attachments/` | change employee + add attachment + 数据范围 | 201 |
| 附件列表 | `GET /api/employees/{id}/attachments/` | view employee + 数据范围 | 200 |
| 下载附件 | `GET /api/attachments/{id}/download/` | view employee + 数据范围 | 200 |

上传使用 `multipart/form-data`，字段名为 `file`。响应返回附件ID、原始文件名、上传者和时间，不返回服务器绝对路径或公开Media URL。

## 3. Serializer与最小PDF检查

在 `employees/serializers.py` 追加：

```python
from pathlib import Path

from rest_framework import serializers

from .models import EmployeeAttachment


class AttachmentSerializer(serializers.ModelSerializer):
    original_name = serializers.CharField(read_only=True)
    uploaded_by = serializers.CharField(
        source="uploaded_by.username",
        read_only=True,
    )

    class Meta:
        model = EmployeeAttachment
        fields = [
            "id",
            "file",
            "original_name",
            "uploaded_by",
            "uploaded_at",
        ]
        extra_kwargs = {"file": {"write_only": True}}

    def validate_file(self, value):
        if value.size == 0:
            raise serializers.ValidationError("文件不能为空。")
        if value.size > 5 * 1024 * 1024:
            raise serializers.ValidationError("文件不能超过5 MB。")
        if Path(value.name).suffix.lower() != ".pdf":
            raise serializers.ValidationError("只允许上传PDF。")

        original_position = value.tell()
        value.seek(0)
        header = value.read(5)
        value.seek(original_position)
        if header != b"%PDF-":
            raise serializers.ValidationError("文件内容不是可识别的PDF。")
        return value
```

扩展名和前5字节只能阻止明显伪装，不等于完整PDF解析或恶意软件检测。生产系统还要根据风险采用成熟解析器、恶意软件扫描、隔离区和异步检测策略。

`Path` 来自 Python 标准库 `pathlib`，`Path(value.name).suffix.lower()` 取得并规范化最后一个扩展名。上传对象的 `tell()` 返回当前读取位置，`seek(offset)` 移动位置，`read(5)` 最多读取5字节并返回 `bytes`；恢复原位置可避免后续存储从错误位置开始。`validate_file(self, value)` 遵循 DRF 字段校验命名约定，成功返回文件对象，失败抛出 `serializers.ValidationError`。

`Meta.extra_kwargs` 用字典补充 `ModelSerializer` 自动生成字段的参数。本例把 `file` 设置为 `write_only=True`：客户端可以上传该字段，但序列化响应不会返回存储路径。私有附件只通过受控下载接口访问，不能把文件字段生成的地址直接暴露给客户端。

## 4. 为附件action补齐权限

在 `employees/permissions.py` 的 `EmployeePermission.has_permission()` 中确保附件分支存在：

```python
if request.method in SAFE_METHODS:
    return request.user.has_perm("employees.view_employee")
if view.action == "create":
    return request.user.has_perm("employees.add_employee")
if view.action in {"update", "partial_update"}:
    return request.user.has_perm("employees.change_employee")
if view.action == "destroy":
    return request.user.has_perm("employees.delete_employee")
if view.action == "attachments":
    return (
        request.user.has_perm("employees.change_employee")
        and request.user.has_perm("employees.add_employeeattachment")
    )
return False
```

GET属于安全方法，需要查看权限；POST附件action同时需要员工修改权限和附件新增权限。新增action时必须更新权限矩阵，不能依赖前端隐藏按钮。

## 5. 上传与附件列表action

在 `employees/api_views.py` 补齐导入，并在 `EmployeeViewSet` 内追加：

```python
from pathlib import Path

from rest_framework import status
from rest_framework.decorators import action
from rest_framework.parsers import FormParser, MultiPartParser
from rest_framework.response import Response

from .serializers import AttachmentSerializer


@action(
    detail=True,
    methods=["get", "post"],
    parser_classes=[MultiPartParser, FormParser],
    url_path="attachments",
)
def attachments(self, request, pk=None):
    employee = self.get_object()
    if request.method == "GET":
        serializer = AttachmentSerializer(
            employee.attachments.select_related("uploaded_by"),
            many=True,
        )
        return Response(serializer.data)

    serializer = AttachmentSerializer(data=request.data)
    serializer.is_valid(raise_exception=True)
    uploaded_file = serializer.validated_data["file"]
    attachment = serializer.save(
        employee=employee,
        uploaded_by=request.user,
        original_name=Path(
            uploaded_file.name.replace("\\", "/")
        ).name,
    )
    return Response(
        AttachmentSerializer(attachment).data,
        status=status.HTTP_201_CREATED,
    )
```

`self.get_object()` 从第22章受限的QuerySet取员工，因此未授权部门员工会返回404。`employee`和`uploaded_by`来自路径对象与认证用户，不接受客户端伪造。

`@action(detail=True, methods=["get", "post"], parser_classes=[MultiPartParser, FormParser], url_path="attachments")` 为单个员工生成附件子路径。`detail` 是必填布尔值，`True` 表示 URL 包含对象主键；`methods` 接受小写 HTTP 方法列表，省略时默认只有 GET；`parser_classes` 接受解析器类列表并覆盖该 action 的默认解析器；`url_path` 指定 URL 片段，省略时使用方法名。装饰器返回已经附带路由信息的方法。

`MultiPartParser` 处理含文件的 `multipart/form-data`，把普通字段与上传文件提供给 `request.data`；`FormParser` 处理不含文件的 `application/x-www-form-urlencoded`。没有解析器匹配请求的 `Content-Type` 时，DRF 返回415；普通表单即使能够解析，没有 `file` 字段仍会在 Serializer 校验时返回400。`pk` 来自 Router 的员工主键。`AttachmentSerializer(data=request.data)` 读取上传字段，`save(employee=..., uploaded_by=..., original_name=...)` 把可信服务端字段与已校验数据一起写入并返回附件对象。

## 6. 受控下载

在 `employees/api_views.py` 追加完整下载View：

```python
from pathlib import Path

from django.http import FileResponse
from django.shortcuts import get_object_or_404
from rest_framework.permissions import IsAuthenticated
from rest_framework.views import APIView

from .access import scope_employee_queryset
from .models import Employee, EmployeeAttachment


class AttachmentDownloadView(APIView):
    permission_classes = [IsAuthenticated]

    def get(self, request, attachment_id):
        if not request.user.has_perm("employees.view_employee"):
            self.permission_denied(request)

        allowed_employees = scope_employee_queryset(
            Employee.objects.filter(is_active=True),
            request.user,
        )
        attachment = get_object_or_404(
            EmployeeAttachment.objects.select_related("employee"),
            pk=attachment_id,
            employee__in=allowed_employees,
        )
        safe_name = Path(
            attachment.original_name.replace("\\", "/")
        ).name
        return FileResponse(
            attachment.file.open("rb"),
            as_attachment=True,
            filename=safe_name,
            content_type="application/pdf",
        )
```

`APIView` 按 HTTP 方法名调用 `get()`。`self.permission_denied(request, message=None, code=None)` 中后两个参数可省略；它不会返回普通结果，而是抛出 DRF 的认证或权限异常，本例对已认证但缺少查看权限的用户生成403。

`get_object_or_404(queryset, **conditions)` 在受限 QuerySet 中查找并返回单个对象，找不到时抛出404。`attachment.file.open(mode="rb")` 以二进制只读模式打开存储文件并返回可读取的文件对象。`FileResponse(open_file, as_attachment=False, filename="", **kwargs)` 接收文件对象并返回流式响应；本例的 `as_attachment=True` 要求浏览器下载，`filename` 是对外文件名而不是服务器路径，`content_type="application/pdf"` 通过响应关键字参数声明 MIME 类型。Django 在响应关闭时关闭它所管理的文件对象，调用代码不要提前关闭。

在 `company_portal/urls.py` 顶部导入 `AttachmentDownloadView`，并把以下条目追加到 `urlpatterns`：

```python
path(
    "api/attachments/<int:attachment_id>/download/",
    AttachmentDownloadView.as_view(),
    name="api-attachment-download",
)
```

路由片段 `<int:attachment_id>` 使用 Django 的整数路径转换器：只有可转换为整数的单段路径才能匹配，转换后的值以 `attachment_id` 参数传给 `get()`；不符合格式的路径不会进入 View，通常得到404。这里传递的是数据库 ID，不是磁盘文件名或路径。

下载 URL 不接受磁盘路径，私有附件不能通过公开 `MEDIA_URL` 绕过权限。若具有离职员工查看权限，是否允许下载其历史附件需要单独写入业务规格；当前实现只允许下载在职员工附件。

## 7. 扩展前端上传与下载

在 `frontend/index.html` 的分页导航后追加：

```html
<form id="attachment-form">
  <label>
    员工ID
    <input id="attachment-employee-id" type="number" min="1" required>
  </label>
  <label>
    PDF附件
    <input id="attachment-file" type="file" accept=".pdf,application/pdf" required>
  </label>
  <button type="submit">上传附件</button>
  <button id="load-attachments-button" type="button">读取附件</button>
</form>
<ul id="attachment-list"></ul>
```

`<input type="number" min="1">` 提供数字输入控件，`min` 是浏览器端允许的最小值提示；`<input type="file" accept=".pdf,application/pdf">` 提供文件选择控件，`accept` 只过滤文件选择界面的候选类型。两者都属于前端辅助校验，客户端仍可绕过，后端必须继续验证员工 ID、权限、大小、扩展名和内容。

在 `frontend/app.js` 追加：

```javascript
const attachmentForm = document.querySelector("#attachment-form");

attachmentForm.addEventListener("submit", async (event) => {
  event.preventDefault();
  const employeeId =
    document.querySelector("#attachment-employee-id").value;
  const file = document.querySelector("#attachment-file").files[0];
  const formData = new FormData();
  formData.append("file", file);

  try {
    const attachment = await apiFetch(
      `/employees/${employeeId}/attachments/`,
      {method: "POST", body: formData},
    );
    showStatus(`附件上传成功：${attachment.original_name}`);
    attachmentForm.reset();
  } catch (error) {
    if (error instanceof ApiError && error.status === 400) {
      showStatus(`附件校验失败：${JSON.stringify(error.body)}`);
    } else if (error instanceof ApiError && error.status === 403) {
      showStatus("当前账号没有上传权限");
    } else {
      showStatus("附件上传失败，请检查Network中的状态码和响应体");
    }
  }
});
```

`FormData`上传时不要手写 `Content-Type`，浏览器必须生成包含boundary的请求头。

`new FormData()` 创建 multipart 表单体，`append(name, value)` 追加字段；这里的字段名 `file` 必须与 Serializer 契约一致。把 `FormData` 作为 `apiFetch()` 的 `body` 后，封装会保留浏览器自动生成的 `Content-Type` 和 boundary。

文件输入元素的 `files` 属性返回 `FileList`，`files[0]` 是用户选择的第一个 `File`，未选择时为 `undefined`。本例的 HTML 使用 `required` 阻止普通空提交，脚本和后端仍应把缺少文件作为失败输入处理，不能只依赖浏览器校验。

在 `frontend/api.js` 追加二进制下载函数。它复用同一access、一次refresh和超时规则，但成功时返回 `Blob` 而不是尝试解析JSON：

```javascript
export async function apiDownload(path, retry = true) {
  const headers = new Headers();
  const access = sessionStorage.getItem(ACCESS_KEY);
  if (access) headers.set("Authorization", `Bearer ${access}`);

  const response = await fetchWithTimeout(
    `${API_BASE_URL}${path}`,
    {headers},
  );
  if (response.status === 401 && retry && await refreshAccessToken()) {
    return apiDownload(path, false);
  }
  if (!response.ok) {
    throw new ApiError(response.status, await readBody(response));
  }
  return response.blob();
}
```

把 `apiDownload` 加入 `app.js` 的import，并追加附件列表与下载：

```javascript
const attachmentList = document.querySelector("#attachment-list");
const loadAttachmentsButton =
  document.querySelector("#load-attachments-button");

loadAttachmentsButton.addEventListener("click", async () => {
  const employeeId =
    document.querySelector("#attachment-employee-id").value;
  if (!employeeId) {
    showStatus("请先输入员工ID");
    return;
  }

  try {
    const attachments = await apiFetch(
      `/employees/${employeeId}/attachments/`,
    );
    attachmentList.replaceChildren();
    for (const attachment of attachments) {
      const item = document.createElement("li");
      const button = document.createElement("button");
      button.type = "button";
      button.textContent = `下载 ${attachment.original_name}`;
      button.addEventListener("click", async () => {
        button.disabled = true;
        showStatus(`正在下载 ${attachment.original_name}`);
        try {
          const blob = await apiDownload(
            `/attachments/${attachment.id}/download/`,
          );
          const url = URL.createObjectURL(blob);
          try {
            const link = document.createElement("a");
            link.href = url;
            link.download = attachment.original_name;
            link.click();
          } finally {
            URL.revokeObjectURL(url);
          }
          showStatus(`已开始下载 ${attachment.original_name}`);
        } catch (error) {
          if (error instanceof ApiError && error.status === 403) {
            showStatus("当前账号没有附件下载权限");
          } else if (error instanceof ApiError && error.status === 404) {
            showStatus("附件不存在或不在当前账号的数据范围内");
          } else {
            showStatus("附件下载失败，请检查Network和请求ID");
          }
        } finally {
          button.disabled = false;
        }
      });
      item.append(button);
      attachmentList.append(item);
    }
    showStatus(`读取到 ${attachments.length} 个附件`);
  } catch {
    showStatus("附件列表读取失败，请检查权限和Network");
  }
});
```

附件列表请求与后来发生的下载按钮点击是两次独立的异步事件，因此各自需要错误处理；外层列表 `try...catch` 无法捕获以后点击按钮产生的异常。下载期间禁用当前按钮，避免同一文件被重复触发。对象URL无论下载触发是否成功都在内层 `finally` 中释放。

`response.blob()` 返回包含响应二进制内容的 `Promise<Blob>`。`URL.createObjectURL(blob)` 创建当前页面可用的临时 URL，使用结束后必须调用 `URL.revokeObjectURL(url)` 释放；这些 URL 不是后端永久文件地址，也不能代替权限控制。

临时创建的 `<a>` 元素通过 `href` 指向对象 URL，`download` 提供建议下载文件名；`link.click()` 不接收参数，用脚本触发一次点击并让浏览器开始处理下载，没有需要使用的返回值。浏览器可能清理不安全的文件名；服务端仍应为直接下载和非 JavaScript 客户端提供安全的 `Content-Disposition`。

## 8. 数据库事务与文件存储边界

数据库事务回滚不会自动删除已经写入文件系统或对象存储的文件；存储写入失败时，数据库记录也可能已经存在。`transaction` 从 `django.db` 导入；`transaction.atomic(using=None, savepoint=True, durable=False)` 返回可作为装饰器或 `with` 上下文管理器使用的事务边界。省略参数时使用默认数据库并建立必要的保存点，区块内异常会回滚对应数据库操作。它适合保证多表数据库写入的一致性，但不能把外部文件存储变成同一事务。

真实项目需要明确临时文件、提交后移动、失败补偿、删除策略、孤儿文件检查、备份和生命周期。大文件应流式处理，不能把完整内容一次读入内存。

## 9. 验证矩阵

- 正常小型PDF：201，元数据正确，可列表、可下载。
- 空文件、超5 MB、错误扩展名、伪装PDF头：400。
- JSON请求上传：415或400，按解析器配置确认。
- 无token：401；缺少操作权限：403。
- 其他部门员工和附件：404，不能通过直接ID绕过范围。
- 不存在附件：404；下载名含路径符时被安全化。
- 日志和错误体不包含token、文件内容或绝对路径。
- 测试使用临时 `MEDIA_ROOT`，完成后没有残留文件。

## 日本企业项目中的实际使用

企业文件接口必须同时考虑业务权限、数据范围、文件名、容量、内容检查、存储和清理策略。返回公开 Media URL 虽然简单，却可能绕过下载接口中的授权判断。

## 新人常见错误

- 手动设置 multipart 的 `Content-Type`，丢失浏览器生成的 boundary。
- 只检查扩展名，不限制大小或最小内容特征。
- 下载前只检查附件 ID，没有复用员工数据范围。
- 把数据库事务回滚误认为文件也会自动删除。
- 在日志或错误体中暴露服务器绝对路径。

## 企业项目调查路径

```text
FormData → multipart parser → Employee 数据范围 → Permission
→ Serializer 校验 → Database / Storage → FileResponse
```

上传失败先区分415、400和权限错误，再检查存储副作用；下载问题从附件所属员工和权限开始，不能只调查文件是否存在。

## 现场任务

复现“把文本文件改名为 `.pdf` 后上传成功”。先增加失败测试，再补最小PDF头校验；同时验证正常PDF、无权限和其他部门员工均符合契约。报告中说明最小文件头检查为什么仍不能替代生产安全扫描。

## 完成检查

- [ ] multipart、JSON和文件响应边界清楚。
- [ ] 大小、扩展名、最小内容特征、文件名和权限都在后端验证。
- [ ] 上传、列表和下载共用部门数据范围。
- [ ] 私有文件不能通过静态URL绕过API。
- [ ] 知道数据库事务与文件存储不是天然同一事务。

下一章用完整测试数据建立JWT、权限、数据范围、CRUD、查询和文件回归测试。
