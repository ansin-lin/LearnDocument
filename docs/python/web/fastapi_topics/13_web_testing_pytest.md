# 第13章 Web 测试基础

> 本章目标：掌握使用 pytest 和 FastAPI TestClient 编写接口测试，能够验证成功、失败、边界和认证场景。

## 一、为什么要写接口测试

接口测试用于确认接口行为是否符合预期。

测试可以检查：

- 状态码
- 响应 JSON
- 参数校验
- 错误响应
- 登录认证
- 数据库写入结果

## 二、安装依赖

```powershell
pip install pytest httpx  # 安装 pytest 和测试客户端依赖
```

## 三、最小测试

文件位置：

```text
tests/test_health.py
```

```python
from fastapi.testclient import TestClient  # 导入 FastAPI 测试客户端
from app.main import app  # 导入 FastAPI 应用对象

client = TestClient(app)  # 创建测试客户端


def test_health_check():  # 定义健康检查测试
    response = client.get("/health")  # 发送 GET 请求
    assert response.status_code == 200  # 断言状态码是 200
    assert response.json() == {"status": "ok"}  # 断言响应 JSON
```

执行：

```powershell
pytest  # 运行全部测试
```

## 四、测试新增接口

```python
def test_create_employee():  # 测试新增员工
    request_body = {  # 准备请求 JSON
        "employee_code": "E001",  # 员工编号
        "name": "Tanaka",  # 员工姓名
        "email": "tanaka@example.com",  # 邮箱
        "department_id": 1,  # 部门 ID
    }
    response = client.post("/employees", json=request_body)  # 发送 POST 请求
    assert response.status_code == 201  # 断言新增成功
    assert response.json()["employee_code"] == "E001"  # 断言员工编号
```

## 五、测试校验失败

```python
def test_create_employee_validation_error():  # 测试新增员工校验失败
    response = client.post("/employees", json={})  # 发送空 JSON
    assert response.status_code == 422  # 断言 FastAPI 返回校验失败
```

## 六、测试认证接口

```python
def test_login_success():  # 测试登录成功
    response = client.post("/auth/login", json={"username": "admin", "password": "password"})  # 发送登录请求
    assert response.status_code == 200  # 断言登录成功
    assert "access_token" in response.json()  # 断言响应中包含 Token
```

## 七、常见测试关注点

| 场景 | 应检查 |
| --- | --- |
| 查询成功 | `200`、响应字段 |
| 新增成功 | `201`、数据库结果 |
| 参数错误 | `422` |
| 数据不存在 | `404` |
| 未登录 | `401` |
| 无权限 | `403` |

## 八、基础练习

请编写测试：

1. 健康检查接口
2. 员工列表接口
3. 新增员工成功
4. 新增员工参数错误
5. 登录成功
6. 未登录访问受保护接口

## 九、本章总结

- `TestClient` 用于模拟 HTTP 请求
- pytest 使用 `assert` 判断结果
- 接口测试要覆盖成功和失败场景
- 认证、权限、校验错误都需要测试
