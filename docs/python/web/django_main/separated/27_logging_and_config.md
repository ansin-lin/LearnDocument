# 第27章 API 配置、日志与异常响应

## 本章成果

把开发、测试和生产的可变配置从代码中分离；请求ID贯穿响应与日志；DRF预期错误保留正确状态和字段，未预期API异常返回通用JSON 500且只在服务端记录堆栈。

## 本章开始状态与修改清单

- 第26章的 API 测试与 schema 验证可以重复通过。
- 本章修改环境化 settings 和请求 ID 中间件，新建 `company_portal/exceptions.py`。
- 不改变员工业务规则、数据库结构或前端契约。

## 本章在整体架构中的位置

```text
Request → Middleware（请求 ID）→ ViewSet → Exception Handler → JSON Response
   └──────────────────────────→ Structured Log ←──────────────┘
                      环境配置控制各组件行为
```

完成后，同一请求可以从前端错误追踪到后端日志，同时避免把秘密和异常堆栈暴露给客户端。

| 概念 | 是什么（What） | 为什么需要（Why） | 什么时候使用（When） |
|---|---|---|---|
| Middleware | 在 View 前后处理所有请求的共通组件 | 统一添加请求 ID 等横切处理 | 逻辑需要覆盖多数请求且不属于单一业务时 |
| 结构化日志 | 使用稳定字段记录事件的日志 | 便于按请求、用户和状态检索关联信息 | 调查测试或生产环境问题时 |
| Exception Handler | 把异常转换为受控 API 响应的统一入口 | 保持错误契约并隐藏内部堆栈 | DRF 接口出现预期或未预期异常时 |

## 先区分配置、日志与异常响应

这三种机制都与运行调查有关，但职责不同：

| 机制 | 负责什么 | 不负责什么 |
|---|---|---|
| 环境配置 | 决定当前环境使用的主机、秘密、数据库、日志级别等值 | 不处理单次请求错误 |
| 日志 | 在服务端保留请求和异常调查线索 | 不作为返回给客户端的错误正文 |
| 异常响应 | 用稳定状态码和 JSON 告诉客户端请求结果 | 不向客户端公开堆栈和内部配置 |

```text
环境变量 → Django settings → 控制当前运行环境

Request
→ Middleware 生成 request_id
→ ViewSet / Serializer / Permission
→ Exception Handler 生成安全响应
→ Response 带回 request_id
→ 服务端日志使用同一 request_id 调查
```

请求 ID 是关联标识，不是用户身份或安全凭据。前端报告错误时可以提供请求 ID，开发人员再用它查找后端日志；日志中仍不能记录密码、Token、Cookie、完整附件或不必要的个人信息。

## 1. 哪些配置会随环境变化

至少包括：`SECRET_KEY`、`DEBUG`、`ALLOWED_HOSTS`、数据库连接、允许的前端 origin、日志级别、文件存储、JWT 时长/签名策略和外部服务地址。配置名称与默认值写入说明，秘密值由部署环境提供。

在 `company_portal/settings.py` 中删除 Django 创建时原有的 `SECRET_KEY`、`DEBUG` 和 `ALLOWED_HOSTS` 定义，再加入下面的完整配置片段：

```python
import os

from django.core.exceptions import ImproperlyConfigured


DEBUG = os.getenv("DJANGO_DEBUG", "True").lower() == "true"

SECRET_KEY = os.getenv("DJANGO_SECRET_KEY")
if not SECRET_KEY:
    if DEBUG:
        SECRET_KEY = "django-insecure-local-development-only"
    else:
        raise ImproperlyConfigured(
            "DJANGO_SECRET_KEY is required when DEBUG=False."
        )

default_hosts = "127.0.0.1,localhost" if DEBUG else ""
ALLOWED_HOSTS = [
    host.strip()
    for host in os.getenv("DJANGO_ALLOWED_HOSTS", default_hosts).split(",")
    if host.strip()
]
if not DEBUG and not ALLOWED_HOSTS:
    raise ImproperlyConfigured(
        "DJANGO_ALLOWED_HOSTS is required when DEBUG=False."
    )
```

本地未设置变量时继续使用明确标注的开发值，因此前面章节的命令仍可运行；`DEBUG=False` 时，秘密或主机缺失会让启动立即失败。开发默认值绝不能用于生产。可以用 `.env.example` 列出变量名，但 Django 不会自动读取 `.env`；若使用环境加载库，应明确直接依赖、加载位置和覆盖顺序。

`os` 是 Python 标准库模块。`os.getenv(name, default=None)` 在变量缺失时返回默认值。`ImproperlyConfigured` 表示项目配置不完整，适合在不安全的生产配置下停止启动。本例 `DEBUG` 只接受大小写不敏感的字符串 `true` 作为开启值，`ALLOWED_HOSTS` 使用逗号分隔并去除空白项。

在项目根目录、不设置任何新环境变量时执行 `python manage.py check`，本地开发应继续通过。第28章再使用当前 PowerShell 进程的环境变量验证 `DEBUG=False`。

## 2. settings 拆分的读法

现场可能看到 `settings/base.py`、`development.py`、`production.py`，也可能保持一个文件由环境变量控制。两者都可以，重点是：共通配置只有一份、差异清楚、启动入口选择明确、测试不会误连生产资源。

```text
settings/
├── __init__.py
├── base.py
├── development.py
└── production.py
```

使用拆分配置时，`DJANGO_SETTINGS_MODULE` 决定加载哪一个模块。调查问题要先记录实际环境变量和最终有效配置，但日志中不得打印秘密。

## 3. API 日志字段

结构化日志至少帮助回答：什么时候、哪个环境、哪次请求、哪个端点、什么方法、什么状态、耗时多久、由哪个用户或角色触发。

创建 `company_portal/middleware.py`，加入下面的完整实现，使请求期间和普通启动日志都能安全取得 `request_id`：

```python
import logging
import uuid
from contextvars import ContextVar


request_id_context = ContextVar("request_id", default="-")


class RequestIdFilter(logging.Filter):
    def filter(self, record):
        record.request_id = request_id_context.get()
        return True


class RequestIdMiddleware:
    def __init__(self, get_response):
        self.get_response = get_response

    def __call__(self, request):
        request.request_id = uuid.uuid4().hex
        token = request_id_context.set(request.request_id)
        try:
            response = self.get_response(request)
            response["X-Request-ID"] = request.request_id
            return response
        finally:
            request_id_context.reset(token)
```

创建文件后，还必须在 `company_portal/settings.py` 的 `MIDDLEWARE` 中注册。下面给出当前项目的完整顺序：

```python
MIDDLEWARE = [
    "django.middleware.security.SecurityMiddleware",
    "company_portal.middleware.RequestIdMiddleware",
    "django.contrib.sessions.middleware.SessionMiddleware",
    "corsheaders.middleware.CorsMiddleware",
    "django.middleware.common.CommonMiddleware",
    "django.middleware.csrf.CsrfViewMiddleware",
    "django.contrib.auth.middleware.AuthenticationMiddleware",
    "django.contrib.messages.middleware.MessageMiddleware",
    "django.middleware.clickjacking.XFrameOptionsMiddleware",
]

CORS_EXPOSE_HEADERS = ["X-Request-ID"]
```

请求 ID 中间件放在靠前位置，使认证、权限和 View 返回的响应都带有标识。`CORS_EXPOSE_HEADERS` 允许第24章的跨源前端 JavaScript 读取 `X-Request-ID`；它不会放宽允许访问 API 的 origin。

`response["X-Request-ID"] = value` 使用 Django Response 的响应头映射接口设置头字段；键和值都应是可转换为 HTTP 头文本的内容，赋值会覆盖已有的同名响应头，没有需要使用的返回结果。本例始终使用后端生成的请求 ID，不回显客户端随意传入的标识。

在 `company_portal/settings.py` 中追加或合并对应的 `LOGGING` 配置：

```python
LOGGING = {
    "version": 1,
    "disable_existing_loggers": False,
    "filters": {
        "request_id": {
            "()": "company_portal.middleware.RequestIdFilter",
        },
    },
    "formatters": {
        "api": {
            "format": "{asctime} {levelname} {name} request_id={request_id} {message}",
            "style": "{",
        },
    },
    "handlers": {
        "console": {
            "class": "logging.StreamHandler",
            "filters": ["request_id"],
            "formatter": "api",
        },
    },
    "loggers": {
        "employees": {"handlers": ["console"], "level": "INFO"},
    },
}
```

`ContextVar` 让同一进程中的并发请求各自保留请求 ID；Filter 为请求外日志提供 `-`，避免 formatter 因缺少字段而报错。反向代理若传入请求 ID，只有在受控代理边界内才能考虑复用；当前实现由 Django 重新生成。

`LOGGING` 使用 Python `logging.config.dictConfig` 能识别的字典结构：`version` 是必填配置格式版本，当前值为 `1`；`disable_existing_loggers=False` 保留 Django 和第三方已有 logger；`filters`、`formatters`、`handlers`、`loggers` 分别定义过滤器、输出格式、输出位置和命名日志入口。过滤器配置中的特殊键 `"()"` 接受可导入类路径并创建 `RequestIdFilter`，handler 的 `class` 同样是可导入处理器路径；`level="INFO"` 表示处理 INFO 及更高等级日志。错误类路径或缺少 formatter 字段会在启动或首次记录日志时暴露配置错误。

`ContextVar(name, default=...)` 创建上下文局部变量；`set(value)` 返回用于恢复旧值的 token，`reset(token)` 必须在 `finally` 中执行。`logging.Filter.filter(record)` 返回真值表示保留日志，本例同时补上格式化需要的 `request_id`。中间件的 `__init__(get_response)` 保存下一处理器，`__call__(request)` 返回响应；任何异常路径都通过 `finally` 清理上下文。

`uuid` 是 Python 标准库模块；`uuid.uuid4()` 生成随机 UUID 对象，`.hex` 返回不带连字符的32位十六进制字符串。本例把它作为关联请求与日志的标识，不把它当作认证凭据或业务主键。

禁止记录 Authorization、refresh/access token、密码、Cookie、完整请求体、附件内容和不必要个人信息。员工 ID 通常比姓名/邮箱更适合调查。

## 4. 统一 API 异常响应

DRF默认会处理校验、未认证、无权限和不存在。创建 `company_portal/exceptions.py`，对预期错误追加请求ID，对未预期异常记录堆栈并返回通用JSON：

```python
import logging

from rest_framework import status
from rest_framework.response import Response
from rest_framework.views import exception_handler


logger = logging.getLogger("employees.api")


def api_exception_handler(exc, context):
    response = exception_handler(exc, context)
    request = context.get("request")
    request_id = getattr(request, "request_id", None)

    if response is None:
        logger.error(
            "unhandled_api_exception",
            exc_info=(type(exc), exc, exc.__traceback__),
        )
        return Response(
            {
                "detail": "服务器内部错误。",
                "request_id": request_id,
            },
            status=status.HTTP_500_INTERNAL_SERVER_ERROR,
        )

    if isinstance(response.data, dict):
        response.data.setdefault("request_id", request_id)
    else:
        response.data = {
            "errors": response.data,
            "request_id": request_id,
        }
    return response
```

`dict.setdefault(key, default=None)` 在 `key` 不存在时写入 `default`，然后返回该键最终对应的值；键已经存在时不会覆盖原值。本例不使用返回值，只保证字典响应至少带有当前 `request_id`，同时保留业务代码已经设置的同名值。

把以下配置片段追加到 `company_portal/settings.py`，并与既有 `REST_FRAMEWORK` 字典合并：

```python
REST_FRAMEWORK["EXCEPTION_HANDLER"] = "company_portal.exceptions.api_exception_handler"
```

不要重新定义 `REST_FRAMEWORK` 后覆盖认证、权限和分页配置。

这个处理器只作用于经过 DRF View 处理的异常；非 DRF 请求仍由 Django 自身的异常处理流程负责。不要把所有错误改成200，也不要把异常字符串、SQL或堆栈放入响应。

DRF 的 `exception_handler(exc, context)` 接收异常和包含 View、request 等信息的上下文，返回已处理的 `Response` 或 `None`。`logging.getLogger(name)` 返回命名 logger；`logger.error(..., exc_info=...)` 在服务端记录异常堆栈。自定义 `api_exception_handler` 必须保持 DRF 已确定的400/401/403/404状态，只在未处理异常时生成通用500。

## 5. 外部 API 与超时

员工系统未来若调用其他服务，请求必须设置连接/读取超时、有限重试和日志关联。只对幂等且适合重试的失败采用有限重试；POST 写操作盲目重试可能产生重复数据。外部服务慢与数据库慢要分开测量。

## 日本企业项目中的实际使用

现场通常使用环境变量、请求 ID 和结构化日志把同一代码部署到多个环境。日志必须支持调查，同时遵守最小记录原则，避免令牌、密码和完整个人资料进入共享平台。

## 新人常见错误

- 把 `SECRET_KEY`、数据库密码或 token 写入源码和日志。
- 在生产开启 `DEBUG` 获取堆栈。
- 捕获所有异常后统一返回200，破坏 HTTP 契约。
- 日志没有请求 ID、环境或状态，无法关联前后端证据。
- 只修改 `.env`，却没有确认 Django 实际加载方式和最终配置。

## 6. 企业项目调查路径

```text
前端时间与Network
→ 响应X-Request-ID
→ 反向代理访问日志
→ Django请求/异常日志
→ ViewSet/Serializer/Permission
→ SQL与外部依赖
```

先确认影响环境、发生时间、请求条件和影响范围，再查日志。不要在生产直接加入大量打印或开启 DEBUG。若需要临时提高日志级别，明确时间窗口、敏感信息风险和恢复操作。

## 验证

先在项目根目录执行 `python manage.py check`，再用 `python manage.py runserver` 启动开发服务器。另开 PowerShell 请求健康接口和受保护接口：

```powershell
curl.exe -i http://127.0.0.1:8000/api/health/
curl.exe -i http://127.0.0.1:8000/api/employees/
```

两个响应都应包含非空 `X-Request-ID`；员工接口未携带 JWT 时预期为401。使用该 ID 检索终端日志，确认同一请求的响应与日志能够对应。

- 正常200、校验400、未认证401、无权限403、不存在404都有请求 ID。
- 临时制造500时客户端无堆栈，服务端日志有堆栈和同一请求 ID。
- 启动日志和管理命令日志没有请求上下文时使用 `request_id=-`，不会触发格式化错误。
- 配置缺失能在启动/检查阶段发现。
- 日志检索不到 token、密码和完整个人资料。

## 现场任务

故障：前端只报告“500”。根据请求 ID 找到 Serializer 自定义字段引发的异常，写出发生条件、影响范围、根因、修复与回归证据。再检查日志字段是否过度记录员工信息。

## 完成检查

- [ ] 可变配置、秘密和源码边界清楚。
- [ ] 请求 ID 能贯穿响应和服务端日志。
- [ ] 预期错误与未预期异常的响应不同。
- [ ] 调试不会依赖生产 DEBUG 或敏感日志。

下一章把API、数据库迁移、Static/Media、反向代理和前端文件整理成经过发布前检查的交付状态，并编写发布与回滚手顺。
