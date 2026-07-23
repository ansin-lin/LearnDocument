# Repository 与 Service 分层

> 本章目标：把上一章已经写通的员工项目 CRUD 代码，整理成更符合真实 Web 项目的分层结构，让数据库访问、业务判断和后续接口代码各司其职。

## 一、为什么这一章要开始分层

如果把数据库查询、业务判断、事务提交全部堆在一个地方，项目很快就会出现这些问题：

1. 代码越来越长
2. 职责混乱
3. 重复逻辑难复用
4. 测试不方便
5. 接口层越来越重

所以这一章要做的事情不是“增加新语法”，而是把已经会写的 CRUD 代码放到更合理的位置。

## 二、先看分层后的项目结构

```text
employee_api/
├── app/
│   ├── db/
│   │   └── session.py
│   ├── models/
│   │   └── employee.py
│   ├── repositories/
│   │   └── employee_repository.py
│   └── services/
│       └── employee_service.py
```

这一章主要新增两个文件：

- `app/repositories/employee_repository.py`
- `app/services/employee_service.py`

### 2.1 为什么这里先只拆两层

因为当前阶段的重点是先建立最基础、最容易看懂的分层思路：

- 数据库访问放到 `Repository`
- 业务规则放到 `Service`

如果一开始就继续拆：

- DTO
- UseCase
- Domain Service
- Unit Of Work

对新人来说成本会太高。

## 三、先明确各层职责

### 3.1 Repository 负责什么

`Repository` 负责直接操作数据库，例如：

- 查询全部员工
- 按员工编号查询
- 新增员工对象
- 删除员工对象

一句话理解：

`Repository` 更靠近数据库。

`Repository` 中最常见的方法类型有：

- 列表查询方法
- 单条查询方法
- 新增方法
- 删除方法

它们的共同特点是：

- 直接接触 `Session`
- 直接接触 ORM 模型
- 直接写查询条件

### 3.2 Service 负责什么

`Service` 负责业务规则和事务边界，例如：

- 新增前先检查员工编号是否重复
- 决定什么时候 `commit()`
- 决定什么时候 `rollback()`
- 组合多个 Repository 操作

一句话理解：

`Service` 更靠近业务。

`Service` 中常见的方法类型有：

- 查询业务方法
- 新增业务方法
- 更新业务方法
- 删除业务方法

它们的共同特点是：

- 调用一个或多个 `Repository`
- 做业务规则判断
- 控制事务提交和回滚

## 四、先写 Repository

文件位置：

```text
app/repositories/employee_repository.py
```

```python
from sqlalchemy import select  # 导入 select，用于构造 ORM 查询语句
from sqlalchemy.orm import Session  # 导入 Session 类型

from app.models.employee import Employee  # 导入员工模型


class EmployeeRepository:  # 员工数据访问层
    def __init__(self, session: Session) -> None:
        self.session = session  # 保存当前数据库会话

    def find_all(self) -> list[Employee]:
        statement = select(Employee).order_by(Employee.id)  # 查询全部员工并按主键排序
        return self.session.execute(statement).scalars().all()  # 返回员工对象列表

    def find_by_employee_id(self, employee_id: str) -> Employee | None:
        statement = select(Employee).where(Employee.employee_id == employee_id)  # 按员工编号查询
        return self.session.execute(statement).scalar_one_or_none()  # 返回一条记录或 None

    def create(
        self,
        employee_id: str,
        employee_name: str,
        department_name: str,
        email: str | None,
    ) -> Employee:
        employee = Employee(  # 创建员工对象
            employee_id=employee_id,  # 员工编号
            employee_name=employee_name,  # 员工姓名
            department_name=department_name,  # 部门名称
            email=email,  # 邮箱
        )

        self.session.add(employee)  # 把对象加入当前会话
        self.session.flush()  # 先同步 SQL，方便后续读取主键
        return employee  # 返回 ORM 对象

    def delete(self, employee: Employee) -> None:
        self.session.delete(employee)  # 删除指定员工对象
```

### 4.1 `__init__(self, session: Session)` 为什么这样写

```python
def __init__(self, session: Session) -> None:
    self.session = session
```

这个写法表示：

- 创建 `Repository` 时，外部把当前数据库会话传进来
- 当前 `Repository` 后续所有数据库操作都使用这一个会话

这样做的好处是：

- 同一业务流程中的数据库操作可以放进同一个事务
- 后续 `Service` 更容易统一控制提交和回滚

### 4.2 `find_all()`、`find_by_employee_id()`、`create()`、`delete()` 的返回值

| 方法 | 返回值 | 说明 |
| --- | --- | --- |
| `find_all()` | `list[Employee]` | 返回员工对象列表 |
| `find_by_employee_id()` | `Employee | None` | 返回单条员工对象或 `None` |
| `create()` | `Employee` | 返回新建的员工对象 |
| `delete()` | `None` | 只负责标记删除，不负责返回业务结果 |

### 4.3 为什么 `Repository` 的返回值通常更贴近 ORM

因为它本来就是数据库访问层。

所以它更常返回：

- ORM 对象
- ORM 对象列表
- 或空值 `None`

而不是一开始就在这里决定 HTTP 返回格式。

## 五、为什么这些代码放在 Repository

因为这些代码的核心任务都属于“数据库访问”：

- 写查询条件
- 执行查询
- 创建 ORM 对象
- 删除 ORM 对象

这里还没有处理真正的业务判断，例如：

- 员工编号是否允许重复
- 删除前是否需要额外检查

这些判断不应该放在 `Repository`。

## 六、再写 Service

文件位置：

```text
app/services/employee_service.py
```

```python
from sqlalchemy.orm import Session  # 导入 Session 类型

from app.repositories.employee_repository import EmployeeRepository  # 导入员工 Repository


class EmployeeService:  # 员工业务层
    def __init__(self, session: Session) -> None:
        self.session = session  # 保存数据库会话
        self.employee_repository = EmployeeRepository(session)  # 创建员工 Repository

    def list_employees(self):
        return self.employee_repository.find_all()  # 查询全部员工

    def create_employee(
        self,
        employee_id: str,
        employee_name: str,
        department_name: str,
        email: str | None,
    ):
        existing_employee = self.employee_repository.find_by_employee_id(employee_id)  # 检查员工编号是否重复

        if existing_employee is not None:  # 已存在同编号员工时阻止新增
            raise ValueError("employee_id already exists")  # 当前先使用 ValueError 表示业务错误

        employee = self.employee_repository.create(  # 调用 Repository 执行新增
            employee_id=employee_id,  # 员工编号
            employee_name=employee_name,  # 员工姓名
            department_name=department_name,  # 部门名称
            email=email,  # 邮箱
        )

        self.session.commit()  # 业务成功后统一提交事务
        self.session.refresh(employee)  # 刷新对象最新状态
        return employee  # 返回新增成功后的员工对象

    def delete_employee(self, employee_id: str) -> bool:
        employee = self.employee_repository.find_by_employee_id(employee_id)  # 先查询目标员工

        if employee is None:  # 没找到时直接返回 False
            return False

        self.employee_repository.delete(employee)  # 调用 Repository 删除员工
        self.session.commit()  # 提交事务
        return True  # 返回删除成功
```

### 6.1 `Service` 方法的参数和返回值为什么要更偏业务

看 `create_employee()`：

- 参数不是整个 HTTP 请求对象
- 参数也不是原始数据库行
- 参数是业务上真正关心的员工信息

它的返回值是：

- 新增成功后的员工对象

再看 `delete_employee()`：

- 参数是业务编号 `employee_id`
- 返回值是 `bool`

这就体现出 `Service` 更关注“业务结果”，而不是只关注数据库动作本身。

### 6.2 为什么 `ValueError` 只是当前阶段的过渡写法

这里先用：

```python
raise ValueError("employee_id already exists")
```

是为了让学员先看懂：

- 业务校验失败时，需要主动阻止后续写入

后面的接口章节里，通常还会继续整理成：

- 项目异常类
- 统一异常处理
- 统一 HTTP 响应

## 七、为什么 `commit()` 放在 Service 更合理

这是这一章最关键的认识之一。

如果每个 `Repository` 方法都自己 `commit()`，会有这些问题：

1. 多步数据库操作没法组成一个完整事务
2. 业务层无法控制整体成功或整体失败
3. 事务边界容易混乱

更合理的分工是：

- `Repository` 负责数据库动作
- `Service` 负责业务规则和事务边界

这也是很多企业项目常见的写法。

## 八、再看一个完整调用示例

示例文件位置：

```text
examples/service_layer_demo.py
```

```python
from app.db.session import SessionLocal  # 导入项目 Session 工厂
from app.services.employee_service import EmployeeService  # 导入员工 Service


with SessionLocal() as session:  # 创建数据库会话
    employee_service = EmployeeService(session)  # 创建业务服务对象

    employee = employee_service.create_employee(  # 调用业务方法新增员工
        employee_id="E300",  # 员工编号
        employee_name="Kobayashi",  # 员工姓名
        department_name="General Affairs",  # 部门名称
        email="kobayashi@example.com",  # 邮箱
    )

    print(employee.employee_id)  # E300
    print(employee.employee_name)  # Kobayashi
```

### 8.1 这段代码的执行流向

```text
SessionLocal()
-> EmployeeService
-> EmployeeRepository
-> SQLAlchemy Session
-> MySQL
```

### 8.2 从调用顺序看每层职责

1. `SessionLocal()` 创建数据库会话
2. `EmployeeService` 负责业务流程组织
3. `EmployeeRepository` 负责数据库操作
4. `Service` 最终决定是否提交事务

### 8.3 为什么要把 `Session` 从外部传入

如果 `Repository` 和 `Service` 都各自内部偷偷创建 `Session`，会有几个问题：

1. 一个业务流程可能分裂成多个事务
2. 提交和回滚边界不清楚
3. 后续测试更难替换会话对象

所以项目中更常见的做法是：

- 由外部先创建 `Session`
- 再把同一个 `Session` 传给 `Service` 和 `Repository`

### 8.4 这一章最想建立的核心意识

这一章最重要的不是背类名，而是建立三个边界意识：

1. 数据库查询逻辑不要直接散落在接口层
2. 业务规则不要全部塞进 `Repository`
3. 事务提交位置要统一控制

## 九、这一章完成后，后面接口层会变成什么样

学完这一章后，后面的 FastAPI 路由层就不必自己直接写数据库操作。

它更适合做这些事情：

- 接收请求
- 解析参数
- 调用 `Service`
- 返回响应

这样接口代码会清楚很多。

## 十、常见错误

### 10.1 把所有业务判断都塞进 Repository

问题：

- 数据库层和业务层职责混在一起

### 10.2 在 Repository 内部直接 `commit()`

问题：

- 事务边界被切碎
- 后面多个操作不好统一控制

### 10.3 路由层直接写 SQLAlchemy 细节

问题：

- 接口函数越来越重
- 后面复用和测试都不方便

## 十一、基础练习

1. 编写 `EmployeeRepository.find_all()`。
2. 编写 `EmployeeRepository.find_by_employee_id()`。
3. 编写 `EmployeeService.list_employees()`。

## 十二、综合练习

完成一个分层版新增流程，要求：

1. 重复检查写在 `Service`
2. 数据新增写在 `Repository`
3. `commit()` 写在 `Service`
4. 新增成功后返回员工对象

## 十三、本章总结

| 层 | 主要职责 |
| --- | --- |
| `Repository` | 直接访问数据库 |
| `Service` | 处理业务规则和事务边界 |
| `Session` | 管理数据库操作上下文 |

完成这一章后，数据库项目落地这组内容就基本完整了。下一组开始进入 FastAPI 接口开发。
