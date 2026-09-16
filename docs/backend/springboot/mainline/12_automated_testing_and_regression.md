# 第12章 用自动化测试保护员工CRUD

> 本章目标：把第10章的手工CRUD验收转换为可重复执行的Service单元测试、Web接口测试和数据库集成测试，并根据改修范围选择回归测试。

手工验证适合初次观察接口和数据库，但每次改修后都重新输入请求、查表并整理结果，容易漏掉旧功能。自动化测试把“前置条件、输入、操作、预期”保存成代码，修改后可再次执行。

```text
已确认的手工结果
  → 整理测试条件和预期
  → 选择Service、Web切片或数据库集成测试
  → 自动执行和断言
  → 保存失败信息与测试证据
```

## 一、从手工验收得到测试规格

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

## 二、完整示例

本章不修改 `src/main` 下的Controller、Service、Mapper和XML。按本节完成全部测试文件，再从第三节开始理解每个第一次出现的测试工具。

本章修改后的新增部分：

```text
employee-management-api/
├── pom.xml                                      ← 完整替换
└── src/test/
    ├── java/com/example/employee/
    │   ├── controller/EmployeeControllerWebTest.java
    │   ├── integration/EmployeeCrudIntegrationTest.java
    │   └── service/EmployeeServiceImplTest.java
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
        <version>3.5.16</version>
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
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>

        <dependency>
            <groupId>org.mybatis.spring.boot</groupId>
            <artifactId>mybatis-spring-boot-starter</artifactId>
            <version>3.0.5</version>
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
            <groupId>com.h2database</groupId>
            <artifactId>h2</artifactId>
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
    url: jdbc:h2:mem:employee_test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1
    username: sa
    password: ""
    driver-class-name: org.h2.Driver
  sql:
    init:
      mode: never

mybatis:
  mapper-locations: classpath:mapper/*.xml
  type-aliases-package: com.example.employee.entity

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
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
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

### 4. 新建EmployeeServiceImplTest.java

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
import com.example.employee.exception.EmployeeNotFoundException;
import com.example.employee.exception.InvalidDepartmentException;
import com.example.employee.mapper.EmployeeMapper;
import com.example.employee.service.impl.EmployeeServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
    void createRejectsInvalidDepartmentBeforeMapperCall() {
        EmployeeCreateRequest request = new EmployeeCreateRequest();
        request.setName("Suzuki");
        request.setDepartment("Unknown");
        request.setEmail("suzuki@example.com");

        assertThatThrownBy(() -> employeeService.create(request))
                .isInstanceOf(InvalidDepartmentException.class);
        verifyNoInteractions(employeeMapper);
    }
}
```

### 5. 新建EmployeeControllerWebTest.java

文件位置：

```text
src/test/java/com/example/employee/controller/EmployeeControllerWebTest.java
```

完整内容：

```java
package com.example.employee.controller;

import com.example.employee.dto.request.EmployeeCreateRequest;
import com.example.employee.dto.response.EmployeeResponse;
import com.example.employee.exception.EmployeeNotFoundException;
import com.example.employee.service.EmployeeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
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

### 6. 新建EmployeeCrudIntegrationTest.java

文件位置：

```text
src/test/java/com/example/employee/integration/EmployeeCrudIntegrationTest.java
```

完整内容：

```java
package com.example.employee.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
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
class EmployeeCrudIntegrationTest {

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
                        + "\"email\":\"TANAKA@example.com\"}";

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

## 三、三类测试分别证明什么

三类测试不是重复写同一件事，它们隔离的范围和可发现的问题不同：

| 类型 | 本章加载的范围 | 用什么替代外部侜用 | 主要证明 |
| --- | --- | --- | --- |
| Service单元测试 | `EmployeeServiceImpl` 普通Java对象 | Mockito创建的Mapper模拟对象 | 业务分支、转换和Mapper调用是否正确 |
| Web切片测试 | Spring MVC、Controller、校验和异常处理 | `@MockitoBean` 提供的Service | URL、HTTP方法、JSON转换、状态、响应头和响应字段 |
| 数据库集成测试 | 完整Spring应用、Mapper XML和测试数据库 | H2内存库替代开发MySQL | Controller→Service→Mapper→数据库→响应的整条链路 |

单元测试失败时范围小、定位快；集成测试能发现组件连接问题，但启动和准备更多。不要只写集成测试，也不要用全部Mock声称SQL已经通过。

## 四、测试依赖和目录

`spring-boot-starter-test` 的 `test` 作用域表示它只参与测试编译和执行，不作为正式运行依赖打包。本章直接使用其中的JUnit Jupiter、AssertJ、Mockito和Spring Test。Spring Boot的测试依赖组成可参考[Spring Boot官方测试说明](https://docs.spring.io/spring-boot/reference/testing/)。

H2是本章直接新增的测试数据库。它的版本由Spring Boot 3.5.16依赖管理确定，因此 `pom.xml` 不再手写版本。H2同样使用 `test` 作用域，不会替换正式运行的MySQL驱动。

Maven按目录分离两类代码：

```text
src/main/java       → 正式代码
src/main/resources  → 正式配置和Mapper XML
src/test/java       → 测试代码
src/test/resources  → 测试配置和测试SQL
```

测试代码可以引用正式代码；正式代码不应反向引用 `src/test` 中的类或配置。

## 五、JUnit怎样组织一次测试

### 1. @BeforeEach和@Test

`@BeforeEach` 和 `@Test` 都来自 `org.junit.jupiter.api`。

| 注解 | 使用位置 | 参数与可接受值 | 触发时机与结果 |
| --- | --- | --- | --- |
| `@BeforeEach` | 无返回值的测试准备方法 | 本章无参数 | 每个 `@Test` 前执行一次，重新创建Mapper模拟和Service |
| `@Test` | 测试方法 | 注解本身无参数 | JUnit发现并执行该方法；断言失败或抛出未预期异常则测试失败 |

方法名 `findByIdReturnsResponseWhenEmployeeExists` 直接表达“操作、预期、条件”。测试报告显示方法名时，可以立即知道哪个场景失败。

### 2. 准备、执行、断言

Service正常测试可分成三段：

```java
// 准备：创建数据并规定Mapper返回
when(employeeMapper.findById(1001L)).thenReturn(employee);

// 执行：调用被测方法
EmployeeResponse actual = employeeService.findById(1001L);

// 断言：比较实际结果和预期
assertThat(actual.getId()).isEqualTo(1001L);
```

“断言”就是自动判定条件是否成立。本章使用AssertJ：

| 断言 | 当前参数 | 可接受的值 | 返回与效果 |
| --- | --- | --- | --- |
| `assertThat(actual)` | 实际值 | AssertJ支持的对象或基本类型 | 返回对应断言对象，可继续连缀判定 |
| `.isEqualTo(expected)` | 预期值 | 与实际值可比较的值 | 不相等时抛出断言错误 |
| `.isZero()` | 无 | 无参数，实际值应为数字 | 实际值不是0时失败 |
| `.isNotBlank()` | 无 | 无参数，实际值应为字符串 | 实际值为 `null`、空字符串或纯空白时失败 |
| `assertThatThrownBy(call)` | 可执行且可抛出异常的Lambda | 不应为 `null` | 返回异常断言对象；没有抛出异常则失败 |
| `.isInstanceOf(type)` | 预期异常类的 `Class` 对象 | `Throwable` 子类 | 异常类型不匹配时失败 |
| `.hasMessage(text)` | 预期完整消息 | 字符串 | 异常消息不一致时失败 |

不应把所有字段都断言两遍，也不能只断言“结果不为 `null`”。选择能证明当前规格的关键值。

## 六、Mockito怎样隔离Mapper

`mock(EmployeeMapper.class)` 由Mockito根据Mapper接口创建模拟对象。它不连接数据库，只会按测试规定的方式回答。

```java
employeeMapper = mock(EmployeeMapper.class);
employeeService = new EmployeeServiceImpl(employeeMapper);
```

这里直接调用第4章已经实现的构造方法，把模拟Mapper传入Service。不需要启动Spring容器，也不需要用反射强行填充私有字段。这就是显式构造器依赖便于测试的实际价值。

| Mockito方法 | 当前参数 | 可接受的值 | 返回与效果 |
| --- | --- | --- | --- |
| `mock(type)` | `EmployeeMapper.class` | 可被Mockito模拟的类或接口 | 返回该类型的模拟对象 |
| `when(call).thenReturn(value)` | 一次模拟调用和预定返回值 | 返回值必须与方法类型兼容 | 之后相同调用返回指定值 |
| `verify(mock).method()` | 模拟对象和预期调用 | 方法参数应与期望一致 | 默认确认该调用恰好发生一次 |
| `verifyNoInteractions(mock)` | 一个或多个模拟对象 | Mockito mock | 只要发生任何调用就失败 |
| `any(type)` | `EmployeeCreateRequest.class` | Mockito能匹配的参数类型 | 返回Mockito参数匹配规则，用于预定或验证调用 |

部门规则失败的测试不只断言异常，还使用 `verifyNoInteractions(employeeMapper)` 证明数据访问没有发生。这比单纯检查错误消息更能证明业务边界。

## 七、Web切片测试怎样检查接口契约

### 1. @WebMvcTest和@MockitoBean

`@WebMvcTest(EmployeeController.class)` 用在测试类上，由Spring Boot Test处理。它只加载Spring MVC相关部分，包括指定Controller、Controller Advice、JSON转换、校验和过滤器，不加载真实Service和Mapper。官方的切片范围可参考[Spring Boot 3.5测试文档](https://docs.spring.io/spring-boot/3.5/reference/testing/spring-boot-applications.html#testing.spring-boot-applications.spring-mvc-tests)。

Controller构造方法需要 `EmployeeService`，因此测试使用：

```java
@MockitoBean
private EmployeeService employeeService;
```

`@MockitoBean` 来自Spring Test，写在测试字段上。它用Mockito mock覆盖测试容器中指定类型的Bean；字段类型决定目标，本章不填其他参数。Spring Boot 3.5官方示例也使用 `@WebMvcTest` 与 `@MockitoBean` 组合，不再使用已弃用的Boot `@MockBean`。

### 2. MockMvc请求与断言

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

`$.data.id` 中的 `$` 表示JSON根对象，`.data.id` 按层级进入字段。JSONPath只是测试中定位响应值的表达式，不会改变Controller的响应。

`jsonPath(path).exists()` 不填参数，断言路径定位的字段存在；`jsonPath(path).isNumber()` 不填参数，断言目标值是JSON数字。前者适合检查字段错误已返回，后者适合检查系统已生成数值id。

Web切片中Service返回值由Mockito预定，所以201测试能证明Controller契约，不能证明INSERT SQL正确。

## 八、完整应用测试怎样使用隔离数据库

### 1. 启动测试上下文

| 注解 | 当前参数 | 可接受的值 | 触发时机与结果 |
| --- | --- | --- | --- |
| `@SpringBootTest` | 本章使用默认值 | 可指定配置类、属性和Web环境 | 通过SpringApplication创建完整应用上下文，默认不启动真实服务器 |
| `@AutoConfigureMockMvc` | 本章使用默认值 | 可配置过滤器、输出方式等 | 在完整上下文中创建MockMvc，默认加入应用过滤器 |
| `@ActiveProfiles("test")` | profile名 `test` | 一个或多个profile名 | 测试时激活test profile，读取 `application-test.yml` |
| `@Sql` | SQL脚本与执行阶段 | classpath脚本路径和 `Sql.ExecutionPhase` 枚举 | 在每个测试方法前重建表和初始数据 |

Spring Boot 3.5中，`@SpringBootTest` 默认创建模拟Web环境而不监听8080；与 `@AutoConfigureMockMvc` 组合后可用MockMvc走完应用内部请求链。

新增接口的链式调用最后使用 `.andReturn()`，该方法无参数，返回 `MvcResult`。`MvcResult` 保存这次MockMvc请求的完整测试结果；`getResponse()` 返回模拟HTTP响应，再用 `getHeader("Location")` 取得指定响应头。这样后续步骤可以使用实际生成的id，而不是假定id必然为某个固定数字。

```java
get("/employees/{id}", employeeId)
```

路径中的 `{id}` 是MockMvc请求构建器的URI变量位置，后面的 `employeeId` 按顺序填入。返回给Controller的仍是实际路径字符串，例如 `/employees/1003`。

### 2. 为什么不连接开发数据库

`application-test.yml` 用H2内存数据库覆盖正式 `application.yml` 的MySQL连接。Web切片和完整集成测试都激活test profile；测试日志写入 `target/test-logs`，`clean` 时一起清理，不污染正式 `logs` 目录。

| H2 URL部分 | 作用 |
| --- | --- |
| `jdbc:h2:mem:employee_test` | 创建名为employee_test的内存数据库 |
| `MODE=MySQL` | 启用部分MySQL兼容行为 |
| `DATABASE_TO_LOWER=TRUE` | 将未加引号的名称归一为小写 |
| `DB_CLOSE_DELAY=-1` | 测试JVM运行期间保留内存库 |

测试绝不应使用开发者正在操作的MySQL schema，更不能指向生产环境。`test-data.sql` 会先 `DROP TABLE`，所以只能在这个专用内存库中执行。

H2的MySQL兼容模式不是MySQL 8.0本身。它能快速验证本章的基础CRUD，不能替代对MySQL特有SQL、排序规则、锁和事务行为的真实验证。

### 3. @Sql为什么在每个方法前执行

一个测试新增或删除员工后，不能把改变留给下一个测试。`BEFORE_TEST_METHOD` 让每个方法都从两条固定记录开始：

```text
方法A前：重建表和两条数据 → 执行A
方法B前：再次重建表和两条数据 → 执行B
```

因此测试不需要固定执行顺序。单独运行任意一个方法，结果也应与整类执行相同。

### 4. JdbcTemplate只用来核对数据库结果

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

## 九、怎样保证测试独立和可重复

可靠的自动化测试应满足：

1. 自己准备前置数据，不要求开发者先手工插入记录。
2. 不依赖另一个测试先执行。
3. 不连接真实业务库、外部API或个人环境数据。
4. 相同代码和环境下重复执行，结果相同。
5. 测试失败后不留下需要手工清理的共享状态。

`completeCrudKeepsHttpAndDatabaseConsistent()` 在一个测试方法内保留了“新增后才能修改这个新id”的业务顺序，但它不依赖其他 `@Test` 方法。

不要使用 `@TestMethodOrder` 来隐藏共享数据问题。只有规格本身要求多步过程时，才在同一测试方法中明确完成该过程。

## 十、运行、读取失败和保存证据

在包含 `pom.xml` 的项目根目录执行：

```powershell
.\mvnw.cmd clean test
```

`clean` 先删除旧的Maven构建输出，`test` 重新编译并执行测试。成功时应看到实际执行数量和：

```text
Tests run: 9, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

只运行一个测试类：

```powershell
.\mvnw.cmd "-Dtest=EmployeeServiceImplTest" test
```

`-Dtest` 是Maven Surefire接收的测试筛选属性；值填测试类名或支持的模式。PowerShell中将整个属性加引号，避免命令解析差异。它适合快速重跑失败类，不代表全部回归已通过。

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

## 十一、怎样根据改修选择回归范围

回归测试用于确认改修没有破坏原有行为。先做影响调查，再选择测试：

| 改修例 | 最小直接测试 | 相关回归 | 理由 |
| --- | --- | --- | --- |
| 修改部门业务规则 | Service单元测试 | POST和PUT的400/成功接口，完整CRUD | 新增和修改共用Service规则 |
| 修改请求DTO校验 | Web切片400测试 | POST、PUT正常与边界值 | JSON绑定和字段契约发生变化 |
| 修改Mapper XML | 数据库集成测试 | 使用同一Mapper的Service与接口 | Mock不能验证真实SQL和映射 |
| 修改全局异常处理 | Web切片异常测试 | 400、404、409、500以及405/415 | 错误响应和框架默认状态都可能受影响 |
| 只修改文案 | 对应响应断言 | 相同错误类型的前端契约 | 消息可能被调用方使用 |

正式提交前仍应执行项目规定的全部测试。“最小直接测试”用于开发中快速反馈，不是减少交付前回归范围的借口。

## 十二、常见失败与定位

| 现象 | 定位点 | 常见原因 | 修正 |
| --- | --- | --- | --- |
| `NoSuchBeanDefinitionException` | 失败的测试类和缺少类型 | Web切片未提供Controller依赖 | 使用 `@MockitoBean` 提供Service |
| 接口测试返404 | MockMvc请求路径与Controller映射 | URL或HTTP方法写错 | 对照接口规格和Controller注解 |
| 应返400却返201 | DTO约束与Controller参数 | 缺少约束或 `@Valid` | 先修正正式契约，不要改测试迁就错误实现 |
| `Table "EMPLOYEES" not found` | active profile、DataSource URL和 `@Sql` | test profile未激活或SQL脚本未执行 | 核对 `@ActiveProfiles("test")` 和classpath路径 |
| 单独运行通过，全部运行失败 | 测试间共享字段和数据 | 依赖执行顺序或留下脏数据 | 每个方法重建自己的前置状态 |
| H2通过但MySQL失败 | 失败SQL和两种数据库行为 | 使用了H2可接受但MySQL不一致的语法或语义 | 对MySQL特有行为增加真实MySQL测试环境 |

## 十三、规格理解、Review与练习

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

## 十四、本章稳定状态

完成后，正式代码和MySQL配置保持第11章状态，新增测试文件：

```text
pom.xml
src/test/
├── java/com/example/employee/
│   ├── controller/EmployeeControllerWebTest.java
│   ├── integration/EmployeeCrudIntegrationTest.java
│   └── service/EmployeeServiceImplTest.java
└── resources/
    ├── application-test.yml
    └── test-data.sql
```

此时你应能够：

1. 从手工验收中整理前置条件、输入、操作和预期；
2. 区分Service单元测试、Web切片测试和数据库集成测试的证明范围；
3. 使用JUnit、AssertJ和Mockito验证Service正常与异常分支；
4. 使用MockMvc验证请求、状态、响应头和JSON契约；
5. 使用test profile、H2和 `@Sql` 建立隔离、可重复的CRUD测试；
6. 阅读测试失败并保存可复核的自测证据；
7. 根据改修影响选择直接测试与回归范围。

下一章会在现有CRUD上增加“多步写入必须整体成功或整体失败”的事务边界，并使用自动化测试证明回滚结果。
