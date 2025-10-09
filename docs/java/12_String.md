# Java 字符串详解

## 一、前言

`String` 是 Java 中最常用的数据类型之一，代表不可变的字符串对象。  
此外，Java 还提供了 `StringBuilder`、`StringBuffer` 和 `StringJoiner` 三个用于拼接或处理字符串的类，极大提高了字符串操作效率。

---

## 二、String 概述

### 1. 基本特性

- `String` 是 **不可变对象**，一旦创建，内容无法修改。
- 每次修改实际上都会创建一个新的 `String` 对象。

```java
String a = "abc";
a += "d";
System.out.println(a); // 实际上创建了新的 "abcd" 对象
```

---

## 三、创建字符串的两种方式

### 1. 直接赋值

```java
String name = "安信株式会社";

```

该方式创建的字符串会存储在 **字符串常量池** 中，相同内容只会保留一份。

### 2. 使用构造方法

```java
String s1 = new String(); // 空字符串
String s2 = new String("安信株式会社");
```

此方式每次调用 `new` 都会在 **堆内存** 中创建新的对象。

### 构造方法汇总

| 构造方法 | 说明 |
|-----------|------|
| `String()` | 创建空白字符串对象 |
| `String(String original)` | 复制字符串内容 |
| `String(char[] chs)` | 从字符数组创建字符串 |
| `String(byte[] bytes)` | 从字节数组创建字符串 |

---

## 四、String 常用 API

### 1. equals / equalsIgnoreCase

```java
String a = "Cunyu";
String b = "cunyu";
System.out.println(a.equals(b));            // false
System.out.println(a.equalsIgnoreCase(b));  // true
```

### 2. length()

获取字符串长度：

```java
String str = "安信株式会社";
System.out.println(str.length()); // 3
```

### 3. charAt()

获取指定索引的字符：

```java
String str = "cunyu1943";
System.out.println(str.charAt(3)); // y
```

### 4. toCharArray()

将字符串转换为字符数组：

```java
char[] arr = "Java".toCharArray();
System.out.println(Arrays.toString(arr)); // [J, a, v, a]
```

### 5. substring()

截取字符串（左闭右开）：

```java
String s = "cunyu1943";
System.out.println(s.substring(2, 5)); // nyu
```

### 6. replace()

替换字符或子串：

```java
String str = "cunyu1943";
System.out.println(str.replace("u", "x")); // cxnyx1943
```

### 7. split()

按规则切割字符串：

```java
String s = "010-110-119";
String[] arr = s.split("-");
System.out.println(Arrays.toString(arr)); // [010, 110, 119]
```

---

## 五、StringBuilder

### 1. 概述

`StringBuilder` 是一个可变字符串容器，设计用于高效拼接和修改字符串。  
相比 `String`，`StringBuilder` 不会频繁创建新对象，从而提升性能。

### 2. 构造方法

| 方法 | 说明 |
|------|------|
| `StringBuilder()` | 创建空容器 |
| `StringBuilder(String str)` | 以指定字符串初始化 |

```java
StringBuilder sb = new StringBuilder("安信株式会社");
System.out.println(sb); // 安信株式会社
```

### 3. 常用方法

| 方法 | 说明 |
|------|------|
| `append()` | 添加内容 |
| `insert()` | 在指定位置插入内容 |
| `delete()` | 删除指定区间内容 |
| `reverse()` | 反转内容 |
| `toString()` | 转换为 String |

```java
StringBuilder sb = new StringBuilder("Hello");
sb.append(" World");
System.out.println(sb);          // Hello World
sb.reverse();
System.out.println(sb);          // dlroW olleH
System.out.println(sb.length()); // 11
```

---

## 六、StringBuffer

### 1. 概述2

`StringBuffer` 与 `StringBuilder` 类似，也是可变字符串类。  
**区别在于**：`StringBuffer` 是 **线程安全的**，在多线程环境中更可靠。

### 2. 构造方法2

```java
StringBuffer sb1 = new StringBuffer();
StringBuffer sb2 = new StringBuffer("Java");
```

### 3. 常用方法2

与 `StringBuilder` 基本一致：

- `append()`：拼接字符串。
- `insert()`：插入字符串。
- `delete()`：删除指定位置内容。
- `reverse()`：反转字符串。
- `toString()`：转为字符串。

### 4. 示例2

```java
public class StringBufferTest {
    public static void main(String[] args) {
        StringBuffer buffer = new StringBuffer("Java");
        buffer.append(" SE");
        buffer.insert(4, " 8");
        System.out.println(buffer); // Java 8 SE
        buffer.reverse();
        System.out.println(buffer); // ES 8 avaJ
    }
}
```

> **总结区别**：
>
> - `StringBuffer`：线程安全（多线程场景适用）。  
> - `StringBuilder`：非线程安全，但更快（单线程场景适用）。

---

## 七、StringJoiner

### 1. 概述3

`StringJoiner` 是 JDK8 引入的工具类，用于高效拼接字符串，支持指定分隔符、前缀、后缀。

### 2. 构造方法3

| 构造函数 | 说明 |
|-----------|------|
| `StringJoiner(CharSequence delimiter)` | 指定分隔符 |
| `StringJoiner(CharSequence delimiter, CharSequence prefix, CharSequence suffix)` | 指定分隔符、前缀、后缀 |

```java
StringJoiner joiner = new StringJoiner("-", "[", "]");
joiner.add("Java").add("Python").add("Go");
System.out.println(joiner); // [Java-Python-Go]
```

### 3. 常用方法3

| 方法 | 说明 |
|------|------|
| `add()` | 添加元素 |
| `length()` | 获取拼接后长度 |
| `toString()` | 转换为字符串 |

---

## 八、性能与适用场景对比

| 类名 | 可变性 | 线程安全 | 性能 | 典型场景 |
|------|----------|------------|----------|-----------|
| **String** | 不可变 | 安全 | 低 | 少量字符串拼接 |
| **StringBuilder** | 可变 | 不安全 | 高 | 单线程下频繁拼接 |
| **StringBuffer** | 可变 | 安全 | 中 | 多线程下字符串操作 |
| **StringJoiner** | 可变 | 安全 | 高 | 格式化拼接带分隔符的内容 |

---

## 九、总结

- **String**：不可变对象，每次修改会生成新字符串。  
- **StringBuilder**：单线程下高性能拼接首选。  
- **StringBuffer**：多线程环境下的安全版本。  
- **StringJoiner**：格式化拼接更优雅。  

> 掌握字符串的底层机制，有助于写出高效、稳定的 Java 程序。  
> **牢记：字符串的不可变性是安全与效率之间的权衡。**
