# A12-2 使用H2完成独立数据库测试实验

完成[第12章的MySQL Mapper案例](../mainline/12_automated_testing_and_regression.md)后，执行本附录，再回到第12章的Service案例。这里对同一组核心Mapper规格使用H2内存数据库做正常系与异常系测试；后续主线和第13章仍统一使用专用MySQL测试库。不要用本附录配置覆盖`application-test.yml`，也不要把“H2测试通过”写成“MySQL行为已经验证”。

## 一、这个实验解决什么问题

H2可以在测试JVM中临时创建数据库，测试结束后丢弃数据，适合快速演示数据库测试的组成：测试依赖、独立profile、建表脚本、Mapper与断言。这个案例仍加载MyBatis、Mapper XML和数据库，因此属于Mapper层切片/数据库测试，不是完全隔离依赖的纯单元测试。H2也不是MySQL本身，即使开启MySQL兼容模式，SQL方言、排序规则、自增、日期时间、锁和事务细节仍可能不同。

本实验的可检查结果是：`EmployeeMapperH2Test` 的已有记录查询、插入、查无记录和唯一约束冲突四种情况均符合预期；随后对照MySQL Mapper测试，指出两种结果各自能证明的范围。

## 二、从已完成MySQL Mapper案例的工程开始

先确认MySQL Mapper测试已经通过，再在同一工程中新增以下**测试范围**的文件。H2使用独立profile与SQL脚本，不更改`application-test.yml`或`test-data.sql`；下一阶段仍运行MySQL测试类。若不希望保留H2实验，也可以在独立工程副本中完成，回到原工程继续学习。

```text
pom.xml                                           ← 增加test作用域H2依赖
src/test/resources/application-h2test.yml         ← 新建
src/test/resources/h2-test-data.sql               ← 新建
src/test/java/com/example/employee/mapper/
└── EmployeeMapperH2Test.java                     ← 新建
```

## 三、增加H2测试依赖

在 `pom.xml` 的 `dependencies` 中增加：

```xml
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <scope>test</scope>
</dependency>
```

`scope` 为 `test` 表示该依赖只用于测试编译和测试执行，不进入正式运行依赖。版本由Spring Boot依赖管理统一决定，本课程不在这里单独固定H2版本。

## 四、建立独立H2 profile

新建 `src/test/resources/application-h2test.yml`：

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:employee_h2_test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1
    username: sa
    password: ""
    driver-class-name: org.h2.Driver
  sql:
    init:
      mode: never

mybatis:
  mapper-locations: classpath:mapper/*.xml
  type-aliases-package: com.example.employee.entity
  configuration:
    map-underscore-to-camel-case: true
```

| 配置 | 作用 |
| --- | --- |
| `jdbc:h2:mem:employee_h2_test` | 在当前JVM内创建名为 `employee_h2_test` 的内存数据库 |
| `MODE=MySQL` | 启用部分MySQL兼容行为，不等于真正MySQL |
| `DATABASE_TO_LOWER=TRUE` | 将未加引号的数据库对象名按小写处理 |
| `DB_CLOSE_DELAY=-1` | JVM存活期间保留内存数据库，方便同一测试上下文继续使用 |
| `spring.sql.init.mode: never` | 不让Spring Boot自动运行其他初始化脚本，本例由 `@Sql` 明确指定脚本 |

## 五、准备H2专用数据脚本

新建 `src/test/resources/h2-test-data.sql`：

```sql
DROP TABLE IF EXISTS employees;

CREATE TABLE employees (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    department VARCHAR(50) NOT NULL,
    email VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_employees_email UNIQUE (email)
);

INSERT INTO employees (
    id, name, department, email, status
) VALUES
    (1001, 'Tanaka', 'Sales', 'tanaka@example.com', 'ACTIVE'),
    (1002, 'Sato', 'Development', 'sato@example.com', 'ACTIVE');
```

脚本使用单独文件名，避免误把H2语法变化带回主线MySQL脚本。

## 六、编写包含正常系与异常系的Mapper测试

新建 `src/test/java/com/example/employee/mapper/EmployeeMapperH2Test.java`：

```java
package com.example.employee.mapper;

import com.example.employee.entity.Employee;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@MybatisTest
@AutoConfigureTestDatabase(
        replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("h2test")
@Sql(
        scripts = "/h2-test-data.sql",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class EmployeeMapperH2Test {

    @Autowired
    private EmployeeMapper employeeMapper;

    @Autowired
    private DataSource dataSource;

    @BeforeAll
    void requireInMemoryH2() throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            assertThat(connection.getMetaData().getDatabaseProductName())
                    .as("本实验只能连接H2")
                    .isEqualToIgnoringCase("H2");
            assertThat(connection.getMetaData().getURL())
                    .as("本实验只能连接H2内存库")
                    .startsWith("jdbc:h2:mem:");
        }
    }

    @Test
    void findByIdReadsPreparedEmployee() {
        Employee employee = employeeMapper.findById(1001L);

        assertThat(employee).isNotNull();
        assertThat(employee.getName()).isEqualTo("Tanaka");
        assertThat(employee.getDepartment()).isEqualTo("Sales");
    }

    @Test
    void insertPersistsNewEmployee() {
        Employee employee = new Employee();
        employee.setName("Suzuki");
        employee.setDepartment("Support");
        employee.setEmail("suzuki@example.com");
        employee.setStatus("ACTIVE");

        assertThat(employeeMapper.insert(employee)).isEqualTo(1);
        assertThat(employee.getId()).isNotNull();
        assertThat(employeeMapper.findById(employee.getId()).getEmail())
                .isEqualTo("suzuki@example.com");
    }

    @Test
    void missingEmployeeReturnsNull() {
        assertThat(employeeMapper.findById(9999L)).isNull();
    }

    @Test
    void duplicateEmailDoesNotAddRow() {
        Employee duplicate = new Employee();
        duplicate.setName("Another");
        duplicate.setDepartment("Sales");
        duplicate.setEmail("tanaka@example.com");
        duplicate.setStatus("ACTIVE");

        assertThatThrownBy(() -> employeeMapper.insert(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThat(employeeMapper.findList(null)).hasSize(2);
    }
}
```

`@MybatisTest`建立Mapper测试切片；`@AutoConfigureTestDatabase(replace = NONE)`保留`application-h2test.yml`的数据源；`@ActiveProfiles("h2test")`选择该配置；`@Sql`在每个方法前重建两条固定数据。`@Autowired`注入真实Mapper代理和数据源，`@Test`让JUnit执行每个场景。这里不能继承`MySqlTestGuard`，因为它应当拒绝H2。

`@BeforeAll`在本类测试方法执行前检查连接，防止`@Sql`的`DROP TABLE`误落到其他数据库。`@TestInstance(PER_CLASS)`让非静态的`@BeforeAll`能使用已经注入的`DataSource`。检查数据库产品名和`jdbc:h2:mem:` URL都通过后，才运行带破坏性DDL的脚本。不要删除这段保护来迁就配置错误。

正常系中，`isNotNull`只说明查到了对象，姓名和部门断言才核对结果映射；INSERT的`isEqualTo(1)`检查影响行数，重新查询检查数据确实可读。异常系中，`isNull`核对“查不到”的Mapper返回约定；`assertThatThrownBy`要求重复邮箱INSERT抛出数据库约束异常，`isInstanceOf(DataIntegrityViolationException.class)`检查Spring翻译后的约束异常类别，最后`hasSize(2)`确认没有多出记录。异常类型只说明H2的本次运行结果，不能推断MySQL所有约束行为。

## 七、运行与观察

在Eclipse中以JUnit Test运行`EmployeeMapperH2Test`。预期四个方法通过；分别单独运行一个正常系与一个异常系方法，结果也应相同。测试结束后无需在MySQL客户端清理数据，因为H2使用本测试进程的内存库。

如需观察保护作用，可以临时把`@ActiveProfiles("h2test")`改为不存在的profile，预期上下文建立失败或在保护检查处停止；**不得为了让测试变绿而删除保护**。恢复profile后重跑。不要并行运行共用同一内存库并反复重建表的测试。

| 规格 | MySQL主线测试 | H2对照测试 | 结论 |
| --- | --- | --- | --- |
| 1001号员工映射 | `findByIdMapsExistingEmployee` | `findByIdReadsPreparedEmployee` | 两种环境分别验证相同的核心字段 |
| 新增后可读取 | `insertPersistsAndGeneratesId` | `insertPersistsNewEmployee` | 各自验证本数据库中的生成id和持久化 |
| 不存在编号 | `findByIdReturnsNullWhenEmployeeDoesNotExist` | `missingEmployeeReturnsNull` | 两者均验证Mapper返回`null` |
| 重复邮箱 | `duplicateEmailViolatesConstraint` | `duplicateEmailDoesNotAddRow` | 两者均拒绝重复值，但异常细节、排序规则及并发行为不能相互替代 |

H2的`MODE=MySQL`只提供部分兼容性。提交或Review证据中分别注明测试类、数据库产品、profile、通过/失败场景；MySQL方言、字符集、排序规则、自增与事务相关结论仍以主线真实MySQL测试为准。

## 八、完成实验后怎样处理

完成后回到[第12章Service案例](../mainline/12_automated_testing_and_regression.md)，继续使用MySQL主线的`test` profile。H2四处测试范围改动可以保留，前提是独立运行MySQL Mapper测试仍通过；如果课程工程不保留H2实验，则只移除H2依赖、`application-h2test.yml`、`h2-test-data.sql`和`EmployeeMapperH2Test.java`，不要误删MySQL测试资源。进入第13章前确认MySQL测试类仍能运行。

完成后应能说明：

1. H2为什么启动快、数据为什么不会写入MySQL；
2. profile、数据源、`@Sql`脚本和Mapper测试怎样协作；
3. 为什么H2的MySQL兼容模式不能证明真实MySQL行为；
4. 为什么后续主线仍以专用MySQL测试库为准，而H2是独立对照实验。

## 参考资料

- [H2官方Features：内存库与MySQL兼容模式](https://h2database.com/html/features.html)
- [Spring Framework：在测试中使用@Sql执行脚本](https://docs.spring.io/spring-framework/reference/testing/testcontext-framework/executing-sql.html)
- [Spring Boot：测试模块与Web切片](https://docs.spring.io/spring-boot/reference/testing/test-modules.html)
