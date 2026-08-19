# 第6章 yield依赖与资源清理

> 本章成果：使用`yield`依赖管理一个请求级演示资源，验证接口成功或异常时都能执行清理，并区分普通依赖、请求级资源和应用级资源。

第5章的分页依赖计算完成后直接`return`结果。有些依赖还会占用数据库Session、临时文件或外部连接，这类资源使用结束后必须释放。

## 一、为什么return不能表达使用后的清理

代码位置：独立Python实验  
操作：只运行观察，不加入项目  
代码类型：错误示例

```python
def get_resource() -> str:  # 定义错误的资源获取示例
    resource = "已获取的资源"  # 创建演示资源
    return resource  # 返回后函数立即结束
    print("释放资源")  # 这一行永远不会执行
```

资源依赖需要表达三个阶段：

```text
获取资源
→ 路径函数使用资源
→ 释放资源
```

## 二、创建可观察的演示资源

文件：`app/resources.py`  
操作：临时新建，第7章前删除  
代码类型：完整实验文件

```python
class DemoResource:  # 定义可观察状态的演示资源
    def __init__(self) -> None:  # 创建资源时执行初始化
        self.closed = False  # 记录资源尚未关闭
        print("获取资源")  # 显示进入生命周期

    def use(self) -> str:  # 定义资源使用方法
        if self.closed:  # 阻止使用已关闭资源
            raise RuntimeError("资源已经关闭")  # 抛出运行时错误
        return "资源使用成功"  # 返回观察结果

    def close(self) -> None:  # 定义资源清理方法
        self.closed = True  # 更新关闭状态
        print("释放资源")  # 显示退出生命周期
```

这个类只用于观察生命周期，不代表真实数据库连接。`closed`保存当前状态；资源关闭后再次调用`use()`会抛出异常。

## 三、使用yield建立进入和退出阶段

文件：`app/dependencies.py`  
操作：临时追加，第7章前删除  
代码类型：项目实验片段

```python
from collections.abc import Generator  # 导入同步生成器类型

from app.resources import DemoResource  # 导入演示资源类


def get_demo_resource() -> Generator[DemoResource, None, None]:  # 定义 yield 依赖
    resource = DemoResource()  # 在路径函数执行前创建资源
    try:  # 开始保证清理的异常处理结构
        yield resource  # 暂停依赖并注入资源
    finally:  # 成功或异常都会进入清理阶段
        resource.close()  # 关闭本次请求使用的资源
```

代码执行过程：

| 位置 | 执行时机 | 结果 |
| --- | --- | --- |
| `resource = DemoResource()` | 路径函数之前 | 创建本次请求使用的资源 |
| `yield resource` | 注入依赖结果时 | 把资源交给路径函数并暂停依赖函数 |
| `finally` | 路径函数结束后的退出阶段 | 成功或异常时都调用`close()` |

`Generator[DemoResource, None, None]`表示该生成器产出`DemoResource`，不接收外部发送值，结束时不返回额外结果。一个FastAPI `yield`依赖只应产出一次。

## 四、把资源注入路径函数

文件：`app/routers/employees.py`  
操作：临时追加，第7章前删除  
代码类型：项目实验片段

```python
from typing import Annotated  # 导入带元数据的类型标注工具

from fastapi import Depends  # 导入依赖声明工具

from app.dependencies import get_demo_resource  # 导入资源依赖函数
from app.resources import DemoResource  # 导入资源类型


DemoResourceDep = Annotated[  # 定义可复用的资源依赖类型
    DemoResource,  # 路径函数取得的对象类型
    Depends(get_demo_resource),  # 指定对象由 yield 依赖提供
]  # 结束类型别名


@router.get("/resource-demo")  # 注册生命周期观察接口
def use_resource(resource: DemoResourceDep) -> dict[str, str]:  # 注入请求级资源
    return {"message": resource.use()}  # 使用资源并返回结果
```

请求`GET /api/resource-demo`时，响应为：

```json
{"message":"资源使用成功"}
```

终端依次出现：

```text
获取资源
释放资源
```

路径函数只使用资源，不负责创建和关闭。生命周期由依赖统一管理。

## 五、验证异常时仍然清理

文件：`app/routers/employees.py`  
操作：临时追加，验证后立即删除  
代码类型：故障实验片段

```python
@router.get("/resource-error")  # 注册故意抛出异常的实验接口
def use_resource_with_error(  # 定义use_resource_with_error函数
    resource: DemoResourceDep,  # 注入需要在异常后清理的资源
) -> dict[str, str]:  # 声明正常情况下的响应类型
    resource.use()  # 先确认资源可用
    raise RuntimeError("模拟路径函数异常")  # 故意触发服务器错误
```

请求后接口返回服务器错误，但终端仍会打印`释放资源`。这是`finally`的作用：无论路径函数正常返回还是抛出异常，清理动作都执行。

完成实验后删除`/resource-error`，避免把故意失败的接口带入后续项目。

## 六、request与function作用域

含`yield`的依赖没有显式设置`scope`时，使用request作用域。它在路径函数前进入，在响应发送完成后退出。

代码位置：`app/routers/employees.py`中的依赖类型定义  
操作：当前只阅读  
代码类型：语法片段

```python
FunctionResourceDep = Annotated[  # 定义较早清理资源的依赖类型
    DemoResource,  # 路径函数取得的对象类型
    Depends(  # 创建依赖声明
        get_demo_resource,  # 指定资源提供函数
        scope="function",  # 在路径函数结束后、响应发送前清理
    ),  # 结束依赖声明
]  # 结束类型别名
```

| `scope`值 | 清理时机 | 适用情况 |
| --- | --- | --- |
| `None` | 对含`yield`依赖采用默认request作用域 | 通常保留默认值 |
| `"request"` | 响应发送完成后退出依赖 | 响应阶段仍可能使用资源 |
| `"function"` | 路径函数结束后、响应发送前退出依赖 | 响应阶段不再需要资源 |

返回流式响应时，如果迭代生成内容仍要读取资源，不能在发送响应前提前关闭资源。课程第13章的数据库Session先使用默认request作用域。

## 七、与Python上下文管理的关系

代码位置：独立Python实验  
操作：只阅读或在临时文件上运行  
代码类型：语法示例

```python
with open("example.txt", encoding="utf-8") as file:  # 进入文件上下文并在退出时自动关闭
    content = file.read()  # 在上下文内部读取文本内容
```

离开`with`代码块时会关闭文件。FastAPI会把含单个`yield`的依赖作为上下文管理流程使用；编写依赖时不需要额外添加`@contextmanager`装饰器。

`yield`表示进入和退出阶段，不等于异步。依赖可以使用`def`或`async def`，应根据它调用的库是否提供异步接口来选择。

## 八、请求级资源与应用级资源

| 能力 | 生命周期 | 典型用途 |
| --- | --- | --- |
| 普通`return`依赖 | 计算完成即返回结果 | 分页参数、当前用户、配置读取 |
| `yield`依赖 | 围绕一次路径函数或请求 | 数据库Session、临时资源 |
| Lifespan | 围绕整个应用启动到关闭 | 连接池客户端、模型预加载、启动检查 |
| Middleware | 包围每次HTTP请求和响应 | 请求编号、耗时、公共响应头 |

不要把请求级Session创建在模块全局变量或Lifespan中共享给所有请求。第16章会实现Lifespan，第17章会实现Middleware，并再次验证完整执行顺序。

## 九、常见错误

| 现象 | 原因 | 修正 |
| --- | --- | --- |
| 异常时没有释放资源 | 清理代码没有放在`finally` | 使用`try/finally`包围`yield` |
| 路径函数自己关闭资源 | 使用者同时管理生命周期 | 由依赖统一清理 |
| 一个依赖产生多个值 | 写了多个`yield` | 一个依赖只`yield`一次，必要时组合对象或拆分依赖 |
| 以为`yield`会自动异步 | 混淆生成器和协程 | 根据底层库选择`def`或`async def` |
| 流式响应读取到关闭资源 | 使用了过早的function作用域 | 保留request作用域或调整数据读取边界 |

## 十、本章练习

1. 给`DemoResource`增加唯一编号，证明连续两次请求得到不同对象。
2. 请求正常接口和故障接口，保存终端中的获取、使用和释放顺序。
3. 删除故障接口后重新打开`/docs`，确认它不再出现在接口列表中。
4. 画出普通依赖、yield依赖和Lifespan三个生命周期的范围。

## 十一、完成检查

- [ ] 能解释`yield`前、路径函数和`yield`后的执行顺序。
- [ ] 能使用`try/finally`保证成功与异常路径都清理资源。
- [ ] 能区分request与function作用域。
- [ ] 能说明`yield`不等于`async/await`。
- [ ] 能说明数据库Session为什么不能保存为所有请求共享的全局对象。

进入第7章前，把本章的临时演示代码全部清理掉：

1. 从`app/routers/employees.py`删除`/resource-demo`路径函数。
2. 从`app/routers/employees.py`删除`DemoResourceDep`，以及只供它使用的`DemoResource`、`get_demo_resource`和`Depends`导入；如果`Annotated`仍被其他代码使用则保留。
3. 从`app/dependencies.py`删除`get_demo_resource()`，以及只供它使用的`Generator`和`DemoResource`导入。
4. 删除`app/resources.py`文件。
5. 保留`app/dependencies.py`中的`PaginationParams`、`get_pagination()`和`PaginationDep`，后续数据库列表接口会继续使用它们。

清理后重新启动服务并打开`/docs`。确认`/resource-demo`和`/resource-error`都不再出现，应用启动时也没有`DemoResource`导入错误。
