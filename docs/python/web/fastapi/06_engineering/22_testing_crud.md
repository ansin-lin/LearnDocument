# 第22章 员工CRUD与事务测试

> 本章成果：使用第21章的隔离测试环境验证员工新增、修改、校验错误、无效部门、重复编号和逻辑删除，并同时断言HTTP响应与数据库状态。

## 一、接口测试为什么要检查数据库

只断言状态码不能证明事务结果正确。例如新增接口可能返回`201`却没有提交，逻辑删除也可能错误地物理删除记录。本章每个写操作都检查两层结果：

```text
HTTP状态码和响应字段
+
数据库中的最终记录状态
```

## 二、准备请求数据

文件：`tests/test_employees.py`  
操作：新建  
代码类型：测试代码片段

```python
from sqlalchemy import select  # 导入数据库结果核对查询

from app.models import Employee  # 导入员工ORM模型


def employee_payload(employee_number: str = "E010") -> dict:  # 创建可复用新增请求体
    return {  # 返回与EmployeeCreate一致的字典
        "employee_number": employee_number,  # 允许测试覆盖员工编号
        "name": "Suzuki",  # 设置测试姓名
        "department_id": 1,  # 引用Fixture准备的开发部
        "email": "suzuki@example.test",  # 使用测试专用邮箱域名
        "joined_on": "2026-04-01",  # 使用API接受的日期字符串
    }  # 完成请求体
```

辅助函数只负责准备数据，不发送请求，也不隐藏断言。每个测试仍能直接看到自己验证的行为。

## 三、测试新增成功

文件：`tests/test_employees.py`  
操作：继续追加  
代码类型：测试代码片段

```python
def test_create_employee(client, db_session, admin_headers):  # 验证管理员新增员工
    response = client.post(  # 发送新增请求
        "/api/employees",  # 指定员工集合路径
        json=employee_payload(),  # 提交JSON请求体
        headers=admin_headers,  # 携带管理员Bearer Token
    )  # 得到接口响应

    assert response.status_code == 201  # 断言创建成功状态码
    assert response.json()["employee_number"] == "E010"  # 断言响应员工编号

    saved = db_session.execute(  # 在同一测试数据库查询写入结果
        select(Employee).where(Employee.employee_number == "E010")  # 按唯一编号筛选
    ).scalar_one()  # 要求数据库中恰好存在一条记录
    assert saved.name == "Suzuki"  # 断言姓名已经提交
    assert saved.is_active is True  # 断言新员工默认在职
```

`client.post(url, json=..., headers=...)`中，`url`是必填路径；`json`接受可JSON序列化对象并自动设置JSON请求头；`headers`接受请求头字典。`admin_headers`通过真实登录端点取得Token，不要为了测试方便删除权限依赖。

## 四、测试请求校验错误

文件：`tests/test_employees.py`  
操作：继续追加  
代码类型：测试代码片段

```python
def test_create_employee_validation_error(client, admin_headers):  # 验证空请求体校验失败
    response = client.post(  # 发送缺少全部必填字段的请求
        "/api/employees",  # 指定员工集合路径
        json={},  # 提交空JSON对象
        headers=admin_headers,  # 携带管理员认证头
    )  # 得到校验响应

    assert response.status_code == 422  # 断言Pydantic校验错误状态码


def test_invalid_page_size(client, admin_headers):  # 验证分页上限
    response = client.get(  # 发送超过允许范围的分页请求
        "/api/employees?size=101",  # size允许范围是1到100
        headers=admin_headers,  # 携带管理员认证头
    )  # 得到参数校验响应

    assert response.status_code == 422  # 断言无效分页不会进入数据库查询
```

继续增加错误日期、`department_id=0`和超过长度限制的字段用例。校验失败发生在路径函数执行前，数据库员工件数不应变化。

## 五、测试重复编号与事务回滚

文件：`tests/test_employees.py`  
操作：继续追加  
代码类型：测试代码片段

```python
def test_create_employee_duplicate(client, db_session, admin_headers):  # 验证重复编号冲突
    payload = employee_payload()  # 两次请求使用同一个员工编号
    first = client.post(  # 第一次创建员工
        "/api/employees",  # 指定员工集合路径
        json=payload,  # 提交有效请求体
        headers=admin_headers,  # 携带管理员认证头
    )  # 得到第一次响应
    second = client.post(  # 第二次提交重复编号
        "/api/employees",  # 使用相同路径
        json=payload,  # 使用相同请求体
        headers=admin_headers,  # 携带管理员认证头
    )  # 得到冲突响应

    assert first.status_code == 201  # 第一次创建成功
    assert second.status_code == 409  # 第二次返回业务冲突

    saved = db_session.execute(  # 查询该编号的数据库记录
        select(Employee).where(Employee.employee_number == "E010")  # 按编号筛选
    ).scalars().all()  # 取得全部匹配记录
    assert len(saved) == 1  # 断言失败事务没有留下第二条记录
```

`scalars().all()`返回ORM对象列表。这里使用列表长度证明数据库唯一约束和回滚边界共同保证只保留第一次成功结果。

## 六、测试无效部门与修改事务

文件：`tests/test_employees.py`  
操作：继续追加  
代码类型：测试代码片段

```python
def test_invalid_department_does_not_create_employee(  # 验证无效部门不会写入员工
    client,  # 注入测试客户端
    db_session,  # 注入测试Session
    admin_headers,  # 注入管理员认证头
):  # 开始无效部门测试
    payload = employee_payload()  # 准备基本有效请求体
    payload["department_id"] = 999  # 改为不存在的部门主键
    response = client.post(  # 提交新增请求
        "/api/employees",  # 指定员工集合路径
        json=payload,  # 提交包含无效部门的请求体
        headers=admin_headers,  # 携带管理员认证头
    )  # 得到业务错误响应

    assert response.status_code == 400  # 断言部门不存在状态码
    saved = db_session.execute(  # 查询数据库确认没有部分写入
        select(Employee).where(Employee.employee_number == "E010")  # 按编号筛选
    ).scalar_one_or_none()  # 返回员工或None
    assert saved is None  # 断言失败事务没有保存员工


def test_update_employee_changes_database(  # 验证修改接口提交数据库变化
    client,  # 注入测试客户端
    db_session,  # 注入测试Session
    admin_headers,  # 注入管理员认证头
):  # 开始修改测试
    client.post(  # 先创建待修改员工
        "/api/employees",  # 指定新增路径
        json=employee_payload(),  # 提交有效新增请求体
        headers=admin_headers,  # 携带管理员认证头
    )  # 完成前置创建
    response = client.put(  # 发送修改请求
        "/api/employees/E010",  # 指定目标员工编号
        json={  # 提交允许修改的完整字段
            "name": "Suzuki Updated",  # 修改姓名
            "department_id": 2,  # 修改为营业部
            "email": "suzuki.updated@example.test",  # 修改邮箱
            "joined_on": "2026-05-01",  # 修改入职日期
        },  # 完成修改请求体
        headers=admin_headers,  # 携带管理员认证头
    )  # 得到修改响应

    assert response.status_code == 200  # 断言修改成功
    assert response.json()["department"]["name"] == "营业部"  # 断言响应关系字段更新
    saved = db_session.execute(  # 查询数据库最终状态
        select(Employee).where(Employee.employee_number == "E010")  # 按编号筛选
    ).scalar_one()  # 取得唯一员工
    assert saved.name == "Suzuki Updated"  # 断言姓名已经提交
    assert saved.department_id == 2  # 断言部门已经提交
```

无效部门用例证明Service在业务检查失败时没有留下员工；修改用例同时检查响应和数据库，避免出现“接口返回新值但事务没有提交”的假成功。

## 七、测试逻辑删除

文件：`tests/test_employees.py`  
操作：继续追加  
代码类型：测试代码片段

```python
def test_deactivate_employee_keeps_record(  # 验证逻辑删除保留数据库记录
    client,  # 注入测试客户端
    db_session,  # 注入测试Session
    admin_headers,  # 注入管理员认证头
):  # 开始测试函数
    client.post(  # 先创建待删除员工
        "/api/employees",  # 指定新增路径
        json=employee_payload(),  # 提交有效请求体
        headers=admin_headers,  # 携带管理员认证头
    )  # 完成前置创建

    response = client.delete(  # 发送逻辑删除请求
        "/api/employees/E010",  # 指定待删除员工编号
        headers=admin_headers,  # 携带管理员认证头
    )  # 得到删除响应

    assert response.status_code == 204  # 断言删除成功且响应体为空
    employee = db_session.execute(  # 查询被删除员工的数据库记录
        select(Employee).where(Employee.employee_number == "E010")  # 按编号筛选
    ).scalar_one()  # 要求记录仍然存在
    assert employee.is_active is False  # 断言只修改在职状态

    detail = client.get(  # 再次查询逻辑删除员工
        "/api/employees/E010",  # 指定详情路径
        headers=admin_headers,  # 携带管理员认证头
    )  # 得到详情响应
    second_delete = client.delete(  # 再次删除相同员工
        "/api/employees/E010",  # 使用相同路径
        headers=admin_headers,  # 携带管理员认证头
    )  # 得到第二次删除响应
    assert detail.status_code == 404  # 默认详情不返回离职员工
    assert second_delete.status_code == 404  # 再次删除返回不存在

    reused = client.post(  # 尝试重新使用离职员工编号
        "/api/employees",  # 指定新增路径
        json=employee_payload(),  # 再次提交E010
        headers=admin_headers,  # 携带管理员认证头
    )  # 得到编号冲突响应
    assert reused.status_code == 409  # 离职记录仍占用唯一员工编号
```

这组断言同时证明：记录仍存在、`is_active=False`、默认详情不再返回该员工、同一逻辑删除不能重复成功。

## 八、运行与排错

在项目根目录执行：

```powershell
pytest -q tests/test_employees.py
```

常见失败：

| 现象 | 原因 | 修正 |
| --- | --- | --- |
| 返回`401` | 没有使用`admin_headers` | 通过真实登录端点取得Token |
| 查询不到刚创建的数据 | 生产`get_db`没有被覆盖 | 核对`dependency_overrides`的键是原函数 |
| 第二次运行提示重复数据 | 测试表或Fixture没有清理 | 保持函数级Fixture并在结束时删除表 |
| 删除后记录不存在 | Service执行了物理删除 | 恢复`is_active=False`逻辑删除 |

## 九、动手任务

1. 增加不存在员工详情和修改返回`404`的测试。
2. 增加`page=0`返回`422`的另一条分页边界测试。
3. 修改更新请求中的日期和邮箱，并增加对应数据库断言。
4. 连续执行两次本文件，确认结果一致。

## 十、完成检查

- [ ] 成功用例同时断言响应与数据库。
- [ ] 校验失败不会进入写事务。
- [ ] 重复编号只保留一条记录。
- [ ] 无效部门不会留下部分员工数据。
- [ ] 修改成功同时反映在响应和数据库中。
- [ ] 逻辑删除不物理删除员工。
- [ ] 测试不依赖执行顺序和上一次数据。
- [ ] `page=0`和`size=101`等分页边界不会进入业务查询。

完成后保留员工测试，第23章将继续覆盖登录、当前用户、角色权限和MySQL迁移。
