# 数据库读取分析与结果回写

这一章默认你已经在 Python 基础课程中学过数据库连接、游标、基础 SQL 和连接关闭。

这里不重复数据库入门，只讲数据分析工作中常用的数据库读写方式：

1. 从数据库读取查询结果。
2. 把读取结果转换成 `DataFrame`。
3. 用 pandas 做汇总分析。
4. 把分析结果写回数据库表。
5. 控制读取范围、参数、日期类型、分块读取和写入风险。

## 本章使用的数据

前面章节主要使用本地文件：

- `orders.csv`
- `customers.xlsx`
- `products.csv`
- `monthly_targets.csv`

本章切换到数据库场景。为了和前面章节保持业务连续，假设数据库里也有类似的订单表：

```text
sales_orders
```

常见字段示例：

```text
order_id
order_date
customer_id
product_id
region
prefecture
store_code
channel
category
quantity
amount
gross_profit
order_status
```

本章示例中的 `conn` 或 `engine` 表示已经创建好的数据库连接对象。连接创建方式已经在 Python 基础课程讲过，这里不展开。

## 一、先写 SQL：只取分析需要的数据

数据库表通常比 Excel / CSV 大很多。数据分析时不要一上来整表读取，应该先用 SQL 控制字段和范围。

```python
# 作用：定义销售订单查询 SQL，只读取分析需要的字段
# 使用场景：数据库表很大时，先在 SQL 里限制字段和日期范围
sql = """
SELECT
    order_id,
    order_date,
    customer_id,
    product_id,
    region,
    channel,
    category,
    amount,
    gross_profit
FROM sales_orders
WHERE order_date >= :start_date
  AND order_date < :end_date
  AND order_status = :order_status
"""

# 作用：定义 SQL 参数
# 使用场景：日期范围、状态、客户编号等条件由程序传入时
params = {
    "start_date": "2025-01-01",
    "end_date": "2026-01-01",
    "order_status": "completed",
}
```

注意：参数占位符写法会受到数据库连接方式影响。

| 数据库 / 连接方式 | 常见参数写法 | 示例 |
| --- | --- | --- |
| SQLAlchemy | `:name` | `WHERE order_date >= :start_date` |
| psycopg / PostgreSQL | `%s` 或 `%(name)s` | `WHERE order_date >= %(start_date)s` |
| mysqlclient / PyMySQL | `%s` 或 `%(name)s` | `WHERE order_date >= %(start_date)s` |
| Oracle 驱动 | `:name` | `WHERE order_date >= :start_date` |

课程中为了统一说明，后续示例主要使用 SQLAlchemy 风格的 `:name`。

## 二、`pd.read_sql()`：读取 SQL 结果

`pd.read_sql()` 是 pandas 中最常用的数据库读取方法。它可以执行 SQL 查询，并把结果直接变成 `DataFrame`。

### 1. 基础功能示例

```python
import pandas as pd

# 作用：把 SQL 查询结果读取成 DataFrame
# 使用场景：数据来源在数据库，后续仍然用 pandas 做分析时
orders_db = pd.read_sql(sql, conn, params=params)

# 作用：查看读取结果的行列数
# 使用场景：确认从数据库读取了多少数据
print(orders_db.shape)  # 例如：(56090, 9) 表示 56090 行、9 列

# 作用：查看前 3 行数据
# 使用场景：确认字段和样例值是否符合预期
print(orders_db.head(3))  # 例如：显示数据库查询结果前 3 行
```

### 2. 常用参数示例

```python
# 作用：读取 SQL 结果，并指定参数、日期字段和索引字段
# 使用场景：正式分析中需要控制查询条件和字段类型时
orders_db = pd.read_sql(
    sql=sql,
    con=conn,
    params=params,
    parse_dates=["order_date"],
    index_col=None,
)

# 作用：查看字段类型
# 使用场景：确认 order_date 是否已经被识别为日期类型
print(orders_db.dtypes)
# 例如：
# order_id                object
# order_date      datetime64[ns]
# amount                   int64
```

`pd.read_sql()` 常用参数：

| 参数 | 含义 | 常用写法 | 使用场景 |
| --- | --- | --- | --- |
| `sql` | SQL 语句或表名 | `sql` | 执行查询并读取结果 |
| `con` | 数据库连接对象 | `conn`、`engine` | 连接 Oracle / PostgreSQL / MySQL 等数据库 |
| `params` | SQL 参数 | `{"start_date": "2025-01-01"}` | 避免拼接 SQL 字符串 |
| `parse_dates` | 指定日期列 | `["order_date"]` | 读取后直接得到日期类型 |
| `index_col` | 指定索引列 | `None`、`"order_id"` | 一般报表分析中可不设索引 |
| `chunksize` | 分块读取行数 | `10000` | 大数据量读取时 |

## 三、`pd.read_sql_query()`：明确执行查询语句

`pd.read_sql_query()` 和 `pd.read_sql()` 很接近。区别是它语义更明确：这里就是执行一段查询 SQL。

### 1. 基础功能示例

```python
# 作用：执行 SELECT 查询，并返回 DataFrame
# 使用场景：明确知道自己要执行 SQL 查询语句时
high_amount_orders = pd.read_sql_query(
    "SELECT order_id, customer_id, amount FROM sales_orders WHERE amount >= 100000",
    conn,
)

# 作用：查看高金额订单
# 使用场景：确认查询条件是否生效
print(high_amount_orders.head(3))
# 例如：
#    order_id customer_id  amount
# 0  O2025001    C0000123  120000
```

### 2. 常用参数示例

```python
# 作用：使用参数化 SQL 查询高金额订单
# 使用场景：金额阈值由程序、配置文件或画面输入时
high_amount_orders = pd.read_sql_query(
    sql="""
    SELECT
        order_id,
        order_date,
        customer_id,
        amount
    FROM sales_orders
    WHERE amount >= :min_amount
      AND order_date >= :start_date
    """,
    con=conn,
    params={
        "min_amount": 100000,
        "start_date": "2025-01-01",
    },
    parse_dates=["order_date"],
)

# 作用：查看读取结果数量
# 使用场景：判断筛选条件是否过宽或过窄
print(high_amount_orders.shape)  # 例如：(1250, 4) 表示读取了 1250 行、4 列
```

`pd.read_sql_query()` 常用参数：

| 参数 | 含义 | 常用写法 | 使用场景 |
| --- | --- | --- | --- |
| `sql` | 查询 SQL | `SELECT ... FROM ... WHERE ...` | 明确执行查询语句 |
| `con` | 数据库连接对象 | `conn`、`engine` | 指定查询哪个数据库 |
| `params` | SQL 参数 | `{"min_amount": 100000}` | 动态筛选条件 |
| `parse_dates` | 日期列转换 | `["order_date"]` | 后续按月、按日分析 |
| `chunksize` | 分块读取 | `50000` | 查询结果很大时 |

## 四、`pd.read_sql_table()`：读取整张表

`pd.read_sql_table()` 用来直接读取数据库中的整张表。它适合小表，例如客户等级表、地区主数据、商品分类表。

### 1. 基础功能示例

```python
# 作用：直接读取客户主数据表
# 使用场景：表数据量较小，确实需要整表作为维表使用时
customers_db = pd.read_sql_table(
    table_name="customers",
    con=conn,
)

# 作用：查看客户表行列数
# 使用场景：确认维表是否读取成功
print(customers_db.shape)  # 例如：(5000, 12) 表示 5000 行、12 列
```

### 2. 常用参数示例

```python
# 作用：读取指定 schema 下的商品主数据表，并指定索引列
# 使用场景：数据库中存在多个 schema，表名可能重复时
products_db = pd.read_sql_table(
    table_name="products",
    con=conn,
    schema="public",
    index_col=None,
    columns=["product_id", "product_name", "category", "brand"],
)

# 作用：查看读取的字段
# 使用场景：确认只读取了分析需要的列
print(products_db.columns.tolist())
# 例如：['product_id', 'product_name', 'category', 'brand']
```

`pd.read_sql_table()` 常用参数：

| 参数 | 含义 | 常用写法 | 使用场景 |
| --- | --- | --- | --- |
| `table_name` | 表名 | `"products"` | 读取整张小表 |
| `con` | 数据库连接对象 | `conn`、`engine` | 指定数据库连接 |
| `schema` | schema 名 | `"public"`、`"sales"` | PostgreSQL / Oracle 项目中常见 |
| `index_col` | 索引列 | `None` | 报表分析中通常不设置 |
| `columns` | 只读取指定列 | `["product_id", "category"]` | 小表字段很多但只用部分字段时 |

注意：

- 大表不建议用 `pd.read_sql_table()` 整表读取。
- 某些数据库连接方式不支持 `read_sql_table()`，这时用 `pd.read_sql_query()` 写 `SELECT` 更稳。

## 五、`chunksize`：分块读取大数据

如果 SQL 查询结果很大，一次性读入内存可能很慢，甚至导致内存不足。`chunksize` 可以让 pandas 每次读取一部分数据。

### 1. 基础功能示例

```python
# 作用：每次从数据库读取 10000 行
# 使用场景：查询结果较大，不适合一次性放入内存时
chunks = pd.read_sql(
    sql=sql,
    con=conn,
    params=params,
    chunksize=10000,
)

# 作用：逐块处理数据
# 使用场景：边读取边汇总，减少内存压力
for chunk in chunks:
    print(chunk.shape)  # 例如：(10000, 9) 表示当前块有 10000 行、9 列
```

### 2. 分块汇总示例

```python
# 作用：准备保存每个数据块的汇总结果
# 使用场景：大数据量时，先分块汇总，再合并汇总结果
summary_parts = []

# 作用：分块读取订单数据
# 使用场景：数据库返回结果较大时
for chunk in pd.read_sql(
    sql=sql,
    con=conn,
    params=params,
    parse_dates=["order_date"],
    chunksize=10000,
):
    # 作用：对当前数据块按地区汇总
    # 使用场景：每次只处理一部分数据，降低内存占用
    chunk_summary = (
        chunk.groupby("region", as_index=False)
        .agg(
            sales_amount=("amount", "sum"),
            gross_profit=("gross_profit", "sum"),
        )
    )

    summary_parts.append(chunk_summary)

# 作用：合并所有数据块的汇总结果后，再按地区二次汇总
# 使用场景：得到完整查询范围内的最终汇总结果
region_summary_db = (
    pd.concat(summary_parts, ignore_index=True)
    .groupby("region", as_index=False)
    .agg(
        sales_amount=("sales_amount", "sum"),
        gross_profit=("gross_profit", "sum"),
    )
)

print(region_summary_db.head())
# 例如：
#    region  sales_amount  gross_profit
# 0   Kanto     345678901      98765432
```

## 六、数据库结果分析

数据库负责保存和筛选数据，pandas 负责灵活分析。读取到 `DataFrame` 之后，后续方法和 CSV / Excel 分析是一样的。

```python
# 作用：按地区汇总数据库订单数据
# 使用场景：SQL 负责取数，pandas 负责生成分析结果
region_summary_db = (
    orders_db.groupby("region", as_index=False)
    .agg(
        sales_amount=("amount", "sum"),
        order_count=("order_id", "nunique"),
        gross_profit=("gross_profit", "sum"),
    )
    .sort_values("sales_amount", ascending=False, ignore_index=True)
)

# 作用：查看汇总结果
# 使用场景：写回数据库或导出报表前确认结果
print(region_summary_db.head(3))
# 例如：
#   region  sales_amount  order_count  gross_profit
# 0  Kanto     345678901         5021      98765432
```

## 七、`to_sql()`：把分析结果写回数据库

`to_sql()` 可以把 `DataFrame` 写入数据库表。这个方法很实用，但也有风险，尤其是 `if_exists="replace"` 会删除旧表后重建。

### 1. 基础功能示例

```python
# 作用：把地区汇总结果写入数据库表
# 使用场景：分析结果要给 BI 工具、后续 SQL、其他系统使用时
region_summary_db.to_sql(
    name="sales_region_summary_work",
    con=conn,
    if_exists="append",
    index=False,
)

# 作用：查看写入前的结果行列数
# 使用场景：确认本次准备写入多少行数据
print(region_summary_db.shape)  # 例如：(7, 4) 表示准备写入 7 行、4 列
```

### 2. 常用参数示例

```python
# 作用：把汇总结果分批写入数据库测试表
# 使用场景：结果数据量较大，或希望降低单次写入压力时
region_summary_db.to_sql(
    name="sales_region_summary_work",
    con=conn,
    schema="analysis",
    if_exists="append",
    index=False,
    chunksize=1000,
    method="multi",
)

# 作用：输出写入完成提示
# 使用场景：批处理脚本执行完成后留下日志
print("write finished")  # 例如：write finished
```

`to_sql()` 常用参数：

| 参数 | 含义 | 常用写法 | 使用场景 |
| --- | --- | --- | --- |
| `name` | 目标表名 | `"sales_region_summary_work"` | 指定写入哪张表 |
| `con` | 数据库连接对象 | `conn`、`engine` | 指定写入哪个数据库 |
| `schema` | schema 名 | `"analysis"` | 区分业务 schema、分析 schema |
| `if_exists` | 表已存在时如何处理 | `"append"`、`"fail"`、`"replace"` | 控制写入策略 |
| `index` | 是否写入 DataFrame 索引 | `False` | 报表结果通常不写索引 |
| `chunksize` | 每批写入行数 | `1000`、`5000` | 大批量写入时 |
| `dtype` | 指定数据库字段类型 | 由 SQLAlchemy 类型指定 | 需要控制字段长度、数值精度时 |
| `method` | 插入方式 | `"multi"` | 多行插入，提高部分数据库写入效率 |

`if_exists` 常见值：

| 值 | 含义 | 风险 | 使用建议 |
| --- | --- | --- | --- |
| `"fail"` | 表存在就报错 | 低 | 不希望误覆盖时使用 |
| `"append"` | 追加写入 | 中 | 写入前要确认不会重复 |
| `"replace"` | 删除旧表后重建 | 高 | 只在本地、测试表、临时表中谨慎使用 |

## 八、回写前的检查

正式项目中，不要只写 `to_sql()`。回写前至少检查行数、空值、重复键和目标表名。

```python
# 作用：检查汇总结果行数
# 使用场景：写回数据库前确认不是空表
print(region_summary_db.shape)  # 例如：(7, 4)

# 作用：检查关键字段是否为空
# 使用场景：避免把没有地区或没有金额的数据写入结果表
print(region_summary_db[["region", "sales_amount"]].isna().sum())
# 例如：
# region          0
# sales_amount    0

# 作用：检查地区是否重复
# 使用场景：目标表要求每个地区只有一行结果时
duplicate_count = region_summary_db.duplicated(subset=["region"]).sum()
print(duplicate_count)  # 例如：0 表示没有重复地区

# 作用：确认目标表名
# 使用场景：避免把测试结果写入正式表
target_table_name = "sales_region_summary_work"
print(target_table_name)  # 例如：sales_region_summary_work
```

日本项目中，数据库回写前通常要确认：

- 写入的是测试表、工作表还是正式表。
- 是否允许 `append`。
- 是否允许删除旧数据后重新写入。
- 是否需要先备份旧数据。
- 是否需要在事务中执行。
- 是否需要留下处理日期、处理人、批处理 ID。

## 九、Oracle / PostgreSQL / MySQL 项目中的注意点

本章不重复连接代码，只整理数据分析时最容易踩的点。

| 数据库 | 注意点 | 数据分析中的处理建议 |
| --- | --- | --- |
| Oracle | schema、日期类型、字段名大小写、NUMBER 精度 | SQL 中明确字段，日期条件不要靠字符串隐式转换 |
| PostgreSQL | schema 常见，日期和时间类型区分明确 | `schema` 参数要写清楚，时间字段读取后检查类型 |
| MySQL | 字符集、时区、数值精度容易影响结果 | 确认连接字符集，金额字段读取后检查类型 |

通用建议：

- 大表先用 SQL 限制字段和范围。
- 查询条件使用参数，不拼接用户输入。
- 写回正式库前先写测试表。
- 汇总结果写回前保存一份 CSV，方便排查。
- 数据库写入权限通常比读取权限更敏感，必须按项目规则申请和确认。

## 十、本章完整代码

下面代码演示从数据库读取订单数据，按地区汇总，然后写回数据库工作表。

```python
import pandas as pd


# 作用：定义查询 SQL
# 使用场景：只读取分析需要的字段和日期范围
sql = """
SELECT
    order_id,
    order_date,
    customer_id,
    product_id,
    region,
    channel,
    category,
    amount,
    gross_profit
FROM sales_orders
WHERE order_date >= :start_date
  AND order_date < :end_date
  AND order_status = :order_status
"""

# 作用：定义 SQL 查询参数
# 使用场景：避免直接拼接 SQL 字符串
params = {
    "start_date": "2025-01-01",
    "end_date": "2026-01-01",
    "order_status": "completed",
}

# 作用：从数据库读取订单数据
# 使用场景：数据库作为分析数据源时
orders_db = pd.read_sql(
    sql=sql,
    con=conn,
    params=params,
    parse_dates=["order_date"],
)

print(orders_db.shape)  # 例如：(56090, 9) 表示读取了 56090 行、9 列
print(orders_db.dtypes)  # 例如：确认 order_date 是否为 datetime64[ns]

# 作用：按地区汇总销售额、订单数、毛利
# 使用场景：生成地区销售分析结果
region_summary_db = (
    orders_db.groupby("region", as_index=False)
    .agg(
        sales_amount=("amount", "sum"),
        order_count=("order_id", "nunique"),
        gross_profit=("gross_profit", "sum"),
    )
    .sort_values("sales_amount", ascending=False, ignore_index=True)
)

print(region_summary_db.head(3))  # 例如：显示销售额最高的前 3 个地区

# 作用：检查写回数据是否为空
# 使用场景：避免把空结果写入数据库
if region_summary_db.empty:
    raise ValueError("region_summary_db is empty")

# 作用：检查关键字段空值
# 使用场景：避免关键字段为空的数据写入结果表
if region_summary_db[["region", "sales_amount"]].isna().any().any():
    raise ValueError("region_summary_db contains null key values")

# 作用：检查地区是否重复
# 使用场景：如果结果表要求每个地区一行，需要提前发现重复
if region_summary_db.duplicated(subset=["region"]).any():
    raise ValueError("region_summary_db contains duplicated region")

# 作用：指定写回的目标表名
# 使用场景：统一管理输出表，避免误写正式表
target_table_name = "sales_region_summary_work"

# 作用：把汇总结果写回数据库
# 使用场景：把分析结果交给 BI 工具、SQL 查询或后续系统使用
region_summary_db.to_sql(
    name=target_table_name,
    con=conn,
    schema="analysis",
    if_exists="append",
    index=False,
    chunksize=1000,
    method="multi",
)

print("write finished")  # 例如：write finished
```

## 十一、方法总结表

| 方法 | 作用 | 常用参数 | 返回结果 | 使用场景 |
| --- | --- | --- | --- | --- |
| `pd.read_sql()` | 读取 SQL 结果或表数据 | `sql`、`con`、`params`、`parse_dates`、`index_col`、`chunksize` | `DataFrame` 或分块迭代器 | 数据库直连分析最常用 |
| `pd.read_sql_query()` | 执行查询 SQL | `sql`、`con`、`params`、`parse_dates`、`chunksize` | `DataFrame` 或分块迭代器 | 明确执行 `SELECT` 查询 |
| `pd.read_sql_table()` | 读取整张表 | `table_name`、`con`、`schema`、`index_col`、`columns` | `DataFrame` | 读取客户、商品、地区等小型维表 |
| `to_sql()` | 写回数据库 | `name`、`con`、`schema`、`if_exists`、`index`、`chunksize`、`dtype`、`method` | 写入行数或 `None` | 保存分析结果给其他系统使用 |
| `pd.concat()` | 合并多个分块结果 | `objs`、`ignore_index` | `DataFrame` | 分块读取后合并中间汇总 |
| `DataFrame.empty` | 判断表是否为空 | 无 | `bool` | 写回数据库前检查是否有数据 |
| `duplicated()` | 检查重复 | `subset`、`keep` | 布尔序列 | 写回前检查业务键是否重复 |

## 十二、练习

1. 写一段 SQL，只读取 `2025-01-01` 到 `2025-12-31` 的订单字段，不要整表读取。
2. 使用 `pd.read_sql()` 加 `params` 读取订单数据。
3. 按 `region` 汇总销售额、订单数、毛利。
4. 使用 `chunksize=10000` 分块读取订单数据，并完成地区汇总。
5. 写回数据库前检查结果是否为空、关键字段是否为空、地区是否重复。
6. 把结果写入测试表 `sales_region_summary_work`，使用 `if_exists="append"`。
7. 说明为什么正式环境中不能随便使用 `if_exists="replace"`。

