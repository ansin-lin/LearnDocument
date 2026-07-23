# 第10章 文件与数据处理

> 本章目标：掌握文本文件、Excel、CSV、JSON 的读取和写入方式，理解二进制文件和大文件处理的基本思路，并能在企业项目中正确选择处理方法。

## 前置知识

- 字符串、列表、字典
- `with` 语句
- `pathlib` 基础
- `json` 基础概念

## 一、为什么要学文件处理

- 企业项目里经常要处理配置文件、日志文件、报表文件、接口数据
- 很多导入导出、批处理、对账、统计任务，本质上都是文件处理
- 文件处理如果写得不规范，很容易出现编码问题、资源泄漏和大文件卡死

## 二、文件处理的基本概念

### 2.1 文件的类型

| 类型 | 说明 | 常见场景 |
| --- | --- | --- |
| 文本文件 | 以字符形式存储 | `.txt`、`.md`、`.log` |
| 表格文件 | 以行列方式组织数据 | `.xlsx`、`.csv` |
| 结构化文本文件 | 有固定格式的文本 | `.json` |
| 二进制文件 | 以字节形式存储 | 图片、音频、视频、压缩包、PDF |

### 2.2 文件操作的基本流程

1. 确定文件路径
2. 选择打开模式
3. 读取或写入
4. 关闭文件

推荐使用 `with`，这样文件会自动关闭。

## 三、路径处理

推荐使用 `pathlib`，不要把路径字符串手动拼得太复杂。

```python
from pathlib import Path  # 导入 Path

base_dir = Path("data")  # 定义基础目录
input_file = base_dir / "input" / "users.csv"  # 拼接路径
print(input_file)  # data\input\users.csv 或 data/input/users.csv
```

常用方法：

| 方法 | 作用 | 示例 |
| --- | --- | --- |
| `exists()` | 判断是否存在 | `path.exists()` |
| `is_file()` | 是否是文件 | `path.is_file()` |
| `is_dir()` | 是否是目录 | `path.is_dir()` |
| `read_text()` | 读取文本 | `path.read_text(encoding="utf-8")` |
| `write_text()` | 写入文本 | `path.write_text("hello", encoding="utf-8")` |

## 四、普通文本文件 txt

### 4.1 为什么先学 txt

`txt` 是最基础的文本文件形式。  
先学它可以把这些基础能力一次讲清楚：

- `open()` 怎么用
- `with` 为什么要用
- 编码为什么要写
- `read()`、`readline()`、`readlines()` 的区别
- `write()`、`writelines()`、`flush()` 的区别

### 4.2 打开文件的基本写法

`open()` 是最基础的文件打开方法。

| 参数 | 作用 | 示例 |
| --- | --- | --- |
| `file` | 文件路径 | `"a.txt"` |
| `mode` | 打开模式 | `"r"`、`"w"`、`"a"`、`"rb"` |
| `encoding` | 编码方式 | `"utf-8"` |

常用打开模式：

| 模式 | 作用 | 适用场景 |
| --- | --- | --- |
| `r` | 只读 | 读取文本文件 |
| `w` | 写入，会覆盖原文件 | 生成新文件 |
| `a` | 追加写入 | 日志、追加记录 |
| `rb` | 以二进制只读 | 图片、PDF |
| `wb` | 以二进制写入 | 图片保存、文件复制 |
| `r+` | 读写 | 需要在原文件基础上修改 |

### 4.3 读取文本文件

#### 4.3.1 一次性读取全部内容

适合小文件。

```python
with open("a.txt", "r", encoding="utf-8") as file:  # 打开文本文件
    content = file.read()  # 读取全部内容
    print(content)  # 输出整个文件内容
```

常用读取方法：

| 方法 | 作用 | 适用场景 |
| --- | --- | --- |
| `read()` | 读取全部内容 | 小文件 |
| `readline()` | 读取一行 | 逐行处理时 |
| `readlines()` | 读取所有行，返回列表 | 需要保留行结构时 |

补充说明：

- `readline()` 每次只读取一行
- `for line in file` 会自动逐行遍历
- `readlines()` 会一次性把所有行读入内存，小文件可以用，大文件不建议用

#### 4.3.2 按行读取

适合日志、配置文件、文本清洗。

```python
with open("a.txt", "r", encoding="utf-8") as file:  # 打开文本文件
    for line in file:  # 逐行遍历
        print(line.strip())  # 去掉换行后输出每一行
```

### 4.4 写入文本文件

#### 4.4.1 覆盖写入

`w` 模式会先清空原文件，再写入新内容。

```python
with open("b.txt", "w", encoding="utf-8") as file:  # 以写入模式打开
    file.write("Hello Python\n")  # 写入一行内容
    file.write("This is a file demo\n")  # 再写入一行内容
```

#### 4.4.2 追加写入

`a` 模式会把内容追加到文件末尾，不会覆盖原内容。

```python
with open("log.txt", "a", encoding="utf-8") as file:  # 以追加模式打开
    file.write("new log line\n")  # 在文件末尾追加一行
```

### 4.5 文本处理常见方法

| 方法 | 作用 | 说明 |
| --- | --- | --- |
| `read()` | 读取全部 | 一次性拿到全文 |
| `write()` | 写入字符串 | 只能写字符串 |
| `writelines()` | 写入多个字符串 | 需要自己处理换行 |
| `flush()` | 刷新缓冲区 | 先把内容从 Python 缓冲区交给系统缓冲区，不代表一定已物理落盘 |

### 4.6 `txt` 例子：读取并清洗日志

```python
with open("app.log", "r", encoding="utf-8") as file:  # 打开日志文件
    for line in file:  # 逐行读取
        text = line.strip()  # 去掉首尾空白和换行
        if text:  # 如果不是空行
            print(text)  # 输出清洗后的日志行
```

### 4.7 `txt` 例子：写入简单报告

```python
report_lines = [  # 准备报告内容
    "Sales Report\n",  # 报表标题
    "Date: 2026-07-10\n",  # 日期
    "Total: 1200\n",  # 总计
]

with open("report.txt", "w", encoding="utf-8") as file:  # 打开报告文件
    file.writelines(report_lines)  # 一次写入多行
```

## 五、Excel 文件

### 5.1 为什么 Excel 要单独讲

在企业项目里，Excel 是报表、统计、导入导出最常见的格式之一。  
很多业务同学、测试同学、现场人员都更习惯直接看 Excel，而不是代码或 JSON。

### 5.2 Excel 的使用前提

Python 里常用 `openpyxl` 处理 `.xlsx` 文件。

```text
pip install openpyxl
```

说明：

- `openpyxl` 主要用于 `.xlsx`
- 它适合读取、写入、修改 Excel
- 如果是老旧 `.xls` 文件，通常需要另外处理

### 5.2.1 `load_workbook()` 常用参数

| 参数 | 作用 | 说明 |
| --- | --- | --- |
| `filename` | 文件路径 | 必填，Excel 文件路径 |
| `read_only` | 只读模式 | 大文件读取时更省内存 |
| `data_only` | 读取公式结果 | 只拿单元格计算后的值，不拿公式本身 |
| `keep_vba` | 保留宏 | 处理带宏的文件时使用 |
| `keep_links` | 保留外部链接 | 一般默认即可 |

### 5.3 读取 Excel

```python
from openpyxl import load_workbook  # 导入读取函数

workbook = load_workbook("users.xlsx")  # 打开 Excel 文件
sheet = workbook.active  # 获取当前激活的工作表

for row in sheet.iter_rows(values_only=True):  # 按行读取数据
    print(row)  # 输出每一行的值
```

### 5.4 读取后的常用属性

`load_workbook()` 得到的是 `Workbook` 对象，工作表是 `Worksheet` 对象。  
这两个对象在项目里最常用的属性如下。

#### 5.4.1 `Workbook` 常用属性和方法

| 属性 / 方法 | 作用 | 说明 |
| --- | --- | --- |
| `active` | 获取当前活动工作表 | 最常用 |
| `sheetnames` | 获取所有工作表名 | 返回列表 |
| `create_sheet()` | 新建工作表 | 可指定位置 |
| `remove()` | 删除工作表 | 传入工作表对象 |
| `copy_worksheet()` | 复制工作表 | 同一工作簿内复制 |
| `save()` | 保存文件 | 写回磁盘 |
| `close()` | 关闭工作簿 | 读取后建议调用 |

#### 5.4.2 `Worksheet` 常用属性和方法

| 属性 / 方法 | 作用 | 说明 |
| --- | --- | --- |
| `title` | 工作表名 | 可读取也可修改 |
| `max_row` | 最大行数 | 通常用于判断数据范围 |
| `max_column` | 最大列数 | 通常用于判断数据范围 |
| `cell()` | 按行列访问单元格 | `sheet.cell(row=2, column=2)` |
| `iter_rows()` | 按行遍历 | 常用于批量读取 |
| `iter_cols()` | 按列遍历 | 少数场景会用 |
| `append()` | 追加一行 | 常用于写入 |
| `insert_rows()` | 插入行 | 结构调整 |
| `delete_rows()` | 删除行 | 结构调整 |
| `merge_cells()` | 合并单元格 | 报表格式化 |
| `unmerge_cells()` | 取消合并 | 反向操作 |
| `freeze_panes` | 冻结窗格 | 报表查看常用 |
| `auto_filter` | 自动筛选 | 报表筛选常用 |
| `column_dimensions` | 列宽设置 | 格式调整 |
| `row_dimensions` | 行高设置 | 格式调整 |

### 5.5 读取指定单元格

```python
from openpyxl import load_workbook  # 导入读取函数

workbook = load_workbook("users.xlsx")  # 打开 Excel 文件
sheet = workbook["Sheet1"]  # 获取指定工作表

print(sheet["A1"].value)  # 输出 A1 单元格内容
print(sheet.cell(row=2, column=2).value)  # 输出第 2 行第 2 列内容
```

### 5.6 Excel 的常用读写方式

#### 5.6.1 `sheet["A1"]`

- 适合直接读取明确位置
- 适合小范围确认
- 报表调试时很常用

#### 5.6.2 `sheet.cell(row, column)`

- 适合程序里按行列坐标处理
- 动态生成报表时更方便

#### 5.6.3 `iter_rows()`

- 适合逐行读取
- 常用于导入、统计、批量转换

#### 5.6.4 `append()`

- 适合逐行写入
- 常用于导出、报表生成

### 5.7 写入 Excel

```python
from openpyxl import Workbook  # 导入创建工作簿的类

workbook = Workbook()  # 创建新的工作簿
sheet = workbook.active  # 获取默认工作表

sheet["A1"] = "id"  # 写入表头
sheet["B1"] = "name"  # 写入表头
sheet["C1"] = "age"  # 写入表头

sheet.append([1, "Tanaka", 28])  # 追加一行数据
sheet.append([2, "Sato", 31])  # 再追加一行数据

workbook.save("users_out.xlsx")  # 保存 Excel 文件
```

### 5.8 Excel 常见操作

| 操作 | 方法 | 说明 |
| --- | --- | --- |
| 读取工作表 | `workbook.active` / `workbook["Sheet1"]` | 获取工作表 |
| 读取单元格 | `sheet["A1"]` / `sheet.cell()` | 获取单个值 |
| 写入单元格 | `sheet["A1"] = "id"` | 直接赋值 |
| 追加行 | `sheet.append([...])` | 批量添加一行 |
| 保存文件 | `workbook.save()` | 写回磁盘 |
| 合并单元格 | `sheet.merge_cells()` | 报表标题常用 |
| 冻结窗格 | `sheet.freeze_panes` | 查看大表时固定表头 |
| 设置列宽 | `sheet.column_dimensions[...]` | 优化显示 |
| 设置行高 | `sheet.row_dimensions[...]` | 优化显示 |

### 5.9 Excel 的项目场景

- 报表导出
- 统计结果查看
- 人工确认数据
- 业务方交付文件
- 批量维护主数据

### 5.10 Excel 常见注意点

- `load_workbook()` 读取后，修改完要 `save()`
- 读大文件时可以考虑 `read_only=True`
- 如果要保留公式结果，考虑 `data_only=True`
- 报表格式和数据内容要分开处理

## 六、CSV 文件

### 6.1 CSV 是什么

CSV 是“逗号分隔值”文件。  
它本质上是文本文件，但结构接近表格，所以在导入导出场景里非常常见。

### 6.2 为什么 CSV 重要

- 轻量
- 通用
- 适合批量数据
- Excel 可以直接打开

### 6.2.1 CSV 的基本结构

CSV 本质上是按分隔符组织的文本，默认分隔符是逗号。

```text
id,name,age
1,Tanaka,28
2,Sato,31
```

### 6.2.2 `open()` 读取 CSV 时的注意点

- 通常要写 `encoding="utf-8"`
- 通常要写 `newline=""`
- 这样可以减少换行处理问题

### 6.2.3 `csv` 常见配置项

| 配置 | 作用 | 说明 |
| --- | --- | --- |
| `delimiter` | 分隔符 | 默认是逗号 |
| `quotechar` | 引号字符 | 默认是 `"` |
| `quoting` | 引号策略 | 控制输出时是否加引号 |
| `skipinitialspace` | 跳过分隔符后的空格 | 清洗时常见 |
| `fieldnames` | 字段名 | `DictWriter` 常用 |

### 6.3 读取 CSV

```python
import csv  # 导入 csv 模块

with open("users.csv", "r", encoding="utf-8", newline="") as file:  # 打开 CSV 文件
    reader = csv.reader(file)  # 创建读取器
    for row in reader:  # 逐行读取
        print(row)  # 每一行都是列表
```

### 6.4 `reader()` 读取后的常用特性

`csv.reader()` 读取后，每一行都是一个列表。

常用读取方式：

- `row[0]`：第一列
- `row[1]`：第二列
- `for row in reader`：逐行处理

适合：

- 不知道字段名的原始 CSV
- 只想快速遍历每一行
- 简单批量导入

### 6.5 读取字典形式的 CSV

```python
import csv  # 导入 csv 模块

with open("users.csv", "r", encoding="utf-8", newline="") as file:  # 打开 CSV 文件
    reader = csv.DictReader(file)  # 按字典方式读取
    for row in reader:  # 逐行遍历
        print(row["name"])  # 读取 name 列
```

### 6.6 `DictReader()` 常用属性和行为

`DictReader` 的每一行是字典，更适合业务字段处理。

常用特性：

- 按表头自动映射字段名
- 读取后可以直接用列名取值
- 更适合接口导出、业务表导入

常见注意点：

- 第一行通常作为表头
- 表头名要和实际字段一致
- 某些列缺失时需要做容错

### 6.7 写入 CSV

```python
import csv  # 导入 csv 模块

rows = [  # 准备要写入的数据
    ["id", "name", "age"],  # 表头
    [1, "Tanaka", 28],  # 第一行数据
    [2, "Sato", 31],  # 第二行数据
]

with open("users_out.csv", "w", encoding="utf-8", newline="") as file:  # 打开输出文件
    writer = csv.writer(file)  # 创建写入器
    writer.writerows(rows)  # 写入多行
```

### 6.8 `writer()` 和 `DictWriter()` 的区别

| 方法 / 类 | 作用 | 说明 |
| --- | --- | --- |
| `reader()` | 读取 CSV | 每行返回列表 |
| `writer()` | 写入 CSV | 逐行写入列表 |
| `DictReader()` | 按字典读取 | 按表头映射字段名 |
| `DictWriter()` | 按字典写入 | 适合字段固定的数据 |

### 6.9 `DictWriter()` 的常用写法

```python
import csv  # 导入 csv 模块

fieldnames = ["id", "name", "age"]  # 定义字段顺序
rows = [  # 准备字典数据
    {"id": 1, "name": "Tanaka", "age": 28},  # 第一行
    {"id": 2, "name": "Sato", "age": 31},  # 第二行
]

with open("users_dict.csv", "w", encoding="utf-8", newline="") as file:  # 打开文件
    writer = csv.DictWriter(file, fieldnames=fieldnames)  # 创建字典写入器
    writer.writeheader()  # 写入表头
    writer.writerows(rows)  # 写入多行字典数据
```

### 6.10 CSV 的企业场景

- 用户列表导出
- 订单列表导出
- 批量数据导入
- 报表统计结果输出
- 业务系统和 Excel 之间的数据交换

## 七、JSON 文件

### 7.1 JSON 是什么

JSON 是常见的数据交换格式。  
在 Web 项目里很常见，但它更偏“接口数据”和“配置数据”。

### 7.2 JSON 转换

```python
import json  # 导入 json 模块

data = {"name": "Tanaka", "age": 28}  # 准备 Python 字典
text = json.dumps(data, ensure_ascii=False)  # 转成 JSON 字符串
print(text)  # {"name": "Tanaka", "age": 28}
```

```python
import json  # 导入 json 模块

text = '{"name": "Tanaka", "age": 28}'  # 准备 JSON 字符串
data = json.loads(text)  # 转回 Python 对象
print(data["name"])  # Tanaka
```

### 7.3 读取和写入 JSON 文件

```python
import json  # 导入 json 模块

data = {"name": "Tanaka", "age": 28}  # 准备数据

with open("user.json", "w", encoding="utf-8") as file:  # 打开写入文件
    json.dump(data, file, ensure_ascii=False, indent=2)  # 写入 JSON 文件

with open("user.json", "r", encoding="utf-8") as file:  # 打开读取文件
    loaded = json.load(file)  # 读取 JSON 文件
    print(loaded["name"])  # Tanaka
```

### 7.4 `indent` 的作用

`indent` 用来控制 JSON 的缩进层级，方便人阅读。

## 八、二进制文件

图片、PDF、压缩包等不能按普通文本方式读写，要使用二进制模式。

```python
with open("image.png", "rb") as file:  # 以二进制只读方式打开
    data = file.read()  # 读取字节数据
    print(type(data))  # <class 'bytes'>
```

```python
with open("copy.png", "wb") as file:  # 以二进制写入方式打开
    file.write(data)  # 写入字节数据
```

二进制文件在本章里以“了解并会用”为主，不作为重点展开。

## 九、大文件处理

### 9.1 为什么要单独处理大文件

- `read()` 一次性读入内存，文件太大时容易占用大量内存
- 日志文件、历史数据文件、批量导出文件通常都比较大
- 大文件处理要尽量“边读边处理”，不要一次性全部加载

### 9.2 大文件读取方式

#### 9.2.1 按行遍历

这是最常见、最安全的方式。

```python
with open("big.log", "r", encoding="utf-8") as file:  # 打开大文件
    for line in file:  # 一次处理一行
        if "ERROR" in line:  # 只关注错误日志
            print(line.strip())  # 输出错误日志行
```

#### 9.2.2 固定大小分块读取

适合超大文件、二进制文件、文件搬运。

```python
chunk_size = 1024 * 1024  # 每次读取 1MB

with open("big.bin", "rb") as file:  # 以二进制方式打开
    while True:  # 循环读取
        chunk = file.read(chunk_size)  # 读取一个块
        if not chunk:  # 如果已经读完
            break  # 结束循环
        print(len(chunk))  # 输出本次读取的字节数
```

### 9.3 大文件写入方式

```python
lines = [f"line {i}\n" for i in range(1, 6)]  # 准备多行数据

with open("out.txt", "w", encoding="utf-8") as file:  # 打开输出文件
    for line in lines:  # 逐行写入
        file.write(line)  # 写入当前行
```

### 9.4 大文件场景说明

大文件处理在项目中确实会出现，但通常不是最频繁的日常任务。  
本章重点仍然应该放在文本、Excel、CSV 这些高频场景上。

## 十、企业级处理思路

- 先确认文件类型，再决定读取方式
- 文本文件优先用 `encoding="utf-8"`
- 读写文件统一使用 `with`
- Excel 和 CSV 这种结构化文件优先使用对应库
- 二进制文件一定要使用 `rb` / `wb`
- 大文件尽量按行或分块读取

## 十一、日本项目中的使用场景

- CSV 取込
- 帳票出力
- ログ解析
- バッチ処理
- Excel 统计表输出
- JSON API 数据处理
- 配置文件和环境文件处理

## 十二、Coding Rule

- 文件路径优先使用 `pathlib`
- 打开文件默认写明编码
- 写入文本不要遗漏换行
- CSV 处理时注意 `newline=""`
- Excel 处理时要区分读取、写入和保存
- 文件处理结束后必须确保资源关闭

## 十三、Code Review 关注点

- 是否使用了 `with`
- 是否错误使用了 `read()` 导致内存占用过大
- 是否把二进制文件当文本文件处理
- 是否忽略了编码问题
- 是否路径写死
- 是否缺少异常处理

## 十四、常见错误

- 文件读写后忘记关闭
- 用 `w` 覆盖了原文件
- 把图片、PDF 当文本文件打开
- 读取 CSV 时编码或换行处理错误
- 大文件直接 `read()` 到内存

## 十五、练习题

### 基础练习

1. 读取一个文本文件并按行输出
2. 创建一个新文本文件并写入两行内容
3. 使用 `Path` 拼接一个文件路径

### 综合练习

1. 用 `openpyxl` 读取一个 Excel 文件并输出每一行
2. 读取一个 CSV 文件，把指定列的数据筛选出来并写入新文件
3. 读取一个 JSON 文件，修改其中一个字段后重新写回文件

## 十六、本章总结

- 文件处理是 Python 企业开发里的基础能力
- `txt`、`Excel`、`CSV` 是项目里最常见的文件格式
- `JSON` 在接口数据处理中很常见，但不是项目里唯一核心格式
- 二进制和大文件重要，但在本章里以了解和会用为主
- `with`、`pathlib`、`openpyxl`、`csv`、`json` 是最常用的组合
