# 第7章 成功与失败怎样返回

> 本章目标：通过员工接口的成功、未找到、业务冲突和系统故障案例，学会让HTTP状态与JSON正文共同表达处理结果。

第6章的Controller已经能够接收请求。本章沿用固定员工样例，逐个完成200、404、409与空列表响应，并通过一次受控故障实验观察500；每个案例完成后立即用Postman验证。

阅读与操作顺序：从案例一开始逐个修改、运行、核对结果；“章末完整代码”只用于完成全部案例后比对最终文件、导入和未展示的原有方法。不要一开始就整体复制章末代码，否则会跳过成功、业务失败与异常处理的递进过程。

## 一、开始状态与案例规格

继续使用第6章的路径和请求方式：

| 案例 | HTTP方法与URL | 预期状态 |
| --- | --- | --- |
| 查询存在员工 | `GET {{baseUrl}}/employees/1001` | 200 |
| 查询不存在员工 | `GET {{baseUrl}}/employees/9999` | 404 |
| 查询空列表 | `GET {{baseUrl}}/employees?department=HR` | 200 |
| 邮箱已经使用 | `POST {{baseUrl}}/employees/preview` | 409 |
| 受控故障实验（临时） | `GET {{baseUrl}}/employees/5000` | 加入临时触发代码后返回500，实验结束必须恢复 |

本章会依次建立：

```text
ApiResponse                 ← 统一JSON正文
业务异常                    ← Service表达失败原因
EmployeeService             ← 成功或抛出业务异常
EmployeeController          ← 使用ResponseEntity返回HTTP响应
GlobalExceptionHandler      ← 把异常统一转换为状态与正文
```

## 二、案例7-01：查询成功并返回200

从第6章工程开始。第6章的 `EmployeeService.findById(1001L)` 已返回 `id=1001`、`name=Tanaka`、`department=Sales`、`email=tanaka@example.com`；因此本案例直接使用这组固定数据，无须修改Service。新建 `common/ApiResponse.java`，完整替换 `controller/EmployeeController.java`，将该员工详情包装为统一响应。本案例只建立200成功响应；404、409及异常处理留给后续案例。

### 新建ApiResponse.java

这个类位于 `common` 包，提供所有Controller和Advice共用的JSON正文格式；它不决定HTTP状态。文件位置：

```text
src/main/java/com/example/employee/common/ApiResponse.java
```

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


### 完整替换EmployeeController.java

Controller保留第6章路径与输入绑定，只把成功结果包装成200；失败时方法被异常中断。

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


### 代码说明与验证

#### 1. HTTP响应有两个需要分别控制的部分

一个接口响应至少要区分：

```text
HTTP状态      → 告诉客户端请求整体结果
JSON响应体    → 提供业务数据或稳定消息
```

例如查询成功：

```http
HTTP/1.1 200 OK
Content-Type: application/json
```

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

#### 2. ResponseEntity控制HTTP响应

Controller的成功方法片段：

```java
EmployeeResponse employee = employeeService.findById(id);
return ResponseEntity.ok(ApiResponse.success(employee));
```

`ResponseEntity<T>` 来自 `org.springframework.http`，代表整个HTTP响应，可以控制状态、Headers和Body。这里：

- `ok(...)` 将状态设为200；
- 括号中的 `ApiResponse` 成为响应体；
- 泛型 `T` 表示响应体的Java类型。

如果Controller只返回 `ApiResponse` 对象，Spring仍能把它写成JSON，但在没有其他设置时通常按成功流程返回200。仅创建失败JSON不会自动把状态改成404或409。

#### 3. ApiResponse统一JSON正文

```java
public class ApiResponse<T> {

    private final boolean success;
    private final String message;
    private final T data;

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, "OK", data);
    }
}
```

`ApiResponse<T>` 是项目自己的普通Java类，不是Spring注解。它只规定JSON正文形状：

- `success` 表示业务结果；
- `message` 提供稳定说明；
- `data` 保存当前接口的数据；
- 泛型让同一结构能够包装员工对象、列表或字段错误。

上方完整类使用私有构造方法，避免调用方随意组合三个字段；静态创建方法固定成功与失败的基本形状。Jackson通过getter生成 `success`、`message` 和 `data`。

因此两者不能互相替代：

| 类型 | 负责什么 | 不负责什么 |
| --- | --- | --- |
| `ResponseEntity` | HTTP状态、Headers、Body | 不规定业务JSON字段 |
| `ApiResponse<T>` | JSON正文结构 | 不会自动改变HTTP状态 |

#### 4. 案例验证

在Postman发送 `GET {{baseUrl}}/employees/1001`，无查询参数和请求体。预期HTTP 200，且 `success=true`、`data.id=1001`。状态和正文必须同时核对。


案例完成状态：`GET /employees/1001` 返回200，`data.department` 为 `Sales`；此时不要求统一404或409。

## 三、案例7-02：员工不存在返回404

在案例7-01基础上，新建 `exception/EmployeeNotFoundException.java` 和 `exception/GlobalExceptionHandler.java`；在 `EmployeeService.java` 中新增固定样例集合并完整替换 `findById` 方法。第6章的方法会对任意编号直接构造员工，因此不能只在原方法末尾追加 `throw`。

### 新建EmployeeNotFoundException.java

```java
package com.example.employee.exception;

public class EmployeeNotFoundException extends RuntimeException {

    public EmployeeNotFoundException(Long id) {
        super("员工不存在：" + id);
    }
}
```

### 修改EmployeeService.java

~~~java
// 放在EmployeeService类内、方法外：
private static final List<EmployeeResponse> SAMPLE_EMPLOYEES = List.of(
        new EmployeeResponse(
                1001L, "Tanaka", "Sales", "tanaka@example.com"));

// 完整替换原findById方法：
public EmployeeResponse findById(Long id) {
    for (EmployeeResponse employee : SAMPLE_EMPLOYEES) {
        if (employee.getId().equals(id)) {
            return employee;
        }
    }
    throw new EmployeeNotFoundException(id);
}
~~~

在Service导入 `com.example.employee.exception.EmployeeNotFoundException`。第6章已经导入 `List` 和 `EmployeeResponse`；其他方法保持原样，案例7-05再修改列表方法。

### 新建GlobalExceptionHandler.java

~~~java
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
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.failure(exception.getMessage()));
    }
}
~~~

#### 代码说明与验证

#### 1. Service用业务异常表达失败

Service不应伪造空员工，也不应直接处理HTTP。找不到员工时抛出明确异常：

```java
throw new EmployeeNotFoundException(id);
```

完整Service会先遍历固定样例，找到时立即返回；只有循环结束仍未找到时才执行这条 `throw`。`EmployeeNotFoundException` 表达“按编号查询但员工不存在”。它属于业务异常，因为原因是业务数据状态，不是程序崩溃。

如果每个Controller都重复 `try-catch`，同一种异常可能被写成不同状态和正文。Service只表达业务事实，由统一位置负责HTTP转换。

#### 2. @RestControllerAdvice建立统一处理位置

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
}
```

`@RestControllerAdvice` 来自Spring MVC，作用在类上。应用启动时，Spring把这个类注册为Controller调用链的全局异常处理组件；处理方法的返回值按响应体方式写出。

它不是“捕获JVM中所有异常”。它主要处理Spring MVC请求过程中传播出来的异常，包括Controller调用Service后继续向外抛出的异常。

#### 3. @ExceptionHandler匹配异常

```java
@ExceptionHandler(EmployeeNotFoundException.class)
public ResponseEntity<ApiResponse<Void>> handleEmployeeNotFound(
        EmployeeNotFoundException exception) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(ApiResponse.failure(exception.getMessage()));
}
```

`@ExceptionHandler` 写在方法上，由Spring MVC在异常传播时匹配。括号中的 `EmployeeNotFoundException.class` 是匹配类型；参数接收实际异常对象。

`HttpStatus.NOT_FOUND` 是Spring提供的HTTP状态枚举，值为404。枚举名称比直接写数字更容易Review。这里的 `ApiResponse.failure(...)` 只创建失败正文，真正设置404的是 `ResponseEntity.status(...)`。

完整流程是：

```text
GET /employees/9999
  → Controller调用Service
  → Service抛EmployeeNotFoundException
  → Controller方法停止
  → GlobalExceptionHandler找到匹配方法
  → 返回HTTP 404和失败JSON
```

如果没有匹配的处理方法，异常会交给Spring Boot默认错误处理，正文格式可能与本项目统一结构不同。

#### 4. 案例验证

在Postman发送 `GET {{baseUrl}}/employees/9999`。预期404、`success=false`，响应中不得出现Java异常类名或调用栈。


案例完成状态：存在编号仍返回200，不存在编号返回统一404；其他异常尚不保证统一响应。

## 四、案例7-03：邮箱冲突返回409

在案例7-02基础上，新建 `exception/DuplicateEmailException.java`；修改 `EmployeeService.previewCreate`，并在已有 `GlobalExceptionHandler` 类中追加409方法。以下是局部修改，不要重新新建同名处理器类。

### 新建DuplicateEmailException.java

```java
package com.example.employee.exception;

public class DuplicateEmailException extends RuntimeException {

    public DuplicateEmailException(String email) {
        super("邮箱已被使用：" + email);
    }
}
```

### 修改EmployeeService.previewCreate

在 `EmployeeService.java` 中完整替换原有 `previewCreate()` 方法，不要另加一个同名方法；导入 `com.example.employee.exception.DuplicateEmailException`，其他Service方法保持不变。

~~~java
public String previewCreate(EmployeeCreateRequest request) {
    if ("used@example.com".equals(request.getEmail())) {
        throw new DuplicateEmailException(request.getEmail());
    }

    return request.getName()
            + " / " + request.getDepartment()
            + " / " + request.getEmail();
}
~~~

冲突判断必须位于正常返回语句之前；未冲突时仍沿用原有预览字符串逻辑。

### 在GlobalExceptionHandler中追加方法

~~~java
@ExceptionHandler(DuplicateEmailException.class)
public ResponseEntity<ApiResponse<Void>> handleDuplicateEmail(
        DuplicateEmailException exception) {
    return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(ApiResponse.failure(exception.getMessage()));
}
~~~

### 代码说明与验证

发送预览请求：

```json
{
  "name": "Sato",
  "department": "Development",
  "email": "used@example.com"
}
```

Service发现邮箱与当前状态冲突时抛出 `DuplicateEmailException`。处理器使用：

```java
@ExceptionHandler(DuplicateEmailException.class)
public ResponseEntity<ApiResponse<Void>> handleDuplicateEmail(
        DuplicateEmailException exception) {
    return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(ApiResponse.failure(exception.getMessage()));
}
```

`HttpStatus.CONFLICT` 对应409，表示请求格式可以理解，但与当前资源状态冲突。它不同于404，也不属于500。

业务异常不应全部映射为同一个状态。异常类名称、接口规格和处理器必须表达同一种业务含义。

验证时使用POST `{{baseUrl}}/employees/preview`，Body选择 **raw → JSON**，预期409和稳定失败正文。


案例完成状态：普通邮箱预览为200、已占用邮箱为409；详情接口的200和404保持不变。

## 五、案例7-04：受控模拟系统故障并恢复

先确认前一案例的200、404、409正常。新建 `exception/EmployeeSystemException.java`；在已有 `GlobalExceptionHandler` 内追加指定异常处理方法。随后临时修改Service，实验结束恢复。

### 新建EmployeeSystemException.java

```java
package com.example.employee.exception;

public class EmployeeSystemException extends RuntimeException {

    public EmployeeSystemException(String message) {
        super(message);
    }

    public EmployeeSystemException(
            String message,
            Throwable cause) {
        super(message, cause);
    }
}
```

### 在GlobalExceptionHandler中追加方法

~~~java
@ExceptionHandler(EmployeeSystemException.class)
public ResponseEntity<ApiResponse<Void>> handleEmployeeSystem(
        EmployeeSystemException exception) {
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiResponse.failure("服务器内部错误"));
}
~~~

### 临时修改EmployeeService.findById

在方法开头加入下方代码，并导入 `EmployeeSystemException`；确认其位置早于遍历或查无结果分支。

~~~java
if (id.equals(5000L)) {
    throw new EmployeeSystemException(
            "internal path=C:/secret, detail=demo");
}
~~~

#### 代码说明与验证

#### 1. 业务异常与系统异常

| 类型 | 示例 | 客户端能否通过修改请求处理 |
| --- | --- | --- |
| 业务异常 | 员工不存在、邮箱冲突 | 有时可以 |
| 系统异常 | 空指针、数据库断线、未预料程序错误 | 通常不能 |

上方编号5000分支是教学用故障入口，正式业务代码不应保留用特殊编号制造异常的逻辑。

#### 2. 500不能泄露内部异常消息

上方指定系统异常处理器对外只返回稳定消息。异常的真实类型、路径、SQL或调用栈应进入受控服务端日志，不能直接使用 `exception.getMessage()` 返回客户端。

本章使用明确的 `EmployeeSystemException` 建立安全边界，不增加会影响所有框架异常的宽范围兜底处理。未匹配异常仍交给Spring Boot默认错误处理；正式项目还需要结合日志与统一安全策略决定是否增加兜底处理。

#### 3. 案例验证

发送 `GET {{baseUrl}}/employees/5000`（无参数、无请求体），预期500，正文只能出现“服务器内部错误”，不能出现模拟异常中的路径和detail。记录结果后删除临时分支和Service中的对应import，重启并重新验证1001返回200、9999返回404。异常类与指定处理方法保留；最终代码不包含故障触发入口。该处理方法只匹配 `EmployeeSystemException` 及符合异常匹配规则的类型，其他未知异常不保证返回项目的 `ApiResponse`。

## 六、案例7-05：空列表仍返回200

在案例7-04恢复后的代码上，修改 `EmployeeService.findList` 为按固定样例筛选。该方法是局部替换，Service其他方法保持不变：

~~~java
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
~~~

所需 `ArrayList`、`List` 和 `EmployeeListItemResponse` 导入见章末完整文件。Controller的 `GET /employees` 继续调用这个方法。

### 代码说明与验证

发送：

```http
GET /employees?department=HR
```

查询执行成功但没有匹配记录时，返回：

```json
{
  "success": true,
  "message": "OK",
  "data": []
}
```

“没有匹配项”和“请求的单个员工不存在”不是同一种语义：

- 列表查询成功且结果数量为0，通常返回200和空数组；
- 按唯一编号查询单个资源而不存在，返回404。

案例7-02建立的 `SAMPLE_EMPLOYEES` 是不可修改的固定样例集合。列表查询从这组数据筛选，不根据请求参数伪造员工部门；它只替代数据库用于本章响应教学，第9章会用MyBatis查询真实数据。

空列表不要返回 `null`，否则客户端还要额外判断数据是否缺失。


案例完成状态：`department=HR` 查询为200且 `data=[]`，按编号查询不存在仍为404。

## 七、案例7-06：为CRUD准备201与204

这里仅阅读状态语义，不修改第7章代码或新增CRUD接口。第10章新增员工时会使用201，删除成功时会使用204。本章先理解语义：

```java
ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success(employee));
```

201表示服务器成功创建了新资源，可以携带创建结果。

```java
ResponseEntity.noContent().build();
```

204表示请求成功，但响应没有Body，因此不应再包装普通 `ApiResponse` JSON。它不是空列表的替代状态。

到这里，本章案例已经覆盖当前项目需要的主要响应决策：

| 场景 | 状态 | 正文 |
| --- | --- | --- |
| 详情成功 | 200 | 成功结构和员工数据 |
| 空列表 | 200 | 成功结构和空数组 |
| 员工不存在 | 404 | 失败结构 |
| 邮箱冲突 | 409 | 失败结构 |
| 受控模拟的EmployeeSystemException | 500 | 安全失败结构 |
| 删除成功（第10章） | 204 | 无正文 |

## 八、章末完整代码

以下文件是所有案例完成、且删除500临时触发代码后的最终状态，用于逐文件核对。已经按案例完成修改的文件无需再次覆盖。

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

### 1. 新建ApiResponse.java

这个类位于 `common` 包，提供所有Controller和Advice共用的JSON正文格式；它不决定HTTP状态。文件位置：

```text
src/main/java/com/example/employee/common/ApiResponse.java
```

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

这些类型放在 `exception` 包，仅表达业务或系统失败的含义，不包含HTTP状态。Service负责抛出，Advice负责转换。

`EmployeeNotFoundException.java`：

```java
package com.example.employee.exception;

public class EmployeeNotFoundException extends RuntimeException {

    public EmployeeNotFoundException(Long id) {
        super("员工不存在：" + id);
    }
}
```

`DuplicateEmailException.java`：

```java
package com.example.employee.exception;

public class DuplicateEmailException extends RuntimeException {

    public DuplicateEmailException(String email) {
        super("邮箱已被使用：" + email);
    }
}
```

`EmployeeSystemException.java`：

```java
package com.example.employee.exception;

public class EmployeeSystemException extends RuntimeException {

    public EmployeeSystemException(String message) {
        super(message);
    }

    public EmployeeSystemException(
            String message,
            Throwable cause) {
        super(message, cause);
    }
}
```

### 3. 完整替换EmployeeService.java

Service继续使用固定样例，查询不到或模拟邮箱冲突时抛出具体异常；它不构造HTTP响应。

```java
package com.example.employee.service;

import com.example.employee.dto.request.EmployeeCreateRequest;
import com.example.employee.dto.response.EmployeeListItemResponse;
import com.example.employee.dto.response.EmployeeResponse;
import com.example.employee.exception.DuplicateEmailException;
import com.example.employee.exception.EmployeeNotFoundException;
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

Controller保留第6章路径与输入绑定，只把成功结果包装成200；失败时方法被异常中断。

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

Advice是跨Controller的异常转换入口：Spring MVC在Controller调用链抛出匹配异常时调用对应方法，统一设置状态和安全正文。

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

## 九、本章综合验证

完成上述案例并恢复临时故障代码后，在Eclipse中从启动类运行应用。Console出现启动成功日志后，选择第6章建立的Postman `Local` 环境和 `Employee API` Collection。

### 1. 详情成功和不存在

| HTTP方法 | URL | 请求参数 | 请求体 | 预期状态 | 预期响应关键内容 |
| --- | --- | --- | --- | ---: | --- |
| GET | `{{baseUrl}}/employees/1001` | 无 | 无 | 200 | `success=true`，`data.id=1001` |
| GET | `{{baseUrl}}/employees/9999` | 无 | 无 | 404 | `success=false`，`message=员工不存在：9999`，`data=null` |

第一条响应体等价于：

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

Postman在4xx时仍会显示状态、Headers和Body，不需要为错误响应更换工具。确认404正文不包含Java异常类名或调用栈。

### 2. 空列表仍然成功

| 项目 | 内容 |
| --- | --- |
| HTTP方法 | GET |
| URL | `{{baseUrl}}/employees` |
| Params | `department=Unknown` |
| 请求体 | 无 |
| 预期状态码 | 200 |
| 预期响应 | `success=true`，`data=[]` |

原始JSON中的 `data` 是空数组，不是 `null`。状态仍为200，因为查询本身已经成功完成。

### 3. 预览成功和邮箱冲突

两个请求都使用 `POST {{baseUrl}}/employees/preview`，Body选择 **raw → JSON**，Headers确认 `Content-Type: application/json`。

普通邮箱请求体：

```json
{
  "name": "Sato",
  "department": "Development",
  "email": "sato@example.com"
}
```

预期状态200，`success=true`，`data` 为预览文本。

冲突请求体：

```json
{
  "name": "Sato",
  "department": "Development",
  "email": "used@example.com"
}
```

预期状态409，`success=false`，消息为 `邮箱已被使用：used@example.com`，正文不包含Java异常类名或调用栈。

### 4. 回归第6章请求阶段错误

| HTTP方法 | URL | Params、Headers与Body | 预期状态 | Controller或Service是否执行 |
| --- | --- | --- | ---: | --- |
| GET | `{{baseUrl}}/employees/abc` | 无Body | 400 | 不进入Service |
| DELETE | `{{baseUrl}}/employees` | 无Body | 405 | 不进入Controller方法 |
| POST | `{{baseUrl}}/employees/preview` | `Content-Type: text/plain`，raw Text为 `not-json` | 415 | 不进入Controller方法 |
| GET | `{{baseUrl}}/health` | 无Body | 200 | 正常执行HealthController |

逐条发送并保存状态码和响应Body，证明本章Advice没有把原有框架错误统一改成500。完成后在Eclipse Console中停止应用。

## 十、常见问题与日本项目Review

### 1. 常见问题

| 现象或写法 | 问题 | 修正或Review意见 |
| --- | --- | --- |
| 失败JSON返回了，但HTTP仍是200 | 只创建了 `ApiResponse.failure()` | 使用 `ResponseEntity` 同时设置正确状态 |
| 项目中创建了 `ResponseEntity.java` | 把Spring类误认为业务类 | 删除同名类，检查依赖和导入 |
| 无结果列表返回404 | 把集合查询与唯一资源查询混淆 | 返回200和空数组 |
| 204响应仍包含JSON | 204不返回正文 | 使用 `.noContent().build()` |
| Service返回 `ResponseEntity` | 业务层依赖HTTP表达 | Service返回数据或抛业务异常，由Web边界转换 |
| Controller重复相同失败正文 | 转换逻辑分散 | 交给共通异常处理器 |
| 500正文返回异常消息和调用栈 | 暴露内部实现和环境 | 对外返回稳定摘要，内部原因留给日志 |
| 捕获所有 `RuntimeException` | 业务失败、框架失败和程序缺陷被混在一起 | 只处理规格明确的异常类型 |
| 预览接口返回201 | 实际没有创建资源 | 预览返回200，真实新增后再使用201 |

### 2. Review场景与判断方向

**场景1：规格规定404，代码返回200和 `success=false`。**

判断方向：HTTP状态与规格矛盾，网关和调用方可能误判成功。应修改异常到响应的转换位置，同时确认调用方是否依赖旧行为。

**场景2：Service直接返回 `ResponseEntity`。**

判断方向：业务层依赖HTTP，批处理或定时任务难以复用。Service应返回业务数据或抛出含义明确的异常，由Controller或Advice决定HTTP表达。

**场景3：修改 `GlobalExceptionHandler` 的失败JSON结构。**

判断方向：这是共通改修（共通部品の改修），必须进行影响调查（影響調査）。检查所有可能抛出对应异常的Controller、前端解析、接口文档和外部调用方。

**场景4：把所有 `RuntimeException` 统一转换为500。**

判断方向：可能把404业务异常、400请求异常和编程错误全部覆盖成500，破坏既有规格并隐藏缺陷。应按异常责任和具体类型设计。

**场景5：前端根据message是否等于某个日文字符串决定分支。**

判断方向：消息措辞和语言可能变化，代码会变得脆弱。若前端需要稳定分支，应在API详细设计中定义业务错误码，而不是依赖显示文字。

Review时按下面顺序核对：

```text
接口场景
  → 正常系还是异常系
  → HTTP状态是否符合规格
  → 响应体是否存在
  → data是对象、数组、null还是没有正文
  → 业务错误类型能否稳定识别
  → 对外消息是否安全
  → 共通修改影响了哪些其他API
```

## 十一、综合操作练习

### 练习1：增加员工编号业务规则

改修规格：`GET /employees/0` 返回400，消息为 `员工编号必须大于0`；员工编号为正数但不存在时仍返回404。

限制：Controller不能直接判断编号；在Service中判断，新建 `InvalidEmployeeIdException`，再由全局异常处理器映射为400。验证编号0、1001和9999，并保存状态码和正文。

完成后删除该异常类和处理方法，恢复本章稳定状态。第8章会完整替换全局异常处理器，不能让练习代码成为隐藏前置条件。

### 练习2：Review系统故障响应的边界

案例7-04的500实验已经完成并恢复，此处不再重复制造故障。阅读现有 `GlobalExceptionHandler` 和案例7-04的Postman证据，写出Review意见：指定的 `@ExceptionHandler(EmployeeSystemException.class)` 能匹配哪些异常？若Service抛出其他未匹配的 `RuntimeException`，是否一定得到当前统一JSON，为什么？为什么不能把 `exception.getMessage()` 直接返回给客户端？实验结束后为何必须删除编号5000的临时分支？

提交一段简短结论，并指出若正式项目需要统一处理未知异常，还须考虑服务端日志、敏感信息保护和框架默认异常处理；复杂兜底机制可阅读本章附录，不在本练习中新增处理器。

### 练习3：完成状态码影响调查

收到改修要求：

> 重复邮箱不再返回409，改为HTTP 200并在message中写失败。

先不修改代码。提交Review意见，至少包含：

1. 与当前API详细设计冲突的位置；
2. 对调用方成功判断的影响；
3. Controller、Advice和前端的影响范围；
4. 继续使用409或正式变更规格的建议。

### 练习4：整理自测证据

为以下场景记录API ID、请求、预期状态、实际状态、响应关键字段和判定：

1. 详情成功；
2. 详情不存在；
3. 有数据列表；
4. 空列表；
5. 预览成功；
6. 邮箱冲突；
7. 第6章的400、405、415回归；
8. 健康检查回归。

这里只整理HTTP接口验证证据，不引入JUnit、Mockito或MockMvc。只写“测试通过”不能说明实际验证了哪个契约。

## 十二、本章稳定状态

完成练习并恢复临时代码后，工程应保持：

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

1. 说明HTTP状态、响应头和响应体的职责；
2. 比较常见2xx、4xx和5xx状态并按规格选择；
3. 使用 `ResponseEntity` 返回正文、响应头或无正文响应；
4. 区分 `ResponseEntity`、`HttpStatus` 和 `ApiResponse<T>`；
5. 区分空数组、`data: null` 和没有响应体；
6. 在Service中抛出含义明确的业务异常；
7. 区分Spring MVC框架异常、业务异常和程序错误；
8. 说明 `@ExceptionHandler` 如何把Service异常转换为HTTP响应；
9. 区分HTTP状态、项目响应正文和显示消息；
10. 根据日本项目API详细设计核对正常系、异常系和共通影响范围。

下一章会在当前响应体系上加入Jakarta Validation，区分JSON能否读取、字段是否合格和Service业务规则是否允许，并把校验失败转换为稳定的400响应。

## 十三、附录：其他响应与异常处理方式

以下内容帮助阅读既有项目，不是本章员工接口的实现前提。

### A. 其他HTTP状态与ResponseEntity写法

#### 1. HTTP状态码的五个类别

| 范围 | 类别 | 基本含义 | 本课程中的处理 |
| ---: | --- | --- | --- |
| 1xx | 信息响应 | 请求仍在处理，返回阶段性信息 | 了解，不在员工接口中手动返回 |
| 2xx | 成功 | 请求已经被成功接收、处理或接受 | 本章重点 |
| 3xx | 重定向 | 客户端应使用其他位置，或可以使用缓存结果 | 了解，不提前实现页面跳转和缓存 |
| 4xx | 客户端请求问题 | 请求格式、身份、权限、目标资源或当前状态不满足要求 | 本章重点 |
| 5xx | 服务端或上游处理问题 | 服务端无法完成本来有效的请求 | 本章重点 |

状态码由接口规格和实际处理结果共同决定，不能按“GET一定200”“POST一定201”机械选择。

#### 2. 项目中常见的状态码

| 状态码 | 名称 | 典型业务场景 | 本章是否实现 |
| ---: | --- | --- | --- |
| 200 | OK | 查询、列表或预览成功 | 是 |
| 201 | Created | 新资源已经创建 | 只讲独立示例，第10章实现 |
| 202 | Accepted | 请求已接受，但异步处理尚未完成 | 了解 |
| 204 | No Content | 操作成功，规格规定不返回正文 | 只讲独立示例，第10章实现 |
| 301 | Moved Permanently | 资源被永久移动到新URL | 了解 |
| 302 | Found | 临时引导客户端访问其他URL | 了解 |
| 304 | Not Modified | 缓存内容仍有效，不返回新的表示内容 | 了解 |
| 400 | Bad Request | 请求语法、参数类型或字段不符合接口要求 | 第6章已观察；第8章扩展字段校验 |
| 401 | Unauthorized | 缺少有效登录身份；名称容易误解，重点是“尚未认证” | 第16章实现认证授权 |
| 403 | Forbidden | 身份已确认，但没有执行当前操作的权限 | 第16章实现认证授权 |
| 404 | Not Found | 指定的员工资源不存在 | 是 |
| 405 | Method Not Allowed | URL存在，但当前HTTP方法不被允许 | 第6章已观察 |
| 409 | Conflict | 请求与资源当前状态冲突，例如邮箱已被占用 | 是 |
| 415 | Unsupported Media Type | 请求体媒体类型不受支持，例如接口要求JSON却发送文本 | 第6章已观察 |
| 422 | Unprocessable Content | 内容类型和语法可读取，但其中指令在语义上无法处理 | 了解；是否用于字段校验由项目规格决定 |
| 500 | Internal Server Error | 应用内部发生未能正常完成请求的故障 | 通过受控练习验证 |
| 502 | Bad Gateway | 网关从上游服务得到无效响应 | 了解，通常由网关产生 |
| 503 | Service Unavailable | 服务暂时不可用，例如维护或过载 | 了解 |
| 504 | Gateway Timeout | 网关等待上游服务超时 | 了解，通常由网关产生 |

状态码语义可对照[RFC 9110 HTTP Semantics](https://www.rfc-editor.org/rfc/rfc9110.html)。不同项目可能对400与422等边界采用不同约定，进入既有项目后必须以API规格为准。

#### 3. 容易混淆的状态码

| 比较 | 判断重点 |
| --- | --- |
| 200与201 | 200表示请求成功；201表示资源确实已经创建，通常还应考虑返回新资源位置 |
| 200与204 | 两者都成功；200可以有响应体，204不返回响应体 |
| 400与422 | 400常用于无法正确理解或不符合输入要求；422强调内容可读取但语义无法处理，项目必须统一约定 |
| 401与403 | 401重点是没有有效身份；403重点是已有身份但权限不足 |
| 400与409 | 400关注请求本身；409关注请求与系统当前状态冲突 |
| 404与405 | 404表示目标资源或路由不存在；405表示路由存在但请求方法不允许 |
| 500与503 | 500表示应用内部故障；503表示服务当前暂时无法提供能力，之后可能恢复 |

不能把所有业务失败都返回400，也不能把所有基础设施问题都简单归为500。状态码选择需要结合责任边界、网关行为和项目规格。

#### 4. 什么是响应构建器

下面的代码不是一次直接创建最终对象：

```java
ResponseEntity
        .status(HttpStatus.NOT_FOUND)
        .header("X-Error-Source", "employee-api")
        .body(ApiResponse.failure("员工不存在"));
```

`status(...)` 先返回一个响应构建器；`header(...)` 在同一个构建器上追加响应头并再次返回构建器；`body(...)` 设置正文并生成最终 `ResponseEntity`。前一个方法返回的对象又提供下一个方法，所以可以连续调用，这种写法称为构建器式调用或链式调用。

#### 5. ResponseEntity常用方法

| 方法 | 当前示例参数 | 可接受的值 | 返回结果或作用 |
| --- | --- | --- | --- |
| `ok(body)` | `ApiResponse.success(data)` | 任意符合目标泛型的响应体 | 直接生成HTTP 200和正文 |
| `ok()` | 无 | 无参数 | 返回状态为200的 `BodyBuilder` |
| `status(status)` | `HttpStatus.NOT_FOUND` | `HttpStatusCode`或100～999的整数 | 返回指定状态的 `BodyBuilder` |
| `badRequest()` | 无 | 无参数 | 返回状态为400的 `BodyBuilder` |
| `notFound()` | 无 | 无参数 | 返回状态为404且只能继续设置响应头或 `build()` 的构建器 |
| `created(uri)` | `URI.create("/employees/1001")` | 非空、语法正确的 `URI` | 返回状态为201的构建器，并设置 `Location` 响应头 |
| `noContent()` | 无 | 无参数 | 返回状态为204且不能设置正文的构建器 |
| `header(name, values...)` | 头名称和一个或多个字符串值 | 合法响应头名称和字符串值 | 添加自定义响应头并返回构建器 |
| `body(body)` | 与目标泛型一致的对象 | 可以被消息转换器写入响应的对象 | 设置正文并完成构建 |
| `build()` | 无 | 无参数 | 不提供正文，完成构建 |

Spring Framework 6.2中的方法签名可参考[ResponseEntity官方API](https://docs.spring.io/spring-framework/docs/6.2.x/javadoc-api/org/springframework/http/ResponseEntity.html)。

#### 6. 独立用法示例

这些片段用于理解API，不表示本章要提前增加新增或删除接口。

200和响应体：

```java
return ResponseEntity.ok(ApiResponse.success(employee));
```

相同的200也可以使用构建器：

```java
return ResponseEntity
        .ok()
        .body(ApiResponse.success(employee));
```

400和失败正文：

```java
return ResponseEntity
        .badRequest()
        .body(ApiResponse.failure("请求参数错误"));
```

404但没有正文：

```java
return ResponseEntity.notFound().build();
```

当前员工接口的404需要统一JSON正文，因此使用 `status(...).body(...)`，不能使用只能 `build()` 的 `notFound()`：

```java
return ResponseEntity
        .status(HttpStatus.NOT_FOUND)
        .body(ApiResponse.failure("员工不存在"));
```

201、响应体和新资源位置：

```java
URI location = URI.create("/employees/1001");
return ResponseEntity
        .created(location)
        .body(ApiResponse.success(employee));
```

`URI` 是 `java.net.URI` 类，用来表示资源标识符；`URI.create(...)` 把字符串转换成 `URI` 对象。`Location` 告诉调用方新资源可以从哪里访问。不是所有201接口都必须采用完全相同的响应体，但是否返回 `Location` 应在接口规格中明确。

204没有响应体：

```java
return ResponseEntity.noContent().build();
```

204的语义是不返回正文，因此用 `build()`，不能再调用 `body()` 附加JSON。

设置自定义响应头：

```java
return ResponseEntity
        .ok()
        .header("X-Request-Id", "sample-request-id")
        .body(ApiResponse.success(employee));
```

响应头名称和值必须符合HTTP和项目安全要求，不能把密码、Token或内部堆栈写入响应头。

#### 7. 并不是所有接口都必须使用ResponseEntity

只需要固定200和响应体时，Controller可以直接返回业务对象；需要控制状态、响应头或无正文响应时，`ResponseEntity` 更清楚。是否统一使用它由项目编码规范决定，不是REST API的强制要求。

### B. 多Advice、匹配优先级与进阶Review

#### 1. 同一Advice中的类型匹配

假设同一个Advice存在两个方法：

```java
@ExceptionHandler(EmployeeNotFoundException.class)
public ResponseEntity<ApiResponse<Void>> handleEmployeeNotFound(
        EmployeeNotFoundException exception) {
    // 404
}

@ExceptionHandler(RuntimeException.class)
public ResponseEntity<ApiResponse<Void>> handleRuntime(
        RuntimeException exception) {
    // 500
}
```

`EmployeeNotFoundException` 继承 `RuntimeException`，两个声明从类型上都能接收它。在同一Advice中，Spring会选择更具体、更接近实际异常类型的 `EmployeeNotFoundException` 处理方法，而不是宽泛的 `RuntimeException` 方法。

这也是不建议随意增加 `@ExceptionHandler(Exception.class)` 或 `RuntimeException.class` 的原因：宽泛处理器会扩大影响范围，并可能把框架异常或编程错误改成不符合规格的统一响应。

#### 2. 多个Advice、Order与原因链

项目有多个 `@RestControllerAdvice` 时，Spring按 `Ordered`、`@Order` 或 `@Priority` 确定Advice顺序；数值越小，优先级越高。Spring会使用第一个找到匹配处理方法的Advice。不要在多个同优先级Advice中重复声明相同异常，否则维护人员很难判断实际处理位置。

`@ExceptionHandler` 不只可能匹配最外层异常，也可能匹配包装异常的cause。在同一个Advice中，直接抛出的根异常匹配通常优先于只匹配内部cause；但更高优先级Advice中的cause匹配，可能先于较低优先级Advice中的根异常匹配。因此不能把复杂规则简化成“所有情况下子类处理器一定优先”。

初学阶段应掌握三条规则：

1. 一个项目尽量为每类核心异常确定唯一、清楚的处理位置；
2. 处理方法参数尽量声明具体异常类型；
3. 多Advice时必须检查优先级和包装异常，不凭类名猜测。

匹配规则可对照[Spring `@ControllerAdvice` API](https://docs.spring.io/spring-framework/docs/6.2.x/javadoc-api/org/springframework/web/bind/annotation/ControllerAdvice.html)。

#### 3. 注解属性怎样识读

以下内容用于阅读既有项目，不要求本章主线全部使用。

`@RestControllerAdvice` 可以缩小生效范围：

| 属性 | 可接受的值 | 默认值或作用 |
| --- | --- | --- |
| `name` | Spring Bean名称字符串 | 默认空字符串，通常省略 |
| `value` / `basePackages` | 一个或多个包名字符串 | 互为别名；默认空数组，不按包筛选 |
| `basePackageClasses` | 一个或多个类的 `Class` 对象 | 使用这些类所在包作为筛选范围 |
| `assignableTypes` | 一个或多个Controller类型 | 只应用于可赋值给这些类型的Controller |
| `annotations` | 一个或多个注解类型 | 只应用于带指定注解的Controller |

完整识读示例：

```java
@RestControllerAdvice(
        name = "employeeAdvice",
        basePackages = "com.example.employee.controller",
        basePackageClasses = EmployeeController.class,
        assignableTypes = EmployeeController.class,
        annotations = RestController.class)
```

多个非空选择条件之间按“或”匹配。实际项目通常选择一种稳定方式，不会为了展示属性把所有条件堆在一起。

Spring Framework 6.2中的 `@ExceptionHandler` 属性包括：

| 属性 | 可接受的值 | 默认值或作用 |
| --- | --- | --- |
| `value` / `exception` | 一个或多个异常类型 | 互为别名；未填写时可从异常参数推断 |
| `produces` | 一个或多个媒体类型字符串 | 默认空数组，不增加响应媒体类型条件 |

完整识读示例：

```java
@ExceptionHandler(
        exception = EmployeeNotFoundException.class,
        produces = "application/json")
```

`produces` 与 `exception` 是Spring Framework 6.2提供的属性。主线使用更简单的 `@ExceptionHandler(EmployeeNotFoundException.class)`。

多个Advice需要明确顺序时，可以使用：

```java
@RestControllerAdvice
@Order(value = Ordered.HIGHEST_PRECEDENCE)
public class PrimaryExceptionHandler {
}
```

本章只有一个Advice，不添加无意义的 `@Order`。

#### 4. Review异常处理器冲突

假设项目新增第二个高优先级Advice，并声明 `@ExceptionHandler(RuntimeException.class)`。说明它可能对本章三个处理方法产生什么影响，需要核对哪些 `@Order`、根异常和cause关系。不需要把示例代码加入主线工程。

### C. 异常原因链

系统异常经常需要包装底层异常，同时保留原因链：

```java
public EmployeeSystemException(
        String message,
        Throwable cause) {
    super(message, cause);
}
```

- `Throwable cause` 是导致当前异常的原始异常。
- `getMessage()` 返回当前异常的摘要消息。
- `getCause()` 返回被包装的原始异常对象。

捕获底层异常后无理由地丢弃cause，会让后续调查失去原始类型和调用位置。本章只建立异常链概念；第11章再说明怎样把内部原因安全地写入服务端日志。

### D. 业务错误码与Problem Details

HTTP状态码只能表达通用结果类别。大型项目还可能在错误响应中增加稳定的业务错误码：

```json
{
  "success": false,
  "code": "EMPLOYEE_NOT_FOUND",
  "message": "员工不存在",
  "data": null
}
```

| 内容 | 面向对象 | 作用 |
| --- | --- | --- |
| HTTP 404 | HTTP客户端、网关、监控 | 表示资源不存在这一通用语义 |
| `EMPLOYEE_NOT_FOUND` | 前后端业务代码 | 稳定识别具体业务错误类型 |
| `message` | 人或画面显示 | 提供允许公开的说明文字 |

前端不应通过“message是否等于某段中文或日文”决定业务分支。消息可能因措辞、翻译或国际化而变化；稳定的业务错误码更适合程序判断。

本章完整代码继续保留 `success`、`message`、`data` 三字段，不强制增加 `code`，因为响应结构必须由项目接口规格决定。如果项目采用业务错误码，API详细设计书至少需要规定：错误码、对应HTTP状态、发生条件、对外消息以及各调用方的处理方式。

`success` 与HTTP状态有信息重叠。既有接口已经采用它时应保持一致；新项目是否保留，需要在接口规范阶段决定。

另一种错误响应设计是[RFC 9457 Problem Details](https://www.rfc-editor.org/rfc/rfc9457.html)，Spring Framework提供 `ProblemDetail` 支持。本章只认识这种替代方案，不同时引入第二套完整实现。
