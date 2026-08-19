# 第12章 EmployeeService、事务边界与DTO

> 本章成果：在第11章Repository之上建立EmployeeService，由Service完成业务检查、状态变化、提交与回滚，并明确ORM Model、Pydantic Schema和DTO的边界。

## 一、本章开始状态

本章直接复用：

- `app.database.SessionLocal`
- `app.models.Department`和`app.models.Employee`
- `app.repositories.employee_repository.EmployeeRepository`
- 第10章迁移和样例数据

新增并保留：

```text
app/
└── services/
    ├── __init__.py
    └── employee_service.py
```

## 二、三层职责

| 层 | 接收 | 负责 | 不负责 |
| --- | --- | --- | --- |
| Repository | `Session`和查询条件 | SQLAlchemy查询、关系加载、`add()`和`flush()` | HTTP状态码、业务事务提交 |
| Service | `Session`和业务参数 | 业务检查、对象状态变化、`commit()`和`rollback()` | URL、Header、JSON响应 |
| Router | HTTP请求 | 参数解析、调用Service、响应转换 | 直接编写复杂数据库查询 |

同一个Service方法表示一个完整业务用例，因此它是本项目的事务边界。Service可以连续调用多个Repository方法，全部成功后一次提交，任一步失败则统一回滚。

## 三、DTO、Schema与ORM Model

DTO（Data Transfer Object）泛指跨边界传递的数据结构。在当前FastAPI项目中，不需要再创建一套字段完全相同的`EmployeeDTO`：

| 对象 | 保存或传递的内容 | 主要用途 |
| --- | --- | --- |
| SQLAlchemy `Employee` | 数据库字段、关系和持久化状态 | 映射数据库表 |
| Pydantic请求Schema | 已校验的HTTP输入 | Router接收JSON请求体 |
| Pydantic响应Schema | 允许返回的接口字段 | 控制JSON响应结构 |
| Service参数和返回值 | 当前业务用例需要的数据 | 在业务层与调用方之间传递 |

FastAPI的Pydantic Schema已经承担接口边界DTO的主要职责。当前Service接收明确的业务参数并返回ORM对象，第13～14章由响应Schema把ORM对象转换为受控JSON。如果以后要求业务层完全脱离ORM，再考虑独立领域对象或DTO。

## 四、创建业务异常和Service

文件：`app/services/__init__.py`  
操作：新建空文件  
代码类型：项目包标记

文件：`app/services/employee_service.py`  
操作：新建文件  
代码类型：完整项目代码的第一部分

```python
from datetime import date  # 导入Service写入参数使用的日期类型

from sqlalchemy.orm import Session  # 导入Session类型

from app.models import Department, Employee  # 导入Service使用的ORM模型
from app.repositories.employee_repository import EmployeeRepository  # 导入数据库访问层


class EmployeeAlreadyExistsError(Exception):  # 表示员工编号已经存在
    pass  # 当前异常只使用父类保存错误参数


class EmployeeNotFoundError(Exception):  # 表示找不到在职员工
    pass  # 当前异常只使用父类保存错误参数


class DepartmentNotFoundError(Exception):  # 表示部门主键不存在
    pass  # 当前异常只使用父类保存错误参数


class EmployeeService:  # 集中实现员工业务用例和事务边界
    def __init__(self, session: Session) -> None:  # 接收当前业务使用的Session
        self.session = session  # 保存Session以控制事务
        self.repository = EmployeeRepository(session)  # 创建共享同一Session的Repository
```

业务异常继承Python的`Exception`，不依赖FastAPI的`HTTPException`。Service只表达“员工不存在”“员工重复”“部门不存在”等业务结果；第13～14章由HTTP边界决定对应状态码。

| 调用 | 参数 | 可接受的值 | 默认值或必填性 | 作用 |
| --- | --- | --- | --- | --- |
| `EmployeeService()` | `session` | SQLAlchemy `Session`对象 | 必填 | 建立本次业务操作使用的事务上下文 |
| `EmployeeAlreadyExistsError()` | `args` | 用于说明冲突的任意值 | 可省略 | 保存重复员工编号等错误信息 |
| `EmployeeNotFoundError()` | `args` | 用于说明缺失对象的任意值 | 可省略 | 保存未找到的员工编号 |
| `DepartmentNotFoundError()` | `args` | 用于说明缺失对象的任意值 | 可省略 | 保存未找到的部门主键 |

这些异常没有自定义`__init__()`，因此使用`Exception(*args)`的原有构造方式。本项目抛出异常时传入一个编号，后续HTTP层不把底层异常详情直接返回给客户端。

## 五、实现只读业务用例

文件：`app/services/employee_service.py`  
操作：追加到`EmployeeService`类内部  
代码类型：类内方法

```python
    def list_employees(  # 查询一页员工并返回总件数
        self,  # 当前Service对象
        keyword: str | None,  # 可选的姓名或员工编号关键字
        page: int,  # 从1开始的页码
        size: int,  # 每页数量
    ) -> tuple[list[Employee], int]:  # 返回员工列表和筛选总件数
        offset = (page - 1) * size  # 把页码转换为数据库偏移量
        items = self.repository.find_active(keyword, offset, size)  # 查询当前页
        total = self.repository.count_active(keyword)  # 查询相同条件下的总件数
        return items, total  # 把列表和总件数一起返回

    def get_employee(self, employee_number: str) -> Employee:  # 查询一名在职员工
        employee = self.repository.find_by_number(employee_number)  # 读取员工
        if employee is None:  # 判断员工是否不存在或已经离职
            raise EmployeeNotFoundError(employee_number)  # 抛出与HTTP无关的业务异常
        return employee  # 返回已经找到的员工对象

    def list_departments(self) -> list[Department]:  # 查询全部部门
        return self.repository.find_departments()  # 复用Repository的部门查询

    def _require_department(self, department_id: int) -> Department:  # 查询并确认部门存在
        department = self.repository.find_department(department_id)  # 按主键读取部门对象
        if department is None:  # 判断外键目标是否不存在
            raise DepartmentNotFoundError(department_id)  # 抛出部门不存在业务异常
        return department  # 返回可直接设置到员工关系属性的部门对象
```

| 方法 | 参数 | 可接受的值 | 默认值或必填性 | 返回值 |
| --- | --- | --- | --- | --- |
| `list_employees()` | `keyword` | 字符串或`None` | 必填，由Router显式传入 | `(员工列表, 总件数)`元组 |
| `list_employees()` | `page` | 大于或等于1的整数 | 必填 | 用于计算`offset` |
| `list_employees()` | `size` | 大于0的整数 | 必填 | 每页最多返回的员工数 |
| `get_employee()` | `employee_number` | 员工编号字符串 | 必填 | 员工对象；不存在时抛出业务异常 |
| `list_departments()` | 无 | 无 | 无 | 部门对象列表 |
| `_require_department()` | `department_id` | 部门主键整数 | 必填 | 部门对象；不存在时抛出业务异常 |

分页范围会在第13章由FastAPI依赖校验。Service仍然显式接收`page`和`size`，负责把业务分页含义转换为数据库偏移量。

## 六、新增员工事务

文件：`app/services/employee_service.py`  
操作：继续追加到`EmployeeService`类内部  
代码类型：类内方法

```python
    def create_employee(  # 新增一名员工
        self,  # 当前Service对象
        employee_number: str,  # 已校验格式的员工编号
        name: str,  # 员工姓名
        department_id: int,  # 已存在的部门主键
        email: str,  # 员工邮箱或空字符串
        joined_on: date,  # 入职日期
    ) -> Employee:  # 返回已经提交的员工对象
        exists = self.repository.employee_number_exists(employee_number)  # 检查所有记录中的编号
        if exists:  # 判断编号是否已经被在职或离职员工使用
            raise EmployeeAlreadyExistsError(employee_number)  # 抛出编号冲突异常
        department = self._require_department(department_id)  # 查询并确认部门外键目标存在

        employee = Employee(  # 创建待保存的员工ORM对象
            employee_number=employee_number,  # 设置员工编号
            name=name,  # 设置姓名
            department=department,  # 设置已经确认存在的部门关系对象
            email=email,  # 设置邮箱
            joined_on=joined_on,  # 设置入职日期
        )  # 完成员工对象

        try:  # 开始写事务
            self.repository.add(employee)  # 执行add和flush但不提交
            self.session.commit()  # 所有业务步骤成功后提交事务
            self.session.refresh(employee)  # 重新读取数据库最终状态
            return employee  # 返回已提交的员工
        except Exception:  # 捕获检查后发生的数据库写入异常
            self.session.rollback()  # 恢复失败事务中的Session
            raise  # 把原异常继续交给上层处理
```

应用层的编号检查可以提供清晰业务错误，但不能替代数据库唯一约束。两个并发请求仍可能同时通过预检查，最终由数据库拒绝其中一个；无论失败原因是什么，Service都必须先回滚。

## 七、修改与逻辑删除事务

文件：`app/services/employee_service.py`  
操作：继续追加到`EmployeeService`类内部  
代码类型：类内方法

```python
    def update_employee(  # 修改一名在职员工
        self,  # 当前Service对象
        employee_number: str,  # 路径中不可修改的员工编号
        name: str,  # 新姓名
        department_id: int,  # 新部门主键
        email: str,  # 新邮箱或空字符串
        joined_on: date,  # 新入职日期
    ) -> Employee:  # 返回修改后的员工对象
        employee = self.get_employee(employee_number)  # 查询目标员工并处理不存在情况
        department = self._require_department(department_id)  # 查询并确认新部门存在
        employee.name = name  # 修改持久化对象的姓名
        employee.department = department  # 修改关系对象并同步对应外键
        employee.email = email  # 修改邮箱
        employee.joined_on = joined_on  # 修改入职日期

        try:  # 开始修改事务
            self.session.commit()  # 提交SQLAlchemy检测到的属性变化
            self.session.refresh(employee)  # 重新读取数据库最终状态
            return employee  # 返回修改后的员工
        except Exception:  # 捕获数据库写入异常
            self.session.rollback()  # 回滚失败事务
            raise  # 继续抛出原异常

    def deactivate_employee(self, employee_number: str) -> None:  # 逻辑删除员工
        employee = self.get_employee(employee_number)  # 查询当前在职员工
        employee.is_active = False  # 把在职状态改为False而不删除记录
        try:  # 开始状态变更事务
            self.session.commit()  # 提交逻辑删除状态
        except Exception:  # 捕获数据库写入异常
            self.session.rollback()  # 回滚失败事务
            raise  # 继续抛出原异常
```

由当前Session查询出的员工对象处于持久化状态，修改属性后不需要再次调用`add()`。逻辑删除只修改`is_active`；默认列表和详情查询会排除该记录，但数据库仍然保留原始员工数据。

## 八、独立验证Service

文件：`app/service_demo.py`  
操作：新建临时实验文件  
代码类型：可删除的Service验证

```python
from datetime import date  # 导入新增员工使用的日期类型

from app.database import SessionLocal  # 导入Session工厂
from app.services.employee_service import EmployeeService  # 导入业务服务


with SessionLocal() as session:  # 创建并自动关闭验证用Session
    service = EmployeeService(session)  # 创建共享当前Session的Service
    employee = service.create_employee(  # 执行完整新增事务
        employee_number="E010",  # 设置练习员工编号
        name="Suzuki",  # 设置练习姓名
        department_id=1,  # 使用现有部门主键
        email="suzuki@example.test",  # 设置练习邮箱
        joined_on=date(2026, 4, 1),  # 设置练习入职日期
    )  # 完成新增调用
    print(employee.employee_number, employee.name)  # 输出提交后的员工
    service.deactivate_employee("E010")  # 执行逻辑删除事务
```

执行：

```powershell
python -m app.service_demo
```

第一次执行会新增并输出`E010 Suzuki`，随后把该员工设置为离职。同一编号受数据库唯一约束保护，不要重复运行创建步骤；需要再次练习时更换编号。验证后删除`app/service_demo.py`，正式项目只保留Service文件。

## 九、事务调用链

新增员工时的职责顺序为：

```text
调用方传入业务参数
→ Service检查编号和部门
→ Repository执行add()与flush()
→ Service提交整个业务事务
→ 失败时Service回滚并继续抛出异常
```

查询方法不提交事务；写方法的成功路径调用`commit()`，数据库写入异常路径调用`rollback()`。业务检查发生在写入前，不需要为了普通的“未找到”结果调用回滚。

## 十、常见错误

| 错误 | 后果 | 修正 |
| --- | --- | --- |
| Repository自行`commit()` | 多步骤业务无法整体回滚 | 由Service决定事务边界 |
| Service抛出`HTTPException` | 业务层绑定FastAPI | 抛出业务异常，由HTTP层转换 |
| Service重新编写`select()` | 数据访问分散到多层 | 通过Repository查询 |
| 删除调用`session.delete()` | 违反项目逻辑删除规则 | 修改`is_active` |
| 捕获写入异常后不回滚 | Session可能无法继续使用 | `rollback()`后重新抛出 |

## 十一、动手任务

1. 使用Service查询员工列表、详情和部门列表。
2. 新增练习员工并确认生成数据库主键。
3. 修改练习员工的姓名和部门，再次查询确认变化。
4. 逻辑删除练习员工，确认默认详情抛出`EmployeeNotFoundError`。
5. 使用不存在的部门主键新增员工，确认事务没有保存员工。
6. 使用重复员工编号测试数据库唯一约束，确认失败后Session可以继续查询。

## 十二、完成检查

- [ ] Repository和Service没有重复实现查询。
- [ ] Service写操作具有清晰的提交与回滚路径。
- [ ] 业务异常不依赖FastAPI。
- [ ] 逻辑删除符合项目规格。
- [ ] DTO是边界概念，没有额外建立字段重复的第三套类。
- [ ] 临时`service_demo.py`已经删除。

完成后保留`EmployeeRepository`和`EmployeeService`。第13章只需为请求创建Session并调用Service，不再重新实现数据库CRUD。
