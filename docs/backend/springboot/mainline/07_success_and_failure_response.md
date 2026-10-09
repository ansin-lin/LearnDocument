# 第7章 成功与失败怎样返回

> 本章目标：从HTTP响应的组成出发，根据接口规格选择状态码，使用 `ResponseEntity` 和 `ApiResponse<T>` 表达成功与失败，并把Service抛出的业务异常统一转换为安全、稳定的HTTP响应。

第6章已经能把HTTP输入传给Java方法，也能把Java对象转换为JSON。但“能够返回JSON”不等于“响应设计正确”。例如员工不存在时，正文写了失败却仍返回200，调用方就无法只根据HTTP结果可靠判断请求是否成功。

另一个反例是把数据库异常的 `getMessage()` 直接交给客户端：消息可能包含表名、SQL或服务器路径。业务上的“找不到员工”应有可公开的404说明；系统故障应返回500和通用说明，内部原因留在服务端调查。若每个Controller都写一遍 `try { ... } catch (...) { ... }`，状态码和消息还容易出现差异。本章因此把成功响应留在Controller，把共通失败转换交给Advice。

本章按照下面的顺序建立完整响应链：

```text
先理解HTTP响应
  → 选择符合规格的状态码
  → 使用ResponseEntity控制HTTP响应
  → 使用ApiResponse组织JSON正文
  → 用异常表达业务失败
  → 用全局异常处理器转换失败响应
  → 完成代码、运行验证和Review
```

本章不接入数据库，不实现认证授权，不展开字段校验，也不讲日志配置和自动化测试。字段校验在第8章完成，数据库在第9章接入，日志和故障调查在第11章处理。

## 一、开始状态与本章接口规格

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

本章保持第6章的URL、请求参数和请求体不变，只改变成功与失败的表达方式：

| 规格编号 | 请求 | 场景 | HTTP状态 | 响应体 |
| --- | --- | --- | ---: | --- |
| `EMP-API-02` | `GET /employees/{id}` | 编号为1001 | 200 | 成功结构和员工详情 |
| `EMP-API-02` | `GET /employees/{id}` | 其他可转换为Long的编号 | 404 | 失败结构和“员工不存在”消息 |
| `EMP-API-03` | `GET /employees` | 部门为Sales | 200 | 成功结构和员工数组 |
| `EMP-API-03` | `GET /employees?department=Unknown` | 无匹配数据 | 200 | 成功结构和空数组 |
| `EMP-PREVIEW-01` | `POST /employees/preview` | 普通邮箱 | 200 | 成功结构和预览文本 |
| `EMP-PREVIEW-01` | `POST /employees/preview` | `used@example.com` | 409 | 失败结构和邮箱冲突消息 |

当前Service仍使用固定样例数据。预览接口只检查请求内容并模拟邮箱冲突，不保存员工；真实新增、201响应和数据库唯一约束在后续章节实现。

## 二、HTTP响应由什么组成

一次HTTP响应至少包含状态、响应头和可选的响应体：

```text
HTTP/1.1 404 Not Found                  ← 状态行
Content-Type: application/json          ← 响应头

{"success":false,"message":"员工不存在：9999","data":null}  ← 响应体
```

- **状态码**供浏览器、前端、网关和监控工具先判断结果类别。
- **响应头**携带媒体类型、缓存策略、资源位置等附加信息。
- **响应体**携带接口规格允许返回的业务数据或错误说明。

下面两种响应并不等价：

```text
HTTP 200 + {"success":false,...}  ← HTTP层仍然表示成功
HTTP 404 + {"success":false,...}  ← HTTP状态与业务结果一致
```

因此，JSON中的 `success` 不能替代HTTP状态码，HTTP状态码也不能代替具体业务数据。

### 1. HTTP状态码的五个类别

| 范围 | 类别 | 基本含义 | 本课程中的处理 |
| ---: | --- | --- | --- |
| 1xx | 信息响应 | 请求仍在处理，返回阶段性信息 | 了解，不在员工接口中手动返回 |
| 2xx | 成功 | 请求已经被成功接收、处理或接受 | 本章重点 |
| 3xx | 重定向 | 客户端应使用其他位置，或可以使用缓存结果 | 了解，不提前实现页面跳转和缓存 |
| 4xx | 客户端请求问题 | 请求格式、身份、权限、目标资源或当前状态不满足要求 | 本章重点 |
| 5xx | 服务端或上游处理问题 | 服务端无法完成本来有效的请求 | 本章重点 |

状态码由接口规格和实际处理结果共同决定，不能按“GET一定200”“POST一定201”机械选择。

### 2. 项目中常见的状态码

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

### 3. 容易混淆的状态码

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

## 三、Spring Boot怎样返回HTTP响应

在 `@RestController` 中直接返回一个Java对象时，Spring MVC会通过消息转换器把对象写入响应体，成功时通常使用200：

```java
@GetMapping("/sample")
public EmployeeResponse sample() {
    return new EmployeeResponse(
            1001L,
            "Tanaka",
            "Sales",
            "tanaka@example.com");
}
```

这种写法适合“固定返回200和正文”的简单接口。如果要根据处理结果改变状态码、响应头或是否返回正文，就需要能表达完整HTTP响应的对象。

### 1. ResponseEntity是什么

`ResponseEntity<T>` 的完整名称是 `org.springframework.http.ResponseEntity`，由Spring Web提供。它表示整个HTTP响应：

```text
ResponseEntity<T>
├── HttpStatusCode  HTTP状态
├── HttpHeaders     响应头
└── T               响应体类型
```

`T` 是响应体的Java类型。例如：

```text
ResponseEntity<EmployeeResponse>              → 正文是员工详情
ResponseEntity<ApiResponse<EmployeeResponse>> → 正文是统一包装后的员工详情
ResponseEntity<Void>                          → 没有正文
```

使用泛型后，编译器可以检查 `.body(...)` 传入的对象是否符合Controller声明的响应体类型。

### 2. 什么是响应构建器

下面的代码不是一次直接创建最终对象：

```java
ResponseEntity
        .status(HttpStatus.NOT_FOUND)
        .header("X-Error-Source", "employee-api")
        .body(ApiResponse.failure("员工不存在"));
```

`status(...)` 先返回一个响应构建器；`header(...)` 在同一个构建器上追加响应头并再次返回构建器；`body(...)` 设置正文并生成最终 `ResponseEntity`。前一个方法返回的对象又提供下一个方法，所以可以连续调用，这种写法称为构建器式调用或链式调用。

### 3. ResponseEntity常用方法

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

### 4. 六个独立用法示例

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

### 5. 并不是所有接口都必须使用ResponseEntity

只需要固定200和响应体时，Controller可以直接返回业务对象；需要控制状态、响应头或无正文响应时，`ResponseEntity` 更清楚。是否统一使用它由项目编码规范决定，不是REST API的强制要求。

## 四、HttpStatus是HTTP状态码枚举

`HttpStatus` 的完整名称是 `org.springframework.http.HttpStatus`，是Spring Framework提供的枚举。Java枚举语法已经在[Java第17章](../../java/17_strings_and_enum.md)讲解，这里只学习框架枚举怎样表达HTTP状态。

```java
HttpStatus.NOT_FOUND          // 枚举常量，代码中表达“资源不存在”
HttpStatus.NOT_FOUND.value()  // 返回整数404
```

使用 `HttpStatus.NOT_FOUND` 比直接写404更容易阅读，也能让编译器检查类型。`ResponseEntity.status(HttpStatusCode status)` 接收 `HttpStatusCode` 接口；`HttpStatus` 实现了该接口，所以可以直接作为参数：

```text
HttpStatus.NOT_FOUND
  → ResponseEntity保存HttpStatusCode
  → Spring MVC写入HTTP状态行
  → 客户端收到404 Not Found
```

必须区分三种容易混淆的状态：

| 表达 | 所属层次 | 回答的问题 | 示例 |
| --- | --- | --- | --- |
| `HttpStatus.NOT_FOUND` | HTTP协议 | 本次HTTP请求怎样结束 | 404 |
| `ApiResponse` 的 `success` 字段 | 项目JSON格式 | 业务响应体表示成功还是失败 | `false` |
| 员工业务状态 | 业务数据 | 员工当前是否在职或停用 | 第9章随数据库字段讲解 |

## 五、ApiResponse统一什么

本项目使用下面的基础JSON结构：

```json
{
  "success": true,
  "message": "OK",
  "data": {}
}
```

`ApiResponse<T>` 是项目自己创建的响应体类，不是Spring提供的类型，也不是HTTP标准强制格式。

| 字段 | 类型 | 成功时 | 失败时 |
| --- | --- | --- | --- |
| `success` | `boolean` | `true` | `false` |
| `message` | `String` | `"OK"` | 稳定、允许公开的失败摘要 |
| `data` | `T` | 业务数据 | 当前基础实现为 `null` |

`ResponseEntity` 与 `ApiResponse` 的职责不同：

```text
ResponseEntity<ApiResponse<EmployeeResponse>>
│              │           └─ 员工详情数据
│              └─ 项目约定的JSON响应体
└─ HTTP状态、响应头和响应体
```

### 1. 类级泛型和方法级泛型

类名中的 `<T>` 是类级泛型参数：

```java
public class ApiResponse<T> {
    private final T data;
}
```

创建不同类型的响应时，`T` 被具体类型替代：

```text
ApiResponse<EmployeeResponse>               → data是员工详情
ApiResponse<List<EmployeeListItemResponse>> → data是员工列表
ApiResponse<String>                         → data是预览文本
ApiResponse<Void>                           → data没有业务值
```

静态方法不能直接使用某个对象的类级 `T`，所以静态创建方法要声明自己的方法级泛型：

```java
public static <T> ApiResponse<T> success(T data)
```

方法名前的 `<T>` 声明“这一次方法调用使用的类型”。传入 `EmployeeResponse` 时，编译器可以从参数推断结果是 `ApiResponse<EmployeeResponse>`。

失败方法没有类型为 `T` 的参数：

```java
public static <T> ApiResponse<T> failure(String message)
```

这时编译器可以从赋值位置或返回类型推断 `T`：

```java
ApiResponse<Void> body = ApiResponse.failure("员工不存在");
```

`Void` 是 `java.lang.Void` 引用类型，可以作为泛型参数；`void` 是方法“不返回值”的关键字，不能写成 `ApiResponse<void>`。当前失败响应没有业务数据，因此使用 `ApiResponse<Void>`，实际 `data` 为 `null`。

不同泛型类型不能随意混用，例如 `ApiResponse<EmployeeResponse>` 不能当成 `ApiResponse<String>` 返回。泛型检查帮助Controller的声明、正文和JSON用途保持一致。

### 2. success字段是否与HTTP状态重复

两者有部分信息重叠，但服务对象不同：HTTP状态供通用HTTP组件判断，`success` 是项目自定义JSON字段。是否同时保留应由接口规范决定；不能在代码中擅自删除，也不能让两者出现200与 `success=false` 这类矛盾组合。

## 六、成功、失败、空列表和无响应体

| 场景 | HTTP状态 | `data`或响应体 | 判断 |
| --- | ---: | --- | --- |
| 查询到员工 | 200 | 员工对象 | 成功 |
| 列表没有匹配项 | 200 | `[]` | 查询成功，结果数量为0 |
| 指定编号不存在 | 404 | `data: null` | 目标资源不存在 |
| 请求与当前状态冲突 | 409 | `data: null` | 业务冲突 |
| 创建资源成功 | 201 | 通常返回新资源，并考虑 `Location` | 成功 |
| 删除成功且规格无正文 | 204 | 整个响应体不存在 | 成功 |

空数组、JSON中的 `null` 和没有响应体是三个不同结果：

```text
[]              可以遍历，元素数量为0
{"data":null}  有JSON正文，但data没有业务值
HTTP 204        没有响应体
```

## 七、Java异常怎样表达失败

Service正常完成时返回业务数据；发现无法继续的业务情况时，可以抛出含义明确的异常：

```java
if (!id.equals(1001L)) {
    throw new EmployeeNotFoundException(id);
}
```

`EmployeeNotFoundException extends RuntimeException` 表示它是非受检异常。构造方法中的 `super(...)` 把消息交给父类保存。执行 `throw` 后：

1. 当前Service方法立即中断；
2. 后面的 `return` 不再执行；
3. 调用它的Controller也不会继续组织200响应；
4. 异常沿调用链交给Spring MVC处理。

不要用同一个 `null` 同时表示不存在、冲突和系统故障。三者需要不同HTTP状态和处理策略，混成 `null` 后，Controller无法可靠判断原因。

### 1. 业务异常与系统异常

| 类型 | 含义 | 示例 | 对外策略 |
| --- | --- | --- | --- |
| 业务异常 | 项目已经预期、可以按规格解释的失败 | 员工不存在、邮箱重复 | 返回规定状态和安全业务消息 |
| 系统异常 | 程序或依赖无法正常完成处理 | 数据库失败、外部API失败、文件错误、网络错误、程序缺陷 | 返回通用消息，内部原因留给后续日志调查 |

不能把所有 `RuntimeException` 都转换成业务异常。`NullPointerException` 等编程错误不代表“员工不存在”，错误转换会掩盖真正缺陷。

### 2. 使用cause保留原始异常

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

## 八、Spring MVC框架异常与业务异常

异常不一定从Service产生。请求进入Controller之前，Spring MVC已经进行路由选择、路径参数转换、媒体类型判断和JSON读取。

| 异常 | 常见发生阶段 | 典型场景 | 本章处理方式 |
| --- | --- | --- | --- |
| `MethodArgumentTypeMismatchException` | Controller调用前 | `/employees/abc` 不能转换成 `Long` | 保留Spring MVC默认400处理 |
| `HttpMessageNotReadableException` | Controller调用前 | JSON语法错误或字段类型无法读取 | 本章只认识；第8章统一处理 |
| `MethodArgumentNotValidException` | Controller调用前 | DTO字段违反Validation约束 | 第8章加入校验后处理 |
| `HttpRequestMethodNotSupportedException` | 路由阶段 | 对只支持GET的URL发送DELETE | 保留默认405处理 |
| `HttpMediaTypeNotSupportedException` | 请求体读取前 | JSON接口收到 `text/plain` | 保留默认415处理 |
| `EmployeeNotFoundException` | Service执行中 | 指定员工不存在 | 本章转换为404 |
| `DuplicateEmailException` | Service执行中 | 邮箱与当前系统状态冲突 | 本章转换为409 |
| `NullPointerException` | 任意Java代码执行阶段 | 对 `null` 调用实例方法 | 编程错误，不伪装成业务失败 |

`DispatcherServlet` 是Spring MVC请求处理的中央入口。它寻找Controller方法、安排参数解析、调用Controller，并在发生异常时把异常交给异常解析机制。这里是简化说明，不展开Spring MVC源码。

创建 `GlobalExceptionHandler` 不会让所有异常自动变成 `ApiResponse`。只有匹配到其中某个 `@ExceptionHandler` 的异常，才会进入对应方法；没有匹配时，Spring MVC会继续交给其他异常解析器或Spring Boot错误处理机制，默认正文不一定符合本项目的三字段结构。

全局异常处理也不等于需要手动 `try-catch` 所有异常。能由框架默认正确转换的405、415等异常可以先保留默认处理；项目确实要求统一结构时，再根据影响范围逐类设计并回归。

## 九、全局异常处理机制

如果每个Controller都复制 `try-catch`，状态码和错误正文容易不一致。`@RestControllerAdvice` 让多个Controller共用“异常类型 → HTTP响应”的转换逻辑。

```text
Service抛出EmployeeNotFoundException
  → Controller正常返回语句不再执行
  → DispatcherServlet进入异常解析流程
  → 找到匹配的@ExceptionHandler方法
  → 处理方法返回404和ApiResponse失败正文
  → Spring MVC生成最终HTTP响应
```

### 1. 两个核心注解

`@RestControllerAdvice` 写在类上，是 `@ControllerAdvice` 与 `@ResponseBody` 的组合：

| 组成 | 作用 |
| --- | --- |
| `@ControllerAdvice` | 声明跨Controller共用的异常处理、绑定或模型处理Bean |
| `@ResponseBody` | 把方法返回对象写入响应体，而不是解释为HTML页面名称 |

`@ExceptionHandler(EmployeeNotFoundException.class)` 写在方法上，声明该方法处理的异常类型。括号中的 `.class` 是异常类型对应的 `Class` 对象，不是在创建异常。

Controller类内部也可以声明局部 `@ExceptionHandler`，它只服务该Controller；`@RestControllerAdvice` 中的是跨Controller处理。Spring MVC通常先检查当前Controller的局部处理方法，再使用全局Advice。共通响应规则优先放在Advice，确有Controller专属语义时才使用局部处理。

### 2. 同一Advice中的类型匹配

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

### 3. 扩展阅读：多个Advice、Order与原因链

项目有多个 `@RestControllerAdvice` 时，Spring按 `Ordered`、`@Order` 或 `@Priority` 确定Advice顺序；数值越小，优先级越高。Spring会使用第一个找到匹配处理方法的Advice。不要在多个同优先级Advice中重复声明相同异常，否则维护人员很难判断实际处理位置。

`@ExceptionHandler` 不只可能匹配最外层异常，也可能匹配包装异常的cause。在同一个Advice中，直接抛出的根异常匹配通常优先于只匹配内部cause；但更高优先级Advice中的cause匹配，可能先于较低优先级Advice中的根异常匹配。因此不能把复杂规则简化成“所有情况下子类处理器一定优先”。

初学阶段应掌握三条规则：

1. 一个项目尽量为每类核心异常确定唯一、清楚的处理位置；
2. 处理方法参数尽量声明具体异常类型；
3. 多Advice时必须检查优先级和包装异常，不凭类名猜测。

匹配规则可对照[Spring `@ControllerAdvice` API](https://docs.spring.io/spring-framework/docs/6.2.x/javadoc-api/org/springframework/web/bind/annotation/ControllerAdvice.html)。

### 4. 扩展：注解属性怎样识读

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

## 十、业务错误码、message与安全边界

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

### 系统异常不能直接公开内部信息

数据库错误、外部API地址、文件路径、服务器信息和调用栈不应直接返回客户端。系统异常处理器可以接收完整异常对象，但本章只对外返回：

```json
{
  "success": false,
  "message": "服务器内部错误",
  "data": null
}
```

内部原因由异常链保留，第11章再进入日志和故障调查。

## 十一、从日本项目API详细设计书进入代码

日本项目常用“正常系（せいじょうけい）”表示预期成功路径，用“異常系（いじょうけい）”表示业务拒绝、输入错误或系统故障路径。详细设计书必须把每种场景映射到状态码和响应体，开发人员不能自行改变。

下面是本章接口的简化设计表。`業務エラーコード` 列同时展示未来扩展候选；当前三字段代码尚未返回 `code`，因此不能声称已经实现这些错误码。

| API ID | HTTPメソッド | エンドポイント | 処理区分 | HTTPステータス | 業務エラーコード | レスポンスメッセージ | レスポンスデータ |
| --- | --- | --- | --- | ---: | --- | --- | --- |
| `EMP-API-02` | GET | `/employees/{id}` | 正常系：员工存在 | 200 | `-` | `OK` | 员工详情 |
| `EMP-API-02` | GET | `/employees/{id}` | 異常系：员工不存在 | 404 | 候选 `EMPLOYEE_NOT_FOUND` | `员工不存在：{id}` | `null` |
| `EMP-API-03` | GET | `/employees` | 正常系：查询结果为空 | 200 | `-` | `OK` | 空数组 `[]` |
| `EMP-PREVIEW-01` | POST | `/employees/preview` | 異常系：邮箱重复 | 409 | 候选 `DUPLICATE_EMAIL` | `邮箱已被使用：{email}` | `null` |
| 共通 | - | - | 異常系：系统故障 | 500 | 候选 `SYSTEM_ERROR` | `服务器内部错误` | `null` |

从设计书进入代码时按下面顺序确认：

1. 根据エンドポイント和HTTPメソッド找到Controller方法；
2. 根据処理区分确认正常系与异常系条件；
3. 在Service中实现业务判断，不把HTTP状态写进业务层；
4. 在Controller或共通异常处理器中实现HTTPステータス；
5. 根据レスポンスメッセージ和レスポンスデータ组装响应体；
6. 如果设计书采用業務エラーコード，再同步修改响应类和所有相关API。

修改共通异常响应结构时，影响范围不只是一条员工接口。必须调查哪些Controller会抛出相同异常、哪些前端读取当前JSON字段、接口文档和调用方是否需要同步。不能因为某个页面方便，就擅自修改既有状态码或字段。

## 十二、完整项目代码

完成前面概念后，再集中修改工程。本章新建或替换：

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

文件位置：

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

私有构造方法阻止调用方随意组合三个字段；两个静态创建方法固定成功与失败的基本形状。Jackson通过getter生成 `success`、`message` 和 `data`。

### 2. 新建三个异常类

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

第二个构造方法用于实际项目包装底层故障并保留cause。本章受控故障练习仍可使用只有message的构造方法。

### 3. 完整替换EmployeeService.java

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

`SAMPLE_EMPLOYEES` 是不可修改的固定样例集合。列表查询从固定数据筛选，不根据请求参数伪造员工部门。它只替代数据库用于本章响应教学；第9章会用MyBatis查询真实数据。

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

Controller只保留成功路线：调用Service、取得数据、返回200。Service抛出异常后，当前方法会中断，不会继续执行成功的 `return`。

### 5. 新建GlobalExceptionHandler.java

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

三个方法分别实现404、409和500。系统异常参数仍保留在方法中，但故意不把 `exception.getMessage()` 放入响应，防止泄露内部信息。

## 十三、正常系与异常系执行流程

### 1. 正常流程

```text
Client
  ↓  GET /employees/1001
DispatcherServlet
  ↓  找到Controller并准备参数
EmployeeController
  ↓  调用findById(1001)
EmployeeService
  ↓  返回EmployeeResponse
EmployeeController
  ↓  创建ApiResponse并返回ResponseEntity
Spring MVC
  ↓  写入200、响应头和JSON正文
Client
```

Service正常返回后，Controller继续执行 `ResponseEntity.ok(...)`。Service不依赖HTTP状态，因此将来仍可以被批处理或其他非HTTP入口复用。

### 2. Service业务异常流程

```text
Client
  ↓  GET /employees/9999
DispatcherServlet
  ↓
EmployeeController
  ↓
EmployeeService
  ↓  throw EmployeeNotFoundException
Controller成功return不再执行
  ↓
Spring MVC异常解析机制
  ↓
GlobalExceptionHandler
  ↓  返回404和失败ApiResponse
Spring MVC
  ↓
Client
```

### 3. 异常不一定经过Service

下面是请求参数转换失败的简化路线：

```text
Client发送GET /employees/abc
  ↓
DispatcherServlet准备Long类型路径参数
  ↓  转换失败
MethodArgumentTypeMismatchException
  ↓
Spring MVC默认异常处理
  ↓
HTTP 400
```

此时Controller和Service都不会执行。全局异常处理器只处理它明确声明的三种项目异常，不会自动接管所有框架异常。

## 十四、在Eclipse中运行并用Postman验证

保存代码，在Eclipse中从启动类运行应用。Console出现启动成功日志后，选择第6章建立的Postman `Local` 环境和 `Employee API` Collection。

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

## 十五、常见问题与日本项目Review

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

## 十六、综合操作练习

### 练习1：增加员工编号业务规则

改修规格：`GET /employees/0` 返回400，消息为 `员工编号必须大于0`；员工编号为正数但不存在时仍返回404。

限制：Controller不能直接判断编号；在Service中判断，新建 `InvalidEmployeeIdException`，再由全局异常处理器映射为400。验证编号0、1001和9999，并保存状态码和正文。

完成后删除该异常类和处理方法，恢复本章稳定状态。第8章会完整替换全局异常处理器，不能让练习代码成为隐藏前置条件。

### 练习2：验证系统故障不泄露内部消息

临时在 `findById()` 最前面加入：

```java
if (id.equals(5000L)) {
    IllegalStateException cause =
            new IllegalStateException("connection refused");
    throw new EmployeeSystemException(
            "database timeout at 192.0.2.10:3306",
            cause);
}
```

同时导入 `EmployeeSystemException`。请求 `GET /employees/5000`，预期500正文只能出现 `服务器内部错误`，不能出现地址、cause或调用栈。记录结果后删除临时代码和导入，重新编译，并确认1001、9999仍分别返回200、404。

在Postman发送 `GET {{baseUrl}}/employees/5000`，无请求参数、无请求体。预期状态码为500，响应正文只能出现 `服务器内部错误`，不能出现地址、cause或调用栈。

`192.0.2.10` 是文档用途地址，不是真实服务器；不要在教学证据中填写公司内部地址。

### 练习3：完成状态码影响调查

收到改修要求：

> 重复邮箱不再返回409，改为HTTP 200并在message中写失败。

先不修改代码。提交Review意见，至少包含：

1. 与当前API详细设计冲突的位置；
2. 对调用方成功判断的影响；
3. Controller、Advice和前端的影响范围；
4. 继续使用409或正式变更规格的建议。

### 练习4：Review异常处理器冲突

假设项目新增第二个高优先级Advice，并声明 `@ExceptionHandler(RuntimeException.class)`。说明它可能对本章三个处理方法产生什么影响，需要核对哪些 `@Order`、根异常和cause关系。不需要把示例代码加入主线工程。

### 练习5：整理自测证据

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

## 十七、本章稳定状态

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
8. 说明 `@ExceptionHandler` 的常见匹配与多Advice优先级；
9. 区分HTTP状态、业务错误码和显示消息；
10. 根据日本项目API详细设计核对正常系、异常系和共通影响范围。

下一章会在当前响应体系上加入Jakarta Validation，区分JSON能否读取、字段是否合格和Service业务规则是否允许，并把校验失败转换为稳定的400响应。
