# 第4章 FastAPI 与 SQLAlchemy 数据库基础

> 本章目标：掌握 FastAPI 项目中使用 SQLAlchemy 连接 MySQL 的基本结构，理解 Engine、Session、Model 和数据库表之间的关系。

## 一、为什么需要数据库

接口不能只返回写死的数据。企业项目中的员工、部门、订单、申请等数据通常保存在数据库中。

FastAPI 负责接收请求和返回响应，SQLAlchemy 负责让 Python 程序操作数据库。

```text
前端请求
-> FastAPI 路由函数
-> SQLAlchemy Session
-> MySQL 数据库
-> 返回查询结果
```

## 二、安装依赖

```powershell
pip install sqlalchemy pymysql  # 安装 SQLAlchemy 和 MySQL 驱动
```

依赖说明：

| 依赖 | 作用 |
| --- | --- |
| `sqlalchemy` | Python 数据库 ORM 和 SQL 工具 |
| `pymysql` | Python 连接 MySQL 的驱动 |

## 三、数据库连接地址

MySQL 连接地址示例：

```python
DATABASE_URL = "mysql+pymysql://root:password@127.0.0.1:3306/employee_management?charset=utf8mb4"  # MySQL 连接地址
```

组成说明：

| 部分 | 说明 |
| --- | --- |
| `mysql+pymysql` | 数据库类型和驱动 |
| `root` | 用户名 |
| `password` | 密码，正式项目不要写死 |
| `127.0.0.1` | 数据库地址 |
| `3306` | MySQL 端口 |
| `employee_management` | 数据库名 |
| `charset=utf8mb4` | 支持中文、日文和 Emoji |

## 四、创建数据库连接

文件位置：

```text
app/database.py
```

```python
from sqlalchemy import create_engine  # 导入 create_engine，用于创建数据库引擎
from sqlalchemy.orm import DeclarativeBase, sessionmaker  # 导入 ORM 基类和 Session 工厂

DATABASE_URL = "mysql+pymysql://root:password@127.0.0.1:3306/employee_management?charset=utf8mb4"  # 数据库连接地址

engine = create_engine(DATABASE_URL, echo=True)  # 创建 Engine，echo=True 会输出 SQL 日志
SessionLocal = sessionmaker(bind=engine, autoflush=False, autocommit=False)  # 创建数据库 Session 工厂


class Base(DeclarativeBase):  # 定义所有 ORM 模型的基类
    pass  # 当前不需要额外配置
```

对象说明：

| 对象 | 作用 |
| --- | --- |
| `engine` | 管理数据库连接能力 |
| `SessionLocal` | 创建数据库会话 |
| `Base` | ORM 模型类的共同父类 |

## 五、定义 ORM Model

文件位置：

```text
app/models.py
```

```python
from sqlalchemy import Boolean, Date, ForeignKey, Integer, String  # 导入常用字段类型
from sqlalchemy.orm import Mapped, mapped_column, relationship  # 导入 SQLAlchemy 2.x ORM 映射工具
from app.database import Base  # 导入 ORM 基类


class Department(Base):  # 定义部门模型
    __tablename__ = "departments"  # 指定数据库表名

    id: Mapped[int] = mapped_column(Integer, primary_key=True, index=True)  # 主键 ID
    code: Mapped[str] = mapped_column(String(20), unique=True, nullable=False)  # 部门编码
    name: Mapped[str] = mapped_column(String(100), nullable=False)  # 部门名称
    is_active: Mapped[bool] = mapped_column(Boolean, default=True)  # 是否有效


class Employee(Base):  # 定义员工模型
    __tablename__ = "employees"  # 指定数据库表名

    id: Mapped[int] = mapped_column(Integer, primary_key=True, index=True)  # 主键 ID
    employee_code: Mapped[str] = mapped_column(String(20), unique=True, nullable=False)  # 员工编号
    name: Mapped[str] = mapped_column(String(100), nullable=False)  # 员工姓名
    email: Mapped[str | None] = mapped_column(String(255), nullable=True)  # 邮箱
    joined_date: Mapped[Date] = mapped_column(Date, nullable=False)  # 入职日期
    is_active: Mapped[bool] = mapped_column(Boolean, default=True)  # 是否在职
    department_id: Mapped[int] = mapped_column(ForeignKey("departments.id"), nullable=False)  # 所属部门 ID
    department: Mapped[Department] = relationship()  # 员工关联的部门对象
```

## 六、创建表

学习阶段可以先用代码创建表。

文件位置：

```text
app/create_tables.py
```

```python
from app.database import Base, engine  # 导入 Base 和 engine
from app import models  # 导入 models，确保模型类被加载

Base.metadata.create_all(bind=engine)  # 根据 ORM 模型创建数据库表
```

执行：

```powershell
python -m app.create_tables  # 创建数据库表
```

正式项目更推荐使用 Alembic 做迁移，避免多人开发时表结构不可控。

## 七、创建数据库 Session

Session 表示一次数据库操作上下文，不是数据库本身。

```python
from app.database import SessionLocal  # 导入 Session 工厂

db = SessionLocal()  # 创建数据库 Session
try:  # 开始异常保护
    pass  # 这里执行数据库操作
finally:  # 无论是否异常都会执行
    db.close()  # 关闭 Session，释放连接
```

Session 常用方法：

| 方法 | 作用 |
| --- | --- |
| `add()` | 添加一个 ORM 对象 |
| `commit()` | 提交事务 |
| `rollback()` | 回滚事务 |
| `refresh()` | 从数据库刷新对象 |
| `close()` | 关闭 Session |
| `execute()` | 执行查询语句 |

## 八、基础练习

请完成：

1. 安装 `sqlalchemy` 和 `pymysql`
2. 创建 `app/database.py`
3. 创建 `Department` 和 `Employee` 模型
4. 执行创建表脚本
5. 在 MySQL 中确认表是否创建成功

## 九、本章总结

- FastAPI 负责接口，SQLAlchemy 负责数据库操作
- `engine` 管理数据库连接能力
- `Session` 表示一次数据库操作上下文
- ORM Model 对应数据库表
- 学习阶段可以用 `create_all()` 创建表，正式项目推荐迁移工具
