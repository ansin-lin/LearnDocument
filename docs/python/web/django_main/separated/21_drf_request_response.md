# 第21章 DRF Request、Response 与 CRUD

## 本章成果

完成员工 API 的列表、详情、新增、整体修改、局部修改和逻辑删除；能读懂 APIView、Generic View、ViewSet 和 Router 的职责，并使用 `ModelViewSet + Router` 组织标准 CRUD。

## 本章开始状态与修改清单

- 第20章的函数式 `employee_api_list` 已支持 GET 和 POST。
- 本章用 `EmployeeViewSet` 替换该函数式员工接口，新建 `employees/api_urls.py` 并接入项目路由。
- 本章不修改 Model；第22章再加入依赖用户身份的数据范围。

## 本章在整体架构中的位置

```text
Request → Router → ViewSet → Serializer → Model
           ↑          ↑ 本章重点
Response ← action ←───┘
```

完成后，员工资源将具备统一路由下的完整 CRUD，请求可以从 URL 稳定追踪到具体 action。

| 概念 | 是什么（What） | 为什么需要（Why） | 什么时候使用（When） |
|---|---|---|---|
| APIView | 按 HTTP 方法组织处理逻辑的 DRF 类 View | 在保留细粒度控制时复用 DRF 请求响应能力 | 接口流程特殊、难以抽象为标准 CRUD 时 |
| Generic View | 把常见查询和保存流程组合成可配置 View | 减少列表、创建、详情等重复代码 | 接口接近通用 CRUD、但不需要完整 ViewSet 时 |
| ViewSet | 按资源集中组织 list、create 等 action | 统一资源行为、权限和查询入口 | 一个业务资源需要多种标准操作时 |
| Router | 根据 ViewSet 自动生成 URL pattern | 减少重复路由并保持命名一致 | 使用 ViewSet 暴露标准资源 URL 时 |

## 1. DRF 的请求和响应

DRF `Request` 包装 Django `HttpRequest`，常用入口为：

| 数据位置 | DRF 写法 | 示例 |
|---|---|---|
| JSON/表单请求体 | `request.data` | 新增、修改字段 |
| 查询参数 | `request.query_params` | `?search=E001` |
| 路径参数 | View 方法参数/`self.kwargs` | `/employees/12/` |
| 用户 | `request.user` | 登录身份 |
| 文件 | `request.FILES` / `request.data` | multipart 上传 |

View 方法的命名路径参数由 Router 或 `path()` 提供，也会保存在 `self.kwargs` 字典中。例如详情路径中的员工 ID 可以作为方法参数 `pk` 取得，或使用 `self.kwargs["pk"]` 读取；键不存在时下标访问会抛出 `KeyError`。业务代码优先使用框架已经传入的方法参数或 `get_object()`，不要自行解析 URL 字符串。

`Response(data, status=...)` 返回可协商格式的响应。业务代码优先使用 `rest_framework.status` 常量，例如 `status.HTTP_201_CREATED`，避免看不懂的数字散落。

## 2. 四种 View 风格的关系

- `@api_view`：函数式、小接口清楚直接。
- `APIView`：按 `get()`、`post()` 等方法组织，控制细。
- Generic Views + Mixins：组合常见列表、创建、详情行为。
- `ViewSet`：围绕同一资源集中 CRUD action，并由 Router 生成路由。

以下代码只用于比较三种类 View 的结构，不写入当前项目：

```python
from rest_framework import generics, viewsets
from rest_framework.response import Response
from rest_framework.views import APIView

from .models import Employee
from .serializers import EmployeeSerializer


class EmployeeListAPIView(APIView):
    def get(self, request):
        return Response([])


class EmployeeListCreateView(generics.ListCreateAPIView):
    queryset = Employee.objects.all()
    serializer_class = EmployeeSerializer


class EmployeeViewSet(viewsets.ModelViewSet):
    queryset = Employee.objects.all()
    serializer_class = EmployeeSerializer
```

`APIView` 把 GET 请求交给 `get()`，查询、序列化和响应都由开发者明确编写，适合特殊流程。`ListCreateAPIView` 已组合列表与新增行为，开发者主要提供 `queryset` 和 `serializer_class`，它只处理这一组通用操作。`ModelViewSet` 再把列表、新增、详情、修改和删除集中到一个资源类中，通常与 Router 配合使用。

这段对照省略了当前项目必须使用的认证、权限、数据范围和查询优化，不能直接替换后面的 `EmployeeViewSet`。本章要求能够识别三种类 View 的职责差异，实际项目主线只实现 `ModelViewSet`；第25章的受控下载会真正使用 `APIView`。

现场可能同时看到四种写法。不要把“更短”误认为“永远更好”。员工资源属于标准 CRUD，使用 `ModelViewSet` 可以统一后续筛选、权限和测试入口。

## 3. 创建员工 ViewSet

将 `employees/api_views.py` 整理为下面的完整内容。删除第20章的 `employee_api_list()`，保留健康检查：

```python
from django.http import JsonResponse
from rest_framework import status, viewsets
from rest_framework.permissions import IsAuthenticated
from rest_framework.response import Response

from .models import Employee
from .serializers import EmployeeSerializer


def api_health(request):
    if request.method != "GET":
        return JsonResponse(
            {"detail": "Method not allowed."},
            status=405,
        )
    return JsonResponse({"status": "ok"})


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

`viewsets.ModelViewSet` 是包含标准 CRUD action 的基类；`serializer_class`、`permission_classes` 和 `queryset` 分别指定序列化器、权限类列表和基础数据集合。`QuerySet.order_by(*fields)` 接受一个或多个字段名并返回新的 QuerySet，字段名前加 `-` 表示倒序；显式调用会覆盖 Model 的默认排序。本例按具有唯一约束的员工编号升序，结果已经稳定；第22章仍追加 `pk` 作为明确的次排序字段，使以后调整编号约束或首排序字段时不易破坏分页稳定性。

`destroy(self, request, *args, **kwargs)` 在 DELETE 详情接口调用；`self.get_object()` 按当前 QuerySet 取得对象并执行对象权限检查，`save(update_fields=["is_active"])` 只更新指定字段，最后返回无正文的204。

## 4. 使用 Router 生成路由

创建 `employees/api_urls.py`：

```python
from rest_framework.routers import DefaultRouter

from .api_views import EmployeeViewSet

router = DefaultRouter()
router.register("employees", EmployeeViewSet, basename="employee")

urlpatterns = router.urls
```

将 `company_portal/urls.py` 整理为下面的完整内容。必须删除第19章直接指向 `employee_api_list` 的导入和 `path("api/employees/", ...)`，员工路由只交给 Router：

```python
from django.contrib import admin
from django.urls import include, path

from employees.api_views import api_health

urlpatterns = [
    path("admin/", admin.site.urls),
    path("api/health/", api_health, name="api-health"),
    path("api-auth/", include("rest_framework.urls")),
    path("api/", include("employees.api_urls")),
]
```

Router 生成列表和详情等 URL 名。未安装第三方 `show_urls` 命令时，直接访问 DRF 根页面、查看 `router.urls` 或使用 `reverse("employee-list")` 验证。`reverse(viewname, args=None, kwargs=None)` 根据路由名称生成 URL 字符串；`viewname` 是必填路由名，`args` 接受位置参数序列，`kwargs` 接受关键字参数字典，二者都可省略但不能同时提供。列表路由不需要对象 ID，因此本例只传 `viewname`。路由名不存在时会抛出表示反向解析失败的 `NoReverseMatch` 异常。

`DefaultRouter()` 创建带 API 根页面的路由器；`register(prefix, viewset, basename=...)` 的 `prefix` 是不带前导斜线的资源路径，`viewset` 是必填处理类，`basename` 用于生成 `employee-list`、`employee-detail` 等名称。`router.urls` 返回 Django URL pattern 列表，交给项目路由的 `include()` 接入。

## 5. PUT 与 PATCH

PUT 通常表示用完整表示替换资源，PATCH 表示只提交变化字段。DRF `partial_update` 通过 `partial=True` 允许省略其他必填字段。下面是 HTTP 请求示例，不是需要保存到项目中的 JSON 文件：

```json
PATCH /api/employees/12/
{
  "email": "new-address@example.test"
}
```

接口是否允许某字段修改不能只靠前端隐藏。Serializer 的只读字段、不同 action 的 Serializer、权限和业务校验必须共同限制。例如员工编号一旦创建不可修改时，应明确设为只读或在更新校验中拒绝。

## 6. `get_queryset()` 与 `get_serializer_class()`

固定 `queryset` 足够完成当前 CRUD，但企业接口通常需要根据登录用户限制部门和离职数据。用户—部门访问关系建立后，再把固定 `queryset` 改成 `get_queryset()`；不要引用尚不存在的 `user.profile`，也不能只依赖客户端传入部门。

不同 action 需要不同字段时可覆盖 `get_serializer_class(self)`。该方法不接收业务参数，必须返回 Serializer **类**，DRF 随后才会用它创建实例；不要在这里返回 `EmployeeSerializer()` 实例。当前 CRUD 使用同一契约，不需要覆盖；只有列表、详情或写入确实使用不同字段集合时才采用这种方式，避免过度拆分。

## 7. 自定义 action

非标准操作例如“恢复员工”可以使用。以下片段追加到 `employees/api_views.py` 的 `EmployeeViewSet` 类内，导入放在文件顶部；省略号表示业务实现尚未提供，因此该片段不能直接运行：

```python
from rest_framework.decorators import action

@action(detail=True, methods=["post"])
def restore(self, request, pk=None):
    ...
```

`detail=True` 表示作用于单个资源。不要把所有业务动作都硬塞成 CRUD；也不要创建含动词但方法和状态码混乱的 URL。自定义 action 必须单独定义权限、输入、幂等性和错误响应。

`@action(detail, methods=...)` 把 ViewSet 方法注册为额外路由：`detail` 必填，`True` 时 URL 包含对象主键；`methods` 接受小写 HTTP 方法列表，省略时默认只接受 GET。本例会形成面向单个员工的 POST action，方法返回值必须是 DRF `Response`。

## 8. CRUD 验证矩阵

| 操作 | 正常 | 必须补测的失败 |
|---|---|---|
| list | 200、只含在职员工 | 未认证、数据范围 |
| retrieve | 200 | 不存在/已离职404 |
| create | 201 | 字段错误400、重复编号400 |
| PUT/PATCH | 200 | 不可改字段、无权限403 |
| DELETE | 204、记录仍存在 | 重复删除404、GET不能删除 |

对每次请求同时确认响应和数据库。204 响应不应携带普通 JSON 正文。

## 日本企业项目中的实际使用

标准资源通常使用 ViewSet 与 Router 统一 CRUD；特殊流程再选择 APIView 或自定义 action。现场 Review 会重点确认 action、HTTP 方法、权限、状态码和副作用是否一致。

## 新人常见错误

- 注册 Router 时漏写 `basename`，且 ViewSet 没有固定 `queryset`。
- 混淆 PUT 与 PATCH，导致未提交字段被覆盖或错误设为必填。
- 覆盖 `destroy()` 后忘记返回204，或误做物理删除。
- 自定义 action 没有单独检查权限、输入和幂等性。

## 企业项目调查路径

```text
Method + URL → Router → ViewSet action → get_queryset()
→ Permission → Serializer → Response / Database
```

先确认请求命中了哪个 action，再检查 QuerySet、权限和 Serializer；不能只凭函数名猜测执行路径。

## 现场任务

规格：员工编号创建后禁止修改。先写 PATCH 会修改成功的再现步骤，然后在 Serializer 中实现限制，添加正常修改邮箱与拒绝修改编号的测试观点。Review 说明为什么不能只在前端禁用输入框。

## 完成检查

- [ ] 能沿 Router → ViewSet → Serializer → Model 追踪请求。
- [ ] 能说明 ViewSet action 与 HTTP 方法的对应。
- [ ] PUT、PATCH 和逻辑删除行为清楚。
- [ ] 正常、错误、权限和数据库结果一起验证。

下一章加入适合分离式客户端的 JWT 认证和按 action 的权限控制。
