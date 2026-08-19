# 第6章 参数校验与统一400错误响应

> 本章目标：使用Jakarta Validation校验新增员工请求，通过全局异常处理返回HTTP 400和可定位到字段的统一JSON错误，并区分参数格式、业务规则和数据库约束。

## 一、开始状态

第5章已经建立：

```text
common/ApiResponse.java
controller/EmployeeController.java
dto/EmployeeCreateRequest.java
service/EmployeeService.java
vo/EmployeeResponse.java
```

有效POST请求返回201，但空姓名、错误邮箱仍会进入Service。本章修改：

```text
pom.xml                                      # 添加Validation依赖
dto/EmployeeCreateRequest.java              # 完整替换，增加约束
controller/EmployeeController.java          # 完整替换，在请求体前增加@Valid
exception/ValidationExceptionHandler.java   # 新建，统一转换400响应
```

## 二、为什么后端必须校验

前端校验用于改善用户体验，不能成为安全边界。调用者可以绕过网页，直接向API发送任意HTTP请求。因此后端必须在业务处理前验证接口输入。

需要区分三类规则：

| 规则类型 | 示例 | 主要处理位置 |
| --- | --- | --- |
| 参数格式 | 姓名为空、邮箱格式错误、文本过长 | DTO Validation |
| 业务规则 | 员工编号重复、部门已停用 | Service |
| 数据库约束 | 唯一键、外键、非空列冲突 | 数据库＋持久化异常转换 |

Validation负责第一类，不要把“部门是否真实存在”写成DTO格式注解。业务规则应由Service处理，数据库约束冲突应在持久化异常中转换为稳定响应。

## 三、添加Validation依赖

打开项目根目录的 `pom.xml`，在 `<dependencies>` 中加入：

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
```

不要手工指定版本；Spring Boot依赖管理会选择与当前3.x版本兼容的Jakarta Validation实现。

`<dependency>` 声明项目需要一个外部类库。`groupId` 是发布组织标识，`artifactId` 是具体库名；两者共同定位 `org.springframework.boot:spring-boot-starter-validation`。这个starter会把Jakarta Validation API及其实现加入编译和运行环境，因此代码才能导入 `jakarta.validation...` 下的类型。它不会因为写入 `pom.xml` 就自动校验，仍需后面的约束注解和 `@Valid` 共同触发。

保存后让IDE刷新Maven项目，或在项目根目录执行：

```powershell
.\mvnw.cmd test
```

构建成功后再编写注解。如果 `jakarta.validation` 无法导入，先确认依赖位于 `<dependencies>` 内并已完成Maven同步。

## 四、完整替换请求DTO

用以下代码完整替换：

```text
src/main/java/com/example/employee/dto/EmployeeCreateRequest.java
```

```java
package com.example.employee.dto;

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

### 当前注解的边界

三个约束注解都来自 `jakarta.validation.constraints`。它们是附着在字段上的校验元数据，由Validation实现读取，不是按书写顺序逐行调用的方法。

| 注解 | 注解参数、可接受值与默认要求 | 当前作用 |
| --- | --- | --- |
| `@NotBlank(message = "...")` | 被校验值必须是字符序列；非 `null` 且至少包含一个非空白字符。`message` 可省略并使用库的默认消息，本例显式提供中文提示 | 阻止缺失、空字符串和纯空格 |
| `@Size(max = 50, message = "...")` | `min` 默认0，`max` 默认允许到整数上限；本例按字段含义设置50或100。`null` 通常由其他注解决定 | 限制字符数量，防止请求文本无限增长 |
| `@Email(message = "...")` | 非空内容需符合实现支持的邮箱格式；`message` 可省略，本例显式提供。空值通常仍需其他注解限制 | 检查邮箱基本格式 |

邮箱同时使用 `@NotBlank` 和 `@Email`：前者决定是否必填，后者决定非空内容的格式。只写 `@Email` 不能完整表达“邮箱必填”。

校验消息是接口的一部分。消息应该帮助调用者修改请求，不包含堆栈、类名、SQL或内部路径。

## 五、在Controller触发请求体校验

`@Valid` 必须放在要校验的 `@RequestBody` 参数上。用下面代码完整替换Controller，避免只复制一个脱离类结构的方法片段：

```java
package com.example.employee.controller;

import com.example.employee.common.ApiResponse;
import com.example.employee.dto.EmployeeCreateRequest;
import com.example.employee.service.EmployeeService;
import com.example.employee.vo.EmployeeResponse;
import jakarta.validation.Valid;
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
            @Valid @RequestBody EmployeeCreateRequest request) {
        EmployeeResponse employee = employeeService.create(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.ok(employee));
    }
}
```

### 第一次出现：@Valid如何触发校验

`@Valid` 的完整类名是 `jakarta.validation.Valid`。它标记 `EmployeeCreateRequest request` 参数，告诉Spring：Jackson创建并填充DTO后、进入 `create()` 方法前，对该对象执行其字段上的约束。

`@RequestBody` 负责“把JSON变成对象”，`@Valid` 负责“检查对象内容”，二者职责不同。校验通过时，Controller收到已经绑定的请求对象；校验失败时，Spring创建并抛出 `MethodArgumentNotValidException`，Controller方法不会执行。

执行顺序：

```text
读取JSON
  ↓ 转换为EmployeeCreateRequest
执行@Valid
  ├─ 通过 → 调用EmployeeService.create() → 返回201
  └─ 失败 → 抛出MethodArgumentNotValidException
```

校验失败时Service不应被调用。这样无效数据在进入业务处理前就被拒绝。

## 六、创建全局校验异常处理器

只添加 `@Valid` 时，Spring能返回400，但响应结构不一定符合本项目的 `ApiResponse`。新建：

```text
src/main/java/com/example/employee/exception/ValidationExceptionHandler.java
```

完整代码：

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
}
```

### 第一次出现：全局异常处理对象和方法

本文件涉及三组来源：`ApiResponse` 是本项目第5章创建的类；`ResponseEntity`、两个异常类和Web注解由Spring提供；`Map`、`LinkedHashMap` 由JDK提供。不要为这些Spring或JDK类型创建同名Java文件。

`@RestControllerAdvice` 来自 `org.springframework.web.bind.annotation`。Spring扫描后创建一个 `ValidationExceptionHandler` Bean，并让其中的异常处理方法作用于多个Controller。`@ExceptionHandler(MethodArgumentNotValidException.class)` 的参数是一个Java `Class` 对象，告诉Spring当前方法接收这种异常；异常发生时，Spring把异常对象传给方法参数 `exception`。

字段校验处理链中的首次方法如下：

| 方法或对象 | 参数 | 返回值与作用 |
| --- | --- | --- |
| `exception.getBindingResult()` | 无 | 返回保存绑定与校验结果的 `BindingResult` 对象 |
| `.getFieldErrors()` | 无 | 返回字段错误组成的 `List<FieldError>` |
| `.forEach(error -> ...)` | 接收一个处理每个元素的函数；无元素时执行0次 | 依次处理每个 `FieldError`；`error` 是当前错误对象 |
| `error.getField()` | 无 | 返回发生错误的字段名，例如 `email` |
| `error.getDefaultMessage()` | 无 | 返回约束注解提供的错误消息；极端情况下可能为 `null` |
| `fieldErrors.putIfAbsent(key, value)` | 键为字段名，值为错误消息 | 仅在键不存在时写入，并返回旧值；这里不使用返回值，从而保留同一字段的第一条错误 |
| `ApiResponse.fail(message, data)` | 错误摘要和字段错误Map | 返回第5章定义的失败响应对象 |
| `ResponseEntity.badRequest()` | 无 | 返回状态为400的构建器；继续 `.body(body)` 得到最终响应对象 |

`error -> ...` 是Java lambda表达式，可以理解为“对列表中的每个错误执行右侧处理”。此处的链式调用仍是从上到下执行：先取得绑定结果，再取得错误列表，最后逐项写入Map。

第二个处理方法面对的是 `HttpMessageNotReadableException`，完整类名位于 `org.springframework.http.converter`。JSON缺少括号、类型无法读取等问题会在DTO校验前由Spring抛出该异常，因此它没有字段校验列表。方法参数 `exception` 保存本次异常对象，当前实现故意不把其内部消息返回给客户端；`ApiResponse<Void>` 中的 `Void` 表示这个响应没有业务数据，所以传入 `null`。

`@RestControllerAdvice` 由组件扫描注册，异常处理方法可以作用于多个Controller。`@ExceptionHandler` 声明当前方法处理的异常类型，`ResponseEntity.badRequest()` 明确设置HTTP 400。

字段错误使用 `LinkedHashMap` 保存，键是请求字段，值是可展示的校验消息。使用 `putIfAbsent()` 时，同一字段一次只返回第一条错误，响应更容易阅读；如果项目要求展示全部错误，可以改为 `Map<String, List<String>>`。

处理JSON解析错误时只返回稳定提示，不把底层异常消息直接暴露给客户端。完整异常应记录到服务端日志中，便于开发和运维排查。

## 七、验证有效请求

启动项目：

```powershell
.\mvnw.cmd spring-boot:run
```

另开PowerShell：

```powershell
$validBody = @'
{
  "name": "Sato",
  "department": "Development",
  "email": "sato@example.com"
}
'@

curl.exe -i `
    -X POST "http://localhost:8080/employees" `
    -H "Content-Type: application/json" `
    --data-binary $validBody
```

`curl.exe` 是发送HTTP请求的命令行工具，不是Java类。`-i` 要求输出响应头，`-X POST` 指定请求方法，`-H` 添加请求头，`--data-binary` 原样发送后面的正文变量；这里四项都用于观察完整POST结果。PowerShell中显式写 `.exe` 可以避免与旧版本命令别名混淆。

预期响应状态行包含：

```text
HTTP/1.1 201
```

响应体的 `success` 为 `true`，`data` 包含创建后的员工。

## 八、验证字段校验失败

```powershell
$invalidBody = @'
{
  "name": "   ",
  "department": "",
  "email": "not-an-email"
}
'@

curl.exe -i `
    -X POST "http://localhost:8080/employees" `
    -H "Content-Type: application/json" `
    --data-binary $invalidBody
```

预期状态行包含 `HTTP/1.1 400`，响应结构类似：

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

字段顺序不属于接口契约。只要状态为400、三个字段都有可理解的错误信息，就符合本章结果。

确认启动日志中没有进入数据库操作；当前还没有数据库，但从流程上已经保证Service不会处理无效请求。

## 九、验证JSON解析失败

下面故意漏掉右花括号：

```powershell
$brokenJson = '{"name":"Sato"'

curl.exe -i `
    -X POST "http://localhost:8080/employees" `
    -H "Content-Type: application/json" `
    --data-binary $brokenJson
```

预期返回400：

```json
{
  "success": false,
  "message": "请求JSON格式错误",
  "data": null
}
```

JSON解析失败发生在生成DTO之前，因此不会产生字段校验结果。它和“JSON合法但字段内容无效”是两个不同阶段。

## 十、没有@Valid会发生什么

临时删除Controller参数前的 `@Valid`，重新启动后发送空姓名请求。请求会进入Service并返回201，说明DTO上的约束注解不会自行触发请求体校验。

观察后立即恢复 `@Valid` 并再次确认响应为400。这个对照实验比只背诵注解作用更重要。

## 十一、常见失败

| 现象 | 原因 | 修正 |
| --- | --- | --- |
| `jakarta.validation` 无法导入 | Validation依赖未加入或Maven未刷新 | 检查 `pom.xml` 并重新加载Maven |
| 空字段仍返回201 | Controller参数缺少 `@Valid` | 恢复 `@Valid @RequestBody` |
| 返回400但不是统一结构 | 异常处理器未被扫描或异常类型不匹配 | 检查包路径和 `@RestControllerAdvice` |
| 邮箱为空时没有“必填”错误 | 只使用了 `@Email` | 同时使用 `@NotBlank` |
| 客户端收到内部异常细节 | 直接返回 `exception.getMessage()` | 返回稳定外部消息，内部细节只进日志 |
| 所有错误仍返回HTTP 200 | 只构造失败响应体 | 使用 `ResponseEntity.badRequest()` |

## 十二、操作练习

初始状态：有效请求返回201，字段错误和JSON语法错误返回400。

任务：

1. 给部门增加“最长30字符”的规则，并验证超长请求。
2. 新增可选字段 `phone`：为空时允许通过，非空时必须匹配数字和连字符，可使用 `jakarta.validation.constraints.Pattern` 提供的 `@Pattern(regexp = "[0-9-]+", message = "电话号码格式不正确")`。`regexp` 必须填写正则表达式，`message` 可省略并使用默认消息；该注解允许 `null`，因此能表达“可选但填写后必须合规”。
3. 临时删除 `@Valid`，记录同一无效请求的状态变化，再恢复。
4. 分别保存有效、字段无效、JSON损坏三次请求的状态码和响应体。

验收标准：

- 有效请求为201，且成功响应结构与第5章一致。
- 无效字段为400，`data` 能指出具体字段。
- 损坏JSON为400，但消息与字段校验不同。
- 错误响应不包含堆栈、SQL、服务器路径或内部类名。
- 能说明参数格式校验与Service业务校验的责任边界。

完成练习后，可保留符合业务需要的部门长度规则；如果添加了 `phone`，需要在DTO、响应和数据库设计中持续维护，否则请撤销该练习字段。

## 十三、当前稳定状态

当前工程应至少包含：

```text
common/ApiResponse.java
controller/EmployeeController.java
dto/EmployeeCreateRequest.java
exception/ValidationExceptionHandler.java
service/EmployeeService.java
vo/EmployeeResponse.java
```

当前请求链路为：

```text
HTTP请求
  → Controller绑定与Validation
  → Service处理
  → ApiResponse与HTTP状态
```

在这个稳定状态上可以继续增加Entity、Mapper、MySQL配置和真实数据查询；不需要重新创建Spring Boot工程，也不需要删除现有校验与响应结构。
