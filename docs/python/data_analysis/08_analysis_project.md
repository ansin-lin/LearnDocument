# 综合练习：四份数据的销售分析与报表输出

这一章是数据分析课程的综合练习。

前面章节分别学习了：

- Excel / CSV 读取与检查。
- 数据清洗。
- 分组汇总、表关联和透视表。
- 可视化与结果输出。
- 数据库读取分析与结果回写。

本章把这些内容串成一个完整案例：使用 4 份企业级样例数据，完成销售分析，并输出 CSV、Excel 和图表文件。

## 一、案例背景

你接到一个日本项目中的销售分析任务。

业务方提供了 4 份数据：

- `orders.csv`：订单明细，核心事实数据。
- `customers.xlsx`：客户资料。
- `products.csv`：商品资料。
- `monthly_targets.csv`：月度目标。

业务方希望你输出一份销售分析报表，回答下面几个问题：

1. 各地区销售额、订单数、毛利是多少？
2. 不同行业客户贡献了多少销售额？
3. 每个月、每个品类的销售趋势如何？
4. 实际销售额和月度目标相比，达成率是多少？
5. 是否存在目标表没有覆盖到的实际销售数据？
6. 能否把结果导出成 Excel，方便业务方查看？

## 二、最终交付物

本练习完成后，至少生成下面这些结果文件：

```text
output/sales_report/
├── region_summary.csv
├── industry_summary.csv
├── monthly_category_summary.csv
├── target_compare.csv
├── region_sales.png
├── monthly_sales_trend.png
└── sales_analysis_report.xlsx
```

Excel 文件中至少包含这些 Sheet：

| Sheet 名 | 内容 |
| --- | --- |
| `region_summary` | 地区销售汇总 |
| `industry_summary` | 行业销售汇总 |
| `monthly_sales` | 月度销售趋势数据 |
| `monthly_category` | 月度品类销售汇总 |
| `target_compare` | 实际销售与目标对比 |
| `missing_target` | 没有匹配到目标的数据 |

## 三、完整实现步骤

下面的代码按真实项目脚本顺序组织。学员应该先完整运行，再逐段理解每一步在做什么。

## 1. 导入库和准备路径

```python
from pathlib import Path

import matplotlib.pyplot as plt
import pandas as pd


# 作用：定义数据文件所在目录
# 使用场景：统一管理输入文件路径，避免代码里反复写字符串路径
data_dir = Path("data")

# 作用：定义输出目录
# 使用场景：统一保存 CSV、Excel、图片等结果文件
output_dir = Path("output") / "sales_report"

# 作用：创建输出目录，如果目录已存在也不报错
# 使用场景：自动化报表脚本会反复执行，目录可能已经存在
output_dir.mkdir(parents=True, exist_ok=True)

print(output_dir)  # 例如：output\sales_report
```

## 2. 读取四份数据

读取阶段要尽量明确字段类型。特别是编号字段，不能被 pandas 当成数字处理，否则前导零可能丢失。

```python
# 作用：读取订单明细 CSV
# 使用场景：订单表是销售分析的核心事实数据
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

# 作用：读取客户资料 Excel
# 使用场景：后续按客户行业、客户等级、客户类型分析销售额
customers = pd.read_excel(
    data_dir / "customers.xlsx",
    dtype={
        "customer_id": "string",
        "customer_name": "string",
    },
)

# 作用：读取商品资料 CSV
# 使用场景：后续按商品品类、品牌、供应商分析销售额
products = pd.read_csv(
    data_dir / "products.csv",
    dtype={
        "product_id": "string",
        "sku": "string",
    },
)

# 作用：读取月度目标 CSV
# 使用场景：后续计算销售额达成率和毛利达成率
targets = pd.read_csv(
    data_dir / "monthly_targets.csv",
    dtype={
        "target_month": "string",
        "store_code": "string",
    },
)

# 作用：查看四份数据的行列数
# 使用场景：确认文件读取成功，并了解数据规模
print(orders.shape)     # 例如：(56090, 24) 表示订单表 56090 行、24 列
print(customers.shape)  # 例如：(5000, 18) 表示客户表 5000 行、18 列
print(products.shape)   # 例如：(24000, 16) 表示商品表 24000 行、16 列
print(targets.shape)    # 例如：(27648, 12) 表示目标表 27648 行、12 列
```

## 3. 数据初步检查

检查阶段的目标不是修改数据，而是先确认数据有没有明显问题。

```python
# 作用：查看订单表前 3 行
# 使用场景：确认字段名、样例值、日期和金额格式是否符合预期
print(orders.head(3))

# 作用：查看订单表字段类型和非空数量
# 使用场景：确认日期、金额、编号字段是否读取正确
orders.info()

# 作用：检查订单表缺失值最多的字段
# 使用场景：找出后续清洗重点字段
print(orders.isna().sum().sort_values(ascending=False).head(10))
# 例如：
# return_reason     53000
# shipment_days       120
# notes                80

# 作用：检查订单明细业务键是否重复
# 使用场景：同一订单的同一行号不应该重复
print(orders.duplicated(subset=["order_id", "line_no"]).sum())  # 例如：0 表示没有重复明细

# 作用：检查客户编号是否重复
# 使用场景：客户表作为维表时，一个 customer_id 应该只有一行
print(customers["customer_id"].duplicated().sum())  # 例如：0 表示客户编号没有重复

# 作用：检查商品编号是否重复
# 使用场景：商品表作为维表时，一个 product_id 应该只有一行
print(products["product_id"].duplicated().sum())  # 例如：0 表示商品编号没有重复

# 作用：查看渠道分布
# 使用场景：确认订单来源渠道是否正常
print(orders["channel"].value_counts())
# 例如：
# Online     24000
# Store      18000
# Partner    14090

# 作用：查看订单状态分布
# 使用场景：确认是否包含取消、退货、未完成等状态
print(orders["order_status"].value_counts())
```

## 4. 复制数据，开始清洗

正式清洗前先复制数据，避免直接修改原始读取结果。

```python
# 作用：复制原始数据
# 使用场景：保留原始读取结果，清洗逻辑写在 clean 变量上
orders_clean = orders.copy()
customers_clean = customers.copy()
products_clean = products.copy()
targets_clean = targets.copy()
```

## 5. 清洗文本字段

企业数据中，文本字段最常见的问题是前后空格、全角半角不统一、空字符串混入等。本练习先处理前后空格。

```python
# 作用：定义一个清理文本前后空格的函数
# 使用场景：多张表都需要做相同文本清洗时，避免重复写代码
def strip_text_columns(df):
    # 作用：找出字符串类型和 object 类型字段
    # 使用场景：只对文本字段做 str.strip()，不影响数值和日期字段
    text_columns = df.select_dtypes(include=["object", "string"]).columns

    for col in text_columns:
        # 作用：去掉文本前后空格，同时保留缺失值
        # 使用场景：合并字段、分类字段、状态字段中可能存在多余空格时
        df[col] = df[col].where(df[col].isna(), df[col].astype("string").str.strip())

    return df


# 作用：清理四张表的文本字段
# 使用场景：避免因为前后空格导致 merge() 匹配失败
orders_clean = strip_text_columns(orders_clean)
customers_clean = strip_text_columns(customers_clean)
products_clean = strip_text_columns(products_clean)
targets_clean = strip_text_columns(targets_clean)

print(orders_clean["channel"].value_counts())  # 例如：确认渠道名称已经统一
```

## 6. 转换日期和数值字段

读取文件后，即使字段看起来像日期或数字，也要检查并明确转换。

```python
# 作用：把订单日期转换成日期类型
# 使用场景：后续要按月份统计销售趋势
orders_clean["order_date"] = pd.to_datetime(
    orders_clean["order_date"],
    errors="coerce",
)

# 作用：把客户注册日期转换成日期类型
# 使用场景：后续如果要分析新客户、老客户，可以直接使用日期方法
customers_clean["signup_date"] = pd.to_datetime(
    customers_clean["signup_date"],
    errors="coerce",
)

# 作用：把商品上线日期转换成日期类型
# 使用场景：后续如果要分析新品销售，可以直接使用日期方法
products_clean["launch_date"] = pd.to_datetime(
    products_clean["launch_date"],
    errors="coerce",
)

# 作用：保证目标月份是 YYYY-MM 字符串
# 使用场景：后续和订单表生成的 order_month 合并
targets_clean["target_month"] = pd.to_datetime(
    targets_clean["target_month"],
    format="%Y-%m",
    errors="coerce",
).dt.strftime("%Y-%m")

# 作用：定义订单表中需要转换成数值的字段
# 使用场景：金额、数量、折扣率必须是数值类型才能汇总计算
numeric_columns = [
    "quantity",
    "unit_price",
    "discount_rate",
    "amount",
    "cost_amount",
    "gross_profit",
    "shipment_days",
]

for col in numeric_columns:
    # 作用：把字段转换成数值，非法值转换成 NaN
    # 使用场景：金额或数量中混入异常字符时，先转换再检查
    orders_clean[col] = pd.to_numeric(orders_clean[col], errors="coerce")

print(orders_clean[numeric_columns].dtypes)
# 例如：
# quantity           int64
# unit_price         int64
# amount             int64
```

## 7. 删除关键字段缺失和重复数据

关键字段缺失会导致后续无法关联或统计。重复明细会导致销售额被重复计算。

```python
# 作用：删除订单关键字段缺失的记录
# 使用场景：订单编号、客户编号、商品编号、订单日期缺失时，无法做可靠分析
orders_clean = orders_clean.dropna(
    subset=["order_id", "customer_id", "product_id", "order_date"]
)

# 作用：把折扣率缺失值填充为 0
# 使用场景：缺失折扣率通常可以按没有折扣处理，具体要以项目规格为准
orders_clean["discount_rate"] = orders_clean["discount_rate"].fillna(0)

# 作用：删除订单明细重复记录
# 使用场景：同一订单编号和同一行号重复时，避免重复计算金额
orders_clean = orders_clean.drop_duplicates(subset=["order_id", "line_no"])

# 作用：删除客户表重复客户
# 使用场景：客户表作为维表参与 merge() 前，确保 customer_id 唯一
customers_clean = customers_clean.drop_duplicates(subset=["customer_id"])

# 作用：删除商品表重复商品
# 使用场景：商品表作为维表参与 merge() 前，确保 product_id 唯一
products_clean = products_clean.drop_duplicates(subset=["product_id"])

print(orders_clean.shape)     # 例如：(56090, 24) 清洗后订单行列数
print(customers_clean.shape)  # 例如：(5000, 18) 清洗后客户行列数
print(products_clean.shape)   # 例如：(24000, 16) 清洗后商品行列数
```

## 8. 统一状态字段

不同系统导出的状态字段可能有 `Y`、`y`、`yes`、`TRUE` 等多种写法。分析前要统一。

```python
# 作用：统一退货标记
# 使用场景：后续统计退货订单时，避免同一含义有多种写法
orders_clean["is_returned"] = orders_clean["is_returned"].replace(
    {
        "y": "Y",
        "yes": "Y",
        "true": "Y",
        "n": "N",
        "no": "N",
        "false": "N",
    }
)

# 作用：查看统一后的退货标记分布
# 使用场景：确认状态字段是否只有预期值
print(orders_clean["is_returned"].value_counts(dropna=False))
# 例如：
# N    54000
# Y     2090
```

## 9. 生成月份字段

目标表是月度目标，所以订单明细也要生成月度字段。

```python
# 作用：从订单日期生成月份字段
# 使用场景：按月汇总销售额，以及和目标表按月份合并
orders_clean["order_month"] = orders_clean["order_date"].dt.to_period("M").astype("string")

# 作用：查看月份字段样例
# 使用场景：确认格式是否可以和 target_month 匹配
print(orders_clean[["order_date", "order_month"]].head(3))
# 例如：
#   order_date order_month
# 0 2025-01-03     2025-01
```

## 10. 地区销售汇总

地区汇总是最基础的销售分析结果。

```python
# 作用：按地区统计销售额、订单数、毛利
# 使用场景：回答“哪个地区销售表现最好”
region_summary = (
    orders_clean.groupby("region", as_index=False)
    .agg(
        sales_amount=("amount", "sum"),
        order_count=("order_id", "nunique"),
        gross_profit=("gross_profit", "sum"),
    )
    .sort_values("sales_amount", ascending=False, ignore_index=True)
)

# 作用：查看销售额最高的前 5 个地区
# 使用场景：确认地区汇总结果
print(region_summary.head(5))
# 例如：
#   region  sales_amount  order_count  gross_profit
# 0  Kanto     345678901         5021      98765432
```

## 11. 行业销售汇总

订单表只有 `customer_id`，没有客户行业。要做行业分析，必须先关联客户表。

```python
# 作用：订单表关联客户表
# 使用场景：补充客户行业、客户等级、客户类型
orders_with_customer = orders_clean.merge(
    customers_clean[["customer_id", "industry", "customer_level", "customer_type"]],
    on="customer_id",
    how="left",
    validate="many_to_one",
)

# 作用：检查客户资料是否匹配成功
# 使用场景：发现订单中的 customer_id 是否有客户主数据缺失
missing_customer_count = orders_with_customer["industry"].isna().sum()
print(missing_customer_count)  # 例如：0 表示订单都匹配到了客户行业

# 作用：按客户行业汇总销售额和订单数
# 使用场景：回答“哪个行业贡献销售额最多”
industry_summary = (
    orders_with_customer.groupby("industry", as_index=False)
    .agg(
        sales_amount=("amount", "sum"),
        order_count=("order_id", "nunique"),
        gross_profit=("gross_profit", "sum"),
    )
    .sort_values("sales_amount", ascending=False, ignore_index=True)
)

print(industry_summary.head(5))  # 例如：显示销售额最高的前 5 个行业
```

## 12. 商品和品类分析

订单表中已有品类字段，但正式项目中更推荐关联商品主数据确认商品属性。

```python
# 作用：订单表关联商品表
# 使用场景：补充商品名称、品牌、供应商等商品属性
orders_full = orders_with_customer.merge(
    products_clean[["product_id", "product_name", "brand", "supplier_name"]],
    on="product_id",
    how="left",
    validate="many_to_one",
)

# 作用：检查商品资料是否匹配成功
# 使用场景：发现订单中的 product_id 是否有商品主数据缺失
missing_product_count = orders_full["product_name"].isna().sum()
print(missing_product_count)  # 例如：0 表示订单都匹配到了商品名称

# 作用：按月份和品类汇总销售额、订单数、毛利
# 使用场景：回答“每个月各品类销售趋势如何”
monthly_category_summary = (
    orders_full.groupby(["order_month", "category"], as_index=False)
    .agg(
        sales_amount=("amount", "sum"),
        order_count=("order_id", "nunique"),
        gross_profit=("gross_profit", "sum"),
    )
    .sort_values(["order_month", "sales_amount"], ascending=[True, False], ignore_index=True)
)

print(monthly_category_summary.head(5))
# 例如：
#   order_month category  sales_amount  order_count  gross_profit
# 0     2025-01       PC     987654321         2031     234567890
```

## 13. 目标达成对比

目标表的粒度是月份、地区、都道府县、门店、渠道、品类。实际订单必须先汇总到同样粒度，再和目标表合并。

```python
# 作用：按目标表粒度汇总实际销售结果
# 使用场景：准备和月度目标表做达成率对比
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

# 作用：把实际结果和目标表合并
# 使用场景：计算销售额达成率和毛利达成率
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

# 作用：检查没有匹配到目标的数据
# 使用场景：发现目标表漏维护、字段不一致或主数据不一致
missing_target = target_compare[target_compare["target_sales_amount"].isna()].copy()
print(missing_target.shape)  # 例如：(12800, 16) 表示有 12800 行没有匹配到目标

# 作用：计算销售额达成率
# 使用场景：实际销售额除以目标销售额
target_compare["sales_achievement_rate"] = (
    target_compare["actual_sales_amount"] / target_compare["target_sales_amount"]
)

# 作用：计算毛利达成率
# 使用场景：实际毛利除以目标毛利
target_compare["profit_achievement_rate"] = (
    target_compare["actual_gross_profit"] / target_compare["target_gross_profit"]
)

print(target_compare[[
    "target_month",
    "region",
    "store_code",
    "category",
    "actual_sales_amount",
    "target_sales_amount",
    "sales_achievement_rate",
]].head(3))
# 例如：sales_achievement_rate 为 1.05 表示销售额达成率 105%
```

如果 `missing_target` 不是空表，不能直接忽略。日本项目里通常要把这部分作为确认事项提交给业务方或上级。

## 14. 月度销售趋势数据

折线图要先准备月度汇总表。

```python
# 作用：按月份汇总销售额
# 使用场景：准备绘制月度销售趋势折线图
monthly_sales = (
    orders_clean.groupby("order_month", as_index=False)
    .agg(
        sales_amount=("amount", "sum"),
        order_count=("order_id", "nunique"),
        gross_profit=("gross_profit", "sum"),
    )
    .sort_values("order_month", ignore_index=True)
)

print(monthly_sales.head(3))
# 例如：
#   order_month  sales_amount  order_count  gross_profit
# 0     2025-01     123456789         1024      34567890
```

## 15. 输出图表

本练习输出两张图：

- 地区销售额柱状图。
- 月度销售趋势折线图。

```python
# 作用：绘制地区销售额柱状图
# 使用场景：直观看出销售额最高和最低的地区
plt.figure(figsize=(10, 5), dpi=120)
plt.bar(region_summary["region"], region_summary["sales_amount"], color="#4C78A8")
plt.title("Sales Amount by Region")
plt.xlabel("Region")
plt.ylabel("Sales Amount")
plt.xticks(rotation=30)
plt.tight_layout()

# 作用：保存地区销售额图表
# 使用场景：把图表插入 Excel、邮件或说明材料
plt.savefig(output_dir / "region_sales.png", dpi=150, bbox_inches="tight", facecolor="white")
plt.close()

print((output_dir / "region_sales.png").exists())  # 例如：True 表示柱状图已生成

# 作用：绘制月度销售趋势折线图
# 使用场景：查看销售额随月份变化的趋势
plt.figure(figsize=(12, 5), dpi=120)
plt.plot(
    monthly_sales["order_month"],
    monthly_sales["sales_amount"],
    marker="o",
    color="#F58518",
    label="Monthly Sales",
)
plt.title("Monthly Sales Trend")
plt.xlabel("Month")
plt.ylabel("Sales Amount")
plt.xticks(rotation=45)
plt.grid(True, axis="y", linestyle="--", alpha=0.4)
plt.legend()
plt.tight_layout()

# 作用：保存月度销售趋势图
# 使用场景：把趋势图作为报表附件或 Excel 插图素材
plt.savefig(output_dir / "monthly_sales_trend.png", dpi=150, bbox_inches="tight", facecolor="white")
plt.close()

print((output_dir / "monthly_sales_trend.png").exists())  # 例如：True 表示折线图已生成
```

## 16. 导出 CSV

CSV 适合作为中间结果、接口文件或简单交付文件。

```python
# 作用：导出地区销售汇总 CSV
# 使用场景：给其他系统继续处理，或作为分析结果留档
region_summary.to_csv(
    output_dir / "region_summary.csv",
    index=False,
    encoding="utf-8-sig",
)

# 作用：导出行业销售汇总 CSV
# 使用场景：单独交付行业分析结果
industry_summary.to_csv(
    output_dir / "industry_summary.csv",
    index=False,
    encoding="utf-8-sig",
)

# 作用：导出月度品类汇总 CSV
# 使用场景：单独交付月度品类分析结果
monthly_category_summary.to_csv(
    output_dir / "monthly_category_summary.csv",
    index=False,
    encoding="utf-8-sig",
)

# 作用：导出目标对比 CSV
# 使用场景：业务方需要筛选、排序、确认目标达成情况时
target_compare.to_csv(
    output_dir / "target_compare.csv",
    index=False,
    encoding="utf-8-sig",
)

print((output_dir / "region_summary.csv").exists())  # 例如：True 表示 CSV 已生成
```

## 17. 导出 Excel 报表

正式交付时，把多张结果表放进一个 Excel 工作簿更方便查看。

```python
# 作用：把多张分析结果写入一个 Excel 文件
# 使用场景：正式交付销售分析报表时
with pd.ExcelWriter(output_dir / "sales_analysis_report.xlsx", engine="openpyxl") as writer:
    # 作用：写入地区销售汇总
    # 使用场景：查看各地区销售额、订单数、毛利
    region_summary.to_excel(writer, sheet_name="region_summary", index=False)

    # 作用：写入行业销售汇总
    # 使用场景：查看不同行业客户的销售贡献
    industry_summary.to_excel(writer, sheet_name="industry_summary", index=False)

    # 作用：写入月度销售趋势数据
    # 使用场景：支持折线图数据追溯
    monthly_sales.to_excel(writer, sheet_name="monthly_sales", index=False)

    # 作用：写入月度品类汇总
    # 使用场景：查看每个月不同品类的销售情况
    monthly_category_summary.to_excel(writer, sheet_name="monthly_category", index=False)

    # 作用：写入目标达成对比
    # 使用场景：查看实际销售和目标之间的差距
    target_compare.to_excel(writer, sheet_name="target_compare", index=False)

    # 作用：写入未匹配目标的数据
    # 使用场景：作为业务确认事项，确认目标表是否漏维护
    missing_target.to_excel(writer, sheet_name="missing_target", index=False)

print((output_dir / "sales_analysis_report.xlsx").exists())  # 例如：True 表示 Excel 报表已生成
```

## 18. 交付前检查

脚本最后要检查结果文件是否都生成。真实项目中，这一步可以写入日志。

```python
# 作用：定义应该生成的文件清单
# 使用场景：脚本执行完成后统一检查交付物
expected_files = [
    "region_summary.csv",
    "industry_summary.csv",
    "monthly_category_summary.csv",
    "target_compare.csv",
    "region_sales.png",
    "monthly_sales_trend.png",
    "sales_analysis_report.xlsx",
]

for file_name in expected_files:
    file_path = output_dir / file_name

    # 作用：检查每个文件是否存在
    # 使用场景：发现导出失败或路径写错的问题
    print(file_name, file_path.exists())
    # 例如：region_summary.csv True
```

## 四、完整代码

下面是可以整体复制运行的完整版本。

```python
from pathlib import Path

import matplotlib.pyplot as plt
import pandas as pd


data_dir = Path("data")
output_dir = Path("output") / "sales_report"
output_dir.mkdir(parents=True, exist_ok=True)

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
customers = pd.read_excel(
    data_dir / "customers.xlsx",
    dtype={
        "customer_id": "string",
        "customer_name": "string",
    },
)
products = pd.read_csv(
    data_dir / "products.csv",
    dtype={
        "product_id": "string",
        "sku": "string",
    },
)
targets = pd.read_csv(
    data_dir / "monthly_targets.csv",
    dtype={
        "target_month": "string",
        "store_code": "string",
    },
)

print(orders.shape)     # 例如：(56090, 24)
print(customers.shape)  # 例如：(5000, 18)
print(products.shape)   # 例如：(24000, 16)
print(targets.shape)    # 例如：(27648, 12)

print(orders.head(3))
orders.info()
print(orders.isna().sum().sort_values(ascending=False).head(10))
print(orders.duplicated(subset=["order_id", "line_no"]).sum())
print(customers["customer_id"].duplicated().sum())
print(products["product_id"].duplicated().sum())
print(orders["channel"].value_counts())
print(orders["order_status"].value_counts())

orders_clean = orders.copy()
customers_clean = customers.copy()
products_clean = products.copy()
targets_clean = targets.copy()


def strip_text_columns(df):
    text_columns = df.select_dtypes(include=["object", "string"]).columns

    for col in text_columns:
        df[col] = df[col].where(df[col].isna(), df[col].astype("string").str.strip())

    return df


orders_clean = strip_text_columns(orders_clean)
customers_clean = strip_text_columns(customers_clean)
products_clean = strip_text_columns(products_clean)
targets_clean = strip_text_columns(targets_clean)

orders_clean["order_date"] = pd.to_datetime(orders_clean["order_date"], errors="coerce")
customers_clean["signup_date"] = pd.to_datetime(customers_clean["signup_date"], errors="coerce")
products_clean["launch_date"] = pd.to_datetime(products_clean["launch_date"], errors="coerce")
targets_clean["target_month"] = pd.to_datetime(
    targets_clean["target_month"],
    format="%Y-%m",
    errors="coerce",
).dt.strftime("%Y-%m")

numeric_columns = [
    "quantity",
    "unit_price",
    "discount_rate",
    "amount",
    "cost_amount",
    "gross_profit",
    "shipment_days",
]
for col in numeric_columns:
    orders_clean[col] = pd.to_numeric(orders_clean[col], errors="coerce")

orders_clean = orders_clean.dropna(
    subset=["order_id", "customer_id", "product_id", "order_date"]
)
orders_clean["discount_rate"] = orders_clean["discount_rate"].fillna(0)
orders_clean = orders_clean.drop_duplicates(subset=["order_id", "line_no"])
customers_clean = customers_clean.drop_duplicates(subset=["customer_id"])
products_clean = products_clean.drop_duplicates(subset=["product_id"])

orders_clean["is_returned"] = orders_clean["is_returned"].replace(
    {
        "y": "Y",
        "yes": "Y",
        "true": "Y",
        "n": "N",
        "no": "N",
        "false": "N",
    }
)

orders_clean["order_month"] = orders_clean["order_date"].dt.to_period("M").astype("string")

region_summary = (
    orders_clean.groupby("region", as_index=False)
    .agg(
        sales_amount=("amount", "sum"),
        order_count=("order_id", "nunique"),
        gross_profit=("gross_profit", "sum"),
    )
    .sort_values("sales_amount", ascending=False, ignore_index=True)
)

orders_with_customer = orders_clean.merge(
    customers_clean[["customer_id", "industry", "customer_level", "customer_type"]],
    on="customer_id",
    how="left",
    validate="many_to_one",
)

missing_customer_count = orders_with_customer["industry"].isna().sum()
print(missing_customer_count)  # 例如：0 表示订单都匹配到了客户行业

industry_summary = (
    orders_with_customer.groupby("industry", as_index=False)
    .agg(
        sales_amount=("amount", "sum"),
        order_count=("order_id", "nunique"),
        gross_profit=("gross_profit", "sum"),
    )
    .sort_values("sales_amount", ascending=False, ignore_index=True)
)

orders_full = orders_with_customer.merge(
    products_clean[["product_id", "product_name", "brand", "supplier_name"]],
    on="product_id",
    how="left",
    validate="many_to_one",
)

missing_product_count = orders_full["product_name"].isna().sum()
print(missing_product_count)  # 例如：0 表示订单都匹配到了商品名称

monthly_category_summary = (
    orders_full.groupby(["order_month", "category"], as_index=False)
    .agg(
        sales_amount=("amount", "sum"),
        order_count=("order_id", "nunique"),
        gross_profit=("gross_profit", "sum"),
    )
    .sort_values(["order_month", "sales_amount"], ascending=[True, False], ignore_index=True)
)

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

missing_target = target_compare[target_compare["target_sales_amount"].isna()].copy()
print(missing_target.shape)  # 例如：(12800, 16)

target_compare["sales_achievement_rate"] = (
    target_compare["actual_sales_amount"] / target_compare["target_sales_amount"]
)
target_compare["profit_achievement_rate"] = (
    target_compare["actual_gross_profit"] / target_compare["target_gross_profit"]
)

monthly_sales = (
    orders_clean.groupby("order_month", as_index=False)
    .agg(
        sales_amount=("amount", "sum"),
        order_count=("order_id", "nunique"),
        gross_profit=("gross_profit", "sum"),
    )
    .sort_values("order_month", ignore_index=True)
)

plt.figure(figsize=(10, 5), dpi=120)
plt.bar(region_summary["region"], region_summary["sales_amount"], color="#4C78A8")
plt.title("Sales Amount by Region")
plt.xlabel("Region")
plt.ylabel("Sales Amount")
plt.xticks(rotation=30)
plt.tight_layout()
plt.savefig(output_dir / "region_sales.png", dpi=150, bbox_inches="tight", facecolor="white")
plt.close()

plt.figure(figsize=(12, 5), dpi=120)
plt.plot(
    monthly_sales["order_month"],
    monthly_sales["sales_amount"],
    marker="o",
    color="#F58518",
    label="Monthly Sales",
)
plt.title("Monthly Sales Trend")
plt.xlabel("Month")
plt.ylabel("Sales Amount")
plt.xticks(rotation=45)
plt.grid(True, axis="y", linestyle="--", alpha=0.4)
plt.legend()
plt.tight_layout()
plt.savefig(output_dir / "monthly_sales_trend.png", dpi=150, bbox_inches="tight", facecolor="white")
plt.close()

region_summary.to_csv(output_dir / "region_summary.csv", index=False, encoding="utf-8-sig")
industry_summary.to_csv(output_dir / "industry_summary.csv", index=False, encoding="utf-8-sig")
monthly_category_summary.to_csv(
    output_dir / "monthly_category_summary.csv",
    index=False,
    encoding="utf-8-sig",
)
target_compare.to_csv(output_dir / "target_compare.csv", index=False, encoding="utf-8-sig")

with pd.ExcelWriter(output_dir / "sales_analysis_report.xlsx", engine="openpyxl") as writer:
    region_summary.to_excel(writer, sheet_name="region_summary", index=False)
    industry_summary.to_excel(writer, sheet_name="industry_summary", index=False)
    monthly_sales.to_excel(writer, sheet_name="monthly_sales", index=False)
    monthly_category_summary.to_excel(writer, sheet_name="monthly_category", index=False)
    target_compare.to_excel(writer, sheet_name="target_compare", index=False)
    missing_target.to_excel(writer, sheet_name="missing_target", index=False)

expected_files = [
    "region_summary.csv",
    "industry_summary.csv",
    "monthly_category_summary.csv",
    "target_compare.csv",
    "region_sales.png",
    "monthly_sales_trend.png",
    "sales_analysis_report.xlsx",
]

for file_name in expected_files:
    file_path = output_dir / file_name
    print(file_name, file_path.exists())  # 例如：region_summary.csv True
```

## 五、数据库扩展任务

如果你已经学完第 07 章，可以把文件型分析改造成数据库读取版本。

数据库版不要求重复清洗所有逻辑，重点是练习：

- 用 SQL 控制读取范围。
- 用 `pd.read_sql()` 读取数据库结果。
- 用 pandas 汇总。
- 写回测试表或工作表。

```python
import pandas as pd


# 作用：定义数据库查询 SQL
# 使用场景：从数据库读取销售订单数据，而不是从 CSV 读取
sql = """
SELECT
    order_id,
    order_date,
    region,
    amount,
    gross_profit
FROM sales_orders
WHERE order_date >= :start_date
  AND order_date < :end_date
  AND order_status = :order_status
"""

# 作用：定义查询参数
# 使用场景：避免直接拼接 SQL 字符串
params = {
    "start_date": "2025-01-01",
    "end_date": "2026-01-01",
    "order_status": "completed",
}

# 作用：读取数据库订单数据
# 使用场景：数据库作为分析数据源时
orders_db = pd.read_sql(
    sql=sql,
    con=conn,
    params=params,
    parse_dates=["order_date"],
)

print(orders_db.shape)  # 例如：(56090, 5)

# 作用：按地区汇总数据库订单数据
# 使用场景：复用文件型分析中的 groupby + agg 思路
region_summary_db = (
    orders_db.groupby("region", as_index=False)
    .agg(
        sales_amount=("amount", "sum"),
        order_count=("order_id", "nunique"),
        gross_profit=("gross_profit", "sum"),
    )
)

# 作用：写回数据库测试表
# 使用场景：把分析结果交给 BI、SQL 查询或后续系统使用
region_summary_db.to_sql(
    name="sales_region_summary_work",
    con=conn,
    schema="analysis",
    if_exists="append",
    index=False,
    chunksize=1000,
    method="multi",
)

print("write finished")  # 例如：write finished
```

注意：正式环境不要随便使用 `if_exists="replace"`。如果必须重建表，需要先确认影响范围、备份策略和项目审批规则。

## 六、交付检查清单

完成练习后，按下面清单自查。

| 检查项 | 应确认的内容 |
| --- | --- |
| 数据读取 | 4 份文件都能读取成功，`shape` 输出合理 |
| 数据检查 | 能说明缺失值、重复值、渠道分布、订单状态分布 |
| 数据清洗 | 日期、数值、文本空格、重复记录、状态字段已处理 |
| 表关联 | 客户表、商品表合并后行数没有异常放大 |
| 目标对比 | 生成 `sales_achievement_rate` 和 `profit_achievement_rate` |
| 未匹配目标 | 能输出 `missing_target`，并说明需要业务确认 |
| 图表输出 | 生成 2 张 PNG 图片 |
| CSV 输出 | 至少生成 4 个 CSV 文件 |
| Excel 输出 | 生成 1 个多 Sheet Excel 报表 |
| 数据库扩展 | 如果完成数据库任务，必须写入测试表或工作表，不直接写正式表 |

## 七、追加练习

如果你已经完成基础要求，可以继续扩展：

1. 增加“门店销售额 Top 10”结果表。
2. 增加“品牌销售额 Top 10”结果表。
3. 增加“退货订单汇总”结果表。
4. 增加“低达成率列表”，筛选 `sales_achievement_rate < 0.8`。
5. 把 `missing_target` 单独导出为 CSV，作为业务确认附件。
6. 使用 `openpyxl` 给 Excel 报表增加列宽、标题和金额格式。
7. 把本章脚本改造成自动化报表流程：数据库或文件读取 → 分析 → Excel → 邮件发送。

这一章完成后，数据分析课程主线形成一个完整闭环：读取数据、检查数据、清洗数据、分析数据、输出报表、扩展到数据库。

