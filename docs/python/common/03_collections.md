# 第3章 容器类型

> 本章目标：掌握 Python 最常用的四类容器 `list`、`tuple`、`dict`、`set`，理解它们的特点、基本使用、常用方法、遍历方式和企业项目中的选择方式。

## 前置知识

- 会使用变量和类型
- 能看懂条件判断和循环
- 了解标识符和命名规则

## 一、为什么要学容器

- 一个变量只能保存一个值
- 业务数据通常是一组、一批、一类数据
- 容器就是组织这些数据的核心结构

在企业项目里，容器常常用来表示：

- 用户列表
- 订单明细
- 配置项
- 接口参数
- 查询结果集

## 二、容器的总体特点

Python 常见容器有四种：

- `list`：列表
- `tuple`：元组
- `dict`：字典
- `set`：集合

它们最核心的区别是：

- 是否有序
- 是否可变
- 是否允许重复
- 是否适合键值映射

## 三、列表 list

### 3.1 特点

- 有序
- 可变
- 可重复
- 最常用的序列类型

### 3.2 创建方式

```python
users = ["Tom", "Alice", "Bob"]
numbers = [1, 2, 3, 4]
mixed = [100, "Python", True]
empty_list = []
```

### 3.3 基本使用

```python
users = ["Tom", "Alice", "Bob"]

print(users[0])   # Tom
print(users[-1])  # Bob
print(len(users)) # 3
```

### 3.4 常用方法

| 方法 | 作用 | 示例 |
| --- | --- | --- |
| `append()` | 追加到末尾 | `users.append("Ken")` |
| `insert()` | 指定位置插入 | `users.insert(1, "Sato")` |
| `remove()` | 删除指定值 | `users.remove("Bob")` |
| `pop()` | 删除并返回元素 | `users.pop(1)` |
| `extend()` | 追加多个元素 | `users.extend([1, 2])` |
| `index()` | 查找位置 | `users.index("Tom")` |
| `count()` | 统计次数 | `users.count("Tom")` |
| `sort()` | 原地排序 | `nums.sort()` |
| `reverse()` | 原地反转 | `nums.reverse()` |

```python
users = ["Tom", "Alice", "Bob"]

users.append("Ken")      # 追加到末尾
users.insert(1, "Sato")  # 指定位置插入
users.remove("Bob")      # 删除指定值
last = users.pop()       # 删除并返回最后一个元素
users[0] = "Jack"        # 更新第一个元素的值
print(last)              # Ken
print(users)             # ['Tom', 'Sato', 'Alice']
```

### 3.5 切片

```python
nums = [10, 20, 30, 40, 50]
print(nums[1:4])  # [20, 30, 40]
print(nums[:3])   # [10, 20, 30]
print(nums[::2])  # [10, 30, 50]
print(nums[::-1]) # [50, 40, 30, 20, 10]
```

### 3.6 解包

列表也可以用于解包，只要元素数量匹配。

```python
first, second, third = ["A", "B", "C"]
print(first)   # A
print(second)  # B
print(third)   # C
```

### 3.7 遍历 / 推导式

```python
users = ["Tom", "Alice", "Bob"]

for user in users:
    print(user)  # 依次输出 Tom / Alice / Bob

nums = [1, 2, 3, 4, 5]
squares = [n * n for n in nums]
print(squares)  # [1, 4, 9, 16, 25]
```

### 3.8 运算方式

```python
a = [1, 2]
b = [3, 4]

print(a + b)   # [1, 2, 3, 4]
print(a * 3)   # [1, 2, 1, 2, 1, 2]
print(2 in a)  # True
```

### 3.9 常见用途 / 企业场景

- 用户列表
- 订单明细
- 批量处理结果
- 分页数据

## 四、元组 tuple

### 4.1 特点

- 有序
- 不可变
- 可以索引和切片
- 适合固定结构数据

### 4.2 创建方式

```python
point = (10, 20)
person = ("Tanaka", 28)
single = ("Python",)
empty_tuple = ()
```

### 4.3 基本使用

```python
point = (10, 20)
print(point[0])  # 10
print(point[1])  # 20
```

### 4.4 常用方法

元组的方法较少，常见的是：

- `count()`
- `index()`

```python
nums = (1, 5, 5, 8)
print(nums.count(5))  # 2
print(nums.index(8))  # 3
```

### 4.5 切片

```python
nums = (10, 20, 30, 40, 50)
print(nums[1:4])  # (20, 30, 40)
print(nums[::-1]) # (50, 40, 30, 20, 10)
```

### 4.6 解包

```python
name, age = ("Tanaka", 28)
print(name)  # Tanaka
print(age)   # 28

head, *middle, tail = (10, 20, 30, 40, 50)
print(head)    # 10
print(middle)  # [20, 30, 40]
print(tail)    # 50
```

### 4.7 遍历 / 推导式

```python
for item in ("A", "B", "C"):
    print(item)  # 依次输出 A / B / C

nums = (1, 2, 3)
squares = tuple(n * n for n in nums)
print(squares)  # (1, 4, 9)
```

### 4.8 运算方式

```python
a = (1, 2)
b = (3, 4)

print(a + b)   # (1, 2, 3, 4)
print(a * 2)   # (1, 2, 1, 2)
print(2 in a)  # True
```

### 4.9 常见用途 / 企业场景

- 函数返回多个值
- 坐标、日期、固定结构数据
- 只读配置
- 不希望被修改的数据

## 五、字典 dict

### 5.1 特点

- 键值对结构
- 键唯一
- 值可以重复
- 适合业务数据映射

### 5.2 创建方式

```python
user = {
    "name": "Tanaka",
    "age": 28,
    "role": "admin",
}

empty_dict = {}
```

### 5.3 基本使用

```python
user = {
    "name": "Tanaka",
    "age": 28,
    "role": "admin",
}

print(user["name"])              # Tanaka
print(user.get("role"))           # admin
print(user.get("email", "N/A"))   # N/A
```

### 5.4 常用方法

| 方法 | 作用 | 示例 |
| --- | --- | --- |
| `keys()` | 获取所有键 | `user.keys()` |
| `values()` | 获取所有值 | `user.values()` |
| `items()` | 获取键值对 | `user.items()` |
| `get()` | 安全取值 | `user.get("age")` |
| `update()` | 更新内容 | `user.update({"city": "Tokyo"})` |
| `pop()` | 删除并返回值 | `user.pop("age")` |
| `setdefault()` | 没有则创建 | `user.setdefault("role", "user")` |

```python
user = {"name": "Tanaka", "age": 28}

print(user.keys())    # dict_keys(['name', 'age'])
print(user.values())  # dict_values(['Tanaka', 28])
print(user.items())   # dict_items([('name', 'Tanaka'), ('age', 28)])

user["role"] = "admin"
user.update({"city": "Tokyo"})
age = user.pop("age")
print(age)  # 28
print(user)  # {'name': 'Tanaka', 'role': 'admin', 'city': 'Tokyo'}
```

### 5.5 切片

字典本身不支持切片，因为字典是键值映射结构，不是顺序切片结构。

### 5.6 解包

字典更常见的是按键读取，不像列表那样直接做位置解包。

### 5.7 遍历 / 推导式

```python
user = {"name": "Tanaka", "age": 28}

for key, value in user.items():
    print(key, value)  # name Tanaka / age 28

scores = {"Tom": 85, "Alice": 92}
passed = {name: score for name, score in scores.items() if score >= 60}
print(passed)  # {'Tom': 85, 'Alice': 92}
```

### 5.8 运算方式

字典不支持像列表那样的 `+`、`*` 运算。  
字典常见的“操作方式”是：

- 按键取值
- 按键更新
- 合并内容
- 遍历键值对

```python
user = {"name": "Tanaka"}
user["age"] = 28
user.update({"role": "admin"})
print(user)  # {'name': 'Tanaka', 'age': 28, 'role': 'admin'}
```

### 5.9 常见用途 / 企业场景

- 接口参数
- JSON 数据
- 配置对象
- 业务详情数据
- 查询结果映射

## 六、集合 set

### 6.1 特点

- 无序
- 不重复
- 适合去重和集合运算

### 6.2 创建方式

```python
tags = {"python", "web", "python"}
print(tags)  # {'python', 'web'}

empty_set = set()
```

### 6.3 基本使用

```python
tags = {"python", "web"}
print("python" in tags)  # True
print("java" in tags)     # False
```

### 6.4 常用方法

| 方法 | 作用 | 示例 |
| --- | --- | --- |
| `add()` | 添加一个元素 | `tags.add("api")` |
| `update()` | 添加多个元素 | `tags.update(["mysql", "linux"])` |
| `remove()` | 删除元素 | `tags.remove("web")` |
| `discard()` | 删除元素（不存在不报错） | `tags.discard("x")` |
| `pop()` | 随机弹出元素 | `tags.pop()` |
| `clear()` | 清空集合 | `tags.clear()` |

```python
tags = {"python", "web"}

tags.add("api")
tags.update(["mysql", "linux"])
tags.remove("web")
tags.discard("not-exists")

print(tags)
```

### 6.5 切片

集合不支持切片，因为集合是无序结构。

### 6.6 解包

集合可以解包，但因为无序，解包结果不适合依赖固定顺序。

```python
values = {1, 2, 3}
a, b, c = values
print(a, b, c)
```

### 6.7 遍历 / 推导式

```python
tags = {"python", "web", "api"}

for tag in tags:
    print(tag)  # 依次输出集合中的元素

nums = [1, 2, 2, 3, 3, 4]
uniq = {n for n in nums}
print(uniq)  # {1, 2, 3, 4}
```

### 6.8 运算方式

```python
a = {1, 2, 3, 4}
b = {3, 4, 5, 6}

print(a | b)  # {1, 2, 3, 4, 5, 6}
print(a & b)  # {3, 4}
print(a - b)  # {1, 2}
print(a ^ b)  # {1, 2, 5, 6}
```

### 6.9 常见用途 / 企业场景

- 数据去重
- 权限判断
- 标签集合
- 交集和差集计算

## 七、容器选择建议

| 需求 | 推荐容器 | 理由 |
| --- | --- | --- |
| 需要按顺序访问且会修改 | `list` | 有序、可变、API 完整 |
| 固定结构只读数据 | `tuple` | 不可变、更安全、可作键 |
| 键值映射、查找频繁 | `dict` | 平均查找快、业务表达强 |
| 去重、集合代数运算 | `set` | 自动去重、并交差运算高效 |

## 八、可变与不可变

这个概念是容器章节的核心之一，必须单独理解。

### 8.1 什么是可变

可变对象是指对象创建后，内容可以在原地被修改。

```python
numbers = [1, 2, 3]
old_id = id(numbers)

numbers.append(4)

print(numbers)   # [1, 2, 3, 4]
print(id(numbers) == old_id)  # True
```

上面例子里，`list` 本身没有变成一个新对象，而是在原对象上增加了内容。

### 8.2 什么是不可变

不可变对象是指对象创建后，内容不能在原地修改。

```python
text = "Python"
old_id = id(text)

new_text = text.replace("Python", "Java")

print(text)      # Python
print(new_text)  # Java
print(id(text) == old_id)  # True
```

字符串看起来“改了”，实际上是生成了一个新字符串，原字符串没有被修改。

### 8.3 为什么这个概念重要

可变与不可变会直接影响：

- 函数参数传递后的行为
- 容器修改时是否会影响原对象
- 字典键、集合元素能不能放某种类型
- 代码中是否容易出现共享修改问题

### 8.4 四种容器的可变性

| 容器 | 是否可变 | 说明 |
| --- | --- | --- |
| `list` | 可变 | 可以增删改元素 |
| `tuple` | 不可变 | 创建后不能修改内容 |
| `dict` | 可变 | 可以增删改键值对 |
| `set` | 可变 | 可以增删元素，自动去重 |

### 8.5 常见的不可变类型

除了 `tuple` 之外，基础阶段常见的不可变类型还有：

- `str`
- `int`
- `float`
- `bool`

### 8.6 常见误区

- 以为“赋值”就是“修改原对象”
- 以为字符串能像列表一样直接改某个位置
- 以为所有容器都能作为字典键

```python
text = "abc"
# text[0] = "x"  # 错误，字符串不可变
```

### 8.7 在企业项目中的意义

- 修改共享列表时要格外小心
- 只读配置更适合用不可变数据
- 函数返回固定结构数据时，`tuple` 更安全
- 需要频繁更新的业务数据更适合 `list`、`dict`、`set`

## 九、浅拷贝与深拷贝

拷贝是容器章节里非常重要的一部分，尤其在处理嵌套列表、嵌套字典时很容易出问题。

### 9.1 什么是拷贝

- 直接赋值：两个变量指向同一个对象
- 浅拷贝：创建一个新的外层容器，但内部元素仍然可能共享
- 深拷贝：连内部嵌套对象也一起复制

### 9.2 直接赋值

```python
original = [1, 2, 3]
alias = original

alias.append(4)

print(original)  # [1, 2, 3, 4]
print(alias)     # [1, 2, 3, 4]
```

### 9.3 浅拷贝

浅拷贝适合一层结构，但遇到嵌套对象时要小心。

```python
import copy

original = [[1, 2], [3, 4]]
copied = copy.copy(original)

copied[0].append(99)

print(original)  # [[1, 2, 99], [3, 4]]
print(copied)    # [[1, 2, 99], [3, 4]]
```

### 9.4 深拷贝

深拷贝会把嵌套对象也一起复制。

```python
import copy

original = [[1, 2], [3, 4]]
copied = copy.deepcopy(original)

copied[0].append(99)

print(original)  # [[1, 2], [3, 4]]
print(copied)    # [[1, 2, 99], [3, 4]]
```

### 9.5 为什么企业项目里要重视拷贝

- 复制配置时不希望互相污染
- 处理嵌套表单数据时不希望误改原数据
- 批量修改前常常需要先复制一份原始数据

### 9.6 常见选择建议

- 一层简单结构，先考虑是否真的需要拷贝
- 嵌套容器，优先判断是否需要深拷贝
- 不确定时，先明确“改动会不会影响原对象”

## 十、日本项目中的使用场景

- `dict` 常用于解析接口参数
- `list` 常用于分页结果和批量处理
- `set` 常用于权限、标签、重复数据过滤

## 十一、Coding Rule

- `list` 适合有顺序的批量数据
- `dict` 适合键值映射
- `set` 适合去重和成员判断
- 不要混用多种容器而不说明意图

## 十二、Code Review 关注点

- 是否选错容器
- 是否存在不必要的嵌套
- 是否滥用索引取值
- 是否考虑空数据和缺失键

## 十三、常见错误

- 把本应是字典的数据写成多个并列变量
- 用列表硬当字典使用
- 直接访问字典键而不做保护
- 把 `set` 当成有序数据使用

## 十四、最佳实践

- 按业务语义选择容器
- 读取字典时优先考虑 `get()`
- 去重优先考虑 `set`
- 需要多个返回值时可考虑 `tuple`

## 十五、面试高频问题

- 中文：`list` 和 `tuple` 有什么区别？
- 日语：`list` と `tuple` の違いは何ですか。
- 思路：说明可变性、有序性和适用场景。
- 简答：`list` 可变，适合需要修改的数据；`tuple` 不可变，适合固定结构数据。

## 十六、本章练习

### 基础练习

- 定义一个用户列表并遍历输出
- 定义一个字典并读取其中的值

### 综合练习

- 将一个重复列表转换成去重集合
- 用元组保存坐标并解包输出

## 十七、本章总结

- 容器是 Python 业务开发的核心数据结构
- `list`、`dict` 最常见，`tuple` 和 `set` 也非常重要
- 理解特点、常用方法和适用场景，才能在项目中选对容器
- 后续的函数、异常、文件处理都会频繁使用容器
