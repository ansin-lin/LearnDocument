# 第19章 REST API、HTTP 与 Django JSON 响应

## 本章成果

在刚创建的员工项目中实现健康检查和员工列表 JSON 接口。完成后能够从浏览器 Network 或 HTTP 客户端读懂方法、URL、请求头、状态码和 JSON 响应，并沿 URL → View → Model/ORM → Database 追踪第一次 API 请求。

## 本章开始状态

继续使用[从零创建的员工项目](setup.md)。开始修改前，在项目根目录执行 `python manage.py check`，并确认迁移正常、Admin 中至少已有一条员工数据。本章不更换 Model、数据库或数据库连接，只把已有员工数据通过 HTTP 返回为 JSON。

## 本章在整体架构中的位置

```text
Client → HTTP Request → Django URL → View → JSON Response
          ↑ 本章重点                         ↑ 本章重点
```

这里直接使用 Django `JsonResponse`，便于观察 REST API 的最小构成。完成后，员工系统将具备可被浏览器、前端程序或 HTTP 客户端调用的 JSON 入口。

| 概念 | 是什么（What） | 为什么需要（Why） | 什么时候使用（When） |
|---|---|---|---|
| REST API | 以资源、HTTP 方法和统一响应约定提供业务能力的接口 | 让不同客户端通过稳定契约调用同一后端 | Web 前端、移动端或外部系统需要交换业务数据时 |
| Request / Response | 一次 HTTP 交互的请求与响应 | 明确输入、处理结果和失败原因 | 调用、开发或调查任何 API 时 |
| JSON | API 常用的结构化文本数据格式 | 便于不同语言和平台交换数据 | 传递请求体或响应数据时 |

JSON 是数据格式，REST 是接口设计风格，两者都不等同于某个框架。

## 1. REST API 的请求与响应链

客户端不会直接访问 Model 或数据库。一次员工 API 请求按下面的主线流转：

```text
Browser / Mobile / HTTP Client
→ HTTP Request
→ company_portal/urls.py
→ employees/api_views.py
→ Employee Model / ORM
→ Database
→ JsonResponse
→ JSON Response
```

Django 负责路由、数据读取、校验、认证、授权、日志和数据库事务；客户端负责发送 HTTP 请求并使用 JSON 结果。REST API 因此必须提供稳定契约。数据库连接边界见[学习入口](index.md)，这里继续使用已经创建的 Model、迁移和 SQLite 连接。

学过服务端渲染时，可以把 Template → HTML 与这里的 `JsonResponse` → JSON 作可选对照；没有相关经历不影响后续学习。

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

将 `company_portal/urls.py` 整理为下面的完整内容：

```python
from django.contrib import admin
from django.urls import path

from employees.api_views import api_health

urlpatterns = [
    path("admin/", admin.site.urls),
    path("api/health/", api_health, name="api-health"),
]
```

启动后访问 `/api/health/`，响应体应为：

```json
{"status": "ok"}
```

响应头 `Content-Type` 应为 `application/json`。健康检查只证明应用能响应；生产健康还可能区分进程存活、数据库就绪和依赖服务状态。

`JsonResponse(data, status=200)` 是 Django 的 JSON 响应类：`data` 必须是可序列化的字典（传入其他类型时需要额外配置），`status` 接受合法 HTTP 状态码，省略时为200；调用后返回一个 `HttpResponse` 子类。`path(route, view, name=...)` 把相对路径交给 View，`name` 是可选的反向解析名称，本例请求 `/api/health/` 时调用 `api_health()`。

## 4. 使用 `JsonResponse` 返回员工列表

在 `employees/api_views.py` 中追加以下完整 View；如果已有同名 `employee_api_list()`，则替换该函数，不要保留两个同名定义：

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

然后再次修改 `company_portal/urls.py`。下面是加入员工列表后的完整内容，不要在文件中保留第二个 `urlpatterns`：

```python
from django.contrib import admin
from django.urls import path

from employees.api_views import api_health, employee_api_list

urlpatterns = [
    path("admin/", admin.site.urls),
    path("api/health/", api_health, name="api-health"),
    path("api/employees/", employee_api_list, name="api-employee-list"),
]
```

这里必须手工把 Model 转为可 JSON 序列化的数据。日期转成字符串，关联对象转成明确字段，不能直接把 QuerySet 放进响应。这些重复工作正是第20章 Serializer 要解决的问题。

`@login_required` 要求存在已登录用户，未登录时按 Django 登录配置跳转；`@permission_required(permission, raise_exception=False)` 的权限字符串必填，本例设置 `raise_exception=True`，已登录但无权限时返回403而不是跳转。`Employee.objects.filter(is_active=True)` 返回只含在职员工的 QuerySet；`select_related("department")` 用 SQL JOIN 同时读取外键部门，减少逐条查询；日期的 `isoformat()` 返回 ISO 格式字符串。

## 5. 读取 JSON 请求体

以下是 `employees/api_views.py` 中读取请求体的调查片段，不要求加入当前只支持 GET 的 `employee_api_list()`：

```python
import json

try:
    payload = json.loads(request.body)
except json.JSONDecodeError:
    return JsonResponse({"detail": "Invalid JSON."}, status=400)
```

`request.body` 是原始字节，`Content-Type: application/json` 告诉服务端请求体格式。仅仅解析成功不代表输入有效；字段类型、必填、唯一性和业务规则仍需校验。后续使用 DRF 的 `request.data` 和 Serializer 统一处理，不在本章手写完整新增逻辑。

`json` 是 Python 标准库模块，不需要安装。`json.loads(value)` 接受 `str`、`bytes` 或 `bytearray` 并返回对应的 Python 对象；格式不合法时抛出 `json.JSONDecodeError`，本例捕获后返回400。它只负责语法解析，不负责字段和业务校验。

## 6. 接口契约要写清什么

接口不是“返回了 JSON 就完成”。至少约定：

- 方法、路径、认证方式和所需权限。
- 路径、查询、请求头和请求体参数。
- 成功状态、响应字段、类型、空值和日期格式。
- 400/401/403/404 等错误结构。
- 排序、筛选、分页与兼容策略。

字段名一旦被前端使用，随意改名会破坏调用方。需要变更时先调查前端、移动端、批处理和外部系统，再按项目版本策略发布。

## 7. 验证与排错

在项目根目录、已激活虚拟环境且开发服务器正在运行时，另开 PowerShell 执行：

```powershell
curl.exe -i http://127.0.0.1:8000/api/health/
curl.exe -i -X POST http://127.0.0.1:8000/api/health/
```

当前项目暂时使用 Django Session 和 Model 权限保护员工接口。项目创建时建立的超级用户可以登录并拥有 Model 权限。这两个装饰器用于避免接口公开；当前先验证无需登录的健康接口，并观察权限失败状态，第22章再统一改为 JWT。调查 API 问题时记录方法、完整 URL、请求头、请求体、响应状态和响应体，不要只保留一个“失败”画面。

`curl.exe -i URL` 发送请求并显示响应头；`-X POST` 显式指定 POST 方法，省略时默认 GET。本例不写入数据：GET 预期200，POST 预期405。

## 日本企业项目中的实际使用

接口开发通常先确认票据、接口设计书或既有请求，再修改代码。方法、URL、状态码和字段属于团队契约；即使后端能返回数据，也不能未经影响调查随意改变。

## 新人常见错误

- 把“返回 JSON”直接等同于符合 REST 契约。
- 只确认画面，不记录请求方法、URL、状态码和响应体。
- 把401、403、404都当成“没有权限”，没有区分调查方向。
- 认为前端可以绕过 API 直接访问数据库。

## 企业项目调查路径

```text
Browser / HTTP Client → Request → URL / View → Response → Backend Log → Database
```

先固定可复现请求，再从状态码和响应体缩小范围；只有确认请求已进入后端后，才继续检查 View、日志和数据库。

## 现场任务

规格：健康检查增加 `service: employee-api` 字段，并保证 POST 仍返回405。修改后给出接口契约、两条请求证据和回归说明。然后调查员工列表为什么不应返回邮箱等不必要个人信息。

## 完成检查

- [ ] 能沿 URL → View → Model/ORM → Database → JSON 追踪请求。
- [ ] 能区分资源、HTTP 方法、状态码和 JSON。
- [ ] 能读 `request.body`，但知道解析不等于校验。
- [ ] API 仍执行后端登录和权限检查。

下一章使用 DRF Serializer 建立稳定的输入输出边界。
