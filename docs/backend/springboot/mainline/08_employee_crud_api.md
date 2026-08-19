# 第8章 使用MyBatis完成员工CRUD API

> 本章目标：在第7章数据库查询基础上完成员工新增、详情、修改和删除；每个接口都经过Controller、Service、Mapper和MySQL，并返回正确的HTTP状态与404错误结构。

## 一、开始状态与接口规格

第7章已经能够从数据库查询员工详情，但列表和新增仍是临时代码，找不到员工时也还不是正确的404。本章新建或替换：

```text
src/main/java/com/example/employee/
├── controller/EmployeeController.java              # 完整替换
├── exception/EmployeeNotFoundException.java         # 新建
├── exception/ValidationExceptionHandler.java        # 完整替换
├── mapper/EmployeeMapper.java                       # 完整替换
└── service/EmployeeService.java                     # 完整替换
src/main/resources/mapper/EmployeeMapper.xml          # 完整替换
```

`Employee.java`、`EmployeeCreateRequest.java`、`EmployeeResponse.java` 和 `ApiResponse.java` 保持不变。当前把同一个 `EmployeeCreateRequest` 用作新增和完整修改请求；如果新增与修改规则不同，应拆成两个DTO。

CRUD与接口规格如下：

| 操作 | SQL | HTTP接口 | 成功状态 | 成功响应体 |
| --- | --- | --- | ---: | --- |
| Create | `INSERT` | `POST /employees` | 201 | 新员工的 `ApiResponse` |
| Read | `SELECT` | `GET /employees/{id}` | 200 | 员工的 `ApiResponse` |
| Read | `SELECT` | `GET /employees?department=Sales` | 200 | 员工列表的 `ApiResponse` |
| Update | `UPDATE` | `PUT /employees/{id}` | 200 | 修改后员工的 `ApiResponse` |
| Delete | `DELETE` | `DELETE /employees/{id}` | 204 | 无响应体 |

`PUT` 在本章表示对可修改字段进行完整替换，因此请求仍要求姓名、部门和邮箱全部合法。删除成功使用204，按HTTP语义不再返回统一JSON；统一响应结构不能覆盖状态码本身的语义。

## 二、完整替换Mapper接口

用下面代码完整替换 `EmployeeMapper.java`：

```java
package com.example.employee.mapper;

import com.example.employee.entity.Employee;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface EmployeeMapper {

    Employee findById(@Param("id") Long id);

    List<Employee> findList(@Param("department") String department);

    int insert(Employee employee);

    int update(Employee employee);

    int deleteById(@Param("id") Long id);
}
```

`@Mapper`、`@Param`、Mapper代理和 `findById()` 已经用于详情查询。本节第一次出现的三个写入方法都是项目定义的方法：

| 方法 | 参数来源与可接受值 | 返回值与数据库结果 |
| --- | --- | --- |
| `insert(Employee employee)` | Service构造的Employee；`id` 可为 `null`，其他写入字段应满足表约束 | 返回受影响行数，通常为1；执行后MyBatis还会把数据库生成的编号写回 `employee.id` |
| `update(Employee employee)` | Service构造的Employee；必须包含目标 `id` 和三个完整修改字段 | 返回受影响行数；1表示更新一行，0表示当前编号没有匹配行 |
| `deleteById(Long id)` | Controller路径参数经过Service传入；必须是可转换的长整数 | 返回受影响行数；1表示删除一行，0表示没有该员工 |

返回类型使用基本类型 `int`，因为MyBatis写入语句会返回确定的行数，不需要用 `null` 表示“没有结果”。方法声明本身不包含SQL，代理会在XML中寻找同名语句。

`findList(String department)` 接收可为 `null` 的部门条件，返回JDK的 `List<Employee>`；列表可能为空，但不会用 `null` 表示“没有行”。`List` 已在第5章介绍，这里元素类型改为数据库Entity。

## 三、完整替换Mapper XML

用下面代码完整替换 `src/main/resources/mapper/EmployeeMapper.xml`：

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper
        PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "https://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.example.employee.mapper.EmployeeMapper">

    <select id="findById" parameterType="long" resultType="Employee">
        SELECT
            id,
            name,
            department,
            email,
            status,
            created_at AS createdAt,
            updated_at AS updatedAt
        FROM employees
        WHERE id = #{id}
    </select>

    <select id="findList" parameterType="string" resultType="Employee">
        SELECT
            id,
            name,
            department,
            email,
            status,
            created_at AS createdAt,
            updated_at AS updatedAt
        FROM employees
        WHERE (#{department} IS NULL OR department = #{department})
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

### 第一次出现：insert、update、delete映射

| XML写法 | 可接受的值、默认值与必填性 | 执行效果 |
| --- | --- | --- |
| `<insert id="insert">` | `id` 必须与接口方法一致；`parameterType` 可省略并推断，本例明确为Employee | 执行INSERT并返回影响行数 |
| `useGeneratedKeys="true"` | 接受 `true` 或 `false`，默认 `false`；MySQL自增键场景设为 `true` | 要求MyBatis通过JDBC读取数据库生成键 |
| `keyProperty="id"` | 必须填写参数对象中接收键的Java属性名 | INSERT后调用等效的setter，把新编号写回Employee对象 |
| `<update id="update">` | `id` 必须与接口方法一致；参数是Employee | 按主键更新姓名、部门和邮箱，数据库自动刷新 `updated_at` |
| `<delete id="deleteById">` | `id` 必须与接口方法一致；参数是Long | 按主键物理删除一行 |

SQL中的 `#{name}`、`#{department}` 等会读取Employee getter对应的属性值并绑定到JDBC `PreparedStatement`。参数绑定与第7章的 `#{id}` 相同，不要改成 `${name}` 等文本拼接。

列表SQL把同一个 `#{department}` 绑定两次：传入 `null` 时左侧条件成立并查询全部员工；传入部门文本时只保留相等行。`ORDER BY id` 让本次接口结果按编号稳定排序。大型数据表通常会根据索引和执行计划重新设计可选条件；当前先完成单一参数的正确绑定，不引入动态SQL。

本章DELETE是物理删除，提交后该行不能通过普通查询恢复。企业系统可能根据审计要求改用状态字段做逻辑删除，但不能在没有业务规格时假定所有删除都必须采用同一种方案。

## 四、创建员工不存在异常

新建：

```text
src/main/java/com/example/employee/exception/EmployeeNotFoundException.java
```

完整代码：

```java
package com.example.employee.exception;

public class EmployeeNotFoundException extends RuntimeException {

    public EmployeeNotFoundException(Long id) {
        super("员工不存在: " + id);
    }
}
```

### 第一次出现：自定义异常对象

`RuntimeException` 来自自动导入的 `java.lang`，是非受检异常。`extends` 表示 `EmployeeNotFoundException` 继承它，因此Service可以直接 `throw`，不必在每个方法声明 `throws`。

构造方法必须接收一个 `Long id`，`super(message)` 调用父类构造方法保存错误消息。Service执行 `new EmployeeNotFoundException(id)` 创建异常对象，再由 `throw` 中断当前流程；异常不会作为普通返回值继续传给Controller。

这个类只表达“按编号找不到员工”的业务结果，不承载HTTP类。HTTP 404由全局异常处理器决定，保持Service与Web边界分离。

## 五、完整替换EmployeeService

用下面代码完整替换 `EmployeeService.java`：

```java
package com.example.employee.service;

import com.example.employee.dto.EmployeeCreateRequest;
import com.example.employee.entity.Employee;
import com.example.employee.exception.EmployeeNotFoundException;
import com.example.employee.mapper.EmployeeMapper;
import com.example.employee.vo.EmployeeResponse;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class EmployeeService {

    private final EmployeeMapper employeeMapper;

    public EmployeeService(EmployeeMapper employeeMapper) {
        this.employeeMapper = employeeMapper;
    }

    public EmployeeResponse findById(Long id) {
        Employee employee = findEmployeeOrThrow(id);
        return toResponse(employee);
    }

    public List<EmployeeResponse> findList(String department) {
        String filter = normalizeDepartmentFilter(department);
        List<Employee> employees = employeeMapper.findList(filter);
        List<EmployeeResponse> responses = new ArrayList<>();
        for (Employee employee : employees) {
            responses.add(toResponse(employee));
        }
        return responses;
    }

    public EmployeeResponse create(EmployeeCreateRequest request) {
        Employee employee = new Employee();
        employee.setName(request.getName().trim());
        employee.setDepartment(request.getDepartment().trim());
        employee.setEmail(normalizeEmail(request.getEmail()));
        employee.setStatus("ACTIVE");

        int affectedRows = employeeMapper.insert(employee);
        if (affectedRows != 1) {
            throw new IllegalStateException("新增员工失败");
        }
        return findById(employee.getId());
    }

    public EmployeeResponse update(Long id, EmployeeCreateRequest request) {
        findEmployeeOrThrow(id);

        Employee employee = new Employee();
        employee.setId(id);
        employee.setName(request.getName().trim());
        employee.setDepartment(request.getDepartment().trim());
        employee.setEmail(normalizeEmail(request.getEmail()));

        int affectedRows = employeeMapper.update(employee);
        if (affectedRows != 1) {
            throw new EmployeeNotFoundException(id);
        }
        return findById(id);
    }

    public void delete(Long id) {
        int affectedRows = employeeMapper.deleteById(id);
        if (affectedRows != 1) {
            throw new EmployeeNotFoundException(id);
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

### 第一次出现：写入对象和Service方法

`new Employee()` 调用第7章Entity的无参数构造方法，随后 `setName()`、`setDepartment()`、`setEmail()` 和 `setStatus()` 把请求数据组装成持久化对象。请求已通过第6章的 `@Valid`，因此这些必填字符串不会是 `null`；Service仍负责去除首尾空白和应用邮箱小写约定。

| Service方法 | 参数 | 返回值或状态变化 |
| --- | --- | --- |
| `create(request)` | 已校验的新增DTO | INSERT一行，再用生成的 `employee.getId()` 回查并返回EmployeeResponse |
| `findList(department)` | 可省略的部门查询参数 | 查询Entity列表，逐个转换后返回 `List<EmployeeResponse>` |
| `update(id, request)` | 路径编号和已校验的完整修改DTO | 先确认存在，UPDATE一行，再查询并返回数据库最终状态 |
| `delete(id)` | 路径编号 | DELETE一行；返回类型 `void`，成功时没有业务返回值 |
| `findEmployeeOrThrow(id)` | 员工编号 | 找到时返回Employee Entity，找不到时抛出项目异常 |

`affectedRows != 1` 比只判断0更严格：当前接口一次只允许影响一行，其他结果也视为异常。`IllegalStateException` 是JDK非受检异常，表示在当前程序状态下新增没有得到预期的一行；本章不把其内部消息直接作为客户端响应。

`new ArrayList<>()` 创建可添加元素的JDK列表对象；增强for循环逐个取得Employee，`responses.add(value)` 接收一个EmployeeResponse并追加到末尾，返回是否成功添加的 `boolean`，本例不使用该返回值。使用单独的响应列表，避免把Entity直接交给Controller。

创建完成后，`useGeneratedKeys` 已把数据库编号写入同一个Employee对象，所以 `employee.getId()` 能取得新编号。修改后再次SELECT，是为了返回数据库实际保存的值及数据库可能应用的默认行为。

当前的“先查询、再更新”可能在并发下发生状态变化。多步业务操作应使用事务明确边界。最终仍检查UPDATE影响行数，不能只依赖前一次查询。

## 六、完整替换EmployeeController

用下面代码完整替换 `EmployeeController.java`：

```java
package com.example.employee.controller;

import com.example.employee.common.ApiResponse;
import com.example.employee.dto.EmployeeCreateRequest;
import com.example.employee.service.EmployeeService;
import com.example.employee.vo.EmployeeResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
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
            @PathVariable Long id) {
        EmployeeResponse employee = employeeService.findById(id);
        return ResponseEntity.ok(ApiResponse.ok(employee));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<EmployeeResponse>>> findList(
            @RequestParam(required = false) String department) {
        List<EmployeeResponse> employees = employeeService.findList(department);
        return ResponseEntity.ok(ApiResponse.ok(employees));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<EmployeeResponse>> create(
            @Valid @RequestBody EmployeeCreateRequest request) {
        EmployeeResponse employee = employeeService.create(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.ok(employee));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<EmployeeResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody EmployeeCreateRequest request) {
        EmployeeResponse employee = employeeService.update(id, request);
        return ResponseEntity.ok(ApiResponse.ok(employee));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        employeeService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
```

### 第一次出现：PUT、DELETE和无正文响应

`@PutMapping` 和 `@DeleteMapping` 都来自 `org.springframework.web.bind.annotation`，处理者仍是Spring MVC。

| 写法 | 可接受的值、默认值与必填性 | 运行效果 |
| --- | --- | --- |
| `@PutMapping("/{id}")` | 路径字符串可省略，默认空路径；本例必须与详情和删除保持同一 `{id}` | 把PUT请求交给 `update()` |
| `@DeleteMapping("/{id}")` | 路径规则与上面相同 | 把DELETE请求交给 `delete()` |
| `ResponseEntity.noContent()` | 无参数 | 创建状态为204的响应构建器 |
| `.build()` | 无参数 | 完成没有响应体的 `ResponseEntity<Void>` |

`update()` 同时接收路径对象 `Long id` 和请求体对象 `EmployeeCreateRequest request`，二者都由Spring提供。`@Valid` 只校验DTO字段，路径文本能否转成Long由Spring MVC参数绑定负责。

删除方法中的 `Void` 表示响应没有正文类型。204响应不能再附加 `ApiResponse` JSON；如果业务规格要求返回删除结果，应改用200并返回明确响应体，而不是把正文塞进204。

## 七、完整替换全局异常处理器

为了保留第6章的400处理并新增404，用下面代码完整替换 `ValidationExceptionHandler.java`：

```java
package com.example.employee.exception;

import com.example.employee.common.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class ValidationExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidation(
            MethodArgumentNotValidException exception) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();

        exception.getBindingResult()
                .getFieldErrors()
                .forEach(error -> fieldErrors.putIfAbsent(
                        error.getField(),
                        error.getDefaultMessage()));

        ApiResponse<Map<String, String>> body = ApiResponse.fail(
                "参数校验失败",
                fieldErrors);
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnreadableJson(
            HttpMessageNotReadableException exception) {
        ApiResponse<Void> body = ApiResponse.fail(
                "请求JSON格式错误",
                null);
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(EmployeeNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleEmployeeNotFound(
            EmployeeNotFoundException exception) {
        ApiResponse<Void> body = ApiResponse.fail(
                exception.getMessage(),
                null);
        return ResponseEntity.status(404).body(body);
    }
}
```

前两个方法用于处理字段校验失败和JSON解析失败。本节新增的 `handleEmployeeNotFound()` 由Spring在捕获 `EmployeeNotFoundException` 时调用：异常对象作为参数传入，`exception.getMessage()` 返回构造异常时保存的稳定业务消息，`ResponseEntity.status(404)` 接受整数HTTP状态并返回构建器，`.body(body)` 形成最终404响应。

当前消息只包含公开的员工编号，可以返回客户端。数据库异常、SQL和堆栈等内部信息不能照此直接返回；应由统一异常处理和日志记录分别面向客户端与服务端排查。

## 八、按顺序验证完整CRUD

先执行构建，确保Java和XML都能被加载：

```powershell
.\mvnw.cmd test
.\mvnw.cmd spring-boot:run
```

以下操作会真实写入并最终删除一行本地测试数据。不要把地址改成共享、测试或生产环境。另开PowerShell，按顺序执行。

### 1. 新增员工

```powershell
$body = @{
    name = "Sato"
    department = "Development"
    email = "sato.crud@example.com"
} | ConvertTo-Json

$created = Invoke-RestMethod `
    -Uri "http://localhost:8080/employees" `
    -Method Post `
    -ContentType "application/json" `
    -Body $body

$created
$employeeId = $created.data.id
```

预期响应对应HTTP 201，`$employeeId` 保存数据库生成的编号。`Invoke-RestMethod` 把JSON转换为PowerShell对象，因此可以通过 `.data.id` 读取嵌套字段。

### 2. 查询刚创建的员工

```powershell
Invoke-RestMethod `
    -Uri "http://localhost:8080/employees/$employeeId" `
    -Method Get
```

预期状态为200，字段与新增结果一致。

再验证列表和部门条件：

```powershell
Invoke-RestMethod `
    -Uri "http://localhost:8080/employees?department=Development" `
    -Method Get
```

预期 `data` 是员工数组，并包含刚创建的员工。省略 `department` 时应返回全部员工；不存在的部门应返回空数组，而不是404。

### 3. 完整修改员工

```powershell
$updateBody = @{
    name = "Sato Haru"
    department = "Platform"
    email = "sato.haru@example.com"
} | ConvertTo-Json

Invoke-RestMethod `
    -Uri "http://localhost:8080/employees/$employeeId" `
    -Method Put `
    -ContentType "application/json" `
    -Body $updateBody
```

预期状态为200，响应包含修改后的三个字段。随后可在MySQL执行SELECT，确认接口响应和数据库一致。

### 4. 删除并验证404

```powershell
$deleteResponse = Invoke-WebRequest `
    -Uri "http://localhost:8080/employees/$employeeId" `
    -Method Delete

$deleteResponse.StatusCode
```

预期状态码为204且正文为空。再查询同一编号：

```powershell
curl.exe -i `
    "http://localhost:8080/employees/$employeeId"
```

预期状态为404，响应类似：

```json
{
  "success": false,
  "message": "员工不存在: 3",
  "data": null
}
```

编号以实际生成值为准。删除操作已经充当本次练习的清理步骤；第7章的Tanaka和Suzuki测试行不受影响。

## 九、主动观察校验与影响行数

使用空姓名调用POST或PUT，应该在Controller进入Service前返回第6章定义的400。使用不存在编号调用PUT或DELETE，应该由 `EmployeeNotFoundException` 转换成404。

可在调试器中为 `affectedRows` 设置断点，观察：

- 成功INSERT、UPDATE或DELETE通常得到1。
- 对不存在编号直接执行UPDATE或DELETE通常得到0。
- 返回值代表数据库报告的受影响行数，不是HTTP状态码，也不是新员工编号。

MySQL对“把字段更新成原值”的影响行数行为可能受JDBC连接选项影响。本章在更新前先确认存在，并且只要求一次接口最多影响一行；不要把所有数据库产品的行数细节简单视为完全相同。

## 十、常见失败

| 现象 | 常见原因 | 修正 |
| --- | --- | --- |
| INSERT成功但 `employee.getId()` 为 `null` | XML遗漏 `useGeneratedKeys` 或 `keyProperty` 写错 | 恢复两个属性并确认Entity存在 `setId()` |
| XML提示找不到属性 | `#{...}` 名称与Employee getter不一致 | 对照 `name`、`department`、`email`、`status`、`id` |
| PUT返回200但数据库没变化 | Controller未调用Service，或XML的UPDATE条件错误 | 按Controller→Service→Mapper→XML逐层设置断点 |
| DELETE误删多行 | DELETE缺少或写错 `WHERE id = #{id}` | 立即停止操作，检查SQL和备份；写操作必须限定主键 |
| 不存在员工返回500 | 仍在抛 `IllegalArgumentException`，或新异常处理器未生效 | 使用 `EmployeeNotFoundException` 并确认处理器在扫描包内 |
| 无效请求仍写入数据库 | Controller缺少 `@Valid` | 恢复POST和PUT参数上的 `@Valid` |
| 启动提示重复映射 | 接口方法或XML语句重复，旧文件未替换 | 确认每个namespace内的id唯一，并按本章完整替换 |

## 十一、操作练习

初始状态：POST、GET详情、PUT和DELETE已经连接MySQL。

任务：

1. 新增员工A，保存HTTP状态、返回编号和数据库SELECT结果。
2. 修改员工A的部门，验证 `updated_at` 发生变化且其他字段符合PUT请求。
3. 删除员工A，保存204证据，再查询并保存404响应。
4. 临时把DELETE XML的 `WHERE id = #{id}` 改成不存在的编号条件，观察影响行数为0和404；不要删除WHERE，实验后恢复。
5. Review一份故意把 `${id}` 用于删除条件的代码，写出SQL注入风险并改回 `#{id}`。

验收标准：

- 四个接口的状态分别符合201、200、200、204。
- 400、404和成功响应可以清楚区分。
- 新增编号由数据库生成并写回Employee对象。
- Controller不直接注入Mapper，Entity不直接作为API返回。
- 所有用户输入都通过 `#{}` 参数绑定，没有字符串拼接SQL。
- 删除实验只影响本次创建的明确编号，并留下请求、响应和数据库结果证据。

## 十二、当前稳定状态

完成后，请保留可运行的完整链路：

```text
HTTP请求
  → EmployeeController：绑定、校验、HTTP状态
  → EmployeeService：业务流程、Entity与Response转换
  → EmployeeMapper代理：选择映射语句
  → EmployeeMapper.xml：参数化SQL
  → MySQL employees表
```

这套CRUD代码已经形成Controller、Service、Mapper、XML和MySQL的完整链路。不要恢复临时列表或固定新增编号，也不要在Controller中直接操作Mapper。
