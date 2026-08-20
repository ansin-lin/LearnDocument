# 第22章 Java JUnit 测试基础

> 本章目标：掌握 JUnit 5 的基本测试写法，能够为普通 Java 方法编写正常值测试、边界值测试、异常测试和空值测试。

## 一、为什么需要测试

测试用于验证代码结果是否符合预期。

程序写完后，不能只靠手动运行一次来判断是否正确。因为代码后续可能被修改，如果没有测试，很难及时发现原来正确的功能被改坏了。

例如下面这个总价计算方法：

```java
public class PriceCalculator {

    public int calculateTotalPrice(int unitPrice, int quantity) {
        return unitPrice * quantity;
    }
}
```

需要验证：

- 单价 100，数量 3，结果是否是 300
- 数量 0，结果是否是 0
- 单价为负数时，是否应该禁止
- 数量为负数时，是否应该禁止

这些验证都可以写成测试用例。

## 二、测试用例是什么

测试用例是对一个具体场景的验证。

一个测试用例通常包含：

1. 准备数据。
2. 执行被测试的方法。
3. 判断实际结果是否等于预期结果。

例如：

| 测试场景 | 输入 | 预期结果 |
| --- | --- | --- |
| 正常计算 | 单价 100，数量 3 | 返回 300 |
| 数量为 0 | 单价 100，数量 0 | 返回 0 |
| 单价为负数 | 单价 -100，数量 3 | 抛出异常 |
| 数量为负数 | 单价 100，数量 -1 | 抛出异常 |

测试不是随便写几个值，而是要覆盖常见场景、边界场景和异常场景。

## 三、添加 JUnit 依赖

在 Maven 项目的 `pom.xml` 中添加 JUnit 5 依赖：

```xml
<dependency>
    <groupId>org.junit.jupiter</groupId>
    <artifactId>junit-jupiter</artifactId>
    <version>5.10.0</version>
    <scope>test</scope>
</dependency>
```

说明：

| 标签 | 作用 |
| --- | --- |
| `groupId` | 依赖所属组织 |
| `artifactId` | 依赖名称 |
| `version` | 依赖版本 |
| `scope` | 依赖使用范围 |

`scope` 写 `test`，表示 JUnit 只在测试代码中使用，不参与正式程序运行。

## 四、测试代码放在哪里

Maven 项目中，正式代码和测试代码要分开放。

```text
employee-demo
├── pom.xml
└── src
    ├── main
    │   └── java
    │       └── PriceCalculator.java
    └── test
        └── java
            └── PriceCalculatorTest.java
```

| 目录 | 作用 |
| --- | --- |
| `src/main/java` | 放正式 Java 代码 |
| `src/test/java` | 放测试 Java 代码 |

测试类名通常使用：

```text
被测试类名 + Test
```

例如：

```text
PriceCalculator
PriceCalculatorTest
```

## 五、准备被测试类

文件位置：

```text
src/main/java/PriceCalculator.java
```

代码：

```java
public class PriceCalculator {

    public int calculateTotalPrice(int unitPrice, int quantity) {
        if (unitPrice < 0) { // 判断单价是否为负数
            throw new IllegalArgumentException("单价不能为负数"); // 单价非法时抛出异常
        }

        if (quantity < 0) { // 判断数量是否为负数
            throw new IllegalArgumentException("数量不能为负数"); // 数量非法时抛出异常
        }

        return unitPrice * quantity; // 返回单价乘以数量后的总价
    }
}
```

这个类只有一个方法：

| 方法 | 参数 | 返回值 | 作用 |
| --- | --- | --- | --- |
| `calculateTotalPrice` | `unitPrice`、`quantity` | `int` | 根据单价和数量计算总价 |

## 六、基本测试写法

文件位置：

```text
src/test/java/PriceCalculatorTest.java
```

代码：

```java
import org.junit.jupiter.api.Test; // 导入 @Test 注解

import static org.junit.jupiter.api.Assertions.assertEquals; // 导入判断相等的断言方法

public class PriceCalculatorTest {

    @Test // 表示下面的方法是一个测试方法
    void shouldCalculateTotalPrice() {
        PriceCalculator calculator = new PriceCalculator(); // 创建被测试类对象

        int totalPrice = calculator.calculateTotalPrice(100, 3); // 执行被测试方法

        assertEquals(300, totalPrice); // 判断预期结果 300 和实际结果是否相等
    }
}
```

说明：

| 写法 | 作用 |
| --- | --- |
| `@Test` | 标记一个测试方法 |
| `assertEquals(expected, actual)` | 判断预期值和实际值是否相等 |
| `expected` | 预期结果 |
| `actual` | 实际结果 |

测试方法名建议表达测试意图。

例如：

```java
void shouldCalculateTotalPrice()
```

含义是：应该可以计算总价。

## 七、正常值测试

正常值测试用于验证最常见、最普通的输入。

```java
import org.junit.jupiter.api.Test; // 导入 @Test 注解

import static org.junit.jupiter.api.Assertions.assertEquals; // 导入 assertEquals 断言

public class PriceCalculatorTest {

    @Test // 标记测试方法
    void shouldCalculateTotalPriceWhenUnitPriceAndQuantityArePositive() {
        PriceCalculator calculator = new PriceCalculator(); // 创建被测试对象

        int totalPrice = calculator.calculateTotalPrice(100, 3); // 单价 100，数量 3

        assertEquals(300, totalPrice); // 输出判断：预期 300，实际也应该是 300
    }
}
```

这个测试验证的是：

| 输入 | 预期 |
| --- | --- |
| `unitPrice = 100`，`quantity = 3` | `300` |

## 八、边界值测试

边界值测试用于验证临界情况。

例如数量为 `0` 时，不是错误，而是总价应该为 `0`。

```java
import org.junit.jupiter.api.Test; // 导入 @Test 注解

import static org.junit.jupiter.api.Assertions.assertEquals; // 导入 assertEquals 断言

public class PriceCalculatorTest {

    @Test // 标记测试方法
    void shouldReturnZeroWhenQuantityIsZero() {
        PriceCalculator calculator = new PriceCalculator(); // 创建被测试对象

        int totalPrice = calculator.calculateTotalPrice(100, 0); // 单价 100，数量 0

        assertEquals(0, totalPrice); // 输出判断：预期 0，实际也应该是 0
    }
}
```

常见边界值：

| 类型 | 示例 |
| --- | --- |
| 数字边界 | `0`、`1`、最大值、最小值 |
| 字符串边界 | 空字符串、最大长度、最小长度 |
| 集合边界 | 空集合、只有一个元素、多个元素 |
| 日期边界 | 月初、月末、年末、闰年 |

## 九、异常测试

异常测试用于验证非法输入是否会抛出指定异常。

单价不能为负数：

```java
import org.junit.jupiter.api.Test; // 导入 @Test 注解

import static org.junit.jupiter.api.Assertions.assertThrows; // 导入异常断言

public class PriceCalculatorTest {

    @Test // 标记测试方法
    void shouldThrowExceptionWhenUnitPriceIsNegative() {
        PriceCalculator calculator = new PriceCalculator(); // 创建被测试对象

        assertThrows(IllegalArgumentException.class, () -> { // 判断是否抛出 IllegalArgumentException
            calculator.calculateTotalPrice(-100, 3); // 执行非法输入：单价为负数
        });
    }
}
```

数量不能为负数：

```java
import org.junit.jupiter.api.Test; // 导入 @Test 注解

import static org.junit.jupiter.api.Assertions.assertThrows; // 导入异常断言

public class PriceCalculatorTest {

    @Test // 标记测试方法
    void shouldThrowExceptionWhenQuantityIsNegative() {
        PriceCalculator calculator = new PriceCalculator(); // 创建被测试对象

        assertThrows(IllegalArgumentException.class, () -> { // 判断是否抛出 IllegalArgumentException
            calculator.calculateTotalPrice(100, -1); // 执行非法输入：数量为负数
        });
    }
}
```

`assertThrows()` 的两个参数：

| 参数 | 含义 |
| --- | --- |
| 第 1 个参数 | 预期抛出的异常类型 |
| 第 2 个参数 | 会执行并可能抛出异常的代码 |

## 十、空值测试

如果方法参数是引用类型，需要考虑 `null`。

下面准备一个员工姓名校验类。

文件位置：

```text
src/main/java/EmployeeValidator.java
```

代码：

```java
public class EmployeeValidator {

    public void validateName(String name) {
        if (name == null) { // 判断姓名是否为 null
            throw new IllegalArgumentException("姓名不能为 null"); // name 为 null 时抛出异常
        }

        if (name.isBlank()) { // 判断姓名是否为空字符串或只有空白字符
            throw new IllegalArgumentException("姓名不能为空"); // name 为空白时抛出异常
        }
    }
}
```

测试代码：

```java
import org.junit.jupiter.api.Test; // 导入 @Test 注解

import static org.junit.jupiter.api.Assertions.assertThrows; // 导入异常断言

public class EmployeeValidatorTest {

    @Test // 标记测试方法
    void shouldThrowExceptionWhenNameIsNull() {
        EmployeeValidator validator = new EmployeeValidator(); // 创建被测试对象

        assertThrows(IllegalArgumentException.class, () -> { // 判断是否抛出 IllegalArgumentException
            validator.validateName(null); // 执行非法输入：姓名为 null
        });
    }

    @Test // 标记测试方法
    void shouldThrowExceptionWhenNameIsBlank() {
        EmployeeValidator validator = new EmployeeValidator(); // 创建被测试对象

        assertThrows(IllegalArgumentException.class, () -> { // 判断是否抛出 IllegalArgumentException
            validator.validateName(""); // 执行非法输入：姓名为空字符串
        });
    }
}
```

空值测试常用于：

- 字符串参数
- 对象参数
- 集合参数
- 查询结果

## 十一、多个测试用例写在一个测试类中

同一个被测试类的多个测试方法，可以写在同一个测试类中。

文件位置：

```text
src/test/java/PriceCalculatorTest.java
```

完整示例：

```java
import org.junit.jupiter.api.Test; // 导入 @Test 注解

import static org.junit.jupiter.api.Assertions.assertEquals; // 导入判断相等的断言
import static org.junit.jupiter.api.Assertions.assertThrows; // 导入异常断言

public class PriceCalculatorTest {

    @Test // 标记正常值测试
    void shouldCalculateTotalPriceWhenUnitPriceAndQuantityArePositive() {
        PriceCalculator calculator = new PriceCalculator(); // 创建被测试对象

        int totalPrice = calculator.calculateTotalPrice(100, 3); // 执行计算

        assertEquals(300, totalPrice); // 判断预期值和实际值是否一致
    }

    @Test // 标记边界值测试
    void shouldReturnZeroWhenQuantityIsZero() {
        PriceCalculator calculator = new PriceCalculator(); // 创建被测试对象

        int totalPrice = calculator.calculateTotalPrice(100, 0); // 数量为 0

        assertEquals(0, totalPrice); // 判断结果是否为 0
    }

    @Test // 标记异常测试
    void shouldThrowExceptionWhenUnitPriceIsNegative() {
        PriceCalculator calculator = new PriceCalculator(); // 创建被测试对象

        assertThrows(IllegalArgumentException.class, () -> { // 判断是否抛出指定异常
            calculator.calculateTotalPrice(-100, 3); // 单价为负数
        });
    }

    @Test // 标记异常测试
    void shouldThrowExceptionWhenQuantityIsNegative() {
        PriceCalculator calculator = new PriceCalculator(); // 创建被测试对象

        assertThrows(IllegalArgumentException.class, () -> { // 判断是否抛出指定异常
            calculator.calculateTotalPrice(100, -1); // 数量为负数
        });
    }
}
```

这个测试类覆盖了：

| 测试方法 | 测试类型 | 验证内容 |
| --- | --- | --- |
| `shouldCalculateTotalPriceWhenUnitPriceAndQuantityArePositive` | 正常值 | 正常单价和数量可以计算 |
| `shouldReturnZeroWhenQuantityIsZero` | 边界值 | 数量为 0 时返回 0 |
| `shouldThrowExceptionWhenUnitPriceIsNegative` | 异常值 | 单价为负数时抛出异常 |
| `shouldThrowExceptionWhenQuantityIsNegative` | 异常值 | 数量为负数时抛出异常 |

## 十二、常用断言

| 断言 | 作用 | 示例 |
| --- | --- | --- |
| `assertEquals(expected, actual)` | 判断两个值相等 | `assertEquals(300, totalPrice)` |
| `assertNotEquals(expected, actual)` | 判断两个值不相等 | `assertNotEquals(0, totalPrice)` |
| `assertTrue(value)` | 判断结果为 `true` | `assertTrue(name.length() > 0)` |
| `assertFalse(value)` | 判断结果为 `false` | `assertFalse(name.isBlank())` |
| `assertNull(value)` | 判断结果为 `null` | `assertNull(email)` |
| `assertNotNull(value)` | 判断结果不为 `null` | `assertNotNull(employee)` |
| `assertThrows(type, executable)` | 判断会抛出指定异常 | `assertThrows(IllegalArgumentException.class, () -> {...})` |

断言是测试的核心。

如果测试方法中没有断言，这个测试通常不能有效证明结果是否正确。

## 十三、执行测试

在 Maven 项目根目录执行：

```powershell
mvn test
```

测试通过时，会看到类似结果：

```text
Tests run: 4, Failures: 0, Errors: 0, Skipped: 0
```

含义：

| 项目 | 说明 |
| --- | --- |
| `Tests run` | 执行的测试数量 |
| `Failures` | 断言失败数量 |
| `Errors` | 测试执行中发生异常的数量 |
| `Skipped` | 被跳过的测试数量 |

## 十四、测试关注点

写测试时不要只测试一个正常值。

常见测试关注点：

| 测试类型 | 关注内容 | 示例 |
| --- | --- | --- |
| 正常值测试 | 正常输入是否返回正确结果 | 单价 100，数量 3，返回 300 |
| 边界值测试 | 临界值是否处理正确 | 数量 0，返回 0 |
| 异常值测试 | 非法输入是否抛出异常 | 单价 -100，抛出异常 |
| 空值测试 | `null` 是否被正确处理 | 姓名为 `null`，抛出异常 |
| 多数据测试 | 多组输入是否稳定 | 不同单价和数量都能正确计算 |

测试用例的重点不是数量越多越好，而是要覆盖容易出错的场景。

## 十五、常见错误

| 错误 | 原因 | 修正 |
| --- | --- | --- |
| 只测试正常情况 | 边界和异常未覆盖 | 增加边界值和异常值测试 |
| 测试名叫 `test1` | 看不出测试意图 | 使用表达预期的命名 |
| 测试没有断言 | 没有判断实际结果 | 使用断言验证结果 |
| 预期值和实际值写反 | 阅读时容易误解失败信息 | `assertEquals(expected, actual)` |
| 测试依赖外部状态 | 结果不稳定 | 准备明确测试数据 |
| 一个测试方法测太多内容 | 失败时不容易定位 | 一个测试方法只验证一个主要场景 |

## 十六、本章练习

请完成：

1. 给总价计算方法写正常值测试。
2. 给数量为 0 的情况写边界值测试。
3. 给单价为负数的情况写异常测试。
4. 给数量为负数的情况写异常测试。
5. 给员工姓名校验方法写 `null` 测试。
6. 给员工姓名校验方法写空字符串测试。
7. 执行 `mvn test` 并确认测试通过。

## 十七、本章总结

- JUnit 用于编写和执行 Java 单元测试。
- `@Test` 用于标记测试方法。
- 测试用例通常包含准备数据、执行方法、断言结果。
- 正常值、边界值、异常值和空值都需要考虑。
- `assertEquals()` 用于判断结果相等。
- `assertThrows()` 用于判断是否抛出指定异常。
- Maven 项目中可以使用 `mvn test` 执行测试。
