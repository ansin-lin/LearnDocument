# Java 后端全栈基础到企业级面试教程

> 适合对象：Java 基础学习者、1~3 年 Java 后端开发、准备 Java / Spring Boot / 后端工程师面试的人。  
> 学习目标：建立 Java 后端知识体系，理解常见面试问题背后的原理，并能结合项目进行表达。

---

## 一、Java 基础层（必须掌握）

### 1. Java 基础语法

#### 1.1 基本数据类型（8 种）

Java 有 8 种基本数据类型：

| 类型 | 占用空间 | 默认值 | 说明 |
| --- | ---: | ---: | --- |
| byte | 1 字节 | 0 | 小整数 |
| short | 2 字节 | 0 | 短整数 |
| int | 4 字节 | 0 | 最常用整数 |
| long | 8 字节 | 0L | 长整数 |
| float | 4 字节 | 0.0f | 单精度浮点数 |
| double | 8 字节 | 0.0d | 双精度浮点数 |
| char | 2 字节 | '\u0000' | 字符，使用 Unicode |
| boolean | JVM 未明确规定 | false | true / false |

#### 重点理解

基本数据类型不是对象，直接存储值，性能较高。它们通常存放在线程栈中的局部变量表中，或者作为对象字段存储在堆中的对象内部。

#### 面试常问

--问：int 和 Integer 有什么区别？--

答：`int` 是基本数据类型，存储具体数值；`Integer` 是包装类，是对象，可以为 `null`，可以用于集合、泛型等只支持引用类型的场景。

---

#### 1.2 引用类型

引用类型包括：

- 类：`String`、`User`、`Object`
- 接口：`List`、`Map`
- 数组：`int[]`、`String[]`
- 枚举：`enum`
- 注解：`@interface`

引用类型变量保存的是对象的引用地址，而不是对象本身。对象本身通常存储在堆内存中。

```java
User user = new User();
```

这里 `user` 是引用变量，`new User()` 创建的对象在堆中。

---

#### 1.3 自动装箱 / 拆箱

自动装箱：基本类型自动转换为包装类。

```java
Integer a = 10; // 等价于 Integer.valueOf(10)
```

自动拆箱：包装类自动转换为基本类型。

```java
Integer a = 10;
int b = a; // 等价于 a.intValue()
```

#### 常见坑

```java
Integer a = 100;
Integer b = 100;
System.out.println(a == b); // true

Integer c = 200;
Integer d = 200;
System.out.println(c == d); // false
```

原因：`Integer` 对 `-128 ~ 127` 有缓存，超出范围会创建新对象。

#### 面试常问

--问：为什么 Integer 的 == 有时候是 true，有时候是 false？--

答：因为 `Integer.valueOf()` 对 `-128 到 127` 范围内的整数使用缓存。这个范围内相同数值返回同一个对象，所以 `==` 为 true；超出范围通常创建新对象，所以 `==` 为 false。比较包装类的值应该使用 `equals()`。

---

#### 1.4 运算符

Java 常见运算符：

| 类型 | 示例 |
| --- | --- |
| 算术运算符 | `+ - - / %` |
| 赋值运算符 | `= += -= -= /=` |
| 比较运算符 | `> < >= <= == !=` |
| 逻辑运算符 | `&& \|\| !` |
| 位运算符 | `& | ^ ~ << >> >>>` |
| 三目运算符 | `条件 ? 值1 : 值2` |

#### && 和 & 的区别

`&&` 是短路与，如果前面为 false，后面不执行。  
`&` 既可以做逻辑与，也可以做位运算；作为逻辑与时，两边都会执行。

```java
int a = 1;
if (a > 2 && ++a > 1) {
}
System.out.println(a); // 1
```

---

#### 1.5 流程控制（if / for / switch）

#### if

适合复杂条件判断。

```java
if (score >= 90) {
    System.out.println("优秀");
} else if (score >= 60) {
    System.out.println("及格");
} else {
    System.out.println("不及格");
}
```

#### for

适合明确次数的循环。

```java
for (int i = 0; i < 10; i++) {
    System.out.println(i);
}
```

#### switch

适合固定值匹配。

```java
switch (status) {
    case 1:
        System.out.println("启用");
        break;
    case 0:
        System.out.println("禁用");
        break;
    default:
        System.out.println("未知");
}
```

Java 7 开始支持 `String` 作为 switch 条件。

---

#### 1.6 数组

数组是长度固定、类型相同的一组数据。

```java
int[] nums = {1, 2, 3};
System.out.println(nums[0]);
```

#### 特点

- 查询快：通过下标直接访问。
- 插入删除慢：可能需要移动元素。
- 长度固定：创建后不能改变。

---

### 2. 面向对象（OOP）

#### 2.1 封装 / 继承 / 多态

#### 封装

封装是把属性私有化，通过方法对外暴露访问入口。

```java
public class User {
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
```

好处：保护数据、降低耦合、提高可维护性。

#### 继承

继承是子类复用父类的属性和方法。

```java
class Animal {
    public void eat() {
        System.out.println("吃东西");
    }
}

class Dog extends Animal {
}
```

Java 是单继承，一个类只能继承一个父类，但可以实现多个接口。

#### 多态

多态是同一个引用类型，在运行时表现出不同的对象行为。

```java
Animal animal = new Dog();
animal.eat();
```

多态的前提：继承或实现、方法重写、父类引用指向子类对象。

---

#### 2.2 抽象类 vs 接口

| 对比项 | 抽象类 | 接口 |
| --- | --- | --- |
| 关键字 | `abstract class` | `interface` |
| 继承数量 | 单继承 | 可多实现 |
| 成员变量 | 可以有普通变量 | 默认 `public static final` |
| 方法 | 可有抽象方法和普通方法 | Java 8 后可有 default / static 方法 |
| 构造方法 | 可以有 | 不能有 |
| 适用场景 | 表示“是什么” | 表示“能做什么” |

#### 示例

```java
abstract class Animal {
    abstract void eat();
}

interface Flyable {
    void fly();
}
```

#### 面试表达

抽象类更适合描述一类对象的共同属性和行为，接口更适合定义能力或规范。例如 `Bird extends Animal implements Flyable`。

---

#### 2.3 重写（override）vs 重载（overload）

| 对比项 | 重写 Override | 重载 Overload |
| --- | --- | --- |
| 发生位置 | 父子类之间 | 同一个类中 |
| 方法名 | 相同 | 相同 |
| 参数列表 | 相同 | 不同 |
| 返回值 | 相同或子类 | 可不同，但不能只靠返回值区分 |
| 作用 | 改变父类方法实现 | 提供多种调用方式 |

```java
class Animal {
    public void eat() {}
}

class Dog extends Animal {
    @Override
    public void eat() {}

    public void eat(String food) {}
}
```

---

#### 2.4 多态实现原理

Java 的多态主要依赖：

- 继承 / 接口实现
- 方法重写
- 动态绑定
- 虚方法表

编译阶段看引用类型，运行阶段看实际对象类型。

```java
Animal animal = new Dog();
animal.eat();
```

编译器只知道 `animal` 是 `Animal` 类型，但运行时 JVM 发现实际对象是 `Dog`，所以调用 `Dog` 的 `eat()` 方法。

---

#### 2.5 final / static / this / super

#### final

- 修饰类：不能被继承。
- 修饰方法：不能被重写。
- 修饰变量：只能赋值一次。

```java
final int age = 18;
```

#### static

属于类，不属于对象。

```java
public static int count;
public static void test() {}
```

静态方法不能直接访问非静态成员，因为非静态成员依赖对象存在。

#### this

表示当前对象。

```java
this.name = name;
```

#### super

表示父类对象部分。

```java
super.eat();
```

---

#### 2.6 访问限定符的范围

| 修饰符 | 同类 | 同包 | 子类 | 不同包 |
| --- | --- | --- | --- | --- |
| private | ✅ | ❌ | ❌ | ❌ |
| default | ✅ | ✅ | ❌ | ❌ |
| protected | ✅ | ✅ | ✅ | ❌ |
| public | ✅ | ✅ | ✅ | ✅ |

---

### 3. Java 核心类库

#### 3.1 Object（equals / hashCode / toString）

所有 Java 类都直接或间接继承自 `Object`。

#### equals

默认比较对象地址，通常需要重写为比较对象内容。

```java
@Override
public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    User user = (User) o;
    return Objects.equals(id, user.id);
}
```

#### hashCode

返回对象的哈希值。用于 HashMap、HashSet 等哈希集合。

#### equals 和 hashCode 关系

- 两个对象 `equals()` 为 true，`hashCode()` 必须相同。
- 两个对象 `hashCode()` 相同，`equals()` 不一定为 true。

#### toString

默认输出类名和哈希值，实际项目中常重写用于日志输出。

---

#### 3.2 String / StringBuilder / StringBuffer

| 类型 | 是否可变 | 是否线程安全 | 适用场景 |
| --- | --- | --- | --- |
| String | 不可变 | 安全 | 少量字符串操作 |
| StringBuilder | 可变 | 不安全 | 单线程大量拼接 |
| StringBuffer | 可变 | 安全 | 多线程大量拼接 |

#### String 为什么不可变？

`String` 内部字符数组不可修改。不可变带来的好处：

- 线程安全
- 可以缓存 hashCode
- 可以放入字符串常量池
- 适合作为 HashMap 的 key

---

#### 3.3 包装类（Integer、Long 等）

包装类让基本类型具备对象能力，可以用于：

- 集合
- 泛型
- 反射
- 数据库字段映射
- JSON 序列化

项目中实体类字段通常使用包装类，而不是基本类型。

```java
private Integer age;
```

原因：包装类可以表示 `null`，适合区分“没有值”和“值为 0”。

---

#### 3.4 BigDecimal（金融必备）

金融系统中不能用 `double` 表示金额，因为浮点数存在精度问题。

```java
System.out.println(0.1 + 0.2); // 0.30000000000000004
```

应该使用 `BigDecimal`。

```java
BigDecimal a = new BigDecimal("0.1");
BigDecimal b = new BigDecimal("0.2");
System.out.println(a.add(b)); // 0.3
```

#### 注意

不要使用 `new BigDecimal(0.1)`，因为传入的 double 本身已经不精确。

推荐：

```java
new BigDecimal("0.1");
BigDecimal.valueOf(0.1);
```

---

#### 3.5 时间 API（LocalDateTime、Instant、ZoneId）

Java 8 引入了新的时间 API。

| 类 | 说明 |
| --- | --- |
| LocalDate | 日期，不含时间 |
| LocalTime | 时间，不含日期 |
| LocalDateTime | 日期 + 时间，不含时区 |
| Instant | 时间戳，通常表示 UTC 时间点 |
| ZoneId | 时区 |
| ZonedDateTime | 带时区的日期时间 |

```java
LocalDateTime now = LocalDateTime.now();
Instant instant = Instant.now();
ZoneId tokyo = ZoneId.of("Asia/Tokyo");
instant.atZone(tokyo);
```

#### 项目建议

- 页面展示：`LocalDateTime`
- 跨时区系统：`Instant + ZoneId`
- 数据库存储：根据项目统一规范，常见为 UTC 时间戳或本地时间。

---

## 二、集合框架（重点）

### 1. Collection 体系

`Collection` 是单列集合的顶层接口，常见子接口有：

- List：有序，可重复
- Set：无序，不重复
- Queue：队列
- Deque：双端队列

---

### 2. List

#### 2.1 ArrayList

底层是动态数组。

特点：

- 查询快
- 尾部添加快
- 中间插入、删除慢
- 线程不安全

扩容机制：容量不足时扩容为原来的约 1.5 倍。

适合：读多写少、按下标访问多的场景。

---

#### 2.2 LinkedList

底层是双向链表。

特点：

- 插入删除较方便
- 随机访问慢
- 额外存储前后节点引用，占用内存较多

实际项目中 `ArrayList` 使用频率远高于 `LinkedList`。

---

### 3. Set

#### 3.1 HashSet

底层基于 `HashMap` 实现。

特点：

- 元素不可重复
- 无序
- 查询速度快

判断重复依赖 `hashCode()` 和 `equals()`。

---

#### 3.2 TreeSet

底层基于红黑树。

特点：

- 元素不重复
- 可以排序
- 查询、插入、删除时间复杂度通常为 O(log n)

元素需要实现 `Comparable`，或者传入 `Comparator`。

---

### 4. Queue / Deque

### Queue

队列，先进先出。

```java
Queue<String> queue = new LinkedList<>();
queue.offer("A");
queue.poll();
```

### Deque

双端队列，两端都可以插入和删除。

```java
Deque<String> deque = new ArrayDeque<>();
deque.addFirst("A");
deque.addLast("B");
```

---

### 5. Map 体系

#### 5.1 HashMap（高频）

`HashMap` 是键值对集合，底层结构在 Java 8 中是：

数组 + 链表 + 红黑树。

#### put 流程简化说明

1. 计算 key 的 hash 值。
2. 根据 hash 定位数组下标。
3. 如果位置为空，直接插入。
4. 如果位置不为空，比较 key 是否相同。
5. key 相同则覆盖 value。
6. key 不同则挂到链表或红黑树上。
7. 元素数量超过阈值则扩容。

#### 为什么 HashMap 的容量通常是 2 的幂？

因为可以用 `(n - 1) & hash` 快速计算下标，比取模效率更高，并且在容量为 2 的幂时分布更均匀。

#### 面试常问

--问：HashMap 是线程安全的吗？--

答：不是。多线程同时 put 可能导致数据覆盖、数据丢失等问题。多线程场景应使用 `ConcurrentHashMap`。

---

#### 5.2 LinkedHashMap

`LinkedHashMap` 继承自 `HashMap`，额外维护一条双向链表。

特点：

- 保持插入顺序
- 可以实现 LRU 缓存

```java
LinkedHashMap<String, Integer> map = new LinkedHashMap<>();
```

---

#### 5.3 TreeMap

底层是红黑树，key 有序。

特点：

- 按 key 排序
- 查询、插入、删除复杂度 O(log n)
- key 需要可比较

---

#### 5.4 ConcurrentHashMap

线程安全的 HashMap。

Java 8 中主要使用 CAS + synchronized 控制并发。

特点：

- 支持高并发访问
- 锁粒度较小
- 不允许 key 或 value 为 null

#### 为什么不允许 null？

因为在并发环境下，`get(key)` 返回 null 时无法判断是 key 不存在，还是 value 本身就是 null。

---

## 三、异常 & IO & 并发基础

### 1. 异常

#### 1.1 checked / unchecked

Java 异常体系：

```text
Throwable
 ├── Error
 └── Exception
      ├── RuntimeException
      └── 其他受检异常
```

#### checked exception

编译期必须处理的异常，例如：

- IOException
- SQLException

#### unchecked exception

运行时异常，编译期不强制处理，例如：

- NullPointerException
- IndexOutOfBoundsException
- IllegalArgumentException

---

#### 1.2 try-catch-finally

```java
try {
    // 可能发生异常的代码
} catch (Exception e) {
    // 异常处理
} finally {
    // 无论是否异常通常都会执行
}
```

`finally` 常用于关闭资源。但现在更推荐 `try-with-resources`。

```java
try (InputStream in = new FileInputStream("a.txt")) {
    // 使用资源
}
```

---

#### 1.3 自定义异常

项目中常用自定义业务异常。

```java
public class BusinessException extends RuntimeException {
    public BusinessException(String message) {
        super(message);
    }
}
```

使用：

```java
if (user == null) {
    throw new BusinessException("用户不存在");
}
```

---

### 2. IO

#### 2.1 BIO / NIO / AIO

| 模型 | 说明 | 特点 |
| --- | --- | --- |
| BIO | Blocking IO | 同步阻塞 |
| NIO | Non-blocking IO | 同步非阻塞，多路复用 |
| AIO | Asynchronous IO | 异步非阻塞 |

#### BIO

一个连接通常对应一个线程，简单但并发能力弱。

#### NIO

通过 Selector 管理多个 Channel，适合高并发网络通信。

#### AIO

异步完成后通过回调通知，编程模型更复杂。

---

#### 2.2 File / Stream / Channel / Buffer

#### File

表示文件或目录路径。

#### Stream

传统 IO 流，按字节或字符读取。

- InputStream / OutputStream
- Reader / Writer

##### 字节流（Byte Stream）与字符流（Character Stream）

###### 什么是字节流

字节流（Byte Stream）以 --字节（Byte）-- 为单位读取和写入数据。

Java 中的字节流体系：

```java
InputStream
OutputStream
```

常见实现类：

```java
FileInputStream
FileOutputStream
BufferedInputStream
BufferedOutputStream
ObjectInputStream
ObjectOutputStream
```

###### 特点

- 以字节为单位处理数据
- 可以处理任何类型文件
- 不关心文件编码格式
- 适用于图片、视频、音频、压缩包等二进制文件

###### 示例

```java
FileInputStream fis = new FileInputStream("test.txt");

int data;
while ((data = fis.read()) != -1) {
    System.out.println(data);
}
```

输出的是字节值：

```text
72
101
108
108
111
```

对应字符：

```text
Hello
```

---

###### 什么是字符流

字符流（Character Stream）以 --字符（Character）-- 为单位读取和写入数据。

Java 中的字符流体系：

```java
Reader
Writer
```

常见实现类：

```java
FileReader
FileWriter
BufferedReader
BufferedWriter
InputStreamReader
OutputStreamWriter
```

###### 特点

- 以字符为单位处理数据
- 自动完成编码和解码
- 适用于文本文件处理
- 读取中文更加方便

###### 示例

```java
FileReader fr = new FileReader("test.txt");

int ch;
while ((ch = fr.read()) != -1) {
    System.out.print((char) ch);
}
```

输出：

```text
Hello
```

---

###### 字节流与字符流的区别

| 对比项 | 字节流 | 字符流 |
| ------ | -------------------------- | ----------------------- |
| 操作单位 | 字节(Byte) | 字符(Character) |
| 父类 | InputStream / OutputStream | Reader / Writer |
| 是否处理编码 | 否 | 是 |
| 是否适合中文 | 不方便 | 方便 |
| 适用场景 | 图片、视频、音频、压缩包等 | txt、json、xml、html 等文本文件 |

---

###### 为什么需要字符流

假设文件内容为：

```text
中国
```

UTF-8编码实际存储为：

```text
中 = E4 B8 AD
国 = E5 9B BD
```

共占用：

```text
6个字节
```

###### 使用字节流读取

```java
FileInputStream fis = new FileInputStream("test.txt");
```

读取结果：

```text
228
184
173
229
155
189
```

得到的是原始字节。

---

###### 使用字符流读取

```java
FileReader fr = new FileReader("test.txt");
```

读取结果：

```text
中
国
```

字符流会自动根据编码将字节转换成字符。

---

###### 字符流底层原理

字符流本质上也是字节流。

例如：

```java
FileReader
```

底层实际上使用：

```java
FileInputStream
```

结构如下：

```text
文件
 ↓
FileInputStream
 ↓
编码解码
 ↓
FileReader
 ↓
字符
```

因此可以理解为：

```text
字符流 = 字节流 + 编码转换
```

---

###### 为什么会出现乱码

乱码本质原因：

```text
编码与解码方式不一致
```

例如：

文件编码：

```text
UTF-8
```

读取时使用：

```text
GBK
```

结果可能变成：

```text
涓浗
```

因此开发中应保证：

```text
保存编码 == 读取编码
```

推荐统一使用：

```text
UTF-8
```

---

###### BufferedReader 为什么效率更高

普通读取：

```java
FileReader
```

每次读取一个字符。

---

BufferedReader：

```java
BufferedReader br =
    new BufferedReader(
        new FileReader("test.txt")
    );
```

会先读取一块数据（默认约8KB）到内存缓冲区，再从缓冲区读取。

优点：

- 减少磁盘IO次数
- 提高读取性能

---

###### 实际开发中的选择

####### 文本文件

例如：

```text
txt
json
xml
html
csv
sql
```

推荐：

```java
BufferedReader
BufferedWriter
Files.readString()
Files.writeString()
```

---

####### 二进制文件

例如：

```text
jpg
png
mp4
zip
pdf
docx
xlsx
```

推荐：

```java
FileInputStream
FileOutputStream
BufferedInputStream
BufferedOutputStream
```

---

###### 面试总结

####### 字节流和字符流有什么区别？

字节流以字节为单位处理数据，可以处理任何类型文件；字符流以字符为单位处理数据，本质上是字节流加上字符编码转换功能，主要用于文本文件处理。

开发中通常遵循：

```text
文本文件 → 字符流

图片、视频、压缩包 → 字节流
```

一句话记忆：

字节流 = 原始数据搬运工

字符流 = 字节流 + 编码解码器

---

#### Channel

NIO 中的数据通道，可以双向读写。

#### Buffer

缓冲区，NIO 读写数据需要经过 Buffer。

---

#### 2.3 序列化与反序列化

序列化：对象转成字节流。  
反序列化：字节流转成对象。

```java
class User implements Serializable {
    private static final long serialVersionUID = 1L;
}
```

常见场景：

- 网络传输
- 缓存存储
- RPC 调用
- Session 持久化

#### 注意

反序列化不可信数据可能存在安全风险，生产系统要谨慎。

---

### 3. 并发基础

#### 3.1 线程创建方式

常见方式：

1. 继承 Thread
2. 实现 Runnable
3. 实现 Callable
4. 使用线程池

推荐使用线程池，而不是频繁 new Thread。

---

#### 3.2 synchronized

`synchronized` 是 Java 内置锁，可以保证：

- 原子性
- 可见性
- 有序性

使用方式：

```java
public synchronized void test() {}

synchronized (this) {
}
```

锁对象不同，锁的范围也不同。

---

#### 3.3 volatile

`volatile` 保证变量的可见性和一定程度的有序性，但不保证复合操作的原子性。

```java
private volatile boolean running = true;
```

#### 面试重点

`volatile` 适合状态标记，不适合 `count++` 这种复合操作。

---

#### 3.4 线程安全问题

多个线程同时访问共享变量，并且至少有一个线程修改它，就可能出现线程安全问题。

```java
count++;
```

这个操作不是原子操作，实际包含：

1. 读取 count
2. 加 1
3. 写回 count

---

#### 3.5 CAS

CAS：Compare And Swap，比较并交换。

它包含三个值：

- 内存值 V
- 旧预期值 A
- 新值 B

如果 V 等于 A，就把 V 改为 B；否则失败重试。

#### 问题

- ABA 问题
- 自旋开销
- 只能保证单个变量的原子操作

---

#### 3.6 Atomic 类

`AtomicInteger`、`AtomicLong` 等基于 CAS 实现原子操作。

```java
AtomicInteger count = new AtomicInteger(0);
count.incrementAndGet();
```

适合简单计数器、状态控制等场景。

---

## 四、JUC 并发包（简单了解）

### 1. ThreadPoolExecutor

线程池核心类。

核心参数：

| 参数 | 说明 |
| --- | --- |
| corePoolSize | 核心线程数 |
| maximumPoolSize | 最大线程数 |
| keepAliveTime | 非核心线程空闲存活时间 |
| workQueue | 任务队列 |
| threadFactory | 线程工厂 |
| rejectedExecutionHandler | 拒绝策略 |

#### 执行流程

1. 线程数小于核心线程数，创建核心线程。
2. 核心线程满了，任务进入队列。
3. 队列满了，创建非核心线程。
4. 达到最大线程数后仍无法处理，执行拒绝策略。

---

### 2. ExecutorService

线程池接口，常用方法：

```java
submit();
execute();
shutdown();
```

`execute()` 没有返回值，`submit()` 可以返回 Future。

---

### 3. Callable / Future / FutureTask

`Callable` 有返回值，可以抛异常。

```java
Callable<Integer> task = () -> 1 + 1;
Future<Integer> future = executor.submit(task);
Integer result = future.get();
```

`Future.get()` 会阻塞等待结果。

---

### 4. CountDownLatch

让一个或多个线程等待其他线程完成。

场景：主线程等待多个子任务结束。

```java
CountDownLatch latch = new CountDownLatch(3);
latch.countDown();
latch.await();
```

---

### 5. CyclicBarrier

让多个线程互相等待，等到指定数量后一起继续执行。

和 `CountDownLatch` 区别：`CyclicBarrier` 可以重复使用。

---

### 6. Semaphore

信号量，用来控制同时访问资源的线程数量。

场景：限流、连接池控制。

```java
Semaphore semaphore = new Semaphore(10);
semaphore.acquire();
semaphore.release();
```

---

### 7. ReentrantLock

可重入锁，比 `synchronized` 更灵活。

特点：

- 可公平 / 非公平
- 可中断
- 可尝试加锁
- 支持多个条件队列

```java
lock.lock();
try {
} finally {
    lock.unlock();
}
```

---

### 8. ReadWriteLock

读写锁。

- 读读共享
- 读写互斥
- 写写互斥

适合读多写少场景。

---

### 9. ForkJoinPool

适合把大任务拆成小任务并行执行，使用工作窃取算法。

常用于并行计算。

---

### 10. CompletableFuture

用于异步编排。

```java
CompletableFuture.supplyAsync(() -> "hello")
        .thenApply(s -> s + " world")
        .thenAccept(System.out::println);
```

适合多个异步任务组合、串行、并行、结果聚合。

---

## 五、JVM（Java Virtual Machine，Java虚拟机）

### JVM是什么

JVM（Java Virtual Machine）即 Java 虚拟机，是运行 Java 程序的核心组件。

主要负责：

- 类加载（Class Loading）
- 内存管理（Memory Management）
- 垃圾回收（Garbage Collection）
- 即时编译（JIT Compilation）

Java 程序能够实现：

```text
Write Once, Run Anywhere
一次编写，到处运行
```

本质上就是依赖 JVM。

---

### 5.1 JVM 内存结构（JVM Memory Structure）

#### 堆（Heap）

Heap（堆）是 JVM 最大的内存区域。

主要存放：

- 对象实例（Object）
- 数组（Array）

例如：

```java
User user = new User();
```

new 创建的对象都存放在 Heap 中。

##### Heap 分代结构

```text
Heap
├── Young Generation（新生代）
│   ├── Eden
│   ├── Survivor From
│   └── Survivor To
│
└── Old Generation（老年代）
```

##### Young Generation（新生代）

存放新创建的对象。

特点：

- 对象产生最多
- GC 最频繁

---

##### Old Generation（老年代）

经过多次 GC 后仍然存活的对象进入老年代。

例如：

```java
Spring Bean
缓存对象
连接池对象
```

---

#### 方法区（Method Area）

用于存储：

- 类信息（Class Metadata）
- 方法信息（Method Metadata）
- 常量池（Runtime Constant Pool）
- 静态变量（Static Variables）

##### JDK8以前

```text
Permanent Generation（PermGen）
永久代
```

##### JDK8开始

```text
MetaSpace（元空间）
```

MetaSpace 使用本地内存（Native Memory）。

常见异常：

```text
java.lang.OutOfMemoryError: Metaspace
```

---

#### Java 虚拟机栈（Java Virtual Machine Stack）

简称：

```text
Stack（栈）
```

特点：

- 线程私有
- 生命周期与线程一致

存储：

- 局部变量（Local Variable）
- 方法参数（Method Parameter）
- 方法调用信息

例如：

```java
public void test() {
    int age = 18;
}
```

变量 age 存放在栈中。

常见异常：

```text
java.lang.StackOverflowError
```

例如无限递归：

```java
public void test(){
    test();
}
```

---

#### 程序计数器（Program Counter Register）

简称：

```text
PC Register
```

作用：

记录当前线程执行到哪条字节码指令。

特点：

- 线程私有
- JVM 唯一不会发生 OOM 的区域

---

#### 本地方法栈（Native Method Stack）

用于执行 Native 方法。

例如：

```java
Thread.start()
Object.hashCode()
```

底层会调用 C/C++ 实现。

---

### 5.2 GC（Garbage Collection，垃圾回收）

GC 的作用：

自动回收无用对象。

避免：

```text
Memory Leak（内存泄漏）
Memory Overflow（内存溢出）
```

---

#### GC Roots（垃圾回收根）

GC 判断对象是否存活的起点。

常见 GC Roots：

- 栈中的引用对象
- 静态变量引用
- 常量池引用
- JNI引用

例如：

```text
GC Roots
   │
   ▼
Object A
   │
   ▼
Object B
```

只要对象能够从 GC Roots 找到，就不会被回收。

---

#### STW（Stop The World）

中文：

```text
暂停世界
```

GC 执行时：

```text
所有用户线程暂停
GC执行完成后恢复
```

STW 时间越短越好。

---

#### Minor GC（Young GC）

回收区域：

```text
Young Generation
新生代
```

特点：

- 频繁发生
- 回收速度快

---

#### Major GC

回收区域：

```text
Old Generation
老年代
```

频率较低。

---

#### Full GC

同时回收：

```text
Young Generation
Old Generation
MetaSpace
```

特点：

```text
最耗性能
停顿时间最长
```

线上系统最怕频繁 Full GC。

---

#### 对象晋升机制（Object Promotion）

对象创建：

```text
Eden
```

经历多次 GC：

```text
Survivor
```

继续存活：

```text
Old Generation
```

这个过程称为：

```text
Object Promotion
对象晋升
```

---

### 5.3 垃圾收集器（Garbage Collector）

#### CMS（Concurrent Mark Sweep）

中文：

```text
并发标记清除
```

特点：

```text
低停顿
```

缺点：

```text
产生内存碎片
```

JDK14后逐渐淘汰。

---

#### G1（Garbage First）

中文：

```text
垃圾优先回收器
```

JDK9以后默认 GC。

特点：

```text
可预测停顿时间
适用于大内存服务器
```

企业项目最常见。

---

#### ZGC（Z Garbage Collector）

特点：

```text
超低停顿
停顿时间通常小于10ms
```

适用于：

```text
TB级内存服务器
```

---

#### Shenandoah

RedHat 推出的低延迟 GC。

特点：

```text
并发回收
低停顿
```

---

### 5.4 JVM 调优（JVM Tuning）

#### 常用 JVM 参数

##### 设置堆大小

```bash
-Xms2g
-Xmx2g
```

含义：

```text
-Xms
Initial Heap Size
初始堆大小

-Xmx
Maximum Heap Size
最大堆大小
```

---

##### 查看GC日志

```bash
-Xlog:gc
```

---

##### 查看详细GC信息

```bash
-XX:+PrintGCDetails
```

---

### 5.5 OOM（Out Of Memory Error，内存溢出）

#### Java Heap Space

堆内存不足：

```text
java.lang.OutOfMemoryError:
Java heap space
```

---

#### Metaspace

元空间不足：

```text
java.lang.OutOfMemoryError:
Metaspace
```

---

#### GC Overhead Limit Exceeded

GC过于频繁。

特点：

```text
98%以上时间在GC
回收效果很差
```

---

### 5.6 JVM问题排查工具

#### MAT（Memory Analyzer Tool）

内存分析工具。

用于分析：

- Heap Dump
- 内存泄漏
- 大对象

---

#### JVisualVM

JDK 自带监控工具。

可查看：

- CPU
- Memory
- Thread
- GC

---

##### Arthas

阿里巴巴开源诊断工具。

常用命令：

```bash
dashboard
thread
heapdump
jad
watch
trace
```

线上排查非常常见。

---

##### JVM面试高频英文缩写

| 缩写 | 英文全称 | 中文 |
| --------- | ------------------------------ | --------- |
| JVM | Java Virtual Machine | Java虚拟机 |
| JIT | Just In Time Compiler | 即时编译器 |
| GC | Garbage Collection | 垃圾回收 |
| OOM | Out Of Memory Error | 内存溢出 |
| STW | Stop The World | 暂停世界 |
| PC | Program Counter | 程序计数器 |
| CMS | Concurrent Mark Sweep | 并发标记清除 |
| G1 | Garbage First | 垃圾优先 |
| ZGC | Z Garbage Collector | Z垃圾回收器 |
| Heap | Heap Memory | 堆 |
| Stack | Java Virtual Machine Stack | 栈 |
| MetaSpace | MetaSpace | 元空间 |
| Young GC | Minor GC | 新生代GC |
| Full GC | Full Garbage Collection | 完整GC |
| GC Roots | Garbage Collection Roots | GC根对象 |
| MAT | Memory Analyzer Tool | 内存分析工具 |
| JFR | Java Flight Recorder | Java飞行记录器 |
| JMC | Java Mission Control | Java监控工具 |
| TLAB | Thread Local Allocation Buffer | 线程本地分配缓冲区 |

---

##### 面试一句话总结

JVM 负责 Java 程序运行，其核心包括：

- 内存管理（Heap、Stack、MetaSpace）
- 垃圾回收（GC）
- 即时编译（JIT）
- 性能调优（JVM Tuning）

实际项目中最常见的问题主要集中在：

```text
OOM
Full GC
CPU过高
内存泄漏
线程阻塞
```

而 JVM 调优和问题排查通常通过：

```text
GC日志
JVisualVM
MAT
Arthas
```

完成分析。

---

## 六、Web 基础

### 1. HTTP / HTTPS

HTTP 是应用层协议，基于请求响应模型。HTTPS = HTTP + TLS/SSL 加密。

HTTPS 解决：

- 数据加密
- 身份认证
- 防止数据被篡改

---

### 2. TCP/IP 三次握手

建立 TCP 连接需要三次握手：

1. 客户端发送 SYN(请求建立连接)。
2. 服务端返回 SYN + ACK(确认收到)。
3. 客户端返回 ACK。

目的：确认双方发送和接收能力都正常。

---

### 3. 状态码

| 状态码 | 含义 |
| --- | --- |
| 200 | 成功 |
| 201 | 创建成功 |
| 301 | 永久重定向 |
| 302 | 临时重定向 |
| 400 | 请求参数错误 |
| 401 | 未认证 |
| 403 | 无权限 |
| 404 | 资源不存在 |
| 500 | 服务器内部错误 |
| 502 | 网关错误 |
| 503 | 服务不可用 |
| 504 | 网关超时 |

---

### 4. Cookie / Session

Cookie 存在客户端浏览器。  
Session 存在服务端。

流程：

1. 用户登录。
2. 服务端创建 Session。
3. 返回 SessionId 给浏览器。
4. 浏览器后续请求携带 Cookie。
5. 服务端根据 SessionId 找到用户状态。

---

### 5. RESTful 规范

RESTful 是一种接口设计风格，强调资源和统一操作。

示例：

```text
GET /users 查询用户列表
GET /users/1 查询单个用户
POST /users 创建用户
PUT /users/1 修改用户
DELETE /users/1 删除用户
```

特点：

- URL 表示资源
- HTTP 方法表示动作
- 使用状态码表达结果
- 返回 JSON 数据

---

### 6. JSON / XML

JSON 更轻量，前后端交互中最常用。

XML 标签更复杂，但在老系统、配置文件、银行和企业系统中仍然常见。

---

### 7. CORS 跨域

浏览器同源策略限制不同源请求。

不同源指：协议、域名、端口任一不同。

解决方式：

- 后端配置 CORS
- 网关代理
- Nginx 反向代理

Spring Boot 示例：

```java
@CrossOrigin
@RestController
public class UserController {
}
```

---

## 七、Java Web & 后端框架

### 1. Servlet & Web 容器（简单了解）

#### Servlet 是什么？

Servlet（Server Applet）是运行在服务器端的 Java 程序，用于接收客户端请求并返回响应结果。

简单来说：

```text
浏览器发送请求
        ↓
     Servlet
        ↓
     业务处理
        ↓
返回响应结果
```

Servlet 是 Java Web 开发最基础的技术，也是 Spring MVC 的底层基础。

---

#### Servlet 的主要作用

1. 接收客户端请求（Request）
2. 处理业务逻辑
3. 调用 Service、DAO 等组件
4. 返回响应（Response）

请求流程：

```text
浏览器
   ↓
Tomcat
   ↓
Servlet
   ↓
Service
   ↓
DAO
   ↓
数据库
```

---

#### Servlet 生命周期

Servlet 由 Tomcat 管理，生命周期包括：

##### 1. init()

初始化方法。

Tomcat 创建 Servlet 时执行一次。

```java
public void init()
```

---

##### 2. service()

处理客户端请求。

每次请求都会执行。

```java
protected void service(
    HttpServletRequest request,
    HttpServletResponse response
)
```

---

##### 3. destroy()

销毁方法。

Tomcat 关闭时执行一次。

```java
public void destroy()
```

---

#### 生命周期流程

```text
Tomcat启动
    ↓
 init()
    ↓

请求1
    ↓
 service()

请求2
    ↓
 service()

请求3
    ↓
 service()

Tomcat关闭
    ↓
 destroy()
```

---

#### HttpServlet

实际开发中通常继承：

```java
HttpServlet
```

而不是直接实现 Servlet 接口。

常见方法：

```java
doGet()
```

处理 GET 请求。

```java
doPost()
```

处理 POST 请求。

---

#### Servlet 与 Tomcat 的关系

```text
Tomcat = Servlet容器
Servlet = 请求处理组件
```

Tomcat 负责：

- 创建 Servlet
- 调用 Servlet
- 管理 Servlet 生命周期
- 销毁 Servlet

因此 Servlet 不能独立运行，必须依赖 Tomcat 等 Web 容器。

---

#### Servlet 与 Spring MVC 的关系

Spring MVC 底层也是基于 Servlet 实现的。

核心组件：

```java
DispatcherServlet
```

本质上也是一个 Servlet。

继承关系：

```text
HttpServlet
    ↑
FrameworkServlet
    ↑
DispatcherServlet
```

因此：

```text
Spring MVC
    ↓
DispatcherServlet
    ↓
Servlet
```

---

#### 面试回答

Servlet 是运行在服务器端的 Java 程序，用于接收客户端请求并返回响应结果。Servlet 由 Tomcat 等 Web 容器管理生命周期，主要经历 init、service、destroy 三个阶段。Spring MVC 底层也是基于 Servlet 实现的，其核心 DispatcherServlet 本质上就是一个 Servlet。

---

#### 一句话记忆

```text
Servlet 负责处理请求，
Tomcat 负责运行 Servlet。
```

#### Servlet 生命周期

运行在服务器上的 Java 程序，用来处理客户端请求并返回响应。

Servlet 生命周期：

1. 加载和实例化
2. init()
3. service()
4. destroy()

`service()` 根据请求方法分发到 `doGet()`、`doPost()` 等方法。

---

#### 1.2 Filter / Listener

#### Filter

过滤器，用于请求进入 Servlet 前后处理。

常见用途：

- 登录校验
- 编码处理
- 日志记录
- 权限控制

#### Listener

监听器，用于监听 Web 应用、Session、Request 等对象的创建和销毁。

---

#### 1.3 Tomcat 架构

##### Tomcat 是什么？

Tomcat（Apache Tomcat）是 Apache 提供的一个 --Servlet 容器（Servlet Container）-- 和 --Web 服务器（Web Server）--，用于运行 Java Web 应用程序。

简单来说：

```text
Servlet 负责处理请求
Tomcat 负责运行 Servlet
```

---

##### Tomcat 的主要作用

1. 监听端口（如 8080）
2. 接收 HTTP 请求
3. 解析 HTTP 协议
4. 调用对应的 Servlet
5. 返回 HTTP 响应

请求流程：

```text
浏览器
   ↓
HTTP请求
   ↓
Tomcat
   ↓
Servlet
   ↓
Service
   ↓
DAO
   ↓
数据库
```

---

##### Tomcat 与 Servlet 的关系

Servlet 本身只是一个 Java 类，无法独立运行。

Tomcat 负责：

- 创建 Servlet
- 调用 Servlet
- 管理 Servlet 生命周期
- 销毁 Servlet

因此：

```text
Tomcat = Servlet 容器
Servlet = 业务处理组件
```

---

##### Spring Boot 与 Tomcat

Spring Boot 默认内置 Tomcat。

启动项目时：

```java
SpringApplication.run(App.class, args);
```

实际上会：

```text
启动 Spring 容器
↓
启动内嵌 Tomcat
↓
监听 8080 端口
↓
等待客户端请求
```

启动日志中经常看到：

```text
Tomcat started on port(s): 8080
```

---

##### 面试回答

Tomcat 是 Apache 提供的 Servlet 容器和 Web 服务器，用于运行 Java Web 应用。它负责监听端口、接收 HTTP 请求、调用 Servlet 处理业务逻辑并返回响应。Spring MVC 和 Spring Boot 底层都依赖 Servlet，而 Servlet 通常运行在 Tomcat 中。

---

##### 一句话记忆

```text
Tomcat 负责运行 Servlet，
Servlet 负责处理请求。
```

Tomcat 是 Servlet 容器。

核心组件：

- Server
- Service
- Connector
- Container
- Engine
- Host
- Context
- Wrapper

简单理解：Connector 负责接收请求，Container 负责处理请求。

---

### 2. Spring 核心

#### 2.1 IOC / DI

IOC：控制反转，把对象创建和管理交给 Spring 容器。  
DI：依赖注入，Spring 把对象需要的依赖注入进去。

传统写法：

```java
UserService service = new UserServiceImpl();
```

Spring 写法：

```java
@Autowired
private UserService userService;
```

好处：降低耦合，方便测试和扩展。

---

#### 2.2 Bean 生命周期

简化流程：

1. 实例化 Bean
2. 属性注入
3. Aware 接口回调
4. BeanPostProcessor 前置处理
5. 初始化方法
6. BeanPostProcessor 后置处理
7. Bean 可使用
8. 容器关闭时销毁 Bean

---

#### 2.3 BeanFactory vs ApplicationContext

| 对比项 | BeanFactory | ApplicationContext |
| --- | --- | --- |
| 定位 | 基础容器 | 高级容器 |
| Bean 创建 | 延迟加载 | 默认预加载单例 Bean |
| 功能 | 基础 IOC | 国际化、事件、AOP 等 |
| 使用频率 | 底层 | 实际项目常用 |

---

#### 2.4 AOP 原理

AOP：面向切面编程，把通用逻辑从业务代码中抽离出来。

常见用途：

- 日志
- 事务
- 权限
- 性能监控

Spring AOP 主要基于动态代理：

- JDK 动态代理：基于接口
- CGLIB：基于子类继承

---

#### 2.5 事务管理

Spring 通过 `@Transactional` 管理事务。

```java
@Transactional
public void createOrder() {
}
```

事务四大特性 ACID：

- Atomicity 原子性
- Consistency 一致性
- Isolation 隔离性
- Durability 持久性

#### 常见失效场景

- 方法不是 public
- 同类内部方法调用
- 异常被 catch 后没有抛出
- 数据库引擎不支持事务
- 没有被 Spring 管理

---

### 3. Spring Boot（核心）

#### 3.1 自动装配原理

Spring Boot 通过自动配置减少手动配置。

核心机制：

- `@SpringBootApplication`
- `@EnableAutoConfiguration`
- AutoConfiguration 配置类
- 条件注解 `@ConditionalOnClass`、`@ConditionalOnMissingBean`

简单理解：Spring Boot 根据 classpath 中存在的依赖，自动配置相关 Bean。

---

#### 3.2 Starter 机制

Starter 是一组依赖的集合。

例如：

```xml
spring-boot-starter-web
```

它会引入 Spring MVC、Tomcat、Jackson 等 Web 开发常用依赖。

好处：减少依赖版本冲突，简化配置。

---

#### 3.3 SpringApplication.run()

主要做了：

1. 创建 SpringApplication 对象。
2. 推断应用类型。
3. 加载环境变量和配置。
4. 创建 ApplicationContext。
5. 刷新容器。
6. 启动内嵌 Web 容器。
7. 执行 Runner。

---

#### 3.4 配置文件加载顺序

常见配置文件：

- application.properties
- application.yml
- application-dev.yml
- application-prod.yml

优先级通常外部配置高于内部配置，命令行参数优先级较高。

---

#### 3.5 Profiles

用于区分不同环境配置。

```yaml
spring:
  profiles:
    active: dev
```

常见环境：

- dev 开发
- test 测试
- prod 生产

---

#### 3.6 Actuator

Spring Boot Actuator 用于监控应用。

常见端点：

- `/actuator/health`
- `/actuator/info`
- `/actuator/metrics`
- `/actuator/env`

生产环境要注意权限控制，不能随便暴露敏感端点。

---

#### 3.7 常用注解

| 注解 | 说明 |
| --- | --- |
| @SpringBootApplication | 启动类注解 |
| @RestController | 返回 JSON 的 Controller |
| @Controller | MVC Controller |
| @Service | Service 层 Bean |
| @Repository | DAO 层 Bean |
| @Component | 普通组件 |
| @Autowired | 自动注入 |
| @Resource | 按名称或类型注入 |
| @Value | 注入配置值 |
| @Configuration | 配置类 |
| @Bean | 注册 Bean |
| @Transactional | 事务 |
| @RequestMapping | 请求映射 |
| @GetMapping | GET 请求 |
| @PostMapping | POST 请求 |

---

### 4. Spring MVC

#### 4.1 DispatcherServlet

`DispatcherServlet` 是 Spring MVC 的前端控制器，所有请求先进入它。

处理流程：

1. 接收请求。
2. 找 HandlerMapping。
3. 找到 Controller。
4. 调用 HandlerAdapter。
5. 执行业务方法。
6. 返回 ModelAndView 或 JSON。
7. 视图解析或响应数据。

---

#### 4.2 Controller / RestController

`@Controller` 通常返回页面。  
`@RestController` = `@Controller + @ResponseBody`，通常返回 JSON。

---

#### 4.3 HandlerMapping

负责根据请求 URL 找到对应的 Controller 方法。

---

#### 4.4 参数绑定

常见参数注解：

```java
@RequestParam
@PathVariable
@RequestBody
@RequestHeader
```

示例：

```java
@GetMapping("/users/{id}")
public User getUser(@PathVariable Long id) {
    return userService.getById(id);
}
```

---

#### 4.5 异常处理（@ControllerAdvice）

全局异常处理：

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(BusinessException.class)
    public Result<?> handle(BusinessException e) {
        return Result.fail(e.getMessage());
    }
}
```

好处：统一返回格式，避免 Controller 中重复 try-catch。

---

## 八、数据层

### 1. ORM

#### 1.1 MyBatis

MyBatis 是半自动 ORM 框架，需要自己写 SQL。

优点：

- SQL 可控
- 适合复杂查询
- 学习成本适中
- 国内项目使用广泛

核心组件：

- Mapper 接口
- XML 映射文件
- SqlSession
- Executor

---

#### 1.2 MyBatis-Plus

MyBatis-Plus 是 MyBatis 增强工具。

优点：

- 简化 CRUD
- 条件构造器
- 分页插件
- 代码生成器

示例：

```java
userMapper.selectById(1L);
```

---

#### 1.3 JPA / Hibernate

JPA 是规范，Hibernate 是实现。

特点：

- 面向对象操作数据库
- SQL 自动生成
- 适合领域模型清晰的项目

缺点：复杂 SQL 不如 MyBatis 灵活。

---

### 2. 数据库

#### 2.1 SQL 基础语法

常用 SQL：

```sql
SELECT - FROM user WHERE id = 1;
INSERT INTO user(name, age) VALUES('Tom', 18);
UPDATE user SET age = 20 WHERE id = 1;
DELETE FROM user WHERE id = 1;
```

---

#### 2.2 索引原理

索引是帮助数据库快速查找数据的数据结构。

MySQL InnoDB 常用 B+ 树索引。

优点：提高查询速度。  
缺点：占用空间，降低写入速度。

#### 索引失效常见情况

- 对索引列使用函数
- 左模糊查询 `%abc`
- 隐式类型转换
- OR 条件使用不当
- 违反最左前缀原则

---

#### 2.3 事务隔离级别

| 隔离级别 | 脏读 | 不可重复读 | 幻读 |
| --- | --- | --- | --- |
| Read Uncommitted | 可能 | 可能 | 可能 |
| Read Committed | 不会 | 可能 | 可能 |
| Repeatable Read | 不会 | 不会 | 可能 |
| Serializable | 不会 | 不会 | 不会 |

MySQL InnoDB 默认是 Repeatable Read。

---

#### 2.4 SQL 优化

常见思路：

1. 使用合适索引。
2. 避免 select -。
3. 减少大表全表扫描。
4. 小表驱动大表。
5. 分页深度优化。
6. 避免在索引列上使用函数。
7. 用 explain 分析执行计划。

---

#### 2.5 慢查询分析

步骤：

1. 开启慢查询日志。
2. 找到慢 SQL。
3. 使用 EXPLAIN 分析。
4. 查看是否走索引。
5. 优化 SQL 或索引。
6. 必要时优化表结构或缓存。

---

## 九、缓存 & 消息中间件

### 1. Redis（重点）

Redis 是高性能内存数据库，常用于缓存、分布式锁、计数器、排行榜等。

---

#### 1.1 数据结构

| 数据结构 | 常见用途 |
| --- | --- |
| String | 缓存、计数器 |
| Hash | 对象存储 |
| List | 队列、列表 |
| Set | 去重、标签 |
| ZSet | 排行榜 |
| Bitmap | 签到 |
| HyperLogLog | UV 统计 |
| Stream | 消息流 |

---

#### 1.2 持久化

Redis 持久化方式：

#### RDB

定时生成快照。恢复快，占用小，但可能丢失最近数据。

#### AOF

记录写命令。数据更安全，但文件较大，恢复可能较慢。

实际项目中可以两者结合。

---

#### 1.3 缓存雪崩 / 穿透 / 击穿

#### 缓存雪崩

大量 key 同时过期，导致请求全部打到数据库。

解决：

- 过期时间加随机值
- 热点数据不过期
- 限流降级

#### 缓存穿透

查询不存在的数据，缓存和数据库都没有。

解决：

- 缓存空值
- 布隆过滤器
- 参数校验

#### 缓存击穿

热点 key 过期，大量请求同时访问数据库。

解决：

- 加互斥锁
- 热点 key 逻辑过期
- 提前刷新缓存

---

### 2. 消息队列

消息队列用于异步、解耦、削峰填谷。

#### 2.1 Kafka

特点：

- 高吞吐
- 分布式日志系统
- 适合大数据、日志、行为数据

#### 2.2 RabbitMQ

特点：

- 功能丰富
- 路由灵活
- 适合业务消息

#### 2.3 RocketMQ

特点：

- 支持事务消息
- 适合金融、电商等业务场景
- 阿里系生态中常见

---

## 十、DevOps & 运维

### 1. Linux 常用命令

```bash
ls        # 查看目录
cd        # 切换目录
pwd       # 当前路径
mkdir     # 创建目录
rm        # 删除
cp        # 复制
mv        # 移动/重命名
cat       # 查看文件
less      # 分页查看
 tail -f  # 实时查看日志
ps -ef    # 查看进程
kill      # 杀进程
df -h     # 查看磁盘
top       # 查看系统资源
```

---

### 2. Shell 脚本

Shell 可用于自动化运维。

```bash
#!/bin/bash
APP_NAME=myapp.jar

pid=$(ps -ef | grep $APP_NAME | grep -v grep | awk '{print $2}')
if [ -n "$pid" ]; then
  kill -9 $pid
fi

nohup java -jar $APP_NAME > app.log 2>&1 &
```

---

### 3. Docker

Docker 是容器化技术，可以把应用和运行环境一起打包。

核心概念：

- Image 镜像
- Container 容器
- Dockerfile
- Volume
- Network

常用命令：

```bash
docker build -t myapp .
docker run -d -p 8080:8080 myapp
docker ps
docker logs -f container_id
```

---

### 4. Docker Compose

用于管理多个容器。

```yaml
services:
  app:
    image: myapp
    ports:
      - "8080:8080"
  redis:
    image: redis
```

启动：

```bash
docker compose up -d
```

---

### 5. Kubernetes（K8s）

K8s 是容器编排平台。

核心概念：

- Pod
- Deployment
- Service
- ConfigMap
- Secret
- Ingress
- Namespace

适合微服务、大规模容器管理。

---

### 6. CI/CD

CI：持续集成。  
CD：持续交付 / 持续部署。

常见工具：

- Jenkins
- GitHub Actions
- GitLab CI

典型流程：

1. 提交代码。
2. 自动拉取代码。
3. 编译构建。
4. 自动测试。
5. 打包镜像。
6. 部署到服务器。

---

## 十一、安全 & 权限

### 1. Spring Security

Spring Security 是 Spring 生态中的安全框架。

功能：

- 认证
- 授权
- 防 CSRF
- 密码加密
- 登录登出

---

### 2. OAuth2

OAuth2 是授权协议，常用于第三方登录和开放平台授权。

典型场景：

用户使用 Google / GitHub / 微信账号登录第三方应用。

---

### 3. JWT

JWT 是一种 Token 格式，由三部分组成：

```text
Header.Payload.Signature
```

特点：

- 服务端无状态
- 适合前后端分离
- 可携带用户信息和权限信息

注意：JWT 一旦签发，在过期前通常不容易主动失效，所以要设计合理过期时间和刷新机制。

---

### 4. RBAC 权限模型

RBAC：基于角色的访问控制。

核心模型：

```text
用户 -> 角色 -> 权限
```

例如：

- 用户 A 拥有管理员角色
- 管理员角色拥有用户新增、删除、修改权限

---

### 5. 接口防刷

常见方案：

- IP 限流
- 用户限流
- Token 校验
- 验证码
- Redis 计数器
- 网关限流
- 黑名单机制

---

## 十二、测试

### 1. JUnit 5

Java 单元测试框架。

```java
@Test
void testAdd() {
    Assertions.assertEquals(2, 1 + 1);
}
```

常用注解：

- @Test
- @BeforeEach
- @AfterEach
- @BeforeAll
- @AfterAll

---

### 2. Mockito

用于 Mock 依赖对象。

```java
when(userMapper.selectById(1L)).thenReturn(user);
```

适合 Service 层单元测试，不依赖真实数据库。

---

### 3. Spring Boot Test

用于 Spring Boot 集成测试。

```java
@SpringBootTest
class UserServiceTest {
}
```

可以加载 Spring 容器，适合测试多个组件协作。

---

### 4. 接口测试（Postman）

Postman 可用于测试 HTTP 接口。

重点：

- 请求方法
- URL
- Header
- Body
- Token
- 响应状态码
- 响应 JSON

---

### 5. 性能测试（JMeter）

JMeter 用于压测接口性能。

关注指标：

- QPS
- TPS
- 响应时间
- 错误率
- 吞吐量
- CPU / 内存 / DB 负载

---

## 十三、架构与设计模式

### 1. 设计模式

#### 1.1 单例模式

保证一个类只有一个实例。

常见场景：配置类、工具类、连接管理器。

---

#### 1.2 工厂模式

把对象创建逻辑封装起来。

适合根据不同条件创建不同对象。

---

#### 1.3 策略模式

把不同算法封装成不同策略。

例如：不同支付方式、不同优惠计算方式。

---

#### 1.4 模板方法模式

父类定义流程，子类实现具体步骤。

适合流程固定、部分步骤可变的场景。

---

#### 1.5 责任链模式

多个处理器按顺序处理请求。

常见场景：过滤器、审批流、风控规则。

---

#### 1.6 观察者模式

一个对象状态变化时通知多个观察者。

常见场景：事件监听、消息通知。

---

#### 1.7 代理模式

通过代理对象增强目标对象功能。

Spring AOP 就大量使用代理思想。

---

### 2. 架构设计

#### 2.1 分层架构

常见分层：

```text
Controller -> Service -> Mapper/Repository -> DB
```

作用：职责清晰，便于维护和测试。

---

#### 2.2 DDD

DDD：领域驱动设计。

强调围绕业务领域建模，而不是只围绕数据库表开发。

适合复杂业务系统。

---

#### 2.3 CQRS

命令查询职责分离。

- Command：写操作
- Query：读操作

适合读写模型差异大、高并发复杂系统。

---

#### 2.4 事件驱动

系统通过事件进行解耦。

例如：订单创建后发布“订单已创建事件”，库存系统、积分系统、通知系统分别订阅处理。

---

## 十四、企业级项目常见模块

### 1. 登录鉴权

常见流程：

1. 用户提交账号密码。
2. 后端校验。
3. 生成 Token 或 Session。
4. 前端保存 Token。
5. 后续请求携带 Token。
6. 后端解析身份和权限。

---

### 2. 用户管理

常见功能：

- 用户新增
- 用户修改
- 用户删除
- 用户查询
- 状态启用 / 禁用
- 密码重置

---

### 3. 权限管理

通常包括：

- 用户
- 角色
- 菜单
- 按钮权限
- 接口权限

常见模型：RBAC。

---

### 4. 日志系统

日志类型：

- 操作日志
- 登录日志
- 异常日志
- 接口访问日志
- 审计日志

常用技术：

- Logback
- SLF4J
- ELK
- Loki

---

### 5. 文件上传

流程：

1. 前端选择文件。
2. 后端接收 MultipartFile。
3. 校验文件大小、类型。
4. 保存到本地、OSS、S3 等。
5. 数据库存储文件 URL。

注意安全：

- 限制文件类型
- 防止脚本上传
- 文件名重命名
- 权限控制

---

### 6. 任务调度（Quartz / XXL-Job）

用于定时任务。

场景：

- 定时生成报表
- 定时清理数据
- 定时同步订单
- 定时发送通知

Quartz 适合应用内调度。  
XXL-Job 适合分布式任务调度平台。

---

### 7. 报表系统

常见功能：

- 数据统计
- Excel 导出
- PDF 导出
- 图表展示
- 定时报表

注意：大数据量导出要异步处理，避免接口超时。

---

### 8. 多租户

多租户指多个客户共用一套系统，但数据相互隔离。

常见方案：

1. 独立数据库
2. 独立 Schema
3. 同表 tenant_id 隔离

中小型 SaaS 常用 `tenant_id` 方案。

---

## 十五、面试高频综合题

### 1. 一个请求从浏览器到数据库经历了什么？

可以按这个顺序回答：

1. 用户在浏览器输入 URL 或点击按钮。
2. 浏览器进行 DNS 解析，找到服务器 IP。
3. 建立 TCP 连接，HTTPS 还会进行 TLS 握手。
4. 浏览器发送 HTTP 请求。
5. 请求经过 Nginx / 网关。
6. 请求进入后端服务，例如 Spring Boot。
7. DispatcherServlet 接收请求。
8. HandlerMapping 找到对应 Controller。
9. Controller 调用 Service。
10. Service 处理业务逻辑。
11. Mapper / Repository 访问数据库。
12. 数据库执行 SQL，返回结果。
13. 后端封装响应 JSON。
14. 浏览器接收响应并渲染页面。

#### 面试加分点

可以补充：

- 网关鉴权
- Redis 缓存
- MQ 异步处理
- 数据库连接池
- 事务控制
- 日志链路追踪

---

### 2. 如何保证接口幂等性？

幂等性：同一个请求执行一次和执行多次，结果应该一致。

常见场景：

- 订单支付
- 表单重复提交
- MQ 重复消费
- 接口超时重试

解决方案：

### 方案一：唯一索引

例如订单号唯一，重复插入会失败。

### 方案二：Token 机制

提交前先获取 Token，提交时带上 Token，服务端校验后删除。

### 方案三：Redis 防重

用请求 ID 或业务 ID 作为 key。

```text
SETNX requestId value EX 60
```

### 方案四：状态机控制

订单状态只能按固定方向流转。

```text
未支付 -> 已支付 -> 已发货
```

已支付订单不能再次支付成功。

---

### 3. 如何定位线上 CPU 飙高？

排查步骤：

1. 使用 `top` 找到 CPU 高的 Java 进程。
2. 使用 `top -Hp pid` 找到 CPU 高的线程。
3. 将线程 ID 转成 16 进制。
4. 使用 `jstack pid` 导出线程栈。
5. 根据 16 进制线程 ID 找到对应线程。
6. 查看线程正在执行的代码。
7. 判断是否死循环、频繁 GC、锁竞争、复杂计算。

常用命令：

```bash
top
 top -Hp <pid>
printf "%x\n" <tid>
jstack <pid> > thread.txt
```

---

### 4. JVM 内存泄漏如何排查？

排查步骤：

1. 查看监控，确认内存持续上涨。
2. 查看 GC 日志，判断是否频繁 Full GC。
3. 导出 heap dump。
4. 使用 MAT 分析。
5. 查看大对象和引用链。
6. 定位代码。
7. 修复后压测验证。

常见原因：

- 静态集合持有对象
- 缓存无过期
- ThreadLocal 未 remove
- 监听器未注销
- 连接未关闭
- 大对象长期引用

---

## 学习路线建议

## 第一阶段：Java 基础

重点掌握：

- 基本语法
- OOP
- String
- 集合
- 异常
- IO

目标：能写基础 Java 代码，看懂普通业务逻辑。

## 第二阶段：数据库 + Web

重点掌握：

- SQL
- 索引
- 事务
- HTTP
- RESTful
- Cookie / Session

目标：能理解前后端交互和数据库访问。

## 第三阶段：Spring Boot 项目开发

重点掌握：

- Spring IOC
- AOP
- 事务
- Spring MVC
- MyBatis
- Redis
- 权限认证

目标：能独立开发普通后台管理系统。

## 第四阶段：并发、JVM、性能优化

重点掌握：

- 线程池
- synchronized
- volatile
- ConcurrentHashMap
- JVM 内存结构
- GC
- 线上问题排查

目标：能应对中级 Java 面试和线上问题分析。

## 第五阶段：企业级架构

重点掌握：

- MQ
- Docker
- CI/CD
- 分布式锁
- 幂等性
- 限流
- 日志链路
- 设计模式

目标：具备企业项目设计和问题解决能力。

---

## 面试复习建议

1. 每个知识点都要能说出“是什么、为什么、怎么用、项目中哪里用过”。
2. 不要只背定义，要结合业务场景回答。
3. 高频重点：HashMap、线程池、Spring IOC、事务、Redis 缓存问题、SQL 优化、JVM 排查。
4. 项目经验要准备具体例子，例如登录鉴权、接口幂等、慢 SQL 优化、Redis 缓存、文件上传。
5. 回答不会的问题时，可以说“这个我了解得不深，但我的理解是……”，不要硬编。

---

## 常见回答模板

## 技术概念类

可以这样回答：

```text
这个技术主要是用来解决 XXX 问题的。
它的核心思想是 XXX。
在项目中通常用于 XXX 场景。
使用时需要注意 XXX。
```

## 原理类

```text
从整体流程看，首先 XXX，然后 XXX，最后 XXX。
它底层主要依赖 XXX。
这样设计的好处是 XXX，但也有 XXX 问题。
```

## 项目经验类

```text
我在项目中主要在 XXX 模块使用过。
当时的业务场景是 XXX。
我负责的是 XXX。
遇到的问题是 XXX。
最后通过 XXX 方式解决。
```

---

## 结语

Java 后端知识体系很大，但面试和实际开发最核心的是：

- Java 基础是否扎实
- Spring Boot 项目是否熟悉
- 数据库和 Redis 是否能解决实际问题
- 是否理解并发、JVM、线上排查
- 是否能把技术点和项目经验结合起来表达

建议按照“基础 -> 框架 -> 数据库 -> 缓存中间件 -> JVM 并发 -> 项目综合题”的顺序复习。
