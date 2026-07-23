# 第7章 FastAPI 项目结构

> 本章目标：掌握 FastAPI 企业项目常见分层方式，理解 router、schema、model、repository、service、dependency 的职责边界。

## 一、为什么需要项目分层

接口示例可以写在一个文件中，但企业项目不能长期这样写。

如果所有代码都放在路由函数里，会出现：

- 文件越来越长
- 数据库操作重复
- 业务规则分散
- 测试困难
- Review 难以定位责任

## 二、推荐目录结构

```text
app/
├── main.py
├── database.py
├── dependencies.py
├── models.py
├── schemas/
│   └── employee.py
├── repositories/
│   └── employee_repository.py
├── services/
│   └── employee_service.py
└── routers/
    └── employees.py
```

职责说明：

| 层 | 作用 |
| --- | --- |
| `router` | 接收 HTTP 请求，返回 HTTP 响应 |
| `schema` | 定义请求和响应数据结构 |
| `model` | 定义数据库表映射 |
| `repository` | 封装数据库访问 |
| `service` | 编写业务规则 |
| `dependencies` | 提供公共依赖 |
| `database` | 数据库连接和 Session |

## 三、Schema 层

文件位置：

```text
app/schemas/employee.py
```

```python
from pydantic import BaseModel, Field  # 导入 Pydantic 模型和字段约束


class EmployeeCreate(BaseModel):  # 新增员工请求模型
    employee_code: str = Field(min_length=1, max_length=20)  # 员工编号
    name: str = Field(min_length=1, max_length=100)  # 员工姓名
    email: str | None = None  # 邮箱
    department_id: int  # 部门 ID


class EmployeeResponse(BaseModel):  # 员工响应模型
    id: int  # 员工 ID
    employee_code: str  # 员工编号
    name: str  # 员工姓名
    email: str | None  # 邮箱
    department_id: int  # 部门 ID
    is_active: bool  # 是否在职
```

## 四、Repository 层

文件位置：

```text
app/repositories/employee_repository.py
```

```python
from sqlalchemy import select  # 导入 select
from sqlalchemy.orm import Session  # 导入 Session 类型
from app.models import Employee  # 导入员工模型


def find_employee_by_code(db: Session, employee_code: str):  # 根据员工编号查询员工
    statement = select(Employee).where(Employee.employee_code == employee_code)  # 构建查询语句
    return db.execute(statement).scalars().first()  # 返回第一条员工对象或 None


def save_employee(db: Session, employee: Employee):  # 保存员工
    db.add(employee)  # 加入 Session
    db.flush()  # 发送到数据库但不提交事务
    return employee  # 返回员工对象
```

Repository 只负责数据库访问，不负责决定业务规则。

## 五、Service 层

文件位置：

```text
app/services/employee_service.py
```

```python
from fastapi import HTTPException  # 导入 HTTPException，用于抛出接口异常
from sqlalchemy.orm import Session  # 导入 Session 类型
from app.models import Employee  # 导入员工模型
from app.repositories.employee_repository import find_employee_by_code, save_employee  # 导入 Repository 函数
from app.schemas.employee import EmployeeCreate  # 导入新增员工请求模型


def create_employee_service(db: Session, request: EmployeeCreate):  # 新增员工业务函数
    existing_employee = find_employee_by_code(db, request.employee_code)  # 查询员工编号是否已经存在
    if existing_employee:  # 如果员工已经存在
        raise HTTPException(status_code=400, detail="员工编号已经存在")  # 返回业务错误

    employee = Employee(  # 创建员工 ORM 对象
        employee_code=request.employee_code,  # 设置员工编号
        name=request.name,  # 设置员工姓名
        email=request.email,  # 设置邮箱
        department_id=request.department_id,  # 设置部门 ID
    )
    saved_employee = save_employee(db, employee)  # 调用 Repository 保存员工
    db.commit()  # 提交事务
    db.refresh(saved_employee)  # 刷新对象
    return saved_employee  # 返回保存后的员工
```

Service 负责业务规则，例如重复检查、权限判断、状态变更等。

## 六、Router 层

文件位置：

```text
app/routers/employees.py
```

```python
from fastapi import APIRouter, Depends  # 导入路由和依赖
from sqlalchemy.orm import Session  # 导入 Session 类型
from app.dependencies import get_db  # 导入数据库依赖
from app.schemas.employee import EmployeeCreate, EmployeeResponse  # 导入请求和响应模型
from app.services.employee_service import create_employee_service  # 导入业务函数

router = APIRouter(prefix="/employees", tags=["employees"])  # 创建员工路由


@router.post("", response_model=EmployeeResponse, status_code=201)  # 新增员工接口
def create_employee(request: EmployeeCreate, db: Session = Depends(get_db)):  # 接收请求体和数据库 Session
    return create_employee_service(db, request)  # 调用 Service 完成业务处理
```

Router 不直接写复杂业务，也不直接堆大量数据库操作。

## 七、日本项目中的分层表达

| 日语表达 | 中文说明 |
| --- | --- |
| ルーティング | 路由 |
| 入力チェック | 输入校验 |
| 業務ロジック | 业务逻辑 |
| DB アクセス | 数据库访问 |
| 共通処理 | 公共处理 |

## 八、基础练习

请把新增员工接口拆成：

1. `schemas/employee.py`
2. `repositories/employee_repository.py`
3. `services/employee_service.py`
4. `routers/employees.py`

## 九、本章总结

- Router 负责 HTTP
- Schema 负责请求和响应结构
- Model 负责数据库表映射
- Repository 负责数据库访问
- Service 负责业务规则
- 分层可以提高可读性、可测试性和可维护性
