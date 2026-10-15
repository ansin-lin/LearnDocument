# A12-3 JUnit Jupiter常用注解与方法

学习[第12章分层测试](../mainline/12_automated_testing_and_regression.md)时，可以用本附录查阅测试方法怎样执行、准备与清理何时发生，以及怎样用JUnit断言与Mockito写出独立测试；第13章事务测试也会沿用这些写法。更多AssertJ、Mockito与MockMvc方法见[A12-1](A12_01_testing_assertions_reference.md)。

“JUnit 5写法”通常指从JUnit 5开始使用的Jupiter编程模型，例如 `org.junit.jupiter.api.Test`。本课程Spring Boot 4.1.1的 `spring-boot-starter-test` 由Spring Boot管理JUnit依赖版本，当前已是JUnit 6系列；下面这些Jupiter导入与基础用法仍适用。**不要因为看到旧称呼就把项目依赖强制降为5.x**。若接手既存项目，应以该项目实际依赖树和JUnit文档为准。

## 一、先分清JUnit和Spring测试注解

JUnit负责发现测试方法、控制生命周期并判断断言；Spring测试框架负责建立应用上下文、选择Profile和准备测试数据。例如第13章的事务回滚测试同时使用两类注解：

| 来自哪里 | 本课程中的例子 | 主要职责 |
| --- | --- | --- |
| JUnit Jupiter | `@Test`、`@BeforeAll`、`@TestInstance` | 发现、组织和运行测试方法 |
| JUnit Jupiter Assertions | `assertEquals()`、`assertThrows()` | 比较实际结果与预期 |
| Spring Test / Spring Boot Test | `@Sql`、`@ActiveProfiles`、`@SpringBootTest` | 准备数据库、环境与Spring上下文 |
| Spring事务 | `@Transactional` | 为经过代理的业务方法建立事务边界；它不是JUnit注解 |

`@Test` 本身不会启动Spring Boot或连接数据库。第12章的Service单元测试可以只用JUnit和Mockito；第13章的HTTP与MySQL回滚测试才需要Spring完整集成测试。**不要给回滚测试方法加 `@Transactional`**，否则测试自身的回滚可能掩盖Service事务边界的缺失。

## 二、一个最小的完整JUnit测试类

第12章工程已有 `EmployeeResponse` 和 `spring-boot-starter-test`。新建 `src/test/java/com/example/employee/test/JUnitJupiterExampleTest.java`；此类不启动Spring、不连接数据库，可在Eclipse中单独选择 **Run As → JUnit Test**：

```java
package com.example.employee.test;

import com.example.employee.dto.response.EmployeeResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DisplayName("员工响应对象")
class JUnitJupiterExampleTest {

    private EmployeeResponse response;

    @BeforeEach
    void setUp() {
        response = new EmployeeResponse(
                1001L, "Tanaka", "Sales", "tanaka@example.com");
    }

    @Test
    @DisplayName("构造后保留员工编号和姓名")
    void keepsIdAndName() {
        assertNotNull(response);
        assertAll("员工响应字段",
                () -> assertEquals(1001L, response.getId()),
                () -> assertEquals("Tanaka", response.getName()));
    }
}
```

- `@BeforeEach` 在本类**每个**测试方法之前重新创建对象；测试方法互不依赖执行顺序。
- `@Test` 让JUnit识别并运行 `keepsIdAndName()`；方法名是Java标识符，`@DisplayName` 则指定JUnit视图中更容易阅读的显示名称。
- `assertNotNull(response)` 只检查对象存在；`assertEquals(expected, actual)` 的参数顺序是**预期在前、实际在后**。
- `assertAll(标题, 多个断言)` 执行组内的各项检查，并在同一测试结果中汇总失败；它不代替为不同业务场景分别编写测试方法。

运行后预期1个测试通过。可临时把预期姓名改为 `"Suzuki"`，观察失败视图中的expected与actual，随后恢复原值并重跑。这个文件只是独立练习，不修改第13章事务业务代码；不需要保留时可在练习工程中移除。

## 三、常用生命周期注解：何时准备、何时清理

一个测试类中，常见执行顺序是：

```text
@BeforeAll：整个类开始前一次
  → @BeforeEach → @Test方法A → @AfterEach
  → @BeforeEach → @Test方法B → @AfterEach
@AfterAll：整个类结束后一次
```

| 注解 | 放在哪里 | 何时运行 | 常见用途与边界 |
| --- | --- | --- | --- |
| `@Test` | 测试方法 | 每个测试方法各运行一次 | 验证一个明确场景；不要依赖另一个测试方法先执行 |
| `@BeforeEach` | 无返回值的生命周期方法 | 每个测试方法之前 | 重建Mock、Service对象或当前测试所需的独立数据 |
| `@AfterEach` | 无返回值的生命周期方法 | 每个测试方法之后 | 关闭本方法打开的资源或清理临时状态；没有资源就不必机械添加 |
| `@BeforeAll` | 通常为 `static` 方法 | 整个测试类的测试方法之前一次 | 检查共享环境或准备昂贵且只需一次的只读资源 |
| `@AfterAll` | 通常为 `static` 方法 | 整个测试类的测试方法之后一次 | 释放由 `@BeforeAll` 建立的类级资源 |
| `@TestInstance(PER_CLASS)` | 测试类 | JUnit建立测试实例时 | 一个类共用一个实例，可让 `@BeforeAll`、`@AfterAll` 使用非静态方法；须防止测试共享可变状态 |

以下是**生命周期位置片段**，不要求加入主线工程：

```java
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GuardExampleTest {
    @BeforeAll
    void checkTestEnvironmentOnce() {
        // 只检查测试环境；第12章MySqlTestGuard在此处检查数据库目标。
    }

    @AfterEach
    void closePerTestResource() {
        // 仅当本测试确实打开了需要关闭的资源时，才在这里清理。
    }

    @AfterAll
    void finishOnce() {
        // 仅当类级资源需要释放时使用。
    }
}
```

这段片段省略了 `org.junit.jupiter.api` 各注解及 `TestInstance` 的import，只展示位置，不是完整可运行测试类。第12章的 `MySqlTestGuard` 是有真实检查逻辑的例子：继承它的测试类在执行含删表操作的脚本前，先确认连接的是专用MySQL测试库。`@BeforeAll` 不能代替每个测试方法自己的数据准备；测试之间仍要相互独立。

## 四、用JUnit Assertions表达常见预期

下例是**测试方法内的用法片段**，假定已静态导入 `org.junit.jupiter.api.Assertions` 中对应方法；`rows`、`missing` 等变量来自实际被测操作，不是JUnit自动创建的对象：

```java
assertEquals(1, rows);                       // 恰好影响1行
assertNotEquals(0, rows);                    // 只排除0，不证明恰好为1
assertTrue(rows > 0);                        // 条件必须为true
assertFalse(names.isEmpty());                // 集合不应为空
assertNull(missing);                         // 结果必须为null
assertNotNull(employee);                     // 对象存在；字段仍需另查

EmployeeNotFoundException exception = assertThrows(
        EmployeeNotFoundException.class,
        () -> service.findById(9999L));
assertEquals("员工不存在：9999", exception.getMessage());

EmployeeResponse actual = assertDoesNotThrow(
        () -> service.findById(1001L));
assertEquals(1001L, actual.getId());
```

- `assertEquals(expected, actual)` 精确比较预期与实际；对象按其 `equals()` 语义比较，不是自动逐字段比较。
- `assertNotEquals()`、`assertTrue()`、`assertFalse()` 只能证明给定条件；规格要求“恰好一行”时，优先写 `assertEquals(1, rows)`。
- `assertNull()` 与 `assertNotNull()` 只判断有无对象；要证明字段正确还要补充字段断言。
- `assertThrows(异常类型, () -> 操作)` 要求lambda中的操作抛出指定类型或其子类型，并返回异常对象；要严格要求完全同一异常类型可用 `assertThrowsExactly()`。
- `assertDoesNotThrow(() -> 操作)` 只要求正常结束；即使取得返回值，也要另写断言检查内容。

第12章主线主要用AssertJ的 `assertThat()` 和 `assertThatThrownBy()` 表达这些规格；JUnit自带断言是另一种常见写法，不要求在同一测试里重复检查同一结果。更多值、集合、异常和HTTP断言方法在[A12-1](A12_01_testing_assertions_reference.md)。

## 五、组织多个场景的常用注解

### 1. 给测试命名、分组和暂时停用

下面是**放在测试类或方法上的片段**：

```java
@Tag("transaction")
class TransactionTest {
    @Test
    @DisplayName("履历写入失败时员工修改也回滚")
    void updateRollsBackWhenLogInsertFails() {
        // 第13章有完整的MySQL集成测试；此处只展示注解位置。
    }

    @Disabled("等待测试环境修复；修复后必须恢复执行")
    @Test
    void temporarilyUnavailableTest() {
    }
}
```

- `@DisplayName("...")` 只改变JUnit报告中的名称，不影响Java方法名、运行顺序或断言结果。
- `@Tag("transaction")` 为测试类或方法打标签，供IDE或构建工具按标签筛选；**不会自动运行或跳过**带标签的测试。
- `@Disabled("原因")` 使目标测试跳过。跳过不是通过；不能用它掩盖失败的事务测试，修复原因后应移除注解并重跑。

### 2. 同一规则用多组输入验证

以下是**可独立运行的完整JUnit参数化测试类**，不依赖Spring或数据库。它验证字符串格式处理，只用于说明输入如何逐次传给测试方法：

```java
package com.example.employee.test;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class DepartmentTextTest {

    @ParameterizedTest
    @ValueSource(strings = {"Sales", "Development", "Support"})
    void departmentTextIsNotBlank(String department) {
        assertFalse(department.isBlank());
    }

    @ParameterizedTest
    @CsvSource({"sales,SALES", "support,SUPPORT"})
    void convertsTextToUpperCase(String input, String expected) {
        assertEquals(expected, input.toUpperCase(java.util.Locale.ROOT));
    }
}
```

- `@ParameterizedTest` 每取得一组参数就执行一次测试方法；不要在同一方法上再加 `@Test`。
- `@ValueSource(strings = {...})` 每次提供一个字符串，依次传给单参数方法。`strings` 是当前使用的属性；提供数字时可选择 `ints` 等对应属性。
- `@CsvSource({...})` 每行提供一组以逗号分隔的参数，按顺序传给 `input` 与 `expected`。包含逗号或特殊空白的复杂数据要按JUnit CSV规则处理，不宜直接套用这个最小示例。
- 本例共运行5次：第一种方法3次、第二种方法2次。每次结果应在JUnit视图中单独显示；它们验证的只是字符串处理，不等于第13章的Service或数据库已通过。

`@Nested` 可把同一对象的正常系与异常系放进非静态内部类，适合场景较多的测试；不是为了强制控制测试顺序：

```java
@Nested
@DisplayName("员工修改异常系")
class UpdateFailures {
    @Test
    void missingEmployeeIsRejected() {
        // 在这里准备输入并断言；外层测试类仍需提供被测对象。
    }
}
```

上方 `@Nested` 是**测试类内部片段**，应放入现有测试类，不是另建顶层类。它只组织报告层级；每个测试仍须独立准备前置条件。

### 3. 给JUnit测试接入扩展

`@ExtendWith(扩展类.class)` 把扩展注册到JUnit。第12章主线在`@BeforeEach`中显式调用`mock(EmployeeMapper.class)`，再通过构造器创建`EmployeeServiceImpl`，便于看清依赖关系。另一种可选写法是注册Mockito扩展，由它初始化`@Mock`字段，并按构造器依赖为`@InjectMocks`标记的被测对象提供Mock。后两个是Mockito注解，不是JUnit注解：

```java
@ExtendWith(MockitoExtension.class)
class EmployeeServiceImplTest {
    @Mock
    private EmployeeMapper employeeMapper;

    @InjectMocks
    private EmployeeServiceImpl employeeService;

    @Test
    void missingEmployeeThrowsException() {
        // 在此准备Mapper返回值，再断言Service行为。
    }
}
```

这是**注解关系片段**，省略了import和Service构造器所需的其他Mock字段；下一节提供可单独运行的完整示例。若被测类构造器有多个依赖，就要为需要替换的依赖分别准备对应类型的Mock，不能只声明一个Mapper字段便假定其余依赖自动可用。`@InjectMocks` 是Mockito组装普通Java对象的功能，**不是Spring依赖注入**。`@ExtendWith` 的 `value` 属性接收一个或多个扩展类；MockitoExtension不会启动Spring Boot或建立应用上下文。`@SpringBootTest` 已通过Spring Boot测试机制接入Spring测试扩展，不要为了“确保注入生效”机械重复标注 `@ExtendWith(SpringExtension.class)`。

## 六、从空白写出一个JUnit＋Mockito测试类

在第12章工程已有的 `spring-boot-starter-test` 依赖下，新建 `src/test/java/com/example/employee/test/EmployeeLookupTest.java`。这是**独立练习文件**：把接口和被测类写成测试类中的嵌套类型，不修改主线的 `EmployeeServiceImpl`，也不启动Spring或访问数据库。

```java
package com.example.employee.test;

import java.util.NoSuchElementException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmployeeLookupTest {

    @Mock
    private EmployeeNameRepository repository;

    @InjectMocks
    private EmployeeLookupService service;

    @Test
    @DisplayName("能根据编号取得员工姓名")
    void findsEmployeeName() {
        when(repository.findNameById(1001L)).thenReturn("Tanaka");

        String actual = service.findName(1001L);

        assertEquals("Tanaka", actual);
        verify(repository).findNameById(1001L);
    }

    @Test
    @DisplayName("找不到员工时抛出异常")
    void rejectsMissingEmployee() {
        when(repository.findNameById(9999L)).thenReturn(null);

        NoSuchElementException exception = assertThrows(
                NoSuchElementException.class,
                () -> service.findName(9999L));

        assertEquals("员工不存在", exception.getMessage());
        verify(repository).findNameById(9999L);
    }

    @Test
    @DisplayName("非法编号在访问依赖前被拒绝")
    void rejectsInvalidIdBeforeCallingRepository() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.findName(0L));

        assertEquals("id必须大于0", exception.getMessage());
        verifyNoInteractions(repository);
    }

    interface EmployeeNameRepository {
        String findNameById(long id);
    }

    static class EmployeeLookupService {
        private final EmployeeNameRepository repository;

        EmployeeLookupService(EmployeeNameRepository repository) {
            this.repository = repository;
        }

        String findName(long id) {
            if (id <= 0) {
                throw new IllegalArgumentException("id必须大于0");
            }
            String name = repository.findNameById(id);
            if (name == null) {
                throw new NoSuchElementException("员工不存在");
            }
            return name;
        }
    }
}
```

按代码从上往下看，自己写测试时也按这个顺序决定要使用的工具：

- **先让测试能被执行**：`@ExtendWith(MockitoExtension.class)` 在测试类上注册Mockito的JUnit扩展。`@Test` 标记每个要运行的方法；`@DisplayName` 只改变测试报告中的名称。Eclipse中右键此文件选择 **Run As → JUnit Test**，应看到3个测试通过。
- **再准备被测对象与替身**：`@Mock` 为接口创建替身，不会连接数据库；`@InjectMocks` 创建 `EmployeeLookupService`，把同类型Mock交给其构造器。这里展示注解方式；第12章主线的显式 `mock()` 加构造器方式也能完成同一目标。两个方式选一种即可。
- **需要指定替身返回值时使用** `when(repository.findNameById(1001L)).thenReturn("Tanaka")`：`when(...)` 选定调用及参数，`thenReturn(...)` 指定该调用的返回值。没有设定的对象返回值通常为 `null`；示例仍明确写出 `thenReturn(null)`，让预期输入一眼可见。`when()` 必须在调用被测方法之前。
- **执行被测方法并检查结果**：`service.findName(1001L)` 是真正要测试的调用，不是Mock；`assertEquals(预期, 实际)` 比较返回值。期望抛异常时，把调用放进 `assertThrows(异常类.class, () -> 调用)` 的lambda中；它返回捕获到的异常，因而可以再检查消息。不要先调用方法、再把调用结果交给 `assertThrows()`。
- **必要时检查依赖交互**：`verify(repository).findNameById(1001L)` 要求这个调用发生一次；`verifyNoInteractions(repository)` 要求整个方法执行期间没有调用这个Mock。这类验证说明调用路径，不能代替对返回值或异常的断言。

这里三组输入分别产生返回值、缺失异常和参数异常，只是让你在同一测试类中练习不同方法的写法。把这些方法迁移到实际Service测试时，先确认业务规格，再决定要断言哪个结果、是否需要验证Mapper调用；不要机械复制示例中的异常类型或消息。

## 七、自己改写测试并检查代码

先单独运行第六节文件，确认3个测试都通过。然后在同一文件中完成以下改写；每改一处就重跑JUnit，观察执行数与失败信息：

1. **练习 `@ParameterizedTest` 与 `@ValueSource`**：把 `rejectsInvalidIdBeforeCallingRepository()` 改为下面的方法。`longs` 中每个值会分别作为参数 `id` 执行一次；此时删除原方法上的 `@Test`，并导入 `org.junit.jupiter.params.ParameterizedTest` 和 `org.junit.jupiter.params.provider.ValueSource`。总执行次数应从3次变为4次。

   ```java
   @ParameterizedTest
   @ValueSource(longs = {0L, -1L})
   void rejectsInvalidIdsBeforeCallingRepository(long id) {
       IllegalArgumentException exception = assertThrows(
               IllegalArgumentException.class,
               () -> service.findName(id));
       assertEquals("id必须大于0", exception.getMessage());
       verifyNoInteractions(repository);
   }
   ```

2. **练习断言的准确性**：暂时把 `assertEquals("Tanaka", actual)` 改成 `assertEquals("Suzuki", actual)`，从JUnit失败视图读出expected与actual，然后恢复。`verify(repository)` 即使通过，也不会使错误的返回值断言通过。
3. **练习异常断言**：在 `rejectsMissingEmployee()` 中暂时把接收变量声明改为 `RuntimeException exception`，再把 `assertThrows()` 的期望类型改成 `IllegalArgumentException.class`。这样代码仍能编译，但JUnit会报告实际抛出的是 `NoSuchElementException`；观察后把两处都恢复。只有把被测调用放在lambda里，`assertThrows()` 才能检查这次执行。
4. **练习Mock调用验证**：暂时把非法编号方法中的 `verifyNoInteractions(repository)` 改为 `verify(repository).findNameById(id)`，观察验证失败，再恢复。参数在Service入口就被拒绝时，不应假装Mapper已被调用。

完成后检查自己的测试代码，而不是只看“绿色”：

| 写代码的阶段 | 要检查的注解或方法 | 常见误用与修正 |
| --- | --- | --- |
| 发现测试 | `@Test`、`@ParameterizedTest`、`@Disabled` | 方法没有运行时先看是否缺测试注解或被跳过；参数化方法不要再叠加 `@Test` |
| 创建替身 | `@ExtendWith`、`@Mock`、`@InjectMocks` | `service` 为 `null` 时检查扩展是否注册、被测类能否由Mock构造；Mock不等于真实Mapper |
| 指定行为 | `when(...).thenReturn(...)` | 先设定返回值再调用Service；参数必须与实际调用相符 |
| 检查结果 | `assertEquals()`、`assertThrows()` | `assertEquals` 预期在前；异常操作要放进lambda，必要时继续检查异常消息 |
| 检查交互 | `verify()`、`verifyNoInteractions()` | 交互验证只能证明调用情况，不能替代结果断言；不要验证业务本来不应发生的调用 |
| 隔离测试 | `@BeforeEach`、默认测试实例生命周期 | 单独运行通过、整类失败时，检查是否共享可变状态或依赖其他方法先运行 |

第13章的数据库事务测试仍沿用JUnit注解与断言，但 `@SpringBootTest`、`@ActiveProfiles`、`@Sql` 属于Spring测试机制，不能当作JUnit生命周期注解；也不能给回滚验证的测试方法加 `@Transactional`，否则测试自身回滚可能掩盖Service事务问题。

官方参考：[JUnit Jupiter注解](https://docs.junit.org/6.0.0/writing-tests/annotations)、[Spring Boot测试依赖说明](https://docs.spring.io/spring-boot/reference/testing/index.html)。
