# 第2章 文件、目录与共享路径处理

日本项目里的自动化脚本，最先碰到的问题通常不是算法，而是“文件放在哪里、脚本从哪里读、结果要写到哪里、共享目录怎么处理”。

## 2.1 为什么这一章很重要

现场常见场景包括：

- 从共享目录读取日报原始文件
- 批量整理上传下载文件
- 按日期创建输出目录
- 按规则归档旧文件
- 检查某个输入文件是否已经到达

如果路径、目录、文件存在性处理不好，后面的自动化流程很容易半路失败。

## 2.2 不要硬拼路径字符串

推荐统一使用 `pathlib`：

```python
from pathlib import Path

base_dir = Path("work")
input_dir = base_dir / "input"
output_dir = base_dir / "output"
archive_dir = base_dir / "archive"

output_dir.mkdir(parents=True, exist_ok=True)
archive_dir.mkdir(parents=True, exist_ok=True)
```

这样比手写字符串更安全，也更适合 Windows 路径环境。

## 2.3 日本项目里常见的目录设计

建议一开始就把目录约定清楚：

```text
automation_report/
├── input/
├── output/
├── archive/
├── logs/
└── config/
```

各目录的职责：

- `input/`：原始输入文件
- `output/`：本次生成结果
- `archive/`：历史归档
- `logs/`：运行日志
- `config/`：配置文件

很多项目失败，不是代码写不出来，而是输入、输出和归档目录混在一起，导致重复处理或误删文件。

## 2.4 文件存在性检查

脚本开始时先检查依赖文件是否存在：

```python
from pathlib import Path

source_file = Path("input/sales_202607.csv")

if not source_file.exists():
    raise FileNotFoundError(f"找不到输入文件: {source_file}")
```

这比脚本跑到一半才报错更容易排查。

## 2.5 批量扫描文件

```python
from pathlib import Path

input_dir = Path("input")
csv_files = list(input_dir.glob("*.csv"))

for file_path in csv_files:
    print(file_path.name)
```

现场经常要处理：

- 每天一份文件
- 一个目录下多家店铺文件
- 固定命名规则的一批日志

所以批量扫描是必会项。

## 2.6 按日期生成输出文件

```python
from datetime import datetime
from pathlib import Path

today_str = datetime.now().strftime("%Y%m%d")
output_file = Path("output") / f"daily_report_{today_str}.xlsx"
```

这是日报、月报、备份文件里最常见的命名方式。

## 2.7 文件移动与归档

```python
from pathlib import Path
import shutil

source = Path("input/sales_202607.csv")
target = Path("archive/sales_202607.csv")

shutil.move(str(source), str(target))
```

要点：

- 先确认目标目录存在
- 不要覆盖未确认的重要文件
- 归档前记录日志

## 2.8 日本项目里常见的注意点

- Windows 路径较多，反斜杠和权限问题要注意
- 共享目录可能会被其他人同时写入
- 文件名可能包含日文
- 月末、月初的文件命名规则可能变化
- 同名文件可能需要按日期归档，不能直接覆盖

## 2.9 本章练习

请完成一个最小脚本，要求：

1. 创建 `input`、`output`、`archive`、`logs` 目录。
2. 扫描 `input` 目录中的全部 CSV 文件。
3. 如果没有文件，输出明确提示。
4. 如果有文件，打印文件名和文件数量。

这个练习虽然简单，但就是企业自动化脚本的起点。
