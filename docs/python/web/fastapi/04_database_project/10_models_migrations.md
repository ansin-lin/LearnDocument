# 第10章 SQLAlchemy员工模型与Alembic迁移

> 本章成果：使用 SQLAlchemy 2.x 和 Alembic 建立员工管理项目的`departments`、`employees`两张表，并验证表、约束和迁移版本。

## 一、本章开始状态

开始前应已完成：

- [第7章：SQLAlchemy基础概念与常用对象](../03_sqlalchemy/07_core_concepts.md)
- [第8章：SQLAlchemy ORM CRUD与事务](../03_sqlalchemy/08_orm_crud_transactions.md)
- [第9章：Alembic迁移基础](../03_sqlalchemy/09_alembic_migrations.md)
- [员工管理API项目规格](../project_spec.md)

本章使用项目专用的空数据库。不要连接已有业务库或生产数据库；Alembic只管理当前项目的迁移历史。

## 二、统一业务模型

| 模型 | 字段 | 业务约束 |
| --- | --- | --- |
| `Department` | `id`、`name` | 部门名唯一 |
| `Employee` | `id`、`employee_number`、`name`、`department_id`、`email`、`joined_on`、`is_active` | 员工编号唯一，部门必须存在 |

这组字段来自项目规格。后续虽然会加入Pydantic Schema、Repository和Router，但字段含义与数据库约束保持不变。

## 三、项目结构

保留第6章结束时的FastAPI应用、员工Router和分页依赖，再增加数据库配置、模型与Alembic目录：

```text
employee_api/
├── app/
│   ├── __init__.py
│   ├── main.py
│   ├── dependencies.py
│   ├── config.py
│   ├── database.py
│   ├── models.py
│   └── routers/
│       ├── __init__.py
│       └── employees.py
├── alembic/
├── alembic.ini
└── requirements.txt
```

代码片段均以项目根目录为工作目录。

## 四、安装和记录依赖

```powershell
python -m pip install sqlalchemy pymysql alembic
```

在 `requirements.txt` 中记录团队确认的版本范围。下面使用 SQLAlchemy 2.x 写法，不混用旧式 `Query` API。

## 五、配置独立数据库

先在本地 MySQL 创建专供 FastAPI 项目使用的空数据库：

```sql
CREATE DATABASE employee_management_fastapi
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
```

这条命令会创建数据库。不要在生产服务器或不明环境中直接执行。

创建 `app/config.py`：

```python
import os  # 导入操作系统环境变量工具


DATABASE_URL = os.environ["DATABASE_URL"]  # 设置或保存DATABASE_URL的值
```

在当前 PowerShell 会话设置本地开发账号，尖括号内容必须替换：

```powershell
$env:DATABASE_URL = "mysql+pymysql://<course_user>:<local_password>@127.0.0.1:3306/employee_management_fastapi?charset=utf8mb4"
```

该账号只授予FastAPI数据库需要的权限。真实账号和密码不能提交到仓库；关闭终端后，新终端需要重新设置环境变量。

## 六、创建 Engine、Session 工厂和 Base

创建 `app/database.py`：

```python
from sqlalchemy import create_engine  # 导入数据库引擎创建函数
from sqlalchemy.orm import DeclarativeBase, sessionmaker  # 从sqlalchemy.orm模块导入DeclarativeBase, sessionmaker

from app.config import DATABASE_URL  # 从app.config模块导入DATABASE_URL


engine = create_engine(  # 设置或保存engine的值
    DATABASE_URL,  # 传入DATABASE_URL参数
    pool_pre_ping=True,  # 设置或保存pool_pre_ping的值
)  # 完成当前调用或数据结构

SessionLocal = sessionmaker(  # 设置或保存SessionLocal的值
    bind=engine,  # 设置或保存bind的值
    autoflush=False,  # 设置或保存autoflush的值
    expire_on_commit=False,  # 设置或保存expire_on_commit的值
)  # 完成当前调用或数据结构


class Base(DeclarativeBase):  # 定义Base类
    pass  # 当前类不需要额外实现
```

| 对象 | 本项目中的职责 |
| --- | --- |
| `engine` | 管理连接池和数据库方言 |
| `SessionLocal` | 创建每次操作使用的 `Session` |
| `Base` | 汇总 ORM 模型的映射信息 |

`SessionLocal` 是工厂，不是已经打开的数据库连接。

首次出现参数：

| 调用 | 参数 | 可接受的值 | 当前值或默认值 | 作用 |
| --- | --- | --- | --- | --- |
| `create_engine()` | `url` | SQLAlchemy URL字符串或`URL`对象 | 当前`DATABASE_URL`；必填 | 指定数据库方言、驱动和连接地址 |
| `create_engine()` | `pool_pre_ping` | `True`或`False` | 当前`True`；默认`False` | 取出连接前检查连接是否仍可用 |
| `sessionmaker()` | `bind` | `Engine`或其他可绑定对象 | 当前`engine`；默认`None` | 指定Session使用的数据库引擎 |
| `sessionmaker()` | `autoflush` | `True`或`False` | 当前`False`；默认`True` | 查询前是否自动把待处理变更flush到数据库 |
| `sessionmaker()` | `expire_on_commit` | `True`或`False` | 当前`False`；默认`True` | 提交后是否让ORM对象属性过期并在访问时重新读取 |

`create_engine()`和`sessionmaker()`还支持连接池、事务和执行选项；未传入的选项保持SQLAlchemy默认值。

## 七、定义部门和员工模型

创建 `app/models.py`：

```python
from datetime import date  # 导入员工入职日期类型

from sqlalchemy import Boolean, Date, ForeignKey, Integer, String  # 从sqlalchemy模块导入Boolean, Date, ForeignKey, Integer, String
from sqlalchemy.orm import Mapped, mapped_column, relationship  # 从sqlalchemy.orm模块导入Mapped, mapped_column, relationship

from app.database import Base  # 从app.database模块导入Base


class Department(Base):  # 定义Department类
    __tablename__ = "departments"  # 设置或保存__tablename__的值

    id: Mapped[int] = mapped_column(Integer, primary_key=True)  # 接收id参数并声明类型
    name: Mapped[str] = mapped_column(  # 接收name参数并声明类型
        String(100),  # 调用String()
        unique=True,  # 设置或保存unique的值
        nullable=False,  # 设置或保存nullable的值
    )  # 完成当前调用或数据结构
    employees: Mapped[list["Employee"]] = relationship(  # 接收employees参数并声明类型
        back_populates="department",  # 设置或保存back_populates的值
    )  # 完成当前调用或数据结构


class Employee(Base):  # 定义Employee类
    __tablename__ = "employees"  # 设置或保存__tablename__的值

    id: Mapped[int] = mapped_column(Integer, primary_key=True)  # 接收id参数并声明类型
    employee_number: Mapped[str] = mapped_column(  # 接收employee_number参数并声明类型
        String(20),  # 调用String()
        unique=True,  # 设置或保存unique的值
        nullable=False,  # 设置或保存nullable的值
    )  # 完成当前调用或数据结构
    name: Mapped[str] = mapped_column(String(100), nullable=False)  # 接收name参数并声明类型
    department_id: Mapped[int] = mapped_column(  # 接收department_id参数并声明类型
        ForeignKey("departments.id", ondelete="RESTRICT"),  # 调用ForeignKey()
        nullable=False,  # 设置或保存nullable的值
        index=True,  # 设置或保存index的值
    )  # 完成当前调用或数据结构
    email: Mapped[str] = mapped_column(  # 接收email参数并声明类型
        String(254),  # 调用String()
        nullable=False,  # 设置或保存nullable的值
        default="",  # 设置或保存default的值
    )  # 完成当前调用或数据结构
    joined_on: Mapped[date] = mapped_column(Date, nullable=False)  # 接收joined_on参数并声明类型
    is_active: Mapped[bool] = mapped_column(  # 接收is_active参数并声明类型
        Boolean,  # 传入Boolean参数
        nullable=False,  # 设置或保存nullable的值
        default=True,  # 设置或保存default的值
    )  # 完成当前调用或数据结构
    department: Mapped[Department] = relationship(  # 接收department参数并声明类型
        back_populates="employees",  # 设置或保存back_populates的值
    )  # 完成当前调用或数据结构
```

业务字段与SQLAlchemy映射的对应关系：

| 业务要求 | SQLAlchemy映射 |
| --- | --- |
| 员工编号最长20字符且唯一 | `mapped_column(String(20), unique=True)` |
| 员工必须属于已有部门 | 外键约束，并由Service检查部门业务状态 |
| 邮箱可以不填写 | 非空字符串列，未填写时保存`""` |
| 入职日期只保存日期 | `mapped_column(Date)` |
| 新员工默认为在职 | `mapped_column(Boolean, default=True)` |

数据库约束负责最终完整性；请求校验和 Service 业务判断不能替代唯一约束和外键约束。

模型映射中首次出现的参数：

| 调用 | 参数 | 可接受的值 | 默认值或必填性 | 作用 |
| --- | --- | --- | --- | --- |
| `mapped_column()` | 第一个位置参数 | `Integer`、`String(...)`、`Date`等SQLAlchemy类型 | 可省略并由类型标注推断 | 指定数据库列类型 |
| `mapped_column()` | `primary_key` | `True`或`False` | 默认`False` | 声明主键列 |
| `mapped_column()` | `unique` | `True`或`False` | 默认`False` | 声明单列唯一约束 |
| `mapped_column()` | `nullable` | `True`、`False`或`None` | 默认根据类型与主键推断 | 控制是否允许SQL `NULL` |
| `mapped_column()` | `default` | 固定值、Python可调用对象，或`None` | 默认无客户端默认值 | 插入时提供Python侧默认值 |
| `mapped_column()` | `index` | `True`或`False` | 默认`False` | 为该列创建索引 |
| `relationship()` | `back_populates` | 对端关系属性名字符串，或`None` | 默认`None` | 建立双向ORM关系同步 |

## 八、配置 Alembic

如果项目还没有迁移目录，在项目根目录执行：

```powershell
alembic init alembic
```

在 `alembic/env.py` 中导入项目模型并设置元数据：

```python
from app.config import DATABASE_URL  # 导入数据库连接地址
from app.database import Base  # 从app.database模块导入Base
from app import models  # 从app模块导入models

config.set_main_option("sqlalchemy.url", DATABASE_URL)  # 调用config.set_main_option()
target_metadata = Base.metadata  # 设置或保存target_metadata的值
```

导入 `models` 是为了让 `Base.metadata` 收集两个模型。自动生成后必须人工检查迁移脚本。

## 九、生成并执行初始迁移

```powershell
alembic revision --autogenerate -m "create departments and employees"
alembic upgrade head
alembic current
```

检查生成的迁移至少包含：

- 创建 `departments`
- 创建 `employees`
- `employee_number` 唯一约束
- `department_id` 外键
- `department_id` 等查询所需索引；唯一字段由唯一约束提供索引能力时不重复创建同列索引

不要同时调用 `Base.metadata.create_all()`。本项目从这里开始统一通过 Alembic 管理表结构。

## 十、准备共同样例数据

创建 `app/seed.py`：

```python
from datetime import date  # 导入初始员工入职日期类型

from sqlalchemy import select  # 从sqlalchemy模块导入select

from app.database import SessionLocal  # 从app.database模块导入SessionLocal
from app.models import Department, Employee  # 从app.models模块导入Department, Employee


def main() -> None:  # 定义main函数
    with SessionLocal.begin() as session:  # 在上下文中管理当前资源
        development = session.execute(  # 设置或保存development的值
            select(Department).where(Department.name == "开发部")  # 调用select()
        ).scalar_one_or_none()  # 取得开发部对象或None
        if development is None:  # 判断当前条件是否成立
            development = Department(name="开发部")  # 设置或保存development的值
            session.add(development)  # 调用session.add()

        sales = session.execute(  # 设置或保存sales的值
            select(Department).where(Department.name == "营业部")  # 调用select()
        ).scalar_one_or_none()  # 取得营业部对象或None
        if sales is None:  # 判断当前条件是否成立
            sales = Department(name="营业部")  # 设置或保存sales的值
            session.add(sales)  # 调用session.add()

        session.flush()  # 调用session.flush()

        sample_employees = [  # 设置或保存sample_employees的值
            {  # 定义第一名样例员工
                "employee_number": "E001",  # 组成当前文本内容
                "name": "山田太郎",  # 组成当前文本内容
                "department_id": development.id,  # 组成当前文本内容
                "email": "yamada@example.com",  # 组成当前文本内容
                "joined_on": date(2026, 4, 1),  # 组成当前文本内容
            },  # 完成当前调用或数据结构
            {  # 定义第二名样例员工
                "employee_number": "E002",  # 组成当前文本内容
                "name": "佐藤花子",  # 组成当前文本内容
                "department_id": sales.id,  # 组成当前文本内容
                "email": "",  # 组成当前文本内容
                "joined_on": date(2025, 10, 1),  # 组成当前文本内容
            },  # 完成当前调用或数据结构
        ]  # 完成当前调用或数据结构

        for values in sample_employees:  # 逐名检查并写入样例员工
            existing = session.execute(  # 设置或保存existing的值
                select(Employee).where(  # 调用select()
                    Employee.employee_number == values["employee_number"]  # 设置或保存Employee.employee_number的值
                )  # 完成当前调用或数据结构
            ).scalar_one_or_none()  # 取得相同编号的员工或None
            if existing is None:  # 判断当前条件是否成立
                session.add(Employee(**values))  # 调用session.add()

        print("sample data is ready")  # 调用print()


if __name__ == "__main__":  # 判断当前条件是否成立
    main()  # 调用main()
```

执行并验证：

```powershell
python -m app.seed
python -m app.seed
```

第一次执行会补齐缺少的部门和员工；第二次应再次输出 `sample data is ready`，并且每个部门和员工仍只有一条。即使数据库中只缺少 `E002`，脚本也会只补充缺少的数据，不会重复插入已经存在的记录。

`SessionLocal.begin()` 创建 Session 并开启事务。代码块正常结束时自动提交；发生异常时自动回滚并关闭 Session。`session.flush()` 先把新增部门写入当前事务，使后面的员工对象可以取得部门主键，但此时事务还没有最终提交。

## 十一、常见失败

| 现象 | 检查位置 |
| --- | --- |
| `Unknown database` | 是否创建了 `employee_management_fastapi` |
| `No module named app` | 是否在项目根目录执行命令 |
| 自动迁移为空 | `env.py` 是否导入 `app.models` 并设置 `target_metadata` |
| 外键插入失败 | 部门是否先写入并完成 `flush()` |
| 第二次种子数据报唯一约束错误 | 是否先按 `employee_number` 检查既有数据 |

## 十二、完成检查

- [ ] 模型字段、约束和样例编号符合项目规格。
- [ ] Alembic只管理当前项目使用的数据库。
- [ ] `alembic current` 显示最新版本。
- [ ] 两张表、唯一约束和外键与模型一致。
- [ ] 样例数据脚本可以重复执行。

完成后，当前模型和迁移版本就是 CRUD 操作使用的稳定数据库基线。
