# 第17章 Java 常用类详解

> 本章目标：掌握 Java 中常用的标准类，能够处理对象比较、数字计算、日期时间、空值判断和简单工具方法。

## 一、为什么学习常用类

Java 标准库提供了大量已经写好的类。学习常用类可以减少重复代码，也能让代码更稳定。

本章只讲基础阶段高频内容：

- `Object`
- `Objects`
- `Math`
- `BigDecimal`
- `LocalDate`
- `LocalDateTime`
- `DateTimeFormatter`
- `Optional`

文件、JDBC相关内容放到后续对应章节。

## 二、Object

`Object` 是所有 Java 类的父类。

常见方法：

| 方法 | 作用 |
| --- | --- |
| `toString()` | 返回对象字符串表示 |
| `equals()` | 判断对象是否相等 |
| `hashCode()` | 返回对象哈希值 |

示例：

```java
public class Employee {
    private Long id;
    private String name;

    @Override
    public String toString() {
        return "Employee{id=" + id + ", name='" + name + "'}";
    }
}
```

打印对象内容时，合理的 `toString()` 可以帮助观察对象状态。

## 三、Objects

`Objects` 是 `java.util` 包下的工具类。

```java
import java.util.Objects;

public class ObjectsDemo {

    public static void main(String[] args) {
        String name = null;

        System.out.println(Objects.isNull(name)); // 输出：true
        System.out.println(Objects.nonNull(name)); // 输出：false
        System.out.println(Objects.equals(name, "Tanaka")); // 输出：false
    }
}
```

常用方法：

| 方法 | 作用 |
| --- | --- |
| `Objects.isNull(value)` | 判断是否为 `null` |
| `Objects.nonNull(value)` | 判断是否不为 `null` |
| `Objects.equals(a, b)` | 安全比较两个对象 |
| `Objects.requireNonNull(value)` | 要求对象不能为 `null` |

## 四、Math

`Math` 用于常见数学计算。

```java
public class MathDemo {

    public static void main(String[] args) {
        System.out.println(Math.max(10, 20)); // 输出：20
        System.out.println(Math.min(10, 20)); // 输出：10
        System.out.println(Math.ceil(10.2)); // 输出：11.0
        System.out.println(Math.floor(10.8)); // 输出：10.0
    }
}
```

## 五、BigDecimal

金额计算不要使用 `double`，应使用 `BigDecimal`。

```java
import java.math.BigDecimal;

public class BigDecimalDemo {

    public static void main(String[] args) {
        BigDecimal unitPrice = new BigDecimal("120.50");
        BigDecimal quantity = new BigDecimal("3");

        BigDecimal totalAmount = unitPrice.multiply(quantity);

        System.out.println(totalAmount); // 输出：361.50
    }
}
```

`BigDecimal` 建议使用字符串创建，避免小数精度问题。

## 六、LocalDate 和 LocalDateTime

`LocalDate` 表示日期，`LocalDateTime` 表示日期时间。

```java
import java.time.LocalDate;
import java.time.LocalDateTime;

public class DateTimeDemo {

    public static void main(String[] args) {
        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();

        System.out.println(today);
        System.out.println(now);
    }
}
```

常用于：

- 入社日期
- 创建时间
- 更新时间
- 查询期间

## 七、DateTimeFormatter

`DateTimeFormatter` 用于日期时间格式化和解析。

```java
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class DateFormatDemo {

    public static void main(String[] args) {
        LocalDate date = LocalDate.of(2026, 8, 12);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd");

        String text = date.format(formatter);

        System.out.println(text); // 输出：2026/08/12
    }
}
```

## 八、Optional

`Optional` 用于表达“可能有值，也可能没有值”。

```java
import java.util.Optional;

public class OptionalDemo {

    public static void main(String[] args) {
        Optional<String> employeeName = Optional.ofNullable(null);

        String result = employeeName.orElse("未设置");

        System.out.println(result); // 输出：未设置
    }
}
```

新人阶段要注意：`Optional` 不应滥用为所有字段类型，常见于方法返回值。

## 九、常见错误

| 错误 | 原因 | 修正 |
| --- | --- | --- |
| 金额用 `double` | 小数精度可能出错 | 使用 `BigDecimal` |
| 日期仍用旧 `Date` 处理所有场景 | API 不够直观 | 优先使用 `java.time` |
| 直接 `a.equals(b)` | `a` 为 null 会异常 | 使用 `Objects.equals(a, b)` |
| 滥用 Optional 字段 | 增加复杂度 | 主要用于返回值 |

## 十、本章练习

请完成：

1. 使用 `BigDecimal` 计算订单总金额。
2. 使用 `LocalDate` 保存入社日期。
3. 使用 `DateTimeFormatter` 输出 `yyyy/MM/dd` 格式日期。
4. 使用 `Objects.equals()` 比较两个状态值。
