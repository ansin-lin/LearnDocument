# CSV / Excel / JSON 文件自动化处理

这一章只讲自动化流程中的文件格式处理，不重复数据分析路线里已经讲过的 Pandas 清洗、分组、合并和可视化。

自动化脚本中常见的文件处理任务是：

- 读取 JSON 配置。
- 批量读取 CSV。
- 合并多个 CSV。
- CSV 转 Excel。
- Excel 多 Sheet 输出。
- 输出 JSON 执行摘要。
- 生成给其他系统或业务方使用的结果文件。

学完本章后，你要能完成一个小型文件转换流程：

```text
config.json
→ 读取 input 目录下多个 CSV
→ 合并数据
→ 输出 merged_orders.csv
→ 输出 automation_result.xlsx
→ 输出 summary.json
```

## 一、本章示例目录

本章统一使用下面的目录结构：

```text
automation_format_demo/
├── config.json
├── input/
│   ├── orders_20260726_01.csv
│   └── orders_20260726_02.csv
└── output/
```

`config.json` 示例：

```json
{
  "input_dir": "input",
  "output_dir": "output",
  "target_date": "2026-07-26",
  "encoding": "utf-8-sig"
}
```

订单 CSV 示例：

```csv
order_id,customer_id,amount
O001,C001,1000
O002,C002,2000
```

## 二、`json.load()`：读取 JSON 配置

自动化脚本不要把路径、日期、编码、收件人全部写死在代码里。常见做法是放到 JSON 配置文件。

### 1. 基础功能示例

```python
import json
from pathlib import Path

# 作用：定义配置文件路径
# 使用场景：自动化脚本启动时先读取配置
config_path = Path("config.json")

# 作用：读取 JSON 配置文件
# 使用场景：路径、日期、编码等参数不写死在代码里
with config_path.open("r", encoding="utf-8") as f:
    config = json.load(f)

# 作用：读取配置项
# 使用场景：后续代码根据配置决定输入目录和处理日期
print(config["input_dir"])  # 例如：input
print(config["target_date"])  # 例如：2026-07-26
```

### 2. 常用检查示例

```python
# 作用：检查配置中是否包含必要字段
# 使用场景：避免配置文件缺字段导致后续脚本报错难定位
required_keys = ["input_dir", "output_dir", "target_date", "encoding"]

for key in required_keys:
    if key not in config:
        raise KeyError(f"config key missing: {key}")

print("config ok")  # 例如：config ok
```

`json.load()` 相关写法：

| 写法 | 作用 | 使用场景 |
| --- | --- | --- |
| `config_path.open("r", encoding="utf-8")` | 用 UTF-8 打开配置文件 | 配置中有中文、日文时 |
| `json.load(f)` | 从文件读取 JSON | 读取配置文件 |
| `config["key"]` | 读取必填配置 | 缺少配置时希望立刻报错 |
| `config.get("key", default)` | 读取可选配置 | 没配置时使用默认值 |

## 三、`pd.read_csv()`：读取 CSV 文件

CSV 是系统导出和接口文件中很常见的格式。自动化场景中读取 CSV 的重点是编码、字段类型和文件来源。

### 1. 基础功能示例

```python
import pandas as pd

# 作用：定义输入 CSV 路径
# 使用场景：读取单个订单文件
input_file = Path(config["input_dir"]) / "orders_20260726_01.csv"

# 作用：读取 CSV 文件
# 使用场景：把系统导出的 CSV 读成 DataFrame
orders = pd.read_csv(input_file, encoding=config["encoding"])

print(orders.shape)  # 例如：(2, 3) 表示 2 行、3 列
print(orders.head(3))  # 例如：显示前 3 行订单数据
```

### 2. 常用参数示例

```python
# 作用：读取 CSV，并指定编码和编号字段类型
# 使用场景：订单号、客户号不能被自动转换成数字时
orders = pd.read_csv(
    filepath_or_buffer=input_file,
    encoding=config["encoding"],
    dtype={
        "order_id": "string",
        "customer_id": "string",
    },
    usecols=["order_id", "customer_id", "amount"],
)

print(orders.dtypes)
# 例如：
# order_id       string
# customer_id    string
# amount          int64
```

`pd.read_csv()` 在自动化中的常用参数：

| 参数 | 含义 | 常用写法 | 使用场景 |
| --- | --- | --- | --- |
| `filepath_or_buffer` | CSV 文件路径 | `input_file` | 指定读取哪个文件 |
| `encoding` | 文件编码 | `"utf-8-sig"`、`"cp932"` | 日本 Windows 导出文件可能需要 `cp932` |
| `dtype` | 指定字段类型 | `{"order_id": "string"}` | 编号字段保留前导零 |
| `usecols` | 只读取指定列 | `["order_id", "amount"]` | 接口文件字段很多但只处理部分字段 |
| `nrows` | 最多读取行数 | `100` | 调试时快速确认格式 |

## 四、`pd.concat()`：合并多个 CSV

自动化任务经常需要处理多个输入文件。例如一个日期下有多个部门文件，最后要合并成一份总文件。

```python
# 作用：根据处理日期生成文件名模式
# 使用场景：只读取指定日期的订单 CSV
target_date_text = config["target_date"].replace("-", "")
input_dir = Path(config["input_dir"])
csv_files = list(input_dir.glob(f"orders_{target_date_text}_*.csv"))

print(len(csv_files))  # 例如：2 表示找到 2 个目标 CSV

# 作用：逐个读取 CSV 并保存到列表
# 使用场景：批量合并文件前，先记录每个文件来源
df_list = []

for file_path in csv_files:
    df = pd.read_csv(
        file_path,
        encoding=config["encoding"],
        dtype={
            "order_id": "string",
            "customer_id": "string",
        },
    )

    # 作用：增加来源文件名
    # 使用场景：合并后还能追踪每行数据来自哪个文件
    df["source_file"] = file_path.name
    df_list.append(df)

if not df_list:
    raise FileNotFoundError("target CSV files not found")

# 作用：合并多个 DataFrame
# 使用场景：多个 CSV 变成一张总表
merged_orders = pd.concat(df_list, ignore_index=True)

print(merged_orders.shape)  # 例如：(3, 4) 表示合并后 3 行、4 列
```

`pd.concat()` 常用参数：

| 参数 | 含义 | 常用写法 | 使用场景 |
| --- | --- | --- | --- |
| `objs` | 要合并的 DataFrame 列表 | `df_list` | 合并多个 CSV |
| `ignore_index` | 是否重排行号 | `True` | 合并后生成连续行号 |
| `axis` | 合并方向 | `0` | 自动化里常见纵向追加行 |

## 五、`to_csv()`：输出 CSV

CSV 适合作为中间结果、系统接口文件或简单交付文件。

```python
# 作用：创建输出目录
# 使用场景：导出文件前先确保目录存在
output_dir = Path(config["output_dir"])
output_dir.mkdir(parents=True, exist_ok=True)

# 作用：导出合并后的订单 CSV
# 使用场景：把多个输入 CSV 合并成一个接口文件或中间文件
merged_csv = output_dir / "merged_orders.csv"
merged_orders.to_csv(
    merged_csv,
    index=False,
    encoding=config["encoding"],
)

print(merged_csv.exists())  # 例如：True 表示合并 CSV 已生成
```

`to_csv()` 常用参数：

| 参数 | 含义 | 常用写法 | 使用场景 |
| --- | --- | --- | --- |
| 第一个参数 | 输出路径 | `output_dir / "merged_orders.csv"` | 指定 CSV 保存位置 |
| `index` | 是否输出行索引 | `False` | 业务文件通常不需要 DataFrame 行号 |
| `encoding` | 文件编码 | `"utf-8-sig"`、`"cp932"` | Excel 打开或日文 Windows 环境交付 |
| `sep` | 分隔符 | `","` | CSV 默认逗号 |

## 六、`to_excel()`：CSV 转 Excel

业务人员经常更习惯查看 Excel。自动化脚本可以把 CSV 结果转换成 Excel。

```python
# 作用：把合并后的订单导出为 Excel
# 使用场景：业务方希望用 Excel 查看结果时
merged_excel = output_dir / "merged_orders.xlsx"

merged_orders.to_excel(
    merged_excel,
    sheet_name="orders",
    index=False,
)

print(merged_excel.exists())  # 例如：True 表示 Excel 已生成
```

`to_excel()` 常用参数：

| 参数 | 含义 | 常用写法 | 使用场景 |
| --- | --- | --- | --- |
| 第一个参数 | 输出路径或 writer | `merged_excel` | 只输出一个 Sheet |
| `sheet_name` | Sheet 名称 | `"orders"` | 指定工作表名称 |
| `index` | 是否输出行索引 | `False` | 报表通常不需要行号 |
| `columns` | 只输出指定列 | `["order_id", "amount"]` | 控制交付字段 |

## 七、`pd.read_excel()`：读取 Excel 文件

自动化脚本也经常需要读取业务方维护的 Excel 文件，例如客户清单、目标值、配置表。

```python
# 作用：读取 Excel 文件
# 使用场景：业务方维护的数据是 Excel 格式时
orders_from_excel = pd.read_excel(
    merged_excel,
    sheet_name="orders",
    dtype={
        "order_id": "string",
        "customer_id": "string",
    },
)

print(orders_from_excel.shape)  # 例如：(3, 4) 表示读取了 3 行、4 列
```

`pd.read_excel()` 在自动化中的常用参数：

| 参数 | 含义 | 常用写法 | 使用场景 |
| --- | --- | --- | --- |
| 第一个参数 | Excel 文件路径 | `merged_excel` | 指定读取哪个 Excel |
| `sheet_name` | Sheet 名称或序号 | `"orders"`、`0` | 读取指定工作表 |
| `dtype` | 指定字段类型 | `{"order_id": "string"}` | 编号字段不丢前导零 |
| `usecols` | 只读取指定列 | `"A:C"`、`["order_id"]` | 只读取必要字段 |
| `skiprows` | 跳过前几行 | `1`、`2` | Excel 前面有标题说明行时 |

## 八、`ExcelWriter`：输出多 Sheet Excel

一个自动化报表通常不只有明细，还会有摘要、处理结果、错误清单等多个 Sheet。

```python
# 作用：生成处理摘要 DataFrame
# 使用场景：Excel 报表中增加 summary Sheet
summary_df = pd.DataFrame(
    [
        {
            "target_date": config["target_date"],
            "file_count": len(csv_files),
            "row_count": len(merged_orders),
            "status": "success",
        }
    ]
)

# 作用：把明细和摘要写入同一个 Excel 文件
# 使用场景：自动化报表需要多个 Sheet
report_file = output_dir / "automation_result.xlsx"

with pd.ExcelWriter(report_file, engine="openpyxl") as writer:
    merged_orders.to_excel(writer, sheet_name="detail", index=False)
    summary_df.to_excel(writer, sheet_name="summary", index=False)

print(report_file.exists())  # 例如：True 表示多 Sheet Excel 已生成
```

`ExcelWriter` 常用参数：

| 参数 | 含义 | 常用写法 | 使用场景 |
| --- | --- | --- | --- |
| 第一个参数 | Excel 输出路径 | `report_file` | 指定工作簿保存位置 |
| `engine` | 写入引擎 | `"openpyxl"` | 写入 `.xlsx` 文件 |
| `mode` | 写入模式 | `"w"`、`"a"` | 新建或追加 Sheet |

## 九、`json.dump()`：输出 JSON 执行摘要

JSON 适合给其他系统读取，也适合记录自动化脚本的执行结果。

```python
# 作用：生成处理结果摘要
# 使用场景：把执行状态提供给其他系统或日志平台读取
summary = {
    "target_date": config["target_date"],
    "file_count": len(csv_files),
    "row_count": len(merged_orders),
    "status": "success",
}

# 作用：把结果摘要写入 JSON 文件
# 使用场景：接口联动或批处理结果记录
summary_path = output_dir / "summary.json"

with summary_path.open("w", encoding="utf-8") as f:
    json.dump(summary, f, ensure_ascii=False, indent=2)

print(summary_path.exists())  # 例如：True 表示 JSON 摘要已生成
```

`json.dump()` 常用参数：

| 参数 | 含义 | 常用写法 | 使用场景 |
| --- | --- | --- | --- |
| 第一个参数 | 要写出的对象 | `summary` | 字典、列表等 JSON 可表示的数据 |
| `ensure_ascii` | 是否把非 ASCII 字符转义 | `False` | 日文、中文正常显示 |
| `indent` | 缩进格式 | `2` | 方便人工查看 |

## 十、编码选择

日本项目中文件编码经常需要确认。

| 编码 | 使用场景 |
| --- | --- |
| `utf-8-sig` | 希望 Excel 直接打开 CSV 不乱码时 |
| `utf-8` | 系统之间传输、程序读取时 |
| `cp932` | 日本 Windows / Shift-JIS 系 CSV |

如果读取 CSV 时报 `UnicodeDecodeError`，优先确认文件编码，不要随便改数据内容。

## 十一、本章完整案例

下面代码会自动创建样例文件，然后完成：

```text
读取 config.json
→ 读取多个订单 CSV
→ 合并订单
→ 输出 merged_orders.csv
→ 输出 merged_orders.xlsx
→ 输出 automation_result.xlsx
→ 输出 summary.json
```

```python
from pathlib import Path
import json

import pandas as pd


base_dir = Path("automation_format_demo")
input_dir = base_dir / "input"
output_dir = base_dir / "output"

# 作用：创建输入和输出目录
# 使用场景：准备自动化文件格式处理练习环境
input_dir.mkdir(parents=True, exist_ok=True)
output_dir.mkdir(parents=True, exist_ok=True)

# 作用：创建 JSON 配置文件
# 使用场景：让路径、日期、编码从配置读取
config_path = base_dir / "config.json"
config_path.write_text(
    json.dumps(
        {
            "input_dir": str(input_dir),
            "output_dir": str(output_dir),
            "target_date": "2026-07-26",
            "encoding": "utf-8-sig",
        },
        ensure_ascii=False,
        indent=2,
    ),
    encoding="utf-8",
)

# 作用：创建两个样例订单 CSV
# 使用场景：没有真实输入文件时，用于本章练习
(input_dir / "orders_20260726_01.csv").write_text(
    "order_id,customer_id,amount\nO001,C001,1000\nO002,C002,2000\n",
    encoding="utf-8-sig",
)
(input_dir / "orders_20260726_02.csv").write_text(
    "order_id,customer_id,amount\nO003,C003,3000\n",
    encoding="utf-8-sig",
)

# 作用：读取 JSON 配置
# 使用场景：脚本启动后先获取输入目录、输出目录、处理日期和编码
with config_path.open("r", encoding="utf-8") as f:
    config = json.load(f)

required_keys = ["input_dir", "output_dir", "target_date", "encoding"]

for key in required_keys:
    if key not in config:
        raise KeyError(f"config key missing: {key}")

# 作用：按处理日期查找订单 CSV
# 使用场景：只处理指定日期的输入文件
target_date_text = config["target_date"].replace("-", "")
csv_files = list(Path(config["input_dir"]).glob(f"orders_{target_date_text}_*.csv"))

if not csv_files:
    raise FileNotFoundError("target CSV files not found")

df_list = []

for file_path in csv_files:
    # 作用：读取每个 CSV
    # 使用场景：批量读取多个输入文件
    df = pd.read_csv(
        file_path,
        encoding=config["encoding"],
        dtype={
            "order_id": "string",
            "customer_id": "string",
        },
    )

    # 作用：记录来源文件
    # 使用场景：合并后仍能追踪数据来源
    df["source_file"] = file_path.name
    df_list.append(df)

# 作用：合并多个 CSV
# 使用场景：把多个输入文件整理成一张总表
merged_orders = pd.concat(df_list, ignore_index=True)

# 作用：输出合并后的 CSV
# 使用场景：生成给其他系统继续处理的接口文件
merged_csv = Path(config["output_dir"]) / "merged_orders.csv"
merged_orders.to_csv(merged_csv, index=False, encoding=config["encoding"])

# 作用：输出单 Sheet Excel
# 使用场景：给业务方直接查看订单合并结果
merged_excel = Path(config["output_dir"]) / "merged_orders.xlsx"
merged_orders.to_excel(merged_excel, sheet_name="orders", index=False)

# 作用：生成处理摘要
# 使用场景：记录处理日期、文件数、行数和状态
summary = {
    "target_date": config["target_date"],
    "file_count": len(csv_files),
    "row_count": len(merged_orders),
    "status": "success",
}

summary_df = pd.DataFrame([summary])

# 作用：输出多 Sheet Excel
# 使用场景：正式自动化报表中同时包含明细和摘要
report_file = Path(config["output_dir"]) / "automation_result.xlsx"

with pd.ExcelWriter(report_file, engine="openpyxl") as writer:
    merged_orders.to_excel(writer, sheet_name="detail", index=False)
    summary_df.to_excel(writer, sheet_name="summary", index=False)

# 作用：输出 JSON 摘要
# 使用场景：给其他系统或日志平台读取处理结果
summary_path = Path(config["output_dir"]) / "summary.json"

with summary_path.open("w", encoding="utf-8") as f:
    json.dump(summary, f, ensure_ascii=False, indent=2)

print(merged_orders.shape)  # 例如：(3, 4) 表示合并后 3 行、4 列
print(merged_csv.exists())  # 例如：True 表示 merged_orders.csv 已生成
print(merged_excel.exists())  # 例如：True 表示 merged_orders.xlsx 已生成
print(report_file.exists())  # 例如：True 表示 automation_result.xlsx 已生成
print(summary_path.exists())  # 例如：True 表示 summary.json 已生成
```

运行后可以看到：

```text
automation_format_demo/
├── config.json
├── input/
│   ├── orders_20260726_01.csv
│   └── orders_20260726_02.csv
└── output/
    ├── merged_orders.csv
    ├── merged_orders.xlsx
    ├── automation_result.xlsx
    └── summary.json
```

## 十二、和数据分析路线的边界

本章重点是文件自动化：

- 读取配置。
- 批量读取文件。
- 文件格式转换。
- 输出结果文件。
- 生成执行摘要。

如果要学习缺失值、重复值、分组统计、表关联、可视化，请回到数据分析路线第 03～06 章。

## 十三、方法总结表

| 方法 | 作用 | 常用参数 | 使用场景 |
| --- | --- | --- | --- |
| `json.load()` | 读取 JSON 文件 | 文件对象 | 读取配置 |
| `json.dump()` | 写出 JSON 文件 | `ensure_ascii`、`indent` | 输出执行摘要 |
| `pd.read_csv()` | 读取 CSV | `encoding`、`dtype`、`usecols` | 读取接口文件、系统导出文件 |
| `pd.concat()` | 合并多个表 | `objs`、`ignore_index`、`axis` | 多个 CSV 合并 |
| `to_csv()` | 输出 CSV | `index`、`encoding`、`sep` | 输出接口文件、中间文件 |
| `to_excel()` | 输出 Excel | `sheet_name`、`index`、`columns` | 输出单 Sheet Excel |
| `pd.read_excel()` | 读取 Excel | `sheet_name`、`dtype`、`usecols`、`skiprows` | 读取业务维护文件 |
| `ExcelWriter` | 写多 Sheet Excel | `engine`、`mode` | 输出正式报表 |

## 十四、本章练习

1. 创建 `automation_format_demo` 目录。
2. 准备 `config.json`，配置输入目录、输出目录、处理日期和编码。
3. 在 `input` 目录下创建两个 `orders_20260726_*.csv`。
4. 使用 `json.load()` 读取配置。
5. 使用 `pd.read_csv()` 批量读取 CSV。
6. 使用 `pd.concat()` 合并多个 CSV，并增加 `source_file` 字段。
7. 导出 `merged_orders.csv`。
8. 导出 `merged_orders.xlsx`。
9. 使用 `ExcelWriter` 导出包含 `detail` 和 `summary` 两个 Sheet 的 Excel。
10. 使用 `json.dump()` 输出 `summary.json`。

