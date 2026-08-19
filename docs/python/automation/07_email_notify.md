# 邮件与通知自动化

自动化报表通常不只是生成文件，还要把结果通知给相关人员或其他系统。

本章学习两类通知方式：

- 邮件通知：适合发送报表附件、成功或失败结果。
- 通知接口：适合公司统一使用 Teams、Slack、内部通知平台时。

注意：本章示例使用占位邮箱和占位服务器，不要把真实账号、密码、Token 写进代码。

## 一、创建邮件对象

```python
from email.message import EmailMessage

# 作用：创建邮件对象
# 使用场景：准备发送报表结果或失败通知
message = EmailMessage()

message["Subject"] = "Daily Sales Report 2026-07-26"
message["From"] = "batch@example.com"
message["To"] = "user@example.com"

# 作用：设置邮件正文
# 使用场景：说明报表日期、处理结果、附件内容
message.set_content(
    """
Daily sales report has been created.

Target date: 2026-07-26
Status: success
"""
)

print(message["Subject"])  # 例如：Daily Sales Report 2026-07-26
```

常用字段：

| 字段 | 作用 |
| --- | --- |
| `Subject` | 邮件标题 |
| `From` | 发件人 |
| `To` | 收件人 |
| `Cc` | 抄送 |
| `Bcc` | 密送 |

## 二、添加附件

```python
from pathlib import Path

# 作用：定义报表附件路径
# 使用场景：自动化报表生成后作为邮件附件发送
report_file = Path("output") / "sales_report.xlsx"

# 作用：读取附件内容并加入邮件
# 使用场景：发送 Excel 报表附件
with report_file.open("rb") as f:
    message.add_attachment(
        f.read(),
        maintype="application",
        subtype="vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        filename=report_file.name,
    )

print(report_file.name)  # 例如：sales_report.xlsx
```

常见附件 MIME：

| 文件 | `maintype` | `subtype` |
| --- | --- | --- |
| `.xlsx` | `application` | `vnd.openxmlformats-officedocument.spreadsheetml.sheet` |
| `.zip` | `application` | `zip` |
| `.csv` | `text` | `csv` |

## 三、发送邮件

```python
import smtplib

# 作用：连接 SMTP 服务器并发送邮件
# 使用场景：公司允许脚本通过 SMTP 发送通知时
with smtplib.SMTP("smtp.example.com", 587, timeout=10) as smtp:
    smtp.starttls()
    smtp.login("USER_NAME", "PASSWORD")
    smtp.send_message(message)

print("mail sent")  # 例如：mail sent
```

正式项目注意：

- 不要把真实密码写死在代码里。
- 凭据通常从环境变量、配置中心或安全管理工具读取。
- 有些公司禁止 SMTP，要求调用内部通知接口。

## 四、成功通知和失败通知

```python
from email.message import EmailMessage
from pathlib import Path


def create_success_message(target_date: str, report_file: Path) -> EmailMessage:
    # 作用：创建成功通知邮件
    # 使用场景：报表生成成功后通知业务方
    message = EmailMessage()
    message["Subject"] = f"Daily Sales Report Success {target_date}"
    message["From"] = "batch@example.com"
    message["To"] = "user@example.com"
    message.set_content(f"Report created successfully.\nTarget date: {target_date}")

    with report_file.open("rb") as f:
        message.add_attachment(
            f.read(),
            maintype="application",
            subtype="vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            filename=report_file.name,
        )

    return message


def create_failure_message(target_date: str, error_message: str) -> EmailMessage:
    # 作用：创建失败通知邮件
    # 使用场景：批处理失败后通知负责人调查
    message = EmailMessage()
    message["Subject"] = f"Daily Sales Report Failed {target_date}"
    message["From"] = "batch@example.com"
    message["To"] = "user@example.com"
    message.set_content(
        f"Report job failed.\nTarget date: {target_date}\nError: {error_message}"
    )
    return message
```

## 五、通知接口

如果项目已经有通知接口，可以用 POST 请求发送通知。

```python
import requests

# 作用：调用通知接口发送处理结果
# 使用场景：公司统一使用 Teams、Slack 或内部通知平台时
payload = {
    "title": "Daily Sales Report",
    "target_date": "2026-07-26",
    "status": "success",
}

response = requests.post("https://api.example.com/notify", json=payload, timeout=10)

print(response.status_code)  # 例如：200 表示通知发送成功
```

## 六、本章完整案例

下面代码只创建邮件对象和附件，不实际发送。这样可以在没有 SMTP 服务器的环境中练习。

```python
from email.message import EmailMessage
from pathlib import Path


output_dir = Path("automation_mail_demo") / "output"
output_dir.mkdir(parents=True, exist_ok=True)

# 作用：创建样例报表文件
# 使用场景：没有真实 Excel 报表时，用文本模拟附件文件
report_file = output_dir / "sales_report.xlsx"
report_file.write_bytes(b"sample report content")


def create_report_message(target_date: str, report_file: Path) -> EmailMessage:
    # 作用：创建带附件的报表邮件
    # 使用场景：自动化报表生成成功后准备发送
    message = EmailMessage()
    message["Subject"] = f"Daily Sales Report {target_date}"
    message["From"] = "batch@example.com"
    message["To"] = "user@example.com"
    message.set_content(f"Report created successfully.\nTarget date: {target_date}")

    with report_file.open("rb") as f:
        message.add_attachment(
            f.read(),
            maintype="application",
            subtype="vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            filename=report_file.name,
        )

    return message


message = create_report_message("2026-07-26", report_file)

eml_file = output_dir / "sample_mail.eml"
eml_file.write_bytes(bytes(message))

print(message["Subject"])  # 例如：Daily Sales Report 2026-07-26
print(eml_file.exists())  # 例如：True 表示邮件文件已生成
```

## 七、方法总结表

| 方法 / 类 | 作用 | 常用参数 | 使用场景 |
| --- | --- | --- | --- |
| `EmailMessage()` | 创建邮件对象 | 无 | 构造邮件 |
| `message[...]` | 设置邮件头 | `Subject`、`From`、`To` | 标题和收发件人 |
| `set_content()` | 设置正文 | 文本内容 | 成功或失败说明 |
| `add_attachment()` | 添加附件 | `maintype`、`subtype`、`filename` | 附加 Excel、CSV、ZIP |
| `smtplib.SMTP()` | 连接邮件服务器 | 主机、端口、`timeout` | 发送邮件 |
| `smtp.starttls()` | 启用 TLS | 无 | 加密连接 |
| `smtp.login()` | 登录 | 用户名、密码 | SMTP 认证 |
| `smtp.send_message()` | 发送邮件 | `message` | 发出通知 |
| POST 请求 | 调用通知接口 | `json`、`timeout` | 内部通知平台 |

## 八、本章练习

1. 创建一封成功通知邮件。
2. 创建一封失败通知邮件。
3. 给成功通知邮件添加 Excel 附件。
4. 把邮件保存成 `.eml` 文件检查内容。
5. 如果有测试 SMTP 环境，尝试发送邮件。
6. 如果没有 SMTP 环境，用 POST 请求模拟调用通知接口，并设置 `timeout=10`。
