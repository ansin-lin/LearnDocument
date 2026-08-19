# Excel / CSV 读取与检查

这一章从实际代码顺序开始学：

1. 先把 CSV / Excel 文件读进来
2. 再查看数据规模、前几行、列名和字段类型
3. 最后检查缺失值、重复值和分类值分布

你要形成一个固定习惯：

> 新文件拿到手，先读取，再检查，不要一上来就做分析。

## 本章使用的数据

这一章使用 `data/` 目录下的 4 份样例数据：

- `orders.csv`
- `customers.xlsx`
- `products.csv`
- `monthly_targets.csv`

## 一、读取 CSV：`read_csv()`

### 基础功能

先看最简单的写法。

```python
from pathlib import Path
import pandas as pd

file_path = Path("data") / "orders.csv"

# 作用：读取 CSV 文件，并转换成 DataFrame
# 使用场景：订单明细、商品资料、目标数据通常会以 CSV 形式提供
orders = pd.read_csv(file_path)

print(orders.shape)  # 例如：(56090, 24) 表示 56090 行、24 列
print(orders.head(3))  # 例如：显示前 3 行订单明细
```

这段代码只解决一个问题：把 CSV 文件读进来。

如果 CSV 第一行就是表头，编码正常，列也都要读取，这种最小写法就够了。

### 完整参数示例

实际项目中的 CSV 经常更复杂。假设文件内容如下：

```text
报表名称：客户信息
导出日期：2026-07-24
customer_code,name,age,joined_date,amount,status,remark
00001,田中太郎,28,2024-04-01,"120,000",有效,
00002,佐藤花子,,2023-10-15,"98,500",有效,N/A
00003,鈴木一郎,35,无效日期,"150,000",无效,-
00004,高橋美咲,30,2025-01-10,"200,000",有效,未設定
```

这份文件有几个问题：

- 前两行是说明，不是数据
- 第三行才是表头
- `customer_code` 有前导零
- `age` 有空值
- `joined_date` 有非法日期
- `amount` 带千位逗号
- 日文 CSV 可能是 `cp932` 编码

```python
from pathlib import Path
import pandas as pd

file_path = Path("data") / "customer.csv"

# 作用：读取带说明行、日文编码、前导零编号、千位逗号金额的 CSV
# 使用场景：日本项目里常见的业务导出 CSV，不一定能直接用最小写法读取
df = pd.read_csv(
    filepath_or_buffer=file_path,
    sep=",",
    skiprows=2,
    header=0,
    usecols=[
        "customer_code",
        "name",
        "age",
        "joined_date",
        "amount",
        "status",
        "remark",
    ],
    dtype={
        "customer_code": "string",
        "name": "string",
        "age": "Int64",
        "status": "string",
        "remark": "string",
    },
    encoding="cp932",
    na_values=["", "N/A", "-", "未設定"],
    keep_default_na=True,
    thousands=",",
    skip_blank_lines=True,
    on_bad_lines="error",
    nrows=1000,
)

# 作用：把日期文本转换成日期类型，非法日期变成 NaT
# 使用场景：后面要按日期筛选、按月份统计时
df["joined_date"] = pd.to_datetime(df["joined_date"], errors="coerce")

print(df.head())  # 例如：显示前 4 行客户数据
print(df.dtypes)  # 例如：customer_code string / age Int64 / joined_date datetime64[ns]
```

常用参数说明：

| 参数 | 作用 | 常见场景 |
| --- | --- | --- |
| `filepath_or_buffer` | 文件路径 | 必须参数 |
| `sep` | 分隔符 | CSV 默认是逗号，TSV 可用 `sep="\t"` |
| `skiprows` | 跳过前几行 | 前面有报表标题、导出日期 |
| `header` | 指定哪一行是表头 | 表头不在第一行 |
| `usecols` | 只读取指定列 | 文件列很多，只取分析需要的列 |
| `dtype` | 指定列类型 | 编号有前导零时要用字符串 |
| `encoding` | 指定编码 | 日本 Windows CSV 常见 `cp932` |
| `na_values` | 自定义空值 | `N/A`、`-`、`未設定` 等 |
| `thousands` | 处理千位分隔符 | `"120,000"` 读成 `120000` |
| `on_bad_lines` | 异常行处理 | 字段数不一致时直接报错 |
| `nrows` | 只读前几行 | 先小范围试读 |

什么时候不用这些参数：

- 文件第一行就是表头时，不需要 `skiprows`
- 文件是 UTF-8 时，不要硬写 `encoding="cp932"`
- 正式全量处理时，通常去掉 `nrows`

## 二、读取 Excel：`read_excel()`

### 基础功能

```python
from pathlib import Path
import pandas as pd

file_path = Path("data") / "customers.xlsx"

# 作用：读取 Excel 文件，并转换成 DataFrame
# 使用场景：客户资料、人工维护报表经常来自 Excel
customers = pd.read_excel(file_path)

print(customers.shape)  # 例如：(24000, 19) 表示 24000 行、19 列
print(customers.head(3))  # 例如：显示前 3 行客户资料
```

如果 Excel 只有一个工作表，并且第一行就是表头，最小写法就可以先跑通。

### 完整参数示例

假设 Excel 长这样：

```text
A1: 月度客户汇总表
A2: 导出日期：2026-07-24
A3: 序号 | customer_code | customer_name | region | sales_amount | status
A4: 1    | 00001         | 田中太郎商事   | Kanto  | 120000       | 有效
A5: 2    | 00002         | 佐藤花子物流   | Kansai | 98500        | 有效
```

这份 Excel 有几个问题：

- 前两行是标题和导出日期
- 第三行才是表头
- 第一列只是序号
- 只需要读取业务字段

```python
from pathlib import Path
import pandas as pd

file_path = Path("data") / "customers.xlsx"

# 作用：读取带标题行、序号列、多工作表可能性的 Excel
# 使用场景：业务部门提供的 Excel 通常不是第一行就能直接分析
df = pd.read_excel(
    io=file_path,
    sheet_name=0,
    skiprows=2,
    header=0,
    usecols="B:F",
    dtype={
        "customer_code": "string",
        "customer_name": "string",
        "region": "string",
        "status": "string",
    },
    na_values=["", "N/A", "-", "未設定"],
    keep_default_na=True,
    nrows=500,
)

print(df.head())  # 例如：显示 customer_code、customer_name、region、sales_amount、status
print(df.dtypes)  # 例如：customer_code string / sales_amount int64 / status string
```

常用参数说明：

| 参数 | 作用 | 常见场景 |
| --- | --- | --- |
| `io` | Excel 文件路径 | 必须参数 |
| `sheet_name` | 指定工作表 | 一个文件有多个 sheet |
| `skiprows` | 跳过前几行 | 前面有标题、导出日期 |
| `header` | 指定表头行 | 表头不在第一行 |
| `usecols` | 只读取指定列 | 忽略序号列，只读业务字段 |
| `dtype` | 指定字段类型 | 编号需要保留前导零 |
| `na_values` | 自定义空值 | `N/A`、`-`、`未設定` |
| `index_col` | 指定索引列 | 第一列是编号并且要作为索引 |
| `nrows` | 只读前几行 | 先试读 |

什么时候不用这些参数：

- 只有一个 sheet 时，可以不写 `sheet_name`
- 第一行就是表头时，不需要 `skiprows`
- 第一列不是序号列时，不要随便 `usecols="B:F"`

## 三、查看数据规模：`shape`

```python
# 作用：查看每份数据的行数和列数
# 使用场景：读取完成后，先确认文件是否为空，数据规模是否符合预期
print("orders:", orders.shape)  # 例如：orders: (56090, 24)
print("customers:", customers.shape)  # 例如：customers: (24000, 19)
print("products:", products.shape)  # 例如：products: (24000, 16)
print("targets:", targets.shape)  # 例如：targets: (27648, 11)
```

注意：

- `shape` 是属性，不是方法，不能写成 `orders.shape()`。

## 四、查看前几行：`head()`

```python
# 作用：查看订单表前 3 行
# 使用场景：确认字段是否读对，日期、金额、分类字段看起来是否正常
print(orders.head(3))  # 例如：显示前 3 行订单明细

# 作用：查看客户表前 3 行
# 使用场景：确认 Excel 是否正确跳过标题行和说明行
print(customers.head(3))  # 例如：显示前 3 行客户资料
```

常用参数：

- `n`：查看前几行，默认是 5。

## 五、查看列名：`columns`

```python
# 作用：查看订单表全部列名
# 使用场景：写筛选、分组、合并前，先确认字段名有没有写错
print(orders.columns.tolist())  # 例如：['order_id', 'line_no', 'order_date', ...]
```

注意：

- `columns` 是属性，不是方法。
- `tolist()` 是为了把列名转换成普通列表，显示更清楚。

## 六、查看字段结构：`info()`

```python
# 作用：查看每列非空数量、字段类型和总行数
# 使用场景：判断哪些列有缺失值，哪些列后面需要做类型转换
orders.info()
# 例如：
# RangeIndex: 56090 entries, 0 to 56089
# Data columns (total 24 columns):
#  ... amount 55979 non-null float64
```

重点看：

- 非空数量是否明显少于总行数
- 日期列是不是还是 `object`
- 金额、数量列是不是数值类型

## 七、检查缺失值：`isna()`

### 基础功能

```python
# 作用：判断每个单元格是否为空
# 使用场景：刚读取完数据后，先检查哪些字段有缺失
missing_count = orders.isna().sum()

print(missing_count.head())  # 例如：显示前几列缺失值数量
```

### 常用组合写法

```python
# 作用：按缺失值数量从大到小排序，只看缺失最多的前 10 列
# 使用场景：快速找出最需要清洗的字段
missing_sorted = orders.isna().sum().sort_values(ascending=False)

print(missing_sorted.head(10))
# 例如：
# return_reason    54000
# region             162
# amount             111
```

说明：

- `isna()` 自己不常单独看结果，通常和 `sum()`、`sort_values()`、`head()` 连用。
- 看到空值不要马上填 0，先判断字段含义。

## 八、检查重复值：`duplicated()`

### 基础功能

```python
# 作用：检查客户编号是否重复
# 使用场景：客户表是主数据，customer_id 通常应该唯一
customer_dup = customers["customer_id"].duplicated().sum()
print("customer_id duplicated:", customer_dup)  # 例如：customer_id duplicated: 0
```

### 常用参数示例

```python
# 作用：检查订单号 + 行号是否重复
# 使用场景：订单表可能一单多行，不能只按 order_id 判断重复
order_line_dup = orders.duplicated(
    subset=["order_id", "line_no"],
    keep="first"
).sum()

print("order line duplicated:", order_line_dup)  # 例如：order line duplicated: 3
```

常用参数说明：

| 参数 | 作用 | 常见场景 |
| --- | --- | --- |
| `subset` | 指定按哪些列判断重复 | 订单表按 `order_id + line_no` |
| `keep` | 指定保留哪条重复记录 | `first`、`last`、`False` |

常见错误：

- 订单表只按 `order_id` 判断重复，会把正常的一单多行误判成重复。
- 主数据重复不检查，后面 `merge()` 可能放大行数。

## 九、查看分类分布：`value_counts()`

### 基础功能

```python
# 作用：统计每个渠道出现了多少次
# 使用场景：查看渠道值是否统一，顺便了解渠道分布
print(orders["channel"].value_counts())
# 例如：
# RetailStore    22010
# EC             18120
# Agency         15960
```

### 常用参数示例

```python
# 作用：统计退货标记，并且把空值也统计出来
# 使用场景：怀疑字段本身有缺失值时，不要忽略空值
print(orders["is_returned"].value_counts(dropna=False))
# 例如：
# N    54433
# Y     1657
```

```python
# 作用：查看每个渠道的占比，而不是原始数量
# 使用场景：想看结构比例时
print(orders["channel"].value_counts(normalize=True))
# 例如：
# RetailStore    0.3924
# EC             0.3230
# Agency         0.2846
```

常用参数说明：

| 参数 | 作用 | 常见场景 |
| --- | --- | --- |
| `dropna` | 是否忽略空值 | `dropna=False` 可以把空值也统计出来 |
| `normalize` | 是否返回比例 | 看渠道占比、状态占比 |

什么时候不用：

- 连续数值列通常不优先用 `value_counts()`。
- 唯一值特别多的列，用频次统计会很长，阅读价值不高。

## 十、按顺序完成第一轮检查

下面这段代码就是本章的完整使用顺序。

```python
from pathlib import Path
import pandas as pd

base_dir = Path("data")

# 作用：读取 4 份样例数据
# 使用场景：文件型数据分析开始前，先统一读入数据
orders = pd.read_csv(base_dir / "orders.csv")
customers = pd.read_excel(base_dir / "customers.xlsx")
products = pd.read_csv(base_dir / "products.csv")
targets = pd.read_csv(base_dir / "monthly_targets.csv")

# 作用：查看每份数据规模
# 使用场景：确认文件是否正常读取
print("orders:", orders.shape)  # 例如：orders: (56090, 24)
print("customers:", customers.shape)  # 例如：customers: (24000, 19)
print("products:", products.shape)  # 例如：products: (24000, 16)
print("targets:", targets.shape)  # 例如：targets: (27648, 11)

# 作用：查看前几行数据
# 使用场景：确认表头和值是否正常
print(orders.head(3))  # 例如：显示前 3 行订单明细
print(customers.head(3))  # 例如：显示前 3 行客户资料

# 作用：查看列名和字段结构
# 使用场景：确认字段名、字段类型和缺失情况
print(orders.columns.tolist())  # 例如：['order_id', 'line_no', 'order_date', ...]
orders.info()

# 作用：检查缺失值
# 使用场景：找出后续清洗重点字段
print(orders.isna().sum().sort_values(ascending=False).head(10))  # 例如：显示缺失值最多的前 10 列

# 作用：检查重复值
# 使用场景：避免后续关联和统计时数据被放大
print("customer_id duplicated:", customers["customer_id"].duplicated().sum())  # 例如：0
print("product_id duplicated:", products["product_id"].duplicated().sum())  # 例如：0
print("order line duplicated:", orders.duplicated(subset=["order_id", "line_no"]).sum())  # 例如：3

# 作用：查看分类字段分布
# 使用场景：检查渠道、状态、退货标记是否正常
print(orders["channel"].value_counts())  # 例如：显示渠道分布
print(orders["order_status"].value_counts())  # 例如：显示订单状态分布
print(orders["is_returned"].value_counts(dropna=False))  # 例如：显示退货标记分布
```

## 方法总结表

| 方法 / 属性 | 作用 | 常用参数 | 返回值 | 使用场景 |
| --- | --- | --- | --- | --- |
| `pd.read_csv()` | 读取 CSV | `filepath_or_buffer`、`skiprows`、`header`、`usecols`、`dtype`、`encoding` | `DataFrame` | 读取订单、商品、目标数据 |
| `pd.read_excel()` | 读取 Excel | `io`、`sheet_name`、`skiprows`、`header`、`usecols`、`dtype` | `DataFrame` | 读取客户资料和业务报表 |
| `shape` | 查看行列数 | 无 | `(行数, 列数)` | 确认数据规模 |
| `head()` | 查看前几行 | `n` | `DataFrame` | 快速检查数据长相 |
| `columns` | 查看列名 | 无 | 列索引对象 | 核对字段 |
| `info()` | 查看结构和类型 | `verbose`、`show_counts` | 控制台输出 | 检查类型和非空数量 |
| `isna()` | 判断缺失值 | 无 | 布尔结果 | 检查空值 |
| `duplicated()` | 判断重复值 | `subset`、`keep` | 布尔结果 | 检查主键或组合键重复 |
| `value_counts()` | 统计频次 | `dropna`、`normalize` | 频次结果 | 查看分类字段分布 |

## 练习

1. 读取 4 份样例数据。
2. 打印 4 份数据的 `shape`。
3. 查看 `orders` 和 `customers` 的前 3 行。
4. 打印 `orders` 的所有列名。
5. 查看 `orders.info()`。
6. 找出 `orders` 缺失值最多的前 5 列。
7. 检查客户、商品、订单明细是否有重复。
8. 查看 `channel`、`order_status`、`is_returned` 的分布。
