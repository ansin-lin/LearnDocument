# 第27章 API 配置、日志与异常响应

## 本章成果

把开发、测试和生产的可变配置从代码中分离；API 日志能通过请求 ID 关联前后端问题；未预期异常返回稳定的 JSON 500，而不会泄露堆栈、SQL、token 或个人信息。

## 1. 哪些配置会随环境变化

至少包括：`SECRET_KEY`、`DEBUG`、`ALLOWED_HOSTS`、数据库连接、允许的前端 origin、日志级别、文件存储、JWT 时长/签名策略和外部服务地址。配置名称与默认值写入说明，秘密值由部署环境提供。

```python
import os

DEBUG = os.getenv("DJANGO_DEBUG", "False").lower() == "true"
SECRET_KEY = os.environ["DJANGO_SECRET_KEY"]
ALLOWED_HOSTS = [
    host.strip()
    for host in os.getenv("DJANGO_ALLOWED_HOSTS", "").split(",")
    if host.strip()
]
```

关键秘密缺失时应启动失败，不使用可预测生产默认值。课程可用 `.env.example` 列变量名，但 Django 不会自动读取 `.env`；项目若使用环境加载库，应明确直接依赖、加载位置和覆盖顺序。

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

结构化日志至少帮助回答：什么时候、哪个环境、哪次请求、哪个端点、什么方法、什么状态、耗时多久、由哪个用户/角色触发。示例 formatter：

```python
LOGGING = {
    "version": 1,
    "disable_existing_loggers": False,
    "formatters": {
        "api": {
            "format": "{asctime} {levelname} {name} request_id={request_id} {message}",
            "style": "{",
        },
    },
    "handlers": {
        "console": {
            "class": "logging.StreamHandler",
            "formatter": "api",
        },
    },
    "loggers": {
        "employees": {"handlers": ["console"], "level": "INFO"},
    },
}
```

第15章中间件应把请求 ID 同时放进 request、响应头和日志上下文。若 formatter 强制需要 `request_id`，启动日志等没有该字段时会格式化失败；真实实现应使用 Filter 提供默认值，或只在请求日志 formatter 中要求。

禁止记录 Authorization、refresh/access token、密码、Cookie、完整请求体、附件内容和不必要个人信息。员工 ID 通常比姓名/邮箱更适合调查。

## 4. 统一 API 异常响应

DRF 默认会处理校验、未认证、无权限和不存在。项目可定制异常处理器统一加入请求 ID，但必须保留正确状态码和字段错误：

```python
from rest_framework.views import exception_handler


def api_exception_handler(exc, context):
    response = exception_handler(exc, context)
    request = context.get("request")
    request_id = getattr(request, "request_id", None)
    if response is not None and isinstance(response.data, dict):
        response.data["request_id"] = request_id
    return response
```

```python
REST_FRAMEWORK["EXCEPTION_HANDLER"] = "company_portal.exceptions.api_exception_handler"
```

未预期异常返回 `None` 后由 Django 进入500处理。中间件或平台记录堆栈，客户端只获得通用错误和请求 ID。不要用异常处理器把所有错误改成200，也不要把异常字符串直接返回。

## 5. 外部 API 与超时

员工系统未来若调用其他服务，请求必须设置连接/读取超时、有限重试和日志关联。只对幂等且适合重试的失败采用有限重试；POST 写操作盲目重试可能产生重复数据。外部服务慢与数据库慢要分开测量。

## 6. 障害调查路径

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

- 正常200、校验400、未认证401、无权限403、不存在404都有请求 ID。
- 临时制造500时客户端无堆栈，服务端日志有堆栈和同一请求 ID。
- 配置缺失能在启动/检查阶段发现。
- 日志检索不到 token、密码和完整个人资料。

## 现场任务

故障：前端只报告“500”。根据请求 ID 找到 Serializer 自定义字段引发的异常，写出发生条件、影响范围、根因、修复与回归证据。再检查日志字段是否过度记录员工信息。

## 完成检查

- [ ] 可变配置、秘密和源码边界清楚。
- [ ] 请求 ID 能贯穿响应和服务端日志。
- [ ] 预期错误与未预期异常的响应不同。
- [ ] 调试不会依赖生产 DEBUG 或敏感日志。

下一章把 API、数据库迁移、Static/Media、反向代理和前端产物组合成可发布系统。
