# 第8章 SQLAlchemy ORM CRUD与事务

> 本章目标：继续只讲 `SQLAlchemy` 本身，不进入员工项目文件。重点掌握 ORM 模型的查询、新增、更新、删除，以及 `flush()`、`commit()`、`rollback()` 等事务方法。

本章建议按四个检查点完成：创建专用练习表；完成列表与单条查询；完成新增、修改和删除；最后执行`flush()`、唯一约束失败和`rollback()`实验。每个检查点运行并核对数据库状态后再继续。

本章完整示例统一使用`examples/sqlalchemy_crud_demo.py`。后续标注某个方法写法的短代码块是从该文件抽出的语法片段，不需要在文件末尾重复追加。

开始前确认已完成第7章的独立MySQL练习数据库和`SQLALCHEMY_TRAINING_URL`配置。本章会删除并重建自己专用的`sqlalchemy_users_demo`表，不能连接员工项目数据库、其他课程数据库或生产数据库。

## 一、先准备一个练习用模型

示例文件位置：

```text
examples/sqlalchemy_crud_demo.py
```

```python
from datetime import datetime  # 导入 datetime，用于记录时间

from sqlalchemy import DateTime  # 导入 DateTime 类型
from sqlalchemy import Integer  # 导入 Integer 类型
from sqlalchemy import String  # 导入 String 类型
from sqlalchemy.orm import DeclarativeBase  # 导入声明式基类
from sqlalchemy.orm import Mapped  # 导入字段类型标记
from sqlalchemy.orm import mapped_column  # 导入字段定义函数


class Base(DeclarativeBase):  # 定义 ORM 基类
    pass  # 当前类不需要额外实现


class User(Base):  # 定义独立实验表对应的模型类
    __tablename__ = "sqlalchemy_users_demo"  # 使用只属于本章的练习表名

    id: Mapped[int] = mapped_column(Integer, primary_key=True, autoincrement=True)  # 自增主键
    user_code: Mapped[str] = mapped_column(String(20), unique=True, nullable=False)  # 用户编号
    user_name: Mapped[str] = mapped_column(String(100), nullable=False)  # 用户姓名
    department_name: Mapped[str] = mapped_column(String(100), nullable=False)  # 部门名称
    email: Mapped[str | None] = mapped_column(String(255), nullable=True)  # 邮箱，可为空
    created_at: Mapped[datetime] = mapped_column(DateTime, default=datetime.now, nullable=False)  # 创建时间
```

### 1.1 为什么这一章继续用简单模型

这一章的重点是：

- 查
- 增
- 改
- 删
- 事务

所以这里刻意使用简单模型，而不是一开始就进入员工项目。

这样做的好处是：

- 学员可以把注意力放在 SQLAlchemy 方法本身
- 不会一边学方法，一边被项目结构分散注意力

## 二、模型定义中常见字段写法

### 2.1 主键字段

```python
id: Mapped[int] = mapped_column(Integer, primary_key=True, autoincrement=True)  # 自增主键
```

这里包含几个重要点：

- `Integer`：数据库整数类型
- `primary_key=True`：主键
- `autoincrement=True`：自增

### 2.2 唯一字段

```python
user_code: Mapped[str] = mapped_column(String(20), unique=True, nullable=False)  # 用户编号
```

这类字段很适合保存：

- 用户编号
- 员工编号
- 商品编号
- 订单编号

### 2.3 可空字段

```python
email: Mapped[str | None] = mapped_column(String(255), nullable=True)  # 邮箱，可为空
```

`nullable=True` 表示数据库层允许为空。

## 三、先准备 Session

示例文件位置：

```text
examples/sqlalchemy_crud_demo.py
```

```python
import os  # 导入环境变量读取工具

from sqlalchemy import create_engine  # 导入创建 Engine 的函数
from sqlalchemy.orm import sessionmaker  # 导入 Session 工厂函数


DATABASE_URL = os.environ["SQLALCHEMY_TRAINING_URL"]  # 读取第7章配置的练习数据库地址

engine = create_engine(DATABASE_URL, pool_pre_ping=True)  # 创建 Engine

SessionLocal = sessionmaker(  # 创建 Session 工厂
    bind=engine,  # 绑定 Engine
    autoflush=False,  # 不自动 flush
    autocommit=False,  # 不自动提交事务
)  # 完成当前调用或数据结构

Base.metadata.drop_all(  # 删除上一次实验留下的练习表
    bind=engine,  # 指定执行结构操作的Engine
    tables=[User.__table__],  # 只删除本章的sqlalchemy_users_demo表
)  # 完成练习表清理
Base.metadata.create_all(  # 根据ORM元数据创建练习表
    bind=engine,  # 指定执行结构操作的Engine
    tables=[User.__table__],  # 只创建本章的sqlalchemy_users_demo表
)  # 完成练习表创建
```

这两行结构操作只用于可重复执行的独立实验。脚本每次运行时先删除并重新创建`sqlalchemy_users_demo`，因此不会因为上一次留下的`U100`、`U200`而触发重复编号错误。

| 调用 | 参数 | 可接受的值 | 默认值或必填性 | 作用 |
| --- | --- | --- | --- | --- |
| `Base.metadata.drop_all()` | `bind` | SQLAlchemy `Engine`或`Connection` | 必填 | 指定在哪个数据库执行删除表操作 |
| `Base.metadata.drop_all()` | `tables` | SQLAlchemy表对象组成的序列或`None` | 默认`None` | 限定只删除指定练习表 |
| `Base.metadata.create_all()` | `bind` | SQLAlchemy `Engine`或`Connection` | 必填 | 指定在哪个数据库创建表 |
| `Base.metadata.create_all()` | `tables` | SQLAlchemy表对象组成的序列或`None` | 默认`None` | 限定只创建指定练习表 |

`drop_all()`会删除表和其中的数据，只能连接第7章准备的可丢弃练习数据库。正式员工项目从第10章开始统一使用Alembic，不能在应用启动时调用`drop_all()`或`create_all()`代替迁移。

### 3.1 为什么 CRUD 示例要先准备 `SessionLocal`

因为所有 ORM 的查询、新增、更新、删除，最终都要放在某个 `Session` 中执行。

也就是说：

- `Engine` 负责连接入口
- `Session` 负责当前这次数据库操作

后面看到的这些方法，基本都属于 `Session` 或查询结果对象：

- `execute()`
- `add()`
- `delete()`
- `commit()`
- `rollback()`
- `refresh()`

## 四、查询全部数据

```python
from sqlalchemy import select  # 导入 select 查询构造函数


with SessionLocal() as session:  # 创建数据库会话
    statement = select(User).order_by(User.id)  # 查询 users 表全部数据并按 id 排序
    users = session.execute(statement).scalars().all()  # 执行查询，取出模型对象列表

    for user in users:  # 遍历用户对象
        print(user.user_code, user.user_name)  # U001 Tanaka
```

### 4.1 这段代码中每一步的对象变化

```text
select(User)
    ↓
statement 查询语句对象
    ↓
session.execute(statement)  # 使用当前Session执行已经构建的查询语句
    ↓
Result 结果集对象
    ↓
scalars()
    ↓
只保留模型对象流
    ↓
all()
    ↓
list[User]
```

### 4.2 `select(User)` 是什么

它表示：

- 查询目标是 `User` 这个模型
- 结果希望按 ORM 对象来处理

`select()` 的返回值不是查询结果，而是：

- 查询语句对象

所以它还不能直接拿到数据，必须再交给：

```python
session.execute(statement)  # 把查询语句交给当前Session执行
```

### 4.3 `order_by(User.id)` 的作用

用于指定排序字段。

项目里常见排序字段：

- 主键
- 创建时间
- 业务编号

### 4.4 `scalars().all()` 的作用

| 调用 | 作用 |
| --- | --- |
| `execute(statement)` | 执行查询 |
| `scalars()` | 只取模型对象本身 |
| `all()` | 取全部结果，返回列表 |

返回值说明：

- `users = session.execute(statement).scalars().all()`
- `users` 的类型可以理解为 `list[User]`

适用场景：

- 查询列表页数据
- 查询全部基础数据
- 查询某个条件下的多条记录

### 4.5 `session.execute(statement)` 返回的不是列表

这一点必须反复强调。

很多新人容易以为：

```python
users = session.execute(statement)  # 此时得到Result对象而不是普通列表
```

此时就已经拿到用户列表了。

实际上并不是。

这里拿到的是：

- 结果集对象 `Result`

只有继续调用：

- `scalars()`
- `all()`

之后，才会真正得到更适合当前代码使用的数据结构。

## 五、按条件查询一条数据

```python
from sqlalchemy import select  # 导入 select


with SessionLocal() as session:  # 创建数据库会话
    statement = select(User).where(User.user_code == "U001")  # 按用户编号筛选
    user = session.execute(statement).scalar_one_or_none()  # 读取一条记录或 None

    if user is not None:  # 找到数据时输出字段
        print(user.user_name)  # Tanaka
```

### 5.1 `where()` 的作用

`where()` 用于添加筛选条件。

最常见的用法就是：

- 编号查询
- 主键查询
- 状态查询

`where()` 的参数通常是一个条件表达式，例如：

```python
User.user_code == "U001"  # 条件表达式
```

返回值仍然是查询语句对象，所以 `where()` 常常是“继续拼接查询”的一步。

### 5.2 `scalar_one_or_none()` 的作用

这个方法非常适合“按唯一条件查一条”的场景。

它的语义是：

- 查到一条，返回这一条
- 没查到，返回 `None`
- 如果查出多条，说明数据本身有问题

返回值说明：

| 查询结果情况 | 返回值 |
| --- | --- |
| 查到 1 条 | 返回 `User` 对象 |
| 查到 0 条 | 返回 `None` |
| 查到多条 | 抛出异常 |

适用场景：

- 根据唯一编号查询
- 根据主键查询
- 业务上本来就应该只有一条的查询

### 5.3 `scalar()`、`scalars()`、`scalar_one_or_none()` 的区别

这几个名字非常像，是新人高频混淆点。

| 方法 | 常见返回结果 | 适用场景 |
| --- | --- | --- |
| `scalar()` | 第一行第一列的值 | 单个值查询、统计值查询 |
| `scalars()` | 一个可继续迭代的标量结果流 | 想继续接 `.all()` 等处理模型对象 |
| `scalar_one_or_none()` | 单个对象或 `None` | 按唯一条件查 1 条 |

可以先这样记忆：

- `scalar()` 更像“单个值”
- `scalars()` 更像“多个值流”
- `scalar_one_or_none()` 更像“按唯一结果取一条对象”

### 5.4 这一章为什么不用 `first()`

因为基础阶段先把“唯一条件查询”和“列表查询”讲清楚更重要。

`first()` 也很常见，但它的语义是：

- 取第一条

它不强调“本来应该只有一条”，所以新人阶段先不作为主方法。

## 六、新增数据

```python
with SessionLocal() as session:  # 创建数据库会话
    user = User(  # 创建新的 ORM 对象
        user_code="U100",  # 用户编号
        user_name="Sato",  # 用户姓名
        department_name="Sales",  # 所属部门
        email="sato@example.com",  # 邮箱
    )  # 完成当前调用或数据结构

    session.add(user)  # 把对象加入当前会话
    session.commit()  # 提交事务
    session.refresh(user)  # 重新读取数据库中的最新值

    print(user.id)  # 例如：1
    print(user.user_code)  # U100
```

### 6.1 `session.add()` 的作用

`add()` 的作用是把 ORM 对象加入当前会话，表示这条数据准备交给本次事务处理。

参数说明：

| 参数 | 作用 | 可接受的值 | 默认值或必填性 |
| --- | --- | --- | --- |
| `instance` | 把ORM对象加入当前Session | 已映射的ORM对象实例 | 必填，可作为第一个位置参数 |

返回值：

- `None`

### 6.2 `commit()` 的作用

`commit()` 才是真正让新增结果生效的关键一步。

如果只 `add()` 不 `commit()`：

- 当前事务不会真正完成
- 数据库里通常看不到最终结果

返回值：

- `None`

使用场景：

- 新增完成后正式保存
- 更新完成后正式保存
- 删除完成后正式保存

### 6.3 `refresh()` 的作用

`refresh()` 会重新从数据库读取对象的最新状态。

它常用于获取：

- 自增主键
- 数据库默认值
- 数据库自动更新字段

参数说明：

| 参数 | 作用 | 可接受的值 | 默认值或必填性 |
| --- | --- | --- | --- |
| `instance` | 从数据库重新加载对象状态 | 当前Session中的持久化ORM对象 | 必填，可作为第一个位置参数 |

返回值：

- `None`

### 6.4 `add()`、`flush()`、`commit()`、`refresh()` 的顺序怎么理解

新增时经常会看到这些方法组合出现。

基础理解如下：

1. `add()`：把对象交给当前会话管理
2. `flush()`：先同步 SQL，但事务还没最终提交
3. `commit()`：正式提交事务
4. `refresh()`：重新从数据库取最新状态

不是每次新增都必须四个都写，但要知道它们各自负责什么。

## 七、更新数据

```python
from sqlalchemy import select  # 导入 select


with SessionLocal() as session:  # 创建数据库会话
    statement = select(User).where(User.user_code == "U100")  # 查询目标用户
    user = session.execute(statement).scalar_one_or_none()  # 读取一条数据

    if user is not None:  # 找到用户时才更新
        user.department_name = "Finance"  # 修改部门
        user.email = "sato.finance@example.com"  # 修改邮箱
        session.commit()  # 提交事务

        print(user.department_name)  # Finance
```

### 7.1 为什么更新时没有再写 `add()`

因为这个对象本来就是通过当前 `Session` 查询出来的。

它已经处于当前会话管理中，所以只要：

1. 修改属性
2. 再 `commit()`

SQLAlchemy 就会把变更同步到数据库。

这类对象通常可以先理解成：

- 已经挂在当前 `Session` 上的对象
- 当前会话会跟踪它的属性变化

### 7.2 更新操作的标准思路

更新最常见的步骤是：

1. 先查到目标对象
2. 判断对象是否存在
3. 修改对象属性
4. 提交事务

这套思路后面放到员工项目里仍然完全适用。

## 八、删除数据

```python
from sqlalchemy import select  # 导入 select


with SessionLocal() as session:  # 创建数据库会话
    statement = select(User).where(User.user_code == "U100")  # 查询目标用户
    user = session.execute(statement).scalar_one_or_none()  # 读取一条数据

    if user is not None:  # 找到用户时才删除
        session.delete(user)  # 标记该对象为删除状态
        session.commit()  # 提交事务

        print("delete completed")  # delete completed
```

### 8.1 `delete()` 的作用

`delete()` 不是立刻删除数据库记录，而是先在当前事务中标记删除，最终是否生效仍取决于 `commit()`。

参数说明：

| 参数 | 作用 | 可接受的值 | 默认值或必填性 |
| --- | --- | --- | --- |
| `instance` | 把ORM对象标记为待删除 | 已映射的ORM对象实例 | 必填，可作为第一个位置参数 |

返回值：

- `None`

### 8.2 删除为什么也要先查

因为大多数业务删除都不是“盲删”。

通常都需要先确认：

- 这条数据是否存在
- 这条数据是否允许删除

所以项目代码中常见的删除流程基本都是：

1. 查询
2. 判断
3. 删除
4. 提交

## 九、事务方法要重点掌握

### 9.1 `flush()`

`flush()` 的作用是：

- 先把当前变更同步到数据库
- 但事务还没有最终提交

它常用于：

- 先拿到自增主键
- 后面还要继续做其他数据库操作

返回值：

- `None`

示例：

```python
with SessionLocal() as session:  # 创建数据库会话
    user = User(  # 创建新用户对象
        user_code="U200",  # 用户编号
        user_name="Yamada",  # 用户姓名
        department_name="IT",  # 部门名称
        email="yamada@example.com",  # 邮箱
    )  # 完成当前调用或数据结构

    session.add(user)  # 加入当前事务
    session.flush()  # 先同步 SQL，但不最终提交

    print(user.id)  # 例如：5

    session.commit()  # 最终提交事务
```

### 9.2 `rollback()`

`rollback()` 用于事务失败时撤销当前未完成的修改。

返回值：

- `None`

示例：

```python
with SessionLocal() as session:  # 创建数据库会话
    try:  # 开始执行可能失败的操作
        user = User(  # 创建故意违反唯一约束的对象
            user_code="U200",  # 重复使用上一段已经提交的用户编号
            user_name="Kobayashi",  # 用户姓名
            department_name="General Affairs",  # 部门名称
            email="kobayashi@example.com",  # 邮箱
        )  # 完成当前调用或数据结构

        session.add(user)  # 加入当前会话
        session.flush()  # 发送INSERT并触发唯一约束错误
        session.commit()  # 没有错误时才会执行提交

    except Exception as error:  # 发生异常时处理
        session.rollback()  # 回滚事务
        print(type(error).__name__)  # 只输出异常类型并确认进入回滚分支
```

这里故意重复`U200`，使`flush()`触发唯一约束异常。执行`rollback()`后，Session恢复可用状态，数据库中仍然只有前一段成功提交的一条`U200`。

### 9.3 `flush()` 和 `commit()` 的区别

| 方法 | 作用 |
| --- | --- |
| `flush()` | 同步 SQL，但事务还没最终提交 |
| `commit()` | 正式提交事务，让结果生效 |

### 9.4 `execute()` 的使用场景

虽然这一章重点是 ORM，但真正执行查询时，核心入口仍然是：

```python
session.execute(statement)  # execute既可以执行查询，也可以执行其他SQL表达式
```

`execute()` 的作用是：

- 执行查询语句
- 返回结果对象

参数通常是：

- `select()` 生成的查询语句
- 或 `text()` 生成的原始 SQL 对象

### 9.5 `one()`、`one_or_none()`、`first()`、`all()` 的基础认识

虽然本章主要使用的是 `all()` 和 `scalar_one_or_none()`，但你在项目里还会见到这些查询结果读取方式。

先建立一个基础认识：

| 方法 | 大致语义 |
| --- | --- |
| `all()` | 全部取出 |
| `first()` | 取第一条 |
| `one()` | 必须正好 1 条 |
| `one_or_none()` | 0 条或 1 条 |

当前阶段不用同时全部深入掌握，但要知道它们的语义差异，避免以后看到时完全陌生。

## 十、常用查询方法整理

| 写法 | 作用 |
| --- | --- |
| `select(User)` | 查询模型 |
| `.where(...)` | 添加筛选条件 |
| `.order_by(...)` | 指定排序 |
| `.scalars().all()` | 取全部模型对象 |
| `.scalar_one_or_none()` | 取一条或返回 `None` |
| `session.add(obj)` | 新增对象 |
| `session.delete(obj)` | 删除对象 |
| `session.flush()` | 先同步 SQL |
| `session.commit()` | 提交事务 |
| `session.rollback()` | 回滚事务 |
| `session.refresh(obj)` | 刷新对象最新状态 |

### 10.1 查询方法怎么选

新人阶段可以先用这个简单规则：

| 场景 | 推荐写法 |
| --- | --- |
| 查询列表 | `select(...).order_by(...)` + `scalars().all()` |
| 按唯一编号查 1 条 | `select(...).where(...)` + `scalar_one_or_none()` |
| 只想取单个统计值 | `scalar()` |

这样学习顺序最稳定，不容易一开始就被太多近似方法搞乱。

## 十一、ORM 对象和字典的区别

很多新人刚开始会把 ORM 对象和字典混用。

例如查询得到 `user` 之后，正确写法是：

```python
print(user.user_name)  # Tanaka
```

不是：

```python
print(user["user_name"])  # 字典通过字段名字符串读取数据
```

因为当前拿到的是 ORM 对象，不是字典。

## 十二、运行完整实验

确认第7章的`training_db`和练习账号已经准备完成，然后在FastAPI课程项目根目录执行：

```powershell
python examples/sqlalchemy_crud_demo.py
```

运行时可以观察到：

```text
新增U100并取得自增主键
把U100的部门修改为Finance
删除U100并输出delete completed
新增U200并在flush后取得主键
重复新增U200时输出IntegrityError并完成回滚
```

运行结束后再次查询`sqlalchemy_users_demo`，应只保留成功提交的`U200`。再次执行同一命令，脚本会先重建专用练习表，因此结果可以重复验证。

## 十三、常见错误

### 13.1 忘记 `commit()`

现象：

- 代码执行结束
- 数据库没有真正变化

### 13.2 查询结果是 `None` 还直接取属性

错误示例：

```python
print(user.user_name)  # ORM对象通过属性读取映射字段
```

如果没有查到数据，会报错。

### 13.3 把 `flush()` 当成最终提交

要明确：

- `flush()` 不是最终保存
- `commit()` 才是正式提交

## 十四、基础练习

1. 编写查询全部用户的代码。
2. 编写按 `user_code` 查询单条用户的代码。
3. 编写一段新增用户并打印主键的代码。

## 十五、综合练习

使用同一个练习模型完成完整流程：

1. 新增一条数据
2. 查询这条数据
3. 更新这条数据
4. 删除这条数据
5. 再写一个包含 `rollback()` 的事务示例

## 十六、本章总结

| 知识点 | 作用 |
| --- | --- |
| `select()` | 构造 ORM 查询语句 |
| `where()` | 添加筛选条件 |
| `add()` | 新增对象 |
| `delete()` | 删除对象 |
| `flush()` | 提前同步 SQL |
| `commit()` | 正式提交事务 |
| `rollback()` | 事务失败回滚 |
| `refresh()` | 读取数据库最新状态 |

下一章继续讲 Alembic，把数据库结构变更管理这件事单独讲清楚。
