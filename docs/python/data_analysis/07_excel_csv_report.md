# 第7章 结果导出与手动报表交付

这一章只讲“分析完成后，如何把结果手动导出交付”。不展开自动化 Excel 模板、批量报表生成、邮件发送和定时任务。

分析做到最后，通常要把结果交出去。最常见的交付形式不是代码，而是 CSV、Excel 和结论说明。

## 7.1 输出 CSV

CSV 适合：

- 给其他程序继续处理
- 给数据库导入
- 作为轻量级中间结果

```python
region_summary.to_csv("output/region_summary.csv", index=False, encoding="utf-8-sig")
monthly_result.to_csv("output/monthly_result.csv", index=False, encoding="utf-8-sig")
```

这里加 `utf-8-sig`，是为了减少中文在部分 Excel 环境里出现乱码的概率。

## 7.2 输出 Excel

Excel 适合：

- 给业务同事查看
- 在一个文件里放多张结果表
- 配合结果说明做手动交付

```python
with pd.ExcelWriter("output/analysis_report.xlsx") as writer:
    region_summary.to_excel(writer, sheet_name="地区汇总", index=False)
    monthly_result.to_excel(writer, sheet_name="月度结果", index=False)
```

这里的重点只是“把分析结果导出为 Excel 文件”。

如果你要继续学习：

- 固定格式模板写入
- 指定单元格输出
- 自动生成多份 Excel
- 按日期批量产出报表

这些内容放在 [Python 自动化路线](../automation/index.md) 中讲。

## 7.3 一份基础结果文件建议包含什么

对于新人阶段的分析结果，至少建议交付：

- `清洗后明细`：已经处理过的分析数据
- `地区汇总`：按地区的核心结果
- `月度结果`：趋势和目标达成率
- `说明`：口径、时间范围、异常处理方式

你不一定每次都放在一个 Excel 里，但这四类信息最好都能交代清楚。

## 7.4 交付前检查

输出文件之前，先确认：

1. 列名能看懂。
2. 金额、日期格式正确。
3. 没有中间调试列混进去。
4. 指标口径和你前文说明一致。
5. 输出目录存在。

如果目录不存在，可以先创建：

```python
from pathlib import Path

Path("output").mkdir(exist_ok=True)
```

## 7.5 结果交付不是只导文件

实际交付时，通常还需要一句简短说明。例如：

> 已输出 2026 年 1 月到 6 月的零售订单分析结果，包含地区汇总、月度趋势和目标达成率。退款订单已剔除，缺失地区统一标记为“未知”。

这句话的价值很高，因为它告诉别人：

- 数据范围是什么
- 你做过什么处理
- 文件里主要看什么

## 7.6 本章练习

请把前几章得到的结果输出为：

1. `output/region_summary.csv`
2. `output/monthly_result.csv`
3. `output/analysis_report.xlsx`

并补一段不超过 100 字的交付说明，写清楚数据范围和处理口径。

## 7.7 到这里先停在“手动交付”

这一章先解决“你能把结果正确导出来”。

如果接下来要继续做：

- 定时生成报表
- 批量处理多份文件
- 自动发邮件
- 增加日志和失败重试

这些内容不在本路线继续展开，而是转到 [Python 自动化路线](../automation/index.md)。
