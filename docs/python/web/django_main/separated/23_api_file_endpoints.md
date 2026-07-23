# 第23章 API 文件上传与安全下载

## 本章成果

把第14章员工附件功能提供为受 JWT 和权限保护的 API：客户端使用 multipart 上传 PDF，服务端校验并记录上传者，通过受控端点下载。完成后能排查415、400、401、403和404，并理解数据库与文件存储的一致性风险。

## 1. 接口契约

| 操作 | 方法与 URL | 权限 | 成功 |
|---|---|---|---:|
| 上传附件 | `POST /api/employees/{id}/attachments/` | change employee | 201 |
| 附件列表 | `GET /api/employees/{id}/attachments/` | view employee | 200 |
| 下载附件 | `GET /api/attachments/{id}/download/` | view employee + 数据范围 | 200 |

上传使用 `multipart/form-data`，字段名为 `file`。响应只返回附件 ID、原始文件名、大小、上传者和时间，不返回服务器绝对路径。

## 2. Serializer

复用第14章 `EmployeeAttachment`，创建：

```python
from pathlib import Path

from rest_framework import serializers

from .models import EmployeeAttachment


class AttachmentSerializer(serializers.ModelSerializer):
    original_name = serializers.CharField(read_only=True)
    uploaded_by = serializers.CharField(source="uploaded_by.username", read_only=True)

    class Meta:
        model = EmployeeAttachment
        fields = ["id", "file", "original_name", "uploaded_by", "uploaded_at"]
        extra_kwargs = {"file": {"write_only": True}}

    def validate_file(self, value):
        if value.size == 0:
            raise serializers.ValidationError("文件不能为空。")
        if value.size > 5 * 1024 * 1024:
            raise serializers.ValidationError("文件不能超过 5 MB。")
        if Path(value.name).suffix.lower() != ".pdf":
            raise serializers.ValidationError("只允许上传 PDF。")
        return value
```

Model 验证器继续作为共通底线，Serializer 提供 API 场景错误。扩展名只是第一层检查；真实项目还需内容签名、恶意软件扫描和隔离策略。

## 3. 上传 action

在 `EmployeeViewSet` 增加：

```python
from pathlib import Path

from rest_framework.decorators import action
from rest_framework.parsers import FormParser, MultiPartParser


@action(
    detail=True,
    methods=["get", "post"],
    parser_classes=[MultiPartParser, FormParser],
    url_path="attachments",
)
def attachments(self, request, pk=None):
    employee = self.get_object()
    if request.method == "GET":
        serializer = AttachmentSerializer(employee.attachments.all(), many=True)
        return Response(serializer.data)

    serializer = AttachmentSerializer(data=request.data)
    serializer.is_valid(raise_exception=True)
    uploaded_file = serializer.validated_data["file"]
    attachment = serializer.save(
        employee=employee,
        uploaded_by=request.user,
        original_name=Path(uploaded_file.name.replace("\\", "/")).name,
    )
    return Response(AttachmentSerializer(attachment).data, status=201)
```

必须把上传权限加入第22章自定义权限类；GET 与 POST 可能需要不同权限。`employee` 和 `uploaded_by` 来自受保护的路径对象和认证用户，不接受客户端伪造。

## 4. 受控下载

文件响应不是普通 JSON，可建立独立 APIView：

```python
from pathlib import Path

from django.http import FileResponse
from rest_framework.permissions import IsAuthenticated
from rest_framework.views import APIView


class AttachmentDownloadView(APIView):
    permission_classes = [IsAuthenticated]

    def get(self, request, attachment_id):
        attachment = get_object_or_404(
            EmployeeAttachment.objects.select_related("employee"),
            pk=attachment_id,
            employee__is_active=True,
        )
        if not request.user.has_perm("employees.view_employee"):
            self.permission_denied(request)
        safe_name = Path(attachment.original_name.replace("\\", "/")).name
        return FileResponse(
            attachment.file.open("rb"),
            as_attachment=True,
            filename=safe_name,
            content_type="application/pdf",
        )
```

Model 权限之外还应套用部门等数据范围。下载 URL 不接受任意磁盘路径，也不能因为前端拿不到链接就公开 `MEDIA_URL`。

## 5. 客户端请求

```powershell
curl.exe -i -X POST http://127.0.0.1:8000/api/employees/12/attachments/ `
  -H "Authorization: Bearer <access-token>" `
  -F "file=@C:\temp\sample.pdf;type=application/pdf"
```

使用 `-F` 时 curl 自动生成 multipart boundary，不要手写一个缺少 boundary 的 `Content-Type`。415 通常表示媒体类型不受支持，400 表示媒体类型已解析但字段或内容校验失败。

## 6. 一致性与运维边界

数据库事务回滚不会自动删除对象存储中已写文件。反过来，存储文件丢失时数据库记录仍可能存在。设计失败补偿、删除策略、定期孤儿文件检查、备份和生命周期时必须同时考虑两边。大文件应流式处理，不能把完整内容读入内存。

## 验证矩阵

- 正常小型 PDF：201，元数据正确，可下载。
- 空文件、超5 MB、错误扩展名、伪装类型：400。
- JSON 请求上传：415或400，按解析器配置确认。
- 无 token：401；无权限/越权：403或项目约定404。
- 不存在附件：404；下载名含路径符时被安全化。
- 日志和错误体不包含 token、文件内容、绝对路径。

## 现场任务

复现“前端上传始终返回415”。使用 Network 对比请求头和请求体，确认是否错误设置 `Content-Type`，修正后补正常/超限/无权限测试证据。不要为了成功而开放所有 parser 或取消校验。

## 完成检查

- [ ] multipart、JSON 和文件响应边界清楚。
- [ ] 大小、类型、文件名、权限都在后端验证。
- [ ] 私有文件不能通过静态 URL 绕过 API。
- [ ] 知道数据库和存储不是天然同一事务。

下一章让独立前端安全地调用这些 API，并集中处理 CORS、CSRF 和错误状态。
