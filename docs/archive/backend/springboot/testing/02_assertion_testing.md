# Spring Boot 测试断言

> 本文档系统讲解 Spring Boot 测试中三大主流断言方式：  
> **JUnit 原生断言**、**AssertJ 链式断言**、**Hamcrest 匹配器断言**。  
> 适用于企业培训、课堂教学或项目实战。

---

## 一、断言的作用与分类

在单元测试中，断言（Assertion）用于判断**程序的实际结果**是否符合**预期结果**。
常见断言库分为三类：

| 框架 | 特点 | 场景 |
|------|------|------|
| JUnit Assertions | 基础断言（最原始） | 适合快速测试逻辑 |
| AssertJ | 链式断言，语义自然 | 推荐 Service 层测试 |
| Hamcrest | 匹配器式断言 | 常用于 Controller + MockMvc 测试 |

---

## 二、JUnit 原生断言

### 基本语法

```java
import static org.junit.jupiter.api.Assertions.*;

@Test
void testBasicAssertions() {
    int result = 5 + 2;
    assertEquals(7, result);
    assertNotEquals(8, result);
    assertTrue(result > 0);
    assertFalse(result < 0);
    assertNotNull(result);
}
```

### 常见断言方法

| 方法 | 说明 | 示例 |
|------|------|------|
| `assertEquals(expected, actual)` | 判断相等 | `assertEquals(5, calc.add(2,3));` |
| `assertNotEquals(expected, actual)` | 判断不相等 | `assertNotEquals(0, result);` |
| `assertTrue(condition)` | 判断条件为真 | `assertTrue(list.size() > 0);` |
| `assertFalse(condition)` | 判断条件为假 | `assertFalse(user.isDeleted());` |
| `assertNull(obj)` | 判断为空 | `assertNull(result);` |
| `assertNotNull(obj)` | 判断非空 | `assertNotNull(user);` |
| `assertSame(a, b)` | 判断引用相同 | `assertSame(obj1, obj2);` |
| `assertNotSame(a, b)` | 判断引用不同 | `assertNotSame(new A(), new A());` |

---

### 异常断言

```java
assertThrows(IllegalArgumentException.class, () -> {
    userService.findById(null);
});
```

---

### 超时断言

```java
assertTimeout(Duration.ofSeconds(2), () -> {
    Thread.sleep(1000);
});
```

---

### 分组断言（assertAll）

> 一次验证多个条件，即使前面断言失败，也会继续执行后续断言。

```java
assertAll(
    () -> assertEquals("Tom", user.getName()),
    () -> assertTrue(user.getAge() > 18),
    () -> assertNotNull(user.getEmail())
);
```

---

### 失败断言

```java
fail("逻辑不应执行到此处");
```

---

- **JUnit Assertions 是所有断言的基础**，几乎所有测试框架都兼容。  
- 在企业项目中，通常结合 **AssertJ** 或 **Hamcrest** 提升可读性。

---

## 三、AssertJ

> AssertJ 是目前最流行的断言库，语义自然、链式调用、类型安全。

### 基本语法

```java
import static org.assertj.core.api.Assertions.assertThat;

assertThat(actualValue)
  .isNotNull()
  .isEqualTo(expectedValue)
  .isInstanceOf(String.class);
```

---

### 对象与基本类型断言

```java
assertThat(5).isPositive().isLessThan(10);
assertThat(user.getName()).isEqualTo("Tom").isNotBlank();
```

---

### 字符串断言

```java
assertThat("SpringBoot")
  .isNotBlank()
  .startsWith("Spring")
  .endsWith("Boot")
  .contains("ing")
  .containsIgnoringCase("boot");
```

---

### 集合断言

```java
List<String> names = List.of("Tom", "Jane", "Alice");

assertThat(names)
  .isNotEmpty()
  .hasSize(3)
  .contains("Tom")
  .doesNotContain("Bob")
  .containsExactly("Tom", "Jane", "Alice")
  .doesNotHaveDuplicates();
```

---

### Map 断言

```java
Map<String, Integer> map = Map.of("A", 1, "B", 2);

assertThat(map)
  .containsKey("A")
  .containsEntry("B", 2)
  .doesNotContainKey("C");
```

---

### 异常断言

```java
assertThatThrownBy(() -> service.call(null))
  .isInstanceOf(IllegalArgumentException.class)
  .hasMessageContaining("参数不能为空");
```

---

### 日期断言

```java
LocalDate today = LocalDate.now();
assertThat(today.plusDays(1)).isAfter(today);
```

---

### AssertJ vs JUnit 对比

| 特性 | JUnit | AssertJ |
|------|--------|----------|
| 风格 | 命令式 | 链式（流式） |
| 可读性 | 较弱 | 强 |
| 错误信息 | 简单 | 自动提示更丰富 |
| 推荐场景 | 基础逻辑 | 大多数单元测试 |

---

## 四、Hamcrest

> Hamcrest 提供“匹配器风格”的断言方式，语法偏函数式。  
> 常用于验证 Web 层返回结果、JSON 内容或复杂条件匹配,MockMvc 专用

---

### 基本语法

```java
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

assertThat(5, is(5));
assertThat(5, allOf(greaterThan(3), lessThan(10)));
assertThat("SpringBoot", containsString("Boot"));
```

---

### 常用匹配器

| 匹配器 | 功能 | 示例 |
|---------|------|------|
| `is(x)` | 等于 | `assertThat(5, is(5));` |
| `equalTo(x)` | 等于 | `assertThat(name, equalTo("Tom"));` |
| `not(x)` | 不等于 | `assertThat(flag, not(true));` |
| `nullValue()` / `notNullValue()` | 空 / 非空 | `assertThat(obj, notNullValue());` |
| `greaterThan(x)` / `lessThan(x)` | 大于 / 小于 | `assertThat(num, greaterThan(0));` |
| `containsString(x)` | 包含字符串 | `assertThat(str, containsString("Spring"));` |
| `startsWith(x)` / `endsWith(x)` | 字符串开头 / 结尾 | `assertThat(str, startsWith("A"));` |
| `hasSize(n)` | 集合长度 | `assertThat(list, hasSize(3));` |
| `hasItem(x)` / `hasItems(...)` | 集合包含元素 | `assertThat(list, hasItem("Tom"));` |
| `anyOf(...)` / `allOf(...)` | 逻辑匹配（或/且） | `assertThat(num, anyOf(is(1), is(2)));` |

---

### MockMvc + Hamcrest 示例

```java
mockMvc.perform(get("/users/1"))
  .andExpect(status().isOk())
  .andExpect(jsonPath("$.name", is("Tom")))
  .andExpect(jsonPath("$.age", greaterThan(18)))
  .andExpect(jsonPath("$.roles", hasItem("ADMIN")));
```

---

### 三者对比总结

| 框架 | 风格 | 适用层 | 优点 |
|------|------|--------|------|
| **JUnit** | 命令式 | 基础层 | 简单直观 |
| **AssertJ** | 链式 | Service / DAO 层 | 可读性高 |
| **Hamcrest** | 匹配器式 | Controller 层 | MockMvc 原生支持 |

---

📘 **最佳实践建议：**

- 项目中推荐默认使用 **AssertJ**。  
- 若测试 Web 接口（MockMvc、JSON）则搭配 **Hamcrest**。  
- **JUnit** 断言适合快速编写简单测试用例。

---
