# 第4章 文本处理、正则表达式与日志抽取

自动化不只是处理表格。日本项目里还有大量文本型任务，例如：

- 从日志里提取错误信息
- 从邮件正文中抓取编号
- 从文件名中提取日期
- 清洗固定格式但不规整的文本

## 4.1 为什么文本处理很常见

很多现场数据并不是漂亮的表格，而是：

- 日志文件
- 错误通知文本
- 接口返回的半结构化内容
- 命名规则不完全统一的文件名

这类任务如果完全手工处理，重复度高、容易漏。

## 4.2 字符串基础处理

```python
text = "  ERROR: user_id=U1001  "

clean_text = text.strip()
print(clean_text)
print(clean_text.startswith("ERROR"))
```

先学会基础字符串处理，再上正则，不要一开始就把所有问题都交给正则表达式。

## 4.3 用正则提取编号

```python
import re

text = "受注番号: ORD-20260723-001"
match = re.search(r"ORD-\d{8}-\d{3}", text)

if match:
    print(match.group())
```

这类提取在日志分析、文件名解析、业务编号抽取中非常常见。

## 4.4 从文件名中提取日期

```python
import re

file_name = "sales_20260723_tokyo.csv"
match = re.search(r"\d{8}", file_name)

if match:
    print(match.group())
```

很多脚本的分支逻辑，就是基于文件名中的日期或店铺代码来决定的。

## 4.5 读取日志并筛选错误行

```python
from pathlib import Path

log_file = Path("input/app.log")

with log_file.open("r", encoding="utf-8") as f:
    for line in f:
        if "ERROR" in line:
            print(line.strip())
```

先用最直接的方法把错误行找出来，再决定要不要做更复杂的提取。

## 4.6 用正则抽取错误时间和错误码

```python
import re

line = "2026-07-23 09:15:21 ERROR E001 Database connection failed"
match = re.search(r"(\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}).*(E\d{3})", line)

if match:
    print("时间:", match.group(1))
    print("错误码:", match.group(2))
```

这一步是后面做日志分析项目的基础。

## 4.7 日本项目里常见的文本自动化任务

- 从异常日志中提取错误码
- 从邮件标题中提取案件编号
- 从文件名中提取日期和分店代码
- 清洗全角半角空格
- 统一换行和空白字符

因此文本处理这章，不是补充内容，而是实战高频能力。

## 4.8 本章练习

请完成下面 3 个任务：

1. 从文件名中提取日期。
2. 从一段文本中提取业务编号。
3. 从日志文件中筛选全部 `ERROR` 行。

如果这三件事能独立完成，后面的日志分析和异常通知项目就能接上。
