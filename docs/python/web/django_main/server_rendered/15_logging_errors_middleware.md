# 第15章 日志、异常页与中间件

## 本章成果

系统能记录可调查的请求线索，生产模式不向用户显示调试信息，并通过一个简单中间件为每次请求添加请求 ID。

## 本章开始状态与修改清单

业务功能已完成。本章不改变员工业务规则，只增加 `settings.py` 日志配置、`company_portal/middleware.py`、错误模板和项目级错误处理入口。

## 先区分三个用途

| 机制 | 用途 |
|---|---|
| 异常页 | 向用户说明请求失败 |
| 日志 | 向开发和运维提供调查线索 |
| 中间件 | 对大量请求执行共通处理 |

- **Middleware 是什么**：包围 Django 请求处理流程、对多条请求统一执行前后处理的组件。
- **Logging 是什么**：把运行事件按级别和格式发送到控制台、文件或日志平台的机制。
- **Request ID 是什么**：为一次请求分配的关联标识，用来连接响应、日志和障害记录。
- **什么时候使用**：认证前后共通处理、请求追踪和异常调查使用；具体员工业务规则仍留在 View 或业务层。

```text
Request
→ Middleware 前处理：生成 Request ID
→ URL / View
→ logger 记录同一 Request ID
→ Middleware 后处理：写入响应头
→ Response
```

## 基础日志配置

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
        "standard": {
            "format": (
                "{asctime} {levelname} request_id={request_id} "
                "employee_id={employee_id} user_id={user_id} {name} {message}"
            ),
            "style": "{",
        },
    },
    "handlers": {
        "console": {
            "class": "logging.StreamHandler",
            "filters": ["request_id"],
            "formatter": "standard",
        },
    },
    "loggers": {
        "employees": {"handlers": ["console"], "level": "INFO"},
    },
}
```

`LOGGING` 是交给Python日志配置系统的字典：`version` 当前必须为1；`disable_existing_loggers=False` 保留已有Logger；`filters` 为记录补充上下文；`formatters` 定义输出格式；`handlers` 决定输出位置；`loggers` 按名称配置处理器和最低级别。配置加载后改变全项目日志行为，不产生供业务代码使用的返回值。

在业务代码中记录有意义的事件：

```python
import logging

logger = logging.getLogger(__name__)
logger.info("employee_updated", extra={"employee_id": employee.pk, "user_id": request.user.pk})
```

`logging.getLogger(__name__)` 按模块名取得 Logger。`info()` 的消息使用稳定事件名，`extra` 把调查所需且不敏感的业务标识加入LogRecord；上面的Formatter会输出 `employee_id` 和 `user_id`，Filter则为没有这些字段的日志补入 `-`，避免格式化失败。Handler决定输出位置，Formatter决定最终文本形式。

`getLogger(name=None)` 接受可选Logger名称并返回同名Logger；重复调用会取得同一日志层级中的对象。`logger.info(msg, *args, extra=None, ...)` 的消息必填，`extra` 是附加字段字典，成功记录后返回 `None`。日志级别低于当前配置时，记录会被过滤。

不要记录密码、Session cookie、附件内容和完整个人信息。异常日志使用 `logger.exception()` 保留堆栈，但用户页面不显示堆栈。

## 自定义 403、404 与 500

异常页用于在请求失败时向用户提供简洁、安全的说明。403 表示用户已被识别但没有操作权限，404 表示请求的资源不存在，500 表示服务器内部发生了未处理异常。生产环境不能把异常堆栈直接显示给用户，因此需要由统一页面接替 Django 的调试页面。

```text
请求失败
    │
    ├─ 没有权限 ──> 403 处理函数 ──> 403.html
    ├─ 资源不存在 -> 404 处理函数 ──> 404.html
    └─ 未处理异常 -> 500 处理函数 ──> 500.html
```

先在 `employees/views.py` 增加三个处理函数：

```python
from django.http import HttpRequest, HttpResponse
from django.shortcuts import render


def permission_denied(request: HttpRequest, exception) -> HttpResponse:
    return render(request, "403.html", status=403)


def page_not_found(request: HttpRequest, exception) -> HttpResponse:
    return render(request, "404.html", status=404)


def server_error(request: HttpRequest) -> HttpResponse:
    return render(request, "500.html", status=500)
```

`permission_denied()` 和 `page_not_found()` 会接收触发错误的 `exception`；`server_error()` 只接收 `request`。`render()` 负责读取模板并返回 `HttpResponse`，`status` 必须明确设置为对应状态码，不能只显示错误文字却仍返回200。

然后在项目总路由 `company_portal/urls.py` 的现有 `urlpatterns` 后声明处理函数：

```python
handler403 = "employees.views.permission_denied"
handler404 = "employees.views.page_not_found"
handler500 = "employees.views.server_error"
```

这些声明必须放在项目总路由中，不要替换第12章已经建立的登录、退出和 App 路由。Django 根据状态码选择对应处理函数，处理函数再选择页面模板。

最后在项目模板目录创建三个完整模板。异常页采用独立、简单的 HTML，避免公共模板本身发生错误时连异常页也无法显示。

`templates/403.html`：

```html
<!doctype html>
<html lang="zh-CN">
<head>
  <meta charset="utf-8">
  <title>权限不足</title>
</head>
<body>
  <h1>403 权限不足</h1>
  <p>当前账号没有执行此操作的权限，请联系管理员确认权限。</p>
  <p><a href="/">返回首页</a></p>
</body>
</html>
```

`templates/404.html`：

```html
<!doctype html>
<html lang="zh-CN">
<head>
  <meta charset="utf-8">
  <title>页面不存在</title>
</head>
<body>
  <h1>404 页面不存在</h1>
  <p>请确认地址是否正确，或返回首页重新操作。</p>
  <p><a href="/">返回首页</a></p>
</body>
</html>
```

`templates/500.html`：

```html
<!doctype html>
<html lang="zh-CN">
<head>
  <meta charset="utf-8">
  <title>系统错误</title>
</head>
<body>
  <h1>500 系统错误</h1>
  <p>系统暂时无法处理请求，请稍后重试。问题持续发生时请联系管理员。</p>
  <p><a href="/">返回首页</a></p>
</body>
</html>
```

使用 `DEBUG=False` 和正确的 `ALLOWED_HOSTS` 才能验证生产异常页；不要在生产环境开启 `DEBUG`。500 页面不要显示异常消息、SQL、文件路径或堆栈，详细原因只写入日志供开发人员调查。

## 请求 ID 中间件

创建 `company_portal/middleware.py`：

```python
import logging
import uuid
from contextvars import ContextVar


request_id_context = ContextVar("request_id", default="-")


class RequestIdFilter(logging.Filter):
    def filter(self, record):
        record.request_id = request_id_context.get()
        record.employee_id = getattr(record, "employee_id", "-")
        record.user_id = getattr(record, "user_id", "-")
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

`ContextVar(name, default=...)` 创建当前执行上下文独立的变量；名称必填，默认值可选，返回ContextVar对象。`set(value)` 保存当前值并返回用于恢复的Token；`get()` 返回当前值；`reset(token)` 恢复设置前状态。这里使用它，是为了并发请求之间不会错误共用Request ID。

Middleware类的 `__init__(get_response)` 在服务器建立处理链时接收下一个处理器；`__call__(request)` 在每次请求时执行并必须返回响应。`RequestIdFilter.filter(record)` 接收一条LogRecord，补充字段后返回 `True` 表示允许输出该记录。

`uuid.uuid4()` 不接参数，使用系统提供的随机源生成UUID对象；`.hex` 是该对象的只读属性，返回不带连字符的32位十六进制字符串。这里每个请求生成一个新标识，用于关联响应头和日志，不把它当作用户身份、权限凭据或业务流水号。

在 `MIDDLEWARE` 中注册。中间件把请求 ID 放入当前请求的上下文，日志 Filter 再把它写入每条 LogRecord；`getattr()` 为没有业务标识的第三方或系统日志补入 `-`，确保Formatter要求的字段始终存在；`finally` 确保请求结束后清理，避免后续请求误用旧值。这样响应头中的 `X-Request-ID` 和日志中的 `request_id=...` 才能相互对应。

中间件有顺序，新增前必须说明它依赖哪些既有中间件。业务权限通常更适合装饰器、Mixin 或服务层，不要把全部业务逻辑塞进中间件。

## 调查练习

人为制造一个仅在开发环境出现的异常，记录发生时间、URL、账号、请求 ID、复现步骤和堆栈关键位置。修复后再次执行相关正常路径和异常路径。

### 验证顺序

1. 正常响应头包含 `X-Request-ID`，并能在同一次请求的日志中找到相同值。
2. 不存在地址返回404。
3. 无权限账号返回403。
4. 在本地临时使用 `DEBUG=False` 验证500模板，验证后恢复开发配置。
5. 检查日志中没有密码、Cookie、表单完整内容和附件内容。

现场报告：`ログのリクエストIDと発生時刻から対象リクエストを特定しました。`

参考方向见[章节练习参考答案](practice_answers.md)。

## 调查功能运行检查

- [ ] 404、403 与 500 的含义没有混淆
- [ ] 日志足以关联请求，但不泄露敏感信息
- [ ] `DEBUG=False` 时用户看不到堆栈和配置
- [ ] 能说明中间件适合与不适合处理的内容

## Logger、Handler、Formatter 与级别

Logger 由代码按名称取得记录器；Handler 决定日志送往控制台、文件或平台；Formatter 决定时间、级别、请求 ID 等输出格式。常用级别从低到高为 DEBUG、INFO、WARNING、ERROR、CRITICAL。生产 INFO 不应记录每个字段的隐私数据，ERROR 也不等于所有业务校验失败。

`try/except` 只捕获能够正确处理或转换的异常。不要用宽泛 `except Exception` 吞掉错误后返回成功。预期不存在使用404，权限不足使用403，输入错误使用表单错误；真正未预期异常记录堆栈并返回500。

## 中间件生命周期识读

新式中间件的 `__call__` 包围 `get_response(request)`：前半段在 View 前执行，后半段在响应返回时执行。既有代码还可能实现 `process_view()`、`process_exception()` 等钩子，当前要求能读懂其执行位置，不要求把业务逻辑迁入钩子。

JWT 不属于本章日志/中间件主线。它是 API 认证方式，第22章结合失效、刷新、401和权限统一学习。

## 障害调查最小记录

记录环境、发生时间、请求 ID、URL/方法、用户或角色（不记录秘密）、状态码、复现条件、堆栈关键位置、影响范围、临时处置和最终原因。修复后用同一请求与相关回归路径复测，并确认日志没有新增敏感信息。

## 本章总结

错误页面向用户，日志面向调查，中间件处理跨请求共通逻辑。状态码、日志级别和异常类型必须保持一致，请求 ID 应能关联响应与日志。下一章用自动测试保护权限、数据写入和异常路径。

## 日本项目中的实际使用

障害调查通常从环境、发生时间、请求 ID、URL、用户角色和状态码开始，再定位堆栈与数据条件。日志格式由团队统一，业务代码记录稳定事件和必要标识，禁止输出密码、Cookie、Token、完整表单和个人资料。

## 新人常见错误

- 使用 `print()` 代替统一日志，缺少级别、时间和请求关联信息。
- 捕获 `Exception` 后什么也不做，使真正错误被隐藏。
- 错误页面显示失败文字却仍返回200，监控和调用方无法判断失败。
- 生成 Request ID 但没有写入日志，无法完成关联调查。
- 把权限等业务逻辑全部放进 Middleware，导致职责和执行顺序难以维护。

## 本章知识将在后续章节继续使用

```text
Middleware
→ Request ID 上下文
→ Logger / Handler / Formatter
→ 403 / 404 / 500
→ 第16章异常与响应测试
→ 第17～18章发布确认和障害交接
```
