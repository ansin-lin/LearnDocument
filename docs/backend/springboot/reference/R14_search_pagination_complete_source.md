# R14 第14章列表查询、排序与分页最终源码

本文件是第14章完成后的可选参考答案，用于课后核对和排错，不是第14章综合实践的起步代码。以第13章稳定工程为起点，只替换列表查询链；不加入第15、16章的Spring Security。独立数据库测试使用专用测试库，执行前必须确认测试保护规则与目标库，不能在生产库运行初始化脚本。

## 最终源码与变更核对

### 最终文件清单与完整实现

先沿一次 `GET /employees` 的处理链读本章代码。Spring MVC用 `@ModelAttribute` 从查询字符串绑定 `EmployeeSearchRequest`，与第6章读取JSON正文的 `@RequestBody` 不同；`@Valid` 在Controller执行前检查页码和pageSize。Service整理筛选条件；请求对象的 `getOffset()` 根据page和pageSize计算偏移量。Mapper只允许规定的排序字段和方向，不能把客户端任意文本拼进SQL；列表SQL与COUNT SQL必须使用同一组筛选条件。排序追加主键作为同值时的稳定次序，避免翻页时同一记录忽前忽后。最后 `PageResponse` 同时给出items、page、pageSize、total和totalPages；空页不等于查询失败。

下面是最终文件的完整代码。阅读顺序建议为“请求DTO及分页偏移量→Service整理条件→Mapper动态条件与白名单排序→COUNT→响应对象→测试”。pageSize设置上限是为了控制单次响应和数据库负担；EXPLAIN与索引分析放到完成正确性测试之后，不把性能优化当成输入绑定的前置要求。

从第13章稳定状态继续。本章不改员工详情和写入接口，新建两个数据对象，完整替换四个业务文件，并新增独立的分页测试和测试数据：

```text
src/main/java/com/example/employee/
├── controller/EmployeeController.java                  ← 完整替换
├── dto/request/EmployeeSearchRequest.java              ← 新建
├── dto/response/PageResponse.java                      ← 新建
├── mapper/EmployeeMapper.java                          ← 完整替换
└── service/
    ├── EmployeeService.java                            ← 完整替换
    └── impl/EmployeeServiceImpl.java                   ← 完整替换
src/main/resources/mapper/EmployeeMapper.xml            ← 完整替换
src/test/java/com/example/employee/integration/
└── EmployeeSearchIntegrationTest.java                 ← 新建
src/test/resources/search-test-data.sql                 ← 新建
```

### 1. 新建EmployeeSearchRequest.java

```java
package com.example.employee.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class EmployeeSearchRequest {

    @Size(max = 50, message = "姓名关键字不能超过50个字符")
    private String name;

    @Size(max = 50, message = "部门不能超过50个字符")
    private String department;

    @Pattern(
            regexp = "^(|ACTIVE|INACTIVE)$",
            message = "状态必须是ACTIVE或INACTIVE")
    private String status;

    @NotNull(message = "页码不能为空")
    @Min(value = 1, message = "页码必须从1开始")
    private Integer page = 1;

    @NotNull(message = "每页件数不能为空")
    @Min(value = 1, message = "每页件数至少为1")
    @Max(value = 100, message = "每页件数不能超过100")
    private Integer pageSize = 20;

    @NotBlank(message = "排序字段不能为空")
    @Pattern(
            regexp = "^(id|createdAt)$",
            message = "排序字段必须是id或createdAt")
    private String sortBy = "id";

    @NotBlank(message = "排序方向不能为空")
    @Pattern(
            regexp = "^(asc|desc)$",
            message = "排序方向必须是asc或desc")
    private String sortDirection = "asc";

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getPage() { return page; }
    public void setPage(Integer page) { this.page = page; }
    public Integer getPageSize() { return pageSize; }
    public void setPageSize(Integer pageSize) { this.pageSize = pageSize; }
    public String getSortBy() { return sortBy; }
    public void setSortBy(String sortBy) { this.sortBy = sortBy; }
    public String getSortDirection() { return sortDirection; }
    public void setSortDirection(String sortDirection) { this.sortDirection = sortDirection; }

    public long getOffset() {
        return (long) (page - 1) * pageSize;
    }
}
```

### 2. 新建PageResponse.java

```java
package com.example.employee.dto.response;

import java.util.List;

public class PageResponse<T> {

    private final List<T> items;
    private final int page;
    private final int pageSize;
    private final long total;
    private final long totalPages;

    public PageResponse(List<T> items, int page, int pageSize, long total) {
        this.items = items;
        this.page = page;
        this.pageSize = pageSize;
        this.total = total;
        this.totalPages = total / pageSize
                + (total % pageSize == 0 ? 0 : 1);
    }

    public List<T> getItems() { return items; }
    public int getPage() { return page; }
    public int getPageSize() { return pageSize; }
    public long getTotal() { return total; }
    public long getTotalPages() { return totalPages; }
}
```

### 3. 完整替换EmployeeMapper.java

```java
package com.example.employee.mapper;

import com.example.employee.dto.request.EmployeeSearchRequest;
import com.example.employee.entity.Employee;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface EmployeeMapper {
    Employee findById(@Param("id") Long id);
    List<Employee> search(EmployeeSearchRequest request);
    long count(EmployeeSearchRequest request);
    int insert(Employee employee);
    int update(Employee employee);
    int deleteById(@Param("id") Long id);
}
```

### 4. 完整替换EmployeeMapper.xml

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "https://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.example.employee.mapper.EmployeeMapper">

    <resultMap id="employeeResultMap" type="Employee">
        <id property="id" column="id"/>
        <result property="name" column="name"/>
        <result property="department" column="department"/>
        <result property="email" column="email"/>
        <result property="status" column="status"/>
        <result property="createdAt" column="created_at"/>
        <result property="updatedAt" column="updated_at"/>
    </resultMap>

    <sql id="searchConditions">
        <where>
            <if test="name != null">
                AND name LIKE CONCAT('%', #{name}, '%')
            </if>
            <if test="department != null">
                AND department = #{department}
            </if>
            <if test="status != null">
                AND status = #{status}
            </if>
        </where>
    </sql>

    <select id="findById" parameterType="long"
            resultMap="employeeResultMap">
        SELECT id, name, department, email, status,
               created_at, updated_at
        FROM employees
        WHERE id = #{id}
    </select>

    <select id="search"
            parameterType="com.example.employee.dto.request.EmployeeSearchRequest"
            resultMap="employeeResultMap">
        SELECT id, name, department, email, status,
               created_at, updated_at
        FROM employees
        <include refid="searchConditions"/>
        <choose>
            <when test="sortBy == 'createdAt'">
                ORDER BY created_at
                <choose>
                    <when test="sortDirection == 'desc'">DESC</when>
                    <otherwise>ASC</otherwise>
                </choose>, id ASC
            </when>
            <otherwise>
                ORDER BY id
                <choose>
                    <when test="sortDirection == 'desc'">DESC</when>
                    <otherwise>ASC</otherwise>
                </choose>
            </otherwise>
        </choose>
        LIMIT #{pageSize} OFFSET #{offset}
    </select>

    <select id="count"
            parameterType="com.example.employee.dto.request.EmployeeSearchRequest"
            resultType="long">
        SELECT COUNT(*)
        FROM employees
        <include refid="searchConditions"/>
    </select>

    <insert id="insert" parameterType="Employee"
            useGeneratedKeys="true" keyProperty="id">
        INSERT INTO employees (name, department, email, status)
        VALUES (#{name}, #{department}, #{email}, #{status})
    </insert>

    <update id="update" parameterType="Employee">
        UPDATE employees
        SET name = #{name}, department = #{department}, email = #{email}
        WHERE id = #{id}
    </update>

    <delete id="deleteById" parameterType="long">
        DELETE FROM employees WHERE id = #{id}
    </delete>
</mapper>
```

### 5. 完整替换EmployeeService.java

```java
package com.example.employee.service;

import com.example.employee.dto.request.EmployeeCreateRequest;
import com.example.employee.dto.request.EmployeeSearchRequest;
import com.example.employee.dto.request.EmployeeUpdateRequest;
import com.example.employee.dto.response.EmployeeListItemResponse;
import com.example.employee.dto.response.EmployeeResponse;
import com.example.employee.dto.response.PageResponse;

public interface EmployeeService {
    EmployeeResponse findById(Long id);
    PageResponse<EmployeeListItemResponse> search(EmployeeSearchRequest request);
    EmployeeResponse create(EmployeeCreateRequest request);
    EmployeeResponse update(Long id, EmployeeUpdateRequest request);
    void delete(Long id);
}
```

### 6. 完整替换EmployeeServiceImpl.java

```java
package com.example.employee.service.impl;

import com.example.employee.dto.request.EmployeeCreateRequest;
import com.example.employee.dto.request.EmployeeSearchRequest;
import com.example.employee.dto.request.EmployeeUpdateRequest;
import com.example.employee.dto.response.EmployeeListItemResponse;
import com.example.employee.dto.response.EmployeeResponse;
import com.example.employee.dto.response.PageResponse;
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
    public PageResponse<EmployeeListItemResponse> search(
            EmployeeSearchRequest request) {
        normalizeSearchRequest(request);
        List<Employee> employees = employeeMapper.search(request);
        long total = employeeMapper.count(request);
        List<EmployeeListItemResponse> items = new ArrayList<>();
        for (Employee employee : employees) {
            items.add(toListItemResponse(employee));
        }
        log.debug(
                "employee_search_completed page={} pageSize={} total={}",
                request.getPage(), request.getPageSize(), total);
        return new PageResponse<>(
                items, request.getPage(), request.getPageSize(), total);
    }

    @Override
    @Transactional
    public EmployeeResponse create(EmployeeCreateRequest request) {
        String department = request.getDepartment().trim();
        validateDepartment(department);
        Employee employee = new Employee();
        employee.setName(request.getName().trim());
        employee.setDepartment(department);
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
        log.info("employee_created employeeId={} department={}",
                employee.getId(), employee.getDepartment());
        return findById(employee.getId());
    }

    @Override
    @Transactional
    public EmployeeResponse update(
            Long id, EmployeeUpdateRequest request) {
        String department = request.getDepartment().trim();
        validateDepartment(department);
        Employee beforeUpdate = findEmployeeOrThrow(id);
        Employee employee = new Employee();
        employee.setId(id);
        employee.setName(request.getName().trim());
        employee.setDepartment(department);
        employee.setEmail(normalizeEmail(request.getEmail()));
        try {
            int affectedRows = employeeMapper.update(employee);
            if (affectedRows > 1) {
                throw new EmployeeSystemException("修改员工影响多行");
            }
            if (affectedRows == 0 && employeeMapper.findById(id) == null) {
                throw new EmployeeNotFoundException(id);
            }
        } catch (DuplicateKeyException exception) {
            throw new DuplicateEmailException(employee.getEmail());
        }
        EmployeeChangeLog changeLog = new EmployeeChangeLog(
                id, ACTION_EMPLOYEE_UPDATED,
                "department=" + beforeUpdate.getDepartment()
                        + "->" + employee.getDepartment());
        int logRows = employeeChangeLogMapper.insert(changeLog);
        if (logRows != 1 || changeLog.getId() == null) {
            throw new EmployeeSystemException("员工变更履历写入失败");
        }
        log.info("employee_updated employeeId={} department={}",
                id, employee.getDepartment());
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

    private void normalizeSearchRequest(EmployeeSearchRequest request) {
        request.setName(normalizeOptionalText(request.getName()));
        request.setDepartment(normalizeOptionalText(request.getDepartment()));
        request.setStatus(normalizeOptionalText(request.getStatus()));
        if (request.getDepartment() != null) {
            validateDepartment(request.getDepartment());
        }
    }

    private String normalizeOptionalText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private Employee findEmployeeOrThrow(Long id) {
        Employee employee = employeeMapper.findById(id);
        if (employee == null) {
            throw new EmployeeNotFoundException(id);
        }
        return employee;
    }

    private EmployeeResponse toResponse(Employee employee) {
        return new EmployeeResponse(employee.getId(), employee.getName(),
                employee.getDepartment(), employee.getEmail());
    }

    private EmployeeListItemResponse toListItemResponse(Employee employee) {
        return new EmployeeListItemResponse(employee.getId(),
                employee.getName(), employee.getDepartment());
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
}
```

与第13章相比，只有列表路径从 `findList(String)` 改为 `search(EmployeeSearchRequest)`；事务、履历和其他CRUD逻辑保持原样。整文件给出是为了避免学员组合局部片段时误删事务代码。

### 7. 完整替换EmployeeController.java

```java
package com.example.employee.controller;

import com.example.employee.common.ApiResponse;
import com.example.employee.dto.request.EmployeeCreateRequest;
import com.example.employee.dto.request.EmployeeSearchRequest;
import com.example.employee.dto.request.EmployeeUpdateRequest;
import com.example.employee.dto.response.EmployeeListItemResponse;
import com.example.employee.dto.response.EmployeeResponse;
import com.example.employee.dto.response.PageResponse;
import com.example.employee.service.EmployeeService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/employees")
public class EmployeeController {
    private final EmployeeService employeeService;

    public EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<EmployeeResponse>> findById(
            @PathVariable(name = "id") Long id) {
        return ResponseEntity.ok(
                ApiResponse.success(employeeService.findById(id)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<EmployeeListItemResponse>>>
            search(@Valid @ModelAttribute EmployeeSearchRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success(employeeService.search(request)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<EmployeeResponse>> create(
            @Valid @RequestBody EmployeeCreateRequest request) {
        EmployeeResponse employee = employeeService.create(request);
        URI location = URI.create("/employees/" + employee.getId());
        return ResponseEntity.created(location)
                .body(ApiResponse.success(employee));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<EmployeeResponse>> update(
            @PathVariable(name = "id") Long id,
            @Valid @RequestBody EmployeeUpdateRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success(employeeService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable(name = "id") Long id) {
        employeeService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
```

### 8. 新建search-test-data.sql

该文件由下一小节的独立测试在基础 `test-data.sql` 之后执行。先把基础数据中1001、1002的默认当前时间固定为更早的时间，再追加分页记录；这样按 `createdAt desc` 排序时，1003、1004会排在1001前面，断言不受执行日期影响：

```sql
UPDATE employees
SET created_at = '2026-01-01 09:00:00',
    updated_at = '2026-01-01 09:00:00'
WHERE id IN (1001, 1002);

INSERT INTO employees (
    id, name, department, email, status, created_at, updated_at
) VALUES
    (1003, 'Tanaka Taro', 'Sales', 'taro@example.com',
     'ACTIVE', '2026-01-02 09:00:00', '2026-01-02 09:00:00'),
    (1004, 'Tanaka Aiko', 'Sales', 'aiko@example.com',
     'ACTIVE', '2026-01-02 09:00:00', '2026-01-02 09:00:00'),
    (1005, 'Yamada', 'Support', 'yamada@example.com',
     'INACTIVE', '2026-01-03 09:00:00', '2026-01-03 09:00:00'),
    (1006, 'Ito', 'Development', 'ito@example.com',
     'ACTIVE', '2026-01-04 09:00:00', '2026-01-04 09:00:00');
```

### 9. 新建EmployeeSearchIntegrationTest.java

```java
package com.example.employee.integration;

import com.example.employee.support.MySqlTestGuard;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Sql(
        scripts = {"/test-data.sql", "/search-test-data.sql"},
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class EmployeeSearchIntegrationTest extends MySqlTestGuard {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void searchUsesSameConditionsForItemsAndTotal() throws Exception {
        mockMvc.perform(get("/employees")
                        .param("name", "Tanaka")
                        .param("department", "Sales")
                        .param("status", "ACTIVE")
                        .param("page", "1")
                        .param("pageSize", "2")
                        .param("sortBy", "createdAt")
                        .param("sortDirection", "desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(2))
                .andExpect(jsonPath("$.data.items[0].id").value(1003))
                .andExpect(jsonPath("$.data.items[1].id").value(1004))
                .andExpect(jsonPath("$.data.total").value(3))
                .andExpect(jsonPath("$.data.totalPages").value(2));
    }

    @Test
    void secondPageKeepsStableOrder() throws Exception {
        mockMvc.perform(get("/employees")
                        .param("name", "Tanaka")
                        .param("page", "2")
                        .param("pageSize", "2")
                        .param("sortBy", "createdAt")
                        .param("sortDirection", "desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].id").value(1001));
    }

    @Test
    void pageBeyondLastReturnsEmptyItemsAndRealTotal() throws Exception {
        mockMvc.perform(get("/employees")
                        .param("department", "Support")
                        .param("page", "99")
                        .param("pageSize", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(0))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.page").value(99));
    }

    @Test
    void invalidPageSizeReturns400() throws Exception {
        mockMvc.perform(get("/employees")
                        .param("pageSize", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data.pageSize").exists());
    }
}
```
