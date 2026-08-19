# 第3章 Pydantic、响应模型与 OpenAPI

> 本章成果：在第2章内存 CRUD 的基础上，统一员工请求与响应结构，验证字段格式，并能通过 `/docs` 区分结构校验错误和业务校验错误。

本章建议按三个检查点完成：先建立请求Schema和字段校验；再建立响应Schema并替换路由；最后观察OpenAPI和`422`、`404`等错误响应。每个检查点都保存并启动一次应用，避免在500多行内容结束后才集中排错。

## 一、本章开始状态

继续使用第2章完成的 `app/main.py`，其中已经包含：

- `/health`
- 部门和员工内存数据
- 员工列表、详情、新增、修改和逻辑删除
- `/api/departments`

本章会替换这个文件的模型定义和路由实现。不要在文件末尾再次定义同名模型或注册相同路径，否则旧路由仍可能继续使用旧模型。

## 二、完成 `app/main.py`

下面是本章完成后的完整文件：

```python
from datetime import date  # 导入日期类型，用于表示员工入职日期
from typing import Annotated  # 导入带附加校验信息的类型工具

from fastapi import FastAPI, HTTPException, Path, Response  # 导入应用、异常、路径和响应类
from pydantic import BaseModel, Field, field_validator  # 导入模型、字段约束和校验器

app = FastAPI()  # 创建 FastAPI 应用对象
```

### 导入说明

本章继续使用第2章已经讲解的 `FastAPI`、`HTTPException`、`Response` 和 `BaseModel`。首次出现的导入如下：

| 名称 | 来源 | 作用 |
| --- | --- | --- |
| `Annotated` | Python `typing` | 在原有类型后附加 FastAPI 校验和文档信息 |
| `Path` | FastAPI | 声明路径参数的长度、格式和文档信息 |
| `Field` | Pydantic | 声明模型字段的默认值、约束和文档信息 |
| `field_validator` | Pydantic | 给指定模型字段注册自定义校验或规范化函数 |

`Annotated[str, Path(...)]` 中的第一个位置保存实际 Python 类型，后面的内容保存附加信息。路由函数实际得到的仍然是 `str`，FastAPI 会读取 `Path(...)` 完成校验和 OpenAPI 描述。

### 创建公共请求模型

继续在 `app/main.py` 中追加：

```python
class EmployeeWriteBase(BaseModel):  # 定义新增和修改共用的请求字段
    name: str = Field(min_length=1, max_length=100, description="员工姓名")  # 限制姓名长度
    department_id: int = Field(ge=1, description="部门主键")  # 要求部门 ID 至少为 1
    email: str = Field(default="", max_length=254, description="邮箱")  # 设置邮箱默认值和长度
    joined_on: date  # 接收并转换入职日期
```

继承 `BaseModel` 后，类中的类型标注会成为 Pydantic 字段。这个机制已经在第2章使用；这里进一步通过 `Field()` 增加约束。

`Field()` 本示例参数：

| 参数 | 可接受的值 | 默认值或必填性 | 作用 |
| --- | --- | --- | --- |
| `min_length` | 大于等于 `0` 的整数或 `None` | 默认 `None` | 限制字符串的最小长度 |
| `max_length` | 大于等于 `0` 的整数或 `None` | 默认 `None` | 限制字符串的最大长度 |
| `pattern` | 正则表达式字符串、已编译正则对象或 `None` | 默认 `None` | 限制字符串格式 |
| `ge` | 可与字段值比较的数字或 `None` | 默认 `None` | 要求字段值大于等于指定值 |
| `default` | 与字段类型兼容的值 | 没有默认值时字段必填 | 设置字段省略时使用的默认值 |
| `description` | 字符串或 `None` | 默认 `None` | 在 OpenAPI 中说明字段含义 |

`joined_on: date` 没有设置默认值，因此请求中必须提供。JSON 使用 `YYYY-MM-DD` 日期字符串，Pydantic 校验成功后，路由函数得到 Python `date` 对象。

当前 `email` 字段只检查字符串类型和最大长度，不验证完整邮箱格式。需要严格验证邮箱时可以使用 Pydantic 的 `EmailStr`，同时安装其邮箱校验依赖。

### 创建新增模型并规范化员工编号

继续追加：

```python
class EmployeeCreate(EmployeeWriteBase):  # 定义新增员工请求模型
    employee_number: str = Field(  # 声明新增时需要员工编号
        min_length=2,  # 要求编号至少 2 个字符
        max_length=20,  # 限制编号最多 20 个字符
        pattern=r"^E[0-9]+$",  # 要求编号由 E 和数字组成
        description="员工编号",  # 设置接口文档中的字段说明
    )  # 结束员工编号字段配置

    @field_validator("employee_number", mode="before")  # 在正式校验前整理员工编号
    @classmethod  # 把校验函数声明为类方法
    def normalize_employee_number(cls, value: object) -> object:  # 接收原始字段值
        if isinstance(value, str):  # 只对字符串执行文本处理
            return value.strip().upper()  # 去除两端空格并转换为大写
        return value  # 其他类型交给 Pydantic 继续校验
```

`EmployeeCreate` 继承 `EmployeeWriteBase`，因此自动拥有姓名、部门、邮箱和入职日期字段，再增加新增接口专用的 `employee_number`。这样可以复用真正相同的字段规则。

`field_validator()` 本示例参数：

| 参数 | 可接受的值 | 默认值或必填性 | 作用 |
| --- | --- | --- | --- |
| 第一个位置参数 `field` | 当前模型中存在的字段名字符串 | 必填 | 指定需要处理的字段 |
| 后续位置参数 `*fields` | 其他字段名字符串 | 可省略 | 让同一个校验器处理多个字段 |
| `mode` | `"before"`、`"after"`、`"plain"`、`"wrap"` | 默认 `"after"` | 决定校验器在 Pydantic 内部校验前后如何执行 |
| `check_fields` | `True`、`False` 或 `None` | 默认 `None` | 决定创建模型类时是否检查字段名存在 |

本例使用 `mode="before"`，所以 `value` 是尚未完成类型校验的原始输入。代码先用 `isinstance()` 判断字符串，再执行 `strip().upper()`；其他值交回 Pydantic 继续校验。

`@classmethod` 让校验函数接收模型类 `cls`，符合字段校验器的类方法写法。本例没有使用 `cls`，但仍保留该参数。

请求体中的 `" e003 "` 会先转换为 `"E003"`，然后再检查 `pattern=r"^E[0-9]+$"`。

### 创建修改模型

继续追加：

```python
class EmployeeUpdate(EmployeeWriteBase):  # 定义修改员工请求模型
    pass  # 直接复用公共字段，不允许修改员工编号
```

`EmployeeUpdate` 不增加新字段，因此只接收公共字段。请求体中不能通过这个模型修改员工编号。

Pydantic 默认会忽略模型中没有声明的额外字段。因此即使请求体带有 `employee_number`，它也不会进入 `EmployeeUpdate` 的校验结果；接口调用方仍应只提交模型声明的字段。

### 创建响应模型

继续追加：

```python
class DepartmentSummary(BaseModel):  # 定义响应中的部门摘要
    id: int  # 返回部门主键
    name: str  # 返回部门名称


class EmployeeResponse(BaseModel):  # 定义单个员工的响应结构
    id: int  # 返回员工主键
    employee_number: str  # 返回员工编号
    name: str  # 返回员工姓名
    department_id: int  # 返回关联的部门 ID
    email: str  # 返回员工邮箱
    joined_on: date  # 返回员工入职日期
    is_active: bool  # 返回员工在职状态
    department: DepartmentSummary  # 返回部门摘要


class EmployeeListResponse(BaseModel):  # 定义员工列表的响应结构
    items: list[EmployeeResponse]  # 返回当前页员工
    total: int  # 返回筛选后的总件数
    page: int  # 返回当前页码
    size: int  # 返回每页数量
```

这些模型用于限制和描述响应：

| 模型 | 可接受的数据 | 作用 |
| --- | --- | --- |
| `DepartmentSummary` | 部门 ID 和名称 | 控制员工响应中的部门摘要 |
| `EmployeeResponse` | 完整员工字段和部门摘要 | 控制单个员工响应 |
| `EmployeeListResponse` | 员工列表、总件数、页码和每页数量 | 控制分页列表响应 |

请求模型描述客户端可以提交什么，响应模型描述服务端允许返回什么。`id` 和 `is_active` 由服务端维护，因此不放入新增和修改请求模型，但需要出现在响应模型中。

### 创建可复用的路径参数类型

继续追加：

```python
EmployeeNumberPath = Annotated[  # 定义可复用的员工编号路径类型
    str,  # 路径参数在路由函数中表现为字符串
    Path(  # 为路径参数添加校验规则
        min_length=2,  # 要求编号至少 2 个字符
        max_length=20,  # 限制编号最多 20 个字符
        pattern=r"^E[0-9]+$",  # 要求编号由 E 和数字组成
        description="员工编号",  # 设置接口文档中的参数说明
    ),  # 结束路径参数配置
]  # 结束员工编号路径类型定义
```

`Path()` 本示例参数：

| 参数 | 可接受的值 | 默认值或必填性 | 作用 |
| --- | --- | --- | --- |
| `min_length` | 大于等于 `0` 的整数或 `None` | 默认 `None` | 限制路径字符串的最小长度 |
| `max_length` | 大于等于 `0` 的整数或 `None` | 默认 `None` | 限制路径字符串的最大长度 |
| `pattern` | 正则表达式字符串、已编译正则对象或 `None` | 默认 `None` | 限制路径字符串格式 |
| `description` | 字符串或 `None` | 默认 `None` | 在 OpenAPI 中说明路径参数 |
| `deprecated` | `True`、`False` 或 `None` | 默认 `None` | 标记路径参数是否已经废弃 |
| `include_in_schema` | `True` 或 `False` | 默认 `True` | 决定参数是否出现在 OpenAPI 中 |

路径参数必须出现在 URL 中，因此始终必填。`EmployeeNumberPath` 是类型别名，可以在详情、修改和删除函数中复用相同规则。

路径参数使用这个类型检查格式，但没有设置规范化函数。因此 `/api/employees/E001` 合法，`/api/employees/e001` 返回 `422`。

### 保留内存数据并更新路由

继续追加以下内存数据、辅助函数和路由：

```python
departments = [  # 使用列表暂存部门数据
    {"id": 1, "name": "开发部"},  # 定义开发部样例数据
    {"id": 2, "name": "营业部"},  # 定义营业部样例数据
]  # 结束部门列表

employees = [  # 使用列表暂存员工数据
    {  # 开始第一名员工的数据
        "id": 1,  # 设置员工主键
        "employee_number": "E001",  # 设置员工编号
        "name": "山田太郎",  # 设置员工姓名
        "department_id": 1,  # 关联开发部
        "email": "yamada@example.com",  # 设置员工邮箱
        "joined_on": date(2026, 4, 1),  # 设置入职日期
        "is_active": True,  # 标记员工为在职
    },  # 结束第一名员工的数据
    {  # 开始第二名员工的数据
        "id": 2,  # 设置员工主键
        "employee_number": "E002",  # 设置员工编号
        "name": "佐藤花子",  # 设置员工姓名
        "department_id": 2,  # 关联营业部
        "email": "",  # 设置未填写邮箱时保存的空字符串
        "joined_on": date(2025, 10, 1),  # 设置入职日期
        "is_active": True,  # 标记员工为在职
    },  # 结束第二名员工的数据
]  # 结束员工列表


def find_department(department_id: int) -> dict | None:  # 根据主键查找部门
    for department in departments:  # 逐个检查部门
        if department["id"] == department_id:  # 判断部门主键是否匹配
            return department  # 返回找到的部门
    return None  # 没有找到时返回空值


def build_employee_response(employee: dict) -> dict:  # 组装包含部门摘要的员工响应
    department = find_department(employee["department_id"])  # 查询员工所属部门
    return {**employee, "department": department}  # 合并员工字段和部门摘要


@app.get("/health")  # 注册健康检查接口
def health_check():  # 定义健康检查处理函数
    return {"status": "ok"}  # 返回服务正常状态


@app.get("/api/employees", response_model=EmployeeListResponse)  # 注册带响应模型的员工列表接口
def list_employees(keyword: str | None = None, page: int = 1, size: int = 20):  # 接收查询和分页参数
    active_employees = [  # 创建只包含在职员工的新列表
        employee for employee in employees if employee["is_active"]  # 筛选在职员工
    ]  # 结束在职员工筛选

    if keyword:  # 只有提供关键字时才执行搜索
        active_employees = [  # 保存符合关键字的员工
            employee  # 返回当前符合条件的员工
            for employee in active_employees  # 遍历所有在职员工
            if keyword.lower() in employee["name"].lower()  # 按姓名忽略大小写搜索
            or keyword.lower() in employee["employee_number"].lower()  # 或按员工编号搜索
        ]  # 结束关键字筛选

    start = (page - 1) * size  # 计算当前页第一条数据的位置
    end = start + size  # 计算当前页切片的结束位置
    items = [  # 组装当前页的员工响应
        build_employee_response(employee)  # 为员工添加部门摘要
        for employee in active_employees[start:end]  # 遍历当前页员工
    ]  # 结束员工响应列表

    return {  # 返回列表数据和分页信息
        "items": items,  # 返回当前页员工响应
        "total": len(active_employees),  # 返回筛选后的总件数
        "page": page,  # 返回当前页码
        "size": size,  # 返回每页数量
    }  # 结束响应字典


@app.get(  # 开始配置员工详情接口
    "/api/employees/{employee_number}",  # 声明包含员工编号的路径
    response_model=EmployeeResponse,  # 使用员工响应模型检查输出
)  # 完成员工详情接口配置
def get_employee(employee_number: EmployeeNumberPath):  # 接收并校验路径中的员工编号
    for employee in employees:  # 逐个查找员工
        if employee["employee_number"] == employee_number and employee["is_active"]:  # 匹配在职员工
            return build_employee_response(employee)  # 返回带部门摘要的员工

    raise HTTPException(status_code=404, detail="Employee not found")  # 未找到时返回 404


@app.post(  # 开始配置员工新增接口
    "/api/employees",  # 声明员工集合路径
    response_model=EmployeeResponse,  # 使用员工响应模型检查输出
    status_code=201,  # 设置新增成功状态码
)  # 完成员工新增接口配置
def create_employee(employee: EmployeeCreate):  # 接收并校验新增请求体
    for current in employees:  # 遍历已有员工
        if current["employee_number"] == employee.employee_number:  # 检查员工编号是否重复
            raise HTTPException(  # 创建业务校验异常
                status_code=400,  # 设置错误状态码
                detail="Employee number already exists",  # 设置重复编号错误信息
            )  # 结束异常配置

    if find_department(employee.department_id) is None:  # 检查部门是否存在
        raise HTTPException(status_code=400, detail="Department not found")  # 不存在时返回 400

    new_employee = employee.model_dump()  # 把请求模型转换成字典
    new_employee["id"] = max(item["id"] for item in employees) + 1  # 生成下一个员工主键
    new_employee["is_active"] = True  # 将新员工设为在职
    employees.append(new_employee)  # 把新员工保存到内存列表

    return build_employee_response(new_employee)  # 返回带部门摘要的新员工


@app.put(  # 开始配置员工修改接口
    "/api/employees/{employee_number}",  # 声明包含员工编号的路径
    response_model=EmployeeResponse,  # 使用员工响应模型检查输出
)  # 完成员工修改接口配置
def update_employee(  # 定义员工修改处理函数
    employee_number: EmployeeNumberPath,  # 接收并校验路径中的员工编号
    employee: EmployeeUpdate,  # 接收并校验修改请求体
):  # 结束函数参数声明
    if find_department(employee.department_id) is None:  # 检查部门是否存在
        raise HTTPException(status_code=400, detail="Department not found")  # 不存在时返回 400

    for current in employees:  # 遍历已有员工
        if current["employee_number"] == employee_number and current["is_active"]:  # 匹配在职员工
            current.update(employee.model_dump())  # 使用请求数据更新员工
            return build_employee_response(current)  # 返回带部门摘要的员工

    raise HTTPException(status_code=404, detail="Employee not found")  # 未找到时返回 404


@app.delete(  # 开始配置员工逻辑删除接口
    "/api/employees/{employee_number}",  # 声明包含员工编号的路径
    status_code=204,  # 设置删除成功状态码
)  # 完成员工删除接口配置
def delete_employee(employee_number: EmployeeNumberPath):  # 接收并校验路径中的员工编号
    for employee in employees:  # 遍历已有员工
        if employee["employee_number"] == employee_number and employee["is_active"]:  # 匹配在职员工
            employee["is_active"] = False  # 把员工状态改为离职
            return Response(status_code=204)  # 返回无响应体的 204

    raise HTTPException(status_code=404, detail="Employee not found")  # 未找到时返回 404


@app.get("/api/departments", response_model=list[DepartmentSummary])  # 注册带响应模型的部门列表接口
def list_departments():  # 定义部门列表处理函数
    return departments  # 返回全部部门数据
```

### 代码说明

一次新增员工请求按下面的顺序执行：

```text
JSON请求体
→ EmployeeCreate校验并转换
→ 路由函数检查员工编号和部门
→ 内存数据发生变化
→ EmployeeResponse检查并过滤响应
→ JSON响应
```

Pydantic负责单次输入即可判断的结构规则：

| 规则 | 接受的值或示例 | 作用 |
| --- | --- | --- |
| 必填字段 | `name`、`department_id`、`joined_on` | 拒绝缺少必要字段的请求 |
| 类型转换 | `"2024-06-01"`转换为`date` | 把JSON值转换为Python类型 |
| 长度和范围 | 姓名最多100个字符，部门ID至少为1 | 限制字段边界 |
| 格式 | 员工编号符合`E`加数字的格式 | 拒绝不符合规则的字符串 |
| 输出结构 | 响应包含`id`、`is_active`等字段 | 过滤并检查返回数据 |

员工编号是否重复、部门是否真实存在，需要读取当前数据，属于业务校验，由路由函数处理。

`response_model=EmployeeResponse` 会：

- 检查路由返回的数据能否组成合法员工响应
- 过滤响应模型中没有声明的字段
- 在 `/docs` 中生成明确的响应结构
- 把 `date` 等 Python 对象序列化成 JSON

当前响应数据来自字典，不需要 `from_attributes=True`。当响应数据来自对象属性时，需要通过该配置允许 Pydantic 读取属性。

如果路由返回值不符合 `response_model`，说明服务端代码没有遵守自己声明的响应结构。FastAPI 会把它视为服务端错误，而不是客户端请求错误，因此不会返回请求校验使用的 `422`。

数据库记录保留 `department_id`，`build_employee_response()` 在响应中增加 `department` 摘要，便于调用方直接显示部门名称。

列表函数中的 `page` 和 `size` 仍使用基础查询参数写法，没有限制最小值。因此本示例只使用正整数测试分页；实际数据库列表接口还应通过 `Query` 等方式限制 `page >= 1` 和 `1 <= size <= 100`。

## 三、查看 OpenAPI

启动应用后先访问：

```text
http://127.0.0.1:8000/openapi.json
```

在返回的 JSON 中搜索 `EmployeeCreate`，可以看到类似结构：

```json
{
  "EmployeeCreate": {
    "properties": {
      "name": {
        "type": "string",
        "maxLength": 100,
        "minLength": 1,
        "description": "员工姓名"
      },
      "employee_number": {
        "type": "string",
        "maxLength": 20,
        "minLength": 2,
        "pattern": "^E[0-9]+$",
        "description": "员工编号"
      }
    }
  }
}
```

这段 JSON 是完整 OpenAPI 文档中的局部示例，实际内容还包括其他字段、必填字段列表、接口路径、请求方法、状态码和响应结构。

OpenAPI、JSON Schema 和 Swagger UI 的关系：

```text
Pydantic模型与路由配置
→ FastAPI生成JSON Schema
→ FastAPI把Schema放入/openapi.json
→ Swagger UI读取OpenAPI
→ /docs显示可交互接口文档
```

| 对象 | 地址或来源 | 作用 |
| --- | --- | --- |
| OpenAPI 文档 | `/openapi.json` | 以机器可读形式描述路径、参数、请求体和响应 |
| JSON Schema | OpenAPI 的 `components.schemas` | 描述 Pydantic 模型的字段、类型和约束 |
| Swagger UI | `/docs` | 读取 OpenAPI，并提供浏览和发送请求的页面 |

`Field(description=...)` 和 `Path(description=...)` 提供字段或参数说明，`response_model` 提供响应 Schema，路由装饰器提供路径、方法和成功状态码。修改这些代码后，OpenAPI 和 `/docs` 会同步变化。

## 四、校验错误与业务错误

### 1. 结构校验错误：422

发送下面的新增请求：

```json
{
  "employee_number": "A003",
  "name": "",
  "department_id": 1,
  "joined_on": "2024/06/01"
}
```

FastAPI 会返回 `422`。响应中的 `loc` 表示错误位置，`msg` 表示错误原因：

```json
{
  "detail": [
    {
      "type": "string_pattern_mismatch",
      "loc": ["body", "employee_number"],
      "msg": "String should match pattern '^E[0-9]+$'"
    }
  ]
}
```

实际响应还会包含姓名和日期的错误。查看每一项的 `loc`，可以确认错误来自请求体中的哪个字段。

### 2. 业务校验错误：400

下面两种输入的 JSON 结构本身合法，但不符合当前业务数据：

- 使用已经存在的员工编号
- 使用不存在的部门 ID

因此路由函数返回 `400`，而不是 Pydantic 的 `422`。结构校验不能代替数据库唯一约束和关联数据检查；接入数据库后，这些规则还需要由 Service 和数据库共同保证。

## 五、使用 `/docs` 验证

在项目根目录启动应用：

```powershell
uvicorn app.main:app --reload
```

打开 `http://127.0.0.1:8000/docs`，按顺序完成：

1. 请求 `GET /api/employees/E001`，确认响应同时包含 `department_id` 和部门摘要。
2. 新增 `e003`，确认响应状态为 `201`，员工编号被保存为 `E003`。
3. 再次新增 `E003`，确认返回 `400`。
4. 使用 `department_id=999` 新增员工，确认返回 `400`。
5. 使用编号 `A004`、空姓名或日期 `2024/07/01`，确认返回 `422`，并检查 `loc`。
6. 请求 `GET /api/employees/e001`，确认路径格式不符合约定并返回 `422`。
7. 修改 `E001`，确认响应仍包含相同的员工编号。
8. 逻辑删除 `E001`，确认返回 `204`；再次查询时返回 `404`。

内存数据会在服务重启后恢复为 `E001`、`E002`。如果需要重新执行全部步骤，可以重启服务。

## 六、完成检查

- [ ] 请求体中的小写员工编号会被规范化
- [ ] 错误格式返回 `422`，重复编号和不存在部门返回 `400`
- [ ] 新增、修改和查询响应都符合 `EmployeeResponse`
- [ ] 数据记录保留 `department_id`，响应同时包含部门摘要
- [ ] 逻辑删除仍然只修改 `is_active`
- [ ] 文件中每种 HTTP 方法和路径组合只注册一次

完成后保留这份 `app/main.py`，并确认员工接口的URL、响应结构和状态码均可通过`/docs`重复验证。

## 七、本章总结

- Pydantic 模型负责请求和响应的数据结构
- `Field` 用于声明长度、范围、格式和文档信息
- `field_validator` 可以在校验前规范化输入
- 请求模型和响应模型应分开
- 结构错误通常返回 `422`，业务规则错误由应用代码处理
- Pydantic Schema 不代替 SQLAlchemy Model 或数据库约束
