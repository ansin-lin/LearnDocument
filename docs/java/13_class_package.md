# Java 常用系统类与包详解

## 一、前言

在 Java 程序开发中，我们经常需要调用各种类与工具方法，而这些类大部分都来自 **Java 标准库（Java API）** 中定义的系统包。  
这些包构成了 Java 平台的基础框架，涵盖了常见的数据结构、输入输出、日期时间、网络通信、数学计算等功能。

---

## 二、Package（包）的概念

**包（Package）** 是 Java 中用于组织类的命名空间，用来防止类名冲突并提升代码可维护性。  
语法定义：

```java
package com.example.project;
```

### 导入包

要使用其他包中的类，可通过以下方式导入：

```java
import java.util.List;           // 导入单个类
import java.util.*;              // 导入整个包
```

> `java.lang` 包会被自动导入，无需显式声明。

---

## 三、Java 核心包总览

| 包名 | 主要功能 |
|------|-----------|
| `java.lang` | Java 基础类（字符串、数学、系统、线程等） |
| `java.util` | 工具包（集合类、日期、随机数、定时器等） |
| `java.io` | 输入输出流 |
| `java.nio` | 新版 I/O（NIO）高性能文件与通道操作 |
| `java.net` | 网络编程（Socket、URL、HTTP） |
| `java.math` | 高精度数学运算（BigDecimal、BigInteger） |
| `java.time` | 日期与时间 API（JDK 8+） |
| `java.sql` | 数据库连接（JDBC） |
| `java.text` | 文本与日期格式化 |
| `java.awt` / `javax.swing` | 图形界面开发 |
| `java.security` | 安全加密相关 |
| `java.util.concurrent` | 并发编程支持 |

---

## 四、常用系统包详解

### java.lang（Java 基础类）

| 常用类 | 说明 | 常用方法 |
|--------|------|----------|
| `String` | 字符串操作 | `length()`, `substring()`, `equals()`, `toUpperCase()`, `replace()` |
| `Math` | 数学运算 | `abs()`, `pow()`, `sqrt()`, `random()`, `round()` |
| `System` | 系统操作 | `currentTimeMillis()`, `exit()`, `gc()`, `getProperty()` |
| `Object` | 所有类的父类 | `equals()`, `hashCode()`, `toString()`, `clone()` |
| `Thread` | 线程控制 | `start()`, `sleep()`, `join()`, `interrupt()` |

**示例：**

```java
System.out.println(Math.pow(2, 3)); // 8.0
String s = "Hello".toUpperCase(); // HELLO
```

---

### java.util（集合与工具包）

| 常用类 | 说明 | 常用方法 |
|--------|------|----------|
| `ArrayList` | 动态数组 | `add()`, `get()`, `remove()`, `size()`, `contains()` |
| `HashMap` | 键值对存储 | `put()`, `get()`, `remove()`, `keySet()`, `values()` |
| `HashSet` | 无序集合 | `add()`, `remove()`, `contains()`, `isEmpty()` |
| `Collections` | 集合工具类 | `sort()`, `reverse()`, `shuffle()`, `max()`, `min()` |
| `Date` / `Calendar` | 时间操作（旧 API） | `getTime()`, `setTime()`, `add()` |
| `Random` | 随机数生成 | `nextInt()`, `nextDouble()` |
| `Scanner` | 控制台输入 | `nextLine()`, `nextInt()` |

**示例：**

```java
List<String> list = new ArrayList<>();
list.add("Java");
Collections.sort(list);
System.out.println(list);
```

---

### java.io（输入输出流）

| 常用类 | 说明 | 常用方法 |
|--------|------|----------|
| `File` | 文件操作 | `exists()`, `createNewFile()`, `delete()`, `listFiles()` |
| `FileReader` / `FileWriter` | 字符流 | `read()`, `write()`, `close()` |
| `FileInputStream` / `FileOutputStream` | 字节流 | `read()`, `write()`, `available()` |
| `BufferedReader` / `BufferedWriter` | 缓冲流 | `readLine()`, `newLine()`, `flush()` |
| `ObjectInputStream` / `ObjectOutputStream` | 对象序列化 | `writeObject()`, `readObject()` |

**示例：**

```java
File file = new File("test.txt");
if (!file.exists()) file.createNewFile();
```

---

### java.nio（高性能 I/O）

| 常用类 | 说明 | 常用方法 |
|--------|------|----------|
| `Files` | 文件操作工具类 | `readAllLines()`, `write()`, `copy()`, `delete()` |
| `Paths` | 路径管理 | `get()`, `toAbsolutePath()` |
| `ByteBuffer` | 缓冲区 | `put()`, `flip()`, `get()` |
| `FileChannel` | 通道读写 | `read()`, `write()`, `close()` |

**示例：**

```java
Files.write(Paths.get("nio.txt"), "Hello".getBytes());
```

---

### java.net（网络编程）

| 常用类 | 说明 | 常用方法 |
|--------|------|----------|
| `URL` | 网络资源定位 | `openConnection()`, `getHost()` |
| `HttpURLConnection` | HTTP 通信 | `connect()`, `getInputStream()`, `getResponseCode()` |
| `Socket` / `ServerSocket` | TCP 通信 | `accept()`, `getInputStream()`, `getOutputStream()` |
| `InetAddress` | IP 地址操作 | `getHostName()`, `getHostAddress()` |

**示例：**

```java
InetAddress ip = InetAddress.getLocalHost();
System.out.println(ip.getHostAddress());

```

---

### java.math（高精度数学计算）

| 常用类 | 说明 | 常用方法 |
|--------|------|----------|
| `BigDecimal` | 高精度浮点数 | `add()`, `subtract()`, `multiply()`, `divide()` |
| `BigInteger` | 高精度整数 | `add()`, `pow()`, `mod()` |

**示例：**

```java
BigDecimal a = new BigDecimal("1.5");
BigDecimal b = new BigDecimal("2.0");
System.out.println(a.multiply(b)); // 3.0
```

---

### java.time（日期与时间 API）

| 常用类 | 说明 | 常用方法 |
|--------|------|----------|
| `LocalDate` | 日期 | `now()`, `plusDays()`, `getYear()` |
| `LocalTime` | 时间 | `now()`, `minusHours()` |
| `LocalDateTime` | 日期时间 | `now()`, `format()`, `plusDays()` |
| `DateTimeFormatter` | 格式化 | `ofPattern()`, `format()` |

**示例：**

```java
LocalDateTime now = LocalDateTime.now();
System.out.println(now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
```

---

### java.sql（数据库连接）

| 常用类 | 说明 | 常用方法 |
|--------|------|----------|
| `Connection` | 数据库连接 | `createStatement()`, `prepareStatement()`, `close()` |
| `Statement` / `PreparedStatement` | SQL 执行 | `executeQuery()`, `executeUpdate()` |
| `ResultSet` | 查询结果集 | `next()`, `getString()`, `getInt()` |
| `DriverManager` | 管理数据库驱动 | `getConnection()` |

**示例：**

```java
Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/test", "root", "1234");
Statement stmt = conn.createStatement();
ResultSet rs = stmt.executeQuery("SELECT * FROM user");
```

---

### java.text（文本与格式化）

| 常用类 | 说明 | 常用方法 |
|--------|------|----------|
| `SimpleDateFormat` | 日期格式化 | `format()`, `parse()` |
| `DecimalFormat` | 数字格式化 | `format()` |
| `MessageFormat` | 模板消息 | `format()` |

**示例：**

```java
SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
System.out.println(sdf.format(new Date()));
```

---

## 十、java.awt / javax.swing（图形界面）

| 常用类 | 说明 | 常用方法 |
|--------|------|----------|
| `Frame` / `JFrame` | 创建窗口 | `setSize()`, `setVisible()` |
| `Button` / `JButton` | 按钮控件 | `addActionListener()` |
| `Label` / `JLabel` | 文本标签 | `setText()` |
| `Panel` / `JPanel` | 面板容器 | `add()` |

**示例：**

```java
JFrame frame = new JFrame("Hello GUI");
frame.setSize(300, 200);
frame.setVisible(true);
```

---

## 十一、java.security（安全加密）

| 常用类 | 说明 | 常用方法 |
|--------|------|----------|
| `MessageDigest` | 信息摘要算法（MD5/SHA） | `getInstance()`, `digest()` |
| `KeyPairGenerator` | 密钥对生成 | `generateKeyPair()` |
| `Signature` | 数字签名 | `initSign()`, `sign()`, `verify()` |

**示例：**

```java
MessageDigest md = MessageDigest.getInstance("SHA-256");
byte[] hash = md.digest("Hello".getBytes());
```

---

## 十二、java.util.concurrent（并发编程）

| 常用类 | 说明 | 常用方法 |
|--------|------|----------|
| `ExecutorService` | 线程池管理 | `submit()`, `shutdown()` |
| `Future` | 异步结果 | `get()`, `cancel()` |
| `CountDownLatch` | 并发同步 | `await()`, `countDown()` |
| `ReentrantLock` | 可重入锁 | `lock()`, `unlock()` |
| `ConcurrentHashMap` | 并发安全 Map | `put()`, `get()`, `remove()` |

**示例：**

```java
ExecutorService pool = Executors.newFixedThreadPool(2);
pool.submit(() -> System.out.println("Task running"));
pool.shutdown();
```

---

## 总结

| 包名 | 主要功能 |
|------|-----------|
| `java.lang` | 基础类（字符串、数学、系统、线程等） |
| `java.util` | 工具类（集合、日期、随机数等） |
| `java.io` | 输入输出流 |
| `java.nio` | 高性能文件与通道操作 |
| `java.net` | 网络编程 |
| `java.math` | 高精度计算 |
| `java.time` | 日期与时间 API |
| `java.sql` | 数据库连接（JDBC） |
| `java.text` | 文本与日期格式化 |
| `java.awt` / `javax.swing` | 图形界面开发 |
| `java.security` | 安全加密 |
| `java.util.concurrent` | 并发编程支持 |

---

> 熟悉以上系统包及其常用类与方法，是掌握 Java 标准库的关键基础。建议结合实际项目多实践、反复查阅本速查表。
