# 第11章 EmployeeRepository与数据库CRUD

> 本章成果：在员工项目中建立可继续复用的`EmployeeRepository`，集中保存员工和部门的SQLAlchemy查询，并通过一次小型事务实验确认Repository不负责提交事务。

## 一、本章开始状态

本章沿用第10章完成的`Department`、`Employee`、`SessionLocal`、Alembic迁移和样例数据。先在项目根目录确认数据库状态：

```powershell
alembic current
python -m app.seed
```

本章新增并保留下面的项目文件：

```text
app/
└── repositories/
    ├── __init__.py
    └── employee_repository.py
```

Repository负责数据库访问，不负责HTTP请求，也不决定一个业务用例何时提交。第12章的Service会直接复用本章文件，因此不再先建立一套临时CRUD函数、下一章再重写。

## 二、创建Repository包

文件：`app/repositories/__init__.py`  
操作：新建空文件  
代码类型：项目包标记

空的`__init__.py`让`repositories`成为可以从项目代码中导入的Python包。

## 三、实现员工查询

文件：`app/repositories/employee_repository.py`  
操作：新建文件  
代码类型：完整项目代码

```python
from sqlalchemy import func, or_, select  # 导入统计、条件组合和查询构造工具
from sqlalchemy.orm import Session, selectinload  # 导入Session类型和关系预加载工具

from app.models import Department, Employee  # 导入项目的部门和员工ORM模型


class EmployeeRepository:  # 集中保存员工项目的数据库访问代码
    def __init__(self, session: Session) -> None:  # 接收由外部创建的Session
        self.session = session  # 保存Session，但不在Repository中关闭它

    def find_active(  # 查询一页在职员工
        self,  # 当前Repository对象
        keyword: str | None = None,  # 可选的员工编号或姓名关键字
        offset: int = 0,  # 从第几条结果开始读取
        limit: int = 20,  # 最多读取多少条结果
    ) -> list[Employee]:  # 返回员工ORM对象列表
        statement = (  # 开始构造员工查询
            select(Employee)  # 选择Employee模型
            .options(selectinload(Employee.department))  # 一并预加载所属部门
            .where(Employee.is_active.is_(True))  # 只查询在职员工
            .order_by(Employee.employee_number)  # 按员工编号稳定排序
            .offset(offset)  # 跳过前面的结果
            .limit(limit)  # 限制本次返回数量
        )  # 完成基础查询
        if keyword:  # 只有提供关键字时才追加筛选条件
            statement = statement.where(  # 向原查询追加WHERE条件
                or_(  # 编号或姓名满足任一条件即可
                    Employee.employee_number.contains(keyword),  # 员工编号包含关键字
                    Employee.name.contains(keyword),  # 员工姓名包含关键字
                )  # 完成OR条件
            )  # 完成关键字筛选
        result = self.session.execute(statement)  # 使用当前Session执行查询
        return list(result.scalars().all())  # 取得全部Employee对象并转换为列表

    def find_by_number(  # 按员工编号查询一名在职员工
        self,  # 当前Repository对象
        employee_number: str,  # 唯一员工编号
    ) -> Employee | None:  # 返回员工对象或None
        statement = (  # 开始构造单条查询
            select(Employee)  # 选择Employee模型
            .options(selectinload(Employee.department))  # 预加载所属部门
            .where(  # 同时应用编号和在职条件
                Employee.employee_number == employee_number,  # 匹配员工编号
                Employee.is_active.is_(True),  # 排除已经逻辑删除的员工
            )  # 完成筛选条件
        )  # 完成查询语句
        result = self.session.execute(statement)  # 执行单条员工查询
        return result.scalar_one_or_none()  # 返回唯一员工或None

    def employee_number_exists(self, employee_number: str) -> bool:  # 检查编号是否已被使用
        statement = select(Employee.id).where(  # 只查询匹配记录的主键
            Employee.employee_number == employee_number  # 匹配员工编号且不排除离职记录
        )  # 完成编号查询
        result = self.session.execute(statement)  # 执行编号存在性查询
        return result.scalar_one_or_none() is not None  # 存在记录时返回True

    def count_active(self, keyword: str | None = None) -> int:  # 统计筛选后的在职员工数
        statement = select(func.count(Employee.id)).where(  # 构造COUNT查询
            Employee.is_active.is_(True)  # 只统计在职员工
        )  # 完成基础统计条件
        if keyword:  # 提供关键字时同步应用列表筛选规则
            statement = statement.where(  # 追加关键字条件
                or_(  # 编号或姓名命中即可
                    Employee.employee_number.contains(keyword),  # 匹配员工编号
                    Employee.name.contains(keyword),  # 匹配员工姓名
                )  # 完成OR条件
            )  # 完成筛选
        result = self.session.execute(statement)  # 执行统计查询
        return result.scalar_one()  # 返回唯一的整数统计值

    def find_departments(self) -> list[Department]:  # 查询全部部门
        statement = select(Department).order_by(Department.id)  # 按主键构造部门查询
        result = self.session.execute(statement)  # 执行部门查询
        return list(result.scalars().all())  # 返回部门对象列表
```

`__init__(session)`要求调用方传入已经创建的`Session`。Repository不会自行创建Session，因此同一个业务用例中的多个数据库操作可以共享同一事务。

主要方法参数：

| 方法 | 参数 | 可接受的值 | 默认值或必填性 | 作用 |
| --- | --- | --- | --- | --- |
| `EmployeeRepository()` | `session` | SQLAlchemy `Session`对象 | 必填 | 提供当前数据库会话 |
| `find_active()` | `keyword` | 字符串或`None` | 默认`None` | 按员工编号或姓名进行包含查询 |
| `find_active()` | `offset` | 大于或等于0的整数 | 默认`0` | 跳过前面的查询结果 |
| `find_active()` | `limit` | 大于0的整数 | 默认`20` | 限制返回条数 |
| `find_by_number()` | `employee_number` | 员工编号字符串 | 必填 | 查询一名在职员工 |
| `employee_number_exists()` | `employee_number` | 员工编号字符串 | 必填 | 检查编号是否已被任何记录使用 |
| `count_active()` | `keyword` | 字符串或`None` | 默认`None` | 统计与列表相同条件下的总件数 |

`selectinload(Employee.department)`会提前读取本页员工对应的部门，避免之后访问`employee.department.name`时为每名员工分别查询一次部门。

## 四、实现新增所需的数据库操作

文件：`app/repositories/employee_repository.py`  
操作：追加到`EmployeeRepository`类内部  
代码类型：类内方法

```python
    def add(self, employee: Employee) -> Employee:  # 把新员工加入当前事务
        self.session.add(employee)  # 将ORM对象加入Session
        self.session.flush()  # 执行待处理SQL并取得数据库生成值
        return employee  # 返回仍属于当前Session的员工对象

    def find_department(self, department_id: int) -> Department | None:  # 按主键查询一个部门
        return self.session.get(Department, department_id)  # 返回部门对象，不存在时返回None
```

| 方法 | 参数 | 可接受的值 | 默认值或必填性 | 返回值与作用 |
| --- | --- | --- | --- | --- |
| `add()` | `employee` | 尚未持久化的`Employee`对象 | 必填 | 执行`add()`和`flush()`，返回员工对象 |
| `find_department()` | `department_id` | 部门主键整数 | 必填 | 返回部门对象，不存在时返回`None` |

`Session.get(Department, department_id)`按照主键读取部门。Service既可以用返回值判断部门是否存在，也可以把已经查询出的部门对象直接设置到`employee.department`关系属性，避免只修改外键后仍读取到Session中旧的关系对象。

`flush()`会把SQL发送到数据库，使员工对象可以取得数据库生成的主键，但不会最终提交事务。Repository不调用`commit()`或`rollback()`；第12章由Service根据整个业务用例的结果统一决定。

修改和逻辑删除不需要额外的Repository方法。由当前Session查询出的员工对象已经处于持久化状态，Service修改对象属性后，SQLAlchemy会在提交时同步变化。

## 五、验证查询结果

在项目根目录执行：

```powershell
python -c "from app.database import SessionLocal; from app.repositories.employee_repository import EmployeeRepository; session = SessionLocal(); repository = EmployeeRepository(session); print([(item.employee_number, item.department.name) for item in repository.find_active()]); session.close()"
```

预期至少包含：

```text
('E001', '开发部')
('E002', '营业部')
```

再验证关键字和统计使用相同筛选规则：

```powershell
python -c "from app.database import SessionLocal; from app.repositories.employee_repository import EmployeeRepository; session = SessionLocal(); repository = EmployeeRepository(session); print(repository.count_active('山田'), [item.employee_number for item in repository.find_active('山田')]); session.close()"
```

预期输出总件数`1`和员工编号`E001`。

## 六、用一次实验观察事务边界

文件：`app/repository_demo.py`  
操作：新建临时实验文件  
代码类型：可删除的事务实验

```python
from datetime import date  # 导入创建员工使用的日期类型

from app.database import SessionLocal  # 导入Session工厂
from app.models import Employee  # 导入员工ORM模型
from app.repositories.employee_repository import EmployeeRepository  # 导入刚完成的Repository


with SessionLocal() as session:  # 创建并在实验结束时关闭Session
    repository = EmployeeRepository(session)  # 让Repository使用当前Session
    employee = Employee(  # 创建尚未保存的员工对象
        employee_number="E099",  # 使用实验员工编号
        name="Transaction Test",  # 设置实验姓名
        department_id=1,  # 使用已经存在的部门主键
        email="transaction@example.test",  # 设置实验邮箱
        joined_on=date(2026, 4, 1),  # 设置入职日期
    )  # 完成员工对象
    try:  # 开始控制实验事务
        repository.add(employee)  # 执行INSERT但不在Repository中提交
        print(employee.id)  # flush后可以看到数据库生成的主键
        session.rollback()  # 主动回滚本次实验事务
    except Exception:  # 捕获数据库写入异常
        session.rollback()  # 先恢复Session可用状态
        raise  # 继续向外抛出原异常
```

执行：

```powershell
python -m app.repository_demo
```

脚本会打印`flush()`后取得的主键，但回滚后再次查询不会找到`E099`。这说明“已经发送INSERT”和“事务已经提交”不是同一件事。

验证完成后删除`app/repository_demo.py`。正式项目只保留`employee_repository.py`，避免把实验入口误当作应用代码。

## 七、常见错误

| 现象 | 原因 | 修正 |
| --- | --- | --- |
| Repository调用`commit()` | 数据库访问层提前结束事务 | 只执行查询、`add()`和必要的`flush()` |
| 列表和统计件数不一致 | 两个方法使用了不同筛选条件 | 让`find_active()`和`count_active()`保持相同规则 |
| 遍历员工时反复查询部门 | 没有为响应需要的关系设置加载策略 | 使用`selectinload(Employee.department)` |
| 逻辑删除后仍能查到员工 | 查询缺少`is_active=True`条件 | 在默认列表和详情中统一排除离职员工 |
| 写入失败后Session不可继续使用 | 没有回滚失败事务 | 由负责事务的上层调用`rollback()` |

## 八、动手任务

1. 使用`find_active()`查询全部在职员工并输出部门名。
2. 分别用姓名和员工编号作为关键字，核对列表长度与`count_active()`。
3. 使用`offset=1, limit=1`验证分页结果。
4. 查询不存在的员工编号，确认返回`None`而不是异常。
5. 执行回滚实验，确认`E099`没有保存在数据库中。

## 九、完成检查

- [ ] 项目只保留一套`EmployeeRepository`。
- [ ] 员工列表、详情、编号检查、统计和部门查询都集中在Repository。
- [ ] Repository不创建、关闭或提交Session。
- [ ] `add()`只执行`flush()`，事务由上层决定。
- [ ] 列表和统计使用相同的在职与关键字规则。
- [ ] 临时`repository_demo.py`已经删除。

完成后保留`app/repositories/employee_repository.py`。第12章直接在它上面建立Service和完整业务事务。
