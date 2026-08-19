# 批量处理与命令行参数

自动化脚本通常不是只处理一个固定文件，而是按日期、目录、客户、系统批量处理。

为了让脚本可以补跑、定时执行、交接给别人使用，处理条件不能全部写死在代码里。本章学习用 `argparse` 接收命令行参数，并完成一个可运行的批量处理脚本。

学完本章后，你要能做到：

- 根据处理日期查找文件。
- 批量处理多个 CSV。
- 单个文件失败时记录失败原因。
- 生成处理结果清单。
- 支持 `--dry-run` 预览模式。
- 成功返回 `0`，失败返回非 `0`。

## 一、本章示例目录

本章统一使用下面的目录结构：

```text
automation_batch_demo/
├── input/
│   ├── orders_20260726_01.csv
│   ├── orders_20260726_02.csv
│   └── orders_20260725_01.csv
└── output/
```

运行脚本时只处理指定日期的文件。

命令示例：

```powershell
python batch_job.py --target-date 2026-07-26 --input-dir input --output-dir output
```

## 二、先理解批量处理

批量处理的核心是：同一套逻辑处理多个对象。

```python
from pathlib import Path

# 作用：定义输入目录
# 使用场景：批量处理目录下的多个文件
input_dir = Path("input")

# 作用：查找所有订单 CSV
# 使用场景：每天可能有多个订单文件需要处理
order_files = list(input_dir.glob("orders_*.csv"))

print(len(order_files))  # 例如：3 表示找到 3 个订单文件

for file_path in order_files:
    # 作用：逐个输出文件名
    # 使用场景：确认本次将处理哪些文件
    print(file_path.name)
```

批量处理脚本必须考虑：

| 问题 | 说明 |
| --- | --- |
| 处理范围 | 这次处理哪些日期、哪些文件 |
| 处理结果 | 每个文件成功还是失败 |
| 失败处理 | 一个文件失败是否影响其他文件 |
| 重复执行 | 重跑时是否覆盖结果 |
| 交接运行 | 别人能否通过参数运行 |

## 三、`argparse.ArgumentParser()`：创建参数解析器

`argparse` 是 Python 标准库，用来接收命令行参数。

### 1. 基础功能示例

```python
import argparse

# 作用：创建命令行参数解析器
# 使用场景：让脚本可以从命令行接收处理日期、输入目录、输出目录
parser = argparse.ArgumentParser()

# 作用：定义必填参数 target-date
# 使用场景：批处理必须知道要处理哪一天的数据
parser.add_argument("--target-date", required=True)

# 作用：定义输入目录参数
# 使用场景：不同环境的输入目录可能不同
parser.add_argument("--input-dir", default="input")

# 作用：定义输出目录参数
# 使用场景：不同环境的输出目录可能不同
parser.add_argument("--output-dir", default="output")

# 作用：解析命令行参数
# 使用场景：脚本启动时读取用户传入的值
args = parser.parse_args()

print(args.target_date)  # 例如：2026-07-26
print(args.input_dir)  # 例如：input
print(args.output_dir)  # 例如：output
```

执行示例：

```powershell
python batch_job.py --target-date 2026-07-26 --input-dir input --output-dir output
```

如果没有传 `--target-date`，脚本会提示参数错误。

## 四、`add_argument()` 常用参数

### 1. 必填参数和默认值

```python
# 作用：定义必填处理日期
# 使用场景：避免脚本不知道处理哪一天
parser.add_argument("--target-date", required=True)

# 作用：定义带默认值的编码参数
# 使用场景：大多数 CSV 使用 utf-8-sig，但需要允许项目调整
parser.add_argument("--encoding", default="utf-8-sig")
```

### 2. 参数类型

```python
# 作用：定义最大处理文件数
# 使用场景：测试时只处理前几个文件
parser.add_argument("--max-files", type=int, default=None)
```

执行示例：

```powershell
python batch_job.py --target-date 2026-07-26 --max-files 2
```

### 3. 开关参数

```python
# 作用：定义 dry-run 预览模式
# 使用场景：只查看将处理哪些文件，不真正输出结果
parser.add_argument("--dry-run", action="store_true")
```

执行示例：

```powershell
python batch_job.py --target-date 2026-07-26 --dry-run
```

`add_argument()` 常用参数：

| 参数 | 含义 | 常用写法 | 使用场景 |
| --- | --- | --- | --- |
| 参数名 | 命令行参数名称 | `"--target-date"` | 定义外部传入的参数 |
| `required` | 是否必填 | `True` | 处理日期等必须传入 |
| `default` | 默认值 | `"input"` | 输入目录、编码等 |
| `type` | 转换类型 | `int` | 数量、阈值、端口 |
| `action` | 参数动作 | `"store_true"` | 开关参数 |
| `help` | 参数说明 | `"target date"` | `--help` 时显示 |

## 五、根据处理日期查找文件

```python
# 作用：把日期转换成文件名中的 YYYYMMDD
# 使用场景：根据日期查找对应订单文件
target_date_text = args.target_date.replace("-", "")

# 作用：拼接文件名模式
# 使用场景：补跑某一天数据时，只处理指定日期文件
file_pattern = f"orders_{target_date_text}_*.csv"

# 作用：查找目标文件
# 使用场景：避免误处理其他日期文件
target_files = list(Path(args.input_dir).glob(file_pattern))

print(file_pattern)  # 例如：orders_20260726_*.csv
print(len(target_files))  # 例如：2 表示找到 2 个目标文件
```

如果要限制测试文件数量：

```python
# 作用：只保留前 N 个文件
# 使用场景：测试阶段先处理少量文件
if args.max_files is not None:
    target_files = target_files[: args.max_files]

print(len(target_files))  # 例如：1 表示本次只处理 1 个文件
```

## 六、定义单文件处理函数

批量处理时，不要把所有逻辑都写在 `for` 循环里。建议先写单个文件的处理函数。

```python
import pandas as pd


def process_order_file(file_path: Path, output_dir: Path, encoding: str) -> dict:
    # 作用：处理单个订单 CSV 文件
    # 使用场景：批量处理时，每个文件都调用同一个函数
    df = pd.read_csv(file_path, encoding=encoding)

    output_file = output_dir / f"{file_path.stem}_checked.csv"
    df.to_csv(output_file, index=False, encoding=encoding)

    return {
        "file_name": file_path.name,
        "row_count": len(df),
        "output_file": output_file.name,
        "status": "success",
        "message": "",
    }


result = process_order_file(
    Path("input/orders_20260726_01.csv"),
    Path("output"),
    "utf-8-sig",
)

print(result)
# 例如：{'file_name': 'orders_20260726_01.csv', 'row_count': 1200, 'output_file': 'orders_20260726_01_checked.csv', 'status': 'success', 'message': ''}
```

## 七、批量处理并记录失败

正式批处理中，一个文件失败时，通常要记录失败原因。是否继续处理其他文件，要看项目规格。本章示例采用“记录失败并继续处理下一个文件”。

```python
results = []

for file_path in target_files:
    try:
        # 作用：处理单个文件
        # 使用场景：每个文件独立处理，互不影响
        result = process_order_file(file_path, output_dir, args.encoding)

    except Exception as e:
        # 作用：记录失败文件和失败原因
        # 使用场景：一个文件坏了时，不让整个处理结果丢失
        result = {
            "file_name": file_path.name,
            "row_count": 0,
            "output_file": "",
            "status": "failed",
            "message": str(e),
        }

    results.append(result)

result_df = pd.DataFrame(results)

print(result_df)
# 例如：
#                    file_name  row_count                    output_file   status message
# 0  orders_20260726_01.csv       1200  orders_20260726_01_checked.csv  success
```

## 八、`--dry-run`：预览模式

预览模式不会真正处理文件，只显示将要处理哪些文件。正式项目中，涉及移动、覆盖、写数据库之前都建议先有预览或检查步骤。

```python
if args.dry_run:
    # 作用：只输出将处理的文件，不真正处理
    # 使用场景：上线前、补跑前确认处理范围
    for file_path in target_files:
        print(file_path.name)

    print("dry-run finished")  # 例如：dry-run finished
```

## 九、生成处理结果清单

```python
# 作用：把处理结果清单转换成 DataFrame
# 使用场景：批处理结束后输出给运维或业务方确认
result_df = pd.DataFrame(results)

# 作用：输出处理结果清单
# 使用场景：记录每个文件成功或失败、行数、输出文件名
summary_file = output_dir / "processed_files.csv"
result_df.to_csv(summary_file, index=False, encoding=args.encoding)

print(summary_file.exists())  # 例如：True 表示 processed_files.csv 已生成
```

## 十、`main()` 和返回码

自动化脚本最终会被调度器执行。调度器通常通过返回码判断成功或失败。

```python
import sys


def main() -> int:
    # 作用：脚本主入口
    # 使用场景：让调度器或命令行统一执行 main()
    return 0


if __name__ == "__main__":
    sys.exit(main())
```

常见返回码：

| 返回码 | 含义 | 使用场景 |
| --- | --- | --- |
| `0` | 成功 | 所有文件处理成功 |
| `1` | 处理失败 | 文件处理失败、运行异常 |
| `2` | 参数错误 | 命令行参数不正确 |

## 十一、本章完整案例

下面代码会自动创建样例文件，然后按命令行参数完成批量处理。

如果保存为 `batch_job.py`，执行方式如下：

```powershell
python batch_job.py --target-date 2026-07-26 --input-dir automation_batch_demo/input --output-dir automation_batch_demo/output
```

预览模式：

```powershell
python batch_job.py --target-date 2026-07-26 --input-dir automation_batch_demo/input --output-dir automation_batch_demo/output --dry-run
```

完整代码：

```python
from pathlib import Path
import argparse
import sys

import pandas as pd


def create_sample_files(input_dir: Path) -> None:
    # 作用：创建样例订单文件
    # 使用场景：本章练习没有真实输入文件时
    input_dir.mkdir(parents=True, exist_ok=True)

    (input_dir / "orders_20260726_01.csv").write_text(
        "order_id,amount\nO001,1000\nO002,2000\n",
        encoding="utf-8-sig",
    )
    (input_dir / "orders_20260726_02.csv").write_text(
        "order_id,amount\nO003,3000\n",
        encoding="utf-8-sig",
    )
    (input_dir / "orders_20260725_01.csv").write_text(
        "order_id,amount\nO999,9999\n",
        encoding="utf-8-sig",
    )


def parse_args() -> argparse.Namespace:
    # 作用：定义并解析命令行参数
    # 使用场景：让脚本支持处理日期、输入目录、输出目录、预览模式
    parser = argparse.ArgumentParser(description="Batch process order CSV files.")
    parser.add_argument("--target-date", required=True, help="target date, example: 2026-07-26")
    parser.add_argument("--input-dir", default="input", help="input directory")
    parser.add_argument("--output-dir", default="output", help="output directory")
    parser.add_argument("--encoding", default="utf-8-sig", help="CSV encoding")
    parser.add_argument("--max-files", type=int, default=None, help="max files for test run")
    parser.add_argument("--dry-run", action="store_true", help="preview target files only")
    return parser.parse_args()


def process_order_file(file_path: Path, output_dir: Path, encoding: str) -> dict:
    # 作用：处理单个订单 CSV
    # 使用场景：批量处理中每个文件复用同一处理逻辑
    df = pd.read_csv(file_path, encoding=encoding)

    output_file = output_dir / f"{file_path.stem}_checked.csv"
    df.to_csv(output_file, index=False, encoding=encoding)

    return {
        "file_name": file_path.name,
        "row_count": len(df),
        "output_file": output_file.name,
        "status": "success",
        "message": "",
    }


def main() -> int:
    args = parse_args()

    input_dir = Path(args.input_dir)
    output_dir = Path(args.output_dir)
    output_dir.mkdir(parents=True, exist_ok=True)

    # 作用：创建样例文件
    # 使用场景：教学练习中保证脚本可以直接运行
    create_sample_files(input_dir)

    target_date_text = args.target_date.replace("-", "")
    file_pattern = f"orders_{target_date_text}_*.csv"
    target_files = list(input_dir.glob(file_pattern))

    if args.max_files is not None:
        target_files = target_files[: args.max_files]

    if not target_files:
        print(f"target files not found: {file_pattern}")
        return 1

    if args.dry_run:
        for file_path in target_files:
            print(file_path.name)

        print("dry-run finished")
        return 0

    results = []

    for file_path in target_files:
        try:
            result = process_order_file(file_path, output_dir, args.encoding)

        except Exception as e:
            result = {
                "file_name": file_path.name,
                "row_count": 0,
                "output_file": "",
                "status": "failed",
                "message": str(e),
            }

        results.append(result)

    result_df = pd.DataFrame(results)
    summary_file = output_dir / "processed_files.csv"
    result_df.to_csv(summary_file, index=False, encoding=args.encoding)

    failed_count = (result_df["status"] == "failed").sum()

    print(result_df.shape)  # 例如：(2, 5) 表示处理了 2 个文件、输出 5 个字段
    print(summary_file.exists())  # 例如：True 表示 processed_files.csv 已生成

    return 0 if failed_count == 0 else 1


if __name__ == "__main__":
    sys.exit(main())
```

运行成功后可以看到：

```text
automation_batch_demo/
├── input/
│   ├── orders_20260726_01.csv
│   ├── orders_20260726_02.csv
│   └── orders_20260725_01.csv
└── output/
    ├── orders_20260726_01_checked.csv
    ├── orders_20260726_02_checked.csv
    └── processed_files.csv
```

`processed_files.csv` 示例：

```text
file_name,row_count,output_file,status,message
orders_20260726_01.csv,2,orders_20260726_01_checked.csv,success,
orders_20260726_02.csv,1,orders_20260726_02_checked.csv,success,
```

## 十二、方法总结表

| 方法 / 写法 | 作用 | 常用参数 | 使用场景 |
| --- | --- | --- | --- |
| `argparse.ArgumentParser()` | 创建参数解析器 | `description` | 命令行脚本入口 |
| `add_argument()` | 定义参数 | `required`、`default`、`type`、`action`、`help` | 定义处理日期、目录、开关 |
| `parse_args()` | 解析参数 | 无 | 获取命令行传入值 |
| `Path.glob()` | 查找文件 | `"orders_*.csv"` | 按日期批量查找文件 |
| `pd.read_csv()` | 读取 CSV | `encoding` | 单文件处理 |
| `to_csv()` | 输出 CSV | `index`、`encoding` | 输出检查文件和处理结果清单 |
| `try except` | 捕获异常 | `Exception as e` | 单个文件失败时记录原因 |
| `sys.exit()` | 返回退出码 | `0`、`1`、`2` | 给调度器判断成功失败 |

## 十三、本章练习

1. 写一个 `batch_job.py`，接收 `--target-date`、`--input-dir`、`--output-dir`。
2. 增加 `--encoding`，默认值为 `utf-8-sig`。
3. 增加 `--dry-run`，只显示将处理的文件，不输出结果。
4. 增加 `--max-files`，测试时只处理指定数量文件。
5. 根据日期查找 `orders_YYYYMMDD_*.csv`。
6. 批量读取并输出 `*_checked.csv`。
7. 生成 `processed_files.csv`，记录文件名、行数、状态、错误信息。
8. 如果有文件失败，脚本返回 `1`。
9. 分别运行正式模式和 dry-run 模式，比较输出差异。

