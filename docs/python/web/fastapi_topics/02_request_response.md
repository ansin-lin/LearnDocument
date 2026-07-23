# 第2章 FastAPI 请求与响应

> 本章目标：掌握路径参数、查询参数、请求体、响应数据和状态码的基本写法，能够编写常见 CRUD 风格接口。

## 一、请求和响应

API 开发的基本流程是：

```text
客户端发送请求
-> FastAPI 匹配路由
-> 执行路由函数
-> 返回响应
```

请求中常见内容：

| 内容 | 示例 |
| --- | --- |
| 请求方法 | `GET`、`POST`、`PUT`、`DELETE` |
| URL 路径 | `/employees/1` |
| 查询参数 | `/employees?keyword=Tanaka` |
| 请求体 | JSON 数据 |
| 请求头 | Token、Content-Type |

## 二、路径参数

路径参数适合表示资源 ID。

```python
from fastapi import FastAPI  # 导入 FastAPI

app = FastAPI()  # 创建应用对象


@app.get("/employees/{employee_id}")  # 注册带路径参数的接口
def get_employee(employee_id: int):  # employee_id 来自 URL，并自动转换为 int
    return {"employee_id": employee_id, "name": "Tanaka"}  # 返回员工数据
```

访问：

```text
GET /employees/1
```

响应：

```json
{"employee_id":1,"name":"Tanaka"}
```

如果访问 `/employees/abc`，FastAPI 会返回校验错误，因为 `employee_id` 要求是整数。

## 三、查询参数

查询参数适合搜索、筛选和分页。

```python
@app.get("/employees")  # 注册员工列表接口
def list_employees(keyword: str | None = None, active: bool = True):  # keyword 可选，active 默认 True
    return {"keyword": keyword, "active": active}  # 返回接收到的查询条件
```

访问：

```text
GET /employees?keyword=Tanaka&active=true
```

响应：

```json
{"keyword":"Tanaka","active":true}
```

参数规则：

| 写法 | 说明 |
| --- | --- |
| `keyword: str` | 必填字符串参数 |
| `keyword: str \| None = None` | 可选字符串参数 |
| `active: bool = True` | 布尔参数，默认值为 `True` |
| `page: int = 1` | 整数参数，默认第 1 页 |

## 四、请求体

新增和修改数据时，通常使用请求体传递 JSON。

```python
from pydantic import BaseModel  # 导入 BaseModel，用于定义请求体结构


class EmployeeCreate(BaseModel):  # 定义新增员工请求模型
    employee_code: str  # 员工编号
    name: str  # 员工姓名
    email: str | None = None  # 邮箱，可选


@app.post("/employees")  # 注册新增员工接口
def create_employee(employee: EmployeeCreate):  # employee 来自 JSON 请求体
    return {"message": "created", "employee": employee}  # 返回创建结果
```

请求：

```json
{
  "employee_code": "E001",
  "name": "Tanaka",
  "email": "tanaka@example.com"
}
```

响应：

```json
{"message":"created","employee":{"employee_code":"E001","name":"Tanaka","email":"tanaka@example.com"}}
```

## 五、状态码

状态码表示请求处理结果。

```python
@app.post("/employees", status_code=201)  # 新增成功时返回 201
def create_employee(employee: EmployeeCreate):  # 接收新增员工请求体
    return {"message": "created", "employee": employee}  # 返回创建结果
```

常用状态码：

| 状态码 | 说明 | 场景 |
| --- | --- | --- |
| `200` | 成功 | 查询、修改成功 |
| `201` | 创建成功 | 新增数据 |
| `204` | 成功但无响应体 | 删除成功 |
| `400` | 请求错误 | 参数不合法 |
| `401` | 未认证 | 未登录或 Token 无效 |
| `403` | 无权限 | 登录了但不能操作 |
| `404` | 不存在 | 数据不存在 |
| `422` | 校验失败 | FastAPI 参数校验失败 |
| `500` | 服务器错误 | 程序异常 |

## 六、返回字典和列表

FastAPI 会自动把字典、列表和 Pydantic 模型转换为 JSON。

```python
@app.get("/departments")  # 注册部门列表接口
def list_departments():  # 定义部门列表函数
    return [  # 返回列表数据
        {"code": "D001", "name": "開発部"},  # 第一条部门数据
        {"code": "D002", "name": "営業部"},  # 第二条部门数据
    ]
```

响应：

```json
[{"code":"D001","name":"開発部"},{"code":"D002","name":"営業部"}]
```

## 七、常见 CRUD 接口

| 功能 | HTTP 方法 | 路径 |
| --- | --- | --- |
| 查询列表 | `GET` | `/employees` |
| 查询详情 | `GET` | `/employees/{employee_id}` |
| 新增 | `POST` | `/employees` |
| 修改 | `PUT` | `/employees/{employee_id}` |
| 删除 | `DELETE` | `/employees/{employee_id}` |

示例：

```python
@app.delete("/employees/{employee_id}", status_code=204)  # 注册删除接口，成功时返回 204
def delete_employee(employee_id: int):  # 接收路径参数 employee_id
    return None  # 204 响应通常不返回响应体
```

## 八、常见错误

| 错误 | 原因 | 处理方式 |
| --- | --- | --- |
| 路径参数类型错误 | URL 中传了非整数 | 检查类型提示 |
| 查询参数收不到 | 参数名不一致 | 检查 URL 参数名 |
| 请求体校验失败 | JSON 字段缺失或类型错误 | 查看 422 响应 |
| 状态码不合适 | 新增、删除仍使用默认 200 | 根据语义设置状态码 |

## 九、基础练习

请实现：

1. `GET /employees/{employee_id}`
2. `GET /employees?keyword=...`
3. `POST /employees`
4. `DELETE /employees/{employee_id}`

## 十、本章总结

- 路径参数来自 URL 路径
- 查询参数来自 `?key=value`
- 请求体通常是 JSON
- Pydantic 模型用于描述请求体结构
- 状态码要和接口语义一致
