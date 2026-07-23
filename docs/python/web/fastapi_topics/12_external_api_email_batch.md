# 第12章 外部 API、邮件与批处理

> 本章目标：掌握 FastAPI 项目中调用外部 API、发送邮件和执行简单后台任务的基本思路。

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

示例：

```python
import httpx  # 导入 httpx


def call_department_api():  # 定义调用外部部门 API 的函数
    url = "https://example.com/api/departments"  # 外部 API 地址，示例地址
    with httpx.Client(timeout=5.0) as client:  # 创建 HTTP 客户端并设置超时时间
        response = client.get(url)  # 发送 GET 请求
        response.raise_for_status()  # 非 2xx 状态码时抛出异常
        return response.json()  # 返回 JSON 数据
```

必须设置 `timeout`，不能让请求无限等待。

## 三、接口中调用外部 API

```python
from fastapi import APIRouter, HTTPException  # 导入路由和异常
import httpx  # 导入 httpx

router = APIRouter(prefix="/external", tags=["external"])  # 创建外部联动路由


@router.get("/departments")  # 注册外部部门查询接口
def get_external_departments():  # 定义接口函数
    try:  # 开始外部请求
        return call_department_api()  # 调用外部 API
    except httpx.TimeoutException:  # 捕获超时异常
        raise HTTPException(status_code=504, detail="外部系统响应超时")  # 返回网关超时
    except httpx.HTTPError:  # 捕获 HTTP 请求异常
        raise HTTPException(status_code=502, detail="外部系统调用失败")  # 返回外部系统错误
```

## 四、发送邮件

学习阶段先理解邮件发送位置。真实项目常由专门邮件服务或后台任务处理。

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

不要把真实邮箱密码写入代码。

## 五、后台任务

FastAPI 提供 `BackgroundTasks`，适合请求返回后执行简单任务。

```python
from fastapi import BackgroundTasks  # 导入后台任务


@router.post("/employees/{employee_id}/notify")  # 注册员工通知接口
def notify_employee(employee_id: int, background_tasks: BackgroundTasks):  # 接收员工 ID 和后台任务对象
    background_tasks.add_task(send_notification_mail, "tanaka@example.com")  # 添加邮件发送任务
    return {"message": "通知任务已提交", "employee_id": employee_id}  # 立即返回响应
```

`BackgroundTasks` 适合简单任务。耗时长、失败需要重试、需要队列的任务，应使用专门任务队列。

## 六、批处理基础

批处理通常不是由用户点击页面立即执行，而是定时或手动执行。

```python
def import_employees_batch():  # 定义员工导入批处理函数
    print("读取 CSV")  # 读取导入文件
    print("校验数据")  # 校验数据格式
    print("保存数据库")  # 保存到数据库
    print("输出结果日志")  # 输出处理结果
```

批处理关注：

- 输入文件
- 处理件数
- 成功件数
- 失败件数
- 错误明细
- 日志和重试

## 七、基础练习

请完成：

1. 使用 `httpx` 调用外部 API
2. 给请求设置 `timeout`
3. 捕获超时异常
4. 使用 `BackgroundTasks` 提交邮件任务
5. 设计一个 CSV 导入批处理流程

## 八、本章总结

- 外部 API 调用必须设置超时
- 外部系统错误要转换成明确响应
- 邮件配置不能写死真实密码
- `BackgroundTasks` 适合简单后台任务
- 批处理要关注日志、件数、失败明细和重试
