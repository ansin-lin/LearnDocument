# Python 自动化路线

这条路线只讲日本项目里的 Python 业务自动化，不讲 AI，也不讲桌面自动点击。主线围绕企业现场高频任务展开：

- 文件处理
- 数据处理
- 批量操作
- 调用接口
- 定时任务
- 运维辅助

重点不是再讲一遍 Python 基础、Pandas 基础或 Web 基础，而是把这些能力组织成可交付、可重复执行的自动化流程。

如果你前面已经学过 [Python 数据分析路线](../data_analysis/index.md)，这里可以继续承接“数据库 -> Excel -> 邮件”这一类企业自动化报表场景。

## 这条路线负责什么

- 文件和目录的自动处理
- 自动化场景下的 CSV、JSON、Excel 读写
- 文本提取与日志筛查
- 调用接口完成数据获取或通知
- 批处理、定时执行、日志保存和失败重试
- 3 类完整的业务自动化项目

## 这条路线不重复什么

- Python 基础语法：已在 `common/` 讲过
- Pandas 基础、数据清洗、汇总分析：已在 `data_analysis/` 讲过
- HTTP、API 设计、Web 框架开发：已在 `web/` 讲过

这里的原则是：只讲“自动化场景下必须补充的部分”，不重复已经有独立主线的基础课程。

## 项目出口

这条路线最终会落到 3 类日本项目高频自动化项目：

1. 自动化报表项目
2. 日志分析与异常提取项目
3. 运维辅助脚本项目

## 目录结构

```text
automation/
├── 01_intro.md
├── 02_file_operations.md
├── 03_csv_json_excel.md
├── 04_regex_text.md
├── 05_requests_api.md
├── 06_batch_schedule.md
├── 07_logging_retry.md
└── 08_automation_project.md
```
