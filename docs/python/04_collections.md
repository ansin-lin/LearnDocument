# 第4章 容器类型详解

> 本章目标：系统掌握 **列表、元组、字典、集合** 四类容器。

---

## 容器类型简介

在 Python 中，**容器类型（Container Types）** 是用于存储多个数据项的数据结构。  
它们允许我们方便地组织、访问和修改数据。

Python 的四种主要容器类型：

| 类型 | 名称 | 是否可变 | 是否有序 | 是否允许重复 |
|------|------|------------|------------|----------------|
| list | 列表 | ✅ 是 | ✅ 是 | ✅ 是 |
| tuple | 元组 | ❌ 否 | ✅ 是 | ✅ 是 |
| dict | 字典 | ✅ 是 | ✅ 键无序（Python 3.7+保持插入顺序） | ❌ 键唯一 |
| set | 集合 | ✅ 是 | ❌ 否 | ❌ 否 |

---

## 一、 列表（list）

### 1. 创建列表

在 Python 中，列表是由一系列元素按**特定顺序**构成的**可变**序列。使用 `[]` 字面量语法定义，多个元素之间用逗号分隔。

```python
items1 = [35, 12, 99, 68, 55, 35, 87]
items2 = ['Python', 'Java', 'Go', 'Kotlin']
items3 = [100, 12.3, 'Python', True]

print(items1)  # [35, 12, 99, 68, 55, 35, 87]
print(items2)  # ['Python', 'Java', 'Go', 'Kotlin']
print(items3)  # [100, 12.3, 'Python', True]
```

- 列表可包含**重复元素**（如 `items1` 中的 `35`）。  
- 列表可包含**不同类型**的元素（如 `items3`），但**不推荐**：后续操作会变复杂。  
- 可用 `type()` 查看类型；对“容器”变量，习惯用**复数名词**命名（如 `items`）。

也可用 `list()` **构造器**将其他序列转换为列表：

```python
items4 = list(range(1, 10))  # 1~9
items5 = list('hello')       # 按字符拆分成列表
print(items4)  # [1, 2, 3, 4, 5, 6, 7, 8, 9]
print(items5)  # ['h', 'e', 'l', 'l', 'o']
```

---

### 2. 列表的运算

- 拼接与重复

```python
items5 = [35, 12, 99, 45, 66]
items6 = [45, 58, 29]
items7 = ['Python', 'Java', 'JavaScript']

print(items5 + items6)
# [35, 12, 99, 45, 66, 45, 58, 29]

print(items6 * 3)
# [45, 58, 29, 45, 58, 29, 45, 58, 29]

print(items7 * 2)
# ['Python', 'Java', 'JavaScript', 'Python', 'Java', 'JavaScript']

items5 += items6  # 原地扩展（等同于 extend）
print(items5)
# [35, 12, 99, 45, 66, 45, 58, 29]
```

- 成员测试

```python
print(29 in items6)        # True
print(99 in items6)        # False
print('C++' not in items7) # True
print('Python' in items7)  # True
```

---

### 3. 索引与切片

- **索引（Indexing）**：`0 ~ N-1` 正向索引；`-1 ~ -N` 反向索引。
- **切片（Slicing）**：`seq[start:end:step]`，左闭右开；`step` 可为负数。

```python
items8 = ['apple', 'waxberry', 'pitaya', 'peach', 'watermelon']

print(items8[0])   # apple（第1个）
print(items8[2])   # pitaya（第3个）
print(items8[-1])  # watermelon（最后1个）

# 修改指定位置元素（列表可变的体现）
items8[2] = 'durian'
print(items8)  # ['apple', 'waxberry', 'durian', 'peach', 'watermelon']

# 切片：左闭右开；步长为正从左向右，为负从右向左
print(items8[1:3:1])     # ['waxberry', 'durian']
print(items8[0:5:2])     # ['apple', 'durian', 'watermelon']
print(items8[-2:-6:-1])  # ['peach', 'durian', 'waxberry', 'apple']

# 省略写法：start=0 可省；end=N 可省；step=1 可省
print(items8[1:3])   # ['waxberry', 'durian']
print(items8[:3])    # ['apple', 'waxberry', 'durian']
print(items8[::2])   # ['apple', 'durian', 'watermelon']

# 切片赋值：批量替换
items8[1:3] = ['x', 'o']
print(items8)  # ['apple', 'x', 'o', 'peach', 'watermelon']
```

> ⚠️ 索引越界会触发 `IndexError: list index out of range`。

---

### 4. 关系比较

```python
nums1 = [1, 2, 3, 4]
nums2 = list(range(1, 5))
nums3 = [3, 2, 1]

print(nums1 == nums2)  # True（对应元素完全相同）
print(nums1 != nums2)  # False
print(nums1 <= nums3)  # True（逐元素字典序比较）
print(nums2 >= nums3)  # False
```

> 实务中较少直接比较两个列表大小，必要时请明确排序规则与 key。

---

### 5. 遍历列表的两种方式

```python
languages = ['Python', 'Java', 'C++', 'Kotlin']

# 方式一：通过索引遍历
for i in range(len(languages)):
    print(languages[i])

# 方式二：直接迭代元素（推荐）
for lang in languages:
    print(lang)
```

---

### 6. 列表常用方法（增删改查）

```python
languages = ['Python', 'Java', 'C++']

# 追加到末尾
languages.append('JavaScript')

# 按位置插入
languages.insert(1, 'SQL')

# 删除指定值（若不存在将抛 ValueError）
if 'Java' in languages:
    languages.remove('Java')

# 弹出最后/指定位置的元素（并返回该元素）
last = languages.pop()   # 默认最后一个
second = languages.pop(1)

# 清空
languages.clear()
```

- `del 列表[索引]`：按索引删除（不返回被删元素，速度略优于 `pop(索引)`）。  
- `index(value, start=..., end=...)`：找元素位置，不存在抛 `ValueError`。  
- `count(value)`：统计出现次数。

```python
items = ['Python', 'Java', 'Java', 'C++', 'Kotlin', 'Python']
print(items.index('Python'))       # 0
print(items.index('Python', 1))    # 5（从索引1开始找）
print(items.count('Python'))       # 2
print(items.count('Swift'))        # 0
```

- 排序与反转

```python
items = ['Python', 'Java', 'C++', 'Kotlin', 'Swift']

items.sort()     # 原地升序
items.reverse()  # 原地反转

# 不改变原列表，返回新列表
sorted_items = sorted(items, key=len, reverse=True)  # 按长度降序
```

> `list.sort()` 原地、返回 `None`；`sorted(iterable, key=..., reverse=...)` 返回新列表。

---

### 7. 列表生成式（推导式）

> 强烈建议：**优先用生成式** 构造新列表。

```python
# 1~99 中能被 3 或 5 整除的数
items = [i for i in range(1, 100) if i % 3 == 0 or i % 5 == 0]

# 平方列表
nums1 = [35, 12, 97, 64, 55]
nums2 = [n ** 2 for n in nums1]

# 过滤大于 50 的元素
nums3 = [n for n in nums1 if n > 50]
```

- 解释器对推导式有专门优化的字节码（如 `LIST_APPEND`），通常 **比 `for+append` 更快**。

---

### 8. 嵌套列表

```python
# 5个学生，3门课
scores = [[95, 83, 92], [80, 75, 82], [92, 97, 90], [80, 78, 69], [65, 66, 89]]
print(scores[0])     # 第1个学生的全部成绩
print(scores[0][1])  # 第1个学生第2门课成绩

# 键盘录入 5×3
scores = []
for _ in range(5):
    row = []
    for _ in range(3):
        row.append(int(input('请输入: ')))
    scores.append(row)

# 随机生成 5×3
import random
scores = [[random.randrange(60, 101) for _ in range(3)] for _ in range(5)]
```

---

## 二、 元组（tuple）

### 1. 创建与特性

```python
colors = ('red', 'green', 'blue')
single = ('one',)  # 单元素元组必须加逗号
empty  = ()

print(type(single))  # <class 'tuple'>
```

- **不可变**：创建后不能增删改（可提升可哈希性/安全性）。  
- **有序**、可索引/切片、可迭代、可嵌套。

### 2. 典型用法

```python
# 解包赋值
person = ('Alice', 25, 'Tokyo')
name, age, city = person

# 元组作字典键（元素需可哈希）
d = {('Tokyo', 'JP'): 37_000_000}
```

---

## 三、 字典（dict）

### 1. 创建与访问

```python
person = {"name": "Tom", "age": 28, "city": "Osaka"}
print(person["name"])          # 不存在 -> KeyError
print(person.get("city"))      # 推荐：不存在 -> None 或给默认
print(person.get("city", "N/A"))
```

- **键唯一**，常用不可变类型（str、int、tuple）。  
- Python 3.7+ **保持插入顺序**。

### 2. 修改 / 新增 / 删除

```python
person["age"] = 30
person["gender"] = "Male"
del person["city"]           # 或 person.pop("city", None)
```

### 3. 遍历 / 推导式 / 排序视图

```python
for k in person.keys(): print(k)
for v in person.values(): print(v)
for k, v in person.items(): print(k, v)

# 推导式：反转映射
m = {"a": 1, "b": 2, "c": 3}
inv = {v: k for k, v in m.items()}

# 排序视图
scores = {"Tom": 85, "Alice": 90, "Bob": 78}
print(sorted(scores))  # 键排序
print(sorted(scores.items(), key=lambda kv: kv[1], reverse=True))
```

---

## 四、 集合（set）

### 1. 创建

```python
numbers = {1, 2, 3, 3, 2}
print(numbers)   # {1, 2, 3}
empty = set()    # 空集合必须用 set()
```

- **无序、唯一、可变**（`frozenset` 为不可变集合）。  
- 不支持索引；适合**去重、判重、集合代数**。

### 2. 集合运算 / 修改 / 推导式

```python
a, b = {1, 2, 3, 4}, {3, 4, 5, 6}
print(a | b, a & b, a - b, a ^ b)

s = {1, 2, 3}
s.add(4); s.update([5, 6])
s.remove(2)       # 不存在抛 KeyError
s.discard(10)     # 不存在不报错

nums = [1, 2, 2, 3, 3, 3]
uniq = {x for x in nums if x % 2 == 1}  # {1, 3}
```

---

## 五、 容器共通的“坑位与技巧”

```python
# 浅拷贝 vs 深拷贝
import copy
a = [[1, 2], [3, 4]]
b = a.copy()
c = copy.deepcopy(a)
a[0][0] = 9
print(b)  # [[9, 2], [3, 4]]（共享内层）
print(c)  # [[1, 2], [3, 4]]（完全独立）

# 稳定排序 + key 组合
students = [
    {"name": "Tom", "age": 20, "score": 85},
    {"name": "Alice", "age": 20, "score": 90},
    {"name": "Bob", "age": 22, "score": 78},
]
students.sort(key=lambda s: s["score"], reverse=True)
students.sort(key=lambda s: s["age"])

# 星号解包
head, *middle, tail = [10, 20, 30, 40, 50]
print(head, middle, tail)  # 10 [20, 30, 40] 50
```

---

## 六、 容器选择建议

| 需求 | 推荐容器 | 理由 |
|-----|---------|------|
| 需要按序访问且会修改 | list | 有序、可变、API 完整 |
| 固定结构只读数据 | tuple | 不可变、更安全、可作键 |
| 键值映射、查找频繁 | dict | O(1) 平均查找、业务表达强 |
| 去重、集合代数运算 | set | 自动去重、并交差运算高效 |

---

## 七、 小结

- `list`：最常用的可变序列，熟练掌握**切片、增删改查、生成式**与**排序**。  
- `tuple`：不可变序列，适于只读与解包，必要时可作字典键。  
- `dict`：业务最常用的数据结构，掌握**增删改查、遍历、推导式、排序视图**。  
- `set`：去重与集合运算的首选，记住**并/交/差/对称差**与**增删**方法。  
- 理解**浅深拷贝**、**排序稳定性**、**解包技巧**，能显著减少 Bug。

---
