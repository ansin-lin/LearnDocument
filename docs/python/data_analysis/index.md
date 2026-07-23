# Python 数据分析路线

这条路线只讲“如何完成数据分析本身”，主线围绕下面这条分析链路展开：

读懂数据 -> 读取数据 -> 检查问题 -> 清洗处理 -> 分组汇总 -> 图表表达 -> 手动导出结果

如果你后面要做的是：

- 定时生成报表
- 批量跑脚本
- 自动发邮件
- 补跑失败任务
- 日志记录和重试

这些内容不在这里展开，而是在 [Python 自动化路线](../automation/index.md) 中讲。

## 这条路线负责什么

- 数据基础概念
- Pandas 读取与检查
- 数据清洗与字段转换
- 分组汇总与表关联
- 数据可视化
- 手动导出 CSV 和 Excel 结果
- 一个完整的数据分析小项目

## 这条路线不重复什么

- Python 基础语法：已在 `common/` 讲过
- 自动化批处理、定时任务、日志重试、邮件通知：已归到 `automation/`
- Web 接口设计和后端开发：已归到 `web/`

## 目录结构

```text
data_analysis/
├── 01_intro.md
├── 02_data_concepts.md
├── 03_pandas_read_inspect.md
├── 04_data_cleaning.md
├── 05_groupby_merge.md
├── 06_visualization.md
├── 07_excel_csv_report.md
└── 08_analysis_project.md
```
