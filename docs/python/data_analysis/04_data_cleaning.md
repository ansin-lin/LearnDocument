# 第4章 数据清洗与字段转换

第一次检查完成后，接下来要做的是把数据整理到“可以放心统计”的状态。清洗不是机械删空值，而是把问题记录清楚、处理清楚、验证清楚。

## 4.1 先复制，再修改

建议不要直接在原始数据对象上反复覆盖，可以先复制一份：

```python
orders_clean = orders.copy()
```

这样做的目的是保留原始数据状态，方便对比清洗前后的差异。

## 4.2 统一列名和文本格式

如果列名里有空格、大小写混乱、中文括号和英文括号混用，后面处理会越来越乱。

可以先统一列名风格：

```python
orders_clean.columns = (
    orders_clean.columns.str.strip()
    .str.lower()
    .str.replace(" ", "_")
)
```

文本字段也常常需要去空格：

```python
orders_clean["region"] = orders_clean["region"].str.strip()
orders_clean["category"] = orders_clean["category"].str.strip()
```

## 4.3 处理缺失值

先看哪些列缺失值最多：

```python
print(orders_clean.isna().sum())
```

不同字段的处理方法不同：

- 关键字段缺失：如 `order_id`、`order_date`，通常不能直接参与分析
- 分类字段缺失：如 `region`，可以先标记为 `"未知"`
- 数值字段缺失：要先确认能不能补，不能盲目填 `0`

示例：

```python
orders_clean["region"] = orders_clean["region"].fillna("未知")
orders_clean = orders_clean.dropna(subset=["order_id", "order_date"])
```

## 4.4 处理重复值

如果 `order_id` 应该唯一，就要重点检查它：

```python
dup_count = orders_clean["order_id"].duplicated().sum()
print("重复订单数:", dup_count)
```

如果同一个 `order_id` 在订单明细表中允许重复，那就不能直接删。你必须先确认当前表的粒度。

只有在确认“这张表是一行一单”时，下面这种写法才安全：

```python
orders_clean = orders_clean.drop_duplicates(subset=["order_id"])
```

## 4.5 转换数据类型

```python
orders_clean["order_date"] = pd.to_datetime(
    orders_clean["order_date"],
    errors="coerce"
)
orders_clean["amount"] = pd.to_numeric(
    orders_clean["amount"],
    errors="coerce"
)
orders_clean["quantity"] = pd.to_numeric(
    orders_clean["quantity"],
    errors="coerce"
)
```

转换完成后要复查：

```python
print(orders_clean.dtypes)
print(orders_clean.isna().sum())
```

因为转换失败的值会变成缺失值。

## 4.6 处理异常值

先不要一看到异常就删除。先把异常找出来：

```python
abnormal_orders = orders_clean[
    (orders_clean["amount"] < 0) | (orders_clean["quantity"] <= 0)
]

print(abnormal_orders)
```

然后判断这些异常值属于哪一类：

- 真实业务：如退款、取消订单
- 录入错误：如数量写成 `9999`
- 测试数据：如客户名为 `test`

处理策略要和业务含义一致。

## 4.7 衍生字段

很多分析都需要先补出衍生字段：

```python
orders_clean["order_month"] = orders_clean["order_date"].dt.strftime("%Y-%m")
orders_clean["order_weekday"] = orders_clean["order_date"].dt.day_name()
```

这类字段后面会直接用于分组统计和图表。

## 4.8 清洗后的最小验证

每做完一次清洗，至少检查：

```python
print("清洗后行数:", len(orders_clean))
print("缺失值统计:")
print(orders_clean.isna().sum())
print("金额最小值:", orders_clean["amount"].min())
print("金额最大值:", orders_clean["amount"].max())
```

如果你删掉了很多行，一定要说明删的是哪一类数据、删了多少、为什么删。

## 4.9 本章练习

请把 `orders.csv` 清洗到满足下面条件：

1. 订单日期已经是日期类型。
2. 金额和数量已经是数值类型。
3. 关键字段缺失的数据已经处理。
4. 地区和品类字段写法已经统一。
5. 你能说清楚删除了哪些数据以及原因。
