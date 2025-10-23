# 第9章 Python 内置函数与标准库详解

> 🎯 本章目标：系统掌握 Python 的内置函数、字符串操作方法与标准库模块的核心功能与用法。

---

## 一、内置函数总览

Python 提供了超过 80 个内置函数，这些函数可以直接使用，无需导入任何模块。  
下表根据功能进行了分类整理。

### 🧮 数学与数值类

| 函数 | 说明 | 示例 |
|------|------|------|
| `abs(x)` | 绝对值 | `abs(-5)` → `5` |
| `pow(x, y)` | 幂运算 | `pow(2, 3)` → `8` |
| `round(x, n)` | 四舍五入，n为小数位 | `round(3.1415, 2)` → `3.14` |
| `divmod(a, b)` | 同时返回商与余数 | `divmod(7, 3)` → `(2, 1)` |
| `sum(iterable)` | 序列求和 | `sum([1, 2, 3])` → `6` |
| `min(iterable)` / `max(iterable)` | 最小/最大值 | `max([3,1,5])` → `5` |

---

### 🔍 逻辑与判断类

| 函数 | 说明 | 示例 |
|------|------|------|
| `all(iterable)` | 所有元素为真返回 True | `all([1,True,3])` → `True` |
| `any(iterable)` | 任一元素为真返回 True | `any([0,False,2])` → `True` |
| `bool(x)` | 转换为布尔类型 | `bool([])` → `False` |

---

### 📦 序列与迭代类

| 函数 | 说明 | 示例 |
|------|------|------|
| `len(obj)` | 返回长度 | `len('abc')` → `3` |
| `sorted(iterable)` | 返回排序后新列表 | `sorted([3,1,2])` → `[1,2,3]` |
| `reversed(seq)` | 返回反向迭代器 | `list(reversed('abc'))` → `['c','b','a']` |
| `enumerate(iterable)` | 返回索引与值对 | `for i,v in enumerate(['a','b']): print(i,v)` |
| `zip(a,b)` | 打包多个序列 | `list(zip([1,2],['a','b']))` → `[(1,'a'),(2,'b')]` |

---

### 🔠 类型转换类

| 函数 | 说明 | 示例 |
|------|------|------|
| `int(x)` / `float(x)` / `str(x)` | 转换数据类型 | `int('123')` → `123` |
| `list()`, `tuple()`, `set()`, `dict()` | 创建容器对象 | `list('abc')` → `['a','b','c']` |

---

### 🧰 对象与反射类

| 函数 | 说明 | 示例 |
|------|------|------|
| `type(obj)` | 查看类型 | `type(3.14)` |
| `isinstance(obj, cls)` | 检查对象类型 | `isinstance(5, int)` |
| `dir(obj)` | 列出对象属性和方法 | `dir(str)` |
| `id(obj)` | 返回唯一标识 | `id('abc')` |
| `hasattr(obj, name)` / `getattr()` / `setattr()` | 属性操作 | `getattr(obj,'name')` |

---

### 🧩 函数操作类

| 函数 | 说明 | 示例 |
|------|------|------|
| `callable(obj)` | 是否可调用 | `callable(len)` → `True` |
| `eval(expr)` | 执行字符串表达式 | `eval('2+3')` → `5` |
| `exec(code)` | 执行代码字符串 | `exec('x=10')` |
| `globals()` / `locals()` | 返回全局/局部命名空间 | `globals()` → 字典 |

---

## 二、标准模块分类汇总

### 1️⃣ 数学与统计类

| 模块 | 常用方法 | 示例 |
|------|-----------|-------|
| `math` | `sqrt`, `ceil`, `floor`, `pow`, `log`, `sin`, `cos` | `math.sqrt(16)` |
| `decimal` | `Decimal`, `getcontext()` | 精确浮点计算 |
| `fractions` | `Fraction`, `limit_denominator()` | 分数计算 |
| `statistics` | `mean`, `median`, `stdev` | 统计分析 |

### 2️⃣ 随机与时间

| 模块 | 方法 | 示例 |
|------|------|------|
| `random` | `randint`, `choice`, `sample`, `shuffle` | 随机抽样 |
| `datetime` | `datetime.now()`, `strftime`, `timedelta` | 日期时间 |
| `time` | `sleep`, `time`, `ctime` | 时间控制 |

### 3️⃣ 文件与系统

| 模块 | 方法 | 示例 |
|------|------|------|
| `os` | `listdir`, `getcwd`, `remove`, `rename` | 文件管理 |
| `sys` | `argv`, `path`, `exit` | 系统控制 |
| `shutil` | `copy`, `move`, `rmtree` | 文件复制与移动 |
| `pathlib` | `Path.exists()`, `iterdir()` | 面向对象文件路径 |

### 4️⃣ 数据结构与算法

| 模块 | 方法 | 示例 |
|------|------|------|
| `collections` | `Counter`, `deque`, `defaultdict`, `namedtuple` | 高级容器 |
| `itertools` | `permutations`, `combinations`, `chain` | 迭代器操作 |
| `heapq` | `heappush`, `heappop`, `nlargest` | 堆操作 |

### 5️⃣ 数据序列化

| 模块 | 方法 | 示例 |
|------|------|------|
| `json` | `dumps`, `loads` | JSON 编解码 |
| `pickle` | `dump`, `load` | Python 对象序列化 |
| `csv` | `reader`, `writer`, `DictReader` | 处理 CSV 文件 |

### 6️⃣ 文本与正则

| 模块 | 方法 | 示例 |
|------|------|------|
| `re` | `findall`, `sub`, `match` | 正则匹配 |
| `string` | `ascii_letters`, `digits`, `Template` | 文本常量 |
| `textwrap` | `fill`, `shorten` | 文本换行 |

### 7️⃣ 调试与性能

| 模块 | 方法 | 示例 |
|------|------|------|
| `logging` | `info`, `warning`, `error` | 日志记录 |
| `traceback` | `print_exc` | 错误堆栈 |
| `timeit` | `timeit()` | 性能分析 |

### 8️⃣ 并发与异步

| 模块 | 方法 | 示例 |
|------|------|------|
| `threading` | `Thread`, `Lock` | 多线程 |
| `multiprocessing` | `Process`, `Queue`, `Pool` | 多进程 |
| `asyncio` | `run`, `create_task`, `sleep` | 异步编程 |

---

## 三、模块与包结构

```python
# mypkg/__init__.py
# mypkg/util.py

import mypkg.util as util
print(util.say_hello())
```

---
