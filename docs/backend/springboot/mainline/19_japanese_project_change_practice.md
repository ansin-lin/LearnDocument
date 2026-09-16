# 第19章 按改修票完成一次日本项目变更

> 本章目标：在不破坏既有接口、权限和分页行为的前提下，根据一张改修票完成规格确认、影响调查、代码修改、Review修正、回归测试和改修报告，使其他人能够复核这次变更。

## 一、先读改修票，不要看到一句需求就开始编码

本章从第18章稳定状态继续。管理员画面希望同时选择多个部门和多个员工状态，项目收到下面的改修票。

```text
票号：EMP-241
标题：员工一览增加部门、状态多选查询

背景：
管理员需要一次查看多个部门或多个状态的员工。

现状：
GET /employees?department=Sales&status=ACTIVE
department和status各只能指定一个值。

期望：
同一个查询参数可以重复出现。
GET /employees?department=Sales&department=Support
              &status=ACTIVE&status=INACTIVE

范围：
- 只修改员工列表查询。
- 不修改员工详情、增删改、登录和权限规则。
- 不修改响应JSON和数据库表结构。

验收条件：
1. 原来的单值请求继续得到相同结果。
2. 多个department之间按OR查询。
3. 多个status之间按OR查询。
4. department组和status组之间按AND查询。
5. items和total使用完全相同的条件。
6. 非法值返回400。
7. 只有ADMIN可以使用列表接口。
8. 全部自动化测试通过。
```

“多个值按OR、不同字段按AND”表示：

```text
(department是Sales或Support)
AND
(status是ACTIVE或INACTIVE)
```

不能把它错误理解成四个条件全部同时成立。单条员工记录不可能既属于Sales又属于Support。

## 二、把模糊点变成确定规格

改修票仍有不能靠开发者猜测的内容。编码前提出确认事项（確認事項），并记录得到的回答：

| No | 待确认事项 | 确认结果 | 为什么会影响实现 |
| --- | --- | --- | --- |
| Q1 | 多值使用重复参数还是逗号分隔 | 使用重复参数 | 决定URL、Spring绑定和测试写法 |
| Q2 | 单值参数是否继续支持 | 必须兼容 | 旧画面和调用方不能同时强制改修 |
| Q3 | 参数未传、空字符串怎样处理 | 视为没有该筛选条件 | 决定空集合和动态SQL语义 |
| Q4 | 重复值怎样处理 | 原始次数在上限内时去重后查询 | 避免生成无意义重复占位符，同时保留请求大小上限 |
| Q5 | 值域和大小写 | 部门为既有3种，状态为ACTIVE/INACTIVE，区分大小写 | 决定校验和400边界 |
| Q6 | 最多出现多少次 | 原始department参数最多3次，status最多2次 | Bean Validation在规范化前限制请求大小 |
| Q7 | 两种状态都选择时是否允许 | 允许；表示状态不再缩小结果 | 与“不传状态”结果可能相同，但请求仍合法 |
| Q8 | 权限是否改变 | 不变，仍仅ADMIN | 防止功能改修意外放宽访问范围 |

确认后的接口规格如下：

| 参数 | 必填 | 默认值 | 可接受的值 | 结果 |
| --- | --- | --- | --- | --- |
| `department` | 否 | 无筛选 | 原始参数最多3次；Sales、Development、Support；空值忽略；上限内重复值去重 | 同组使用SQL IN |
| `status` | 否 | 无筛选 | 原始参数最多2次；ACTIVE、INACTIVE；空值忽略；上限内重复值去重 | 同组使用SQL IN |
| 其他列表参数 | 否 | 沿用第14章 | name、page、pageSize、sortBy、sortDirection原规格 | 行为不变 |

请求和响应示例：

```text
GET /employees?department=Sales&department=Support
    &status=ACTIVE&status=INACTIVE
    &page=1&pageSize=20&sortBy=id&sortDirection=asc
```

```json
{
  "success": true,
  "message": "OK",
  "data": {
    "items": [
      {"id": 1001, "name": "Tanaka", "department": "Sales"},
      {"id": 1003, "name": "Tanaka Taro", "department": "Sales"},
      {"id": 1004, "name": "Tanaka Aiko", "department": "Sales"},
      {"id": 1005, "name": "Yamada", "department": "Support"}
    ],
    "page": 1,
    "pageSize": 20,
    "total": 4,
    "totalPages": 1
  }
}
```

## 三、完整改修示例

本章只修改四个既有文件，不新增依赖、数据库列、Controller方法或URL。先完成下列文件的最终状态，再逐项理解本章第一次出现的写法：

```text
src/main/java/com/example/employee/
├── dto/request/EmployeeSearchRequest.java       ← 完整替换
└── service/impl/EmployeeServiceImpl.java         ← 完整替换
src/main/resources/mapper/
└── EmployeeMapper.xml                            ← 完整替换
src/test/java/com/example/employee/integration/
└── EmployeeSearchIntegrationTest.java            ← 完整替换
```

### 1. 完整替换EmployeeSearchRequest.java

```java
package com.example.employee.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public class EmployeeSearchRequest {

    @Size(max = 50, message = "姓名关键字不能超过50个字符")
    private String name;

    @Size(max = 3, message = "部门参数最多传3次")
    private List<
            @Size(max = 50, message = "部门不能超过50个字符")
            String> department;

    @Size(max = 2, message = "状态参数最多传2次")
    private List<
            @Pattern(
                    regexp = "^(|ACTIVE|INACTIVE)$",
                    message = "状态必须是ACTIVE或INACTIVE")
            String> status;

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
    public List<String> getDepartment() { return department; }
    public void setDepartment(List<String> department) {
        this.department = department;
    }
    public List<String> getStatus() { return status; }
    public void setStatus(List<String> status) { this.status = status; }
    public Integer getPage() { return page; }
    public void setPage(Integer page) { this.page = page; }
    public Integer getPageSize() { return pageSize; }
    public void setPageSize(Integer pageSize) { this.pageSize = pageSize; }
    public String getSortBy() { return sortBy; }
    public void setSortBy(String sortBy) { this.sortBy = sortBy; }
    public String getSortDirection() { return sortDirection; }
    public void setSortDirection(String sortDirection) {
        this.sortDirection = sortDirection;
    }

    public long getOffset() {
        return (long) (page - 1) * pageSize;
    }
}
```

### 2. 完整替换EmployeeServiceImpl.java

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
        request.setDepartment(
                normalizeOptionalTexts(request.getDepartment()));
        request.setStatus(normalizeOptionalTexts(request.getStatus()));
        if (request.getDepartment() != null) {
            for (String department : request.getDepartment()) {
                validateDepartment(department);
            }
        }
    }

    private List<String> normalizeOptionalTexts(List<String> values) {
        if (values == null) {
            return null;
        }
        List<String> normalizedValues = new ArrayList<>();
        for (String value : values) {
            String normalizedValue = normalizeOptionalText(value);
            if (normalizedValue != null
                    && !normalizedValues.contains(normalizedValue)) {
                normalizedValues.add(normalizedValue);
            }
        }
        return normalizedValues.isEmpty() ? null : normalizedValues;
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

### 3. 完整替换EmployeeMapper.xml

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
            <if test="department != null and !department.isEmpty()">
                AND department IN
                <foreach collection="department"
                         item="departmentValue"
                         open="(" separator="," close=")">
                    #{departmentValue}
                </foreach>
            </if>
            <if test="status != null and !status.isEmpty()">
                AND status IN
                <foreach collection="status"
                         item="statusValue"
                         open="(" separator="," close=")">
                    #{statusValue}
                </foreach>
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

### 4. 完整替换EmployeeSearchIntegrationTest.java

```java
package com.example.employee.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser(username = "integration-admin", roles = "ADMIN")
@Sql(
        scripts = {"/test-data.sql", "/search-test-data.sql"},
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class EmployeeSearchIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void singleValueRequestKeepsPreviousBehavior() throws Exception {
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
    void repeatedParametersUseOrInsideEachGroupAndAndBetweenGroups()
            throws Exception {
        mockMvc.perform(get("/employees")
                        .param("department", "Sales", "Support")
                        .param("status", "ACTIVE", "INACTIVE")
                        .param("pageSize", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(4))
                .andExpect(jsonPath("$.data.items[0].id").value(1001))
                .andExpect(jsonPath("$.data.items[1].id").value(1003))
                .andExpect(jsonPath("$.data.items[2].id").value(1004))
                .andExpect(jsonPath("$.data.items[3].id").value(1005))
                .andExpect(jsonPath("$.data.total").value(4));
    }

    @Test
    void oneRepeatedParameterStillWorksAsOneValue() throws Exception {
        mockMvc.perform(get("/employees")
                        .param("status", "INACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].id").value(1005))
                .andExpect(jsonPath("$.data.total").value(1));
    }

    @Test
    void blankAndDuplicateValuesAreRemovedBeforeSql() throws Exception {
        mockMvc.perform(get("/employees")
                        .param("department", "", "Sales", "Sales"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(3))
                .andExpect(jsonPath("$.data.total").value(3));
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
    void invalidValueInsideRepeatedStatusReturns400() throws Exception {
        mockMvc.perform(get("/employees")
                        .param("status", "ACTIVE", "UNKNOWN"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void tooManyDepartmentOccurrencesReturns400() throws Exception {
        mockMvc.perform(get("/employees")
                        .param("department",
                                "Sales", "Development", "Support", "Sales"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data.department").exists());
    }

    @Test
    void invalidPageSizeStillReturns400() throws Exception {
        mockMvc.perform(get("/employees")
                        .param("pageSize", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data.pageSize").exists());
    }
}
```

## 四、重复查询参数怎样进入List

第14章的 `EmployeeController` 不需要修改，仍然使用：

```java
public ResponseEntity<ApiResponse<PageResponse<EmployeeListItemResponse>>>
        search(@Valid @ModelAttribute EmployeeSearchRequest request) {
    return ResponseEntity.ok(ApiResponse.success(
            employeeService.search(request)));
}
```

Spring MVC的数据绑定会按照属性名调用setter。请求中只有一个 `department=Sales` 时，传给 `setDepartment` 的列表含一个元素；同名参数重复时，列表按请求值形成多个元素：

```text
department=Sales
→ setDepartment(["Sales"])

department=Sales&department=Support
→ setDepartment(["Sales", "Support"])
```

因此把属性类型从 `String` 改为 `List<String>` 后，旧单值URL仍然有效。这里使用专用请求DTO，不把不受信任的查询参数直接绑定到Entity。Spring MVC数据绑定的职责与安全边界可参考[Spring官方Data Binding说明](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-data-binding.html)。

## 五、List本身和List元素是两种校验

下面两层 `@Size` 约束的对象不同：

```java
@Size(max = 3, message = "部门参数最多传3次")
private List<
        @Size(max = 50, message = "部门不能超过50个字符")
        String> department;
```

| 写法 | 约束对象 | 当前值 | 可接受的值 | 失败结果 |
| --- | --- | --- | --- | --- |
| 字段上的 `@Size` | 规范化前List的原始元素个数 | max=3 | null或0～3项 | 400，部门参数出现过多 |
| `List<@Size String>` | 每个字符串 | max=50 | 每项最多50字符 | 400，具体元素过长 |
| `List<@Pattern String>` | 每个状态字符串 | ACTIVE或INACTIVE，也允许空串后续忽略 | 两个规定值或空串 | 400，含UNKNOWN等值 |

尖括号内的注解称为容器元素约束：约束List里面的每个值，而不是只检查List对象。Controller已有的 `@Valid` 会在进入Service前触发这些约束；失败仍由第8章的全局异常处理转换为统一400响应。Spring MVC校验触发条件可回顾[官方Validation说明](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-validation.html)。

## 六、为什么在Service统一空值和重复值

HTTP绑定只负责把输入装入请求对象，不负责决定本项目的“空值忽略、上限内重复值去重”规格。Bean Validation先检查原始参数次数，`normalizeOptionalTexts` 再按固定顺序处理：

```text
原始值 ["", "Sales", "Sales"]
  → 空字符串转为null并忽略
  → 第一个Sales加入结果
  → 第二个Sales因contains为true而忽略
  → 最终 ["Sales"]
```

该方法的参数和返回如下：

| 项目 | 当前类型 | 可接受的值 | 结果 |
| --- | --- | --- | --- |
| `values` | `List<String>` | null、空List、含空白或普通文字的List | 原始查询值 |
| `normalizedValues` | `ArrayList<String>` | 方法内部创建 | 保持首次出现顺序的非空、非重复值 |
| 返回值 | `List<String>`或null | 非空规范化列表，或没有有效值 | null使MyBatis不生成该条件 |

`contains(value)` 使用字符串内容判断列表中是否已有同值；`isEmpty()` 判断集合没有元素。这里最多只有3项，直接使用List足够清楚，不需要为了去重额外引入集合转换或第三方库。

部门白名单仍是Service业务规则，所以规范化后逐项调用既有 `validateDepartment`。状态的固定格式由请求DTO的 `@Pattern` 检查。不要因为现在是List就删除任一层校验。

## 七、MyBatis foreach怎样生成安全的IN条件

当department为Sales和Support时，新的动态SQL生成近似下面的结构：

```sql
WHERE department IN (?, ?)
  AND status IN (?, ?)
ORDER BY id ASC
LIMIT ? OFFSET ?
```

`<foreach>` 遍历集合并生成括号与逗号：

| 属性 | 当前值 | 可接受的值 | 作用 |
| --- | --- | --- | --- |
| `collection` | department或status | MyBatis上下文中可迭代的集合名 | 指定遍历哪组请求值 |
| `item` | departmentValue或statusValue | 当前XML内未冲突的变量名 | 表示当前循环元素 |
| `open` | `(` | 任意需要的起始文本 | 第一个元素前生成左括号 |
| `separator` | `,` | 任意分隔文本 | 只在元素之间生成逗号 |
| `close` | `)` | 任意需要的结束文本 | 最后一个元素后生成右括号 |

集合为null或空集合时，外层 `<if>` 不生成整个IN条件，避免出现无效的 `IN ()`。Service虽然已经把空列表变成null，XML仍保留非空检查，使SQL边界能够独立读懂。

循环内部继续使用 `#{departmentValue}` 和 `#{statusValue}` 参数绑定。不能改成 `${...}` 拼接：请求值不得成为SQL结构。`<foreach>` 支持List等可迭代对象，详细语义见[MyBatis动态SQL官方文档](https://mybatis.org/mybatis-3/dynamic-sql.html#foreach)。

search和count都通过 `<include refid="searchConditions"/>` 使用同一份条件。若只修改列表查询而漏掉总数查询，画面会出现“items有4条，total仍按旧条件计算”的规格错误。

## 八、测试中的param为什么可以传多个值

MockMvc的 `param` 方法支持同一个参数名后传入多个字符串：

```java
.param("department", "Sales", "Support")
```

| 参数 | 当前值 | 可接受的值 | 作用 |
| --- | --- | --- | --- |
| name | department | 非null的请求参数名 | 决定绑定到哪个DTO属性 |
| values | Sales、Support | 一个或多个字符串 | 模拟重复出现的同名查询参数 |
| 返回值 | 请求构建器 | 当前请求构建过程 | 可继续追加其他参数与断言 |

测试类继续保留 `@WithMockUser(... roles="ADMIN")`。改修查询条件不等于可以绕过第16章权限；若删除管理员身份，测试得到403，根本没有进入本次查询逻辑。

本次测试必须覆盖四类结果：

| 类别 | 代表用例 | 要证明的内容 |
| --- | --- | --- |
| 正常 | 两个部门＋两个状态 | 同组OR、组间AND、items与total一致 |
| 兼容 | 每组各一个值 | 旧单值请求行为不变 |
| 边界 | 空值、重复值、参数次数上限 | 规范化和请求大小边界明确 |
| 异常与回归 | UNKNOWN、pageSize=101、越界页 | 400规则与旧分页行为未破坏 |

## 九、修改前先保存基准行为

开始编码前，在项目根目录确认当前工作区和原测试结果。下面是PowerShell命令：

```powershell
git status --short
.\mvnw.cmd clean test
```

记录当时的提交编号、测试Profile、测试总数和 `BUILD SUCCESS`。如果改修前已经失败，应先区分既有故障和本次任务，不能把旧失败算成本次改修造成，也不能未经确认顺手修复。

还要用批准的ADMIN测试账号保存两个基准请求：

```text
GET /employees?department=Sales
GET /employees?status=INACTIVE
```

至少记录HTTP状态、items编号、total和排序。改修完成后用相同请求再测一次，才能证明单值兼容。

`git status --short` 只显示工作区和暂存区的简洁状态，不会修改文件；`clean test` 会清理旧构建产物后重新编译并运行测试。Git工作区、分支和提交的完整操作继续参考[Git团队协作课程](../../../tools/git/06_teamwork_and_conflicts.md)。

## 十、按调用链做影响调查

从URL入口沿真实调用关系调查，而不是只按文件名猜测：

```text
GET /employees重复查询参数
  → EmployeeController的@ModelAttribute绑定
  → EmployeeSearchRequest的List与校验
  → EmployeeServiceImpl规范化和值域检查
  → EmployeeMapper.search/count
  → EmployeeMapper.xml共享searchConditions
  → MySQL employees.department/status
  → PageResponse与统一ApiResponse
```

调查结果写出“修改”或“无修改及依据”：

| 调查对象 | 结论 | 依据 |
| --- | --- | --- |
| 接口规格 | 修改 | department、status从单值扩展为重复参数 |
| EmployeeController | 无修改 | 既有 `@ModelAttribute` 可绑定专用DTO的List属性 |
| EmployeeSearchRequest | 修改 | 属性类型、集合大小与元素校验变化 |
| EmployeeService接口 | 无修改 | 方法参数仍是同一个请求DTO类型 |
| EmployeeServiceImpl | 修改 | 需要逐项规范化、去重和部门检查 |
| EmployeeMapper接口 | 无修改 | search/count签名继续接收整个请求对象 |
| EmployeeMapper.xml | 修改 | 等值条件改成IN和foreach，共享给search/count |
| 数据库表与初始化SQL | 无修改 | department、status列和值域已经存在，无DDL和数据迁移 |
| 响应DTO | 无修改 | 票据明确禁止改变响应JSON |
| SecurityConfig | 无修改 | GET列表仍只允许ADMIN |
| 配置与部署手顺 | 无修改 | 没有新增依赖、环境变量、端口或服务设置 |
| 自动化测试 | 修改 | 增加多值、兼容、边界、异常和权限前提 |
| 调用方 | 需要联调 | 新画面要按重复参数方式构造URL，旧画面可继续单值请求 |

“无修改”不是“不调查”。例如数据库不需要DDL，是因为现有列已经能表达多选结果；权限不修改，是因为改修票明确沿用ADMIN限制。

## 十一、控制修改范围并查看差分

完成四个文件后先查看范围：

```powershell
git status --short
git diff --check
git diff -- src/main/java/com/example/employee/dto/request/EmployeeSearchRequest.java
git diff -- src/main/java/com/example/employee/service/impl/EmployeeServiceImpl.java
git diff -- src/main/resources/mapper/EmployeeMapper.xml
git diff -- src/test/java/com/example/employee/integration/EmployeeSearchIntegrationTest.java
```

`git diff --check` 检查差分中的空白错误；`git diff -- 路径` 只查看指定文件的未暂存差分。独立的 `--` 表示后面是路径，避免文件名被当成选项。这些命令不会提交代码。

Review前确认差分中没有以下无关内容：

- 格式化整个项目；
- 重命名无关类和变量；
- 升级Spring Boot、MyBatis或Java版本；
- 改变详情、写接口或响应字段；
- 放宽SecurityConfig；
- 顺手建立未经调查的索引。

## 十二、Review不是只看代码能不能编译

Reviewer按规格和风险检查以下内容：

| 观点 | 本次检查问题 | 通过标准 |
| --- | --- | --- |
| 职责 | 绑定、规范化、SQL是否放在正确位置 | DTO约束输入，Service处理规格，Mapper生成SQL |
| 兼容性 | 单值请求是否仍有效 | 旧URL、响应和排序结果不变 |
| 异常 | 非法元素、过多元素怎样返回 | 统一400，不进入SQL |
| 权限 | USER能否借新参数访问列表 | 仍然403，只有ADMIN通过 |
| SQL | 空集合、占位符、逻辑组合是否正确 | 无IN()，使用#{}，同组IN、组间AND |
| 分页 | items和total条件是否同步 | 共用searchConditions且测试total |
| 数据 | 是否需要DDL、迁移或索引 | 有明确无影响依据，不凭感觉改库 |
| 日志 | 是否打印完整请求或敏感数据 | 只保留必要页码、件数和requestId |
| 测试 | 正常、异常、边界、回归是否齐全 | 每类都有可复核断言 |

### 一次指摘与修正示例

初次实现若只写下面的循环，会保留空值和重复值：

```java
for (String value : values) {
    normalizedValues.add(value.trim());
}
```

Review指摘：`department=&department=Sales&department=Sales` 会产生空字符串和重复参数，规格Q3、Q4没有落实，SQL证据也未覆盖。

修正后使用完整示例中的null检查、空白过滤和 `contains` 去重，并追加 `blankAndDuplicateValuesAreRemovedBeforeSql` 测试。修正完成后重新执行全部测试，不只重跑新用例；Review状态记录为“已修正并再确认”。

## 十三、执行测试并保存可复核证据

在项目根目录执行：

```powershell
.\mvnw.cmd clean test
```

自动化测试通过后，再以ADMIN身份从正式测试入口完成接口验证。自测表至少包含：

| No | 分类 | 请求条件 | 预期 |
| --- | --- | --- | --- |
| 1 | 兼容 | department=Sales | 与改修前编号、total一致 |
| 2 | 正常 | Sales＋Support | 两个部门的记录，组内OR |
| 3 | 正常 | 两部门＋两状态 | 组内OR、组间AND |
| 4 | 边界 | 空值＋重复值 | 忽略空值并去重，200 |
| 5 | 边界 | department 3项 | 允许，结果正确 |
| 6 | 异常 | department 4项 | 400和字段错误 |
| 7 | 异常 | status含UNKNOWN | 400，不执行查询 |
| 8 | 回归 | 越界页 | 200、空items、真实total |
| 9 | 权限 | USER请求列表 | 403 |
| 10 | 回归 | 员工详情与写接口既有测试 | 全部通过 |

证据不是只写“测试OK”。至少保存：

- 票号、代码版本和执行时间；
- 测试环境与test Profile；
- Maven命令、测试总数和最终结果；
- 代表请求、HTTP状态、items编号、total；
- 非法输入的统一错误响应；
- USER的403和ADMIN的200；
- 失败用例、修正内容和再测试结果；
- 测试数据脚本版本。

截图或日志必须隐藏Cookie、Session ID、CSRF令牌、密码、内部地址和个人信息。数据库内容只使用批准的测试数据。

## 十四、完成改修报告

可以按下面格式提交。尖括号内容必须替换为实际证据，不能原样保留：

```text
票号：EMP-241
改修概要：员工列表department、status支持重复参数多选

确定规格：
- 同名参数重复传递
- 单值兼容
- 空值忽略，上限内重复值去重
- 原始department参数最多3次，status最多2次
- 原响应、分页和ADMIN权限不变

修改文件：
- EmployeeSearchRequest.java
- EmployeeServiceImpl.java
- EmployeeMapper.xml
- EmployeeSearchIntegrationTest.java

无影响确认：
- 数据库DDL/DML：无
- 响应DTO：无
- 登录和权限规则：无
- 配置、依赖和部署手顺：无

自测：
- 执行命令：.\mvnw.cmd clean test
- 结果：<实际测试总数与BUILD SUCCESS>
- 接口证据：<证据编号或保存位置>
- 权限证据：<USER 403、ADMIN 200>

Review：
- 指摘：空值与重复值处理不足
- 修正：规范化并追加边界测试
- 再确认：<执行结果>

问题与残留事项：
<没有时明确写“无”；有时写影响、临时处置、负责人和期限>
```

改修报告中的修改文件必须与Git差分一致，自测条件必须与确定规格一致，残留事项不能用“后续确认”等无法追踪的文字结束。

## 十五、常见失败与定位

| 现象 | 原因 | 定位 | 修正 |
| --- | --- | --- | --- |
| 第二个参数值消失 | DTO仍是String | 查看绑定后的请求对象 | 改为List属性并保留同名setter |
| 请求在进入Service前400 | 容器元素校验失败 | 查看统一错误字段 | 核对大小写、长度和值域 |
| SQL出现IN () | 空集合也生成foreach | 查看MyBatis SQL与规范化结果 | 空集合转null并在XML检查非空 |
| items正确而total错误 | search和count条件不一致 | 对照两条SQL | 共用searchConditions |
| 多部门结果为0 | 错写成department=A AND department=B | 查看生成SQL | 同字段改成IN，字段组之间才AND |
| USER得到200 | 改修时误改授权规则 | 执行权限回归测试 | 恢复第16章ADMIN限制 |
| 单值请求行为改变 | 只测试新画面URL | 对照改修前基准 | 增加单值兼容测试并修正绑定 |
| 测试通过但画面失败 | 前端用了逗号格式 | 查看浏览器实际URL | 按确定规格发送重复参数并联调 |

## 十六、练习

### 练习1：独立重做EMP-241

从第18章稳定状态开始，不复制本章最终代码。先提交确认事项和影响调查，再完成多选改修。验收必须包含差分、全部自动化测试、单值兼容、多值、异常、权限和改修报告。

### 练习2：处理追加指摘

Reviewer追加要求：“department重复值应返回400，不再自动去重。”先写出这与当前Q4的冲突，取得规格确认后再修改。不得在规格未更新时只按口头指摘改代码。若确认采用新规则，补充重复值异常测试和统一错误响应。

### 练习3：调查但不实现逗号格式

产品提出也支持 `department=Sales,Support`。调查Spring绑定、URL编码、现有重复参数、前端实现、兼容性和歧义，提交方案比较与待确认事项。本练习不改主线代码。

### 练习4：Review有缺陷的Mapper

把status的 `<foreach>` 临时只写到search查询，不写入共享条件。构造能够让items和total不一致的测试并说明画面影响，然后恢复共享条件并重新执行全部测试。

### 练习5：写障害与残留事项

假设测试环境发现status多选返回500，日志显示XML找不到 `statusValue`。写出时间线、影响范围、requestId、最早错误、原因、修正、再测试和残留事项；不得只写“Mapper错误，已解决”。

## 十七、追加改修演练：把物理删除改为逻辑删除

这是一张独立练习票，不替换本章已经完成的EMP-241，也不直接修改当前稳定代码。目标是训练“一个删除规格会影响读取、唯一性、权限、日志和运维”的调查能力。

```text
票号：EMP-268
标题：员工删除改为逻辑删除
初始要求：DELETE /employees/{id} 不再删除数据库行；普通业务查询不显示已删除员工；管理员历史查询仍可调查删除记录；必须记录deleted_at。
```

### 1. 编码前必须确认的规格

至少向业务或设计负责人确认：

1. 已删除员工的详情返回404还是专用状态？
2. email能否被新员工再次使用？数据库唯一约束如何处理？
3. 已删除员工能否登录，已有Session是否立即失效？
4. 管理员历史查询使用哪个接口、允许哪些条件，是否还需要恢复功能，谁可以恢复？
5. `employee_change_logs` 保留多久，是否记录删除人、删除时间和理由？
6. 关联数据、批处理、统计、导出和外部系统是否仍能看到该员工？
7. 法务或审计要求的保存与最终物理清除期限是什么？

在这些问题没有答案前，不能只加一个字段就宣称完成。

### 2. 设计增量示例

若确定规格采用“保留原email、所有普通查询排除删除记录、管理员可查询但暂不恢复”，可提出以下DDL：

```sql
ALTER TABLE employees
    ADD COLUMN deleted BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN deleted_at DATETIME NULL,
    ADD COLUMN deleted_by BIGINT NULL;
```

逻辑删除Mapper不再执行DELETE，而是只允许未删除记录发生一次状态变化：

```xml
<update id="logicalDelete">
    UPDATE employees
    SET deleted = TRUE,
        deleted_at = CURRENT_TIMESTAMP,
        deleted_by = #{operatorId},
        updated_at = CURRENT_TIMESTAMP
    WHERE id = #{employeeId}
      AND deleted = FALSE
</update>
```

所有普通读取条件都要补 `deleted = FALSE`，包括详情、列表、件数、邮箱存在检查和登录账号关联。可以把共享条件集中在MyBatis SQL片段，但必须检查每一个入口，不能只改列表SQL。影响行数为0时还要区分“不存在”和“已经删除”是否按同一规格处理。

### 3. 影响调查表

| 调查对象 | 必须回答的影响 |
| --- | --- |
| 数据库 | 新字段默认值、索引、email唯一性、旧数据迁移、清除策略 |
| Mapper | DELETE改UPDATE，全部SELECT和COUNT是否排除删除记录 |
| Service与事务 | 删除人来源、履历写入、重复删除、恢复操作是否同事务 |
| Security与Session | 被删除账号能否继续认证，既有Session何时失效 |
| API | 状态码、响应体、详情与搜索行为是否变化 |
| 批处理和外部接口 | 导出、同步、统计是否需要包含删除记录 |
| 运维 | 表增长、备份、审计、最终物理清除和回退方法 |
| 测试 | 正常删除、重复删除、读取不可见、登录禁止、事务回滚、既有功能回归 |

### 4. 自测证据与恢复

至少设计：删除前可查、删除后普通列表/详情不可见、数据库行仍存在且三个删除字段正确、重复删除符合规格、已删除账号不能登录、履历与员工状态共同回滚、未删除员工CRUD和EMP-241多选查询不受影响。还要验证count和items使用相同删除条件，避免分页总件数错误。

本练习只提交规格确认表、影响调查、DDL/SQL差分草案、测试用例和回退步骤，不直接执行ALTER TABLE，也不改写EMP-241代码。这样既练习逻辑删除改修，又保持第20章所需的稳定成果不变。

## 十八、本章稳定状态

完成后，员工列表在保持原单值URL、分页响应和ADMIN权限不变的前提下，支持原始department参数最多3次、status最多2次的重复参数多选；输入经过集合与元素校验、空值处理和上限内去重，MyBatis用安全参数生成IN条件，items与total复用相同条件；本次改修具有基准行为、影响调查、差分、Review指摘、正常/异常/边界/回归测试和可追踪的改修报告。

下一章将使用这套最终代码和第17～18章的构建部署结果完成综合验收与交付，不再新增核心框架能力。
