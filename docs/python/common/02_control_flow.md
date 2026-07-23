# 第2章 条件判断与循环

> 本章目标：掌握 `if`、`match-case`、`for`、`while`、`break`、`continue`、循环 `else` 的基本用法，理解程序如何按条件分支和循环执行。

## 前置知识

- 会使用变量和比较运算符
- 能看懂基本的字符串、列表和字典

## 一、为什么需要流程控制

程序不是一直从上往下顺序执行到底，而是经常要根据不同情况做不同处理。

例如：

- 用户登录成功和失败，处理不同
- 订单金额大于 0 和小于等于 0，处理不同
- 状态是 `new`、`processing`、`done`，处理不同
- 需要重复读取数据、逐条处理、持续重试

流程控制就是用来解决这些问题的。

## 二、条件判断 if

`if` 用于根据条件决定是否执行某段代码。

### 2.1 基本写法

```python
score = 85

if score >= 90:
    print("优秀")
elif score >= 60:
    print("及格")
else:
    print("不及格")
```

### 2.2 执行过程

上面的代码执行时，会按这个顺序判断：

1. 先判断 `score >= 90`
2. 如果成立，就执行 `print("优秀")`
3. 如果不成立，再判断 `score >= 60`
4. 如果成立，就执行 `print("及格")`
5. 如果都不成立，就执行 `else` 里的代码

也就是说，`if / elif / else` 是按顺序逐个判断的。

### 2.3 常见用法

#### 2.3.1 单分支

```python
age = 20

if age >= 18:
    print("成年")
```

#### 2.3.2 多分支

```python
score = 75

if score >= 90:
    print("优秀")
elif score >= 60:
    print("及格")
else:
    print("不及格")
```

#### 2.3.3 条件表达式

条件表达式适合简单的二选一。

```python
age = 20
status = "成年" if age >= 18 else "未成年"
print(status)  # 成年
```

## 三、match-case

`match-case` 是 Python 3.10+ 提供的模式匹配语法，适合处理多个固定分支、结构化数据和状态分发。

### 3.1 为什么需要 match-case

当一个变量要对应很多固定值时，`match-case` 通常比一长串 `if/elif` 更清晰。

例如：

- 命令分发
- 状态处理
- 简单结构解包
- 固定值分类

### 3.2 基本写法

```python
command = "start"

match command:
    case "start":
        print("系统启动")
    case "stop":
        print("系统停止")
    case _:
        print("未知命令")
```

### 3.3 执行过程

1. 先看 `command` 的值
2. 从上到下匹配 `case`
3. 找到第一个符合的分支就执行
4. 如果都不符合，就执行 `_` 这个默认分支

### 3.4 多值匹配

```python
status = "processing"

match status:
    case "new" | "processing":
        print("处理中")
    case "done":
        print("已完成")
    case _:
        print("其他状态")
```

### 3.5 条件匹配

```python
score = 82

match score:
    case n if n >= 90:
        print("优秀")
    case n if n >= 60:
        print("及格")
    case _:
        print("不及格")
```

### 3.6 解包匹配

```python
point = (10, 20)

match point:
    case (0, 0):
        print("原点")
    case (x, 0):
        print(f"X 轴上的点，x = {x}")
    case (0, y):
        print(f"Y 轴上的点，y = {y}")
    case (x, y):
        print(f"一般坐标点：({x}, {y})")
```

### 3.7 和 if/elif 的区别

| 对比项 | `if/elif` | `match-case` |
| --- | --- | --- |
| 适合场景 | 通用条件判断 | 固定值分支、结构匹配 |
| 可读性 | 分支少时清晰，分支多时容易变长 | 多分支时更清楚 |
| 结构匹配 | 不擅长 | 很适合 |
| Python 版本 | 所有现代版本 | Python 3.10+ |

## 四、for 循环

`for` 用于遍历序列、列表、字典、字符串、集合等可迭代对象。

### 4.1 基本写法

```python
for i in range(3):
    print(i)
```

输出过程：

1. `i` 先取 `0`
2. 执行循环体
3. `i` 再取 `1`
4. 再执行循环体
5. `i` 再取 `2`
6. 结束循环

### 4.2 range 的常见用法

```python
for i in range(5):
    print(i)

for i in range(1, 6):
    print(i)

for i in range(10, 0, -2):
    print(i)
```

### 4.3 遍历容器

```python
users = ["Tanaka", "Sato", "Suzuki"]

for user in users:
    print(user)
```

```python
user = {"name": "Tanaka", "age": 28}

for key in user:
    print(key, user[key])
```

### 4.4 for-else

`for-else` 中的 `else` 会在循环没有被 `break` 中断时执行。

```python
for i in range(3):
    print(i)
else:
    print("循环正常结束")
```

### 4.5 常见用途

- 遍历用户列表
- 批量处理订单
- 按行处理文件
- 遍历接口返回结果

## 五、while 循环

`while` 只要条件成立就一直执行。

### 5.1 基本写法

```python
count = 0

while count < 3:
    print(count)
    count += 1
```

### 5.2 执行过程

1. 先判断 `count < 3`
2. 如果成立，执行循环体
3. 执行 `count += 1`
4. 再回到开头继续判断
5. 条件不成立时退出循环

### 5.3 常见用途

- 登录失败次数限制
- 重试机制
- 持续轮询
- 输入校验直到合法

### 5.4 while-else

```python
count = 0

while count < 3:
    print(count)
    count += 1
else:
    print("循环正常结束")
```

## 六、break 与 continue

### 6.1 break

`break` 用于直接结束当前循环。

```python
for i in range(1, 6):
    if i == 4:
        break
    print(i)
```

常见场景：

- 找到目标后立即停止
- 达到条件后提前结束循环

### 6.2 continue

`continue` 用于跳过本次循环，进入下一轮。

```python
for i in range(1, 6):
    if i == 3:
        continue
    print(i)
```

常见场景：

- 跳过非法数据
- 跳过不需要处理的记录

### 6.3 嵌套循环

```python
for i in range(2):
    for j in range(3):
        print(i, j)
```

嵌套循环适合：

- 表格数据处理
- 二维坐标
- 行列数据遍历

## 七、日本项目中的使用场景

- 状态分发使用 `match-case`
- 查询条件使用 `if/elif`
- 批量数据处理使用 `for`
- 重试和等待使用 `while`
- 提前退出用 `break`
- 跳过异常记录用 `continue`

在日本项目中，分支不要太深，逻辑要让别人一眼能读懂。

## 八、Coding Rule

- 分支优先扁平化
- 能用 `match-case` 表达清楚时，不要堆很多 `if/elif`
- 循环体只放必要逻辑
- 条件判断尽量早返回

## 九、Code Review 关注点

- 是否存在过深嵌套
- 循环是否可能死循环
- 是否漏掉边界条件
- 状态分支是否清晰
- `break`、`continue` 是否用得合理

## 十、常见错误

- 在 `while` 里忘记更新条件变量
- 多层 `if` 导致逻辑难读
- `match-case` 没有处理默认分支
- 循环里把本来应该在循环外的逻辑写进去了

## 十一、最佳实践

- 状态分发优先考虑 `match-case`
- 循环前先明确终止条件
- 把复杂判断拆成多个小步骤
- 处理列表时尽量先明确“遍历对象是什么”

## 十二、面试高频问题

- 中文：`match-case` 和 `if/elif` 有什么区别？
- 日语：`match-case` と `if/elif` の違いは何ですか。
- 思路：说明 `match-case` 更适合固定值和结构匹配。
- 简答：`match-case` 适合多分支、固定值和结构化数据，`if/elif` 更通用。

## 十三、本章练习

### 基础练习

- 用 `if` 判断一个数字是正数、负数还是零
- 用 `for` 输出 1 到 5
- 用 `while` 输出 1 到 3

### 综合练习

- 编写一个命令分发程序，使用 `match-case` 处理 `start`、`stop`、`status`
- 编写一个循环程序，跳过偶数，只输出奇数

## 十四、本章总结

- 流程控制是业务逻辑的核心
- `if`、循环和 `match-case` 是基础中的基础
- `match-case` 还能做多值匹配、条件匹配和结构解包
- `for`、`while` 还要掌握 `else`、`break`、`continue` 和嵌套循环
- 后续函数、容器和异常处理都会和流程控制配合使用
