# 第3章 CSV、Excel、JSON 与编码处理

这一章只讲自动化场景下必须处理的“文件格式、模板输出、编码问题”，不重复展开 Pandas 的分析型用法。

日本项目里的自动化，经常围绕“文件格式转换”和“报表输出”展开。这里真正高频的不是花哨功能，而是：

- CSV 能不能稳定读写
- Excel 能不能生成给业务使用的文件
- 编码会不会乱码
- 日期、金额、空值会不会错

## 3.1 为什么编码问题必须单独讲

在日本项目里，CSV 文件很可能不是 UTF-8，而是：

- `shift_jis`
- `cp932`
- `utf-8-sig`

如果你默认按 UTF-8 读取，常见结果就是：

- 日文乱码
- 读取报错
- 列名异常

## 3.2 读取 CSV 时先确认编码

```python
import pandas as pd

df = pd.read_csv("input/sales.csv", encoding="cp932")
print(df.head())
```

如果不确定来源，至少要先和提供方确认编码规则，不要靠反复试错硬猜。

## 3.3 输出 CSV 时考虑接收方环境

```python
df.to_csv("output/sales_summary.csv", index=False, encoding="utf-8-sig")
```

如果接收方主要用 Excel 打开 CSV，`utf-8-sig` 往往更稳。

但如果现场明确要求 `Shift-JIS`，就要按现场要求输出。

## 3.4 用 Pandas 读取和写入 Excel

```python
import pandas as pd

report_df = pd.read_excel("input/monthly_report.xlsx")
report_df.to_excel("output/result.xlsx", index=False)
```

这里的 Pandas 只作为“文件中转和批量整理工具”使用。更系统的清洗、汇总、统计方法，放在 [数据分析路线](../data_analysis/index.md)。

Pandas 在这一章适合：

- 先读数据
- 做最小限度的字段整理
- 快速导出表格结果

如果你需要系统讲解：

- 缺失值处理
- 分组统计
- 多表合并
- 分析指标

这些内容请回到 [数据分析路线](../data_analysis/index.md)。

## 3.5 为什么还要学 `openpyxl`

如果只是导出一个数据表，Pandas 足够。

但日本项目里经常要处理：

- 固定样式 Excel 模板
- 指定单元格写值
- 多工作表
- 标题、边框、列宽
- 汇总表和明细表写到同一文件

这些就需要 `openpyxl`。

这里的重点不是“Excel 分析”，而是“Excel 交付物生成”。

## 3.6 `openpyxl` 最小示例

```python
from openpyxl import Workbook

wb = Workbook()
ws = wb.active
ws.title = "月报"

ws["A1"] = "报表日期"
ws["B1"] = "2026-07-23"
ws["A2"] = "总销售额"
ws["B2"] = 1580000

wb.save("output/monthly_report.xlsx")
```

## 3.7 多格式之间的典型转换

现场常见链路：

- CSV -> Pandas -> Excel
- JSON API 返回值 -> Pandas -> CSV
- 数据库查询结果 -> Pandas -> Excel

也就是说，文件格式转换不是独立技能，而是自动化流程的中间环节。

## 3.8 日本项目里常见的 Excel 输出要求

常见要求包括：

- 文件名包含年月日
- 工作表名称固定
- 列顺序必须和既有模板一致
- 金额列带千分位
- 日期列格式统一
- 空值不要显示 `NaN`

这类要求比“写出一个 Excel 文件”更接近真实现场。

## 3.9 本章练习

请完成下面 3 个任务：

1. 读取一个 `cp932` 编码的 CSV 文件。
2. 把结果导出为 UTF-8 的 CSV。
3. 用 `openpyxl` 生成一个包含标题和两行数据的 Excel 文件。

做完后，你应该能处理日本项目里最常见的报表文件输入输出问题。
