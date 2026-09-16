# 第13章 用事务保证多步写入一致

> 本章目标：把“修改员工”扩展为“修改员工并写入变更履历”，使用Spring事务保证两次写入全部成功或全部回滚，并用数据库状态和自动化测试证明结果。

## 一、一个业务动作可能包含多次SQL

现在的PUT只修改 `employees` 一张表。业务规格增加一项要求：

```text
修改员工
  1. UPDATE employees
  2. INSERT employee_change_logs
```

变更履历用于回答“哪个员工发生了什么操作”。本章还没有登录用户，因此不虚构操作人字段；后续认证功能建立后再扩展。

两条SQL分别都是原子操作，但整个业务动作仍可能半完成：

```text
UPDATE成功
  → INSERT履历失败
  → 如果没有共同事务，员工已改但没有履历
```

本章的验收条件：

| 场景 | employees | employee_change_logs | HTTP |
| --- | --- | --- | ---: |
| 两次写入成功 | 保存新值 | 新增1条 | 200 |
| 第二次写入失败 | 恢复旧值 | 不新增 | 500 |

## 二、完整示例

本章保留第12章的接口契约和测试配置，增加一张表和一个Mapper，并替换Service实现与直接受影响的测试文件。

```text
src/main/java/com/example/employee/
├── entity/EmployeeChangeLog.java                  ← 新建
├── mapper/EmployeeChangeLogMapper.java            ← 新建
└── service/impl/EmployeeServiceImpl.java          ← 完整替换

src/main/resources/mapper/
└── EmployeeChangeLogMapper.xml                     ← 新建

src/test/
├── java/com/example/employee/
│   ├── integration/EmployeeCrudIntegrationTest.java  ← 完整替换
│   └── service/EmployeeServiceImplTest.java          ← 完整替换
└── resources/test-data.sql                         ← 完整替换
```

### 1. 在MySQL 8.0创建履历表

只在个人练习用的 `employee_db` 执行下面DDL；先确认当前连接的不是共享或生产数据库。

```sql
USE employee_db;

CREATE TABLE employee_change_logs (
    id BIGINT NOT NULL AUTO_INCREMENT,
    employee_id BIGINT NOT NULL,
    action VARCHAR(30) NOT NULL,
    detail VARCHAR(200) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_employee_change_logs PRIMARY KEY (id),
    INDEX idx_employee_change_logs_employee_id (employee_id)
);
```

| 字段 | 类型 | 必填 | 生成者 | 本章含义 |
| --- | --- | --- | --- | --- |
| `id` | BIGINT | 是 | MySQL | 履历主键 |
| `employee_id` | BIGINT | 是 | Service | 发生变更的员工编号 |
| `action` | VARCHAR(30) | 是 | Service | 固定操作类型 `EMPLOYEE_UPDATED` |
| `detail` | VARCHAR(200) | 是 | Service | 仅记录部门由旧值变为新值，不记姓名或邮箱 |
| `created_at` | DATETIME | 是 | MySQL | 写入履历的时间 |

本表暂不建外键。员工目前使用物理删除，而履历可能需要在员工删除后保留；是否使用外键、限制删除或逻辑删除应由正式数据保留规格决定。

`INDEX idx_employee_change_logs_employee_id (employee_id)` 为员工编号建立普通索引，便于以后按员工查找履历。索引会占用存储空间，也会增加写入时的维护成本，因此应根据实际查询条件建立，不能给每个字段都机械加索引。

### 2. 新建EmployeeChangeLog.java

`EmployeeChangeLog` 是履历表一行数据在Java中的表示，职责与第9章的 `Employee` 相同，但对应的是另一张表。它不是接口请求DTO：Controller不会直接接收它，Service根据已经确认的业务结果创建它，再交给Mapper保存。

```java
package com.example.employee.entity;

public class EmployeeChangeLog {

    private Long id;
    private Long employeeId;
    private String action;
    private String detail;

    public EmployeeChangeLog(
            Long employeeId,
            String action,
            String detail) {
        this.employeeId = employeeId;
        this.action = action;
        this.detail = detail;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getEmployeeId() {
        return employeeId;
    }

    public String getAction() {
        return action;
    }

    public String getDetail() {
        return detail;
    }
}
```

### 3. 新建EmployeeChangeLogMapper.java

这里复用第9章已经学过的Mapper模式：接口声明数据库操作，MyBatis根据方法名和XML中的 `id` 找到SQL。`insert()` 返回受影响行数，Service据此确认履历是否真的写入一行。

```java
package com.example.employee.mapper;

import com.example.employee.entity.EmployeeChangeLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface EmployeeChangeLogMapper {

    int insert(EmployeeChangeLog changeLog);
}
```

### 4. 新建EmployeeChangeLogMapper.xml

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper
        PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "https://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.example.employee.mapper.EmployeeChangeLogMapper">

    <insert id="insert"
            parameterType="com.example.employee.entity.EmployeeChangeLog"
            useGeneratedKeys="true"
            keyProperty="id">
        INSERT INTO employee_change_logs (
            employee_id,
            action,
            detail
        ) VALUES (
            #{employeeId},
            #{action},
            #{detail}
        )
    </insert>

</mapper>
```

### 5. 完整替换EmployeeServiceImpl.java

```java
package com.example.employee.service.impl;

import com.example.employee.dto.request.EmployeeCreateRequest;
import com.example.employee.dto.request.EmployeeUpdateRequest;
import com.example.employee.dto.response.EmployeeListItemResponse;
import com.example.employee.dto.response.EmployeeResponse;
import com.example.employee.entity.Employee;
import com.example.employee.entity.EmployeeChangeLog;
import com.example.employee.exception.DuplicateEmailException;
import com.example.employee.exception.EmployeeNotFoundException;
import com.example.employee.exception.EmployeeSystemException;
import com.example.employee.exception.InvalidDepartmentException;
import com.example.employee.mapper.EmployeeChangeLogMapper;
import com.example.employee.mapper.EmployeeMapper;
import com.example.employee.service.EmployeeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class EmployeeServiceImpl implements EmployeeService {

    private static final String ACTION_EMPLOYEE_UPDATED =
            "EMPLOYEE_UPDATED";

    private static final Logger log =
            LoggerFactory.getLogger(EmployeeServiceImpl.class);

    private final EmployeeMapper employeeMapper;
    private final EmployeeChangeLogMapper employeeChangeLogMapper;

    public EmployeeServiceImpl(
            EmployeeMapper employeeMapper,
            EmployeeChangeLogMapper employeeChangeLogMapper) {
        this.employeeMapper = employeeMapper;
        this.employeeChangeLogMapper = employeeChangeLogMapper;
    }

    @Override
    public EmployeeResponse findById(Long id) {
        return toResponse(findEmployeeOrThrow(id));
    }

    @Override
    public List<EmployeeListItemResponse> findList(String department) {
        String filter = normalizeDepartmentFilter(department);
        List<Employee> employees = employeeMapper.findList(filter);
        List<EmployeeListItemResponse> responses = new ArrayList<>();
        for (Employee employee : employees) {
            responses.add(toListItemResponse(employee));
        }
        log.debug(
                "employee_list_completed filterApplied={} resultCount={}",
                filter != null,
                responses.size());
        return responses;
    }

    @Override
    @Transactional
    public EmployeeResponse create(EmployeeCreateRequest request) {
        validateDepartment(request.getDepartment());
        Employee employee = new Employee();
        employee.setName(request.getName().trim());
        employee.setDepartment(request.getDepartment().trim());
        employee.setEmail(normalizeEmail(request.getEmail()));
        employee.setStatus("ACTIVE");
        try {
            int affectedRows = employeeMapper.insert(employee);
            if (affectedRows != 1 || employee.getId() == null) {
                throw new EmployeeSystemException("新增员工结果异常");
            }
        } catch (DuplicateKeyException exception) {
            throw new DuplicateEmailException(employee.getEmail());
        }
        log.info(
                "employee_created employeeId={} department={}",
                employee.getId(),
                employee.getDepartment());
        return findById(employee.getId());
    }

    @Override
    @Transactional
    public EmployeeResponse update(
            Long id,
            EmployeeUpdateRequest request) {
        validateDepartment(request.getDepartment());
        Employee beforeUpdate = findEmployeeOrThrow(id);

        Employee employee = new Employee();
        employee.setId(id);
        employee.setName(request.getName().trim());
        employee.setDepartment(request.getDepartment().trim());
        employee.setEmail(normalizeEmail(request.getEmail()));

        try {
            int affectedRows = employeeMapper.update(employee);
            if (affectedRows > 1) {
                throw new EmployeeSystemException("修改员工影响多行");
            }
            if (affectedRows == 0
                    && employeeMapper.findById(id) == null) {
                throw new EmployeeNotFoundException(id);
            }
        } catch (DuplicateKeyException exception) {
            throw new DuplicateEmailException(employee.getEmail());
        }

        EmployeeChangeLog changeLog = new EmployeeChangeLog(
                id,
                ACTION_EMPLOYEE_UPDATED,
                "department=" + beforeUpdate.getDepartment()
                        + "->" + employee.getDepartment());
        int logRows = employeeChangeLogMapper.insert(changeLog);
        if (logRows != 1 || changeLog.getId() == null) {
            throw new EmployeeSystemException("员工变更履历写入失败");
        }

        log.info(
                "employee_updated employeeId={} department={}",
                id,
                employee.getDepartment());
        return findById(id);
    }

    @Override
    public void delete(Long id) {
        int affectedRows = employeeMapper.deleteById(id);
        if (affectedRows == 0) {
            throw new EmployeeNotFoundException(id);
        }
        if (affectedRows != 1) {
            throw new EmployeeSystemException("删除员工影响多行");
        }
        log.info("employee_deleted employeeId={}", id);
    }

    private Employee findEmployeeOrThrow(Long id) {
        Employee employee = employeeMapper.findById(id);
        if (employee == null) {
            throw new EmployeeNotFoundException(id);
        }
        return employee;
    }

    private EmployeeResponse toResponse(Employee employee) {
        return new EmployeeResponse(
                employee.getId(),
                employee.getName(),
                employee.getDepartment(),
                employee.getEmail());
    }

    private EmployeeListItemResponse toListItemResponse(
            Employee employee) {
        return new EmployeeListItemResponse(
                employee.getId(),
                employee.getName(),
                employee.getDepartment());
    }

    private void validateDepartment(String department) {
        if (!"Sales".equals(department)
                && !"Development".equals(department)
                && !"Support".equals(department)) {
            throw new InvalidDepartmentException(department);
        }
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeDepartmentFilter(String department) {
        if (department == null || department.isBlank()) {
            return null;
        }
        return department.trim();
    }
}
```

### 6. 完整替换EmployeeServiceImplTest.java

```java
package com.example.employee.service;

import com.example.employee.dto.request.EmployeeCreateRequest;
import com.example.employee.dto.response.EmployeeResponse;
import com.example.employee.entity.Employee;
import com.example.employee.exception.EmployeeNotFoundException;
import com.example.employee.exception.InvalidDepartmentException;
import com.example.employee.mapper.EmployeeChangeLogMapper;
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
    private EmployeeChangeLogMapper employeeChangeLogMapper;
    private EmployeeServiceImpl employeeService;

    @BeforeEach
    void setUp() {
        employeeMapper = mock(EmployeeMapper.class);
        employeeChangeLogMapper = mock(EmployeeChangeLogMapper.class);
        employeeService = new EmployeeServiceImpl(
                employeeMapper,
                employeeChangeLogMapper);
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
        verifyNoInteractions(employeeChangeLogMapper);
    }

    @Test
    void findByIdThrowsWhenEmployeeDoesNotExist() {
        when(employeeMapper.findById(9999L)).thenReturn(null);

        assertThatThrownBy(() -> employeeService.findById(9999L))
                .isInstanceOf(EmployeeNotFoundException.class)
                .hasMessage("员工不存在：9999");
        verify(employeeMapper).findById(9999L);
        verifyNoInteractions(employeeChangeLogMapper);
    }

    @Test
    void createRejectsInvalidDepartmentBeforeMapperCall() {
        EmployeeCreateRequest request = new EmployeeCreateRequest();
        request.setName("Suzuki");
        request.setDepartment("Unknown");
        request.setEmail("suzuki@example.com");

        assertThatThrownBy(() -> employeeService.create(request))
                .isInstanceOf(InvalidDepartmentException.class);
        verifyNoInteractions(employeeMapper, employeeChangeLogMapper);
    }
}
```

### 7. 完整替换test-data.sql

```sql
DROP TABLE IF EXISTS employee_change_logs;
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

CREATE TABLE employee_change_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    action VARCHAR(30) NOT NULL,
    detail VARCHAR(200) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
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

### 8. 完整替换EmployeeCrudIntegrationTest.java

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

    @Test
    void updateCommitsEmployeeAndChangeLogTogether()
            throws Exception {
        String updateJson =
                "{\"name\":\"Tanaka\","
                        + "\"department\":\"Development\","
                        + "\"email\":\"tanaka@example.com\"}";

        mockMvc.perform(put("/employees/1001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson))
                .andExpect(status().isOk());

        String department = jdbcTemplate.queryForObject(
                "SELECT department FROM employees WHERE id = 1001",
                String.class);
        Integer logCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM employee_change_logs "
                        + "WHERE employee_id = 1001 "
                        + "AND action = 'EMPLOYEE_UPDATED'",
                Integer.class);
        assertThat(department).isEqualTo("Development");
        assertThat(logCount).isEqualTo(1);
    }

    @Test
    void updateRollsBackWhenChangeLogInsertFails()
            throws Exception {
        jdbcTemplate.execute("DROP TABLE employee_change_logs");
        String updateJson =
                "{\"name\":\"Tanaka\","
                        + "\"department\":\"Development\","
                        + "\"email\":\"tanaka@example.com\"}";

        mockMvc.perform(put("/employees/1001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson))
                .andExpect(status().isInternalServerError());

        String department = jdbcTemplate.queryForObject(
                "SELECT department FROM employees WHERE id = 1001",
                String.class);
        assertThat(department).isEqualTo("Sales");
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

`DROP TABLE` 只在第12章创建的专用H2内存库中制造第二步失败，不得把这段测试连接到开发MySQL或共享环境。`@Sql` 会在下一个测试方法前重建两张表。

`JdbcTemplate.execute(String sql)` 直接执行不需要返回结果集的SQL。本例用它删除测试库中的履历表，是为了稳定地制造“第一条UPDATE成功、第二条INSERT失败”；它不是业务代码，也不是生产环境的删表方案。其余测试中使用的 `queryForObject()` 则会执行查询并把单个结果转换为指定Java类型。

完整代码中的几个选择都来自业务规格：

- `beforeUpdate` 在UPDATE前保存旧记录，因为履历需要同时写出旧部门和新部门；更新后再查已经拿不到旧值。
- `ACTION_EMPLOYEE_UPDATED` 是不会随对象变化的固定操作代码，因此声明为 `private static final` 常量，避免在多处散写容易拼错的字符串。
- 履历INSERT必须恰好影响一行，并且MyBatis必须回填主键；任一条件不满足都作为系统异常抛出，不能假装接口成功。
- `created_at` 由数据库生成，本章写入后不立即返回履历详情，所以 `EmployeeChangeLog` 暂时不增加没有被代码使用的 `createdAt` 属性。
- 成功响应仍通过 `findById(id)` 读取数据库最终状态，保持第10章已经确定的响应规则。

## 三、事务、提交和回滚

事务是数据库对一组操作的一致性边界：

```text
事务开始
  → UPDATE employees
  → INSERT employee_change_logs
  ├─ 方法正常结束：commit，两次写入同时可见
  └─ 出现需回滚异常：rollback，撤销两次写入
```

commit是确认当前事务的改变；rollback是撤销当前事务中尚未提交的改变。回滚不是把数据库恢复到任意历史时间，也不能撤销事务开始前已提交的数据。

## 四、为什么事务边界放在Service

Controller只知道HTTP请求，单个Mapper只知道自己的SQL。只有Service知道“修改员工和写履历是一个业务动作”：

```text
Controller
  → EmployeeServiceImpl.update()  ← 事务边界
      ├─ EmployeeMapper.update()
      └─ EmployeeChangeLogMapper.insert()
```

把 `@Transactional` 写在两个Mapper方法上会把一个业务动作拆成两个边界；写在Controller则会让HTTP层承担业务一致性。

`delete()` 目前只执行一条DELETE，单条SQL由数据库保证原子性，本章不因为“写方法”就机械加注解。`create()` 在INSERT后还会回查刚创建的员工，因此本章也把新增与回查放在一个事务内。

## 五、@Transactional由谁处理

`@Transactional` 的完整名称是 `org.springframework.transaction.annotation.Transactional`。它是事务元数据，不是Java编译器直接执行的commit命令。

Spring Boot根据已有DataSource和事务相关依赖配置事务管理器。Spring在Service Bean外部创建代理：

```text
Controller调用Service Bean
  → 事务代理读取@Transactional
  → 开始或加入事务
  → 调用真实EmployeeServiceImpl.update()
  → 根据返回或异常提交/回滚
```

本章把注解写在具体实现类的 `public` 方法上，并从Controller注入的Service Bean调用。这是容易识别且符合默认代理模式的写法。Spring Framework 6.2对注解位置和代理调用的说明可参考[Spring官方文档](https://docs.spring.io/spring-framework/reference/6.2/data-access/transaction/declarative/annotations.html)。

MyBatis-Spring使Mapper使用与Spring事务管理器对应的DataSource和SqlSession。同一事务中的两个Mapper调用因此可共同提交或回滚，业务代码不能手工调用 `SqlSession.commit()`、`rollback()` 或 `close()`。详见[MyBatis-Spring事务文档](https://mybatis.org/spring/transactions.html)。

## 六、正常结束与异常回滚

`@Transactional` 的默认回滚规则：

| 方法离开方式 | 默认结果 |
| --- | --- |
| 正常 `return` | commit |
| 抛出 `RuntimeException` 或其子类 | rollback |
| 抛出 `Error` | rollback |
| 抛出受检 `Exception` | 默认不回滚 |

`EmployeeSystemException`、`DuplicateEmailException` 和MyBatis-Spring转换后的 `DataAccessException` 都是运行时异常，向上抛出时会触发默认回滚。默认规则和自定义回滚规则可参考[Spring回滚规则官方说明](https://docs.spring.io/spring-framework/reference/6.2/data-access/transaction/declarative/rolling-back.html)。

如果业务方法确实会抛出受检异常，应按规格明确回滚类型。下面是规则片段，不加入当前项目：

```java
@Transactional(rollbackFor = java.io.IOException.class)
public void importEmployees(java.nio.file.Path file)
        throws java.io.IOException {
    // 读取文件并写入数据库
}
```

`java.nio.file.Path` 表示文件系统路径，`java.io.IOException` 表示文件读写可能发生的受检异常。这里写完整类名是为了把重点放在回滚规则上；它们不是本章员工项目新增的业务类型。

| 属性 | 当前值 | 可接受的值 | 默认值与作用 |
| --- | --- | --- | --- |
| `rollbackFor` | `java.io.IOException.class` | `Throwable` 子类的Class数组 | 默认空数组；增加必须回滚的异常类型 |

不要无理由统一写 `rollbackFor = Exception.class`。先区分当前方法会抛出哪种异常、失败后是应回滚还是允许提交，再定义规则。

## 七、为什么吞掉异常会误提交

下面是错误片段：

```java
try {
    employeeChangeLogMapper.insert(changeLog);
} catch (DataAccessException exception) {
    log.warn("change_log_failed");
}
```

捕获后方法继续正常结束，事务代理看不到异常，前面的employees更新就可能被提交。本章让 `DataAccessException` 继续向外抛出，交给事务代理回滚，再由第11章的全局异常处理器记录并返回500。

如果必须在当前层转换异常，应抛出能触发回滚的新异常，不能只写日志后继续返回成功。

## 八、同类内部调用为什么会绕过代理

默认代理模式只能拦截经过Spring代理的外部调用。下面的 `updateWithoutTransaction()` 如果本身没有事务，它在同一对象内调用 `update()` 时不会再经过外层代理：

```java
public EmployeeResponse updateWithoutTransaction(
        Long id,
        EmployeeUpdateRequest request) {
    return update(id, request);
}
```

因此不要依赖“同类内部调用带注解方法”来启动事务。可以把完整业务边界放在对外的Service方法，或将需要独立事务的职责拆到另一个Bean。

本章 `create()` 和 `update()` 已经由Controller经Service代理调用。它们内部调用 `findById()` 不需要新建第二个事务；查询会在外层已开始的事务中执行。

## 九、默认传播与已有事务

`@Transactional` 默认使用 `Propagation.REQUIRED`：

```text
调用时没有事务 → 开始新事务
调用时已有事务 → 加入已有事务
```

这适合“一个Service业务边界组织多个数据访问”的常见调用。Spring对REQUIRED的完整语义见[官方传播文档](https://docs.spring.io/spring-framework/reference/6.2/data-access/transaction/declarative/tx-propagation.html)。

`REQUIRES_NEW` 会暂停外层事务并使用独立物理事务，它可能额外占用数据库连接；`NESTED` 通常依赖JDBC保存点实现局部回滚。它们都不符合本章“两次写入共同成败”的规格，因此只需能识别，不替换默认REQUIRED。

## 十、隔离、只读和超时

| 属性 | 当前章节 | 可接受的值 | 默认值与影响 |
| --- | --- | --- | --- |
| `propagation` | 未显式填写 | `Propagation` 枚举 | `REQUIRED`；新建或加入已有事务 |
| `isolation` | 未显式填写 | `Isolation` 枚举 | `DEFAULT`；使用底层数据库默认隔离级别 |
| `readOnly` | 未显式填写 | `true` 或 `false` | `false`；当前事务允许写入 |
| `timeout` | 未显式填写 | 秒整数 | 使用事务系统默认值；超时支持受事务管理器影响 |

隔离级别控制并发事务之间能观察到哪些未完成或已完成的变化。本章没有一个需要改变MySQL默认隔离级别的规格，因此保留 `DEFAULT`。

`readOnly = true` 是给事务基础设施的只读提示，不应当成拦截所有写SQL的安全权限。`timeout` 用来限制事务最长时间，不是HTTP客户端超时，也不代替SQL性能优化。

## 十一、事务、锁和唯一约束解决不同问题

| 机制 | 当前项目用途 | 不能单独解决 |
| --- | --- | --- |
| 事务 | 保证员工更新和履历写入共同成败 | 不自动防止两个请求同时改同一行 |
| 数据库锁 | 协调并发读写对行或表的访问 | 不代替“邮箱不重复”规则 |
| 唯一约束 | 从数据库最终拒绝重复邮箱 | 不负责撤销同一业务中的其他SQL |

第10章已经使用唯一约束保护email；本章的事务让该约束失败、履历SQL失败等运行时异常可以撤销当前业务动作中的所有写入。

## 十二、并发更新为什么可能覆盖别人的修改

本节属于“设计识读与SQL机制实验”，用于理解既存项目中的并发控制思路。它不会完整修改Employee主线，也不会要求把version字段永久加入后续章节。示例中的Mapper、DTO和异常代码用于说明关键设计点，不构成一套可直接复制运行的完整功能。

本节要理解的是：Lost Update、version条件更新、`affectedRows = 0`、409 Conflict、`SELECT ... FOR UPDATE`，以及事务与锁解决的问题为什么不同。不要在缺少Version DTO、Entity字段、完整Mapper、Controller、异常处理和集成测试时，把下面的片段当成第二套可运行CRUD。

事务能保证一次请求中的多条SQL共同提交或回滚，但不能自动阻止两个已登录用户几乎同时修改同一名员工。假设员工1001初始数据是：

```text
department = Sales
email = tanaka@example.com
```

A和B同时打开第10章的完整更新画面。A把department从Sales改为Development并先提交成功。B只想把email改成 `tanaka.new@example.com`，但B随后提交的仍是旧画面中的完整PUT：

```text
department = Sales
email = tanaka.new@example.com
```

B的UPDATE会把A已经提交的Development重新覆盖为Sales。这就是Lost Update：两次请求都成功、每次事务也都完整，却丢掉了A的结果。

主线暂不改变 `employees` 表和既有接口。下面只展示设计中的关键SQL和判断；若自行做数据库实验，理解后必须恢复到本章开始时的稳定状态。

### 1. 乐观锁：更新时确认版本没有变化

乐观锁适合冲突不频繁、不能长时间占用数据库锁的画面编辑。先为实验表增加版本号：

```sql
ALTER TABLE employees
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
```

读取员工时把version一并返回给编辑画面；提交时同时带回id和旧version。Mapper的关键SQL如下：

```xml
<update id="updateWithVersion">
    UPDATE employees
    SET name = #{name},
        email = #{email},
        department = #{department},
        status = #{status},
        version = version + 1,
        updated_at = CURRENT_TIMESTAMP
    WHERE id = #{id}
      AND version = #{version}
</update>
```

如果别人已经更新，数据库中的version会先增加，本次UPDATE影响行数就是0。Service必须把0行转换为“数据已被其他用户更新，请重新读取后再操作”这类409 Conflict业务错误，不能仍返回成功，也不能自动覆盖。

```java
int updatedRows = employeeMapper.updateWithVersion(request);
if (updatedRows == 0) {
    throw new DataConflictException("员工信息已被更新，请重新读取");
}
```

version是并发控制字段，不是修改次数展示字段。前端重读后应让用户确认新内容，不应偷偷重试完整UPDATE。

### 2. 悲观锁：事务内先锁定再处理

必须先读取最新状态、计算后立即写入且冲突概率高时，可以在同一事务中使用：

```sql
SELECT id, name, email, department, status
FROM employees
WHERE id = #{id}
FOR UPDATE;
```

`FOR UPDATE` 取得的行锁一般持续到当前事务提交或回滚。读取和更新必须处在同一个Service事务中；事务期间不要等待用户输入、调用慢速外部API或执行无关工作，否则会拉长等待并增加死锁风险。悲观锁不是“更安全的默认选项”，要根据冲突频率、等待时间和业务规格选择。

| 机制 | 主要保证 | 典型结果 |
| --- | --- | --- |
| 事务 | 一次业务的多条SQL共同成败 | 中途异常则整体回滚 |
| 唯一约束 | 某列或组合不能重复 | 冲突写入由数据库拒绝 |
| 乐观锁 | 只更新自己读取过的版本 | 0行更新后返回409并要求重读 |
| 悲观锁 | 事务期间让竞争者等待该行 | 提交、回滚或超时后释放 |

`@Transactional` 不代表自动解决并发覆盖、重复数据、死锁或所有数据库一致性问题。它只提供已确定事务边界内的提交和回滚语义；唯一约束、并发控制、锁顺序、超时和失败处理仍要根据规格分别设计。

实验至少设计两个并发用户读取同一version、A先提交、B后提交的用例，并保存A成功、B冲突、最终数据保留A结果的证据。完成实验后删除version列及实验Mapper方法，运行原有11个测试，确认主线恢复：

```sql
ALTER TABLE employees DROP COLUMN version;
```

## 十三、用数据库状态证明回滚

运行全部测试：

```powershell
.\mvnw.cmd clean test
```

第12章有9个测试，本章增加2个事务场景，预期：

```text
Tests run: 11, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

回滚测试不在测试方法或测试类上加 `@Transactional`。否则测试框架自己的外层事务可能在方法结束时统一回滚，无法证明Service的事务边界真正生效。

证明回滚需要同时保存：

1. PUT实际返500；
2. 日志中有履历表不存在的原因；
3. 失败前员工1001的部门为Sales；
4. 失败后部门仍为Sales；
5. 下一个测试前两张表已恢复。

只看500不能证明回滚；只看日志也不能证明数据库状态。

## 十四、常见失败与定位

| 现象 | 定位点 | 原因 | 修正 |
| --- | --- | --- | --- |
| 履历失败后employees仍改变 | Service Bean调用路径 | 方法没有事务或同类内部调用绕过代理 | 让外部调用经过带注解的Service Bean方法 |
| 异常已记录但数据仍提交 | catch后的控制流 | 捕获异常后正常返回 | 继续抛出可触发回滚的异常 |
| 一个Mapper回滚、另一个没有 | DataSource和事务管理器 | Mapper使用不同DataSource或绕过MyBatis-Spring | 确保SqlSessionFactory与事务管理器使用同一DataSource |
| 测试总是回滚 | 测试类注解 | 测试方法自己带 `@Transactional` | 移除外层测试事务，通过 `@Sql` 隔离数据 |
| 启动时找不到新Mapper | 包名、`@Mapper`、XML namespace | Java接口和XML名称不一致 | 按完整类名核对namespace |

## 十五、规格理解、影响调查、Review与练习

### 练习1：Review事务边界

审查“Controller调用两个Service，每个Service各自提交”的设计。指出半完成时的数据状态，将一个完整业务动作放到一个Service边界，并列出需回归的HTTP与数据库结果。

### 练习2：验证捕获异常的影响

在专用测试分支中临时捕获履历Mapper异常且不再抛出，运行回滚测试，记录actual为何变成Development。随后恢复正确代码，重跑并确认Sales保持不变。

### 练习3：受检异常规格

假设批量导入方法遇到 `IOException` 时必须撤销已写数据。写出方法签名和 `rollbackFor`，再写一个“异常抛出后表中件数不变”的测试规格。

### 练习4：影响调查

规格变更：删除员工时也必须写 `EMPLOYEE_DELETED` 履历。调查Service、履历数据、物理删除与履历保留顺序、事务、接口响应和回归测试的影响；先不实施，提交调查表。

### 练习5：整理事务证据

分别保存成功与中途失败场景的前置数据、HTTP状态、employees结果、employee_change_logs结果、异常原因和测试结果。结论必须指向数据库证据，不能只写“已加 `@Transactional`”。

### 练习6：选择并发控制方式

对“管理员在编辑画面停留10分钟后提交”和“夜间作业读取后立即更新”两个场景，分别比较不加锁、乐观锁和悲观锁。写出选择、失败时的接口状态、用户可见信息、超时或重试策略以及需补充的并发测试，不只回答机制名称。

## 十六、本章稳定状态

完成后，PUT会在同一事务中更新员工并写变更履历：

```text
src/main/java/com/example/employee/
├── entity/EmployeeChangeLog.java
├── mapper/EmployeeChangeLogMapper.java
└── service/impl/EmployeeServiceImpl.java
src/main/resources/mapper/EmployeeChangeLogMapper.xml
src/test/java/com/example/employee/
├── integration/EmployeeCrudIntegrationTest.java
└── service/EmployeeServiceImplTest.java
src/test/resources/test-data.sql
```

此时你应能够：

1. 区分单条SQL原子性和多步业务一致性；
2. 把commit、rollback和Service事务边界对应到真实数据变化；
3. 解释 `@Transactional` 的代理、外部调用和默认回滚规则；
4. 识别同类内部调用、吞掉异常和测试外层事务的风险；
5. 说明REQUIRED、隔离、只读与超时在当前用例中的选择；
6. 区分事务、锁和唯一约束的责任；
7. 用成功和中途失败测试的数据库状态证明提交与回滚。

下一章会在员工数量增多的前提下实现条件查询、稳定排序和分页，并继续用自动化测试保护旧有CRUD。
