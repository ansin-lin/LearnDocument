# 第12章 员工CRUD的分层测试与接口验证

> 本章目标：分别完成Mapper数据库测试、Service单元测试和Controller层MockMvc测试的正常系与异常系，比较专用MySQL测试库和H2内存库，最后用Postman与`@SpringBootTest`检查完整调用链，并根据改修范围选择回归测试。

手工验证适合初次观察接口和数据库，但每次改修后都重新输入请求、查表并整理结果，容易漏掉旧功能。自动化测试把“前置条件、输入、操作、预期”保存成代码，修改后可再次执行。

```text
已确认的手工结果
  → 整理测试条件和预期
  → 在MySQL和H2分别验证Mapper
  → 隔离依赖验证Service
  → 用MockMvc验证Controller
  → 用Postman观察真实HTTP接口
  → 最后验证完整Spring Boot调用链
  → 保存断言结果、失败信息与测试证据
```

## 一、先认识测试阶段与证据

测试不是“运行一次没报错”，而是给定前置数据和输入，比较实际结果与预期结果，并留下可复核证据。日本项目常称単体テスト（单体测试）、結合テスト（结合测试）、システムテスト（系统测试）；名称对应项目阶段，但不与某个框架注解绝对一一对应。单体测试尽量缩小被测范围，结合测试验证组件协作，系统测试从系统对外行为和业务流程验收。回归测试是在改修后重复相关旧用例，确认原功能未被破坏。

### 日本项目中的测试阶段与测试范围

| 阶段 | 常见日语名称 | 主要验证范围 | 本项目中的例子 |
| --- | --- | --- | --- |
| 单体测试 | 単体テスト（UT） | 单个模块或组件是否符合详细设计 | Service的Mock测试、Controller的Web切片测试 |
| 结合测试 | 結合テスト（IT） | 多个真实组件、功能或系统能否协作 | Controller→Service→Mapper→MySQL |
| 系统测试 | システムテスト（ST） | 整体系统是否满足业务及非功能要求 | 完整业务流程和系统层面的验收 |
| 回归测试 | リグレッションテスト | 改修后旧行为是否仍成立 | 重跑受影响测试及相关旧用例 |

JUnit负责发现、执行和报告测试方法，不代表某个测试阶段；MockMvc在测试进程内模拟请求进入Spring MVC，也不能单凭它判断是UT还是IT。Eclipse可以启动单体测试，也可以启动结合测试。**分类依据是被验证的范围与项目测试计划，而不是所用工具。**日本项目的阶段名称和边界可能不同，以上是本课程的整理方式，不是所有企业通用的固定标准。

本章Mapper测试连接真实MySQL，技术上是Mapper切片/数据库测试，并非完全隔离依赖的纯Java单元测试。项目测试计划可以把它归入Mapper层的単体テスト，也可以把它纳入内部结合测试；不能仅因连接数据库就绝对判为IT。

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
| 修改为他人邮箱 | 员工1001与1002存在，邮箱不同 | PUT `/employees/1001`，邮箱改为1002的邮箱 | 409；1001原姓名、部门、邮箱不变，总记录数仍为2 |
| 完整CRUD | 固定初始数据 | 新增→查询→修改→删除 | 各阶段HTTP与表状态一致，删除后再查返404 |

这张表是测试规格，不是“调用过方法就算通过”。每个测试方法都必须有可判断的预期结果。

## 三、沿调用链选择测试对象

员工接口的调用顺序是 `HTTP请求 → Controller → Service → Mapper → 数据库 → HTTP响应`。本章分别检查数据库访问、业务逻辑、HTTP输入输出，最后检查它们能否协作；单独通过其中一种测试，不代表其他部分也正确。

- 要检查SQL与字段映射，运行连接专用数据库的Mapper测试。
- 要检查部门规则与异常转换，隔离数据库来测试Service。
- 要检查URL、请求体、状态码和响应JSON，测试Controller。
- 要观察应用启动后的真实HTTP结果，用Postman发送请求；要自动检查完整调用链，运行最后的集成测试。

### 日本前后端分离项目中的结合测试

下表按**一起验证的对象**整理常见结合测试，不要求本章为每一种情况新增测试类或依赖；名称和阶段边界以项目测试计划书为准。

| 类型 | 常见日语说法 | 一起验证什么 | 员工管理项目中的例子 |
| --- | --- | --- | --- |
| 后端内部结合 | 内部結合テスト / モジュール間結合テスト | Controller、Service、Mapper与数据库 | 新增员工API实际写入MySQL |
| 前后端结合 | 画面・API結合テスト | 真实前端页面与Spring Boot API | 员工新增画面调用POST并显示结果 |
| 功能间结合 | 機能間結合テスト | 多个业务功能的连续行为 | 新增员工后查询、修改和删除 |
| 子系统间结合 | サブシステム間結合テスト | 不同子系统之间的交互 | 员工系统与人事系统交换数据 |
| 外部系统结合 | 外部システム結合テスト / 外部IFテスト | 外部API或文件接口 | 从外部部门信息API取得资料 |
| 批处理结合 | バッチ結合テスト | Batch、JOB、数据库和业务处理 | 定时同步员工状态 |

部分项目还使用ITa表示内部結合テスト、ITb表示外部結合テスト；另一些项目可能赋予它们不同范围。这些缩写不是统一标准，实际工作以该项目测试计划书的定义为准。

例如前后端结合测试可以沿着下面的真实调用观察：

```text
Vue / React员工新增页面输入姓名、部门和邮箱
  → POST /employees发送JSON
  → Spring Boot处理并写入MySQL
  → 返回201、400或409
  → 页面显示新增结果或对应错误
```

要核对JSON字段是否正确、201后页面是否显示新员工、400是否显示字段错误、409是否提示邮箱重复，以及请求失败时页面不会误报成功；需要登录的场景还应检查401与403。若前端把API全部替换为Mock，只能验证页面自身处理，不能证明真实前后端联携。本节只建立测试对象的概念，不引入前端测试框架。

具体加载哪些组件、哪些依赖被替换，以及各测试究竟能证明什么，等完成对应代码示例后再对照。每个自动化测试类可在Eclipse中直接选择 **Run As → JUnit Test**；只有Postman案例需要运行启动类。本章尚未引入Spring Security，不增加认证配置。

## 四、案例一：先运行一个最小JUnit测试

从第11章的稳定工程开始。第11章的 `pom.xml` 已包含 `spring-boot-starter-test`。先新建 `src/test/java/com/example/employee/dto/response/EmployeeResponseTest.java`，只检查一个已有响应对象，不启动Spring或连接数据库：

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

- `@Test` 来自JUnit Jupiter，标记需要执行并报告结果的测试方法。
- `assertThat(response.getName())` 来自AssertJ，括号中传入**实际值**；`isEqualTo("Tanaka")` 给出**预期值**。
- 这条断言只证明姓名，不证明id、部门或邮箱；要证明其他字段，需分别读取并断言。

JUnit负责执行测试，AssertJ负责判断结果，两者不能互相替代。

在Eclipse中右键这个文件，选择 **Run As → JUnit Test**。先观察绿色通过，再把预期值临时改成 `"Suzuki"`，阅读失败视图里的expected和actual，最后恢复 `"Tanaka"` 并重跑。每个测试方法都应自己准备数据，不依赖其他测试先执行。`@BeforeEach` 等生命周期注解会在Service案例首次使用时讲解。

接下来的案例在真实数据、业务逻辑和HTTP响应三种不同对象上写断言。每次先看测试要证明什么，再看代码及断言的准确检查范围；需要横向查找更多方法时使用 [A12-1 常用测试断言库与断言方法](../appendix/A12_01_testing_assertions_reference.md)。

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

- `mybatis-spring-boot-starter-test`：提供后文Mapper切片测试所需的测试自动配置。
- `spring-boot-starter-webmvc-test`：提供Web MVC测试自动配置，并让后文的 `MockMvc` 与请求构建器等测试类可用。若Eclipse仍找不到这些类或静态方法，先核对依赖已加入当前项目，再执行 **Maven → Update Project**。
- `<scope>test</scope>`：两个依赖只参与编译与运行测试，不进入正式业务JAR。

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

这份YAML只提供测试环境的配置值，**不会因为文件存在就自动激活**。先核对其中与数据库有关的设置：

- `spring.datasource.url`：默认指向本机的 `employee_test_db`；实际测试库地址不同才用 `TEST_DB_URL` 覆盖，绝不能指向开发库、共享库或生产库。
- `spring.datasource.username`、`password`：默认沿用第9章的 `employee_app`，密码由JUnit运行配置中的 `DB_PASSWORD` 环境变量提供；实际账号不同才设置 `DB_USERNAME`。
- `driver-class-name`：指定MySQL JDBC驱动；第9章已有驱动依赖，不在这里重复添加。
- `spring.sql.init.mode: never`：关闭Spring Boot自动执行初始化脚本；下一步创建的 `test-data.sql` 将由测试类显式指定执行时机。
- `mybatis` 与 `logging`：沿用主线的Mapper XML扫描、对象映射和日志规则；测试日志单独写入 `target/test-logs`。

Spring Boot App与JUnit在Eclipse中是不同运行配置。第一次运行数据库测试前，在 **Run Configurations → JUnit** 选择对应测试类，于 **Environment** 设置 `DB_PASSWORD` 和实际需要覆盖的变量；最后运行集成测试类时也要检查其JUnit配置。Service和Controller的隔离测试不连接数据库，不需要这些数据库变量。测试类如何选择 `test` 配置，将在它第一次出现时结合代码说明。

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

这份脚本的内容与风险要分开看：

- `DROP TABLE IF EXISTS employees` 会删除测试库中已有的员工表及数据，因此只能用于专用测试库；不要手工拿它重置第9章的开发库。
- `CREATE TABLE` 重建本章要验证的字段、主键和邮箱唯一约束；MySQL方言与第9章主线一致。
- `INSERT` 准备员工1001与1002，供后面的正常系、查无数据和重复邮箱测试使用。

下一小节先建立数据库保护类，随后在Mapper测试类的代码中指定由谁执行脚本、何时执行；在看到测试类之前，不需要记住尚未出现的注解写法。

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

- `@TestInstance(TestInstance.Lifecycle.PER_CLASS)`：让JUnit为整个测试类使用一个实例，因此下面的 `@BeforeAll` 方法可以是非静态方法。
- `@BeforeAll`：在本测试类任何测试方法开始前执行保护检查；这里的 `DataSource` 已由Spring注入。
- `getDatabaseProductName()` 与 `getCatalog()`：从实际数据库连接读取产品名和当前库名，而不是只相信配置文件名。
- `assertThat(...).matches(...)`：要求库名以 `_test` 或 `_test_db` 结尾；产品或库名检查失败时，测试不应继续执行重建表的脚本。

保护检查不能替代账号权限隔离；测试库和库名检查都必须保留。

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

    @Test void findByIdMapsExistingEmployee() {
        Employee found = mapper.findById(1001L);
        assertThat(found).isNotNull();
        assertThat(found.getId()).isEqualTo(1001L);
        assertThat(found.getName()).isEqualTo("Tanaka");
        assertThat(found.getDepartment()).isEqualTo("Sales");
        assertThat(found.getEmail()).isEqualTo("tanaka@example.com");
    }

    @Test void findByIdReturnsNullWhenEmployeeDoesNotExist() {
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
        // 失败的INSERT不应增加员工；重新查询表状态，而不只检查异常类型。
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

先按测试方法理解它们各自证明的结果：

- `findByIdMapsExistingEmployee`、`findListBindsDepartment`：分别检查一行数据的字段映射和部门条件查询。列表数量正确还不够，还要核对返回的id、部门。
- `insertPersistsAndGeneratesId`、`updateAndDeleteChangeDatabaseState`：检查影响行数，并在写入后再次读取数据库，确认实际状态改变；删除第二次影响0行也是预期分支。
- `findByIdReturnsNullWhenEmployeeDoesNotExist`：验证Mapper对不存在id的约定是返回 `null`，不是抛异常。
- `duplicateEmailViolatesConstraint`：验证重复邮箱被MySQL唯一约束拒绝，并且表中员工数没有增加。“异常系”也包含返回空值或0行等非命中分支，不一定都抛Java异常。

| 首次出现的写法 | 谁处理、何时发生 | 本例要证明什么 |
| --- | --- | --- |
| `@MybatisTest` | Spring Boot与MyBatis测试自动配置在运行测试类时建立Mapper切片 | 真正创建Mapper代理、解析XML并执行SQL；不加载完整业务应用 |
| `@AutoConfigureTestDatabase(replace = NONE)` | Spring Boot建立测试数据源时处理 | 保留`application-test.yml`明确指定的MySQL连接，不自动换为嵌入式库 |
| `@ActiveProfiles("test")` | Spring Test创建上下文前选择profile | 读取测试专用数据库配置，不能误连开发库 |
| `@Sql(..., BEFORE_TEST_METHOD)` | Spring Test在每个测试方法前执行脚本 | 每个方法均从两条固定员工记录开始，不依赖执行顺序 |
| `@Autowired` | Spring建立测试上下文时注入字段 | 取得实际MyBatis Mapper代理，不是Mockito替身 |
| `@Test` | JUnit运行测试时识别方法 | 一个方法检查一个可说明的场景 |

再看测试中第一次用到的断言：

- `isNotNull()` 只证明对象存在；后续字段的 `isEqualTo(...)` 才证明映射值正确。
- `hasSize(1)` 只证明列表有一项；继续检查元素的id和部门，才能证明筛选结果正确。
- `isEqualTo(1)` 要求写操作影响恰好一行，`isZero()` 要求影响0行，`isNull()` 检查查无数据时的返回值。
- `assertThatThrownBy(() -> mapper.insert(duplicate))` 执行lambda中的INSERT；`isInstanceOf(DuplicateKeyException.class)` 检查异常类型。还要再查表仍为两条，才能证明失败没有新增记录。

更多按数据类型查阅的断言见[A12-1](../appendix/A12_01_testing_assertions_reference.md)。

- `@MybatisTest` 通常给每个测试方法建立测试事务，方法结束后回滚DML；仍须在**当前方法内**核对写入结果。
- `@Sql` 在每个方法前执行包含 `DROP TABLE` 的脚本；MySQL的DDL会隐式提交，不能依赖测试事务撤销删表。因此必须使用专用测试库和上面的 `MySqlTestGuard`，不能把脚本用于开发库。

这在日本项目中可能被归入Mapper的“単体テスト”，但从技术边界看，它连接了MyBatis、Mapper XML和真实MySQL，因此不是完全隔离依赖的纯Java单元测试，而是范围受限的Mapper切片/数据库测试。术语可以服从项目测试阶段，测试证据必须准确写出实际加载范围。

在Eclipse中右键 `EmployeeMapperTest.java`，选择 **Run As → JUnit Test**。预期6个测试全部通过；不需要先运行启动类或其他测试文件。如果Mapper注入失败，先检查测试依赖、扫描包、XML路径和测试数据库连接。

现在完成[A12-2 使用H2完成独立数据库测试实验](../appendix/A12_02_h2_test_database.md)：用另一套profile与脚本运行相同的核心Mapper规格，记录两种数据库的结果和差异。实验结束后回到本章Service案例；后续主线和第13章仍以专用MySQL测试库为准。

## 六、案例三：用JUnit与Mockito编写Service测试

被测对象是实际的 `EmployeeServiceImpl`，但不启动Spring容器或数据库。`EmployeeMapper`由Mockito创建替身：测试预设它的返回值或异常，然后检查Service的转换、业务规则和错误处理。Mock不会执行SQL，因此Mapper案例不能被跳过。

新建 `src/test/java/com/example/employee/service/EmployeeServiceImplTest.java`。下面是**可直接运行的完整测试类**；每个方法自己建立前置条件，没有测试方法之间的执行顺序要求：

```java
package com.example.employee.service;

import com.example.employee.dto.request.EmployeeCreateRequest;
import com.example.employee.dto.request.EmployeeUpdateRequest;
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
import static org.mockito.Mockito.never;
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
        EmployeeUpdateRequest request =
                new EmployeeUpdateRequest();
        request.setName("Suzuki");
        request.setDepartment("Sales");
        request.setEmail("suzuki@example.com");
        when(employeeMapper.findById(9999L)).thenReturn(null);

        assertThatThrownBy(() -> employeeService.update(9999L, request))
                .isInstanceOf(EmployeeNotFoundException.class);
        verify(employeeMapper).findById(9999L);
        verify(employeeMapper, never()).update(any(Employee.class));
    }
}
```

### 1. 先让JUnit准备被测对象

- `@Test` 写在每个测试方法上，JUnit才会发现并执行该方法；方法名说明要验证的行为，JUnit视图会分别报告结果。
- `@BeforeEach` 写在`setUp()`上。JUnit在**每个**`@Test`方法前调用它，所以每次都重新执行`mock(EmployeeMapper.class)`和`new EmployeeServiceImpl(employeeMapper)`；测试不依赖上一方法留下的Mock调用或数据。
- `mock(EmployeeMapper.class)`由Mockito创建接口替身，不会执行Mapper XML或访问数据库。被测对象仍是**真实**`EmployeeServiceImpl`；构造器参数明确告诉学员Service依赖哪个Mapper。附录[A12-3](../appendix/A12_03_junit_jupiter_annotations_methods.md)另有`@Mock`、`@InjectMocks`写法，本章先用手动构造方式。

### 2. 在调用Service之前设定Mapper行为

- `when(employeeMapper.findById(1001L)).thenReturn(employee)`表示：当Mock收到**这个参数**的调用时返回预先构造的`Employee`。`findByIdReturnsResponseWhenEmployeeExists`因此能检查真实Service如何把Entity转换为Response DTO。
- `when(...).thenReturn(null)`用于`findByIdThrowsWhenEmployeeDoesNotExist`：它明确给Service一个“查不到”的Mapper结果；Service随后应抛业务异常。对象返回值未设定时Mockito通常也返回`null`，但显式写出更容易看懂前置条件。
- `when(...).thenThrow(new DuplicateKeyException(...))`用于`createConvertsDuplicateKeyToBusinessConflict`：模拟Mapper写入失败，检查Service是否转换为`DuplicateEmailException`。这里没有真的触发MySQL唯一约束；真实约束由Mapper测试验证。
- `when(...).thenAnswer(invocation -> {...})`用于`createReturnsGeneratedEmployee`：`invocation.getArgument(0)`取得传给INSERT的`Employee`，设置模拟生成的id并返回影响行数1。只写`thenReturn(1)`不会给这个对象回填id，无法继续模拟“插入后按id查询”。`when(employeeMapper.findById(2001L)).thenReturn(stored)`再为第二次Mapper调用提供结果。
- `any(Employee.class)`是Mockito的参数匹配器，表示此处匹配一个`Employee`参数；它**不检查**对象的姓名、邮箱等字段。若字段本身是规格，还要增加字段断言或参数捕获。

### 3. 调用真实Service，再选择断言方法

- 像`EmployeeResponse actual = employeeService.findById(1001L)`这样直接调用被测方法，拿到实际返回值后，再写`assertThat(actual.getName()).isEqualTo("Tanaka")`。`assertThat`接收**实际值**，`isEqualTo`给出预期值；本例分别检查id、姓名、部门和邮箱，不能只断言对象非空。
- 期望Service抛异常时，将调用放进`assertThatThrownBy(() -> employeeService.findById(9999L))`的lambda中；`isInstanceOf(...)`检查异常类型或其子类型，`hasMessage(...)`检查完整消息。`createConvertsDuplicateKeyToBusinessConflict`同样用这组方法检查异常转换。不要先执行被测方法，再试图断言已经抛出的异常。
- `createReturnsGeneratedEmployee`检查返回DTO的id和姓名，说明模拟回填及重新读取后的**Service返回值**正确；Mock设置和断言必须分开，`thenReturn(...)`本身不是测试通过的证据。

### 4. 最后用Mockito核对必要的调用

- `verify(employeeMapper).findById(1001L)`默认要求该调用恰好发生一次；它证明协作路径，不证明DTO字段，更不证明SQL执行。
- `verifyNoInteractions(employeeMapper)`要求整个方法执行期间没有调用这个Mock。`createRejectsInvalidDepartmentBeforeMapperCall`用它证明非法部门在访问数据库前被拒绝。
- `verify(employeeMapper, never()).update(any(Employee.class))`只要求**指定的UPDATE调用从未发生**，并不代表Mapper没有其他调用。`updateMissingEmployeeThrowsNotFound`先通过`findById(9999L)`检查存在性，查不到后不执行UPDATE；它不是“UPDATE返回0行才发现不存在”。

自己写下一条Service测试时，依次确定被测方法和Mapper替身、设定`when(...)`、调用真实Service、写返回值或异常断言，最后只对有意义的协作写`verify(...)`。合法结果、查无数据、数据库异常和非法部门在此只是不同输入；它们都按同一写法组织，不需要为“正常系”和“异常系”分别背一套工具。

在Eclipse中右键这个类选择 **Run As → JUnit Test**，先运行一个方法，再运行整类；临时改错一个预期消息，观察JUnit失败信息后恢复。下一案例仍使用同一个Service接口，但会把Service整体替换为Mock，转而检查Controller的HTTP契约。

## 七、案例四：用MockMvc编写Controller测试

Controller测试要证明请求是否命中正确方法、JSON是否绑定和校验、状态码/响应头/响应体是否符合接口规格。它不启动真实HTTP端口，也不连接数据库。`@WebMvcTest`加载MVC切片，`@MockitoBean`提供Service替身；因此通过的Controller测试**不能证明Service业务逻辑或Mapper SQL**。

新建 `src/test/java/com/example/employee/controller/EmployeeControllerWebTest.java`。下面的完整文件包括GET、POST、PUT、DELETE的成功路径，以及字段校验、JSON格式、查无员工、重复邮箱等失败路径：

```java
package com.example.employee.controller;

import com.example.employee.dto.request.EmployeeCreateRequest;
import com.example.employee.dto.request.EmployeeUpdateRequest;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
        when(employeeService.update(eq(1001L),
                any(EmployeeUpdateRequest.class)))
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

### 1. 用注解决定测试范围并准备Service替身

- `@WebMvcTest(EmployeeController.class)`由Spring Boot测试框架处理，只加载指定Controller及MVC相关组件，不加载完整Service、Mapper和数据库。括号中的`EmployeeController.class`是本例要测试的Controller类型；漏写它可能扩大或改变切片范围。
- `@ActiveProfiles("test")`由Spring Test在建立上下文前处理，选用测试配置；**它本身不会启动数据库**。本类的目标是MVC切片，不应连接真实MySQL。
- `@Autowired private MockMvc mockMvc`由Spring把测试进程内的MVC请求入口注入字段；不是学员自己`new MockMvc()`。`@MockitoBean private EmployeeService employeeService`由Spring Test在上下文中提供Service替身，满足Controller的构造器依赖。它与第六节普通Mockito的`mock(EmployeeMapper.class)`不同：后者只创建Java对象，不注册Spring Bean。
- `@Test`仍由JUnit负责发现并运行每个方法。先判断要测Controller的哪个HTTP契约，再为该方法准备输入和预期，不是给`@WebMvcTest`类里的所有方法套同一个返回值。

### 2. 需要业务返回或抛错时，先设定Mock Service

- `when(employeeService.create(any(EmployeeCreateRequest.class))).thenReturn(created)`使`createReturns201LocationAndEmployeeJson`中的Service替身返回指定DTO。`any(...)`匹配一个指定类型的请求对象，但**不核对其字段**；若输入字段本身是测试规格，仍要另行验证绑定结果。
- `when(employeeService.update(eq(1001L), any(EmployeeUpdateRequest.class))).thenReturn(...)`同时匹配路径id和请求对象。`eq(1001L)`要求id等于1001；与`any(...)`混用时，两个参数都使用Mockito匹配器。
- `when(...).thenThrow(new EmployeeNotFoundException(9999L))`和重复邮箱的`thenThrow(...)`让Service替身抛出业务异常，从而测试Controller及全局异常处理怎样生成404或409。它们**不证明真实Service会抛出该异常**；这一点由第六节Service测试负责。
- 字段校验失败或JSON无法解析时，预期请求在进入Service前就被拒绝，不需要为了得到400而给Mock Service设定返回值。

### 3. 构造并执行一次MVC请求

- `post("/employees")`、`get("/employees/1001")`、`put("/employees/1001")`、`delete("/employees/1001")`分别指定HTTP方法与URL。它们是`MockMvcRequestBuilders`的**静态方法**，完整文件顶部已逐个静态导入；若只复制测试方法而漏掉导入，Eclipse会报“方法未定义”。它们不是`MockMvc`对象或Controller的方法。[Spring Framework API](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/test/web/servlet/request/MockMvcRequestBuilders.html)列出了这些构建方法。
- 对有JSON请求体的POST、PUT，`.contentType(MediaType.APPLICATION_JSON)`声明请求体类型，`.content(requestJson)`放入实际JSON文本。DELETE示例没有请求体，就不需要机械添加这两个调用。
- `mockMvc.perform(请求构建器)`在测试进程内执行请求，让它经过MVC映射、JSON绑定、校验、Controller和异常处理；返回的结果供后续`andExpect(...)`检查。它不打开8080端口，也不是浏览器或Postman请求。

### 4. 用`andExpect`检查响应的不同部分

- `status().isCreated()`、`isOk()`、`isNoContent()`分别核对201、200、204；`isBadRequest()`、`isNotFound()`、`isConflict()`分别核对400、404、409。只检查状态码不能证明响应字段正确。
- `header().string("Location", "/employees/2001")`核对完整资源地址；`header().exists("X-Request-Id")`只证明请求标识头存在，不检查它的值，可与第11章Filter日志对照。
- `jsonPath("$.success").value(true)`与`jsonPath("$.data.id").value(2001)`检查JSON字段的具体值。`$`是根对象，`.data.id`沿层级取字段；`.exists()`只检查字段有值，不能替代有明确预期值的`.value(...)`。校验失败示例的`$.data.name`与`$.data.email`证明字段错误已写进响应，但若接口规格要求完整文案，还要检查值。
- 以上`status()`、`header()`、`jsonPath()`来自`MockMvcResultMatchers`，是传给`andExpect(...)`的**响应匹配器**；不要和上一小节构造请求的`post()`等方法混淆。更多匹配方法见[A12-1](../appendix/A12_01_testing_assertions_reference.md)。

### 5. 最后确认请求是否到达Service

- `verify(employeeService).create(any(EmployeeCreateRequest.class))`或`verify(employeeService).delete(1001L)`证明Controller调用了对应Service方法；这不检查真实业务结果或数据库写入。
- `verifyNoInteractions(employeeService)`要求整个请求期间没有调用Service。`invalidBodyReturns400BeforeServiceCall`中JSON语法合法、字段值不合约束；`malformedJsonReturns400`中JSON根本无法解析。两者都应在进入Service前返回400，但失败位置不同。
- `missingEmployeeReturns404`与`duplicateEmailReturns409`的重点是Mock抛出业务异常后，HTTP状态与响应是否按契约转换；不能仅凭这些测试断言数据库状态。

照这个顺序自己扩充`getExistingEmployeeReturns200`：先看已有`when(...).thenReturn(...)`，再在现有`mockMvc.perform(get(...))`链上增加`jsonPath("$.data.id").value(1001)`和`jsonPath("$.data.department").value("Sales")`，最后补`verify(employeeService).findById(1001L)`。运行单个方法并确认新断言通过。GET、POST、PUT、DELETE，以及合法输入、字段错误、解析错误和业务异常，都可以沿“准备替身→构造请求→执行→断言响应→必要时验证调用”的同一顺序编写；正常系、异常系和边界值只是输入与预期的选择，不是另一套注解或方法。

在Eclipse中单独运行`EmployeeControllerWebTest.java`。若实际返回404，先核对请求路径与Controller映射；若返回500，查看异常是否被既有全局处理器接住。此时无需启动正式应用；下一案例使用Postman时才需要启动类。


## 八、案例五：启动应用并用Postman测试真实接口

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

在Postman中新建请求，选择GET并输入URL，点击Send。保存状态码、响应Headers和响应JSON；再与数据库查询结果比较。

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

### 3. 异常系与清理

下面三项分别从“查不到记录”“违反字段约束”“违反业务/数据库唯一性”观察响应。不要只截取状态码，还要保存响应JSON和数据库核对结果。

| 场景 | HTTP方法与URL | 请求参数与请求体 | 预期状态码与响应 | 数据库核对 |
| --- | --- | --- | --- | --- |
| 员工不存在 | GET `http://localhost:8080/employees/9999` | Path参数`id=9999`；无Query参数、无请求体 | 404；`success=false`，消息指出员工不存在 | 目标id仍无记录 |
| 字段校验失败 | POST `http://localhost:8080/employees`，Header为`Content-Type: application/json` | 无Path/Query参数；把上述JSON的`name`改为`""`，其余字段保持合法 | 400；`success=false`，返回`name`字段错误 | 此邮箱仍只有第一次新增的1行 |
| 邮箱重复 | POST `http://localhost:8080/employees`，Header为`Content-Type: application/json` | 无Path/Query参数；再次发送上述完全相同的合法JSON | 409；`success=false`，消息说明邮箱已使用 | 此邮箱仍只有第一次新增的1行 |

按新增响应中的实际id发送DELETE `http://localhost:8080/employees/{id}`（把`{id}`替换为生成值）；无Query参数、无请求体，预期204且响应体为空。随后在个人练习数据库查询该id，预期0行。不要删除第9章的固定样例数据。Postman能够证明真实HTTP入口和当前运行环境的结果，但手工步骤不具备JUnit自动回归的速度和可重复性。

## 九、案例六：内部结合测试（内部結合テスト）——验证完整应用协作

前面已经分别验证各层，并用Postman观察过真实HTTP接口。现在复用案例二建立的专用MySQL测试库、`application-test.yml`和`test-data.sql`，用 `@SpringBootTest` 加载真实Controller、Service、Mapper XML和MySQL连接，不用Mock替换业务组件，验证它们之间的协作。因此本课程把它归为**内部结合测试**；虽然仍通过Eclipse的JUnit功能执行，但不是本课程定义的单体测试。

学员仍然直接右键运行 `EmployeeCrudIntegrationTest.java`。`@SpringBootTest` 不是另一个启动按钮，也不要求预先运行启动类；它只告诉当前JUnit测试建立完整Spring应用上下文。默认模拟Web环境不监听8080；MockMvc在测试进程内把请求交给Spring MVC，不经过真实浏览器和网络端口。因此本案例验证Spring应用内部调用链，**不等于前后端结合测试**。

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
    void duplicateEmailOnUpdateReturns409AndKeepsOriginalFields()
            throws Exception {
        String requestJson =
                "{\"name\":\"Tanaka Updated\","
                        + "\"department\":\"Support\","
                        + "\"email\":\"sato@example.com\"}";

        mockMvc.perform(put("/employees/1001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false));

        assertThat(jdbcTemplate.queryForObject(
                "SELECT name FROM employees WHERE id = ?",
                String.class, 1001L)).isEqualTo("Tanaka");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT department FROM employees WHERE id = ?",
                String.class, 1001L)).isEqualTo("Sales");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT email FROM employees WHERE id = ?",
                String.class, 1001L)).isEqualTo("tanaka@example.com");
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM employees", Integer.class);
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

新方法使用`@Sql`在方法前准备的两名员工：1001号邮箱为`tanaka@example.com`，1002号邮箱为`sato@example.com`。PUT试图把1001改成1002已占用的邮箱，预期409且`success=false`。随后`JdbcTemplate`直接读取1001号的姓名、部门和邮箱，分别确认仍为原值，最后确认总行数仍为2。**UPDATE失败不一定改变记录数量**，只检查COUNT不能证明原字段没有被部分改写；这也是内部结合测试中的失败路径验证，不改变现有CRUD业务规格。

### 1. 启动范围和测试数据

#### 1.1 启动测试上下文

| 注解 | 当前参数 | 可接受的值 | 触发时机与结果 |
| --- | --- | --- | --- |
| `@SpringBootTest` | 本章使用默认值 | 可指定配置类、属性和Web环境 | 通过SpringApplication创建完整应用上下文，默认不启动真实服务器 |
| `@AutoConfigureMockMvc` | 本章使用默认值 | 可配置过滤器、输出方式等 | 在完整上下文中创建MockMvc，默认加入应用过滤器 |
| `@ActiveProfiles("test")` | profile名 `test` | 一个或多个profile名 | 测试时激活test profile，读取 `application-test.yml` |
| `@Sql` | SQL脚本与执行阶段 | classpath脚本路径和 `Sql.ExecutionPhase` 枚举 | 在每个测试方法前重建表和初始数据 |

- `@SpringBootTest` 在JUnit运行该类时建立完整应用上下文；本例默认使用模拟Web环境，不监听8080。
- `@AutoConfigureMockMvc` 在完整上下文中创建 `MockMvc`，让请求在测试进程内经过Controller、Service、Mapper和数据库。
- `.andReturn()` 无参数，返回本次请求的 `MvcResult`；`getResponse().getHeader("Location")` 从模拟响应中取得实际生成的资源地址，后续操作不预设固定id。

```java
get("/employees/{id}", employeeId)
```

- `get("/employees/{id}", employeeId)` 中的 `{id}` 是URI变量位置，`employeeId` 是填入的实际值；Controller收到的是替换后的路径，例如 `/employees/1003`。

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

- `queryForObject()` 期待查询恰好返回一行、一个可转换为指定类型的值；`COUNT(*)` 在这里返回一行数值。
- `?` 由最后的 `employeeId` 参数绑定，避免把id拼入SQL字符串。
- `countEmployee()` 对理论上的 `null` 做保守处理，正常COUNT结果仍应是数值。

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

在Eclipse中右键 `EmployeeCrudIntegrationTest.java`，选择 **Run As → JUnit Test**。不需要先启动 `EmployeeManagementApiApplication`。确认新增、查询、修改和删除后的HTTP与数据库行数一致；重复邮箱PUT还须确认1001号原字段未变。单独运行任意测试方法也应通过，证明数据准备不依赖执行顺序。


数据库边界：测试脚本与第9章一样使用MySQL的 `DATETIME`、自增主键和唯一约束，因此能发现内存兼容数据库可能隐藏的差异。仍要在证据中记录MySQL版本、schema名、字符集和排序规则；本章的重复邮箱测试主要验证Service小写化后的冲突，不能自动代表所有生产数据与并发情况。

## 十、案例七：执行一次回归测试

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


## 十一、运行测试、读取失败并保存证据

### 1. 对照各案例的测试边界

完成上面的案例后，再比较它们的证明范围：

| 测试案例 | 实际加载的范围与工具 | 被替代的依赖 | 主要证明 | 日本项目中可能对应的测试阶段 |
| --- | --- | --- | --- | --- |
| `EmployeeResponseTest` | JUnit、AssertJ、普通Java对象 | 无 | 响应对象保存构造值 | 单体测试 |
| `EmployeeMapperTest` | `@MybatisTest`、Mapper XML、真实MySQL | 不Mock Mapper | 参数、映射、写入和数据库约束 | Mapper层测试；UT/IT按项目规定 |
| `EmployeeMapperH2Test`（A12-2） | `@MybatisTest`、Mapper XML、H2 | 不Mock Mapper；独立profile和脚本 | 核心规格在H2中的结果及与MySQL的差异 | Mapper层测试；UT/IT按项目规定 |
| `EmployeeServiceImplTest` | JUnit、Mockito、Service普通Java对象 | Mapper替身 | 业务分支、转换和Mapper调用 | 单体测试 |
| `EmployeeControllerWebTest` | `@WebMvcTest`、MockMvc、MVC切片 | `@MockitoBean` 提供Service替身 | URL、JSON、状态、响应头和响应字段 | Controller层单体测试 |
| Postman完整CRUD | Eclipse启动的真实应用、Postman | 不替换业务组件 | 外部HTTP请求与当前开发库中的结果 | 可用于接口结合测试，具体看测试目的 |
| `EmployeeCrudIntegrationTest` | `@SpringBootTest`、MockMvc、真实MySQL | 不替换业务组件；隔离开发库 | Controller→Service→Mapper→数据库→响应 | 后端内部结合测试 |

Service测试中的Mock调用不能证明SQL正确；Mapper测试通过也不能证明HTTP状态正确。范围小的测试定位快，完整集成测试覆盖协作链，但需要更多启动和数据准备。同一种JUnit或MockMvc工具可以服务不同阶段，UT/IT归类最终由验证范围与项目测试计划决定。

### 2. 逐类运行与读取失败

学习阶段直接运行当前测试文件，不通过默认ApplicationTests统一启动，也不要求先把所有测试一起执行：

| 顺序 | Eclipse中直接运行的文件 | 主要确认 |
| --- | --- | --- |
| 1 | `EmployeeResponseTest.java` | JUnit和AssertJ基本断言 |
| 2 | `EmployeeMapperTest.java` | Mapper XML、真实SQL和映射 |
| 3 | `EmployeeMapperH2Test.java`（A12-2） | H2环境中的相同核心Mapper规格；独立profile |
| 4 | `EmployeeServiceImplTest.java` | 隔离Mapper后的业务规则 |
| 5 | `EmployeeControllerWebTest.java` | MockMvc模拟请求与HTTP契约 |
| 6 | 启动类，不是测试文件 | 使用Postman手工验证真实接口 |
| 7 | `EmployeeCrudIntegrationTest.java` | `@SpringBootTest`下的完整协作 |

在Eclipse中可以按需要选择运行范围：

1. **单个方法**：在编辑器或Outline中选中目标`@Test`方法，右键选择 **Run As → JUnit Test**，先快速确认当前改动。
2. **整个测试类**：在Package Explorer中右键测试Java文件，选择 **Run As → JUnit Test**，确认同类其他方法未被破坏。
3. **测试包中的多个类**：右键目标测试包，选择 **Run As → JUnit Test**；正式回归还应按项目要求覆盖受影响的其他包。
4. **读取结果**：在JUnit视图核对运行、失败、错误和跳过数量；失败时选中方法，阅读Failure Trace、expected/actual及首个相关异常，不要直接改预期迁就错误。
5. **修复与留证**：修复后先重跑失败方法，再运行所属类和相关回归；记录测试类/方法、输入和前置数据、test或h2test profile、数据库schema、执行时间与结果，必要时保存JUnit视图及脱敏日志。

单体测试和内部结合测试都可以由Eclipse启动JUnit；IDE只是启动工具，不决定测试阶段。Postman用于开发中的手工接口确认，不能替代自动化断言、数据隔离和完整回归。

Maven测试只作为构建或CI自动执行的补充。若项目根目录有Maven Wrapper，可在包含`pom.xml`的目录运行下面的命令；它不是新人日常运行每个测试的必经步骤。命令行进程也必须取得`DB_PASSWORD`及实际需要的`TEST_DB_URL`，否则MySQL测试会失败，不应通过跳过测试来掩盖；H2测试继续使用自己的`h2test` profile。

在包含 `pom.xml` 的项目根目录执行：

```powershell
.\mvnw.cmd test
```

- `test` 编译并执行自动化测试，不启动Postman手工请求；查看实际运行的测试类和最终汇总，而不只看`BUILD SUCCESS`。
- 核对`Failures: 0`、`Errors: 0`和应运行测试的跳过数量；测试数量会随课程进度变化。MySQL连接失败时先修复环境，不跳过测试换取表面成功。

```text
BUILD SUCCESS
```

失败时按顺序阅读：

1. 先看失败的测试类和方法名。
2. 再区分 `Failure` 的断言不一致与 `Error` 的未预期异常。
3. 核对expected和actual，不要看到红色就先修改测试期望。
4. 使用第11章日志和堆栈定位实际失败层。
5. 修复后先在Eclipse重跑失败测试与相关回归；正式构建时再执行Maven测试。

Maven的详细结果位于：

```text
target/surefire-reports/
```

自测证据至少保留：Eclipse运行的测试范围或构建命令、代码版本或提交编号、测试数量、失败数、执行时间与必要报告。不要只截取 `BUILD SUCCESS` 一行，也不要上传包含密码、Token或个人数据的日志。

## 十二、常见失败与定位

| 现象 | 定位点 | 常见原因 | 修正 |
| --- | --- | --- | --- |
| `NoSuchBeanDefinitionException` | 失败的测试类和缺少类型 | Web切片未提供Controller依赖 | 使用 `@MockitoBean` 提供Service |
| 接口测试返404 | MockMvc请求路径与Controller映射 | URL或HTTP方法写错 | 对照接口规格和Controller注解 |
| 应返400却返201 | DTO约束与Controller参数 | 缺少约束或 `@Valid` | 先修正正式契约，不要改测试迁就错误实现 |
| `Table 'employees' doesn't exist` | active profile、DataSource URL和 `@Sql` | test profile未激活、连接错库或SQL脚本未执行 | 核对test profile、实际schema和classpath路径 |
| 单独运行通过，全部运行失败 | 测试间共享字段和数据 | 依赖执行顺序或留下脏数据 | 每个方法重建自己的前置状态 |
| 保护检查失败 | 数据库产品名和catalog | URL误指向非MySQL或非测试库 | 修正环境变量，禁止绕过保护继续执行 |

## 十三、日本项目测试规格、打键与Review练习

“打键测试”通常指测试人员按手顺实际操作并记录结果，不是JUnit的另一个名称。本项目没有前端画面，不能伪造画面打键证据；可用Postman手工发送接口请求，核对响应与数据库。手工操作能观察真实配置和人员使用流程，但不能代替高频回归的自动化；Service Mock测试也不能代替真实接口/数据库验证。

| 编号/对象 | 前置条件 | 输入与操作 | 预期结果 | 实际与判定 | 证据 |
| --- | --- | --- | --- | --- | --- |
| UT-M-01 Mapper查询 | 专用MySQL测试库存在员工1001 | `findById(1001)` | id、姓名、部门、邮箱均映射正确 | 运行后填写Pass/Fail | Eclipse JUnit结果 |
| UT-S-02 Service异常 | Mapper模拟返回null | `findById(9999)` | 抛 `EmployeeNotFoundException` | 运行后填写 | JUnit失败/成功记录 |
| IT-C-03 接口新增 | 测试库无目标邮箱 | Postman POST合法JSON | 201、Location、数据库新行 | 执行后填写 | Postman响应与数据库查询 |

规格书还应记录测试日期、环境和执行人；这里只给可复核的最小示例，不预填“通过”。正常系、异常系与边界值都要覆盖：Mapper可测存在/不存在、唯一约束；Service可测新增成功/非法部门；Controller可测201/400与姓名50/51字符。改修后根据影响范围选回归用例并保存Eclipse JUnit结果、Maven Surefire报告、Postman响应和数据库查询。Review时问：断言能否真正证明预期业务？是否只验证Mock调用、没有验证结果？测试数据是否独立？测试实际连接的是哪个数据库、什么版本和排序规则？

### 练习1：增加更新异常测试

前置：使用本章固定测试数据。自行实现PUT不存在id返404的集成测试，核对HTTP响应、员工1001和1002仍存在且总数为2；再扩展本章已有的PUT重复邮箱409测试，增加员工1002的姓名、部门和邮箱未变的断言。COUNT只能证明行数，不能代替字段状态检查。

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

在Eclipse中执行一次指定测试类和一次相关回归；项目要求正式构建时再补充Maven测试结果，整理下表：

| 项目 | 证据 |
| --- | --- |
| 实施内容 | 本次新增或修改的测试场景 |
| 执行方式 | Eclipse中运行的类/包；若执行Maven，再记实际命令 |
| 结果 | 测试总数、Failure、Error、Skipped |
| 失败与对应 | 失败方法、原因、修正和重跑结果；没有则写无 |
| 回归范围 | 根据影响调查选择了哪些测试 |
| 未验证项 | 例如MySQL特有行为、并发或未建立的权限功能 |

日本项目资料中常见的“正常系”是预期成功场景，“异常系”是预期拒绝或错误场景，“エビデンス”是能证明执行条件和结果的证据。名称不能代替具体输入、预期和实际结果。

## 十四、完成本章后的文件核对

本章不修改`src/main`下的Controller、Service、Mapper及XML。按照前面案例，`pom.xml`只增加Mapper与Web MVC测试依赖；测试资源和类应如下。每个文件的完整内容已在首次创建它的案例中给出，不需要到章末寻找另一份可能不同步的实现。

```text
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

第3章生成的仅有`contextLoads()`的`EmployeeManagementApiApplicationTests.java`按第五节说明删除；是否额外保留[A12-2 H2实验](../appendix/A12_02_h2_test_database.md)的测试文件与profile，由实验后的工程选择决定。保留时不要覆盖MySQL测试配置，也不要把H2通过记录写成MySQL证据。

## 十五、附录：测试扩展方向

以下内容不属于本章基本测试闭环：

需要查阅JUnit测试生命周期、常用注解、自带断言和参数化测试写法时，使用[A12-3 JUnit Jupiter常用注解与方法](../appendix/A12_03_junit_jupiter_annotations_methods.md)。

- **Mockito注解方式**：普通Service单元测试也可使用 `@ExtendWith(MockitoExtension.class)`、`@Mock` 和 `@InjectMocks` 自动初始化，但本章主线固定使用显式 `mock()` 加构造器。它不同于Web切片中用于替换Spring Bean的 `@MockitoBean`。
- **JUnit异常断言**：`assertThrows(EmployeeNotFoundException.class, () -> service.findById(9999L))` 也是常见写法，可取得异常对象继续检查消息；本章主线统一使用AssertJ的 `assertThatThrownBy()`。

- **高级Mockito技巧**：参数捕获、严格桩和复杂Answer应在基本Mock边界稳定后学习。
- **复杂参数化测试**：本章只要求能为明确边界编写独立测试；大量组合再考虑参数化组织。
- **Testcontainers**：可以用容器提供更接近MySQL的测试环境，但需要Docker、启动成本和数据生命周期管理。
- **H2实验的后续扩展**：本章已在Mapper案例后完成[A12-2](../appendix/A12_02_h2_test_database.md)的正常系与异常系对照。更多SQL方言、并发与事务差异仍需真实MySQL环境验证。
- **高级测试数据管理**：数据工厂、迁移工具和大规模场景需要独立规范。
- **覆盖率工具**：覆盖率用于发现未执行区域，不等于断言质量或业务完整性。
- **性能测试**：负载、容量和稳定性测试不属于JUnit功能测试的替代品。
- **测试上下文优化**：上下文缓存和切片组合属于测试规模扩大后的优化主题。

## 十六、本章稳定状态

完成后，正式代码和MySQL开发配置保持第11章状态，主线新增以下测试文件。A12-2的H2文件如果保留，始终使用`h2test` profile，不替换这里的MySQL测试资源：

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
2. 区分Mapper切片测试、Service单元测试、Controller Web切片测试和完整集成测试的证明范围；
3. 在Eclipse中直接运行每一个测试文件，并读取JUnit结果；
4. 分别在MySQL和H2运行Mapper正常系与异常系，说明两种证据不能互相代替；
5. 使用JUnit、AssertJ和Mockito验证Service正常与异常分支；用MockMvc验证Controller的成功、校验和业务错误响应；
6. 启动应用后使用Postman验证真实HTTP接口及数据库结果；
7. 最后使用`@SpringBootTest`验证Controller、Service、Mapper与MySQL的完整协作；
8. 根据改修影响选择直接测试、整套回归范围和自测证据。

下一章会在现有CRUD上增加“多步写入必须整体成功或整体失败”的事务边界，并使用自动化测试证明回滚结果。
