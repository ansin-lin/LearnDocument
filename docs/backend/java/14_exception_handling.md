# 第14章 Java 异常处理

> 本章目标：理解 Java 异常体系，掌握 `try-catch-finally`、`throw`、`throws`、自定义异常和 `try-with-resources`，能够写出不吞异常、能定位问题的基础异常处理代码。

## 一、异常是什么

异常是程序运行过程中出现的非正常情况。

常见异常场景：

- 使用 `null` 调用方法。
- 数组下标越界。
- 字符串转数字失败。
- 文件不存在。
- 数据库连接失败。

Java 使用异常对象描述这些问题，并通过异常处理机制决定程序是否继续执行、如何提示错误、如何释放资源。

## 二、异常体系

Java 异常体系的顶层类型是 `Throwable`。

```text
Throwable
├── Error
└── Exception
    ├── RuntimeException
    └── 其他受检异常
```

| 类型 | 含义 | 是否通常捕获 | 示例 |
| --- | --- | --- | --- |
| `Error` | JVM 或系统级严重问题 | 通常不捕获 | `OutOfMemoryError`、`StackOverflowError` |
| `Exception` | 程序可以处理或向上抛出的问题 | 可以处理 | `IOException`、`SQLException` |
| `RuntimeException` | 运行时异常，多数来自代码逻辑或参数问题 | 根据场景处理 | `NullPointerException`、`IllegalArgumentException` |

`Error` 通常不是业务代码应该处理的问题。日常开发重点是处理 `Exception` 和 `RuntimeException`。

## 三、受检异常和非受检异常

| 分类 | 是否必须处理 | 常见类型 | 常见来源 |
| --- | --- | --- | --- |
| 受检异常 Checked Exception | 编译器要求处理 | `IOException`、`SQLException` | 文件、数据库、网络等外部资源 |
| 非受检异常 Unchecked Exception | 编译器不强制处理 | `NullPointerException`、`IllegalArgumentException` | 参数错误、状态错误、代码逻辑错误 |

受检异常必须 `try-catch` 或 `throws`。

```java
public void readFile(String path) throws IOException {
    Files.readString(Path.of(path));
}
```

非受检异常可以不声明，但仍然要通过参数校验和合理设计减少发生。

```java
public void updateName(String name) {
    if (name == null || name.isBlank()) {
        throw new IllegalArgumentException("姓名不能为空");
    }
}
```

## 四、try-catch

`try` 中写可能发生异常的代码，`catch` 中写异常发生后的处理。

```java
public class TryCatchDemo {

    public static void main(String[] args) {
        try {
            int result = 10 / 0;
            System.out.println(result);
        } catch (ArithmeticException e) {
            System.out.println("除数不能为 0");
        }

        System.out.println("程序继续执行"); // 输出：程序继续执行
    }
}
```

执行流程：

1. 程序进入 `try`。
2. `10 / 0` 发生 `ArithmeticException`。
3. `try` 中异常后面的代码不再执行。
4. 程序进入匹配的 `catch`。
5. `catch` 执行完后，继续执行后续代码。

## 五、多重 catch

一个 `try` 可以对应多个 `catch`。

```java
public class MultiCatchDemo {

    public static void main(String[] args) {
        try {
            String text = "abc";
            int number = Integer.parseInt(text);
            System.out.println(number);
        } catch (NumberFormatException e) {
            System.out.println("数字格式不正确");
        } catch (Exception e) {
            System.out.println("其他异常");
        }
    }
}
```

子类异常必须写在父类异常前面。否则父类先捕获后，子类 `catch` 永远没有机会执行，代码会编译失败。

## 六、finally

`finally` 中的代码通常用于释放资源，不管是否发生异常，都会尽量执行。

```java
public class FinallyDemo {

    public static void main(String[] args) {
        try {
            System.out.println("开始处理");
            int result = 10 / 2;
            System.out.println(result); // 输出：5
        } catch (ArithmeticException e) {
            System.out.println("计算失败");
        } finally {
            System.out.println("执行 finally"); // 输出：执行 finally
        }
    }
}
```

极端情况下，例如 JVM 被强制停止，`finally` 可能无法执行。普通业务代码中，可以把它理解为“异常处理结束前尽量执行的清理代码”。

## 七、throw

`throw` 用于主动抛出一个异常对象。

```java
public class EmployeeValidator {

    public void validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("员工姓名不能为空");
        }
    }
}
```

当参数不符合要求时，主动抛出异常比让程序继续执行更安全。

## 八、throws

`throws` 写在方法声明上，用于告诉调用者：这个方法可能抛出某种异常。

```java
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileReaderDemo {

    public String readText(String path) throws IOException {
        return Files.readString(Path.of(path));
    }
}
```

调用方需要继续 `throws`，或者使用 `try-catch` 处理。

```java
public class FileReaderMain {

    public static void main(String[] args) {
        FileReaderDemo demo = new FileReaderDemo();

        try {
            String text = demo.readText("data.txt");
            System.out.println(text);
        } catch (IOException e) {
            System.out.println("文件读取失败：" + e.getMessage());
        }
    }
}
```

## 九、throw 和 throws 的区别

| 对比 | `throw` | `throws` |
| --- | --- | --- |
| 位置 | 方法体内部 | 方法声明上 |
| 作用 | 抛出一个具体异常对象 | 声明方法可能抛出的异常类型 |
| 后面内容 | `new 异常类型(...)` | 异常类型列表 |
| 示例 | `throw new IllegalArgumentException(...)` | `public void read() throws IOException` |

## 十、自定义异常

当 Java 自带异常不能准确表达业务含义时，可以自定义异常。

### 10.1 自定义运行时异常

业务参数错误、数据状态不正确等场景，常用运行时异常。

```java
public class EmployeeNotFoundException extends RuntimeException {

    public EmployeeNotFoundException(String message) {
        super(message);
    }
}
```

```java
public class EmployeeService {

    public String findEmployeeName(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("员工 ID 不能为空");
        }

        if (id != 1001L) {
            throw new EmployeeNotFoundException("员工不存在：" + id);
        }

        return "Tanaka";
    }
}
```

`RuntimeException` 不强制调用方处理，但调用方仍然可以捕获。

### 10.2 自定义受检异常

如果希望调用方必须处理，可以继承 `Exception`。

```java
public class FileImportException extends Exception {

    public FileImportException(String message) {
        super(message);
    }
}
```

```java
public class CsvImporter {

    public void importFile(String fileName) throws FileImportException {
        if (fileName == null || !fileName.endsWith(".csv")) {
            throw new FileImportException("CSV 文件名不正确");
        }
    }
}
```

受检异常会影响方法签名。是否使用受检异常，要考虑调用方是否真的能够处理。

## 十一、常见运行时异常

| 异常类型 | 常见原因 |
| --- | --- |
| `NullPointerException` | 对 `null` 调用属性或方法 |
| `ArrayIndexOutOfBoundsException` | 数组下标越界 |
| `StringIndexOutOfBoundsException` | 字符串下标越界 |
| `NumberFormatException` | 字符串不能转换为数字 |
| `ArithmeticException` | 算术错误，例如除数为 0 |
| `ClassCastException` | 类型强制转换失败 |
| `IllegalArgumentException` | 方法参数不合法 |
| `IllegalStateException` | 对象当前状态不允许执行该操作 |

`IllegalArgumentException` 更偏向“传入参数不对”。`IllegalStateException` 更偏向“对象状态不对”。

## 十二、异常链

异常链是指抛出新异常时保留原始异常。

```java
public class ImportService {

    public void importFile(String fileName) {
        try {
            readFile(fileName);
        } catch (Exception e) {
            throw new IllegalStateException("文件导入失败：" + fileName, e);
        }
    }

    private void readFile(String fileName) {
        throw new IllegalArgumentException("文件格式错误");
    }
}
```

`new IllegalStateException(message, e)` 中的 `e` 是原始异常。保留原始异常可以帮助定位真正原因。

## 十三、try-with-resources

手动打开文件、数据库连接、网络连接等资源时，推荐使用 `try-with-resources` 自动关闭资源。

```java
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class TryWithResourcesDemo {

    public static void main(String[] args) {
        Path path = Path.of("employees.txt");

        try (BufferedReader reader = Files.newBufferedReader(path)) {
            String line = reader.readLine();
            System.out.println(line);
        } catch (IOException e) {
            System.out.println("文件读取失败：" + e.getMessage());
        }
    }
}
```

`try (...)` 中声明的资源必须实现 `AutoCloseable`。代码离开 `try` 后，Java 会自动调用资源的 `close()` 方法。

## 十四、日志和 printStackTrace

学习阶段可以用 `e.printStackTrace()` 观察异常堆栈。

```java
try {
    int result = 10 / 0;
    System.out.println(result);
} catch (ArithmeticException e) {
    e.printStackTrace();
}
```

正式项目中不建议只使用 `printStackTrace()`。项目通常使用日志框架记录异常，例如后续 Spring Boot 中会使用日志输出异常信息。

异常日志至少要保留：

- 发生了什么问题。
- 关键参数是什么。
- 原始异常堆栈。

## 十五、常见错误

| 错误 | 原因 | 修正 |
| --- | --- | --- |
| `catch (Exception e)` 一把抓 | 看不出具体错误类型 | 优先捕获具体异常 |
| 空 `catch` | 异常被吞掉，问题无法定位 | 至少记录错误信息或重新抛出 |
| 用异常代替普通判断 | 控制流程不清晰 | 普通业务分支使用 `if` |
| 捕获后只打印一句固定文本 | 丢失原始原因 | 保留 `e.getMessage()` 或异常链 |
| 忘记关闭资源 | 文件或连接泄漏 | 使用 `try-with-resources` |
| 自定义异常名称太模糊 | 无法表达具体问题 | 使用 `EmployeeNotFoundException` 这类明确名称 |

## 十六、本章练习

请完成：

1. 编写 `validateName(String name)`，姓名为空时抛出 `IllegalArgumentException`。
2. 编写 `parseEmployeeId(String text)`，捕获数字格式错误并输出提示。
3. 创建 `EmployeeNotFoundException extends RuntimeException`。
4. 使用 `try-with-resources` 读取一个文本文件。
5. 说明 `throw` 和 `throws` 的区别。

## 十七、本章总结

- `Error` 表示严重错误，通常不由业务代码捕获。
- 受检异常必须处理或声明，运行时异常不强制处理。
- `try-catch-finally` 用于捕获异常和清理资源。
- `throw` 用于主动抛出异常，`throws` 用于声明异常。
- 自定义异常可以让错误含义更清楚。
- 正式项目中应使用日志记录异常，不应只依赖 `printStackTrace()`。
