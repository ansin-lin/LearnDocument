# 项目数据库配置与模型定义

> 本章目标：从这一章开始正式进入员工管理项目，把前面学过的 `SQLAlchemy` 和 `Alembic` 基础知识放到项目目录中，完成数据库配置、会话入口、模型定义、建表 SQL 和测试数据准备。

## 一、先看项目结构

这一章开始，代码不再是单独练习片段，而是正式写进项目文件。

```text
employee_api/
├── app/
│   ├── __init__.py
│   ├── config.py
│   ├── db/
│   │   ├── __init__.py
│   │   ├── base.py
│   │   └── session.py
│   └── models/
│       ├── __init__.py
│       └── employee.py
├── alembic/
├── alembic.ini
└── examples/
```

这一章要处理的文件是：

- `app/config.py`
- `app/db/base.py`
- `app/db/session.py`
- `app/models/employee.py`

## 二、先准备 `employees` 表结构和测试数据

在项目里定义模型之前，先明确数据库表要长什么样。

### 2.1 `employees` 表结构

| 字段名 | 类型 | 作用 |
| --- | --- | --- |
| `id` | `INT` | 自增主键 |
| `employee_id` | `VARCHAR(20)` | 员工编号，唯一 |
| `employee_name` | `VARCHAR(100)` | 员工姓名 |
| `department_name` | `VARCHAR(100)` | 部门名称 |
| `email` | `VARCHAR(255)` | 邮箱，可为空 |
| `created_at` | `DATETIME` | 创建时间 |
| `updated_at` | `DATETIME` | 更新时间 |

### 2.2 建表 SQL

```sql
CREATE TABLE employees (
    id INT NOT NULL AUTO_INCREMENT,
    employee_id VARCHAR(20) NOT NULL,
    employee_name VARCHAR(100) NOT NULL,
    department_name VARCHAR(100) NOT NULL,
    email VARCHAR(255) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_employees_employee_id (employee_id)
);
```

### 2.3 测试数据 SQL

```sql
INSERT INTO employees (
    employee_id,
    employee_name,
    department_name,
    email
) VALUES
    ('E001', 'Tanaka', 'Sales', 'tanaka@example.com'),
    ('E002', 'Suzuki', 'IT', 'suzuki@example.com'),
    ('E003', 'Sato', 'HR', NULL);
```

### 2.4 为什么这一章就准备测试数据

因为下一章开始要直接做项目 CRUD，如果数据库里完全没有数据，很多查询示例不容易验证。

### 2.5 为什么模型定义前先看表结构

因为在项目里，模型不是随便想到什么就写什么。

模型字段应该和数据库设计保持一致。

先看表结构的好处是：

- 字段职责更明确
- 模型命名更稳定
- 后面迁移和 CRUD 更不容易出错

## 三、项目数据库配置文件

文件位置：

```text
app/config.py
```

```python
from pathlib import Path  # 导入 Path，统一使用 pathlib 处理路径


BASE_DIR = Path(__file__).resolve().parent.parent  # app 目录的上一级

DB_HOST = "127.0.0.1"  # MySQL 主机地址
DB_PORT = 3306  # MySQL 端口
DB_USER = "root"  # 数据库用户名
DB_PASSWORD = "root123"  # 数据库密码
DB_NAME = "employee_management"  # 数据库名

DATABASE_URL = (  # SQLAlchemy 使用的连接字符串
    f"mysql+pymysql://{DB_USER}:{DB_PASSWORD}@{DB_HOST}:{DB_PORT}/{DB_NAME}?charset=utf8mb4"
)
```

### 3.1 每个配置项分别表示什么

| 配置项 | 作用 |
| --- | --- |
| `DB_HOST` | 数据库服务器地址 |
| `DB_PORT` | 数据库端口 |
| `DB_USER` | 登录数据库的用户名 |
| `DB_PASSWORD` | 登录数据库的密码 |
| `DB_NAME` | 要连接的数据库名 |
| `DATABASE_URL` | 交给 SQLAlchemy 使用的完整连接字符串 |

### 3.2 为什么数据库地址单独放在配置文件

如果每个文件都各自写一遍数据库地址，后续变更会非常麻烦。

单独集中管理后：

- 修改数据库环境更方便
- 后面切到环境变量也更自然
- 项目结构更统一

## 四、项目 Base 文件

文件位置：

```text
app/db/base.py
```

```python
from sqlalchemy.orm import DeclarativeBase  # 导入声明式基类


class Base(DeclarativeBase):  # 所有 ORM 模型的共同父类
    pass
```

### 4.1 这个文件在项目里的作用

后面所有 ORM 模型都会继承这个 `Base`，例如：

```python
class Employee(Base):
    ...
```

这样项目里的模型定义方式会统一。

## 五、项目 Session 文件

文件位置：

```text
app/db/session.py
```

```python
from sqlalchemy import create_engine  # 导入创建 Engine 的函数
from sqlalchemy.orm import sessionmaker  # 导入 Session 工厂

from app.config import DATABASE_URL  # 导入数据库连接字符串


engine = create_engine(  # 创建数据库 Engine
    DATABASE_URL,  # 数据库连接地址
    echo=False,  # 不打印 SQL
    pool_pre_ping=True,  # 取连接前先检查连接是否可用
)

SessionLocal = sessionmaker(  # 创建 Session 工厂
    bind=engine,  # 绑定 Engine
    autoflush=False,  # 不自动 flush
    autocommit=False,  # 不自动提交事务
)
```

### 5.1 `create_engine()` 和 `sessionmaker()` 在项目中的分工

| 对象或函数 | 作用 |
| --- | --- |
| `create_engine()` | 创建项目数据库入口 |
| `engine` | 管理连接与连接池 |
| `sessionmaker()` | 创建 Session 工厂 |
| `SessionLocal` | 生成每次数据库操作使用的 Session |

### 5.2 这一段代码以后会被谁使用

后面这些位置都会依赖它：

- CRUD 脚本
- Repository
- Service
- FastAPI 依赖注入

所以 `session.py` 是项目数据库层的核心入口之一。

### 5.3 为什么项目里通常只保留一个 `SessionLocal`

因为项目希望：

- 会话创建方式统一
- 后续依赖注入方式统一
- 事务控制方式统一

如果每个模块都自己创建一套会话工厂，后面维护会比较乱。

## 六、员工模型文件

文件位置：

```text
app/models/employee.py
```

```python
from datetime import datetime  # 导入 datetime，用于记录时间

from sqlalchemy import DateTime  # 导入 DateTime 类型
from sqlalchemy import Integer  # 导入 Integer 类型
from sqlalchemy import String  # 导入 String 类型
from sqlalchemy.orm import Mapped  # 导入字段类型标记
from sqlalchemy.orm import mapped_column  # 导入字段定义函数

from app.db.base import Base  # 导入项目 Base


class Employee(Base):  # 定义 employees 表对应的模型类
    __tablename__ = "employees"  # 指定数据库表名

    id: Mapped[int] = mapped_column(Integer, primary_key=True, autoincrement=True)  # 自增主键
    employee_id: Mapped[str] = mapped_column(String(20), unique=True, nullable=False)  # 员工编号
    employee_name: Mapped[str] = mapped_column(String(100), nullable=False)  # 员工姓名
    department_name: Mapped[str] = mapped_column(String(100), nullable=False)  # 部门名称
    email: Mapped[str | None] = mapped_column(String(255), nullable=True)  # 邮箱，可为空
    created_at: Mapped[datetime] = mapped_column(DateTime, default=datetime.now, nullable=False)  # 创建时间
    updated_at: Mapped[datetime] = mapped_column(
        DateTime,
        default=datetime.now,
        onupdate=datetime.now,
        nullable=False,
    )  # 更新时间
```

### 6.1 `Employee` 模型里每个字段为什么这样定义

| 字段 | 这样定义的原因 |
| --- | --- |
| `id` | 作为数据库内部主键，便于关联和排序 |
| `employee_id` | 作为业务编号，要求唯一 |
| `employee_name` | 保存员工姓名，不能为空 |
| `department_name` | 保存部门名称，不能为空 |
| `email` | 邮箱不一定每条数据都有，所以允许为空 |
| `created_at` | 保存创建时间 |
| `updated_at` | 保存最后更新时间 |

### 6.2 `default=datetime.now` 和 `onupdate=datetime.now` 的作用

| 写法 | 作用 |
| --- | --- |
| `default=datetime.now` | 新增对象时默认写入当前时间 |
| `onupdate=datetime.now` | 更新对象时自动刷新时间 |

这类字段在项目里非常常见，因为很多表都需要记录：

- 什么时候创建
- 最后一次什么时候修改

## 七、从模型代码反向看表结构

这一段代码要和前面的表结构一一对应。

| 模型字段 | 数据库字段 | 说明 |
| --- | --- | --- |
| `id` | `id` | 自增主键 |
| `employee_id` | `employee_id` | 员工编号 |
| `employee_name` | `employee_name` | 员工姓名 |
| `department_name` | `department_name` | 部门名称 |
| `email` | `email` | 邮箱 |
| `created_at` | `created_at` | 创建时间 |
| `updated_at` | `updated_at` | 更新时间 |

### 7.1 `mapped_column()` 里这些参数在项目里分别表示什么

| 写法 | 含义 |
| --- | --- |
| `primary_key=True` | 当前字段是主键 |
| `autoincrement=True` | 当前字段是自增字段 |
| `unique=True` | 当前字段值不能重复 |
| `nullable=False` | 当前字段不能为空 |
| `nullable=True` | 当前字段允许为空 |
| `default=datetime.now` | Python 侧默认值 |
| `onupdate=datetime.now` | 更新时自动刷新时间 |

### 7.2 为什么这里要坚持字段一一对应

如果表字段和模型字段不一致，后面会直接影响：

- 查询
- 新增
- 更新
- 迁移
- 接口返回

所以这一章最核心的事情之一，就是把“数据库表”和“项目模型”对齐。

### 7.3 项目里字段命名为什么要稳定

一旦字段名进入项目，它通常会影响：

- ORM 模型
- CRUD 代码
- 接口响应
- 前端联调
- 报表导出

所以项目中字段命名最好一开始就保持稳定、清楚。

## 八、数据库连通与数据确认脚本

示例文件位置：

```text
examples/project_db_setup_check.py
```

```python
from sqlalchemy import text  # 导入 text，用于执行原始 SQL

from app.db.session import engine  # 导入项目 Engine


with engine.connect() as connection:  # 创建数据库连接
    result = connection.execute(text("SELECT 1"))  # 执行最简单的测试 SQL
    print(result.scalar())  # 1
```

这里用 `text("SELECT 1")` 而不是直接做员工查询，是因为要先确认：

- 项目配置能否连接数据库
- 驱动是否正常
- 连接入口是否可用

再写一个确认员工表数据的示例：

```python
from sqlalchemy import text  # 导入 text

from app.db.session import engine  # 导入项目 Engine


with engine.connect() as connection:  # 创建数据库连接
    result = connection.execute(  # 执行员工表查询
        text(
            """
            SELECT id, employee_id, employee_name, department_name, email
            FROM employees
            ORDER BY id
            """
        )
    )

    for row in result:  # 遍历查询结果
        print(row)  # 例如：(1, 'E001', 'Tanaka', 'Sales', 'tanaka@example.com')
```

## 九、这一章和前面工具讲解章的区别

前面第 3 到第 5 章是“先学库本身”。

这一章开始，进入的是正式项目代码，重点已经变成：

- 代码写在哪个文件
- 数据库表如何和模型对齐
- 后续 CRUD 如何依赖这些代码

## 十、常见错误

### 10.1 `DATABASE_URL` 写错

常见现象：

- 连接失败
- 用户名或密码错误
- 数据库不存在

### 10.2 模型没有继承 `Base`

如果没有继承 `Base`，这个类就不是 ORM 模型。

### 10.3 表结构和模型字段不一致

例如数据库里是 `department_name`，模型里却写成 `department`，后面很容易出问题。

### 10.4 建了模型但数据库没准备测试数据

这样下一章做查询时，结果就不直观。

## 十一、基础练习

1. 完成 `app/config.py`。
2. 完成 `app/db/base.py`。
3. 完成 `app/db/session.py`。
4. 完成 `app/models/employee.py`。

## 十二、综合练习

在自己的员工项目中完成下面内容：

1. 创建 `employees` 表
2. 插入三条测试数据
3. 配置项目数据库连接
4. 定义 `Employee` 模型
5. 运行数据库连通和数据确认脚本

## 十三、本章总结

| 文件或内容 | 作用 |
| --- | --- |
| `app/config.py` | 管理数据库配置 |
| `app/db/base.py` | 定义项目 ORM 基类 |
| `app/db/session.py` | 创建 Engine 和 Session 工厂 |
| `app/models/employee.py` | 定义员工模型 |
| `employees` 表 | 员工业务数据表 |
| 测试数据 | 用于验证项目 CRUD |

下一章开始，基于这些正式项目文件编写员工项目中的 CRUD 和事务控制代码。
