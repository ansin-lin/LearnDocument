# 第5章 函数与作用域详解（def / return / 参数 / lambda / 高阶函数）

> 本章目标：全面掌握 Python 函数定义、调用、作用域规则与参数机制，并深入理解匿名函数与高阶函数（map/filter/reduce/lambda 等）的使用。

---

## 1 函数的概念与定义

函数（Function）是实现**代码复用**和**结构化程序设计**的重要工具。  
它将一段逻辑封装成可重复调用的模块。

### 1.1 定义语法

```python
def 函数名(参数列表):
    """文档字符串（可选）"""
    函数体
    return 返回值
```

**说明：**

- `def` 是定义函数的关键字。
- 函数名遵循标识符命名规则。
- `return` 可返回任意对象；若省略则返回 `None`。

**示例：**

```python
def greet(name):
    """打印问候语"""
    print(f"Hello, {name}!")

greet("Python")  # 调用函数
```

---

## 2 return 语句与返回值

### 2.1 基本返回

```python
def add(a, b):
    return a + b

result = add(3, 5)
print(result)  # 8
```

### 2.2 多个返回值（实为元组）

```python
def calc(a, b):
    return a + b, a - b, a * b, a / b

res = calc(10, 2)
print(res)       # (12, 8, 20, 0)

s, d, m, q = calc(10, 2)
print(s, d, m, q)  # 解包赋值
```

> 💡 Python 的多返回值其实是将多个值打包成一个 `tuple`。

---

## 3 函数的参数详解

### 3.1 位置参数（Positional Arguments）

调用时按顺序匹配参数：

```python
    def greet(name, age):
        print(f"{name} is {age} years old.")

    greet("Alice", 25)
```

---

### 3.2 关键字参数（Keyword Arguments）

可以不按顺序指定参数：

```python
greet(age=30, name="Tom")
```

---

### 3.3 默认参数（Default Arguments）

为参数设置默认值：

```python
    def power(base, exp=2):
        return base ** exp

    print(power(3))      # 默认平方：9
    print(power(3, 3))   # 三次方：27
```

> ⚠️ 默认参数在定义时只会**计算一次**，不要将可变对象（如 `[]`、`{}`）设为默认值。

```python
def append_item(item, lst=[]):
    lst.append(item)
    return lst

print(append_item(1))  # [1]
print(append_item(2))  # [1, 2] ← 复用同一个列表！
```

正确写法：

```python
    def append_item_safe(item, lst=None):
        if lst is None:
            lst = []
        lst.append(item)
        return lst
```

---

### 3.4 可变参数：`*args` 与 `**kwargs`

- 收集位置参数（*args）

```python
    def add_all(*args):
        return sum(args)

    print(add_all(1, 2, 3, 4))  # 10
```

- 收集关键字参数（**kwargs）

```python
    def show_info(**kwargs):
        for k, v in kwargs.items():
            print(f"{k} = {v}")

    show_info(name="Alice", age=25, city="Tokyo")
```

- 混合使用顺序

    定义顺序必须为：

```mkdocs
    位置参数 → 默认参数 → *args → **kwargs
```

**示例：**

```python
def demo(a, b=10, *args, **kwargs):
    print(a, b, args, kwargs)

demo(1, 2, 3, 4, x=5, y=6)
# 输出：1 2 (3, 4) {'x': 5, 'y': 6}
```

---

## 4 作用域与变量查找（LEGB 法则）

Python 查找变量遵循 **LEGB** 顺序：

| 级别 | 名称 | 说明 |
|------|------|------|
| L | Local | 当前函数内的局部作用域 |
| E | Enclosing | 嵌套函数外层（非全局）作用域 |
| G | Global | 当前模块的全局作用域 |
| B | Built-in | 内建作用域（如 len、range） |

### 4.1 示例演示

```python
x = "global"

def outer():
    x = "enclosing"
    def inner():
        x = "local"
        print(x)
    inner()

outer()  # 输出 local
```

> Python 按 LEGB 从内到外依次查找变量。

---

### 4.2 global 与 nonlocal

#### global：修改全局变量

```python
count = 0

def add():
    global count
    count += 1

add()
print(count)  # 1
```

#### nonlocal：修改外层函数变量（闭包）

```python
def outer():
    num = 10
    def inner():
        nonlocal num
        num += 5
        print(num)
    inner()

outer()  # 15
```

---

## 5 lambda 表达式（匿名函数）

### 1 基本语法

```python
lambda 参数: 表达式
```

**示例：**

```python
add = lambda x, y: x + y
print(add(3, 5))  # 8
```

> ⚠️ 匿名函数只能写**单个表达式**，不可包含多条语句。

### 2 与内置函数配合使用

```python
nums = [3, 1, 5, 2, 4]
nums.sort(key=lambda x: -x)  # 按降序排序
print(nums)
```

---

## 6 高阶函数（Higher-Order Function）

高阶函数：接受函数作为参数或返回函数的函数。

---

### 6.1 map()

将函数映射到序列：

```python
nums = [1, 2, 3, 4, 5]
squares = list(map(lambda x: x**2, nums))
print(squares)  # [1, 4, 9, 16, 25]
```

---

### 6.2 filter()

过滤序列中的元素：

```python
nums = [10, 15, 20, 25, 30]
even = list(filter(lambda x: x % 2 == 0, nums))
print(even)  # [10, 20, 30]
```

---

### 6.3 reduce()

对序列执行累计计算：

```python
from functools import reduce

nums = [1, 2, 3, 4, 5]
product = reduce(lambda x, y: x * y, nums)
print(product)  # 120
```

> `reduce` 位于 `functools` 模块中。

---

### 6.4 sorted() + key

可结合 lambda、len、自定义函数使用：

```python
words = ["Python", "Java", "C", "JavaScript"]
result = sorted(words, key=lambda w: len(w), reverse=True)
print(result)  # ['JavaScript', 'Python', 'Java', 'C']
```

---

## 7 闭包（Closure）

闭包是**函数中返回函数**的结构，内部函数记住了外部函数的变量。

```python
def make_adder(n):
    def adder(x):
        return x + n
    return adder

add5 = make_adder(5)
print(add5(10))  # 15
```

> 即使外层函数执行完毕，内部函数仍可访问其外部变量。

---

## 8 装饰器（Decorator）—— 函数的高级用法

装饰器是对函数的“包装”，常用于**日志记录**、**权限控制**、**性能统计**等。

```python
def logger(func):
    def wrapper(*args, **kwargs):
        print(f"调用函数: {func.__name__}")
        result = func(*args, **kwargs)
        print("执行完毕")
        return result
    return wrapper

@logger
def add(a, b):
    return a + b

print(add(2, 3))
```

输出：

```bash
调用函数: add
执行完毕
5
```

---

## 9 递归函数（Recursive Function）

函数调用自身的结构称为**递归**。

```python
def factorial(n):
    if n == 1:
        return 1
    return n * factorial(n - 1)

print(factorial(5))  # 120
```

> 递归要有**终止条件**，否则会导致栈溢出。

---

## 10 综合案例：学生成绩分析函数

定义函数：输入成绩列表，返回平均分、最高分、最低分
{
    "Tom": [85, 92, 78],
    "Alice": [88, 90, 95],
    "Bob": [70, 80, 75]
}

---

## 11 小结

| 知识点 | 关键词 | 功能 / 特点 |
|----------|-------------|-------------|
| 函数定义 | `def` | 定义可复用代码块 |
| 参数类型 | 位置 / 关键字 / 默认 / *args / **kwargs | 提高灵活性 |
| 作用域规则 | LEGB | 决定变量查找顺序 |
| 匿名函数 | `lambda` | 简洁表达式函数 |
| 高阶函数 | map / filter / reduce / sorted | 函数式编程核心 |
| 闭包 | 内层函数访问外层变量 | 延迟执行、状态保持 |
| 装饰器 | `@`语法糖 | 函数增强与复用 |
| 递归 | 自身调用 | 分治与结构化逻辑 |

---
