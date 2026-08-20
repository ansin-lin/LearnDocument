# 第17章 Java 字符串相关类与枚举

> 本章目标：掌握 `String`、`StringBuilder`、`StringBuffer`、`StringJoiner` 和 `enum` 的常见使用场景，能够在项目中正确处理字符串拼接、导出文本、状态值和类型值。

## 一、字符串相关类总览

| 类 | 是否可变 | 线程安全 | 常见用途 |
| --- | --- | --- | --- |
| `String` | 不可变 | 是 | 保存普通文本、名称、状态、编码值 |
| `StringBuilder` | 可变 | 否 | 单线程下高效拼接字符串 |
| `StringBuffer` | 可变 | 是 | 老项目或需要同步的字符串拼接 |
| `StringJoiner` | 可变 | 否 | 用分隔符拼接文本，例如 CSV 一行数据 |

日常项目中最常见的是 `String` 和 `StringBuilder`。`StringBuffer` 在老项目中可能看到。`StringJoiner` 适合处理带分隔符的字符串。

## 二、String

`String` 表示字符串，是不可变对象。

```java
public class StringDemo {

    public static void main(String[] args) {
        String name = "Tanaka";
        String upperName = name.toUpperCase();

        System.out.println(name); // 输出：Tanaka
        System.out.println(upperName); // 输出：TANAKA
    }
}
```

常用方法：

```java
public int length()
public boolean isEmpty()
public boolean isBlank()
public char charAt(int index)
public boolean equals(Object anObject)
public boolean equalsIgnoreCase(String anotherString)
public boolean contains(CharSequence s)
public boolean startsWith(String prefix)
public boolean endsWith(String suffix)
public int indexOf(String str)
public String substring(int beginIndex, int endIndex)
public String replace(CharSequence target, CharSequence replacement)
public String[] split(String regex)
public String trim()
public String strip()
public String toUpperCase()
public String toLowerCase()
public static String valueOf(Object obj)
```

使用示例：

```java
public class StringMethodDemo {

    public static void main(String[] args) {
        String email = " tanaka@example.com ";
        String trimmedEmail = email.trim();

        System.out.println(trimmedEmail.contains("@")); // 输出：true
        System.out.println(trimmedEmail.endsWith(".com")); // 输出：true
        System.out.println(trimmedEmail.substring(0, 6)); // 输出：tanaka
        System.out.println(String.valueOf(1001)); // 输出：1001
    }
}
```

字符串内容比较使用 `equals()`，不要使用 `==`。

## 三、StringBuilder

`StringBuilder` 是可变字符串容器，适合在循环中或多段内容中拼接字符串。

```java
public class StringBuilderDemo {

    public static void main(String[] args) {
        String line = new StringBuilder()
                .append("1001")
                .append(",")
                .append("Tanaka")
                .append(",")
                .append("Sales")
                .toString();

        System.out.println(line); // 输出：1001,Tanaka,Sales
    }
}
```

常用方法：

```java
public StringBuilder append(String str)
public StringBuilder insert(int offset, String str)
public StringBuilder delete(int start, int end)
public StringBuilder replace(int start, int end, String str)
public StringBuilder reverse()
public int length()
public String toString()
```

## 四、StringBuffer

`StringBuffer` 和 `StringBuilder` 用法相似，也是可变字符串容器。

主要区别：

- `StringBuffer` 是线程安全的。
- `StringBuilder` 通常性能更好。
- 新项目中单线程拼接优先使用 `StringBuilder`。

```java
public class StringBufferDemo {

    public static void main(String[] args) {
        StringBuffer buffer = new StringBuffer();
        buffer.append("Java");
        buffer.append(" SQL");

        System.out.println(buffer.toString()); // 输出：Java SQL
    }
}
```

## 五、StringJoiner

`StringJoiner` 用于使用分隔符拼接字符串。

```java
import java.util.StringJoiner;

public class StringJoinerDemo {

    public static void main(String[] args) {
        StringJoiner joiner = new StringJoiner(",");
        joiner.add("1001");
        joiner.add("Tanaka");
        joiner.add("Sales");

        System.out.println(joiner.toString()); // 输出：1001,Tanaka,Sales
    }
}
```

常用构造方法和方法：

```java
public StringJoiner(CharSequence delimiter)
public StringJoiner(CharSequence delimiter, CharSequence prefix, CharSequence suffix)
public StringJoiner add(CharSequence newElement)
public StringJoiner merge(StringJoiner other)
public String toString()
public int length()
```

## 六、枚举是什么

枚举用于表示固定范围内的一组值。

```java
public enum EmployeeStatus {
    ACTIVE,
    RETIRED,
    SUSPENDED
}
```

如果状态值只允许几个固定选项，使用枚举比直接写字符串更安全。

```java
public class EnumDemo {

    public static void main(String[] args) {
        EmployeeStatus status = EmployeeStatus.ACTIVE;
        System.out.println(status); // 输出：ACTIVE
    }
}
```

## 七、枚举常用方法

```java
public final String name()
public final int ordinal()
public static EmployeeStatus valueOf(String name)
public static EmployeeStatus[] values()
```

`ordinal()` 返回枚举声明顺序，从 0 开始。不建议在业务逻辑中依赖 `ordinal()`，因为调整枚举顺序会影响结果。

```java
public class EnumMethodDemo {

    public static void main(String[] args) {
        EmployeeStatus status = EmployeeStatus.ACTIVE;

        System.out.println(status.name()); // 输出：ACTIVE
        System.out.println(status.ordinal()); // 输出：0

        EmployeeStatus parsedStatus = EmployeeStatus.valueOf("RETIRED");
        System.out.println(parsedStatus); // 输出：RETIRED

        for (EmployeeStatus item : EmployeeStatus.values()) {
            System.out.println(item.name());
        }
    }
}
```

## 八、带字段的枚举

枚举可以包含字段、构造方法和普通方法。

```java
public enum EmployeeStatus {
    ACTIVE("在职"),
    RETIRED("退职"),
    SUSPENDED("暂停");

    private final String label;

    EmployeeStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
```

```java
public class EnumFieldDemo {

    public static void main(String[] args) {
        EmployeeStatus status = EmployeeStatus.ACTIVE;
        System.out.println(status.getLabel()); // 输出：在职
    }
}
```

枚举构造方法默认是私有的，不能在外部 `new EmployeeStatus(...)`。

## 九、switch 中使用枚举

```java
public class EnumSwitchDemo {

    public static void main(String[] args) {
        EmployeeStatus status = EmployeeStatus.ACTIVE;

        switch (status) {
            case ACTIVE:
                System.out.println("可以登录系统");
                break;
            case RETIRED:
                System.out.println("不能登录系统");
                break;
            case SUSPENDED:
                System.out.println("账号暂停");
                break;
            default:
                System.out.println("未知状态");
        }
    }
}
```

## 十、常见错误

| 错误 | 原因 | 修正 |
| --- | --- | --- |
| 字符串内容比较使用 `==` | `==` 比较引用地址 | 使用 `equals()` |
| 循环中大量使用 `+` 拼接字符串 | 可能产生大量临时对象 | 使用 `StringBuilder` |
| 普通单线程代码使用 `StringBuffer` | 不需要同步开销 | 使用 `StringBuilder` |
| 手写多个状态字符串 | 容易拼写错误 | 使用 `enum` |
| 依赖 `ordinal()` 保存业务状态 | 枚举顺序变化会影响数据 | 使用明确编码或名称 |
| `valueOf()` 参数不校验 | 名称不存在会抛异常 | 调用前确认输入范围或捕获异常 |

## 十一、本章练习

请完成：

1. 使用 `String` 判断邮箱是否包含 `@`。
2. 使用 `StringBuilder` 拼接 `1001,Tanaka,Sales`。
3. 使用 `StringJoiner` 拼接同样的 CSV 行。
4. 创建 `EmployeeStatus` 枚举，包含 `ACTIVE`、`RETIRED`、`SUSPENDED`。
5. 给 `EmployeeStatus` 增加中文显示名字段 `label`。
6. 使用 `switch` 根据员工状态输出处理结果。

## 十二、本章总结

- `String` 不可变，适合保存普通文本。
- `StringBuilder` 可变，适合高效拼接字符串。
- `StringBuffer` 线程安全，新项目中使用频率低于 `StringBuilder`。
- `StringJoiner` 适合按分隔符拼接字符串。
- 枚举适合表示固定范围的状态值和类型值。
