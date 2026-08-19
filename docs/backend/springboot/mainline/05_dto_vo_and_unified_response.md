# 第5章 请求DTO、响应对象与统一响应

> 本章目标：用明确类型替换Controller中的临时Map，区分接口输入、接口输出和未来数据库实体，并同时保持正确的HTTP状态码与统一JSON结构。

## 一、开始状态

第4章已有：

```text
controller/EmployeeController.java
dto/EmployeeCreateRequest.java
service/EmployeeService.java
```

Controller→Service调用已经成立，但响应使用 `Map<String, Object>`。Map适合快速观察JSON，不适合作为长期接口契约：字段名容易拼错、字段类型不清楚，重构时编译器也无法充分检查。

本章修改：

```text
src/main/java/com/example/employee/
├── common/
│   └── ApiResponse.java           # 新建
├── controller/
│   └── EmployeeController.java    # 完整替换
├── dto/
│   └── EmployeeCreateRequest.java # 保持字段不变
├── service/
│   └── EmployeeService.java       # 完整替换
└── vo/
    └── EmployeeResponse.java      # 新建
```

## 二、区分接口输入、输出与数据库对象

| 对象 | 数据方向 | 当前例子 | 变化原因 |
| --- | --- | --- | --- |
| Request DTO | 客户端→Controller | `EmployeeCreateRequest` | 请求字段、校验规则会变化 |
| Response/VO | Controller→客户端 | `EmployeeResponse` | 对外字段和显示形式会变化 |
| Entity | Mapper↔数据库 | 接入数据库后创建 | 表字段和持久化规则会变化 |

本项目使用 `VO` 包保存响应对象；其他项目也常命名为 `response` 或 `dto.response`。名称可以不同，但必须明确输入、输出和持久化对象的边界。

不要为了减少类的数量，直接把未来的Employee Entity作为请求和响应对象。否则数据库新增内部字段时可能意外暴露给客户端，接口字段变化也会反向影响表映射。

## 三、确认请求DTO保持连续

`EmployeeCreateRequest.java` 继续使用第3章确定的三个字段，不重复新建，也不突然改变项目状态：

```java
package com.example.employee.dto;

public class EmployeeCreateRequest {
    private String name;
    private String department;
    private String email;

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

当前请求DTO先保持纯字段结构。校验注解应在引入校验依赖和统一错误响应后加入。

## 四、创建员工响应对象

新建：

```text
src/main/java/com/example/employee/vo/EmployeeResponse.java
```

完整代码：

```java
package com.example.employee.vo;

public class EmployeeResponse {
    private final Long id;
    private final String name;
    private final String department;
    private final String email;

    public EmployeeResponse(
            Long id,
            String name,
            String department,
            String email) {
        this.id = id;
        this.name = name;
        this.department = department;
        this.email = email;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDepartment() {
        return department;
    }

    public String getEmail() {
        return email;
    }
}
```

响应对象由构造方法一次建立，不提供setter，避免返回过程中被意外修改。Jackson通过公共getter读取属性并生成JSON字段。

### 第一次出现：响应对象的创建与读取

`EmployeeResponse` 是项目自己定义的输出类，不是Spring类。Service执行 `new EmployeeResponse(id, name, department, email)` 时调用它的构造方法，四个参数必须依次对应 `Long`、`String`、`String`、`String`；构造方法没有返回类型，但会完成新对象的初始化。

字段上的 `final` 表示每个字段只能在声明或构造过程中赋值一次。`getId()`、`getName()` 等无参数方法返回字段值，既供业务代码读取，也供Jackson序列化响应。这里不需要setter：请求DTO需要被逐项写入，而响应对象是在Service中一次构造完成的。

## 五、创建统一响应结构

新建：

```text
src/main/java/com/example/employee/common/ApiResponse.java
```

完整代码：

```java
package com.example.employee.common;

public class ApiResponse<T> {
    private final boolean success;
    private final String message;
    private final T data;

    private ApiResponse(boolean success, String message, T data) {
        this.success = success;
        this.message = message;
        this.data = data;
    }

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, "success", data);
    }

    public static <T> ApiResponse<T> fail(String message, T data) {
        return new ApiResponse<>(false, message, data);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public T getData() {
        return data;
    }
}
```

泛型参数 `T` 表示真实业务数据的类型：

```text
ApiResponse<EmployeeResponse>
            └─ data是一个员工

ApiResponse<List<EmployeeResponse>>
            └─ data是一组员工

ApiResponse<Map<String, String>>
            └─ 用它保存字段错误
```

### 第一次出现：泛型、静态工厂方法和ApiResponse对象

`T` 是类型占位符，创建或返回具体类型时才确定。例如 `ApiResponse<EmployeeResponse>` 中，字段 `T data` 就等同于 `EmployeeResponse data`。这样一个响应类可以安全地包装员工、列表或错误信息，而不必把 `data` 写成含义过宽的 `Object`。

| 代码 | 参数 | 返回值与作用 |
| --- | --- | --- |
| `ApiResponse.ok(T data)` | 必须传入本次成功响应的数据；具体类型由实参推断 | 返回 `success=true`、消息为 `success` 的 `ApiResponse<T>` |
| `ApiResponse.fail(String message, T data)` | 必须传入外部错误消息和错误详情；没有详情时可传 `null` | 返回 `success=false` 的 `ApiResponse<T>` |
| `new ApiResponse<>(...)` | 依次传入成功标识、消息、数据 | 创建包装对象；`<>` 由返回类型推断实际泛型 |

两个工厂方法带有 `static`，因此通过类名调用，不需要先创建 `ApiResponse` 对象。构造方法声明为 `private`，表示类外不能随意 `new`，统一通过 `ok()` 和 `fail()` 建立符合约定的结果。`isSuccess()` 使用布尔属性常见的 `is...` getter命名，其余getter的作用与第3章相同。

统一响应体是本项目的接口约定，不是REST API唯一正确的格式。即使响应体里有 `success`，仍必须使用正确的HTTP状态码；客户端和网关首先依靠状态码判断请求结果。

## 六、完整替换EmployeeService

当前还没有数据库，Service先返回稳定响应对象。完整替换：

```java
package com.example.employee.service;

import com.example.employee.dto.EmployeeCreateRequest;
import com.example.employee.vo.EmployeeResponse;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

@Service
public class EmployeeService {

    public EmployeeResponse findById(Long id) {
        String name = id == 1001L ? "Tanaka" : "Unknown";
        return new EmployeeResponse(
                id,
                name,
                "Sales",
                "tanaka@example.com");
    }

    public List<EmployeeResponse> findList(String department) {
        String selectedDepartment = normalizeDepartment(department);
        return List.of(new EmployeeResponse(
                1001L,
                "Tanaka",
                selectedDepartment.equals("ALL") ? "Sales" : selectedDepartment,
                "tanaka@example.com"));
    }

    public EmployeeResponse create(EmployeeCreateRequest request) {
        return new EmployeeResponse(
                1002L,
                request.getName(),
                normalizeDepartment(request.getDepartment()),
                normalizeEmail(request.getEmail()));
    }

    private String normalizeDepartment(String department) {
        if (department == null || department.isBlank()) {
            return "ALL";
        }
        return department.trim();
    }

    private String normalizeEmail(String email) {
        if (email == null) {
            return null;
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
```

### 第一次出现：List及本节新增方法

`List` 来自JDK的 `java.util.List`，表示有顺序的一组元素。`List<EmployeeResponse>` 限定每个元素必须是员工响应对象。`List.of(element...)` 是静态工厂方法，接收一个或多个元素并返回不可修改的列表；本例只传入一个 `EmployeeResponse`。如果继续调用 `add()` 会抛出异常。接入数据库查询后，可把这里替换为真实查询结果集合。

`new EmployeeResponse(...)` 每执行一次就创建一个新的响应对象。`selectedDepartment.equals("ALL")` 调用字符串的 `equals(Object other)` 比较内容，参数是另一个对象，返回 `boolean`；它和比较对象身份的 `==` 不同。

`email.trim()` 会返回去除首尾空白后的新字符串；随后调用 `toLowerCase(Locale.ROOT)` 返回转为小写的新字符串。`Locale` 来自 `java.util`，`Locale.ROOT` 表示不依赖服务器语言环境的中性规则，避免部署到不同地区时转换结果变化。本项目把邮箱转小写作为简化约定；真实系统应先确认业务是否允许统一处理整个邮箱地址。

这是接入Entity和Mapper前的临时实现。Service方法不接触 `ResponseEntity`、`HttpStatus` 或请求URL，保持业务代码与HTTP边界分离。

## 七、完整替换EmployeeController

```java
package com.example.employee.controller;

import com.example.employee.common.ApiResponse;
import com.example.employee.dto.EmployeeCreateRequest;
import com.example.employee.service.EmployeeService;
import com.example.employee.vo.EmployeeResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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
            @RequestBody EmployeeCreateRequest request) {
        EmployeeResponse employee = employeeService.create(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.ok(employee));
    }
}
```

责任分配如下：

| 代码 | 责任 |
| --- | --- |
| `employeeService.create(request)` | 执行创建员工用例并返回结果 |
| `ApiResponse.ok(employee)` | 形成项目约定的成功响应体 |
| `ResponseEntity.status(CREATED)` | 设置真实HTTP状态码201 |

不要让 `ApiResponse.ok()` 决定HTTP状态。相同响应体结构可以配合200、201等不同状态，错误响应也必须配合4xx或5xx。

这里第一次使用了 `ResponseEntity.ok(body)`：它是Spring提供的静态快捷方法，必须传入响应体，返回状态为200的 `ResponseEntity<T>`。详情接口中 `T` 是 `ApiResponse<EmployeeResponse>`；列表接口中则是 `ApiResponse<List<EmployeeResponse>>`。POST仍使用第3章已经解释的 `status(...).body(...)`，因为创建成功需要201而不是200。

## 八、运行并验证成功响应

```powershell
.\mvnw.cmd spring-boot:run
```

### 详情接口

```powershell
$response = Invoke-WebRequest `
    -Uri "http://localhost:8080/employees/1001" `
    -Method Get

$response.StatusCode
$response.Content
```

预期状态码200，响应体：

```json
{
  "success": true,
  "message": "success",
  "data": {
    "id": 1001,
    "name": "Tanaka",
    "department": "Sales",
    "email": "tanaka@example.com"
  }
}
```

### 新增接口

```powershell
$body = @{
    name = "Sato"
    department = " Development "
    email = " SATO@EXAMPLE.COM "
} | ConvertTo-Json

$response = Invoke-WebRequest `
    -Uri "http://localhost:8080/employees" `
    -Method Post `
    -ContentType "application/json" `
    -Body $body

$response.StatusCode
$response.Content
```

预期状态码201；响应中部门首尾空白被删除，邮箱变成小写。说明Controller绑定、Service处理、响应对象、Jackson序列化和HTTP状态已经形成完整链路。

## 九、统一结构不能替代HTTP语义

下面的组合是正确方向：

| 场景 | HTTP状态 | `success` |
| --- | ---: | --- |
| 查询成功 | 200 | `true` |
| 创建成功 | 201 | `true` |
| 请求参数错误 | 400 | `false` |
| 未登录 | 401 | `false` |
| 无权限 | 403 | `false` |
| 资源不存在 | 404 | `false` |
| 服务端未知错误 | 500 | `false` |

如果所有情况都返回HTTP 200，监控、代理、前端公共处理和接口测试就难以正确区分结果。参数错误应返回400；资源不存在应返回404；服务端未知异常应返回500。

## 十、常见失败

| 现象 | 原因 | 修正 |
| --- | --- | --- |
| JSON中没有对象字段 | 响应类缺少可访问的getter | 检查 `EmployeeResponse` 和 `ApiResponse` getter |
| 编译提示泛型类型不兼容 | `ApiResponse<T>` 中的T与data类型不一致 | 从方法返回类型反推实际data类型 |
| 创建成功仍返回200 | 直接返回对象，没有设置201 | 使用 `ResponseEntity.status(CREATED)` |
| DTO字段改了但响应没变 | 输入对象和输出对象职责不同 | 同步检查Service映射逻辑和响应对象 |
| Entity字段被直接暴露 | 把持久化对象作为Controller返回值 | 转换为专用响应对象 |

## 十一、操作练习

初始状态：详情、列表和新增接口均返回 `ApiResponse<T>`。

任务：

1. 新建 `EmployeeListItemResponse`，只包含 `id`、`name`、`department`。
2. 把列表接口改为 `ApiResponse<List<EmployeeListItemResponse>>`，列表中不返回邮箱。
3. 保持详情接口仍返回完整邮箱，比较两个接口的数据契约。
4. 检查POST仍返回201，GET仍返回200。

验收标准：

- 请求DTO没有被直接作为响应返回。
- 列表和详情可以拥有不同输出字段。
- `ApiResponse<T>` 的泛型与实际 `data` 类型一致。
- HTTP状态码与响应体中的成功标识一致。

## 十二、当前稳定状态

当前工程至少保留：

```text
common/ApiResponse.java
controller/EmployeeController.java
dto/EmployeeCreateRequest.java
service/EmployeeService.java
vo/EmployeeResponse.java
```

接口成功响应已经稳定。请求校验能力可以在请求DTO、Controller和异常处理包上继续增加，不改变成功响应字段。
