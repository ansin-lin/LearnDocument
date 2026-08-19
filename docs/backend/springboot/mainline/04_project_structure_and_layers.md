# 第4章 Spring Bean、依赖注入与项目分层

> 本章目标：理解Spring如何创建和连接Controller、Service，使用构造器注入把业务处理从Controller移出，并通过接口验证重构前后行为一致。

## 一、开始状态

第3章已经存在：

```text
controller/EmployeeController.java
dto/EmployeeCreateRequest.java
```

三个员工接口可以访问，但Controller同时负责HTTP和数据构造。随着业务增加，这种写法会使Controller难以阅读、复用和测试。

本次修改：

```text
src/main/java/com/example/employee/
├── controller/
│   └── EmployeeController.java    # 完整替换
├── dto/
│   └── EmployeeCreateRequest.java # 保持不变
└── service/
    └── EmployeeService.java       # 新建
```

## 二、为什么要分层

典型后端请求会逐步形成：

```text
HTTP请求
  ↓
Controller：绑定请求、决定HTTP响应
  ↓
Service：执行业务规则、组织用例
  ↓
Mapper：执行数据库操作
  ↓
数据库
```

各层边界：

| 层 | 主要职责 | 不应该承担 |
| --- | --- | --- |
| Controller | URL、请求参数、请求体、状态码 | SQL和复杂业务规则 |
| Service | 业务判断、业务流程、事务入口 | HTTP请求对象和响应状态 |
| Mapper | 执行SQL、转换数据库记录 | 业务流程和HTTP处理 |
| DTO | 表达接口输入 | 数据库持久化行为 |
| Response/VO | 表达接口输出 | 修改数据库状态 |
| Entity | 对应数据库记录 | 直接作为长期前端契约 |

分层不是为了创建更多目录，而是让一次修改的责任和影响范围更清楚。只有空包、没有调用关系，不能算完成分层。

## 三、什么是Spring Bean

Spring Bean是由Spring容器创建和管理的对象。常见组件注解包括：

| 注解 | 常见位置 | 表达的职责 |
| --- | --- | --- |
| `@RestController` | Controller类 | 接收Web请求并写入响应体 |
| `@Service` | Service类 | 执行业务用例和规则 |
| `@Component` | 通用组件类 | 不属于更具体分层的组件 |

应用启动时，`@SpringBootApplication` 从启动类所在包向下扫描这些组件注解，创建对象并注册到Spring容器。

这些注解的来源分别是 `org.springframework.web.bind.annotation.RestController`、`org.springframework.stereotype.Service`、`org.springframework.stereotype.Component` 和 `org.springframework.boot.autoconfigure.SpringBootApplication`。前三个注解标记要管理的组件类；`@SpringBootApplication` 是放在启动类上的组合注解，其中包含组件扫描能力。Spring容器可以理解为保存并管理这些Bean对象的运行环境，不是需要新建的业务类。

项目启动类位于：

```text
com.example.employee.EmployeeManagementApiApplication
```

因此以下包处于默认扫描范围：

```text
com.example.employee.controller
com.example.employee.service
com.example.employee.exception
```

如果把Service放到 `com.example.other`，默认扫描不会发现它，Controller注入时应用会启动失败。

## 四、什么是依赖注入

Controller需要Service，但不在内部直接创建：

```java
// 不推荐：Controller决定了具体对象的创建方式
private final EmployeeService employeeService = new EmployeeService();
```

改为通过构造方法声明需要的对象：

```java
private final EmployeeService employeeService;

public EmployeeController(EmployeeService employeeService) {
    this.employeeService = employeeService;
}
```

Spring发现 `EmployeeController` 只有一个构造方法后，会从容器中寻找 `EmployeeService` 类型的Bean并传入。单构造器场景不需要额外写 `@Autowired`。

`@Autowired` 的完整类名是 `org.springframework.beans.factory.annotation.Autowired`，作用是声明由Spring寻找并注入依赖。本例只有一个构造方法，Spring会自动选择它，所以不导入、不书写该注解；如果看到IDE建议添加它，应先理解这是可选的显式写法，而不是缺少依赖。

构造器注入的优点：

- 依赖是必需的，创建对象时就必须提供。
- 字段可以声明为 `final`。
- 类的依赖在构造方法中清楚可见。
- 普通Java测试可以直接传入替代实现。

## 五、新建EmployeeService

新建文件：

```text
src/main/java/com/example/employee/service/EmployeeService.java
```

完整代码：

```java
package com.example.employee.service;

import com.example.employee.dto.EmployeeCreateRequest;
import org.springframework.stereotype.Service;

@Service
public class EmployeeService {

    public String findEmployeeName(Long id) {
        if (id == 1001L) {
            return "Tanaka";
        }
        return "Unknown";
    }

    public String normalizeDepartment(String department) {
        if (department == null || department.isBlank()) {
            return "ALL";
        }
        return department.trim();
    }

    public Long createEmployee(EmployeeCreateRequest request) {
        // 当前方法先返回示例编号，用于验证Controller和Service调用链。
        // 接入数据库后，这个编号应由数据库主键生成。
        return 1002L;
    }
}
```

`@Service` 由Spring组件扫描处理。类中的方法当前使用固定数据，但业务判断已经不再放在Controller。

### 第一次出现：@Service和Service方法

`@Service` 的完整类名是 `org.springframework.stereotype.Service`，由 `spring-boot-starter` 间接提供。它标记的是“这个类应由Spring创建并管理”，不是立即执行某个方法。应用启动扫描到它后，默认创建一个共享的 `EmployeeService` Bean。

本节三个方法都是项目自己定义的业务方法，不是Spring内置API：

| 方法 | 调用者 | 参数 | 返回值与当前行为 |
| --- | --- | --- | --- |
| `findEmployeeName(Long id)` | Controller详情接口 | 必须提供员工编号对象；当前允许任意 `Long` | 返回 `String`；1001返回 `Tanaka`，其他编号返回 `Unknown` |
| `normalizeDepartment(String department)` | Controller列表接口 | 部门文本，也允许传入 `null` | 返回整理后的新字符串；空值或空白返回 `ALL` |
| `createEmployee(EmployeeCreateRequest request)` | Controller新增接口 | Spring绑定完成的请求DTO | 返回 `Long`；当前固定为1002，接入数据库后改为数据库生成 |

`department.isBlank()` 是JDK 11起提供的 `String` 实例方法，无参数，返回 `boolean`；字符串为空或只含空白字符时为 `true`。`department.trim()` 也是无参数实例方法，返回删除首尾部分常见空白后的新字符串，不会修改原字符串。先判断 `department == null` 很重要，否则对 `null` 调用方法会产生 `NullPointerException`。

`id == 1001L` 会把 `Long` 对象拆箱为基本类型 `long` 后比较数值；如果 `id` 可能为 `null`，拆箱会失败。本章的路径参数必填且转换成功后才进入方法，所以这里不会收到 `null`。

不要在Service中接收 `HttpServletRequest` 或返回HTTP状态码；这些属于Web边界。当前Service只接收普通Java值和DTO。`HttpServletRequest` 是Jakarta Servlet提供的请求对象，完整类名为 `jakarta.servlet.http.HttpServletRequest`，其中保存请求头、参数等Web信息；这里只用于说明错误的分层方式，代码不需要导入它。

## 六、完整替换EmployeeController

用下面代码完整替换第3章的 `EmployeeController.java`：

```java
package com.example.employee.controller;

import com.example.employee.dto.EmployeeCreateRequest;
import com.example.employee.service.EmployeeService;
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

    private final EmployeeService employeeService;

    public EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    @GetMapping("/{id}")
    public Map<String, Object> findById(@PathVariable Long id) {
        Map<String, Object> employee = new LinkedHashMap<>();
        employee.put("id", id);
        employee.put("name", employeeService.findEmployeeName(id));
        employee.put("department", "Sales");
        employee.put("email", "tanaka@example.com");
        return employee;
    }

    @GetMapping
    public Map<String, Object> findList(
            @RequestParam(required = false) String department) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("department", employeeService.normalizeDepartment(department));
        result.put("count", 1);
        return result;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> create(
            @RequestBody EmployeeCreateRequest request) {
        Long employeeId = employeeService.createEmployee(request);

        Map<String, Object> createdEmployee = new LinkedHashMap<>();
        createdEmployee.put("id", employeeId);
        createdEmployee.put("name", request.getName());
        createdEmployee.put("department", request.getDepartment());
        createdEmployee.put("email", request.getEmail());

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(createdEmployee);
    }
}
```

### 第一次出现：被注入的EmployeeService对象

`private final EmployeeService employeeService` 声明一个对象引用：类型是本项目的 `EmployeeService`，变量名是 `employeeService`。`final` 表示构造完成后不能把该字段改为指向另一个Service对象。

Spring创建Controller时会调用 `EmployeeController(EmployeeService employeeService)` 构造方法，并把容器中的Service Bean作为参数传入；`this.employeeService = employeeService` 再把该参数保存到当前Controller对象。这里没有 `new EmployeeService()`，也不需要 `@Autowired`，因为Spring对唯一构造方法会自动执行构造器注入。

之后的 `employeeService.findEmployeeName(id)` 等调用，就是Controller通过已注入对象调用上一节定义的方法。Spring Bean默认是单例作用域，因此Service中不要用普通字段保存某一次请求的姓名、邮箱等可变数据，否则并发请求可能互相影响。

重构后的调用关系：

```text
GET /employees/1001
  → EmployeeController.findById()
  → EmployeeService.findEmployeeName()
  → Controller生成HTTP响应
```

Controller仍负责读取HTTP参数和设置201状态；Service负责姓名判断、部门标准化和创建用例。数据库职责尚未出现。

## 七、启动并验证行为没有改变

```powershell
.\mvnw.cmd spring-boot:run
```

验证详情：

```powershell
Invoke-RestMethod `
    -Uri "http://localhost:8080/employees/1001" `
    -Method Get
```

`name` 应为 `Tanaka`。再访问不存在的示例编号：

```powershell
Invoke-RestMethod `
    -Uri "http://localhost:8080/employees/9999" `
    -Method Get
```

`name` 应为 `Unknown`，说明Controller确实调用了Service中的判断。

验证部门标准化：

```powershell
Invoke-RestMethod `
    -Uri "http://localhost:8080/employees?department=%20Sales%20" `
    -Method Get
```

响应中的部门应去掉首尾空白。

最后重新执行第3章的POST请求，确认仍返回201。重构的第一目标是改变内部结构而不破坏外部接口。

## 八、主动制造注入失败

为了观察组件扫描，临时删除 `EmployeeService` 上的 `@Service` 后启动应用。预期启动失败，日志包含找不到 `EmployeeService` Bean的含义。

恢复 `@Service` 后重新启动，应用应恢复正常。

另一种常见失败是把Service移动到根包之外。修正方法不是在Controller里重新 `new`，而是把组件放回合理扫描范围，或在更复杂项目中显式配置扫描范围。

## 九、常见失败

| 现象 | 原因 | 修正 |
| --- | --- | --- |
| 应用启动时提示找不到Bean | Service没有组件注解或不在扫描范围 | 恢复 `@Service` 并检查包路径 |
| 提示找到多个同类型Bean | 同一接口存在多个实现且无法选择 | 使用明确实现、`@Primary`或`@Qualifier` |
| `employeeService` 为 `null` | 自己使用无参构造或绕过Spring创建Controller | 由Spring管理Controller并使用构造器注入 |
| Controller越来越长 | 业务和对象转换仍留在Controller | 按职责移入Service或映射组件 |
| 重构后接口字段变化 | 只关注内部代码，没有回归HTTP契约 | 重新执行上一章全部请求并比较响应 |

## 十、操作练习

初始状态：Controller已经通过构造方法使用EmployeeService。

任务：

1. 在Service新增 `normalizeEmail(String email)`，把邮箱去除首尾空白并转成小写。
2. POST创建员工时调用该方法，再放入响应。
3. 临时删除 `@Service`，记录启动失败中的关键Bean类型，然后恢复。
4. 说明为什么Controller负责201，而Service不返回 `HttpStatus.CREATED`。

验收标准：

- Controller中没有 `new EmployeeService()`。
- 依赖字段是 `private final`，通过唯一构造方法注入。
- 邮箱标准化逻辑位于Service，并能通过POST响应验证。
- 删除和恢复 `@Service` 后，能够解释启动结果差异。

## 十一、当前稳定状态

保留：

```text
controller/EmployeeController.java
dto/EmployeeCreateRequest.java
service/EmployeeService.java
```

当前还没有创建Mapper、Entity或数据库表。项目已经形成Controller→Service调用关系，接下来可以使用明确的响应对象替换临时 `Map`，并统一成功响应结构。
