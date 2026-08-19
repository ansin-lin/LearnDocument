# 第5章 Depends与请求级复用

> 本章成果：把员工列表的分页参数整理成可复用的函数依赖，并能解释FastAPI怎样解析依赖、注入返回值和复用同一次请求中的结果。

第4章已经把员工接口移动到`app/routers/employees.py`。列表接口仍然使用内存数据，本章只整理公共请求参数，不改变URL和响应结构。

## 一、提取重复的分页参数

随着部门列表、员工列表和其他查询接口增加，每个接口都可能重复声明页码、每页数量和范围约束。

文件：`app/dependencies.py`  
操作：新建  
代码类型：完整文件

```python
from typing import Annotated  # 导入可附加参数元数据的类型工具

from fastapi import Depends, Query  # 导入依赖声明和查询参数声明工具
from pydantic import BaseModel  # 导入 Pydantic 模型基类


class PaginationParams(BaseModel):  # 定义分页结果对象
    page: int  # 保存当前页码
    size: int  # 保存每页数量


def get_pagination(  # 定义由 FastAPI 调用的分页依赖函数
    page: Annotated[int, Query(ge=1)] = 1,  # 读取页码并限制最小值为 1
    size: Annotated[int, Query(ge=1, le=100)] = 20,  # 读取每页数量并限制在 1 到 100
) -> PaginationParams:  # 返回经过校验的分页对象
    return PaginationParams(page=page, size=size)  # 组合两个查询参数


PaginationDep = Annotated[  # 定义可复用的分页依赖类型
    PaginationParams,  # 路径函数最终取得的数据类型
    Depends(get_pagination),  # 指定数据由 get_pagination 提供
]  # 结束类型别名
```

代码说明：

- `Query()`声明值来自查询字符串，同时把范围约束写入校验规则和OpenAPI文档。
- `PaginationParams`把两个相关参数组合为一个对象，路径函数可以读取`pagination.page`和`pagination.size`。
- `get_pagination()`仍然是普通Python函数；被`Depends()`引用后，FastAPI才负责为它准备参数并调用它。

`Query()`本例参数：

| 参数 | 作用 | 可接受的值 | 默认值或必填性 |
| --- | --- | --- | --- |
| `ge` | 约束数值大于等于指定值 | 与字段类型兼容的数字 | 不传时不设置下限 |
| `le` | 约束数值小于等于指定值 | 与字段类型兼容的数字 | 不传时不设置上限 |

请求`?page=0`或`?size=101`时，FastAPI在调用依赖前返回`422`，路径函数不会执行。

## 二、使用Depends注入结果

文件：`app/routers/employees.py`  
操作：替换导入和`list_employees()`路径函数  
代码类型：项目代码片段

```python
from app.dependencies import PaginationDep  # 导入分页依赖类型


@router.get("/employees", response_model=EmployeeListResponse)  # 注册员工列表接口
def list_employees(  # 定义list_employees函数
    pagination: PaginationDep,  # 注入经过校验的分页对象
    keyword: str | None = None,  # 读取可选关键字
):  # 响应由 EmployeeListResponse 校验
    active_employees = [  # 先筛选在职员工
        employee for employee in employees if employee["is_active"]  # 排除逻辑删除数据
    ]  # 完成当前调用或数据结构

    if keyword:  # 仅在提交关键字时筛选
        normalized_keyword = keyword.lower()  # 统一大小写
        active_employees = [  # 重建匹配结果列表
            employee  # 保留当前员工
            for employee in active_employees  # 遍历在职员工
            if normalized_keyword in employee["name"].lower()  # 匹配姓名
            or normalized_keyword in employee["employee_number"].lower()  # 或匹配员工编号
        ]  # 完成当前调用或数据结构

    start = (pagination.page - 1) * pagination.size  # 计算起始下标
    end = start + pagination.size  # 计算结束下标
    items = [  # 构建当前页响应项
        build_employee_response(employee)  # 补充响应需要的字段
        for employee in active_employees[start:end]  # 只处理当前页数据
    ]  # 完成当前调用或数据结构

    return {  # 返回分页响应字典
        "items": items,  # 当前页数据
        "total": len(active_employees),  # 筛选后的总件数
        "page": pagination.page,  # 当前页码
        "size": pagination.size,  # 每页数量
    }  # 结束响应字典
```

FastAPI处理请求时依次完成：

```text
读取page和size
→ 转换为int并检查范围
→ 调用get_pagination()
→ 得到PaginationParams对象
→ 把对象传给pagination参数
→ 执行list_employees()
```

路径函数不写`get_pagination()`，因为这里声明的是“依赖哪个函数”，不是立即调用该函数。写成`Depends(get_pagination())`会在应用导入阶段先执行函数，FastAPI也无法为它解析当前请求参数。

`Depends()`参数：

| 参数 | 作用 | 可接受的值 | 默认值或必填性 |
| --- | --- | --- | --- |
| `dependency` | 指定由FastAPI调用的依赖提供者 | 函数、可调用对象或`None`；使用`Annotated`时也可以根据标注推断 | 默认`None`，通常明确传入函数本身 |
| `use_cache` | 是否在同一次请求中复用相同依赖的结果 | `True`、`False` | 默认`True` |
| `scope` | 设置含`yield`依赖的退出时机 | `"request"`、`"function"`或`None` | 默认`None`；第6章详细说明 |

`Depends()`返回依赖声明对象，真正注入给路径函数的是依赖函数的返回值。

## 三、复用依赖类型别名

`PaginationDep`是Python类型别名，它同时保存：

- 路径函数得到的值是`PaginationParams`。
- 这个值由`get_pagination`提供。

代码位置：第13章的数据库员工Router  
操作：当前只阅读，不加入项目  
代码类型：后续项目代码片段

```python
def list_database_employees(  # 定义后续数据库列表路径函数
    pagination: PaginationDep,  # 复用分页依赖
):  # 省略后续才会加入的其他参数
    return service.list_employees(  # 调用 Service 查询员工
        page=pagination.page,  # 传递页码
        size=pagination.size,  # 传递每页数量
    )  # 返回查询结果
```

这是后续章节才会接入的代码片段，用于说明`PaginationDep`可以跨Router复用；当前不要把它加入项目。第13章会把分页依赖正式连接到数据库Service。修改分页上限时，只修改`get_pagination()`即可。

## 四、只执行检查的依赖

有些依赖只需要在路径函数之前完成检查，路径函数不使用其返回值。下面使用请求头做一个独立实验；正式登录和角色权限在后续认证章节实现。

文件：`app/dependencies.py`  
操作：临时追加，完成本章实验后删除  
代码类型：独立实验片段

```python
from typing import Annotated  # 导入带元数据的类型标注工具

from fastapi import Header, HTTPException, status  # 导入请求头、HTTP异常和状态码工具


def require_client_name(  # 定义只执行检查的依赖
    client_name: Annotated[  # 接收client_name参数并声明类型
        str | None,  # 请求头可以是字符串或不存在
        Header(alias="X-Client-Name"),  # 映射实际请求头名称
    ] = None,  # 未提交时使用 None
) -> None:  # 检查成功时不返回业务数据
    if client_name is None:  # 判断请求头是否缺失
        raise HTTPException(  # 中断请求
            status_code=status.HTTP_400_BAD_REQUEST,  # 返回 400
            detail="缺少X-Client-Name请求头",  # 返回错误信息
        )  # 结束异常对象
```

`Header()`声明参数来自HTTP请求头。Python参数名通常会把下划线转换为连字符；本例使用`alias`明确指定客户端发送的名称。

| 参数 | 作用 | 可接受的值 | 默认值或必填性 |
| --- | --- | --- | --- |
| `alias` | 指定客户端实际发送的请求头名称 | 非空字符串或`None` | 默认`None`，根据Python参数名生成名称 |
| `convert_underscores` | 是否把Python参数名中的下划线转换为连字符 | `True`、`False` | 默认`True` |

`Header()`返回请求头参数声明；FastAPI会读取请求头、完成类型转换，再把结果传给依赖函数。

文件：`app/routers/employees.py`  
操作：临时替换员工列表路径装饰器，完成实验后恢复  
代码类型：独立实验片段

```python
@router.get(  # 为下面的函数注册框架行为
    "/employees",  # 保持原有列表路径
    dependencies=[Depends(require_client_name)],  # 路径函数前执行请求头检查
)  # 结束装饰器参数
def list_employees(pagination: PaginationDep) -> list[dict]:  # 注入分页对象
    start = (pagination.page - 1) * pagination.size  # 计算起始下标
    end = start + pagination.size  # 计算结束下标
    return employees[start:end]  # 返回当前页数据
```

两种声明位置的差异：

| 声明位置 | 何时使用 | 路径函数能否取得返回值 |
| --- | --- | --- |
| 函数参数中的`Annotated[..., Depends(...)]` | 路径函数需要依赖结果 | 可以 |
| 装饰器的`dependencies=[...]` | 只要求依赖先成功执行 | 不可以 |

`dependencies`是路径操作装饰器参数，接受由`Depends()`或`Security()`创建的依赖声明列表，默认不执行额外依赖。依赖抛出异常时，路径函数不会执行。

## 五、子依赖

依赖函数本身也可以继续声明依赖。下面把请求头读取和检查分开。

文件：`app/dependencies.py`  
操作：独立实验时替换前一个函数并追加新函数，完成后删除  
代码类型：独立实验片段

```python
def get_client_name(  # 定义读取请求头的下层依赖
    client_name: Annotated[  # 接收client_name参数并声明类型
        str | None,  # 接收字符串或缺失值
        Header(alias="X-Client-Name"),  # 读取指定请求头
    ] = None,  # 缺失时使用 None
) -> str:  # 成功时返回字符串
    if client_name is None:  # 检查请求头
        raise HTTPException(  # 缺失时中断依赖树
            status_code=status.HTTP_400_BAD_REQUEST,  # 返回 400
            detail="缺少X-Client-Name请求头",  # 说明缺失内容
        )  # 结束异常对象
    return client_name  # 把结果交给上层依赖


def require_known_client(  # 定义允许值检查依赖
    client_name: Annotated[str, Depends(get_client_name)],  # 注入下层依赖结果
) -> str:  # 成功时返回已确认的客户端名称
    if client_name not in {"web", "mobile"}:  # 检查允许集合
        raise HTTPException(  # 不允许时中断请求
            status_code=status.HTTP_403_FORBIDDEN,  # 返回 403
            detail="不允许的客户端",  # 返回权限错误信息
        )  # 结束异常对象
    return client_name  # 返回通过检查的值
```

执行顺序是：

```text
get_client_name读取请求头
→ require_known_client检查值
→ 路径函数取得检查后的client_name
```

依赖树适合组合参数读取、当前用户、权限和数据库Session等公共能力。员工新增、修改和删除等完整业务流程仍应放在Service中，不应全部塞进Depends。

## 六、同一次请求中的复用

默认`use_cache=True`。同一个依赖在一棵依赖树中被多次需要时，FastAPI通常只调用一次，并把结果复用于当前请求。这个缓存不会跨请求保存，也不是Redis或应用级缓存。

代码位置：依赖声明处  
操作：当前只阅读  
代码类型：语法片段

```python
Depends(get_client_name, use_cache=False)  # 同一请求中每次都重新执行该依赖
```

关闭复用会增加调用次数。分页、当前用户和数据库Session通常应保留默认值。

## 七、运行验证

启动服务后依次请求：

```text
GET /api/employees?page=1&size=1
GET /api/employees?page=0&size=20
GET /api/employees?page=1&size=101
```

检查：

1. 第一个请求只返回一名员工。
2. 后两个请求返回`422`。
3. `/docs`中显示`page`和`size`的默认值与范围。
4. 请求头实验中，没有`X-Client-Name`时返回`400`，传入`web`时继续执行路径函数。

验证完成后恢复稳定项目状态：从`app/routers/employees.py`的列表装饰器删除`dependencies=[Depends(require_client_name)]`，恢复本章第二节中保留关键字筛选和响应模型的`list_employees()`；再从`app/dependencies.py`删除`require_client_name()`、`get_client_name()`、`require_known_client()`及其专用导入。重新打开`/docs`，确认员工列表只保留`keyword`、`page`和`size`参数。

## 八、常见错误

| 现象 | 原因 | 修正 |
| --- | --- | --- |
| 应用启动时就执行依赖 | 写成`Depends(get_pagination())` | 传入函数本身：`Depends(get_pagination)` |
| 路径函数中再次手动调用依赖 | 没有使用注入结果 | 直接使用`pagination`参数 |
| 所有接口都被同一规则拦截 | 把业务检查设为应用全局依赖 | 按路径或Router边界声明 |
| 不同请求共享可变对象 | 把请求数据保存为模块全局变量 | 让依赖按请求创建结果 |

## 九、本章练习

1. 增加`keyword`查询参数依赖，去除首尾空格；空字符串转换为`None`。
2. 让员工列表同时依赖分页对象和关键字结果。
3. 使用`/docs`验证参数默认值、范围和错误响应。
4. 故意写成`Depends(get_pagination())`，记录启动错误后恢复正确写法。

## 十、完成检查

- [ ] 能解释FastAPI为什么调用依赖函数。
- [ ] 能区分依赖函数、依赖声明和注入结果。
- [ ] 能选择函数参数依赖或装饰器依赖。
- [ ] 能说明`use_cache=True`只复用同一次请求中的结果。
- [ ] 能说明完整业务流程为什么不属于Depends。
