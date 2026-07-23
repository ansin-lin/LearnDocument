# 第9章 模块、包与标准库

> 本章目标：理解 Python 如何拆分代码、如何导入模块与包，以及标准库在企业项目中的常见用途，能够按业务场景选择合适的标准库。

## 前置知识

- 变量、函数、列表、字典、字符串
- `import` 的基本概念
- 能看懂简单的 Python 文件结构

## 一、为什么要学模块、包和标准库

- 项目代码不可能都写在一个文件里
- 把功能拆开以后，代码更容易维护、复用、测试
- 标准库可以直接用，很多基础能力不需要额外安装第三方包
- 日本项目里常见的日志、路径、日期、CSV、JSON、正则处理，标准库就能覆盖很多

## 二、模块与包

### 2.1 什么是模块

模块就是一个 `.py` 文件。

```python
# math_tool.py
def add(a, b):
    return a + b
```

使用模块：

```python
import math_tool

print(math_tool.add(1, 2))  # 3
```

### 2.2 什么是包

包是一个包含多个模块的目录，通常目录下会有 `__init__.py`。

```text
project/
├── main.py
├── utils/
│   ├── __init__.py
│   ├── date_utils.py
│   └── file_utils.py
└── services/
    ├── __init__.py
    └── order_service.py
```

### 2.3 常见导入方式

| 写法 | 作用 | 示例 |
| --- | --- | --- |
| `import 模块名` | 导入整个模块 | `import math` |
| `from 模块名 import 函数名` | 只导入需要的内容 | `from math import sqrt` |
| `import 模块名 as 别名` | 给模块起别名 | `import datetime as dt` |
| `from 模块名 import 函数名 as 别名` | 给函数起别名 | `from math import sqrt as root` |

### 2.4 导入示例

```python
import math
from math import sqrt

print(math.sqrt(16))  # 4.0
print(sqrt(25))       # 5.0
```

### 2.5 企业项目中的使用习惯

- `utils`：放通用工具函数
- `config`：放配置
- `services`：放业务逻辑
- `models`：放数据结构或实体对象

## 三、标准库总览

Python 标准库是安装 Python 后默认就能使用的功能集合。

常见分类：

- 路径与文件：`pathlib`、`os`、`shutil`
- 日期时间：`datetime`、`time`
- 数据格式：`json`、`csv`
- 文本处理：`re`
- 日志：`logging`
- 数学与随机：`math`、`random`
- 运行环境：`sys`

## 四、标准库选择建议

先记住这条顺序：

1. 先选最贴近业务的标准库
2. 再选更现代、更易读的写法
3. 最后才看底层或兼容性工具

### 4.1 常用优先级

| 场景 | 优先选择 | 原因 |
| --- | --- | --- |
| 路径处理 | `pathlib` | 对象化、可读性高、跨平台更清晰 |
| 日期时间 | `datetime` | 功能完整，适合业务时间处理 |
| 文件格式 | `json`、`csv` | Web 和批处理都常见 |
| 文本校验 | `re` | 适合模式匹配和提取 |
| 日志记录 | `logging` | 企业项目基础能力 |
| 简单数学 | `math` | 现成函数足够用 |
| 随机数据 | `random` | 测试、抽样、验证码常用 |
| 底层环境 | `os`、`sys`、`shutil` | 更偏工具和兼容性补充 |

11. `sys`
12. `shutil`


## 五、pathlib

**作用：** 以对象方式处理路径，比字符串拼接更安全、更清晰。  
强烈建议在新项目中优先使用 `pathlib`。

| 方法 | 作用 | 示例 |
| --- | --- | --- |
| `Path()` | 创建路径对象 | `Path("data")` |
| `/` | 拼接路径 | `Path("a") / "b.txt"` |
| `exists()` | 是否存在 | `path.exists()` |
| `is_file()` | 是否是文件 | `path.is_file()` |
| `is_dir()` | 是否是目录 | `path.is_dir()` |
| `read_text()` | 读取文本 | `path.read_text(encoding="utf-8")` |
| `write_text()` | 写入文本 | `path.write_text("hi", encoding="utf-8")` |

```python
from pathlib import Path

path = Path("data") / "input.csv"
print(path)  # data\input.csv 或 data/input.csv
```

## 六、datetime

**作用：** 日期、时间、时间差、字符串日期转换。  
Web 项目中很常见，比如订单时间、创建时间、更新时间、定时任务。

| 方法 / 类 | 作用 | 示例 |
| --- | --- | --- |
| `datetime.now()` | 当前日期时间 | `datetime.now()` |
| `date.today()` | 当前日期 | `date.today()` |
| `timedelta` | 时间差 | `timedelta(days=1)` |
| `strftime()` | 日期转字符串 | `dt.strftime("%Y-%m-%d")` |
| `strptime()` | 字符串转日期 | `datetime.strptime(text, fmt)` |

```python
from datetime import datetime, date, timedelta

now = datetime.now()
print(now)  # 2026-07-10 10:30:00.123456
print(now.strftime("%Y-%m-%d"))  # 2026-07-10
print(now + timedelta(days=1))  # 2026-07-11 10:30:00.123456

text = "2026-07-10 13:45:00"
dt = datetime.strptime(text, "%Y-%m-%d %H:%M:%S")
print(dt)  # 2026-07-10 13:45:00
print(date.today())  # 2026-07-10
```

## 七、time

**作用：** 时间戳、暂停、系统时间字符串、简单耗时处理。  
常用于接口耗时统计、脚本等待、时间戳转换。

| 函数 | 作用 | 示例 |
| --- | --- | --- |
| `time()` | 当前时间戳 | `time.time()` |
| `sleep()` | 暂停执行 | `time.sleep(1)` |
| `ctime()` | 当前时间字符串 | `time.ctime()` |

```python
import time

print(time.time())  # 例如 1720500000.123456
print(time.ctime())  # 例如 Thu Jul 10 10:30:00 2026
```

## 八、json

**作用：** JSON 数据序列化和反序列化。  
Web 项目中非常常用，接口请求和响应基本都会碰到。

| 方法 | 作用 | 示例 |
| --- | --- | --- |
| `dumps()` | Python 转 JSON 字符串 | `json.dumps(data)` |
| `loads()` | JSON 字符串转 Python | `json.loads(text)` |
| `dump()` | 写入 JSON 文件 | `json.dump(data, file)` |
| `load()` | 读取 JSON 文件 | `json.load(file)` |

```python
import json

data = {"name": "Tanaka", "age": 28}
print(json.dumps(data, ensure_ascii=False))  # {"name": "Tanaka", "age": 28}
```

### 8.1 `dump()` 与 `indent`

`indent` 用来控制 JSON 输出的缩进层级，方便阅读。

```python
import json

data = {"name": "Tanaka", "age": 28}

print(json.dumps(data, ensure_ascii=False, indent=2))
# {
#   "name": "Tanaka",
#   "age": 28
# }
```

## 九、csv

**作用：** CSV 文件读写。  
日本企业里导入导出、报表、批处理经常会用到。

| 方法 / 类 | 作用 | 示例 |
| --- | --- | --- |
| `reader()` | 读取 CSV | `csv.reader(file)` |
| `writer()` | 写入 CSV | `csv.writer(file)` |
| `DictReader()` | 读取成字典 | `csv.DictReader(file)` |
| `DictWriter()` | 写入字典形式 CSV | `csv.DictWriter(file, fieldnames=...)` |

## 十、re

**作用：** 用正则表达式按“模式”查找、验证、提取和替换文本。  
在企业项目里常用于手机号、邮箱、日期格式校验，也常用于日志解析、批量文本清洗、字段提取。

正则表达式可以理解为“文本匹配规则”。它不是固定字符串匹配，而是用一套简短语法描述一类文本。

**常用符号**

| 符号 | 含义 | 示例 |
| --- | --- | --- |
| `\d` | 数字 | `\d+` |
| `\w` | 字母、数字、下划线 | `\w+` |
| `.` | 任意字符 | `a.c` |
| `+` | 1 次或多次 | `\d+` |
| `*` | 0 次或多次 | `\s*` |
| `?` | 0 次或 1 次 | `colou?r` |
| `{n}` | 恰好 n 次 | `\d{4}` |
| `{n,m}` | n 到 m 次 | `\d{2,4}` |
| `^` | 开头 | `^abc` |
| `$` | 结尾 | `abc$` |
| `[]` | 字符集合 | `[abc]` |
| `()` | 分组 | `(\d{4})-(\d{2})-(\d{2})` |

**常用方法表**

| 方法 | 作用 | 说明 |
| --- | --- | --- |
| `match()` | 从开头匹配 | 只检查字符串开头 |
| `search()` | 查找第一次匹配 | 在整段文本中找第一个匹配 |
| `findall()` | 查找所有匹配 | 返回所有匹配结果 |
| `sub()` | 替换文本 | 按规则替换内容 |
| `split()` | 按规则分割 | 用正则切分字符串 |
| `fullmatch()` | 整串匹配 | 整个字符串都符合才成功 |

```python
import re

text = "Order No: 20260710-001"
pattern = r"\d{8}-\d{3}"

match = re.search(pattern, text)
if match:
    print(match.group())  # 20260710-001
```

**示例 1：校验邮箱格式**

```python
import re

email = "tanaka@example.com"
pattern = r"^[\w.-]+@[\w.-]+\.[a-zA-Z]{2,}$"

print(bool(re.fullmatch(pattern, email)))  # True
```

**示例 2：提取日志中的日期和订单号**

```python
import re

log_text = "2026-07-10 10:30:21 [INFO] order_id=ORD-1024 status=ok"

date_result = re.search(r"\d{4}-\d{2}-\d{2}", log_text)
order_result = re.search(r"order_id=(ORD-\d+)", log_text)

print(date_result.group())   # 2026-07-10
print(order_result.group(1)) # ORD-1024
```

**企业项目中的使用场景**

- 表单校验：手机号、邮箱、日期格式
- 日志解析：提取时间、级别、订单号、错误码
- 文本清洗：去掉多余空格、统一分隔符
- 批量替换：把旧字段名替换成新字段名

## 十一、logging

**作用：** 记录程序运行过程、错误信息、排查线索和业务关键事件。  
在企业项目里，日志不是可选项，而是基础设施的一部分。没有日志，出问题后很难定位。

`logging` 的三个核心概念：

- `logger`：日志入口，负责发出日志
- `handler`：日志输出目标，决定日志写到哪里
- `formatter`：日志格式，决定日志长什么样

### 11.1 logger、handler、formatter 的关系

| 名称 | 作用 | 说明 |
| --- | --- | --- |
| `logger` | 发出日志 | 业务代码通常调用它 |
| `handler` | 接收并输出日志 | 可以输出到控制台、文件等 |
| `formatter` | 格式化日志内容 | 决定时间、级别、模块名、消息的展示方式 |

可以理解为：

- `logger` 负责“说什么”
- `handler` 负责“说到哪里”
- `formatter` 负责“怎么说”

### 11.2 日志级别

| 级别 | 说明 | 常见用途 |
| --- | --- | --- |
| `DEBUG` | 调试信息 | 开发阶段排查问题 |
| `INFO` | 普通信息 | 记录业务流程 |
| `WARNING` | 警告信息 | 出现异常但程序还能继续 |
| `ERROR` | 错误信息 | 功能失败，需要关注 |
| `CRITICAL` | 严重错误 | 系统级严重故障 |

### 11.3 常用配置对象

| 对象 / 方法 | 作用 | 示例 |
| --- | --- | --- |
| `getLogger()` | 获取 logger 对象 | `logging.getLogger(__name__)` |
| `StreamHandler()` | 输出到控制台 | `logging.StreamHandler()` |
| `FileHandler()` | 输出到文件 | `logging.FileHandler("app.log", encoding="utf-8")` |
| `Formatter()` | 设置格式 | `logging.Formatter(fmt)` |
| `setLevel()` | 设置级别 | `logger.setLevel(logging.INFO)` |
| `addHandler()` | 绑定输出目标 | `logger.addHandler(handler)` |
| `setFormatter()` | 绑定格式器 | `handler.setFormatter(formatter)` |

### 11.4 企业级用法示例

下面示例演示一个常见的企业写法：

- 同时输出到控制台和文件
- 文件里保留完整日志
- 控制台只看关键日志
- 每行都加上时间、级别、文件名、行号

```python
import logging  # 导入日志模块

logger = logging.getLogger(__name__)  # 创建当前模块的 logger
logger.setLevel(logging.INFO)  # 设置 logger 的最低输出级别

console_handler = logging.StreamHandler()  # 创建控制台输出器
console_handler.setLevel(logging.INFO)  # 控制台只输出 INFO 及以上日志

file_handler = logging.FileHandler("app.log", encoding="utf-8")  # 创建文件输出器
file_handler.setLevel(logging.DEBUG)  # 文件记录更完整的日志

formatter = logging.Formatter(  # 创建日志格式器
    "%(asctime)s - %(levelname)s - %(name)s - %(filename)s:%(lineno)d - %(message)s"
)  # 日志格式包含时间、级别、模块名、文件名、行号和消息

console_handler.setFormatter(formatter)  # 给控制台输出器设置格式
file_handler.setFormatter(formatter)  # 给文件输出器设置格式

logger.addHandler(console_handler)  # 把控制台输出器挂到 logger 上
logger.addHandler(file_handler)  # 把文件输出器挂到 logger 上

logger.debug("debug message")  # 这条不会输出，因为 logger 级别是 INFO
logger.info("start order import")  # 输出业务开始日志
logger.warning("csv row is missing order_id")  # 输出警告日志
logger.error("order save failed")  # 输出错误日志
```

### 11.5 业务场景示例

下面示例模拟订单导入流程。  
重点看日志在不同步骤中的作用。

```python
import logging  # 导入日志模块

logger = logging.getLogger("order_import")  # 创建订单导入专用 logger
logger.setLevel(logging.INFO)  # 设置最低输出级别

handler = logging.StreamHandler()  # 创建控制台输出器
formatter = logging.Formatter("%(levelname)s - %(message)s")  # 定义简单格式
handler.setFormatter(formatter)  # 设置输出格式
logger.addHandler(handler)  # 绑定输出器

def import_order(order_id, amount):  # 定义订单导入函数
    logger.info(f"import start: order_id={order_id}")  # 记录开始导入

    if not order_id:  # 如果订单号为空
        logger.error("order_id is empty")  # 记录错误日志
        return False  # 返回失败

    if amount <= 0:  # 如果金额非法
        logger.warning(f"invalid amount: {amount}")  # 记录警告日志
        return False  # 返回失败

    logger.info(f"import success: order_id={order_id}, amount={amount}")  # 记录成功日志
    return True  # 返回成功

import_order("ORD-1001", 1200)  # 正常导入
import_order("", 1200)  # 订单号为空
import_order("ORD-1002", -50)  # 金额非法
```

### 11.6 企业项目中的使用场景

- Web 接口请求记录
- 批处理任务执行记录
- CSV 导入导出记录
- 权限校验和异常追踪
- 调用外部接口失败时记录上下文

## 十二、math

**作用：** 数学计算、数值处理、取整、组合数、距离判断。  
在 Web 项目和数据分析中很常用，比如金额计算、分页计算、比例处理、统计运算。

| 函数 | 作用 | 示例 |
| --- | --- | --- |
| `sqrt()` | 开平方 | `math.sqrt(16)` |
| `ceil()` | 向上取整 | `math.ceil(1.2)` |
| `floor()` | 向下取整 | `math.floor(1.8)` |
| `trunc()` | 截断小数 | `math.trunc(1.8)` |
| `fabs()` | 取绝对值 | `math.fabs(-3.5)` |
| `gcd()` | 最大公约数 | `math.gcd(12, 18)` |
| `lcm()` | 最小公倍数 | `math.lcm(12, 18)` |
| `isclose()` | 浮点数近似比较 | `math.isclose(0.1 + 0.2, 0.3)` |
| `comb()` | 组合数 | `math.comb(5, 2)` |
| `perm()` | 排列数 | `math.perm(5, 2)` |

```python
import math

print(math.sqrt(16))   # 4.0
print(math.ceil(1.2))   # 2
print(math.floor(1.8))  # 1
print(math.trunc(1.8))  # 1
print(math.fabs(-3.5))  # 3.5
print(math.gcd(12, 18)) # 6
print(math.isclose(0.1 + 0.2, 0.3))  # True
```

## 十三、random

**作用：** 生成随机数、随机选择、随机打乱。  
常用于验证码、抽奖、测试数据、样本抽样。

| 函数 | 作用 | 示例 |
| --- | --- | --- |
| `randint()` | 随机整数 | `random.randint(1, 10)` |
| `random()` | 随机小数 | `random.random()` |
| `choice()` | 随机选一个 | `random.choice(items)` |
| `choices()` | 随机选多个 | `random.choices(items, k=2)` |
| `shuffle()` | 打乱列表 | `random.shuffle(items)` |
| `sample()` | 不重复抽样 | `random.sample(items, 2)` |

```python
import random

print(random.randint(1, 10))  # 1 到 10 的随机整数
print(random.choice(["A", "B", "C"]))  # 随机输出 A / B / C 其中一个
```

## 十四、os

**作用：** 路径、目录、环境变量、进程相关基础操作。  
适合做跨平台脚本和简单运维工具。

| 方法 | 作用 | 示例 |
| --- | --- | --- |
| `getcwd()` | 获取当前目录 | `os.getcwd()` |
| `listdir()` | 列出目录内容 | `os.listdir(".")` |
| `mkdir()` | 创建目录 | `os.mkdir("tmp")` |
| `remove()` | 删除文件 | `os.remove("a.txt")` |
| `rename()` | 重命名文件 | `os.rename("a.txt", "b.txt")` |

```python
import os

print(os.getcwd())   # 当前工作目录
print(os.listdir("."))  # 当前目录下的文件和文件夹
```

## 十五、sys

**作用：** 访问解释器、命令行参数、模块搜索路径、退出程序。

| 属性 / 方法 | 作用 | 示例 |
| --- | --- | --- |
| `argv` | 命令行参数 | `sys.argv` |
| `path` | 模块搜索路径 | `sys.path` |
| `exit()` | 退出程序 | `sys.exit()` |

```python
import sys

print(sys.argv)  # 命令行参数列表
```

## 十六、shutil

**作用：** 高级文件操作，比如复制、移动、删除目录。

| 方法 | 作用 | 示例 |
| --- | --- | --- |
| `copy()` | 复制文件 | `shutil.copy(src, dst)` |
| `copy2()` | 复制文件并保留元数据 | `shutil.copy2(src, dst)` |
| `move()` | 移动文件 | `shutil.move(src, dst)` |
| `rmtree()` | 删除目录 | `shutil.rmtree(path)` |

## 十七、标准库使用示例

```python
from datetime import datetime
from pathlib import Path
import json

now = datetime.now()
path = Path("logs") / "app.json"
data = {"time": now.strftime("%Y-%m-%d %H:%M:%S")}

print(json.dumps(data, ensure_ascii=False))  # {"time": "2026-07-10 10:30:00"}
print(path)  # logs/app.json
```

## 十八、日本项目中的使用习惯

- 路径处理优先使用 `pathlib`
- 日期时间处理优先使用 `datetime`
- 日志记录优先使用 `logging`
- JSON / CSV 是最常见的外部数据交换格式
- 正则常用于输入校验和日志解析

## 十九、Coding Rule

- 模块命名清晰
- 导入顺序统一
- 优先使用标准库解决基础问题
- `pathlib` 优先于字符串拼接路径
- 正则表达式要写注释，避免只有作者自己看得懂

## 二十、Code Review 关注点

- 是否重复导入
- 是否把标准库写成了硬编码字符串
- 是否应该用 `pathlib` 替代 `os.path`
- 正则是否过于复杂
- 是否缺少异常处理
- 是否缺少日志输出

## 二十一、常见错误

- 把模块名和包名混用
- 路径字符串直接拼接
- 正则写得太宽松，导致误匹配
- 正则写得太严格，导致合法数据被拒绝
- 忽略编码问题

## 二十二、最佳实践

- 模块职责单一
- 包按业务分层
- 常见路径和日期处理用标准库
- 正则只解决“文本模式匹配”问题，不要滥用
- 日志等级要合理

## 二十三、面试高频问题

1. 什么是模块，什么是包？
2. `import` 和 `from ... import ...` 有什么区别？
3. 为什么推荐使用 `pathlib`？

## 二十四、本章总结

- 模块和包用于组织代码
- 标准库提供了大量即用能力
- 学习标准库时要先按业务优先级排序
- `pathlib`、`datetime`、`json`、`csv`、`re`、`logging` 是企业项目高频内容
- `os`、`sys`、`shutil` 更偏底层和工具场景
