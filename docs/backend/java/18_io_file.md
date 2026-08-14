# 第18章 Java I/O 与文件处理

> 本章目标：掌握 Java 中文本文件读取、写入、路径处理、常用文件操作和资源关闭，能够完成基础的文件读取、文件生成和目录检查。

## 一、为什么需要文件处理

Java 程序运行时，数据不一定都来自键盘输入或数据库，也可能来自文件。

常见文件处理场景：

- 读取配置文件
- 读取 CSV 文件
- 生成文本报表
- 保存处理结果
- 读取批处理输入文件
- 检查文件是否存在
- 创建目录
- 复制、移动、删除文件

例如：

- 公司每天导出一个员工列表文件，Java 程序需要读取这个文件。
- 程序处理完成后，需要生成一个结果文件。
- 上传文件之前，需要判断保存目录是否存在。

## 二、Path 是什么

`Path` 表示文件或目录的路径。

它不负责读取文件内容，也不负责写入文件内容，只负责表示“文件在哪里”。

使用 `Path` 需要导入：

```java
import java.nio.file.Path;
```

示例：

```java
import java.nio.file.Path;

public class PathDemo {

    public static void main(String[] args) {
        Path path = Path.of("employees.txt"); // 创建一个表示 employees.txt 的路径对象

        System.out.println(path); // 输出：employees.txt
    }
}
```

这里的 `path` 只是一个路径对象，不代表文件一定存在。

## 三、Path.of 方法

`Path.of(...)` 用于创建 `Path` 对象。

常见写法有两种：

```java
Path path = Path.of("employees.txt");
```

```java
Path path = Path.of("data", "employees.txt");
```

### 3.1 Path.of 的参数

| 写法 | 参数含义 | 示例 | 结果 |
| --- | --- | --- | --- |
| `Path.of(String first)` | 传入一个路径字符串 | `Path.of("employees.txt")` | 表示当前运行目录下的 `employees.txt` |
| `Path.of(String first, String... more)` | 第一个参数是起始路径，后续参数是子目录或文件名 | `Path.of("data", "employees.txt")` | 表示 `data/employees.txt` |

`String... more` 表示可以传入多个字符串参数。

例如：

```java
import java.nio.file.Path;

public class PathOfDemo {

    public static void main(String[] args) {
        Path path1 = Path.of("employees.txt"); // 当前运行目录下的 employees.txt
        Path path2 = Path.of("data", "employees.txt"); // data 目录下的 employees.txt
        Path path3 = Path.of("data", "input", "employees.txt"); // data/input 目录下的 employees.txt

        System.out.println(path1); // 输出：employees.txt
        System.out.println(path2); // 输出：data\employees.txt
        System.out.println(path3); // 输出：data\input\employees.txt
    }
}
```

在 Windows 中，路径显示通常使用 `\`。

在 macOS 和 Linux 中，路径显示通常使用 `/`。

使用 `Path.of("data", "employees.txt")` 比直接写 `"data/employees.txt"` 更推荐，因为 Java 会根据操作系统处理路径分隔符。

### 3.2 相对路径的查找位置

下面的代码使用的是相对路径：

```java
Path path = Path.of("employees.txt");
```

相对路径不是从 Java 文件所在目录开始查找，也不是从 `.class` 文件所在目录开始查找。

它通常从程序的运行目录开始查找。

可以使用下面的代码确认当前运行目录：

```java
public class CurrentDirectoryDemo {

    public static void main(String[] args) {
        String currentDirectory = System.getProperty("user.dir"); // 获取当前程序运行目录

        System.out.println(currentDirectory); // 输出：当前 Java 程序的运行目录
    }
}
```

例如在项目根目录运行程序：

```text
D:\java-study
```

那么：

```java
Path.of("employees.txt")
```

查找的是：

```text
D:\java-study\employees.txt
```

如果写成：

```java
Path.of("data", "employees.txt")
```

查找的是：

```text
D:\java-study\data\employees.txt
```

### 3.3 绝对路径

绝对路径是从磁盘根位置开始写的完整路径。

Windows 示例：

```java
Path path = Path.of("D:", "java-study", "data", "employees.txt");
```

macOS / Linux 示例：

```java
Path path = Path.of("/Users/tanaka/java-study/data/employees.txt");
```

学习阶段建议优先使用项目目录下的相对路径，方便移动项目和提交代码。

## 四、Files 是什么

`Files` 是 Java 提供的文件工具类。

它提供了很多静态方法，用来读取、写入、判断、创建、复制、移动和删除文件。

使用 `Files` 需要导入：

```java
import java.nio.file.Files;
```

常见搭配是：

```java
Path path = Path.of("employees.txt"); // 先用 Path 表示文件位置
String content = Files.readString(path); // 再用 Files 读取文件内容
```

可以简单理解为：

| 类 | 作用 |
| --- | --- |
| `Path` | 表示文件或目录在哪里 |
| `Files` | 对文件或目录执行具体操作 |

## 五、Files 常用方法

### 5.1 判断与检查

| 方法 | 作用 | 常见返回值 |
| --- | --- | --- |
| `Files.exists(path)` | 判断文件或目录是否存在 | `boolean` |
| `Files.notExists(path)` | 判断文件或目录是否不存在 | `boolean` |
| `Files.isRegularFile(path)` | 判断是否是普通文件 | `boolean` |
| `Files.isDirectory(path)` | 判断是否是目录 | `boolean` |
| `Files.isReadable(path)` | 判断是否可读 | `boolean` |
| `Files.isWritable(path)` | 判断是否可写 | `boolean` |

示例：

```java
import java.nio.file.Files;
import java.nio.file.Path;

public class FileCheckDemo {

    public static void main(String[] args) {
        Path path = Path.of("employees.txt"); // 表示当前运行目录下的 employees.txt

        System.out.println(Files.exists(path)); // 输出：true 或 false，表示文件是否存在
        System.out.println(Files.isRegularFile(path)); // 输出：true 或 false，表示是否是普通文件
        System.out.println(Files.isReadable(path)); // 输出：true 或 false，表示是否可读取
    }
}
```

### 5.2 读取文件

| 方法 | 作用 | 适合场景 | 返回值 |
| --- | --- | --- | --- |
| `Files.readString(path)` | 一次性读取整个文本文件 | 小文本文件 | `String` |
| `Files.readAllLines(path)` | 一次性读取所有行 | 小文本文件，需要逐行处理 | `List<String>` |
| `Files.newBufferedReader(path)` | 创建缓冲读取对象 | 大文件或逐行读取 | `BufferedReader` |
| `Files.readAllBytes(path)` | 一次性读取所有字节 | 图片、PDF 等二进制文件 | `byte[]` |

读取整个文本文件：

```java
import java.nio.file.Files;
import java.nio.file.Path;

public class FileReadDemo {

    public static void main(String[] args) throws Exception {
        Path path = Path.of("employees.txt"); // 表示要读取的文件路径

        String content = Files.readString(path); // 一次性读取文件中的全部文本内容

        System.out.println(content); // 输出：文件中的全部内容
    }
}
```

逐行读取小文件：

```java
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class ReadAllLinesDemo {

    public static void main(String[] args) throws Exception {
        Path path = Path.of("employees.txt"); // 表示要读取的文件路径

        List<String> lines = Files.readAllLines(path); // 读取所有行，每一行作为 List 中的一个元素

        for (String line : lines) { // 逐行遍历文件内容
            System.out.println(line); // 输出：当前行内容
        }
    }
}
```

逐行读取大文件：

```java
import java.io.BufferedReader;
import java.nio.file.Files;
import java.nio.file.Path;

public class BufferedReadDemo {

    public static void main(String[] args) throws Exception {
        Path path = Path.of("employees.txt"); // 表示要读取的文件路径

        try (BufferedReader reader = Files.newBufferedReader(path)) { // 打开文件读取对象，使用后自动关闭
            String line; // 保存每次读取到的一行内容

            while ((line = reader.readLine()) != null) { // 每次读取一行，直到文件结束
                System.out.println(line); // 输出：当前行内容
            }
        }
    }
}
```

`readString()` 和 `readAllLines()` 会把内容一次性读入内存。文件较大时，不建议这样做。

## 六、写入文本文件

### 6.1 Files 写入常用方法

| 方法 | 作用 | 适合场景 | 返回值 |
| --- | --- | --- | --- |
| `Files.writeString(path, text)` | 写入字符串 | 生成小文本文件 | `Path` |
| `Files.write(path, bytes)` | 写入字节数组 | 生成二进制文件 | `Path` |
| `Files.newBufferedWriter(path)` | 创建缓冲写入对象 | 大文件或逐行写入 | `BufferedWriter` |

写入字符串：

```java
import java.nio.file.Files;
import java.nio.file.Path;

public class FileWriteDemo {

    public static void main(String[] args) throws Exception {
        Path path = Path.of("output.txt"); // 表示要写入的文件路径

        Files.writeString(path, "Tanaka"); // 将字符串写入文件；文件不存在会创建，存在会覆盖原内容

        System.out.println("写入完成"); // 输出：写入完成
    }
}
```

逐行写入：

```java
import java.io.BufferedWriter;
import java.nio.file.Files;
import java.nio.file.Path;

public class BufferedWriteDemo {

    public static void main(String[] args) throws Exception {
        Path path = Path.of("employees.txt"); // 表示要写入的文件路径

        try (BufferedWriter writer = Files.newBufferedWriter(path)) { // 打开文件写入对象，使用后自动关闭
            writer.write("Tanaka"); // 写入第一行内容
            writer.newLine(); // 写入换行
            writer.write("Suzuki"); // 写入第二行内容
            writer.newLine(); // 写入换行
            writer.write("Sato"); // 写入第三行内容
        }

        System.out.println("写入完成"); // 输出：写入完成
    }
}
```

默认情况下，`writeString()` 和 `newBufferedWriter()` 如果文件已存在，会覆盖原内容。

## 七、追加写入

如果希望在文件末尾追加内容，需要使用 `StandardOpenOption.APPEND`。

```java
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class FileAppendDemo {

    public static void main(String[] args) throws Exception {
        Path path = Path.of("employees.txt"); // 表示要追加内容的文件路径

        Files.writeString(path, "Yamada\n", StandardOpenOption.CREATE, StandardOpenOption.APPEND); // 文件不存在则创建，存在则在末尾追加

        System.out.println("追加完成"); // 输出：追加完成
    }
}
```

`StandardOpenOption` 常用值：

| 选项 | 作用 |
| --- | --- |
| `CREATE` | 文件不存在时创建文件 |
| `APPEND` | 在文件末尾追加内容 |
| `TRUNCATE_EXISTING` | 文件已存在时清空原内容 |
| `WRITE` | 以写入方式打开文件 |
| `READ` | 以读取方式打开文件 |

## 八、目录处理

写入文件之前，经常需要先确认目录是否存在。

```java
import java.nio.file.Files;
import java.nio.file.Path;

public class DirectoryDemo {

    public static void main(String[] args) throws Exception {
        Path directoryPath = Path.of("data"); // 表示 data 目录

        if (Files.notExists(directoryPath)) { // 判断目录是否不存在
            Files.createDirectories(directoryPath); // 创建目录；如果父目录不存在，也会一起创建
        }

        Path filePath = Path.of("data", "employees.txt"); // 表示 data 目录下的 employees.txt
        Files.writeString(filePath, "Tanaka"); // 写入文本内容

        System.out.println("文件生成完成"); // 输出：文件生成完成
    }
}
```

目录相关常用方法：

| 方法 | 作用 | 说明 |
| --- | --- | --- |
| `Files.createDirectory(path)` | 创建单层目录 | 父目录不存在时会失败 |
| `Files.createDirectories(path)` | 创建多层目录 | 父目录不存在时会一起创建 |
| `Files.list(path)` | 获取当前目录下一层内容 | 返回 `Stream<Path>` |
| `Files.walk(path)` | 递归获取目录下内容 | 返回 `Stream<Path>` |

## 九、复制、移动和删除

| 方法 | 作用 |
| --- | --- |
| `Files.copy(source, target)` | 复制文件 |
| `Files.move(source, target)` | 移动文件或重命名文件 |
| `Files.delete(path)` | 删除文件或空目录，不存在会报错 |
| `Files.deleteIfExists(path)` | 删除文件或空目录，不存在不报错 |

示例：

```java
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class FileCopyMoveDeleteDemo {

    public static void main(String[] args) throws Exception {
        Path sourcePath = Path.of("employees.txt"); // 原文件路径
        Path copyPath = Path.of("employees_copy.txt"); // 复制后的文件路径
        Path movedPath = Path.of("employees_moved.txt"); // 移动后的文件路径

        Files.copy(sourcePath, copyPath, StandardCopyOption.REPLACE_EXISTING); // 复制文件；目标存在时覆盖
        Files.move(copyPath, movedPath, StandardCopyOption.REPLACE_EXISTING); // 移动文件；也可以理解为重命名
        Files.deleteIfExists(movedPath); // 删除文件；文件不存在也不会报错

        System.out.println("文件操作完成"); // 输出：文件操作完成
    }
}
```

`StandardCopyOption.REPLACE_EXISTING` 表示目标文件已经存在时覆盖。

## 十、编码

文件编码决定了文字如何保存成字节。

如果编码不一致，中文或日文可能出现乱码。

Java 读取和写入文本时可以指定编码。

```java
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileEncodingDemo {

    public static void main(String[] args) throws Exception {
        Path path = Path.of("employees.txt"); // 表示要写入和读取的文件路径

        Files.writeString(path, "田中\n鈴木", StandardCharsets.UTF_8); // 使用 UTF-8 写入文本

        String content = Files.readString(path, StandardCharsets.UTF_8); // 使用 UTF-8 读取文本

        System.out.println(content); // 输出：文件中的日文内容
    }
}
```

常见编码：

| 编码 | 说明 |
| --- | --- |
| `UTF-8` | 推荐使用，跨平台支持好 |
| `MS932` | 日本 Windows 环境中可能遇到 |
| `Shift_JIS` | 日本旧系统或旧文件中可能遇到 |

学习阶段建议统一使用 UTF-8。

## 十一、try-with-resources

`Files.readString()`、`Files.writeString()` 这类方法内部会自动处理资源。

但是使用 `BufferedReader`、`BufferedWriter` 这类对象时，需要关闭资源。

推荐使用 try-with-resources：

```java
import java.io.BufferedReader;
import java.nio.file.Files;
import java.nio.file.Path;

public class TryWithResourcesDemo {

    public static void main(String[] args) throws Exception {
        Path path = Path.of("employees.txt"); // 表示要读取的文件路径

        try (BufferedReader reader = Files.newBufferedReader(path)) { // try 结束后 reader 会自动关闭
            String line; // 保存读取到的一行内容

            while ((line = reader.readLine()) != null) { // 一行一行读取，直到没有内容
                System.out.println(line); // 输出：当前读取到的一行
            }
        }
    }
}
```

如果不关闭文件资源，可能导致文件被占用、内容没有及时写入或系统资源浪费。

## 十二、常见错误

| 错误 | 原因 | 修正 |
| --- | --- | --- |
| 找不到文件 | 相对路径的运行目录理解错误 | 使用 `System.getProperty("user.dir")` 确认运行目录 |
| 中文或日文乱码 | 读取编码和文件实际编码不一致 | 明确指定 `StandardCharsets.UTF_8` 或实际编码 |
| 大文件读取慢或内存占用高 | 一次性读入全部内容 | 使用 `newBufferedReader()` 逐行读取 |
| 写入文件失败 | 目标目录不存在 | 先使用 `Files.createDirectories()` 创建目录 |
| 删除文件报错 | 文件不存在或目录非空 | 使用 `deleteIfExists()`，删除目录前先确认内容 |

## 十三、本章练习

请完成：

1. 创建 `data` 目录。
2. 在 `data` 目录中创建 `employees.txt`。
3. 写入三行员工姓名。
4. 读取文件并逐行输出。
5. 在文件末尾追加一行员工姓名。
6. 说明为什么大文件不建议一次性读取。

## 十四、本章总结

- `Path` 表示文件或目录路径。
- `Path.of(...)` 可以根据字符串创建路径对象。
- 相对路径通常从程序运行目录开始查找。
- `Files` 用于执行读取、写入、判断、创建、复制、移动和删除等文件操作。
- 小文件可以使用 `readString()`、`readAllLines()`。
- 大文件建议使用 `newBufferedReader()` 逐行读取。
- 文本文件处理时要注意编码。
- 手动打开资源时，推荐使用 try-with-resources。
