# Excel / CSV 数据清洗

上一章已经把文件读进了 `pandas`，这一章开始处理读取后的常见问题。

清洗代码的顺序通常是：

1. 先复制原始数据
2. 再清理文本
3. 再转换日期和数值
4. 再处理缺失值
5. 再处理重复值
6. 最后统一分类字段写法

这一章就按这个顺序讲。

## 一、复制数据：`copy()`

### 基础功能

```python
# 作用：复制一份订单数据，避免直接修改原始表
# 使用场景：清洗前先保留原始数据，后面需要对比时还能回看
orders_clean = orders.copy()

print(orders.shape)  # 例如：(56090, 24) 原始订单表
print(orders_clean.shape)  # 例如：(56090, 24) 复制后的订单表
```

### 什么时候用

- 准备开始清洗数据时。
- 不确定清洗逻辑是否正确，需要保留原始数据时。

### 常见错误

- 直接在 `orders` 上修改，后面发现清洗错了，却没有原始数据可以对比。

## 二、清理文本前后空格：`str.strip()`

### 基础功能

```python
# 作用：去掉 channel 字段前后的空格
# 使用场景：避免 'EC' 和 ' EC ' 被当成两个不同渠道
orders_clean["channel"] = orders_clean["channel"].str.strip()

print(orders_clean["channel"].value_counts().head())  # 例如：显示清理后的渠道分布
```

### 批量清理文本列

```python
# 作用：找出订单表中所有文本列
# 使用场景：一次性清理多个文本字段，不用逐列手写
text_cols = orders_clean.select_dtypes(include="object").columns

print(text_cols.tolist())  # 例如：['order_id', 'order_date', 'customer_id', ...]

# 作用：批量去掉所有文本列的前后空格，同时保留原本的空值
# 使用场景：CSV / Excel 中多个字段都可能带前后空格
for col in text_cols:
    orders_clean[col] = orders_clean[col].where(
        orders_clean[col].isna(),
        orders_clean[col].astype(str).str.strip()
    )
```

### 为什么这里用 `where()`

如果直接写：

```python
orders_clean[col] = orders_clean[col].astype(str).str.strip()
```

真正的空值可能会变成字符串 `"nan"`。

所以批量清理时更建议保留空值：

```python
orders_clean[col] = orders_clean[col].where(
    orders_clean[col].isna(),
    orders_clean[col].astype(str).str.strip()
)
```

## 三、转换日期：`pd.to_datetime()`

### 基础功能

```python
# 作用：把 order_date 从字符串转换成日期类型
# 使用场景：后面要按月份统计、筛选日期范围、提取年份月份
orders_clean["order_date"] = pd.to_datetime(orders_clean["order_date"])

print(orders_clean["order_date"].dtype)  # 例如：datetime64[ns]
```

### 常用参数示例

```python
# 作用：把非法日期转换成 NaT，而不是让程序直接报错
# 使用场景：Excel / CSV 中可能混入 '无效日期'、空字符串等脏数据
orders_clean["order_date"] = pd.to_datetime(
    orders_clean["order_date"],
    errors="coerce"
)

print(orders_clean["order_date"].isna().sum())  # 例如：0 或少量非法日期数量
print(orders_clean["order_date"].head(3))  # 例如：2025-01-26 / 2025-08-06 / 2025-03-14
```

### 指定日期格式

```python
# 作用：按固定格式解析日期
# 使用场景：日期格式统一时，指定 format 可以减少误判
orders_clean["order_date"] = pd.to_datetime(
    orders_clean["order_date"],
    format="%Y-%m-%d",
    errors="coerce"
)

print(orders_clean["order_date"].dtype)  # 例如：datetime64[ns]
```

常用参数说明：

| 参数 | 作用 | 常见场景 |
| --- | --- | --- |
| `errors="coerce"` | 非法日期变成 `NaT` | 数据里可能有非法日期 |
| `format` | 指定日期格式 | 日期格式统一时使用 |

什么时候不用：

- 如果只是展示原始文本，不做日期筛选和时间统计，可以暂时不转换。

## 四、转换数值：`pd.to_numeric()`

### 基础功能

```python
# 作用：把 amount 转换成数值类型
# 使用场景：金额字段如果是文本，不能直接求和、排序、分组统计
orders_clean["amount"] = pd.to_numeric(orders_clean["amount"])

print(orders_clean["amount"].dtype)  # 例如：float64
```

### 常用参数示例

```python
# 作用：把无法转换的金额值变成 NaN
# 使用场景：金额列里可能混入空字符串、文字、异常符号
orders_clean["amount"] = pd.to_numeric(
    orders_clean["amount"],
    errors="coerce"
)

print(orders_clean["amount"].isna().sum())  # 例如：111 表示有 111 条金额无法转换或为空
```

### 批量转换数值列

```python
# 作用：批量转换订单表中的金额、数量、成本、毛利等数值字段
# 使用场景：进入汇总统计前，先保证这些字段都能参与计算
numeric_cols = [
    "quantity",
    "unit_price",
    "discount_rate",
    "amount",
    "cost_amount",
    "gross_profit",
    "shipment_days",
]

for col in numeric_cols:
    orders_clean[col] = pd.to_numeric(
        orders_clean[col],
        errors="coerce"
    )

print(orders_clean[["quantity", "amount", "gross_profit"]].dtypes)
# 例如：
# quantity          int64
# amount          float64
# gross_profit    float64
```

常用参数说明：

| 参数 | 作用 | 常见场景 |
| --- | --- | --- |
| `errors="coerce"` | 无法转换的值变成 `NaN` | 金额列混入异常文字 |
| `downcast` | 尝试转换成更小的数值类型 | 数据量大、需要节省内存时 |

注意：

- 带千位逗号的金额最好在 `read_csv(thousands=",")` 阶段处理。
- 已经读进来的文本金额，也可以先用字符串方法去掉逗号，再转数值。

## 五、删除缺失值：`dropna()`

### 基础功能

```python
# 作用：删除包含缺失值的行
# 使用场景：只适合临时演示，不建议一上来就对整表使用
sample_dropna = orders_clean.dropna()

print(sample_dropna.shape)  # 例如：行数可能明显减少
```

### 按关键字段删除

```python
# 作用：先查看关键字段缺失数量
# 使用场景：删除前先确认缺失影响范围
critical_cols = ["order_id", "customer_id", "product_id", "order_date"]

print(orders_clean[critical_cols].isna().sum())
# 例如：
# order_id       0
# customer_id    0
# product_id     0
# order_date     0

# 作用：只删除关键字段缺失的订单
# 使用场景：主键、客户、商品、日期缺失时，后续分析无法可靠进行
orders_clean = orders_clean.dropna(subset=critical_cols)

print(orders_clean.shape)  # 例如：(56090, 24) 如果没有关键缺失，行数不变
```

### `how` 参数示例

```python
# 作用：只有指定字段全部为空时才删除
# 使用场景：几列备注字段全部为空才认为这行无效
orders_clean = orders_clean.dropna(
    subset=["amount", "gross_profit"],
    how="all"
)

print(orders_clean.shape)  # 例如：删除金额和毛利都为空的行后剩余行数
```

常用参数说明：

| 参数 | 作用 | 常见场景 |
| --- | --- | --- |
| `subset` | 只看指定列 | 删除关键字段缺失 |
| `how="any"` | 任意一个为空就删除 | 关键字段必须全部完整 |
| `how="all"` | 全部为空才删除 | 多个辅助字段全部为空才删除 |

常见错误：

- 对整张表直接 `dropna()`，导致大量正常数据被删。

## 六、填充缺失值：`fillna()`

### 基础功能

```python
# 作用：把折扣率缺失值填成 0
# 使用场景：折扣率为空可以理解为没有折扣
orders_clean["discount_rate"] = orders_clean["discount_rate"].fillna(0)

print(orders_clean["discount_rate"].isna().sum())  # 例如：0 表示折扣率已无缺失
```

### 固定文本填充

```python
# 作用：把缺失地区填成 Unknown
# 使用场景：暂时不知道真实地区，但又不想删除整行数据
orders_clean["region"] = orders_clean["region"].fillna("Unknown")

print(orders_clean["region"].value_counts(dropna=False).head())  # 例如：包含 Unknown 的地区分布
```

### 前后值填充

```python
# 作用：用上一行的值填充当前缺失值
# 使用场景：某些 Excel 合并单元格导出后，只有第一行有地区，下面几行为空
orders_clean["region"] = orders_clean["region"].fillna(method="ffill")

print(orders_clean["region"].isna().sum())  # 例如：0 或剩余无法填充的数量
```

常用参数说明：

| 参数 | 作用 | 常见场景 |
| --- | --- | --- |
| `value` | 固定值填充 | 折扣率填 0、未知地区填 Unknown |
| `method="ffill"` | 用上一行填充 | Excel 合并单元格导出 |
| `method="bfill"` | 用下一行填充 | 少量结构化空值 |

什么时候不用：

- 金额、客户编号、商品编号不要随便填默认值。
- 不知道缺失含义时，先统计和保留问题数据。

## 七、删除重复值：`drop_duplicates()`

### 基础功能

```python
# 作用：删除完全重复的行
# 使用场景：临时处理整行完全相同的重复数据
orders_no_dup = orders_clean.drop_duplicates()

print(orders_no_dup.shape)  # 例如：删除完全重复行后的数据规模
```

### 按组合键删除重复

```python
# 作用：按 order_id + line_no 删除订单明细重复行
# 使用场景：订单表一单多行，不能只按 order_id 判断重复
orders_clean = orders_clean.drop_duplicates(
    subset=["order_id", "line_no"],
    keep="first"
)

print(orders_clean.duplicated(subset=["order_id", "line_no"]).sum())  # 例如：0
```

### 主数据去重

```python
# 作用：删除客户表 customer_id 重复记录
# 使用场景：客户表后面要参与 merge，customer_id 应该唯一
customers_clean = customers.drop_duplicates(subset=["customer_id"], keep="first")

print(customers_clean["customer_id"].duplicated().sum())  # 例如：0

# 作用：删除商品表 product_id 重复记录
# 使用场景：商品表后面要参与 merge，product_id 应该唯一
products_clean = products.drop_duplicates(subset=["product_id"], keep="first")

print(products_clean["product_id"].duplicated().sum())  # 例如：0
```

常用参数说明：

| 参数 | 作用 | 常见场景 |
| --- | --- | --- |
| `subset` | 按哪些列判断重复 | 主键、组合键 |
| `keep="first"` | 保留第一条 | 默认常用 |
| `keep="last"` | 保留最后一条 | 后面的记录更新时 |
| `keep=False` | 重复项全部删除 | 需要彻底排除重复数据时 |

常见错误：

- 订单表只按 `order_id` 去重，会误删一单多行的正常明细。

## 八、替换文本值：`replace()`

### 基础功能

```python
# 作用：把小写 y 替换成大写 Y
# 使用场景：分类字段写法不统一时
orders_clean["is_returned"] = orders_clean["is_returned"].replace("y", "Y")

print(orders_clean["is_returned"].value_counts(dropna=False))  # 例如：显示替换后的退货标记分布
```

### 字典批量替换

```python
# 作用：统一退货标记写法
# 使用场景：同一个含义出现 y / yes / n / no 等多种写法
orders_clean["is_returned"] = orders_clean["is_returned"].replace({
    "y": "Y",
    "yes": "Y",
    "n": "N",
    "no": "N",
})

print(orders_clean["is_returned"].value_counts(dropna=False))
# 例如：
# N    54433
# Y     1657
```

### 正则替换

```python
# 作用：批量去掉备注中的多余空白字符
# 使用场景：文本字段中混入多个连续空格、制表符时
orders_clean["return_reason"] = orders_clean["return_reason"].replace(
    r"\s+",
    " ",
    regex=True
)

print(orders_clean["return_reason"].head(3))  # 例如：显示清理后的前 3 条退货原因
```

常用参数说明：

| 参数 | 作用 | 常见场景 |
| --- | --- | --- |
| `to_replace` | 要替换的旧值 | 单值替换 |
| `value` | 替换后的新值 | 单值替换 |
| `regex=True` | 按正则替换 | 批量清理文本 |

## 九、按顺序完成一次清洗

下面这段代码就是本章的完整清洗顺序。

```python
# 作用：复制订单、客户、商品三张表
# 使用场景：清洗前保留原始数据
orders_clean = orders.copy()
customers_clean = customers.copy()
products_clean = products.copy()

# 作用：清理订单表文本字段前后空格
# 使用场景：避免分类字段因为空格导致统计错误
for col in orders_clean.select_dtypes(include="object").columns:
    orders_clean[col] = orders_clean[col].where(
        orders_clean[col].isna(),
        orders_clean[col].astype(str).str.strip()
    )

# 作用：转换订单日期
# 使用场景：后面要提取月份和做时间统计
orders_clean["order_date"] = pd.to_datetime(orders_clean["order_date"], errors="coerce")

# 作用：转换订单数值字段
# 使用场景：后面要计算销售额、成本、毛利
for col in ["quantity", "unit_price", "discount_rate", "amount", "cost_amount", "gross_profit"]:
    orders_clean[col] = pd.to_numeric(orders_clean[col], errors="coerce")

# 作用：删除关键字段缺失记录
# 使用场景：关键字段缺失会影响后续关联和统计
orders_clean = orders_clean.dropna(subset=["order_id", "customer_id", "product_id", "order_date"])

# 作用：填充折扣率缺失值
# 使用场景：折扣率为空可以理解为没有折扣
orders_clean["discount_rate"] = orders_clean["discount_rate"].fillna(0)

# 作用：删除订单明细重复行
# 使用场景：按 order_id + line_no 判断明细唯一性
orders_clean = orders_clean.drop_duplicates(subset=["order_id", "line_no"])

# 作用：删除客户和商品主数据重复记录
# 使用场景：保证后续 merge 时右表主键唯一
customers_clean = customers_clean.drop_duplicates(subset=["customer_id"])
products_clean = products_clean.drop_duplicates(subset=["product_id"])

# 作用：提取订单月份
# 使用场景：后面按月份汇总销售额
orders_clean["order_month"] = orders_clean["order_date"].dt.to_period("M").astype(str)

print(orders_clean.shape)  # 例如：(56090, 25) 新增 order_month 后列数增加
```

## 方法总结表

| 方法 | 作用 | 常用参数 | 返回值 | 使用场景 |
| --- | --- | --- | --- | --- |
| `copy()` | 复制数据 | 无 | 新对象 | 清洗前保留原始表 |
| `str.strip()` | 去前后空格 | 无 | 文本列 | 清理文本字段 |
| `pd.to_datetime()` | 转日期 | `errors`、`format` | 日期列 | 日期统计前 |
| `pd.to_numeric()` | 转数值 | `errors`、`downcast` | 数值列 | 金额、数量计算前 |
| `dropna()` | 删除缺失 | `subset`、`how` | 新表 | 关键字段缺失时 |
| `fillna()` | 填充缺失 | `value`、`method` | 新列或新表 | 缺失有明确默认值 |
| `drop_duplicates()` | 删除重复 | `subset`、`keep` | 新表 | 主键或组合键重复 |
| `replace()` | 替换值 | `to_replace`、`value`、`regex` | 新列或新表 | 统一文本写法 |

## 练习

1. 复制 `orders` 为 `orders_clean`。
2. 清理订单表所有文本列前后空格。
3. 把 `order_date` 转成日期类型。
4. 把 `amount`、`gross_profit` 转成数值类型。
5. 删除关键字段缺失的订单。
6. 把 `discount_rate` 缺失值填成 0。
7. 删除 `order_id + line_no` 重复记录。
8. 统一 `is_returned` 为 `Y / N`。
