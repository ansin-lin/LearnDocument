# 第13章 Session依赖与只读查询API

> 本章成果：把第4章的内存查询替换为数据库查询，通过请求级Session依赖完成员工列表、员工详情和部门列表，并把“员工不存在”业务异常转换为`404`响应。

本章建议按三个检查点完成：先建立`SessionDep`并确认请求结束会关闭Session；再迁移响应Schema和三个只读接口；最后注册`404`处理器并执行完整查询回归。每一步都保持应用可以启动。

本章接入第7～9章完成的`Department`、`Employee`、样例数据和业务规则，把第5～6章掌握的依赖生命周期应用到SQLAlchemy Session。

## 一、本章开始状态

开始前应已依次完成：

- [第10章：数据库配置、模型与迁移](../04_database_project/10_models_migrations.md)
- [第11章：EmployeeRepository与数据库CRUD](../04_database_project/11_crud_transactions.md)
- [第12章：EmployeeService、事务边界与DTO](../04_database_project/12_repository_service_dto.md)

当前项目根目录为 `employee_api`，稳定状态至少包含：

```text
employee_api/
├── app/
│   ├── __init__.py
│   ├── main.py
│   ├── config.py
│   ├── database.py
│   ├── models.py
│   ├── seed.py
│   ├── routers/
│   │   ├── __init__.py
│   │   └── employees.py
│   ├── repositories/
│   │   ├── __init__.py
│   │   └── employee_repository.py
│   └── services/
│       ├── __init__.py
│       └── employee_service.py
├── alembic/
├── alembic.ini
└── requirements.txt
```

数据库中已经存在：

- 开发部和营业部
- `E001 山田太郎`
- `E002 佐藤花子`
- Service验证中创建的练习员工已经设置为离职

不要重新定义第二套 SQLAlchemy `Employee`，也不要调用 `Base.metadata.create_all()`。表结构统一由 Alembic 管理。

## 二、从内存实现迁移

第4章已经把内存员工 API 移到 `app/routers/employees.py`，`app/main.py` 只负责创建应用、注册 Router 和健康检查。本章按下面的范围替换数据来源：

| 第4章状态 | 本章处理 |
| --- | --- |
| Pydantic 响应模型 | 移入新文件 `app/schemas.py` |
| `departments`、`employees` 内存列表 | 删除，由数据库表代替 |
| `find_department()`、`build_employee_response()` | 删除，由 ORM 关系和响应模型代替 |
| `app/routers/employees.py` 中的内存 CRUD | 本章替换为数据库员工列表和详情查询 |
| `GET /api/departments` | 移入 `departments.py`，改为数据库部门列表查询 |
| `app/main.py` 与 `/health` | 保留唯一应用对象和健康检查，并注册两个数据库 Router |
| 员工列表 | 改为 Router → Service → Repository → 数据库 |

本章完成全部只读接口。新增、修改和逻辑删除留到第14章，形成“先确认查询链，再增加写事务”的清晰顺序。替换`employees.py`时不要保留第4章的内存路由，否则相同路径可能被注册两次。

## 三、增加请求级Session依赖

第5章已经创建`app/dependencies.py`并保留分页依赖。现在把文件整理为下面的完整状态，在原有内容之后增加Session依赖，不要删除`PaginationDep`：

文件：`app/dependencies.py`  
操作：替换为下面的完整状态  
代码类型：完整项目代码

```python
from collections.abc import Generator  # 导入同步生成器类型以标注yield依赖
from typing import Annotated  # 导入可附加FastAPI元数据的类型工具

from fastapi import Depends, Query  # 导入依赖声明和查询参数工具
from pydantic import BaseModel  # 导入分页参数模型基类
from sqlalchemy.orm import Session  # 导入数据库Session类型

from app.database import SessionLocal  # 导入项目Session工厂


class PaginationParams(BaseModel):  # 定义分页依赖的返回结构
    page: int  # 保存从1开始的页码
    size: int  # 保存每页数量


def get_pagination(  # 读取并校验公共分页参数
    page: Annotated[int, Query(ge=1)] = 1,  # 页码至少为1且默认1
    size: Annotated[int, Query(ge=1, le=100)] = 20,  # 每页1到100条且默认20
) -> PaginationParams:  # 返回统一分页对象
    return PaginationParams(page=page, size=size)  # 组合已校验的分页值


PaginationDep = Annotated[  # 定义可在Router中复用的分页依赖类型
    PaginationParams,  # 路径函数最终取得的值类型
    Depends(get_pagination),  # 指定由get_pagination创建该值
]  # 完成分页依赖别名


def get_db() -> Generator[Session, None, None]:  # 定义请求级Session依赖
    db = SessionLocal()  # 为当前请求创建Session
    try:  # 开始保证资源释放的结构
        yield db  # 把Session注入路径函数
    finally:  # 无论请求成功或失败都执行清理
        db.close()  # 关闭Session并释放数据库资源


SessionDep = Annotated[Session, Depends(get_db)]  # 定义请求级Session依赖类型
```

每次请求的执行过程是：

```text
请求到达
→ FastAPI 调用 get_db
→ 创建本次请求使用的 Session
→ yield 把 Session 交给路由函数
→ 路由和响应处理结束
→ finally 关闭 Session
```

`SessionDep`保存`Session`类型和`Depends(get_db)`依赖来源。FastAPI会调用`get_db()`，并把`yield`产生的Session注入路由参数。

每次请求都会创建自己的 Session。`close()` 负责释放当前请求使用的数据库资源，不代表提交事务；写操作仍由 Service 根据业务用例执行 `commit()` 或 `rollback()`。

首次出现的类型参数：

| 写法 | 参数 | 可接受的值 | 当前值 | 作用 |
| --- | --- | --- | --- | --- |
| `Generator[Y, S, R]` | `Y` | `yield`产生的类型 | `Session` | 声明依赖向路由提供Session |
| `Generator[Y, S, R]` | `S` | 传回生成器的值类型 | `None` | 当前生成器不接收send值 |
| `Generator[Y, S, R]` | `R` | 生成器结束时的返回类型 | `None` | 当前生成器不返回额外结果 |
| `Depends()` | `dependency` | 可调用对象，或`None` | `get_db` | 声明需要由FastAPI调用的依赖 |

`Depends()`的完整参数已经在第5章说明；本章关注`yield`产生的Session在请求结束时如何关闭。

## 四、迁移响应 Schema

文件：`app/schemas.py`  
操作：新建文件并迁移第3章响应模型  
代码类型：完整项目代码

```python
from datetime import date  # 导入Schema中的日期类型

from pydantic import BaseModel, ConfigDict  # 导入模型基类和模型配置工具


class DepartmentSummary(BaseModel):  # 定义嵌套在员工响应中的部门摘要
    model_config = ConfigDict(from_attributes=True)  # 允许从部门ORM对象读取属性

    id: int  # 返回部门主键
    name: str  # 返回部门名称


class EmployeeResponse(BaseModel):  # 定义单名员工响应结构
    model_config = ConfigDict(from_attributes=True)  # 允许从员工ORM对象读取属性

    id: int  # 返回数据库主键
    employee_number: str  # 返回业务员工编号
    name: str  # 返回员工姓名
    department_id: int  # 返回所属部门主键
    email: str  # 返回员工邮箱
    joined_on: date  # 返回入职日期
    is_active: bool  # 返回在职状态
    department: DepartmentSummary  # 返回所属部门摘要


class EmployeeListResponse(BaseModel):  # 定义分页列表响应结构
    items: list[EmployeeResponse]  # 返回当前页员工
    total: int  # 返回筛选后的总件数
    page: int  # 返回当前页码
    size: int  # 返回当前每页数量
```

与第3章相比，数据库字段和响应字段没有改变：

- `department_id` 继续表示数据库外键值。
- `department` 继续提供前端显示所需的部门摘要。
- 列表继续返回 `items`、`total`、`page`、`size`。

新增的 `from_attributes=True` 允许 Pydantic 从 SQLAlchemy ORM 对象属性读取字段。Repository 已通过 `selectinload(Employee.department)` 提前读取部门关系，避免序列化列表时为每名员工重复查询部门。

`ConfigDict()` 当前使用参数说明：

| 参数 | 可接受的值 | 默认值或必填性 | 作用 |
| --- | --- | --- | --- |
| `from_attributes` | `True` 或 `False` | 默认 `False` | 为 `True` 时允许模型从 ORM 对象等 Python 对象的属性读取字段 |

当前响应来源是 SQLAlchemy ORM 对象，因此两个响应模型都设置为 `True`。请求模型接收字典或 JSON，不需要重复设置该参数。

本章只迁移查询接口所需的响应模型。第14章恢复写接口时，再把`EmployeeCreate`和`EmployeeUpdate`移入这个文件。

## 五、替换员工 Router 并迁移部门 Router

第4章已经创建 `app/routers/__init__.py` 和 `app/routers/employees.py`。现在用下面的数据库查询版本整体替换 `employees.py`：

文件：`app/routers/employees.py`  
操作：整体替换原有内存实现  
代码类型：完整查询Router代码

```python
from fastapi import APIRouter  # 导入员工业务子路由类

from app.dependencies import PaginationDep, SessionDep  # 导入分页和Session依赖类型
from app.schemas import EmployeeListResponse, EmployeeResponse  # 导入列表和详情响应模型
from app.services.employee_service import EmployeeService  # 导入员工业务服务


router = APIRouter(prefix="/api/employees", tags=["employees"])  # 创建员工子路由


@router.get("", response_model=EmployeeListResponse)  # 注册员工列表接口
def list_employees(  # 定义员工列表路径函数
    pagination: PaginationDep,  # 注入已校验的分页对象
    db: SessionDep,  # 注入当前请求使用的Session
    keyword: str | None = None,  # 接收可选查询关键字
):  # 返回值由EmployeeListResponse校验
    items, total = EmployeeService(db).list_employees(  # 调用Service查询列表和总件数
        keyword,  # 传递关键字
        pagination.page,  # 传递当前页码
        pagination.size,  # 传递每页数量
    )  # 完成Service调用
    return EmployeeListResponse(  # 创建明确的分页响应对象
        items=items,  # 设置当前页员工
        total=total,  # 设置筛选后的总件数
        page=pagination.page,  # 设置当前页码
        size=pagination.size,  # 设置每页数量
    )  # 返回列表响应


@router.get("/{employee_number}", response_model=EmployeeResponse)  # 注册员工详情接口
def get_employee(  # 定义员工详情路径函数
    employee_number: str,  # 读取路径中的员工编号
    db: SessionDep,  # 注入当前请求使用的Session
):  # 返回值由EmployeeResponse转换和过滤
    service = EmployeeService(db)  # 创建使用当前Session的Service
    return service.get_employee(employee_number)  # 查询并返回在职员工
```

调用链为：

```text
GET /api/employees
→ list_employees()
→ EmployeeService.list_employees()
→ EmployeeRepository.find_active() 和 count_active()
→ SQLAlchemy Session
→ employee_management_fastapi 数据库
```

详情查询复用同一条分层链路：

```text
GET /api/employees/E001
→ EmployeeService.get_employee()
→ EmployeeRepository.find_by_number()
→ EmployeeResponse
```

Router 负责 HTTP 输入输出，Service 负责业务用例，Repository 负责 SQLAlchemy 查询。Router 不直接编写数据库查询。

当前Router首次出现的参数：

| 调用 | 参数 | 可接受的值 | 当前值或默认值 | 作用 |
| --- | --- | --- | --- | --- |
| `APIRouter()` | `prefix` | 路径前缀字符串 | 当前`"/api/employees"`；默认空字符串 | 为Router中的路径增加公共前缀 |
| `APIRouter()` | `tags` | 字符串或枚举组成的列表，或`None` | 当前`["employees"]`；默认`None` | 在OpenAPI文档中对接口分组 |
| `PaginationDep` | 类型别名 | `PaginationParams`与`Depends(get_pagination)` | 第5章定义 | 校验并注入分页参数 |
| `SessionDep` | 类型别名 | `Session`与`Depends(get_db)` | 本章定义 | 创建并注入请求级Session |
| 路径参数 | `employee_number` | 非空字符串 | 必填 | 指定要查询的员工编号 |

`page`至少为1，`size`范围为1～100。无效值由第5章的`get_pagination()`拒绝，不会进入Service或数据库查询。

文件：`app/routers/departments.py`  
操作：新建文件，替换第4章内存部门列表  
代码类型：完整查询Router代码

```python
from fastapi import APIRouter  # 导入部门业务子路由类

from app.dependencies import SessionDep  # 导入请求级Session依赖类型
from app.schemas import DepartmentSummary  # 导入部门响应模型
from app.services.employee_service import EmployeeService  # 导入员工业务服务


router = APIRouter(prefix="/api/departments", tags=["departments"])  # 创建部门子路由


@router.get("", response_model=list[DepartmentSummary])  # 注册部门列表接口
def list_departments(db: SessionDep):  # 注入Session并定义路径函数
    return EmployeeService(db).list_departments()  # 通过Service返回数据库部门
```

最终路径仍然是 `GET /api/departments`。Router 只接收请求并调用 Service，不直接编写 `select()`。

## 六、更新应用入口并把员工不存在转换为404

Service在找不到在职员工时抛出`EmployeeNotFoundError`。业务异常本身不包含HTTP状态码，因此在应用边界增加一个最小异常处理器。

文件：`app/main.py`  
操作：替换为下面的完整状态  
代码类型：完整项目代码

```python
from fastapi import FastAPI, Request  # 导入应用类和请求对象类型
from fastapi.responses import JSONResponse  # 导入JSON响应类

from app.routers.departments import router as departments_router  # 导入部门Router
from app.routers.employees import router as employees_router  # 导入员工Router
from app.services.employee_service import EmployeeNotFoundError  # 导入员工不存在业务异常


app = FastAPI()  # 创建项目中唯一的FastAPI应用对象
app.include_router(departments_router)  # 注册部门查询接口
app.include_router(employees_router)  # 注册员工查询接口


@app.exception_handler(EmployeeNotFoundError)  # 注册员工不存在异常处理器
def handle_employee_not_found(  # 把业务异常转换为HTTP响应
    _request: Request,  # 接收当前请求，本处理器暂时不读取其内容
    _exc: EmployeeNotFoundError,  # 接收已经抛出的业务异常
) -> JSONResponse:  # 返回明确的JSON响应对象
    return JSONResponse(  # 创建404 JSON响应
        status_code=404,  # 设置资源不存在状态码
        content={"detail": "employee not found"},  # 返回稳定且不泄露内部信息的消息
    )  # 完成异常响应


@app.get("/health")  # 注册健康检查接口
def health_check():  # 定义健康检查路径函数
    return {"status": "ok"}  # 返回固定健康状态
```

`@app.exception_handler()`把一种异常类型注册到统一处理函数。当Router或Service抛出该异常时，FastAPI不再生成服务器错误，而是使用处理函数返回的响应。

| 调用 | 参数 | 可接受的值 | 默认值或必填性 | 作用 |
| --- | --- | --- | --- | --- |
| `app.exception_handler()` | `exc_class_or_status_code` | 异常类或HTTP状态码整数 | 必填 | 指定当前处理器负责的异常 |
| `JSONResponse()` | `content` | 可编码为JSON的数据 | 必填 | 设置响应体内容 |
| `JSONResponse()` | `status_code` | 合法HTTP状态码整数 | 默认`200` | 设置响应状态码 |

参数名以`_`开头表示函数签名需要接收该值，但当前处理逻辑没有读取它。第17章会在相同机制上增加统一错误结构、请求编号和日志；本章只先保证查询接口能够正确返回`404`。

第4章已经完成应用入口整理。本章仍然只保留一个`app = FastAPI()`。`include_router()`把两个数据库Router注册到应用；打开`/docs`后，应该看到`/health`、员工列表、员工详情和部门列表，不应再保留第4章的内存实现。

## 七、检查数据库状态并启动

在项目根目录操作。首先确认当前 PowerShell 会话仍然设置了 `DATABASE_URL`：

```powershell
$env:DATABASE_URL
```

如果没有输出数据库 URL，请重新设置本地 FastAPI 数据库连接。不要把真实密码写入仓库。

然后确认迁移、准备样例数据并启动：

```powershell
alembic current
python -m app.seed
uvicorn app.main:app --reload
```

`alembic current` 应显示当前迁移版本，种子脚本第二次执行不能重复插入样例员工。

访问：

```text
GET http://127.0.0.1:8000/api/employees?page=1&size=20
GET http://127.0.0.1:8000/api/employees/E001
GET http://127.0.0.1:8000/api/departments
```

预期响应结构：

```json
{
  "items": [
    {
      "id": 1,
      "employee_number": "E001",
      "name": "山田太郎",
      "department_id": 1,
      "email": "yamada@example.com",
      "joined_on": "2026-04-01",
      "is_active": true,
      "department": {
        "id": 1,
        "name": "开发部"
      }
    }
  ],
  "total": 2,
  "page": 1,
  "size": 20
}
```

实际 `items` 还应包含 `E002 佐藤花子`。数据库主键由实际数据库生成，不要只依赖示例中的固定主键判断结果。

部门接口应返回：

```json
[
  {
    "id": 1,
    "name": "开发部"
  },
  {
    "id": 2,
    "name": "营业部"
  }
]
```

主键以实际数据库为准，验证重点是两个部门都来自数据库，而不是第4章的内存列表。

再访问不存在的员工编号，确认返回状态码`404`和`{"detail": "employee not found"}`。

## 八、数据库请求链

现在可以把一次员工列表请求完整读成：

```text
Router解析查询参数
→ Depends(get_db)创建Session
→ Service处理分页与业务规则
→ Repository执行select()并加载部门
→ Pydantic响应模型转换ORM对象
→ get_db的finally关闭Session
```

每一层只承担自己的职责。Session属于当前请求，模型由Alembic对应的数据库结构保存，接口输出由Pydantic响应模型约束。

## 九、失败排查

| 现象 | 检查位置 |
| --- | --- |
| 启动时提示缺少 `DATABASE_URL` | 当前 PowerShell 会话是否重新设置环境变量 |
| Router 不出现在 `/docs` | `main.py` 是否调用 `include_router()` |
| 同一路径出现两套实现 | 是否删除第4章 Router 中的内存路由 |
| 返回 500 且提示表不存在 | 是否执行迁移，`alembic current` 是否正确 |
| 返回空列表 | 是否执行 `python -m app.seed`，员工是否为在职 |
| 部门接口返回 404 | 是否创建并注册 `departments_router` |
| 响应模型校验失败 | `from_attributes=True`、`department_id` 和 ORM 字段是否一致 |
| 访问部门时重复查询 | Repository 是否配置 `selectinload(Employee.department)` |
| 请求结束后连接未释放 | `get_db()` 是否在 `finally` 中关闭 Session |
| 不存在员工返回500 | `main.py`是否注册`EmployeeNotFoundError`处理器 |

## 十、动手任务

1. 启动 API，确认 `total=2`，`items` 包含 `E001`、`E002`。
2. 使用 `keyword=山田` 查询，确认只返回 `E001`。
3. 使用 `keyword=E002` 查询，确认员工编号也可以作为搜索条件。
4. 使用不存在的关键字，确认返回 `items=[]`、`total=0`，而不是 500。
5. 暂时移除 `EmployeeResponse` 的 `from_attributes=True`，观察响应模型错误后立即恢复。
6. 连续请求两次列表，确认结果稳定且数据库没有新增记录。
7. 请求 `/api/departments`，确认开发部和营业部来自数据库且路径与第4章一致。
8. 在终端记录一次请求中`get_db()`创建和关闭Session的顺序，确认连续请求不会复用同一个Session对象。
9. 查询`E001`详情，再查询不存在编号，分别确认`200`和`404`。

## 十一、完成检查

- [ ] 第4章 Router 中的内存列表和内存路由已经删除
- [ ] FastAPI 复用已有 SQLAlchemy Model、Repository 和 Service
- [ ] 路由前缀固定为 `/api/employees`
- [ ] `/api/departments` 已改为数据库查询，接口路径没有消失或改变
- [ ] 员工列表、详情和部门列表三个只读接口均来自数据库
- [ ] 列表响应继续包含 `items`、`total`、`page`、`size`
- [ ] 员工响应继续包含 `department_id` 和部门摘要
- [ ] 姓名和员工编号都可以用于关键字查询
- [ ] 每次请求获得独立 Session，请求结束后 Session 会关闭
- [ ] Alembic 是唯一表结构管理入口
- [ ] 不存在的员工由HTTP边界转换为404

完成后的稳定状态：

```text
app/
├── main.py
├── dependencies.py
├── schemas.py
├── models.py
├── routers/
│   ├── __init__.py
│   ├── departments.py
│   └── employees.py
├── repositories/
└── services/
```

请求级 Session、员工列表和部门列表接口构成稳定调用链，写接口也复用同一数据库依赖和分层边界。
