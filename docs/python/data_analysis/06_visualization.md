# 第6章 数据可视化与结果表达

图表不是装饰，而是让别人更快看懂结果。做图前先确认你要回答的问题，再选图。

## 6.1 先选图，再写代码

最常见的选择规则：

- 比较不同类别：柱状图
- 看时间趋势：折线图
- 看分布情况：直方图
- 看两个数值的关系：散点图

如果只是想展示“华东、华北、华南哪个销售额更高”，柱状图比饼图更清楚。

## 6.2 柱状图示例

先准备汇总数据：

```python
region_summary = (
    orders.groupby("region")
    .agg(sales_amount=("amount", "sum"))
    .reset_index()
    .sort_values("sales_amount", ascending=False)
)
```

再画图：

```python
import matplotlib.pyplot as plt

plt.figure(figsize=(8, 4))
plt.bar(region_summary["region"], region_summary["sales_amount"])
plt.title("各地区销售额")
plt.xlabel("地区")
plt.ylabel("销售额")
plt.tight_layout()
plt.show()
```

## 6.3 折线图示例

```python
monthly_sales = (
    orders.groupby("order_month")
    .agg(sales_amount=("amount", "sum"))
    .reset_index()
)

plt.figure(figsize=(8, 4))
plt.plot(monthly_sales["order_month"], monthly_sales["sales_amount"], marker="o")
plt.title("月度销售额趋势")
plt.xlabel("月份")
plt.ylabel("销售额")
plt.xticks(rotation=45)
plt.tight_layout()
plt.show()
```

这类图适合回答：

- 销售额是在增长还是下降
- 哪个月份波动明显
- 是否存在异常高峰

## 6.4 做图时必须写清楚的信息

至少要有：

- 标题
- 横轴名称
- 纵轴名称
- 单位

如果缺少这些信息，图表即使画出来，也很难用于汇报。

## 6.5 避免误导性的图表

常见问题有：

- 纵轴单位不清楚
- 时间顺序乱了
- 类别过多导致标签挤在一起
- 指标口径不同却画在同一张图里

图表要帮助理解，而不是制造误解。

## 6.6 图表后的文字结论怎么写

图表后不要只放一句“如图所示”。至少要写清楚：

1. 你观察到了什么。
2. 这个现象可能说明什么。
3. 还需要补充确认什么。

例如：

> 华东地区销售额最高，但订单数并不是最高，说明客单价可能更高。下一步需要继续拆分客户等级和品类结构。

这种表达比单纯贴图更接近实际工作交付。

## 6.7 本章练习

请基于前一章的汇总结果完成两张图：

1. 各地区销售额柱状图。
2. 月度销售额趋势折线图。

完成后再写 3 句话：

- 你从图中看到了什么。
- 哪个结论最重要。
- 哪个地方还不能直接下结论。
