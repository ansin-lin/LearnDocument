# 第7章 Java 字符串基础

> 本章目标：理解 `String` 的基本特点，掌握字符串创建、比较、查找、截取、替换、分割、去空白和大小写转换，为后续字符串相关类学习打基础。

## 一、String 是什么

`String` 用于保存字符串数据，例如姓名、邮箱、状态、文件名、请求参数等。

```java
public class StringIntroDemo {

    public static void main(String[] args) {
        String name = "Tanaka";
        String email = "tanaka@example.com";

        System.out.println(name); // 输出：Tanaka
        System.out.println(email); // 输出：tanaka@example.com
    }
}
```

`String` 是引用类型，不是基本类型。

## 二、String 的特点

`String` 最重要的特点是不可变。

```java
public class StringImmutableDemo {

    public static void main(String[] args) {
        String name = "Tanaka";
        String upperName = name.toUpperCase();

        System.out.println(name); // 输出：Tanaka
        System.out.println(upperName); // 输出：TANAKA
    }
}
```

`toUpperCase()` 不会修改原来的 `name`，而是返回一个新的字符串。

## 三、字符串创建

常见创建方式：

```java
String name = "Tanaka";
String message = new String("Hello");
```

日常开发中优先使用双引号方式。

```java
public class StringCreateDemo {

    public static void main(String[] args) {
        String name = "Suzuki";
        System.out.println(name); // 输出：Suzuki
    }
}
```

## 四、字符串长度和空判断

常用方法：

```java
public int length()
public boolean isEmpty()
public boolean isBlank()
```

示例：

```java
public class StringBlankDemo {

    public static void main(String[] args) {
        String name = " Tanaka ";
        String blankText = "   ";

        System.out.println(name.length()); // 输出：8
        System.out.println(blankText.isEmpty()); // 输出：false
        System.out.println(blankText.isBlank()); // 输出：true
    }
}
```

`isEmpty()` 判断长度是否为 0。`isBlank()` 判断是否为空字符串或全是空白字符。

## 五、字符串比较

字符串内容比较使用 `equals()`。

```java
public class StringEqualsDemo {

    public static void main(String[] args) {
        String status = "ACTIVE";

        System.out.println(status.equals("ACTIVE")); // 输出：true
        System.out.println(status.equals("RETIRED")); // 输出：false
    }
}
```

为了避免空指针异常，可以把固定值写在前面。

```java
public class SafeEqualsDemo {

    public static void main(String[] args) {
        String status = null;

        System.out.println("ACTIVE".equals(status)); // 输出：false
    }
}
```

忽略大小写比较使用 `equalsIgnoreCase()`。

```java
public class IgnoreCaseDemo {

    public static void main(String[] args) {
        String code = "active";

        System.out.println(code.equalsIgnoreCase("ACTIVE")); // 输出：true
    }
}
```

## 六、查找和判断

常用方法：

```java
public boolean contains(CharSequence s)
public boolean startsWith(String prefix)
public boolean endsWith(String suffix)
public int indexOf(String str)
public int lastIndexOf(String str)
```

示例：

```java
public class StringSearchDemo {

    public static void main(String[] args) {
        String email = "tanaka@example.com";

        System.out.println(email.contains("@")); // 输出：true
        System.out.println(email.startsWith("tanaka")); // 输出：true
        System.out.println(email.endsWith(".com")); // 输出：true
        System.out.println(email.indexOf("@")); // 输出：6
    }
}
```

`indexOf()` 找不到时返回 `-1`。

## 七、截取和替换

常用方法：

```java
public String substring(int beginIndex)
public String substring(int beginIndex, int endIndex)
public String replace(CharSequence target, CharSequence replacement)
```

示例：

```java
public class StringSubstringDemo {

    public static void main(String[] args) {
        String email = "tanaka@example.com";
        String domain = email.substring(email.indexOf("@") + 1);
        String maskedEmail = email.replace("tanaka", "******");

        System.out.println(domain); // 输出：example.com
        System.out.println(maskedEmail); // 输出：******@example.com
    }
}
```

`substring(beginIndex, endIndex)` 包含开始位置，不包含结束位置。

## 八、分割和去空白

常用方法：

```java
public String[] split(String regex)
public String trim()
public String strip()
```

示例：

```java
import java.util.Arrays;

public class StringSplitDemo {

    public static void main(String[] args) {
        String line = "1001,Tanaka,Sales";
        String[] values = line.split(",");

        System.out.println(Arrays.toString(values)); // 输出：[1001, Tanaka, Sales]

        String text = "  Tanaka  ";
        System.out.println(text.trim()); // 输出：Tanaka
        System.out.println(text.strip()); // 输出：Tanaka
    }
}
```

`split()` 的参数是正则表达式。按普通点号 `.` 分割时要写成 `"\\."`。

## 九、大小写转换和类型转换

常用方法：

```java
public String toUpperCase()
public String toLowerCase()
public static String valueOf(Object obj)
```

示例：

```java
public class StringConvertDemo {

    public static void main(String[] args) {
        String code = "active";
        String idText = String.valueOf(1001);

        System.out.println(code.toUpperCase()); // 输出：ACTIVE
        System.out.println("ACTIVE".toLowerCase()); // 输出：active
        System.out.println(idText); // 输出：1001
    }
}
```

`String.valueOf()` 常用于把数字、布尔值或对象转换成字符串。

## 十、常见错误

| 错误 | 原因 | 修正 |
| --- | --- | --- |
| 使用 `==` 比较字符串内容 | `==` 比较引用地址 | 使用 `equals()` |
| 对可能为 `null` 的字符串调用方法 | 会出现空指针异常 | 先判空，或固定值写在前面 |
| 误以为 `toUpperCase()` 会修改原字符串 | `String` 不可变 | 接收方法返回的新字符串 |
| `substring()` 下标写错 | 结束位置不包含 | 明确开始和结束下标 |
| `split(".")` 结果异常 | `.` 是正则特殊字符 | 使用 `split("\\.")` |

## 十一、本章练习

请完成：

1. 定义一个邮箱字符串，判断是否包含 `@`。
2. 从邮箱中截取域名部分。
3. 判断状态字符串是否等于 `ACTIVE`。
4. 把 `"1001,Tanaka,Sales"` 分割成数组。
5. 去除字符串前后空白，并转换成大写。

## 十二、本章总结

- `String` 用于保存文本数据。
- `String` 是不可变对象。
- 字符串内容比较使用 `equals()`。
- `contains()`、`startsWith()`、`endsWith()` 常用于字符串判断。
- `substring()`、`replace()`、`split()` 常用于字符串处理。
- `String.valueOf()` 常用于把其他类型转换成字符串。
