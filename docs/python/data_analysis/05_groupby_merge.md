# 第5章 分组汇总、表关联与核心指标

清洗完成后，分析才真正开始。这一章解决三个高频任务：

1. 按维度汇总。
2. 把多张表关联起来。
3. 计算可以解释业务的指标。

## 5.1 `groupby` 是最常用的分析动作

例如按地区统计销售额和订单数：

```python
region_summary = (
    orders.groupby("region")
    .agg(
        sales_amount=("amount", "sum"),
        order_count=("order_id", "nunique"),
        avg_order_amount=("amount", "mean"),
    )
    .reset_index()
)

print(region_summary)
```

这里要注意：

- 销售额通常对 `amount` 求和
- 订单数通常对 `order_id` 去重后计数
- 客单价要先确认口径，是“订单平均金额”还是“客户平均消费金额”

## 5.2 同时按多个维度统计

```python
category_month_summary = (
    orders.groupby(["order_month", "category"])
    .agg(
        sales_amount=("amount", "sum"),
        quantity=("quantity", "sum"),
    )
    .reset_index()
)
```

这类结果适合后面做趋势图或透视表。

## 5.3 透视表

如果你想把“行转列”，可以用 `pivot_table()`：

```python
pivot = pd.pivot_table(
    orders,
    index="region",
    columns="category",
    values="amount",
    aggfunc="sum",
    fill_value=0,
)

print(pivot)
```

它适合快速生成“地区 × 品类”的汇总结果。

## 5.4 表关联

订单表里通常只有 `product_id`、`customer_id`，很多信息要从别的表补进来。

```python
orders = orders.merge(customers, on="customer_id", how="left")
orders = orders.merge(products, on="product_id", how="left")
```

常见连接方式：

- `left`：保留左表全部记录，最常用
- `inner`：只保留双方都匹配上的记录
- `right`：较少用
- `outer`：做数据对账时有价值

## 5.5 关联后必须做验证

多表分析最容易出错的地方，不是语法，而是关联结果悄悄变了。

关联前后至少检查：

```python
before_count = len(orders_raw)
after_count = len(orders)

print("关联前行数:", before_count)
print("关联后行数:", after_count)
```

还要检查是否存在未匹配记录：

```python
print(orders["customer_name"].isna().sum())
print(orders["product_name"].isna().sum())
```

如果关联后行数变多，通常说明：

- 右表主键不唯一
- 一对一关系被误写成了一对多

## 5.6 常用业务指标

以零售订单分析为例，至少要会算这些指标：

- 销售额：`amount` 求和
- 订单数：`order_id` 去重计数
- 销量：`quantity` 求和
- 客单价：销售额 ÷ 订单数
- 目标达成率：实际销售额 ÷ 目标销售额

例如：

```python
monthly_sales = (
    orders.groupby("order_month")
    .agg(
        sales_amount=("amount", "sum"),
        order_count=("order_id", "nunique"),
    )
    .reset_index()
)

monthly_sales["avg_order_amount"] = (
    monthly_sales["sales_amount"] / monthly_sales["order_count"]
)
```

## 5.7 和目标表做关联

```python
monthly_result = monthly_sales.merge(targets, on="order_month", how="left")
monthly_result["achievement_rate"] = (
    monthly_result["sales_amount"] / monthly_result["target_amount"]
)
```

写指标时必须把口径写清楚：

- 销售额是否含退款
- 订单数是否去重
- 达成率分母是哪一张目标表

## 5.8 本章练习

请基于清洗后的订单数据完成下面 3 个结果：

1. 按地区统计销售额、订单数、客单价。
2. 按月份和品类统计销售额。
3. 关联月度目标表，计算每月目标达成率。

每做完一个结果，都要检查：

- 分组字段是否正确
- 计数是否需要去重
- 关联前后行数是否符合预期
