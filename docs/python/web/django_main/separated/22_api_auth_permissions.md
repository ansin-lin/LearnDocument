# 第22章 API 认证、JWT 与权限

## 本章成果

为员工 API 配置 JWT 登录、刷新和失效边界，让查看与维护操作使用不同权限，并把普通用户的数据限制在已分配部门内。完成后能够区分401、403与隐藏资源时的404，并说明认证、操作权限和数据范围分别在哪里执行。

## 本章开始状态与修改清单

- 第21章的 `EmployeeViewSet + Router` CRUD 可以运行。
- 本章记录 Simple JWT 依赖，修改 settings 与项目路由。
- 新增用户—部门访问 Model、迁移、`employees/access.py` 和自定义权限类，并修改 Serializer 与 ViewSet。

## 本章在整体架构中的位置

```text
JWT → Authentication → request.user → Permission → QuerySet 数据范围
 ↑          ↑                ↑             ↑ 本章重点
Request ─────────────────────────────────────────→ ViewSet
```

完成后，API 不再只判断“是否发送请求”，而是依次确认调用者身份、可执行操作和可访问数据。

| 概念 | 是什么（What） | 为什么需要（Why） | 什么时候使用（When） |
|---|---|---|---|
| JWT | 可签名并携带声明的令牌格式 | 让 API 客户端在请求头中提交可验证身份凭据 | 分离式客户端调用受保护 API 时 |
| Authentication | 把凭据解析为 `request.user` 的过程 | 为后续权限判断提供可信身份 | 每次访问受保护接口时 |
| Permission | 判断当前用户能否执行某个操作的组件 | 把授权规则集中到后端强制实施 | list、create、update 等 action 执行前 |
| QuerySet 数据范围 | 在查询入口限制用户可以看到的记录集合 | 防止列表、详情或直接 ID 越权 | 数据按部门、组织或负责人隔离时 |

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

JWT、Permission 和 QuerySet 数据范围解决的是三个不同问题：

```text
JWT Authentication：你是谁
→ Permission：你能执行什么操作
→ QuerySet 数据范围：你能操作哪些记录
```

只验证 JWT，不能阻止普通用户修改其他部门员工；只检查操作权限，不能阻止用户通过列表或直接 ID 读取未授权数据；只在前端隐藏按钮，不能形成任何后端安全边界。DRF 会先认证并建立 `request.user`，再检查 Permission，随后 ViewSet 从受限 QuerySet 中取得对象。

## 2. 安装 Simple JWT

在项目根目录激活虚拟环境后执行：

```powershell
python -m pip install "djangorestframework-simplejwt==5.5.1"
```

在项目根目录的 `requirements.txt` 中追加：

```text
djangorestframework-simplejwt==5.5.1
```

在 `company_portal/settings.py` 中追加或合并以下配置：

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

这里把 access 设为15分钟、refresh 设为1天。实际时长应根据风险、用户体验、撤销策略和组织安全规范确定。

在 `company_portal/urls.py` 顶部追加导入，并把 token 路由加入 `urlpatterns`：

```python
from rest_framework_simplejwt.views import TokenObtainPairView, TokenRefreshView

urlpatterns += [
    path("api/auth/token/", TokenObtainPairView.as_view(), name="token-obtain-pair"),
    path("api/auth/token/refresh/", TokenRefreshView.as_view(), name="token-refresh"),
]
```

JWT 启用后，从 `company_portal/urls.py` 删除第20章仅用于 Session 联调的 `path("api-auth/", include("rest_framework.urls"))`。Admin 的 `/admin/` 登录不受影响，业务 API 统一使用 JWT。

`JWTAuthentication` 从 `Authorization: Bearer <token>` 解析身份并设置 `request.user`。`TokenObtainPairView.as_view()` 返回接收用户名和密码、签发 access/refresh 的 Django View；`TokenRefreshView.as_view()` 接收必填 `refresh` 字段并返回新的 access。`timedelta()` 来自 Python 标准库 `datetime` 模块，接收 `days`、`seconds`、`minutes` 等数值并返回表示一段时间的 `timedelta` 对象；Simple JWT 用它计算 token 过期时间。`minutes=15` 和 `days=1` 是本例时长，不是 Simple JWT 的固定默认策略。

## 3. 获取并使用 access token

在项目根目录、开发服务器运行且练习账号已创建时，使用 PowerShell 执行：

```powershell
curl.exe -i -X POST http://127.0.0.1:8000/api/auth/token/ `
  -H "Content-Type: application/json" `
  -d '{"username":"<超级用户名>","password":"<练习密码>"}'
```

取得响应中的 access 后，在同一 PowerShell 调用业务 API：

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

以下对照片段位于 `employees/api_views.py` 的 `EmployeeViewSet` 类定义处，用于观察 DRF 内置权限；随后会用明确的自定义权限替换它：

```python
from rest_framework.permissions import DjangoModelPermissions


class EmployeeViewSet(viewsets.ModelViewSet):
    permission_classes = [DjangoModelPermissions]
    ...
```

`DjangoModelPermissions` 会把写操作映射到 Model 权限。项目若要求 GET 也必须有 `view` 权限，应确认当前 DRF 版本行为并按项目自定义权限映射。下面改用明确权限类：

在 `employees/permissions.py` 中创建以下权限类，然后在 `employees/api_views.py` 导入并设置 `permission_classes = [EmployeePermission]`：

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

`BasePermission` 是自定义权限的基类；`has_permission(self, request, view)` 返回布尔值，DRF 在进入 action 前调用它。`SAFE_METHODS` 是只读方法集合，当前包含 GET、HEAD 和 OPTIONS；`request.user.has_perm("app_label.codename")` 返回用户是否拥有指定 Django 权限。这里未明确允许的 action 返回 `False`，避免新增接口默认放行。

## 6. 建立可运行的部门数据范围

数据范围必须来自可信关系，不能由客户端提交的部门 ID 决定。这里增加用户—部门关系：超级用户可查看全部部门，普通用户只能查看分配给自己的部门。

在 `employees/models.py` 追加：

```python
from django.conf import settings


class UserDepartmentAccess(models.Model):
    user = models.ForeignKey(
        settings.AUTH_USER_MODEL,
        on_delete=models.CASCADE,
        related_name="department_accesses",
    )
    department = models.ForeignKey(
        Department,
        on_delete=models.CASCADE,
        related_name="user_accesses",
    )

    class Meta:
        constraints = [
            models.UniqueConstraint(
                fields=["user", "department"],
                name="unique_user_department_access",
            )
        ]

    def __str__(self) -> str:
        return f"{self.user} → {self.department}"
```

同时在 `employees/models.py` 的 `Employee.Meta` 中保留既有排序并增加一个自定义权限：

```python
class Meta:
    ordering = ["employee_number"]
    permissions = [
        ("view_inactive_employee", "Can view inactive employees"),
]
```

`models.ForeignKey(to, on_delete, related_name=...)` 创建多对一关系：`to` 指向关联模型，`on_delete=models.CASCADE` 表示关联用户或部门删除时同时删除访问关系，`related_name` 是反向查询名称。`models.UniqueConstraint(fields=..., name=...)` 要求用户与部门组合唯一，并生成数据库约束；`fields` 和 `name` 都是必填参数。

`django.conf.settings` 是 Django 当前有效配置的代理对象；`settings.AUTH_USER_MODEL` 返回项目配置的用户模型标签。外键通过它引用用户，而不是硬编码 `auth.User`，从而兼容项目替换用户模型的情况。

在项目根目录激活虚拟环境后生成并检查迁移：

```powershell
python manage.py makemigrations employees
python manage.py sqlmigrate employees 0002
python manage.py migrate
python manage.py check
```

`sqlmigrate app_label migration_name` 显示某个迁移将执行的 SQL，不会修改数据库；`app_label` 是 App 标签，本例为 `employees`，`migration_name` 是不带 `.py` 的迁移名称，本例暂写 `0002`。迁移编号以实际生成的文件名为准。查看 SQL 并确认目标环境后再执行 `migrate`；生产迁移还要遵守备份、发布和回滚手顺。

为了通过 Admin 建立可观察的数据范围，修改 `employees/admin.py` 顶部的 Model 导入，并在文件末尾追加管理类：

```python
from .models import Department, Employee, UserDepartmentAccess


@admin.register(UserDepartmentAccess)
class UserDepartmentAccessAdmin(admin.ModelAdmin):
    list_display = ["user", "department"]
    list_filter = ["department"]
    search_fields = ["user__username", "department__name"]
```

文件中原有的 `DepartmentAdmin` 和 `EmployeeAdmin` 保持不变；只替换 Model 导入并追加新类。启动服务器，以超级用户进入 `/admin/`，依次准备：

1. 新增部门“营业部”，并新增属于营业部的员工 `E002`。
2. 在 Users 中创建普通账号 `api-viewer` 和 `api-maintainer`，分别设置本地练习密码并保持 Active。
3. 只给 `api-viewer` 分配 `employees.view_employee` 权限。
4. 给 `api-maintainer` 分配 `view_employee`、`add_employee`、`change_employee`、`delete_employee` 权限。
5. 在 User department accesses 中建立 `api-viewer → 开发部` 和 `api-maintainer → 开发部` 两条关系。

分别把第3节 token 命令中的用户名改为 `api-viewer` 和 `api-maintainer`，取得各自的 access token。先用 `api-viewer` 验证：员工列表只返回开发部员工，直接访问营业部员工详情返回404，POST、PATCH和DELETE返回403。再用 `api-maintainer` 验证：可以维护开发部员工，但把员工创建或移动到营业部时返回400，数据库不发生越权变化。

这里直接给用户分配权限，便于观察权限差异。实际项目通常通过 Django Group 统一维护“查看者”“维护者”等角色，再把用户加入对应组。无论权限来自用户还是组，`request.user.has_perm()` 的检查方式相同。客户端提交的 `department_id` 不能决定当前用户的数据范围。

## 7. 在 QuerySet 中统一限制范围

创建 `employees/access.py`：

```python
from django.db.models import QuerySet

from .models import Employee, UserDepartmentAccess


def scope_employee_queryset(
    queryset: QuerySet[Employee],
    user,
) -> QuerySet[Employee]:
    if not user or not user.is_authenticated:
        return queryset.none()
    if user.is_superuser:
        return queryset

    department_ids = UserDepartmentAccess.objects.filter(
        user=user,
    ).values_list("department_id", flat=True)
    return queryset.filter(department_id__in=department_ids)
```

`QuerySet[Employee]` 是类型提示，表示参数和返回值应是以 `Employee` 为模型的 QuerySet，便于阅读和静态检查；它不会在运行时自动校验元素类型，也不会执行数据库查询。真正的数据限制来自函数内部的 `none()`、`filter()` 和最终返回值。

然后把第21章的固定 `queryset` 改为：

以下代码替换 `employees/api_views.py` 中 `EmployeeViewSet` 的固定 `queryset`，导入放在文件顶部：

```python
from .access import scope_employee_queryset


class EmployeeViewSet(viewsets.ModelViewSet):
    ...

    def get_queryset(self):
        queryset = (
            Employee.objects
            .select_related("department")
            .order_by("employee_number", "pk")
        )
        queryset = scope_employee_queryset(queryset, self.request.user)
        if not self.request.user.has_perm(
            "employees.view_inactive_employee"
        ):
            queryset = queryset.filter(is_active=True)
        return queryset
```

列表、详情、修改、逻辑删除和后续附件 action 都通过 `self.get_queryset()` / `self.get_object()` 取得对象，因此共用同一范围。不要在每个 action 复制一套过滤条件。

创建或修改员工时，还要验证请求中的部门属于当前用户范围。在 `employees/serializers.py` 的 `EmployeeSerializer` 类中追加：

```python
from .models import UserDepartmentAccess


def validate_department(self, department):
    request = self.context.get("request")
    user = getattr(request, "user", None)
    if user and user.is_superuser:
        return department
    if (
        user
        and user.is_authenticated
        and UserDepartmentAccess.objects.filter(
            user=user,
            department=department,
        ).exists()
    ):
        return department
    raise serializers.ValidationError("不能选择未授权部门。")
```

DRF的ViewSet会把当前request放入Serializer context。这样前端即使篡改部门ID，也不能把员工创建或移动到未授权部门。

`QuerySet.exists()` 不接收参数，用尽量精简的数据库查询判断当前条件是否至少命中一条记录，并返回布尔值。本例只关心“用户与部门的授权关系是否存在”，不需要读取完整授权对象，因此使用 `exists()`。

`get_queryset(self)` 必须返回当前请求可操作的 QuerySet，DRF 的列表和详情 action 都会调用它。`QuerySet.none()` 返回不会命中记录的空 QuerySet；`values_list("department_id", flat=True)` 只取一列 ID，`flat=True` 时返回一维结果；`filter(department_id__in=...)` 用这些 ID 继续缩小集合。`self.context.get("request")` 从 Serializer 上下文取得当前请求，缺失时返回 `None`，因此校验仍需处理非 HTTP 调用场景。

`has_object_permission()` 适合补充单对象规则，但列表不会自动逐条调用它。数据范围必须先进入 QuerySet，否则用户可能在列表看到不该看的员工，或通过直接 ID 越权。

### 阶段检查：分别验证三层控制

完成到这里后，暂时不要继续增加其他功能。分别使用两个账号确认：

1. 不带 token 请求列表返回401，说明 Authentication 生效。
2. `api-viewer` 写入返回403，说明操作 Permission 生效。
3. 两个账号都看不到营业部员工，说明 QuerySet 数据范围生效。
4. `api-maintainer` 向营业部写入返回400且数据库不变化，说明 Serializer 的部门输入校验生效。

如果结果不符，按“JWT → `request.user` → `EmployeePermission` → `get_queryset()` → `validate_department()`”的顺序调查，不要同时修改所有组件。

## 8. Refresh、撤销与密码变更

签名正确的 JWT 在过期前可能保持有效。高风险系统需要考虑 refresh rotation、blacklist、用户停用、密码变更、密钥轮换和紧急撤销。Simple JWT 提供 blacklist App 等能力，但是否启用取决于项目策略；当前实现先保证短期 access、受控 refresh 和用户停用测试。

## 9. 权限与数据范围测试矩阵

| 身份/条件 | 预期 |
|---|---|
| 无 token | 列表和写操作均401 |
| `api-viewer`、已分配部门 | 列表200，只含该部门在职员工 |
| `api-viewer`、直接访问其他部门员工ID | 404 |
| `api-viewer` 提交POST/PATCH/DELETE | 403 |
| `api-maintainer`、已分配部门 | 按Model权限执行写操作 |
| `api-maintainer` 提交未授权部门ID | 400，数据库不变化 |
| 无 `view_inactive_employee` | 离职员工不出现在列表和详情 |
| 有 `view_inactive_employee` | 可按接口契约查询离职员工 |
| 过期token或停用用户 | 401 |

认证测试应真实调用token获取和refresh端点；业务权限测试可使用 `force_authenticate()` 聚焦本项目规则。每种失败都确认数据库没有变化。

## 日本企业项目中的实际使用

日本企业项目常把“认证”“操作权限”“数据范围”分开设计和测试。JWT 只证明令牌通过验证，不代表当前用户可以修改任意员工，也不代表列表可以返回全部部门数据。

## 新人常见错误

- 把请求头写成 `Authentication`，或漏写 `Bearer`。
- 混用 access 与 refresh token，或把长期 refresh 用于业务 API。
- 只在前端隐藏按钮，没有后端 Permission。
- 只写对象权限，忘记列表不会逐条调用 `has_object_permission()`。
- 在 Serializer 中过滤输出，却让未授权记录先进入 QuerySet。

## 企业项目调查路径

```text
Authorization header → JWT 验证 → request.user → Permission
→ get_queryset() 数据范围 → action → Response
```

401先查凭据和认证配置，403再查操作权限，隐藏资源的404查数据范围。调查时不得把完整 token 贴入日志或票据。

## 现场任务

票据：开发部查看者能通过员工ID读取营业部员工。先建立两个部门、两个员工和一条用户部门访问记录重现，沿 token → permission → `get_queryset()` → 数据库关系调查，修复并添加列表与详情回归测试。报告中不得包含真实密码或token。

## 完成检查

- [ ] 能解释 JWT 流程、过期和撤销边界。
- [ ] 401、403、404使用场景清楚。
- [ ] 用户—部门关系有迁移和可验证数据。
- [ ] 列表、详情、修改、删除和附件共用后端数据范围。
- [ ] token 不进入 URL、日志、仓库和交付截图。

下一章在授权后的 QuerySet 上加入过滤、排序、分页和 OpenAPI，先稳定列表契约再进行前端联调。
