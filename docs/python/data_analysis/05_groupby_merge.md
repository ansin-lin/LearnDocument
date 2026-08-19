# Excel / CSV 分组汇总、表关联与透视

这一章学习把清洗后的明细数据整理成可以交给业务方查看的汇总结果。

前面章节已经完成了数据读取和数据清洗。本章从清洗后的 `orders_clean`、`customers_clean`、`products_clean`、`targets_clean` 开始，按实际分析代码的顺序学习：

1. 先准备月份字段。
2. 再按地区、月份、品类分组统计。
3. 然后把订单表关联客户表、商品表、目标表。
4. 最后生成透视表，形成适合报表展示的结果。

## 本章使用的数据

本章继续使用这 4 份样例数据：

- `orders.csv`：订单明细，核心事实数据。
- `customers.xlsx`：客户资料。
- `products.csv`：商品资料。
- `monthly_targets.csv`：月度目标。

本章假设前面已经得到清洗后的数据：

```python
orders_clean
customers_clean
products_clean
targets_clean
```

如果你是单独练习本章，也可以先使用第 04 章最后的完整清洗代码生成这些变量。

## 一、先准备分组字段：月份字段

订单表里的 `order_date` 是每天一行订单。月度报表通常不按“日”统计，而是按“月”统计，所以要先从订单日期中提取月份。

```python
# 作用：从订单日期中提取月份，生成 order_month 字段
# 使用场景：后续要按月份统计销售额、订单数、毛利时
orders_clean["order_month"] = orders_clean["order_date"].dt.to_period("M").astype("string")

# 作用：查看新增字段是否生成成功
# 使用场景：确认月份格式是否是 YYYY-MM
print(orders_clean[["order_date", "order_month"]].head(3))
# 例如：
#   order_date order_month
# 0 2025-01-03     2025-01
# 1 2025-01-03     2025-01
# 2 2025-01-04     2025-01
```

这里用到的代码含义：

| 写法 | 作用 | 使用场景 |
| --- | --- | --- |
| `.dt` | 使用日期时间字段的方法 | 日期列已经是 datetime 类型时 |
| `.to_period("M")` | 把日期转换成月份周期 | 做月度统计、月度目标对比时 |
| `.astype("string")` | 转成字符串类型 | 后面要和目标表的 `target_month` 合并时 |

## 二、`groupby()`：按字段分组

`groupby()` 的作用是把数据按某些字段分成多组。它本身只是“分组准备动作”，还没有真正算出结果，通常要继续接 `agg()`。

### 1. 基础功能示例

```python
# 作用：按地区对订单数据分组
# 使用场景：准备统计每个地区的销售额、订单数、毛利时
region_group = orders_clean.groupby("region")

# 作用：查看 groupby() 返回的对象类型
# 使用场景：理解 groupby() 只是分组对象，不是最终汇总表
print(type(region_group))  # 例如：<class 'pandas.core.groupby.generic.DataFrameGroupBy'>
```

### 2. 常用参数示例

```python
# 作用：按月份和地区进行分组，并让分组字段保留为普通列
# 使用场景：生成可以继续排序、合并、导出 Excel 的汇总表时
month_region_group = orders_clean.groupby(
    by=["order_month", "region"],
    as_index=False,
    sort=True,
    dropna=True,
)

# 作用：查看分组对象中有多少组
# 使用场景：确认分组维度是否符合预期
print(month_region_group.ngroups)  # 例如：72 表示一共分出了 72 个“月份 × 地区”组合
```

`groupby()` 常用参数：

| 参数 | 含义 | 常用写法 | 使用场景 |
| --- | --- | --- | --- |
| `by` | 按哪些字段分组 | `"region"`、`["order_month", "region"]` | 按地区、月份、门店、品类统计 |
| `as_index` | 分组字段是否作为索引 | `False` | 教学和报表中建议常用 `False`，结果更像普通 Excel 表 |
| `sort` | 是否对分组字段排序 | `True` / `False` | 数据量大时可设为 `False` 提高速度 |
| `dropna` | 分组字段为空时是否丢弃 | `True` / `False` | 想统计“未设定地区”等空值组时设为 `False` |

## 三、`agg()`：对分组结果做统计

`agg()` 用来指定每个分组要计算哪些指标，比如销售额合计、订单数、平均客单价、最大金额等。

### 1. 基础功能示例

```python
# 作用：按地区统计销售额合计
# 使用场景：想知道每个地区总销售额时
region_sales = (
    orders_clean.groupby("region", as_index=False)
    .agg(sales_amount=("amount", "sum"))
)

# 作用：查看地区销售额汇总结果
# 使用场景：确认 groupby() + agg() 已经生成真正的汇总表
print(region_sales.head(3))
# 例如：
#      region  sales_amount
# 0     Chubu     123456789
# 1    Kansai     234567890
# 2     Kanto     345678901
```

### 2. 常用参数示例

```python
# 作用：按月份和品类一次统计多个指标
# 使用场景：制作月度品类销售分析表时
monthly_category_summary = (
    orders_clean.groupby(
        by=["order_month", "category"],
        as_index=False,
    )
    .agg(
        sales_amount=("amount", "sum"),
        order_count=("order_id", "nunique"),
        quantity=("quantity", "sum"),
        gross_profit=("gross_profit", "sum"),
        avg_unit_price=("unit_price", "mean"),
        max_order_amount=("amount", "max"),
    )
)

# 作用：查看汇总结果的前 5 行
# 使用场景：确认每个“月份 × 品类”都有统计指标
print(monthly_category_summary.head())
# 例如：
#   order_month category  sales_amount  order_count  quantity  gross_profit
# 0     2025-01       OA     123456789         1024      3050      34567890
```

`agg()` 常用写法：

| 写法 | 含义 | 使用场景 |
| --- | --- | --- |
| `新列名=("原列名", "sum")` | 对原列求和，并命名为新列 | 销售额、数量、毛利汇总 |
| `("order_id", "nunique")` | 统计去重后的订单数 | 一张订单有多行明细时 |
| `("amount", "count")` | 统计非空行数 | 检查有效数据行数 |
| `("unit_price", "mean")` | 求平均值 | 平均单价、平均折扣率 |
| `("amount", "max")` | 求最大值 | 找最大订单金额 |
| `("amount", "min")` | 求最小值 | 找最小订单金额 |

注意：订单明细表里一张订单可能有多行商品，所以统计订单数时更常用 `nunique`，不要直接用 `count`。

## 四、`sort_values()`：对汇总结果排序

业务分析通常不会只看完整列表，而是先看销售额最高、毛利最低、达成率最差等重点结果。`sort_values()` 用来对表格排序。

### 1. 基础功能示例

```python
# 作用：按销售额从高到低排序
# 使用场景：查看销售额最高的地区
region_sales_sorted = region_sales.sort_values("sales_amount", ascending=False)

# 作用：查看销售额最高的前 5 个地区
# 使用场景：制作 Top 排名表时
print(region_sales_sorted.head(5))  # 例如：显示销售额最高的前 5 行
```

### 2. 常用参数示例

```python
# 作用：先按月份升序，再按销售额降序排序
# 使用场景：每个月内部查看销售额最高的品类时
monthly_category_sorted = monthly_category_summary.sort_values(
    by=["order_month", "sales_amount"],
    ascending=[True, False],
    na_position="last",
    ignore_index=True,
)

# 作用：查看排序后的结果
# 使用场景：确认每个月内部销售额从高到低排列
print(monthly_category_sorted.head(5))
# 例如：
#   order_month category  sales_amount  order_count
# 0     2025-01       PC     987654321         2031
# 1     2025-01       OA     123456789         1024
```

`sort_values()` 常用参数：

| 参数 | 含义 | 常用写法 | 使用场景 |
| --- | --- | --- | --- |
| `by` | 按哪些列排序 | `"sales_amount"`、`["order_month", "sales_amount"]` | 单字段排序或多字段排序 |
| `ascending` | 是否升序 | `False`、`[True, False]` | 金额通常降序，月份通常升序 |
| `na_position` | 空值放前面还是后面 | `"last"` | 排名时通常把空值放最后 |
| `ignore_index` | 是否重置行号 | `True` | 排序后导出报表更整齐 |

## 五、`merge()`：把多张表关联起来

在企业数据分析中，订单表通常只保存编号，比如 `customer_id`、`product_id`。如果要分析客户行业、客户等级、商品品牌、供应商，就需要把客户表和商品表关联进来。

### 1. 基础功能示例：订单表关联客户表

```python
# 作用：把订单表和客户表按 customer_id 关联
# 使用场景：订单表只有客户编号，需要补充客户行业、客户等级、客户类型时
orders_with_customer = orders_clean.merge(
    customers_clean[["customer_id", "industry", "customer_level", "customer_type"]],
    on="customer_id",
    how="left",
)

# 作用：查看关联后的表格行列数
# 使用场景：确认关联后行数没有异常变多或变少
print(orders_clean.shape)           # 例如：(56090, 25) 原订单表
print(orders_with_customer.shape)   # 例如：(56090, 28) 行数不变，列数增加

# 作用：查看补充进来的客户字段
# 使用场景：确认 customer_id 已经找到对应客户属性
print(orders_with_customer[["customer_id", "industry", "customer_level"]].head(3))
# 例如：
#   customer_id industry customer_level
# 0    C0000123       製造業              A
```

### 2. 常用参数示例：订单表继续关联商品表

```python
# 作用：把订单表继续和商品表按 product_id 关联
# 使用场景：需要按商品名称、品牌、供应商做销售分析时
orders_full = orders_with_customer.merge(
    products_clean[["product_id", "product_name", "brand", "supplier_name", "category"]],
    on="product_id",
    how="left",
    validate="many_to_one",
    suffixes=("", "_product"),
)

# 作用：查看关联后的商品字段
# 使用场景：确认每行订单都补充了商品名称和品牌
print(orders_full[["product_id", "product_name", "brand", "supplier_name"]].head(3))
# 例如：
#   product_id product_name brand supplier_name
# 0  P00001234      業務PC-A    NEC       東京商事
```

`merge()` 常用参数：

| 参数 | 含义 | 常用写法 | 使用场景 |
| --- | --- | --- | --- |
| `right` | 要关联的右表 | `customers_clean[...]` | 左表是订单，右表是客户、商品、目标 |
| `on` | 两张表字段名相同时的关联键 | `"customer_id"` | 两边字段都叫 `customer_id` |
| `left_on` / `right_on` | 两张表字段名不同时的关联键 | `left_on="order_month"`、`right_on="target_month"` | 实际表和目标表字段名不同 |
| `how` | 关联方式 | `"left"`、`"inner"`、`"outer"` | 报表中常用 `left` 保留订单明细 |
| `validate` | 检查关联关系 | `"many_to_one"`、`"one_to_one"` | 防止右表重复导致行数被放大 |
| `suffixes` | 重名字段后缀 | `("", "_product")` | 两张表有同名列时区分来源 |

`how` 的常见选择：

| 写法 | 含义 | 使用场景 |
| --- | --- | --- |
| `how="left"` | 保留左表全部行 | 订单明细关联客户、商品时最常用 |
| `how="inner"` | 只保留两边都匹配的行 | 只分析有完整主数据的数据 |
| `how="outer"` | 保留两边所有行 | 排查目标表有但实际表没有的数据 |

## 六、`rename()`：修改字段名

不同数据来源的字段名经常不一致。例如订单汇总表叫 `order_month`，目标表叫 `target_month`。如果要直接用 `on=[...]` 合并，字段名需要先统一。

### 1. 基础功能示例

```python
# 作用：把 order_month 字段改名为 target_month
# 使用场景：准备和 monthly_targets.csv 按月份字段关联时
actual_monthly = monthly_category_summary.rename(
    columns={"order_month": "target_month"}
)

# 作用：查看字段名是否修改成功
# 使用场景：确认后续 merge() 可以使用 target_month 作为关联字段
print(actual_monthly.columns.tolist())
# 例如：['target_month', 'category', 'sales_amount', 'order_count', ...]
```

### 2. 常用参数示例

```python
# 作用：一次修改多个字段名
# 使用场景：把英文技术字段名改成更适合报表阅读的字段名时
report_summary = actual_monthly.rename(
    columns={
        "sales_amount": "actual_sales_amount",
        "gross_profit": "actual_gross_profit",
        "order_count": "actual_order_count",
    },
    errors="raise",
)

# 作用：查看改名后的字段
# 使用场景：确认报表字段命名更清楚
print(report_summary.columns.tolist())
# 例如：['target_month', 'category', 'actual_sales_amount', 'actual_order_count', ...]
```

`rename()` 常用参数：

| 参数 | 含义 | 常用写法 | 使用场景 |
| --- | --- | --- | --- |
| `columns` | 指定列名修改关系 | `{"旧列名": "新列名"}` | 修改一个或多个字段名 |
| `index` | 指定索引名修改关系 | `{0: "合计"}` | 较少用于本课程 |
| `errors` | 字段不存在时是否报错 | `"raise"` | 教学和正式脚本中建议使用，避免写错列名却没发现 |

## 七、实际结果与目标表对比

月度目标表通常不是明细表，而是按月份、地区、门店、渠道、品类提前设定好的目标。要计算达成率，先把订单明细汇总到相同粒度，再和目标表关联。

```python
# 作用：按目标表的粒度汇总实际销售结果
# 使用场景：准备和月度目标表做达成率对比时
actual_target_level = (
    orders_clean.groupby(
        by=[
            "order_month",
            "region",
            "prefecture",
            "store_code",
            "store_name",
            "channel",
            "category",
        ],
        as_index=False,
    )
    .agg(
        actual_orders=("order_id", "nunique"),
        actual_sales_amount=("amount", "sum"),
        actual_gross_profit=("gross_profit", "sum"),
    )
    .rename(columns={"order_month": "target_month"})
)

# 作用：把实际结果和目标表按相同粒度合并
# 使用场景：计算销售额达成率、毛利达成率、新客目标差异时
target_compare = actual_target_level.merge(
    targets_clean,
    on=[
        "target_month",
        "region",
        "prefecture",
        "store_code",
        "store_name",
        "channel",
        "category",
    ],
    how="left",
    validate="one_to_one",
)

# 作用：计算销售额达成率
# 使用场景：实际销售额除以目标销售额，判断是否完成目标
missing_target_count = target_compare["target_sales_amount"].isna().sum()
print(missing_target_count)  # 例如：12800 表示有 12800 行实际销售记录没有匹配到目标

# 作用：计算销售额达成率
# 使用场景：只要目标销售额存在，就可以用实际销售额除以目标销售额
target_compare["sales_achievement_rate"] = (
    target_compare["actual_sales_amount"] / target_compare["target_sales_amount"]
)

# 作用：查看目标对比结果
# 使用场景：确认实际值、目标值、达成率都已经生成
print(target_compare[[
    "target_month",
    "region",
    "store_code",
    "category",
    "actual_sales_amount",
    "target_sales_amount",
    "sales_achievement_rate",
]].head(3))
# 例如：
#   target_month region store_code category  actual_sales_amount  target_sales_amount  sales_achievement_rate
# 0     2025-01  Kanto   S000001       PC            12345678             12000000                 1.0288
```

注意：如果 `missing_target_count` 大于 0，表示有实际销售记录没有找到对应目标。日本项目中这类情况通常要先确认规格：

- 是目标表漏维护。
- 是实际订单出现了目标表没有覆盖的门店、渠道或品类。
- 是合并字段格式不一致，例如门店编号、月份、品类名称前后有空格。

达成率字段说明：

| 字段 | 含义 |
| --- | --- |
| `actual_sales_amount` | 实际销售额 |
| `target_sales_amount` | 目标销售额 |
| `sales_achievement_rate` | 销售额达成率，`1.0` 表示 100% |

## 八、`pivot_table()`：生成透视表

`pivot_table()` 类似 Excel 透视表。它适合把明细数据或汇总数据转换成“行 × 列”的交叉统计结果。

### 1. 基础功能示例

```python
# 作用：生成“月份 × 品类”的销售额透视表
# 使用场景：横向比较每个月不同品类的销售额时
monthly_category_pivot = pd.pivot_table(
    orders_clean,
    index="order_month",
    columns="category",
    values="amount",
    aggfunc="sum",
)

# 作用：查看透视表前 5 行
# 使用场景：确认行是月份，列是品类，单元格是销售额
print(monthly_category_pivot.head())
# 例如：
# category           OA         PC     Software
# order_month
# 2025-01     12345678   98765432     45678901
# 2025-02     22345678   88765432     55678901
```

### 2. 常用参数示例

```python
# 作用：生成带空值填充和合计行列的透视表
# 使用场景：制作可直接导出 Excel 的月度品类销售报表时
monthly_category_report = pd.pivot_table(
    data=orders_clean,
    index="order_month",
    columns="category",
    values="amount",
    aggfunc="sum",
    fill_value=0,
    margins=True,
    margins_name="合计",
)

# 作用：查看透视表的形状
# 使用场景：确认月份行、品类列和合计列都已生成
print(monthly_category_report.shape)  # 例如：(25, 9) 表示 24 个月 + 合计行，8 个品类 + 合计列

# 作用：查看透视表前几行
# 使用场景：检查空值是否已经用 0 填充
print(monthly_category_report.head())
```

`pivot_table()` 常用参数：

| 参数 | 含义 | 常用写法 | 使用场景 |
| --- | --- | --- | --- |
| `data` | 原始数据表 | `orders_clean` | 明细表或汇总表 |
| `index` | 透视表行字段 | `"order_month"` | 按月份、地区、客户等级作为行 |
| `columns` | 透视表列字段 | `"category"` | 按品类、渠道、地区作为列 |
| `values` | 要统计的数值字段 | `"amount"` | 销售额、毛利、数量 |
| `aggfunc` | 聚合方式 | `"sum"`、`"mean"`、`"count"` | 求和、平均、计数 |
| `fill_value` | 空值填充值 | `0` | 没有销售额的组合显示为 0 |
| `margins` | 是否增加合计行列 | `True` | 报表需要总计时 |
| `margins_name` | 合计名称 | `"合计"` | 中文或日文报表展示 |

## 九、本章完整代码

下面代码把本章方法串起来，形成一个完整分析流程。

```python
from pathlib import Path

import pandas as pd


data_dir = Path("data")

# 作用：读取订单明细
# 使用场景：订单表是后续所有销售分析的核心事实表
orders = pd.read_csv(
    data_dir / "orders.csv",
    dtype={
        "order_id": "string",
        "customer_id": "string",
        "product_id": "string",
        "store_code": "string",
    },
    parse_dates=["order_date"],
)

# 作用：读取客户资料
# 使用场景：后续按客户等级、行业、客户类型分析销售额
customers = pd.read_excel(
    data_dir / "customers.xlsx",
    dtype={
        "customer_id": "string",
        "customer_name": "string",
    },
)

# 作用：读取商品资料
# 使用场景：后续按商品名称、品牌、供应商分析销售额
products = pd.read_csv(
    data_dir / "products.csv",
    dtype={
        "product_id": "string",
        "sku": "string",
    },
)

# 作用：读取月度目标
# 使用场景：后续计算实际销售额和目标销售额的达成率
targets = pd.read_csv(
    data_dir / "monthly_targets.csv",
    dtype={
        "target_month": "string",
        "store_code": "string",
    },
)

# 作用：复制数据，避免直接修改原始读取结果
# 使用场景：教学、调试和正式脚本中都建议保留原始数据
orders_clean = orders.copy()
customers_clean = customers.copy()
products_clean = products.copy()
targets_clean = targets.copy()

# 作用：生成月份字段
# 使用场景：按月统计和按月对比目标时
orders_clean["order_month"] = orders_clean["order_date"].dt.to_period("M").astype("string")

# 作用：按地区汇总销售额、订单数和毛利
# 使用场景：制作地区销售概览
region_summary = (
    orders_clean.groupby("region", as_index=False)
    .agg(
        sales_amount=("amount", "sum"),
        order_count=("order_id", "nunique"),
        gross_profit=("gross_profit", "sum"),
    )
    .sort_values("sales_amount", ascending=False, ignore_index=True)
)

print(region_summary.head(5))  # 例如：显示销售额最高的前 5 个地区

# 作用：订单表关联客户表
# 使用场景：补充客户行业、客户等级、客户类型
orders_with_customer = orders_clean.merge(
    customers_clean[["customer_id", "industry", "customer_level", "customer_type"]],
    on="customer_id",
    how="left",
    validate="many_to_one",
)

# 作用：订单表关联商品表
# 使用场景：补充商品名称、品牌、供应商
orders_full = orders_with_customer.merge(
    products_clean[["product_id", "product_name", "brand", "supplier_name"]],
    on="product_id",
    how="left",
    validate="many_to_one",
)

print(orders_full.shape)  # 例如：(56090, 32) 行数不变，列数增加

# 作用：按目标表粒度汇总实际结果
# 使用场景：准备和 monthly_targets.csv 对比
actual_target_level = (
    orders_clean.groupby(
        [
            "order_month",
            "region",
            "prefecture",
            "store_code",
            "store_name",
            "channel",
            "category",
        ],
        as_index=False,
    )
    .agg(
        actual_orders=("order_id", "nunique"),
        actual_sales_amount=("amount", "sum"),
        actual_gross_profit=("gross_profit", "sum"),
    )
    .rename(columns={"order_month": "target_month"})
)

# 作用：实际结果关联目标表
# 使用场景：计算目标达成率
target_compare = actual_target_level.merge(
    targets_clean,
    on=[
        "target_month",
        "region",
        "prefecture",
        "store_code",
        "store_name",
        "channel",
        "category",
    ],
    how="left",
    validate="one_to_one",
)

# 作用：检查实际结果中有多少行没有匹配到目标
# 使用场景：发现目标表漏维护、字段不一致、主数据不一致等问题
missing_target_count = target_compare["target_sales_amount"].isna().sum()
print(missing_target_count)  # 例如：12800 表示有 12800 行没有匹配到目标

# 作用：计算销售额达成率
# 使用场景：目标销售额存在时，判断实际销售额是否达到目标
target_compare["sales_achievement_rate"] = (
    target_compare["actual_sales_amount"] / target_compare["target_sales_amount"]
)

print(target_compare[["target_month", "region", "sales_achievement_rate"]].head(3))
# 例如：1.05 表示实际销售额达到目标的 105%

# 作用：生成月份 × 品类销售额透视表
# 使用场景：制作 Excel 报表中的交叉汇总页
monthly_category_report = pd.pivot_table(
    data=orders_clean,
    index="order_month",
    columns="category",
    values="amount",
    aggfunc="sum",
    fill_value=0,
    margins=True,
    margins_name="合计",
)

print(monthly_category_report.head())  # 例如：行是月份，列是品类，单元格是销售额
```

## 十、方法总结表

| 方法 | 作用 | 常用参数 | 返回结果 | 使用场景 |
| --- | --- | --- | --- | --- |
| `groupby()` | 按字段分组 | `by`、`as_index`、`sort`、`dropna` | 分组对象 | 按月份、地区、门店、品类准备统计 |
| `agg()` | 对分组结果聚合 | `新列名=("原列名", "统计方式")` | 汇总表 | 一次统计销售额、订单数、毛利等多个指标 |
| `sort_values()` | 排序 | `by`、`ascending`、`na_position`、`ignore_index` | 排序后的表 | 查看 Top 排名、低达成率列表 |
| `merge()` | 表关联 | `right`、`on`、`left_on`、`right_on`、`how`、`validate`、`suffixes` | 合并后的表 | 订单关联客户、商品、目标 |
| `rename()` | 修改字段名 | `columns`、`index`、`errors` | 改名后的表 | 合并前统一字段名，或输出报表前改字段名 |
| `pivot_table()` | 生成透视表 | `data`、`index`、`columns`、`values`、`aggfunc`、`fill_value`、`margins` | 交叉汇总表 | 制作月份 × 品类、地区 × 渠道等报表 |

## 十一、练习

1. 按 `region` 汇总销售额、订单数、毛利，并按销售额从高到低排序。
2. 关联客户表后，按 `industry` 汇总销售额和订单数。
3. 关联商品表后，按 `brand` 汇总销售额和毛利。
4. 按 `order_month` 和 `channel` 汇总销售额，找出每个月销售额最高的渠道。
5. 使用 `pivot_table()` 生成“地区 × 品类”的销售额透视表。
6. 使用实际销售汇总表和目标表计算 `sales_achievement_rate`，筛选出达成率低于 `0.8` 的记录。
