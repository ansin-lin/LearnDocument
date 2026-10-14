# 第12章 员工CRUD的分层测试与接口验证

> 本章目标：分别掌握Mapper层测试、Service单体测试、Controller层MockMvc测试和Postman手工接口测试，最后使用`@SpringBootTest`验证完整调用链，并根据改修范围选择回归测试。

手工验证适合初次观察接口和数据库，但每次改修后都重新输入请求、查表并整理结果，容易漏掉旧功能。自动化测试把“前置条件、输入、操作、预期”保存成代码，修改后可再次执行。

```text
已确认的手工结果
  → 整理测试条件和预期
  → 分别验证Mapper、Service和Controller
  → 用Postman观察真实HTTP接口
  → 最后验证完整Spring Boot调用链
  → 自动执行和断言
  → 保存失败信息与测试证据
```

## 一、先认识测试阶段与证据

测试不是“运行一次没报错”，而是给定前置数据和输入，比较实际结果与预期结果，并留下可复核证据。日本项目常称単体テスト（单体测试）、結合テスト（结合测试）、システムテスト（系统测试）；名称对应项目阶段，但不与某个框架注解绝对一一对应。单体测试尽量缩小被测范围，结合测试验证组件协作，系统测试从系统对外行为和业务流程验收。回归测试是在改修后重复相关旧用例，确认原功能未被破坏。

同一规格至少思考正常系（合法输入成功）、异常系（如员工不存在/邮箱重复）和边界值（姓名50字符允许、51字符拒绝、空值）。测试人员先写清测试编号、前置条件、输入、预期状态/数据库状态，再选择自动化或Postman手工验证；不能先选工具再倒推要证明什么。Eclipse中的JUnit结果、Postman响应、数据库查询和测试规格书各是不同证据。

## 二、从手工验收得到测试规格

第10章已经规定CRUD的HTTP结果和数据库结果。先把它们改写成可判定的测试条件：

| 测试场景 | 前置条件 | 输入与操作 | 必须断言的结果 |
| --- | --- | --- | --- |
| 详情查询成功 | 员工1001存在 | GET `/employees/1001` | 200，`data.id=1001`，姓名与部门正确 |
| 详情查询失败 | 员工9999不存在 | GET `/employees/9999` | 404，`success=false` |
| 新增成功 | 邮箱未被使用 | POST合法JSON | 201，Location和生成id一致，数据库有记录 |
| 字段校验失败 | 数据库已重置 | POST空姓名和非法邮箱 | 400，包含字段错误，不增加记录 |
| 邮箱冲突 | `tanaka@example.com` 已存在 | POST重复邮箱 | 409，不增加记录 |
| 完整CRUD | 固定初始数据 | 新增→查询→修改→删除 | 各阶段HTTP与表状态一致，删除后再查返404 |

这张表是测试规格，不是“调用过方法就算通过”。每个测试方法都必须有可判断的预期结果。

## 三、测试路线与三层边界

接口调用链是 `Postman/HTTP → Controller → Service → Mapper → 数据库`。分层测试只观察链中的一段：Mapper证明SQL、参数绑定、结果映射及约束；Service证明业务规则、Entity/DTO转换、流程与异常转换；Controller证明路径、HTTP方法、请求绑定、校验、状态、Header和JSON；完整集成测试再证明它们在测试环境中能协作。四类测试互补，不能用“Mock调用过Mapper”证明SQL正确。

| 类型 | 本章加载的范围 | 用什么替代外部依赖 | 主要证明 |
| --- | --- | --- | --- |
| Mapper切片测试 | MyBatis Mapper、XML、专用MySQL测试库 | 不Mock Mapper，执行真实SQL | 参数、映射、写入和数据库约束 |
| Service单元测试 | `EmployeeServiceImpl` 普通Java对象 | Mockito创建的Mapper模拟对象 | 业务分支、转换和Mapper调用是否正确 |
| Web切片测试 | Spring MVC、Controller、校验和异常处理 | `@MockitoBean` 提供的Service | URL、HTTP方法、JSON转换、状态、响应头和响应字段 |
| Postman手工接口测试 | 已在Eclipse启动的真实应用 | 不替换业务组件 | 从HTTP客户端观察方法、URL、状态、Header、JSON和数据库结果 |
| Spring Boot完整集成测试 | 完整Spring应用、Mapper XML和专用MySQL测试库 | 与开发库隔离的MySQL schema | Controller→Service→Mapper→数据库→响应的整条链路 |

单元测试失败时范围小、定位快；集成测试能发现组件连接问题，但启动和准备更多。不要只写集成测试，也不要用全部Mock声称SQL已经通过。


本章按JUnit与断言基础、Mapper、Service、Controller、Postman手工接口、`@SpringBootTest`完整集成、回归与证据依次推进。每个自动化测试类都在Eclipse中直接选择 **Run As → JUnit Test**；不先运行默认的ApplicationTests，也不要求先启动Spring Boot。只有Postman案例需要从Eclipse运行启动类。Mock只存在于测试上下文，不修改正式运行环境；本章尚未引入Spring Security，不增加认证配置。

## 四、案例一：JUnit与断言从什么开始

JUnit Jupiter负责发现并执行 `@Test` 方法、报告通过或失败；`@BeforeEach` 每个测试前运行，`@AfterEach` 每个测试后运行，适合重建/清理独立资源。一个测试方法的前置准备、操作和断言应可独立运行，不依赖其他方法的执行顺序。Eclipse右键测试类选择 **Run As → JUnit Test**，绿色表示当前断言满足，红色可查看失败值和堆栈。

断言是用程序比较“实际”与“预期”。JUnit自身提供 `org.junit.jupiter.api.Assertions.assertEquals(1001L, id)`；AssertJ提供流式 `org.assertj.core.api.Assertions.assertThat(id).isEqualTo(1001L)`。AssertJ是断言库，不能替代负责运行测试的JUnit，两者可在一类中并用。下面按数据形态选择，不要只记方法名：

| 要证明的事 | JUnit Assertions示例 | AssertJ示例 | 失败时观察 |
| --- | --- | --- | --- |
| 数值相等/不等 | `assertEquals(1, rows)`、`assertNotEquals(0, rows)` | `assertThat(rows).isEqualTo(1)`、`.isNotEqualTo(0)` | expected/actual不同 |
| null状态 | `assertNull(missing)`、`assertNotNull(found)` | `assertThat(missing).isNull()`、`assertThat(found).isNotNull()` | 是否找到对象不符 |
| 布尔条件 | `assertTrue(valid)`、`assertFalse(invalid)` | `assertThat(valid).isTrue()`、`assertThat(invalid).isFalse()` | 条件与预期相反 |
| 集合大小、内容及顺序 | `assertEquals(2, list.size())` | `assertThat(list).hasSize(2).contains(a).containsExactly(a, b)` | 少项、多项或顺序错误 |
| 空字符串/集合 | `assertTrue(list.isEmpty())` | `assertThat(list).isEmpty()` | 实际仍有元素 |
| 对象属性/类型 | `assertEquals("Tanaka", employee.getName())` | `assertThat(employee.getName()).isEqualTo("Tanaka")`、`assertThat(employee).isInstanceOf(Employee.class)` | 映射字段或类型不符 |
| 异常类型/消息 | `assertThrows(EmployeeNotFoundException.class, () -> service.findById(9999L))` | `assertThatThrownBy(() -> service.findById(9999L)).isInstanceOf(EmployeeNotFoundException.class).hasMessage("员工不存在：9999")` | 未抛、类型或消息不符 |
| 不抛异常/多项一起检查 | `assertDoesNotThrow(() -> service.findById(1001L))`、`assertAll(() -> assertEquals(1, rows), () -> assertNotNull(found))` | 分别写流式断言 | 任一检查失败 |

字符串也可用 `assertThat(text).contains("员工")`，但只有完整值是接口契约时应优先核对完整消息。`containsExactly` 是集合的完整顺序检查，不应用在单个字符串。Mockito的 `verify(mapper).findById(1001L)` 检查Mock交互，不是结果断言，更不能证明SQL执行。HTTP状态、Header、JSON用后面的MockMvc `status()`、`header()`、`jsonPath()` 检查。

需要按数据类型查阅更多断言方法及其检查范围时，参见附录 [A12-1 常用测试断言库与断言方法](../appendix/A12_01_testing_assertions_reference.md)。

从第11章稳定工程开始。新建 `src/test/java/com/example/employee/dto/response/EmployeeResponseTest.java`，使用已有的响应对象，不修改正式业务代码。第11章 `pom.xml` 已有 `spring-boot-starter-test` 测试依赖。

```java
package com.example.employee.dto.response;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EmployeeResponseTest {

    @Test
    void employeeResponseKeepsConstructorValues() {
        EmployeeResponse response = new EmployeeResponse(
                1001L, "Tanaka", "Sales", "tanaka@example.com");

        assertThat(response.getName()).isEqualTo("Tanaka");
    }
}
```

`@Test` 来自JUnit Jupiter，标记需要执行的方法；`assertThat` 来自AssertJ，左侧读取实际姓名，`isEqualTo` 给出预期姓名。JUnit执行方法，断言一致时通过，不一致时失败并显示预期与实际。测试位于 `src/test/java`，不会作为正式业务类打包。

在Eclipse中右键该测试类，选择 **Run As → JUnit Test**。先确认绿色通过；把预期姓名临时改为 `Suzuki`，观察红色失败及expected/actual；恢复 `Tanaka` 并再次通过。此时尚未使用Mock、MockMvc或数据库；下一案例开始验证真实Mapper SQL。

## 五、案例二：Mapper真实SQL测试

先准备只供自动化测试使用的MySQL数据库，再编写Mapper测试。它与第9章的开发库完全分开，也供最后的完整集成测试使用。测试脚本会反复删表、建表和写入数据，因此数据库名固定以 `_test` 或 `_test_db` 结尾。课程不另外创建测试账号，继续使用第9章已经配置的本地MySQL账号；禁止把测试连接指向开发库、共享库或生产库。

`pom.xml` 的 `<dependencies>` 中加入以下两个测试依赖。第9章已经加入的MySQL Connector继续提供驱动，不再增加第二种数据库依赖：

```xml
<dependency>
    <groupId>org.mybatis.spring.boot</groupId>
    <artifactId>mybatis-spring-boot-starter-test</artifactId>
    <version>4.1.0</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-webmvc-test</artifactId>
    <scope>test</scope>
</dependency>
```

MyBatis测试Starter让`@MybatisTest`加载Mapper测试切片；Boot 4把MockMvc与MVC测试注解拆入 `spring-boot-starter-webmvc-test`。两个依赖都标为`test`作用域，不进入正式运行的JAR。

### 1. 创建专用MySQL测试库

在MySQL数据库客户端中，使用本机管理账号创建独立测试数据库，并把测试schema所需权限授予第9章已有的 `employee_app`。这里只复用已有账号，不创建新的测试账号：

```sql
CREATE DATABASE IF NOT EXISTS employee_test_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;

GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, DROP, ALTER
ON employee_test_db.*
TO 'employee_app'@'localhost';
```

课程不创建新的测试账号，测试继续使用第9章已有的 `employee_app`。由于第9章只授予了开发库的DML权限，这里需要由本机管理账号追加 `employee_test_db` 的读写与DDL权限；授权范围仍限定在测试schema。测试脚本具有删表权限，因此必须保留库名保护检查。切换到测试库后执行下面的查询，确认当前连接目标和账号：

```sql
SELECT DATABASE(), CURRENT_USER();
```

### 2. 新建测试Profile

新建`src/test/resources/application-test.yml`：

```yaml
spring:
  datasource:
    url: ${TEST_DB_URL:jdbc:mysql://localhost:3306/employee_test_db?useUnicode=true&characterEncoding=UTF-8&connectionTimeZone=Asia/Tokyo}
    username: ${DB_USERNAME:employee_app}
    password: ${DB_PASSWORD}
    driver-class-name: com.mysql.cj.jdbc.Driver
  sql:
    init:
      mode: never

mybatis:
  mapper-locations: classpath:mapper/*.xml
  type-aliases-package: com.example.employee.entity
  configuration:
    map-underscore-to-camel-case: true
    default-statement-timeout: 10

logging:
  file:
    name: target/test-logs/employee-api.log
  level:
    root: WARN
    com.example.employee: WARN
```

`@ActiveProfiles("test")`使测试读取这份配置，连接专用MySQL测试库而不是第9章开发库。账号默认沿用第9章的 `employee_app`，`DB_PASSWORD`继续从环境变量取得；实际账号或URL不同时再设置`DB_USERNAME`、`TEST_DB_URL`。Spring Boot App与JUnit在Eclipse中是不同运行配置：第一次执行数据库测试前，在 **Run Configurations → JUnit** 新建或选择 `EmployeeMapperTest`，然后在 **Environment** 设置 `DB_PASSWORD` 和实际需要覆盖的变量；最后运行 `EmployeeCrudIntegrationTest` 时也要在它的JUnit配置中设置。Service和Controller测试不连接数据库，不需要这些变量。`spring.sql.init.mode: never`关闭自动脚本执行，下面由`@Sql`在每个测试方法前明确重建数据。

### 3. 准备MySQL测试数据

新建`src/test/resources/test-data.sql`，供Mapper测试和后面的集成测试复用。该脚本包含破坏性DDL，只允许在上面的专用测试库执行：

```sql
DROP TABLE IF EXISTS employees;

CREATE TABLE employees (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(50) NOT NULL,
    department VARCHAR(50) NOT NULL,
    email VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_employees PRIMARY KEY (id),
    CONSTRAINT uk_employees_email UNIQUE (email)
) AUTO_INCREMENT = 1001;

INSERT INTO employees (
    id, name, department, email, status
) VALUES
    (1001, 'Tanaka', 'Sales', 'tanaka@example.com', 'ACTIVE'),
    (1002, 'Sato', 'Development', 'sato@example.com', 'ACTIVE');
```

`@Sql(scripts = "/test-data.sql", executionPhase = BEFORE_TEST_METHOD)`在每个Mapper测试前执行脚本，所以单独运行任意测试方法都从相同两条记录开始。因为脚本会执行`DROP TABLE`，下一小节还会加入数据库保护类，在脚本执行前确认产品确实是MySQL，并确认数据库名以`_test`或`_test_db`结尾。

### 4. 新建MySqlTestGuard.java

新建`src/test/java/com/example/employee/support/MySqlTestGuard.java`：

```java
package com.example.employee.support;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class MySqlTestGuard {

    @Autowired
    private DataSource dataSource;

    @BeforeAll
    void requireDedicatedMySqlTestDatabase() throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            String product = connection.getMetaData()
                    .getDatabaseProductName();
            String databaseName = connection.getCatalog();

            assertThat(product)
                    .as("测试必须直接连接MySQL")
                    .isEqualToIgnoringCase("MySQL");
            assertThat(databaseName)
                    .as("测试库名必须以_test或_test_db结尾")
                    .matches("(?i).*(?:_test|_test_db)$");
        }
    }
}
```

`@TestInstance(PER_CLASS)`让JUnit为整个测试类使用一个实例，使非静态`@BeforeAll`方法能够读取Spring注入的`DataSource`。保护类取得真实连接后检查数据库产品和catalog；检查不通过时测试立即失败，不继续执行测试方法。这个保护不能替代账号权限隔离，两项都要保留。

第3章由Spring Initializr生成的 `EmployeeManagementApiApplicationTests.java` 只有 `contextLoads()`，只能证明上下文能够建立，不能证明某一层的业务结果。为让每个测试文件都有明确目标，本章删除这个默认测试文件，不把它作为执行入口。保存依赖文件、测试配置、数据脚本和保护类，在Eclipse更新Maven项目后，直接运行下面的 `EmployeeMapperTest.java`。

### 5. 新建并运行Mapper测试类

MyBatis测试Starter 4.1.0支持当前Spring Boot 4.1技术基线。Mapper测试不Mock被测Mapper：MyBatis根据接口及XML创建代理，真正执行SQL。新建 `src/test/java/com/example/employee/mapper/EmployeeMapperTest.java`：

```java
package com.example.employee.mapper;

import com.example.employee.entity.Employee;
import com.example.employee.support.MySqlTestGuard;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@MybatisTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Sql(scripts = "/test-data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class EmployeeMapperTest extends MySqlTestGuard {
    @Autowired private EmployeeMapper mapper;

    @Test void findByIdMapsFieldsAndMissingReturnsNull() {
        Employee found = mapper.findById(1001L);
        assertThat(found).isNotNull();
        assertThat(found.getId()).isEqualTo(1001L);
        assertThat(found.getName()).isEqualTo("Tanaka");
        assertThat(found.getDepartment()).isEqualTo("Sales");
        assertThat(found.getEmail()).isEqualTo("tanaka@example.com");
        assertThat(mapper.findById(9999L)).isNull();
    }

    @Test void findListBindsDepartment() {
        List<Employee> found = mapper.findList("Sales");
        assertThat(found).hasSize(1);
        assertThat(found.get(0).getId()).isEqualTo(1001L);
        assertThat(found.get(0).getDepartment()).isEqualTo("Sales");
    }

    @Test void insertPersistsAndGeneratesId() {
        Employee employee = newEmployee("Suzuki", "suzuki@example.com");
        assertThat(mapper.insert(employee)).isEqualTo(1);
        assertThat(employee.getId()).isNotNull();
        assertThat(mapper.findById(employee.getId()).getEmail())
                .isEqualTo("suzuki@example.com");
    }

    @Test void updateAndDeleteChangeDatabaseState() {
        Employee employee = mapper.findById(1001L);
        employee.setName("Tanaka Updated");
        assertThat(mapper.update(employee)).isEqualTo(1);
        assertThat(mapper.findById(1001L).getName()).isEqualTo("Tanaka Updated");
        assertThat(mapper.deleteById(1001L)).isEqualTo(1);
        assertThat(mapper.findById(1001L)).isNull();
        assertThat(mapper.deleteById(1001L)).isZero();
    }

    @Test void duplicateEmailViolatesConstraint() {
        Employee duplicate = newEmployee("Another", "tanaka@example.com");
        assertThatThrownBy(() -> mapper.insert(duplicate))
                .isInstanceOf(DuplicateKeyException.class);
        assertThat(mapper.findList(null)).hasSize(2);
    }

    private Employee newEmployee(String name, String email) {
        Employee employee = new Employee();
        employee.setName(name);
        employee.setDepartment("Sales");
        employee.setEmail(email);
        employee.setStatus("ACTIVE");
        return employee;
    }
}
```

`@MybatisTest` 加载Mapper所需切片；`@ActiveProfiles("test")` 选择专用MySQL配置；`@AutoConfigureTestDatabase(replace = NONE)` 禁止测试切片替换显式数据源；`@Sql` 每个测试前重建固定数据。切片测试通常在事务中回滚，但仍须核对当前测试方法中的数据库状态。

这在日本项目中可能被归入Mapper的“単体テスト”，但从技术边界看，它连接了MyBatis、Mapper XML和真实MySQL，因此不是完全隔离依赖的纯Java单元测试，而是范围受限的Mapper切片/数据库测试。术语可以服从项目测试阶段，测试证据必须准确写出实际加载范围。

在Eclipse中右键 `EmployeeMapperTest.java`，选择 **Run As → JUnit Test**。预期5个测试全部通过；不需要先运行启动类或其他测试文件。如果Mapper注入失败，先检查测试依赖、扫描包、XML路径和测试数据库连接。

## 六、案例三：隔离Mapper测试Service成功分支

这里的被测对象是 `EmployeeServiceImpl`，依赖对象是 `EmployeeMapper`。Mock是可预设行为并记录调用的替身；Stub通常只强调预设返回。我们用Mockito的 `mock()` 代替依赖，`when(...).thenReturn(...)` 给定查询结果，`thenThrow(...)` 模拟异常；Service本身保持真实，因而可检查部门校验、数据整理、Entity到DTO转换和异常转换。Mock不执行SQL，故需要上一案例的Mapper测试与后面的集成测试补足。

| Mockito方法 | 本章意义 | 何时选用 |
| --- | --- | --- |
| `mock(EmployeeMapper.class)` | 创建Mapper替身 | 不希望单元测试依赖数据库 |
| `when(call).thenReturn(value)` / `thenThrow(error)` | 预设查询或故障 | 准备正常/异常输入条件 |
| `verify(mapper).findById(id)` | 检查发生过一次调用 | 业务流程要求必须查询 |
| `verify(mapper, times(2))` / `never()` | 指定调用次数/从未调用 | 次数本身是规格时 |
| `verifyNoInteractions(mapper)` | Mapper完全未调用 | 非法部门应先被拒绝 |
| `any(Employee.class)` / `eq(id)` | 参数匹配 | 关心对象类型或指定编号时；混用匹配器时同一调用的参数要一致使用匹配器 |

`assertThat(actual.getName()).isEqualTo("Tanaka")` 检查业务结果；`verify(mapper).findById(1001L)` 检查协作。两者互补，但不必验证每个无业务意义的内部调用。不要让只写 `verify`、未检查返回内容的测试假通过。

新建 `src/test/java/com/example/employee/service/EmployeeServiceImplTest.java`，先放入以下完整起步类。被测Service是真实对象，Mapper是Mockito替身，测试不启动Spring容器或MySQL。

```java
package com.example.employee.service;

import com.example.employee.dto.response.EmployeeResponse;
import com.example.employee.entity.Employee;
import com.example.employee.mapper.EmployeeMapper;
import com.example.employee.service.impl.EmployeeServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EmployeeServiceImplTest {

    private EmployeeMapper employeeMapper;
    private EmployeeServiceImpl employeeService;

    @BeforeEach
    void setUp() {
        employeeMapper = mock(EmployeeMapper.class);
        employeeService = new EmployeeServiceImpl(employeeMapper);
    }

    @Test
    void findByIdReturnsResponseWhenEmployeeExists() {
        Employee employee = new Employee();
        employee.setId(1001L);
        employee.setName("Tanaka");
        employee.setDepartment("Sales");
        employee.setEmail("tanaka@example.com");
        when(employeeMapper.findById(1001L)).thenReturn(employee);

        EmployeeResponse actual = employeeService.findById(1001L);

        assertThat(actual.getId()).isEqualTo(1001L);
        assertThat(actual.getName()).isEqualTo("Tanaka");
        verify(employeeMapper).findById(1001L);
    }
}
```

`@BeforeEach` 在每个 `@Test` 前重新创建对象；`mock(EmployeeMapper.class)` 返回不会执行SQL的Mapper替身，构造器把它传给真实Service。`when(...).thenReturn(...)` 预设返回Entity，`verify(...).findById(...)` 确认Service调用过Mapper；它不能证明Mapper XML正确。执行链为创建Mock → 构造Service → 预设返回 → 调用 `findById` → 断言DTO → 验证依赖调用。

在Eclipse运行此类，确认正常查询测试通过；临时把预期姓名改错观察失败后恢复。案例完成后保留这个类，下一案例往同一类追加异常测试，不再创建第二个Service测试类。

## 七、案例四：验证Service异常分支

继续修改上一案例的 `EmployeeServiceImplTest.java`。在文件顶部补充 `EmployeeCreateRequest`、`EmployeeNotFoundException`、`DuplicateEmailException`、`InvalidDepartmentException`、`DuplicateKeyException` 的导入，以及静态导入 `assertThatThrownBy`、`any`、`verifyNoInteractions`；下面三个方法放在原测试类的右大括号之前，原 `@BeforeEach` 和成功测试保持不变。

```java
    @Test
    void findByIdThrowsWhenEmployeeDoesNotExist() {
        when(employeeMapper.findById(9999L)).thenReturn(null);

        assertThatThrownBy(() -> employeeService.findById(9999L))
                .isInstanceOf(EmployeeNotFoundException.class)
                .hasMessage("员工不存在：9999");
        verify(employeeMapper).findById(9999L);
    }

    @Test
    void createConvertsDuplicateKeyToBusinessConflict() {
        EmployeeCreateRequest request = new EmployeeCreateRequest();
        request.setName("Suzuki");
        request.setDepartment("Sales");
        request.setEmail("tanaka@example.com");
        when(employeeMapper.insert(any(Employee.class)))
                .thenThrow(new DuplicateKeyException("duplicate email"));

        assertThatThrownBy(() -> employeeService.create(request))
                .isInstanceOf(DuplicateEmailException.class)
                .hasMessage("邮箱已被使用：tanaka@example.com");
        verify(employeeMapper).insert(any(Employee.class));
    }

    @Test
    void createRejectsInvalidDepartmentBeforeMapperCall() {
        EmployeeCreateRequest request = new EmployeeCreateRequest();
        request.setName("Suzuki");
        request.setDepartment("Unknown");
        request.setEmail("suzuki@example.com");

        assertThatThrownBy(() -> employeeService.create(request))
                .isInstanceOf(InvalidDepartmentException.class)
                .hasMessage("不允许的部门：Unknown");
        verifyNoInteractions(employeeMapper);
    }
```

`assertThatThrownBy(() -> ...)` 执行lambda中的代码：没有抛异常就失败，`isInstanceOf` 核对异常类型，`hasMessage` 核对完整消息。Mapper返回null应成为404对应的业务异常；Mock抛出 `DuplicateKeyException` 应被Service转成重复邮箱业务异常；非法部门要在接触Mapper之前拒绝，因此使用 `verifyNoInteractions`。Mock测试只证明Service分支，SQL仍需后面的集成测试。

在Eclipse中右键 `EmployeeServiceImplTest.java`，选择 **Run As → JUnit Test**，确认正常与三个异常场景通过。它不启动Spring容器，也不连接MySQL。临时把非法部门预期改为其他消息观察失败，然后恢复。案例完成后，测试类内容可与章末完整文件逐行核对。

## 八、案例五：用MockMvc验证Controller契约

Controller测试关注路由、HTTP方法、路径/查询参数、JSON请求体、校验、状态码、响应头和响应JSON，而不是Mapper SQL。`@WebMvcTest` 建立MVC切片，`@MockitoBean` 替代Service；MockMvc在进程内执行MVC请求，不监听真实端口。过滤器是否参与取决于测试切片加载和过滤器配置，本章的 `X-Request-Id` 断言以第11章 `RequestLoggingFilter` 被加载为前提；若未来加入Security，还需补认证与CSRF相关测试设置。

### 1. 新建Controller测试类

下面完整类先覆盖POST成功201、字段400和GET不存在404；练习需补GET成功200、JSON格式错误400、重复邮箱409、PUT成功200与DELETE成功204。`get()`、`post()`、`put()`、`delete()` 构造不同方法；`contentType()` 告诉MVC请求体格式，`content()` 提供JSON；`perform()` 执行，`andExpect()` 加断言，`andReturn()` 在需要读取Location等结果时取得响应。对于 `.andExpect(status().isCreated()).andExpect(header().string("Location", "/employees/2001")).andExpect(jsonPath("$.success").value(true)).andExpect(jsonPath("$.data.id").value(2001))`，四项依次检查201、资源地址、JSON根对象 `$` 下的成功标志与生成id；只检查201会漏掉错误的Location或错误数据。`header().exists()` 只检查存在，`content().json(expectedJson)` 可比较响应JSON结构，适合较完整的响应契约。

新建 `src/test/java/com/example/employee/controller/EmployeeControllerWebTest.java`。下面是本案例完整测试类，包含POST成功201与Location、字段错误400、详情不存在404；Service由Spring测试上下文中的 `@MockitoBean` 提供，数据库不参与。

```java
package com.example.employee.controller;

import com.example.employee.dto.request.EmployeeCreateRequest;
import com.example.employee.dto.response.EmployeeResponse;
import com.example.employee.exception.DuplicateEmailException;
import com.example.employee.exception.EmployeeNotFoundException;
import com.example.employee.service.EmployeeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EmployeeController.class)
@ActiveProfiles("test")
class EmployeeControllerWebTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EmployeeService employeeService;

    @Test
    void createReturns201LocationAndEmployeeJson() throws Exception {
        EmployeeResponse created = new EmployeeResponse(
                2001L,
                "Suzuki",
                "Support",
                "suzuki@example.com");
        when(employeeService.create(any(EmployeeCreateRequest.class)))
                .thenReturn(created);
        String requestJson =
                "{\"name\":\"Suzuki\","
                        + "\"department\":\"Support\","
                        + "\"email\":\"suzuki@example.com\"}";

        mockMvc.perform(post("/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(header().string(
                        "Location",
                        "/employees/2001"))
                .andExpect(header().exists("X-Request-Id"))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(2001))
                .andExpect(jsonPath("$.data.department")
                        .value("Support"));
        verify(employeeService)
                .create(any(EmployeeCreateRequest.class));
    }

    @Test
    void invalidBodyReturns400BeforeServiceCall() throws Exception {
        String requestJson =
                "{\"name\":\"\","
                        + "\"department\":\"Sales\","
                        + "\"email\":\"not-an-email\"}";

        mockMvc.perform(post("/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message")
                        .value("参数校验失败"))
                .andExpect(jsonPath("$.data.name").exists())
                .andExpect(jsonPath("$.data.email").exists());
        verifyNoInteractions(employeeService);
    }

    @Test
    void missingEmployeeReturns404() throws Exception {
        when(employeeService.findById(9999L))
                .thenThrow(new EmployeeNotFoundException(9999L));

        mockMvc.perform(get("/employees/9999"))
                .andExpect(status().isNotFound())
                .andExpect(header().exists("X-Request-Id"))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message")
                        .value("员工不存在：9999"));
        verify(employeeService).findById(9999L);
    }
}
```

### 2. MockMvc、请求和断言

#### 2.1 `@WebMvcTest`和`@MockitoBean`

`@WebMvcTest(EmployeeController.class)` 用在测试类上，由Spring Boot Web MVC Test处理。它只加载Spring MVC相关部分，包括指定Controller、Controller Advice、JSON转换、校验和过滤器，不加载真实Service和Mapper。Boot 4把这个注解移动到 `org.springframework.boot.webmvc.test.autoconfigure` 包；切片范围可参考[Spring Boot 4.1测试文档](https://docs.spring.io/spring-boot/reference/testing/spring-boot-applications.html#testing.spring-boot-applications.spring-mvc-tests)。

Controller构造方法需要 `EmployeeService`，因此测试使用：

```java
@MockitoBean
private EmployeeService employeeService;
```

`@MockitoBean` 来自Spring Test，写在测试字段上。它用Mockito mock覆盖测试容器中指定类型的Bean；字段类型决定目标，本章不填其他参数。Spring Boot 4.1官方示例使用 `@WebMvcTest` 与 `@MockitoBean` 组合，不再使用旧教程中常见的Boot `@MockBean`。

#### 2.2 MockMvc请求与断言

`MockMvc` 是Spring MVC服务端测试工具。它让请求经过MVC的路径匹配、JSON转换、校验、Controller和异常处理，但不启动真实HTTP端口。

| 对象或方法 | 当前参数 | 可接受的值 | 返回与效果 |
| --- | --- | --- | --- |
| `@Autowired` | 本章无显式参数 | 可注入的Bean字段、构造器或方法 | 测试容器把自动配置的MockMvc设置给字段 |
| `post(path)` | `/employees` | 合法路径字符串 | 返回POST请求构建器 |
| `.contentType(type)` | `MediaType.APPLICATION_JSON` | Spring支持的媒体类型 | 设置 `Content-Type: application/json` |
| `.content(body)` | JSON字符串 | 字符串、字节数组等 | 设置请求体 |
| `mockMvc.perform(request)` | 已构建请求 | MockMvc请求构建器 | 执行MVC请求并返回结果操作对象 |
| `.andExpect(matcher)` | 一个结果匹配器 | 状态、响应头、JSON等匹配器 | 条件不成立时使测试失败，返回同一链式结果 |
| `status().isCreated()` | 无 | 无参数 | 断言HTTP状态为201 |
| `header().string(name, value)` | 响应头名和预期值 | 合法响应头名与字符串值 | 断言响应头完全相等 |
| `jsonPath(path).value(value)` | JSONPath表达式和预期值 | 可定位响应JSON的路径与可比较值 | 断言目标JSON字段值 |

`$` 表示JSON根对象，`.data.id` 按层级进入字段。JSONPath只是测试中定位响应值的表达式，不会改变Controller的响应。

`jsonPath(path).exists()` 不填参数，断言路径定位的字段存在；`jsonPath(path).isNumber()` 不填参数，断言目标值是JSON数字。前者适合检查字段错误已返回，后者适合检查系统已生成数值id。

Web切片中Service返回值由Mockito预定，所以201测试能证明Controller契约，不能证明INSERT SQL正确。


`@WebMvcTest` 只建立Spring MVC相关测试上下文，不启动完整应用或外部HTTP服务器。`MockMvc` 在进程内模拟请求，检查路由、绑定、校验、状态、Headers和JSON；Service由Mock替换，因此该测试不能证明业务逻辑或数据库SQL正确。

### 3. 验证和完成状态

在Eclipse中右键 `EmployeeControllerWebTest.java`，选择 **Run As → JUnit Test**。验证POST成功201和Location、字段错误400、GET不存在404等当前契约。临时改错URL观察404后恢复。这个测试只启动MVC切片，不需要先启动应用；Controller测试通过后再用Postman观察真实HTTP请求。


## 九、案例六：启动应用并用Postman测试真实接口

前面的测试类分别验证一层，Postman用于从应用外部发送真实HTTP请求。这个案例不是JUnit单体测试：需要先在Eclipse运行 `EmployeeManagementApiApplication.java`，请求会经过真实Controller、Service、Mapper和第9章开发数据库。确认Console出现应用启动完成信息后再操作Postman，不运行任何测试类。

先在数据库客户端确认1001号员工存在：

```sql
SELECT id, name, department, email, status
FROM employees
WHERE id = 1001;
```

### 1. 查询接口

| 项目 | 设置或预期 |
| --- | --- |
| HTTP方法 | GET |
| URL | `http://localhost:8080/employees/1001` |
| Path参数 | `id=1001`，已经包含在URL中 |
| Query参数 | 无 |
| Headers | 可接受默认值，不需要设置 `Content-Type` |
| 请求体 | 无 |
| 预期状态码 | 200 |
| 预期响应 | `success=true`，`data.id=1001`，姓名、部门和邮箱与数据库一致 |

在Postman中新建请求，选择GET并输入URL，点击Send。保存状态码、响应Headers和响应JSON；再与数据库查询结果比较。把id改为9999重新发送，预期状态码为404且 `success=false`。

### 2. 新增接口

| 项目 | 设置或预期 |
| --- | --- |
| HTTP方法 | POST |
| URL | `http://localhost:8080/employees` |
| Path/Query参数 | 无 |
| Header | `Content-Type: application/json` |
| 请求体 | 下方合法JSON |
| 预期状态码 | 201 |
| 预期Headers | `Location` 指向新员工URL，存在 `X-Request-Id` |
| 预期响应 | `success=true`，`data.id`为数据库生成的数字 |

在Postman的 **Body → raw → JSON** 输入：

```json
{
  "name": "Postman Test",
  "department": "Support",
  "email": "postman-test-12@example.com"
}
```

发送后复制响应中的id，并在数据库客户端确认：

```sql
SELECT id, name, department, email, status
FROM employees
WHERE email = 'postman-test-12@example.com';
```

再次发送相同请求，预期409；把姓名改为空字符串，预期400。验证完成后使用DELETE接口删除本次新增的数据，或在个人练习数据库中按实际id清理，不能留下会影响后续练习的重复邮箱。Postman能够证明真实HTTP入口和当前运行环境的结果，但手工步骤不具备JUnit自动回归的速度和可重复性。

## 十、案例七：最后用@SpringBootTest验证完整应用协作

前面已经分别验证各层，并用Postman观察过真实HTTP接口。现在复用案例二建立的专用MySQL测试库、`application-test.yml`和`test-data.sql`，用 `@SpringBootTest` 把Controller、Service、Mapper XML和MySQL放进同一个自动化测试。

学员仍然直接右键运行 `EmployeeCrudIntegrationTest.java`。`@SpringBootTest` 不是另一个启动按钮，也不要求预先运行启动类；它只告诉当前JUnit测试建立完整Spring应用上下文。默认模拟Web环境不监听8080，因此本案例继续使用MockMvc在测试进程内发送请求。

**src/test/java/com/example/employee/integration/EmployeeCrudIntegrationTest.java：** 新建完整文件。

```java
package com.example.employee.integration;

import com.example.employee.support.MySqlTestGuard;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Sql(
        scripts = "/test-data.sql",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class EmployeeCrudIntegrationTest extends MySqlTestGuard {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void completeCrudKeepsHttpAndDatabaseConsistent()
            throws Exception {
        String createJson =
                "{\"name\":\"Suzuki\","
                        + "\"department\":\"Support\","
                        + "\"email\":\"suzuki@example.com\"}";
        MvcResult createResult = mockMvc.perform(post("/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").isNumber())
                .andReturn();

        String location = createResult.getResponse()
                .getHeader("Location");
        assertThat(location).isNotBlank();
        long employeeId = Long.parseLong(
                location.substring(location.lastIndexOf('/') + 1));
        assertThat(countEmployee(employeeId)).isEqualTo(1);

        mockMvc.perform(get("/employees/{id}", employeeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name")
                        .value("Suzuki"));

        String updateJson =
                "{\"name\":\"Suzuki Ichiro\","
                        + "\"department\":\"Development\","
                        + "\"email\":\"suzuki@example.com\"}";
        mockMvc.perform(put("/employees/{id}", employeeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.department")
                        .value("Development"));

        mockMvc.perform(delete("/employees/{id}", employeeId))
                .andExpect(status().isNoContent());
        assertThat(countEmployee(employeeId)).isZero();

        mockMvc.perform(get("/employees/{id}", employeeId))
                .andExpect(status().isNotFound());
    }

    @Test
    void duplicateEmailReturns409AndDoesNotInsert() throws Exception {
        String requestJson =
                "{\"name\":\"Another Tanaka\","
                        + "\"department\":\"Sales\","
                        + "\"email\":\"tanaka@example.com\"}";

        mockMvc.perform(post("/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false));

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM employees",
                Integer.class);
        assertThat(count).isEqualTo(2);
    }

    @Test
    void invalidRequestReturns400AndDoesNotInsert() throws Exception {
        String requestJson =
                "{\"name\":\"\","
                        + "\"department\":\"Sales\","
                        + "\"email\":\"bad\"}";

        mockMvc.perform(post("/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.name").exists())
                .andExpect(jsonPath("$.data.email").exists());

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM employees",
                Integer.class);
        assertThat(count).isEqualTo(2);
    }

    private int countEmployee(long employeeId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM employees WHERE id = ?",
                Integer.class,
                employeeId);
        return count == null ? 0 : count;
    }
}
```

### 1. 启动范围和测试数据

#### 1.1 启动测试上下文

| 注解 | 当前参数 | 可接受的值 | 触发时机与结果 |
| --- | --- | --- | --- |
| `@SpringBootTest` | 本章使用默认值 | 可指定配置类、属性和Web环境 | 通过SpringApplication创建完整应用上下文，默认不启动真实服务器 |
| `@AutoConfigureMockMvc` | 本章使用默认值 | 可配置过滤器、输出方式等 | 在完整上下文中创建MockMvc，默认加入应用过滤器 |
| `@ActiveProfiles("test")` | profile名 `test` | 一个或多个profile名 | 测试时激活test profile，读取 `application-test.yml` |
| `@Sql` | SQL脚本与执行阶段 | classpath脚本路径和 `Sql.ExecutionPhase` 枚举 | 在每个测试方法前重建表和初始数据 |

Spring Boot 4.1中，`@SpringBootTest` 默认创建模拟Web环境而不监听8080；与Boot 4新包名下的 `@AutoConfigureMockMvc` 组合后可用MockMvc走完应用内部请求链。

新增接口的链式调用最后使用 `.andReturn()`，该方法无参数，返回 `MvcResult`。`MvcResult` 保存这次MockMvc请求的完整测试结果；`getResponse()` 返回模拟HTTP响应，再用 `getHeader("Location")` 取得指定响应头。这样后续步骤可以使用实际生成的id，而不是假定id必然为某个固定数字。

```java
get("/employees/{id}", employeeId)
```

路径中的 `{id}` 是MockMvc请求构建器的URI变量位置，后面的 `employeeId` 按顺序填入。返回给Controller的仍是实际路径字符串，例如 `/employees/1003`。

#### 1.2 为什么不连接开发数据库

`application-test.yml` 用专用MySQL测试库覆盖正式 `application.yml` 的开发库连接。Mapper测试和完整集成测试激活test profile；测试日志写入 `target/test-logs`，`clean` 时一起清理，不污染正式 `logs` 目录。

| 配置或保护 | 作用 |
| --- | --- |
| `TEST_DB_URL` | 指向名称以 `_test` 或 `_test_db` 结尾的专用MySQL数据库 |
| `DB_USERNAME` | 沿用第9章已有的本地MySQL账号 |
| `DB_PASSWORD` | 沿用第9章环境变量中的本地密码，不写入Git |
| `MySqlTestGuard` | 执行SQL脚本前再次检查数据库产品与数据库名 |

测试绝不应使用开发者正在操作的MySQL schema，更不能指向共享库或生产环境。`test-data.sql` 会先 `DROP TABLE`；本课程虽然复用本地账号，仍必须同时使用独立数据库和保护类，不能只靠文件名或口头约定。多个开发者或多个测试进程也不能同时共用同一个会反复重建表的测试schema。

#### 1.3 @Sql为什么在每个方法前执行

一个测试新增或删除员工后，不能把改变留给下一个测试。`BEFORE_TEST_METHOD` 让每个方法都从两条固定记录开始：

```text
方法A前：重建表和两条数据 → 执行A
方法B前：再次重建表和两条数据 → 执行B
```

因此测试不需要固定执行顺序。单独运行任意一个方法，结果也应与整类执行相同。

#### 1.4 JdbcTemplate只用来核对数据库结果

`JdbcTemplate` 是Spring JDBC提供的数据库操作对象。生产业务仍使用MyBatis；测试用JdbcTemplate直接数行数，避免用同一个Mapper同时执行和验证自己的SQL。

```java
Integer count = jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM employees WHERE id = ?",
        Integer.class,
        employeeId);
```

| 参数 | 当前值 | 可接受的值 | 作用 |
| --- | --- | --- | --- |
| SQL | 带一个 `?` 的COUNT查询 | 当前数据库可执行的SQL | 指定要查询的数值 |
| 返回类型 | `Integer.class` | 与单列结果可转换的类型 | 要求把COUNT结果转成Integer |
| SQL参数 | `employeeId` | 与 `?` 数量和顺序一致的值 | 使用参数绑定，不拼接输入 |

`queryForObject()` 期待一行结果并返回指定类型对象。COUNT查询会返回一行；本方法对理论上的 `null` 做了保守处理。

可靠的自动化测试应满足：

1. 自己准备前置数据，不要求开发者先手工插入记录。
2. 不依赖另一个测试先执行。
3. 不连接真实业务库、外部API或个人环境数据。
4. 相同代码和环境下重复执行，结果相同。
5. 测试失败后不留下需要手工清理的共享状态。

`completeCrudKeepsHttpAndDatabaseConsistent()` 在一个测试方法内保留了“新增后才能修改这个新id”的业务顺序，但它不依赖其他 `@Test` 方法。

不要使用 `@TestMethodOrder` 来隐藏共享数据问题。只有规格本身要求多步过程时，才在同一测试方法中明确完成该过程。


本案例使用真实Controller、Service、Mapper XML和专用MySQL测试库，不使用Mapper Mock。该数据库只服务test profile，不能指向开发库、共享库或生产库。

### 2. 必须明确的数据库边界

使用MySQL测试库可以验证当前MySQL版本、字符集、排序规则和SQL方言下的真实行为，但仍不能代表生产环境的全部条件。生产版本、参数、数据规模、并发、权限和网络可能不同；本章测试只能证明已记录的测试环境和用例通过。

### 3. 验证和完成状态

在Eclipse中右键 `EmployeeCrudIntegrationTest.java`，选择 **Run As → JUnit Test**。不需要先启动 `EmployeeManagementApiApplication`。确认新增、查询、修改和删除后的HTTP与数据库行数一致；单独运行任意测试方法也应通过，证明数据准备不依赖执行顺序。


数据库边界：测试脚本与第9章一样使用MySQL的 `DATETIME`、自增主键和唯一约束，因此能发现内存兼容数据库可能隐藏的差异。仍要在证据中记录MySQL版本、schema名、字符集和排序规则；本章的重复邮箱测试主要验证Service小写化后的冲突，不能自动代表所有生产数据与并发情况。

## 十一、案例八：执行一次回归测试

选择“非法部门返回400且不写入数据库”这一已有规则：

1. 正常运行对应Service、Web和集成测试，保存通过结果；
2. 临时删除Service中的部门判断；
3. 重跑测试，观察失败方法、expected和actual；
4. 根据失败信息定位Service业务规则；
5. 恢复正确代码；
6. 先重跑失败测试，再运行全部测试。

这个过程说明回归测试的价值：改修后自动确认既有行为仍然成立，而不是只证明新代码能够编译。故意修改只能在本地受控进行，恢复后必须检查工作区差异，不能把错误代码提交。

回归测试用于确认改修没有破坏原有行为。先做影响调查，再选择测试：

| 改修例 | 最小直接测试 | 相关回归 | 理由 |
| --- | --- | --- | --- |
| 修改部门业务规则 | Service单元测试 | POST和PUT的400/成功接口，完整CRUD | 新增和修改共用Service规则 |
| 修改请求DTO校验 | Web切片400测试 | POST、PUT正常与边界值 | JSON绑定和字段契约发生变化 |
| 修改Mapper XML | 数据库集成测试 | 使用同一Mapper的Service与接口 | Mock不能验证真实SQL和映射 |
| 修改全局异常处理 | Web切片异常测试 | 400、404、409、500以及405/415 | 错误响应和框架默认状态都可能受影响 |
| 只修改文案 | 对应响应断言 | 相同错误类型的前端契约 | 消息可能被调用方使用 |

正式提交前仍应执行项目规定的全部测试。“最小直接测试”用于开发中快速反馈，不是减少交付前回归范围的借口。


## 十二、运行测试、读取失败并保存证据

学习阶段直接运行当前测试文件，不通过默认ApplicationTests统一启动，也不要求先把所有测试一起执行：

| 顺序 | Eclipse中直接运行的文件 | 主要确认 |
| --- | --- | --- |
| 1 | `EmployeeResponseTest.java` | JUnit和AssertJ基本断言 |
| 2 | `EmployeeMapperTest.java` | Mapper XML、真实SQL和映射 |
| 3 | `EmployeeServiceImplTest.java` | 隔离Mapper后的业务规则 |
| 4 | `EmployeeControllerWebTest.java` | MockMvc模拟请求与HTTP契约 |
| 5 | 启动类，不是测试文件 | 使用Postman手工验证真实接口 |
| 6 | `EmployeeCrudIntegrationTest.java` | `@SpringBootTest`下的完整协作 |

右键目标Java文件，选择 **Run As → JUnit Test**。某一类失败时只重跑该类，修复后再按表中顺序确认受影响范围。Postman用于开发中的手工接口确认，不能替代自动化断言、数据隔离和完整回归。

全部单独测试通过后，正式提交前再保留一次Maven整套回归。在包含 `pom.xml` 的项目根目录执行：

在包含 `pom.xml` 的项目根目录执行：

```powershell
.\mvnw.cmd clean test
```

`clean` 先删除旧的Maven构建输出，`test` 重新编译并执行测试。成功时应看到每个测试类的执行情况、最终汇总和 `BUILD SUCCESS`。测试数量会随已创建的测试类和后续章节增加，不要把某个固定数字当成通过条件；重点核对最终汇总中的 `Failures: 0`、`Errors: 0` 和 `Skipped: 0`，并确认没有遗漏应运行的测试类。

```text
BUILD SUCCESS
```

失败时按顺序阅读：

1. 先看失败的测试类和方法名。
2. 再区分 `Failure` 的断言不一致与 `Error` 的未预期异常。
3. 核对expected和actual，不要看到红色就先修改测试期望。
4. 使用第11章日志和堆栈定位实际失败层。
5. 修复后先重跑失败测试，再执行完整 `clean test`。

Maven的详细结果位于：

```text
target/surefire-reports/
```

自测证据至少保留：执行命令、代码版本或提交编号、测试数量、失败数、执行时间与必要报告。不要只截取 `BUILD SUCCESS` 一行，也不要上传包含密码、Token或个人数据的日志。

## 十三、常见失败与定位

| 现象 | 定位点 | 常见原因 | 修正 |
| --- | --- | --- | --- |
| `NoSuchBeanDefinitionException` | 失败的测试类和缺少类型 | Web切片未提供Controller依赖 | 使用 `@MockitoBean` 提供Service |
| 接口测试返404 | MockMvc请求路径与Controller映射 | URL或HTTP方法写错 | 对照接口规格和Controller注解 |
| 应返400却返201 | DTO约束与Controller参数 | 缺少约束或 `@Valid` | 先修正正式契约，不要改测试迁就错误实现 |
| `Table 'employees' doesn't exist` | active profile、DataSource URL和 `@Sql` | test profile未激活、连接错库或SQL脚本未执行 | 核对test profile、实际schema和classpath路径 |
| 单独运行通过，全部运行失败 | 测试间共享字段和数据 | 依赖执行顺序或留下脏数据 | 每个方法重建自己的前置状态 |
| 保护检查失败 | 数据库产品名和catalog | URL误指向非MySQL或非测试库 | 修正环境变量，禁止绕过保护继续执行 |

## 十四、日本项目测试规格、打键与Review练习

“打键测试”通常指测试人员按手顺实际操作并记录结果，不是JUnit的另一个名称。本项目没有前端画面，不能伪造画面打键证据；可用Postman手工发送接口请求，核对响应与数据库。手工操作能观察真实配置和人员使用流程，但不能代替高频回归的自动化；Service Mock测试也不能代替真实接口/数据库验证。

| 编号/对象 | 前置条件 | 输入与操作 | 预期结果 | 实际与判定 | 证据 |
| --- | --- | --- | --- | --- | --- |
| UT-M-01 Mapper查询 | 专用MySQL测试库存在员工1001 | `findById(1001)` | id、姓名、部门、邮箱均映射正确 | 运行后填写Pass/Fail | Eclipse JUnit结果 |
| UT-S-02 Service异常 | Mapper模拟返回null | `findById(9999)` | 抛 `EmployeeNotFoundException` | 运行后填写 | JUnit失败/成功记录 |
| IT-C-03 接口新增 | 测试库无目标邮箱 | Postman POST合法JSON | 201、Location、数据库新行 | 执行后填写 | Postman响应与数据库查询 |

规格书还应记录测试日期、环境和执行人；这里只给可复核的最小示例，不预填“通过”。正常系、异常系与边界值都要覆盖：Mapper可测存在/不存在、唯一约束；Service可测新增成功/非法部门；Controller可测201/400与姓名50/51字符。改修后根据影响范围选回归用例并保存Eclipse JUnit结果、Maven Surefire报告、Postman响应和数据库查询。Review时问：断言能否真正证明预期业务？是否只验证Mock调用、没有验证结果？测试数据是否独立？测试实际连接的是哪个数据库、什么版本和排序规则？

### 练习1：增加更新异常测试

前置：使用本章固定测试数据。实现PUT不存在id返404、PUT重复邮箱返409两个集成测试。各自核对HTTP响应和employees总数，证明失败请求没有创建新记录。

### 练习2：设计边界值

为姓名50个字符和51个字符分别建立POST测试。前者应通过字段校验，后者应返400。证据中写明规格上限、输入长度和实际状态。

### 练习3：Review假通过测试

Review下面代码，指出它没有证明的内容，并增加必要断言：

```java
@Test
void createEmployee() throws Exception {
    mockMvc.perform(post("/employees")
            .contentType(MediaType.APPLICATION_JSON)
            .content(validJson));
}
```

至少检查状态、Location、关键JSON和数据库记录；如果是Web切片测试，数据库检查应改为Service调用验证，不得声称INSERT已通过。

### 练习4：改修影响调查

规格变更：Support部门员工不允许删除。先列出应修改的Service、新异常或处理器、接口规格和测试文件；再实现Support拒绝、Sales仍可删除、不存在id仍返404的测试。

### 练习5：整理自测证据

执行一次指定测试类和一次完整 `clean test`，整理下表：

| 项目 | 证据 |
| --- | --- |
| 实施内容 | 本次新增或修改的测试场景 |
| 执行命令 | 实际使用的Maven Wrapper命令 |
| 结果 | 测试总数、Failure、Error、Skipped |
| 失败与对应 | 失败方法、原因、修正和重跑结果；没有则写无 |
| 回归范围 | 根据影响调查选择了哪些测试 |
| 未验证项 | 例如MySQL特有行为、并发或未建立的权限功能 |

日本项目资料中常见的“正常系”是预期成功场景，“异常系”是预期拒绝或错误场景，“エビデンス”是能证明执行条件和结果的证据。名称不能代替具体输入、预期和实际结果。

## 十五、章末完整代码

JUnit把前置条件、执行和断言写成可重复运行的检查。先写一个只验证响应对象的JUnit测试，再分别完成Mapper层、Service层和Controller层测试。Mockito的 `when()` 规定替身在指定调用下返回什么，`verify()` 检查某个调用是否真的发生，替身不会执行真实SQL。`@WebMvcTest` 只加载Web切片，通过MockMvc模拟请求与响应，不启动真实HTTP监听端口；`@MockitoBean` 把测试上下文中的Service Bean替换为Mock。完成分层测试后，启动应用并用Postman观察真实HTTP结果；最后才用 `@SpringBootTest`、`@AutoConfigureMockMvc`、`@ActiveProfiles("test")` 和 `@Sql` 自动验证真实Mapper与隔离测试库。

三层测试证明的范围不同：Service Mock测试不能证明Controller映射，Web切片不能证明SQL，数据库集成测试才能检查实际读写。以IDE运行JUnit作为学习主线；Maven测试用于整套回归和构建。失败时先区分断言不符、Spring测试上下文无法建立、测试数据库初始化失败，再查看对应日志。本章不修改 `src/main` 下的Controller、Service、Mapper和XML；下方保留所有测试文件的完整最终内容。

本章修改后的新增部分：

```text
employee-management-api/
├── pom.xml                                      ← 完整替换
└── src/test/
    ├── java/com/example/employee/
    │   ├── dto/response/EmployeeResponseTest.java
    │   ├── mapper/EmployeeMapperTest.java
    │   ├── controller/EmployeeControllerWebTest.java
    │   ├── integration/EmployeeCrudIntegrationTest.java
    │   ├── service/EmployeeServiceImplTest.java
    │   └── support/MySqlTestGuard.java
    └── resources/
        ├── application-test.yml
        └── test-data.sql
```

### 1. 完整替换pom.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>4.1.1</version>
        <relativePath/>
    </parent>

    <groupId>com.example</groupId>
    <artifactId>employee-management-api</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <name>employee-management-api</name>
    <description>Employee management REST API</description>

    <properties>
        <java.version>17</java.version>
    </properties>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-webmvc</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>

        <dependency>
            <groupId>org.mybatis.spring.boot</groupId>
            <artifactId>mybatis-spring-boot-starter</artifactId>
            <version>4.1.0</version>
        </dependency>

        <dependency>
            <groupId>com.mysql</groupId>
            <artifactId>mysql-connector-j</artifactId>
            <scope>runtime</scope>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-webmvc-test</artifactId>
            <scope>test</scope>
        </dependency>

        <dependency>
            <groupId>org.mybatis.spring.boot</groupId>
            <artifactId>mybatis-spring-boot-starter-test</artifactId>
            <version>4.1.0</version>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

### 2. 新建application-test.yml

文件位置：

```text
src/test/resources/application-test.yml
```

完整内容：

```yaml
spring:
  datasource:
    url: ${TEST_DB_URL:jdbc:mysql://localhost:3306/employee_test_db?useUnicode=true&characterEncoding=UTF-8&connectionTimeZone=Asia/Tokyo}
    username: ${DB_USERNAME:employee_app}
    password: ${DB_PASSWORD}
    driver-class-name: com.mysql.cj.jdbc.Driver
  sql:
    init:
      mode: never

mybatis:
  mapper-locations: classpath:mapper/*.xml
  type-aliases-package: com.example.employee.entity
  configuration:
    map-underscore-to-camel-case: true
    default-statement-timeout: 10

logging:
  file:
    name: target/test-logs/employee-api.log
  level:
    root: WARN
    com.example.employee: WARN
```

### 3. 新建test-data.sql

文件位置：

```text
src/test/resources/test-data.sql
```

完整内容：

```sql
DROP TABLE IF EXISTS employees;

CREATE TABLE employees (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    department VARCHAR(50) NOT NULL,
    email VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_employees_email UNIQUE (email)
);

INSERT INTO employees (
    id,
    name,
    department,
    email,
    status
) VALUES
    (1001, 'Tanaka', 'Sales', 'tanaka@example.com', 'ACTIVE'),
    (1002, 'Sato', 'Development', 'sato@example.com', 'ACTIVE');
```

### 4. 新建MySqlTestGuard.java

文件位置：`src/test/java/com/example/employee/support/MySqlTestGuard.java`。所有会执行建表或改写数据的MySQL测试都继承这个保护类：

```java
package com.example.employee.support;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class MySqlTestGuard {

    @Autowired
    private DataSource dataSource;

    @BeforeAll
    void requireDedicatedMySqlTestDatabase() throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            String databaseProduct = connection.getMetaData().getDatabaseProductName();
            String databaseName = connection.getCatalog();

            assertThat(databaseProduct)
                    .as("测试必须直接连接MySQL")
                    .isEqualToIgnoringCase("MySQL");
            assertThat(databaseName)
                    .as("测试库名必须以_test或_test_db结尾")
                    .matches("(?i).*(?:_test|_test_db)$");
        }
    }
}
```

### 5. 删除默认ApplicationTests.java

删除第3章由Spring Initializr生成的 `src/test/java/com/example/employee/EmployeeManagementApiApplicationTests.java`。本章直接运行每一个有明确断言目标的测试类，最后再由 `EmployeeCrudIntegrationTest` 使用 `@SpringBootTest` 检查完整协作。

### 6. 新建EmployeeResponseTest.java

文件位置：`src/test/java/com/example/employee/dto/response/EmployeeResponseTest.java`。

```java
package com.example.employee.dto.response;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EmployeeResponseTest {

    @Test
    void employeeResponseKeepsConstructorValues() {
        EmployeeResponse response = new EmployeeResponse(
                1001L, "Tanaka", "Sales", "tanaka@example.com");

        assertThat(response.getName()).isEqualTo("Tanaka");
    }
}
```

### 7. 新建EmployeeMapperTest.java

```java
package com.example.employee.mapper;

import com.example.employee.entity.Employee;
import com.example.employee.support.MySqlTestGuard;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@MybatisTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Sql(scripts = "/test-data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class EmployeeMapperTest extends MySqlTestGuard {
    @Autowired private EmployeeMapper mapper;

    @Test void findByIdMapsFieldsAndMissingReturnsNull() {
        Employee found = mapper.findById(1001L);
        assertThat(found).isNotNull();
        assertThat(found.getId()).isEqualTo(1001L);
        assertThat(found.getName()).isEqualTo("Tanaka");
        assertThat(found.getDepartment()).isEqualTo("Sales");
        assertThat(found.getEmail()).isEqualTo("tanaka@example.com");
        assertThat(mapper.findById(9999L)).isNull();
    }

    @Test void findListBindsDepartment() {
        List<Employee> found = mapper.findList("Sales");
        assertThat(found).hasSize(1);
        assertThat(found.get(0).getId()).isEqualTo(1001L);
        assertThat(found.get(0).getDepartment()).isEqualTo("Sales");
    }

    @Test void insertPersistsAndGeneratesId() {
        Employee employee = newEmployee("Suzuki", "suzuki@example.com");
        assertThat(mapper.insert(employee)).isEqualTo(1);
        assertThat(employee.getId()).isNotNull();
        assertThat(mapper.findById(employee.getId()).getEmail())
                .isEqualTo("suzuki@example.com");
    }

    @Test void updateAndDeleteChangeDatabaseState() {
        Employee employee = mapper.findById(1001L);
        employee.setName("Tanaka Updated");
        assertThat(mapper.update(employee)).isEqualTo(1);
        assertThat(mapper.findById(1001L).getName()).isEqualTo("Tanaka Updated");
        assertThat(mapper.deleteById(1001L)).isEqualTo(1);
        assertThat(mapper.findById(1001L)).isNull();
        assertThat(mapper.deleteById(1001L)).isZero();
    }

    @Test void duplicateEmailViolatesConstraint() {
        Employee duplicate = newEmployee("Another", "tanaka@example.com");
        assertThatThrownBy(() -> mapper.insert(duplicate))
                .isInstanceOf(DuplicateKeyException.class);
        assertThat(mapper.findList(null)).hasSize(2);
    }

    private Employee newEmployee(String name, String email) {
        Employee employee = new Employee();
        employee.setName(name);
        employee.setDepartment("Sales");
        employee.setEmail(email);
        employee.setStatus("ACTIVE");
        return employee;
    }
}
```

### 8. 新建EmployeeServiceImplTest.java

文件位置：

```text
src/test/java/com/example/employee/service/EmployeeServiceImplTest.java
```

完整内容：

```java
package com.example.employee.service;

import com.example.employee.dto.request.EmployeeCreateRequest;
import com.example.employee.dto.response.EmployeeResponse;
import com.example.employee.entity.Employee;
import com.example.employee.exception.DuplicateEmailException;
import com.example.employee.exception.EmployeeNotFoundException;
import com.example.employee.exception.InvalidDepartmentException;
import com.example.employee.mapper.EmployeeMapper;
import com.example.employee.service.impl.EmployeeServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.dao.DuplicateKeyException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class EmployeeServiceImplTest {

    private EmployeeMapper employeeMapper;
    private EmployeeServiceImpl employeeService;

    @BeforeEach
    void setUp() {
        employeeMapper = mock(EmployeeMapper.class);
        employeeService = new EmployeeServiceImpl(employeeMapper);
    }

    @Test
    void findByIdReturnsResponseWhenEmployeeExists() {
        Employee employee = new Employee();
        employee.setId(1001L);
        employee.setName("Tanaka");
        employee.setDepartment("Sales");
        employee.setEmail("tanaka@example.com");
        when(employeeMapper.findById(1001L)).thenReturn(employee);

        EmployeeResponse actual = employeeService.findById(1001L);

        assertThat(actual.getId()).isEqualTo(1001L);
        assertThat(actual.getName()).isEqualTo("Tanaka");
        assertThat(actual.getDepartment()).isEqualTo("Sales");
        assertThat(actual.getEmail()).isEqualTo("tanaka@example.com");
        verify(employeeMapper).findById(1001L);
    }

    @Test
    void findByIdThrowsWhenEmployeeDoesNotExist() {
        when(employeeMapper.findById(9999L)).thenReturn(null);

        assertThatThrownBy(() -> employeeService.findById(9999L))
                .isInstanceOf(EmployeeNotFoundException.class)
                .hasMessage("员工不存在：9999");
        verify(employeeMapper).findById(9999L);
    }

    @Test
    void createConvertsDuplicateKeyToBusinessConflict() {
        EmployeeCreateRequest request = new EmployeeCreateRequest();
        request.setName("Suzuki");
        request.setDepartment("Sales");
        request.setEmail("tanaka@example.com");
        when(employeeMapper.insert(any(Employee.class)))
                .thenThrow(new DuplicateKeyException("duplicate email"));

        assertThatThrownBy(() -> employeeService.create(request))
                .isInstanceOf(DuplicateEmailException.class)
                .hasMessage("邮箱已被使用：tanaka@example.com");
        verify(employeeMapper).insert(any(Employee.class));
    }

    @Test
    void createRejectsInvalidDepartmentBeforeMapperCall() {
        EmployeeCreateRequest request = new EmployeeCreateRequest();
        request.setName("Suzuki");
        request.setDepartment("Unknown");
        request.setEmail("suzuki@example.com");

        assertThatThrownBy(() -> employeeService.create(request))
                .isInstanceOf(InvalidDepartmentException.class)
                .hasMessage("不允许的部门：Unknown");
        verifyNoInteractions(employeeMapper);
    }

    @Test
    void createReturnsGeneratedEmployee() {
        EmployeeCreateRequest request = new EmployeeCreateRequest();
        request.setName("Suzuki");
        request.setDepartment("Sales");
        request.setEmail("suzuki@example.com");
        when(employeeMapper.insert(any(Employee.class))).thenAnswer(invocation -> {
            Employee inserted = invocation.getArgument(0);
            inserted.setId(2001L);
            return 1;
        });
        Employee stored = new Employee();
        stored.setId(2001L);
        stored.setName("Suzuki");
        stored.setDepartment("Sales");
        stored.setEmail("suzuki@example.com");
        when(employeeMapper.findById(2001L)).thenReturn(stored);

        EmployeeResponse actual = employeeService.create(request);
        assertThat(actual.getId()).isEqualTo(2001L);
        assertThat(actual.getName()).isEqualTo("Suzuki");
        verify(employeeMapper).insert(any(Employee.class));
        verify(employeeMapper).findById(2001L);
    }

    @Test
    void updateMissingEmployeeThrowsNotFound() {
        com.example.employee.dto.request.EmployeeUpdateRequest request =
                new com.example.employee.dto.request.EmployeeUpdateRequest();
        request.setName("Suzuki");
        request.setDepartment("Sales");
        request.setEmail("suzuki@example.com");
        when(employeeMapper.update(any(Employee.class))).thenReturn(0);
        when(employeeMapper.findById(9999L)).thenReturn(null);

        assertThatThrownBy(() -> employeeService.update(9999L, request))
                .isInstanceOf(EmployeeNotFoundException.class);
        verify(employeeMapper).update(any(Employee.class));
        verify(employeeMapper).findById(9999L);
    }
}
```

### 9. 新建EmployeeControllerWebTest.java

文件位置：

```text
src/test/java/com/example/employee/controller/EmployeeControllerWebTest.java
```

完整内容：

```java
package com.example.employee.controller;

import com.example.employee.dto.request.EmployeeCreateRequest;
import com.example.employee.dto.response.EmployeeResponse;
import com.example.employee.exception.DuplicateEmailException;
import com.example.employee.exception.EmployeeNotFoundException;
import com.example.employee.service.EmployeeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EmployeeController.class)
@ActiveProfiles("test")
class EmployeeControllerWebTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EmployeeService employeeService;

    @Test
    void createReturns201LocationAndEmployeeJson() throws Exception {
        EmployeeResponse created = new EmployeeResponse(
                2001L,
                "Suzuki",
                "Support",
                "suzuki@example.com");
        when(employeeService.create(any(EmployeeCreateRequest.class)))
                .thenReturn(created);
        String requestJson =
                "{\"name\":\"Suzuki\","
                        + "\"department\":\"Support\","
                        + "\"email\":\"suzuki@example.com\"}";

        mockMvc.perform(post("/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(header().string(
                        "Location",
                        "/employees/2001"))
                .andExpect(header().exists("X-Request-Id"))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(2001))
                .andExpect(jsonPath("$.data.department")
                        .value("Support"));
        verify(employeeService)
                .create(any(EmployeeCreateRequest.class));
    }

    @Test
    void invalidBodyReturns400BeforeServiceCall() throws Exception {
        String requestJson =
                "{\"name\":\"\","
                        + "\"department\":\"Sales\","
                        + "\"email\":\"not-an-email\"}";

        mockMvc.perform(post("/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message")
                        .value("参数校验失败"))
                .andExpect(jsonPath("$.data.name").exists())
                .andExpect(jsonPath("$.data.email").exists());
        verifyNoInteractions(employeeService);
    }

    @Test
    void missingEmployeeReturns404() throws Exception {
        when(employeeService.findById(9999L))
                .thenThrow(new EmployeeNotFoundException(9999L));

        mockMvc.perform(get("/employees/9999"))
                .andExpect(status().isNotFound())
                .andExpect(header().exists("X-Request-Id"))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message")
                        .value("员工不存在：9999"));
        verify(employeeService).findById(9999L);
    }

    @Test
    void getExistingEmployeeReturns200() throws Exception {
        when(employeeService.findById(1001L)).thenReturn(
                new EmployeeResponse(1001L, "Tanaka", "Sales", "tanaka@example.com"));
        mockMvc.perform(get("/employees/1001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Tanaka"));
    }

    @Test
    void malformedJsonReturns400() throws Exception {
        mockMvc.perform(post("/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{invalid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
        verifyNoInteractions(employeeService);
    }

    @Test
    void duplicateEmailReturns409() throws Exception {
        when(employeeService.create(any(EmployeeCreateRequest.class)))
                .thenThrow(new DuplicateEmailException("tanaka@example.com"));
        mockMvc.perform(post("/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Another\",\"department\":\"Sales\","
                                + "\"email\":\"tanaka@example.com\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void updateReturns200() throws Exception {
        when(employeeService.update(org.mockito.ArgumentMatchers.eq(1001L),
                any(com.example.employee.dto.request.EmployeeUpdateRequest.class)))
                .thenReturn(new EmployeeResponse(
                        1001L, "Tanaka Updated", "Sales", "tanaka@example.com"));
        mockMvc.perform(put("/employees/1001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Tanaka Updated\",\"department\":\"Sales\","
                                + "\"email\":\"tanaka@example.com\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Tanaka Updated"));
    }

    @Test
    void deleteReturns204() throws Exception {
        mockMvc.perform(delete("/employees/1001"))
                .andExpect(status().isNoContent());
        verify(employeeService).delete(1001L);
    }
}
```

### 10. 新建EmployeeCrudIntegrationTest.java

文件位置：

```text
src/test/java/com/example/employee/integration/EmployeeCrudIntegrationTest.java
```

完整内容：

```java
package com.example.employee.integration;

import com.example.employee.support.MySqlTestGuard;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Sql(
        scripts = "/test-data.sql",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class EmployeeCrudIntegrationTest extends MySqlTestGuard {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void completeCrudKeepsHttpAndDatabaseConsistent()
            throws Exception {
        String createJson =
                "{\"name\":\"Suzuki\","
                        + "\"department\":\"Support\","
                        + "\"email\":\"suzuki@example.com\"}";
        MvcResult createResult = mockMvc.perform(post("/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").isNumber())
                .andReturn();

        String location = createResult.getResponse()
                .getHeader("Location");
        assertThat(location).isNotBlank();
        long employeeId = Long.parseLong(
                location.substring(location.lastIndexOf('/') + 1));
        assertThat(countEmployee(employeeId)).isEqualTo(1);

        mockMvc.perform(get("/employees/{id}", employeeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name")
                        .value("Suzuki"));

        String updateJson =
                "{\"name\":\"Suzuki Ichiro\","
                        + "\"department\":\"Development\","
                        + "\"email\":\"suzuki@example.com\"}";
        mockMvc.perform(put("/employees/{id}", employeeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.department")
                        .value("Development"));

        mockMvc.perform(delete("/employees/{id}", employeeId))
                .andExpect(status().isNoContent());
        assertThat(countEmployee(employeeId)).isZero();

        mockMvc.perform(get("/employees/{id}", employeeId))
                .andExpect(status().isNotFound());
    }

    @Test
    void duplicateEmailReturns409AndDoesNotInsert() throws Exception {
        String requestJson =
                "{\"name\":\"Another Tanaka\","
                        + "\"department\":\"Sales\","
                        + "\"email\":\"tanaka@example.com\"}";

        mockMvc.perform(post("/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false));

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM employees",
                Integer.class);
        assertThat(count).isEqualTo(2);
    }

    @Test
    void invalidRequestReturns400AndDoesNotInsert() throws Exception {
        String requestJson =
                "{\"name\":\"\","
                        + "\"department\":\"Sales\","
                        + "\"email\":\"bad\"}";

        mockMvc.perform(post("/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.name").exists())
                .andExpect(jsonPath("$.data.email").exists());

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM employees",
                Integer.class);
        assertThat(count).isEqualTo(2);
    }

    private int countEmployee(long employeeId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM employees WHERE id = ?",
                Integer.class,
                employeeId);
        return count == null ? 0 : count;
    }
}
```

## 十六、附录：测试扩展方向

以下内容不属于本章基本测试闭环：

- **Mockito注解方式**：普通Service单元测试也可使用 `@ExtendWith(MockitoExtension.class)`、`@Mock` 和 `@InjectMocks` 自动初始化，但本章主线固定使用显式 `mock()` 加构造器。它不同于Web切片中用于替换Spring Bean的 `@MockitoBean`。
- **JUnit异常断言**：`assertThrows(EmployeeNotFoundException.class, () -> service.findById(9999L))` 也是常见写法，可取得异常对象继续检查消息；本章主线统一使用AssertJ的 `assertThatThrownBy()`。

- **高级Mockito技巧**：参数捕获、严格桩和复杂Answer应在基本Mock边界稳定后学习。
- **复杂参数化测试**：本章只要求能为明确边界编写独立测试；大量组合再考虑参数化组织。
- **Testcontainers**：可以用容器提供更接近MySQL的测试环境，但需要Docker、启动成本和数据生命周期管理。
- **H2独立实验**：如果需要理解内存数据库的快速测试方式，可完成附录 [A12-2 使用H2完成独立数据库测试实验](../appendix/A12_02_h2_test_database.md)。它使用另一套profile和脚本，仅用于比较学习，不是第13章之后的前置环境，也不能代替本章MySQL测试结果。
- **高级测试数据管理**：数据工厂、迁移工具和大规模场景需要独立规范。
- **覆盖率工具**：覆盖率用于发现未执行区域，不等于断言质量或业务完整性。
- **性能测试**：负载、容量和稳定性测试不属于JUnit功能测试的替代品。
- **测试上下文优化**：上下文缓存和切片组合属于测试规模扩大后的优化主题。

## 十七、本章稳定状态

完成后，正式代码和MySQL配置保持第11章状态，新增测试文件：

```text
pom.xml
src/test/
├── java/com/example/employee/
│   ├── controller/EmployeeControllerWebTest.java
│   ├── dto/response/EmployeeResponseTest.java
│   ├── integration/EmployeeCrudIntegrationTest.java
│   ├── mapper/EmployeeMapperTest.java
│   ├── service/EmployeeServiceImplTest.java
│   └── support/MySqlTestGuard.java
└── resources/
    ├── application-test.yml
    └── test-data.sql
```

此时你应能够：

1. 从手工验收中整理前置条件、输入、操作和预期；
2. 区分Mapper切片测试、Service单体测试、Controller Web切片测试和完整集成测试的证明范围；
3. 在Eclipse中直接运行每一个测试文件，并读取JUnit结果；
4. 使用JUnit、AssertJ和Mockito验证Service正常与异常分支；
5. 使用MockMvc验证请求、状态、响应头和JSON契约；
6. 启动应用后使用Postman验证真实HTTP接口及数据库结果；
7. 最后使用 `@SpringBootTest` 验证Controller、Service、Mapper与MySQL的完整协作；
8. 根据改修影响选择直接测试、整套回归范围和自测证据。

下一章会在现有CRUD上增加“多步写入必须整体成功或整体失败”的事务边界，并使用自动化测试证明回滚结果。
