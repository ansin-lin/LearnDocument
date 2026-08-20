# 第18章 Java 常用类与常用方法

> 本章目标：掌握 Java 标准库中高频使用的类、包和方法，能够完成对象比较、数字计算、金额处理、日期时间处理、集合辅助操作、空值判断和唯一编号生成。

## 一、为什么学习常用类

Java 标准库已经提供了很多稳定的工具类。实际开发时，不需要所有功能都自己写。

常用类主要解决这些问题：

- 对象怎么安全比较
- 数字怎么取整、比较、计算
- 金额怎么避免精度问题
- 日期时间怎么创建、格式化、计算
- 集合和数组怎么快速处理
- 空值怎么表达和处理
- 唯一编号怎么生成

本章重点使用这些包：

| 包名 | 常见类 | 主要用途 |
| --- | --- | --- |
| `java.lang` | `Object`、`Integer`、`Long`、`Double`、`Boolean`、`System` | Java 基础类，默认导入 |
| `java.util` | `Objects`、`Optional`、`Arrays`、`Collections`、`UUID` | 工具类、集合辅助、空值处理、唯一编号 |
| `java.math` | `BigDecimal`、`RoundingMode` | 精确金额和小数计算 |
| `java.time` | `LocalDate`、`LocalDateTime`、`LocalTime` | 日期和时间 |
| `java.time.format` | `DateTimeFormatter` | 日期时间格式化和解析 |

文件处理和 JDBC 涉及独立的 API 和运行环境，适合在对应主题中单独学习。

## 二、Object

`Object` 是所有 Java 类的父类。只要是 Java 对象，都直接或间接继承 `Object`。

### 2.1 常用方法定义

```java
public String toString()
```

返回对象的字符串表示。打印对象、记录日志、调试对象内容时经常使用。

```java
public boolean equals(Object obj)
```

接收一个要比较的对象，返回 `boolean`。用于判断当前对象和参数对象是否相等。

```java
public int hashCode()
```

无参数，返回 `int` 类型哈希值。对象放入 `HashSet`、`HashMap` 等基于哈希的集合时会使用。

```java
public final Class<?> getClass()
```

无参数，返回当前对象运行时类型。

### 2.2 toString 示例

```java
public class Employee {
    private Long id;
    private String name;

    public Employee(Long id, String name) {
        this.id = id;
        this.name = name;
    }

    @Override
    public String toString() {
        return "Employee{id=" + id + ", name='" + name + "'}";
    }

    public static void main(String[] args) {
        Employee employee = new Employee(1001L, "Tanaka");
        System.out.println(employee.toString()); // 输出：Employee{id=1001, name='Tanaka'}
        System.out.println(employee); // 输出：Employee{id=1001, name='Tanaka'}
    }
}
```

`System.out.println(employee)` 会自动调用对象的 `toString()`。如果不重写，默认输出通常是类名加哈希值，不利于观察对象内容。

### 2.3 equals 和 hashCode

```java
import java.util.Objects;

public class Employee {
    private Long id;
    private String name;

    public Employee(Long id, String name) {
        this.id = id;
        this.name = name;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Employee other)) {
            return false;
        }
        return Objects.equals(this.id, other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    public static void main(String[] args) {
        Employee employee1 = new Employee(1001L, "Tanaka");
        Employee employee2 = new Employee(1001L, "Tanaka Taro");

        System.out.println(employee1.equals(employee2)); // 输出：true
    }
}
```

如果对象要放入 `HashSet`、作为 `HashMap` 的 key，或者需要按业务字段判断相等，通常要同时重写 `equals()` 和 `hashCode()`。

## 三、Objects

`Objects` 是 `java.util` 包下的工具类，主要用于空值安全判断、对象比较和生成哈希值。

### 3.1 常用方法定义

```java
public static boolean isNull(Object obj)
```

接收任意对象，返回 `boolean`。对象为 `null` 时返回 `true`。

```java
public static boolean nonNull(Object obj)
```

接收任意对象，返回 `boolean`。对象不为 `null` 时返回 `true`。

```java
public static boolean equals(Object a, Object b)
```

接收两个对象，返回 `boolean`。两个对象都为 `null` 时返回 `true`；只有一个为 `null` 时返回 `false`；都不为 `null` 时调用对象自身的 `equals()` 比较。

```java
public static <T> T requireNonNull(T obj)
```

接收一个对象，返回原对象。对象为 `null` 时抛出 `NullPointerException`。

```java
public static <T> T requireNonNullElse(T obj, T defaultObj)
```

接收原值和默认值，返回 `T`。原值不为 `null` 时返回原值，原值为 `null` 时返回默认值。

```java
public static int hash(Object... values)
```

接收一个或多个对象，返回 `int`。常用于根据多个字段生成哈希值。

```java
public static String toString(Object o, String nullDefault)
```

接收对象和默认字符串，返回 `String`。对象为 `null` 时返回默认字符串。

### 3.2 示例

```java
import java.util.Objects;

public class ObjectsDemo {

    public static void main(String[] args) {
        String name = null;
        String status = "ACTIVE";

        System.out.println(Objects.isNull(name)); // 输出：true
        System.out.println(Objects.nonNull(name)); // 输出：false
        System.out.println(Objects.equals(status, "ACTIVE")); // 输出：true
        System.out.println(Objects.requireNonNullElse(name, "未设置")); // 输出：未设置
        System.out.println(Objects.toString(name, "")); // 输出：
    }
}
```

`Objects.equals(a, b)` 比 `a.equals(b)` 更安全，因为 `a` 为 `null` 时不会抛出异常。

## 四、包装类

Java 有基本类型，也有对应的包装类。

| 基本类型 | 包装类 |
| --- | --- |
| `int` | `Integer` |
| `long` | `Long` |
| `double` | `Double` |
| `boolean` | `Boolean` |
| `char` | `Character` |

包装类可以表示 `null`，也提供字符串转换、比较等常用方法。

### 4.1 Integer 和 Long 常用方法定义

```java
public static int parseInt(String s)
```

`Integer` 的静态方法，接收数字字符串，返回基本类型 `int`。

```java
public static Integer valueOf(String s)
```

`Integer` 的静态方法，接收数字字符串，返回包装类型 `Integer`。

```java
public static long parseLong(String s)
```

`Long` 的静态方法，接收数字字符串，返回基本类型 `long`。

```java
public static Long valueOf(String s)
```

`Long` 的静态方法，接收数字字符串，返回包装类型 `Long`。

```java
public static int compare(int x, int y)
```

`Integer` 的静态方法，接收两个 `int`，返回比较结果。左边小于右边返回负数，相等返回 0，左边大于右边返回正数。

```java
public static String toString(int i)
```

`Integer` 的静态方法，接收 `int`，返回对应的字符串。

示例：

```java
public class WrapperDemo {

    public static void main(String[] args) {
        String employeeIdText = "1001";

        int employeeId = Integer.parseInt(employeeIdText);
        Long orderId = Long.valueOf("90001");

        System.out.println(employeeId + 1); // 输出：1002
        System.out.println(orderId); // 输出：90001
        System.out.println(Integer.compare(10, 20)); // 输出：-1
    }
}
```

`parseInt()` 返回基本类型，`valueOf()` 返回包装类。项目中如果值允许为空，通常使用包装类；如果一定有值并且只做计算，可以使用基本类型。

### 4.2 Boolean 常用方法定义

```java
public static boolean parseBoolean(String s)
```

接收字符串，返回基本类型 `boolean`。字符串为 `"true"` 时返回 `true`，忽略大小写；其他字符串返回 `false`。

```java
public static Boolean valueOf(String s)
```

接收字符串，返回包装类型 `Boolean`。

```java
public boolean equals(Object obj)
```

`Boolean.TRUE.equals(value)` 调用的是 `Boolean` 对象的 `equals()` 方法。它接收任意对象，返回 `boolean`，常用于安全判断包装类型是否为 `true`。

示例：

```java
public class BooleanDemo {

    public static void main(String[] args) {
        Boolean enabled = null;

        System.out.println(Boolean.parseBoolean("true")); // 输出：true
        System.out.println(Boolean.parseBoolean("TRUE")); // 输出：true
        System.out.println(Boolean.parseBoolean("yes")); // 输出：false
        System.out.println(Boolean.TRUE.equals(enabled)); // 输出：false
    }
}
```

不要直接写 `enabled == true`，因为 `enabled` 为 `null` 时可能发生空指针异常。

## 五、Math

`Math` 用于常见数学计算。它在 `java.lang` 包中，不需要手动导入。

### 5.1 常用方法定义

```java
public static int max(int a, int b)
public static long max(long a, long b)
public static double max(double a, double b)
```

接收两个数字，返回较大的值。

```java
public static int min(int a, int b)
public static long min(long a, long b)
public static double min(double a, double b)
```

接收两个数字，返回较小的值。

```java
public static int abs(int a)
public static long abs(long a)
public static double abs(double a)
```

接收一个数字，返回绝对值。

```java
public static double ceil(double a)
```

接收 `double`，返回向上取整后的 `double`。

```java
public static double floor(double a)
```

接收 `double`，返回向下取整后的 `double`。

```java
public static long round(double a)
public static int round(float a)
```

接收小数，返回四舍五入后的整数。

```java
public static double pow(double a, double b)
```

接收两个 `double`，返回 `a` 的 `b` 次方。

```java
public static double sqrt(double a)
```

接收 `double`，返回平方根。

```java
public static double random()
```

无参数，返回 `[0.0, 1.0)` 之间的随机数。

### 5.2 示例

```java
public class MathDemo {

    public static void main(String[] args) {
        System.out.println(Math.max(10, 20)); // 输出：20
        System.out.println(Math.min(10, 20)); // 输出：10
        System.out.println(Math.abs(-5)); // 输出：5
        System.out.println(Math.ceil(10.2)); // 输出：11.0
        System.out.println(Math.floor(10.8)); // 输出：10.0
        System.out.println(Math.round(10.5)); // 输出：11
        System.out.println(Math.pow(2, 3)); // 输出：8.0
        System.out.println(Math.sqrt(16)); // 输出：4.0
    }
}
```

金额计算不要使用 `Math.round()` 加 `double` 简单处理，应使用 `BigDecimal` 明确精度和舍入规则。

## 六、BigDecimal

金额、税率、汇率等精确小数计算不要使用 `double`，应使用 `BigDecimal`。

### 6.1 常用方法定义

```java
public BigDecimal(String val)
```

构造方法，接收数字字符串，创建精确小数对象。

```java
public static BigDecimal valueOf(long val)
public static BigDecimal valueOf(double val)
```

静态方法，接收数字，返回 `BigDecimal` 对象。

```java
public BigDecimal add(BigDecimal augend)
```

接收另一个 `BigDecimal`，返回相加后的新对象。

```java
public BigDecimal subtract(BigDecimal subtrahend)
```

接收另一个 `BigDecimal`，返回相减后的新对象。

```java
public BigDecimal multiply(BigDecimal multiplicand)
```

接收另一个 `BigDecimal`，返回相乘后的新对象。

```java
public BigDecimal divide(BigDecimal divisor, int scale, RoundingMode roundingMode)
```

接收除数、小数位数和舍入方式，返回相除后的新对象。除不尽时必须明确舍入规则。

```java
public BigDecimal setScale(int newScale, RoundingMode roundingMode)
```

接收小数位数和舍入方式，返回调整小数位后的新对象。

```java
public int compareTo(BigDecimal val)
```

接收另一个 `BigDecimal`，返回比较结果。当前对象小于参数返回负数，相等返回 0，大于参数返回正数。

```java
public String toPlainString()
```

无参数，返回普通数字字符串。

### 6.2 金额计算示例

```java
import java.math.BigDecimal;
import java.math.RoundingMode;

public class BigDecimalDemo {

    public static void main(String[] args) {
        BigDecimal unitPrice = new BigDecimal("120.50");
        BigDecimal quantity = new BigDecimal("3");
        BigDecimal taxRate = new BigDecimal("0.10");

        BigDecimal amount = unitPrice.multiply(quantity);
        BigDecimal tax = amount.multiply(taxRate).setScale(0, RoundingMode.HALF_UP);
        BigDecimal totalAmount = amount.add(tax);

        System.out.println(amount); // 输出：361.50
        System.out.println(tax); // 输出：36
        System.out.println(totalAmount); // 输出：397.50
    }
}
```

### 6.3 比较大小

```java
import java.math.BigDecimal;

public class BigDecimalCompareDemo {

    public static void main(String[] args) {
        BigDecimal amount = new BigDecimal("100.00");
        BigDecimal limit = new BigDecimal("100");

        System.out.println(amount.equals(limit)); // 输出：false
        System.out.println(amount.compareTo(limit) == 0); // 输出：true
    }
}
```

`equals()` 会比较数值和小数位，`compareTo()` 只比较数值大小。金额判断相等时，通常优先使用 `compareTo()`。

## 七、LocalDate、LocalTime 和 LocalDateTime

`java.time` 包用于处理日期和时间。

| 类 | 含义 | 示例 |
| --- | --- | --- |
| `LocalDate` | 日期，不包含时间 | `2026-08-19` |
| `LocalTime` | 时间，不包含日期 | `09:30:00` |
| `LocalDateTime` | 日期时间，不包含时区 | `2026-08-19T09:30:00` |

### 7.1 常用方法定义

```java
public static LocalDate now()
```

`LocalDate` 的静态方法，无参数，返回当前日期。

```java
public static LocalDate of(int year, int month, int dayOfMonth)
```

`LocalDate` 的静态方法，接收年、月、日，返回指定日期。

```java
public static LocalDate parse(CharSequence text)
```

`LocalDate` 的静态方法，接收 ISO 格式日期字符串，例如 `"2026-08-19"`，返回日期对象。

```java
public LocalDate plusDays(long daysToAdd)
```

接收天数，返回增加天数后的新日期对象。

```java
public LocalDate plusMonths(long monthsToAdd)
```

接收月数，返回增加月数后的新日期对象。

```java
public LocalDate minusMonths(long monthsToSubtract)
```

接收月数，返回减少月数后的新日期对象。

```java
public boolean isBefore(ChronoLocalDate other)
```

接收另一个日期，返回当前日期是否早于参数日期。

```java
public boolean isAfter(ChronoLocalDate other)
```

接收另一个日期，返回当前日期是否晚于参数日期。

```java
public int getYear()
public int getMonthValue()
public int getDayOfMonth()
```

无参数，分别返回年、月份数字、日期中的日。

```java
public static LocalDateTime now()
```

`LocalDateTime` 的静态方法，无参数，返回当前日期时间。

```java
public static LocalDateTime of(int year, int month, int dayOfMonth, int hour, int minute)
```

`LocalDateTime` 的静态方法，接收年、月、日、小时、分钟，返回指定日期时间。

```java
public LocalDate toLocalDate()
```

`LocalDateTime` 的实例方法，无参数，返回日期部分。

### 7.2 示例

```java
import java.time.LocalDate;
import java.time.LocalDateTime;

public class DateTimeDemo {

    public static void main(String[] args) {
        LocalDate hireDate = LocalDate.of(2026, 4, 1);
        LocalDate probationEndDate = hireDate.plusMonths(3);
        LocalDateTime createdAt = LocalDateTime.of(2026, 8, 19, 9, 30);

        System.out.println(hireDate); // 输出：2026-04-01
        System.out.println(probationEndDate); // 输出：2026-07-01
        System.out.println(hireDate.isBefore(probationEndDate)); // 输出：true
        System.out.println(createdAt.toLocalDate()); // 输出：2026-08-19
    }
}
```

`LocalDate` 和 `LocalDateTime` 是不可变对象。`plusMonths()`、`plusDays()` 等方法不会修改原对象，而是返回新对象。

## 八、DateTimeFormatter

`DateTimeFormatter` 用于日期时间格式化和解析。

### 8.1 常用方法定义

```java
public static DateTimeFormatter ofPattern(String pattern)
```

接收日期格式字符串，返回 `DateTimeFormatter` 格式化器。

```java
public String format(DateTimeFormatter formatter)
```

`LocalDate`、`LocalDateTime` 等日期时间对象的方法，接收格式化器，返回格式化后的字符串。

```java
public static LocalDate parse(CharSequence text, DateTimeFormatter formatter)
```

`LocalDate` 的静态方法，接收日期字符串和格式化器，返回日期对象。

```java
public static LocalDateTime parse(CharSequence text, DateTimeFormatter formatter)
```

`LocalDateTime` 的静态方法，接收日期时间字符串和格式化器，返回日期时间对象。

常见格式：

| 格式 | 示例结果 | 说明 |
| --- | --- | --- |
| `yyyy-MM-dd` | `2026-08-19` | 常见系统日期格式 |
| `yyyy/MM/dd` | `2026/08/19` | 日本项目中也常见 |
| `yyyyMMdd` | `20260819` | 文件名、批处理参数常见 |
| `yyyy-MM-dd HH:mm:ss` | `2026-08-19 09:30:00` | 日期时间格式 |

### 8.2 示例

```java
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class DateFormatDemo {

    public static void main(String[] args) {
        LocalDate date = LocalDate.of(2026, 8, 19);
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd");

        String text = date.format(dateFormatter);
        System.out.println(text); // 输出：2026/08/19

        LocalDate parsedDate = LocalDate.parse("2026/08/19", dateFormatter);
        System.out.println(parsedDate); // 输出：2026-08-19

        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDateTime dateTime = LocalDateTime.parse("2026-08-19 09:30:00", dateTimeFormatter);
        System.out.println(dateTime); // 输出：2026-08-19T09:30
    }
}
```

格式字符串大小写有含义。`MM` 表示月份，`mm` 表示分钟，写错会导致结果不符合预期。

## 九、Optional

`Optional` 用于表达“可能有值，也可能没有值”。

它常用于方法返回值，不建议滥用为实体类字段或方法参数。

### 9.1 常用方法定义

```java
public static <T> Optional<T> of(T value)
```

接收非空对象，返回 `Optional<T>`。传入 `null` 会抛出异常。

```java
public static <T> Optional<T> ofNullable(T value)
```

接收可为空对象，返回 `Optional<T>`。传入 `null` 时得到空的 `Optional`。

```java
public boolean isPresent()
```

无参数，返回是否有值。

```java
public boolean isEmpty()
```

无参数，返回是否没有值。

```java
public T get()
```

无参数，直接返回内部值。没有值时抛出 `NoSuchElementException`。

```java
public T orElse(T other)
```

接收默认值。内部有值时返回内部值，没有值时返回默认值。

```java
public T orElseGet(Supplier<? extends T> supplier)
```

接收提供默认值的函数。没有值时才执行该函数并返回结果。

```java
public T orElseThrow()
```

无参数。内部有值时返回值，没有值时抛出异常。

```java
public <U> Optional<U> map(Function<? super T, ? extends U> mapper)
```

接收转换函数，有值时把内部值转换成另一个类型，并返回新的 `Optional`。

```java
public Optional<T> filter(Predicate<? super T> predicate)
```

接收判断函数。有值且满足条件时保留，否则返回空的 `Optional`。

### 9.2 示例

```java
import java.util.Optional;

public class OptionalDemo {

    public static void main(String[] args) {
        Optional<String> employeeName = Optional.ofNullable(null);

        String result = employeeName.orElse("未设置");
        System.out.println(result); // 输出：未设置

        Optional<String> email = Optional.of("tanaka@example.com");
        String domain = email
                .filter(value -> value.contains("@"))
                .map(value -> value.substring(value.indexOf("@") + 1))
                .orElse("unknown");

        System.out.println(domain); // 输出：example.com
    }
}
```

不要在没有判断的情况下直接调用 `get()`。如果没有值，`get()` 会抛出 `NoSuchElementException`。

## 十、Arrays

`Arrays` 是 `java.util` 包下的数组工具类。

### 10.1 常用方法定义

```java
public static String toString(int[] a)
public static String toString(Object[] a)
```

接收一维数组，返回可读字符串。不同数组类型有不同重载方法。

```java
public static String deepToString(Object[] a)
```

接收多维数组，返回可读字符串。

```java
public static void sort(int[] a)
public static void sort(Object[] a)
```

接收数组，对原数组进行排序，无返回值。

```java
public static int[] copyOf(int[] original, int newLength)
public static <T> T[] copyOf(T[] original, int newLength)
```

接收原数组和新长度，返回复制后的新数组。

```java
public static boolean equals(int[] a, int[] a2)
public static boolean equals(Object[] a, Object[] a2)
```

接收两个数组，返回内容是否相等。

```java
public static <T> List<T> asList(T... a)
```

接收多个元素或数组，返回固定长度列表。

### 10.2 示例

```java
import java.util.Arrays;
import java.util.List;

public class ArraysDemo {

    public static void main(String[] args) {
        int[] scores = {80, 95, 70};

        Arrays.sort(scores);
        System.out.println(Arrays.toString(scores)); // 输出：[70, 80, 95]

        int[] copiedScores = Arrays.copyOf(scores, 5);
        System.out.println(Arrays.toString(copiedScores)); // 输出：[70, 80, 95, 0, 0]

        List<String> names = Arrays.asList("Tanaka", "Suzuki");
        System.out.println(names); // 输出：[Tanaka, Suzuki]
    }
}
```

`Arrays.asList()` 返回的列表长度固定，不能直接 `add()` 或 `remove()`。如果需要可变列表，可以再创建 `new ArrayList<>(Arrays.asList(...))`。

## 十一、Collections

`Collections` 是 `java.util` 包下的集合工具类，注意它和集合接口 `Collection` 不是同一个东西。

### 11.1 常用方法定义

```java
public static <T extends Comparable<? super T>> void sort(List<T> list)
```

接收可变列表，对列表原地排序，无返回值。

```java
public static void reverse(List<?> list)
```

接收可变列表，反转列表元素顺序，无返回值。

```java
public static <T extends Object & Comparable<? super T>> T max(Collection<? extends T> coll)
```

接收集合，返回最大元素。

```java
public static <T extends Object & Comparable<? super T>> T min(Collection<? extends T> coll)
```

接收集合，返回最小元素。

```java
public static final <T> List<T> emptyList()
```

无参数，返回不可修改的空列表。

```java
public static <T> List<T> singletonList(T o)
```

接收一个元素，返回只包含该元素的不可修改列表。

```java
public static <T> List<T> unmodifiableList(List<? extends T> list)
```

接收一个列表，返回不可修改列表视图。

### 11.2 示例

```java
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CollectionsDemo {

    public static void main(String[] args) {
        List<Integer> scores = new ArrayList<>();
        scores.add(80);
        scores.add(95);
        scores.add(70);

        Collections.sort(scores);
        System.out.println(scores); // 输出：[70, 80, 95]

        Collections.reverse(scores);
        System.out.println(scores); // 输出：[95, 80, 70]

        System.out.println(Collections.max(scores)); // 输出：95
        System.out.println(Collections.min(scores)); // 输出：70

        List<String> emptyNames = Collections.emptyList();
        System.out.println(emptyNames); // 输出：[]
    }
}
```

`Collections.emptyList()` 常用于返回“没有结果”的空集合，优于返回 `null`。

## 十二、UUID

`UUID` 是 `java.util` 包下的唯一编号工具类。

它常用于生成临时唯一编号、外部公开编号、文件名后缀、请求追踪编号等。

### 12.1 常用方法定义

```java
public static UUID randomUUID()
```

无参数，返回随机生成的 `UUID` 对象。

```java
public String toString()
```

无参数，返回标准 UUID 字符串。

```java
public static UUID fromString(String name)
```

接收 UUID 字符串，返回 `UUID` 对象。字符串格式不正确时会抛出异常。

### 12.2 示例

```java
import java.util.UUID;

public class UuidDemo {

    public static void main(String[] args) {
        UUID uuid = UUID.randomUUID();
        String requestId = uuid.toString();

        System.out.println(requestId); // 输出示例：550e8400-e29b-41d4-a716-446655440000

        UUID parsedUuid = UUID.fromString(requestId);
        System.out.println(parsedUuid.equals(uuid)); // 输出：true
    }
}
```

`randomUUID()` 每次运行结果不同，示例输出只是格式说明，不是固定值。

## 十三、System

`System` 是 `java.lang` 包下的系统工具类。

### 13.1 常用方法和属性定义

```java
public static final PrintStream out
```

`System` 的标准输出对象。常用 `System.out.println(value)` 输出内容。

```java
public void println(String x)
public void println(int x)
public void println(Object x)
```

`System.out` 的方法，接收要输出的内容，无返回值。不同参数类型有不同重载方法。

```java
public static long currentTimeMillis()
```

无参数，返回当前时间戳，单位毫秒。

```java
public static long nanoTime()
```

无参数，返回高精度时间值，常用于计算耗时。

```java
public static String getenv(String name)
```

接收环境变量名，返回环境变量值。不存在时返回 `null`。

```java
public static String getProperty(String key)
```

接收系统属性名，返回 Java 系统属性值。不存在时返回 `null`。

```java
public static String lineSeparator()
```

无参数，返回当前系统换行符。

### 13.2 示例

```java
public class SystemDemo {

    public static void main(String[] args) {
        long start = System.nanoTime();

        String javaVersion = System.getProperty("java.version");
        String userHome = System.getProperty("user.home");

        long end = System.nanoTime();

        System.out.println(javaVersion); // 输出示例：17.0.12
        System.out.println(userHome); // 输出示例：C:\Users\user
        System.out.println(end - start > 0); // 输出：true
    }
}
```

`currentTimeMillis()` 适合记录当前时间点，`nanoTime()` 更适合计算代码执行耗时。

## 十四、常用类选择建议

| 需求 | 推荐类或方法 | 不推荐写法 |
| --- | --- | --- |
| 安全比较两个对象 | `Objects.equals(a, b)` | `a.equals(b)` |
| 字符串转数字 | `Integer.parseInt()`、`Long.valueOf()` | 手动逐字符转换 |
| 金额计算 | `BigDecimal` | `double` |
| 日期时间 | `java.time` | 旧的 `Date` 处理所有场景 |
| 日期格式化 | `DateTimeFormatter` | 手动拼接年月日 |
| 空集合返回 | `Collections.emptyList()` | 返回 `null` |
| 数组打印 | `Arrays.toString()` | 直接 `System.out.println(array)` |
| 唯一编号 | `UUID.randomUUID()` | 自己用时间戳随意拼接 |

## 十五、常见错误

| 错误 | 原因 | 修正 |
| --- | --- | --- |
| 金额用 `double` | 小数精度可能出错 | 使用 `BigDecimal` |
| `BigDecimal` 用 `equals()` 判断金额相等 | 小数位不同会导致结果为 `false` | 使用 `compareTo()` |
| 日期仍用旧 `Date` 处理所有场景 | API 不够直观 | 优先使用 `java.time` |
| 直接 `a.equals(b)` | `a` 为 `null` 会异常 | 使用 `Objects.equals(a, b)` |
| 滥用 Optional 字段 | 增加复杂度 | 主要用于返回值 |
| 直接打印数组对象 | 输出类型和地址信息 | 使用 `Arrays.toString()` |
| 修改 `Arrays.asList()` 返回的列表长度 | 该列表长度固定 | 需要可变列表时创建 `new ArrayList<>(...)` |
| 使用 `Math.random()` 生成安全令牌 | 随机性不适合安全场景 | 安全场景使用专门的安全随机方案 |

## 十六、本章练习

请完成：

1. 使用 `BigDecimal` 计算订单总金额、消费税和含税金额。
2. 使用 `LocalDate` 保存入社日期，并计算试用期结束日期。
3. 使用 `DateTimeFormatter` 输出 `yyyy/MM/dd` 和 `yyyyMMdd` 两种格式。
4. 使用 `Objects.equals()` 比较两个状态值。
5. 使用 `Arrays.sort()` 对分数数组排序并输出。
6. 使用 `Collections.emptyList()` 表示没有查询结果。
7. 使用 `UUID.randomUUID()` 生成一个请求编号并打印。

## 十七、本章总结

- `Object` 是所有类的父类，常用方法包括 `toString()`、`equals()` 和 `hashCode()`。
- `Objects` 可以进行空值安全比较和默认值处理。
- 包装类用于基本类型和对象类型之间的转换，也可以表达空值。
- `BigDecimal` 适合金额和精确小数计算。
- `java.time` 和 `DateTimeFormatter` 用于日期时间处理。
- `Arrays` 和 `Collections` 提供数组与集合的常用辅助方法。
- `Optional` 主要用于表达方法返回值可能为空。
