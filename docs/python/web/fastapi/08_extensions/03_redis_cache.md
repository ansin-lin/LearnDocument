# 扩展专题3 Redis 员工列表缓存

> 本专题成果：为员工列表增加短时间 Cache-Aside 缓存，在 Redis 不可用时回退数据库，并在写操作成功后使旧缓存失效。

## 一、适用边界

Redis 不是数据库的替代品。当前项目仍以 MySQL 和 SQLAlchemy 为事实来源：

```text
读取：先查 Redis → 未命中再查数据库 → 写入短期缓存
写入：先提交数据库事务 → 再使列表缓存失效
```

只有在重复查询带来明确性能需求时才引入缓存。没有可验证的性能目标时，保持数据库查询链即可。

## 二、准备依赖和服务

安装 Python 客户端：

```powershell
python -m pip install redis
```

还需要一套独立的本地或测试 Redis。不要连接企业生产实例练习，也不要在教程中使用无密码公网 Redis。

文件：`app/config.py`  
操作：向`Settings`追加字段  
代码类型：配置代码片段

```python
redis_url: str = "redis://127.0.0.1:6379/0"  # 设置本地Redis连接URL和数据库编号
```

`.env.example` 增加：

```text
REDIS_URL=redis://127.0.0.1:6379/0
```

## 三、让Lifespan管理Redis客户端

当前路由和SQLAlchemy Session使用同步函数，因此Redis客户端也使用同步`redis-py`，同一调用链不混用同步与异步客户端。

文件：`app/lifespan.py`  
操作：整体替换现有Lifespan实现  
代码类型：完整文件

```python
from contextlib import asynccontextmanager  # 导入异步上下文管理装饰器

from fastapi import FastAPI  # 导入应用类型
from redis import Redis  # 导入同步Redis客户端

from app.config import settings  # 导入应用配置
from app.database import engine  # 导入数据库引擎


@asynccontextmanager  # 把生成器函数转换为应用生命周期上下文管理器
async def lifespan(app: FastAPI):  # 接收当前应用对象
    cache = Redis.from_url(  # 根据配置创建Redis客户端
        settings.redis_url,  # 使用配置中的连接URL
        decode_responses=True,  # 自动把字节响应解码为字符串
        socket_connect_timeout=1,  # 连接最多等待1秒
        socket_timeout=1,  # 读写最多等待1秒
    )  # 完成客户端配置
    app.state.cache = cache  # 把客户端保存到应用状态
    yield  # 应用在这里运行并处理请求
    cache.close()  # 应用停止时关闭Redis客户端
    engine.dispose()  # 同时释放数据库连接池
```

`Redis.from_url()` 本示例参数：

| 参数 | 可接受的值 | 默认值或必填性 | 作用 |
| --- | --- | --- | --- |
| `url` | `redis://`、`rediss://` 或 `unix://` URL 字符串 | 必填 | 指定 Redis 地址、端口、数据库和可选认证信息 |
| `decode_responses` | `True` 或 `False` | 默认 `False` | 是否把 Redis 返回的字节自动解码为字符串 |
| `socket_connect_timeout` | 非负秒数或 `None` | 默认 `None` | 限制建立 Redis 连接的等待时间 |
| `socket_timeout` | 非负秒数或 `None` | 默认 `None` | 限制已连接后的读写等待时间 |

Redis客户端内部管理连接池，可以在应用生命周期内复用；停止服务时必须关闭。

## 四、创建缓存依赖

文件：`app/dependencies.py`  
操作：追加  
代码类型：项目代码片段

```python
from fastapi import Request  # 导入当前请求对象
from redis import Redis  # 导入Redis客户端类型


def get_cache(request: Request) -> Redis:  # 定义缓存依赖函数
    return request.app.state.cache  # 返回Lifespan创建的共享客户端
```

这里不使用 `yield`，因为客户端由应用级Lifespan统一关闭，不是每个请求创建一次。

## 五、实现Cache-Aside

文件：`app/routers/employees.py`  
操作：追加导入、日志对象和缓存版本函数  
代码类型：项目代码片段

```python
import logging  # 导入标准日志模块

from fastapi import Depends  # 导入依赖声明工具
from redis import Redis  # 导入Redis客户端类型
from redis.exceptions import RedisError  # 导入Redis操作异常

from app.dependencies import get_cache  # 导入缓存依赖


logger = logging.getLogger(__name__)  # 创建当前模块日志记录器


def get_cache_version(cache: Redis) -> str:  # 读取员工列表缓存版本
    try:  # 尝试访问Redis
        return cache.get("employees:version") or "1"  # 缺少版本时使用初始值1
    except RedisError:  # Redis不可用时回退数据库
        logger.warning("cache unavailable")  # 记录缓存故障
        return "disabled"  # 返回禁用标记
```

文件：`app/routers/employees.py`  
操作：插入到员工列表函数的数据库查询之前  
代码类型：项目代码片段

```python
version = get_cache_version(cache)  # 取得当前缓存版本
cache_key = (  # 组合所有会影响列表结果的参数
    f"employees:list:{version}:"  # 加入业务前缀和版本号
    f"{keyword or '-'}:{page}:{size}"  # 加入筛选词和分页值
)  # 完成缓存Key

if version != "disabled":  # Redis可用时才读取缓存
    try:  # 捕获缓存读取失败
        cached = cache.get(cache_key)  # 按Key读取JSON字符串
        if cached is not None:  # 命中缓存时直接返回
            return EmployeeListResponse.model_validate_json(  # 校验并还原响应模型
                cached  # 传入缓存中的JSON字符串
            )  # 返回模型对象
    except RedisError:  # 缓存读取失败时不中断核心查询
        logger.warning("cache read failed")  # 记录警告并继续查询数据库
```

文件：`app/routers/employees.py`  
操作：插入到员工列表函数的数据库查询成功之后  
代码类型：项目代码片段

```python
response = EmployeeListResponse(  # 创建已经校验的列表响应
    items=items,  # 当前页数据
    total=total,  # 筛选后的总件数
    page=page,  # 当前页码
    size=size,  # 每页数量
)  # 完成响应对象

if version != "disabled":  # Redis可用时才写缓存
    try:  # 捕获写入失败
        cache.set(  # 保存短期缓存
            cache_key,  # 使用与读取相同的Key
            response.model_dump_json(),  # 把响应模型序列化为JSON
            ex=60,  # 设置60秒过期时间
        )  # 完成缓存写入
    except RedisError:  # 写缓存失败时保留数据库结果
        logger.warning("cache write failed")  # 记录警告

return response  # 返回数据库查询结果
```

本示例使用的 Redis 方法：

| 方法 | 可接受的值 | 默认值或必填性 | 作用 |
| --- | --- | --- | --- |
| `get(name)` | Key 字符串或字节数据 | `name` 必填 | 读取 Key；不存在时返回 `None` |
| `set(name, value, ex=...)` | Key、可保存的值，以及整数秒数或 `timedelta` | `name`、`value` 必填；`ex` 默认 `None` | 保存缓存，并可用 `ex` 设置过期时间 |
| `incr(name, amount=1)` | Key 和整数增量 | `name` 必填；`amount` 默认 `1` | 原子增加整数值；用于切换缓存版本 |

Redis故障不能让核心员工查询一起失败，因此读取和写入都允许回退数据库。

## 六、写操作后的失效

文件：`app/routers/employees.py`  
操作：追加函数，并在每个写操作提交成功后调用  
代码类型：项目代码片段

```python
def invalidate_employee_list(cache: Redis) -> None:  # 定义列表缓存失效函数
    try:  # 尝试切换版本号
        cache.incr("employees:version")  # 原子增加版本，使旧Key不再命中
    except RedisError:  # Redis故障不回滚已提交事务
        logger.warning("cache invalidation failed")  # 记录失效失败
```

失效必须发生在数据库事务成功之后。缓存失败不能回滚已经成功的业务事务，但需要日志和监控。

## 七、验证

1. 第一次请求列表，确认执行数据库查询。
2. 第二次使用相同参数请求，确认得到相同响应。
3. 暂停Redis，确认列表仍从数据库返回。
4. 恢复Redis并新增员工，确认下一次列表能看到新员工。
5. 等待60秒，确认缓存自动过期。

测试应替换缓存依赖，不依赖真实Redis才能运行单元测试。

## 八、安全与运维边界

- 缓存Key不能包含密码、Token或完整个人信息。
- 缓存内容必须设置TTL，不能无限增长。
- Redis需要认证、网络隔离、连接超时和容量监控。
- Cache-Aside存在短时间旧数据风险，不适合强一致余额或权限判断。
- 不使用 `KEYS *` 清理生产缓存；本例用版本号完成逻辑失效。

参考：[Redis官方redis-py文档](https://redis.io/docs/latest/develop/clients/redis-py/)。
