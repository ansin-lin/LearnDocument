# 第2章 FastAPI 请求与响应

> 本章成果：使用内存数据完成员工列表、详情、新增、修改、逻辑删除和部门列表接口，并能解释每段代码中新出现的类、装饰器、参数和返回值。

本章内容较多，建议按三个检查点完成：先完成健康检查、列表和详情；再完成请求模型与新增、修改、删除；最后加入部门列表并执行全部HTTP验证。前一个检查点能够启动和返回预期结果后，再继续追加代码。

## 一、创建应用并准备内存数据

打开 `app/main.py`，整理为下面的起始状态：

```python
from datetime import date  # 导入日期类型，用于表示员工入职日期

from fastapi import FastAPI  # 导入 FastAPI 应用类

app = FastAPI()  # 创建 FastAPI 应用对象

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
        "joined_on": date(2026, 4, 1),  # 创建入职日期
        "is_active": True,  # 标记员工为在职
    },  # 结束第一名员工的数据
    {  # 开始第二名员工的数据
        "id": 2,  # 设置员工主键
        "employee_number": "E002",  # 设置员工编号
        "name": "佐藤花子",  # 设置员工姓名
        "department_id": 2,  # 关联营业部
        "email": "",  # 使用空字符串表示未填写邮箱
        "joined_on": date(2025, 10, 1),  # 创建入职日期
        "is_active": True,  # 标记员工为在职
    },  # 结束第二名员工的数据
]  # 结束员工列表
```

### 代码说明

`from datetime import date` 从 Python 标准库导入日期类。`date(2026, 4, 1)` 的三个位置参数依次是：

| 参数 | 当前值 | 可接受的值 | 作用 |
| --- | ---: | --- | --- |
| `year` | `2026` | `1`～`9999`的整数 | 年 |
| `month` | `4` | `1`～`12`的整数 | 月 |
| `day` | `1` | 当前年月中有效的日期整数 | 日 |

也可以写成 `date(year=2026, month=4, day=1)`。这三个参数都必须提供；

`FastAPI` 是创建整个 Web 应用的类。`app` 保存应用对象，后面的路由都会注册到这个对象。

当前代码使用 `FastAPI()` 的默认值。常用可选参数如下：

| 参数 | 可接受的值 | 作用 | 示例或默认值 |
| --- | --- | --- | --- |
| `title` | 字符串 | OpenAPI和`/docs`中的应用标题 | 默认`"FastAPI"` |
| `summary` | 字符串或`None` | 应用摘要 | 默认`None` |
| `description` | 字符串 | 应用详细说明，支持Markdown | 默认空字符串 |
| `version` | 字符串 | API版本说明 | 默认`"0.1.0"` |
| `docs_url` | 以`/`开头的路径字符串或`None` | Swagger UI路径；设为`None`可关闭 | 默认`"/docs"` |
| `redoc_url` | 以`/`开头的路径字符串或`None` | ReDoc路径；设为`None`可关闭 | 默认`"/redoc"` |
| `debug` | `True`或`False` | 是否启用调试模式 | 默认`False` |
| `dependencies` | `Depends(...)`组成的序列或`None` | 应用于全部路由的依赖列表 | 默认`None` |
| `middleware` | `Middleware(...)`组成的序列或`None` | 应用创建时注册的中间件列表 | 默认`None` |
| `exception_handlers` | 异常类型与处理函数组成的映射或`None` | 异常类型与处理函数的映射 | 默认`None` |
| `lifespan` | 异步上下文管理函数或`None` | 应用启动和关闭的生命周期函数 | 默认`None` |
| `default_response_class` | `Response`子类 | 设置默认响应类 | 默认`JSONResponse` |
| `redirect_slashes` | `True`或`False` | 是否自动处理路径末尾斜杠跳转 | 默认`True` |

本章使用表中出现的关键字参数；参数名拼写错误时，应用启动阶段会直接报错。

`departments` 和 `employees` 是普通 Python 列表，每一项是字典。它们只存在于当前程序进程中，服务重启后会恢复为代码中的初始值。

## 二、创建第一个路由

在 `app/main.py` 末尾追加：

```python
@app.get("/health")  # 把 GET /health 注册到下面的函数
def health_check():  # 定义健康检查处理函数
    return {"status": "ok"}  # 返回可以转换为 JSON 的字典
```

### 代码说明

`@app.get("/health")` 是路径操作装饰器。它把下面的 `health_check()` 函数注册为：

```text
HTTP方法：GET
URL路径：/health
处理函数：health_check
```

请求到达时，FastAPI根据“HTTP方法 + URL路径”选择函数。`GET /health`会执行该函数，`POST /health`不会执行它。

`app.get()`、`app.post()`、`app.put()`和`app.delete()`使用相同类型的配置参数：

| 参数组 | 参数 | 可接受的值 | 作用 |
| --- | --- | --- | --- |
| 路径 | `path` | 路径字符串，例如`"/api/employees"` | 必填路径字符串，是唯一可以直接放在第一个位置的参数 |
| 响应 | `response_model` | Pydantic模型类、`list[模型类]`、`dict`等有效字段类型，或`None` | 声明、校验、转换和过滤响应结构 |
| 响应 | `status_code` | 整数状态码、`IntEnum`状态码，或`None` | 声明默认成功状态码 |
| 文档 | `tags` | 字符串列表，例如`["employees"]`，或`None` | 在`/docs`中对接口分组 |
| 文档 | `summary` | 字符串，或`None` | 接口摘要 |
| 文档 | `description` | 字符串，或`None`；支持Markdown | 接口详细说明 |
| 文档 | `response_description` | 字符串；默认`"Successful Response"` | 默认成功响应的说明 |
| 文档 | `deprecated` | `True`、`False`或`None`；默认`None` | 标记接口已废弃 |
| 文档 | `include_in_schema` | `True`或`False`；默认`True` | 是否显示在OpenAPI文档中 |
| 额外响应 | `responses` | 以状态码为键的字典，或`None` | 声明其他状态码及其响应说明 |
| 依赖 | `dependencies` | `Depends(...)`组成的列表，或`None` | 在进入函数前执行依赖 |
| 响应类 | `response_class` | `JSONResponse`、`FileResponse`、`HTMLResponse`等响应类 | 指定响应的类型 |
| 响应过滤 | `response_model_include`、`response_model_exclude` | 字段名集合、嵌套字段字典，或`None` | 返回的响应数据中只包含或排除指定响应字段 |
| 响应过滤 | `response_model_by_alias` | `True`或`False`；默认`True` | 是否使用字段别名 |
| 响应过滤 | `response_model_exclude_unset` | `True`或`False`；默认`False` | 返回的响应数据中是否排除未显式赋值字段 |
| 响应过滤 | `response_model_exclude_none` | `True`或`False`；默认`False` | 返回的响应数据中是否排除值为`None`的字段 |

这些参数中，`path`写在前面；其余参数应按名称传递，例如`status_code=201`。接口不接受任意的未知`**kwargs`，拼错参数名会在启动时产生错误。

`health_check()`没有参数，表示这个接口不接收路径参数、查询参数或请求体。普通Python函数可以定义`*args`和`**kwargs`，但FastAPI依靠明确的函数签名生成校验和文档，因此路由函数需要把请求参数逐个写清楚。

返回字典时，FastAPI会把它转换为JSON，并默认返回`200 OK`。

## 三、实现员工列表和查询参数

继续追加：

```python
@app.get("/api/employees")  # 注册员工列表接口
def list_employees(keyword: str | None = None, page: int = 1, size: int = 20):  # 接收查询参数
    active_employees = [  # 创建只包含在职员工的新列表
        employee for employee in employees if employee["is_active"]  # 筛选在职员工
    ]  # 结束在职员工筛选

    if keyword:  # 只有提供关键字时才执行搜索
        active_employees = [  # 保存符合关键字的员工
            employee  # 返回当前符合条件的员工
            for employee in active_employees  # 遍历所有在职员工
            if keyword.lower() in employee["name"].lower()  # 按姓名搜索
            or keyword.lower() in employee["employee_number"].lower()  # 按员工编号搜索
        ]  # 结束关键字筛选

    start = (page - 1) * size  # 计算当前页第一条数据的位置
    end = start + size  # 计算当前页切片的结束位置

    return {  # 返回列表数据和分页信息
        "items": active_employees[start:end],  # 返回当前页员工
        "total": len(active_employees),  # 返回筛选后的总件数
        "page": page,  # 返回当前页码
        "size": size,  # 返回每页数量
    }  # 结束响应字典
```

### 代码说明

装饰器路径中没有`{keyword}`、`{page}`或`{size}`，并且它们是`str`、`int`等简单类型，所以FastAPI把它们识别为查询参数。

| 函数参数 | 类型与默认值 | 可接受的值 | 请求示例 | 作用 |
| --- | --- | --- | --- | --- |
| `keyword` | `str \| None = None` | 任意字符串，或省略 | `?keyword=山田` | 可选搜索关键字 |
| `page` | `int = 1` | 能转换为整数的查询参数 | `?page=2` | 页码，省略时为1 |
| `size` | `int = 20` | 能转换为整数的查询参数 | `?size=10` | 每页件数，省略时为20 |

`str | None`表示值可以是字符串，也可以是`None`。`= None`表示客户端可以省略该参数。

普通Python调用可以写成：

```python
list_employees("山田", 1, 20)  # 使用位置参数调用普通 Python 函数
list_employees(keyword="山田", page=1, size=20)  # 使用关键字参数调用
```

### 调用参数说明

- 位置参数按顺序对应`keyword`、`page`、`size`。
- 关键字参数按名称匹配，不依赖书写顺序。
- `*args`用于收集没有逐个声明的位置参数。
- `**kwargs`用于收集没有逐个声明的关键字参数。
- FastAPI路由函数使用明确参数接收HTTP数据；`*args`和`**kwargs`不能提供固定的字段名、类型、来源和OpenAPI结构。

HTTP请求不是直接执行上面的普通Python调用。FastAPI读取URL后，按照函数签名转换类型并传入对应参数。

列表推导式`[结果 for 元素 in 列表 if 条件]`用于筛选数据。`active_employees[start:end]`使用切片取得当前页数据。

访问：

```text
GET /api/employees?keyword=山田&page=1&size=20
```

`?`表示查询参数开始，多个参数使用`&`连接。

## 四、实现员工详情和错误响应

员工不存在时需要返回`404`。先把文件顶部的FastAPI导入修改为：

```python
from fastapi import FastAPI, HTTPException  # 增加 HTTP 异常类
```

### 导入说明

一条`from ... import ...`可以导入多个公开名称，名称之间使用逗号分隔。当前代码需要`HTTPException`；`Response`只在构造空响应时才需要。

然后追加：

```python
@app.get("/api/employees/{employee_number}")  # 声明员工编号路径参数
def get_employee(employee_number: str):  # 接收路径中的员工编号
    for employee in employees:  # 逐个查找员工
        if employee["employee_number"] == employee_number and employee["is_active"]:  # 匹配在职员工
            return employee  # 返回找到的员工

    raise HTTPException(status_code=404, detail="Employee not found")  # 中断请求并返回 404
```

### 详情接口说明

`{employee_number}`是路径占位符，函数中必须使用同名参数接收。请求`/api/employees/E001`时，`employee_number`得到字符串`"E001"`。

`HTTPException`的正式参数是：

| 参数 | 是否必填 | 可接受的值 | 作用 |
| --- | --- | --- | --- |
| `status_code` | 是 | 整数HTTP状态码 | 返回给客户端的HTTP状态码 |
| `detail` | 否 | 任意可转换为JSON的数据，或`None` | 放入JSON响应的`detail`字段，作为错误信息 |
| `headers` | 否 | 字符串键值映射，或`None` | 增加额外响应头 |

可以使用位置参数`HTTPException(404, "Employee not found")`，但关键字写法更容易看懂。

这里必须使用`raise`，不能写成`return HTTPException(...)`。抛出后，当前函数剩余代码停止执行，FastAPI生成错误响应。

分别访问：

```text
GET /api/employees/E001
GET /api/employees/E999
```

第一个请求返回员工数据；第二个请求返回`404`和`{"detail":"Employee not found"}`。

## 五、定义新增请求模型

先在导入区域增加：

```python
from pydantic import BaseModel  # 导入 Pydantic 数据模型基类
```

### 导入说明

`BaseModel`不是FastAPI类，而是Pydantic提供的模型基类。FastAPI使用Pydantic模型解析、转换和校验请求体。

在`app = FastAPI()`后面加入：

```python
class EmployeeCreate(BaseModel):  # 继承 BaseModel，定义新增请求体
    employee_number: str  # 员工编号是必填字符串
    name: str  # 员工姓名是必填字符串
    department_id: int  # 部门 ID 是必填整数
    email: str = ""  # 邮箱可以省略，默认使用空字符串
    joined_on: date  # 入职日期是必填日期
```

### 模型说明

继承`BaseModel`后，类型标注会成为模型字段。没有默认值的字段必须提供，有默认值的字段可以省略。

Pydantic模型实例的创建形式可以理解为：

```python
employee = EmployeeCreate(  # 使用关键字参数创建并校验模型
    employee_number="E003",  # 传入员工编号
    name="Suzuki",  # 传入姓名
    department_id=1,  # 传入部门 ID
    joined_on="2026-04-01",  # 传入可转换为日期的字符串
)  # 完成模型创建
```

### 调用参数说明

`BaseModel`生成的初始化方法接收`**data`：

- `**data`表示字段名与字段值组成的任意关键字参数。
- 这里允许的业务字段由`EmployeeCreate`中的类型标注决定。
- `employee_number`等字段不是`BaseModel`固定参数，而是当前子类声明的字段。
- 缺少必填字段、类型无法转换时，Pydantic产生校验错误。
- HTTP请求中不需要手动调用`EmployeeCreate(**data)`，FastAPI会根据JSON请求体完成调用。

`joined_on`会从`"2026-04-01"`转换为`date`对象。当前模型只声明基本类型和必填关系，不限制字符串长度或额外字段。

## 六、实现新增接口

继续追加：

```python
@app.post("/api/employees", status_code=201)  # 注册新增接口并声明成功状态码
def create_employee(employee: EmployeeCreate):  # 接收并校验 JSON 请求体
    for current in employees:  # 遍历已有员工
        if current["employee_number"] == employee.employee_number:  # 检查员工编号是否重复
            raise HTTPException(status_code=400, detail="Employee number already exists")  # 返回业务错误

    new_employee = employee.model_dump()  # 把请求模型转换成字典
    new_employee["id"] = max(item["id"] for item in employees) + 1  # 生成下一个员工主键
    new_employee["is_active"] = True  # 将新员工设为在职
    employees.append(new_employee)  # 把新员工保存到内存列表

    return new_employee  # 返回新增后的完整员工数据
```

### 代码说明

`@app.post()`处理`POST`请求，通常用于新增资源。它与`app.get()`的可传参数相同。当前代码传入：

| 参数 | 当前值 | 可接受的值 | 作用 |
| --- | --- | --- | --- |
| `path` | `"/api/employees"` | 路径字符串 | 声明接口路径 |
| `status_code` | `201` | 整数或`IntEnum`状态码，或`None` | 设置成功响应状态码，并写入OpenAPI文档 |

`employee: EmployeeCreate`是请求体参数。FastAPI看到Pydantic模型类型后，会读取JSON、创建模型对象并把它传给函数。校验失败时，函数不会执行，FastAPI直接返回`422`。

`employee.model_dump()`把模型转换为字典。常用关键字参数包括：

| 参数 | 默认值 | 可接受的值 | 作用 |
| --- | --- | --- | --- |
| `mode` | `"python"` | `"python"`、`"json"`或受支持的序列化模式字符串 | 控制输出保留Python对象还是只产生JSON兼容值 |
| `include` | `None` | 字段名集合、嵌套字段字典，或`None` | 只输出指定字段 |
| `exclude` | `None` | 字段名集合、嵌套字段字典，或`None` | 排除指定字段 |
| `by_alias` | `None` | `True`、`False`或`None` | 是否使用字段别名作为键 |
| `exclude_unset` | `False` | `True`或`False` | 是否排除没有显式提交的字段 |
| `exclude_defaults` | `False` | `True`或`False` | 是否排除等于默认值的字段 |
| `exclude_none` | `False` | `True`或`False` | 是否排除值为`None`的字段 |
| `round_trip` | `False` | `True`或`False` | 是否生成适合再次校验的值 |

`model_dump()`的参数位于`*`之后，必须按关键字传递，例如`model_dump(exclude_none=True)`；不能把它们当成任意位置`*args`。

`max(item["id"] for item in employees)`使用生成器表达式取得最大ID。`list.append(value)`只接收一个要追加的对象，代码把完整员工字典追加到列表末尾。

请求体示例：

```json
{
  "employee_number": "E003",
  "name": "Suzuki",
  "department_id": 1,
  "email": "suzuki@example.test",
  "joined_on": "2026-04-01"
}
```

字段名必须与`EmployeeCreate`一致。正常新增返回`201`；重复编号返回`400`；缺少必填字段或日期无法转换时返回`422`。

## 七、实现修改接口

先在`EmployeeCreate`后面加入修改请求模型：

```python
class EmployeeUpdate(BaseModel):  # 定义修改员工的请求体
    name: str  # 接收修改后的姓名
    department_id: int  # 接收修改后的部门 ID
    email: str = ""  # 接收修改后的邮箱
    joined_on: date  # 接收修改后的入职日期
```

### 模型说明

`EmployeeUpdate`同样继承`BaseModel`，因此初始化方式、`**data`字段传递、类型转换和错误处理与`EmployeeCreate`相同。

修改模型不包含`employee_number`，表示员工编号创建后不能通过这个接口修改。请求模型应只开放当前操作允许修改的字段。

然后追加：

```python
@app.put("/api/employees/{employee_number}")  # 注册员工修改接口
def update_employee(employee_number: str, employee: EmployeeUpdate):  # 接收路径参数和请求体
    for current in employees:  # 遍历已有员工
        if current["employee_number"] == employee_number and current["is_active"]:  # 匹配在职员工
            current.update(employee.model_dump())  # 使用请求字段更新员工字典
            return current  # 返回修改后的员工

    raise HTTPException(status_code=404, detail="Employee not found")  # 未找到时返回 404
```

### 修改接口说明

`@app.put()`处理`PUT`请求，通常用于修改指定资源。路径中的`employee_number`确定修改对象，请求体中的`employee`提供新值。

一个路由函数可以同时接收多种来源的参数：

| 函数参数 | 来源 | 可接受的值 | 判断依据 |
| --- | --- | --- | --- |
| `employee_number` | 路径参数 | 字符串 | 出现在`{employee_number}`中 |
| `employee` | JSON请求体 | 符合`EmployeeUpdate`字段结构的JSON对象 | 类型是Pydantic模型 |

`dict.update(mapping)`使用另一个映射中的键值更新当前字典。它可以接收一个映射位置参数，也可以接收`**kwargs`，例如`current.update(name="New Name")`。当前代码传入`model_dump()`产生的字典。

请求体示例：

```json
{
  "name": "山田太郎（更新）",
  "department_id": 1,
  "email": "yamada.updated@example.com",
  "joined_on": "2026-04-01"
}
```

修改成功默认返回`200`。员工不存在或已经离职时返回`404`。

## 八、实现逻辑删除

删除成功时需要返回没有响应体的`204`。先修改导入：

```python
from fastapi import FastAPI, HTTPException, Response  # 增加基础响应类
```

### 导入说明

`Response`可以直接创建并返回HTTP响应。构造没有响应体的`204`响应时需要导入这个类。

然后追加：

```python
@app.delete("/api/employees/{employee_number}", status_code=204)  # 注册逻辑删除接口
def delete_employee(employee_number: str):  # 接收路径中的员工编号
    for employee in employees:  # 遍历已有员工
        if employee["employee_number"] == employee_number and employee["is_active"]:  # 匹配在职员工
            employee["is_active"] = False  # 把员工状态改为离职
            return Response(status_code=204)  # 返回没有响应体的 204

    raise HTTPException(status_code=404, detail="Employee not found")  # 未找到时返回 404
```

### 删除接口说明

`@app.delete()`处理`DELETE`请求。当前代码采用逻辑删除，不从列表中移除字典，只把`is_active`改为`False`。

`Response`可传参数如下：

| 参数 | 默认值 | 可接受的值 | 作用 |
| --- | --- | --- | --- |
| `content` | `None` | 字符串、字节数据、内存视图，或`None` | 响应体内容 |
| `status_code` | `200` | 整数HTTP状态码 | 设置HTTP状态码 |
| `headers` | `None` | 字符串键值映射，或`None` | 设置自定义响应头 |
| `media_type` | `None` | 媒体类型字符串，或`None` | 设置响应媒体类型，例如`text/plain` |
| `background` | `None` | `BackgroundTask`对象，或`None` | 设置响应发送后执行的后台任务 |

`content`可以放在第一个位置，其他配置建议按名称传递。当前使用`Response(status_code=204)`，可以直接看出该响应只设置状态码且不包含正文。

装饰器中的`status_code=204`负责声明接口文档和默认成功状态，返回的`Response(status_code=204)`负责构造本次实际响应。`204 No Content`不能包含响应体，因此`content`保持默认`None`。

## 九、实现部门列表

继续追加：

```python
@app.get("/api/departments")  # 注册部门列表接口
def list_departments():  # 定义部门列表处理函数
    return departments  # 返回全部部门数据
```

### 代码说明

这个接口再次使用`@app.get()`，没有出现新的装饰器参数。函数没有参数，所以不读取路径参数、查询参数或请求体。

返回列表时，FastAPI会遍历其中的字典，并生成JSON数组：

```json
[
  {"id": 1, "name": "开发部"},
  {"id": 2, "name": "营业部"}
]
```

Python列表对应JSON数组，Python字典对应JSON对象。

## 十、启动并验证全部接口

在项目根目录执行：

```powershell
uvicorn app.main:app --reload
```

命令参数说明：

| 部分 | 可接受的值 | 作用 |
| --- | --- | --- |
| `app.main` | 可导入模块路径 | 导入`app/main.py`模块 |
| `:app` | 模块中存在的ASGI应用对象名称 | 从模块中取得名为`app`的FastAPI对象 |
| `--reload` | 提供该开关或省略 | 监视代码变化并自动重启，仅用于开发环境 |

打开`http://127.0.0.1:8000/docs`，依次验证：

1. `GET /health`返回`200`。
2. `GET /api/departments`返回部门列表。
3. `GET /api/employees?keyword=山田&page=1&size=20`返回分页结构。
4. `GET /api/employees/E001`返回员工详情。
5. `GET /api/employees/E999`返回`404`。
6. `POST /api/employees`新增`E003`并返回`201`。
7. 再次新增`E003`返回`400`。
8. 缺少`joined_on`时返回`422`。
9. `PUT /api/employees/E001`修改员工并返回`200`。
10. `DELETE /api/employees/E001`返回`204`且响应体为空。
11. 再次查询`E001`返回`404`。

## 十一、常见错误

| 现象 | 原因 | 修正 |
| --- | --- | --- |
| 接口没有出现在`/docs` | 装饰器没有紧挨函数 | 检查装饰器和函数位置 |
| 返回`405` | URL正确但HTTP方法错误 | 检查GET、POST、PUT、DELETE |
| 路径参数收不到 | 占位符与函数参数不同名 | 统一`employee_number` |
| 查询参数没有生效 | URL参数名写错 | 检查`keyword`、`page`、`size` |
| 请求体返回`422` | 缺少字段或类型、日期错误 | 查看错误响应中的`loc`和`msg` |
| `BaseModel`未定义 | 使用前没有导入 | 添加Pydantic导入 |
| `HTTPException`未定义 | 使用前没有导入 | 更新FastAPI导入 |
| 删除响应仍有内容 | 返回了字典 | 返回`Response(status_code=204)` |
| 重启后新增数据消失 | 数据只保存在内存列表 | 内存数据不会持久化 |

## 十二、练习

1. 为员工列表增加可选的`department_id`查询参数。
2. 分别用位置参数和关键字参数直接调用`list_employees()`，比较结果。
3. 说明为什么不使用`def list_employees(*args, **kwargs)`接收HTTP参数。
4. 使用`HTTPException`的`headers`参数增加一个练习响应头。
5. 使用`model_dump(exclude_defaults=True)`观察默认邮箱是否保留。
6. 删除员工后，确认字典仍然存在但`is_active`已经变为`False`。

## 十三、本章总结

- 路由装饰器把HTTP方法、路径和处理函数绑定在一起。
- 路径参数、查询参数和请求体通过明确的函数签名声明。
- 路由函数不应使用任意`*args`、`**kwargs`代替明确的请求参数。
- Pydantic模型通过`BaseModel.__init__(**data)`接收字段并完成校验。
- `HTTPException`使用`status_code`、`detail`和`headers`描述预期错误。
- `Response`可以控制响应体、状态码、响应头、媒体类型和后台任务。
- 当前接口已经形成请求、处理、错误响应和成功响应的完整闭环。

## 参考资料

- [FastAPI：第一个接口与路径操作装饰器](https://fastapi.tiangolo.com/tutorial/first-steps/)
- [FastAPI：路径操作配置](https://fastapi.tiangolo.com/tutorial/path-operation-configuration/)
- [FastAPI：HTTPException](https://fastapi.tiangolo.com/reference/exceptions/)
- [FastAPI：Response](https://fastapi.tiangolo.com/reference/response/)
- [Pydantic：BaseModel与model_dump](https://docs.pydantic.dev/latest/api/base_model/)
