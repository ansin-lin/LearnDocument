# 第3章 Controller、请求绑定与JSON响应

> 本章目标：在第2章工程中增加员工接口，能够接收路径参数、查询参数和JSON请求体，返回正确的HTTP状态与JSON，并区分400、404、405和415。

## 一、开始状态

继续使用第2章创建的 `employee-management-api`，不要重新生成项目。开始前确认：

- 应用使用8080端口。
- `GET /health` 返回200和 `OK`。
- `pom.xml` 已包含 `spring-boot-starter-web`。
- 根包是 `com.example.employee`。

本章新建或替换：

```text
src/main/java/com/example/employee/
├── controller/
│   └── EmployeeController.java       # 新建
└── dto/
    └── EmployeeCreateRequest.java    # 新建
```

此时还没有数据库。接口使用固定数据观察HTTP路径匹配、参数绑定、JSON转换和状态码。

## 二、一次请求如何进入Controller

以 `GET /employees/1001` 为例：

```text
客户端
  ↓ GET /employees/1001
内置Tomcat
  ↓
Spring MVC DispatcherServlet
  ↓ 根据路径和请求方法寻找处理方法
EmployeeController.findById(1001)
  ↓ 返回Java对象
Jackson转换为JSON
  ↓
HTTP 200响应
```

Controller负责HTTP边界：接收路径、参数和请求体，调用业务代码，再决定响应内容与状态。复杂业务规则和数据库操作不放在Controller中。

流程图中出现的内置Tomcat由 `spring-boot-starter-web` 带入；`DispatcherServlet` 是Spring MVC的前端控制器对象，完整类名为 `org.springframework.web.servlet.DispatcherServlet`，负责接收请求并寻找匹配的Controller方法；Jackson是Spring默认使用的JSON类库，核心转换对象是 `com.fasterxml.jackson.databind.ObjectMapper`。Spring Boot会配置这些对象，业务代码通过注解声明接口，不直接调用这些框架内部对象。

## 三、创建请求DTO

新建文件：

```text
src/main/java/com/example/employee/dto/EmployeeCreateRequest.java
```

完整代码：

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

DTO表示接口允许接收的数据。Spring Web通过JSON转换组件创建对象并调用setter，把请求中的 `name`、`department` 和 `email` 写入字段。

### 第一次出现：DTO对象、getter和setter

这里的 `EmployeeCreateRequest` 是本项目自己创建的类；收到POST请求时，通常不是Controller手写 `new EmployeeCreateRequest()`，而是Jackson先创建对象，Spring MVC再把完成绑定的对象交给 `create()` 方法。

| 代码 | 含义 | 参数 | 返回值或结果 |
| --- | --- | --- | --- |
| `String` | JDK的字符串类，完整类名是 `java.lang.String`；`java.lang` 会被自动导入 | — | 用来保存姓名、部门和邮箱文本 |
| `getName()` 等getter | 读取当前对象中的字段 | 无 | 返回对应的 `String` 字段值 |
| `setName(String name)` 等setter | 把参数写入当前对象的字段；Jackson绑定JSON时会调用它们 | 一个对应类型的字段值 | `void`，表示不返回结果 |
| `this.name = name` | 左侧是当前对象的字段，右侧是方法参数 | — | 把参数保存到当前对象 |

代码没有显式编写构造方法时，Java会提供一个无参数构造方法。Jackson可以先通过它创建空对象，再依次调用setter。以后如果自行增加了有参数构造方法，就不能再假设无参数构造方法仍然自动存在。

当前DTO先用于观察数据绑定。非空、长度和邮箱格式属于请求校验能力，需要在引入校验依赖和统一错误响应后加入。

## 四、创建完整Controller

新建文件：

```text
src/main/java/com/example/employee/controller/EmployeeController.java
```

完整代码：

```java
package com.example.employee.controller;

import com.example.employee.dto.EmployeeCreateRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/employees")
public class EmployeeController {

    @GetMapping("/{id}")
    public Map<String, Object> findById(@PathVariable Long id) {
        Map<String, Object> employee = new LinkedHashMap<>();
        employee.put("id", id);
        employee.put("name", "Tanaka");
        employee.put("department", "Sales");
        employee.put("email", "tanaka@example.com");
        return employee;
    }

    @GetMapping
    public Map<String, Object> findList(
            @RequestParam(required = false) String department) {
        String selectedDepartment = department == null ? "ALL" : department;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("department", selectedDepartment);
        result.put("count", 1);
        return result;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> create(
            @RequestBody EmployeeCreateRequest request) {
        Map<String, Object> createdEmployee = new LinkedHashMap<>();
        createdEmployee.put("id", 1002L);
        createdEmployee.put("name", request.getName());
        createdEmployee.put("department", request.getDepartment());
        createdEmployee.put("email", request.getEmail());

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(createdEmployee);
    }
}
```

### 怎样判断类和包来自哪里

`package` 声明当前类属于哪个包，`import` 让当前文件可以使用其他包中类的短名称。看到陌生类型时，先查看文件顶部的导入：

| 包名前缀 | 本章示例 | 来源与处理方式 |
| --- | --- | --- |
| `com.example.employee...` | `EmployeeCreateRequest` | 本项目代码，需要创建对应文件 |
| `org.springframework...` | `ResponseEntity`、`@GetMapping` | Spring框架，由Maven依赖提供，不自行创建同名类 |
| `java...` | `Map`、`LinkedHashMap` | JDK标准库，不需要写入 `pom.xml` |
| `java.lang...` | `String`、`Long`、`Object` | JDK基础类型所在包，Java会自动导入 |

`Long` 是可以保存 `null` 的整数对象类型；Spring会把路径中的文本转换成它。`Object` 是Java类层次的根类型，`Map<String, Object>` 因而可以同时保存编号、文本等不同类型的值。

### 第一次出现：Spring MVC注解

这些注解都来自 `org.springframework.web.bind.annotation`。注解不是普通方法调用，而是写给Spring读取的配置；应用启动或处理请求时，Spring根据这些信息完成路由和绑定。

| 注解 | 当前写法、可接受值与默认值 | 运行时作用 |
| --- | --- | --- |
| `@RestController` | 写在类上；本例不传参数 | 把类注册为Web控制器，并把方法返回值写入响应体 |
| `@RequestMapping("/employees")` | `value`/`path` 接受路径字符串，默认是空路径；本例使用员工接口的公共前缀 | 给当前Controller的全部接口增加 `/employees` 前缀 |
| `@GetMapping("/{id}")` | 路径参数可省略，默认是空路径；请求方法固定为GET | 把GET子路径交给 `findById()` |
| `@PostMapping` | 本例不写路径，使用默认空路径；请求方法固定为POST | 与类级路径组合成 `POST /employees` |
| `@PathVariable` | 默认必填；当前参数名 `id` 对应 `{id}` | 从URL路径取值并转换为 `Long` |
| `@RequestParam(required = false)` | `required` 接受 `true`/`false`，默认 `true`；本例改为 `false` | 读取查询参数；省略时得到 `null` |
| `@RequestBody` | `required` 默认 `true`，本例保持默认 | 让消息转换器读取JSON并生成 `EmployeeCreateRequest` |

这段代码依靠参数名让 `{id}` 与 `id` 对应。企业项目也常写成 `@PathVariable("id")`，在重构参数名时能更明确地保留接口名称。

### 第一次出现：Map对象及其方法

`Map` 和 `LinkedHashMap` 都来自 `java.util`：

- `Map<String, Object>` 是接口类型，规定数据按“键—值”保存；键固定为字符串，值允许是不同Java对象。
- `new LinkedHashMap<>()` 真正创建对象。`LinkedHashMap` 是 `Map` 的一种实现，会保留当前示例的插入顺序；但JSON字段顺序仍不应成为接口契约。
- `employee.put("id", id)` 调用 `put(K key, V value)`：第一个参数是键，第二个参数是值；它返回该键原来的值，没有旧值时返回 `null`。本例只需要写入数据，所以没有接收返回值。
- `new` 表示创建对象；`<>` 让编译器根据左侧的 `Map<String, Object>` 推断泛型参数。

### ResponseEntity不需要自己创建

`ResponseEntity` 是Spring Web已经提供的框架类，不是员工管理项目中的业务类，因此不需要新建 `ResponseEntity.java`。它的完整类名是：

```text
org.springframework.http.ResponseEntity
```

Controller顶部的导入语句让当前文件可以使用它的短类名：

```java
import org.springframework.http.ResponseEntity;
```

第2章选择的 `Spring Web` 最终在 `pom.xml` 中形成 `spring-boot-starter-web` 依赖。Maven根据该依赖下载Spring Web类库，编译器才能找到 `ResponseEntity`。`import` 只声明当前代码使用哪个类，本身不会创建类，也不会下载依赖。

本段代码中的类型来源如下：

| 类型 | 来源 | 是否需要在项目中新建文件 |
| --- | --- | --- |
| `EmployeeController` | 项目业务代码 | 是，创建 `EmployeeController.java` |
| `EmployeeCreateRequest` | 项目业务代码 | 是，创建 `EmployeeCreateRequest.java` |
| `ResponseEntity`、`HttpStatus` | Spring Web | 否，通过 `org.springframework.http` 导入 |
| `@RestController`、`@GetMapping` 等 | Spring Web | 否，通过 `org.springframework.web.bind.annotation` 导入 |
| `Map`、`LinkedHashMap` | Java标准库 | 否，通过 `java.util` 导入 |

如果IDE提示 `ResponseEntity cannot be resolved` 或无法导入，正确的排查顺序是：

1. 检查 `pom.xml` 是否存在 `spring-boot-starter-web`。
2. 刷新或重新加载Maven项目，等待依赖下载完成。
3. 检查导入是否为 `org.springframework.http.ResponseEntity`。
4. 不要自行创建一个同名 `ResponseEntity` 类来掩盖依赖问题。

### 类级路径和方法级路径

`@RequestMapping("/employees")` 给整个Controller设置共同前缀，方法只写各自追加的部分：

| 方法 | 最终请求 | 处理方法 |
| --- | --- | --- |
| `GET` | `/employees/1001` | `findById()` |
| `GET` | `/employees?department=Sales` | `findList()` |
| `POST` | `/employees` | `create()` |

### 三种输入来源

| 注解 | 数据位置 | 当前示例 | 默认是否必填 |
| --- | --- | --- | --- |
| `@PathVariable` | URL路径 | `/employees/{id}` | 是 |
| `@RequestParam` | URL查询字符串 | `?department=Sales` | 是；本例显式设为可选 |
| `@RequestBody` | HTTP请求体 | 员工JSON | 是 |

`@RequestParam(required = false)` 允许省略部门。省略后参数值为 `null`，本例把它转换成 `ALL`，避免把 `null` 放入不接受空值的响应结构。

### Java对象为什么会成为JSON

`@RestController` 表示方法返回值写入HTTP响应体。Spring Web根据客户端可以接收的媒体类型选择转换器；当前Java对象由Jackson转换成JSON。

`ResponseEntity` 可以同时控制响应体、状态码和响应头。新增成功使用201 Created，比所有请求都返回200更准确。

本例中的 `HttpStatus` 是Spring提供的HTTP状态枚举，`HttpStatus.CREATED` 表示201。`ResponseEntity.status(HttpStatus.CREATED)` 接受一个状态值并返回响应构建器；随后调用 `.body(createdEmployee)`，传入响应体并得到最终的 `ResponseEntity<Map<String, Object>>` 对象。该对象由Spring MVC写入HTTP响应，而不是作为普通JSON整体序列化。

## 五、启动项目

在包含 `pom.xml` 的项目根目录执行：

```powershell
.\mvnw.cmd spring-boot:run
```

保持这个终端窗口运行，再打开另一个PowerShell窗口发送请求。

## 六、验证GET请求

### 路径参数

```powershell
$response = Invoke-WebRequest `
    -Uri "http://localhost:8080/employees/1001" `
    -Method Get

$response.StatusCode
$response.Content
```

这里的 `$response` 是PowerShell变量，保存 `Invoke-WebRequest` 返回的响应对象。`Invoke-WebRequest` 的 `-Uri` 是必填请求地址，`-Method` 指定HTTP方法；返回对象的 `StatusCode` 属性用于查看状态码，`Content` 属性保存原始响应正文。后面的 `Invoke-RestMethod` 参数用途相同，但它会进一步把JSON正文转换成便于PowerShell读取的对象。

预期状态码为200，响应体包含：

```json
{
  "id": 1001,
  "name": "Tanaka",
  "department": "Sales",
  "email": "tanaka@example.com"
}
```

JSON字段顺序不属于接口契约，客户端应按字段名读取。

### 查询参数

```powershell
Invoke-RestMethod `
    -Uri "http://localhost:8080/employees?department=Sales" `
    -Method Get
```

预期内容：

```json
{
  "department": "Sales",
  "count": 1
}
```

再省略查询参数：

```powershell
Invoke-RestMethod `
    -Uri "http://localhost:8080/employees" `
    -Method Get
```

`department` 应变成 `ALL`，说明可选参数分支确实执行。

## 七、验证POST JSON请求

先构造JSON请求体：

```powershell
$body = @{
    name = "Sato"
    department = "Development"
    email = "sato@example.com"
} | ConvertTo-Json

$response = Invoke-WebRequest `
    -Uri "http://localhost:8080/employees" `
    -Method Post `
    -ContentType "application/json" `
    -Body $body

$response.StatusCode
$response.Content
```

`@{...}` 是PowerShell哈希表对象，`ConvertTo-Json` 接收管道传来的对象并返回JSON字符串；`-ContentType "application/json"` 声明正文格式，`-Body $body` 传入实际请求体。

预期状态码为201，响应体包含新生成的示例编号1002和请求中的三个字段。

`Content-Type: application/json` 告诉服务端请求体的格式。如果请求头与正文格式不一致，Spring无法选择正确的转换方式。

## 八、主动观察四类失败

| 操作 | 预期状态 | 含义 |
| --- | ---: | --- |
| 访问 `/employee/1001` | 404 | 没有匹配的路径 |
| 对 `/employees` 发送 `DELETE` | 405 | 路径存在，但不支持该请求方法 |
| 把路径编号写成 `/employees/abc` | 400 | 文本无法转换成 `Long` |
| POST JSON但把Content-Type设为 `text/plain` | 415 | 服务端不支持该请求体媒体类型 |

这些错误分别发生在“找路径、找方法、绑定参数、读取请求体”阶段。出现错误时先看状态码和启动日志，不要立即修改数据库或重装JDK。

## 九、常见失败

| 现象 | 原因 | 修正 |
| --- | --- | --- |
| Controller没有被发现 | 类放在根包扫描范围外 | 放到 `com.example.employee` 的子包 |
| POST请求字段全部为 `null` | JSON字段名不匹配或缺少setter | 对照DTO字段并保留公共setter |
| 返回一段普通文本而不是JSON | 方法返回的是 `String` | 返回Java对象，或使用统一响应对象 |
| POST得到415 | 没有声明JSON媒体类型 | 设置 `Content-Type: application/json` |
| POST得到400 | JSON语法错误或类型转换失败 | 查看响应和日志中的具体字段、行列信息 |

## 十、操作练习

初始状态：三个员工接口已经能够返回200或201。

任务：

1. 给详情接口增加 `includeEmail` 查询参数，默认值为 `true`；传入 `false` 时不返回邮箱。
2. 给 `EmployeeCreateRequest` 增加 `String position`，让POST请求和响应都包含职位。
3. 分别制造404、405、400和415，并记录请求、状态码和原因。
4. 把POST的状态码临时改成200，比较后恢复为201。

验收标准：

- 三种输入来源都能独立说明并成功调用。
- POST请求使用JSON媒体类型并返回201。
- 能根据状态码判断失败发生在哪个HTTP处理阶段。
- 修改字段后，DTO、请求JSON和响应JSON名称保持一致。

完成练习后，如果增加了 `position`，请撤销该练习字段，恢复项目固定字段：`name`、`department`、`email`。

## 十一、当前稳定状态

当前工程保留：

```text
controller/HealthController.java
controller/HelloController.java
controller/EmployeeController.java
dto/EmployeeCreateRequest.java
```

员工接口此时仍由Controller构造固定数据。项目可以在保持URL和响应结果不变的前提下，把业务处理移入Service，并观察Spring如何创建和注入对象。
