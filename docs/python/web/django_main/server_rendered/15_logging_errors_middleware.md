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

## 基础日志配置

```python
LOGGING = {
    "version": 1,
    "disable_existing_loggers": False,
    "handlers": {"console": {"class": "logging.StreamHandler"}},
    "loggers": {
        "employees": {"handlers": ["console"], "level": "INFO"},
    },
}
```

在业务代码中记录有意义的事件：

```python
import logging

logger = logging.getLogger(__name__)
logger.info("employee_updated", extra={"employee_id": employee.pk, "user_id": request.user.pk})
```

不要记录密码、Session cookie、附件内容和完整个人信息。异常日志使用 `logger.exception()` 保留堆栈，但用户页面不显示堆栈。

## 自定义 404 与 500

在项目模板目录创建 `404.html`、`500.html`。使用 `DEBUG=False` 和正确 `ALLOWED_HOSTS` 才能验证生产异常页；不要在生产开启 `DEBUG`。

项目总路由可声明自定义处理函数：

```python
handler403 = "employees.views.permission_denied"
handler404 = "employees.views.page_not_found"
handler500 = "employees.views.server_error"
```

处理函数分别返回403、404、500状态码，不能只显示错误文字却仍返回200。

## 请求 ID 中间件

创建 `company_portal/middleware.py`：

```python
import uuid


class RequestIdMiddleware:
    def __init__(self, get_response):
        self.get_response = get_response

    def __call__(self, request):
        request.request_id = uuid.uuid4().hex
        response = self.get_response(request)
        response["X-Request-ID"] = request.request_id
        return response
```

在 `MIDDLEWARE` 中注册。中间件有顺序，新增前必须说明它依赖哪些既有中间件。业务权限通常更适合装饰器、Mixin 或服务层，不要把全部业务逻辑塞进中间件。

## 调查练习

人为制造一个仅在开发环境出现的异常，记录发生时间、URL、账号、请求 ID、复现步骤和堆栈关键位置。修复后再次执行相关正常路径和异常路径。

### 验证顺序

1. 正常响应头包含 `X-Request-ID`。
2. 不存在地址返回404。
3. 无权限账号返回403。
4. 在本地临时使用 `DEBUG=False` 验证500模板，验证后恢复开发配置。
5. 检查日志中没有密码、Cookie、表单完整内容和附件内容。

现场报告：`ログのリクエストIDと発生時刻から対象リクエストを特定しました。`

参考方向见[章节练习参考答案](practice_answers.md)。

## 完成检查

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
