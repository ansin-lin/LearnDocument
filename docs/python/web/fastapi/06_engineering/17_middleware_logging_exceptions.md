# 第17章 Middleware、日志与统一异常

> 本章成果：让每个请求统一经过 Middleware，记录请求编号、方法、路径、状态码和耗时，并为业务异常及未预期异常提供稳定响应。

## 一、本章开始状态

以下代码不修改数据库模型、业务字段或接口路径，只增加应用级请求处理、日志和异常响应。

三类对象的职责不同：

| 对象 | 处理范围 |
| --- | --- |
| Router | 某个方法和路径的 HTTP 输入输出 |
| Middleware | 每一次 HTTP 请求和响应 |
| Exception Handler | 某一类异常的统一转换 |

Middleware 适合请求编号、耗时、通用响应头和访问日志，不适合编写员工新增、权限判断或数据库事务。

## 二、日志基础

创建 `app/logging_config.py`：

```python
import logging  # 导入标准日志模块


def configure_logging() -> None:  # 定义configure_logging函数
    logging.basicConfig(  # 调用logging.basicConfig()
        level=logging.INFO,  # 设置或保存level的值
        format=(  # 设置或保存format的值
            "%(asctime)s %(levelname)s "  # 组成当前文本内容
            "%(name)s %(message)s"  # 组成当前文本内容
        ),  # 完成当前调用或数据结构
    )  # 完成当前调用或数据结构
```

`logging.basicConfig()` 本示例参数：

| 参数 | 可接受的值 | 默认值或必填性 | 作用 |
| --- | --- | --- | --- |
| `level` | 日志级别整数或名称字符串 | 默认 `WARNING` | 设置根 Logger 记录的最低日志级别 |
| `format` | 日志格式字符串 | 默认使用日志模块内置格式 | 决定每条日志显示的时间、级别、模块名和消息等内容 |

日志级别：

| 级别 | 使用场景 |
| --- | --- |
| `DEBUG` | 本地详细调试 |
| `INFO` | 正常启动、停止和请求完成 |
| `WARNING` | 可恢复但需要关注的业务情况 |
| `ERROR` | 请求失败或外部依赖失败 |
| `CRITICAL` | 服务无法继续运行的严重错误 |

业务日志不要记录密码、JWT、完整数据库 URL、请求体中的个人敏感数据或异常响应堆栈。

## 三、创建请求 Middleware

在 `app/main.py` 增加导入：

```python
import logging  # 导入请求日志工具
from time import perf_counter  # 从time模块导入perf_counter
from uuid import uuid4  # 从uuid模块导入uuid4

from fastapi import FastAPI, Request  # 从fastapi模块导入FastAPI, Request
```

在唯一的 `app` 对象创建后注册：

```python
logger = logging.getLogger(__name__)  # 创建当前模块日志记录器


@app.middleware("http")  # 为下面的函数注册框架行为
async def add_request_context(  # 定义add_request_context函数
    request: Request,  # 接收request参数并声明类型
    call_next,  # 传入call_next参数
):  # 结束参数列表并开始处理请求
    request_id = uuid4().hex  # 设置或保存request_id的值
    request.state.request_id = request_id  # 设置或保存request.state.request_id的值
    started_at = perf_counter()  # 设置或保存started_at的值

    response = await call_next(request)  # 设置或保存response的值

    elapsed_ms = (perf_counter() - started_at) * 1000  # 设置或保存elapsed_ms的值
    response.headers["X-Request-ID"] = request_id  # 把请求编号写入响应头
    logger.info(  # 调用logger.info()
        "request completed request_id=%s method=%s "  # 组成当前文本内容
        "path=%s status=%s elapsed_ms=%.2f",  # 组成当前文本内容
        request_id,  # 传入request_id参数
        request.method,  # 传入request.method的值
        request.url.path,  # 传入request.url.path的值
        response.status_code,  # 传入response.status_code的值
        elapsed_ms,  # 传入elapsed_ms参数
    )  # 完成当前调用或数据结构
    return response  # 返回当前处理结果
```

`@app.middleware()` 参数：

| 参数 | 可接受的值 | 默认值或必填性 | 作用 |
| --- | --- | --- | --- |
| `middleware_type` | `"http"` | 必填 | 注册处理 HTTP 请求和响应的函数式 Middleware |

Middleware 函数接收当前 `request` 和可继续调用后续处理流程的 `call_next`。`request.state` 可保存本次请求范围内的自定义数据；这里保存的 `request_id` 可被异常处理器继续读取。

执行顺序：

```text
请求进入
→ Middleware 生成 request_id 并开始计时
→ call_next(request)
→ Router、Depends、Service、Repository
→ 得到响应
→ Middleware 写响应头和完成日志
→ 响应返回调用方
```

`call_next()`必须被调用并返回响应，否则请求不会进入Router。浏览器前端如果需要读取`X-Request-ID`，第24章配置CORS时还要把它加入`expose_headers`。

## 四、注册日志配置

在 `app/main.py` 创建应用前调用：

```python
from app.logging_config import configure_logging  # 导入应用级日志配置函数


configure_logging()  # 调用configure_logging()
app = FastAPI(  # 设置或保存app的值
    title=settings.app_name,  # 设置或保存title的值
    lifespan=lifespan,  # 设置或保存lifespan的值
)  # 完成当前调用或数据结构
```

应用只配置一次基础日志。各模块继续使用：

```python
logger = logging.getLogger(__name__)  # 各模块按模块名创建记录器
```

`logging.getLogger()` 参数：

| 参数 | 可接受的值 | 默认值或必填性 | 作用 |
| --- | --- | --- | --- |
| `name` | Logger 名称字符串或 `None` | 默认 `None` | 获取指定名称的 Logger；`__name__` 可以在日志中保留模块来源 |

不要在每个 Router 或 Service 中重复调用 `basicConfig()`。

## 五、业务异常与 HTTP 响应

Service 使用与 HTTP 无关的业务异常，例如：

```python
class EmployeeNotFoundError(Exception):  # 定义不依赖HTTP的员工不存在异常
    pass  # 当前类不需要额外实现
```

第13～14章已经在应用入口把业务异常转换为基础HTTP响应。本章扩展这些处理器，使所有错误响应都具有相同结构和`request_id`，并增加服务端日志。Router主动拒绝某个HTTP请求时仍可使用`HTTPException`，例如第19章的登录失败和第20章的权限不足。

## 六、创建统一异常处理器

在 `app/main.py` 增加：

```python
from fastapi.responses import JSONResponse  # 导入JSON错误响应类
from sqlalchemy.exc import IntegrityError  # 从sqlalchemy.exc模块导入IntegrityError

from app.services.employee_service import (  # 从Service模块导入三种业务异常
    DepartmentNotFoundError,  # 传入DepartmentNotFoundError参数
    EmployeeAlreadyExistsError,  # 传入EmployeeAlreadyExistsError参数
    EmployeeNotFoundError,  # 传入EmployeeNotFoundError参数
)  # 完成当前调用或数据结构


def get_request_id(request: Request) -> str:  # 定义get_request_id函数
    return getattr(request.state, "request_id", "unavailable")  # 返回当前处理结果


@app.exception_handler(EmployeeNotFoundError)  # 为下面的函数注册框架行为
async def handle_employee_not_found(  # 定义handle_employee_not_found函数
    request: Request,  # 接收request参数并声明类型
    exc: EmployeeNotFoundError,  # 接收exc参数并声明类型
):  # 结束参数列表并开始处理员工不存在异常
    return JSONResponse(  # 返回当前处理结果
        status_code=404,  # 设置或保存status_code的值
        content={  # 设置或保存content的值
            "detail": "员工不存在",  # 组成当前文本内容
            "request_id": get_request_id(request),  # 组成当前文本内容
        },  # 完成当前调用或数据结构
    )  # 完成当前调用或数据结构


@app.exception_handler(EmployeeAlreadyExistsError)  # 为下面的函数注册框架行为
async def handle_employee_already_exists(  # 定义handle_employee_already_exists函数
    request: Request,  # 接收request参数并声明类型
    exc: EmployeeAlreadyExistsError,  # 接收exc参数并声明类型
):  # 结束参数列表并开始处理员工编号冲突异常
    return JSONResponse(  # 返回当前处理结果
        status_code=409,  # 设置或保存status_code的值
        content={  # 设置或保存content的值
            "detail": "员工编号已经存在",  # 组成当前文本内容
            "request_id": get_request_id(request),  # 组成当前文本内容
        },  # 完成当前调用或数据结构
    )  # 完成当前调用或数据结构


@app.exception_handler(DepartmentNotFoundError)  # 为下面的函数注册框架行为
async def handle_department_not_found(  # 定义handle_department_not_found函数
    request: Request,  # 接收request参数并声明类型
    exc: DepartmentNotFoundError,  # 接收exc参数并声明类型
):  # 结束参数列表并开始处理部门不存在异常
    return JSONResponse(  # 返回当前处理结果
        status_code=400,  # 设置或保存status_code的值
        content={  # 设置或保存content的值
            "detail": "部门不存在",  # 组成当前文本内容
            "request_id": get_request_id(request),  # 组成当前文本内容
        },  # 完成当前调用或数据结构
    )  # 完成当前调用或数据结构


@app.exception_handler(IntegrityError)  # 为下面的函数注册框架行为
async def handle_integrity_error(  # 定义handle_integrity_error函数
    request: Request,  # 接收request参数并声明类型
    exc: IntegrityError,  # 接收exc参数并声明类型
):  # 结束参数列表并开始处理数据库约束异常
    request_id = get_request_id(request)  # 设置或保存request_id的值
    logger.warning(  # 调用logger.warning()
        "database constraint conflict request_id=%s path=%s",  # 组成当前文本内容
        request_id,  # 传入request_id参数
        request.url.path,  # 传入request.url.path的值
    )  # 完成当前调用或数据结构
    return JSONResponse(  # 返回当前处理结果
        status_code=409,  # 设置或保存status_code的值
        content={  # 设置或保存content的值
            "detail": "数据与现有记录冲突",  # 组成当前文本内容
            "request_id": request_id,  # 组成当前文本内容
        },  # 完成当前调用或数据结构
    )  # 完成当前调用或数据结构


@app.exception_handler(Exception)  # 为下面的函数注册框架行为
async def handle_unexpected_exception(  # 定义handle_unexpected_exception函数
    request: Request,  # 接收request参数并声明类型
    exc: Exception,  # 接收exc参数并声明类型
):  # 结束参数列表并开始处理未预期异常
    request_id = get_request_id(request)  # 设置或保存request_id的值
    logger.exception(  # 调用logger.exception()
        "unexpected error request_id=%s path=%s",  # 组成当前文本内容
        request_id,  # 传入request_id参数
        request.url.path,  # 传入request.url.path的值
    )  # 完成当前调用或数据结构
    return JSONResponse(  # 返回当前处理结果
        status_code=500,  # 设置或保存status_code的值
        content={  # 设置或保存content的值
            "detail": "系统错误",  # 组成当前文本内容
            "request_id": request_id,  # 组成当前文本内容
        },  # 完成当前调用或数据结构
    )  # 完成当前调用或数据结构
```

`@app.exception_handler()` 参数：

| 参数 | 可接受的值 | 默认值或必填性 | 作用 |
| --- | --- | --- | --- |
| `exc_class_or_status_code` | 异常类或 HTTP 状态码整数 | 必填 | 指定由当前函数统一处理的异常类型或状态码 |

`JSONResponse()` 本示例参数：

| 参数 | 可接受的值 | 默认值或必填性 | 作用 |
| --- | --- | --- | --- |
| `content` | 可转换为 JSON 的数据 | 必填 | 设置 JSON 响应正文 |
| `status_code` | `100`～`599` 的 HTTP 状态码整数 | 默认 `200` | 设置响应状态码 |

`IntegrityError` 处理器覆盖两个并发请求同时通过员工编号预检查、最终由数据库唯一约束拒绝其中一个的情况。Service 已在重新抛出异常前执行 `rollback()`，异常处理器只负责转换响应，不能在这里继续使用失败事务中的 Session。

未预期异常的详细堆栈只进入服务端日志。响应只返回稳定信息和可供调查的 `request_id`。

## 七、Middleware 与异常处理的边界

```text
正常请求
Middleware → Router → Response → Middleware日志

业务异常
Middleware → Router/Service抛出异常
→ Exception Handler转换响应
→ Middleware记录最终状态码

未预期异常
Middleware → 应用代码抛出异常
→ 全局Handler记录堆栈并返回500
```

未预期异常可能在 `call_next()` 中向外传播，因此普通完成日志不一定产生；全局异常处理器必须使用同一个 `request_id` 记录堆栈。不要捕获所有异常后返回 `200`，也不要把 `str(exc)`、SQL、文件路径或堆栈直接放进响应。

## 八、运行与验证

启动应用并完成：

1. 请求 `/health`，确认响应头包含 `X-Request-ID`。
2. 请求员工列表，确认日志包含方法、路径、状态码和耗时。
3. 查询不存在员工，确认返回 `404` 和 `request_id`。
4. 重复新增同一员工，确认返回 `409` 和 `request_id`。
5. 使用不存在的部门新增员工，确认返回 `400` 和 `request_id`。
6. 临时制造一次未预期异常，确认返回 `500`，响应不含堆栈。
7. 使用响应中的 `request_id` 在服务端日志中定位同一次请求。
8. 恢复临时代码并重新验证正常请求。

## 九、常见错误

| 现象 | 原因 | 修正 |
| --- | --- | --- |
| 请求一直没有到达 Router | Middleware 没有调用 `call_next()` | 调用并返回生成的响应 |
| 浏览器读不到 `X-Request-ID` | CORS 未公开自定义响应头 | 第24章配置`expose_headers` |
| 一次错误记录两份堆栈 | Middleware 和全局 Handler 都调用 `logger.exception()` | 只在统一异常处理器记录 |
| 响应泄露数据库或路径信息 | 直接返回 `str(exc)` | 返回稳定公开消息 |
| 日志缺少请求关联 | 没有统一 request ID | 在 Middleware 生成并贯穿日志 |

## 十、完成检查

- [ ] 每个请求都有 `X-Request-ID`。
- [ ] 正常请求日志包含方法、路径、状态码和耗时。
- [ ] 业务异常和未预期异常职责分开。
- [ ] `500` 响应不泄露堆栈或内部配置。
- [ ] 能用 `request_id` 关联响应和日志。
- [ ] Middleware 不包含员工业务逻辑或事务。

完成后确认正常请求和异常请求都能通过`request_id`关联客户端响应与服务端日志。
