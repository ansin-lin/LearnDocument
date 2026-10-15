# A12-1 常用测试断言库与断言方法

本附录配合[第12章：员工CRUD的分层测试与接口验证](../mainline/12_automated_testing_and_regression.md)按需查阅：完成MySQL/H2 Mapper案例时看值与异常，完成Service案例时看业务结果与Mock交互，完成Controller案例时看HTTP匹配器；通读全部方法不是开始写测试的前置条件。目标是根据“规格要求证明什么”选择足够精确的检查，并读懂失败信息。下面的`actual`、`employee`、`names`等变量代表测试中已取得的结果；代码是用法片段，不是可直接运行的完整测试类。

第12章工程的 `spring-boot-starter-test` 已用于 JUnit Jupiter、AssertJ、Mockito 与 Spring Test。既存项目若使用 Hamcrest，先检查 `pom.xml` 中的实际依赖版本；不要仅凭示例就盲目增加依赖。以下导入分别来自不同库，同名 `assertThat` 不要在同一个类中同时静态导入。

```java
import static org.junit.jupiter.api.Assertions.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
// Hamcrest示例单独使用：import static org.hamcrest.MatcherAssert.assertThat;
// Hamcrest示例单独使用：import static org.hamcrest.Matchers.*;
```

第12章先分别运行测试文件，最后才学习完整Spring Boot集成测试。本附录按检查对象提供查阅内容，不改变主线执行顺序：

| 第12章阶段 | 主要检查工具 | 本附录查阅位置 |
| --- | --- | --- |
| MySQL和H2的Mapper层切片/数据库测试 | AssertJ检查查询结果、行数、未写入状态和约束异常 | 第二、三节 |
| Service单元测试 | AssertJ检查业务结果和异常，Mockito验证必要的依赖交互 | 第二、三、六节 |
| Controller层MockMvc测试 | MockMvc检查状态、Header和JSON | 第五节 |
| Postman手工接口测试 | 人工核对HTTP结果和数据库，不使用Java断言库 | 第七节 |
| `@SpringBootTest`完整集成测试 | MockMvc、AssertJ和数据库查询组合 | 第三、五、八节 |

## 一、先按检查对象选工具

先写清前置数据、被测操作和预期，再选择工具。一个测试通常需要**结果断言**，有时还需要**状态或交互验证**：例如“重复邮箱不得新增员工”既要检查抛出的异常，也要检查表仍只有原来的两行。下面的库并不是互相排斥的备选整套方案；JUnit负责运行测试，其他工具针对不同结果提供检查方法。

| 要检查的对象 | 首选方式 | 说明 |
| --- | --- | --- |
| 单个值、异常、多个基础条件 | JUnit Jupiter `Assertions` | 测试运行器 JUnit 自带的断言；调用简短 |
| 字符串、集合、对象属性、异常细节 | AssertJ | 从实际值 `assertThat(actual)` 开始链式描述预期 |
| 既存测试中的组合匹配条件 | Hamcrest | `assertThat(actual, matcher)`；先识别项目是否已有该库 |
| Controller 的 HTTP 响应 | Spring Test `MockMvcResultMatchers` | 检查状态、响应头、JSON 等接口契约 |
| Mock 对象有没有被调用 | Mockito `verify` | 这是交互验证，不是返回值或真实 SQL 的断言 |

在本课程的Service测试里，优先使用AssertJ表达结果和异常；Controller测试用MockMvc结果匹配器表达HTTP契约。JUnit Assertions也必须能读懂，Hamcrest主要用于识读采用该风格的既存项目；不要为了展示多个库，在同一测试中对同一个结果无意义地重复断言。

## 二、JUnit Jupiter：基础值和执行结果

这里的 `assertEquals(expected, actual)` **预期在前、实际在后**；可在最后追加失败说明，例如 `assertEquals(1, rows, "应只更新一行")`。下表每个片段展示一个检查，实际值来自被测方法执行结果。

| 方法与片段 | 准确检查的事 |
| --- | --- |
| `assertEquals(1, rows)` | `rows` 与预期值 `1` 相等；比较对象时依赖 `equals`，不是比较内存地址。 |
| `assertNotEquals(0, rows)` | `rows` 不等于 `0`；只能排除一个值，不能证明它一定是 `1`。 |
| `assertSame(expectedEmployee, actualEmployee)` | 两个引用指向**同一个对象实例**；适合缓存返回同一实例等契约。 |
| `assertNotSame(first, second)` | 两个引用不是同一个实例；即使字段值相同也可通过。 |
| `assertNull(missing)` | `missing` 必须是 `null`；适合明确约定“不存在时返回 null”的 API。 |
| `assertNotNull(employee)` | `employee` 不能是 `null`；**不证明**其字段正确。 |
| `assertTrue(valid)` | 布尔表达式必须为 `true`；如 `assertTrue(rows > 0)`，失败时通常不如精确值断言清楚。 |
| `assertFalse(deleted)` | 布尔表达式必须为 `false`；如 `assertFalse(names.isEmpty())`。 |
| `assertArrayEquals(new int[]{1, 2}, actualIds)` | 数组长度及对应位置元素相等；普通 `assertEquals` 不适合比较数组内容。 |
| `assertIterableEquals(List.of("A", "B"), names)` | 两个可迭代对象按迭代顺序逐项相等；顺序变化会失败。 |

执行过程也可以作为检查对象：

```java
EmployeeNotFoundException ex = assertThrows(
    EmployeeNotFoundException.class, () -> service.findById(9999L));
assertEquals("员工不存在：9999", ex.getMessage());

assertDoesNotThrow(() -> service.findById(1001L));

assertAll("员工响应",
    () -> assertEquals(1001L, response.getId()),
    () -> assertEquals("Tanaka", response.getName()));
```

- `assertThrows(类型, () -> 操作)` 要求操作抛出指定类型**或其子类型**，并返回异常对象，以便继续检查消息或字段。只有把操作放进 lambda，JUnit 才能捕获这次执行的异常；若必须严格等于指定类型，用 `assertThrowsExactly`。
- `assertDoesNotThrow(() -> 操作)` 要求不抛异常；它**不证明返回值正确**，需要另加断言。若操作有返回值，可接收 `assertDoesNotThrow(() -> service.findById(1001L))` 的结果再检查。
- `assertAll(标题, 多个断言 lambda)` 会执行组内各项并一起报告失败；适合检查同一个响应对象的多字段，不用于替代彼此无关的多个测试场景。

## 三、AssertJ：从实际值链式检查

AssertJ 的方向与 JUnit 的 `assertEquals` 不同：`assertThat(actual).isEqualTo(expected)`，**实际在前**。链式调用对同一实际值连续增加要求，任何一个要求不满足即失败。

### 1. 值、空值、类型与大小

| 方法与片段 | 准确检查的事 |
| --- | --- |
| `assertThat(rows).isEqualTo(1)` | 值相等；对象通常按 `equals` 比较。 |
| `assertThat(rows).isNotEqualTo(0)` | 值不等于 `0`，但不能据此断定等于 `1`。 |
| `assertThat(employee).isNull()` | 实际值为 `null`。 |
| `assertThat(employee).isNotNull()` | 实际值非 `null`，字段还需另外检查。 |
| `assertThat(valid).isTrue()` | 布尔值为 `true`；`isFalse()` 则要求为 `false`。 |
| `assertThat(employee).isInstanceOf(Employee.class)` | 对象属于该类型或子类型；不检查字段内容。 |
| `assertThat(rows).isGreaterThan(0)` | 数值大于 `0`；`isLessThan(10)` 要求小于 `10`。 |

如果业务契约要求“成功更新恰好一行”，写 `isEqualTo(1)` 比只写 `isGreaterThan(0)` 更精确。

### 2. 字符串、集合、Map 和对象字段

| 方法与片段 | 准确检查的事 |
| --- | --- |
| `assertThat(message).isEmpty()` | 字符串长度为 0；不等同于 `null`，也不等同于仅含空格。 |
| `assertThat(message).isNotEmpty()` | 字符串至少有一个字符；空格也算字符。 |
| `assertThat(message).contains("员工")` | 字符串包含指定**子串**，不要求整句相同。 |
| `assertThat(message).startsWith("错误：")` | 字符串以前缀开头；`endsWith("失败")` 检查后缀。 |
| `assertThat(names).isEmpty()` | 集合没有元素；`isNotEmpty()` 要求至少一个元素。 |
| `assertThat(names).hasSize(2)` | 集合元素个数恰好为 2；不检查元素值。 |
| `assertThat(names).contains("Tanaka")` | 集合中至少有此元素；允许其他元素。这里的 `contains` 是**元素包含**，不同于字符串的子串包含。 |
| `assertThat(names).containsExactly("Tanaka", "Sato")` | 元素、数量和顺序都完全一致。 |
| `assertThat(names).containsExactlyInAnyOrder("Sato", "Tanaka")` | 元素与数量完全一致，但不要求顺序。 |
| `assertThat(countByStatus).containsKey("ACTIVE")` | Map 存在这个键；不证明对应值。 |
| `assertThat(countByStatus).containsEntry("ACTIVE", 2)` | Map 中该键对应的值为 `2`。 |
| `assertThat(employee).extracting(Employee::getName).isEqualTo("Tanaka")` | 从单个对象取 `name` 属性后比较；对象本身不必实现按字段比较的 `equals`。 |
| `assertThat(employees).extracting(Employee::getName).containsExactly("Tanaka", "Sato")` | 从集合中逐项取姓名，并核对完整顺序。 |

当顺序是规格的一部分（例如按入职日期排序）才用 `containsExactly`；顺序没有约定时，用 `containsExactlyInAnyOrder`，避免测试依赖数据库的偶然返回顺序。

### 3. 异常

```java
assertThatThrownBy(() -> service.findById(9999L))
    .isInstanceOf(EmployeeNotFoundException.class)
    .hasMessage("员工不存在：9999");

assertThatCode(() -> service.findById(1001L))
    .doesNotThrowAnyException();
```

- `assertThatThrownBy(() -> 操作)` 要求抛出异常；后续 `isInstanceOf` 检查类型或子类型，`hasMessage` 检查**完整**消息。若只约定消息的一部分，可用 `hasMessageContaining("员工不存在")`，但不能借此漏检已明确的完整文案。
- `assertThatCode(() -> 操作).doesNotThrowAnyException()` 要求操作正常结束；不检查返回值。要检查结果，先取得实际值，再使用 `assertThat(actual)`。

## 四、Hamcrest：阅读既存项目的匹配器

Hamcrest 的写法是 `org.hamcrest.MatcherAssert.assertThat(actual, matcher)`。第二个参数是“匹配规则”，如 `equalTo(1)`；和 AssertJ 的同名 `assertThat` 不是一个方法。以下片段假定静态导入 `org.hamcrest.Matchers.*`。

| 方法与片段 | 准确检查的事 |
| --- | --- |
| `assertThat(rows, equalTo(1))` | 实际值与 `1` 相等。 |
| `assertThat(rows, is(1))` | `is(1)` 是易读的等值匹配写法。 |
| `assertThat(rows, not(equalTo(0)))` | 实际值不等于 `0`；仍不能证明恰好等于 `1`。 |
| `assertThat(employee, nullValue())` | 实际值必须为 `null`；`notNullValue()` 要求非 `null`。 |
| `assertThat(message, containsString("员工"))` | 字符串包含该子串，不要求全文相等。 |
| `assertThat(names, hasItem("Tanaka"))` | 可迭代对象至少有一个匹配元素，允许其他元素。 |
| `assertThat(names, hasSize(2))` | 集合大小恰好为 2；不检查内容。 |
| `assertThat(names, contains("Tanaka", "Sato"))` | 列表等可迭代对象的内容与顺序完全一致。 |
| `assertThat(names, containsInAnyOrder("Sato", "Tanaka"))` | 内容和数量完全一致，顺序不限。 |
| `assertThat(rows, allOf(greaterThan(0), lessThan(10)))` | 两个规则都要成立，即 `0 < rows < 10`。 |
| `assertThat(status, anyOf(equalTo("NEW"), equalTo("ACTIVE")))` | 至少一个规则成立。 |

`hasItem` 是“包含至少一个”；`contains` / `containsInAnyOrder` 是“完整内容一致”。名称接近，检查范围不同。

## 五、MockMvc：HTTP 响应断言

MockMvc 通过 `andExpect(...)` 接收 Spring Test 的结果匹配器。这些断言验证 Controller 层的请求处理和响应，不自动证明真实浏览器、外部服务或数据库已通过。以下片段省略 `mockMvc.perform(...)` 的准备代码，假定请求已发送。

| 方法与片段 | 准确检查的事 |
| --- | --- |
| `.andExpect(status().isOk())` | HTTP 状态码为 200；`isCreated()` 为 201，`isBadRequest()` 为 400，`isNotFound()` 为 404。 |
| `.andExpect(status().is(422))` | 状态码精确等于 422；用于没有专门快捷方法或项目自定义的状态。 |
| `.andExpect(header().exists("Location"))` | 响应中存在该 Header；不检查值。 |
| `.andExpect(header().string("Location", "/employees/1001"))` | 该响应 Header 的字符串值恰好相等。 |
| `.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))` | 响应媒体类型与 JSON 兼容；可接受 `application/json;charset=UTF-8` 等带参数形式。 |
| `.andExpect(content().string("done"))` | 响应体全文等于指定字符串。 |
| `.andExpect(content().json("{\"code\":\"OK\"}"))` | 响应 JSON 包含示例中要求的字段/值；默认比较并非严格禁止额外字段。 |
| `.andExpect(jsonPath("$.data.id").value(1001))` | JSON 路径对应的值为 `1001`。 |
| `.andExpect(jsonPath("$.data.id").exists())` | JSON 路径有值；若规格要求具体 ID，还需 `.value(...)`。 |
| `.andExpect(jsonPath("$.data.password").doesNotExist())` | JSON 路径不存在或值为 null；如需严格要求字段不存在，使用 `doesNotHaveJsonPath()`。 |

例如创建员工接口需要同时检查状态和返回 ID，而不能只看 200：

```java
mockMvc.perform(post("/employees")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"name\":\"Tanaka\","
                + "\"department\":\"Sales\","
                + "\"email\":\"tanaka.new@example.com\"}"))
    .andExpect(status().isCreated())
    .andExpect(header().string("Location", "/employees/1001"))
    .andExpect(jsonPath("$.data.id").value(1001));
```

这里的 `post` 是请求构造，`status`、`header`、`jsonPath` 才是响应断言。上方是**方法用法片段**：只有测试上下文中已提供MockMvc、Controller所需的Mock Service，且该Service预设返回id为1001的新增结果时，Location和id断言才成立；不能把片段单独粘贴后声称可以运行。实际项目还应按接口规格检查错误码、消息、必要Header和数据库结果；使用Mock Service的Controller测试不能证明SQL执行。

## 六、Mockito 的边界：验证调用，不代替结果断言

```java
verify(employeeMapper).findById(1001L);       // 调用过一次
verify(employeeMapper, times(2)).findById(1001L); // 恰好两次
verify(employeeMapper, never()).deleteById(1001L); // 从未这样调用
verifyNoInteractions(employeeMapper);          // 与该Mock没有交互
```

`verify` 检查被测对象与 Mock 的交互；它不判断返回内容是否正确，也不证明真实 Mapper SQL、事务或数据库状态。测试 Service 时通常同时检查返回值/异常和必要交互；测试真实持久化行为时需要相应集成测试。

## 七、Postman检查与自动化断言的边界

Postman主线案例由学员手工确认HTTP方法、URL、参数、请求体、状态码、Headers、JSON和数据库结果。它不会因为“看起来正确”就自动留下Java断言，也不能替代JUnit回归。证据中应写明输入、预期、实际结果和数据库确认；后续重复频繁、规则稳定的场景再转换为MockMvc或完整集成测试。

## 八、写断言时的检查顺序

1. 先写明规格中的预期：精确值、至少包含、完整集合、异常，还是 HTTP 契约。
2. 用与预期强度相符的方法：完整顺序选 `containsExactly`，不关心顺序选 `containsExactlyInAnyOrder`，只要一个元素才选 `contains` / `hasItem`。
3. 失败时确认看到的是实际值、预期值还是异常堆栈；不要只因测试变绿就削弱断言。
4. 对数据库、事务或安全规则，补充对应层级的测试和证据，不把 Mock 交互验证当作端到端证明。

| 测试规格 | 不足以证明规格的写法 | 更合适的检查 |
| --- | --- | --- |
| Mapper新增一行且能读取邮箱 | 只写`assertThat(rows).isGreaterThan(0)` | `isEqualTo(1)`，再按生成id查询并核对邮箱 |
| 重复邮箱不得新增 | 只写`assertThatThrownBy(...).isInstanceOf(...)` | 再查表行数或目标邮箱对应记录，确认数据未增加 |
| Service遇到不存在id应抛业务异常 | 只写`verify(mapper).findById(id)` | 断言异常类型与规定消息，再按需要验证Mapper调用 |
| Controller新增返回正确资源 | 只写`status().isCreated()` | 同时检查Location、`success`、返回id与关键字段 |
| 列表必须按规格排序 | 只写`contains("Tanaka")` | 用`containsExactly(...)`检查完整元素和顺序 |

“正常系”与“异常系”并非对应不同的断言库：同一个`assertThat`可检查成功结果、空结果和数据库状态；`assertThatThrownBy`仅用于实际应该抛Java异常的场景。HTTP 400/404/409是Controller返回的响应，不应在MockMvc测试中误写成“JUnit必须抛异常”。

## 参考资料

- [JUnit Jupiter Assertions API](https://docs.junit.org/current/api/org.junit.jupiter.api/org/junit/jupiter/api/Assertions.html)
- [AssertJ 官方文档](https://assertj.github.io/doc/)
- [Hamcrest Java 教程](https://hamcrest.org/JavaHamcrest/tutorial)
- [Spring Framework MockMvc 文档](https://docs.spring.io/spring-framework/reference/testing/spring-mvc-test-framework.html)
