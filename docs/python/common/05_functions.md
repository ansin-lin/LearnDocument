# 第5章 函数与类型提示

> 本章目标：掌握函数的定义、参数、返回值、作用域、`lambda`、高阶函数、闭包和装饰器的基本用法，能够写出可复用的企业项目函数。

## 前置知识

- 会使用变量、条件、循环和容器
- 能看懂字符串和字典的基本操作
- 了解 `list`、`dict` 等常见数据结构

## 一、为什么要学习函数

- 避免重复代码
- 提高复用性
- 便于测试和维护
- 便于多人协作

在企业项目里，函数通常承担这些职责：

- 处理输入数据
- 封装校验逻辑
- 组织业务步骤
- 生成统一结果

## 二、函数是什么

函数是一段可复用的代码块，用来完成一个相对独立的任务。

```python
def greet(name):
    return f"Hello, {name}!"

print(greet("Python"))  # Hello, Python!
```

函数的基本特点：

- 有名称
- 可以接收参数
- 可以返回结果
- 可以重复调用

## 三、函数的基本语法

```python
def function_name(parameters):
    # 函数体
    return result
```

例如：

```python
def add(a, b):
    result = a + b
    return result

print(add(2, 3))  # 5
```

## 四、返回值

### 4.1 返回一个值

```python
def square(number):
    return number * number

print(square(4))  # 16
```

### 4.2 返回多个值

Python 返回多个值时，底层本质上是返回一个元组。

```python
def calc(a, b):
    return a + b, a - b, a * b, a / b

result = calc(10, 2)
print(result)  # (12, 8, 20, 5.0)
```

### 4.3 没有 return 的情况

函数没有写 `return` 时，默认返回 `None`。

```python
def show_message():
    print("hello")

result = show_message()
print(result)  # None
```

## 五、参数体系

参数决定函数“接收什么数据”和“如何调用”。这是函数章节的核心。

### 5.1 位置参数

按照传入顺序匹配参数。

```python
def greet(name, message):
    print(name, message)

greet("Tanaka", "こんにちは")  # Tanaka こんにちは
```

### 5.2 关键字参数

按参数名传值，顺序可以变化。

```python
def greet(name, message):
    print(name, message)

greet(message="こんにちは", name="Tanaka")  # Tanaka こんにちは
```

### 5.3 默认参数

调用时可以不传，函数自动使用默认值。

```python
def greet(name, message="Hello"):
    print(name, message)

greet("Tanaka")            # Tanaka Hello
greet("Tanaka", "Hi")      # Tanaka Hi
```

默认参数适合：

- 默认分页大小
- 默认状态
- 默认开关值

### 5.4 可变位置参数 `*args`

当参数数量不固定时，可以用 `*args` 接收多个位置参数。

```python
def show_items(*args):
    print(args)

show_items("A", "B", "C")  # ('A', 'B', 'C')
```

### 5.5 可变关键字参数 `**kwargs`

当参数名和数量都不固定时，可以用 `**kwargs` 接收多个关键字参数。

```python
def show_user(**kwargs):
    print(kwargs)

show_user(name="Tanaka", age=28)  # {'name': 'Tanaka', 'age': 28}
```

### 5.6 参数顺序规则

函数定义时，一般顺序是：

1. 必需位置参数
2. 默认参数
3. `*args`
4. `**kwargs`

```python
def demo(a, b=10, *args, **kwargs):
    print(a, b, args, kwargs)

demo(1, 2, 3, 4, name="Tanaka")  # 1 2 (3, 4) {'name': 'Tanaka'}
```

### 5.7 参数传递的理解

Python 常见说法是“按对象引用传递”。可以简单理解为：

- 传入的是对象的引用
- 对象是否会被改动，取决于它是否可变

```python
def append_item(items):
    items.append(3)

numbers = [1, 2]
append_item(numbers)
print(numbers)  # [1, 2, 3]
```

```python
def change_text(text):
    text = text + " world"

message = "hello"
change_text(message)
print(message)  # hello
```

### 5.8 默认参数的坑

默认参数不要使用可变对象。

```python
def bad_add(item, items=[]):
    items.append(item)
    return items
```

这个写法会让多次调用共享同一个列表，不适合企业项目。

推荐写法：

```python
def good_add(item, items=None):
    if items is None:
        items = []
    items.append(item)
    return items
```

## 六、作用域

### 6.1 局部变量与全局变量

- 局部变量只在函数内部有效
- 全局变量在整个模块中可见

```python
count = 0

def add_one():
    global count
    count += 1

add_one()
print(count)  # 1
```

### 6.2 `global` 的作用

`global` 表示函数内部要使用外层模块级别的全局变量。

企业项目中建议：

- 能不改全局变量就不改
- 尽量通过参数和返回值传递数据
- 只在少量必要场景使用 `global`

### 6.3 `nonlocal` 的作用

`nonlocal` 用于嵌套函数中，表示使用外层函数的局部变量。

```python
def outer():
    count = 0

    def inner():
        nonlocal count
        count += 1
        return count

    return inner

func = outer()
print(func())  # 1
print(func())  # 2
```

## 七、类型提示

类型提示首先是给人和工具看的，不是运行时强制限制。

```python
def add(a: int, b: int) -> int:
    return a + b

print(add(2, 3))  # 5
```

常见写法：

- 参数后面写 `: 类型`
- 返回值后面写 `-> 类型`

企业项目中，类型提示的价值主要是：

- 提高可读性
- 方便静态检查
- 减少调用误解

## 八、`lambda` 与高阶函数

### 8.1 `lambda` 是什么

`lambda` 是匿名函数，适合写简单、短小的函数。

```python
add = lambda a, b: a + b
print(add(1, 2))  # 3
```

### 8.2 `lambda` 的限制

- 只能写一个表达式
- 不适合复杂业务逻辑
- 不适合做主流程函数

如果逻辑复杂，优先使用 `def`。

### 8.3 高阶函数是什么

高阶函数是“把函数当作参数传入”或者“返回函数”的函数。

常见高阶函数：

- `map()`
- `filter()`
- `sorted(key=...)`
- `any()`
- `all()`

```python
nums = [1, 2, 3, 4]
result = list(map(lambda x: x * 2, nums))
print(result)  # [2, 4, 6, 8]
```

### 8.4 高阶函数的项目用途

- 批量数据处理
- 排序时指定规则
- 条件过滤
- 对结果做统一变换

## 九、闭包

### 9.1 什么是闭包

闭包是“内部函数引用了外部函数变量，并且外部函数返回了内部函数”的结构。

```python
def outer(message):
    def inner(name):
        return f"{message}, {name}"

    return inner

greet = outer("Hello")
print(greet("Tanaka"))  # Hello, Tanaka
```

### 9.2 闭包的作用

- 记住外层函数的上下文
- 生成带状态的函数
- 简化参数传递

### 9.3 项目中的闭包场景

- 生成带前缀的日志函数
- 生成不同业务规则的校验函数
- 为回调函数保存固定参数

## 十、装饰器

### 10.1 什么是装饰器

装饰器是在不修改原函数代码的前提下，给函数增加额外功能的方法。

### 10.2 装饰器的基本结构

```python
def decorator(func):
    def wrapper(*args, **kwargs):
        print("before")
        result = func(*args, **kwargs)
        print("after")
        return result

    return wrapper
```

### 10.3 使用装饰器

```python
def log_call(func):
    def wrapper(*args, **kwargs):
        print(f"calling {func.__name__}")
        result = func(*args, **kwargs)
        print(f"finished {func.__name__}")
        return result

    return wrapper


@log_call
def add(a, b):
    return a + b

print(add(2, 3))
# calling add
# finished add
# 5
```

### 10.4 装饰器的作用

- 统一加日志
- 统一权限检查
- 统一性能统计
- 统一异常处理

### 10.5 企业项目中的装饰器场景

- 接口权限校验
- 登录状态检查
- 请求耗时统计
- 调试日志打印

### 10.6 初学阶段的学习边界

装饰器在基础阶段要先掌握：

- 它能做什么
- 基本写法
- 基本执行过程

底层机制和复杂装饰器可以后续再深入。

## 十一、常见错误

- 一个函数承担太多职责
- 参数过多且没有默认值
- 默认参数使用可变对象
- 滥用 `global`
- `lambda` 写得过长
- 装饰器写法和返回值不清晰

## 十二、日本项目中的写法

- 函数命名要能直接看出业务动作
- 一个函数尽量只做一类明确任务
- 参数名要能看出数据含义
- 装饰器常用于统一日志、权限和耗时处理
- 提交代码时要能说明函数输入、输出和副作用

## 十三、练习题

### 基础练习

1. 写一个 `add(a, b)` 函数，返回两个数的和。
2. 写一个 `greet(name, message="Hello")` 函数。
3. 写一个函数接收任意多个数字并打印出来。

### 综合练习

1. 编写一个 `normalize_name()` 函数，去掉首尾空白并转换为小写。
2. 编写一个校验函数，输入邮箱地址，返回是否符合简单规则。

## 十四、本章总结

- 函数是复用和维护的基础
- 参数、返回值和作用域是函数的核心
- `lambda`、高阶函数、闭包和装饰器属于函数进阶能力
- 企业项目中要优先追求清晰、可维护、可测试
