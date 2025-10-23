# 第2章 语法基础与变量

> 本章目标：理解 Python 程序的基本结构，掌握变量与数据类型的概念、输入输出方法、运算符与表达式。

---

## 一、初识变量与程序概念

对于编程新手，首先需要了解两个问题：

1. **什么是程序？** —— 程序是数据和指令的有序集合。  
2. **编程能做什么？** —— 通过程序控制计算机执行特定任务。  

Python 之所以受欢迎，是因为它**简单、优雅、功能强大**。相比 C、C++、Java来说，Python 对初学者更友好，能够用更少的代码表达复杂逻辑。

---

## 二、计算机基础知识

计算机的硬件系统由五大部件构成：
  
- **运算器**、**控制器**（合称 CPU）：执行运算与控制指令。  
- **存储器**：包括内存与外部存储设备。  
- **输入设备**：键盘、鼠标、麦克风。  
- **输出设备**：显示器、打印机、扬声器。  

现代计算机遵循“冯·诺依曼体系结构”，其关键在于：

1. 将存储器与处理器分离。  
2. 所有数据均以 **二进制形式** 存储。  

Python 程序中的一切数据（文字、数字、图片等）在底层都以二进制形式存在。

---

## 三、变量与数据类型

### 1. 变量的概念

变量是数据的载体，本质上是一块命名的内存空间。  
它可以存储、修改和读取数据，是所有程序逻辑的基础。

```python
a = 45
b = 12
print(a + b)  # 输出57
```

---

### 2. Python 常用数据类型

| 类型 | 说明 | 示例 |
|------|------|------|
| int | 整数 | `a = 10` |
| float | 浮点数 | `b = 3.14` |
| str | 字符串 | `name = "Tom"` |
| bool | 布尔值 | `is_ok = True` |
| NoneType | 空值 | `value = None` |

#### （1）整型（int）

Python 支持多种进制：

```python
print(0b100)  # 二进制 → 4
print(0o100)  # 八进制 → 64
print(0x100)  # 十六进制 → 256
```

#### （2）浮点型（float）

支持科学计数法：

```python
print(123.456)
print(1.23456e2)  # 1.23456 × 10² = 123.456
```

#### （3）字符串（str）

用单引号或双引号包裹：

```python
msg = "Hello, Python!"
print(msg)
```

#### （4）布尔型（bool）

```python
flag = True
print(not flag)  # False
```

#### （5）None

表示“没有值”或“空对象”：

```python
result = None
print(result is None)  # True
```

---

### 3. 变量命名规则

**规则：**

1. 只能使用字母、数字、下划线，不能以数字开头。  
2. 区分大小写。  
3. 不能与关键字或内置函数重名（如 `list`, `str`, `sum`）。  

**命名惯例（PEP 8）：**

- 普通变量：`user_name`
- 常量：`PI = 3.14`
- 受保护变量：`_name`
- 私有变量：`__password`

---

### 4. 检查类型与类型转换

```python
a = 100
b = 123.45
c = '123'
print(type(a))  # <class 'int'>
print(float(a))  # 100.0
print(int(b))    # 123
print(int(c, 16))  # 十六进制 '123' → 291
print(chr(100))  # 'd'
print(ord('d'))  # 100
```

> 💡 `str` 转 `int` 可指定进制；`bool` 转 `int` 时，True 为 1，False 为 0。

---

## 四、运算符与表达式

Python 运算符优先级如下（从高到低）：

| 运算符 | 描述 |
|--------|------|
| []、[:] | 索引、切片 |
| ** | 幂 |
| *、/、%、// | 乘、除、取模、整除 |
| +、- | 加、减 |
| ==、!=、>、<、>=、<= | 比较运算 |
| and、or、not | 逻辑运算 |
| =、+=、-=、*=、/= 等 | 赋值运算 |
|is、is not | 身份运算符 |
|in、not in | 成员运算符 |

---

### 1. 算术运算符

```python
print(321 + 12)
print(321 - 12)
print(321 * 12)
print(321 / 12)
print(321 // 12)
print(321 % 12)
print(2 ** 3)
```

**优先级示例：**

```python
print(2 + 3 * 5)       # 17
print((2 + 3) * 5)     # 25
print((2 + 3) * 5 ** 2)# 125
```

---

### 2. 赋值运算符

```python
a = 10
b = 3
a += b        # 等价于 a = a + b
a *= a + 2    # 等价于 a = a * (a + 2)
print(a)
```

Python 3.8 引入“海象运算符”`:=`：

```python
print(a := 10)  # 同时赋值并返回 10
```

---

### 3. 比较与逻辑运算

```python
flag0 = 1 == 1
flag1 = 3 > 2
flag2 = 2 < 1
flag3 = flag1 and flag2
flag4 = flag1 or flag2
flag5 = not flag0
print(flag0, flag1, flag2, flag3, flag4, flag5)
```

输出：

```bash
True True False False True False
```

---

### 4. 身份运算符（Identity Operators）

身份运算符用于判断两个变量是否引用自同一个对象（内存地址是否相同）。

```python
a = [1, 2, 3]
b = a         # b 和 a 指向同一个对象
c = [1, 2, 3] # c 内容相同，但不是同一个对象

print(a is b)      # True，因为它们指向同一个列表对象
print(a is c)      # False，虽然内容相同，但内存地址不同
print(a == c)      # True，因为 == 比较的是内容是否相同
print(a is not c)  # True，因为 a 和 c 不是同一对象
```

is 比较的是 身份（内存地址）。

== 比较的是 值（内容）。

### 4. 成员运算符（Membership Operators）

成员运算符用于判断某个元素是否在序列（如字符串、列表、元组、字典、集合等）中。

```python
# 字符串
print('a' in 'apple')      # True
print('x' not in 'apple')  # True

# 列表
nums = [1, 2, 3, 4]
print(3 in nums)            # True
print(5 not in nums)        # True

# 字典（只检查 key）
d = {'name': 'Tom', 'age': 18}
print('name' in d)          # True
print('Tom' in d)           # False，因为只判断键，不判断值
```

---

## 五、输入与输出

### 1. 输入 input()

```python
name = input("请输入你的名字：")
age = int(input("请输入你的年龄："))
print(f"你好 {name}, 你今年 {age} 岁。")
```

### 2. 输出 print()

```python
print("A", "B", "C", sep="-")  # A-B-C
print("Hello", end="!")        # 不换行
```

---

## 六、运算符实践案例

### 例1：华氏温度转摄氏温度

---

### 例2：计算圆的周长和面积

---

### 例3：判断闰年

> 判断规则
>
> - 非4的倍数 → 平年  
> - 能被4整除但不能被100整除 → 闰年  
> - 能被400整除 → 闰年

---

## 七、小结

- 变量是数据的容器，可保存多种类型数据；  
- Python 提供丰富的运算符与表达式；
- 掌握类型转换与输入输出是编程的基础；  
- 运算符优先级可用小括号改变执行顺序。  

---
