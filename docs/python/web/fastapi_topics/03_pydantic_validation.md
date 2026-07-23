# 第3章 Pydantic 数据模型与校验

> 本章目标：掌握 Pydantic 模型、字段校验、默认值、响应模型和校验错误，能够为 FastAPI 接口定义稳定的数据结构。

## 一、Pydantic 的作用

Pydantic 用来定义数据结构和校验规则。

在 FastAPI 中，它常用于：

| 用途 | 说明 |
| --- | --- |
| 请求体模型 | 校验前端提交的 JSON |
| 响应模型 | 控制接口返回字段 |
| 配置模型 | 管理环境变量和配置 |
| 数据转换 | 把输入数据转换成指定类型 |

## 二、定义基础模型

```python
from pydantic import BaseModel  # 导入 BaseModel，用于定义数据模型


class EmployeeCreate(BaseModel):  # 定义新增员工请求模型
    employee_code: str  # 员工编号
    name: str  # 员工姓名
    email: str | None = None  # 邮箱，可选字段
```

字段规则：

| 写法 | 说明 |
| --- | --- |
| `employee_code: str` | 必填字符串 |
| `email: str \| None = None` | 可选字符串 |
| `age: int = 20` | 整数，有默认值 |

## 三、字段约束

可以使用 `Field` 设置长度、说明和示例。

```python
from pydantic import BaseModel, Field  # 导入 BaseModel 和 Field


class EmployeeCreate(BaseModel):  # 新增员工请求模型
    employee_code: str = Field(min_length=1, max_length=20, description="员工编号")  # 员工编号长度 1 到 20
    name: str = Field(min_length=1, max_length=100, description="员工姓名")  # 员工姓名长度 1 到 100
    email: str | None = Field(default=None, max_length=255, description="邮箱")  # 邮箱可选，最大 255
```

常用约束：

| 参数 | 作用 |
| --- | --- |
| `min_length` | 最小长度 |
| `max_length` | 最大长度 |
| `ge` | 大于等于 |
| `le` | 小于等于 |
| `default` | 默认值 |
| `description` | 文档说明 |

## 四、请求模型和响应模型分开

请求模型和响应模型不要混在一起。

```python
class EmployeeCreate(BaseModel):  # 新增请求模型
    employee_code: str  # 员工编号
    name: str  # 员工姓名


class EmployeeResponse(BaseModel):  # 员工响应模型
    id: int  # 员工 ID
    employee_code: str  # 员工编号
    name: str  # 员工姓名
```

接口中使用响应模型：

```python
@app.post("/employees", response_model=EmployeeResponse, status_code=201)  # 指定响应模型和状态码
def create_employee(employee: EmployeeCreate):  # 接收新增员工请求
    return {"id": 1, "employee_code": employee.employee_code, "name": employee.name}  # 返回符合响应模型的数据
```

`response_model` 的作用：

- 控制返回字段
- 生成接口文档
- 过滤不应该返回的数据
- 让前后端约定更稳定

## 五、校验错误

请求数据不符合模型时，FastAPI 会返回 `422`。

请求：

```json
{
  "employee_code": "",
  "name": ""
}
```

可能响应：

```json
{
  "detail": [
    {
      "type": "string_too_short",
      "loc": ["body", "employee_code"],
      "msg": "String should have at least 1 character"
    }
  ]
}
```

`loc` 表示错误字段位置，`msg` 表示错误说明。

## 六、模型继承

多个模型字段相同时，可以用继承减少重复。

```python
class EmployeeBase(BaseModel):  # 员工基础模型
    employee_code: str  # 员工编号
    name: str  # 员工姓名
    email: str | None = None  # 邮箱


class EmployeeCreate(EmployeeBase):  # 新增员工模型
    pass  # 新增时暂时不增加额外字段


class EmployeeResponse(EmployeeBase):  # 员工响应模型
    id: int  # 响应中包含数据库 ID
```

## 七、企业项目中的用法

推荐按用途拆分模型：

| 模型 | 用途 |
| --- | --- |
| `EmployeeCreate` | 新增请求 |
| `EmployeeUpdate` | 修改请求 |
| `EmployeeResponse` | 返回给前端 |
| `EmployeeSearchParams` | 查询条件 |

不要把数据库模型直接当成接口请求体，也不要把密码、Token、内部字段直接返回给前端。

## 八、基础练习

请定义：

1. `EmployeeCreate`
2. `EmployeeUpdate`
3. `EmployeeResponse`

要求：

- 员工编号必填，最大 20
- 姓名必填，最大 100
- 邮箱可选
- 响应模型包含 `id`

## 九、本章总结

- Pydantic 用于定义数据结构和校验规则
- `BaseModel` 是模型基类
- `Field` 用于设置字段约束
- 请求模型和响应模型应分开
- 校验失败时 FastAPI 返回 `422`
- `response_model` 可以控制接口返回结构
