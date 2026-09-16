# 第14章 让员工列表支持条件查询与分页

> 本章目标：把原来的简单员工列表升级为可筛选、可稳定排序、可分页的列表接口，并用接口响应和数据库测试证明筛选条件、总件数、空页及非法参数都符合规格。

## 一、先确定接口规格

数据增多后，接口不能每次返回全部员工。本章修改 `GET /employees`：

```text
GET /employees?name=Tanaka&department=Sales&status=ACTIVE
    &page=1&pageSize=20&sortBy=createdAt&sortDirection=desc
```

| 参数 | 必填 | 默认值 | 可接受的值 | 含义 |
| --- | --- | --- | --- | --- |
| `name` | 否 | 无 | 最长50字符；空白按未传处理 | 姓名中包含的文字 |
| `department` | 否 | 无 | Sales、Development、Support；空白按未传处理 | 完全一致的部门 |
| `status` | 否 | 无 | ACTIVE、INACTIVE；空字符串按未传处理 | 完全一致的状态 |
| `page` | 否 | 1 | 1以上整数 | 从1开始的页码 |
| `pageSize` | 否 | 20 | 1～100整数 | 每页最多返回的件数 |
| `sortBy` | 否 | id | id、createdAt | 排序字段白名单 |
| `sortDirection` | 否 | asc | asc、desc | 升序或降序 |

响应仍使用第7章的 `ApiResponse`，`data` 改为分页对象：

```json
{
  "success": true,
  "message": "OK",
  "data": {
    "items": [
      {"id": 1003, "name": "Tanaka Taro", "department": "Sales"}
    ],
    "page": 1,
    "pageSize": 20,
    "total": 1,
    "totalPages": 1
  }
}
```

无结果或超过最后一页都返回200和空 `items`，同时保留真实 `total`。格式或范围非法则返回400，不静默改成其他值。

## 二、完整示例

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
        log.info("employee_created employeeId={} department={}",
                employee.getId(), employee.getDepartment());
        return findById(employee.getId());
    }

    @Override
    @Transactional
    public EmployeeResponse update(
            Long id, EmployeeUpdateRequest request) {
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

该文件只追加分页用数据，由下一小节的独立测试在基础 `test-data.sql` 之后执行：

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

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
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
class EmployeeSearchIntegrationTest {

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

## 三、查询参数怎样变成请求对象

`@ModelAttribute` 来自Spring Web MVC，放在方法参数上时，Spring创建 `EmployeeSearchRequest`，再按同名属性把查询字符串写入setter。参数省略时保留字段初始化的默认值；显式传入非法数字或不在白名单中的文字时，绑定或校验失败并返回400。

`@Valid` 触发第8章已经使用的Bean Validation。`@Min`、`@Max`、`@NotNull`、`@NotBlank`、`@Size` 和 `@Pattern` 都来自 `jakarta.validation.constraints`；它们分别限制数字下限、数字上限、null、空白、长度和正则格式。Spring MVC 6.2对请求对象校验的处理见[官方说明](https://docs.spring.io/spring-framework/reference/6.2/web/webmvc/mvc-controller/ann-validation.html)。

未传、空值和非法值必须区分：

| 输入 | 结果 |
| --- | --- |
| 不传 `pageSize` | 使用默认20 |
| `name=` 或只有空格 | Service转为null，不生成姓名条件 |
| `status=` | 校验允许空字符串，Service转为null |
| `pageSize=0`、`pageSize=101` | 校验失败，返回400 |
| `sortBy=email` | 不在白名单，返回400 |

## 四、页码、偏移量和分页响应

MySQL 8.0的 `LIMIT` 限制返回件数，`OFFSET` 表示跳过多少条：

```text
offset = (page - 1) × pageSize
```

第2页、每页20条时offset为20。`getOffset()` 使用 `long` 参与乘法，避免先用int计算时溢出。页码超过最后一页不是请求格式错误，所以返回空列表；客户端仍可从total和totalPages判断真实范围。

`PageResponse<T>` 中的 `T` 是当前页元素类型，本章实际为 `EmployeeListItemResponse`。total是全部符合条件的件数，不是当前页件数；totalPages使用整数除法和余数计算，total为0时结果也是0。

## 五、动态SQL怎样只加入有效条件

`<if test="...">` 在条件成立时生成内部SQL；`<where>` 仅在至少一个条件存在时生成WHERE，并移除开头多余的AND。`<sql>` 定义可复用片段，`<include>` 将同一组筛选条件放入数据查询和总数查询，防止两条SQL逐渐不一致。语义可参考[MyBatis动态SQL官方文档](https://mybatis.org/mybatis-3/dynamic-sql.html)。

姓名使用 `#{name}` 参数绑定，`CONCAT` 只在数据库端添加 `%` 通配符。不得改成 `${name}`：后者是文本替换，用户输入可能改变SQL结构并造成SQL注入。

## 六、为什么分页必须稳定排序

如果只按 `created_at` 排序，两名员工创建时间相同时先后顺序未确定；不同请求可能使一条记录重复出现在相邻两页或被漏掉。本章在创建时间后追加唯一的 `id ASC`：

```sql
ORDER BY created_at DESC, id ASC
```

这样相同时间仍有确定顺序。MySQL也说明，当ORDER BY列存在相同值时，其他列的顺序可能不确定，LIMIT还可能影响执行计划；参见[MySQL 8.0 LIMIT优化说明](https://dev.mysql.com/doc/refman/8.0/en/limit-optimization.html)。

排序列和ASC/DESC不能通过 `ORDER BY ${sortBy} ${sortDirection}` 直接接收用户输入。本章先用校验限定接口值，再由MyBatis `<choose>` 只生成预先写好的列名和关键字，因此请求无法注入任意SQL片段。

## 七、数据查询和总数查询必须一致

分页需要两次查询：

```text
search() → 当前页items，带ORDER BY、LIMIT、OFFSET
count()  → 相同条件的total，不需要排序和分页
```

只要name、department或status有一个条件不一致，页面就可能显示“两条记录、总数却是十条”。共享 `searchConditions` 能减少复制错误，但新增条件时仍要同时更新请求规格、请求对象、SQL、测试和接口示例。

## 八、索引与EXPLAIN只根据真实查询判断

先在个人练习MySQL中使用与实际接口一致的条件观察执行计划：

```sql
EXPLAIN
SELECT id, name, department, email, status, created_at, updated_at
FROM employees
WHERE department = 'Sales' AND status = 'ACTIVE'
ORDER BY created_at DESC, id ASC
LIMIT 20 OFFSET 0;
```

重点观察访问类型、可能使用和实际使用的索引、预计扫描行数及Extra。是否增加组合索引取决于数据量、值分布、常用筛选组合和排序方向；不能因为查询出现三个字段就机械建立三个单列索引。MySQL对索引与排序的关系见[ORDER BY优化文档](https://dev.mysql.com/doc/refman/8.0/en/order-by-optimization.html)，执行计划和复杂优化继续参考SQL课程。

OFFSET越大，数据库通常需要跳过越多结果。当前接口适合基础管理页面；数据达到需要频繁访问深页时，应根据产品的跳页需求评估游标分页，而不是先修改本章接口契约。

## 九、运行与验收

```powershell
.\mvnw.cmd clean test
```

第13章累计11个测试，本章新增4个，预期：

```text
Tests run: 15, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

手工验收至少保存请求URL、HTTP状态、items编号顺序、page、pageSize、total和totalPages。只截取第一页列表，不能证明总数、稳定排序和越界页正确。

## 十、常见失败与定位

| 现象 | 原因 | 修正 |
| --- | --- | --- |
| 无条件查询SQL出现孤立WHERE | 手工拼接WHERE | 使用 `<where>` 并测试全部条件为空 |
| items正确但total错误 | 两条SQL条件不同 | 复用同一筛选片段并增加组合条件测试 |
| 翻页出现重复或遗漏 | 排序值相同且无唯一补充列 | 在业务排序后追加id |
| pageSize过大仍返回200 | 请求对象未校验或未加 `@Valid` | 核对注解、Controller参数和400响应 |
| ORDER BY可被插入任意文字 | 使用 `${}` 拼接请求值 | 改为白名单和 `<choose>` |
| 深页查询逐渐变慢 | OFFSET需要跳过大量结果 | 用EXPLAIN调查，再评估索引或游标分页 |

## 十一、规格理解、改修与Review练习

### 练习1：增加邮箱关键字

规格要求按邮箱包含文字筛选，最大100字符。完成请求对象、共享SQL片段和集成测试；证明items和total使用同一条件。

### 练习2：Review不稳定分页

把排序临时改成只有 `created_at DESC`，说明1003和1004顺序为何没有保证，再恢复 `id ASC` 并保存连续两页的编号证据。

### 练习3：非法参数测试

分别验证 `page=0`、`sortBy=email`、`sortDirection=up` 和无法转换为整数的 `page=abc`。记录每次HTTP状态和错误字段。

### 练习4：影响调查

改修票要求新增department和status多选筛选。先调查URL规格、请求对象类型、MyBatis `<foreach>`、空集合语义、SQL、索引和回归范围，不直接编码。

### 练习5：自测证据

提交默认查询、组合条件、相同时间稳定排序、无结果、越界页、最大pageSize和非法参数的请求与响应证据，并注明测试数据版本。

## 十二、本章稳定状态

完成后，原列表接口已经被分页查询替换，详情、新增、修改、删除和事务履历保持不变。你应能够：

1. 把筛选、排序和分页要求写成明确接口规格；
2. 区分参数省略、空白、非法和超出最后一页；
3. 计算offset并解释items、total和totalPages；
4. 使用 `<if>`、`<where>`、`<sql>`、`<include>` 和 `<choose>` 生成受控动态SQL；
5. 保证数据查询和总数查询的条件一致；
6. 用唯一补充列建立稳定排序，并拒绝ORDER BY文本注入；
7. 用EXPLAIN和测试证据调查查询，而不是凭感觉添加索引。

下一章将在现有接口上增加登录和Session身份识别；本章不提前实现权限判断。
