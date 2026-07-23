# 第19章 从页面请求走向 REST API

## 本章成果

在已经完成的员工管理系统中增加两个不依赖模板的 JSON 接口：健康检查和员工列表。完成后能从浏览器 Network 或 HTTP 客户端读懂方法、URL、请求头、状态码和 JSON 响应，并能说明服务端渲染与前后端分离的责任差异。

## 开始状态与边界

- 保留第1–18章的 `Department`、`Employee`、权限和数据，不重新创建业务模型。
- 本章先使用 Django 自带 `JsonResponse` 理解 API 本质；第20章再安装 DRF。
- JSON 不是 REST，REST 也不等于某个框架。本章只建立后续项目需要的资源、方法和响应约定。

## 1. 一体式与分离式的相同和不同

两种项目都经过 URL → View → Model → Response。区别在响应和界面职责：

```text
服务端渲染：浏览器 → Django → Template → HTML
前后端分离：前端应用 → Django API → JSON → 前端组件渲染
```

Django 仍负责数据、校验、认证、授权、日志和数据库事务。前端负责页面状态、交互和 API 调用。分离后并不是“后端不需要懂 HTTP”，反而更需要稳定接口契约。

## 2. 把业务对象看成资源

员工和部门是资源。URL 使用名词表示资源，HTTP 方法表达操作：

| 目的 | 方法与 URL | 成功状态 |
|---|---|---:|
| 员工列表 | `GET /api/employees/` | 200 |
| 员工详情 | `GET /api/employees/12/` | 200 |
| 新增员工 | `POST /api/employees/` | 201 |
| 整体修改 | `PUT /api/employees/12/` | 200 |
| 局部修改 | `PATCH /api/employees/12/` | 200 |
| 删除 | `DELETE /api/employees/12/` | 204 |

常见错误状态：400 表示输入不符合接口要求，401 表示尚未通过认证，403 表示身份已知但无权限，404 表示资源不存在，405 表示该 URL 不接受此方法，500 表示未预期的服务端错误。

## 3. 先做一个健康检查 JSON

在 `employees/api_views.py` 创建：

```python
from django.http import JsonResponse


def api_health(request):
    if request.method != "GET":
        return JsonResponse(
            {"detail": "Method not allowed."},
            status=405,
        )
    return JsonResponse({"status": "ok"})
```

在项目路由追加：

```python
from employees.api_views import api_health

urlpatterns = [
    # 既有路由保持不变
    path("api/health/", api_health, name="api-health"),
]
```

启动后访问 `/api/health/`，响应体应为：

```json
{"status": "ok"}
```

响应头 `Content-Type` 应为 `application/json`。健康检查只证明应用能响应；生产健康还可能区分进程存活、数据库就绪和依赖服务状态。

## 4. 使用 `JsonResponse` 返回员工列表

```python
from django.contrib.auth.decorators import login_required, permission_required
from django.http import JsonResponse

from .models import Employee


@login_required
@permission_required("employees.view_employee", raise_exception=True)
def employee_api_list(request):
    if request.method != "GET":
        return JsonResponse({"detail": "Method not allowed."}, status=405)

    employees = Employee.objects.filter(is_active=True).select_related("department")
    data = [
        {
            "id": employee.pk,
            "employee_number": employee.employee_number,
            "name": employee.name,
            "department": {
                "id": employee.department_id,
                "name": employee.department.name,
            },
            "joined_on": employee.joined_on.isoformat(),
        }
        for employee in employees
    ]
    return JsonResponse({"results": data})
```

这里必须手工把 Model 转为可 JSON 序列化的数据。日期转成字符串，关联对象转成明确字段，不能直接把 QuerySet 放进响应。这些重复工作正是第20章 Serializer 要解决的问题。

## 5. 读取 JSON 请求体

既有项目中可能看到：

```python
import json

try:
    payload = json.loads(request.body)
except json.JSONDecodeError:
    return JsonResponse({"detail": "Invalid JSON."}, status=400)
```

`request.body` 是原始字节，`Content-Type: application/json` 告诉服务端请求体格式。仅仅解析成功不代表输入有效；字段类型、必填、唯一性和业务规则仍需校验。后续使用 DRF 的 `request.data` 和 Serializer 统一处理，不在本章手写完整新增逻辑。

## 6. 接口契约要写清什么

接口不是“返回了 JSON 就完成”。至少约定：

- 方法、路径、认证方式和所需权限。
- 路径、查询、请求头和请求体参数。
- 成功状态、响应字段、类型、空值和日期格式。
- 400/401/403/404 等错误结构。
- 排序、筛选、分页与兼容策略。

字段名一旦被前端使用，随意改名会破坏调用方。需要变更时先调查前端、移动端、批处理和外部系统，再按项目版本策略发布。

## 7. 验证与排错

```powershell
curl.exe -i http://127.0.0.1:8000/api/health/
curl.exe -i -X POST http://127.0.0.1:8000/api/health/
```

员工接口受 Session 认证保护，可先登录页面再用浏览器 Network 观察；也可在自动测试中使用 Client 登录。调查 API 问题时记录：方法、完整 URL、请求头、请求体、响应状态、响应体和服务端请求 ID。不要只截图一个“失败”画面。

## 现场任务

规格：健康检查增加 `service: employee-api` 字段，并保证 POST 仍返回405。修改后给出接口契约、两条请求证据和回归说明。然后调查员工列表为什么不应返回邮箱等不必要个人信息。

## 完成检查

- [ ] 能说明 HTML 响应与 JSON 响应的责任差异。
- [ ] 能区分资源、HTTP 方法、状态码和 JSON。
- [ ] 能读 `request.body`，但知道解析不等于校验。
- [ ] API 仍执行后端登录和权限检查。

下一章使用 DRF Serializer 建立稳定的输入输出边界。
