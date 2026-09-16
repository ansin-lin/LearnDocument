# 第10章 完成员工增删改查

> 本章目标：在第9章真实详情查询的基础上完成员工列表、新增、完整修改和删除，并用HTTP响应与数据库结果共同证明每次操作正确。

CRUD是Create、Read、Update、Delete的缩写。接口返回成功只说明请求处理结束；对于写操作，还必须确认数据库实际新增、修改或删除了预期的一行。

本章完成下面的闭环：

```text
新增员工
  → 用生成编号查询
  → 完整修改
  → 再次查询
  → 删除
  → 查询确认404
```

## 一、接口规格与改动范围

### 1. 本章接口规格

| 规格编号 | 操作 | HTTP接口 | 输入 | 成功结果 |
| --- | --- | --- | --- | --- |
| EMP-LIST-01 | 查询列表 | `GET /employees?department=Sales` | `department`可选；省略、空或空白表示全部 | 200和列表响应；无结果返回空数组 |
| EMP-POST-01 | 新增 | `POST /employees` | 新增请求JSON | 201、Location响应头和保存后的详情 |
| EMP-GET-01 | 查询详情 | `GET /employees/{id}` | 路径编号 | 200和详情；不存在返回404 |
| EMP-PUT-01 | 完整修改 | `PUT /employees/{id}` | 路径编号和修改请求JSON | 200和修改后的详情；不存在返回404 |
| EMP-DELETE-01 | 物理删除 | `DELETE /employees/{id}` | 路径编号 | 204且无正文；不存在或重复删除返回404 |

新增和修改都要求姓名、部门和邮箱完整提供，并继续使用第8章的格式校验及部门业务规则。邮箱与其他记录重复时返回409。JSON损坏或字段校验失败仍返回400。

### 2. 为什么PUT要求三个字段全部提供

本章把PUT定义为“完整替换员工当前允许修改的字段”。因此即使只想改部门，也要同时提交姓名和邮箱。路径中的 `id` 决定修改哪一行，请求体不允许另带一个可能冲突的编号。

部分修改通常使用PATCH，但它需要定义“字段省略”和“明确改为null”的区别。本章不同时引入另一套更新语义。

### 3. 本章文件改动

```text
src/main/java/com/example/employee/
├── controller/EmployeeController.java              ← 完整替换
├── dto/request/EmployeeUpdateRequest.java           ← 新建
├── mapper/EmployeeMapper.java                       ← 完整替换
└── service/
    ├── EmployeeService.java                         ← 完整替换
    └── impl/EmployeeServiceImpl.java                ← 完整替换

src/main/resources/mapper/EmployeeMapper.xml         ← 完整替换
```

第9章的 `Employee`、数据库表、连接配置继续使用。第7～8章的 `ApiResponse`、异常类和全局异常处理器也保持不变。

第8章的 `POST /employees/preview` 是不保存数据的过渡接口。本章用真实的 `POST /employees` 替换它，因此Controller和Service不再保留preview方法。

## 二、完整示例

先完成本节全部文件，再从第三节开始依次理解第一次出现的内容。只替换一部分文件会造成接口方法和XML语句暂时不对应。

### 1. 新建EmployeeUpdateRequest.java

```java
package com.example.employee.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class EmployeeUpdateRequest {

    @NotBlank(message = "员工姓名不能为空")
    @Size(max = 50, message = "员工姓名不能超过50个字符")
    private String name;

    @NotBlank(message = "部门不能为空")
    @Size(max = 50, message = "部门不能超过50个字符")
    private String department;

    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    @Size(max = 100, message = "邮箱不能超过100个字符")
    private String email;

    public EmployeeUpdateRequest() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
```

### 2. 完整替换EmployeeMapper.java

```java
package com.example.employee.mapper;

import com.example.employee.entity.Employee;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface EmployeeMapper {

    Employee findById(@Param("id") Long id);

    List<Employee> findList(
            @Param("department") String department);

    int insert(Employee employee);

    int update(Employee employee);

    int deleteById(@Param("id") Long id);
}
```

### 3. 完整替换EmployeeMapper.xml

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper
        PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
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

    <select id="findById"
            parameterType="long"
            resultMap="employeeResultMap">
        SELECT
            id,
            name,
            department,
            email,
            status,
            created_at,
            updated_at
        FROM employees
        WHERE id = #{id}
    </select>

    <select id="findList"
            parameterType="string"
            resultMap="employeeResultMap">
        SELECT
            id,
            name,
            department,
            email,
            status,
            created_at,
            updated_at
        FROM employees
        WHERE (#{department} IS NULL
               OR department = #{department})
        ORDER BY id
    </select>

    <insert id="insert"
            parameterType="Employee"
            useGeneratedKeys="true"
            keyProperty="id">
        INSERT INTO employees (
            name,
            department,
            email,
            status
        ) VALUES (
            #{name},
            #{department},
            #{email},
            #{status}
        )
    </insert>

    <update id="update" parameterType="Employee">
        UPDATE employees
        SET
            name = #{name},
            department = #{department},
            email = #{email}
        WHERE id = #{id}
    </update>

    <delete id="deleteById" parameterType="long">
        DELETE FROM employees
        WHERE id = #{id}
    </delete>

</mapper>
```

### 4. 完整替换EmployeeService.java

```java
package com.example.employee.service;

import com.example.employee.dto.request.EmployeeCreateRequest;
import com.example.employee.dto.request.EmployeeUpdateRequest;
import com.example.employee.dto.response.EmployeeListItemResponse;
import com.example.employee.dto.response.EmployeeResponse;

import java.util.List;

public interface EmployeeService {

    EmployeeResponse findById(Long id);

    List<EmployeeListItemResponse> findList(String department);

    EmployeeResponse create(EmployeeCreateRequest request);

    EmployeeResponse update(
            Long id,
            EmployeeUpdateRequest request);

    void delete(Long id);
}
```

### 5. 完整替换EmployeeServiceImpl.java

```java
package com.example.employee.service.impl;

import com.example.employee.dto.request.EmployeeCreateRequest;
import com.example.employee.dto.request.EmployeeUpdateRequest;
import com.example.employee.dto.response.EmployeeListItemResponse;
import com.example.employee.dto.response.EmployeeResponse;
import com.example.employee.entity.Employee;
import com.example.employee.exception.DuplicateEmailException;
import com.example.employee.exception.EmployeeNotFoundException;
import com.example.employee.exception.EmployeeSystemException;
import com.example.employee.exception.InvalidDepartmentException;
import com.example.employee.mapper.EmployeeMapper;
import com.example.employee.service.EmployeeService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class EmployeeServiceImpl implements EmployeeService {

    private final EmployeeMapper employeeMapper;

    public EmployeeServiceImpl(EmployeeMapper employeeMapper) {
        this.employeeMapper = employeeMapper;
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

        return responses;
    }

    @Override
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

        return findById(employee.getId());
    }

    @Override
    public EmployeeResponse update(
            Long id,
            EmployeeUpdateRequest request) {
        validateDepartment(request.getDepartment());
        findEmployeeOrThrow(id);

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

### 6. 完整替换EmployeeController.java

```java
package com.example.employee.controller;

import com.example.employee.common.ApiResponse;
import com.example.employee.dto.request.EmployeeCreateRequest;
import com.example.employee.dto.request.EmployeeUpdateRequest;
import com.example.employee.dto.response.EmployeeListItemResponse;
import com.example.employee.dto.response.EmployeeResponse;
import com.example.employee.service.EmployeeService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

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
        EmployeeResponse employee = employeeService.findById(id);
        return ResponseEntity.ok(ApiResponse.success(employee));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<EmployeeListItemResponse>>> findList(
            @RequestParam(
                    name = "department",
                    required = false) String department) {
        List<EmployeeListItemResponse> employees =
                employeeService.findList(department);
        return ResponseEntity.ok(ApiResponse.success(employees));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<EmployeeResponse>> create(
            @Valid @RequestBody EmployeeCreateRequest request) {
        EmployeeResponse employee = employeeService.create(request);
        URI location = URI.create("/employees/" + employee.getId());
        return ResponseEntity
                .created(location)
                .body(ApiResponse.success(employee));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<EmployeeResponse>> update(
            @PathVariable(name = "id") Long id,
            @Valid @RequestBody EmployeeUpdateRequest request) {
        EmployeeResponse employee = employeeService.update(id, request);
        return ResponseEntity.ok(ApiResponse.success(employee));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable(name = "id") Long id) {
        employeeService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
```

## 三、新增和修改为什么使用不同请求DTO

`EmployeeCreateRequest` 和 `EmployeeUpdateRequest` 当前字段与约束相同，但表达的接口用途不同。以后新增可能允许省略某个默认字段，修改也可能增加版本号；分开后可以独立改变契约，不会无意影响另一个接口。

请求DTO只包含客户端允许填写的姓名、部门和邮箱。下面这些系统字段不能从请求直接复制：

| 字段 | 来源 | 为什么不由客户端填写 |
| --- | --- | --- |
| `id` | MySQL自增主键 | 客户端不能决定系统身份 |
| `status` | Service固定初始值 `ACTIVE` | 初始状态由业务规则决定 |
| `createdAt` | 数据库默认时间 | 记录真实插入时间 |
| `updatedAt` | 数据库自动维护 | 记录真实数据库更新时间 |

Controller上的 `@Valid` 先检查DTO格式；Service随后检查允许部门并整理文本；Mapper最后把Entity属性绑定到SQL。不同检查位置保护不同边界。

## 四、INSERT和生成主键怎样工作

Mapper的 `insert(Employee employee)` 接收Service创建的持久化对象，返回数据库报告的影响行数。

XML中的关键属性是：

| 属性 | 可接受的值 | 默认值或必填性 | 作用 |
| --- | --- | --- | --- |
| `useGeneratedKeys` | `true`或 `false` | 默认 `false` | 使用JDBC取得数据库生成键 |
| `keyProperty` | 参数对象的Java属性名 | 启用生成键时必须正确填写 | 把生成编号写回 `employee.id` |
| `parameterType` | 类型别名或完整类名 | 可由MyBatis推断 | 本例明确参数是Employee |

INSERT之前 `employee.getId()` 是 `null`；成功后MyBatis调用等效的 `setId()`，同一个对象便能取得新编号。Service用该编号重新查询，是为了返回数据库实际保存的最终状态。参阅[MyBatis Mapper XML官方说明](https://mybatis.org/mybatis-3/sqlmap-xml.html)。

`affectedRows != 1` 或生成编号仍为 `null` 都不符合“新增一名员工”的规格，因此转为内部系统异常。它不是HTTP状态，也不是生成的员工编号。

## 五、列表查询与响应转换

查询参数省略、空字符串或纯空白时，`normalizeDepartmentFilter()` 返回 `null`；SQL左侧条件成立，从而查询全部员工。提供部门时只返回完全相等的记录。

Mapper返回 `List<Employee>`，Service逐项转换成 `EmployeeListItemResponse`。列表响应不包含邮箱、状态和时间。没有记录时返回空列表和HTTP 200，而不是404。

```text
List<Employee>
  → for循环逐项转换
  → List<EmployeeListItemResponse>
```

当前 `WHERE (参数为空 OR 列等于参数)` 便于先完成一个可选条件，但大型数据表仍需结合索引与执行计划评估。条件增多和分页将在后续章节统一处理。

## 六、PUT、路径编号和影响行数

`@PutMapping("/{id}")` 来自Spring MVC，写在Controller方法上，把PUT请求交给 `update()`。方法同时收到：

- 路径中的 `Long id`，决定目标记录；
- JSON转换并校验后的 `EmployeeUpdateRequest`，提供完整新值。

Service不会信任请求体中的编号，因为修改DTO根本不定义id。这样不会出现URL要求修改1001、请求体却要求修改1002的冲突。

UPDATE返回的 `int` 是数据库影响行数。不同驱动设置对“把值改成原值”的计数可能不同，因此代码先确认记录存在；UPDATE返回0时再次查询，只有记录确实消失才返回404。超过1行违反主键更新规格，返回内部错误。

当前“查询存在→UPDATE→再次查询”不是一个不可分割的整体。并发请求可能在步骤之间改变同一行，后提交者也可能覆盖先提交者。复杂业务需要事务和乐观锁等机制；当前必须先认识这种风险，不能把一次演示成功当作并发安全。

## 七、DELETE语义与物理删除

`@DeleteMapping("/{id}")` 把DELETE请求交给Controller的 `delete()`。Mapper SQL必须保留 `WHERE id = #{id}`，一次只允许影响一行：

- 返回1：删除成功，Controller返回204；
- 返回0：员工不存在或已经删除，返回404；
- 其他值：违反单行删除规格，返回500通用错误。

`ResponseEntity.noContent().build()` 创建204响应。204表示没有响应正文，因此不再包裹 `ApiResponse`。

本章采用物理删除：数据库行真正消失，普通查询无法恢复。逻辑删除通常新增删除标志并让所有查询排除已删除记录，适用于审计或恢复需求，但也会影响唯一约束、查询条件和数据保留规则。两者必须由业务规格决定，本章不能同时混用。

## 八、数据库唯一约束怎样成为409

第9章已经在 `email` 上定义唯一约束。新增或修改成已有邮箱时，MySQL拒绝语句；MyBatis-Spring把对应数据访问错误转换为Spring的 `DuplicateKeyException`。Service捕获它并抛出项目的 `DuplicateEmailException`，第7章全局异常处理器再返回409。

```text
MySQL唯一约束失败
  → DuplicateKeyException
  → DuplicateEmailException
  → GlobalExceptionHandler
  → HTTP 409
```

这不是把数据库内部错误直接暴露给客户端。外部只收到稳定业务消息，SQL、表名和堆栈仍留在服务端。`DuplicateKeyException`用于主键或唯一约束冲突，参阅[Spring官方API说明](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/dao/DuplicateKeyException.html)。

当前表由数据库生成主键，唯一业务键只有邮箱，所以能够这样转换。如果以后增加其他唯一约束，应按约束和业务规格细分，不能把所有数据完整性错误都说成“邮箱重复”。MySQL唯一约束会拒绝重复值，可参考[MySQL 8.0约束说明](https://dev.mysql.com/doc/refman/8.0/en/constraint-primary-key.html)。

## 九、完整CRUD调用链

### 1. 新增

```text
POST JSON
→ EmployeeCreateRequest校验
→ Service检查部门并创建Employee
→ Mapper执行INSERT并回填id
→ Service按id查询并转换Response
→ Controller返回201和Location
```

`URI` 来自JDK的 `java.net.URI`。`URI.create(String)` 接收非空且语法正确的URI文本，返回URI对象；本例的编号来自数据库，所以形成 `/employees/{id}`。`ResponseEntity.created(location)` 固定状态为201并设置 `Location` 响应头，指出新资源的查询地址。

### 2. 修改和删除

```text
PUT路径id + JSON → 完整替换 → SELECT确认 → 200
DELETE路径id → 物理删除 → 204无正文
```

Controller不直接注入Mapper，Entity也不直接作为JSON返回。HTTP、业务流程、SQL和接口字段继续由不同对象负责。

## 十、按顺序验证数据库状态

以下操作会在本地 `employee_db` 中创建、修改并删除一行。执行前确认连接的不是共享、测试或生产数据库。

### 1. 构建和启动

在设置了数据库环境变量的PowerShell中执行：

```powershell
.\mvnw.cmd clean test
.\mvnw.cmd spring-boot:run
```

### 2. 新增并保存生成编号

```powershell
$createBody = @{
    name = "Sato"
    department = "Development"
    email = "sato.crud@example.com"
} | ConvertTo-Json

$created = Invoke-WebRequest `
    -Uri "http://localhost:8080/employees" `
    -Method Post `
    -ContentType "application/json" `
    -Body $createBody

$created.StatusCode
$created.Headers.Location
$created.Content

$createdObject = $created.Content | ConvertFrom-Json
$employeeId = $createdObject.data.id
```

预期状态201，Location为 `/employees/实际编号`。随后在MySQL确认：

```sql
SELECT id, name, department, email, status, created_at, updated_at
FROM employees
WHERE email = 'sato.crud@example.com';
```

### 3. 查询和修改

```powershell
Invoke-RestMethod `
    -Uri "http://localhost:8080/employees/$employeeId" `
    -Method Get

$updateBody = @{
    name = "Sato Haru"
    department = "Support"
    email = "sato.haru@example.com"
} | ConvertTo-Json

Invoke-RestMethod `
    -Uri "http://localhost:8080/employees/$employeeId" `
    -Method Put `
    -ContentType "application/json" `
    -Body $updateBody
```

修改后再次查询接口，并用SQL核对实际数据库值：

```sql
SELECT id, name, department, email, status, created_at, updated_at
FROM employees
WHERE id = /* 替换为实际编号 */ 1003;
```

不要直接照抄1003；必须替换成 `$employeeId`显示的真实编号。

### 4. 重复邮箱、404和400

尝试把测试员工邮箱改为 `tanaka@example.com`，预期409且原数据库值不变。再使用不存在编号执行PUT和DELETE，预期404。提交缺失姓名或非法部门，预期400且数据库没有新增或修改记录。

每次失败后都执行SELECT确认数据库状态，不能只看HTTP响应。

### 5. 删除和清理

```powershell
$deleted = Invoke-WebRequest `
    -Uri "http://localhost:8080/employees/$employeeId" `
    -Method Delete

$deleted.StatusCode
```

预期204且正文为空。再次GET同一编号应为404，再执行相同DELETE也应为404。最后用SQL确认行已不存在：

```sql
SELECT id, name, department, email
FROM employees
WHERE id = /* 替换为实际编号 */ 1003;
```

结果应为0行。本次练习只删除自己刚创建并记录编号的员工，不删除第9章的1001和1002样例。

## 十一、常见失败与定位

| 现象 | 所在位置 | 常见原因 | 修正 |
| --- | --- | --- | --- |
| INSERT成功但id为null | Mapper XML | 缺少 `useGeneratedKeys`或 `keyProperty`错误 | 核对Entity的 `id`属性和setter |
| XML找不到属性 | 参数映射 | `#{...}`与Employee属性不一致 | 对照Entity getter和XML参数名 |
| POST仍进入preview | Controller | 旧方法未完整替换 | 确认映射是 `@PostMapping`且无 `/preview` |
| PUT返回200但数据库没变化 | 调用链或SQL | Controller、Service、Mapper或WHERE错误 | 逐层核对并用SELECT确认 |
| DELETE影响多行 | SQL安全 | WHERE缺失或错误 | 立即停止，检查备份和影响范围；不要继续请求 |
| 重复邮箱返回500 | 异常转换 | 表缺少唯一约束或Service未转换异常 | 核对DDL、异常类型和409处理器 |
| 204响应带JSON | Controller | 删除接口仍调用 `.body(...)` | 使用 `noContent().build()` |
| 无效请求仍写入数据库 | Controller | POST或PUT缺少 `@Valid` | 恢复校验并验证写入前停止 |

## 十二、规格理解、影响调查、Review与练习

### 练习1：保存完整CRUD证据

按“新增→查询→修改→再查询→删除→确认不存在”执行一次。每一步保存规格编号、请求、预期状态、实际状态、响应关键字段、SQL查询结果、判定和清理结果。

### 练习2：验证更新边界

分别提交与原值相同的数据、缺少一个字段的数据和不存在编号。记录影响行数或HTTP状态，并解释为什么三种结果不能用同一个“更新失败”消息代替。

### 练习3：Review危险DELETE

Review下面的错误SQL，但不要执行：

```sql
DELETE FROM employees
WHERE department = #{department};
```

指出它为什么不符合“按员工编号只删除一行”的规格，并给出应核对的Mapper方法、XML参数和自测项目。

### 练习4：影响调查

改修要求：“删除员工后仍需保留审计记录，普通查询不显示已删除员工。”只做影响调查，不直接改代码。至少调查表字段、既有数据、所有SELECT、DELETE、唯一邮箱规则、Response、恢复方式和测试数据。

### 练习5：并发覆盖风险说明

两名操作人员先后读取同一员工，A修改部门，B用旧页面修改姓名并提交完整PUT。说明为什么B可能覆盖A的部门，并列出需要向规格负责人确认的冲突处理规则；本练习不自行增加锁或版本字段。

## 十三、本章稳定状态

完成练习并删除测试员工后，工程核心文件为：

```text
src/main/java/com/example/employee/
├── controller/EmployeeController.java
├── dto/request/
│   ├── EmployeeCreateRequest.java
│   └── EmployeeUpdateRequest.java
├── entity/Employee.java
├── mapper/EmployeeMapper.java
└── service/
    ├── EmployeeService.java
    └── impl/EmployeeServiceImpl.java

src/main/resources/mapper/EmployeeMapper.xml
```

此时你应能够：

1. 按接口规格实现列表、新增、详情、完整修改和删除；
2. 区分请求DTO可写字段和数据库生成字段；
3. 取得自增主键并回查数据库最终状态；
4. 根据影响行数处理成功、404和内部异常；
5. 用唯一约束和异常转换稳定返回409；
6. 解释PUT完整替换、DELETE物理删除和204无正文；
7. 用HTTP响应与SELECT结果完成CRUD验证闭环；
8. 识别完整更新覆盖他人修改的风险。

下一章会在当前可运行CRUD上增加结构化日志和故障定位方法，不改变这些接口的业务语义。
