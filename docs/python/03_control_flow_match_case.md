# 第3章 流程控制语句详解

> 本章目标：深入理解 Python 流程控制语法的执行原理，掌握条件判断、循环语句、跳转控制与模式匹配的具体写法与逻辑。

---

## 一、条件判断语句

### 1. 基本语法结构

```python
if 条件表达式:
    # 当条件为 True 时执行这里的语句块
    语句1
elif 其他条件:
    # 当上一个条件不满足但这个条件为 True 时执行这里
    语句2
else:
    # 当所有条件都不成立时执行这里
    语句3
```

**说明：**

- Python 使用 **缩进（Indentation）** 表示语句块，不使用 `{}`。
- 条件表达式的结果必须是布尔值（True / False）。
- `elif` 可以出现多次，`else` 最多出现一次，也可以省略。

**示例：**

```python
score = int(input("请输入分数："))

if score >= 90:               # 判断是否 ≥90
    print("优秀")             # 满足条件执行
elif score >= 80:             # 否则判断是否 ≥80
    print("良好")
elif score >= 60:             # 否则判断是否 ≥60
    print("及格")
else:                         # 其他所有情况
    print("不及格")
```

---

### 2. 条件表达式

（条件表达式又称三元表达式或三目运算符）

```python
变量 = 值1 if 条件 else 值2
```

**执行逻辑：**

- 若条件为真，表达式取值为 `值1`；
- 若条件为假，取 `值2`。

**示例：**

```python
age = 20
status = "成年" if age >= 18 else "未成年"  # 条件成立返回“成年”
print(status)
```

输出：

```bash
成年
```

---

### 3. 嵌套判断

```python
if 条件1:
    if 条件2:
        执行语句
```

**示例：**

```python
age = 20
gender = "男"

if age >= 18:
    if gender == "男":
        print("成年男性")
    else:
        print("成年女性")
else:
    print("未成年")
```

---

## 二、match-case 模式匹配

（Python 3.10+以上版本适用）

### 1. 基本语法

```python
match 表达式:
    case 模式1:
        执行语句1
    case 模式2:
        执行语句2
    case _:
        执行默认语句
```

**语法说明：**

- `match` 表达式类似于 switch。
- `case` 是匹配模式分支。
- `_` 表示通配符（默认匹配）。

**示例：**

```python
command = input("请输入命令：")

match command:
    case "start":                     # 匹配 "start"
        print("系统启动中...")
    case "stop":                      # 匹配 "stop"
        print("系统已停止。")
    case "restart":                   # 匹配 "restart"
        print("系统重启中...")
    case _:                           # 其他任意输入
        print("未知命令。")
```

---

### 2. 多模式匹配

（多个条件匹配时使用 `|` 连接）

```python
weather = input("天气状态（sunny/cloudy/rainy）：")

match weather:
    case "sunny" | "cloudy":          # 匹配多个值
        print("今天适合出门～")
    case "rainy":
        print("记得带伞！")
    case _:
        print("输入无效")
```

---

### 3. 带条件匹配

（守卫语句 if）

```python
x = int(input("请输入整数："))

match x:
    case n if n > 0:                  # 匹配正数
        print("正数")
    case n if n < 0:                  # 匹配负数
        print("负数")
    case _:
        print("零")
```

> 💡 守卫语句（if）使得匹配模式可以带条件判断。

---

### 4. 解构匹配

（结构化模式）

```python
point = (2, 0)

match point:
    case (0, 0):                     # 匹配原点
        print("原点")
    case (x, 0):                     # 匹配 X 轴上的点
        print(f"X轴：x={x}")
    case (0, y):                     # 匹配 Y 轴上的点
        print(f"Y轴：y={y}")
    case (x, y):                     # 其他点
        print(f"一般坐标点 ({x}, {y})")
```

---

## 三、for 循环语句

### 1. 基本语法a

```python
for 变量 in 可迭代对象:
    # 每次循环自动取下一个元素
    执行语句
```

**可迭代对象**：如 `list`, `tuple`, `str`, `range()` 等。

**示例：**

```python
for i in range(3):             # 从 0 循环到 2
    print(f"第 {i+1} 次循环")
```

输出：

```bash
第 1 次循环
第 2 次循环
第 3 次循环
```

---

### 2. range() 函数详解

```python
range(start, stop, step)
```

- `start`：起始值（默认 0）  
- `stop`：结束值（不含）  
- `step`：步长（可为负数）

**示例：**

```python
for i in range(1, 10, 3):
    print(i)
```

输出：

```bash
1
4
7
```

---

### 3. for + else 结构

```python
for i in range(3):
    print(i)
else:
    print("循环正常结束")
```

> 💡 `else` 部分仅在循环**未被 break 提前终止**时执行。

---

## 四、while 循环语句

### 1. 基本语法b

```python
while 条件表达式:
    执行语句
```

**执行流程：**

1. 判断条件是否为真；  
2. 若真 → 执行循环体；  
3. 执行完毕后重新判断；  
4. 条件为假 → 循环结束。

**示例：**

```python
i = 0
while i < 3:
    print("循环中，i =", i)
    i += 1
```

---

### 2. while + else

```python
count = 0
while count < 3:
    print("执行中...")
    count += 1
else:
    print("循环正常结束")
```

> 💡 与 for 一样，`else` 只在循环未被 break 打断时执行。

---

## 五、break 与 continue

### 1. break

立即终止整个循环

```python
for i in range(1, 6):
    if i == 3:
        break          # 遇到 3 时终止循环
    print(i)
```

输出：

```bash
1
2
```

### 2. continue

跳过当前迭代

```python
for i in range(1, 6):
    if i == 3:
        continue        # 遇到 3 时跳过本次循环
    print(i)
```

输出：

```bash
1
2
4
5
```

---

## 六、综合案例

### 案例1：猜数字游戏

计算机出一个 1 到 100 之间的随机数，玩家输入自己猜的数字，计算机给出对应的提示信息“大一点”、“小一点”或“猜对了”，如果玩家猜中了数字，计算机提示用户一共猜了多少次，游戏结束，否则游戏继续。

### 案例2：判断素数

要求：输入一个大于 1 的正整数，判断它是不是素数。

---

## 七、小结

| 控制语句 | 关键字 | 功能描述 |
|-----------|----------|-----------|
| if / elif / else | 条件判断 | 根据条件执行不同分支 |
| match / case | 模式匹配 | 简化多条件结构 |
| for | 遍历序列 | 对可迭代对象逐一处理 |
| while | 条件循环 | 条件为真时持续执行 |
| break | 终止循环 | 立即退出当前循环 |
| continue | 跳过迭代 | 跳过当前循环继续下一次 |

---
