# 文件、路径与目录处理

自动化脚本最先遇到的问题通常不是算法，而是文件在哪里、目录是否存在、文件是否到齐、处理完成后放到哪里。

本章学习用 `pathlib`、`shutil`、`zipfile` 完成文件和目录处理。

学完本章后，你要能完成：

- 创建自动化脚本需要的目录。
- 检查文件是否存在、是否为空。
- 批量查找指定类型文件。
- 复制、移动、归档文件。
- 压缩输出结果。
- 避免危险的误删除和误覆盖。

## 一、本章示例目录

本章统一使用下面的目录结构：

```text
automation_file_demo/
├── input/
│   ├── orders_20260726_01.csv
│   └── orders_20260726_02.csv
├── output/
├── backup/
└── processed/
```

你可以先创建一个练习目录，然后在里面运行本章代码。

## 二、`Path`：表示文件和目录路径

`Path` 是 `pathlib` 提供的路径对象。自动化脚本建议优先使用 `Path`，不要手动拼接路径字符串。

### 1. 基础功能示例

```python
from pathlib import Path

# 作用：定义项目根目录
# 使用场景：自动化脚本用一个根目录统一管理 input、output、backup
base_dir = Path("automation_file_demo")

# 作用：定义输入目录
# 使用场景：自动化脚本统一从 input 目录读取文件
input_dir = base_dir / "input"

# 作用：拼接订单文件路径
# 使用场景：根据目录和文件名得到完整路径
orders_file = input_dir / "orders_20260726_01.csv"

print(orders_file)  # 例如：automation_file_demo\input\orders_20260726_01.csv
```

### 2. 常用属性示例

```python
# 作用：查看文件名
# 使用场景：生成处理清单或日志时记录文件名
print(orders_file.name)  # 例如：orders_20260726_01.csv

# 作用：查看文件扩展名
# 使用场景：判断文件类型
print(orders_file.suffix)  # 例如：.csv

# 作用：查看不带扩展名的文件名
# 使用场景：根据输入文件名生成输出文件名
print(orders_file.stem)  # 例如：orders_20260726_01

# 作用：查看父目录
# 使用场景：确认文件属于哪个输入目录
print(orders_file.parent)  # 例如：automation_file_demo\input
```

`Path` 常用写法：

| 写法 | 作用 | 使用场景 |
| --- | --- | --- |
| `Path("input")` | 创建路径对象 | 表示目录或文件路径 |
| `path / "file.csv"` | 拼接路径 | 跨 Windows / Linux 更安全 |
| `path.name` | 文件名 | 生成处理清单 |
| `path.suffix` | 扩展名 | 判断 CSV、Excel、日志 |
| `path.stem` | 不带扩展名的文件名 | 生成输出文件名 |
| `path.parent` | 父目录 | 检查文件所在目录 |

## 三、`mkdir()`：创建目录

自动化脚本开始前，通常要先创建输入、输出、备份、处理完成目录。

### 1. 基础功能示例

```python
# 作用：创建项目根目录
# 使用场景：第一次准备自动化练习目录时
base_dir.mkdir(exist_ok=True)

print(base_dir.exists())  # 例如：True 表示目录已存在
```

### 2. 常用参数示例

```python
# 作用：创建多层目录，如果目录已存在也不报错
# 使用场景：自动化脚本重复运行时，输出目录可能已经存在
output_dir = base_dir / "output" / "sales_report"
output_dir.mkdir(
    parents=True,
    exist_ok=True,
)

print(output_dir.exists())  # 例如：True 表示输出目录已创建
```

`mkdir()` 常用参数：

| 参数 | 含义 | 常用写法 | 使用场景 |
| --- | --- | --- | --- |
| `parents` | 父目录不存在时是否一起创建 | `True` | 创建多层目录 |
| `exist_ok` | 目录已存在时是否忽略错误 | `True` | 脚本重复执行 |

## 四、`exists()`、`is_file()`、`is_dir()`：检查路径状态

自动化脚本不能默认文件一定存在。处理前要先检查路径状态。

```python
# 作用：检查路径是否存在
# 使用场景：判断输入文件是否到达
print(orders_file.exists())  # 例如：True 表示路径存在

# 作用：检查是否是文件
# 使用场景：避免把目录当成文件处理
print(orders_file.is_file())  # 例如：True 表示这是文件

# 作用：检查是否是目录
# 使用场景：确认 input 目录是否正确
print(input_dir.is_dir())  # 例如：True 表示这是目录
```

常见判断：

| 方法 | 作用 | 使用场景 |
| --- | --- | --- |
| `exists()` | 路径是否存在 | 文件到达检查 |
| `is_file()` | 是否是文件 | 读取文件前 |
| `is_dir()` | 是否是目录 | 遍历目录前 |

## 五、`stat()`：检查文件大小

文件存在不代表可以处理。大小为 0 的文件通常是异常文件。

```python
# 作用：获取文件大小
# 使用场景：处理前确认文件不是空文件
file_size = orders_file.stat().st_size

print(file_size)  # 例如：2048 表示文件大小是 2048 字节

if file_size == 0:
    # 作用：空文件主动报错
    # 使用场景：避免继续处理无效文件
    raise ValueError(f"empty file: {orders_file}")
```

## 六、`glob()`：按规则批量查找文件

`glob()` 用来按文件名模式查找文件，是自动化批量处理的核心方法。

### 1. 基础功能示例

```python
# 作用：查找 input 目录下所有 CSV 文件
# 使用场景：批量处理当天到达的多个文件
csv_files = list(input_dir.glob("*.csv"))

print(len(csv_files))  # 例如：2 表示找到 2 个 CSV 文件

for file_path in csv_files:
    # 作用：逐个输出文件名
    # 使用场景：确认本次将处理哪些文件
    print(file_path.name)
```

### 2. 按日期查找文件

```python
# 作用：根据处理日期生成文件名模式
# 使用场景：只处理指定日期的订单文件
target_date = "2026-07-26"
target_date_text = target_date.replace("-", "")
file_pattern = f"orders_{target_date_text}_*.csv"

target_files = list(input_dir.glob(file_pattern))

print(file_pattern)  # 例如：orders_20260726_*.csv
print(len(target_files))  # 例如：2 表示找到 2 个目标文件
```

常见模式：

| 写法 | 作用 |
| --- | --- |
| `"*.csv"` | 当前目录下所有 CSV |
| `"*.xlsx"` | 当前目录下所有 Excel |
| `"**/*.log"` | 当前目录和子目录下所有日志 |
| `"orders_*.csv"` | 所有订单文件 |
| `"orders_20260726_*.csv"` | 指定日期订单文件 |

## 七、`iterdir()`：遍历目录内容

`iterdir()` 会列出目录下的直接子项，不会按模式筛选。

```python
# 作用：遍历 input 目录下的所有内容
# 使用场景：想查看目录里到底有哪些文件和文件夹
for path in input_dir.iterdir():
    print(path.name)  # 例如：orders_20260726_01.csv
```

`glob()` 和 `iterdir()` 的区别：

| 方法 | 特点 | 使用场景 |
| --- | --- | --- |
| `glob("*.csv")` | 按模式查找 | 批量处理指定类型文件 |
| `iterdir()` | 列出当前目录全部内容 | 检查目录内容 |

## 八、`read_text()`、`write_text()`：读取和写入小文本文件

配置、执行结果、简单日志可以用文本方式读写。

```python
# 作用：定义结果文件路径
# 使用场景：保存本次文件检查结果
result_file = base_dir / "output" / "file_check_result.txt"
result_file.parent.mkdir(parents=True, exist_ok=True)

# 作用：写入文本结果
# 使用场景：输出简单处理结果
result_file.write_text("status=success\nfile_count=2\n", encoding="utf-8")

# 作用：读取文本结果
# 使用场景：确认文件内容是否写入成功
result_text = result_file.read_text(encoding="utf-8")

print(result_text)
# 例如：
# status=success
# file_count=2
```

注意：`read_text()` 适合小文件。大日志文件要逐行读取。

## 九、`shutil.copy()`：复制文件

复制文件常用于处理前备份。复制后原文件仍然保留，风险相对较低。

```python
import shutil

# 作用：创建备份目录
# 使用场景：处理输入文件前先保留原始文件
backup_dir = base_dir / "backup"
backup_dir.mkdir(exist_ok=True)

# 作用：复制订单文件到备份目录
# 使用场景：保留本次处理使用的原始文件
backup_file = backup_dir / orders_file.name
shutil.copy(orders_file, backup_file)

print(backup_file.exists())  # 例如：True 表示备份文件已生成
```

## 十、`shutil.move()`：移动文件

移动文件常用于把处理完成的输入文件移到 `processed` 目录，避免下次重复处理。

```python
# 作用：创建 processed 目录
# 使用场景：保存处理完成的输入文件
processed_dir = base_dir / "processed"
processed_dir.mkdir(exist_ok=True)

# 作用：移动订单文件到 processed 目录
# 使用场景：避免下次批处理重复读取同一个文件
processed_file = processed_dir / orders_file.name
shutil.move(orders_file, processed_file)

print(processed_file.exists())  # 例如：True 表示文件已移动
print(orders_file.exists())  # 例如：False 表示原位置文件已不存在
```

移动会改变原文件位置。正式项目中常见顺序是：

```text
先复制到 backup → 处理成功 → 移动到 processed
```

## 十一、`unlink()`：删除文件的安全边界

删除是有风险的操作。新人阶段不建议在自动化脚本中直接批量删除正式文件。

如果确实要删除临时文件，必须限定范围。

```python
# 作用：定义临时文件路径
# 使用场景：只删除脚本自己生成的临时文件
temp_file = base_dir / "output" / "temp_result.txt"
temp_file.write_text("temporary data", encoding="utf-8")

# 作用：删除临时文件
# 使用场景：清理脚本自己生成、可重新生成的文件
if temp_file.exists() and temp_file.parent == base_dir / "output":
    temp_file.unlink()

print(temp_file.exists())  # 例如：False 表示临时文件已删除
```

不要写这类代码：

```python
# 错误示例：范围太大，不适合自动化脚本
# for file_path in Path("input").glob("*"):
#     file_path.unlink()
```

## 十二、`ZipFile`：压缩结果文件

报表文件较多时，可以先压缩再发送邮件。

```python
from zipfile import ZipFile

# 作用：定义压缩包路径
# 使用场景：把 output 目录中的结果文件打包
zip_path = base_dir / "output" / "sales_report.zip"

with ZipFile(zip_path, mode="w") as zip_file:
    for file_path in output_dir.glob("*.*"):
        # 作用：把文件加入 zip，并只保留文件名
        # 使用场景：避免压缩包里出现很长的本地目录
        zip_file.write(file_path, arcname=file_path.name)

print(zip_path.exists())  # 例如：True 表示压缩包已生成
```

## 十三、本章完整案例

下面代码完成一个完整文件处理流程：

```text
创建目录
→ 生成样例输入文件
→ 按日期查找 CSV
→ 检查文件大小
→ 复制到 backup
→ 写入检查结果
→ 移动到 processed
→ 压缩 output
```

```python
from pathlib import Path
from zipfile import ZipFile
import shutil


base_dir = Path("automation_file_demo")
input_dir = base_dir / "input"
output_dir = base_dir / "output"
backup_dir = base_dir / "backup"
processed_dir = base_dir / "processed"

# 作用：创建自动化脚本需要的目录
# 使用场景：第一次运行或目录不存在时
for directory in [input_dir, output_dir, backup_dir, processed_dir]:
    directory.mkdir(parents=True, exist_ok=True)

# 作用：创建两个样例 CSV 文件
# 使用场景：没有真实输入文件时，用于本章练习
(input_dir / "orders_20260726_01.csv").write_text(
    "order_id,amount\nO001,1000\nO002,2000\n",
    encoding="utf-8",
)
(input_dir / "orders_20260726_02.csv").write_text(
    "order_id,amount\nO003,3000\n",
    encoding="utf-8",
)

# 作用：按处理日期查找订单文件
# 使用场景：只处理指定日期的输入文件
target_date = "2026-07-26"
file_pattern = f"orders_{target_date.replace('-', '')}_*.csv"
target_files = list(input_dir.glob(file_pattern))

if not target_files:
    raise FileNotFoundError(f"target files not found: {file_pattern}")

result_lines = []

for file_path in target_files:
    # 作用：检查文件是否为空
    # 使用场景：空文件不进入后续处理
    file_size = file_path.stat().st_size

    if file_size == 0:
        result_lines.append(f"{file_path.name},failed,empty file")
        continue

    # 作用：复制文件到 backup
    # 使用场景：处理前保留原始文件
    backup_file = backup_dir / file_path.name
    shutil.copy(file_path, backup_file)

    # 作用：移动文件到 processed
    # 使用场景：处理完成后避免重复处理
    processed_file = processed_dir / file_path.name
    shutil.move(file_path, processed_file)

    result_lines.append(f"{file_path.name},success,size={file_size}")

# 作用：输出处理结果清单
# 使用场景：记录本次处理了哪些文件
result_file = output_dir / "file_process_result.csv"
result_file.write_text(
    "file_name,status,message\n" + "\n".join(result_lines) + "\n",
    encoding="utf-8",
)

# 作用：压缩 output 目录中的结果文件
# 使用场景：后续邮件发送或归档
zip_path = output_dir / "file_process_result.zip"

with ZipFile(zip_path, mode="w") as zip_file:
    zip_file.write(result_file, arcname=result_file.name)

print(len(target_files))  # 例如：2 表示处理了 2 个文件
print(result_file.exists())  # 例如：True 表示结果清单已生成
print(zip_path.exists())  # 例如：True 表示压缩包已生成
```

运行后可以看到：

```text
automation_file_demo/
├── input/
├── output/
│   ├── file_process_result.csv
│   └── file_process_result.zip
├── backup/
│   ├── orders_20260726_01.csv
│   └── orders_20260726_02.csv
└── processed/
    ├── orders_20260726_01.csv
    └── orders_20260726_02.csv
```

## 十四、方法总结表

| 方法 / 类 | 作用 | 常用参数 / 写法 | 使用场景 |
| --- | --- | --- | --- |
| `Path()` | 创建路径对象 | `Path("input")` | 表示文件或目录 |
| `/` | 拼接路径 | `base_dir / "input"` | 跨系统拼接路径 |
| `mkdir()` | 创建目录 | `parents=True`、`exist_ok=True` | 创建输出、备份、日志目录 |
| `exists()` | 检查路径是否存在 | 无 | 输入文件到达检查 |
| `is_file()` | 判断是否是文件 | 无 | 读取前确认路径类型 |
| `is_dir()` | 判断是否是目录 | 无 | 遍历前确认路径类型 |
| `stat()` | 获取文件属性 | `.st_size` | 检查文件大小 |
| `glob()` | 按模式查找文件 | `"*.csv"`、`"**/*.log"` | 批量处理文件 |
| `iterdir()` | 遍历目录内容 | 无 | 查看目录下所有子项 |
| `read_text()` | 读取小文本文件 | `encoding="utf-8"` | 读取配置、结果、简单日志 |
| `write_text()` | 写入小文本文件 | `encoding="utf-8"` | 输出状态文件、结果摘要 |
| `shutil.copy()` | 复制文件 | `源文件`、`目标文件` | 处理前备份 |
| `shutil.move()` | 移动文件 | `源文件`、`目标文件` | 处理完成后归档到 processed |
| `unlink()` | 删除文件 | 无 | 只删除脚本生成的临时文件 |
| `ZipFile` | 创建压缩包 | `mode="w"` | 打包报表或处理结果 |

## 十五、本章练习

1. 创建 `automation_file_demo` 目录，并在下面创建 `input`、`output`、`backup`、`processed`。
2. 在 `input` 中创建两个 `orders_20260726_*.csv` 文件。
3. 使用 `glob()` 查找指定日期的订单文件。
4. 检查每个文件是否存在、是否是文件、大小是否大于 0。
5. 把输入文件复制到 `backup`。
6. 把处理完成的输入文件移动到 `processed`。
7. 在 `output` 中生成 `file_process_result.csv`。
8. 把结果文件压缩成 `file_process_result.zip`。
9. 思考：如果第二次运行完整案例，结果会发生什么？为什么？

