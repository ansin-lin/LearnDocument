# Java 字符串详解（方法签名 + 版本说明版）

## 一、前言

`String` 是 Java 中最常用的类之一，表示不可变的字符串。  
此外，还有 `StringBuilder`、`StringBuffer` 和 `StringJoiner` 等辅助类，
用于提高字符串拼接与处理的性能与灵活性。

---

## 二、String 类

### 1. 基本特性

- `String` 是 **不可变对象**，每次修改都会生成新的字符串实例。
- 字符串常量存储在字符串常量池中。

### 2. 常用构造方法

- `public String()`【JDK 1.0】：创建空字符串。
- `public String(String original)`【JDK 1.0】：复制已有字符串内容。
- `public String(char[] value)`【JDK 1.0】：从字符数组创建字符串。
- `public String(byte[] bytes)`【JDK 1.0】：从字节数组创建字符串。

### 3. 常用方法（附版本说明）

- `public int length()`【JDK 1.0】：返回字符串长度。
- `public boolean isEmpty()`【JDK 1.6】：判断字符串是否为空。
- `public char charAt(int index)`【JDK 1.0】：返回指定索引处字符。
- `public int indexOf(String str)`【JDK 1.0】：返回子串首次出现位置。
- `public int lastIndexOf(String str)`【JDK 1.0】：返回子串最后出现位置。
- `public boolean equals(Object anObject)`【JDK 1.0】：比较字符串内容。
- `public boolean equalsIgnoreCase(String anotherString)`【JDK 1.0】：忽略大小写比较。
- `public boolean contains(CharSequence s)`【JDK 1.5】：判断是否包含子串。
- `public boolean startsWith(String prefix)`【JDK 1.0】：判断是否以指定前缀开头。
- `public boolean endsWith(String suffix)`【JDK 1.0】：判断是否以指定后缀结尾。
- `public String substring(int beginIndex, int endIndex)`【JDK 1.0】：截取子串。
- `public String concat(String str)`【JDK 1.0】：拼接字符串。
- `public String replace(CharSequence target, CharSequence replacement)`【JDK 1.5】：替换子串。
- `public String replaceAll(String regex, String replacement)`【JDK 1.4】：使用正则替换所有匹配项。
- `public boolean matches(String regex)`【JDK 1.4】：判断是否匹配正则表达式。
- `public String[] split(String regex)`【JDK 1.4】：根据正则切割字符串。
- `public String trim()`【JDK 1.0】：去除前后空白。
- `public String toUpperCase()`【JDK 1.0】：转换为大写。
- `public String toLowerCase()`【JDK 1.0】：转换为小写。
- `public static String format(String format, Object... args)`【JDK 1.5】：格式化输出。
- `public static String join(CharSequence delimiter, CharSequence... elements)`【JDK 1.8】：使用分隔符连接字符串。
- `public String intern()`【JDK 1.0】：返回常量池中的字符串引用。

### 示例

```java
String name = "安信";
String result = name.concat(" 株式会社").toUpperCase();
System.out.println(result); // 安信 株式会社
```

---

## 三、StringBuilder 类

### 1. 概述

`StringBuilder` 是一个可变的字符串容器，效率高于 `String`，但非线程安全。

### 2. StringBuilder常用构造方法

- `public StringBuilder()`【JDK 1.5】：创建空容器。
- `public StringBuilder(String str)`【JDK 1.5】：使用初始字符串。

### 3. 常用方法

- `public StringBuilder append(String str)`【JDK 1.5】：追加字符串。
- `public StringBuilder insert(int offset, String str)`【JDK 1.5】：在指定位置插入字符串。
- `public StringBuilder delete(int start, int end)`【JDK 1.5】：删除指定区间。
- `public StringBuilder deleteCharAt(int index)`【JDK 1.5】：删除单个字符。
- `public StringBuilder replace(int start, int end, String str)`【JDK 1.5】：替换区间内容。
- `public StringBuilder reverse()`【JDK 1.5】：反转字符序列。
- `public int capacity()`【JDK 1.5】：返回当前容量。
- `public void ensureCapacity(int minimumCapacity)`【JDK 1.5】：确保容量足够。
- `public void setCharAt(int index, char ch)`【JDK 1.5】：修改指定位置字符。
- `public String substring(int start, int end)`【JDK 1.5】：提取子串。
- `public String toString()`【JDK 1.5】：转换为 String。

### StringBuilder示例

```java
StringBuilder sb = new StringBuilder("Java");
sb.append(" Programming").insert(4, " SE");
System.out.println(sb.reverse()); // gnimmargorP ES avaJ
```

---

## 四、StringBuffer 类

### 1. StringBuffer概述

`StringBuffer` 与 `StringBuilder` 功能类似，但它是线程安全的，适合多线程环境。

### 2. StringBuffer常用构造方法

- `public StringBuffer()`【JDK 1.0】：创建空缓冲区。
- `public StringBuffer(String str)`【JDK 1.0】：使用初始字符串。

### 3. StringBuffer常用方法

- `public synchronized StringBuffer append(String str)`【JDK 1.0】：追加字符串。
- `public synchronized StringBuffer insert(int offset, String str)`【JDK 1.0】：插入字符串。
- `public synchronized StringBuffer delete(int start, int end)`【JDK 1.0】：删除区间内容。
- `public synchronized StringBuffer reverse()`【JDK 1.0】：反转字符序列。
- `public synchronized int capacity()`【JDK 1.0】：返回容量。
- `public synchronized void ensureCapacity(int minimumCapacity)`【JDK 1.0】：确保容量。
- `public synchronized String toString()`【JDK 1.0】：转为字符串。

### StringBuffer示例

```java
StringBuffer buffer = new StringBuffer("Java");
buffer.append(" SE");
buffer.insert(4, " 8");
System.out.println(buffer); // Java 8 SE
```

---

## 五、StringJoiner 类

### 1. StringJoiner概述

`StringJoiner` 是 JDK 1.8 新增类，用于高效地拼接带分隔符的字符串。

### 2. StringJoiner构造方法

- `public StringJoiner(CharSequence delimiter)`【JDK 1.8】：指定分隔符。
- `public StringJoiner(CharSequence delimiter, CharSequence prefix, CharSequence suffix)`【JDK 1.8】：指定分隔符、前缀与后缀。

### 3. StringJoiner常用方法

- `public StringJoiner add(CharSequence newElement)`【JDK 1.8】：添加元素。
- `public int length()`【JDK 1.8】：返回拼接后长度。
- `public StringJoiner merge(StringJoiner other)`【JDK 1.8】：合并另一个 Joiner。
- `public StringJoiner setEmptyValue(CharSequence emptyValue)`【JDK 1.8】：设置空值时的输出内容。
- `public String toString()`【JDK 1.8】：返回拼接后的字符串。

### StringJoiner示例

```java
StringJoiner joiner = new StringJoiner("-", "[", "]");
joiner.add("Java").add("Python").add("Go");
System.out.println(joiner); // [Java-Python-Go]
```

---

## 六、性能与适用场景对比

| 类名 | 可变性 | 线程安全 | 引入版本 | 性能 | 典型场景 |
|------|----------|------------|------------|-----------|-----------|
| String | 不可变 | 安全 | JDK 1.0 | 低 | 不频繁拼接 |
| StringBuilder | 可变 | 否 | JDK 1.5 | 高 | 单线程高频拼接 |
| StringBuffer | 可变 | 是 | JDK 1.0 | 中 | 多线程字符串操作 |
| StringJoiner | 可变 | 否 | JDK 1.8 | 高 | 格式化分隔拼接 |

---

## 七、总结

- **String**：不可变，适合少量字符串操作。  
- **StringBuilder**：非线程安全但性能优异。  
- **StringBuffer**：线程安全，适合多线程场景。  
- **StringJoiner**：JDK 1.8+ 推荐用于带分隔符的格式化拼接。

> 合理选择字符串类，可以显著提升 Java 程序的性能与可维护性。
