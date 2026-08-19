# 第1章 FastAPI 入门

> 本章目标：理解 FastAPI 用来开发什么，掌握项目创建、启动服务、编写第一个接口和查看自动接口文档。

本章开始 [FastAPI 员工管理 API](../index.md)。课程将从最小健康检查接口开始，逐步完成数据库、认证、测试和部署，不要求先学习其他Web框架。

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

## 二、创建课程项目和虚拟环境

打开PowerShell，先创建本课程独立项目目录：

```powershell
New-Item -ItemType Directory -Path employee_api  # 创建课程项目根目录
Set-Location employee_api  # 进入后续命令统一使用的项目根目录
Get-Location  # 确认当前目录末尾是employee_api
python --version  # 确认当前终端可以调用Python
```

本课程后续所说的“项目根目录”，就是当前`employee_api`目录。命令提示找不到`python`时，先完成Python安装和PATH配置，再继续操作。

在项目根目录创建并激活虚拟环境：

```powershell
python -m venv .venv  # 在项目中创建独立Python环境
.\.venv\Scripts\Activate.ps1  # 激活当前项目的PowerShell虚拟环境
python -m pip install --upgrade pip  # 更新当前虚拟环境中的pip
```

激活后，PowerShell提示符通常会出现`(.venv)`。执行下面的命令确认`python`来自当前项目：

```powershell
python -c "import sys; print(sys.executable)"  # 输出当前Python解释器路径
```

输出路径应指向`employee_api\.venv\Scripts\python.exe`。以后重新打开PowerShell时，先进入`employee_api`，再执行激活命令。

创建最初的应用目录和文件：

```powershell
New-Item -ItemType Directory -Path app  # 创建应用包目录
New-Item -ItemType File -Path app/__init__.py  # 创建Python包标记文件
New-Item -ItemType File -Path app/main.py  # 创建应用入口文件
```

执行后目录应为：

```text
employee_api/
├── .venv/
└── app/
    ├── __init__.py
    └── main.py
```

`.venv`只属于本地开发环境，后续加入Git时不能提交该目录。

## 三、安装依赖

确认PowerShell提示符中已经出现`(.venv)`，再安装依赖：

```powershell
python -m pip install "fastapi[standard]"  # 安装 FastAPI 官方标准依赖组合
```

依赖说明：

| 依赖 | 作用 |
| --- | --- |
| `fastapi` | Web API 框架本体 |
| `uvicorn` | 标准依赖中包含的 ASGI 服务器，用于运行应用 |

## 四、创建第一个接口

文件位置：

```text
app/
├── __init__.py
└── main.py
```

```python
from fastapi import FastAPI  # 导入 FastAPI 类，用于创建应用对象

app = FastAPI()  # 创建 FastAPI 应用对象


@app.get("/health")  # 注册健康检查路由
def health_check():  # 定义路由处理函数
    return {"status": "ok"}  # 返回 JSON 响应
```

代码说明：

| 代码 | 作用 |
| --- | --- |
| `FastAPI()` | 创建应用对象 |
| `@app.get("/health")` | 注册 `GET /health` 路由 |
| `health_check()` | 请求到达时执行的函数 |
| `return {"status": "ok"}` | 返回 JSON 数据 |

## 五、启动项目

在包含 `app` 目录的项目根目录执行：

```powershell
uvicorn app.main:app --reload  # 启动 FastAPI 应用，开发环境开启自动重载
```

命令说明：

| 内容 | 说明 |
| --- | --- |
| `app.main` | `app/main.py` 模块路径 |
| `app` | `main.py` 中的 FastAPI 应用对象 |
| `--reload` | 代码变更后自动重启，开发环境使用 |

访问：

```text
http://127.0.0.1:8000/health
```

预期响应：

```json
{"status":"ok"}
```

## 六、自动接口文档

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

## 七、同步函数和异步函数

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

## 八、日本项目中的使用场景

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

## 九、基础练习

请完成：

1. 确认当前目录是`employee_api`并激活`.venv`。
2. 创建`app/__init__.py`和`app/main.py`。
3. 编写`GET /health`接口。
4. 启动服务并访问`/health`。
5. 打开`/docs`查看接口文档。

## 十、综合练习

把响应临时改成下面的错误结构并访问 `/health`：

```text
{"state":"ok"}
```

观察自动文档和实际响应都发生了什么变化，再恢复为 `{"status": "ok"}`。记录修改前后结果，确认代码、文档和响应来自同一个应用。

## 十一、本章总结

- FastAPI 主要用于开发 Web API
- `FastAPI()` 用于创建应用对象
- `@app.get()` 用于注册 GET 接口
- `uvicorn app.main:app --reload` 用于启动开发服务器
- `/docs` 可以查看自动接口文档
- 新人阶段先掌握请求、路由函数和响应的最小闭环
- 后续命令统一从`employee_api`项目根目录执行，并使用项目自己的`.venv`
