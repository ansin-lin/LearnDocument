# 第7章 项目中的 CRUD 与事务控制

> 本章目标：基于项目里的 `SessionLocal` 和 `Employee` 模型，正式编写员工管理项目中的查询、新增、更新、删除和事务控制代码。

## 一、本章使用的已有文件

进入这一章前，项目中至少已经有这些文件：

- `app/config.py`
- `app/db/base.py`
- `app/db/session.py`
- `app/models/employee.py`

这一章先把项目级 CRUD 练习代码统一写在：

```text
examples/project_crud_demo.py
```

先集中练明白，再在下一章拆到 `Repository` 和 `Service`。

### 1.1 为什么这一章还不马上拆分层

因为如果学员连最基本的 CRUD 都还没真正看明白，就直接进入：

- `Repository`
- `Service`
- 依赖传递

反而容易只记住结构名词，没搞懂数据库代码本身。

所以这一章先把“项目里最基础的数据库操作”练熟，再进入下一章分层。

## 二、查询全部员工

文件位置：

```text
examples/project_crud_demo.py
```

```python
from sqlalchemy import select  # 导入 select，用于构造 ORM 查询语句

from app.db.session import SessionLocal  # 导入项目 Session 工厂
from app.models.employee import Employee  # 导入员工模型


with SessionLocal() as session:  # 创建数据库会话
    statement = select(Employee).order_by(Employee.id)  # 查询全部员工并按主键排序
    employees = session.execute(statement).scalars().all()  # 执行查询并取出员工对象列表

    for employee in employees:  # 遍历员工对象
        print(employee.employee_id, employee.employee_name)  # E001 Tanaka
```

### 2.1 这里为什么直接使用 `Employee`

因为项目里已经有正式模型类了。

这一章开始不再用练习用 `User` 模型，而是直接进入员工项目数据。

### 2.2 `scalars().all()` 返回的是什么

返回的是：

- `list[Employee]`

也就是说，结果中的每一项都是员工对象，不是字典。

适用场景：

- 员工列表页
- 部门列表页
- 基础主数据列表查询

### 2.3 为什么查询全部时常加 `order_by()`

因为如果不写排序，数据库返回顺序在很多场景下并不稳定。

项目中为了让结果可预期，常见做法是主动指定排序字段。

## 三、按员工编号查询一条员工

文件位置：

```text
examples/project_crud_demo.py
```

```python
from sqlalchemy import select  # 导入 select

from app.db.session import SessionLocal  # 导入项目 Session 工厂
from app.models.employee import Employee  # 导入员工模型


with SessionLocal() as session:  # 创建数据库会话
    statement = select(Employee).where(Employee.employee_id == "E001")  # 按员工编号筛选
    employee = session.execute(statement).scalar_one_or_none()  # 取一条记录或 None

    if employee is not None:  # 找到员工时输出姓名
        print(employee.employee_name)  # Tanaka
```

### 3.1 为什么员工编号适合用来查一条

因为 `employee_id` 在表设计中是唯一字段。

所以这个查询非常适合配合：

```python
scalar_one_or_none()
```

返回值说明：

| 情况 | 返回结果 |
| --- | --- |
| 查到 1 条 | `Employee` 对象 |
| 查到 0 条 | `None` |
| 查到多条 | 抛出异常 |

## 四、新增员工

文件位置：

```text
examples/project_crud_demo.py
```

```python
from app.db.session import SessionLocal  # 导入项目 Session 工厂
from app.models.employee import Employee  # 导入员工模型


with SessionLocal() as session:  # 创建数据库会话
    employee = Employee(  # 创建新的员工对象
        employee_id="E100",  # 员工编号
        employee_name="Yamada",  # 员工姓名
        department_name="Finance",  # 部门名称
        email="yamada@example.com",  # 邮箱
    )

    session.add(employee)  # 把员工对象加入当前会话
    session.commit()  # 提交事务
    session.refresh(employee)  # 刷新对象，读取数据库中的最新值

    print(employee.id)  # 例如：4
    print(employee.employee_id)  # E100
```

### 4.1 每一行代码分别在做什么

| 代码 | 作用 |
| --- | --- |
| `Employee(...)` | 先在 Python 中创建员工对象 |
| `session.add(employee)` | 把对象加入当前事务 |
| `session.commit()` | 正式提交到数据库 |
| `session.refresh(employee)` | 重新读取最新状态 |

### 4.2 新增时这些方法的参数和返回值

| 方法 | 参数 | 返回值 | 说明 |
| --- | --- | --- | --- |
| `session.add(employee)` | ORM 对象 | `None` | 把对象加入当前事务 |
| `session.commit()` | 无 | `None` | 正式提交事务 |
| `session.refresh(employee)` | ORM 对象 | `None` | 重新同步数据库最新状态 |

### 4.3 为什么新增后常常要 `refresh()`

因为像下面这些值，往往是在数据库真正写入后才最终确定：

- 自增主键
- 默认时间
- 数据库自动更新值

### 4.4 新增操作在项目里的典型使用场景

最常见的就是：

- 员工新增
- 部门新增
- 用户注册
- 主数据录入

## 五、更新员工

文件位置：

```text
examples/project_crud_demo.py
```

```python
from sqlalchemy import select  # 导入 select

from app.db.session import SessionLocal  # 导入项目 Session 工厂
from app.models.employee import Employee  # 导入员工模型


with SessionLocal() as session:  # 创建数据库会话
    statement = select(Employee).where(Employee.employee_id == "E100")  # 查询目标员工
    employee = session.execute(statement).scalar_one_or_none()  # 取一条员工记录

    if employee is not None:  # 找到员工时才更新
        employee.department_name = "General Affairs"  # 修改部门名称
        employee.email = "yamada.ga@example.com"  # 修改邮箱
        session.commit()  # 提交事务

        print(employee.department_name)  # General Affairs
```

### 5.1 为什么更新时不用重新 `add()`

因为这个对象是从当前 `Session` 查询出来的，已经处于当前会话管理之中。

所以：

1. 修改属性
2. `commit()`

就够了。

更新场景中最常见的顺序是：

1. 先查询目标对象
2. 修改对象属性
3. `commit()` 提交事务

## 六、删除员工

文件位置：

```text
examples/project_crud_demo.py
```

```python
from sqlalchemy import select  # 导入 select

from app.db.session import SessionLocal  # 导入项目 Session 工厂
from app.models.employee import Employee  # 导入员工模型


with SessionLocal() as session:  # 创建数据库会话
    statement = select(Employee).where(Employee.employee_id == "E100")  # 查询目标员工
    employee = session.execute(statement).scalar_one_or_none()  # 取一条员工记录

    if employee is not None:  # 找到员工时才删除
        session.delete(employee)  # 标记当前对象为删除状态
        session.commit()  # 提交事务

        print("delete completed")  # delete completed
```

### 6.1 删除操作的要点

删除时最常见的顺序是：

1. 先查询目标对象
2. 确认对象存在
3. `session.delete()` 标记删除
4. `session.commit()` 正式提交

这样写的原因是：

- 避免删除不存在的数据
- 便于删除前先做业务判断

## 七、项目中的事务控制示例

在真实项目中，一次业务操作往往不止一条 SQL。

所以新人必须真正理解：

- `flush()`
- `commit()`
- `rollback()`

文件位置：

```text
examples/project_crud_demo.py
```

```python
from app.db.session import SessionLocal  # 导入项目 Session 工厂
from app.models.employee import Employee  # 导入员工模型


with SessionLocal() as session:  # 创建数据库会话
    try:
        employee = Employee(  # 创建员工对象
            employee_id="E200",  # 员工编号
            employee_name="Kobayashi",  # 员工姓名
            department_name="IT",  # 部门名称
            email="kobayashi@example.com",  # 邮箱
        )

        session.add(employee)  # 把对象加入当前会话
        session.flush()  # 先同步 SQL，但事务还没有最终提交

        print(employee.id)  # 例如：5

        session.commit()  # 全部成功后正式提交事务

    except Exception as error:  # 出现异常时处理
        session.rollback()  # 回滚当前事务
        print(error)  # 输出异常信息
```

### 7.1 这段代码的执行顺序

1. 创建 `Employee` 对象
2. `add()` 把对象加入当前事务
3. `flush()` 先把 SQL 发给数据库
4. 拿到自增主键等结果
5. 一切正常时 `commit()`
6. 出错时 `rollback()`

### 7.2 项目里为什么不能只会 `commit()`

因为业务代码经常会发生：

- 唯一约束冲突
- 数据不合法
- 中途抛异常
- 多步数据库操作失败

如果不会处理 `rollback()`，项目数据就容易出现不一致问题。

### 7.3 `flush()`、`commit()`、`rollback()` 在项目里的常见场景

| 方法 | 常见场景 |
| --- | --- |
| `flush()` | 先拿到主键，再继续后续数据库操作 |
| `commit()` | 整个业务流程确认成功后正式保存 |
| `rollback()` | 中途出错时撤销本次事务 |

### 7.4 为什么事务控制是项目章的重点

因为基础查询只是“读数据”，而真实项目更危险的是“改数据”。

一旦涉及：

- 新增
- 更新
- 删除

就必须考虑事务是否完整、失败后是否回滚。

## 八、项目代码中的几个关键认识

### 8.1 现在的代码还不是最终分层结构

这一章故意先把 CRUD 代码集中写到练习脚本，是为了让学员先把数据库操作本身练熟。

### 8.2 下一章才会拆分为项目结构

下一章会把这些逻辑拆到：

- `Repository`
- `Service`

这样后面的 FastAPI 路由层就不会直接贴数据库细节。

## 九、常见错误

### 9.1 忘记 `commit()`

现象：

- 代码执行结束
- 数据库没有新增、更新、删除结果

### 9.2 查询结果为 `None` 还直接访问属性

错误示例：

```python
print(employee.employee_name)
```

如果没查到员工，会报错。

### 9.3 把 ORM 对象当成字典

错误示例：

```python
print(employee["employee_name"])
```

正确写法应该是：

```python
print(employee.employee_name)
```

### 9.4 把 `flush()` 当成最终保存

要明确：

- `flush()` 只是先同步 SQL
- `commit()` 才是最终提交

## 十、基础练习

1. 查询全部员工。
2. 按员工编号查询单条员工。
3. 新增一条员工数据。
4. 更新一条员工数据。
5. 删除一条员工数据。

## 十一、综合练习

在 `examples/project_crud_demo.py` 中完成完整流程：

1. 查询全部员工
2. 新增一名员工
3. 更新这名员工的部门
4. 删除这名员工
5. 再编写一个包含 `rollback()` 的事务示例

## 十二、本章总结

| 操作 | 常用写法 |
| --- | --- |
| 查询全部 | `select()`、`order_by()`、`scalars().all()` |
| 查询单条 | `where()`、`scalar_one_or_none()` |
| 新增 | `add()`、`commit()`、`refresh()` |
| 更新 | 查询后修改属性，再 `commit()` |
| 删除 | `delete()`、`commit()` |
| 事务 | `flush()`、`commit()`、`rollback()` |

下一章继续把这些项目 CRUD 代码拆分到 `Repository` 和 `Service` 层。
