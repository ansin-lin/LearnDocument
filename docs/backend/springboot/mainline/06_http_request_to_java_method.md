# 第6章 把HTTP请求连接到Java方法

> 本章目标：按照接口规格把路径参数、查询参数和JSON请求体传入Controller方法，理解Spring MVC和Jackson怎样完成绑定与转换，并能根据400、404、405和415定位请求阶段的问题。

第5章已经根据数据方向创建请求DTO和响应对象。本章要解决的是：客户端发送的文本和JSON，怎样成为Java方法参数；Java方法返回的对象，又怎样成为JSON响应。

本章主线如下：

```text
HTTP请求
  → 根据方法和路径找到Controller方法
  → 从路径、查询字符串或请求体取得数据
  → 转换为Java参数或请求DTO
  → Controller调用Service
  → Service返回响应对象
  → 转换为JSON响应
```

## 一、开始状态与完成结果

继续使用第5章工程。开始前应包含：

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

本章完整替换两个已有文件：

```text
controller/EmployeeController.java    ← 完整替换
service/EmployeeService.java          ← 完整替换
```

第4章的 `/employees/sample-name` 和练习用 `/employees/sample-greeting` 到这里停止使用，由下面三个员工接口替代。`/health` 保持不变，用于回归确认。

| 规格编号 | 方法与路径 | 输入位置 | 成功状态 | 成功响应 |
| --- | --- | --- | ---: | --- |
| `EMP-API-02` | `GET /employees/{id}` | 路径中的员工编号 | 200 | 员工详情JSON |
| `EMP-API-03` | `GET /employees` | 可选查询参数 `department` | 200 | 员工列表JSON数组 |
| `EMP-PREVIEW-01` | `POST /employees/preview` | 员工JSON请求体 | 200 | 文本形式的绑定结果 |

`POST /employees/preview` 只用于确认JSON绑定结果，不保存数据，也不代表第5章的真实新增接口已经完成。真实新增需要数据库写入和201响应，后续章节再实现。

## 二、完整示例

先完整替换Service和Controller，运行成功后再从第三节开始逐项理解本章第一次出现的注解、参数和框架对象。

### 1. 完整替换EmployeeService.java

文件位置：

```text
src/main/java/com/example/employee/service/EmployeeService.java
```

完整内容：

```java
package com.example.employee.service;

import com.example.employee.dto.request.EmployeeCreateRequest;
import com.example.employee.dto.response.EmployeeListItemResponse;
import com.example.employee.dto.response.EmployeeResponse;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EmployeeService {

    public EmployeeResponse findById(Long id) {
        return new EmployeeResponse(
                id,
                "Tanaka",
                "Sales",
                "tanaka@example.com");
    }

    public List<EmployeeListItemResponse> findList(String department) {
        return List.of(new EmployeeListItemResponse(
                1001L,
                "Tanaka",
                department));
    }

    public String previewCreate(EmployeeCreateRequest request) {
        return request.getName()
                + " / " + request.getDepartment()
                + " / " + request.getEmail();
    }
}
```

### 2. 完整替换EmployeeController.java

文件位置：

```text
src/main/java/com/example/employee/controller/EmployeeController.java
```

完整内容：

```java
package com.example.employee.controller;

import com.example.employee.dto.request.EmployeeCreateRequest;
import com.example.employee.dto.response.EmployeeListItemResponse;
import com.example.employee.dto.response.EmployeeResponse;
import com.example.employee.service.EmployeeService;
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
    public EmployeeResponse findById(
            @PathVariable(name = "id") Long id) {
        return employeeService.findById(id);
    }

    @GetMapping
    public List<EmployeeListItemResponse> findList(
            @RequestParam(
                    name = "department",
                    defaultValue = "Sales") String department) {
        return employeeService.findList(department);
    }

    @PostMapping("/preview")
    public String previewCreate(
            @RequestBody EmployeeCreateRequest request) {
        return employeeService.previewCreate(request);
    }
}
```

完成后，三个请求分别进入三个Controller方法：

```text
GET  /employees/1001             → findById(1001L)
GET  /employees?department=Sales → findList("Sales")
POST /employees/preview          → previewCreate(request对象)
```

### 3. 先建立请求绑定的整体图

HTTP请求是一条结构化消息，不是直接调用Java方法。请求中的数据可能位于路径、查询字符串、请求头或请求体；Controller参数只是Java方法需要的数据。Spring MVC负责在两者之间搭桥，这个过程称为请求映射和参数绑定。

```text
HTTP方法和URL到达应用
  → 1. 路由匹配：选择哪个Controller方法
  → 2. 数据提取：按照注解从路径、查询字符串或请求体取值
  → 3. 类型转换：把文本转换成Long等Java类型
  → 4. JSON反序列化：需要时把JSON创建成请求DTO
  → 5. 方法调用：参数准备成功后才执行Controller方法
  → 6. 响应序列化：把返回的Java对象写成JSON
```

当前三个接口分别展示了三种常用输入位置：

| HTTP中的位置 | 当前示例 | Controller中的声明 | 得到的Java值 |
| --- | --- | --- | --- |
| 路径的一部分 | `/employees/1001` | `@PathVariable Long id` | `1001L` |
| 问号后的查询参数 | `?department=Sales` | `@RequestParam String department` | `"Sales"` |
| JSON请求体 | `{"name":"Sato",...}` | `@RequestBody EmployeeCreateRequest request` | 一个请求DTO对象 |

开发者负责声明“哪个请求对应哪个方法”和“每个参数从哪里来”；Spring负责读取请求、转换类型、创建DTO并调用方法。如果路由不匹配、`abc` 不能转换成 `Long`，或者JSON无法读取，流程会在Controller方法执行前停止。这一点可以帮助判断错误是在HTTP到Java的桥接阶段，还是已经进入了业务处理阶段。

## 三、类级路径和方法级路径怎样组合

类上的注解：

```java
@RequestMapping("/employees")
```

`@RequestMapping` 的完整名称是 `org.springframework.web.bind.annotation.RequestMapping`，由Spring Web提供。它可以写在类或方法上；本例写在类上，为这个Controller中的全部接口设置共同路径 `/employees`。

这里的 `"/employees"` 是 `value = "/employees"` 的简写。`value` 接收一个或多个路径字符串；不写时表示不追加路径。本章只有一个公共路径，所以采用简写：

```java
@RequestMapping(value = "/employees")  // 完整写法
@RequestMapping("/employees")          // 含义相同的简写
```

方法上的映射继续追加在公共路径后面：

| 类级路径 | 方法注解 | 最终请求 |
| --- | --- | --- |
| `/employees` | `@GetMapping("/{id}")` | `GET /employees/1001` |
| `/employees` | `@GetMapping` | `GET /employees` |
| `/employees` | `@PostMapping("/preview")` | `POST /employees/preview` |

`@GetMapping`、`@PostMapping`、`@PutMapping` 和 `@DeleteMapping` 都位于 `org.springframework.web.bind.annotation` 包中。它们是 `@RequestMapping` 针对常用HTTP方法提供的快捷注解：

| HTTP方法 | 快捷注解 | 当前阶段的典型用途 | 本课程详细使用位置 |
| --- | --- | --- | --- |
| GET | `@GetMapping` | 查询资源 | 第3、4、6章 |
| POST | `@PostMapping` | 提交数据、新增资源或执行动作 | 本章预览接口、第10章新增 |
| PUT | `@PutMapping` | 按接口规格完整修改资源 | 第10章 |
| DELETE | `@DeleteMapping` | 删除资源 | 第10章 |

例如，`@GetMapping("/{id}")` 相当于使用 `@RequestMapping` 同时声明路径 `/{id}` 和HTTP方法GET。快捷注解的括号中可以填写一个或多个方法级路径；完全省略时不追加方法级路径，只使用类级路径。

这些映射注解不只能写路径。项目中还可能根据请求参数、请求头或媒体类型进一步限制匹配条件：

| 属性 | 作用 | 默认值或补充说明 |
| --- | --- | --- |
| `name` | 给映射起一个名称 | 默认空字符串，普通业务接口很少需要填写 |
| `value` / `path` | 声明一个或多个路径 | 二者互为别名，选择一个填写，不要重复声明同一组路径 |
| `method` | 限制HTTP方法 | 仅 `@RequestMapping` 具有；快捷注解已经固定对应的HTTP方法 |
| `params` | 根据查询参数是否存在或取值继续筛选 | 支持 `name`、`!name`、`name=value`、`name!=value` 等表达式 |
| `headers` | 根据请求头是否存在或取值继续筛选 | 表达式写法与 `params` 类似 |
| `consumes` | 限制请求正文的媒体类型 | 例如 `application/json`，主要与有请求体的POST、PUT等请求配合 |
| `produces` | 限制响应能够生成的媒体类型 | 例如 `application/json`，还会参与客户端 `Accept` 请求头的匹配 |

下面是每个注解的“全部独立属性”一行写法，用于识读既存代码，不要求在本章项目中照抄。数组属性只有一个值时可以省略花括号；空数组表示不增加该项限制：

```java
@RequestMapping(name = "createEmployee", path = "/employees", method = RequestMethod.POST, params = {}, headers = {}, consumes = "application/json", produces = "application/json")
@GetMapping(name = "findEmployee", path = "/{id}", params = {}, headers = {}, consumes = {}, produces = "application/json")
@PostMapping(name = "previewEmployee", path = "/preview", params = {}, headers = {}, consumes = "application/json", produces = "application/json")
@PutMapping(name = "updateEmployee", path = "/{id}", params = {}, headers = {}, consumes = "application/json", produces = "application/json")
@DeleteMapping(name = "deleteEmployee", path = "/{id}", params = {}, headers = {}, consumes = {}, produces = "application/json")
```

第一行还需要导入 `org.springframework.web.bind.annotation.RequestMethod`。`value` 与 `path` 是同一含义的别名，所以示例只写 `path`；这不代表遗漏属性，也不应为了展示而同时填写。GET和DELETE通常没有请求正文，因此示例把 `consumes` 保持为空。实际开发应按接口规格填写条件，不能为了“属性齐全”人为增加客户端并不发送的参数或请求头。

本章只实现GET和POST接口，因此不为了展示注解提前制造修改、删除功能。这里先建立四种映射关系；第10章会在数据库CRUD场景中完整讲解PUT的修改语义、DELETE的删除语义、状态码和数据验证。

路径映射同时检查HTTP方法和URL。路径正确但HTTP方法不匹配时，不会进入Controller方法。

## 四、路径参数怎样成为Long

详情路径中使用花括号声明变量位置：

```java
@GetMapping("/{id}")
```

请求 `/employees/1001` 时，`1001` 位于 `{id}` 对应的位置。Controller参数使用：

```java
@PathVariable(name = "id") Long id
```

`@PathVariable` 的完整名称是 `org.springframework.web.bind.annotation.PathVariable`，写在方法参数上。Spring MVC读取路径中的文本 `1001`，按照参数类型把它转换为 `Long`，再调用：

```text
findById(1001L)
```

当前写法的重要参数如下：

| 注解参数 | 当前值 | 可接受的值 | 默认值与作用 |
| --- | --- | --- | --- |
| `name` | `"id"` | 与路径占位符对应的字符串 | 默认尝试使用Java参数名；本例显式写出，固定对应 `{id}` |
| `required` | 未写 | `true` 或 `false` | 默认 `true`，表示该路径变量必须存在 |

`value` 与 `name` 是同一含义的别名。全部独立属性的一行写法是：

```java
@PathVariable(name = "id", required = true) Long id
```

`Long id` 决定目标Java类型。如果请求路径是 `/employees/abc`，文本 `abc` 不能转换为 `Long`，请求会在进入方法前失败，通常返回400。

Controller把转换完成的 `id` 交给Service。当前Service没有数据库，只用传入编号创建示例响应：

```java
return new EmployeeResponse(id, "Tanaka", "Sales", "tanaka@example.com");
```

`new EmployeeResponse(...)` 调用第5章创建的构造方法。返回的是Java对象，不是Controller手写的JSON字符串。

## 五、查询参数怎样进入方法

查询参数位于URL中问号之后：

```text
/employees?department=Development
```

Controller参数使用：

```java
@RequestParam(
        name = "department",
        defaultValue = "Sales") String department
```

`@RequestParam` 的完整名称是 `org.springframework.web.bind.annotation.RequestParam`，写在方法参数上。它从查询字符串读取指定名称的值，并转换为参数类型。

| 注解参数 | 当前值 | 可接受的值 | 默认值与作用 |
| --- | --- | --- | --- |
| `name` | `"department"` | 查询参数名称字符串 | 默认尝试使用Java参数名；本例显式对应URL中的 `department` |
| `defaultValue` | `"Sales"` | 能转换成目标类型的字符串 | 默认没有业务值；本例省略查询参数时使用 `Sales` |
| `required` | 未写 | `true` 或 `false` | 默认 `true`；但设置 `defaultValue` 后，该参数会按可选参数处理 |

`value` 与 `name` 是同一含义的别名。全部独立属性的一行写法是：

```java
@RequestParam(name = "department", required = false, defaultValue = "Sales") String department
```

因此两种请求分别产生：

```text
GET /employees?department=Development
  → findList("Development")

GET /employees
  → findList("Sales")
```

另一种可选参数写法是 `required = false`。没有提供参数且没有默认值时，引用类型参数会得到 `null`，代码必须明确处理空值。本章采用 `defaultValue`，让Service始终收到可使用的部门字符串。

Service使用JDK的 `List` 表示有顺序的一组列表项：

```java
return List.of(new EmployeeListItemResponse(
        1001L,
        "Tanaka",
        department));
```

`List` 的完整名称是 `java.util.List`。`List<EmployeeListItemResponse>` 限定列表元素类型；`List.of(element)` 是JDK提供的静态工厂方法，本例接收一个列表项并返回不可修改的列表。当前固定数据只用于观察参数传递，真实查询在接入数据库后替换。

## 六、JSON请求体怎样成为请求DTO

预览接口接收下面的HTTP请求体：

```json
{
  "name": "Sato",
  "department": "Development",
  "email": "sato@example.com"
}
```

Controller参数使用：

```java
@RequestBody EmployeeCreateRequest request
```

`@RequestBody` 的完整名称是 `org.springframework.web.bind.annotation.RequestBody`，写在方法参数上。它告诉Spring MVC从HTTP请求体读取内容，并使用消息转换器生成目标类型的对象。

| 注解参数 | 当前值 | 可接受的值 | 默认值与作用 |
| --- | --- | --- | --- |
| `required` | 未写 | `true` 或 `false` | 默认 `true`，本例必须提供可以读取的请求体 |

该注解只有 `required` 一个属性，全部属性的一行写法是：

```java
@RequestBody(required = true) EmployeeCreateRequest request
```

本章请求头声明 `Content-Type: application/json`，所以Spring选择JSON消息转换器。转换过程可以简化为：

```text
读取JSON对象
  → 创建EmployeeCreateRequest
  → 把name写入setName(...)
  → 把department写入setDepartment(...)
  → 把email写入setEmail(...)
  → 把完成的request对象传给Controller方法
```

Controller再把请求对象交给Service。`previewCreate()` 依次调用三个getter读取字段，并组合成文本：

```java
return request.getName()
        + " / " + request.getDepartment()
        + " / " + request.getEmail();
```

该方法只证明请求体已经转换并传递成功，不写入数据库。方法名称、路径和响应都明确使用 `preview`，避免调用方误认为数据已经保存。

## 七、Java对象怎样成为JSON

详情方法返回：

```java
EmployeeResponse
```

列表方法返回：

```java
List<EmployeeListItemResponse>
```

类上已有 `@RestController`，所以Spring MVC会把方法返回值写入HTTP响应体。对于这些Java对象，Spring Web使用Jackson完成JSON转换。

Jackson是Spring Web Starter带入的JSON处理库，本章不需要在 `pom.xml` 中重复添加版本。它的核心转换类是 `com.fasterxml.jackson.databind.ObjectMapper`；Spring Boot会自动配置所需对象，本章业务代码不直接创建或调用它。

两个方向使用不同名称：

- **反序列化**：JSON文本转换为 `EmployeeCreateRequest` Java对象；
- **序列化**：`EmployeeResponse` 或列表转换为JSON文本。

Jackson读取响应对象的公共getter决定可输出属性。例如 `EmployeeListItemResponse` 没有 `getEmail()`，所以列表项JSON不会包含邮箱。这正是第5章把详情响应和列表响应分开的实际效果。

JSON数组对应Java集合。一个 `List<EmployeeListItemResponse>` 会成为：

```json
[
  {
    "id": 1001,
    "name": "Tanaka",
    "department": "Sales"
  }
]
```

JSON字段顺序不属于接口规格，客户端应按字段名读取。

## 八、Content-Type和Accept分别表达什么

这两个请求头方向不同：

| 请求头 | 示例值 | 表达的内容 |
| --- | --- | --- |
| `Content-Type` | `application/json` | 客户端发送的请求体是什么格式 |
| `Accept` | `application/json` | 客户端希望接收什么格式的响应 |

POST预览接口必须让服务端知道请求体是JSON。如果把同一段JSON标记为 `text/plain`，当前 `@RequestBody EmployeeCreateRequest` 没有合适的转换方式，通常返回415 Unsupported Media Type。

GET请求通常没有请求体，所以不需要为本章GET请求设置 `Content-Type`。请求JSON响应时可以发送 `Accept: application/json`；省略Accept时，常见HTTP客户端会接受任意服务端可返回的类型。

`Content-Type` 和 `Accept` 都只是媒体类型协商，不负责判断姓名是否为空、邮箱格式是否正确。内容校验属于第8章。

## 九、一次请求经过哪些对象

以 `GET /employees/1001` 为例：

```text
客户端发送HTTP请求
  → 内置Tomcat接收网络请求
  → DispatcherServlet接收Spring MVC请求
  → 根据GET和/employees/{id}找到Controller方法
  → 把路径文本1001转换为Long
  → 调用EmployeeController.findById(1001L)
  → Controller调用EmployeeService.findById(1001L)
  → Service返回EmployeeResponse
  → JSON消息转换器序列化响应对象
  → 客户端收到HTTP 200和JSON正文
```

内置Tomcat由 `spring-boot-starter-web` 提供，是当前应用接收HTTP连接的Web服务器。`DispatcherServlet` 是Spring MVC的核心请求分发对象，完整名称是 `org.springframework.web.servlet.DispatcherServlet`，负责寻找匹配的Controller方法并组织后续处理。

业务代码不需要手动调用Tomcat、DispatcherServlet或ObjectMapper。Controller通过注解声明请求规则，框架在运行时完成匹配、转换和调用。

## 十、默认共享Bean与一次请求的数据

第4章说明Spring在启动时创建Controller和Service Bean。默认情况下，这些Bean采用 `singleton` 作用域：在一个Spring容器中，一个Bean定义通常对应一个共享对象。

因此，多个HTTP请求会复用同一个 `EmployeeController` 和 `EmployeeService` 对象，而不是每次请求都重新创建一套：

```text
请求A ─┐
       ├→ 同一个EmployeeController → 同一个EmployeeService
请求B ─┘
```

但每次方法调用的参数和局部变量属于本次调用：

```java
public EmployeeResponse findById(Long id) {
    return employeeService.findById(id);
}
```

请求A的 `id` 和请求B的 `id` 分别作为各自的方法参数传递。`@RequestBody` 生成的 `EmployeeCreateRequest` 也属于当前请求，不应保存到共享Bean的普通字段中。

下面是错误示意，不要加入完整代码：

```java
private EmployeeCreateRequest currentRequest;

public String previewCreate(EmployeeCreateRequest request) {
    this.currentRequest = request;
    return this.currentRequest.getName();
}
```

Web服务器可能同时处理多个请求。如果请求A和请求B先后修改同一个Service字段，一个请求就可能读取到另一个请求留下的数据。

本章可以先记住：

```text
Controller与Service的固定依赖 → 构造器注入后保存为final字段
某一次请求的数据             → 通过方法参数传递，在局部变量中处理
```

Spring的singleton描述Bean在容器中的复用范围，不等于要求开发者自己实现普通Java的全局单例模式。

## 十一、缺少字段、null和类型错误不是一回事

本章尚未加入Validation，因此要区分“成功生成Java对象”和“字段符合业务规格”。

| 请求情况 | 当前阶段结果 | 原因 |
| --- | --- | --- |
| JSON缺少 `email` | 请求对象可以生成，`email` 为 `null` | 尚未执行必填校验 |
| JSON写成 `"email": null` | 请求对象可以生成，`email` 为 `null` | JSON明确提供空值 |
| 路径写成 `/employees/abc` | 返回400，Controller方法不执行 | `abc` 不能转换为 `Long` |
| JSON缺少右花括号 | 返回400，Controller方法不执行 | JSON语法无法解析 |
| POST标记为 `text/plain` | 返回415 | 请求媒体类型没有可用转换方式 |

如果缺少 `email`，预览文本中可能出现 `null`。这不是正确业务结果，只说明JSON语法和Java类型转换已经完成。第8章会根据第5章字段规格加入必填、长度和邮箱格式校验。

## 十二、先建立其他输入位置的地图

本章主线只要求掌握路径参数、查询参数和JSON请求体。继续阅读既存项目时，还会看到下面几种输入：

| HTTP中的位置 | Spring MVC中常见写法 | 当前要求 |
| --- | --- | --- |
| 请求头 | `@RequestHeader` | 知道它从Header取值 |
| Cookie | `@CookieValue` | 知道Cookie仍属于客户端请求数据 |
| multipart中的JSON或文件 | `@RequestPart`、`MultipartFile` | 知道一次请求可以包含多个part |

这些输入各自还有必填规则、媒体类型、大小限制和安全边界，不适合与三种基础输入同时展开。需要实现相关功能时，使用附录[Header、Cookie与文件上传](../appendix/A08_http_headers_cookies_file_upload.md)完成独立实验。

## 十三、构建、运行与成功验证

在项目根目录执行：

```powershell
.\mvnw.cmd clean test
.\mvnw.cmd spring-boot:run
```

保持启动窗口运行，在另一个PowerShell窗口依次验证。

### 1. 路径参数

```powershell
$detail = Invoke-WebRequest `
    -Uri "http://localhost:8080/employees/1001" `
    -Method Get `
    -Headers @{ Accept = "application/json" }

$detail.StatusCode
$detail.Content
```

预期状态为200，正文内容等价于：

```json
{
  "id": 1001,
  "name": "Tanaka",
  "department": "Sales",
  "email": "tanaka@example.com"
}
```

### 2. 查询参数和默认值

```powershell
$filtered = Invoke-RestMethod `
    -Uri "http://localhost:8080/employees?department=Development" `
    -Method Get

$filtered[0].department
```

预期输出：

```text
Development
```

省略查询参数：

```powershell
$defaultList = Invoke-RestMethod `
    -Uri "http://localhost:8080/employees" `
    -Method Get

$defaultList[0].department
```

预期输出：

```text
Sales
```

### 3. JSON请求体

```powershell
$body = @{
    name = "Sato"
    department = "Development"
    email = "sato@example.com"
} | ConvertTo-Json

$preview = Invoke-WebRequest `
    -Uri "http://localhost:8080/employees/preview" `
    -Method Post `
    -ContentType "application/json" `
    -Body $body

$preview.StatusCode
$preview.Content
```

`@{...}` 创建PowerShell哈希表；`ConvertTo-Json` 把它转换为JSON字符串。`-ContentType` 设置请求体媒体类型，`-Body` 传入实际请求体。

预期结果：

```text
200
Sato / Development / sato@example.com
```

最后回归请求 `/health`，确认仍返回200和 `OK`。验证完成后在启动窗口按 `Ctrl+C` 停止应用。

## 十四、主动观察四类请求失败

下面的请求都只读取当前服务状态，不修改数据库：

| 操作 | 预期状态 | 发生阶段 |
| --- | ---: | --- |
| `GET /employee/1001` | 404 | 没有匹配的请求路径 |
| `DELETE /employees` | 405 | 路径存在，但没有匹配的HTTP方法 |
| `GET /employees/abc` | 400 | 路径文本不能转换为 `Long` |
| `POST /employees/preview` 使用 `text/plain` | 415 | 请求体媒体类型不受当前参数支持 |

可以用 `curl.exe` 查看完整响应状态行：

```powershell
curl.exe -i "http://localhost:8080/employee/1001"

curl.exe -i `
    -X DELETE "http://localhost:8080/employees"

curl.exe -i "http://localhost:8080/employees/abc"

curl.exe -i `
    -X POST "http://localhost:8080/employees/preview" `
    -H "Content-Type: text/plain" `
    --data-binary "not-json"
```

`curl.exe -i` 会同时显示响应头和正文；`-X` 指定HTTP方法，`-H` 添加请求头，`--data-binary` 原样发送请求体。这里没有使用Linux续行符，反引号是PowerShell续行符。

出现错误时先判断发生阶段，不要立即修改Service或数据库：路径和请求方法尚未匹配时，业务方法根本没有执行。

## 十五、常见问题与Review

| 现象或写法 | 原因 | 修正或Review意见 |
| --- | --- | --- |
| 访问详情返回404 | 类级路径和方法级路径组合错误 | 核对 `/employees` 与 `/{id}` |
| `/employees/abc` 返回400 | 文本不能转换为 `Long` | 传入长整数；不要把类型错误当作员工不存在 |
| 查询参数省略后没有预期默认值 | `defaultValue` 拼写或注解位置错误 | 核对Controller方法参数 |
| POST字段全部为 `null` | JSON字段名与Java属性不一致，或DTO缺少setter | 对照第5章请求DTO |
| POST返回415 | `Content-Type` 不是 `application/json` | 修正请求头和正文格式 |
| 列表意外出现邮箱 | 错用了详情响应对象 | 返回 `EmployeeListItemResponse` 列表 |
| Controller保存 `currentRequest` 字段 | 把一次请求数据写入共享Bean | 改为方法参数和局部变量传递 |
| 预览接口被命名为create并声称已保存 | 当前没有数据库写入 | 明确使用preview名称和不保存规格 |

Review接口时沿着下面的关系检查：

```text
接口规格
  → HTTP方法和最终路径
  → 输入数据所在位置
  → Controller注解与Java参数
  → Service方法参数和返回类型
  → JSON字段
  → 实际请求与响应证据
```

## 十六、操作练习

### 练习1：修改查询参数默认值

改修规格：省略 `department` 时，默认部门由 `Sales` 改为 `Development`；显式传入部门时仍使用请求值。

修改前确认影响位置，至少包括Controller注解参数、无参数请求和显式参数请求。修改后验证：

| 请求 | 预期列表项部门 |
| --- | --- |
| `GET /employees` | `Development` |
| `GET /employees?department=Sales` | `Sales` |

保存状态码和响应正文。练习完成后恢复主线默认值 `Sales`，再次验证。

### 练习2：增加一个路径参数接口

新增规格：

| 项目 | 内容 |
| --- | --- |
| 方法和路径 | `GET /employees/{id}/email` |
| 输入 | 路径参数 `id`，类型为 `Long` |
| 成功状态 | 200 |
| 成功正文 | `tanaka@example.com` |

先在Service增加 `findEmailById(Long id)`，再在Controller增加映射方法并调用Service。Controller不能直接写固定邮箱。

验收：`GET /employees/1001/email` 返回200和指定邮箱；`GET /employees/abc/email` 返回400；原有三个接口和 `/health` 仍能回归成功。

练习完成后可以保留该方法，下一章不依赖它。

### 练习3：记录请求阶段的失败证据

分别制造第十四节的404、405、400和415，记录：

1. 实际请求方法和URL；
2. 请求头及请求体是否存在；
3. 实际状态码；
4. 失败发生阶段；
5. Controller或Service是否会执行。

不能只写“接口报错”，也不要把四种状态都归因于参数校验。

### 练习4：Review共享字段方案

Review下面的改修建议：

> 在 `EmployeeService` 增加 `currentRequest` 字段，预览请求到达后先保存DTO，其他方法需要时再读取。

指出默认Bean复用范围、两个请求可能互相覆盖的风险，并给出使用方法参数和局部变量传递的修正方案。不需要把错误代码加入工程。

## 十七、本章稳定状态

完成练习并恢复临时默认值后，工程应保持：

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

此时你应能够：

1. 根据方法和路径判断请求进入哪个Controller方法；
2. 区分 `@PathVariable`、`@RequestParam` 和 `@RequestBody` 的数据来源；
3. 说明 `Content-Type` 和 `Accept` 的方向；
4. 解释Jackson的序列化与反序列化；
5. 区分缺少字段、`null`、类型转换失败和JSON语法错误；
6. 说明为什么请求DTO不能保存在共享Controller或Service字段中；
7. 根据400、404、405和415判断失败发生阶段；

下一章会在这些已能正常传递的数据上，明确成功、无记录、业务拒绝和系统故障分别应该使用什么HTTP状态与响应正文。
