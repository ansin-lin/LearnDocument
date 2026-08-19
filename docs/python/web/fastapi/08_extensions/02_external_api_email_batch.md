# 扩展专题2 外部 API、邮件与批处理

> 本章目标：掌握 FastAPI 项目中调用外部 API、发送邮件和执行简单后台任务的基本思路。

外部 API、邮件和批处理分别解决不同的系统联动问题。练习时使用本地模拟服务、测试邮箱或测试替身，不连接企业生产系统。

## 一、系统联动场景

企业项目中，一个系统很少完全独立运行。

常见联动：

| 场景 | 说明 |
| --- | --- |
| 外部 API | 查询其他系统数据 |
| 邮件通知 | 申请完成后发送通知 |
| 批处理 | 定时同步数据、生成报表 |
| 后台任务 | 接口返回后继续处理耗时任务 |

## 二、调用外部 API

推荐使用 `httpx`。

```powershell
pip install httpx  # 安装 HTTP 客户端
```

文件：`app/services/external_service.py`  
操作：新建  
代码类型：项目代码片段

```python
import httpx  # 导入 httpx


def call_department_api():  # 定义调用外部部门 API 的函数
    url = "https://example.com/api/departments"  # 外部 API 地址，示例地址
    with httpx.Client(timeout=5.0) as client:  # 创建 HTTP 客户端并设置超时时间
        response = client.get(url)  # 发送 GET 请求
        response.raise_for_status()  # 非 2xx 状态码时抛出异常
        return response.json()  # 返回 JSON 数据
```

`httpx.Client()` 的 `timeout` 可接受秒数、`httpx.Timeout` 对象或 `None`，默认总超时为 5 秒；生产代码不要设置为 `None`。`client.get(url)` 的 `url` 是必填请求地址，`raise_for_status()` 会在 `4xx` 或 `5xx` 响应时抛出 `HTTPStatusError`。

必须设置 `timeout`，不能让请求无限等待。

## 三、接口中调用外部 API

文件：`app/routers/external.py`  
操作：新建  
代码类型：项目代码片段

```python
from fastapi import APIRouter, HTTPException  # 导入路由和异常
import httpx  # 导入 httpx

router = APIRouter(prefix="/api/external", tags=["external"])  # 设置或保存router的值


@router.get("/departments")  # 注册外部部门查询接口
def get_external_departments():  # 定义接口函数
    try:  # 开始外部请求
        return call_department_api()  # 调用外部 API
    except httpx.TimeoutException:  # 捕获超时异常
        raise HTTPException(status_code=504, detail="外部系统响应超时")  # 返回网关超时
    except httpx.HTTPError:  # 捕获 HTTP 请求异常
        raise HTTPException(status_code=502, detail="外部系统调用失败")  # 返回外部系统错误
```

`TimeoutException` 表示请求在规定时间内没有完成，这里转换为 `504 Gateway Timeout`。`HTTPError` 是 httpx 请求错误的基础类型，这里把其他连接或响应错误转换为 `502 Bad Gateway`。对外只返回稳定消息，详细地址和异常信息写入受控日志。

## 四、发送邮件

下面先把邮件发送封装为独立函数。需要可靠投递、重试和状态追踪时，应接入专门邮件服务或任务队列。

文件：`app/services/mail_service.py`  
操作：新建  
代码类型：项目代码片段

```python
import smtplib  # 导入 SMTP 标准库
from email.message import EmailMessage  # 导入邮件消息类


def send_notification_mail(to_address: str):  # 定义发送通知邮件函数
    message = EmailMessage()  # 创建邮件对象
    message["Subject"] = "员工信息已更新"  # 设置邮件标题
    message["From"] = "noreply@example.com"  # 设置发件人
    message["To"] = to_address  # 设置收件人
    message.set_content("员工信息已经更新，请确认。")  # 设置邮件正文

    with smtplib.SMTP("localhost", 25, timeout=5) as smtp:  # 连接 SMTP 服务器
        smtp.send_message(message)  # 发送邮件
```

`EmailMessage()`创建一封可设置标题、发件人、收件人和正文的邮件对象，当前不传构造参数。`message.set_content(content)`把字符串正文设置为纯文本内容；`content`必填，也可使用`subtype`、`charset`等关键字参数调整内容类型和编码，本例保留默认值。`smtp.send_message(message, from_addr=None, to_addrs=None)`发送邮件对象：`message`必填；发件人与收件人默认从邮件头读取，也可以显式传入字符串或地址列表。成功时返回服务器拒绝的收件人字典，全部接受时通常为空字典；连接、认证或发送失败会抛出SMTP相关异常。

`smtplib.SMTP()` 本示例参数：

| 参数 | 可接受的值 | 默认值或必填性 | 作用 |
| --- | --- | --- | --- |
| `host` | SMTP 主机名或 IP 字符串 | 默认空字符串 | 指定邮件服务器地址 |
| `port` | 端口整数 | 默认 `0`，由库选择标准端口 | 指定 SMTP 服务端口 |
| `timeout` | 秒数或 `None` | 默认使用全局网络超时 | 限制建立连接等网络操作的等待时间 |

示例中的 `localhost:25` 只适用于本地测试 SMTP 服务。真实环境还需要按邮件服务要求配置 TLS、认证和密钥管理。

不要把真实邮箱密码写入代码。

## 五、后台任务

FastAPI 提供 `BackgroundTasks`，适合请求返回后执行简单任务。

文件：`app/routers/employees.py`  
操作：追加通知接口  
代码类型：项目代码片段

```python
from fastapi import BackgroundTasks  # 导入后台任务


@router.post("/employees/{employee_number}/notify")  # 为下面的函数注册框架行为
def notify_employee(  # 定义notify_employee函数
    employee_number: str,  # 接收employee_number参数并声明类型
    background_tasks: BackgroundTasks,  # 接收background_tasks参数并声明类型
):  # 结束参数列表并开始处理通知请求
    background_tasks.add_task(send_notification_mail, "tanaka@example.com")  # 添加邮件发送任务
    return {  # 返回当前处理结果
        "message": "通知任务已提交",  # 组成当前文本内容
        "employee_number": employee_number,  # 组成当前文本内容
    }  # 完成当前调用或数据结构
```

`background_tasks.add_task()` 参数：

| 参数 | 可接受的值 | 默认值或必填性 | 作用 |
| --- | --- | --- | --- |
| `func` | 可调用对象 | 必填 | 指定响应发送后执行的函数 |
| `*args` | 与目标函数位置参数匹配的任意值 | 可省略 | 按位置传递给目标函数 |
| `**kwargs` | 与目标函数关键字参数匹配的任意值 | 可省略 | 按名称传递给目标函数 |

本示例把 `send_notification_mail` 作为 `func`，把邮件地址作为该函数的第一个位置参数。FastAPI 先返回响应，再在同一应用进程中执行任务。

`BackgroundTasks` 适合简单任务。耗时长、失败需要重试、需要队列的任务，应使用专门任务队列。

`BackgroundTasks` 与当前应用运行在同一进程内，不提供持久化队列保证；进程退出时任务可能丢失。重要通知应设计可追踪状态、有限重试和幂等处理。

## 六、批处理基础

批处理通常不是由用户点击页面立即执行，而是定时或手动执行。

文件：`app/batch/import_employees.py`  
操作：新建并独立运行  
代码类型：完整实验文件

```python
def import_employees_batch():  # 定义员工导入批处理函数
    print("读取 CSV")  # 读取导入文件
    print("校验数据")  # 校验数据格式
    print("保存数据库")  # 保存到数据库
    print("输出结果日志")  # 输出处理结果
```

这个函数展示批处理的最小阶段顺序。实际实现应把读取、校验、保存和结果记录拆成可测试函数，并明确一次批次的事务范围；不能只输出日志就认为数据已经导入。

批处理关注：

- 输入文件
- 处理件数
- 成功件数
- 失败件数
- 错误明细
- 日志和重试

## 七、基础练习

请完成：

1. 使用本地模拟服务或测试替身完成 `httpx` 调用，不依赖 `example.com` 返回业务 JSON
2. 给请求设置 `timeout`
3. 捕获超时异常
4. 使用 `BackgroundTasks` 提交邮件任务
5. 设计一个 CSV 导入批处理流程

练习结果必须至少包含一次成功、一次超时或外部错误，以及不会泄露连接信息的日志。未配置真实 SMTP 时，不声称邮件已经实际送达。

## 八、本章总结

- 外部 API 调用必须设置超时
- 外部系统错误要转换成明确响应
- 邮件配置不能写死真实密码
- `BackgroundTasks` 适合简单后台任务
- 批处理要关注日志、件数、失败明细和重试
