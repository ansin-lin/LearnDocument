# 第3章 用 Pandas 读取数据并做第一次检查

从这一章开始正式写代码。目标不是马上出图，而是把数据稳定读进来，并知道数据现在是什么状态。

## 3.1 环境准备

本路线默认你已经学过 Python 基础。如果还没有，请先看 [Python 基础总览](../common/00_intro.md)。

建议先准备一个独立环境，并安装本路线要用到的库：

```bash
pip install pandas matplotlib
```

- `pandas`：读取和处理表格数据
- `matplotlib`：后面做可视化要用

如果你要读取 `.xlsx` 文件，还需要安装 `openpyxl`：

```bash
pip install openpyxl
```

这里只把它当作 Excel 文件读取依赖，不展开讲 Excel 模板生成。Excel 自动化输出在 [Python 自动化路线](../automation/index.md) 中单独讲。

## 3.2 读取 CSV 和 Excel

```python
import pandas as pd

orders = pd.read_csv("data/orders.csv")
customers = pd.read_excel("data/customers.xlsx")
products = pd.read_csv("data/products.csv")
targets = pd.read_csv("data/monthly_targets.csv")
```

第一次读取后，不要立刻清洗，先确认有没有读错。

## 3.3 先看前几行

```python
print(orders.head())
print(customers.head())
```

你要重点确认：

- 列名是不是你预期的名字
- 数据有没有错位
- 金额列是不是读成了文本
- 日期列是不是还是普通字符串

## 3.4 看结构信息

`info()` 是最重要的初检方法之一：

```python
orders.info()
```

它会告诉你：

- 总行数
- 每列非空数量
- 每列数据类型
- 内存占用情况

如果你看到：

- `amount` 是 `object`
- `order_date` 是 `object`

那通常说明还需要后续转换。

## 3.5 看基础统计信息

```python
print(orders.describe())
print(orders.describe(include="object"))
```

这里主要看：

- 数值列的平均值、最大值、最小值
- 文本列有多少个不同值
- 是否出现明显离谱的数据

例如金额最小值如果是 `-50000`，就要马上追查。

## 3.6 检查缺失值和重复值

```python
print(orders.isna().sum())
print(orders.duplicated().sum())
print(orders["order_id"].duplicated().sum())
```

这三种检查含义不同：

- `orders.isna().sum()`：每列缺失多少
- `orders.duplicated().sum()`：整行完全重复多少
- `orders["order_id"].duplicated().sum()`：业务主键是否重复

实际工作里，第三种往往比“整行重复”更重要。

## 3.7 检查唯一值和类别分布

```python
print(orders["region"].value_counts())
print(orders["category"].value_counts())
print(customers["customer_level"].unique())
```

你能借此发现：

- 地区名称是否写法不统一
- 品类是否有脏值
- 客户等级是否混入了空格或大小写问题

## 3.8 首次转换常见写法

```python
orders["order_date"] = pd.to_datetime(orders["order_date"], errors="coerce")
orders["amount"] = pd.to_numeric(orders["amount"], errors="coerce")
```

- `errors="coerce"` 表示遇到无法转换的值时，先转成缺失值
- 这比直接报错更适合数据检查阶段

转换后要再次确认：

```python
orders.info()
```

## 3.9 建议保留一份“初检记录”

哪怕只是一个简单文本，也建议记下：

- 原始行数
- 每列缺失数量
- 主键重复数量
- 转换失败的字段
- 你怀疑有问题的列

这会直接影响后面的清洗决策。

## 3.10 本章最小示例

```python
import pandas as pd

orders = pd.read_csv("data/orders.csv")

print("原始行数:", len(orders))
print("列名:", list(orders.columns))
print("缺失值统计:")
print(orders.isna().sum())

orders["order_date"] = pd.to_datetime(orders["order_date"], errors="coerce")
orders["amount"] = pd.to_numeric(orders["amount"], errors="coerce")

print("转换后的类型:")
print(orders.dtypes)
```

完成这一章后，你应该能独立回答两件事：

1. 数据有没有成功读进来。
2. 当前最需要处理的质量问题是什么。
