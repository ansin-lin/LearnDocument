# 第7章 异常、调试与上下文管理

> 本章目标：理解 Python 的异常机制，掌握 `try / except / else / finally`、主动抛出异常、自定义异常以及 `with` 上下文管理的基本用法，能够写出更稳定的代码。

## 前置知识

- 会使用函数、条件判断和文件操作
- 能看懂返回值和变量作用域
- 了解基础的程序执行流程

## 一、为什么要学异常

程序运行时不可能永远正确，必须考虑出错后的处理方式。

```python
def divide(a, b):
    return a / b

print(divide(10, 0))  # 会触发异常
```

如果没有异常处理，程序一旦出错就可能直接中断，影响后续业务。

## 二、什么是异常

异常是程序运行过程中出现的错误事件。  
当 Python 遇到无法继续正常执行的情况时，会抛出异常对象。

常见场景：

- 除零
- 类型不匹配
- 索引越界
- 文件不存在
- 网络请求失败

## 三、错误和异常的区别

### 3.1 错误

错误更偏向“程序写法有问题”或者“系统级问题”。

例如：

- 语法错误
- 缩进错误
- 变量名写错
- 代码逻辑本身写错

### 3.2 异常

异常更偏向“程序运行时出现的可预期非正常情况”。

例如：

- 除零
- 文件找不到
- 字典键不存在
- 类型转换失败

### 3.3 简单理解

- 错误：程序写法有问题，或者底层出现严重问题
- 异常：程序运行时发生了可以处理的问题

企业项目里，很多“错误”最终会表现为异常或报错信息，所以通常统一纳入异常机制处理。

## 四、异常分类

### 4.1 语法错误

语法错误在程序运行前就会被发现。

```python
if True
    print("hello")
```

这类问题连解释执行都过不去。

### 4.2 运行时异常

运行时异常是在程序执行过程中发生的。

常见类型如下：

| 异常类型 | 含义 | 常见场景 |
| --- | --- | --- |
| `ZeroDivisionError` | 除以 0 | 数学计算 |
| `ValueError` | 值不合法 | 字符串转数字 |
| `TypeError` | 类型不匹配 | 字符串和整数相加 |
| `IndexError` | 索引越界 | 列表取值 |
| `KeyError` | 键不存在 | 字典取值 |
| `FileNotFoundError` | 文件不存在 | 文件读取 |
| `AttributeError` | 属性不存在 | 调用对象不存在的方法 |
| `ImportError` | 导入失败 | 模块导入 |
| `NameError` | 变量未定义 | 变量名写错 |
| `OSError` | 操作系统错误 | 文件、路径、权限问题 |

## 五、常见异常示例

### 5.1 `ZeroDivisionError`

```python
print(10 / 0)
```

### 5.2 `ValueError`

```python
print(int("abc"))
```

### 5.3 `TypeError`

```python
print("10" + 5)
```

### 5.4 `IndexError`

```python
nums = [1, 2, 3]
print(nums[5])
```

### 5.5 `KeyError`

```python
user = {"name": "Tanaka"}
print(user["age"])
```

### 5.6 `FileNotFoundError`

```python
with open("not_exists.txt", "r", encoding="utf-8") as file:
    print(file.read())
```

### 5.7 `AttributeError`

```python
text = "hello"
print(text.not_exists())
```

## 六、try / except / else / finally

这是 Python 异常处理的核心结构。

### 6.1 基本语法

```python
try:
    # 可能出错的代码
    ...
except SomeError:
    # 捕获并处理异常
    ...
else:
    # 没有异常时执行
    ...
finally:
    # 不管有没有异常都会执行
    ...
```

### 6.2 完整示例

```python
def parse_int(text):
    try:
        number = int(text)
    except ValueError as e:
        print(f"转换失败: {e}")  # 转换失败: invalid literal for int() with base 10: 'abc'
        return None
    else:
        print("转换成功")  # 转换成功
        return number
    finally:
        print("执行结束")  # 一定会执行

print(parse_int("123"))  # 123
print(parse_int("abc"))  # None
```

### 6.3 各部分作用

- `try`：放可能出错的代码
- `except`：捕获并处理异常
- `else`：没有异常时才执行
- `finally`：无论如何都会执行，常用于收尾

## 七、捕获多个异常

### 7.1 分别处理

```python
try:
    value = int("abc")
    result = 10 / 0
except ValueError:
    print("类型转换失败")  # 类型转换失败
except ZeroDivisionError:
    print("不能除以 0")
```

### 7.2 统一处理

```python
try:
    value = int("abc")
except (ValueError, TypeError):
    print("转换失败")  # 转换失败
```

### 7.3 裸 `except`

虽然可以写裸 `except`，但不建议在企业项目里随便使用。

```python
try:
    ...
except:
    ...
```

原因是它会把很多问题一起吞掉，不利于排查。

## 八、异常传播与异常链

### 8.1 异常传播

如果函数内部没有捕获异常，异常会向外层调用者继续抛出。

```python
def inner():
    return 10 / 0

def outer():
    return inner()

outer()  # ZeroDivisionError
```

### 8.2 异常链

可以保留原始异常信息，方便排查。

```python
try:
    int("abc")
except ValueError as e:
    raise RuntimeError("转换用户输入失败") from e
```

## 九、主动抛出异常

当数据不符合要求时，可以主动抛出异常。

```python
def set_age(age):
    if age < 0:
        raise ValueError("年龄不能小于 0")
    return age

print(set_age(20))  # 20
```

主动抛出异常的作用：

- 提前终止错误流程
- 明确问题原因
- 让上层调用者决定如何处理

## 十、自定义异常

当内置异常不足以表达业务问题时，可以自定义异常。

```python
class LoginError(Exception):
    pass


def check_login(is_valid):
    if not is_valid:
        raise LoginError("登录失败")

check_login(True)
```

### 10.1 为什么要自定义异常

- 更贴近业务含义
- 更方便区分不同错误来源
- 更利于上层统一处理

### 10.2 企业中的使用方式

- 参数校验异常
- 业务规则异常
- 权限异常
- 状态异常

## 十一、调试基础

### 11.1 调试的目的

调试不是“找借口”，而是确认程序在每一步到底发生了什么。

### 11.2 常用调试手段

- `print()` 临时输出
- 断点调试
- 查看变量值
- 逐步执行

### 11.3 `print()` 调试示例

```python
def calc_total(price, count):
    print(f"price={price}, count={count}")
    total = price * count
    print(f"total={total}")
    return total

print(calc_total(10, 3))
# price=10, count=3
# total=30
# 30
```

### 11.4 调试时的注意点

- 调试输出不要长期留在正式代码里
- 复杂问题要看调用路径，不要只盯着单行代码
- 出错时先确认输入，再确认中间变量

## 十二、上下文管理与 `with`

### 12.1 什么是上下文管理

上下文管理是一种“进入时准备资源、退出时自动清理资源”的机制。

### 12.2 `with` 的作用

`with` 最常见的用途是自动关闭文件。

```python
with open("demo.txt", "r", encoding="utf-8") as file:
    content = file.read()
    print(content)
```

离开 `with` 代码块后，文件会自动关闭。

### 12.3 为什么不能把 `with` 只理解成异常处理

`with` 的核心是资源管理，不只是异常场景。

它常用于：

- 文件打开与关闭
- 锁的获取与释放
- 数据库连接的管理
- 临时资源的创建与清理

### 12.4 `with` 的企业价值

- 少写收尾代码
- 降低忘记关闭资源的风险
- 让资源管理更统一

## 十三、常见错误

- 只捕获异常，不分析原因
- 统一用裸 `except`
- 把业务校验全写在 `except` 里
- 错误发生后没有日志和上下文
- 误把 `with` 当成异常语法

## 十四、日本项目中的写法

- 异常信息要尽量清楚
- 处理异常时要说明影响范围
- 失败原因要能方便现场排查
- `finally` 和 `with` 常用于清理资源、关闭连接、释放文件句柄

## 十五、练习题

### 基础练习

1. 写一个函数，接收字符串并尝试转换成整数，转换失败时返回 `None`。
2. 写一个自定义异常 `InputError`。
3. 写一个使用 `with` 读取文本文件的示例。

### 综合练习

1. 编写一个函数，校验年龄是否在合理范围内，不合法时抛出异常。
2. 编写一个带调试输出的小函数，观察输入、处理中间结果和返回值。

## 十六、本章总结

- 异常是程序运行时必须处理的问题
- `try / except / else / finally` 是核心结构
- 主动抛出异常和自定义异常可以让业务规则更清晰
- `with` 的核心是上下文管理和资源清理
- 调试的目标是明确程序实际执行过程
