# 第16章 配置管理与Lifespan

> 本章成果：把数据库地址和密钥迁移到经过校验的环境配置，并使用 FastAPI Lifespan 明确应用启动与关闭时的处理。

## 一、本章开始状态

第15章已经形成唯一应用入口和稳定项目结构。当前请求级数据库Session由`get_db()`创建和关闭，但数据库地址、应用名称和密钥还需要统一管理。

本章区分两种生命周期：

| 生命周期 | 范围 | 当前项目示例 |
| --- | --- | --- |
| 请求级 | 一次请求到一次响应 | `get_db()` 创建和关闭 Session |
| 应用级 | 进程启动到进程关闭 | Lifespan 启动日志、释放连接池 |

不要使用 Lifespan 创建一个供全部请求共享的 SQLAlchemy Session。Session 仍然是请求级资源。

## 二、安装配置依赖

在项目虚拟环境中执行：

```powershell
python -m pip install pydantic-settings
```

把 `pydantic-settings` 记录到项目直接依赖中。它负责从环境变量和 `.env` 读取并校验配置。

## 三、创建配置类

创建或整体替换 `app/config.py`：

```python
from functools import lru_cache  # 导入配置对象缓存装饰器

from pydantic import Field  # 从pydantic模块导入Field
from pydantic_settings import BaseSettings, SettingsConfigDict  # 从pydantic_settings模块导入BaseSettings, SettingsConfigDict


class Settings(BaseSettings):  # 定义Settings类
    model_config = SettingsConfigDict(  # 设置或保存model_config的值
        env_file=".env",  # 设置或保存env_file的值
        env_file_encoding="utf-8",  # 设置或保存env_file_encoding的值
        extra="ignore",  # 设置或保存extra的值
    )  # 完成当前调用或数据结构

    app_name: str = "Employee API"  # 接收app_name参数并声明类型
    database_url: str  # 接收database_url参数并声明类型
    secret_key: str = Field(min_length=32)  # 接收secret_key参数并声明类型


@lru_cache  # 为下面的函数注册框架行为
def get_settings() -> Settings:  # 定义get_settings函数
    return Settings()  # 返回当前处理结果


settings = get_settings()  # 设置或保存settings的值
```

`SettingsConfigDict()` 参数：

| 参数 | 可接受的值 | 默认值或必填性 | 作用 |
| --- | --- | --- | --- |
| `env_file` | 环境文件路径、路径列表或 `None` | 默认 `None` | 指定从哪个环境文件读取配置 |
| `env_file_encoding` | 编码名称字符串或 `None` | 默认 `None` | 指定环境文件的字符编码 |
| `extra` | `"ignore"`、`"allow"`、`"forbid"` | 默认 `"forbid"` | 决定如何处理配置类中未声明的额外配置 |

`@lru_cache` 会缓存 `get_settings()` 的返回值。第一次调用时创建 `Settings` 对象，后续调用复用同一个配置对象，避免重复读取环境配置。直接写成 `@lru_cache` 时，常用参数采用默认值：

| 参数 | 可接受的值 | 默认值或必填性 | 作用 |
| --- | --- | --- | --- |
| `maxsize` | 正整数、`0` 或 `None` | 默认 `128` | 设置最多缓存多少种调用结果；`None` 表示不限制数量 |
| `typed` | `True` 或 `False` | 默认 `False` | 是否把不同类型但值相等的参数分别缓存 |

字段作用：

| 字段 | 可接受的值 | 默认值或必填性 | 用途 |
| --- | --- | --- | --- |
| `app_name` | 字符串 | 默认 `"Employee API"` | OpenAPI 标题和启动日志 |
| `database_url` | SQLAlchemy 数据库 URL 字符串 | 必填 | SQLAlchemy Engine 与 Alembic |
| `secret_key` | 长度至少为 32 的随机字符串 | 必填 | 签名 JWT |

`Settings()` 会在读取时校验必填配置。缺少 `DATABASE_URL` 或 `SECRET_KEY` 时，应用应明确失败，而不是带着不完整配置继续运行。

## 四、准备环境文件

仓库提交 `.env.example`：

```text
APP_NAME=Employee API
DATABASE_URL=mysql+pymysql://app_user:change-me@127.0.0.1:3306/employee_management_fastapi?charset=utf8mb4
SECRET_KEY=replace-with-a-random-development-value
```

开发者在本地复制为 `.env` 并替换实际值：

```powershell
Copy-Item .env.example .env
```

安全边界：

- `.env` 必须加入 `.gitignore`。
- `.env.example` 只保留配置名和无效示例值。
- 生产密钥必须单独生成，不能使用公开示例值，也不能写入镜像或前端代码。
- 日志不能输出完整数据库 URL 或 `SECRET_KEY`。

## 五、统一数据库配置

把 `app/database.py` 中数据库地址来源改为：

```python
from sqlalchemy import create_engine  # 导入数据库引擎创建函数
from sqlalchemy.orm import sessionmaker  # 从sqlalchemy.orm模块导入sessionmaker

from app.config import settings  # 从app.config模块导入settings


engine = create_engine(  # 设置或保存engine的值
    settings.database_url,  # 传入settings.database_url的值
    pool_pre_ping=True,  # 设置或保存pool_pre_ping的值
)  # 完成当前调用或数据结构

SessionLocal = sessionmaker(  # 设置或保存SessionLocal的值
    bind=engine,  # 设置或保存bind的值
    autoflush=False,  # 设置或保存autoflush的值
    expire_on_commit=False,  # 设置或保存expire_on_commit的值
)  # 完成当前调用或数据结构
```

`alembic/env.py` 也要读取同一个 `settings.database_url`。修改后执行：

```powershell
alembic current
```

应用和Alembic必须连接同一个项目数据库。练习、测试和生产环境分别使用独立配置，测试不能污染开发数据。

## 六、理解 Lifespan

Lifespan 管理整个 FastAPI 应用启动和关闭时的逻辑：

```text
进程启动
→ 执行 yield 前的代码
→ 开始接收多次请求
→ 停止服务
→ 执行 yield 后的代码
→ 进程结束
```

它与第13章的`yield`依赖相似，但作用范围不同：依赖围绕一次请求，Lifespan围绕整个应用。

## 七、创建 Lifespan

创建 `app/lifespan.py`：

```python
import logging  # 导入应用启动和停止日志工具
from collections.abc import AsyncIterator  # 从collections.abc模块导入AsyncIterator
from contextlib import asynccontextmanager  # 从contextlib模块导入asynccontextmanager

from fastapi import FastAPI  # 导入支持lifespan参数的应用类

from app.config import settings  # 从app.config模块导入settings
from app.database import engine  # 从app.database模块导入engine


logger = logging.getLogger(__name__)  # 设置或保存logger的值


@asynccontextmanager  # 为下面的函数注册框架行为
async def lifespan(app: FastAPI) -> AsyncIterator[None]:  # 定义lifespan函数
    logger.info("application starting: %s", settings.app_name)  # 调用logger.info()
    yield  # 传入yield参数
    engine.dispose()  # 调用engine.dispose()
    logger.info("application stopped")  # 调用logger.info()
```

`@asynccontextmanager`把“在`yield`前进入、在`yield`后退出”的异步生成器函数转换为异步上下文管理器。装饰器直接写在函数上，不传配置参数；被装饰函数必须是包含一次`yield`的`async def`函数。调用后返回异步上下文管理器对象，FastAPI在应用启动和停止时分别进入、退出它。`AsyncIterator[None]`表示该函数异步地产生一次`None`，不向应用提供额外资源对象。

代码含义：

| 位置 | 执行时机 | 当前处理 |
| --- | --- | --- |
| `yield` 前 | 应用开始接收请求前 | 记录启动日志 |
| `yield` | 应用运行期间 | FastAPI 处理请求 |
| `yield` 后 | 应用停止时 | 释放 Engine 连接池并记录日志 |

`engine.dispose()` 释放连接池，不代替 Session 的 `close()`，也不提交业务事务。

## 八、注册 Lifespan

第15章的`app/main.py`已经包含Router、健康检查和四个异常处理器。本节只增加两个导入，并替换原来的单行`app = FastAPI()`；其他代码原样保留。

文件：`app/main.py`  
操作：在导入区追加配置和Lifespan导入，再只替换应用创建语句  
代码类型：局部替换片段

```python
from app.config import settings  # 在现有导入区增加应用配置
from app.lifespan import lifespan  # 在现有导入区增加生命周期函数


app = FastAPI(  # 替换原来的app = FastAPI()
    title=settings.app_name,  # 使用配置设置OpenAPI标题
    lifespan=lifespan,  # 注册应用启动和关闭生命周期
)  # 完成应用创建
```

`FastAPI()` 本示例参数：

| 参数 | 可接受的值 | 默认值或必填性 | 作用 |
| --- | --- | --- | --- |
| `title` | 字符串 | 默认 `"FastAPI"` | 设置 OpenAPI 文档中的应用标题 |
| `lifespan` | 异步上下文管理器函数或 `None` | 默认 `None` | 注册应用启动和关闭期间执行的逻辑 |

替换完成后，原有的部门Router、员工Router、`/health`和四个异常处理器都必须仍在同一个文件中。不要创建第二个`FastAPI()`对象。应用统一使用`lifespan=`，不混用旧式启动和关闭事件装饰器。

## 九、运行与验证

在项目根目录执行：

```powershell
uvicorn app.main:app --reload
```

验证：

1. 配置完整时，终端出现应用启动日志。
2. `/health` 和员工接口仍能使用。
3. 停止服务，终端出现应用停止日志。
4. 暂时删除本地 `DATABASE_URL`，确认应用因配置缺失而明确失败，然后恢复。
5. 执行 `alembic current`，确认迁移工具仍连接正确数据库。
6. 再次制造员工不存在、编号重复和无效部门请求，确认仍分别返回`404`、`409`和`400`。

## 十、常见错误

| 现象 | 原因 | 修正 |
| --- | --- | --- |
| 启动时报配置校验错误 | `.env` 缺少必填项 | 对照 `.env.example` 补充本地配置 |
| Alembic 与应用状态不一致 | 两处读取了不同数据库地址 | 统一读取 `settings.database_url` |
| 每次请求都执行启动逻辑 | 把应用级逻辑写进了依赖 | 移到 Lifespan 的 `yield` 前 |
| 多个请求共享同一 Session | 在 Lifespan 创建并保存 Session | 继续使用第13章请求级`get_db()` |
| 停止时未释放资源 | 清理代码写在 `yield` 前或没有执行 | 把释放逻辑放在 `yield` 后 |

## 十一、完成检查

- [ ] 配置来自环境变量或本地 `.env`。
- [ ] `.env` 不进入版本控制。
- [ ] 应用与 Alembic 使用同一数据库配置。
- [ ] 能区分请求级依赖和应用级 Lifespan。
- [ ] 应用启动与停止日志可以观察。
- [ ] Lifespan 不共享数据库 Session。
- [ ] 第15章已有的Router、健康检查和异常处理器全部保留。

完成后确认启动、关闭和请求级Session使用各自独立的生命周期边界。
