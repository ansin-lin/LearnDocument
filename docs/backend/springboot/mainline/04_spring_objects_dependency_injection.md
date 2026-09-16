# 第4章 Spring怎样创建并连接各层对象

> 本章目标：在第3章工程中完成 `Controller → Service` 调用，理解对象为什么由Spring创建、Service为什么通过构造方法传入Controller，并能定位“没有候选Bean”和“存在多个候选Bean”两类注入问题。

第2章已经说明Controller负责接收请求，Service负责处理业务。现在要解决一个具体问题：Controller需要调用Service，这个Service对象应该由谁创建？

本章只围绕下面一条主线展开：

```text
Controller需要Service
  → 不由Controller手动创建Service
  → Spring创建并管理Service对象
  → Spring通过构造方法把Service交给Controller
  → Controller调用Service并返回结果
```

完成Employee主线示例后，本章还会使用一个可删除的Payment独立实验，观察同一接口有两个实现时Spring怎样选择Bean。实验不会修改 `EmployeeService` 或员工接口。

## 一、开始状态与完成结果

继续使用第3章创建的 `employee-management-api` 工程。开始前应确认：

- `GET /health` 返回状态码200和正文 `OK`；
- 启动类位于根包 `com.example.employee`；
- `pom.xml` 已包含 `spring-boot-starter-web`；
- 项目可以执行 `mvnw.cmd clean test`。

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

这一接口暂时使用固定字符串，不接收请求数据，也不访问数据库。本章的学习重点是对象创建和连接。完成后还应能在独立实验中观察Qualifier、Primary和候选不唯一三种状态，并把实验文件清理干净。

## 二、完整示例

先完成两个文件并运行，再从第三节开始依次理解代码。文件名、包名和目录必须一致。

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

## 九、构建、运行与验证

在PowerShell中进入包含 `pom.xml` 的项目根目录，先执行测试：

```powershell
.\mvnw.cmd clean test
```

第3章生成的 `contextLoads()` 会尝试加载完整Spring应用。如果Controller需要的Service没有注册为Bean，应用上下文无法创建，测试就会失败。成功时应看到：

```text
BUILD SUCCESS
```

然后启动应用：

```powershell
.\mvnw.cmd spring-boot:run
```

另开一个PowerShell窗口，请求新接口：

```powershell
$response = Invoke-WebRequest `
    -Uri "http://localhost:8080/employees/sample-name"
$response.StatusCode
$response.Content
```

预期结果：

```text
200
Suzuki
```

再回归验证第3章接口：

```powershell
$healthResponse = Invoke-WebRequest `
    -Uri "http://localhost:8080/health"
$healthResponse.StatusCode
$healthResponse.Content
```

预期结果：

```text
200
OK
```

新接口成功只能证明新增调用可用；重新检查 `/health` 是为了确认本次修改没有破坏已有功能。验证结束后，在启动应用的窗口按 `Ctrl+C` 停止服务。

## 十、主动制造一次注入失败

完成正常验证后，临时删除 `EmployeeService` 类上的 `@Service`：

```java
public class EmployeeService {
```

再次执行：

```powershell
.\mvnw.cmd clean test
```

此时Java类仍然存在，但Spring组件扫描失去了把它注册为Bean的依据。创建 `EmployeeController` 时，容器找不到构造参数需要的 `EmployeeService` Bean，因此应用上下文加载失败。

阅读错误时按下面顺序定位：

1. 找到哪个对象创建失败；
2. 查看它的哪个构造参数无法满足；
3. 确认所需类型是否带有正确组件注解；
4. 确认类是否位于启动类根包的子包中。

记录失败现象后，必须恢复 `@Service`，重新执行 `clean test` 并确认 `BUILD SUCCESS`。不要把故障状态留给下一章。

## 十一、同一接口有多个Bean时怎样选择

前面的 `EmployeeService` 只有一个实现，Spring按照类型就能找到唯一对象。既存项目中，一个接口可能有多种实现，例如信用卡支付和银行转账都实现 `PaymentService`：

```text
PaymentService
├── CreditPaymentService Bean
└── BankPaymentService Bean
```

如果Controller只声明“需要一个 `PaymentService`”，候选对象却有两个，Spring不能替业务决定使用哪一种。下面先完成独立实验，再解释选择规则。

### 1. 完整的多Bean实验

实验文件放在独立的 `com.example.employee.dilab` 包中：

```text
src/main/java/com/example/employee/dilab/
├── PaymentService.java
├── CreditPaymentService.java
├── BankPaymentService.java
└── PaymentDemoController.java
```

这些文件位于启动类根包下面，会被当前项目的组件扫描发现；它们只用于观察依赖注入，不属于Employee Management业务。实验结束后会删除整个 `dilab` 包。

#### PaymentService.java

```java
package com.example.employee.dilab;

public interface PaymentService {

    String getPaymentMethod();
}
```

#### CreditPaymentService.java

```java
package com.example.employee.dilab;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
@Qualifier("credit")
public class CreditPaymentService implements PaymentService {

    @Override
    public String getPaymentMethod() {
        return "CREDIT";
    }
}
```

#### BankPaymentService.java

```java
package com.example.employee.dilab;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
@Qualifier("bank")
public class BankPaymentService implements PaymentService {

    @Override
    public String getPaymentMethod() {
        return "BANK";
    }
}
```

#### PaymentDemoController.java

```java
package com.example.employee.dilab;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PaymentDemoController {

    private final PaymentService paymentService;

    public PaymentDemoController(
            @Qualifier("credit") PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @GetMapping("/di-lab/payment-method")
    public String getPaymentMethod() {
        return paymentService.getPaymentMethod();
    }
}
```

先执行测试并启动项目：

```powershell
.\mvnw.cmd clean test
.\mvnw.cmd spring-boot:run
```

另开PowerShell窗口执行：

```powershell
$response = Invoke-WebRequest `
    -Uri "http://localhost:8080/di-lab/payment-method"
$response.StatusCode
$response.Content
```

预期结果：

```text
200
CREDIT
```

两个Service都是 `PaymentService` 类型的候选Bean。构造参数上的 `@Qualifier("credit")` 把候选范围缩小到带有 `credit` 限定值的Bean，因此Controller收到 `CreditPaymentService`。

### 2. @Qualifier解决什么问题

`@Qualifier` 的完整名称是 `org.springframework.beans.factory.annotation.Qualifier`。它可以写在组件类和注入参数等位置。本例分成两端：

```java
@Qualifier("credit")
public class CreditPaymentService implements PaymentService {
```

类上的注解给这个候选Bean附加 `credit` 限定值。

```java
public PaymentDemoController(
        @Qualifier("credit") PaymentService paymentService) {
```

构造参数上的注解要求Spring只在 `PaymentService` 候选中选择具有同一限定值的Bean。字符串必须准确匹配；写成 `@Qualifier("cash")` 时没有符合条件的Bean，应用仍会启动失败。

这里仍然是构造器注入。`@Qualifier` 只负责缩小候选范围，不负责创建对象，也不是从HTTP请求读取的参数。

### 3. Bean名称是什么

每个Bean在容器中都有名称。没有显式指定名称时，Spring通常根据类名生成默认名称：

| Bean类型 | 本例默认Bean名称 |
| --- | --- |
| `CreditPaymentService` | `creditPaymentService` |
| `BankPaymentService` | `bankPaymentService` |
| `PaymentDemoController` | `paymentDemoController` |

也可以写成 `@Service("creditPayment")` 显式指定名称。Bean名称用于容器内部标识和按名称查找；类名表示Java类型，`credit` 则是本例主动声明的Qualifier值，三者不要混为一谈。

Spring在没有更明确选择条件时，可以把注入点名称与Bean名称进行后备匹配，但这还受参数名是否保留等编译条件影响。业务代码有多个实现时，应明确使用 `@Qualifier` 或 `@Primary`，不要仅靠构造参数碰巧与Bean同名。Spring官方也将Qualifier定义为“在按类型得到的候选中进一步缩小范围”，而不是单纯按名称取得对象，参见[Spring Framework的Qualifier说明](https://docs.spring.io/spring-framework/reference/core/beans/annotation-config/autowired-qualifiers.html)。

### 4. @Primary提供默认实现

当一个实现是大多数调用方的默认选择时，可以在该实现上使用 `@Primary`。前面的完整Payment代码保持不变，只对 `CreditPaymentService.java` 增加下面两处差分：

```java
package com.example.employee.dilab;

@Service
@Primary
public class CreditPaymentService implements PaymentService {
    // getPaymentMethod()保持不变
}
```

其中还需要新增 `import org.springframework.context.annotation.Primary;`。同时只修改Controller构造器，移除参数上的Qualifier：

```java
public PaymentDemoController(PaymentService paymentService) {
    this.paymentService = paymentService;
}
```

`@Primary` 的完整名称是 `org.springframework.context.annotation.Primary`，写在候选实现类上。本例存在两个 `PaymentService` Bean，但只有 `CreditPaymentService` 被标记为主要候选，因此未指定Qualifier的注入点默认得到它。

重新执行 `clean test`、启动项目并请求 `/di-lab/payment-method`，预期仍然返回200和 `CREDIT`。相同结果来自不同规则：前一种写法是当前注入点明确指定credit候选，后一种写法是没有特别指定时采用主要候选。

一个类型不应同时出现两个主要候选，否则又会失去唯一选择。`@Primary` 表达“默认用谁”，`@Qualifier` 表达“这个注入点明确需要哪一类候选”：

| 场景 | 选择方式 |
| --- | --- |
| 全项目通常使用一个默认实现 | 在默认实现上使用 `@Primary` |
| 不同调用方明确使用不同实现 | 在实现和注入点使用匹配的 `@Qualifier` |
| 只有一个同类型Bean | 直接按类型构造器注入 |

可以把本章范围内的选择过程简化为：

```text
先按PaymentService类型寻找候选
  → 注入点有Qualifier：按限定值缩小候选
  → 没有Qualifier且存在唯一Primary：使用主要候选
  → 没有明确规则：可能尝试注入点名称与Bean名称的后备匹配
  → 最终仍有多个候选：启动失败
```

Qualifier已经明确选中某类候选时，不会因为另一个不匹配的Bean带有 `@Primary` 就改选另一个实现。真实项目还存在泛型限定、集合注入等规则，本章不展开；新人先掌握“类型、明确限定、默认候选、歧义失败”这条主线。

### 5. 主动制造NoUniqueBeanDefinitionException

保持两个实现都带有 `@Service`，删除 `CreditPaymentService` 上的 `@Primary`，并让Controller构造参数不带 `@Qualifier`：

```java
public PaymentDemoController(PaymentService paymentService) {
    this.paymentService = paymentService;
}
```

再次执行：

```powershell
.\mvnw.cmd clean test
```

应用上下文会加载失败。日志外层可能先显示 `UnsatisfiedDependencyException`，继续查看最深层原因，应能看到 `NoUniqueBeanDefinitionException`，并列出类似下面的两个候选名称：

```text
creditPaymentService
bankPaymentService
```

`NoUniqueBeanDefinitionException` 表示需要一个Bean时找到了多个同类型候选，并不表示Bean完全不存在。官方定义可参考[Spring Framework API](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/beans/factory/NoUniqueBeanDefinitionException.html)。定位时按下面顺序检查：

1. 哪个Bean创建失败；
2. 哪个构造参数需要唯一对象；
3. 日志列出了哪些同类型候选；
4. 业务规格是否规定默认实现；
5. 应使用明确的Qualifier，还是确实存在全局默认实现。

不要看到异常后随意删除一个实现。两个实现可能都被其他业务使用，真正缺少的是当前注入点的选择规则。

### 6. 恢复Employee主线状态

完成三种实验并保存结果后，停止应用，删除临时的 `src/main/java/com/example/employee/dilab` 目录，再执行：

```powershell
.\mvnw.cmd clean test
```

最终必须再次看到 `BUILD SUCCESS`，并确认员工接口和 `/health` 没有变化。实验中的Payment类型不进入后续章节。

## 十二、常见问题与Review

| 现象或代码 | 原因 | 修正或Review意见 |
| --- | --- | --- |
| 编译提示找不到 `EmployeeService` | 文件目录、包声明或import不一致 | 核对三者是否都使用 `com.example.employee.service` |
| 启动提示缺少 `EmployeeService` Bean | 缺少 `@Service`，或类不在扫描范围 | 检查组件注解和启动类根包位置 |
| Controller中出现 `new EmployeeService()` | Controller承担了依赖创建责任 | 删除手动创建，通过构造方法声明依赖 |
| 构造参数有对象但字段仍无法使用 | 漏写 `this.employeeService = employeeService` | 补上构造赋值并重新构建 |
| Controller直接返回业务固定值 | 业务处理写进请求入口 | 让Service返回业务结果，Controller只调用和返回 |
| 新接口成功但旧接口未检查 | 缺少回归确认 | 同时保存新接口和 `/health` 的状态码与正文 |
| 一个接口有两个实现，启动出现 `NoUniqueBeanDefinitionException` | 注入点没有唯一候选 | 根据业务规则使用 `@Qualifier` 或标记唯一的 `@Primary` |
| 只因参数名碰巧匹配Bean名称而能启动 | 依赖隐式名称后备匹配，意图不清楚 | 多实现时明确写Qualifier或Primary |
| 同一类型有两个 `@Primary` | 主要候选仍不唯一 | 每组候选最多保留一个真正的默认实现 |

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

练习按顺序完成。每次修改后都要先构建，再启动并请求接口。

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
4. 恢复后 `clean test` 的结果。

这项练习考查的不是记忆错误全文，而是能否沿依赖关系找到缺少的Bean。

### 练习4：比较三种多Bean状态

使用第十一节的独立实验，依次记录：

1. `@Qualifier("credit")` 时接口返回什么；
2. 改用唯一 `@Primary` 时接口返回什么；
3. 两者都不使用时，日志中的依赖类型、候选数量和候选名称；
4. 为什么不能通过删除另一个业务实现来掩盖选择规则缺失；
5. 删除 `dilab` 后主线测试和原接口是否恢复正常。

提交的证据必须同时包含两次成功状态、一次预期启动失败和最终恢复成功，不能把故障实验留在工程中。

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
7. 一个接口存在多个实现时为什么不能只按类型选择；
8. `@Qualifier`、`@Primary` 和Bean名称分别解决什么问题；
9. 怎样从错误链中识别 `NoUniqueBeanDefinitionException` 并找到候选Bean。

下一章将在这个稳定的 `Controller → Service` 结构上定义接口数据边界。请求数据对象、响应数据对象和数据库对象承担不同职责，但不会改变本章已经建立的对象注入方式。
