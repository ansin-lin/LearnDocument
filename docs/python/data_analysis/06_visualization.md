# Excel / CSV 可视化与结果输出

这一章学习把分析结果展示出来，并保存成 CSV、Excel、图片文件。

前面章节已经完成了读取、清洗、分组汇总、表关联。本章重点是把结果变成可以交付、保存、发送或继续加工的文件。

本章按实际代码顺序学习：

1. 准备输出目录。
2. 生成用于画图和导出的汇总表。
3. 使用 Matplotlib 画柱状图和折线图。
4. 保存图表图片。
5. 导出 CSV。
6. 导出包含多个 Sheet 的 Excel 报表。

## 本章需要的库

本章会使用 `pandas`、`openpyxl` 和 `matplotlib`。

如果运行图表代码时报下面的错误：

```text
ModuleNotFoundError: No module named 'matplotlib'
```

先安装 Matplotlib：

```powershell
pip install matplotlib
```

如果要导出 Excel，也需要安装 `openpyxl`：

```powershell
pip install openpyxl
```

## 本章使用的数据

本章继续使用这 4 份样例数据：

- `orders.csv`：订单明细，核心事实数据。
- `customers.xlsx`：客户资料。
- `products.csv`：商品资料。
- `monthly_targets.csv`：月度目标。

本章假设前面已经得到以下结果表：

```python
orders_clean
region_summary
monthly_category_summary
target_compare
```

如果你是单独练习本章，可以先运行第 05 章最后的完整代码生成这些变量。

## 一、`Path.mkdir()`：准备输出目录

导出文件前，先准备一个固定输出目录。这样 CSV、Excel、图片不会散落在代码目录里。

### 1. 基础功能示例

```python
from pathlib import Path

# 作用：创建 output 文件夹
# 使用场景：准备导出 CSV、Excel、图表图片之前
output_dir = Path("output")
output_dir.mkdir(exist_ok=True)

# 作用：确认目录是否存在
# 使用场景：检查输出目录是否创建成功
print(output_dir.exists())  # 例如：True 表示 output 文件夹已存在
```

### 2. 常用参数示例

```python
from pathlib import Path

# 作用：创建多层输出目录，如果目录已存在也不报错
# 使用场景：企业脚本每天定时输出报表时，目录可能已经存在
output_dir = Path("output") / "sales_report"
output_dir.mkdir(
    parents=True,
    exist_ok=True,
)

# 作用：输出目录路径，方便确认文件会保存到哪里
# 使用场景：排查“文件导出了但找不到”的问题
print(output_dir)  # 例如：output\sales_report
```

`Path.mkdir()` 常用参数：

| 参数 | 含义 | 常用写法 | 使用场景 |
| --- | --- | --- | --- |
| `mode` | 目录权限 | 一般不写 | Windows 项目中通常不用特别设置 |
| `parents` | 父目录不存在时是否一起创建 | `True` | 创建多层目录时 |
| `exist_ok` | 目录已存在时是否忽略错误 | `True` | 自动化报表反复执行时 |

## 二、先准备图表数据

画图前不要直接把全部订单明细丢给图表方法。通常先用 `groupby()` 汇总出较小、含义明确的结果表。

```python
# 作用：按地区汇总销售额、订单数、毛利，并按销售额降序排序
# 使用场景：准备绘制地区销售额柱状图时
region_summary = (
    orders_clean.groupby("region", as_index=False)
    .agg(
        sales_amount=("amount", "sum"),
        order_count=("order_id", "nunique"),
        gross_profit=("gross_profit", "sum"),
    )
    .sort_values("sales_amount", ascending=False, ignore_index=True)
)

print(region_summary.head(3))
# 例如：
#    region  sales_amount  order_count  gross_profit
# 0   Kanto     345678901         5021      98765432

# 作用：按月份汇总销售额，并按月份升序排序
# 使用场景：准备绘制月度销售趋势折线图时
monthly_sales = (
    orders_clean.groupby("order_month", as_index=False)
    .agg(sales_amount=("amount", "sum"))
    .sort_values("order_month", ignore_index=True)
)

print(monthly_sales.head(3))
# 例如：
#   order_month  sales_amount
# 0     2025-01     123456789
# 1     2025-02     135000000
```

## 三、`plt.figure()`：创建画布

`plt.figure()` 用来创建图表画布。画布大小会影响图表是否拥挤、标题是否被遮挡、横轴文字是否能看清。

### 1. 基础功能示例

```python
import matplotlib.pyplot as plt

# 作用：创建默认大小的画布
# 使用场景：快速测试图表能否画出来
plt.figure()

# 作用：绘制简单柱状图
# 使用场景：确认数据和图表方法可以正常工作
plt.bar(region_summary["region"], region_summary["sales_amount"])

plt.show()
```

输出样式：

```text
显示一张柱状图：横轴是地区，纵轴是销售额
```

### 2. 常用参数示例

```python
# 作用：创建指定尺寸和清晰度的画布
# 使用场景：图表要保存成图片、插入 Excel 或发邮件时
plt.figure(
    figsize=(10, 5),
    dpi=120,
)

# 作用：绘制柱状图
# 使用场景：比较不同地区销售额大小
plt.bar(region_summary["region"], region_summary["sales_amount"])

plt.show()
```

`plt.figure()` 常用参数：

| 参数 | 含义 | 常用写法 | 使用场景 |
| --- | --- | --- | --- |
| `figsize` | 图表尺寸，单位是英寸 | `(10, 5)` | 横轴项目较多时加宽 |
| `dpi` | 图片清晰度 | `120`、`150` | 保存图片或插入报表时 |
| `facecolor` | 背景色 | `"white"` | 输出正式报表图片时 |

## 四、`plt.bar()`：绘制柱状图

柱状图适合比较不同分类之间的大小，例如地区销售额、品牌销售额、客户等级订单数。

### 1. 基础功能示例

```python
# 作用：绘制地区销售额柱状图
# 使用场景：比较各地区销售额高低时
plt.figure(figsize=(10, 5))
plt.bar(region_summary["region"], region_summary["sales_amount"])
plt.show()
```

### 2. 常用参数示例

```python
# 作用：绘制带颜色、宽度、标签的柱状图
# 使用场景：制作正式报表图表时
plt.figure(figsize=(10, 5), dpi=120)
plt.bar(
    x=region_summary["region"],
    height=region_summary["sales_amount"],
    width=0.6,
    color="#4C78A8",
    label="Sales Amount",
)

# 作用：设置图表标题
# 使用场景：让查看者知道图表主题
plt.title("Sales Amount by Region")

# 作用：设置横轴和纵轴标题
# 使用场景：说明横轴分类和纵轴指标含义
plt.xlabel("Region")
plt.ylabel("Sales Amount")

# 作用：旋转横轴文字
# 使用场景：横轴标签较长或重叠时
plt.xticks(rotation=30)

# 作用：显示图例
# 使用场景：图中有多个系列或需要说明颜色含义时
plt.legend()

# 作用：自动调整边距
# 使用场景：避免标题、坐标轴文字被截断
plt.tight_layout()

plt.show()
```

输出样式：

```text
显示一张柱状图：不同地区对应不同高度的柱子，柱子越高表示销售额越大
```

`plt.bar()` 常用参数：

| 参数 | 含义 | 常用写法 | 使用场景 |
| --- | --- | --- | --- |
| `x` | 横轴分类 | `region_summary["region"]` | 地区、品类、品牌、客户等级 |
| `height` | 柱子高度 | `region_summary["sales_amount"]` | 销售额、订单数、毛利 |
| `width` | 柱子宽度 | `0.6` | 控制柱子是否太粗或太细 |
| `color` | 柱子颜色 | `"#4C78A8"` | 正式报表统一配色 |
| `label` | 图例名称 | `"Sales Amount"` | 多个图形系列时 |

## 五、`plt.plot()`：绘制折线图

折线图适合查看趋势，例如月度销售额变化、每周订单数变化、达成率走势。

### 1. 基础功能示例

```python
# 作用：绘制月度销售趋势折线图
# 使用场景：查看销售额随月份变化的趋势时
plt.figure(figsize=(10, 5))
plt.plot(monthly_sales["order_month"], monthly_sales["sales_amount"])
plt.show()
```

### 2. 常用参数示例

```python
# 作用：绘制带标记、线型和颜色的折线图
# 使用场景：制作月度趋势报表时
plt.figure(figsize=(12, 5), dpi=120)
plt.plot(
    monthly_sales["order_month"],
    monthly_sales["sales_amount"],
    marker="o",
    linestyle="-",
    linewidth=2,
    color="#F58518",
    label="Monthly Sales",
)

plt.title("Monthly Sales Trend")
plt.xlabel("Month")
plt.ylabel("Sales Amount")
plt.xticks(rotation=45)
plt.legend()
plt.tight_layout()
plt.show()
```

输出样式：

```text
显示一张折线图：横轴是月份，纵轴是销售额，折线表示销售趋势
```

`plt.plot()` 常用参数：

| 参数 | 含义 | 常用写法 | 使用场景 |
| --- | --- | --- | --- |
| 第一个参数 | 横轴数据 | `monthly_sales["order_month"]` | 日期、月份、周 |
| 第二个参数 | 纵轴数据 | `monthly_sales["sales_amount"]` | 销售额、订单数、达成率 |
| `marker` | 数据点标记 | `"o"` | 强调每个月的数据点 |
| `linestyle` | 线条样式 | `"-"`、`"--"` | 区分实际值和目标值 |
| `linewidth` | 线条粗细 | `2` | 正式图表增强可读性 |
| `color` | 线条颜色 | `"#F58518"` | 统一报表配色 |
| `label` | 图例名称 | `"Monthly Sales"` | 多条线时说明含义 |

## 六、标题、坐标轴和布局方法

图表不是只画线和柱子，还要让别人看得懂。标题、坐标轴、刻度、布局是报表图表的基本要素。

```python
# 作用：创建画布并绘制折线图
# 使用场景：统一演示标题、坐标轴、刻度和布局设置
plt.figure(figsize=(12, 5))
plt.plot(monthly_sales["order_month"], monthly_sales["sales_amount"], marker="o")

# 作用：设置图表标题
# 使用场景：说明图表整体主题
plt.title("Monthly Sales Trend")

# 作用：设置横轴标题
# 使用场景：说明横轴代表月份
plt.xlabel("Month")

# 作用：设置纵轴标题
# 使用场景：说明纵轴代表销售额
plt.ylabel("Sales Amount")

# 作用：旋转横轴刻度文字
# 使用场景：月份文字重叠时
plt.xticks(rotation=45)

# 作用：添加网格线
# 使用场景：方便查看数值变化
plt.grid(True, axis="y", linestyle="--", alpha=0.4)

# 作用：自动调整图表边距
# 使用场景：保存图片前避免文字被截断
plt.tight_layout()

plt.show()
```

常用图表修饰方法：

| 方法 | 作用 | 常用参数 | 使用场景 |
| --- | --- | --- | --- |
| `plt.title()` | 设置标题 | `label`、`fontsize` | 说明图表主题 |
| `plt.xlabel()` | 设置横轴标题 | `xlabel` | 说明横轴字段 |
| `plt.ylabel()` | 设置纵轴标题 | `ylabel` | 说明纵轴指标 |
| `plt.xticks()` | 设置横轴刻度 | `rotation` | 标签重叠时旋转 |
| `plt.legend()` | 显示图例 | `loc` | 多条线、多组柱子时 |
| `plt.grid()` | 显示网格线 | `axis`、`linestyle`、`alpha` | 提高读数便利性 |
| `plt.tight_layout()` | 自动调整布局 | 一般不写参数 | 保存图片前常用 |

## 七、`plt.savefig()`：保存图表图片

在企业报表中，图表经常不是只在屏幕上显示，而是保存成图片，再插入 Excel、邮件或说明资料。

### 1. 基础功能示例

```python
# 作用：绘制地区销售额柱状图
# 使用场景：准备保存成图片文件
plt.figure(figsize=(10, 5))
plt.bar(region_summary["region"], region_summary["sales_amount"])
plt.tight_layout()

# 作用：保存图表图片
# 使用场景：把图表作为报表附件或插入 Excel 时
plt.savefig(output_dir / "region_sales.png")

# 作用：关闭当前图表
# 使用场景：脚本批量生成多张图时，避免图表互相影响
plt.close()

print((output_dir / "region_sales.png").exists())  # 例如：True 表示图片已生成
```

### 2. 常用参数示例

```python
# 作用：绘制并保存高清图表
# 使用场景：图片要插入正式 Excel 报表或发送给业务方时
plt.figure(figsize=(10, 5), dpi=120)
plt.bar(region_summary["region"], region_summary["sales_amount"], color="#4C78A8")
plt.title("Sales Amount by Region")
plt.xlabel("Region")
plt.ylabel("Sales Amount")
plt.xticks(rotation=30)
plt.tight_layout()

plt.savefig(
    output_dir / "region_sales.png",
    dpi=150,
    bbox_inches="tight",
    facecolor="white",
)
plt.close()

print((output_dir / "region_sales.png").exists())  # 例如：True 表示图片已生成
```

`plt.savefig()` 常用参数：

| 参数 | 含义 | 常用写法 | 使用场景 |
| --- | --- | --- | --- |
| 第一个参数 | 保存路径 | `output_dir / "region_sales.png"` | 指定图片文件位置 |
| `dpi` | 图片清晰度 | `150` | 正式报表图片 |
| `bbox_inches` | 是否裁剪多余边距 | `"tight"` | 避免文字被截断 |
| `facecolor` | 图片背景色 | `"white"` | 避免透明背景在邮件中显示异常 |

## 八、`to_csv()`：导出 CSV

CSV 适合作为中间文件、系统接口文件、简单交付文件。它结构简单，很多系统都能读取。

### 1. 基础功能示例

```python
# 作用：把地区汇总表导出成 CSV
# 使用场景：给其他系统继续处理，或作为中间结果留档
region_summary.to_csv(output_dir / "region_summary.csv")

# 作用：确认 CSV 文件是否生成
# 使用场景：检查导出是否成功
print((output_dir / "region_summary.csv").exists())  # 例如：True 表示 CSV 文件已生成
```

### 2. 常用参数示例

```python
# 作用：导出适合 Excel 打开的 CSV
# 使用场景：日本项目中业务人员可能直接用 Excel 打开 CSV 文件
region_summary.to_csv(
    path_or_buf=output_dir / "region_summary.csv",
    index=False,
    encoding="utf-8-sig",
    sep=",",
    date_format="%Y-%m-%d",
)

# 作用：确认文件是否生成
# 使用场景：脚本执行结束前做简单检查
print((output_dir / "region_summary.csv").exists())  # 例如：True 表示 CSV 文件已生成
```

`to_csv()` 常用参数：

| 参数 | 含义 | 常用写法 | 使用场景 |
| --- | --- | --- | --- |
| `path_or_buf` | 输出文件路径 | `output_dir / "region_summary.csv"` | 指定 CSV 保存位置 |
| `index` | 是否导出行索引 | `False` | 报表文件通常不需要 DataFrame 行号 |
| `encoding` | 文件编码 | `"utf-8-sig"`、`"cp932"` | Excel 打开或日本 Windows 环境交付 |
| `sep` | 分隔符 | `","`、`"\t"` | CSV 用逗号，TSV 用制表符 |
| `date_format` | 日期格式 | `"%Y-%m-%d"` | 控制日期列导出样式 |

编码选择建议：

| 编码 | 使用场景 |
| --- | --- |
| `utf-8-sig` | 希望 Excel 直接打开不乱码时常用 |
| `cp932` | 对方明确要求日本 Windows Shift-JIS 系 CSV 时使用 |
| `utf-8` | 系统之间传输、程序读取时常用 |

## 九、`ExcelWriter` 和 `to_excel()`：导出多 Sheet Excel

Excel 是日本项目里最常见的业务交付格式之一。多个分析结果通常放在一个工作簿的不同 Sheet 中。

### 1. 基础功能示例

```python
import pandas as pd

# 作用：把一个 DataFrame 导出成 Excel 文件
# 使用场景：只需要交付一张简单结果表时
region_summary.to_excel(
    output_dir / "region_summary.xlsx",
    sheet_name="region_summary",
    index=False,
)

# 作用：确认 Excel 文件是否生成
# 使用场景：检查导出是否成功
print((output_dir / "region_summary.xlsx").exists())  # 例如：True 表示 Excel 文件已生成
```

### 2. 常用参数示例：一个 Excel 多个 Sheet

```python
# 作用：创建 Excel 写入器
# 使用场景：一个工作簿里需要写入多个 Sheet 时
with pd.ExcelWriter(
    output_dir / "sales_analysis_report.xlsx",
    engine="openpyxl",
    mode="w",
) as writer:
    # 作用：写入地区汇总 Sheet
    # 使用场景：查看地区销售额、订单数、毛利
    region_summary.to_excel(
        excel_writer=writer,
        sheet_name="region_summary",
        index=False,
    )

    # 作用：写入月度品类汇总 Sheet
    # 使用场景：查看不同月份、不同品类的销售额变化
    monthly_category_summary.to_excel(
        excel_writer=writer,
        sheet_name="monthly_category",
        index=False,
    )

    # 作用：写入目标对比 Sheet
    # 使用场景：查看实际销售与目标之间的差距
    target_compare.to_excel(
        excel_writer=writer,
        sheet_name="target_compare",
        index=False,
    )

print((output_dir / "sales_analysis_report.xlsx").exists())  # 例如：True 表示 Excel 报表已生成
```

`ExcelWriter` 常用参数：

| 参数 | 含义 | 常用写法 | 使用场景 |
| --- | --- | --- | --- |
| 第一个参数 | Excel 输出路径 | `output_dir / "sales_analysis_report.xlsx"` | 指定工作簿保存位置 |
| `engine` | Excel 写入引擎 | `"openpyxl"` | 写入 `.xlsx` 文件 |
| `mode` | 写入模式 | `"w"`、`"a"` | 新建文件用 `w`，追加 Sheet 用 `a` |
| `if_sheet_exists` | Sheet 已存在时如何处理 | `"replace"` | 追加写入已有文件时使用 |

`to_excel()` 常用参数：

| 参数 | 含义 | 常用写法 | 使用场景 |
| --- | --- | --- | --- |
| `excel_writer` | Excel 写入器或路径 | `writer` | 多 Sheet 导出时传入 writer |
| `sheet_name` | Sheet 名称 | `"region_summary"` | 指定工作表名称 |
| `index` | 是否导出行索引 | `False` | 报表通常不需要 DataFrame 行号 |
| `columns` | 只导出指定列 | `["region", "sales_amount"]` | 只交付必要字段 |
| `startrow` | 从第几行开始写 | `1` | 上方要预留标题时 |
| `startcol` | 从第几列开始写 | `0` | 左侧要预留说明列时 |

注意：Excel 的 Sheet 名称不能超过 31 个字符，也不能包含 `[]:*?/\\` 这些特殊字符。

## 十、本章完整代码

下面代码从读取数据开始，完成汇总、画图、保存图片、导出 CSV、导出 Excel。

```python
from pathlib import Path

import matplotlib.pyplot as plt
import pandas as pd


data_dir = Path("data")
output_dir = Path("output") / "sales_report"

# 作用：创建输出目录
# 使用场景：导出报表、图片、CSV 前统一准备目录
output_dir.mkdir(parents=True, exist_ok=True)

# 作用：读取订单明细
# 使用场景：订单表是销售分析的核心数据
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

# 作用：读取月度目标
# 使用场景：后续生成目标对比 Sheet
targets = pd.read_csv(
    data_dir / "monthly_targets.csv",
    dtype={
        "target_month": "string",
        "store_code": "string",
    },
)

# 作用：复制数据，避免直接修改原始读取结果
# 使用场景：清洗和加工时保留原始数据
orders_clean = orders.copy()
targets_clean = targets.copy()

# 作用：生成月份字段
# 使用场景：月度趋势图和目标对比都需要月份
orders_clean["order_month"] = orders_clean["order_date"].dt.to_period("M").astype("string")

# 作用：按地区汇总销售指标
# 使用场景：生成地区销售额柱状图和地区汇总 CSV
region_summary = (
    orders_clean.groupby("region", as_index=False)
    .agg(
        sales_amount=("amount", "sum"),
        order_count=("order_id", "nunique"),
        gross_profit=("gross_profit", "sum"),
    )
    .sort_values("sales_amount", ascending=False, ignore_index=True)
)

# 作用：按月份汇总销售额
# 使用场景：生成月度销售趋势折线图
monthly_sales = (
    orders_clean.groupby("order_month", as_index=False)
    .agg(sales_amount=("amount", "sum"))
    .sort_values("order_month", ignore_index=True)
)

# 作用：按月份和品类汇总销售指标
# 使用场景：导出月度品类分析 Sheet
monthly_category_summary = (
    orders_clean.groupby(["order_month", "category"], as_index=False)
    .agg(
        sales_amount=("amount", "sum"),
        order_count=("order_id", "nunique"),
        gross_profit=("gross_profit", "sum"),
    )
)

# 作用：按目标表粒度汇总实际结果
# 使用场景：准备和月度目标表合并
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

# 作用：合并实际销售和目标数据
# 使用场景：生成目标达成率 Sheet
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
# 使用场景：判断实际销售额是否达到目标
target_compare["sales_achievement_rate"] = (
    target_compare["actual_sales_amount"] / target_compare["target_sales_amount"]
)

# 作用：绘制地区销售额柱状图
# 使用场景：保存为图片后插入报表或邮件
plt.figure(figsize=(10, 5), dpi=120)
plt.bar(region_summary["region"], region_summary["sales_amount"], color="#4C78A8")
plt.title("Sales Amount by Region")
plt.xlabel("Region")
plt.ylabel("Sales Amount")
plt.xticks(rotation=30)
plt.tight_layout()
plt.savefig(output_dir / "region_sales.png", dpi=150, bbox_inches="tight", facecolor="white")
plt.close()

# 作用：绘制月度销售趋势折线图
# 使用场景：保存为图片后展示销售趋势
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

# 作用：导出地区汇总 CSV
# 使用场景：给其他系统继续处理，或作为中间结果留档
region_summary.to_csv(
    output_dir / "region_summary.csv",
    index=False,
    encoding="utf-8-sig",
)

# 作用：导出多 Sheet Excel 报表
# 使用场景：正式交付分析结果时
with pd.ExcelWriter(output_dir / "sales_analysis_report.xlsx", engine="openpyxl") as writer:
    region_summary.to_excel(writer, sheet_name="region_summary", index=False)
    monthly_sales.to_excel(writer, sheet_name="monthly_sales", index=False)
    monthly_category_summary.to_excel(writer, sheet_name="monthly_category", index=False)
    target_compare.to_excel(writer, sheet_name="target_compare", index=False)

print((output_dir / "region_sales.png").exists())          # 例如：True 表示柱状图已生成
print((output_dir / "monthly_sales_trend.png").exists())   # 例如：True 表示折线图已生成
print((output_dir / "region_summary.csv").exists())        # 例如：True 表示 CSV 已生成
print((output_dir / "sales_analysis_report.xlsx").exists())  # 例如：True 表示 Excel 已生成
```

## 十一、方法总结表

| 方法 | 作用 | 常用参数 | 返回结果 | 使用场景 |
| --- | --- | --- | --- | --- |
| `Path.mkdir()` | 创建目录 | `parents`、`exist_ok` | 无 | 导出文件前准备输出目录 |
| `plt.figure()` | 创建画布 | `figsize`、`dpi`、`facecolor` | 图表对象 | 控制图表大小和清晰度 |
| `plt.bar()` | 绘制柱状图 | `x`、`height`、`width`、`color`、`label` | 图形对象 | 比较地区、品类、品牌等分类数据 |
| `plt.plot()` | 绘制折线图 | 横轴、纵轴、`marker`、`linestyle`、`linewidth`、`color`、`label` | 图形对象 | 查看月份、日期、周别趋势 |
| `plt.title()` | 设置标题 | `label`、`fontsize` | 文本对象 | 说明图表主题 |
| `plt.xlabel()` | 设置横轴标题 | `xlabel` | 文本对象 | 说明横轴字段 |
| `plt.ylabel()` | 设置纵轴标题 | `ylabel` | 文本对象 | 说明纵轴指标 |
| `plt.xticks()` | 设置横轴刻度 | `rotation` | 刻度对象 | 避免横轴文字重叠 |
| `plt.legend()` | 显示图例 | `loc` | 图例对象 | 多系列图表说明含义 |
| `plt.grid()` | 添加网格线 | `axis`、`linestyle`、`alpha` | 网格线 | 提高读数便利性 |
| `plt.tight_layout()` | 自动调整布局 | 一般不写参数 | 无 | 保存图片前避免文字被截断 |
| `plt.savefig()` | 保存图片 | 保存路径、`dpi`、`bbox_inches`、`facecolor` | 无 | 把图表保存为 PNG 文件 |
| `plt.close()` | 关闭图表 | 一般不写参数 | 无 | 批量生成图表时释放当前图表 |
| `to_csv()` | 导出 CSV | `path_or_buf`、`index`、`encoding`、`sep`、`date_format` | 无 | 导出中间结果或接口文件 |
| `ExcelWriter` | 创建 Excel 写入器 | 输出路径、`engine`、`mode`、`if_sheet_exists` | 写入器 | 一个 Excel 文件写入多个 Sheet |
| `to_excel()` | 导出 Excel | `excel_writer`、`sheet_name`、`index`、`columns`、`startrow`、`startcol` | 无 | 输出正式分析报表 |

## 十二、练习

1. 用 `region_summary` 画地区销售额柱状图，并保存为 `region_sales.png`。
2. 用 `monthly_sales` 画月度销售趋势折线图，并保存为 `monthly_sales_trend.png`。
3. 把 `region_summary` 导出为 `region_summary.csv`，要求 Excel 打开不乱码。
4. 把 `region_summary`、`monthly_sales`、`monthly_category_summary`、`target_compare` 写入同一个 Excel 文件的 4 个 Sheet。
5. 修改柱状图颜色和图表尺寸，让图表更适合插入 Excel 报表。
6. 追加一个“地区 × 品类”的销售额透视表，并写入 Excel 的新 Sheet。
