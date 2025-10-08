# Java 常用系统类与包详解
**作者：村雨遥**  

> 不要哀求，学会争取，若是如此，终有所获  

---

## 🎈 号外
最近，公众号之外，建立了微信交流群，不定期会在群里分享各种资源（影视、IT 编程、考试提升……）与知识。  
如果有需要，可以扫码或者后台添加小编微信备注入群。进群后优先看群公告，呼叫群中【资源分享小助手】，还能免费帮找资源哦～  

---

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

### 1. java.lang
自动导入的核心包，包含最基础的系统类。

| 类名 | 功能 |
|------|------|
| `Object` | 所有类的根父类 |
| `String` / `StringBuilder` / `StringBuffer` | 字符串处理 |
| `Math` | 数学运算 |
| `System` | 系统资源访问，如输入输出、时间、环境变量 |
| `Thread` / `Runnable` | 多线程编程 |
| `Exception` / `Error` | 异常处理 |
| `Enum` | 枚举类型支持 |

**示例：**
```java
System.out.println(Math.pow(2, 10)); // 1024.0
System.out.println(System.currentTimeMillis()); // 当前时间戳
```

---

### 2. java.util
Java 工具包，是使用最频繁的包之一。

| 类 | 功能 |
|-----|------|
| `ArrayList`、`HashMap`、`HashSet` | 集合框架核心类 |
| `Collections` | 集合工具类 |
| `Date`、`Calendar` | 日期操作（旧 API） |
| `Random` | 生成随机数 |
| `Timer`、`TimerTask` | 定时任务 |
| `Scanner` | 控制台输入 |

**示例：**
```java
import java.util.*;

public class Demo {
    public static void main(String[] args) {
        List<String> list = new ArrayList<>();
        list.add("Java");
        list.add("Python");
        Collections.sort(list);
        System.out.println(list); // [Java, Python]
    }
}
```

---

### 3. java.io
用于文件读写与输入输出流操作。

| 类 | 功能 |
|-----|------|
| `File` | 文件和目录的封装 |
| `FileReader` / `FileWriter` | 字符流读写 |
| `FileInputStream` / `FileOutputStream` | 字节流读写 |
| `BufferedReader` / `BufferedWriter` | 缓冲流提高性能 |
| `ObjectInputStream` / `ObjectOutputStream` | 对象序列化 |

**示例：**
```java
import java.io.*;

public class FileTest {
    public static void main(String[] args) throws IOException {
        FileWriter writer = new FileWriter("demo.txt");
        writer.write("Hello Java IO");
        writer.close();
    }
}
```

---

### 4. java.nio
NIO（New I/O）是 Java 1.4 引入的高性能 IO 库，支持 **通道（Channel）** 与 **缓冲区（Buffer）**。

| 类 | 功能 |
|-----|------|
| `FileChannel` | 文件通道 |
| `ByteBuffer` | 缓冲区 |
| `Paths` / `Files` | 文件路径操作（JDK 7+） |

**示例：**
```java
import java.nio.file.*;

public class NioTest {
    public static void main(String[] args) throws Exception {
        String content = "Hello NIO!";
        Files.write(Paths.get("nio.txt"), content.getBytes());
    }
}
```

---

### 5. java.net
用于网络通信与数据传输。

| 类 | 功能 |
|-----|------|
| `URL` / `URLConnection` | 处理网络资源 |
| `Socket` / `ServerSocket` | 套接字通信 |
| `InetAddress` | IP 地址操作 |
| `HttpURLConnection` | HTTP 请求 |

**示例：**
```java
import java.net.*;

public class NetTest {
    public static void main(String[] args) throws Exception {
        InetAddress ip = InetAddress.getLocalHost();
        System.out.println("主机名：" + ip.getHostName());
        System.out.println("IP 地址：" + ip.getHostAddress());
    }
}
```

---

### 6. java.math
支持高精度计算。

| 类 | 功能 |
|-----|------|
| `BigInteger` | 任意精度整数 |
| `BigDecimal` | 高精度浮点数 |

**示例：**
```java
import java.math.*;

public class MathTest {
    public static void main(String[] args) {
        BigDecimal a = new BigDecimal("1.0");
        BigDecimal b = new BigDecimal("3.0");
        System.out.println(a.divide(b, 2, RoundingMode.HALF_UP)); // 0.33
    }
}
```

---

### 7. java.time（JDK 8+）
全新日期时间 API，线程安全、功能强大。

| 类 | 功能 |
|-----|------|
| `LocalDate` | 日期（年/月/日） |
| `LocalTime` | 时间（时/分/秒） |
| `LocalDateTime` | 日期时间 |
| `DateTimeFormatter` | 格式化 |
| `Duration` / `Period` | 时间间隔 |

**示例：**
```java
import java.time.*;

public class TimeTest {
    public static void main(String[] args) {
        LocalDateTime now = LocalDateTime.now();
        System.out.println("当前时间：" + now);
    }
}
```

---

### 8. java.sql
JDBC 数据库操作接口。

| 类 | 功能 |
|-----|------|
| `DriverManager` | 管理数据库驱动 |
| `Connection` | 数据库连接 |
| `Statement` / `PreparedStatement` | SQL 执行 |
| `ResultSet` | 查询结果集 |

**示例：**
```java
import java.sql.*;

public class JdbcTest {
    public static void main(String[] args) throws Exception {
        Connection conn = DriverManager.getConnection("jdbc:mysql://localhost/test", "root", "1234");
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT * FROM user");
        while (rs.next()) {
            System.out.println(rs.getString("name"));
        }
        conn.close();
    }
}
```

---

### 9. java.text
用于文本格式化与国际化支持。

| 类 | 功能 |
|-----|------|
| `SimpleDateFormat` | 日期格式化 |
| `DecimalFormat` | 数字格式化 |
| `MessageFormat` | 模板消息 |

**示例：**
```java
import java.text.*;
import java.util.*;

public class TextTest {
    public static void main(String[] args) {
        Date date = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        System.out.println(sdf.format(date));
    }
}
```

---

### 10. java.awt / javax.swing
提供图形界面开发支持（GUI）。

| 类 | 功能 |
|-----|------|
| `Frame` / `Panel` | 窗口与面板 |
| `Button` / `Label` | 控件组件 |
| `JFrame` / `JPanel` | Swing 界面类 |
| `JButton` / `JLabel` | Swing 控件 |

**示例：**
```java
import javax.swing.*;

public class SwingTest {
    public static void main(String[] args) {
        JFrame frame = new JFrame("Hello Swing");
        JButton button = new JButton("点击我");
        frame.add(button);
        frame.setSize(200, 200);
        frame.setVisible(true);
    }
}
```

---

## 五、导入与使用规范

1. **只导入需要的类**，避免使用 `*` 影响性能与可读性。  
2. **使用静态导入** 提高可读性（如 `import static java.lang.Math.*;`）。  
3. 按模块导入常用包，保持包结构清晰。  
4. 建议将自定义类放在自定义包中，遵循公司命名规范。  

---

## 六、总结

| 包名 | 功能定位 |
|------|-----------|
| `java.lang` | 基础核心类 |
| `java.util` | 集合、工具、时间 |
| `java.io` / `java.nio` | 文件与流操作 |
| `java.net` | 网络通信 |
| `java.sql` | 数据库交互 |
| `java.time` | 日期时间 API |
| `java.text` | 格式化 |
| `java.math` | 高精度数学 |
| `javax.swing` | 图形界面 |

> Java 的系统包构成了其强大生态的基础，熟悉这些包的结构与常见类，能让开发者快速定位所需功能，写出更高效、可维护的程序。

---

**延伸阅读建议：**
- 《Effective Java》 第二版，第 2 章：创建和销毁对象  
- 《Java 核心技术卷Ⅰ》：第 9 章 类与接口  
- 官方 Java SE API 文档：https://docs.oracle.com/en/java/javase/
