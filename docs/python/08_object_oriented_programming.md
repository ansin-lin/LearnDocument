
# 第8章 面向对象（OOP）

> 🔑 目标：掌握封装 / 继承 / 多态 / 组合等核心，以及属性管理、三类方法、魔术方法、数据类与协议、实战模式。

---

## 为什么需要面向对象？

- **过程式**：把步骤写成函数，数据与操作分散，不同地方容易“散架”。  
- **面向对象**：把数据（状态）和操作（行为）**装到对象里**，对象对外提供方法（接口），内部细节可改变但外部调用方式尽量不变。  
- 结果：更**好维护**（局部变化不影响全局）、更**可复用**（对象可组合）、更**贴近业务**（人-车-订单等“名词即对象”）。

## 1 OOP 核心思想与术语

1. 封装（Encapsulation）
    - 把数据（状态）与操作（行为）封装进对象内部，对外提供受控接口。
    - 目的：隐藏实现细节、降低耦合、保护不变量、便于演进与测试。

2. 继承（Inheritance）
    - 子类获得父类的属性和方法，实现代码复用与行为扩展。

3. 多态（Polymorphism）
    - 同一接口，针对不同对象呈现出不同的实现（方法同名，不同行为）。
    - Python 更强调“鸭子类型”：只要“像鸭子一样叫”，就当它是鸭子。

4. 组合（Composition）
    - 用“对象包含对象”的方式复用与拼装能力，在很多情况下优于“继承层级越来越深”。

---

## 2 类与对象

- **类**：蓝图/模板，描述“这类对象”应该拥有哪些属性（数据）和方法（行为）。  
- **对象**：根据类创建出来的“真实个体”，每个对象有自己的**独立状态**。

```python
class Person:
    # __init__ 是“构造方法”，在创建对象时自动调用
    def __init__(self, name, age):
        self.name = name      # 给“当前对象(self)”绑定属性
        self.age = age

    # 实例方法：描述对象的行为，第一个参数约定名为 self（表示当前对象）
    def introduce(self):
        return f"我叫 {self.name}，今年 {self.age} 岁。"

# === 调用演示 ===
p = Person("Alice", 25)          # 创建对象（实例化）
print(p.name, p.age)             # 访问实例属性 -> Alice 25
print(p.introduce())             # 调用实例方法 -> 我叫 Alice，今年 25 岁。
```

- `self`：实例方法首参的**约定名**（解释器传入）。  
- 构造时进行**校验/建不变量/注入依赖**。

---

## 3 实例属性 vs 类属性

- **实例属性**：写在 `self.xxx = ...`，**每个对象独立**。  
- **类属性**：直接写在类体内（不在方法中），**所有对象共享**。

```python
class Student:
    school = "CS"  # 类属性（共享）
    def __init__(self, name):
        self.name = name  # 实例属性（独立）

a, b = Student("Tom"), Student("Ann")
Student.school = "AI"
print(a.school, b.school)  # AI AI
```

!!! warning "避坑"
    - 在**实例**上写同名属性会**遮蔽**类属性，仅对该实例生效。  
    - 想真正修改类属性，请用 `Class.attr = ...` 或在类方法(`@classmethod`)里用 `cls.attr = ...`。

---

## 4 实例 / 类 / 静态方法

### **一屏对比**

| 方法类型 | 绑定对象 | 首参 | 访问实例状态 | 访问类状态 | 常见用途 | 典型调用 |
|---|---|---|---|---|---|---|
| 实例方法 | 实例 | `self` | ✅ | 可经 `self.__class__` 间接访问 | 读/改实例字段，业务行为 | `obj.m(...)` |
| 类方法 | 类 | `cls` | ❌ | ✅ | 备用构造、类级配置/计数器、工厂 | `Cls.m(...)` / `obj.m(...)` |
| 静态方法 | 无 | 无 | ❌ | ❌ | 与类语义相关且不依赖状态的纯工具 | `Cls.m(...)` / `obj.m(...)` |

### **实例方法（需要 self）**

- **作用**：操作某个实例的状态（读/改 `self.xxx`）。
- **定义**：首参为 `self`（约定）。
- **调用**：`obj.m(...)`（解释器自动把 `self=obj` 绑定）。

```python
class Counter:
    def __init__(self, start=0):
        self.value = start
    def inc(self, step=1):
        self.value += step
        return self.value

c = Counter(10)
print(c.inc())        # 11
print(Counter.inc(c)) # 等价写法（演示绑定原理）
```

### **类方法**

- **作用**：操作类级状态或产出实例（工厂/备用构造器）。
- **定义**：`@classmethod` 修饰，首参为 `cls`（当前类对象）。
- **调用**：`Cls.m(...)` 或 `obj.m(...)`（都会把所属类绑定为 `cls`）。

```python
class App:
    _version = "1.0"
    def __init__(self, name):
        self.name = name
    @classmethod
    def set_version(cls, v):
        cls._version = v
    @classmethod
    def from_env(cls):
        return cls("from_env")
```

### **静态方法**

- **作用**：与类语义相关但不依赖任何状态的纯工具函数。
- **定义**：`@staticmethod` 修饰，无 `self/cls`。
- **调用**：`Cls.m(...)` 或 `obj.m(...)`。

```python
class Math:
    @staticmethod
    def add(a, b):
        return a + b
```

### **选择指南（决策树）**

- 读/改 **实例数据** → 实例方法
- 读/改 **类级数据** 或返回 **本类/子类实例** → 类方法
- 与类相关但 **不依赖状态** → 静态方法（或放模块级）

### **常见坑**

- 在实例上写同名属性会遮蔽类属性;真正要改类属性用 `Class.attr` 或在 `@classmethod` 里 `cls.attr`。
- 滥用 `@staticmethod`：若与类关系不强，应放模块级函数。
- 多态构造错用静态方法：应使用 `@classmethod`，让 `cls` 指向实际子类。
- 误以为类方法能直接访问实例：类方法没有 `self`。

### **综合对照**

```python
class Demo:
    kind = "tool"
    def __init__(self, name):
        self.name = name
    def who_am_i(self):
        return f"{self.name} ({self.kind})"
    @classmethod
    def change_kind(cls, k):
        cls.kind = k
    @classmethod
    def from_upper(cls, name):
        return cls(name.upper())
    @staticmethod
    def valid_name(s):
        return isinstance(s, str) and s.strip() != ""
```

## 5 封装与属性管理

### 5.1 可见性约定

- public：不以下划线开头，对外可访问。
- _protected：单下划线，表示“内部使用”，约定外部不直接访问。
- __private：双下划线，触发名称重整，避免子类无意覆盖（不是绝对安全，仅温和保护）。

```python
class Box:
    def __init__(self):
        self.pub = 1; self._inner = 2; self.__secret = 3
print(Box()._Box__secret)  # 名称重整访问
```

### 5.2 `@property`（受控属性，首选）

把方法“伪装成属性”（受控读写/只读/派生值）

- 适合做**校验**、**只读派生**（如摄氏↔华氏）、**保持 API 简洁**。

```python
class Celsius:
    def __init__(self, c):
        self._c = float(c)            # 约定内部存放用 _c

    @property
    def celsius(self):                # 读属性 celsius（不加括号）
        return self._c

    @celsius.setter
    def celsius(self, v):             # 写属性 celsius 时触发校验
        v = float(v)
        if v < -273.15:
            raise ValueError("低于绝对零度")
        self._c = v

    @property
    def fahrenheit(self):             # 只读派生属性
        return self._c * 9/5 + 32

# === 调用演示 ===
t = Celsius(0)
print(t.celsius, t.fahrenheit)  # -> 0.0 32.0
t.celsius = 100
print(t.celsius, t.fahrenheit)  # -> 100.0 212.0
```

### 5.3 访问拦截与描述符

- `__getattr__(self, name)`：**常规找不到属性时**才调用，适合“懒加载/代理/默认值”。  
- `__getattribute__(self, name)`：**每次访问**都调用，威力大且危险，请谨慎。  
- `__setattr__(self, name, value)`：拦截**所有属性写入**，可做审计/冻结。

这三个方法属于 “属性访问拦截器”，也叫 “魔法方法（Magic Methods）”。
Python 中几乎一切访问 obj.xxx 的行为，都会经过它们。

可以理解为：

- 平常访问属性像“走正门”，
- 这三个方法是“在门口加保安”，
- 你可以在任何访问或赋值发生时插入自己的逻辑。

- 三者的区别与作用场景

| 方法名                              | 触发时机        | 用来干嘛                    | 典型场景         |
| :------------------------------- | :---------- | :---------------------- | :----------- |
| `__getattr__(self, name)`        | 访问不存在的属性时触发 | 给**不存在的属性**一个默认值、懒加载或容错 | “如果没有，就临时生成” |
| `__getattribute__(self, name)`   | 访问任何属性时都触发  | **统一监控、日志、权限检查、代理转发**   | “所有访问都要过我”   |
| `__setattr__(self, name, value)` | 给属性赋值时触发    | **限制修改、类型校验、自动转换**      | “设置属性时自动做点事” |

- 自动补充或默认属性（用 `__getattr__`）
      - 当用户访问不存在的属性时，不想直接报错，而是“动态计算”或“提供默认值”。

```python
class Config:
    def __init__(self):
        self.db_host = "localhost"

    def __getattr__(self, name):
        # 当访问不存在的属性时才会被调用
        print(f"属性 {name} 不存在，返回默认值 None")
        return None

cfg = Config()
print(cfg.db_host)  # 正常存在 -> localhost
print(cfg.timeout)  # 不存在 -> 自动返回 None 而不是报错
```

- 拦截所有属性访问（用 `__getattribute__`）
      - 想在每次访问属性时都做一些事情，比如：
        - 打印日志;
        - 检查权限;
        - 实现“代理对象”（把访问转发给另一个对象）。

```python
class UserProxy:
    def __init__(self, real_user):
        object.__setattr__(self, "real_user", real_user)

    def __getattribute__(self, name):
        print(f"正在访问属性：{name}")
        real_user = object.__getattribute__(self, "real_user")
        return getattr(real_user, name)  # 转发给真实对象

user = {"name": "Tom", "age": 18}
proxy = UserProxy(user)

print(proxy["name"])  # => 打印“正在访问属性：__getitem__”
```

- 拦截赋值操作（用 `__setattr__`）
      - 当你希望对赋值过程“插手”，比如：
          - 校验数据是否合法;
          - 自动类型转换;
          - 实现只读属性;
          - 记录“谁改了这个值”。

```python
class User:
    def __setattr__(self, name, value):
        if name == "age" and value < 0:
            raise ValueError("年龄不能为负数")
        print(f"设置属性 {name} = {value}")
        object.__setattr__(self, name, value)

u = User()
u.name = "Tom"   # 设置属性 name = Tom
u.age = 18       # 设置属性 age = 18
u.age = -3       # 报错：年龄不能为负数
```

---

### 5.4 `__slots__`

**`__slots__`是类级变量**，用来显式声明对象允许有哪些属性。
这样 Python 就不会为每个实例创建一个可扩展的字典（**`__dict__`**），从而：

- 节省内存；
- 限制属性种类（防止随意增加属性）；
- 访问速度略快。

```python
class User:
    __slots__ = ('name', 'age')  # 仅允许这两个属性存在

    def __init__(self, name, age):
        self.name = name
        self.age = age

u = User("Tom", 18)
print(u.name)     # ✅ 正常访问
u.name = "Jack"   # ✅ 可以修改已定义的属性

u.city = "Tokyo"  # ❌ 报错：'User' object has no attribute 'city'
```

- 背后机制
平常每个对象都带一个 `__dict__`：

```python
obj.__dict__ = {'name': 'Tom', 'age': 18}
```

但定义了 `__slots__` 后，对象不再有 `__dict__`（除非你手动加上）。
这样:

- 不能随意新增属性；
- 内存布局更紧凑；
- 适合大量小对象的场景（如百万条数据记录）。

- 常见细节

| 情况                | 说明                                                     |
| ----------------- | ------------------------------------------------------ |
| 子类未定义 `__slots__` | 子类仍然可以动态添加属性                                           |
| 子类定义 `__slots__`  | 需显式包含父类的 slots，否则父类字段无法使用                              |
| 想保留动态属性           | 可以这样写：`__slots__ = ('name', 'age', '__dict__')`        |

---

## 6 继承（Inheritance）

一个类（子类）从另一个类（父类）继承属性和方法，以便代码复用、扩展功能或保持层次结构。
子类会自动获得父类的属性与方法，可以直接用，也可以重写（override）。

### 基本语法

```python
class Animal:                  # 父类（基类）
    def eat(self):
        print("动物在吃")

class Dog(Animal):             # 子类（派生类）
    def bark(self):
        print("狗在叫")

d = Dog()
d.eat()    # ✅ 继承自 Animal  输出：动物在吃
d.bark()   # ✅ 自己的方法   输出：狗在叫
```

子类 Dog 继承了父类 Animal 的 eat() 方法。
这就是继承的核心：**代码复用 + 扩展新功能**。

### 构造方法的继承与扩展

如果子类没有写 `__init__()`，就会自动继承父类的构造方法。
如果你写了，就会覆盖父类的，需要自己手动调用父类的构造逻辑。

```python
#继承父类构造方法
class Animal:
    def __init__(self, name):
        self.name = name

class Dog(Animal):
    pass

dog = Dog("小白")
print(dog.name)  # 小白
```

```python
#子类重写并扩展构造方法（用 super()）
class Animal:
    def __init__(self, name):
        self.name = name

class Dog(Animal):
    def __init__(self, name, breed):
        super().__init__(name)   # 调用父类的构造方法
        self.breed = breed

dog = Dog("小白", "柴犬")
print(dog.name, dog.breed)  # 小白 柴犬
```

- super() 是父类的一个“代理对象”，用来调用父类方法。
- 它遵循 MRO（方法解析顺序），在多继承中非常重要。

### 方法重写（Override）

子类中可以定义与父类同名的方法，以实现“替换父类逻辑”的效果。

```python
class Animal:
    def speak(self):
        print("动物在叫")

class Dog(Animal):
    def speak(self):     # 重写父类方法
        print("狗在汪汪叫")

Dog().speak()  # 狗在汪汪叫
```

如果仍想在子类中调用父类的逻辑，可以使用：

```python
class Dog(Animal):
    def speak(self):
        super().speak()   # 调用父类方法
        print("狗在汪汪叫")
```

### 属性继承规则

1. 子类可以访问父类的属性；
2. 子类可定义与父类同名属性 → 会覆盖；
3. `super()` 可以访问父类版本的属性。

```python
class A:
    x = 10

class B(A):
    x = 20

print(A.x)   # 10
print(B.x)   # 20
```

### 多重继承

Python 允许一个类继承多个父类：

```python
class A:
    def greet(self):
        print("A say hi")

class B:
    def greet(self):
        print("B say hello")

class C(A, B):   # 同时继承 A 和 B
    pass

C().greet()  # A say hi
```

👉 说明：Python 遵循 MRO（Method Resolution Order）方法解析顺序。
优先查找从左到右的继承顺序。

可以查看解析顺序：

```python
print(C.__mro__) #(<class '__main__.C'>, <class '__main__.A'>, <class '__main__.B'>, <class 'object'>)
```

### object 类

在 Python 3 中，所有类都默认继承自 object：

```python
class A:
    pass

print(A.__mro__)
# (<class '__main__.A'>, <class 'object'>)
```

因此即使你不写 (object)，也自动继承。

### 继承的类型总结

| 类型   | 说明            | 语法                            |
| ---- | ------------- | ----------------------------- |
| 单继承  | 一个子类继承一个父类    | `class Dog(Animal):`          |
| 多重继承 | 一个子类继承多个父类    | `class C(A, B):`              |
| 多层继承 | 一层继承另一层       | `class C(B):` 且 `class B(A):` |
| 组合继承 | 类中既继承又组合使用其他类 | 在类中以属性形式包含另一个类实例              |

### 什么时候使用继承？

- 多个类有相似属性或方法 → 提取到父类中；
- 想在子类中复用父类逻辑；
- 想通过“多态”统一接口行为；
- 想用 super() 链式调用多个类的初始化。

### 继承 vs 组合

| 对比项 | 继承（is-a）     | 组合（has-a）     |
| :-- | :----------- | :------------ |
| 含义  | 子类是父类的一种     | 类中“包含”另一个类    |
| 关系  | Dog 是 Animal | Dog 拥有一个 Tail |
| 优点  | 代码复用，层级清晰    | 灵活，不受父类结构限制   |
| 缺点  | 耦合度高，结构僵化    | 代码略繁琐但解耦      |

```python
# 继承关系
class Animal: pass
class Dog(Animal): pass  # Dog 是 Animal

# 组合关系
class Tail: pass
class Dog:
    def __init__(self):
        self.tail = Tail()  # Dog 拥有 Tail
```

### 总结

class SubClass(BaseClass): 表示继承。
子类自动拥有父类的属性与方法。
可通过 super() 调用父类逻辑。
Python 支持多重继承，遵循 MRO 顺序。
合理使用继承能大幅提高代码复用性与结构清晰度。

---

## 7 多态

多态（Polymorphism） 意思是：“同一个接口，不同对象表现出不同的行为”。

简单来说：

- 同样的方法名；
- 调用时根据对象的真实类型执行不同逻辑。

```python
class Animal:
    def speak(self):
        print("动物在叫")

class Dog(Animal):
    def speak(self):
        print("狗在汪汪叫")

class Cat(Animal):
    def speak(self):
        print("猫在喵喵叫")

def make_sound(animal):
    animal.speak()

# 传入不同对象
make_sound(Dog())   # 狗在汪汪叫
make_sound(Cat())   # 猫在喵喵叫
```

`make_sound()` 函数不需要知道参数是 Dog 还是 Cat，
它只关心 “这个对象有没有 speak() 方法”。
这就是 多态的精髓：接口统一，行为多样。

### Python 的多态是“动态多态”

在像 Java、C++ 这样的静态语言中，多态依赖类型声明，
但 Python 是动态语言，天生就支持多态（鸭子类型 Duck Typing）。

```mkdocs
🦆 “如果它走起来像鸭子，叫起来也像鸭子，那它就是鸭子。”
```

即：
只要一个对象**实现了相应的方法**，无论它是什么类，都可以被当作某种类型使用。

```python
class Duck:
    def quack(self):
        print("嘎嘎")

class Person:
    def quack(self):
        print("我学鸭子叫：嘎嘎")

def make_it_quack(obj):
    obj.quack()  # 不关心类型，只要求有 quack() 方法

make_it_quack(Duck())        #嘎嘎
make_it_quack(Person())      #我学鸭子叫：嘎嘎
```

Python 的多态是基于行为而非类型，
这也称为 **行为多态（behavioral polymorphism）**。

### 继承 + 重写 = 实现多态的基础

多数情况下，多态通过继承来实现。
父类定义一个接口，子类重写它。
这样就能在统一的结构下实现不同表现。

```python
class Shape:
    def area(self):
        raise NotImplementedError("子类必须实现 area()")

class Circle(Shape):
    def __init__(self, r):
        self.r = r
    def area(self):
        return 3.14 * self.r ** 2

class Rectangle(Shape):
    def __init__(self, w, h):
        self.w = w
        self.h = h
    def area(self):
        return self.w * self.h

shapes = [Circle(3), Rectangle(4, 5)] 
for s in shapes:
    print(s.area())  #28.26  20
```

> 多态的关键：
> 同样调用 s.area()，但不同类给出不同实现。
> 这让代码更通用、可扩展、易维护。

### Python 多态的分类

| 类型         | 含义               | 示例                           |
| :--------- | :--------------- | :--------------------------- |
| **继承多态**   | 父类引用指向子类对象       | `animal.speak()`             |
| **运算符多态**  | 同一运算符对不同类型行为不同   | `"a"+"b"`, `1+2`, `[1]+[2]`  |
| **函数多态**   | 同一函数处理不同类型对象     | `len("abc")`, `len([1,2,3])` |
| **鸭子类型多态** | 不依赖继承，只要实现同名方法即可 | `obj.quack()`                |

### 运算符的多态（运算符重载）

Python 中的运算符本身就是多态的例子：

```python
print(1 + 2)          # 数字相加
print("a" + "b")      # 字符串拼接
print([1] + [2, 3])   # 列表合并

#底层实际上分别调用了：
int.__add__
str.__add__
list.__add__
```

👉 这叫做**运算符重载（Operator Overloading）**，
它让 + 在不同类型上具有不同含义，也是多态的一种体现。

### 接口多态（面向抽象编程）

多态的最大意义在于“面向抽象编程”：
你可以针对“行为接口”编程，而不是具体类型。

```python
class File:
    def read(self): pass

class TextFile(File):
    def read(self): return "读取文本文件"

class JsonFile(File):
    def read(self): return "读取 JSON 文件"

def load_file(file: File):
    print(file.read())

load_file(TextFile())
load_file(JsonFile())
```

✅ 新增一个 CsvFile(File) 类，也能直接被 load_file() 调用，
而不用改原函数逻辑 —— 这就是“可扩展性”的体现。

### 和“方法重载”区别

| 对比项         | 方法重载（Overloading） | 多态（Polymorphism） |
| :---------- | :---------------- | :--------------- |
| 发生位置        | 同一个类中             | 父类与子类之间          |
| 参数差异        | 参数数量或类型不同         | 相同接口，不同实现        |
| Python 是否支持 | 仅伪重载（动态参数）        | ✅ 天生支持           |
| 核心思想        | 同名多实现（按参数选）       | 同接口多表现（按对象选）     |

### 实际项目中多态的好处

1. 可扩展性强

```python
def draw(shape):
    shape.area()  # 不用改这行
```

你可以随时新增 Triangle, Ellipse 类，而无需改 draw()。
2. 接口统一
让上层代码只关心“能干什么”，而不关心“是谁干的”。
3. 低耦合
不同模块可独立实现自己的行为，只需遵守同一个接口。
4. 测试更方便
多态让你可以在测试中用“假对象（mock）”替换真对象。

### 关键点总结

| 概念               | 含义                    |
| ---------------- | --------------------- |
| 多态（Polymorphism） | 同一方法名，作用于不同对象，表现出不同行为 |
| 实现基础             | 继承 + 方法重写             |
| Python 特点        | 动态多态，基于行为（鸭子类型）       |
| 优点               | 代码复用、扩展性强、易维护         |
| 常见体现             | 函数多态、运算符多态、接口多态       |

### 多态总结

多态让代码“面向接口”而不是“面向实现”。
它是让你的 Python 程序从“能跑”变成“优雅且可扩展”的关键

---

## 8 组合与委托

### 前置理解：继承的局限

我们先回顾继承的特点👇

```python
class Animal:
    def speak(self):
        print("动物在叫")

class Dog(Animal):
    def speak(self):
        print("狗在汪汪叫")
```

继承确实方便，但有这些问题：

- ❌ 子类强依赖父类结构（耦合度高）；
- ❌ 修改父类会影响所有子类；
- ❌ 继承只能表达“is-a”关系，而不是“has-a”；
- ❌ 多重继承结构复杂，容易出错。
于是，就有了更灵活的两种替代方案：
👉 **组合（Composition）** 与 **委托（Delegation）**

### 组合（Composition）详解

>组合是一种“has-a（拥有）”关系。
>指一个类把另一个类的对象作为自己的属性，以此实现功能复用。
换句话说：
>“不是去继承它，而是去包含它。”

```python
#组合关系

class Engine:
    def start(self):
        print("引擎启动")

class Car:
    def __init__(self):
        self.engine = Engine()   # Car 拥有一个 Engine

    def drive(self):
        print("汽车开始行驶")
        self.engine.start()      # 调用引擎的功能

car = Car()     #汽车开始行驶
car.drive()     #引擎启动
```

👉 Car 通过“拥有” Engine 的实例来使用它的功能。
这种“Car has a Engine”的关系就是 组合。

| 优点        | 说明                       |
| --------- | ------------------------ |
| ✅ 低耦合     | 修改 Engine 不会影响 Car 的继承结构 |
| ✅ 灵活      | 可以自由组合不同组件（如换不同 Engine）  |
| ✅ 更贴近现实建模 | “汽车有引擎”比“汽车是引擎”更合理       |
| ✅ 可替换性强   | 不同实现类可随时替换               |

```python
#可替换的组件组合
class ElectricEngine:
    def start(self):
        print("电动引擎启动")

class GasEngine:
    def start(self):
        print("汽油引擎启动")

class Car:
    def __init__(self, engine):
        self.engine = engine    # 通过组合注入不同引擎

    def drive(self):
        self.engine.start()

Car(ElectricEngine()).drive()   #电动引擎启动
Car(GasEngine()).drive()        #汽油引擎启动
```

>✅ 这就是“组合 + 多态”的典型应用。
>Car 并不关心 engine 是哪种类型，只要有 .start() 方法即可。

### 委托（Delegation）详解

>委托（Delegation） 是一种行为转发：
>当前类不自己实现某个功能，而是把这个请求交给另一个对象去做。
也就是说：
>“我不干这活，我让别人（被委托对象）干。”

```python
#简单的委托
class Printer:
    def print_doc(self):
        print("打印文档...")

class Computer:
    def __init__(self):
        self.printer = Printer()

    def print(self):
        # 将请求“委托”给 Printer 对象
        self.printer.print_doc()

pc = Computer()
pc.print()      #打印文档...
```

委托是“行为转发”，而组合是“结构包含”。
两者经常一起出现，但不是一回事。

- 组合 vs 委托 的区别

| 对比项      | 组合（Composition） | 委托（Delegation）      |
| :------- | :-------------- | :------------------ |
| 含义       | 一个对象“拥有”另一个对象   | 一个对象“把请求交给”另一个对象    |
| 关系       | **has-a**       | **uses-a**          |
| 目的       | 结构复用            | 行为转发                |
| 实现方式     | 属性引用另一个对象       | 方法内部调用另一个对象的方法      |
| 示例       | Car 包含 Engine   | Computer 调用 Printer |
| 是否一定转发调用 | ❌ 不一定（可独立使用）    | ✅ 必定是调用转发           |

- 委托的高级写法：动态转发

有时候我们希望“自动”把未知属性的访问都委托给某个对象。
可以用 `__getattr__` 或 `__getattribute__` 实现。

```python
#动态委托
class Proxy:
    def __init__(self, target):
        self._target = target

    def __getattr__(self, name):
        # 当 Proxy 自己没有这个属性时，自动转发到 target
        return getattr(self._target, name)

class RealObject:
    def action(self):
        print("执行真实对象的操作")

proxy = Proxy(RealObject())
proxy.action()  # 实际调用的是 RealObject.action()  执行真实对象的操作
```

>这就是 Python 版的“代理模式（Proxy Pattern）”。
>Proxy 通过委托转发，实现了“控制访问”的中间层。

- 组合 + 委托 在设计模式中的典型应用

| 模式名称                  | 使用方式           | 示例        |
| --------------------- | -------------- | --------- |
| **代理模式 (Proxy)**      | 用委托转发访问目标对象    | 网络代理、权限控制 |
| **装饰器模式 (Decorator)** | 组合 + 委托包装功能    | 日志包装、缓存功能 |
| **适配器模式 (Adapter)**   | 组合旧对象 + 委托接口调用 | 兼容不同接口    |
| **组合模式 (Composite)**  | 层层组合子对象        | 树形结构（如目录） |
| **策略模式 (Strategy)**   | 组合不同算法对象       | 动态切换算法逻辑  |

- 继承 vs 组合 vs 委托

| 特性   | 继承 (is-a)     | 组合 (has-a)    | 委托 (uses-a)         |
| ---- | ------------- | ------------- | ------------------- |
| 关系类型 | 父子关系          | 拥有关系          | 使用关系                |
| 耦合性  | 高             | 低             | 低                   |
| 代码复用 | 通过继承获得        | 通过组合复用        | 通过调用复用              |
| 扩展方式 | 重写父类          | 更换组件          | 更换代理对象              |
| 示例   | `Dog(Animal)` | `Car(Engine)` | `Proxy(RealObject)` |

- 实战对比示例：继承 vs 组合+委托

```python
#使用继承（耦合太强）
class FileLogger:
    def log(self, msg):
        print("[File] " + msg)

class App(FileLogger):
    def run(self):
        self.log("App start")
```

问题：
如果要改成数据库日志，还得改继承关系。

```python
✅ 使用组合+委托（更灵活）
class FileLogger:
    def log(self, msg):
        print("[File] " + msg)

class DBLogger:
    def log(self, msg):
        print("[DB] " + msg)

class App:
    def __init__(self, logger):
        self.logger = logger    # 组合
    def run(self):
        self.logger.log("App start")  # 委托

App(FileLogger()).run()     #[File] App start
App(DBLogger()).run()       #[DB] App start
```

> 不用改 App 类，只需传入不同的 logger 对象。
> 这正是“组合优于继承”的体现。

### 思想总结

| 思想     | 一句话解释             |
| ------ | ----------------- |
| **继承** | “我就是它” → 复用结构但耦合高 |
| **组合** | “我有它” → 复用功能且灵活   |
| **委托** | “我让它干” → 复用行为且可控制 |

>面向对象编程的黄金法则：
>优先使用组合与委托，而非继承。
---
