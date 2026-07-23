# SQLAlchemy 基础概念与常用对象

> 本章目标：先不进入员工项目代码，单独掌握 `SQLAlchemy` 这套数据库工具最核心的对象、安装方式、连接方式和模型定义方式。

## 一、SQLAlchemy 是什么

`SQLAlchemy` 是 Python 中非常常用的数据库访问库。

它主要解决两类问题：

1. Python 程序如何连接数据库
2. Python 程序如何更清楚地操作表、字段和数据

在真实项目中，开发者通常不会每次都手写一大段原始数据库驱动代码，而是借助 `SQLAlchemy` 统一管理：

- 数据库连接
- 会话对象
- ORM 模型
- 查询语句
- 事务控制

如果你后面要学习：

- FastAPI
- Flask
- Django 以外的 ORM 方案
- Python 脚本访问 MySQL

那么 `SQLAlchemy` 都是非常值得掌握的基础工具。

## 二、为什么 Web 项目经常用 SQLAlchemy

对于新人来说，最容易感受到的好处有这些：

### 2.1 代码结构更清楚

如果直接拼 SQL 字符串，数据结构、字段意义、返回结果很快就会变乱。

使用 ORM 后，可以把表定义成类，把字段定义成属性，阅读成本会低很多。

### 2.2 便于和业务代码配合

Web 项目不只是查数据，还要做：

- 参数校验
- 业务判断
- 事务控制
- 分层调用

`SQLAlchemy` 在这些场景下更容易与 `Service`、`Repository`、接口层配合。

### 2.3 支持 ORM 和原始 SQL 两种方式

项目中并不是所有查询都必须写成 ORM。

`SQLAlchemy` 可以：

- 用 ORM 写常规增删改查
- 在需要时执行原始 SQL

这点在企业项目里非常实用。

## 三、安装 SQLAlchemy 和 MySQL 驱动

`SQLAlchemy` 本身不是数据库，它只是数据库访问工具。

如果要连接 MySQL，还需要安装 MySQL 驱动。

安装命令：

```bash
python -m pip install sqlalchemy pymysql
```

确认安装：

```bash
python -m pip show sqlalchemy
python -m pip show pymysql
```

说明：

| 库名 | 作用 |
| --- | --- |
| `sqlalchemy` | 数据库访问与 ORM 主库 |
| `pymysql` | 连接 MySQL 的驱动 |

## 四、SQLAlchemy 中最重要的几个对象

先把这一章最重要的对象记住：

| 对象 | 作用 | 可以怎么理解 |
| --- | --- | --- |
| `Engine` | 数据库连接入口 | 程序访问数据库的大门 |
| `Session` | 数据库操作会话 | 一次数据库操作上下文 |
| `DeclarativeBase` | ORM 基类来源 | 所有模型类的共同基础 |
| `Model` | 表对应的 Python 类 | 数据库表的类表示 |
| `mapped_column()` | 字段定义函数 | 描述数据库字段 |
| `select()` | 查询语句构造函数 | 用来写查询条件 |
| `text()` | 原始 SQL 包装函数 | 执行简单 SQL 或特殊 SQL |

### 4.1 先建立一个整体认识

很多新人一开始容易把这些对象混成一团。

可以先用下面这个思路理解：

1. 先有数据库连接入口 `Engine`
2. 再由 `Session` 基于 `Engine` 发起数据库操作
3. `Model` 用来描述表结构和承接 ORM 查询结果
4. `select()` 和 `text()` 用来表达“要执行什么查询”

如果这几个角色区分不清，后面学习 CRUD、事务和分层时会一直混乱。

### 4.2 基础对象之间的关系

```text
DATABASE_URL
    ↓
create_engine()
    ↓
Engine
    ↓
sessionmaker()
    ↓
SessionLocal
    ↓
Session
    ↓
execute(select(...) / text(...))
    ↓
Result

Model(Base)
    ↕
数据库表
```

这张关系图里，最容易混淆的是三组关系：

- `Engine` 和 `Session`
- `Model` 和查询语句
- `execute()` 和真正的查询结果

后面各节会分别拆开讲。

## 五、Engine 是什么

`Engine` 是 SQLAlchemy 中非常重要的基础对象。

它主要负责：

- 根据连接字符串连接数据库
- 管理底层连接池
- 给后续操作提供数据库入口

最基础的写法如下：

```python
from sqlalchemy import create_engine  # 导入创建 Engine 的函数


DATABASE_URL = "mysql+pymysql://root:root123@127.0.0.1:3306/training_db?charset=utf8mb4"  # MySQL 连接字符串

engine = create_engine(  # 创建数据库 Engine
    DATABASE_URL,  # 告诉 SQLAlchemy 连接哪个数据库
    echo=False,  # 不在控制台打印 SQL
    pool_pre_ping=True,  # 取连接前先检查连接是否有效
)
```

### 5.1 `create_engine()` 的常见参数

`create_engine()` 是创建 `Engine` 的入口函数。

本章先掌握最常见的几个参数：

| 参数 | 含义 | 常见取值 | 说明 |
| --- | --- | --- | --- |
| `url` | 数据库连接地址 | `DATABASE_URL` | 必填，告诉 SQLAlchemy 连接哪个数据库 |
| `echo` | 是否打印 SQL 日志 | `True` / `False` | 学习阶段开 `True` 更容易观察 SQL，项目中常设为 `False` |
| `pool_pre_ping` | 取连接前是否先检查连接有效性 | `True` / `False` | Web 项目中常用 `True` |

除了上面这几个参数，后面在更完整的项目中你还可能看到：

| 参数 | 作用 |
| --- | --- |
| `pool_size` | 连接池中保持的连接数量 |
| `max_overflow` | 超出基础连接池后允许额外创建的连接数量 |
| `future` | 旧版本过渡参数，SQLAlchemy 2.x 新项目一般不再重点关注 |

基础阶段先不用背这些高级连接池参数，但要知道企业项目里它们经常和性能、并发有关。

`create_engine()` 的返回值是：

- `Engine` 对象

也就是说，后面无论你是直接 `connect()`，还是交给 `Session` 使用，都是先从这个返回值开始。

### 5.2 `create_engine()` 在做什么

`create_engine()` 并不是“把所有 SQL 都执行掉”。

它做的是准备工作：

1. 记录数据库地址
2. 确认使用哪个驱动
3. 建立连接池管理方式
4. 提供后续访问数据库的统一入口

### 5.3 `echo=True` 和 `echo=False` 的区别

如果写成：

```python
engine = create_engine(DATABASE_URL, echo=True, pool_pre_ping=True)  # 创建 Engine 并打印 SQL
```

那么程序执行 SQL 时，控制台会输出类似日志：

```text
SELECT 1
```

这对于学习阶段很有帮助，因为可以看到：

- 当前到底发了什么 SQL
- 查询和事务何时执行

但正式项目中，通常不会长期直接打开 `echo=True`，而是交给日志系统统一管理。

### 5.4 `pool_pre_ping=True` 的作用

Web 项目运行一段时间后，旧连接可能失效。

开启这个参数后，SQLAlchemy 会在取连接时先做一次可用性检查，更适合长时间运行的服务程序。

## 六、数据库连接字符串怎么理解

MySQL 常见写法：

```text
mysql+pymysql://root:root123@127.0.0.1:3306/training_db?charset=utf8mb4
```

可以拆开理解：

| 部分 | 含义 |
| --- | --- |
| `mysql` | 目标数据库类型 |
| `pymysql` | 底层驱动 |
| `root` | 用户名 |
| `root123` | 密码 |
| `127.0.0.1` | 主机地址 |
| `3306` | MySQL 端口 |
| `training_db` | 数据库名 |
| `charset=utf8mb4` | 字符集设置 |

如果项目中有中文、日文等多语言数据，建议一开始就写清楚 `utf8mb4`。

## 七、先用 Engine 测试数据库连通

示例文件位置：

```text
examples/sqlalchemy_engine_check.py
```

```python
from sqlalchemy import create_engine  # 导入创建 Engine 的函数
from sqlalchemy import text  # 导入 text，用于包装原始 SQL


DATABASE_URL = "mysql+pymysql://root:root123@127.0.0.1:3306/training_db?charset=utf8mb4"  # 数据库连接字符串

engine = create_engine(DATABASE_URL, pool_pre_ping=True)  # 创建 Engine

with engine.connect() as connection:  # 申请一个数据库连接
    result = connection.execute(text("SELECT 1"))  # 执行最简单的测试 SQL
    print(result.scalar())  # 1
```

### 7.1 `engine.connect()` 的返回值

`engine.connect()` 返回的是：

- `Connection` 对象

这个对象负责：

- 执行 SQL
- 读取结果
- 在 `with` 结束后自动关闭或归还连接

学习阶段可以先这样理解：

- `Engine` 像“连接工厂”
- `Connection` 像“当前手里拿到的一条连接”

### 7.2 `text()` 的作用

`text()` 用来把原始 SQL 字符串包装成 SQLAlchemy 可执行对象。

例如：

```python
text("SELECT 1")
```

返回的是一个可以交给 `execute()` 执行的 SQL 对象。

它适合这些场景：

- 连接测试
- 执行简单 SQL
- 写 ORM 不方便表达的原始 SQL

### 7.3 `connection.execute()` 的参数和返回值

最常见写法：

```python
result = connection.execute(text("SELECT 1"))  # 执行 SQL 并接收结果
```

这里：

- 参数：一个可执行 SQL 对象
- 返回值：结果对象 `Result`

这个结果对象后面常见的读取方式有：

| 写法 | 作用 |
| --- | --- |
| `result.scalar()` | 取第一行第一列的值 |
| `for row in result:` | 按行遍历结果 |
| `result.all()` | 读取全部结果 |

### 7.4 `Result`、`Row` 是什么

当你执行：

```python
result = connection.execute(text("SELECT 1"))  # 执行 SQL 并接收结果
```

可以这样理解：

- `result` 是结果集对象
- 结果集里的一行数据可以看成 `row`

如果查询的是多列：

```python
result = connection.execute(text("SELECT 100 AS total_count, 'Tanaka' AS user_name"))  # 执行多列查询

for row in result:  # 遍历结果集
    print(row)  # 例如：(100, 'Tanaka')
```

此时：

- `result` 负责管理整批结果
- `row` 代表其中一行

### 7.5 `Result` 常见读取方式对比

| 写法 | 返回结果 | 适用场景 |
| --- | --- | --- |
| `result.scalar()` | 第一行第一列的值 | `SELECT 1`、统计值、单值查询 |
| `result.all()` | 所有结果行列表 | 想直接一次性取完全部行 |
| `for row in result` | 逐行遍历 | 结果较多或只想一行一行处理 |

### 7.6 为什么要先写 `SELECT 1`

因为这个 SQL 很简单，目的只是确认：

- Python 程序能连上数据库
- 驱动安装正常
- 连接字符串没有明显错误

### 7.7 `result.scalar()` 的作用

`scalar()` 常用于读取：

- 第一行第一列
- 单个统计值
- 单个测试值

例如：

```python
print(result.scalar())  # 1
```

这里返回值就是：

- `1`

### 7.8 执行过程

上面这段代码执行时，会发生这些事情：

1. `create_engine()` 准备数据库访问入口
2. `engine.connect()` 向连接池申请一个连接
3. `connection.execute()` 执行 SQL
4. 数据库返回结果
5. `result.scalar()` 取出单个值
6. `with` 结束后，连接被归还给连接池

## 八、Session 是什么

如果说 `Engine` 是数据库入口，那么 `Session` 更像是一次数据库操作上下文。

它负责：

- 查询数据
- 暂存新增、修改、删除
- 提交事务
- 回滚事务

最常见写法如下：

```python
from sqlalchemy import create_engine  # 导入创建 Engine 的函数
from sqlalchemy.orm import sessionmaker  # 导入 Session 工厂函数


DATABASE_URL = "mysql+pymysql://root:root123@127.0.0.1:3306/training_db?charset=utf8mb4"  # 数据库连接字符串

engine = create_engine(DATABASE_URL, pool_pre_ping=True)  # 创建 Engine

SessionLocal = sessionmaker(  # 创建 Session 工厂
    bind=engine,  # 绑定当前 Engine
    autoflush=False,  # 不自动 flush
    autocommit=False,  # 不自动提交事务
)
```

### 8.1 `sessionmaker()` 的参数和返回值

这一章先掌握最常见的几个参数：

| 参数 | 含义 | 说明 |
| --- | --- | --- |
| `bind` | 绑定哪个 `Engine` | 让 Session 知道要操作哪个数据库 |
| `autoflush` | 是否自动同步 SQL | 新人阶段常设为 `False` |
| `autocommit` | 是否自动提交事务 | 项目中通常设为 `False` |

`sessionmaker()` 的返回值不是 `Session`，而是：

- 一个创建 `Session` 的工厂对象

所以后面要真正拿到会话时，要再写一次：

```python
SessionLocal()  # 创建 Session 实例
```

这个关系可以直接记成：

```text
sessionmaker() -> Session 工厂
SessionLocal() -> Session 实例
```

### 8.2 `sessionmaker()` 的作用

`sessionmaker()` 不是数据库会话本身，它是一个“生产 Session 的工厂”。

后面要真正创建会话时，通常这样写：

```python
with SessionLocal() as session:  # 创建一个 Session 对象
    print(session)  # <sqlalchemy.orm.session.Session object at 0x...>
```

### 8.3 `autoflush=False` 和 `autocommit=False`

这两个参数在企业项目里很常见。

| 参数 | 作用 |
| --- | --- |
| `autoflush=False` | 不自动把内存中的变更同步到数据库 |
| `autocommit=False` | 不自动提交事务 |

这样做的好处是：事务边界更清楚，项目代码更容易控制。

### 8.4 `with SessionLocal() as session` 的意义

```python
with SessionLocal() as session:  # 创建一个 Session 对象
    print(session)  # <sqlalchemy.orm.session.Session object at 0x...>
```

这段代码的意义是：

1. 创建一个数据库会话
2. 在 `with` 代码块中使用它
3. 代码块结束后自动关闭会话

这是项目里非常常见的基本写法。

### 8.5 `Session` 和 `Connection` 的区别

这也是新人最容易混的一个点。

| 对象 | 更适合做什么 |
| --- | --- |
| `Connection` | 执行原始 SQL、做连接测试、做较底层的数据库操作 |
| `Session` | 执行 ORM 查询、管理对象状态、管理事务 |

可以先这样理解：

- `Connection` 更接近“数据库连接”
- `Session` 更接近“ORM 操作上下文”

## 九、ORM 模型是什么

ORM 的核心思路是：

- 用 Python 类表示数据库表
- 用类属性表示表字段

这样你在 Python 代码里操作的就不只是“零散数据”，而是结构明确的对象。

## 十、DeclarativeBase 和模型定义

先看最基础写法。

示例文件位置：

```text
examples/sqlalchemy_model_demo.py
```

```python
from sqlalchemy import Integer  # 导入整数类型
from sqlalchemy import String  # 导入字符串类型
from sqlalchemy.orm import DeclarativeBase  # 导入声明式基类
from sqlalchemy.orm import Mapped  # 导入字段类型标记
from sqlalchemy.orm import mapped_column  # 导入字段定义函数


class Base(DeclarativeBase):  # 定义所有 ORM 模型共同继承的基类
    pass


class User(Base):  # 定义 users 表对应的模型类
    __tablename__ = "users"  # 指定数据库表名

    id: Mapped[int] = mapped_column(Integer, primary_key=True, autoincrement=True)  # 自增主键
    user_code: Mapped[str] = mapped_column(String(20), unique=True, nullable=False)  # 用户编号
    user_name: Mapped[str] = mapped_column(String(100), nullable=False)  # 用户姓名
```

### 10.1 `class Base(DeclarativeBase)` 的作用

`DeclarativeBase` 是 SQLAlchemy 提供的声明式 ORM 基类来源。

你可以把 `Base` 理解为：

- 所有 ORM 模型共同继承的父类
- SQLAlchemy 识别模型的重要基础

### 10.2 `__tablename__` 的作用

```python
__tablename__ = "users"
```

这行表示当前模型对应数据库里的 `users` 表。

### 10.3 `Mapped[...]` 和 `mapped_column()` 怎么理解

这一对写法是 SQLAlchemy 2.x 非常常见的写法。

| 写法 | 作用 |
| --- | --- |
| `Mapped[str]` | 说明这个属性在 Python 中是什么类型 |
| `mapped_column(String(100))` | 说明这个属性在数据库中如何存储 |

也就是说，一个字段同时有两层信息：

1. Python 中的类型
2. 数据库中的字段定义

### 10.4 `mapped_column()` 的常见参数

在基础阶段最常见的参数有这些：

| 参数 | 作用 | 常见示例 |
| --- | --- | --- |
| 第一个位置参数 | 数据库类型 | `Integer`、`String(100)` |
| `primary_key` | 是否主键 | `primary_key=True` |
| `autoincrement` | 是否自增 | `autoincrement=True` |
| `unique` | 是否唯一 | `unique=True` |
| `nullable` | 是否允许为空 | `nullable=False` |
| `default` | Python 侧默认值 | `default=datetime.now` |

例如：

```python
user_name: Mapped[str] = mapped_column(String(100), nullable=False)  # 字符串字段，不允许为空
```

这里可以拆开理解：

- `Mapped[str]`：Python 中这是字符串
- `String(100)`：数据库中这是长度 100 的字符串字段
- `nullable=False`：数据库中不允许为空

### 10.5 `String(100)`、`Integer` 这些类型到底表示什么

这些写法表示数据库字段类型。

常见类型对照如下：

| 写法 | 数据库含义 | 常见用途 |
| --- | --- | --- |
| `Integer` | 整数字段 | 主键、数量、状态码 |
| `String(20)` | 长度 20 的字符串 | 编号、短名称 |
| `String(100)` | 长度 100 的字符串 | 姓名、标题 |
| `DateTime` | 日期时间字段 | 创建时间、更新时间 |

基础阶段先掌握“它们是在描述数据库字段类型”，不要把它们理解成 Python 普通变量类型。

## 十一、`text()`、`select()`、模型对象各自的定位

很多新人容易把这些对象混在一起，这里单独区分一下：

| 名称 | 更适合做什么 |
| --- | --- |
| `text()` | 包装原始 SQL |
| `select()` | 用 ORM 风格构造查询语句 |
| 模型对象 | 表结构定义和查询结果承载 |

例如：

```python
from sqlalchemy import select  # 导入 select
from sqlalchemy import text  # 导入 text


raw_sql = text("SELECT 1")  # 原始 SQL 方式
statement = select(User)  # ORM 查询语句方式
```

这两种方式都在项目里出现过，只是用途不同。

### 11.1 `text()` 和 `select()` 的区别表

| 对比项 | `text()` | `select()` |
| --- | --- | --- |
| 风格 | 原始 SQL 风格 | ORM / SQLAlchemy 风格 |
| 主要用途 | 连接测试、简单 SQL、复杂原始 SQL | 常规 ORM 查询 |
| 可读性 | 更接近数据库原始写法 | 更接近 Python 模型写法 |
| 和模型关系 | 不一定依赖模型 | 通常直接依赖模型 |

### 11.2 学习阶段怎么选

当前阶段可以先这样记：

- 连接测试先用 `text("SELECT 1")`
- 常规表数据查询后面优先学习 `select(Model)`

## 十二、SQLAlchemy 基础对象关系图

```text
Python 业务代码
    ↓
Session
    ↓
Engine
    ↓
PyMySQL
    ↓
MySQL

ORM Model
    ↑
数据库表
```

理解这个关系后，后面学习 CRUD、事务、迁移和分层会自然很多。

### 12.1 这一章最容易混淆的几个点

1. `Engine` 不是查询结果
2. `SessionLocal` 不是 `Session` 本身，而是工厂
3. `select()` 不是结果，而是查询语句对象
4. `execute()` 之后拿到的才是结果对象
5. `Model` 不是数据库表本身，而是表在 Python 中的类表示

## 十三、常见错误

### 13.1 只安装了 SQLAlchemy，没安装 MySQL 驱动

常见现象：

- 连接 MySQL 时提示驱动模块找不到

处理方式：

```bash
python -m pip install pymysql
```

### 13.2 连接字符串写错

常见问题：

- 用户名错误
- 密码错误
- 数据库名错误
- 端口错误

### 13.3 把 Engine 和 Session 当成一回事

要区分：

- `Engine` 是数据库入口
- `Session` 是数据库操作上下文

### 13.4 定义了普通类，但没有继承 `Base`

如果模型没有继承 `Base`，它只是普通 Python 类，不是 ORM 模型。

## 十四、基础练习

1. 安装 `sqlalchemy` 和 `pymysql`。
2. 写出一个 MySQL 连接字符串，并说出每一部分的作用。
3. 使用 `create_engine()` 和 `text("SELECT 1")` 测试数据库是否可连接。

## 十五、综合练习

编写一个练习文件，完成以下内容：

1. 创建 `Engine`
2. 创建 `SessionLocal`
3. 定义 `Base`
4. 定义一个简单的 `User` 模型
5. 用自己的话说明 `Engine`、`Session`、`Model` 三者分别负责什么

## 十六、本章总结

| 知识点 | 作用 |
| --- | --- |
| `create_engine()` | 创建数据库访问入口 |
| `Engine` | 管理连接与连接池 |
| `sessionmaker()` | 创建 Session 工厂 |
| `Session` | 管理数据库操作与事务 |
| `DeclarativeBase` | ORM 基类来源 |
| `Mapped` / `mapped_column()` | 定义 ORM 字段 |
| `text()` | 执行原始 SQL |
| ORM Model | 用类映射数据库表 |

下一章开始，在这些基础对象之上继续学习最常用的 CRUD 和事务方法。
