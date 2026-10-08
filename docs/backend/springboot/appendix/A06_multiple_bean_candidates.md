# A06 多个Bean候选的选择与排错

本附录只讲一个问题：同一接口存在多个Spring Bean时，容器怎样选择构造器需要的对象，以及候选不唯一时怎样定位。它不修改Employee主线，所有代码放在可删除的 `dilab` 包中。

## 一、前置知识与完成结果

开始前应完成第4章，能够解释Bean、Spring容器和构造器注入。完成本附录后，应能：

- 区分没有候选Bean与候选Bean过多；
- 根据业务意图选择 `@Qualifier` 或唯一的 `@Primary`；
- 从异常链识别 `NoUniqueBeanDefinitionException`；
- 删除实验文件并用主线测试确认工程已恢复。
## 二、完整实验：同一接口有多个Bean时怎样选择

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

### 6. 清理实验并恢复主线

完成三种实验并保存结果后，停止应用，删除临时的 `src/main/java/com/example/employee/dilab` 目录，再执行：

```powershell
.\mvnw.cmd clean test
```

最终必须再次看到 `BUILD SUCCESS`，并确认员工接口和 `/health` 没有变化。实验中的Payment类型不进入后续章节。

## 三、附录练习

依次保存三组证据：显式Qualifier成功、唯一Primary成功、没有选择规则时启动失败。最后删除 `dilab` 实验文件，执行 `clean test`，确认Employee主线恢复。Review时必须说明选择规则表达的业务意图，不能只记录“加上某个注解就能启动”。