# 第14章 写入API与异常边界

> 本章成果：在第13章只读接口的稳定状态上增加员工新增、修改和逻辑删除接口，并把业务异常与数据库唯一约束异常转换为明确的HTTP响应。

## 一、本章开始状态

第13章已经完成并保留：

- `GET /api/employees`员工列表
- `GET /api/employees/{employee_number}`员工详情
- `GET /api/departments`部门列表
- `SessionDep`请求级数据库Session
- `EmployeeNotFoundError`的`404`异常处理器

本章不重复定义列表和详情路径函数，只在现有`employees.py`末尾追加三个写入接口。完整调用链保持不变：

```text
HTTP请求
→ Pydantic请求Schema
→ employees Router
→ EmployeeService事务
→ EmployeeRepository数据库操作
→ Pydantic响应Schema
```

## 二、增加写入请求Schema

文件：`app/schemas.py`  
操作：补充导入，并追加到文件末尾  
代码类型：项目代码片段

```python
from datetime import date  # 确认文件已经导入日期类型

from pydantic import BaseModel, ConfigDict, Field, field_validator  # 补充字段约束和校验器导入


class EmployeeWrite(BaseModel):  # 定义新增和修改共用的可写字段
    name: str = Field(min_length=1, max_length=100)  # 限制姓名长度
    department_id: int = Field(ge=1)  # 要求部门主键至少为1
    email: str = Field(default="", max_length=254)  # 允许空邮箱并限制最大长度
    joined_on: date  # 接收YYYY-MM-DD日期字符串并转换为date


class EmployeeCreate(EmployeeWrite):  # 定义新增员工请求体
    employee_number: str = Field(  # 增加只在创建时出现的员工编号
        min_length=2,  # 至少包含E和一位数字
        max_length=20,  # 限制数据库字段允许的最大长度
        pattern=r"^E[0-9]+$",  # 要求格式为E后接数字
    )  # 完成员工编号约束

    @field_validator("employee_number", mode="before")  # 在其他字段校验前整理员工编号
    @classmethod  # 让校验器接收模型类而不是模型实例
    def normalize_employee_number(cls, value: object) -> object:  # 接收原始输入值
        if isinstance(value, str):  # 只对字符串执行文本处理
            return value.strip().upper()  # 去除首尾空格并转换为大写
        return value  # 其他类型交给Pydantic继续校验


class EmployeeUpdate(EmployeeWrite):  # 定义修改员工请求体
    pass  # 不增加employee_number，防止修改业务编号
```

员工编号在创建后不能修改，因此`EmployeeUpdate`只继承公共可写字段。`EmployeeCreate`额外增加编号，并在正则校验前把` e010 `整理为`E010`。

本例使用的参数：

| 调用 | 参数 | 可接受的值 | 默认值或必填性 | 作用 |
| --- | --- | --- | --- | --- |
| `Field()` | `default` | 与字段类型兼容的值或Pydantic默认值标记 | 未传时字段必填；邮箱为`""` | 设置字段默认值 |
| `Field()` | `min_length` | 大于或等于0的整数 | 默认不限制 | 限制字符串最短长度 |
| `Field()` | `max_length` | 大于或等于0的整数 | 默认不限制 | 限制字符串最长长度 |
| `Field()` | `ge` | 与字段可比较的值 | 默认不限制 | 要求字段值大于或等于该值 |
| `Field()` | `pattern` | 正则表达式字符串 | 默认不限制 | 要求整个输入满足编号格式 |
| `field_validator()` | `field` | 当前模型中的字段名字符串 | 至少传一个 | 指定需要校验的字段 |
| `field_validator()` | `mode` | `"before"`、`"after"`、`"plain"`或`"wrap"` | 默认`"after"` | 决定校验器执行阶段 |

这些约束在进入路径函数前执行。字段缺失、类型错误或格式不正确时，FastAPI直接返回`422`，Service不会收到无效请求体。

## 三、新增员工接口

文件：`app/routers/employees.py`  
操作：在现有导入区加入`EmployeeCreate`和`EmployeeUpdate`，再把路径函数追加到文件末尾  
代码类型：项目代码片段

```python
@router.post("", response_model=EmployeeResponse, status_code=201)  # 注册新增接口并声明201
def create_employee(  # 定义新增员工路径函数
    request: EmployeeCreate,  # 接收并校验JSON请求体
    db: SessionDep,  # 注入当前请求使用的Session
):  # 返回值由EmployeeResponse转换和过滤
    service = EmployeeService(db)  # 创建使用当前Session的Service
    return service.create_employee(  # 调用新增员工事务
        employee_number=request.employee_number,  # 传递整理后的员工编号
        name=request.name,  # 传递姓名
        department_id=request.department_id,  # 传递部门主键
        email=request.email,  # 传递邮箱
        joined_on=request.joined_on,  # 传递已经转换的日期
    )  # 返回提交后的员工对象
```

`request`由FastAPI根据`EmployeeCreate`读取JSON请求体。Service完成编号检查、部门检查、`flush()`、`commit()`与失败回滚；Router不直接操作Session事务。

`@router.post()`本例参数：

| 参数 | 可接受的值 | 默认值或必填性 | 作用 |
| --- | --- | --- | --- |
| `path` | 路径字符串 | 必填；当前为空字符串 | 与Router前缀组合为`/api/employees` |
| `response_model` | Pydantic模型类型或兼容类型标注 | 默认`None` | 校验、转换并过滤成功响应 |
| `status_code` | 合法HTTP状态码整数 | 默认`200` | 声明新增成功状态码为`201` |

## 四、修改员工接口

文件：`app/routers/employees.py`  
操作：继续追加  
代码类型：项目代码片段

```python
@router.put("/{employee_number}", response_model=EmployeeResponse)  # 注册员工修改接口
def update_employee(  # 定义修改员工路径函数
    employee_number: str,  # 读取路径中的员工编号
    request: EmployeeUpdate,  # 接收允许修改的JSON字段
    db: SessionDep,  # 注入当前请求使用的Session
):  # 返回值由EmployeeResponse转换和过滤
    service = EmployeeService(db)  # 创建使用当前Session的Service
    return service.update_employee(  # 调用修改员工事务
        employee_number=employee_number,  # 使用路径编号定位员工
        name=request.name,  # 传递新姓名
        department_id=request.department_id,  # 传递新部门主键
        email=request.email,  # 传递新邮箱
        joined_on=request.joined_on,  # 传递新入职日期
    )  # 返回提交后的员工对象
```

员工编号只用于定位记录，不从请求体接收，因此客户端不能通过修改请求改变业务编号。目标员工或部门不存在时，Service抛出相应业务异常。

## 五、逻辑删除接口

文件：`app/routers/employees.py`  
操作：在导入区增加`Response`，并在文件末尾继续追加  
代码类型：项目代码片段

```python
from fastapi import APIRouter, Response  # 在现有APIRouter导入中增加Response


@router.delete("/{employee_number}", status_code=204)  # 注册员工逻辑删除接口
def deactivate_employee(  # 定义逻辑删除路径函数
    employee_number: str,  # 读取路径中的员工编号
    db: SessionDep,  # 注入当前请求使用的Session
):  # 成功时返回空响应
    service = EmployeeService(db)  # 创建使用当前Session的Service
    service.deactivate_employee(employee_number)  # 提交is_active=False状态
    return Response(status_code=204)  # 返回204且不包含响应体
```

`Response()`用于直接创建基础HTTP响应。当前只传入`status_code=204`，不传`content`，因此响应体为空。它还可以接收：

| 参数 | 可接受的值 | 默认值或必填性 | 作用 |
| --- | --- | --- | --- |
| `content` | 字符串、字节或`None` | 默认`None` | 设置原始响应体 |
| `status_code` | 合法HTTP状态码整数 | 默认`200` | 设置响应状态码 |
| `headers` | 字符串键值映射或`None` | 默认`None` | 增加响应头 |
| `media_type` | MIME类型字符串或`None` | 默认`None` | 设置响应内容类型 |
| `background` | Starlette后台任务对象或`None` | 默认`None` | 响应发送后执行后台任务 |

删除成功后数据库记录仍然存在，只是`is_active=False`。员工列表和详情默认只查询在职员工，因此之后不会再返回该员工。

## 六、补齐HTTP异常转换

第13章已经处理`EmployeeNotFoundError`。现在为员工编号冲突、部门不存在和数据库唯一约束冲突增加处理器。

文件：`app/main.py`  
操作：保留第13章处理器，把原有`EmployeeNotFoundError`单行导入替换为下面的分组导入，再追加三个处理器  
代码类型：项目代码片段

```python
from sqlalchemy.exc import IntegrityError  # 导入数据库完整性约束异常

from app.services.employee_service import (  # 导入需要转换的业务异常
    DepartmentNotFoundError,  # 部门不存在异常
    EmployeeAlreadyExistsError,  # 员工编号重复异常
    EmployeeNotFoundError,  # 保留第13章员工不存在异常
)  # 完成业务异常导入


@app.exception_handler(EmployeeAlreadyExistsError)  # 注册员工编号冲突处理器
def handle_employee_already_exists(  # 把业务冲突转换为409
    _request: Request,  # 接收当前请求但暂不读取
    _exc: EmployeeAlreadyExistsError,  # 接收编号冲突异常
) -> JSONResponse:  # 返回JSON错误响应
    return JSONResponse(  # 创建冲突响应
        status_code=409,  # 设置Conflict状态码
        content={"detail": "employee number already exists"},  # 返回稳定错误消息
    )  # 完成冲突响应


@app.exception_handler(DepartmentNotFoundError)  # 注册部门不存在处理器
def handle_department_not_found(  # 把无效部门转换为400
    _request: Request,  # 接收当前请求但暂不读取
    _exc: DepartmentNotFoundError,  # 接收部门不存在异常
) -> JSONResponse:  # 返回JSON错误响应
    return JSONResponse(  # 创建错误请求响应
        status_code=400,  # 设置Bad Request状态码
        content={"detail": "department not found"},  # 返回稳定错误消息
    )  # 完成错误请求响应


@app.exception_handler(IntegrityError)  # 注册数据库完整性异常处理器
def handle_integrity_error(  # 把并发下的约束冲突转换为409
    _request: Request,  # 接收当前请求但暂不读取
    _exc: IntegrityError,  # 接收数据库完整性异常
) -> JSONResponse:  # 返回JSON错误响应
    return JSONResponse(  # 创建数据库冲突响应
        status_code=409,  # 设置Conflict状态码
        content={"detail": "database constraint conflict"},  # 隐藏SQL和内部约束名称
    )  # 完成数据库冲突响应
```

应用层预检查可以让常见错误更清晰，数据库约束负责并发条件下的最终完整性。Service在重新抛出写入异常前已经执行`rollback()`；异常处理器只负责生成HTTP响应，不能继续使用失败事务中的Session。

本章形成两类异常边界：

| 异常来源 | 异常类型 | HTTP状态码 | 响应含义 |
| --- | --- | --- | --- |
| Service | `EmployeeNotFoundError` | `404` | 员工不存在或已经离职 |
| Service | `EmployeeAlreadyExistsError` | `409` | 员工编号已被使用 |
| Service | `DepartmentNotFoundError` | `400` | 请求中的部门不存在 |
| 数据库 | `IntegrityError` | `409` | 唯一约束等完整性冲突 |
| Pydantic/FastAPI | 请求校验错误 | `422` | 请求体或参数不符合Schema |

第17章会把这些基础处理器整理为统一错误结构，增加`request_id`和服务端日志，但不会改变Service异常与HTTP层之间的职责边界。

## 七、按顺序验证写入流程

启动应用后，使用`/docs`按顺序验证：

```text
GET    /api/employees
POST   /api/employees
GET    /api/employees/E010
PUT    /api/employees/E010
DELETE /api/employees/E010
GET    /api/employees/E010
GET    /api/employees
```

新增请求体：

```json
{
  "employee_number": "E010",
  "name": "Suzuki",
  "department_id": 1,
  "email": "suzuki@example.test",
  "joined_on": "2026-04-01"
}
```

预期结果：

1. 新增返回`201`和完整员工响应。
2. 详情返回`200`。
3. 修改返回`200`，再次查询能看到新值。
4. 删除返回`204`且响应体为空。
5. 删除后详情返回`404`。
6. 默认列表不再包含该员工，但数据库记录仍存在。

同一个员工编号受数据库唯一约束保护。若第12章已经使用过`E010`，请改用新的练习编号，不要删除历史记录来绕过逻辑删除规则。

## 八、失败场景验证

| 操作 | 预期结果 | 数据库检查 |
| --- | --- | --- |
| 提交缺少`name`的请求体 | `422` | 不新增记录 |
| 使用不存在的`department_id` | `400` | 不新增或修改记录 |
| 重复使用已有员工编号 | `409` | 原记录保持不变 |
| 修改不存在的员工 | `404` | 不产生新记录 |
| 再次删除已离职员工 | `404` | 原记录仍为离职状态 |

每次失败后再请求员工列表，确认Session已经回滚，应用仍然可以正常查询。

## 九、常见错误

| 现象 | 原因 | 修正 |
| --- | --- | --- |
| 第14章再次出现列表函数 | 把第13章代码重复粘贴到本章 | 保留原函数，只追加写接口 |
| 删除成功却返回JSON正文 | `204`使用了普通字典返回 | 返回`Response(status_code=204)` |
| 离职员工编号可以再次创建 | 重复检查只查询在职员工 | 使用Repository的`employee_number_exists()`检查全部记录 |
| 数据库错误后后续查询失败 | Service没有回滚 | 捕获写入异常后先`rollback()` |
| 响应中出现SQL或约束名称 | 直接返回了`str(exc)` | 返回稳定消息，详细异常只进入服务端日志 |

## 十、动手任务

1. 完成新增、修改和逻辑删除三个接口。
2. 按成功流程验证状态码、响应体和数据库值。
3. 分别制造`400`、`404`、`409`和`422`。
4. 确认每次失败后仍可正常执行列表查询。
5. 确认逻辑删除不会减少数据库总记录数。

## 十一、完成检查

- [ ] 第13章只读接口没有在本章重复定义。
- [ ] 三个写接口全部调用同一套EmployeeService。
- [ ] 员工编号创建后不可修改，也不能在离职后重复使用。
- [ ] Service负责提交与回滚，Router不直接操作事务。
- [ ] 业务异常和数据库约束异常都在HTTP边界转换。
- [ ] 删除是状态变化而不是物理删除。
- [ ] 成功与失败路径都已用HTTP请求和数据库结果验证。

完成后，第4阶段提供稳定的数据库分层，第5阶段提供完整HTTP接口。第15章只整理工程目录，不再重新实现Model、Repository、Service或CRUD。
