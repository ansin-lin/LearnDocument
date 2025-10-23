
# 第7章 常用数据结构之字符串

> 目标：全面掌握 Python 字符串（`str`）的定义、运算、方法与应用。

---

## 1. 引言

随着发展，计算机需要处理大量文本数据，**字符串（String）** 成为编程中的核心数据类型之一。

---

## 2. 字符串的定义与创建

- 字符串是由零个或多个字符组成的**有序、不可变**序列。
- 用单引号 `'...'`、双引号 `"..."`
  或三引号 `'''...'''` / `"""..."""` 创建。

```python
s1 = 'hello, world!'
s2 = "你好，世界！❤️"
s3 = '''hello,
wonderful
world!'''
print(s1)
print(s2)
print(s3)
```

---

## 3. 转义字符与原始字符串

常见转义：`\n` 换行、`\t` 制表、`\r` 回车、`\'` 单引号、`\"` 双引号、`\\` 反斜杠。

示例：

```python
s1 = '\'hello\''
s2 = '\\path\\to\\file'
print(s1)  # 'hello'
print(s2)  # \path\to\file
```

原始字符串（raw string）：以 `r` / `R` 前缀，禁用转义。

```python
s1 = '\t\n'     # 含转义
s2 = r'\t\n'    # 原样输出
print(s1, s2)
```

---

## 4. 字符特殊表示（八进制/十六进制/Unicode）

```python
s1 = '\141\142\143'     # 'abc'（八进制）
s2 = '\x61\x62\x63'     # 'abc'（十六进制）
s3 = '\u9a86\u660a'      # '骆昊'（Unicode）
print(s1, s2, s3)
```

---

## 5. 字符串运算

- 拼接：`+` 或 `''.join([...])`
- 重复：`*`
- 成员运算：`in` / `not in`
- 比较：按字符编码逐位比较

```python
s = 'hello'
print(s + ' world')     # 拼接
print('a' * 5)          # 重复
print('he' in s)        # True
print('A' < 'a')        # True
```

---

## 6. 索引与切片

- 正向索引从 0 开始，反向索引从 -1 开始。
- 切片 `s[start:end:step]`，不含 `end` 位置。

```python
s = 'abc123456'
print(s[0], s[-1])   # a 6
print(s[2:5])        # c12
print(s[::2])        # ac246
print(s[::-1])       # 654321cba
```

> 注意：字符串**不可变**，不能通过索引修改字符。

---

## 7. 遍历方式

```python
s = 'hello'
for i in range(len(s)):
    print(i, s[i])
for ch in s:
    print(ch)
```

---

## 8. 字符串方法大全（40+ 常用方法）

### 8.1 大小写与标题

| 方法 | 说明 | 示例 |
|---|---|---|
| `upper()` | 全大写 | `'py'.upper()` → `'PY'` |
| `lower()` | 全小写 | `'PY'.lower()` → `'py'` |
| `capitalize()` | 首字母大写 | `'hello'.capitalize()` → `'Hello'` |
| `title()` | 单词首字母大写 | `'hello world'.title()` |
| `swapcase()` | 大小写互换 | `'AbC'.swapcase()` |

### 8.2 查找与计数

| 方法 | 说明 | 示例 |
|---|---|---|
| `find(sub[, start[, end]])` | 找不到返回 -1 | `'hello'.find('l')` → 2 |
| `rfind(sub)` | 反向查找 | `'hello'.rfind('l')` → 3 |
| `index(sub)` | 找不到抛异常 | `'abc'.index('b')` → 1 |
| `rindex(sub)` | 反向 index | `'hello'.rindex('l')` → 3 |
| `count(sub)` | 子串出现次数 | `'banana'.count('a')` → 3 |

### 8.3 判断类（均返回 `bool`）

| 方法                   | 说明                       | 示例                                                                 |
| -------------------- | ------------------------ | ------------------------------------------------------------------ |
| `startswith(prefix)` | 判断字符串是否以指定前缀开头           | `'hello'.startswith('he') → True`                                  |
| `endswith(suffix)`   | 判断字符串是否以指定后缀结尾           | `'python.py'.endswith('.py') → True`                               |
| `isalnum()`          | 判断是否由字母和数字组成（至少一个字符）     | `'abc123'.isalnum() → True`；`'abc_123'.isalnum() → False`          |
| `isalpha()`          | 判断是否全为字母（至少一个字符）         | `'Hello'.isalpha() → True`；`'Hello123'.isalpha() → False`          |
| `isdigit()`          | 判断是否全为数字（适用于阿拉伯数字）       | `'12345'.isdigit() → True`；`'Ⅱ'.isdigit() → False`                 |
| `isdecimal()`        | 判断是否全为十进制数字（更严格，仅 0–9）   | `'123'.isdecimal() → True`；`'½'.isdecimal() → False`               |
| `isnumeric()`        | 判断是否全为数字字符（包含中文数字、罗马数字等） | `'一二三'.isnumeric() → True`；`'123'.isnumeric() → True`              |
| `islower()`          | 判断是否全为小写字母（且至少有一个字母）     | `'python'.islower() → True`；`'Python'.islower() → False`           |
| `isupper()`          | 判断是否全为大写字母（且至少有一个字母）     | `'ABC'.isupper() → True`；`'AbC'.isupper() → False`                 |
| `istitle()`          | 判断字符串是否为标题格式（单词首字母大写）    | `'Hello World'.istitle() → True`；`'Hello world'.istitle() → False` |
| `isspace()`          | 判断是否全为空白字符（空格、换行、制表符等）   | `'   '.isspace() → True`；`'a '.isspace() → False`                  |

### 8.4 替换与修剪

| 方法 | 说明 | 示例 |
|---|---|---|
| `replace(old, new[, count])` | 替换 | `'foo'.replace('o','@')` → `'f@@'` |
| `strip([chars])` | 去首尾 | `'  hi  '.strip()` → `'hi'` |
| `lstrip([chars])` / `rstrip([chars])` | 去左/右 | `'*hi*'.rstrip('*')` → `'*hi'` |

### 8.5 分割与拼接

| 方法 | 说明 | 示例 |
|---|---|---|
| `split(sep=None, maxsplit=-1)` | 从左拆 | `'a,b,c'.split(',')` |
| `rsplit(sep=None, maxsplit=-1)` | 从右拆 | `'a,b,c'.rsplit(',',1)` |
| `splitlines(keepends=False)` | 按行拆 | `'a\nb'.splitlines()` |
| `'sep'.join(iterable)` | 连接 | `'-'.join(['a','b'])` → `'a-b'` |

### 8.6 对齐与填充

| 方法 | 说明 | 示例 |
|---|---|---|
| `center(width[, fillchar])` | 居中 | `'hi'.center(6,'*')` → `'**hi**'` |
| `ljust(width[, fillchar])` | 左对齐 | `'hi'.ljust(5,'~')` |
| `rjust(width[, fillchar])` | 右对齐 | `'hi'.rjust(5)` |
| `zfill(width)` | 左补零 | `'7'.zfill(3)` → `'007'` |

### 8.7 制表与翻译

| 方法 | 说明 | 示例 |
|---|---|---|
| `expandtabs(tabsize=8)` | `\t` 展开为空格 | `'1\t2'.expandtabs(4)` |
| `maketrans(x, y, z)` + `translate(table)` | 批量替换/删除 | `'abc'.translate(str.maketrans('ab','12'))` → `'12c'` |

### 8.8 格式化

- `%` 风格：`'%d * %d = %d' % (a, b, a*b)`
- `str.format`：`'{0:.2f}'.format(3.14159)`
- f-string：`f'{a} * {b} = {a*b}'`

常用占位符：`{:.2f}`, `{:+.2f}`, `{:0>10d}`, `{:<10}`, `{:>10}`, `{:,}`, `{:.2%}`, `{:.2e}`。

---

## 9. 编码与解码

- `str.encode(encoding, errors='strict')` → `bytes`
- `bytes.decode(encoding, errors='strict')` → `str`

示例：

```python
    a = '骆昊'
    b = a.encode('utf-8')
    c = a.encode('gbk')
    print(b)                  # b'...'
    print(c)                  # b'...'
    print(b.decode('utf-8'))  # 骆昊
    print(c.decode('gbk'))    # 骆昊
```

> 编码/解码方式不一致会导致乱码或 `UnicodeDecodeError`。

---

## 10. 小结

- 字符串是最常用的容器之一，**不可变**且**可切片**。
- 掌握方法族、切片与格式化是文本处理的核心。
