# 第22章 API 认证、JWT 与权限

## 本章成果

为员工 API 配置 JWT 登录、刷新和失效边界，并让查看与维护操作使用不同权限。学完能区分401与403、认证与授权、前端显示控制与后端强制检查。

## 1. Session、Token 与 JWT

Session 适合同源浏览器页面，浏览器保存 Session Cookie，服务端保存会话状态。分离式客户端常在 `Authorization` 请求头携带访问令牌。JWT 是一种可签名的令牌格式，不代表“天然更安全”或“不需要服务端设计”。

JWT 常见流程：

```text
账号密码 → token端点 → access + refresh
access → 调用业务API
access过期 → refresh端点 → 新access
refresh失效/过期 → 重新登录
```

访问令牌应短期有效；HTTPS 是前提；令牌不得写进日志、URL、Git 或公开截图。浏览器存储位置涉及 XSS、CSRF 和产品架构权衡，不能用一句“放 localStorage”当作通用安全答案。

## 2. 安装 Simple JWT

```powershell
python -m pip install djangorestframework-simplejwt
```

settings：

```python
from datetime import timedelta

REST_FRAMEWORK = {
    "DEFAULT_AUTHENTICATION_CLASSES": [
        "rest_framework_simplejwt.authentication.JWTAuthentication",
    ],
    "DEFAULT_PERMISSION_CLASSES": [
        "rest_framework.permissions.IsAuthenticated",
    ],
}

SIMPLE_JWT = {
    "ACCESS_TOKEN_LIFETIME": timedelta(minutes=15),
    "REFRESH_TOKEN_LIFETIME": timedelta(days=1),
}
```

时长是课程示例，真实项目根据风险、用户体验、撤销策略和组织安全规范确定。

项目路由：

```python
from rest_framework_simplejwt.views import TokenObtainPairView, TokenRefreshView

urlpatterns += [
    path("api/auth/token/", TokenObtainPairView.as_view(), name="token-obtain-pair"),
    path("api/auth/token/refresh/", TokenRefreshView.as_view(), name="token-refresh"),
]
```

## 3. 获取并使用 access token

```powershell
curl.exe -i -X POST http://127.0.0.1:8000/api/auth/token/ `
  -H "Content-Type: application/json" `
  -d '{"username":"api-user","password":"<练习密码>"}'
```

调用业务 API：

```powershell
curl.exe -i http://127.0.0.1:8000/api/employees/ `
  -H "Authorization: Bearer <access-token>"
```

失败时确认 `Bearer`、空格、access/refresh 是否混用、过期时间、服务器时钟和用户状态。不要把完整 token 粘贴到票据或聊天中。

## 4. 认证与授权的响应

- 没有有效凭据且端点要求登录：401，并通常包含 `WWW-Authenticate`。
- 已认证但权限不足：403。
- 为防止资源枚举，某些对象级越权按项目策略返回404。

状态码必须与接口契约和安全策略一致。不能把所有失败都返回200再在 JSON 写 `success: false`，那会破坏客户端、监控和缓存对 HTTP 的理解。

## 5. 按 action 检查 Django Model 权限

```python
from rest_framework.permissions import DjangoModelPermissions


class EmployeeViewSet(viewsets.ModelViewSet):
    permission_classes = [DjangoModelPermissions]
    ...
```

`DjangoModelPermissions` 会把写操作映射到 Model 权限。项目若要求 GET 也必须有 `view` 权限，应确认当前 DRF 版本行为并按项目自定义权限映射。课程使用明确权限类：

```python
from rest_framework.permissions import BasePermission, SAFE_METHODS


class EmployeePermission(BasePermission):
    def has_permission(self, request, view):
        if not request.user or not request.user.is_authenticated:
            return False
        if request.method in SAFE_METHODS:
            return request.user.has_perm("employees.view_employee")
        if view.action == "create":
            return request.user.has_perm("employees.add_employee")
        if view.action in {"update", "partial_update"}:
            return request.user.has_perm("employees.change_employee")
        if view.action == "destroy":
            return request.user.has_perm("employees.delete_employee")
        return False
```

ViewSet 设置 `permission_classes = [EmployeePermission]`。自定义权限默认拒绝未明确允许的 action，新增 action 时必须同步评审。

## 6. 对象级权限与 QuerySet

`has_object_permission()` 可检查单个对象，但列表不会自动逐条调用它。数据范围必须在 `get_queryset()` 过滤，详情和写操作再做对象检查。否则用户可能在列表看到不该看的员工，或通过直接 ID 越权。

权限代码还要避免通过错误差异泄露资源存在性。角色、部门和对象关系应有数据库来源与测试，不信任客户端提交的 `user_id`、`department_id` 来决定权限。

## 7. Refresh、撤销与密码变更

签名正确的 JWT 在过期前可能保持有效。高风险系统需要考虑 refresh rotation、blacklist、用户停用、密码变更、密钥轮换和紧急撤销。Simple JWT 提供 blacklist App 等能力，但是否启用取决于项目策略；课程主线先保证短期 access、受控 refresh 和用户停用测试。

## 8. 权限测试矩阵

| 身份 | GET列表 | POST新增 | PATCH修改 | DELETE |
|---|---:|---:|---:|---:|
| 无 token | 401 | 401 | 401 | 401 |
| 查看组 | 200 | 403 | 403 | 403 |
| 维护组 | 200 | 201 | 200 | 按规格 |
| 过期 token | 401 | 401 | 401 | 401 |

再增加“不在部门数据范围”的对象级用例。每种失败确认数据库没有变化。

## 现场任务

票据：查看组误能 PATCH 员工邮箱。先用最小账号和请求重现，沿 settings → authentication → ViewSet permission → Group 权限调查，修复并添加回归测试。报告中不得包含真实密码或 token。

## 完成检查

- [ ] 能解释 JWT 流程、过期和撤销边界。
- [ ] 401、403、404使用场景清楚。
- [ ] 后端按 action 与数据范围实施权限。
- [ ] token 不进入 URL、日志、仓库和交付截图。

下一章把第14章的安全文件原则迁移到 multipart API。
