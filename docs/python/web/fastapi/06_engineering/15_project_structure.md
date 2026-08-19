# 第15章 FastAPI项目结构

> 本章成果：检查第10～14章形成的员工数据库API，确认Model、Schema、Repository、Service、Router、Session依赖和异常处理器已经组合为一套稳定项目结构，并能沿请求链定位每层责任。

## 一、本章开始状态

已有代码已经完成：

- Pydantic 请求与响应 Schema
- SQLAlchemy Department/Employee 模型
- Alembic 初始迁移
- Repository 与 Service
- 员工 CRUD Router
- 部门列表 Router
- 请求级数据库 Session 依赖

前面章节已经逐步创建目标目录，本章把它作为工程检查点，不要求再次机械移动相同文件。先运行现有接口并记录结果，再对照本章目录检查遗漏、重复文件和错误导入；只有实际位置不一致时才移动文件，并同步修改导入路径。

## 二、最终目录

```text
employee_api/
├── app/
│   ├── __init__.py
│   ├── main.py
│   ├── config.py
│   ├── database.py
│   ├── dependencies.py
│   ├── models.py
│   ├── schemas.py
│   ├── repositories/
│   │   ├── __init__.py
│   │   └── employee_repository.py
│   ├── services/
│   │   ├── __init__.py
│   │   └── employee_service.py
│   └── routers/
│       ├── __init__.py
│       ├── departments.py
│       └── employees.py
├── alembic/
├── tests/
├── alembic.ini
└── requirements.txt
```

## 三、各层责任

| 层 | 输入 | 输出或状态变化 |
| --- | --- | --- |
| Router | HTTP 参数、请求体、依赖 | 状态码和响应模型 |
| Schema | Python/JSON 数据 | 校验后的输入或受控输出 |
| Service | 业务参数和 Session | 业务结果、事务提交或业务异常 |
| Repository | 查询条件和 ORM 对象 | ORM 对象、数据库写入准备 |
| Model | Python 属性 | 表映射和关系 |
| Dependency | 请求上下文 | Session、当前用户等资源 |
| Config | 环境变量 | 已校验配置 |

## 四、完整请求链

新增员工时：

```text
POST /api/employees
→ EmployeeCreate 校验和规范化
→ employees Router
→ EmployeeService 重复检查与事务
→ EmployeeRepository 查询和 add/flush
→ SQLAlchemy Model
→ 数据库
→ EmployeeResponse 过滤和序列化
→ 201 Created
```

出现失败时，按照下面的顺序反向定位：

- `422`：先检查请求 Schema。
- `404`、`409`：检查 Service 业务分支和 Router 转换。
- 数据库约束异常：检查 Model、迁移、数据和事务日志。
- 响应校验异常：检查 ORM 对象、关系加载和响应 Schema。

## 五、应用入口

`app/main.py`只负责应用级组合，但第13～14章已经建立的业务异常处理器也属于应用级组合，必须继续保留。

文件：`app/main.py`  
操作：核对为下面的完整状态，不要用简化入口覆盖异常处理器  
代码类型：完整项目代码

```python
from fastapi import FastAPI, Request  # 导入应用类和异常处理器使用的请求类型
from fastapi.responses import JSONResponse  # 导入JSON错误响应类
from sqlalchemy.exc import IntegrityError  # 导入数据库完整性约束异常

from app.routers.departments import router as departments_router  # 导入部门Router
from app.routers.employees import router as employees_router  # 导入员工Router
from app.services.employee_service import (  # 导入业务异常
    DepartmentNotFoundError,  # 部门不存在异常
    EmployeeAlreadyExistsError,  # 员工编号冲突异常
    EmployeeNotFoundError,  # 员工不存在异常
)  # 完成业务异常导入


app = FastAPI()  # 创建项目中唯一的应用对象
app.include_router(departments_router)  # 注册部门接口
app.include_router(employees_router)  # 注册员工接口


@app.exception_handler(EmployeeNotFoundError)  # 注册员工不存在处理器
def handle_employee_not_found(  # 把业务异常转换为404
    _request: Request,  # 接收当前请求但暂不读取
    _exc: EmployeeNotFoundError,  # 接收员工不存在异常
) -> JSONResponse:  # 返回JSON错误响应
    return JSONResponse(  # 创建404响应
        status_code=404,  # 设置资源不存在状态码
        content={"detail": "employee not found"},  # 返回稳定错误消息
    )  # 完成响应


@app.exception_handler(EmployeeAlreadyExistsError)  # 注册编号冲突处理器
def handle_employee_already_exists(  # 把业务冲突转换为409
    _request: Request,  # 接收当前请求但暂不读取
    _exc: EmployeeAlreadyExistsError,  # 接收员工编号冲突异常
) -> JSONResponse:  # 返回JSON错误响应
    return JSONResponse(  # 创建409响应
        status_code=409,  # 设置Conflict状态码
        content={"detail": "employee number already exists"},  # 返回稳定错误消息
    )  # 完成响应


@app.exception_handler(DepartmentNotFoundError)  # 注册部门不存在处理器
def handle_department_not_found(  # 把无效部门转换为400
    _request: Request,  # 接收当前请求但暂不读取
    _exc: DepartmentNotFoundError,  # 接收部门不存在异常
) -> JSONResponse:  # 返回JSON错误响应
    return JSONResponse(  # 创建400响应
        status_code=400,  # 设置Bad Request状态码
        content={"detail": "department not found"},  # 返回稳定错误消息
    )  # 完成响应


@app.exception_handler(IntegrityError)  # 注册数据库约束异常处理器
def handle_integrity_error(  # 把数据库冲突转换为409
    _request: Request,  # 接收当前请求但暂不读取
    _exc: IntegrityError,  # 接收数据库完整性异常
) -> JSONResponse:  # 返回JSON错误响应
    return JSONResponse(  # 创建409响应
        status_code=409,  # 设置Conflict状态码
        content={"detail": "database constraint conflict"},  # 隐藏SQL和约束名称
    )  # 完成响应


@app.get("/health")  # 注册健康检查接口
def health_check():  # 定义健康检查路径函数
    return {"status": "ok"}  # 返回固定健康状态
```

不要在入口文件中堆放员工SQL、事务、密码校验或文件处理。第17章会在当前异常处理器上增加统一错误结构、日志和`request_id`，不是重新恢复已经丢失的处理器。

## 六、项目对象与目录对应关系

| 项目对象 | 主要目录或文件 |
| --- | --- |
| SQLAlchemy模型 | `app/models.py` |
| 数据库迁移 | `alembic/versions/` |
| Pydantic输入输出模型 | `app/schemas.py` |
| HTTP路由 | `app/routers/` |
| 数据库查询 | `app/repositories/` |
| 业务规则和事务 | `app/services/` |
| Session与当前用户 | `app/dependencies.py` |

层数不需要为了“企业化”机械增加。当前 Repository/Service 用于独立测试事务和业务规则；简单只读接口可以保持更短调用链。

## 七、整理后的回归验证

移动或重命名文件后依次执行：

```powershell
alembic current
uvicorn app.main:app --reload
```

本章还没有建立自动化测试，因此先用`/docs`回归；第21章创建测试基础后再执行`pytest`：

- `/health`
- 部门列表
- 员工列表
- 员工详情
- 新增
- 修改
- 逻辑删除
- 重复编号和不存在编号

## 八、动手任务

1. 对照最终目录检查现有文件，只移动位置不一致的文件并同步修改导入。
2. 画出员工新增和逻辑删除的调用链。
3. 删除一个错误的重复 Router 注册，确认 `/docs` 只显示一组员工接口。
4. 制造一次错误导入路径，根据堆栈定位并恢复。
5. 选择一个员工新增请求，写出它经过Router、Service、Repository和Session的顺序。

## 九、完成检查

- [ ] 项目只有一套 Model、Schema、Repository 和 Service。
- [ ] 员工和部门路径分别保持 `/api/employees`、`/api/departments`。
- [ ] 整理前后数据库和接口行为不变。
- [ ] 每层责任能从实际请求链解释。
- [ ] 项目可以继续加入配置、异常、认证和测试。
- [ ] 第13～14章的四个异常处理器仍然存在，`404`、`400`和`409`回归结果不变。

完成后保留唯一应用入口和稳定目录，配置及生命周期代码直接接入这些位置。
