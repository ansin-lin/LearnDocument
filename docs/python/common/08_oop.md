# 第8章 面向对象

> 本章目标：理解类与对象、属性与方法、封装、继承、多态、组合、委托和魔术方法的基本用法，能够用对象思维分析企业业务。

## 前置知识

- 会使用函数、字典、列表和异常
- 能看懂前面章节的变量、容器和函数
- 了解基本的程序组织方式

## 一、为什么需要面向对象

现实业务里，大部分信息不是单个值，而是“一个对象”。

比如：

- 一个用户
- 一个订单
- 一个商品
- 一个配置对象

面向对象的核心作用是：

- 用类描述业务模型
- 用对象保存具体数据
- 把数据和行为放在一起
- 让代码更容易维护和扩展

## 二、类与对象

### 2.1 什么是类

类是模板，是对一类事物的抽象。

### 2.2 什么是对象

对象是类创建出来的实例，是具体存在的数据实体。

### 2.3 基本语法

```python
class Person:
    def __init__(self, name, age):
        self.name = name
        self.age = age

    def introduce(self):
        return f"{self.name} is {self.age} years old"

person = Person("Tanaka", 28)
print(person.introduce())  # Tanaka is 28 years old
```

### 2.4 执行过程

1. 定义类
2. 调用类创建对象
3. 自动执行 `__init__`
4. 把属性存到对象里
5. 通过对象调用方法

### 2.5 `self` 是什么

`self` 表示当前对象本身。  
在实例方法中，Python 会自动把对象传给第一个参数。

## 三、属性与方法

### 3.1 实例属性

实例属性是每个对象自己独有的数据。

```python
class User:
    def __init__(self, name, age):
        self.name = name
        self.age = age

u1 = User("Tanaka", 28)
u2 = User("Sato", 30)

print(u1.name)  # Tanaka
print(u2.name)  # Sato
```

### 3.2 类属性

类属性是整个类共享的数据。

```python
class Config:
    app_name = "LearnDocument"

print(Config.app_name)  # LearnDocument
```

### 3.3 实例方法

实例方法需要通过对象调用，第一个参数通常是 `self`。

```python
class User:
    def __init__(self, name):
        self.name = name

    def greet(self):
        return f"Hello, {self.name}"

user = User("Tanaka")
print(user.greet())  # Hello, Tanaka
```

### 3.4 类方法

类方法通过 `@classmethod` 定义，第一个参数通常是 `cls`，代表类本身。

```python
class Config:
    app_name = "LearnDocument"

    @classmethod
    def get_name(cls):
        return cls.app_name

print(Config.get_name())  # LearnDocument
```

### 3.5 静态方法

静态方法通过 `@staticmethod` 定义，不依赖实例，也不依赖类本身。

```python
class MathTool:
    @staticmethod
    def add(a, b):
        return a + b

print(MathTool.add(3, 4))  # 7
```

### 3.6 方法选择建议

- 需要访问对象数据，用实例方法
- 需要操作类级别信息，用类方法
- 只是工具逻辑，不依赖类状态，用静态方法

## 四、对象的使用方式

这一节先讲对象怎么创建、怎么使用，再去看封装、继承、多态，会更符合学习顺序。

### 4.1 创建对象

```python
class Order:
    def __init__(self, order_id, user_name, amount):
        self.order_id = order_id
        self.user_name = user_name
        self.amount = amount

order = Order("O20260708001", "Tanaka", 1280)
print(order.order_id)  # O20260708001
print(order.amount)    # 1280
```

### 4.2 访问对象属性

```python
class User:
    def __init__(self, name, age):
        self.name = name
        self.age = age

user = User("Tanaka", 28)
print(user.name)  # Tanaka
print(user.age)   # 28
```

### 4.3 调用对象方法

```python
class Order:
    def __init__(self, order_id, amount):
        self.order_id = order_id
        self.amount = amount

    def is_large_order(self):
        return self.amount >= 1000

order = Order("O001", 1280)
print(order.is_large_order())  # True
```

### 4.4 修改对象属性

```python
user = User("Tanaka", 28)
user.age = 29
print(user.age)  # 29
```

### 4.5 对象创建后的常见操作

- 访问属性
- 调用方法
- 修改属性
- 传递给函数
- 放入列表或字典中统一管理

## 五、封装

封装就是隐藏内部实现，只暴露必要接口。

### 5.1 约定式封装

Python 通常用下划线表示“内部使用”。

```python
class User:
    def __init__(self, name):
        self._name = name

user = User("Tanaka")
print(user._name)  # Tanaka
```

这里 `_name` 表示“内部属性”，不是强制私有，但约定外部尽量不要直接访问。

### 5.2 使用 property

`@property` 可以让方法看起来像属性一样访问。

```python
class User:
    def __init__(self, name):
        self._name = name

    @property
    def name(self):
        return self._name

user = User("Tanaka")
print(user.name)  # Tanaka
```

`property` 常见有三种写法：

### 5.2.1 只读属性

只读属性可以读取，不能直接修改。

```python
class User:
    def __init__(self, name):
        self._name = name

    @property
    def name(self):
        return self._name

user = User("Tanaka")
print(user.name)  # Tanaka
# user.name = "Sato"  # 错误，属性没有 setter
```

### 5.2.2 可读可写属性

读取和修改都由 `property` 管理。

```python
class User:
    def __init__(self, name):
        self._name = name

    @property
    def name(self):
        return self._name

    @name.setter
    def name(self, value):
        self._name = value

user = User("Tanaka")
print(user.name)  # Tanaka

user.name = "Sato"
print(user.name)  # Sato
```

### 5.2.3 带校验的属性

企业开发中最常见的是“修改时自动校验数据是否合法”。

```python
class User:
    def __init__(self, name):
        self._name = name

    @property
    def name(self):
        return self._name

    @name.setter
    def name(self, value):
        if not value:
            raise ValueError("姓名不能为空")

        self._name = value

user = User("Tanaka")
print(user.name)  # Tanaka

user.name = "Sato"
print(user.name)  # Sato

# user.name = ""  # ValueError: 姓名不能为空
```

### 5.2.4 这三种写法的区别

- 只读属性：适合不希望外部修改的字段
- 可读可写属性：适合需要统一管理读写的字段
- 带校验的属性：适合需要保证数据合法性的业务字段

### 5.3 封装的意义

- 控制数据访问
- 增加校验逻辑
- 减少外部直接修改内部状态

## 六、继承

继承用于复用父类能力。

### 6.1 基本语法

```python
class Person:
    def __init__(self, name, age):
        self.name = name
        self.age = age

class Employee(Person):
    def __init__(self, name, age, emp_no):
        super().__init__(name, age)
        self.emp_no = emp_no

emp = Employee("Tanaka", 28, "E001")
print(emp.name)    # Tanaka
print(emp.emp_no)  # E001
```

### 6.2 方法重写

子类可以覆盖父类方法。

```python
class Person:
    def greet(self):
        return "Hello"

class Employee(Person):
    def greet(self):
        return "Hello, employee"

emp = Employee()
print(emp.greet())  # Hello, employee
```

### 6.3 `super()` 的作用

`super()` 用来调用父类的方法，常用于初始化父类属性。

### 6.4 多重继承

Python 支持多重继承，但企业项目里要谨慎使用。

```python
class A:
    pass

class B:
    pass

class C(A, B):
    pass
```

### 6.5 继承适用场景

- 子类“是一个”父类
- 子类在父类基础上增加功能
- 子类复用父类通用逻辑

## 七、多态

多态指的是“同一个接口，不同对象有不同表现”。

### 7.1 基本理解

```python
class Cat:
    def speak(self):
        return "meow"

class Dog:
    def speak(self):
        return "woof"

def make_speak(animal):
    print(animal.speak())

make_speak(Cat())  # meow
make_speak(Dog())  # woof
```

### 7.2 多态的价值

- 让代码更灵活
- 便于扩展新类型
- 减少大量 `if/elif`

### 7.3 企业场景

- 不同支付方式
- 不同文件导出方式
- 不同通知发送方式

## 八、组合与委托

很多场景下，组合比继承更好。  
在企业项目里，很多对象关系其实不是“是一个”，而是“有一个”。

### 8.1 组合是什么

组合就是一个对象内部包含另一个对象。

```python
class Engine:
    def start(self):
        return "engine start"

class Car:
    def __init__(self):
        self.engine = Engine()

    def start_engine(self):
        return self.engine.start()

car = Car()
print(car.start_engine())  # engine start
```

这个例子强调的是：

- `Car` 里面“有”一个 `Engine`
- 这是对象之间的包含关系
- `Car` 可以直接使用内部组件的能力

### 8.2 委托是什么

委托是把某个行为交给内部对象处理。  
也就是说，外层对象自己不直接做事，而是把工作交给内部组件。

```python
class Logger:
    def info(self, message):
        return f"[INFO] {message}"

class OrderService:
    def __init__(self):
        self.logger = Logger()

    def create_order(self):
        log_text = self.logger.info("create order start")
        print(log_text)  # [INFO] create order start
        return "order created"

service = OrderService()
print(service.create_order())  # order created
```

这里：

- `OrderService` 关注的是“创建订单”
- `Logger` 关注的是“输出日志”
- `OrderService.create_order()` 把日志记录交给 `Logger`

### 8.3 组合和委托的区别

- 组合强调的是对象之间的包含关系
- 委托强调的是行为转交给谁来做

可以这样记：

- 组合：我里面有你
- 委托：这件事交给你来做

### 8.4 为什么组合常常更好

- 职责更清晰
- 结构更灵活
- 更容易替换内部组件
- 更符合企业项目分层思路
- 不容易形成很深的继承树

### 8.5 企业项目里的常见场景

- `UserService` 组合 `UserRepository`
- `OrderService` 组合 `Logger`
- `PaymentService` 组合 `PaymentClient`
- `ReportService` 组合 `ExcelWriter`

### 8.6 使用建议

- 继承只在“是一个”关系非常明确时使用
- 如果只是复用一部分能力，优先考虑组合
- 如果只是把某个动作交给内部组件处理，可以考虑委托

### 8.7 一个更贴近项目的例子

```python
class TaxCalculator:
    def calculate(self, amount):
        return amount * 1.1

class OrderService:
    def __init__(self):
        self.tax_calculator = TaxCalculator()

    def final_amount(self, amount):
        return self.tax_calculator.calculate(amount)

service = OrderService()
print(service.final_amount(1000))  # 1100.0
```

## 九、魔术方法

### 9.1 什么是魔术方法

魔术方法是 Python 里一类特殊方法，方法名通常以双下划线开头和结尾，例如 `__init__`。  
它们会在特定场景下自动被调用，不需要手动显式调用。

### 9.2 魔术方法的作用

- 控制对象的创建过程
- 控制对象的打印显示
- 控制对象的长度
- 控制对象的比较
- 控制对象的迭代和索引

### 9.3 常用魔术方法总表

| 方法 | 作用 | 不重写时的默认行为 | 示例 |
| --- | --- | --- | --- |
| `__init__` | 初始化对象 | 创建对象时不额外设置属性 | `Product("Book")` |
| `__str__` | 给用户看的字符串 | 显示对象默认地址信息 | `print(obj)` |
| `__repr__` | 给开发者看的字符串 | 显示对象默认地址信息 | `repr(obj)` |
| `__len__` | 返回对象长度 | 不能使用 `len(obj)` | `len(obj)` |
| `__eq__` | 判断是否相等 | 按对象身份比较 | `obj1 == obj2` |
| `__lt__` | 判断是否小于 | 不支持大小比较 | `obj1 < obj2` |
| `__getitem__` | 支持索引访问 | 不支持下标访问 | `obj[0]` |
| `__setitem__` | 支持索引赋值 | 不支持下标赋值 | `obj[0] = x` |
| `__iter__` | 支持迭代 | 不能用于 `for` | `for x in obj` |
| `__contains__` | 支持成员判断 | `in` 可能不可用 | `x in obj` |

### 9.4 `__init__`

`__init__` 是初始化方法，对象创建时自动执行。

```python
class Product:
    def __init__(self, name):
        self.name = name

product = Product("Book")
print(product.name)  # Book
```

如果不写 `__init__`，对象仍然可以创建，但不会自动设置你需要的属性。

### 9.5 `__str__`

`__str__` 用来定义给用户看的对象字符串。

```python
class Product:
    def __init__(self, name):
        self.name = name

    def __str__(self):
        return f"Product({self.name})"

product = Product("Book")
print(product)  # Product(Book)
```

如果不重写 `__str__`，`print(product)` 通常会输出默认对象地址信息，不够友好。

### 9.6 `__repr__`

`__repr__` 更偏向开发调试时的对象表示。

```python
class Product:
    def __init__(self, name):
        self.name = name

    def __repr__(self):
        return f"Product(name={self.name!r})"

product = Product("Book")
print(repr(product))  # Product(name='Book')
```

如果不重写 `__repr__`，调试时看到的也是默认对象地址信息。

### 9.7 `__len__`

`__len__` 让对象可以被 `len()` 调用。

```python
class Basket:
    def __init__(self, items):
        self.items = items

    def __len__(self):
        return len(self.items)

basket = Basket(["A", "B", "C"])
print(len(basket))  # 3
```

如果不重写 `__len__`，`len(basket)` 会报错。

### 9.8 `__eq__`

`__eq__` 控制 `==` 的比较逻辑。

```python
class User:
    def __init__(self, user_id):
        self.user_id = user_id

    def __eq__(self, other):
        return self.user_id == other.user_id

u1 = User(1)
u2 = User(1)
print(u1 == u2)  # True
```

如果不重写 `__eq__`，默认比较的是对象是不是同一个实例，而不是内容是否相同。

### 9.9 `__getitem__`

`__getitem__` 让对象支持下标访问。

```python
class Box:
    def __init__(self, items):
        self.items = items

    def __getitem__(self, index):
        return self.items[index]

box = Box(["A", "B", "C"])
print(box[0])  # A
```

### 9.10 `__iter__`

`__iter__` 让对象可以被 `for` 循环遍历。

```python
class Box:
    def __init__(self, items):
        self.items = items

    def __iter__(self):
        return iter(self.items)

box = Box(["A", "B", "C"])
for item in box:
    print(item)
# A
# B
# C
```

### 9.11 `__contains__`

`__contains__` 让对象支持 `in` 判断。

```python
class Box:
    def __init__(self, items):
        self.items = items

    def __contains__(self, item):
        return item in self.items

box = Box(["A", "B", "C"])
print("B" in box)  # True
```

### 9.12 魔术方法的学习原则

- 先理解作用，不要一开始追底层
- 先会用常见的几个，再逐步扩展
- 不重写时的默认行为也要知道
- 企业项目里只在需要时引入，不要为了“炫技”滥用

## 十、日本项目中的写法

- 类名要能直接表达业务对象
- 方法名要能看出动作
- 属性不要随意暴露
- 组合通常优先于继承
- 日志、状态和对象表示要方便团队确认

## 十一、练习题

### 基础练习

1. 定义一个 `User` 类，包含 `name` 和 `age`。
2. 给 `User` 增加一个 `greet()` 方法。
3. 给 `Product` 类实现 `__str__`。

### 综合练习

1. 用组合实现一个 `OrderService`，内部使用 `Logger`。
2. 为一个集合类实现 `__len__` 和 `__getitem__`。

## 十二、本章总结

- 类是模板，对象是实例
- 属性和方法是对象的核心组成
- 封装、继承、多态、组合和委托是对象建模的关键
- 魔术方法决定对象在很多语法场景下的表现
- 企业项目中，组合和清晰职责通常比复杂继承更重要
