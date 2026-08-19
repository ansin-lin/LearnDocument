# 第4章 APIRouter 与多文件路由

> 本章成果：把第3章集中在 `app/main.py` 中的员工内存 API 拆到独立 Router，保持 URL、请求、响应和状态码不变，并能解释应用入口与业务路由的关系。

## 一、本章开始状态

第3章结束时，`app/main.py` 同时包含：

- `FastAPI` 应用对象和 `/health`
- Pydantic 请求与响应模型
- 部门、员工内存数据
- 员工 CRUD 路由和部门列表路由

接口能够运行，但应用入口已经承担过多职责。本章只整理路由文件，不连接数据库、不改变字段，也不引入依赖注入。

开始前先启动原项目，在 `/docs` 中确认以下接口可以使用：

```text
GET    /health
GET    /api/employees
GET    /api/employees/{employee_number}
POST   /api/employees
PUT    /api/employees/{employee_number}
DELETE /api/employees/{employee_number}
GET    /api/departments
```

## 二、Router 解决什么问题

`FastAPI` 对象代表整个应用，`APIRouter` 用来组织某一组业务接口。

```text
请求
→ app/main.py 中的 FastAPI 应用
→ include_router() 注册的业务 Router
→ 匹配到具体路由函数
→ 返回响应
```

Router 不会自动加入应用。只有执行 `app.include_router(router)`，其中的接口才会真正出现在应用和 OpenAPI 文档中。

## 三、准备路由目录

在 `app` 下创建 `routers` 目录：

```text
app/
├── __init__.py
├── main.py
└── routers/
    ├── __init__.py
    └── employees.py
```

`__init__.py` 保持为空，用于明确 `routers` 是 Python 包。

## 四、移动员工路由

本步骤是代码移动，不是重新实现 CRUD。先把第3章的 `app/main.py` 复制为 `app/routers/employees.py`，然后只做下面四类修改。

### 4.1 修改导入

把原来的：

```text
from fastapi import FastAPI, HTTPException
```

改为：

```text
from fastapi import APIRouter, HTTPException
```

如果第3章最终导入的名称顺序略有不同，只需要移除 `FastAPI` 并加入 `APIRouter`，其余 Pydantic、日期和类型导入继续保留。

### 4.2 删除应用级内容

从 `employees.py` 删除：

- `app = FastAPI(...)`
- `/health` 路由及其函数

业务路由文件不创建第二个应用对象，也不负责应用级健康检查。

### 4.3 创建 Router

文件：`app/routers/employees.py`  
操作：追加  
代码类型：项目代码片段

```python
router = APIRouter(  # 创建员工业务使用的子路由对象
    prefix="/api",  # 为本 Router 中的路径统一增加 /api 前缀
    tags=["employee-management"],  # 在 OpenAPI 文档中归入员工管理分组
)  # 完成 Router 配置
```

参数作用：

| 参数 | 可接受的值 | 当前值或默认值 | 作用 |
| --- | --- | --- | --- |
| `prefix` | 路径前缀字符串 | 当前`"/api"`；默认空字符串 | 给Router中的全部路径增加公共前缀 |
| `tags` | 字符串或枚举值组成的列表，或`None` | 当前`["employee-management"]`；默认`None` | 在Swagger UI中把接口放到指定分类 |

### 4.4 修改路由装饰器

把业务路由的 `@app` 全部改为 `@router`，并从装饰器路径中移除已经由 `prefix` 提供的 `/api`。

例如：

```text
@app.get("/api/employees", response_model=EmployeeListResponse)
```

改为：

```text
@router.get("/employees", response_model=EmployeeListResponse)
```

部门接口同样处理：

```text
@router.get(
    "/departments",
    response_model=list[DepartmentSummary],
)
```

修改完成后，`employees.py` 应满足：

- 文件中不存在 `FastAPI()`。
- 文件中不存在 `@app.get`、`@app.post`、`@app.put` 或 `@app.delete`。
- 路由装饰器的路径不再以 `/api` 开头。
- Pydantic 模型、内存数据、辅助函数和 CRUD 函数保持第3章状态。

## 五、整理应用入口

文件：`app/main.py`  
操作：整体替换  
代码类型：完整文件

```python
from fastapi import FastAPI  # 导入 FastAPI 应用类

from app.routers.employees import router as employees_router  # 导入员工子路由并设置清楚的别名


app = FastAPI()  # 创建唯一的应用对象
app.include_router(employees_router)  # 把员工子路由注册到应用


@app.get("/health")  # 注册应用级健康检查接口
def health_check() -> dict[str, str]:  # 声明返回字符串键值字典
    return {"status": "ok"}  # 返回健康状态
```

这里的职责很清楚：

| 代码 | 职责 |
| --- | --- |
| `FastAPI(...)` | 创建唯一应用对象 |
| `employees_router` | 取得员工业务 Router |
| `include_router()` | 把业务路由注册到应用 |
| `/health` | 提供应用级健康检查 |

`include_router()` 当前使用参数说明：

| 参数 | 可接受的值 | 默认值或是否必填 | 作用 |
| --- | --- | --- | --- |
| `router` | `APIRouter` 对象 | 必填，可放在第一个位置 | 指定要注册到应用的 Router |
| `prefix` | 以 `/` 开头的路径字符串 | `""` | 在 Router 原有路径前再增加公共前缀 |
| `tags` | 字符串列表或 `None` | `None` | 覆盖该 Router 在 OpenAPI 文档中的分组标签 |
| `dependencies` | `Depends(...)` 对象列表或 `None` | `None` | 为该 Router 的所有接口增加共同依赖 |

当前 Router 已经在 `APIRouter(prefix="/api", tags=[...])` 中声明前缀和标签，因此这里只传入 `employees_router`，避免路径重复。

不要在 `main.py` 中保留第二套员工路由，否则相同方法和路径会被重复注册。

## 六、为什么 URL 没有变化

Router 前缀与装饰器路径会组合：

```text
prefix="/api"
+ 路由路径="/employees"
= 最终路径="/api/employees"
```

因此这次改修只改变代码位置，不改变前端或调用方使用的接口规格。

## 七、运行与回归验证

在项目根目录启动：

```powershell
uvicorn app.main:app --reload
```

按顺序验证：

1. 打开 `http://127.0.0.1:8000/docs`。
2. 确认员工和部门接口仍位于原来的 `/api/...` 路径。
3. 查询 `E001`，确认响应字段没有变化。
4. 新增、修改并逻辑删除一个练习员工。
5. 请求 `/health`，确认返回 `{"status":"ok"}`。
6. 暂时注释 `app.include_router(employees_router)`，确认业务接口从 `/docs` 消失而 `/health` 仍存在，然后恢复代码。

## 八、常见错误

| 现象 | 原因 | 修正 |
| --- | --- | --- |
| Router 接口没有出现在 `/docs` | 忘记调用 `include_router()` | 在唯一应用对象上注册 Router |
| 路径变成 `/api/api/employees` | Router 和装饰器都保留了 `/api` | 公共前缀只在一处声明 |
| 出现两组相同接口 | `main.py` 仍保留旧业务路由 | 删除入口文件中的重复实现 |
| `No module named app.routers` | 缺少 `routers/__init__.py` 或启动目录错误 | 补充空文件并从项目根目录启动 |
| `/health` 消失 | 移动代码时把健康检查也移走了 | 把 `/health` 保留在 `main.py` |

## 九、完成检查

- [ ] `main.py` 只负责创建应用、注册 Router 和健康检查。
- [ ] 员工与部门路由位于 `app/routers/employees.py`。
- [ ] `/api/employees` 等原有 URL 没有变化。
- [ ] `/docs` 中只有一组员工接口。
- [ ] 能说明 `FastAPI` 应用对象、`APIRouter` 和 `include_router()` 的关系。

完成后保留当前Router路径和应用入口，数据库接入时只替换数据来源，不改变公开URL。
