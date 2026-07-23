# 第1章 FastAPI 入门

> 本章目标：理解 FastAPI 用来开发什么，掌握项目创建、启动服务、编写第一个接口和查看自动接口文档。

## 一、FastAPI 是什么

FastAPI 是 Python 的 Web API 框架，主要用于开发后端接口。

它常用于：

| 场景 | 说明 |
| --- | --- |
| 前后台分离系统 | 给 Vue、React、移动端提供 JSON API |
| 后端服务 | 提供业务接口、查询接口、保存接口 |
| 内部系统接口 | 系统之间进行数据联动 |
| 自动化平台 | 提供任务执行、结果查询接口 |
| AI 应用接口 | 把模型能力包装成 HTTP API |

FastAPI 的特点：

- 写法接近普通 Python 函数
- 自动根据类型提示做参数转换
- 自动生成接口文档
- 支持同步函数和异步函数
- 适合开发 REST API

## 二、安装依赖

建议在虚拟环境中安装。

```powershell
pip install fastapi uvicorn  # 安装 FastAPI 和 Uvicorn 开发服务器
```

依赖说明：

| 依赖 | 作用 |
| --- | --- |
| `fastapi` | Web API 框架本体 |
| `uvicorn` | 运行 FastAPI 应用的 ASGI 服务器 |

## 三、创建第一个接口

文件位置：

```text
main.py
```

```python
from fastapi import FastAPI  # 导入 FastAPI 类，用于创建应用对象

app = FastAPI()  # 创建 FastAPI 应用对象


@app.get("/")  # 注册 GET / 路由
def read_root():  # 定义路由处理函数
    return {"message": "Hello FastAPI"}  # 返回 JSON 响应
```

代码说明：

| 代码 | 作用 |
| --- | --- |
| `FastAPI()` | 创建应用对象 |
| `@app.get("/")` | 注册 GET 请求路由 |
| `read_root()` | 请求到达时执行的函数 |
| `return {"message": ...}` | 返回 JSON 数据 |

## 四、启动项目

在 `main.py` 所在目录执行：

```powershell
uvicorn main:app --reload  # 启动 FastAPI 应用，开发环境开启自动重载
```

命令说明：

| 内容 | 说明 |
| --- | --- |
| `main` | Python 文件名，不写 `.py` |
| `app` | `main.py` 中的 FastAPI 应用对象 |
| `--reload` | 代码变更后自动重启，开发环境使用 |

访问：

```text
http://127.0.0.1:8000/
```

预期响应：

```json
{"message":"Hello FastAPI"}
```

## 五、自动接口文档

FastAPI 会自动生成接口文档。

| 地址 | 说明 |
| --- | --- |
| `http://127.0.0.1:8000/docs` | Swagger UI 文档 |
| `http://127.0.0.1:8000/redoc` | ReDoc 文档 |

自动文档可以查看：

- 接口地址
- 请求方法
- 参数
- 请求体
- 响应结构
- 在线测试接口

## 六、同步函数和异步函数

FastAPI 支持两种路由函数。

同步函数：

```python
@app.get("/sync")  # 注册 GET /sync 路由
def sync_api():  # 定义同步接口函数
    return {"type": "sync"}  # 返回同步接口结果
```

异步函数：

```python
@app.get("/async")  # 注册 GET /async 路由
async def async_api():  # 定义异步接口函数
    return {"type": "async"}  # 返回异步接口结果
```

新人阶段不需要把所有接口都写成 `async def`。如果代码中调用的是普通同步数据库驱动或同步文件处理，盲目使用 `async def` 不会自动提升性能。

## 七、日本项目中的使用场景

| 日语表达 | 中文说明 |
| --- | --- |
| API 開発 | 接口开发 |
| バックエンド | 后端 |
| リクエスト | 请求 |
| レスポンス | 响应 |
| 画面連携 | 页面联动 |
| 単体試験 | 单体测试 |

Code Review 中常见关注点：

1. 路由路径是否清晰
2. HTTP 方法是否正确
3. 返回结构是否稳定
4. 是否有输入校验
5. 是否有错误处理
6. 是否能通过接口文档验证

## 八、基础练习

请完成：

1. 创建 `main.py`
2. 编写 `GET /` 接口
3. 启动服务
4. 访问 `/`
5. 打开 `/docs` 查看接口文档

## 九、综合练习

新增一个接口：

```text
GET /health
```

要求返回：

```json
{"status":"ok"}
```

参考代码：

```python
@app.get("/health")  # 注册健康检查接口
def health_check():  # 定义健康检查函数
    return {"status": "ok"}  # 返回系统状态
```

## 十、本章总结

- FastAPI 主要用于开发 Web API
- `FastAPI()` 用于创建应用对象
- `@app.get()` 用于注册 GET 接口
- `uvicorn main:app --reload` 用于启动开发服务器
- `/docs` 可以查看自动接口文档
- 新人阶段先掌握请求、路由函数和响应的最小闭环
