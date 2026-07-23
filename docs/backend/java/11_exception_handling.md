# Java 异常处理详解

## 前言

在 Java 程序中，异常是程序运行过程中出现的非正常情况。Java 提供了一套完整的 **异常处理机制**，用于提高程序的健壮性与可维护性。  

异常处理的核心目的：

- 发现并捕获程序中的错误。  
- 防止程序在出错时崩溃。  
- 提供友好的错误信息或补救措施。  

---

## 异常体系结构

Java 中的异常体系位于 `java.lang` 包下：  

```markdown
Throwable
├── Error（错误）
│   ├── OutOfMemoryError
│   ├── StackOverflowError
│   └── ...
└── Exception（异常）
    ├── RuntimeException（运行时异常）
    │   ├── NullPointerException
    │   ├── ArrayIndexOutOfBoundsException
    │   ├── ArithmeticException
    │   ├── ClassCastException
    │   └── ...
    └── Checked Exception（受检异常）
        ├── IOException
        ├── SQLException
        └── FileNotFoundException
```

### Throwable

所有异常与错误的父类，定义了常见方法：

- `getMessage()`：获取异常信息。
- `printStackTrace()`：打印堆栈信息。

### Error（错误）

严重问题，如内存溢出、系统崩溃，程序**无法恢复**，不应捕获。

### Exception（异常）

程序可处理的问题，分为：

- **受检异常（Checked Exception）**：必须显式处理（如 IO、SQL）。  
- **非受检异常（Unchecked Exception）**：运行时异常，可选择处理（如空指针）。  

---

## try-catch-finally

### 基本语法

```java
try {
    // 可能发生异常的代码
} catch (ExceptionType e) {
    // 异常处理逻辑
} finally {
    // 无论是否发生异常，都会执行的代码
}
```

### 示例1

```java
public class Demo {
    public static void main(String[] args) {
        try {
            int result = 10 / 0;
            System.out.println(result);
        } catch (ArithmeticException e) {
            System.out.println("错误：除数不能为0！");
            e.printStackTrace();
        } finally {
            System.out.println("执行 finally 块，资源释放。");
        }
    }
}
```

输出：

```bash
错误：除数不能为0！
执行 finally 块，资源释放。
```

> **finally 块的常见用途**：关闭文件、数据库连接、网络流等资源。

---

## 多重 catch 与异常链

### 多重 catch

```java
try {
    int[] arr = {1, 2, 3};
    System.out.println(arr[5]);
} catch (ArrayIndexOutOfBoundsException e) {
    System.out.println("数组越界异常！");
} catch (Exception e) {
    System.out.println("其他异常！");
}
```

> 注意：子类异常必须写在父类异常之前，否则会编译错误。

### 异常链（Exception Chaining）

可在抛出新异常时，将原始异常作为参数传递。

```java
try {
    readFile("abc.txt");
} catch (IOException e) {
    throw new RuntimeException("文件读取失败", e);
}
```

---

## throw 与 throws

### throw —— 抛出异常对象

用于手动抛出异常。

```java
public void checkAge(int age) {
    if (age < 18) {
        throw new IllegalArgumentException("未成年禁止访问");
    }
}
```

### throws —— 声明可能抛出的异常

用于方法声明处，告知调用者需要处理的异常。

```java
public void readFile(String path) throws IOException {
    FileReader reader = new FileReader(path);
    reader.read();
}
```

调用方需显式处理：

```java
try {
    readFile("test.txt");
} catch (IOException e) {
    e.printStackTrace();
}
```

### 两者区别

| 关键字 | 位置 | 作用 | 用法 |
|---------|--------|--------|--------|
| throw | 方法内 | 抛出具体异常对象 | throw new Exception() |
| throws | 方法签名 | 声明可能抛出的异常类型 | public void method() throws Exception |

---

## 自定义异常

当系统异常无法满足业务需求时，可自定义异常类。

### 自定义异常类

```java
public class MyException extends Exception {
    public MyException(String message) {
        super(message);
    }
}
```

### 使用自定义异常

```java
public class Test {
    public static void checkScore(int score) throws MyException {
        if (score < 0 || score > 100) {
            throw new MyException("分数必须在 0 到 100 之间！");
        } else {
            System.out.println("成绩合法：" + score);
        }
    }

    public static void main(String[] args) {
        try {
            checkScore(120);
        } catch (MyException e) {
            System.out.println("捕获异常：" + e.getMessage());
        }
    }
}
```

---

## 常见异常类型

| 异常类型 | 说明 |
|-----------|------|
| `NullPointerException` | 空指针引用 |
| `ArrayIndexOutOfBoundsException` | 数组越界 |
| `ClassCastException` | 类型转换错误 |
| `NumberFormatException` | 数字格式错误 |
| `ArithmeticException` | 算术错误（如除以 0） |
| `IOException` | 输入输出异常 |
| `FileNotFoundException` | 文件未找到 |
| `SQLException` | 数据库操作异常 |
| `IllegalArgumentException` | 参数非法 |
| `InterruptedException` | 线程中断异常 |

---

## 异常处理最佳实践

### ✅ 优先使用具体异常类型

不要用 `Exception` 或 `Throwable` 一把抓。

### ✅ 不要吞掉异常

避免只写 `catch(Exception e){}` 而不处理。应记录日志或重新抛出。

### ✅ 不滥用异常控制流程

不要将异常当作普通逻辑判断使用。

### ✅ 记录日志，保留堆栈信息

使用 `e.printStackTrace()` 或日志框架记录完整堆栈。

### ✅ 使用自定义异常封装业务逻辑错误

使系统异常与业务异常区分明确。

### ✅ 善用 try-with-resources 自动关闭资源（JDK7+）

```java
try (FileReader reader = new FileReader("test.txt")) {
    reader.read();
} catch (IOException e) {
    e.printStackTrace();
}
```

> try-with-resources 会自动调用实现了 `AutoCloseable` 接口的资源的 `close()` 方法。

---

## 九、总结

| 分类 | 是否必须处理 | 示例 |
|------|---------------|------|
| **受检异常** | 是 | IOException, SQLException |
| **运行时异常** | 否 | NullPointerException, ArithmeticException |
| **错误（Error）** | 否 | OutOfMemoryError, StackOverflowError |

**核心要点**：

- 使用 `try-catch-finally` 捕获可预期异常；  
- 使用 `throw` 主动抛出业务异常；  
- 使用 `throws` 声明异常类型；  
- 使用自定义异常提升系统可读性；  
- 合理记录日志与释放资源，确保系统稳定运行。

---

> 掌握异常机制，是写出高质量 Java 代码的必经之路。  
> 异常不是“错误”，而是程序设计中的“控制信号”。
