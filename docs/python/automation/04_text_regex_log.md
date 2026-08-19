# 文本、日志与正则处理

自动化不只处理 CSV 和 Excel。日本项目中也经常需要处理日志、配置文件、固定格式文本。

常见任务包括：

- 从日志中提取 `ERROR` / `WARN`。
- 提取订单号、用户 ID、接口路径。
- 统计异常发生次数。
- 对邮箱、手机号等信息脱敏。
- 输出日志调查结果 CSV。

学完本章后，你要能完成一个小型日志分析流程：

```text
读取日志文件
→ 逐行筛选 ERROR / WARN
→ 正则提取 order_id、user_id、path
→ 脱敏邮箱
→ 输出 error_summary.csv
```

## 一、本章示例目录

本章统一使用下面的目录结构：

```text
automation_log_demo/
├── logs/
│   └── app_20260726.log
└── output/
```

日志内容示例：

```text
2026-07-26 09:00:01 INFO user_id=U001 path=/api/orders message=start
2026-07-26 09:01:10 ERROR user_id=U002 order_id=O202607260001 path=/api/orders email=taro@example.com message=payment failed
2026-07-26 09:02:20 WARN user_id=U003 order_id=O202607260002 path=/api/customers message=response slow
```

## 二、`read_text()`：读取小文本文件

如果文本文件不大，可以一次性读取。

```python
from pathlib import Path

# 作用：定义日志文件路径
# 使用场景：自动化脚本读取应用日志或批处理日志
log_file = Path("automation_log_demo") / "logs" / "app_20260726.log"

# 作用：读取整个日志文件
# 使用场景：文件不大，需要一次性分析全部内容时
log_text = log_file.read_text(encoding="utf-8")

print(len(log_text))  # 例如：350 表示读取到 350 个字符
print(log_text.splitlines()[0])  # 例如：显示第一行日志
```

`read_text()` 常用参数：

| 参数 | 含义 | 常用写法 | 使用场景 |
| --- | --- | --- | --- |
| `encoding` | 文本编码 | `"utf-8"`、`"cp932"` | 日志中包含日文时必须确认编码 |
| `errors` | 解码失败时如何处理 | `"strict"`、`"ignore"` | 日志编码异常时临时排查 |

注意：大日志不建议一次性 `read_text()`，应逐行读取。

## 三、`open()`：逐行处理大日志

日志文件可能很大，逐行处理更稳定。

```python
# 作用：保存 ERROR 日志行
# 使用场景：障害调查时先筛选错误行
error_lines = []

with log_file.open("r", encoding="utf-8") as f:
    for line in f:
        # 作用：判断当前行是否包含 ERROR
        # 使用场景：只提取错误日志，减少后续处理量
        if "ERROR" in line:
            error_lines.append(line.strip())

print(len(error_lines))  # 例如：1 表示找到 1 行 ERROR 日志
print(error_lines[:3])  # 例如：显示前 3 行 ERROR 日志
```

逐行处理常用场景：

| 场景 | 原因 |
| --- | --- |
| 日志文件很大 | 避免一次性占用过多内存 |
| 只需要筛选部分行 | 不必把全部内容放入内存 |
| 批量处理多个日志 | 每个文件逐行处理更稳定 |

## 四、`re.search()`：查找第一个匹配

`re.search()` 用来判断一行文本中是否存在某种模式，并提取第一个匹配结果。

### 1. 基础功能示例

```python
import re

line = "2026-07-26 09:01:10 ERROR user_id=U002 order_id=O202607260001 path=/api/orders"

# 作用：查找订单号
# 使用场景：从一行日志中提取 order_id
match = re.search(r"order_id=(O\d+)", line)

if match:
    print(match.group(1))  # 例如：O202607260001
```

### 2. 常用参数示例

```python
# 作用：忽略大小写查找 ERROR
# 使用场景：日志级别可能出现 error、Error、ERROR 等不同写法时
level_match = re.search(
    pattern=r"\berror\b",
    string=line,
    flags=re.IGNORECASE,
)

print(level_match.group(0))  # 例如：ERROR
```

`re.search()` 常用参数：

| 参数 | 含义 | 常用写法 | 使用场景 |
| --- | --- | --- | --- |
| `pattern` | 正则表达式 | `r"order_id=(O\d+)"` | 定义要找的文本规则 |
| `string` | 被搜索的文本 | `line` | 一行日志或一段文本 |
| `flags` | 匹配选项 | `re.IGNORECASE` | 忽略大小写、多行匹配 |

## 五、捕获分组和 `group()`

圆括号 `()` 表示捕获分组。`group(1)` 取第一个分组。

```python
# 作用：同时提取日期、级别、用户 ID
# 使用场景：把一行日志拆成结构化字段
match = re.search(
    r"(\d{4}-\d{2}-\d{2}) .* \b(INFO|WARN|ERROR)\b user_id=(U\d+)",
    line,
)

if match:
    print(match.group(1))  # 例如：2026-07-26
    print(match.group(2))  # 例如：ERROR
    print(match.group(3))  # 例如：U002
```

常用写法：

| 写法 | 含义 | 示例 |
| --- | --- | --- |
| `group(0)` | 完整匹配内容 | `order_id=O202607260001` |
| `group(1)` | 第一个括号捕获内容 | `O202607260001` |
| `group(2)` | 第二个括号捕获内容 | `ERROR` |

## 六、`re.findall()`：提取所有匹配

`re.findall()` 会返回所有匹配结果，适合从整段日志中提取所有订单号、用户 ID、接口路径。

```python
# 作用：从整段日志中提取所有订单号
# 使用场景：统计受影响订单时
order_ids = re.findall(r"order_id=(O\d+)", log_text)

print(len(order_ids))  # 例如：2 表示提取到 2 个订单号
print(order_ids[:3])  # 例如：['O202607260001', 'O202607260002']
```

注意：如果正则中有括号，`findall()` 返回括号中的内容；如果没有括号，返回完整匹配内容。

## 七、`re.finditer()`：逐个取得匹配对象

如果既要匹配内容，又要知道位置，可以使用 `finditer()`。

```python
# 作用：逐个查找日志中的接口路径
# 使用场景：需要取得匹配内容和位置时
for match in re.finditer(r"path=([/\w-]+)", log_text):
    print(match.group(1), match.start())
    # 例如：/api/orders 31
```

`findall()` 和 `finditer()` 的区别：

| 方法 | 返回结果 | 使用场景 |
| --- | --- | --- |
| `findall()` | 匹配到的文本列表 | 只关心提取结果 |
| `finditer()` | 匹配对象迭代器 | 还需要位置、分组等信息 |

## 八、`re.sub()`：替换和脱敏

日志调查结果对外发送前，经常需要脱敏邮箱、手机号、Token 等信息。

```python
# 作用：隐藏日志中的邮箱地址
# 使用场景：输出调查结果前脱敏个人信息
masked_text = re.sub(
    pattern=r"[\w\.-]+@[\w\.-]+",
    repl="***@***",
    string=log_text,
)

print(masked_text)
# 例如：email=***@*** message=payment failed
```

`re.sub()` 常用参数：

| 参数 | 含义 | 常用写法 | 使用场景 |
| --- | --- | --- | --- |
| `pattern` | 要替换的规则 | 邮箱正则、手机号正则 | 查找敏感信息 |
| `repl` | 替换成什么 | `"***@***"` | 脱敏 |
| `string` | 原始文本 | `log_text` | 日志内容 |
| `count` | 最多替换几次 | `0` 表示全部 | 只替换前几个匹配时 |

## 九、`re.compile()`：复用正则规则

同一个正则要反复使用时，可以先编译。

```python
# 作用：编译订单号正则
# 使用场景：逐行处理大量日志时复用规则
order_pattern = re.compile(r"order_id=(O\d+)")

for line in log_text.splitlines():
    match = order_pattern.search(line)

    if match:
        print(match.group(1))  # 例如：O202607260001
```

`re.compile()` 适合：

- 同一个规则反复使用。
- 多个函数之间共享规则。
- 日志分析代码需要更清晰地管理多个规则。

## 十、生成日志分析结果

下面代码把日志行转换成结构化数据。

```python
import pandas as pd

# 作用：编译常用日志提取规则
# 使用场景：逐行分析日志时复用
level_pattern = re.compile(r"\b(INFO|WARN|ERROR)\b")
order_pattern = re.compile(r"order_id=(O\d+)")
user_pattern = re.compile(r"user_id=(U\d+)")
path_pattern = re.compile(r"path=([/\w-]+)")

records = []

with log_file.open("r", encoding="utf-8") as f:
    for line in f:
        level_match = level_pattern.search(line)

        if not level_match:
            continue

        level = level_match.group(1)

        if level not in ["ERROR", "WARN"]:
            continue

        # 作用：对日志行做邮箱脱敏
        # 使用场景：输出调查结果前避免暴露个人信息
        masked_line = re.sub(r"[\w\.-]+@[\w\.-]+", "***@***", line.strip())

        order_match = order_pattern.search(line)
        user_match = user_pattern.search(line)
        path_match = path_pattern.search(line)

        records.append(
            {
                "level": level,
                "order_id": order_match.group(1) if order_match else None,
                "user_id": user_match.group(1) if user_match else None,
                "api_path": path_match.group(1) if path_match else None,
                "message": masked_line,
            }
        )

log_result = pd.DataFrame(records)

print(log_result.shape)  # 例如：(2, 5) 表示提取到 2 条 WARN / ERROR 记录
print(log_result.head())
```

## 十一、输出日志调查结果

```python
# 作用：创建输出目录
# 使用场景：保存日志调查结果
output_dir = Path("automation_log_demo") / "output"
output_dir.mkdir(parents=True, exist_ok=True)

# 作用：导出日志调查结果 CSV
# 使用场景：交给开发、运维或负责人进一步确认
result_file = output_dir / "error_summary.csv"
log_result.to_csv(result_file, index=False, encoding="utf-8-sig")

print(result_file.exists())  # 例如：True 表示 error_summary.csv 已生成
```

## 十二、常用正则写法

| 写法 | 含义 | 示例 |
| --- | --- | --- |
| `\d+` | 一个或多个数字 | `20260726` |
| `\w+` | 字母、数字、下划线 | `user_id` |
| `.*` | 任意内容 | 粗略匹配中间文本 |
| `(...)` | 捕获分组 | 提取订单号 |
| `\s+` | 一个或多个空白 | 分隔字段 |
| `[A-Z]+` | 一个或多个大写字母 | `ERROR` |
| `\b` | 单词边界 | 匹配完整的 `ERROR` |
| `?` | 前一个字符出现 0 次或 1 次 | 可选内容 |
| `+` | 前一个规则出现 1 次或多次 | 多位数字 |
| `*` | 前一个规则出现 0 次或多次 | 任意长度 |

正则不要一开始写得过于复杂。项目中更推荐：

1. 先匹配一行样例。
2. 再增加捕获分组。
3. 再放到循环中处理整个文件。
4. 最后处理匹配不到的情况。

## 十三、本章完整案例

下面代码会自动创建样例日志，然后完成日志分析和结果导出。

```python
from pathlib import Path
import re

import pandas as pd


base_dir = Path("automation_log_demo")
log_dir = base_dir / "logs"
output_dir = base_dir / "output"

# 作用：创建日志和输出目录
# 使用场景：准备本章练习环境
log_dir.mkdir(parents=True, exist_ok=True)
output_dir.mkdir(parents=True, exist_ok=True)

# 作用：创建样例日志文件
# 使用场景：没有真实日志时用于练习
log_file = log_dir / "app_20260726.log"
log_file.write_text(
    "\n".join(
        [
            "2026-07-26 09:00:01 INFO user_id=U001 path=/api/orders message=start",
            "2026-07-26 09:01:10 ERROR user_id=U002 order_id=O202607260001 path=/api/orders email=taro@example.com message=payment failed",
            "2026-07-26 09:02:20 WARN user_id=U003 order_id=O202607260002 path=/api/customers message=response slow",
            "2026-07-26 09:03:30 INFO user_id=U004 path=/api/orders message=end",
        ]
    ),
    encoding="utf-8",
)

# 作用：读取日志文本
# 使用场景：提取所有订单号或做整体脱敏时
log_text = log_file.read_text(encoding="utf-8")

print(len(log_text))  # 例如：350 表示读取到日志字符数

# 作用：编译正则规则
# 使用场景：逐行处理时复用多个规则
level_pattern = re.compile(r"\b(INFO|WARN|ERROR)\b")
order_pattern = re.compile(r"order_id=(O\d+)")
user_pattern = re.compile(r"user_id=(U\d+)")
path_pattern = re.compile(r"path=([/\w-]+)")
email_pattern = re.compile(r"[\w\.-]+@[\w\.-]+")

records = []

with log_file.open("r", encoding="utf-8") as f:
    for line in f:
        level_match = level_pattern.search(line)

        if not level_match:
            continue

        level = level_match.group(1)

        if level not in ["ERROR", "WARN"]:
            continue

        order_match = order_pattern.search(line)
        user_match = user_pattern.search(line)
        path_match = path_pattern.search(line)
        masked_line = email_pattern.sub("***@***", line.strip())

        records.append(
            {
                "level": level,
                "order_id": order_match.group(1) if order_match else None,
                "user_id": user_match.group(1) if user_match else None,
                "api_path": path_match.group(1) if path_match else None,
                "message": masked_line,
            }
        )

log_result = pd.DataFrame(records)

# 作用：按日志级别统计件数
# 使用场景：快速确认 ERROR 和 WARN 数量
level_summary = (
    log_result.groupby("level", as_index=False)
    .agg(count=("message", "count"))
    .sort_values("count", ascending=False, ignore_index=True)
)

# 作用：导出异常明细
# 使用场景：作为障害调查附件
detail_file = output_dir / "error_summary.csv"
log_result.to_csv(detail_file, index=False, encoding="utf-8-sig")

# 作用：导出级别汇总
# 使用场景：快速查看 WARN / ERROR 件数
summary_file = output_dir / "level_summary.csv"
level_summary.to_csv(summary_file, index=False, encoding="utf-8-sig")

print(log_result.shape)  # 例如：(2, 5) 表示提取到 2 条 WARN / ERROR
print(detail_file.exists())  # 例如：True 表示 error_summary.csv 已生成
print(summary_file.exists())  # 例如：True 表示 level_summary.csv 已生成
```

运行后可以看到：

```text
automation_log_demo/
├── logs/
│   └── app_20260726.log
└── output/
    ├── error_summary.csv
    └── level_summary.csv
```

`error_summary.csv` 内容示例：

```text
level,order_id,user_id,api_path,message
ERROR,O202607260001,U002,/api/orders,2026-07-26 ... email=***@*** message=payment failed
WARN,O202607260002,U003,/api/customers,2026-07-26 ... message=response slow
```

## 十四、方法总结表

| 方法 | 作用 | 常用参数 / 写法 | 使用场景 |
| --- | --- | --- | --- |
| `read_text()` | 一次性读取文本 | `encoding`、`errors` | 小日志、配置、结果文件 |
| `open()` | 打开文件 | `"r"`、`encoding="utf-8"` | 大日志逐行处理 |
| `splitlines()` | 按行拆分字符串 | 无 | 小文本按行处理 |
| `re.search()` | 查找第一个匹配 | `pattern`、`string`、`flags` | 从一行日志提取字段 |
| `group()` | 取得匹配内容 | `group(0)`、`group(1)` | 读取捕获分组 |
| `re.findall()` | 提取所有匹配 | `pattern`、`string` | 从整段文本提取所有订单号 |
| `re.finditer()` | 遍历匹配对象 | `pattern`、`string` | 需要匹配位置或多个分组 |
| `re.sub()` | 替换文本 | `pattern`、`repl`、`string` | 脱敏邮箱、手机号、Token |
| `re.compile()` | 编译正则 | `pattern`、`flags` | 重复使用同一规则 |
| `to_csv()` | 导出结果 | `index=False`、`encoding="utf-8-sig"` | 输出日志调查结果 |

## 十五、本章练习

1. 创建 `automation_log_demo/logs/app_20260726.log`。
2. 写入至少 5 行日志，其中包含 `INFO`、`WARN`、`ERROR`。
3. 使用逐行读取筛选出 `ERROR` 和 `WARN`。
4. 使用 `re.search()` 提取 `order_id`。
5. 使用 `re.search()` 提取 `user_id` 和 `path`。
6. 使用 `re.sub()` 对邮箱地址脱敏。
7. 使用 `DataFrame` 保存提取结果。
8. 导出 `error_summary.csv`。
9. 增加一个 `level_summary.csv`，统计不同日志级别的件数。

