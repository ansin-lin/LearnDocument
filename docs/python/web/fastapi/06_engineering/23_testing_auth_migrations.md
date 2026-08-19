# 第23章 认证、权限与迁移测试

> 本章成果：自动验证未认证、有效认证、角色不足和无效Token，并在可丢弃MySQL测试库确认Alembic迁移可以前进、回退和重建。

## 一、认证测试覆盖什么

第21章提供管理员、人事和只读账号认证头，第22章已经证明管理员可以执行员工写操作。本章固定认证与权限契约：

- 未携带Token返回`401`。
- 有效Token可以访问允许的接口。
- 有效账号但角色不足返回`403`，数据不变化。
- 无效Token返回`401`。
- 错误密码和禁用账号不能取得或继续使用有效身份。
- `VIEWER`、`HR_STAFF`和`SYSTEM_ADMIN`遵守项目权限矩阵。

## 二、编写认证与权限测试

文件：`tests/test_auth.py`  
操作：新建  
代码类型：完整测试文件

```python
from sqlalchemy import select  # 导入账号和员工状态核对查询

from app.models import Employee, UserAccount  # 导入员工和账号ORM模型


def employee_payload() -> dict:  # 创建权限测试使用的员工请求体
    return {  # 返回与EmployeeCreate一致的字典
        "employee_number": "E010",  # 设置测试员工编号
        "name": "Suzuki",  # 设置测试姓名
        "department_id": 1,  # 引用Fixture准备的开发部
        "email": "suzuki@example.test",  # 使用测试邮箱
        "joined_on": "2026-04-01",  # 使用API日期格式
    }  # 完成请求体


def test_employee_list_requires_token(client):  # 验证未认证请求
    response = client.get("/api/employees")  # 不携带Authorization头访问列表

    assert response.status_code == 401  # 断言返回未认证


def test_admin_can_list_employees(client, admin_headers):  # 验证有效管理员Token
    response = client.get(  # 请求受保护列表
        "/api/employees",  # 指定列表路径
        headers=admin_headers,  # 携带管理员Bearer Token
    )  # 得到列表响应

    assert response.status_code == 200  # 断言认证成功并允许查询


def test_viewer_cannot_delete_employee(  # 验证只读角色不能删除
    client,  # 注入测试客户端
    db_session,  # 注入测试Session
    admin_headers,  # 注入管理员认证头用于准备数据
    viewer_headers,  # 注入只读账号认证头用于权限检查
):  # 开始权限测试
    client.post(  # 先由管理员创建待删除员工
        "/api/employees",  # 指定新增路径
        json=employee_payload(),  # 提交有效请求体
        headers=admin_headers,  # 携带管理员认证头
    )  # 完成前置数据创建

    response = client.delete(  # 由只读账号尝试删除
        "/api/employees/E010",  # 指定待删除员工
        headers=viewer_headers,  # 携带只读账号Token
    )  # 得到权限响应

    assert response.status_code == 403  # 断言已认证但权限不足
    employee = db_session.execute(  # 查询数据库确认数据未改变
        select(Employee).where(Employee.employee_number == "E010")  # 按编号筛选
    ).scalar_one()  # 取得待删除员工
    assert employee.is_active is True  # 断言权限失败没有执行逻辑删除


def test_invalid_token_is_rejected(client):  # 验证无效Token
    response = client.get(  # 请求受保护列表
        "/api/employees",  # 指定列表路径
        headers={"Authorization": "Bearer invalid-token"},  # 携带格式错误Token
    )  # 得到认证响应

    assert response.status_code == 401  # 断言返回未认证
```

`client.delete(url, headers=...)`的`url`必填，`headers`接受请求头字典。认证测试使用数据库Fixture账号和真实密码哈希流程，不能在生产代码中增加“测试专用后门”。

## 三、补充关键认证场景

文件：`tests/test_auth.py`  
操作：继续追加  
代码类型：认证与权限测试片段

```python
def test_wrong_password_is_rejected(client):  # 验证错误密码不能登录
    response = client.post(  # 提交OAuth2登录表单
        "/api/auth/token",  # 指定登录端点
        data={"username": "admin", "password": "wrong-password"},  # 使用错误密码
    )  # 得到认证响应

    assert response.status_code == 401  # 断言登录失败


def test_hr_staff_can_edit_but_cannot_delete(  # 验证人事角色权限边界
    client,  # 注入测试客户端
    hr_staff_headers,  # 注入人事账号认证头
):  # 开始角色权限测试
    created = client.post(  # 由人事账号新增员工
        "/api/employees",  # 指定新增路径
        json=employee_payload(),  # 提交有效员工数据
        headers=hr_staff_headers,  # 携带人事账号Token
    )  # 得到新增响应
    updated = client.put(  # 由人事账号修改员工
        "/api/employees/E010",  # 指定目标员工
        json={  # 提交允许修改的字段
            "name": "HR Updated",  # 设置新姓名
            "department_id": 1,  # 保持开发部
            "email": "hr.updated@example.test",  # 设置新邮箱
            "joined_on": "2026-04-02",  # 设置新日期
        },  # 完成修改请求体
        headers=hr_staff_headers,  # 携带人事账号Token
    )  # 得到修改响应
    deleted = client.delete(  # 尝试执行管理员专用删除
        "/api/employees/E010",  # 指定目标员工
        headers=hr_staff_headers,  # 携带人事账号Token
    )  # 得到权限响应

    assert created.status_code == 201  # 人事角色允许新增
    assert updated.status_code == 200  # 人事角色允许修改
    assert deleted.status_code == 403  # 人事角色不允许删除


def test_disabled_account_old_token_is_rejected(  # 验证禁用后旧Token失效
    client,  # 注入测试客户端
    db_session,  # 注入测试Session
    viewer_headers,  # 先取得只读账号的有效Token
):  # 开始账号状态测试
    viewer = db_session.execute(  # 查询当前只读账号
        select(UserAccount).where(UserAccount.login_id == "viewer")  # 按登录账号筛选
    ).scalar_one()  # 取得唯一账号
    viewer.is_active = False  # 在数据库中禁用账号
    db_session.commit()  # 提交账号状态变化

    response = client.get(  # 使用禁用前取得的Token再次请求
        "/api/employees",  # 请求受保护员工列表
        headers=viewer_headers,  # 携带旧Token
    )  # 得到当前用户检查结果

    assert response.status_code == 401  # 每次请求重新查库并拒绝禁用账号
```

继续完成以下扩展测试：

1. `VIEWER`新增和修改员工返回`403`。
2. `SYSTEM_ADMIN`删除成功。
3. 使用已过期Token返回`401`。

权限失败用例必须查询数据库，证明路径函数和Service没有执行写操作。

## 四、验证MySQL迁移

SQLite快速测试不能覆盖MySQL字段、约束和方言差异。准备专用、可丢弃的MySQL测试数据库，并把`DATABASE_URL`明确指向该数据库。

在项目根目录执行：

```powershell
alembic upgrade head
alembic current
pytest -q
alembic downgrade -1
alembic upgrade head
```

执行顺序说明：

| 命令 | 可接受的值 | 默认值或必填性 | 作用 |
| --- | --- | --- | --- |
| `alembic upgrade <revision>` | `head`、具体版本号或相对版本 | 目标版本必填 | 把测试数据库升级到目标版本 |
| `alembic current` | 可配合`--verbose`等选项 | 无目标版本 | 显示数据库当前迁移版本 |
| `alembic downgrade <revision>` | `-1`、具体版本号或`base` | 目标版本必填 | 回退迁移；可能造成数据丢失 |

回退前必须再次确认目标是可丢弃测试数据库。生产回退需要单独评估数据丢失、应用兼容和备份恢复，不能直接照搬本练习。

## 五、完整回归

执行全部测试两次：

```powershell
pytest -q
pytest -q
```

两次结果必须一致。若第二次失败，优先检查测试数据库清理、依赖覆盖清理、全局可变状态和测试顺序依赖。

## 六、动手任务

1. 补齐登录成功、过期Token和`VIEWER`写操作测试。
2. 为每个权限失败用例增加数据库状态断言。
3. 在空MySQL测试库执行迁移和完整测试。
4. 保存测试汇总、迁移当前版本和失败排查记录，不能保存密码或Token。

## 七、完成检查

- [ ] `401`与`403`分别有自动测试。
- [ ] 三个角色的关键操作符合项目规格。
- [ ] 权限失败不会修改数据库。
- [ ] 测试可连续运行且结果一致。
- [ ] MySQL迁移已在可丢弃环境验证。
- [ ] 没有在生产代码中加入测试后门。

完成后保留可重复运行的回归测试，第24章前后端联调和后续交付都以这些接口契约为基准。
