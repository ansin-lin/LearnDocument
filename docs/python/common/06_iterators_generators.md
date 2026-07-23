# 第6章 迭代器与生成器

> 本章目标：理解可迭代对象、迭代器、`iter()`、`next()`、`yield` 和生成器表达式，能够用更节省内存的方式处理数据。

## 前置知识

- 会使用循环、列表和函数
- 了解容器类型
- 能看懂 `for` 循环的执行过程

## 一、为什么要学迭代器与生成器

- `for` 循环背后就依赖迭代机制
- 有些数据不适合一次性全部放进内存
- 生成器可以按需产出数据，更节省资源
- 企业项目里，大文件处理、流式处理、批量数据处理都很常见

## 二、可迭代对象

### 2.1 什么是可迭代对象

能够被 `for` 循环遍历的对象，通常就是可迭代对象。

常见可迭代对象：

- `list`
- `tuple`
- `dict`
- `set`
- `str`
- 文件对象

```python
users = ["Tanaka", "Sato", "Suzuki"]

for user in users:
    print(user)  # 依次输出每个元素
```

### 2.2 可迭代对象的特点

- 可以被循环遍历
- 不一定能直接用下标访问
- 可能一次性返回全部数据，也可能按需返回

## 三、迭代器

### 3.1 什么是迭代器

迭代器是实现了“逐个取值”能力的对象。

### 3.2 `iter()` 和 `next()`

```python
numbers = [1, 2, 3]

iterator = iter(numbers)  # 把列表转换为迭代器

print(next(iterator))  # 1
print(next(iterator))  # 2
print(next(iterator))  # 3
```

### 3.3 `next()` 用完会怎样

```python
numbers = [1, 2]
iterator = iter(numbers)

print(next(iterator))  # 1
print(next(iterator))  # 2
# print(next(iterator))  # StopIteration
```

### 3.4 迭代器的特点

- 一次取一个值
- 取完后就结束
- 不能像列表那样反复随意重用

## 四、`for` 循环和迭代器的关系

`for` 循环本质上就是不断调用迭代器取值。

```python
numbers = [1, 2, 3]

for number in numbers:
    print(number)  # 1 / 2 / 3
```

可以简单理解为：

1. 先拿到迭代器
2. 不断调用 `next()`
3. 没有数据时结束循环

## 五、生成器

### 5.1 什么是生成器

生成器是一种按需生成数据的特殊迭代器。

### 5.2 使用 `yield`

```python
def count_up_to(max_value):
    current = 1
    while current <= max_value:
        yield current
        current += 1

generator = count_up_to(3)

print(next(generator))  # 1
print(next(generator))  # 2
print(next(generator))  # 3
```

### 5.3 `yield` 和 `return` 的区别

- `return`：函数执行结束并返回结果
- `yield`：暂停函数，把当前值交出去，下次继续执行

### 5.4 生成器的优点

- 更省内存
- 适合流式处理
- 适合大批量数据

## 六、生成器表达式

生成器表达式看起来像列表推导式，但不会一次性生成全部数据。

```python
numbers = (n * n for n in range(3))

print(next(numbers))  # 0
print(next(numbers))  # 1
print(next(numbers))  # 4
```

## 七、生成器与列表推导式的区别

| 对比项 | 列表推导式 | 生成器表达式 |
| --- | --- | --- |
| 结果类型 | `list` | 生成器 |
| 是否一次性生成 | 是 | 否 |
| 内存占用 | 相对更大 | 更小 |
| 适合场景 | 小数据、需要反复使用 | 大数据、按需消费 |

## 八、企业项目中的使用场景

- 读取大文件时按需产出数据
- 批量查询结果逐条处理
- 日志流式处理
- 分页数据惰性生成

## 九、日本项目中的使用场景

- 大量订单数据分批处理
- 批量报表生成
- 长日志逐行分析
- 流式导出和导入任务

## 十、常见错误

- 把生成器当成普通列表重复使用
- 不理解 `yield` 导致流程判断错误
- 对大数据场景仍然用一次性列表装载

## 十一、练习题

### 基础练习

1. 写一个函数，使用 `yield` 依次返回 1 到 n。
2. 用 `next()` 手动获取迭代器中的值。
3. 写一个生成器表达式，生成 0 到 4 的平方。

### 综合练习

1. 写一个生成器，逐条返回文件中的行。
2. 写一个函数，模拟分页数据的惰性生成。

## 十二、本章总结

- 可迭代对象可以被遍历
- 迭代器负责逐个产出数据
- `yield` 可以把普通函数变成生成器
- 生成器适合节省内存和流式处理
- 这一章是理解 `for` 和大数据处理的重要基础
