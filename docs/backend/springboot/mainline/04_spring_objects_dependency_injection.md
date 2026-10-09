# 第4章 Spring怎样创建并连接各层对象

> 本章目标：在第3章工程中完成 `Controller → Service` 调用，理解对象为什么由Spring创建、Service为什么通过构造方法传入Controller，并能定位“没有候选Bean”的注入问题。

第2章已经说明Controller负责接收请求，Service负责处理业务。现在要解决一个具体问题：Controller需要调用Service，这个Service对象应该由谁创建？

本章只围绕下面一条主线展开：

```text
Controller需要Service
  → 不由Controller手动创建Service
  → Spring创建并管理Service对象
  → Spring通过构造方法把Service交给Controller
  → Controller调用Service并返回结果
```


## 一、开始状态与完成结果

继续使用第3章创建的 `employee-management-api` 工程。开始前应确认：

- `GET /health` 返回状态码200和正文 `OK`；
- 启动类位于根包 `com.example.employee`；
- `pom.xml` 已包含 `spring-boot-starter-web`；
- 项目已在Eclipse中导入，并能从启动类正常启动。

Employee主线新增两个文件，不修改第3章的启动类、配置文件和 `HealthController`：

```text
src/main/java/com/example/employee/
├── EmployeeManagementApiApplication.java
├── controller/
│   ├── EmployeeController.java       ← 本章新建
│   └── HealthController.java
└── service/
    └── EmployeeService.java          ← 本章新建
```

完成后的新接口规格如下：

| 项目 | 内容 |
| --- | --- |
| 用途 | 取得一个示例员工姓名 |
| HTTP方法 | `GET` |
| 路径 | `/employees/sample-name` |
| 请求参数 | 无 |
| 成功状态 | `200 OK` |
| 响应正文 | `Suzuki` |

这一接口暂时使用固定字符串，不接收请求数据，也不访问数据库。本章的学习重点是对象创建和连接。完成后应能解释Spring怎样创建并连接Controller与Service，并能通过启动日志定位缺少Bean的问题。

## 二、先解决对象由谁创建的问题，再看完整示例

普通Java可以先创建 `EmployeeService service = new EmployeeService();`，再用 `new EmployeeController(service)` 把它交给Controller。若Controller自己在字段里 `new EmployeeService()`，它就必须知道Service的创建细节；后续Service需要Mapper时，Controller还要跟着改。本项目改由Spring容器负责创建和连接对象：容器管理的对象叫Bean，创建控制权交给容器是IoC，通过构造方法交入所需Bean是DI。它们描述的是同一套对象管理过程，不是四个独立功能。

应用启动时，启动类的组件扫描发现类上的 `@Service` 与 `@RestController`；Spring注册并创建相应Bean，解析Controller构造方法需要的 `EmployeeService`，找到唯一合适的Bean后传入。请求到达时使用已建立的对象关系，不会在每次请求里重新创建Service。Controller以 `private final` 保存依赖，表示它必须在构造时提供，之后不随意替换，也使测试时能显式传入替身。下文第三至八节将逐步核对这些阶段和失败原因。

下面保留两个可直接复制的完整文件。先读懂以上运行顺序，再写入工程、启动验证；文件名、包名和目录必须一致。

### 1. 新建EmployeeService.java

文件位置：

```text
src/main/java/com/example/employee/service/EmployeeService.java
```

完整内容：

```java
package com.example.employee.service;

import org.springframework.stereotype.Service;

@Service
public class EmployeeService {

    public String getSampleEmployeeName() {
        return "Suzuki";
    }
}
```

### 2. 新建EmployeeController.java

文件位置：

```text
src/main/java/com/example/employee/controller/EmployeeController.java
```

完整内容：

```java
package com.example.employee.controller;

import com.example.employee.service.EmployeeService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class EmployeeController {

    private final EmployeeService employeeService;

    public EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    @GetMapping("/employees/sample-name")
    public String getSampleEmployeeName() {
        return employeeService.getSampleEmployeeName();
    }
}
```

两个文件组合后的调用方向是：

```text
GET /employees/sample-name
  → EmployeeController
  → EmployeeService
  → 返回"Suzuki"
```

### 3. 先建立对象管理的整体图

完整示例里同时发生了两类事情，但发生时间不同：

```text
应用启动时
  → Spring扫描到@RestController和@Service
  → 创建EmployeeService对象
  → 发现EmployeeController的构造方法需要EmployeeService
  → 创建EmployeeController对象，并把EmployeeService传进去
  → 两个对象都成为Spring管理的Bean

收到请求时
  → Spring找到EmployeeController中的处理方法
  → Controller使用已经保存的EmployeeService
  → Service返回业务结果
```

也就是说，Spring不是每收到一次请求才临时 `new EmployeeService()`。对象的创建和连接主要发生在应用启动阶段；请求到达后，Controller使用已经连接好的对象完成处理。

先用下面四个问题区分本章概念：

| 概念 | 它回答的问题 | 当前示例 |
| --- | --- | --- |
| 类 | 可以创建什么样的对象？ | `EmployeeController`、`EmployeeService` 的源码定义 |
| Bean | 哪个运行时对象由Spring管理？ | Spring创建的Controller对象和Service对象 |
| IoC | 对象的创建和连接由谁统一控制？ | 从Controller自己创建，改为由Spring容器管理 |
| DI | 一个对象需要的另一个对象怎样交给它？ | Spring通过构造方法传入 `EmployeeService` |

开发者负责写类、添加组件注解并用构造方法声明依赖；Spring负责发现这些类、创建对象、寻找匹配对象并完成连接。`@Service` 说明“这个类的对象交给Spring管理”，构造方法说明“Controller必须得到一个EmployeeService才能建立”。两者组合后，Spring才知道要创建什么以及怎样连接。

接下来按照实际执行关系解释其中的内容。

## 三、Controller为什么依赖Service

### 1. 先理解对象和依赖

类规定对象有哪些字段和方法；对象是程序运行时根据类创建出来的实例。`EmployeeController` 和 `EmployeeService` 是两个类，程序运行时需要对应的对象才能调用其中的方法。

Controller中的这一行：

```java
return employeeService.getSampleEmployeeName();
```

表示Controller要通过 `employeeService` 引用调用Service的方法。没有可用的 `EmployeeService` 对象，这一调用就无法执行。因此可以说：

```text
EmployeeController依赖EmployeeService
```

这里的“依赖”不是错误，而是对象协作关系：Controller完成请求处理时，需要Service提供业务结果。

### 2. 普通Java可以怎样创建对象

不使用Spring时，可以用 `new` 创建对象，再通过构造方法传给Controller：

```java
EmployeeService employeeService = new EmployeeService();
EmployeeController employeeController = new EmployeeController(employeeService);
```

第一行创建Service对象，第二行创建Controller对象，同时把Service对象交给Controller。这说明创建顺序必须满足依赖顺序。

如果直接在Controller内部创建Service，代码可能写成：

```java
private final EmployeeService employeeService = new EmployeeService();
```

这样虽然能够得到对象，但Controller同时承担了两项责任：

1. 接收请求并调用业务方法；
2. 决定Service怎样创建。

当Service以后还需要Mapper、配置或其他对象时，Controller也会被迫了解这些创建细节。修改对象创建方式就可能连带修改Controller，职责会逐渐混在一起。

本章保留第一种“从外部传入”的关系，但把对象创建和连接工作交给Spring。

## 四、Spring容器、Bean、IoC和DI

### 1. Spring容器

第3章执行 `SpringApplication.run(...)` 时，Spring Boot会创建应用上下文。对于本章，可以把应用上下文理解为Spring容器：它负责保存对象定义、创建需要管理的对象，并按照对象之间的依赖关系把它们连接起来。

容器不是一个需要我们手动遍历的Java集合。开发者通过注解和构造方法声明规则，Spring在启动过程中读取这些规则。

### 2. Bean

由Spring容器创建、保存和管理的对象称为Bean。

本章有两个与业务代码直接相关的Bean：

| Bean | Spring识别它的依据 | 作用 |
| --- | --- | --- |
| `EmployeeService` 对象 | 类上的 `@Service` | 提供取得示例姓名的业务方法 |
| `EmployeeController` 对象 | 类上的 `@RestController` | 接收HTTP请求并调用Service |

“类”和“Bean”不能混为一谈：`EmployeeService` 是源码中的类；Spring根据这个类创建并管理的运行时对象才是Bean。

### 3. IoC：对象创建的控制权发生变化

IoC是Inversion of Control，中文通常译为“控制反转”。这里的“控制”主要指对象由谁创建、怎样连接。

```text
Controller内部new Service
  → Controller控制Service的创建

Spring创建Service并传给Controller
  → 创建和连接工作转交给Spring容器
```

这不是说程序失去控制，而是开发者声明对象关系，由框架统一完成创建和装配。

### 4. DI：把需要的对象传进来

DI是Dependency Injection，中文通常译为“依赖注入”。本章中，Spring把已经创建的 `EmployeeService` Bean传入 `EmployeeController` 的构造方法，这个动作就是依赖注入。

IoC描述整体控制方式的变化，DI描述实现这种变化的一种具体方法。可以先这样记忆：

```text
IoC：对象创建和连接交给Spring管理
DI：Spring把一个对象需要的依赖传给它
```

## 五、Spring怎样创建EmployeeService

重新观察完整Service：

```java
package com.example.employee.service;

import org.springframework.stereotype.Service;

@Service
public class EmployeeService {

    public String getSampleEmployeeName() {
        return "Suzuki";
    }
}
```

### 1. package和import

`package com.example.employee.service;` 声明这个类属于Service包。该包位于根包 `com.example.employee` 下面，因此处于默认组件扫描范围内。

`import org.springframework.stereotype.Service;` 导入Spring Framework提供的 `Service` 注解类型。有了import，类上才可以使用简写 `@Service`。

### 2. @Service

`@Service` 的完整名称是 `org.springframework.stereotype.Service`，写在类上。应用启动并执行组件扫描时，Spring发现这个注解，根据 `EmployeeService` 类创建对象，并把对象注册为Bean。

`@Service` 表达“这个类承担业务处理职责”。它本身基于通用组件注解 `@Component`。两者都能使类被组件扫描发现，但在业务类上使用 `@Service` 能更清楚地表达分层职责。本章不需要再给同一个类重复添加 `@Component`。

`@Component` 的完整名称是 `org.springframework.stereotype.Component`，用于没有Controller、Service等更明确角色的通用组件。本章完整示例没有这种对象，因此不额外创建一个只为展示注解的类。

### 3. getSampleEmployeeName方法

```java
public String getSampleEmployeeName() {
    return "Suzuki";
}
```

- `public` 表示其他类可以调用这个方法；
- `String` 是返回值类型；
- `getSampleEmployeeName` 是项目自己定义的方法名；
- 空括号表示当前不接收参数；
- `return "Suzuki";` 返回固定字符串。

固定字符串只是当前阶段的最小业务结果。以后接入请求对象和数据库时，仍由Service组织业务处理，Controller不直接承担业务规则。

## 六、Spring怎样把Service交给Controller

### 1. 字段保存Service引用

```java
private final EmployeeService employeeService;
```

这个字段用于保存传入的Service对象引用：

- `private` 表示字段只在 `EmployeeController` 类内部直接使用；
- `EmployeeService` 是字段类型；
- `employeeService` 是字段名；
- `final` 表示字段完成一次赋值后，不能再指向另一个Service对象。

这里还没有创建对象，也没有执行注入，只是声明Controller必须长期持有一个 `EmployeeService` 引用。

### 2. 构造方法声明必须提供的依赖

```java
public EmployeeController(EmployeeService employeeService) {
    this.employeeService = employeeService;
}
```

构造方法与类同名，没有返回值类型，在创建对象时执行。

参数 `EmployeeService employeeService` 表示：要创建可用的 `EmployeeController`，必须先提供一个 `EmployeeService` 对象。这个参数值不是HTTP请求传来的，也不是Controller自己创建的，而是Spring从容器中查找到的 `EmployeeService` Bean。

`this.employeeService` 表示当前Controller对象的字段；右侧的 `employeeService` 表示构造方法参数。赋值语句把Spring传入的对象引用保存到字段中，后面的方法才能继续使用。

完整注入过程是：

```text
Spring准备创建EmployeeController
  → 检查构造方法需要EmployeeService参数
  → 在容器中找到EmployeeService Bean
  → 调用构造方法并传入该Bean
  → 构造方法把Bean引用保存到字段
  → EmployeeController创建完成
```

这种通过构造方法提供依赖的写法称为**构造器注入**。对象创建完成时，必需依赖也已经具备，代码还能通过构造参数直接看出这个类需要哪些对象。

### 3. 单构造器与@Autowired

当前 `EmployeeController` 只有一个构造方法，Spring没有其他构造方法可选，因此会自动使用它完成注入，不需要添加 `@Autowired`。

既有项目中可能看到下面的写法：

```java
@Autowired
public EmployeeController(EmployeeService employeeService) {
    this.employeeService = employeeService;
}
```

`@Autowired` 的完整名称是 `org.springframework.beans.factory.annotation.Autowired`。写在构造方法上时，它表示让Spring通过该构造方法提供依赖。在只有一个构造方法时，写与不写的注入结果相同，所以本课程主线省略它。

本章只要求能够读懂这种写法，不讨论多个构造方法的选择规则，也不改用字段注入。

## 七、Controller怎样处理请求

完整Controller上使用了两个第3章已经出现的Web注解：

```java
@RestController
public class EmployeeController {
```

`@RestController` 来自 `org.springframework.web.bind.annotation`，写在类上。它一方面让这个类参与Web请求处理，另一方面也使它成为组件扫描能够发现并交给Spring管理的Controller Bean。

```java
@GetMapping("/employees/sample-name")
public String getSampleEmployeeName() {
    return employeeService.getSampleEmployeeName();
}
```

`@GetMapping` 来自同一个包，写在方法上。字符串参数 `/employees/sample-name` 是必须匹配的请求路径；收到对应GET请求时，Spring MVC调用下面的无参数方法。

Controller方法通过字段中的Service引用调用 `getSampleEmployeeName()`，得到 `String` 结果。因为类上使用了 `@RestController`，返回值会作为HTTP响应正文写回客户端。

这一方法只完成请求入口和调用转交，没有在Controller中重复写 `"Suzuki"`，也没有使用 `new EmployeeService()`。

## 八、启动时Spring怎样连接两个对象

第3章已经讲过：启动类上的 `@SpringBootApplication` 包含组件扫描功能，默认从启动类所在包开始扫描其子包。

本项目启动类位于：

```text
com.example.employee
```

本章两个类分别位于：

```text
com.example.employee.controller.EmployeeController
com.example.employee.service.EmployeeService
```

它们都处于根包下面。应用启动时的简化过程是：

```text
SpringApplication.run(...)
  → 创建Spring容器
  → 从com.example.employee开始组件扫描
  → 发现带有@Service的EmployeeService
  → 创建EmployeeService Bean
  → 发现带有@RestController的EmployeeController
  → 找到构造方法需要的EmployeeService Bean
  → 把Service传入构造方法并创建Controller Bean
  → 启动完成，等待HTTP请求
```

收到请求后的过程是：

```text
GET /employees/sample-name
  → Spring MVC调用EmployeeController.getSampleEmployeeName()
  → Controller调用EmployeeService.getSampleEmployeeName()
  → Service返回"Suzuki"
  → Controller把结果返回给Spring MVC
  → 客户端收到200和Suzuki
```

启动过程解决“对象怎样创建和连接”，请求过程解决“方法怎样调用和返回”。本章不把二者混为同一个步骤。

## 九、在Eclipse中运行并用Postman验证

保存代码，在Eclipse中右键 `EmployeeManagementApiApplication.java`，选择 **Run As → Java Application**。Console应显示应用成功启动；如果Controller需要的Service没有注册为Bean，应用会在建立上下文时失败，因此不能继续发送请求。

应用启动后，在Postman依次发送：

| HTTP方法 | URL | 请求参数 | 请求体 | 预期状态码 | 预期响应体 |
| --- | --- | --- | --- | ---: | --- |
| GET | `http://localhost:8080/employees/sample-name` | 无 | 无 | 200 | `Suzuki` |
| GET | `http://localhost:8080/health` | 无 | 无 | 200 | `OK` |

在Postman中选择GET、填写URL并点击 **Send**，然后记录响应区域中的状态码和Body。新接口成功只能证明新增调用可用；重新检查 `/health` 是为了确认本次修改没有破坏已有功能。验证结束后，在Eclipse Console中停止当前进程。

## 十、主动制造一次注入失败

完成正常验证后，临时删除 `EmployeeService` 类上的 `@Service`：

```java
public class EmployeeService {
```

保存后在Eclipse中重新启动应用。此时Java类仍然存在，但Spring组件扫描失去了把它注册为Bean的依据。创建 `EmployeeController` 时，容器找不到构造参数需要的 `EmployeeService` Bean，因此应用会启动失败，原因显示在Console中。

阅读错误时按下面顺序定位：

1. 找到哪个对象创建失败；
2. 查看它的哪个构造参数无法满足；
3. 确认所需类型是否带有正确组件注解；
4. 确认类是否位于启动类根包的子包中。

记录失败现象后，必须恢复 `@Service`，重新启动并确认Console出现启动成功日志，再用Postman回归两个接口。不要把故障状态留给下一章。

## 十一、同一类型出现多个Bean时怎么办

当前Employee主线中，每一种依赖都只有一个候选Bean，构造器按类型就能完成注入。真实项目中，一个接口可能有多个实现；此时Spring需要进一步知道应该选择哪一个，否则应用会在启动阶段报告候选不唯一。

现阶段只需要能够识别“缺少候选”和“候选过多”是两类不同问题。`@Qualifier`、`@Primary`、Bean名称及 `NoUniqueBeanDefinitionException` 的完整独立实验，放在附录[多个Bean候选的选择与排错](../appendix/A06_multiple_bean_candidates.md)中。

## 十二、常见问题与Review

| 现象或代码 | 原因 | 修正或Review意见 |
| --- | --- | --- |
| 编译提示找不到 `EmployeeService` | 文件目录、包声明或import不一致 | 核对三者是否都使用 `com.example.employee.service` |
| 启动提示缺少 `EmployeeService` Bean | 缺少 `@Service`，或类不在扫描范围 | 检查组件注解和启动类根包位置 |
| Controller中出现 `new EmployeeService()` | Controller承担了依赖创建责任 | 删除手动创建，通过构造方法声明依赖 |
| 构造参数有对象但字段仍无法使用 | 漏写 `this.employeeService = employeeService` | 补上构造赋值并重新构建 |
| Controller直接返回业务固定值 | 业务处理写进请求入口 | 让Service返回业务结果，Controller只调用和返回 |
| 新接口成功但旧接口未检查 | 缺少回归确认 | 同时保存新接口和 `/health` 的状态码与正文 |

Review这两个文件时，不要只确认“注解是否存在”。还要沿着下面的方向阅读：

```text
请求路径
  → Controller方法
  → Controller构造方法中的依赖
  → Service Bean
  → Service业务方法
  → 返回结果
```

只要其中任意一段名称、类型或职责不一致，代码就可能编译失败、启动失败，或者虽然能运行但分层职责混乱。

## 十三、操作练习

练习按顺序完成。每次修改后都要保存代码、在Eclipse重新启动，再用Postman请求接口。

### 练习1：修改Service返回值

把 `EmployeeService` 中的返回值从 `"Suzuki"` 改为 `"Tanaka"`。

验收结果：

- `GET /employees/sample-name` 返回200和 `Tanaka`；
- `GET /health` 仍返回200和 `OK`；
- Controller中没有出现员工姓名固定值。

### 练习2：按照小规格增加问候接口

改修规格：

| 项目 | 内容 |
| --- | --- |
| 用途 | 取得示例员工问候语 |
| HTTP方法 | `GET` |
| 路径 | `/employees/sample-greeting` |
| 请求参数 | 无 |
| 成功状态 | `200 OK` |
| 响应正文 | `Hello, Tanaka` |

修改要求：

1. 在 `EmployeeService` 中新增无参数方法 `getSampleEmployeeGreeting()`；
2. 该方法通过调用 `getSampleEmployeeName()` 取得姓名，再返回问候语；
3. 在 `EmployeeController` 中新增同名方法，并使用 `@GetMapping("/employees/sample-greeting")`；
4. Controller只能调用Service，不能直接拼接问候语，也不能 `new EmployeeService()`。

修改前先写出影响文件：

```text
service/EmployeeService.java
controller/EmployeeController.java
```

完成后保存三条自测证据：

| 请求 | 预期状态 | 预期正文 |
| --- | --- | --- |
| `GET /employees/sample-greeting` | 200 | `Hello, Tanaka` |
| `GET /employees/sample-name` | 200 | `Tanaka` |
| `GET /health` | 200 | `OK` |

提交Review前再次确认：路径与规格一致，问候语组合位于Service，Controller没有创建Service，三个接口的状态码和正文都有记录。

### 练习3：保留注入失败与恢复证据

按照第十节临时删除 `@Service`，记录：

1. 创建失败的对象；
2. 无法满足的构造参数类型；
3. 恢复的文件和注解；
4. 恢复后Eclipse启动成功及接口回归结果。

这项练习考查的不是记忆错误全文，而是能否沿依赖关系找到缺少的Bean。

## 十四、本章稳定状态

完成练习并恢复故障实验后，工程应保持：

```text
src/main/java/com/example/employee/
├── EmployeeManagementApiApplication.java
├── controller/
│   ├── EmployeeController.java
│   └── HealthController.java
└── service/
    └── EmployeeService.java
```

此时你应能够解释：

1. `EmployeeController` 为什么依赖 `EmployeeService`；
2. Spring容器和Bean分别是什么；
3. `@Service` 在什么时候产生什么结果；
4. Spring从哪里取得Controller构造方法的参数；
5. 为什么Controller中不写 `new EmployeeService()`；
6. 缺少Service Bean时怎样沿构造参数定位问题；

下一章将在这个稳定的 `Controller → Service` 结构上定义接口数据边界。请求数据对象、响应数据对象和数据库对象承担不同职责，但不会改变本章已经建立的对象注入方式。
