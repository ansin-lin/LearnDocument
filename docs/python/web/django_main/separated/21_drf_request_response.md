# 第21章 DRF Request、Response 与 CRUD

## 本章成果

完成员工 API 的列表、详情、新增、整体修改、局部修改和逻辑删除；能读懂 APIView、Generic View、ViewSet 和 Router 的职责，并选择 `ModelViewSet + Router` 作为课程主线。

## 1. DRF 的请求和响应

DRF `Request` 包装 Django `HttpRequest`，常用入口为：

| 数据位置 | DRF 写法 | 示例 |
|---|---|---|
| JSON/表单请求体 | `request.data` | 新增、修改字段 |
| 查询参数 | `request.query_params` | `?search=E001` |
| 路径参数 | View 方法参数/`self.kwargs` | `/employees/12/` |
| 用户 | `request.user` | 登录身份 |
| 文件 | `request.FILES` / `request.data` | multipart 上传 |

`Response(data, status=...)` 返回可协商格式的响应。业务代码优先使用 `rest_framework.status` 常量，例如 `status.HTTP_201_CREATED`，避免看不懂的数字散落。

## 2. 四种 View 风格的关系

- `@api_view`：函数式、小接口清楚直接。
- `APIView`：按 `get()`、`post()` 等方法组织，控制细。
- Generic Views + Mixins：组合常见列表、创建、详情行为。
- `ViewSet`：围绕同一资源集中 CRUD action，并由 Router 生成路由。

现场可能同时看到四种写法。不要把“更短”误认为“永远更好”；课程主线选择 `ModelViewSet`，因为员工资源是标准 CRUD，后续筛选、权限和测试也容易统一。

## 3. 创建员工 ViewSet

在 `employees/api_views.py`：

```python
from rest_framework import status, viewsets
from rest_framework.permissions import IsAuthenticated
from rest_framework.response import Response

from .models import Employee
from .serializers import EmployeeSerializer


class EmployeeViewSet(viewsets.ModelViewSet):
    serializer_class = EmployeeSerializer
    permission_classes = [IsAuthenticated]
    queryset = (
        Employee.objects
        .filter(is_active=True)
        .select_related("department")
        .order_by("employee_number")
    )

    def destroy(self, request, *args, **kwargs):
        employee = self.get_object()
        employee.is_active = False
        employee.save(update_fields=["is_active"])
        return Response(status=status.HTTP_204_NO_CONTENT)
```

`list`、`retrieve`、`create`、`update`、`partial_update`、`destroy` 分别对应标准 action。逻辑删除覆盖 `destroy()`，并让基础 QuerySet 排除离职员工。真实项目还要确认重复删除、恢复、唯一性和审计规则。

## 4. 使用 Router 生成路由

创建 `employees/api_urls.py`：

```python
from rest_framework.routers import DefaultRouter

from .api_views import EmployeeViewSet

router = DefaultRouter()
router.register("employees", EmployeeViewSet, basename="employee")

urlpatterns = router.urls
```

项目路由：

```python
path("api/", include("employees.api_urls")),
```

Router 生成列表和详情等 URL 名。可用 `python manage.py show_urls` 的第三方工具不是课程依赖；直接访问 DRF 根页面、查看 `router.urls` 或使用 `reverse("employee-list")` 验证。

## 5. PUT 与 PATCH

PUT 通常表示用完整表示替换资源，PATCH 表示只提交变化字段。DRF `partial_update` 通过 `partial=True` 允许省略其他必填字段。

```json
PATCH /api/employees/12/
{
  "email": "new-address@example.test"
}
```

接口是否允许某字段修改不能只靠前端隐藏。Serializer 的只读字段、不同 action 的 Serializer、权限和业务校验必须共同限制。例如员工编号一旦创建不可修改时，应明确设为只读或在更新校验中拒绝。

## 6. `get_queryset()` 与 `get_serializer_class()`

固定 `queryset` 足够入门，但数据范围依赖用户时应覆盖：

```python
def get_queryset(self):
    queryset = super().get_queryset()
    if self.request.user.has_perm("employees.view_all_departments"):
        return queryset
    return queryset.filter(department=self.request.user.profile.department)
```

这只是结构示例，课程当前 User 尚无 `profile`，不能直接复制运行。重点是：对象级数据范围在服务器 QuerySet 中执行，不能只让前端少传一个部门。

不同 action 需要不同字段时可覆盖 `get_serializer_class()`，但先确认是否真的存在不同契约，避免过度拆分。

## 7. 自定义 action

非标准操作例如“恢复员工”可以使用：

```python
from rest_framework.decorators import action

@action(detail=True, methods=["post"])
def restore(self, request, pk=None):
    ...
```

`detail=True` 表示作用于单个资源。不要把所有业务动作都硬塞成 CRUD；也不要创建含动词但方法和状态码混乱的 URL。自定义 action 必须单独定义权限、输入、幂等性和错误响应。

## 8. CRUD 验证矩阵

| 操作 | 正常 | 必须补测的失败 |
|---|---|---|
| list | 200、只含在职员工 | 未认证、数据范围 |
| retrieve | 200 | 不存在/已离职404 |
| create | 201 | 字段错误400、重复编号400 |
| PUT/PATCH | 200 | 不可改字段、无权限403 |
| DELETE | 204、记录仍存在 | 重复删除404、GET不能删除 |

对每次请求同时确认响应和数据库。204 响应不应携带普通 JSON 正文。

## 现场任务

规格：员工编号创建后禁止修改。先写 PATCH 会修改成功的再现步骤，然后在 Serializer 中实现限制，添加正常修改邮箱与拒绝修改编号的测试观点。Review 说明为什么不能只在前端禁用输入框。

## 完成检查

- [ ] 能沿 Router → ViewSet → Serializer → Model 追踪请求。
- [ ] 能说明 ViewSet action 与 HTTP 方法的对应。
- [ ] PUT、PATCH 和逻辑删除行为清楚。
- [ ] 正常、错误、权限和数据库结果一起验证。

下一章加入适合分离式客户端的 JWT 认证和按 action 的权限控制。
