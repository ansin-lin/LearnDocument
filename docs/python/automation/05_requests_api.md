# 第5章 API 调用、通知处理与系统联动

这一章不重复讲 HTTP 基础或 Web 开发，只讲自动化脚本在企业现场如何“调用现成接口、拉数据、回传状态、发送通知”。

日本项目里的自动化脚本，往往不是孤立运行，而是要和其他系统联动。最常见的联动方式包括：

- 调用 API 获取数据
- 调用 API 发送通知
- 上传或下载文件
- 根据执行结果反馈状态

## 5.1 为什么自动化要会 API

很多现场任务已经不再是“人工下载 CSV”，而是：

- 从内部接口拉日报数据
- 把处理结果提交到别的系统
- 失败时发送通知

所以自动化路线里，API 调用是必会项。

如果你需要更完整的 HTTP、REST、认证、后端开发背景，请回到 `web/` 路线。这里的目标只是把脚本顺利接进企业流程。

## 5.2 最小 GET 请求

```python
import requests

response = requests.get("https://example.com/api/reports")
response.raise_for_status()

data = response.json()
print(data)
```

`raise_for_status()` 很重要。不要默默忽略失败状态。

## 5.3 带参数的请求

```python
import requests

params = {
    "report_date": "2026-07-23",
    "branch_code": "TOKYO01",
}

response = requests.get("https://example.com/api/sales", params=params)
response.raise_for_status()
```

这类写法适合按日期、店铺、部门拉取数据。

## 5.4 基本 POST 请求

```python
import requests

payload = {
    "job_name": "daily_sales_report",
    "status": "success",
}

response = requests.post("https://example.com/api/job-status", json=payload)
response.raise_for_status()
```

这类请求常用于：

- 回传脚本执行状态
- 调用通知服务
- 触发后续批处理

## 5.5 超时设置不能省略

```python
response = requests.get(
    "https://example.com/api/sales",
    timeout=30,
)
```

如果不设超时，脚本可能一直卡住，定时任务也会连锁受影响。

## 5.6 日本项目里常见的联动任务

- 从 API 获取日报原始数据
- 调用社内通知接口发送执行结果
- 调用邮件服务或消息接口
- 调用上传接口提交结果文件

重点不是把所有 HTTP 细节都讲完，而是会把自动化脚本接到系统流程里。

## 5.7 邮件通知在课程中的位置

“数据库 -> Excel -> 邮件”是典型企业自动化报表流程。

这里先把“系统联动”和“通知”概念讲清楚。真正把邮件发送、附件、执行结果通知完整串起来，建议放到后面的自动化报表项目中统一实现。

## 5.8 本章练习

请完成下面 3 个任务：

1. 调用一个 GET 接口获取 JSON 数据。
2. 发送一个 POST 请求回传执行状态。
3. 为请求增加超时设置和错误检查。

做完后，你就具备了把自动化脚本接入系统流程的基础能力。
