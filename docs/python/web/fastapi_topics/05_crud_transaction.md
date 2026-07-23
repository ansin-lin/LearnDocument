# 第5章 FastAPI CRUD 与事务

> 本章目标：掌握使用 SQLAlchemy Session 完成新增、查询、修改、删除和事务控制，理解 `commit()`、`rollback()`、`refresh()` 的作用。

## 一、CRUD 是什么

CRUD 是企业项目中最常见的数据操作。

| 操作 | 英文 | 接口场景 |
| --- | --- | --- |
| 新增 | Create | 新增员工 |
| 查询 | Read | 员工列表、员工详情 |
| 修改 | Update | 编辑员工 |
| 删除 | Delete | 删除员工或设置离职 |

## 二、新增数据

```python
from datetime import date  # 导入 date，用于设置入职日期
from app.database import SessionLocal  # 导入 Session 工厂
from app.models import Employee  # 导入员工模型

db = SessionLocal()  # 创建数据库 Session
try:  # 开始数据库操作
    employee = Employee(  # 创建员工 ORM 对象
        employee_code="E001",  # 设置员工编号
        name="Tanaka",  # 设置员工姓名
        email="tanaka@example.com",  # 设置邮箱
        joined_date=date(2026, 4, 1),  # 设置入职日期
        department_id=1,  # 设置所属部门 ID
    )
    db.add(employee)  # 把员工对象加入 Session
    db.commit()  # 提交事务，真正写入数据库
    db.refresh(employee)  # 从数据库刷新对象，取得自动生成的 ID
    print(employee.id)  # 输出员工 ID
except Exception:  # 捕获异常
    db.rollback()  # 出错时回滚事务
    raise  # 继续抛出异常，方便上层处理
finally:  # 最终处理
    db.close()  # 关闭 Session
```

## 三、查询数据

SQLAlchemy 2.x 推荐使用 `select()`。

```python
from sqlalchemy import select  # 导入 select，用于构建查询
from app.database import SessionLocal  # 导入 Session 工厂
from app.models import Employee  # 导入员工模型

db = SessionLocal()  # 创建数据库 Session
try:  # 开始数据库操作
    statement = select(Employee).where(Employee.is_active == True)  # 构建查询语句，查询在职员工
    employees = db.execute(statement).scalars().all()  # 执行查询并取得 Employee 对象列表
    print(employees)  # 输出查询结果
finally:  # 最终处理
    db.close()  # 关闭 Session
```

返回值说明：

| 写法 | 作用 |
| --- | --- |
| `db.execute(statement)` | 执行 SQLAlchemy 查询 |
| `.scalars()` | 取得 ORM 对象结果 |
| `.all()` | 取得全部结果列表 |
| `.first()` | 取得第一条，可能为 `None` |

## 四、修改数据

```python
from sqlalchemy import select  # 导入 select
from app.database import SessionLocal  # 导入 Session 工厂
from app.models import Employee  # 导入员工模型

db = SessionLocal()  # 创建 Session
try:  # 开始事务
    statement = select(Employee).where(Employee.employee_code == "E001")  # 按员工编号查询
    employee = db.execute(statement).scalars().first()  # 取得第一条员工数据

    if employee:  # 判断员工是否存在
        employee.email = "new-tanaka@example.com"  # 修改邮箱
        db.commit()  # 提交事务
        db.refresh(employee)  # 刷新对象
except Exception:  # 捕获异常
    db.rollback()  # 回滚事务
    raise  # 继续抛出异常
finally:  # 最终处理
    db.close()  # 关闭 Session
```

## 五、删除数据

重要业务数据通常不建议直接物理删除，可以先做逻辑删除。

```python
employee.is_active = False  # 设置员工为非在职状态
db.commit()  # 提交状态变更
```

物理删除写法：

```python
db.delete(employee)  # 标记删除员工对象
db.commit()  # 提交后真正删除数据库记录
```

## 六、事务控制

事务保证一组数据库操作要么全部成功，要么全部失败。

| 方法 | 状态变化 |
| --- | --- |
| `add()` | 加入待保存对象 |
| `flush()` | 把变更发送到数据库，但不最终提交 |
| `commit()` | 提交事务 |
| `rollback()` | 回滚未提交变更 |
| `refresh()` | 用数据库最新值刷新对象 |
| `close()` | 关闭 Session |

事务边界应由业务决定。不要在底层方法中随意隐藏 `commit()`，否则上层无法控制多个操作的一致性。

## 七、在 FastAPI 中使用

下一章会把数据库 Session 做成依赖。

当前先理解最小流程：

```text
创建 Session
-> 执行查询或写入
-> 成功 commit
-> 失败 rollback
-> 最后 close
```

## 八、基础练习

请完成：

1. 新增一名员工
2. 查询所有在职员工
3. 修改员工邮箱
4. 将员工设置为离职
5. 故意制造错误并确认 `rollback()` 被执行

## 九、本章总结

- CRUD 是接口项目最常见的数据操作
- `select()` 用于构建查询
- `commit()` 才会真正提交事务
- 出错时应使用 `rollback()`
- `refresh()` 可以取得数据库生成或更新后的值
- 重要业务数据优先考虑逻辑删除
