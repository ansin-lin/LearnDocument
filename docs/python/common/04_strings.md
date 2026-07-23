# 第4章 字符串与编码

> 本章目标：掌握字符串的创建、索引、切片、常用方法、格式化输出以及编码与解码的基本用法，能够处理企业项目中的文本数据。

## 前置知识

- 会使用变量、类型、运算符和容器
- 了解条件判断和循环
- 知道“文本”和“编码”是不同概念

## 一、为什么要学字符串

- 字符串是最常见的数据类型之一
- 用户输入、文件内容、日志、接口参数本质上都可能是字符串
- 企业开发中，字符串处理几乎每天都会遇到

常见场景：

- 用户名、密码、邮箱、手机号
- 文件路径、URL、SQL 片段
- 日志内容、接口返回值
- 编码转换、格式清洗、数据拆分

## 二、字符串是什么

- 字符串是文本数据
- 字符串是有序的
- 字符串是不可变对象，修改时通常会生成新字符串

```python
text = "Python"
print(text[0])  # P
```

## 三、字符串的创建方式

### 3.1 单引号和双引号

```python
a = "hello"
b = 'hello'

print(a)  # hello
print(b)  # hello
```

单引号和双引号在大多数场景下没有本质区别，主要看哪种写法更方便。

### 3.2 三引号

三引号适合多行文本、说明文本和 SQL 片段。

```python
message = """第一行
第二行"""

print(message)
# 第一行
# 第二行
```

### 3.3 原始字符串

原始字符串会把反斜杠当作普通字符，常用于 Windows 路径和正则表达式。

```python
path = r"D:\project\demo.txt"
print(path)  # D:\project\demo.txt
```

## 四、转义字符

当字符串里需要表示特殊符号时，可以使用转义字符。

| 转义字符 | 含义 | 示例 |
| --- | --- | --- |
| `\n` | 换行 | `"a\nb"` |
| `\t` | 制表符 | `"a\tb"` |
| `\\` | 反斜杠 | `"C:\\temp"` |
| `\'` | 单引号 | `'it\'s'` |
| `\"` | 双引号 | `"He said \"Hi\""` |

```python
print("Hello\nPython")
# Hello
# Python

print("A\tB")      # A       B
print("C:\\temp")  # C:\temp
```

## 五、索引与切片

### 5.1 索引

```python
s = "abcdef"

print(s[0])   # a
print(s[3])   # d
print(s[-1])  # f
print(s[-2])  # e
```

### 5.2 切片

切片格式：

```text
字符串[开始位置:结束位置:步长]
```

注意：

- 开始位置包含
- 结束位置不包含
- 步长默认是 `1`

```python
s = "abcdef"

print(s[1:4])   # bcd
print(s[:3])    # abc
print(s[3:])    # def
print(s[::2])   # ace
print(s[::-1])  # fedcba
```

## 六、字符串的基本运算

### 6.1 拼接

```python
first = "Hello"
second = "Python"

print(first + " " + second)  # Hello Python
```

### 6.2 重复

```python
print("ha" * 3)  # hahaha
```

### 6.3 成员判断

```python
text = "Python"

print("Py" in text)    # True
print("Java" in text)  # False
```

### 6.4 长度

```python
text = "Python"

print(len(text))  # 6
```

## 七、常用字符串方法

| 方法 | 作用 | 示例 | 典型场景 |
| --- | --- | --- | --- |
| `strip()` | 去掉首尾空白 | `text.strip()` | 输入清洗 |
| `lstrip()` | 去掉左侧空白 | `text.lstrip()` | 左侧处理 |
| `rstrip()` | 去掉右侧空白 | `text.rstrip()` | 右侧处理 |
| `lower()` | 转小写 | `text.lower()` | 统一大小写 |
| `upper()` | 转大写 | `text.upper()` | 统一大小写 |
| `capitalize()` | 首字母大写 | `text.capitalize()` | 文本首字母处理 |
| `title()` | 每个单词首字母大写 | `text.title()` | 英文标题处理 |
| `swapcase()` | 大小写互换 | `text.swapcase()` | 特殊文本处理 |
| `find()` | 查找子串，找不到返回 `-1` | `text.find("py")` | 安全查找 |
| `rfind()` | 从右侧查找 | `text.rfind("py")` | 反向查找 |
| `index()` | 查找子串，找不到报错 | `text.index("py")` | 必须存在时 |
| `rindex()` | 从右侧查找并报错 | `text.rindex("py")` | 反向精确查找 |
| `replace()` | 替换文本 | `text.replace("a", "b")` | 文本替换 |
| `split()` | 按分隔符拆分 | `text.split(",")` | CSV 风格拆分 |
| `rsplit()` | 从右侧拆分 | `text.rsplit(",", 1)` | 文件名、路径拆分 |
| `join()` | 拼接序列 | `"-".join(parts)` | 字符串拼接 |
| `startswith()` | 判断前缀 | `text.startswith("py")` | 文件名、URL 判断 |
| `endswith()` | 判断后缀 | `text.endswith(".py")` | 扩展名判断 |
| `isalpha()` | 是否全字母 | `text.isalpha()` | 字母校验 |
| `isdigit()` | 是否全数字 | `text.isdigit()` | 数字校验 |
| `isalnum()` | 是否字母或数字 | `text.isalnum()` | 编号校验 |
| `isspace()` | 是否全空白 | `text.isspace()` | 空白判断 |
| `count()` | 统计出现次数 | `text.count("a")` | 计数 |
| `center()` | 居中 | `text.center(10, "*")` | 输出对齐 |
| `ljust()` | 左对齐 | `text.ljust(10, "-")` | 输出对齐 |
| `rjust()` | 右对齐 | `text.rjust(10, "-")` | 输出对齐 |
| `zfill()` | 左侧补零 | `text.zfill(6)` | 编号格式化 |
| `removeprefix()` | 删除前缀 | `text.removeprefix("pre")` | Python 3.9+ |
| `removesuffix()` | 删除后缀 | `text.removesuffix("txt")` | Python 3.9+ |

### 7.1 去空白

```python
text = "  python web  "

print(text.strip())   # python web
print(text.lstrip())  # python web  
print(text.rstrip())  #   python web
```

### 7.2 大小写转换

```python
text = "Python Web"

print(text.lower())  # python web
print(text.upper())  # PYTHON WEB
print(text.title())  # Python Web
```

### 7.3 查找与判断

```python
text = "hello python"

print(text.find("python"))     # 6
print(text.find("java"))       # -1
print(text.startswith("he"))   # True
print(text.endswith("on"))     # True
```

### 7.4 替换

```python
text = "hello python"

print(text.replace("python", "web"))  # hello web
```

### 7.5 拆分与拼接

```python
text = "a,b,c"
parts = text.split(",")

print(parts)           # ['a', 'b', 'c']
print("-".join(parts)) # a-b-c
```

### 7.6 判断类方法

```python
text = "Python123"

print(text.isalpha())  # False
print(text.isdigit())   # False
print(text.isalnum())   # True
```

### 7.7 对齐和补零

```python
print("hi".center(6, "*"))  # **hi**
print("hi".ljust(6, "-"))    # hi----
print("hi".rjust(6, "-"))    # ----hi
print("7".zfill(3))          # 007
```

## 八、字符串格式化输出

字符串格式化是把变量按指定格式放进字符串中。企业项目里经常用于日志、报表、接口输出和提示信息。

### 8.1 f-string

这是 Python 中最推荐的写法，清晰且可读性高。

```python
name = "Tanaka"
age = 28

print(f"{name} is {age} years old")  # Tanaka is 28 years old
```

### 8.2 `%` 格式化

```python
name = "Tanaka"
age = 28

print("%s is %d years old" % (name, age))  # Tanaka is 28 years old
```

### 8.3 `str.format()`

```python
name = "Tanaka"
age = 28

print("{} is {} years old".format(name, age))  # Tanaka is 28 years old
```

### 8.4 格式化常见用法

```python
price = 12.3456
count = 5

print(f"{price:.2f}")   # 12.35
print(f"{count:04d}")   # 0005
```

## 九、字符串与编码

### 9.1 什么是编码

- 字符串是人能看懂的文本
- 编码是把文本转换成字节的方法
- 解码是把字节还原成文本的方法

Python 中常见的编码有：

- `utf-8`
- `gbk`
- `ascii`

### 9.2 编码与解码

```python
text = "你好"

data = text.encode("utf-8")
print(data)  # b'\xe4\xbd\xa0\xe5\xa5\xbd'

restored = data.decode("utf-8")
print(restored)  # 你好
```

### 9.3 企业项目中的注意点

- 读取文本文件时，通常要明确指定编码
- 写入文件时，也要明确指定编码
- 不同系统之间传输文本时，要注意编码一致

### 9.4 乱码的本质

乱码通常不是“字符串坏了”，而是：

- 编码方式和解码方式不一致
- 文件保存编码和读取编码不一致

`ensure_ascii=False` 只能影响 JSON 输出形式，不能修复已经乱码的数据。

## 十、日本项目中的写法

- 字符串清洗要明确处理空白、前后缀和特殊字符
- 文件名、URL、状态文案建议使用统一命名规则
- 外部输入进入系统前，先做编码和格式检查
- 日志和提示信息尽量便于现场排查

## 十一、常见错误

- 把字符串当成可变对象直接修改
- 切片下标写错
- 误把 `find()` 和 `index()` 混用
- 读取文件时不写编码
- 误以为 `ensure_ascii=False` 可以修复乱码

## 十二、本章总结

- 字符串是文本处理的核心
- 索引、切片和常用方法是基础能力
- 格式化输出是企业项目中的常用写法
- 编码与解码是处理文件和接口数据时必须掌握的内容
