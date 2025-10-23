# Java 常用系统类与包详解（非集合版）

## 一、前言

在 Java 开发中，除了集合框架，标准库还提供了大量实用的**系统类与工具包**，
用于处理字符串、文件、日期、网络、数学计算、安全、并发等功能。
这些类构成了 Java 应用的基础能力。

---

## 二、包的导入与使用

**包（Package）** 是 Java 中组织类的命名空间，用于防止命名冲突。

```java
package com.example.project;
import java.util.*;
import java.io.File;
```

> `java.lang` 包会被自动导入。

---

## 三、核心系统包概览

| 包名 | 功能描述 |
|------|-----------|
| `java.lang` | 基础类：字符串、数学、系统、线程 |
| `java.util` | 工具类：随机数、日期、扫描器等 |
| `java.io` | 文件与输入输出流 |
| `java.nio` | 高性能 NIO 通道与文件操作 |
| `java.net` | 网络编程 |
| `java.math` | 高精度数学计算 |
| `java.time` | 日期与时间 API（JDK 8+） |
| `java.sql` | 数据库连接（JDBC） |
| `java.text` | 文本与格式化 |
| `java.security` | 加密与安全 |
| `java.util.concurrent` | 并发编程支持 |

---

## 四、java.lang

### String（字符串类）

- `public int length()`：返回字符串长度。
- `public char charAt(int index)`：返回指定索引处字符。
- `public String substring(int begin, int end)`：截取子串。
- `public boolean equals(Object obj)`：比较内容是否相同。
- `public boolean isEmpty()`：判断是否为空字符串。
- `public String toUpperCase()`：转为大写。
- `public String replace(CharSequence old, CharSequence new)`：替换子串。

### Math（数学类）

- `public static double abs(double a)`：返回绝对值。
- `public static double pow(double a, double b)`：幂运算。
- `public static double sqrt(double a)`：平方根。
- `public static long round(double a)`：四舍五入。
- `public static double random()`：返回 0~1 随机数。

### System（系统类）

- `public static void exit(int status)`：退出程序。
- `public static long currentTimeMillis()`：当前时间毫秒值。
- `public static void gc()`：请求垃圾回收。
- `public static String getProperty(String key)`：获取系统属性。

### Object（根类）

- `public boolean equals(Object obj)`：判断是否相等。
- `public int hashCode()`：返回哈希值。
- `public String toString()`：返回字符串表示。
- `protected Object clone()`：克隆对象。

### Thread（线程类）

- `public void start()`：启动线程。
- `public void run()`：线程任务。
- `public void sleep(long millis)`：暂停执行。
- `public void join()`：等待线程结束。
- `public void interrupt()`：中断线程。

---

## 五、java.util（非集合）

### Random（随机数）

- `public int nextInt()`：返回随机整数。
- `public int nextInt(int bound)`：返回指定范围随机数。
- `public double nextDouble()`：返回随机小数。
- `public boolean nextBoolean()`：返回随机布尔值。

### Date（旧日期类）

- `public long getTime()`：获取时间戳。
- `public void setTime(long time)`：设置时间戳。
- `public int getYear()`：获取年份（过时）。
- `public int getMonth()`：获取月份（过时）。

### Calendar（日期类）

- `public static Calendar getInstance()`：获取实例。
- `public void set(int field, int value)`：设置时间字段。
- `public Date getTime()`：转换为 Date。
- `public void add(int field, int amount)`：增减时间。

### Scanner（控制台输入）

- `public String nextLine()`：读取一行文本。
- `public int nextInt()`：读取整数。
- `public double nextDouble()`：读取浮点数。
- `public boolean hasNext()`：判断是否有输入。

### Collections（工具类）

- `public static <T> void sort(List<T> list)`：排序。
- `public static void reverse(List<?> list)`：反转。
- `public static void shuffle(List<?> list)`：随机打乱。
- `public static <T> T max(Collection<? extends T> coll)`：返回最大值。

---

## 六、java.io

### File（文件类）

- `public boolean exists()`：判断文件是否存在。
- `public boolean createNewFile()`：创建文件。
- `public boolean delete()`：删除文件。
- `public String[] list()`：列出目录内容。
- `public boolean mkdir()`：创建目录。

### FileReader / FileWriter（字符流）

- `public int read()`：读取单个字符。
- `public void write(String str)`：写入字符串。
- `public void close()`：关闭流。

### FileInputStream / FileOutputStream（字节流）

- `public int read()`：读取字节。
- `public void write(int b)`：写入字节。
- `public void close()`：关闭流。

### BufferedReader / BufferedWriter（缓冲流）

- `public String readLine()`：读取一行文本。
- `public void newLine()`：写入换行符。
- `public void flush()`：刷新缓冲区。

### ObjectInputStream / ObjectOutputStream（对象流）

- `public void writeObject(Object obj)`：序列化对象。
- `public Object readObject()`：反序列化对象。

---

## 七、java.nio

### Files（文件操作）

- `public static List<String> readAllLines(Path path)`：读取所有行。
- `public static void write(Path path, byte[] bytes)`：写入文件。
- `public static void copy(Path src, Path dest)`：复制文件。
- `public static void delete(Path path)`：删除文件。

### Paths（路径管理）

- `public static Path get(String first, String... more)`：获取路径。
- `public Path toAbsolutePath()`：返回绝对路径。

### ByteBuffer（缓冲区）

- `public ByteBuffer put(byte b)`：写入一个字节。
- `public byte get()`：读取一个字节。
- `public void flip()`：准备读操作。

### FileChannel（通道）

- `public int read(ByteBuffer dst)`：读取到缓冲区。
- `public int write(ByteBuffer src)`：写入通道。
- `public void close()`：关闭通道。

---

## 八、java.net

### URL（统一资源定位）

- `public URL(String spec)`：构造 URL。
- `public URLConnection openConnection()`：打开连接。
- `public String getHost()`：获取主机名。

### HttpURLConnection（HTTP 连接）

- `public void connect()`：建立连接。
- `public InputStream getInputStream()`：获取输入流。
- `public int getResponseCode()`：获取响应码。

### Socket / ServerSocket（TCP 通信）

- `public Socket accept()`：等待客户端连接。
- `public InputStream getInputStream()`：获取输入流。
- `public OutputStream getOutputStream()`：获取输出流。

### InetAddress（IP 地址）

- `public static InetAddress getLocalHost()`：获取本机地址。
- `public String getHostName()`：主机名。
- `public String getHostAddress()`：IP 地址。

---

## 九、java.math

### BigDecimal（高精度浮点数）

- `public BigDecimal add(BigDecimal val)`：加法。
- `public BigDecimal subtract(BigDecimal val)`：减法。
- `public BigDecimal multiply(BigDecimal val)`：乘法。
- `public BigDecimal divide(BigDecimal val)`：除法。

### BigInteger（高精度整数）

- `public BigInteger add(BigInteger val)`：加法。
- `public BigInteger subtract(BigInteger val)`：减法。
- `public BigInteger multiply(BigInteger val)`：乘法。
- `public BigInteger mod(BigInteger m)`：取模。

---

## 十、java.time

### LocalDate / LocalTime / LocalDateTime

- `public static LocalDate now()`：当前日期。
- `public LocalDate plusDays(long days)`：增加天数。
- `public int getYear()`：获取年份。
- `public LocalDateTime format(DateTimeFormatter fmt)`：格式化。

### DateTimeFormatter

- `public static DateTimeFormatter ofPattern(String pattern)`：定义格式。
- `public String format(TemporalAccessor temporal)`：格式化日期时间。

---

## 十一、java.sql

### Connection（数据库连接）

- `public Statement createStatement()`：创建 SQL 语句。
- `public PreparedStatement prepareStatement(String sql)`：预编译语句。
- `public void close()`：关闭连接。

### Statement / PreparedStatement

- `public ResultSet executeQuery(String sql)`：执行查询。
- `public int executeUpdate(String sql)`：执行更新。

### ResultSet（结果集）

- `public boolean next()`：移动到下一行。
- `public String getString(String column)`：获取字符串。
- `public int getInt(String column)`：获取整数。

### DriverManager

- `public static Connection getConnection(String url, String user, String pass)`：建立连接。

---

## 十二、java.text

### SimpleDateFormat

- `public String format(Date date)`：格式化日期。
- `public Date parse(String text)`：解析日期字符串。

### DecimalFormat

- `public String format(double number)`：格式化数字。

### MessageFormat

- `public static String format(String pattern, Object... args)`：按模板输出文本。

---

## 十三、java.security

### MessageDigest（摘要）

- `public static MessageDigest getInstance(String algorithm)`：获取算法实例。
- `public byte[] digest(byte[] input)`：生成摘要。

### KeyPairGenerator（密钥生成）

- `public static KeyPairGenerator getInstance(String algorithm)`：获取生成器。
- `public KeyPair generateKeyPair()`：生成密钥对。

### Signature（签名）

- `public void initSign(PrivateKey key)`：初始化签名。
- `public byte[] sign()`：生成签名。
- `public boolean verify(byte[] sig)`：验证签名。

---

## 十四、java.util.concurrent

### ExecutorService（线程池）

- `public Future<?> submit(Runnable task)`：提交任务。
- `public void shutdown()`：关闭线程池。

### Future（异步结果）

- `public Object get()`：获取结果。
- `public boolean cancel(boolean mayInterruptIfRunning)`：取消任务。

### CountDownLatch（同步工具）

- `public void await()`：等待计数归零。
- `public void countDown()`：递减计数。

### ReentrantLock（可重入锁）

- `public void lock()`：加锁。
- `public void unlock()`：解锁。

### ConcurrentHashMap（并发 Map）

- `public V put(K key, V value)`：放入键值。
- `public V get(Object key)`：获取值。
- `public V remove(Object key)`：移除键值。

---

## 十五、总结

- 本文涵盖 Java 常用系统包中除集合类外的核心类与方法。
- 建议多结合官方文档与实践理解其用法。

