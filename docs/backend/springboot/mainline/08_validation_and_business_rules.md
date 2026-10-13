# 第8章 输入校验与业务规则

> 本章目标：通过一组连续案例，观察JSON读取、DTO字段校验和Service业务规则分别在什么阶段生效，并把失败转换为第7章建立的稳定HTTP响应。

第7章已经能返回200、404、409和500，但请求正文中的空白姓名、错误邮箱和非法部门仍需要在不同位置处理。本章从合法预览请求出发，依次加入字段约束、校验触发、统一错误响应和部门业务规则；每一步都使用同一个接口验证当前代码状态。

阅读与操作顺序：先按案例逐步修改并用Postman观察当前阶段，再用“章末完整代码”核对所有案例完成后的最终文件。章末代码不是第一个案例的起始状态，不能提前整体替换。

## 一、开始状态与案例规格

继续使用第7章工程。所有案例都使用：

| 项目 | 内容 |
| --- | --- |
| HTTP方法 | POST |
| URL | `{{baseUrl}}/employees/preview` |
| 查询参数 | 无 |
| 请求头 | `Content-Type: application/json` |
| 请求体 | Postman选择 **raw → JSON** |

本章要修改或新增：

```text
pom.xml                                             ← 添加Validation依赖
src/main/java/com/example/employee/
├── common/ApiResponse.java                         ← 增加可携带字段错误的失败方法
├── controller/EmployeeController.java              ← 在请求参数上触发校验
├── dto/EmployeeCreateRequest.java                  ← 声明字段约束
├── exception/InvalidDepartmentException.java       ← 新建业务异常
├── exception/GlobalExceptionHandler.java            ← 增加两类400处理
└── service/EmployeeService.java                    ← 保留并补充业务规则
```

字段规格如下：

| 字段 | 字段规则 | 业务规则 |
| --- | --- | --- |
| `name` | 必填、不能只含空白，最多50字符 | 本章无额外规则 |
| `department` | 必填、不能只含空白，最多50字符 | 暂按Sales、Development、Support（正式业务规格待确认） |
| `email` | 必填、不能只含空白，最多100字符，格式基本正确 | `used@example.com`视为已占用 |

目前未找到高于现有实现的正式部门规格。本章暂按已有Service及第10章行为统一为 `Sales`、`Development`、`Support`，字段上限与第9章DDL统一为50字符；收到正式规格后，应同步调整DTO、Service、数据库及接口测试。

三条失败路线将在下面逐一验证：

| 失败类型 | 失败阶段 | Controller方法 | Service | 预期状态 |
| --- | --- | --- | --- | --- |
| JSON语法损坏 | 消息转换器读取正文 | 不执行 | 不执行 | 400 |
| DTO字段不合格 | Controller参数解析完成后、方法调用前 | 不执行 | 不执行 | 400 |
| 业务规则不允许 | Service执行期间 | 已执行 | 已执行并拒绝 | 400或409 |

## 二、案例8-01：合法员工请求通过处理

从第7章稳定工程开始。在 `pom.xml` 的 `<dependencies>` 中新增下列依赖；第7章已有的DTO、Controller、Service保持不变。本案例只验证成功路径，不依赖尚未建立的字段错误处理器。

~~~xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
~~~

### 代码说明与验证

#### 1. 案例目标

先建立成功基准。发送：

```json
{
  "name": "Sato",
  "department": "Development",
  "email": "sato@example.com"
}
```

本章完成后仍应返回200。后面的失败案例都只改变其中一个条件，避免同时制造多个原因。

#### 2. Validation依赖解决什么问题

Spring MVC负责读取HTTP请求，Jakarta Validation负责执行DTO上声明的约束。项目需要添加：

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
```

这个Starter提供Jakarta Validation API及其实现。只写约束注解而没有实现依赖，项目不能完成实际校验。它不会替代Controller或Service，也不会自动判断数据库中的业务状态。

#### 3. 成功调用链

```text
Postman发送合法JSON
  → Spring MVC通过Jackson读取JSON
  → 创建EmployeeCreateRequest
  → EmployeeController.previewCreate()
  → EmployeeService.previewCreate()
  → HTTP 200和统一响应
```

当前阶段只添加Validation依赖：DTO尚未声明字段约束，Controller也尚未使用 `@Valid`，所以这里没有执行对象字段校验。此案例只能确认正常请求仍可处理，不能证明输入字段已受Validation保护。下一案例先声明约束，案例8-03再触发校验。


Postman发送 `POST {{baseUrl}}/employees/preview`，Body为 `{"name":"Sato","department":"Development","email":"sato@example.com"}`，无查询参数；预期200且 `data` 为预览字符串。此时DTO尚无约束，添加依赖本身不会拒绝非法输入。

## 三、案例8-02：在DTO声明字段约束

在案例8-01基础上完整替换 `EmployeeCreateRequest.java`。本案例只声明规则；Controller尚无 `@Valid`，因此先观察“约束存在但HTTP请求尚未触发校验”的状态。

### 完整替换EmployeeCreateRequest.java

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


### 代码说明： 在DTO上声明约束

在 `EmployeeCreateRequest` 的字段上使用：

```java
@NotBlank(message = "员工姓名不能为空")
@Size(max = 50, message = "员工姓名不能超过50个字符")
private String name;
```

`@NotBlank` 和 `@Size` 都来自 `jakarta.validation.constraints`，但职责不同：

- `@NotBlank` 拒绝 `null`、空字符串和只包含空白的字符串；
- `@Size(max = 50)` 限制字符数量，不能替代必填判断；
- `message` 是校验失败时保存的消息，不会自己决定HTTP状态。

邮箱需要组合“必填、长度、基本格式”三类约束：

```java
@NotBlank(message = "邮箱不能为空")
@Size(max = 100, message = "邮箱不能超过100个字符")
@Email(message = "邮箱格式不正确")
private String email;
```

`@Email` 只检查基本格式，不会判断邮箱是否真实存在，也不会判断是否已被使用。


`@NotBlank` 同时拒绝 `null`、空字符串与纯空白；`@Size` 只限制长度。部门上限为50，允许值由后续Service业务规则决定。

使用Postman发送 `POST {{baseUrl}}/employees/preview`，Body为 `{"name":"   ","department":"Development","email":"sato@example.com"}`；当前仍可能得到200，证明字段上的注解本身尚未触发这次HTTP校验。下一案例加入触发入口。

## 四、案例8-03：用@Valid触发对象校验

在已有 `EmployeeController.java` 中导入 `jakarta.validation.Valid`，仅修改 `previewCreate` 方法的参数，保留原方法体及其他接口：

~~~java
@Valid @RequestBody EmployeeCreateRequest request
~~~

### 代码说明： 用@Valid触发本次对象校验

Controller参数写成：

```java
public ResponseEntity<ApiResponse<String>> previewCreate(
        @Valid @RequestBody EmployeeCreateRequest request)
```

`@RequestBody` 先让Spring MVC通过Jackson读取JSON；`@Valid` 来自 `jakarta.validation.Valid`，要求Spring MVC在调用Controller方法前检查这个对象上的约束。它没有属性，完整写法就是 `@Valid`。

如果没有 `@Valid`，DTO上的注解仍然存在，但这次HTTP参数不会因此自动执行对象校验。


现在发送与上一案例相同的空白姓名请求：Spring MVC在调用Controller前拒绝它，HTTP为400。此时尚未新增项目专用的字段错误处理方法，因此响应体可能是Spring Boot默认错误结构，不能要求已有 `ApiResponse` 字段Map。

### 对照实验：移除并恢复@Valid

在首次验证400后进行以下受控实验：

1. 临时删除Controller参数前的 `@Valid`；
2. 在Eclipse中重新启动；
3. 发送空白姓名，但部门和邮箱填写有效值；
4. 预期请求进入Service，当前预览逻辑返回200；
5. 恢复 `@Valid` 并重新启动；
6. 发送相同请求，预期在进入Controller前返回400。

实验结束后必须保留 `@Valid`。这个结果证明：字段上的约束负责声明规则，Controller参数上的 `@Valid` 负责触发这次对象校验。

案例完成状态：恢复并保留 `@Valid`；非法字段返回400，统一字段Map将在下一案例加入。

## 五、案例8-04：将字段错误转换为统一400

在现有 `ApiResponse.java` 中增加下面的重载方法，并将已有 `failure(String message)` 改为调用 `failure(message, null)`；在第7章的 `GlobalExceptionHandler.java` 中追加字段错误处理方法与所需import。这些都是已有文件的局部修改。

~~~java
public static <T> ApiResponse<T> failure(String message) {
    return failure(message, null);
}

public static <T> ApiResponse<T> failure(String message, T data) {
    return new ApiResponse<>(false, message, data);
}
~~~

在处理器导入 `java.util.LinkedHashMap`、`java.util.Map`、`org.springframework.validation.FieldError` 和 `org.springframework.web.bind.MethodArgumentNotValidException`，追加：

~~~java
@ExceptionHandler(MethodArgumentNotValidException.class)
public ResponseEntity<ApiResponse<Map<String, String>>> handleValidation(
        MethodArgumentNotValidException exception) {
    Map<String, String> fieldErrors = new LinkedHashMap<>();
    for (FieldError fieldError : exception.getBindingResult().getFieldErrors()) {
        fieldErrors.putIfAbsent(
                fieldError.getField(), fieldError.getDefaultMessage());
    }
    return ResponseEntity.badRequest()
            .body(ApiResponse.failure("参数校验失败", fieldErrors));
}
~~~

### 代码说明： 把字段错误转换为统一400响应

校验失败时Spring MVC抛出 `MethodArgumentNotValidException`。处理器的关键代码是：

```java
for (FieldError fieldError : exception.getBindingResult().getFieldErrors()) {
    fieldErrors.putIfAbsent(
            fieldError.getField(), fieldError.getDefaultMessage());
}
return ResponseEntity.badRequest()
        .body(ApiResponse.failure("参数校验失败", fieldErrors));
```

这里：

- `getBindingResult()` 取得本次参数绑定和校验结果；
- `getFieldErrors()` 返回字段错误列表；
- `FieldError.getField()` 返回字段名；
- `getDefaultMessage()` 返回约束中的消息；
- `putIfAbsent()` 让同一字段只保留第一条消息；
- `ResponseEntity.badRequest()` 决定HTTP状态为400；
- `ApiResponse.failure(..., fieldErrors)` 决定JSON正文结构。

为此，`ApiResponse` 保留原有 `failure(String message)`，再增加重载：

```java
public static <T> ApiResponse<T> failure(String message, T data) {
    return new ApiResponse<>(false, message, data);
}
```

### 验证： 立即验证

发送：

```json
{
  "name": "   ",
  "department": "Development",
  "email": "sato@example.com"
}
```

预期状态为400，`data.name` 显示“员工姓名不能为空”，Controller和Service都不执行。可在Controller方法第一行放置断点确认；验证后不需要保留额外调试代码。


`LinkedHashMap` 保持字段首次出现的插入顺序；同一字段可能同时违反多个约束，`putIfAbsent` 只保存遍历到的首条消息，不能承诺多条约束错误中哪一条一定先出现。Controller和Service均不执行。

## 六、案例8-05：区分JSON读取失败与字段校验失败

在已有 `GlobalExceptionHandler.java` 导入 `org.springframework.http.converter.HttpMessageNotReadableException`，追加下面的方法；已有字段错误方法保持不变。

~~~java
@ExceptionHandler(HttpMessageNotReadableException.class)
public ResponseEntity<ApiResponse<Void>> handleUnreadableJson(
        HttpMessageNotReadableException exception) {
    return ResponseEntity.badRequest()
            .body(ApiResponse.failure("请求JSON格式错误"));
}
~~~

### 代码说明与验证

#### 1. 制造JSON读取失败

发送不完整正文：

```json
{"name":"Sato"
```

此时Jackson还没有创建完整的 `EmployeeCreateRequest`，所以字段约束无从执行。Spring MVC在消息转换阶段抛出 `HttpMessageNotReadableException`。

#### 2. 增加对应处理器

下面是 `GlobalExceptionHandler` 中的方法片段：

```java
@ExceptionHandler(HttpMessageNotReadableException.class)
public ResponseEntity<ApiResponse<Void>> handleUnreadableJson(
        HttpMessageNotReadableException exception) {
    return ResponseEntity.badRequest()
            .body(ApiResponse.failure("请求JSON格式错误"));
}
```

`@ExceptionHandler` 由Spring MVC处理；括号中的异常类型决定它匹配哪类失败。不要把 `exception.getMessage()` 直接返回客户端，其中可能包含Java类型和解析器内部信息。

#### 3. 比较两个400

| 对比 | JSON读取失败 | DTO字段校验失败 |
| --- | --- | --- |
| DTO是否正常建立 | 不保证 | 是 |
| 字段约束是否执行 | 否 | 是 |
| 主要异常 | `HttpMessageNotReadableException` | `MethodArgumentNotValidException` |
| 是否有字段错误Map | 否 | 有 |
| Controller、Service | 都不执行 | 都不执行 |

两者状态都是400，但消息和诊断入口不同。相同状态码不代表相同处理阶段。


在Postman使用相同POST URL，raw JSON正文为 `{"name":"Sato"`，预期400、无字段错误Map；再发送合法JSON但空白姓名，预期400且 `data.name` 有错误消息。两次请求均未进入Controller方法。

## 七、案例8-06：Service拒绝不允许的部门

在已有项目中新建 `InvalidDepartmentException.java`；修改 `EmployeeService.previewCreate`，使部门判断位于邮箱冲突判断之前；在现有 `GlobalExceptionHandler` 内追加对应方法。

### 新建InvalidDepartmentException.java

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


在Service导入 `InvalidDepartmentException`，将下面的片段放在 `previewCreate` 方法开头，并在同一类中增加私有方法；其他返回逻辑保持不变：

~~~java
if (!isAllowedDepartment(request.getDepartment())) {
    throw new InvalidDepartmentException(request.getDepartment());
}

private boolean isAllowedDepartment(String department) {
    return "Sales".equals(department)
            || "Development".equals(department)
            || "Support".equals(department);
}
~~~

注意 `private boolean isAllowedDepartment` 是类中的独立方法，不能粘贴到 `previewCreate` 方法内部。

~~~java
@ExceptionHandler(InvalidDepartmentException.class)
public ResponseEntity<ApiResponse<Void>> handleInvalidDepartment(
        InvalidDepartmentException exception) {
    return ResponseEntity.badRequest()
            .body(ApiResponse.failure(exception.getMessage()));
}
~~~

### 代码说明与验证

#### 1. 为什么字段约束不能完成全部判断

`Other` 不是空白，长度也不超过50，因此可以通过DTO约束；但它不在当前业务允许的部门中。Service必须再次判断：

```java
if (!isAllowedDepartment(request.getDepartment())) {
    throw new InvalidDepartmentException(request.getDepartment());
}
```

`isAllowedDepartment()` 是项目自己的私有方法，不是Spring或Validation API。业务规则放在Service中，是因为同一规则将来还可能被其他Controller、批处理或测试调用。

#### 2. 异常怎样成为400

`InvalidDepartmentException` 表达明确的业务含义。全局异常处理器增加对应方法：

```java
@ExceptionHandler(InvalidDepartmentException.class)
public ResponseEntity<ApiResponse<Void>> handleInvalidDepartment(
        InvalidDepartmentException exception) {
    return ResponseEntity.badRequest()
            .body(ApiResponse.failure(exception.getMessage()));
}
```

发送：

```json
{
  "name": "Sato",
  "department": "Other",
  "email": "sato@example.com"
}
```

预期状态为400。这个请求已经进入Controller和Service，和前两个400的执行阶段不同。


使用Postman发送合法姓名与邮箱、部门为 `Other` 的请求，预期400；改为 `Support`，预期200。业务允许值暂按现有实现确定，正式规格待确认。

## 八、案例8-07：邮箱格式与占用状态

沿用第7章已有的 `DuplicateEmailException` 和409处理器，本案例不修改代码。先发送普通邮箱确认200，再发送：

```json
{
  "name": "Sato",
  "department": "Development",
  "email": "used@example.com"
}
```

它能通过 `@Email`，因为格式合法；Service再根据当前系统状态抛出 `DuplicateEmailException`，第7章的处理器返回409。

```text
格式是否合法       → DTO Validation
当前是否允许使用   → Service业务规则
系统状态发生冲突   → HTTP 409
```

这说明“格式正确”不等于“业务允许”。不要把重复邮箱检查塞进 `@Email`，也不要只依靠前端校验。

## 九、章末完整代码

以下文件是所有案例及临时实验结束后的最终状态，用于逐文件核对。已经按案例完成修改的文件无需再次覆盖。

### 1. 完整替换pom.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.5.16</version>
        <relativePath/>
    </parent>

    <groupId>com.example</groupId>
    <artifactId>employee-management-api</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <name>employee-management-api</name>
    <description>Employee management REST API</description>

    <properties>
        <java.version>17</java.version>
    </properties>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

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

import java.util.ArrayList;
import java.util.List;

@Service
public class EmployeeService {

    private static final List<EmployeeResponse> SAMPLE_EMPLOYEES = List.of(
            new EmployeeResponse(
                    1001L,
                    "Tanaka",
                    "Sales",
                    "tanaka@example.com"));

    public EmployeeResponse findById(Long id) {
        for (EmployeeResponse employee : SAMPLE_EMPLOYEES) {
            if (employee.getId().equals(id)) {
                return employee;
            }
        }
        throw new EmployeeNotFoundException(id);
    }

    public List<EmployeeListItemResponse> findList(String department) {
        List<EmployeeListItemResponse> results = new ArrayList<>();
        for (EmployeeResponse employee : SAMPLE_EMPLOYEES) {
            if (employee.getDepartment().equals(department)) {
                results.add(new EmployeeListItemResponse(
                        employee.getId(),
                        employee.getName(),
                        employee.getDepartment()));
            }
        }
        return results;
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


## 十、本章综合验证

完成全部案例并恢复 `@Valid` 后，在Eclipse中从启动类运行应用。Console没有启动错误后，打开第6章建立的Postman Collection。下面的POST请求都使用 `{{baseUrl}}/employees/preview`；除特别说明外，Body选择 **raw → JSON**，并确认 `Content-Type: application/json`。

### 1. 有效请求

```json
{
  "name": "Sato",
  "department": "Development",
  "email": "sato@example.com"
}
```

请求参数为无，预期状态为200，`success` 为 `true`，`data` 是预览文本。

### 2. 字段校验失败

```json
{
  "name": "   ",
  "department": "",
  "email": "not-an-email"
}
```

请求参数为无，预期状态为400，正文包含：

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

在Postman中保留HTTP方法POST和 `Content-Type: application/json`，Body选择 **raw → JSON**，故意输入不完整正文：

```json
{"name":"Sato"
```

预期返回400，消息是 `请求JSON格式错误`，`data` 为 `null`，而不是字段错误Map。

### 4. 业务规则失败

```json
{
  "name": "Sato",
  "department": "Other",
  "email": "sato@example.com"
}
```

请求参数为无，预期状态为400，消息是 `不允许的部门：Other`，`data` 为 `null`。这证明DTO字段校验通过后，Service仍会执行独立的业务判断。

### 5. 状态冲突和原接口回归

继续验证：

| HTTP方法 | URL | Params、Headers与Body | 预期状态 | 关键结果 |
| --- | --- | --- | ---: | --- |
| POST | `{{baseUrl}}/employees/preview` | JSON部门合法、邮箱为 `used@example.com` | 409 | 邮箱冲突消息 |
| GET | `{{baseUrl}}/employees/1001` | 无Body | 200 | 员工详情成功结构 |
| GET | `{{baseUrl}}/employees/9999` | 无Body | 404 | 员工不存在结构 |
| GET | `{{baseUrl}}/employees/abc` | 无Body | 400 | 路径类型转换失败 |
| POST | `{{baseUrl}}/employees/preview` | `Content-Type: text/plain`，raw Text为 `not-json` | 415 | 媒体类型错误 |
| GET | `{{baseUrl}}/health` | 无Body | 200 | `OK` |

逐条保存实际状态码和响应Body。验证完成后在Eclipse Console中停止应用，并把损坏JSON请求恢复成合法示例，避免下次误用。

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

### 练习3：证明@Valid不可缺少

临时删除Controller参数前的 `@Valid`，在Eclipse中重新启动后发送空白姓名（部门和邮箱仍填写有效值）。预期请求进入Service，当前预览逻辑返回200；记录这一结果。随后恢复 `@Valid` 并重新启动，同一请求应在进入Service前返回400。这个实验结束后必须保留 `@Valid`。

### 练习4：整理测试证据和Review指摘

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

## 十四、附录：其他约束与可选字段

以下内容用于阅读既有项目或独立练习，不是本章预览接口的实现前提。完成练习后必须恢复主线稳定状态。

### A. 约束注解的其他属性

Jakarta Validation约束注解都有三个共同属性：

| 共同属性 | 类型 | 默认值与作用 |
| --- | --- | --- |
| `message` | `String` | 使用该约束的默认消息；可以填写固定文字，也可以填写消息资源键 |
| `groups` | `Class<?>[]` | 默认空数组，约束属于默认校验组；只有按业务阶段选择校验规则时才需要显式填写 |
| `payload` | `Class<? extends Payload>[]` | 默认空数组，用于附带给校验客户端读取的元数据，普通业务项目很少使用 |

`groups` 和 `payload` 不是字段允许值的条件。新人阶段不要因为看到它们就随意填写；分组用法统一放在附录[使用@Validated选择校验分组](../appendix/A10_validated_validation_groups.md)。本章涉及的每个约束注解，其全部属性一行写法如下：

```java
@NotBlank(message = "文本不能为空", groups = {}, payload = {})
@NotEmpty(message = "集合或文本不能为空", groups = {}, payload = {})
@NotNull(message = "值不能为空", groups = {}, payload = {})
@Size(min = 0, max = 50, message = "长度不能超过50", groups = {}, payload = {})
@Email(message = "邮箱格式不正确", regexp = ".+@example\\.com", flags = {}, groups = {}, payload = {})
@Min(value = 1, message = "不能小于1", groups = {}, payload = {})
@Max(value = 100, message = "不能大于100", groups = {}, payload = {})
@Positive(message = "必须大于0", groups = {}, payload = {})
@PositiveOrZero(message = "不能小于0", groups = {}, payload = {})
@Pattern(regexp = "[0-9-]+", flags = {}, message = "格式不正确", groups = {}, payload = {})
```

这些行是属性查阅示例，不是一组应该同时加到同一字段上的规则。`@Email` 的 `regexp` 用于叠加项目自己的正则限制，`flags` 用于改变该正则的匹配方式；本章项目只需要基础邮箱格式，所以正文仍使用更简单的 `@Email(message = "邮箱格式不正确")`。

### B. 其他常见约束

#### 1. @NotNull、@NotEmpty和@NotBlank

三个注解不能只看名称选择：

| 注解 | `null` | `""` | `"   "` | 适合场景 |
| --- | --- | --- | --- | --- |
| `@NotNull` | 失败 | 通过 | 通过 | 对象、日期、数字等只要求必须有值 |
| `@NotEmpty` | 失败 | 失败 | 通过 | 集合或字符串不能没有元素 |
| `@NotBlank` | 失败 | 失败 | 失败 | 姓名、部门等不能只有空白的文本 |

当前三个请求字段都是必填文本，所以选择 `@NotBlank`，不重复叠加 `@NotNull`。以后出现必须提供的数字或日期对象时，再使用 `@NotNull`。

#### 2. 数值范围约束

数值字段常用：

| 注解 | 适用规则 | 示例 |
| --- | --- | --- |
| `@Min(1)` | 数值不能小于指定下限 | 页码至少为1 |
| `@Max(100)` | 数值不能大于指定上限 | 每页最多100条 |
| `@Positive` | 数值必须大于0 | 系统编号必须为正数 |
| `@PositiveOrZero` | 数值必须大于或等于0 | 数量不能为负数 |

这些注解同样来自 `jakarta.validation.constraints`。当前DTO没有数字字段，不应为了展示注解虚构年龄或级别字段。分页参数出现时，应根据正式规格选择 `@Min`、`@Max`；员工编号的业务含义也必须先由接口规格确认。

`@Min`、`@Max`、`@Positive` 和 `@PositiveOrZero` 通常把 `null` 视为不由当前约束判断的值。因此，“数值范围正确”和“数值必须提供”是两条不同规则。必填数字需要组合约束：

```java
@NotNull(message = "页码不能为空")
@Min(value = 1, message = "页码必须大于等于1")
private Integer page;
```

这里使用包装类型 `Integer`，才能区分“没有提供”形成的 `null` 和实际数字。若使用基本类型 `int`，字段本身不能保存 `null`，还要结合请求绑定方式判断缺失值会在哪个阶段失败。

### C. @Validated与分组

既存项目中还经常出现Spring提供的 `@Validated`。它也能触发请求对象校验，并且可以选择校验分组；本章没有分组需求，继续使用Jakarta Validation的 `@Valid`。完整示例和两者区别见附录[使用@Validated选择校验分组](../appendix/A10_validated_validation_groups.md)。

### D. 可选电话号码练习


临时给请求DTO增加可选字段 `phone`。省略或 `null` 时允许通过，填写后只能包含数字和连字符：

```java
@Pattern(
        regexp = "[0-9-]+",
        message = "电话号码格式不正确")
private String phone;
```

`@Pattern` 来自 `jakarta.validation.constraints`。`regexp` 必须是Java正则表达式字符串，`message` 省略时使用默认消息。该约束允许 `null`，因此可以表达“可选，但填写后必须合规”。

补充getter和setter，验证省略、合法值和非法值。完成后删除该字段、导入和方法，避免没有接口规格的练习字段进入后续章节。
