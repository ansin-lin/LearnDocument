# 第15章 RESTful API 与 FastAPI 入门与实战

> 学习目标
>
> - 理解 RESTful API 设计规范与常见约定（资源、HTTP 方法、状态码、版本化、分页）
> - 掌握 FastAPI 的核心用法：路径参数 / Query 参数、请求体与 Pydantic 校验、响应模型
> - 能构建一个前后端分离的接口项目，具备文档、自校验、错误处理、CORS 支持

---

## 一、RESTful API 基础规范

### 1️⃣ 资源与URL设计

- 资源名使用**复数英文名词**，小写，使用连字符 `-` 分割：`/api/v1/users`、`/api/v1/order-items`
- 嵌套资源用层级表达：`/users/{user_id}/orders`
- 过滤、排序、分页通过 **query 参数** 表达：`/users?role=admin&sort=-created_at&page=2&page_size=20`

### 2️⃣ HTTP 方法与语义

| 方法 | 语义 | 幂等 | 场景 |
|-----|------|------|------|
| GET | 读取资源 | ✅ | 列表/详情 |
| POST | 创建资源 | ❌ | 新增 |
| PUT | 全量更新 | ✅ | 覆盖更新 |
| PATCH | 局部更新 | ❌ | 修改部分字段 |
| DELETE | 删除 | ✅ | 删除 |

### 3️⃣ 常见状态码

| 状态码 | 含义 | 说明 |
|-------|------|------|
| 200 OK | 成功 | GET/PUT/PATCH 的正常返回 |
| 201 Created | 已创建 | POST 成功创建资源 |
| 204 No Content | 成功无响应体 | DELETE 成功 |
| 400 Bad Request | 请求无效 | 参数或格式错误 |
| 401 Unauthorized | 未认证 | 需要登录 |
| 403 Forbidden | 已认证但无权限 | 访问受限 |
| 404 Not Found | 不存在 | 资源不存在 |
| 409 Conflict | 资源冲突 | 重复、版本冲突 |
| 422 Unprocessable Entity | 语义校验失败 | Pydantic 校验错误 |
| 500 Internal Server Error | 服务器错误 | 未处理异常 |

### 4️⃣ 版本化与分页

- 版本化：`/api/v1/...`、`/api/v2/...`（或 Header: `Accept: application/vnd.myapp.v1+json`）
- 分页：`page` 与 `page_size`（限制最大 `page_size`，如 100）
- 响应中返回分页元数据：`{"items":[...], "total": 123, "page": 2, "page_size": 20}`

---

## 二、FastAPI 快速上手

### 1️⃣ 安装与运行

```bash
pip install fastapi uvicorn[standard] pydantic
uvicorn app:app --reload
```

- `fastapi`：框架主体  
- `uvicorn`：ASGI 服务器  
- `pydantic`：数据模型与校验（FastAPI 内部使用）

### 2️⃣ 项目结构推荐

```python
fastapi-demo/
 ├── app.py
 ├── models.py        # Pydantic 模型
 ├── routers/
 │   ├── users.py     # 用户路由
 │   └── items.py
 ├── deps.py          # 依赖注入
 ├── schemas.py       # 响应/请求模式（可与 models 合并）
 └── main.py          # 启动入口（可选）
```

### 3️⃣ 最小示例

```python
# app.py
from fastapi import FastAPI

app = FastAPI(title="Demo API", version="1.0.0")

@app.get("/ping")
def ping():
    return {"message": "pong"}
```

文档：`/docs` (Swagger UI) 、`/redoc` (ReDoc)

---

## 三、路径参数与 Query 参数

### 1️⃣ 路径参数

```python
from fastapi import FastAPI
app = FastAPI()

@app.get("/users/{user_id}")
def read_user(user_id: int):
    return {"user_id": user_id}
```

### 2️⃣ Query 参数（含默认值与校验）

```python
from fastapi import Query

@app.get("/search")
def search(q: str = Query(..., min_length=1, max_length=50),
           page: int = Query(1, ge=1),
           page_size: int = Query(20, ge=1, le=100)):
    return {"q": q, "page": page, "page_size": page_size}
```

### 3️⃣ Path/Query 常用校验参数（要点）

- `Query(..., min_length, max_length, regex, ge, le, gt, lt, description, example)`
- `Path(..., ge, le, title, description)`

---

## 四、Pydantic 模型与请求体校验

### 1️⃣ 定义请求/响应模型

```python
from pydantic import BaseModel, Field, EmailStr
from typing import Optional, List
from datetime import datetime

class UserIn(BaseModel):
    name: str = Field(..., min_length=1, max_length=50)
    email: EmailStr
    age: Optional[int] = Field(None, ge=0, le=150)

class UserOut(BaseModel):
    id: int
    name: str
    email: EmailStr
    created_at: datetime
```

### 2️⃣ 在路由中使用模型

```python
from fastapi import FastAPI, status
from datetime import datetime

app = FastAPI()

@app.post("/users", response_model=UserOut, status_code=status.HTTP_201_CREATED)
def create_user(user: UserIn):
    new_user = UserOut(id=1, name=user.name, email=user.email, created_at=datetime.utcnow())
    return new_user
```

### 3️⃣ 模型嵌套与列表

```python
class Item(BaseModel):
    name: str
    price: float = Field(..., gt=0)

class OrderIn(BaseModel):
    user_id: int
    items: List[Item]
```

### 4️⃣ 常用 Pydantic 功能（无序列举）

- **类型**：`str/int/float/bool/datetime/date/UUID/EmailStr/AnyUrl`  
- **校验**：`Field(..., ge/le/gt/lt, min_length/max_length, regex)`  
- **默认值**与**可选**：`Optional[T] = None`  
- **配置**：`model_config`（v2）/ `Config`（v1）控制 json 序列化、别名、任意类型等  
- **自定义校验**：`field_validator`/`model_validator`（v2）  

---

## 五、响应模型、状态码与错误处理

### 1️⃣ 响应模型（response_model）

- **作用**：过滤返回字段、自动生成文档、保证返回结构一致

```python
from typing import List

@app.get("/users", response_model=List[UserOut])
def list_users():
    return [
        {"id": 1, "name": "Tom", "email": "tom@mail.com", "created_at": "2025-01-01T00:00:00Z"}
    ]
```

### 2️⃣ 错误处理与 HTTPException

```python
from fastapi import HTTPException, status

@app.get("/users/{user_id}", response_model=UserOut)
def get_user(user_id: int):
    if user_id != 1:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="User not found")
    # 假数据
    return UserOut(id=1, name="Tom", email="tom@mail.com", created_at=datetime.utcnow())
```

### 3️⃣ 自定义响应与头

```python
from fastapi import Response
from starlette.responses import JSONResponse

@app.get("/custom")
def custom():
    return JSONResponse(content={"ok": True}, headers={"X-Trace-Id": "abc-123"})
```

---

## 六、依赖注入、路由拆分与 CORS

### 1️⃣ 依赖注入（Depends）

```python
from fastapi import Depends

def get_pagination(page: int = Query(1, ge=1), page_size: int = Query(20, ge=1, le=100)):
    return {"page": page, "page_size": page_size}

@app.get("/orders")
def list_orders(p=Depends(get_pagination)):
    return {"meta": p, "items": []}
```

### 2️⃣ 拆分路由（APIRouter）

```python
# routers/users.py
from fastapi import APIRouter
router = APIRouter(prefix="/users", tags=["users"])

@router.get("")
def list_users():
    return []

# app.py
from fastapi import FastAPI
from routers.users import router as users_router

app = FastAPI()
app.include_router(users_router, prefix="/api/v1")
```

### 3️⃣ CORS（跨域）

```python
from fastapi.middleware.cors import CORSMiddleware

app.add_middleware(
    CORSMiddleware,
    allow_origins=["http://localhost:3000"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)
```

---

## 七、分页、排序、过滤与统一响应结构

### 1️⃣ 统一响应结构（推荐）

```python
from typing import Generic, TypeVar, List
from pydantic import BaseModel
T = TypeVar("T")

class PageMeta(BaseModel):
    total: int
    page: int
    page_size: int

class Page(BaseModel):
    items: list
    meta: PageMeta
```

### 2️⃣ 分页参数与返回

```python
@app.get("/products")
def list_products(page: int = Query(1, ge=1),
                  page_size: int = Query(20, ge=1, le=100),
                  sort: str = Query("-created_at")):
    # 省略数据库操作
    items = [{"id": 1, "name": "Book"}]
    total = 1
    return {"items": items, "meta": {"total": total, "page": page, "page_size": page_size}}
```

---

## 八、认证授权（概念速览，进阶可扩展为 JWT/OAuth2）

- **Header**：`Authorization: Bearer <token>`  
- **依赖注入**解码 token，获取当前用户  
- **分角色/权限**：在路由层或服务层判断角色

> 可选依赖：`python-jose`（JWT）、`passlib`（密码哈希）、`fastapi.security`（OAuth2PasswordBearer 等）

---

## 九、自动文档与数据示例

- Swagger UI：`/docs` 自动生成交互式文档  
- ReDoc：`/redoc` 档案风格文档  
- 通过 `Field(example=...)` / `Query(example=...)` 提供示例  
- `responses` 参数自定义不同状态码响应模型

```python
@app.get("/health", responses={200: {"description": "Service healthy"}})
def health():
    return {"status": "ok"}
```

---

## 🔧 常用模块/依赖（无序列举：函数与参数要点）

- **fastapi.FastAPI**：创建应用，关键参数：`title`、`version`、`docs_url`、`redoc_url`、`openapi_url`
- **fastapi.APIRouter**：路由拆分，参数：`prefix`、`tags`、`dependencies`
- **fastapi.params**：`Path`、`Query`、`Body`、`Header`、`Cookie`、`Depends`
- **fastapi.responses**：`JSONResponse`、`PlainTextResponse`、`HTMLResponse`、`StreamingResponse`
- **fastapi.exceptions.HTTPException**：`status_code`、`detail`、`headers`
- **fastapi.middleware.cors.CORSMiddleware**：跨域设置，`allow_origins`、`allow_methods`、`allow_headers`
- **pydantic.BaseModel / Field**：描述请求/响应模型与校验，`ge/le/gt/lt`、`min_length/max_length`、`pattern/regex`、`default_factory`
- **starlette.status**：HTTP 状态码常量：`HTTP_200_OK`、`HTTP_201_CREATED` 等
- **uvicorn.run**：开发运行，参数：`host`、`port`、`reload`

---

## 🔬 测试与调试（简单示例）

### 方式一：`requests` 发起 HTTP 请求

```python
import requests
resp = requests.get("http://127.0.0.1:8000/ping")
print(resp.status_code, resp.json())
```

### 方式二：`fastapi.testclient`（单元测试）

```python
from fastapi.testclient import TestClient
from app import app

client = TestClient(app)

def test_ping():
    resp = client.get("/ping")
    assert resp.status_code == 200
    assert resp.json()["message"] == "pong"
```

---

## ✅ 小结

| 能力 | 你将掌握 |
|------|----------|
| REST 设计 | 资源化 URL、方法语义、状态码、版本化、分页 |
| FastAPI | 路由、参数、请求体、响应模型、错误处理 |
| Pydantic | 强类型校验与自动文档 |
| 工程化 | 路由拆分、依赖注入、CORS、统一响应结构 |
| 文档与测试 | Swagger/Redoc、单元测试 |

---

## 📝 课后练习

1. 建立一个 `/api/v1/users` 路由：  
   - `POST /users` 创建用户（请求体用 `UserIn`，响应 `UserOut`，返回 201）  
   - `GET /users` 支持 `page/page_size` 分页与 `role` 过滤  
   - `GET /users/{id}` 返回 404 时给出明确 `detail`  
2. 为以上路由编写 `fastapi.testclient` 单元测试。  
3. 打开 `/docs`，为每个接口补充示例与状态码说明。

---
