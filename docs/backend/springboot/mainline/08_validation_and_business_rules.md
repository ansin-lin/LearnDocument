# 第8章 输入校验与业务规则

> 本章目标：使用Jakarta Validation拒绝空白、超长和格式错误的请求字段，区分JSON读取、字段校验和Service业务规则三个阶段，并把失败转换为第7章建立的稳定HTTP响应。

第7章已经能区分200、404、409和500，但普通邮箱的预览请求即使姓名为空、部门只有空格，也会继续进入Service。本章要让不合格输入在正确的位置停止。

请求处理顺序如下：

```text
读取JSON
  → 创建EmployeeCreateRequest
  → 检查DTO字段约束
  → 进入Controller
  → Service检查业务规则
  → 返回成功响应或业务失败响应
```

## 一、开始状态与校验规格

继续使用第7章工程。本章新增或替换：

```text
pom.xml                                             ← 添加Validation依赖
src/main/java/com/example/employee/
├── common/
│   └── ApiResponse.java                           ← 完整替换
├── controller/
│   └── EmployeeController.java                    ← 完整替换
├── dto/request/
│   └── EmployeeCreateRequest.java                 ← 完整替换
├── exception/
│   ├── GlobalExceptionHandler.java                ← 完整替换
│   └── InvalidDepartmentException.java            ← 新建
└── service/
    └── EmployeeService.java                       ← 完整替换
```

其他数据对象、三个第7章异常类和 `HealthController` 保持不变。

### 1. 字段规格

| 规格编号 | 字段 | 规则 | 失败结果 |
| --- | --- | --- | --- |
| `EMP-VAL-01` | `name` | 必填、不能全为空白、最多50字符 | 400，`data.name` 保存字段消息 |
| `EMP-VAL-02` | `department` | 必填、不能全为空白、最多50字符 | 400，`data.department` 保存字段消息 |
| `EMP-VAL-03` | `email` | 必填、邮箱格式、最多100字符 | 400，`data.email` 保存字段消息 |
| `EMP-VAL-04` | 请求体 | 必须是可以读取的JSON | 400，消息为“请求JSON格式错误” |

### 2. 业务规则

当前预览接口允许的部门只有：

```text
Sales
Development
Support
```

`Other` 是长度合格的普通字符串，能够通过DTO字段校验，但不在允许部门中，因此由Service拒绝并返回400。

固定邮箱 `used@example.com` 继续表示与当前系统状态冲突，由第7章的 `DuplicateEmailException` 返回409。它不是邮箱格式错误。

## 二、完整示例

先完成本节所有修改并执行构建，再从第三节开始依照代码顺序理解第一次出现的依赖、注解、异常和方法。只给DTO添加注解而不在Controller使用 `@Valid`，不会形成完整校验流程。

### 1. 在pom.xml添加Validation依赖

在已有 `<dependencies>` 内加入：

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
```

这是对第3章现有 `pom.xml` 的追加片段，不要删除 `spring-boot-starter-web` 和 `spring-boot-starter-test`。本课程使用Spring Boot 3.5.16，由Spring Boot父项目管理兼容版本，因此这里不单独填写 `<version>`。

### 2. 完整替换EmployeeCreateRequest.java

```java
package com.example.employee.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class EmployeeCreateRequest {

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

    public EmployeeCreateRequest() {
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

### 3. 完整替换ApiResponse.java

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

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, "OK", data);
    }

    public static <T> ApiResponse<T> failure(String message) {
        return failure(message, null);
    }

    public static <T> ApiResponse<T> failure(String message, T data) {
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

### 4. 新建InvalidDepartmentException.java

文件位置：

```text
src/main/java/com/example/employee/exception/InvalidDepartmentException.java
```

完整内容：

```java
package com.example.employee.exception;

public class InvalidDepartmentException extends RuntimeException {

    public InvalidDepartmentException(String department) {
        super("不允许的部门：" + department);
    }
}
```

### 5. 完整替换EmployeeService.java

```java
package com.example.employee.service;

import com.example.employee.dto.request.EmployeeCreateRequest;
import com.example.employee.dto.response.EmployeeListItemResponse;
import com.example.employee.dto.response.EmployeeResponse;
import com.example.employee.exception.DuplicateEmailException;
import com.example.employee.exception.EmployeeNotFoundException;
import com.example.employee.exception.InvalidDepartmentException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EmployeeService {

    public EmployeeResponse findById(Long id) {
        if (!id.equals(1001L)) {
            throw new EmployeeNotFoundException(id);
        }

        return new EmployeeResponse(
                id,
                "Tanaka",
                "Sales",
                "tanaka@example.com");
    }

    public List<EmployeeListItemResponse> findList(String department) {
        if ("Unknown".equals(department)) {
            return List.of();
        }

        return List.of(new EmployeeListItemResponse(
                1001L,
                "Tanaka",
                department));
    }

    public String previewCreate(EmployeeCreateRequest request) {
        if (!isAllowedDepartment(request.getDepartment())) {
            throw new InvalidDepartmentException(
                    request.getDepartment());
        }

        if ("used@example.com".equals(request.getEmail())) {
            throw new DuplicateEmailException(request.getEmail());
        }

        return request.getName()
                + " / " + request.getDepartment()
                + " / " + request.getEmail();
    }

    private boolean isAllowedDepartment(String department) {
        return "Sales".equals(department)
                || "Development".equals(department)
                || "Support".equals(department);
    }
}
```

### 6. 完整替换EmployeeController.java

```java
package com.example.employee.controller;

import com.example.employee.common.ApiResponse;
import com.example.employee.dto.request.EmployeeCreateRequest;
import com.example.employee.dto.response.EmployeeListItemResponse;
import com.example.employee.dto.response.EmployeeResponse;
import com.example.employee.service.EmployeeService;
import jakarta.validation.Valid;
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
            @PathVariable(name = "id") Long id) {
        EmployeeResponse employee = employeeService.findById(id);
        return ResponseEntity.ok(ApiResponse.success(employee));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<EmployeeListItemResponse>>> findList(
            @RequestParam(
                    name = "department",
                    defaultValue = "Sales") String department) {
        List<EmployeeListItemResponse> employees =
                employeeService.findList(department);
        return ResponseEntity.ok(ApiResponse.success(employees));
    }

    @PostMapping("/preview")
    public ResponseEntity<ApiResponse<String>> previewCreate(
            @Valid @RequestBody EmployeeCreateRequest request) {
        String preview = employeeService.previewCreate(request);
        return ResponseEntity.ok(ApiResponse.success(preview));
    }
}
```

### 7. 完整替换GlobalExceptionHandler.java

```java
package com.example.employee.exception;

import com.example.employee.common.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidation(
            MethodArgumentNotValidException exception) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();

        for (FieldError fieldError
                : exception.getBindingResult().getFieldErrors()) {
            fieldErrors.putIfAbsent(
                    fieldError.getField(),
                    fieldError.getDefaultMessage());
        }

        return ResponseEntity
                .badRequest()
                .body(ApiResponse.failure(
                        "参数校验失败",
                        fieldErrors));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnreadableJson(
            HttpMessageNotReadableException exception) {
        return ResponseEntity
                .badRequest()
                .body(ApiResponse.failure("请求JSON格式错误"));
    }

    @ExceptionHandler(InvalidDepartmentException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidDepartment(
            InvalidDepartmentException exception) {
        return ResponseEntity
                .badRequest()
                .body(ApiResponse.failure(exception.getMessage()));
    }

    @ExceptionHandler(EmployeeNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleEmployeeNotFound(
            EmployeeNotFoundException exception) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.failure(exception.getMessage()));
    }

    @ExceptionHandler(DuplicateEmailException.class)
    public ResponseEntity<ApiResponse<Void>> handleDuplicateEmail(
            DuplicateEmailException exception) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ApiResponse.failure(exception.getMessage()));
    }

    @ExceptionHandler(EmployeeSystemException.class)
    public ResponseEntity<ApiResponse<Void>> handleEmployeeSystem(
            EmployeeSystemException exception) {
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.failure("服务器内部错误"));
    }
}
```

## 三、Validation依赖提供了什么

`spring-boot-starter-validation` 是Spring Boot提供的起步依赖，主要把Jakarta Validation API及兼容实现加入工程。代码中的约束类型位于 `jakarta.validation` 包，而不是旧项目中可能看到的 `javax.validation` 包。

Spring Boot 3使用Jakarta命名空间：

```java
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
```

如果复制Spring Boot 2时代的 `javax.validation...` 导入，当前工程会找不到类型。不要自行创建同名注解解决依赖错误。

依赖、约束和触发位置三者缺一不可：

```text
Validation依赖       → 工程具有校验API和实现
DTO字段上的约束注解  → 声明每个字段的规则
Controller中的@Valid → 在请求绑定后触发校验
```

只把Starter写进 `pom.xml` 不会自动知道员工字段规则；只写约束但不触发，也不会在当前请求体入口执行校验。

## 四、字段约束注解怎样选择

三个约束都来自 `jakarta.validation.constraints`，写在字段上，由Validation实现读取。它们不是从上到下依次调用的普通Java方法。

### 1. @NotBlank

```java
@NotBlank(message = "员工姓名不能为空")
```

`@NotBlank` 用于字符序列，要求值不是 `null`，并且去除空白后至少还有一个字符。因此下面四种情况都会失败：字段缺失、显式 `null`、空字符串和纯空白。

| 参数 | 当前值 | 可接受的值 | 默认值与作用 |
| --- | --- | --- | --- |
| `message` | `"员工姓名不能为空"` | 消息字符串或消息键 | 省略时使用库的默认消息；本例固定中文接口消息 |

### 2. @Size

```java
@Size(max = 50, message = "员工姓名不能超过50个字符")
```

`@Size` 可以检查字符串、集合、Map或数组的元素数量。本章用于限制字符串字符数量。

| 参数 | 当前值 | 可接受的值 | 默认值与作用 |
| --- | --- | --- | --- |
| `min` | 未写 | 0及以上整数 | 默认0，本章的必填由 `@NotBlank` 负责 |
| `max` | 50或100 | 不小于 `min` 的整数 | 默认整数最大值；本章按字段规格限制上限 |
| `message` | 中文字段消息 | 消息字符串或消息键 | 省略时使用库的默认消息 |

`@Size` 通常把 `null` 交给其他约束处理，所以不能单独表达必填。本章把它和 `@NotBlank` 组合。

### 3. @Email

```java
@Email(message = "邮箱格式不正确")
```

`@Email` 检查非空内容是否符合实现支持的邮箱基本格式。它不负责表达“必须提供邮箱”，因此同时使用 `@NotBlank`。

| 参数 | 当前值 | 可接受的值 | 默认值与作用 |
| --- | --- | --- | --- |
| `message` | `"邮箱格式不正确"` | 消息字符串或消息键 | 省略时使用库的默认消息 |
| `regexp` | 未写 | Java正则表达式字符串 | 默认不增加额外正则限制 |
| `flags` | 未写 | `Pattern.Flag`枚举数组 | 默认空数组，不额外改变正则匹配方式 |

邮箱格式校验不证明邮箱真实存在，也不检查是否已被其他员工使用。

### 4. @NotNull、@NotEmpty和@NotBlank

三个注解不能只看名称选择：

| 注解 | `null` | `""` | `"   "` | 适合场景 |
| --- | --- | --- | --- | --- |
| `@NotNull` | 失败 | 通过 | 通过 | 对象、日期、数字等只要求必须有值 |
| `@NotEmpty` | 失败 | 失败 | 通过 | 集合或字符串不能没有元素 |
| `@NotBlank` | 失败 | 失败 | 失败 | 姓名、部门等不能只有空白的文本 |

当前三个请求字段都是必填文本，所以选择 `@NotBlank`，不重复叠加 `@NotNull`。以后出现必须提供的数字或日期对象时，再使用 `@NotNull`。

### 5. 数值范围约束

数值字段常用：

| 注解 | 适用规则 | 示例 |
| --- | --- | --- |
| `@Min(1)` | 数值不能小于指定下限 | 页码至少为1 |
| `@Max(100)` | 数值不能大于指定上限 | 每页最多100条 |
| `@Positive` | 数值必须大于0 | 系统编号必须为正数 |
| `@PositiveOrZero` | 数值必须大于或等于0 | 数量不能为负数 |

这些注解同样来自 `jakarta.validation.constraints`。当前DTO没有数字字段，不应为了展示注解虚构年龄或级别字段。分页参数出现时，应根据正式规格选择 `@Min`、`@Max`；员工编号的业务含义也必须先由接口规格确认。

## 五、@Valid在什么时候执行

`@Valid` 的完整名称是 `jakarta.validation.Valid`，写在要校验的Controller参数上：

```java
@Valid @RequestBody EmployeeCreateRequest request
```

两个注解职责不同：

```text
@RequestBody → 读取请求体并通过Jackson创建DTO
@Valid       → 对已经创建的DTO执行字段约束
```

执行顺序：

```text
JSON语法正确
  → Jackson创建并填充EmployeeCreateRequest
  → Spring执行@Valid
      ├─ 全部通过：调用Controller方法，再进入Service
      └─ 任一失败：抛出MethodArgumentNotValidException
```

`@Valid` 没有需要填写的参数。校验失败发生在Controller方法执行前，因此Service不会收到字段校验失败的请求。

临时删除 `@Valid` 后，DTO上的约束仍存在，但当前HTTP请求不会触发这次对象校验。约束注解不是看到对象就自动执行。

## 六、字段错误怎样成为400响应

`MethodArgumentNotValidException` 的完整名称是 `org.springframework.web.bind.MethodArgumentNotValidException`。Spring在 `@Valid` 失败时抛出它，其中保存了绑定和校验结果。

处理器依次执行：

```text
exception.getBindingResult()
  → getFieldErrors()
  → 遍历每个FieldError
  → 取得字段名和消息
  → 写入fieldErrors Map
  → 返回HTTP 400和ApiResponse
```

`Map<String, String>` 是JDK的键值接口：键保存字段名，值保存该字段的第一条错误消息。`LinkedHashMap` 是保持写入顺序的Map实现，但JSON字段顺序仍不属于接口契约。

| 对象或方法 | 当前参数 | 可接受的值 | 返回结果 |
| --- | --- | --- | --- |
| `exception.getBindingResult()` | 无 | 无参数 | 返回本次绑定和校验结果 |
| `.getFieldErrors()` | 无 | 无参数 | 返回 `List<FieldError>` |
| `fieldError.getField()` | 无 | 无参数 | 返回字段名，例如 `email` |
| `fieldError.getDefaultMessage()` | 无 | 无参数 | 返回约束注解的消息 |
| `putIfAbsent(key, value)` | 字段名、消息 | 符合Map泛型的键和值 | 键不存在时写入；返回旧值，本章不接收 |
| `ResponseEntity.badRequest()` | 无 | 无参数 | 返回HTTP 400响应构建器 |

同一字段可能同时违反多条约束，例如空邮箱既可能涉及必填，也可能涉及格式。`putIfAbsent()` 让每个字段只保留第一条消息，使本章响应容易阅读。正式项目若要求返回全部消息，可以把值类型改成 `List<String>`，但接口规格也要同步改变。

第8章为 `ApiResponse` 增加了第二个失败创建方法：

```java
failure(String message, T data)
```

它允许字段错误Map放进 `data`。原来的 `failure(String message)` 仍然存在，并通过：

```java
return failure(message, null);
```

调用同名的两个参数版本。这叫方法重载：方法名相同，但参数数量不同。第7章原有404、409和500代码无需修改调用方式。

## 七、JSON解析失败为什么没有字段错误

损坏的JSON在DTO创建完成前就会失败。例如：

```json
{"name":"Sato"
```

Spring会抛出 `HttpMessageNotReadableException`，完整名称位于 `org.springframework.http.converter`。这时没有完整的 `EmployeeCreateRequest`，也没有可以执行的字段约束，所以不能返回 `data.name` 等字段结果。

本章统一返回：

```json
{
  "success": false,
  "message": "请求JSON格式错误",
  "data": null
}
```

处理方法接收异常对象，但不把 `exception.getMessage()` 返回给客户端，因为底层消息可能包含Java类型、解析器和内部结构。详细诊断信息留给服务端日志。

## 八、字段校验和业务规则怎样分工

字段校验回答“这个值本身的形状是否合格”：

```text
是否缺失
是否为空白
长度是否超限
邮箱格式是否基本正确
```

业务规则回答“在当前业务状态下是否允许”：

```text
部门是否属于允许范围
邮箱是否已经被使用
当前员工状态是否允许修改
```

因此 `Other` 能通过 `@NotBlank` 和 `@Size`，随后在Service失败：

```java
if (!isAllowedDepartment(request.getDepartment())) {
    throw new InvalidDepartmentException(
            request.getDepartment());
}
```

`isAllowedDepartment()` 是本项目的私有业务判断方法，不是Spring或Validation提供的方法。它使用 `||` 表示三个允许条件中任意一个成立就返回 `true`。

当前规则全部不成立时抛出 `InvalidDepartmentException`，全局异常处理器把它转换为400。重复邮箱则是与当前系统状态冲突，继续返回409。

不要把部门名单硬编码进自定义字段注解，也不要在Controller中复制Service业务判断。以后部门来自数据库时，只需要替换Service或数据访问逻辑，HTTP入口和DTO格式规则可以保持稳定。

## 九、四层约束边界

| 位置 | 主要目的 | 示例 | 能否被其他层替代 |
| --- | --- | --- | --- |
| 前端校验 | 尽早提示用户、改善操作体验 | 输入时提示邮箱格式 | 不能替代后端校验 |
| DTO Validation | 保护HTTP输入边界 | 必填、长度、格式 | 不能判断数据库当前状态 |
| Service业务规则 | 保证业务行为一致 | 允许部门、重复邮箱 | 不能只依靠Controller |
| 数据库约束 | 保护最终持久数据 | 非空、唯一键、外键 | 不能替代友好的接口校验 |

调用者可以绕过网页直接请求API，因此后端必须校验。Service还可能被批处理或其他入口调用，因此核心业务规则不能只写在前端或Controller。

数据库约束是最后防线，但数据库错误通常不足以直接作为对外业务消息。接入数据库后，需要把持久化冲突转换为稳定HTTP响应。

## 十、构建、运行与分阶段验证

在项目根目录执行：

```powershell
.\mvnw.cmd clean test
.\mvnw.cmd spring-boot:run
```

保持应用运行，在另一个PowerShell窗口执行下面请求。

### 1. 有效请求

```powershell
$validBody = @{
    name = "Sato"
    department = "Development"
    email = "sato@example.com"
} | ConvertTo-Json

$valid = Invoke-WebRequest `
    -Uri "http://localhost:8080/employees/preview" `
    -Method Post `
    -ContentType "application/json" `
    -Body $validBody

$valid.StatusCode
$valid.Content
```

预期状态为200，`success` 为 `true`，`data` 是预览文本。

### 2. 字段校验失败

```powershell
$invalidBody = @{
    name = "   "
    department = ""
    email = "not-an-email"
} | ConvertTo-Json

$invalid = Invoke-WebRequest `
    -Uri "http://localhost:8080/employees/preview" `
    -Method Post `
    -ContentType "application/json" `
    -Body $invalidBody `
    -SkipHttpErrorCheck

$invalid.StatusCode
$invalid.Content
```

预期状态为400，正文包含：

```json
{
  "success": false,
  "message": "参数校验失败",
  "data": {
    "name": "员工姓名不能为空",
    "department": "部门不能为空",
    "email": "邮箱格式不正确"
  }
}
```

字段顺序不属于接口契约。一个字段存在多条错误时，本章只保证返回其中第一条，不依赖具体约束执行顺序。

### 3. JSON读取失败

使用 `curl.exe` 原样发送损坏JSON：

```powershell
$brokenJson = '{"name":"Sato"'

curl.exe -i `
    -X POST "http://localhost:8080/employees/preview" `
    -H "Content-Type: application/json" `
    --data-binary $brokenJson
```

预期返回400，消息是 `请求JSON格式错误`，`data` 为 `null`，而不是字段错误Map。

### 4. 业务规则失败

```powershell
$businessBody = @{
    name = "Sato"
    department = "Other"
    email = "sato@example.com"
} | ConvertTo-Json

$business = Invoke-WebRequest `
    -Uri "http://localhost:8080/employees/preview" `
    -Method Post `
    -ContentType "application/json" `
    -Body $businessBody `
    -SkipHttpErrorCheck

$business.StatusCode
$business.Content
```

预期状态为400，消息是 `不允许的部门：Other`，`data` 为 `null`。这证明DTO字段校验通过后，Service仍会执行独立的业务判断。

### 5. 状态冲突和原接口回归

继续验证：

| 请求或场景 | 预期状态 | 关键结果 |
| --- | ---: | --- |
| 邮箱为 `used@example.com`，部门合法 | 409 | 邮箱冲突消息 |
| `GET /employees/1001` | 200 | 员工详情成功结构 |
| `GET /employees/9999` | 404 | 员工不存在结构 |
| `GET /employees/abc` | 400 | 路径类型转换失败 |
| POST预览使用 `text/plain` | 415 | 媒体类型错误 |
| `GET /health` | 200 | `OK` |

验证完成后在启动窗口按 `Ctrl+C` 停止应用。

## 十一、常见问题与Review

| 现象或写法 | 原因 | 修正或Review意见 |
| --- | --- | --- |
| `jakarta.validation` 无法导入 | 依赖未加入或Maven未刷新 | 检查Starter位置并重新加载依赖 |
| DTO有约束但空字段仍进入Service | Controller参数缺少 `@Valid` | 使用 `@Valid @RequestBody` |
| 邮箱为空只出现格式提示 | 只使用 `@Email` | 同时使用 `@NotBlank` 表达必填 |
| `Other` 部门通过所有DTO约束 | DTO只检查格式，不知道允许名单 | 在Service执行部门业务规则 |
| Controller直接判断允许部门 | 业务规则放错位置，其他入口难以复用 | 移到Service私有方法 |
| JSON损坏却尝试读取字段错误 | DTO尚未创建 | 单独处理 `HttpMessageNotReadableException` |
| 所有错误都返回同一句“参数错误” | 调用方无法定位字段或阶段 | 区分字段Map、JSON错误和业务消息 |
| 把异常内部消息直接返回客户端 | 可能泄露Java类型或内部结构 | 返回稳定消息，详细信息留给日志 |
| 只做前端校验 | API可以绕过网页直接调用 | 后端继续执行DTO和业务校验 |
| 修改长度只改DTO | 接口规格、数据库列和测试可能不一致 | 修改前完成影响调查 |

Review校验代码时按顺序检查：

```text
接口字段规格
  → DTO约束是否一致
  → Controller是否触发
  → 失败是否在Service前停止
  → Service业务规则是否独立存在
  → HTTP状态和错误结构是否符合规格
  → 数据库约束是否需要同步调查
```

## 十二、操作练习

### 练习1：缺失、null、空字符串和空白字符串

分别发送下面四种姓名输入，记录状态和 `data.name`：

1. JSON中省略 `name`；
2. `"name": null`；
3. `"name": ""`；
4. `"name": "   "`。

四种情况都应被 `@NotBlank` 拒绝。记录请求正文，不能只记录错误消息。

### 练习2：受控修改部门长度

改修规格：部门最大长度从50改成30。

修改前调查接口字段规格、DTO约束、测试请求和未来数据库列。修改 `@Size(max = 30)` 后，分别验证30和31字符边界；保存证据后恢复主线值50。

### 练习3：增加可选电话号码

临时给请求DTO增加可选字段 `phone`。省略或 `null` 时允许通过，填写后只能包含数字和连字符：

```java
@Pattern(
        regexp = "[0-9-]+",
        message = "电话号码格式不正确")
private String phone;
```

`@Pattern` 来自 `jakarta.validation.constraints`。`regexp` 必须是Java正则表达式字符串，`message` 省略时使用默认消息。该约束允许 `null`，因此可以表达“可选，但填写后必须合规”。

补充getter和setter，验证省略、合法值和非法值。完成后删除该字段、导入和方法，避免没有接口规格的练习字段进入后续章节。

### 练习4：证明@Valid不可缺少

临时删除Controller参数前的 `@Valid`，重新启动后发送空白姓名。记录请求是否进入Service以及状态变化；随后恢复 `@Valid`，重新构建并确认同一请求返回400。

### 练习5：整理测试证据和Review指摘

至少覆盖：有效值、每个长度边界、缺失、null、空字符串、空白、邮箱错误、损坏JSON、不允许部门、重复邮箱和原接口回归。

测试记录包含规格编号、请求、预期状态、实际状态、响应关键字段、Service是否执行和判定。Review一份“只把前端最大长度改成30”的修改，列出接口规格、DTO、数据库设计和测试的影响。

## 十三、本章稳定状态

完成练习并恢复临时字段和长度后，工程应保持：

```text
src/main/java/com/example/employee/
├── common/ApiResponse.java
├── controller/
│   ├── EmployeeController.java
│   └── HealthController.java
├── dto/
│   ├── request/EmployeeCreateRequest.java
│   └── response/
│       ├── EmployeeListItemResponse.java
│       └── EmployeeResponse.java
├── exception/
│   ├── DuplicateEmailException.java
│   ├── EmployeeNotFoundException.java
│   ├── EmployeeSystemException.java
│   ├── GlobalExceptionHandler.java
│   └── InvalidDepartmentException.java
└── service/EmployeeService.java
```

此时你应能够：

1. 根据字段类型和业务含义选择常用约束；
2. 说明Validation依赖、约束注解和 `@Valid` 各自的作用；
3. 区分缺失、`null`、空字符串和空白字符串；
4. 区分JSON解析失败、DTO字段校验失败和Service业务规则失败；
5. 把字段错误转换为可定位字段的统一400响应；
6. 区分前端校验、DTO校验、Service规则和数据库约束；
7. 为正常值、边界值和非法值保存可追溯的测试证据。

下一章会为当前接口建立真实表定义、Entity、Mapper和MyBatis执行链。DTO校验继续保护HTTP输入，数据库约束继续保护最终持久数据，两者不能互相替代。
