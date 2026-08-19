# 第25章 安全与性能基础

> 本章目标：理解 FastAPI 项目中常见安全风险和基础性能问题，掌握参数校验、认证授权、分页、超时和日志边界。

以下检查不新增业务功能。发现安全或性能问题后，修改对应实现并补充回归测试。

## 一、安全不是最后才做

接口开发中常见风险：

| 风险 | 说明 |
| --- | --- |
| 参数未校验 | 脏数据进入系统 |
| SQL 注入 | 字符串拼接 SQL |
| 密码明文保存 | 数据泄露后风险极高 |
| Token 泄露 | 用户身份被冒用 |
| 权限缺失 | 普通用户访问管理接口 |
| 文件上传无限制 | 上传恶意文件或超大文件 |

## 二、参数校验

使用 Pydantic 和类型提示进行基础校验。

文件：`app/schemas/employee.py`  
操作：修改`EmployeeCreate`中的员工编号和姓名字段约束  
代码类型：类定义局部片段

```python
from pydantic import BaseModel, Field  # 导入模型和字段约束


class EmployeeCreate(BaseModel):  # 新增员工请求模型
    employee_number: str = Field(  # 接收employee_number参数并声明类型
        min_length=2,  # 设置或保存min_length的值
        max_length=20,  # 设置或保存max_length的值
        pattern=r"^E[0-9]+$",  # 设置或保存pattern的值
    )  # 完成当前调用或数据结构
    name: str = Field(min_length=1, max_length=100)  # 限制姓名长度
```

`Field()` 的长度和正则参数已经在第3章说明。这里组合使用 `min_length`、`max_length` 和 `pattern`，让员工编号必须以 `E` 开头并继续使用数字。Pydantic 校验请求格式，员工编号是否重复等业务规则仍由 Service 判断。

## 三、SQL 安全

推荐使用 ORM 或参数化查询，不拼接用户输入。

代码位置：独立安全对比实验  
操作：只阅读，不加入项目  
代码类型：错误示例

```python
sql = f"select * from employees where name = '{keyword}'"  # 不推荐，存在 SQL 注入风险
```

代码位置：`EmployeeRepository`查询条件  
操作：项目采用此写法  
代码类型：语法片段

```python
statement = select(Employee).where(Employee.name.contains(keyword))  # 推荐，使用 SQLAlchemy 构建查询
```

第一段代码把用户输入直接拼进 SQL，输入中的引号和 SQL 片段可能改变原语句。第二段代码由 SQLAlchemy 生成带绑定参数的 SQL，`keyword` 作为数据交给数据库驱动处理，不作为 SQL 结构执行。

## 四、分页

文件：`app/routers/employees.py`  
操作：核对现有列表接口  
代码类型：项目代码片段

```python
@router.get("", response_model=EmployeeListResponse)  # 注册带分页限制的员工列表接口
def list_employees(  # 定义list_employees函数
    page: Annotated[int, Query(ge=1)] = 1,  # 接收page参数并声明类型
    size: Annotated[int, Query(ge=1, le=100)] = 20,  # 接收size参数并声明类型
    db: Session = Depends(get_db),  # 接收db参数并声明类型
):  # 结束参数列表并开始处理请求
    items, total = EmployeeService(db).list_employees(  # 调用Service取得当前页数据和总件数
        keyword=None,  # 设置或保存keyword的值
        page=page,  # 设置或保存page的值
        size=size,  # 设置或保存size的值
    )  # 完成当前调用或数据结构
    return EmployeeListResponse(  # 返回当前处理结果
        items=items,  # 设置或保存items的值
        total=total,  # 设置或保存total的值
        page=page,  # 设置或保存page的值
        size=size,  # 设置或保存size的值
    )  # 完成当前调用或数据结构
```

`EmployeeService.list_employees()`返回`(items, total)`元组，不能直接把元组交给`list[EmployeeResponse]`响应模型。这里继续使用第13章的`EmployeeListResponse`，同时返回当前页数据和筛选后的总件数。

分页必须落实到 SQL 的 `offset()`、`limit()`，不能先加载全部数据再切片。稳定分页还要使用稳定排序，本项目按唯一的 `employee_number` 排序。

## 五、外部请求超时

文件：`app/services/external_service.py`  
操作：新建  
代码类型：完整示例文件

```python
import httpx  # 导入支持超时控制的HTTP客户端


def fetch_external_data() -> dict:  # 定义fetch_external_data函数
    try:  # 开始执行可能失败的操作
        with httpx.Client(timeout=5.0) as client:  # 在上下文中管理当前资源
            response = client.get("https://example.com/api")  # 设置或保存response的值
            response.raise_for_status()  # 调用response.raise_for_status()
            return response.json()  # 返回当前处理结果
    except httpx.TimeoutException as exc:  # 捕获外部请求超时异常
        raise RuntimeError("外部服务请求超时") from exc  # 抛出稳定的应用异常
    except httpx.HTTPError as exc:  # 捕获其他HTTP客户端异常
        raise RuntimeError("外部服务调用失败") from exc  # 抛出稳定的应用异常
```

`httpx.Client()` 本示例参数：

| 参数 | 可接受的值 | 默认值或必填性 | 作用 |
| --- | --- | --- | --- |
| `timeout` | 秒数、`httpx.Timeout` 对象或 `None` | 默认 `5.0` 秒 | 设置连接、发送、接收和连接池等待的超时；`None` 会关闭超时限制 |

`client.get()` 的第一个位置参数是 URL 字符串。`raise_for_status()` 把 `4xx`、`5xx` 响应转换为 `HTTPStatusError`，它属于 `HTTPError`。`TimeoutException` 单独处理，可以向上层提供更明确的超时结果；响应正文和内部连接信息不直接返回给调用方。

外部请求必须设置超时和异常处理。

## 六、日志安全

日志不能输出：

- 密码
- Token
- 个人敏感信息
- 数据库真实密码
- 内部密钥

可以输出：

- 请求路径
- 状态码
- 耗时
- 业务 ID
- 错误摘要

## 七、性能基础

常见性能关注点：

| 问题 | 处理方向 |
| --- | --- |
| 列表数据太多 | 分页 |
| 重复查询 | 关联查询或减少循环查询 |
| 外部接口慢 | 超时、缓存、异步任务 |
| 文件太大 | 限制大小、流式处理 |
| 日志过多 | 控制日志级别 |

关联部门响应还要观察 SQL 次数。当前 Repository 使用关系加载策略，避免列表遍历时每名员工再次查询部门；是否优化成功应通过 SQL 日志或测试测量。

## 八、基础练习

请完成：

1. 给列表接口增加分页参数
2. 限制每页最大 100 条
3. 确认外部 API 调用有超时
4. 检查日志中是否输出 Token
5. 说明 CORS 为什么不是授权
6. 回归文件扩展章节中的大小限制和安全下载
7. 使用测试或 SQL 日志确认员工列表没有 N+1 查询

## 九、本章总结

- 安全要贯穿接口开发全过程
- 参数校验可以减少脏数据
- 不拼接用户输入生成 SQL
- 列表接口必须考虑分页
- 外部请求必须设置超时
- 日志不能泄露敏感信息
