# 第7章 成功与失败怎样返回

> 本章目标：根据接口规格区分成功、无记录、业务冲突和系统故障，使用 `ResponseEntity` 控制HTTP状态与响应体，并通过统一响应对象和全局异常处理器形成可检查的响应契约。

第6章已经能把HTTP输入传给Java方法，也能把Java对象转换为JSON。但“能够返回JSON”还不等于“响应设计正确”。例如员工不存在时，正文写了“失败”却仍返回200，调用方就难以可靠判断请求结果。

本章围绕下面的关系完成改修：

```text
Service得到业务结果或抛出异常
  → Controller或全局异常处理器选择HTTP状态
  → ApiResponse提供稳定的JSON字段
  → 客户端同时检查状态码和响应体
```

## 一、开始状态与响应规格

继续使用第6章工程。开始前已经存在：

```text
src/main/java/com/example/employee/
├── controller/
│   ├── EmployeeController.java
│   └── HealthController.java
├── dto/
│   ├── request/EmployeeCreateRequest.java
│   └── response/
│       ├── EmployeeListItemResponse.java
│       └── EmployeeResponse.java
└── service/
    └── EmployeeService.java
```

本章新建或替换：

```text
src/main/java/com/example/employee/
├── common/
│   └── ApiResponse.java                    ← 新建
├── controller/
│   └── EmployeeController.java             ← 完整替换
├── exception/
│   ├── DuplicateEmailException.java        ← 新建
│   ├── EmployeeNotFoundException.java      ← 新建
│   ├── EmployeeSystemException.java        ← 新建
│   └── GlobalExceptionHandler.java         ← 新建
└── service/
    └── EmployeeService.java                ← 完整替换
```

`HealthController` 和第5章的三个数据对象保持不变。

### 1. 本章实际接口规格

| 规格编号 | 请求 | 场景 | HTTP状态 | 响应体 |
| --- | --- | --- | ---: | --- |
| `EMP-API-02` | `GET /employees/{id}` | 编号为1001 | 200 | 成功结构和员工详情 |
| `EMP-API-02` | `GET /employees/{id}` | 其他可转换为Long的编号 | 404 | 失败结构和“员工不存在”消息 |
| `EMP-API-03` | `GET /employees` | 有匹配数据 | 200 | 成功结构和员工数组 |
| `EMP-API-03` | `GET /employees?department=Unknown` | 无匹配数据 | 200 | 成功结构和空数组 |
| `EMP-PREVIEW-01` | `POST /employees/preview` | 普通邮箱 | 200 | 成功结构和预览文本 |
| `EMP-PREVIEW-01` | `POST /employees/preview` | `used@example.com` | 409 | 失败结构和邮箱冲突消息 |

当前Service仍使用固定样例数据。预览接口只检查请求数据和样例冲突，不保存员工；真实新增及201响应要在接入数据库后实现。

成功响应统一为：

```json
{
  "success": true,
  "message": "OK",
  "data": {}
}
```

失败响应统一为：

```json
{
  "success": false,
  "message": "员工不存在：9999",
  "data": null
}
```

HTTP状态和JSON字段承担不同职责，后文会分别说明。

## 二、完整示例

先完成本节全部文件并运行，再从第三节开始依照代码出现顺序理解新类型、方法和注解。不要只替换Controller而保留旧Service，否则返回类型和异常流程无法对应。

### 1. 新建ApiResponse.java

文件位置：

```text
src/main/java/com/example/employee/common/ApiResponse.java
```

完整内容：

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
        return new ApiResponse<>(false, message, null);
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

### 2. 新建三个异常类

文件位置：

```text
src/main/java/com/example/employee/exception/EmployeeNotFoundException.java
```

完整内容：

```java
package com.example.employee.exception;

public class EmployeeNotFoundException extends RuntimeException {

    public EmployeeNotFoundException(Long id) {
        super("员工不存在：" + id);
    }
}
```

文件位置：

```text
src/main/java/com/example/employee/exception/DuplicateEmailException.java
```

完整内容：

```java
package com.example.employee.exception;

public class DuplicateEmailException extends RuntimeException {

    public DuplicateEmailException(String email) {
        super("邮箱已被使用：" + email);
    }
}
```

文件位置：

```text
src/main/java/com/example/employee/exception/EmployeeSystemException.java
```

完整内容：

```java
package com.example.employee.exception;

public class EmployeeSystemException extends RuntimeException {

    public EmployeeSystemException(String message) {
        super(message);
    }
}
```

### 3. 完整替换EmployeeService.java

```java
package com.example.employee.service;

import com.example.employee.dto.request.EmployeeCreateRequest;
import com.example.employee.dto.response.EmployeeListItemResponse;
import com.example.employee.dto.response.EmployeeResponse;
import com.example.employee.exception.DuplicateEmailException;
import com.example.employee.exception.EmployeeNotFoundException;
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
        if ("used@example.com".equals(request.getEmail())) {
            throw new DuplicateEmailException(request.getEmail());
        }

        return request.getName()
                + " / " + request.getDepartment()
                + " / " + request.getEmail();
    }
}
```

### 4. 完整替换EmployeeController.java

```java
package com.example.employee.controller;

import com.example.employee.common.ApiResponse;
import com.example.employee.dto.request.EmployeeCreateRequest;
import com.example.employee.dto.response.EmployeeListItemResponse;
import com.example.employee.dto.response.EmployeeResponse;
import com.example.employee.service.EmployeeService;
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
            @RequestBody EmployeeCreateRequest request) {
        String preview = employeeService.previewCreate(request);
        return ResponseEntity.ok(ApiResponse.success(preview));
    }
}
```

### 5. 新建GlobalExceptionHandler.java

文件位置：

```text
src/main/java/com/example/employee/exception/GlobalExceptionHandler.java
```

完整内容：

```java
package com.example.employee.exception;

import com.example.employee.common.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

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

## 三、状态码和响应体不是同一件事

HTTP响应至少包含状态码、响应头和响应体：

```text
HTTP状态：404 Not Found
Content-Type：application/json
响应体：{"success":false,"message":"员工不存在：9999","data":null}
```

状态码是HTTP协议层的结果，客户端、网关和监控工具无需理解业务JSON也能先分类。响应体提供业务数据或可显示的稳定消息。

下面两种写法并不等价：

```text
HTTP 200 + {"success":false,...}  ← 协议层仍然声称成功
HTTP 404 + {"success":false,...}  ← 协议状态和业务结果一致
```

本项目根据具体场景使用：

| 状态 | 当前含义 | 本章是否实际产生 |
| ---: | --- | --- |
| 200 OK | 查询、列表或预览成功 | 是 |
| 201 Created | 新资源已经创建 | 否，真实新增时使用 |
| 204 No Content | 操作成功且没有响应体 | 否，真实删除时使用 |
| 400 Bad Request | 请求格式、类型或字段不符合要求 | 第6章已观察；字段校验在第8章统一 |
| 404 Not Found | 指定员工不存在 | 是 |
| 409 Conflict | 请求与当前系统状态冲突，例如邮箱已被占用 | 是 |
| 500 Internal Server Error | 服务端执行过程中发生系统故障 | 通过受控练习验证 |

状态码不能只按方法名称决定。例如POST不一定是201：本章POST只是预览，没有创建资源，所以仍返回200。

## 四、ApiResponse<T>解决什么问题

`ApiResponse` 是当前项目自己创建的业务响应类，不是Spring提供的类。它固定三个JSON字段：

| 字段 | 类型 | 成功时 | 失败时 |
| --- | --- | --- | --- |
| `success` | `boolean` | `true` | `false` |
| `message` | `String` | `"OK"` | 稳定、可公开的失败摘要 |
| `data` | `T` | 业务数据 | `null` |

类名后的 `<T>` 是泛型类型参数。创建对象时，`T` 会由当前响应的数据类型替代：

```text
ApiResponse<EmployeeResponse>              → data是员工详情
ApiResponse<List<EmployeeListItemResponse>> → data是员工列表
ApiResponse<String>                        → data是预览文本
ApiResponse<Void>                          → data没有业务值
```

`Void` 是 `java.lang.Void`，这里用来表达失败响应没有业务数据。实际JSON中的 `data` 为 `null`。

### 1. 私有构造方法

```java
private ApiResponse(boolean success, String message, T data)
```

`private` 使业务代码不能随意组合三个字段，而要使用下面两个创建方法。这样成功和失败的布尔值不会被写反。

### 2. 静态泛型创建方法

```java
public static <T> ApiResponse<T> success(T data)
```

方法名前的 `<T>` 声明这是静态方法自己的类型参数；参数 `data` 提供实际类型，返回值是相同数据类型的 `ApiResponse<T>`。

```java
ApiResponse.success(employee)
```

当 `employee` 是 `EmployeeResponse` 时，编译器推断结果为 `ApiResponse<EmployeeResponse>`。

失败方法只接收可公开消息：

```java
public static <T> ApiResponse<T> failure(String message)
```

它固定写入 `false` 和 `null`。`new ApiResponse<>(...)` 中的菱形符号让编译器根据返回类型推断泛型，不需要重复写类型名。

统一响应结构不是HTTP强制标准。有些项目直接返回业务对象，有些项目使用不同字段名。进入既有项目时应先看接口规格，不能因为本课程使用 `ApiResponse` 就认为所有项目都必须采用同一种包装。

## 五、ResponseEntity与业务响应对象的区别

`ResponseEntity` 的完整名称是 `org.springframework.http.ResponseEntity`，由Spring Web提供，不需要创建 `ResponseEntity.java`。`spring-boot-starter-web` 已在第3章加入，因此当前可以直接导入。

两类对象职责不同：

```text
ResponseEntity<ApiResponse<EmployeeResponse>>
│              └─ 业务响应体的JSON结构
└─ HTTP响应：状态、响应头、响应体
```

成功查询使用：

```java
return ResponseEntity.ok(ApiResponse.success(employee));
```

| 方法 | 当前参数 | 可接受的值 | 返回结果 |
| --- | --- | --- | --- |
| `ResponseEntity.ok(body)` | `ApiResponse<EmployeeResponse>` | 任意可作为响应体的对象 | HTTP 200和包含该对象的 `ResponseEntity` |
| `ApiResponse.success(data)` | `EmployeeResponse` | 与目标泛型一致的业务数据 | `success=true` 的业务响应对象 |

失败响应需要选择其他状态：

```java
ResponseEntity
        .status(HttpStatus.NOT_FOUND)
        .body(ApiResponse.failure(exception.getMessage()));
```

`HttpStatus` 的完整名称是 `org.springframework.http.HttpStatus`，是Spring提供的枚举。`NOT_FOUND`、`CONFLICT` 和 `INTERNAL_SERVER_ERROR` 分别代表404、409和500。

| 方法 | 当前参数 | 可接受的值 | 返回结果 |
| --- | --- | --- | --- |
| `ResponseEntity.status(status)` | `HttpStatus.NOT_FOUND` | `HttpStatusCode`或整数状态码 | 保存指定状态的响应构建器 |
| `.body(body)` | `ApiResponse<Void>` | 与最终响应体类型一致的对象 | 带状态和响应体的 `ResponseEntity` |

真实新增成功时会使用：

```java
return ResponseEntity
        .status(HttpStatus.CREATED)
        .body(ApiResponse.success(createdEmployee));
```

`HttpStatus.CREATED` 表示201。只有资源已经创建成功时才能返回它；本章固定数据预览不能使用201。

真实删除成功且规格不要求正文时会使用：

```java
return ResponseEntity.noContent().build();
```

`noContent()` 创建204响应构建器，`build()` 完成没有响应体的 `ResponseEntity<Void>`。204不能再附加 `ApiResponse` JSON；“统一结构”不能覆盖状态码本身的语义。

## 六、空列表、无记录和无响应体

这三个说法容易混淆：

| 情况 | 示例 | 状态 | data或响应体 |
| --- | --- | ---: | --- |
| 列表查询没有匹配项 | 部门为 `Unknown` | 200 | `data` 是 `[]` |
| 按唯一编号查询不到 | 员工9999不存在 | 404 | `data` 是 `null` |
| 操作成功且规格规定不返回正文 | 未来删除成功 | 204 | 整个响应体不存在 |

空列表仍是有效的查询结果，客户端可以正常遍历零个元素。按编号查询表达“请给我这个特定资源”，资源不存在时使用404。204则表示操作成功但没有正文，不等于JSON中的 `data: null`。

Service中的：

```java
return List.of();
```

调用JDK的 `List.of()` 无参数形式，返回不可修改的空列表。Jackson把它序列化为 `[]`。

## 七、异常怎样中断并离开Service

详情查询先判断固定样例中是否存在员工：

```java
if (!id.equals(1001L)) {
    throw new EmployeeNotFoundException(id);
}
```

`equals(1001L)` 比较 `Long` 保存的数值。本章路径参数必填且已经成功转换，因此进入Service时 `id` 不为 `null`。

`EmployeeNotFoundException extends RuntimeException` 表示自定义异常继承JDK的非受检异常。构造方法中的：

```java
super("员工不存在：" + id);
```

调用父类构造方法保存异常消息。`throw` 抛出异常后，当前Service方法立即停止，不再创建或返回 `EmployeeResponse`，Controller中的正常返回语句也不会继续执行。

邮箱冲突采用同样流程：

```java
if ("used@example.com".equals(request.getEmail())) {
    throw new DuplicateEmailException(request.getEmail());
}
```

这条固定规则只模拟“当前系统已存在该邮箱”的状态，不代表已经查询数据库。使用409是因为请求格式可以读取，但请求与当前资源状态冲突。

不要用 `return null` 同时表示不存在、冲突和系统故障。三种结果需要不同的处理方式和HTTP状态，混成一个 `null` 后，Controller无法可靠判断原因。

## 八、为什么需要全局异常处理器

### 1. 先看没有集中处理时的问题

Service发现员工不存在后会抛出异常：

```java
throw new EmployeeNotFoundException(id);
```

异常一旦抛出，Controller中正常返回200的代码不会继续执行。如果项目没有为这种业务异常规定转换方式，Spring只能按照现有默认机制处理；客户端得到的状态和JSON结构就可能与员工接口规格不一致。

一种直接做法是在每个Controller方法中写 `try-catch`：

```java
try {
    EmployeeResponse employee = employeeService.findById(id);
    return ResponseEntity.ok(ApiResponse.success(employee));
} catch (EmployeeNotFoundException exception) {
    return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(ApiResponse.failure(exception.getMessage()));
}
```

这段代码能够处理当前方法，但员工查询、修改、删除都可能遇到“不存在”。如果每个方法都复制一次，会出现三个问题：

1. Controller反复出现相同的状态码和响应组装代码；
2. 修改错误结构时容易漏改某个接口；
3. 正常业务流程被大量重复的异常转换代码打断。

### 2. 为什么不让Service直接返回ResponseEntity

也不能为了减少Controller代码，就让Service返回 `ResponseEntity`。`ResponseEntity` 表达HTTP状态、响应头和响应体，属于Web边界；Service应该表达员工查询结果、业务冲突或系统失败。

如果Service直接返回HTTP对象，批处理、定时任务或其他非HTTP入口复用业务逻辑时，也会被迫理解404、409等Web概念。正确分工是：

```text
Service                 → 返回业务数据，或抛出含义明确的异常
全局异常处理器          → 把异常转换成HTTP状态和ApiResponse
Controller正常处理方法  → 只保留成功调用与成功响应
```

### 3. 集中处理带来的结果

全局异常处理器相当于多个Controller共用的“异常到HTTP响应”转换位置：

```text
EmployeeNotFoundException → 404
DuplicateEmailException   → 409
EmployeeSystemException   → 500和安全的通用消息
```

这样做不是为了消灭异常，而是让相同异常在不同接口中得到一致响应，并把业务判断与HTTP表达分开。新增异常类型时，仍要根据接口规格明确添加转换，不能假设Global类会自动理解所有异常。

### 4. Spring怎样找到对应处理方法

`@RestControllerAdvice` 和 `@ExceptionHandler` 都来自 `org.springframework.web.bind.annotation`。

`@RestControllerAdvice` 写在类上。Spring组件扫描会发现 `GlobalExceptionHandler` 并创建Bean；其中返回的对象会写入HTTP响应体，因此多个Controller不必重复编写相同失败转换。

`@ExceptionHandler(EmployeeNotFoundException.class)` 写在方法上，声明该方法处理哪一种异常。括号中的 `EmployeeNotFoundException.class` 是这个异常类型对应的 `Class` 对象，不是创建异常。

执行过程如下：

```text
Service抛出EmployeeNotFoundException
  → Controller正常流程停止
  → Spring找到匹配的@ExceptionHandler方法
  → 异常对象传入exception参数
  → exception.getMessage()取得稳定业务消息
  → 处理器返回HTTP 404和ApiResponse失败正文
```

| 注解或方法 | 当前参数 | 可接受的值 | 默认值或结果 |
| --- | --- | --- | --- |
| `@RestControllerAdvice` | 无 | 可配置包、注解或类型范围 | 无筛选时作用于扫描到的Controller |
| `@ExceptionHandler(...)` | `EmployeeNotFoundException.class` | 一个或多个异常类型的 `Class` 对象 | 未声明时可根据方法参数推断；本章显式声明 |
| `exception.getMessage()` | 无 | 无参数 | 返回异常构造时保存的消息字符串 |

三个异常处理方法分别对应404、409、500。类名叫Global，表示它可以服务多个Controller，不表示它应该捕获所有Java异常。

本章没有添加 `@ExceptionHandler(Exception.class)`。过早捕获所有异常会把第6章的参数转换错误、请求方法错误等框架异常也拦截成500，还可能隐藏尚未设计处理方式的编程错误。第11章建立日志和故障定位方式后，再决定项目级兜底策略。

## 九、对外消息与内部细节分开

业务异常的消息由项目代码明确创建，可以返回给客户端：

```text
员工不存在：9999
邮箱已被使用：used@example.com
```

系统异常可能包含SQL、文件路径、服务器地址、驱动信息或调用栈，不应直接放入响应。系统异常处理器虽然接收异常对象，但对外只返回：

```json
{
  "success": false,
  "message": "服务器内部错误",
  "data": null
}
```

```java
@ExceptionHandler(EmployeeSystemException.class)
public ResponseEntity<ApiResponse<Void>> handleEmployeeSystem(
        EmployeeSystemException exception) {
    return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiResponse.failure("服务器内部错误"));
}
```

参数 `exception` 保存内部异常对象，本章故意不调用 `getMessage()` 写入响应。第11章会把诊断信息记录到服务端日志；客户端只得到稳定且不泄露内部结构的消息。

## 十、构建、运行与契约验证

在项目根目录执行：

```powershell
.\mvnw.cmd clean test
.\mvnw.cmd spring-boot:run
```

保持应用运行，在另一个PowerShell窗口验证。

### 1. 详情成功

```powershell
$detail = Invoke-WebRequest `
    -Uri "http://localhost:8080/employees/1001" `
    -Method Get

$detail.StatusCode
$detail.Content
```

预期状态为200，正文等价于：

```json
{
  "success": true,
  "message": "OK",
  "data": {
    "id": 1001,
    "name": "Tanaka",
    "department": "Sales",
    "email": "tanaka@example.com"
  }
}
```

### 2. 详情不存在

```powershell
$missing = Invoke-WebRequest `
    -Uri "http://localhost:8080/employees/9999" `
    -Method Get `
    -SkipHttpErrorCheck

$missing.StatusCode
$missing.Content
```

`-SkipHttpErrorCheck` 允许PowerShell在收到4xx、5xx时仍把响应保存到变量。预期状态为404，`success` 为 `false`，消息为 `员工不存在：9999`。

### 3. 空列表仍然成功

```powershell
$empty = Invoke-RestMethod `
    -Uri "http://localhost:8080/employees?department=Unknown" `
    -Method Get

$empty.success
$empty.data.Count
```

预期分别得到 `True` 和 `0`。原始JSON中的 `data` 是 `[]`，不是 `null`。

### 4. 邮箱状态冲突

先验证普通邮箱的预览成功响应：

```powershell
$normalBody = @{
    name = "Sato"
    department = "Development"
    email = "sato@example.com"
} | ConvertTo-Json

$preview = Invoke-WebRequest `
    -Uri "http://localhost:8080/employees/preview" `
    -Method Post `
    -ContentType "application/json" `
    -Body $normalBody

$preview.StatusCode
$preview.Content
```

预期状态为200，`success` 为 `true`，`data` 是三项请求数据组成的预览文本。然后把邮箱改为固定的已占用样例：

```powershell
$body = @{
    name = "Sato"
    department = "Development"
    email = "used@example.com"
} | ConvertTo-Json

$conflict = Invoke-WebRequest `
    -Uri "http://localhost:8080/employees/preview" `
    -Method Post `
    -ContentType "application/json" `
    -Body $body `
    -SkipHttpErrorCheck

$conflict.StatusCode
$conflict.Content
```

预期状态为409，正文不包含Java异常类名或调用栈。

### 5. 回归第6章请求阶段错误

继续确认：

| 请求 | 预期状态 |
| --- | ---: |
| `GET /employees/abc` | 400 |
| `DELETE /employees` | 405 |
| POST预览并使用 `Content-Type: text/plain` | 415 |
| `GET /health` | 200 |

这些结果证明新的异常处理器没有把原有请求阶段错误改成500。完成后在启动窗口按 `Ctrl+C` 停止应用。

## 十一、常见问题与Review

| 现象或写法 | 问题 | 修正或Review意见 |
| --- | --- | --- |
| 失败JSON返回了，但HTTP仍是200 | 只创建了 `ApiResponse.failure()` | 使用 `ResponseEntity` 同时设置正确状态 |
| 项目中创建了 `ResponseEntity.java` | 把Spring类误认为业务类 | 删除同名类，检查Starter和正确导入 |
| 无结果列表返回404 | 把集合查询与唯一资源查询混淆 | 返回200和空数组 |
| 204响应仍包含JSON | 204语义是不返回正文 | 使用 `.noContent().build()` |
| Service返回 `ResponseEntity` | 业务层开始依赖HTTP表达 | Service返回业务数据或抛业务异常，由Web边界转换 |
| Controller到处写相同失败正文 | 转换逻辑重复 | 交给全局异常处理器 |
| 500正文返回异常消息和调用栈 | 暴露内部实现和环境信息 | 对外返回稳定摘要，内部细节留给日志 |
| 使用 `@ExceptionHandler(Exception.class)` 后400变500 | 捕获范围过宽 | 保留框架默认处理或显式设计各异常映射 |
| 预览接口返回201 | 实际没有创建资源 | 本章预览返回200，真实新增成功后再用201 |

Review时按下面顺序核对：

```text
接口场景
  → 是否成功
  → HTTP状态是否符合规格
  → 响应体是否应该存在
  → data是对象、数组、null还是没有正文
  → 对外消息是否稳定且安全
```

## 十二、操作练习

### 练习1：增加员工编号业务规则

改修规格：`GET /employees/0` 返回400，消息为 `员工编号必须大于0`；员工编号为正数但不存在时仍返回404。

限制：Controller不能直接判断编号；在Service中完成业务判断，新建 `InvalidEmployeeIdException`，再由全局异常处理器映射为400。修改后至少验证编号0、1001和9999三个请求，分别保存状态码和正文。

记录验证证据后，删除该异常类和对应处理方法，恢复本章稳定状态。下一章会完整替换全局异常处理器，不能让练习代码成为未说明的前置条件。

### 练习2：验证系统故障不泄露内部消息

这是受控故障实验。临时在 `findById()` 最前面加入：

```java
if (id.equals(5000L)) {
    throw new EmployeeSystemException(
            "database timeout at 192.0.2.10:3306");
}
```

同时导入：

```java
import com.example.employee.exception.EmployeeSystemException;
```

请求 `GET /employees/5000`，预期HTTP状态为500，对外正文只能出现 `服务器内部错误`，不能出现模拟地址和端口。记录证据后删除临时代码和导入，重新构建并确认1001、9999仍分别返回200、404。

示例地址 `192.0.2.10` 属于文档用途，不是真实服务器；不要在教学记录中填写公司内部地址。

### 练习3：完成状态码影响调查

收到下面的改修要求：

> 预览接口遇到重复邮箱时，不再返回409，改为HTTP 200并在message中写失败。

先不要修改代码。提交一份Review意见，至少包含：

1. 与当前接口规格冲突的位置；
2. 对调用方成功判断的影响；
3. Controller、异常处理器和接口测试的影响范围；
4. 建议继续使用409的理由。

### 练习4：整理自测证据

为以下场景记录规格编号、请求、预期状态、实际状态、响应关键字段和判定：

1. 详情成功；
2. 详情不存在；
3. 有数据列表；
4. 空列表；
5. 预览成功；
6. 邮箱冲突；
7. 第6章的400、405、415回归；
8. 健康检查回归。

只写“测试通过”不能说明实际验证了哪个契约。

## 十三、本章稳定状态

完成练习并恢复受控故障代码后，工程应保持：

```text
src/main/java/com/example/employee/
├── common/
│   └── ApiResponse.java
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
│   └── GlobalExceptionHandler.java
└── service/
    └── EmployeeService.java
```

此时你应能够：

1. 分开判断HTTP状态和响应体；
2. 为200、201、204、400、404、409和500选择具体使用场景；
3. 区分 `ResponseEntity`、`HttpStatus` 和 `ApiResponse<T>` 的职责；
4. 区分空列表、资源不存在、`data: null` 和没有响应体；
5. 说明自定义异常从Service传播到全局异常处理器的过程；
6. 保证系统故障响应不泄露内部异常细节；
7. 用状态码和关键JSON字段形成可追溯的自测证据。

下一章会在现有响应体系上加入字段约束，区分JSON能否读取、字段是否合格和业务规则是否允许，并把校验失败转换为稳定的400响应。
