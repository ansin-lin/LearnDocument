# A11-3 使用AOP统一记录方法日志

本附录建议在完成[第11章：用日志定位接口故障](../mainline/11_logging_and_diagnosis.md)和[A11-2 Filter与请求处理执行时机](A11_2_filter_execution_timing.md)后学习。第11章已经用Filter记录HTTP请求、用Service记录业务事实、用Controller Advice记录异常；这里增加一种新的观察范围：围绕指定Spring Bean方法统一记录执行结果和耗时。

本附录使用Spring Boot 4.1.x提供的 `spring-boot-starter-aspectj`。示例是独立实验，不要求把所有Service都加入切面，也不允许记录完整参数、返回对象、密码、Token、Cookie、Session ID、姓名或邮箱。

## 一、AOP日志解决什么问题

假设多个Service方法都需要测量耗时，如果每个方法都手写下面代码，会出现重复：

```java
long startedAt = System.nanoTime();
try {
    // 原来的业务逻辑
} finally {
    long elapsedMs =
            (System.nanoTime() - startedAt) / 1_000_000;
    log.debug("service_elapsed elapsedMs={}", elapsedMs);
}
```

AOP可以把“在一批方法调用前后执行相同处理”集中到一个切面中：

```text
调用方
  → Spring代理
      → AOP Advice开始计时
          → 目标Service方法
      ← AOP Advice计算结果、记录耗时
  ← 返回原结果或继续抛出原异常
```

适合AOP的通常是跨多个Bean或方法重复出现的技术关注点，例如方法耗时、审计入口标记或框架已经实现的事务和方法权限。AOP不应替代业务代码：

- “员工新增成功，员工ID是多少”属于Service明确知道的业务事实；
- HTTP method、path、status和requestId属于Filter范围；
- 异常到HTTP响应的转换属于Controller Advice；
- “这个被标记的方法执行了多久”可以由AOP统一处理。

## 二、先建立五个核心概念

| 概念 | 本附录中的含义 | Employee示例 |
| --- | --- | --- |
| Aspect，切面 | 集中保存横切处理的类 | `MethodLoggingAspect` |
| Join Point，连接点 | Spring AOP能够增强的方法执行点 | 调用 `EmployeeServiceImpl.findById()` |
| Pointcut，切点 | 从连接点中选出哪些方法 | 选择带 `@MethodLog` 的方法 |
| Advice，通知 | 命中切点后实际执行的附加逻辑 | 开始计时、调用原方法、记录结果 |
| Proxy，代理 | Spring放在调用方与目标Bean之间的对象 | Controller实际调用Service代理 |

Spring AOP主要围绕Spring Bean的方法执行。它不是修改Java源文件，也不是拦截任意 `new` 出来的对象。调用必须经过代理，Advice才有机会执行。

## 三、Filter日志、业务日志和AOP日志的边界

| 日志 | 观察单位 | 适合字段 | 不适合字段 |
| --- | --- | --- | --- |
| Filter请求日志 | 一次HTTP请求 | requestId、method、path、status、总耗时 | 业务是否真正成功、完整Body |
| Service业务日志 | 一次业务结果 | 事件名、员工ID、状态变化 | 所有方法统一进入/退出 |
| Controller Advice异常日志 | 一次最终异常处理 | 路径、异常类型、必要业务ID、受控堆栈 | 每个正常方法耗时 |
| AOP方法日志 | 一次匹配的Bean方法调用 | 稳定事件名、方法名、结果类别、耗时 | 完整参数、返回对象、事务提交证明 |

同一次请求可以有一条Filter日志、一条业务日志和一条AOP耗时日志，因为三者表达不同事实。但同一异常堆栈不应在Service、AOP和Advice各打印一遍。本附录的切面只记录异常类型，不打印异常对象；最终堆栈仍由第11章的异常边界记录一次。

## 四、增加AOP依赖

在 `pom.xml` 的 `dependencies` 中增加：

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-aspectj</artifactId>
</dependency>
```

Spring Boot父POM管理版本，本课程不单独填写 `<version>`。在Eclipse保存POM后执行 **Maven → Update Project**。

Spring Boot检测到AspectJ相关类后会自动配置Spring AOP代理，本例不需要额外添加 `@EnableAspectJAutoProxy`。这里使用的是“Spring AOP代理＋AspectJ注解和切点表达式”，不是AspectJ编译期织入或加载期织入。

依赖加入后，建议先启动一次原工程，确认CRUD仍能运行，再开始添加注解和切面。这样如果后面启动失败，可以区分是依赖问题还是代码问题。

## 五、案例一：只为一个方法记录耗时

### 1. 新建最小切面完整文件

新建：

```text
src/main/java/com/example/employee/logging/ServiceTimingAspect.java
```

```java
package com.example.employee.logging;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class ServiceTimingAspect {

    private static final Logger log =
            LoggerFactory.getLogger(ServiceTimingAspect.class);

    @Around("execution(* com.example.employee.service.impl.EmployeeServiceImpl.findById(..))")
    public Object measure(ProceedingJoinPoint joinPoint)
            throws Throwable {
        long startedAt = System.nanoTime();
        try {
            return joinPoint.proceed();
        } finally {
            long elapsedMs =
                    (System.nanoTime() - startedAt) / 1_000_000;
            log.debug(
                    "service_method_completed method={} elapsedMs={}",
                    joinPoint.getSignature().toShortString(),
                    elapsedMs);
        }
    }
}
```

### 2. 每个首次出现的对象和方法

| 写法 | 类型或来源 | 当前作用 | 返回或运行效果 |
| --- | --- | --- | --- |
| `ProceedingJoinPoint` | AspectJ API对象 | 表示当前被 `@Around` 包围的方法调用 | 可取得签名并继续目标调用 |
| `joinPoint.proceed()` | `ProceedingJoinPoint`方法 | 执行代理链中的下一个Advice或目标方法 | 返回目标结果，也可能抛出原异常 |
| `getSignature()` | Join Point方法 | 取得当前方法签名描述 | 返回 `Signature` |
| `toShortString()` | `Signature`方法 | 生成较短的方法标识 | 例如 `EmployeeServiceImpl.findById(..)` |
| `System.nanoTime()` | Java时间测量方法 | 取得适合计算持续时间的单调时间值 | 返回纳秒计数，不是业务日期时间 |

`proceed()` 必须恰好执行一次并把结果原样返回。遗漏它会导致目标Service根本不执行；执行两次会让查询或写入重复发生；用其他对象替换返回值会改变原接口行为。

`finally` 使成功和异常路径都能计算耗时，但这里无法区分结果。下一案例会显式区分成功、失败和慢调用。

### 3. 切点表达式逐段解释

```text
execution(* com.example.employee.service.impl.EmployeeServiceImpl.findById(..))
          │ │                                                        │
          │ │                                                        └ 任意参数列表
          │ └ 类的完整名称和方法名
          └ 任意返回类型
```

这个表达式只匹配指定实现类的 `findById`。它适合第一次观察，但类名或方法名变化时必须同步修改切点。直接写 `execution(* com.example.employee..*(..))` 会匹配范围过大，可能把Controller、Service、Mapper和配置类都卷入日志。

### 4. 开启这个切面的DEBUG日志

在 `application.yml` 的 `logging.level` 中临时增加：

```yaml
logging:
  level:
    com.example.employee.logging.ServiceTimingAspect: DEBUG
```

这只是局部开启切面Logger，不需要把 `root` 调成DEBUG。配置合并时应把这一行放进主线已有的 `logging.level`，不要再创建第二个顶层 `logging`。

### 5. 验证

在Eclipse启动应用，用Postman发送：

- HTTP方法：GET
- URL：`{{baseUrl}}/employees/1001`
- 参数和Body：无
- 预期状态：200
- 预期AOP日志：

```text
service_method_completed method=EmployeeServiceImpl.findById(..) elapsedMs=...
```

日志pattern已有MDC字段，因此这条AOP日志会自动带上Filter设置的requestId。切面不需要自己读取HTTP请求，也不需要把requestId作为Service参数传入。

## 六、案例二：用自定义注解明确选择方法

固定类名切点适合观察原理，但真实项目更需要可控范围。下面建立 `@MethodLog`：只有明确标记的方法才进入日志切面。

### 1. 新建注解完整文件

新建：

```text
src/main/java/com/example/employee/logging/MethodLog.java
```

```java
package com.example.employee.logging;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MethodLog {

    String value();

    long warnThresholdMs() default 1000;
}
```

### 2. 注解及属性说明

| 注解或属性 | 标注位置、处理者 | 可接受值 | 当前作用 |
| --- | --- | --- | --- |
| `@Target` | 注解类型；Java编译器 | 一个或多个 `ElementType` | `METHOD`限制 `@MethodLog` 只能标在方法上 |
| `@Retention` | 注解类型；Java/JVM | `SOURCE`、`CLASS`、`RUNTIME` | `RUNTIME`让Spring AOP运行时能够读取它 |
| `@Documented` | 注解类型；Javadoc工具 | 无属性 | 生成文档时保留该注解信息 |
| `value` | `@MethodLog`必填属性 | 稳定、低基数的事件名字符串 | 标识日志事件，不使用自由变化的参数值 |
| `warnThresholdMs` | `@MethodLog`可选属性 | 非负毫秒整数 | 超过阈值时记WARN；默认1000毫秒 |

完整一行写法：

```java
@MethodLog(value = "employee_find_by_id", warnThresholdMs = 500)
```

只有 `value` 时可使用简写：

```java
@MethodLog("employee_find_by_id")
```

### 3. 在目标Service方法上使用

在 `EmployeeServiceImpl` 的import区域增加：

```java
import com.example.employee.logging.MethodLog;
```

在现有方法上增加一行，方法内部代码不变：

```java
@Override
@MethodLog(
        value = "employee_find_by_id",
        warnThresholdMs = 500)
public EmployeeResponse findById(Long id) {
    // 保留第11章完整实现
}
```

`@Override` 由Java编译器检查该方法确实实现接口声明；`@MethodLog` 由后面的AOP切面在运行时读取。两个注解处理者和作用完全不同。

## 七、案例三：完整实现成功、失败和慢调用日志

删除案例一的 `ServiceTimingAspect.java`，改为新建下面文件，避免两个切面同时匹配造成重复日志：

```text
src/main/java/com/example/employee/logging/MethodLoggingAspect.java
```

```java
package com.example.employee.logging;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class MethodLoggingAspect {

    private static final Logger log =
            LoggerFactory.getLogger(MethodLoggingAspect.class);

    @Around(
            value = "@annotation(methodLog)",
            argNames = "methodLog")
    public Object logMethod(
            ProceedingJoinPoint joinPoint,
            MethodLog methodLog) throws Throwable {
        long startedAt = System.nanoTime();
        String methodName =
                joinPoint.getSignature().toShortString();

        log.debug(
                "method_started event={} method={}",
                methodLog.value(),
                methodName);

        try {
            Object result = joinPoint.proceed();
            long elapsedMs = elapsedMs(startedAt);
            if (elapsedMs >= methodLog.warnThresholdMs()) {
                log.warn(
                        "method_completed event={} method={} "
                                + "outcome=success slow=true elapsedMs={}",
                        methodLog.value(),
                        methodName,
                        elapsedMs);
            } else {
                log.debug(
                        "method_completed event={} method={} "
                                + "outcome=success elapsedMs={}",
                        methodLog.value(),
                        methodName,
                        elapsedMs);
            }
            return result;
        } catch (Throwable exception) {
            long elapsedMs = elapsedMs(startedAt);
            log.debug(
                    "method_completed event={} method={} "
                            + "outcome=failed exceptionType={} elapsedMs={}",
                    methodLog.value(),
                    methodName,
                    exception.getClass().getSimpleName(),
                    elapsedMs);
            throw exception;
        }
    }

    private long elapsedMs(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000;
    }
}
```

### 1. `@Around("@annotation(methodLog)")` 怎样工作

`@annotation(methodLog)` 选择带 `@MethodLog` 的方法，并把当前方法上的注解实例绑定到Advice参数 `methodLog`。第一个参数是框架规定的 `ProceedingJoinPoint`，可以不写入 `argNames`；这里的 `argNames = "methodLog"` 明确注解绑定参数，使切面能够稳定读取 `value()` 和 `warnThresholdMs()`。

### 2. 为什么失败后必须重新抛出

```java
} catch (Throwable exception) {
    // 只记录受控字段
    throw exception;
}
```

如果catch后返回null或伪造成功对象，就会改变原有异常流程：第7章全局异常处理器无法生成正确404/409/500，第13章事务也可能错误提交。日志切面只能观察，不应吞掉或转换本例异常。

### 3. 为什么不在切面打印异常堆栈

第11章已经让全局异常边界记录最终系统异常堆栈。如果切面再次使用 `log.error("...", exception)`，一场故障会产生多份相同堆栈。这里DEBUG只记录异常类型和耗时，原异常继续传播；最终负责响应的边界仍记录一次必要堆栈。

### 4. 为什么不记录参数和返回值

`joinPoint.getArgs()` 能取得参数，但“能够取得”不等于“应该输出”。员工DTO、密码请求、Session对象、上传文件和返回列表都可能包含敏感数据或大量内容。通用切面很难理解每个字段的业务含义，因此默认只记录稳定事件名、方法名、结果类别和耗时。

## 八、本附录出现的AOP注解完整说明

| 注解 | 所属与标注位置 | 常用属性 | 当前写法和效果 |
| --- | --- | --- | --- |
| `@Aspect` | `org.aspectj.lang.annotation`；切面类 | 无常用属性 | `@Aspect` 把类声明为切面定义；仍需成为Spring Bean |
| `@Component` | Spring；组件类 | 可选Bean名称 | `@Component` 让组件扫描创建切面Bean |
| `@Around` | AspectJ注解；Advice方法 | `value`/`argNames` | 表达式选中方法，Advice必须调用并返回 `proceed()` 结果 |
| `@Pointcut` | AspectJ注解；空方法 | `value`/`argNames` | 给复杂或复用切点命名，本例主要使用内联表达式 |
| `@Order` | Spring；切面类 | `int value` | 调整多个Advisor顺序，数值越小优先级越高；不能据此宣称事务已提交 |
| `@Target` | Java；注解类型 | `ElementType[]` | 限制自定义注解可用位置 |
| `@Retention` | Java；注解类型 | `RetentionPolicy` | 决定注解保留到源码、class或运行时 |
| `@Documented` | Java；注解类型 | 无 | 让注解出现在生成文档中 |
| `@Override` | Java；重写方法 | 无 | 编译器检查接口/父类方法重写，与AOP无关 |

常见一行写法：

```java
@Around(value = "@annotation(methodLog)", argNames = "methodLog")
```

简单项目有时省略 `argNames`，由方法参数信息完成绑定；本例为了让新人明确对应关系而显式填写。

命名切点示例：

```java
@Pointcut("within(com.example.employee.service..*)")
public void employeeServiceMethods() {
}
```

`@Pointcut` 方法体必须为空，它只是给表达式起名。`within(...)` 按声明类型范围匹配；这个示例会覆盖service包下大量方法，因此不如 `@MethodLog` 精确，不要与注解切点同时应用到同一方法后重复记录。

## 九、常见切点表达式怎样阅读

| 表达式 | 选择范围 | 风险 |
| --- | --- | --- |
| `execution(* com.example.employee.service..*(..))` | service包及子包的任意方法执行 | 范围大，新方法会自动进入 |
| `within(com.example.employee.service..*)` | 声明在service包类型中的方法 | 同样容易覆盖过多 |
| `@annotation(com.example.employee.logging.MethodLog)` | 带指定方法注解的方法 | 精确，但必须记得标注 |
| `bean(employeeServiceImpl)` | 指定Bean名称 | 重命名Bean会影响匹配 |
| `args(java.lang.Long)` | 参数形状匹配 | 重载和类型变化会改变范围 |

切点属于运行行为配置，Review时必须确认“现在匹配哪些方法”和“未来新增方法是否会自动被匹配”。过宽切点可能突然增加日志量或把敏感接口纳入记录。

## 十、代理边界：为什么有时切面不执行

### 1. 同类内部调用

```java
public EmployeeResponse create(...) {
    // ...
    return findById(employee.getId());
}
```

如果 `create()` 在目标对象内部直接调用 `findById()`，调用使用当前对象的 `this`，通常不重新经过外部代理，因此 `findById()` 上的切面可能不执行。Controller直接调用Service代理的 `findById()` 才会命中。

不要为了触发日志而使用 `AopContext.currentProxy()` 或随意自注入。优先重新划分Bean责任，或把注解放在真实外部业务入口上。

### 2. 手工创建对象

```java
EmployeeServiceImpl service = new EmployeeServiceImpl(...);
```

这个对象不是Spring容器创建的代理对象，AOP不参与。普通Mockito纯单元测试手工创建Service时，也不应期待切面自动执行。

### 3. private、final和代理方式

Spring Boot默认通常使用CGLIB类代理。`private` 方法不能被子类覆盖，`final` 方法不能被Advice覆盖，`final` 类也不能建立所需子类代理。项目若切换到JDK动态代理，代理主要暴露接口方法。看到切面失效时，应检查实际代理类型、方法可见性和调用入口。

## 十一、切面、事务和方法权限的顺序

同一个Service方法可能同时经过：

```text
AOP方法日志
  ↔ @PreAuthorize方法权限
  ↔ @Transactional事务
  → 目标Service方法
```

多个Advisor的嵌套顺序会影响耗时范围：切面在事务外层时，可能把开启、提交或回滚时间计算进去；在事务内层时则可能不包含提交。方法正常返回也不等于数据库提交已经成功。

可以给切面类添加：

```java
@Order(100)
```

数值越小通常优先级越高、越靠外层进入，但其他框架Advisor也有自己的顺序配置。不能仅凭一个数字猜测完整顺序；应结合实际代理链、日志、事务失败实验和官方配置确认。主线不依赖AOP耗时日志证明提交。

## 十二、验证成功、失败、慢调用和自调用

把 `MethodLoggingAspect` Logger临时设为DEBUG：

```yaml
logging:
  level:
    com.example.employee.logging.MethodLoggingAspect: DEBUG
```

### 验证1：成功

- 方法：GET
- URL：`{{baseUrl}}/employees/1001`
- Body：无
- 预期状态：200
- 预期：同一requestId下出现started和`outcome=success`

### 验证2：业务失败

- 方法：GET
- URL：`{{baseUrl}}/employees/999999`
- Body：无
- 预期状态：404
- 预期：AOP出现 `outcome=failed exceptionType=EmployeeNotFoundException`，原全局处理器仍生成404；AOP不打印第二份堆栈

### 验证3：慢调用阈值

不要在正式业务代码长期加入 `Thread.sleep()`。如需个人实验，可临时把 `warnThresholdMs` 设为0，使当前调用必然满足WARN判断；验证后恢复500，并重新启动应用。

### 验证4：自调用

比较Controller直接GET详情与 `create()` 内部调用 `findById()` 的日志。记录哪些调用命中切面，并用代理边界解释差异，不要把差异误判为Logger等级问题。

## 十三、常见失败与修正

| 症状 | 常见原因 | 修正 |
| --- | --- | --- |
| 应用找不到AspectJ类 | 未加入Starter或Eclipse未更新Maven | 检查POM并更新项目 |
| 切面类存在但完全不执行 | 缺少 `@Aspect`/`@Component`、对象不是Bean、切点不匹配 | 从Bean创建和切点范围逐项确认 |
| Service执行两次 | Advice调用 `proceed()` 两次 | 保证恰好调用一次 |
| 接口返回null | Advice遗漏返回 `proceed()` 的结果 | 保存并原样返回结果 |
| 404变成200或500 | catch后吞异常或转换错误 | 原异常继续抛出 |
| 日志输出两份 | 案例一和案例三切面同时存在、切点重叠 | 删除旧实验切面或缩小范围 |
| 内部调用没有日志 | self-invocation绕过代理 | 把注解放在外部入口或重新划分Bean |
| 有AOP日志但没有requestId | 调用不在HTTP线程、Filter未覆盖或发生线程切换 | 根据入口决定是否应有关联编号 |
| 日志量突然增大 | 切点过宽或Logger长期DEBUG | 改为明确注解并恢复INFO |

## 十四、安全、性能和Review原则

### 安全边界

- 默认不记录 `getArgs()` 和返回对象；
- 不记录密码、Token、Authorization、Cookie、Session ID；
- 不把姓名、邮箱、地址等个人信息作为通用切面字段；
- 事件名使用固定有限集合，不能拼接用户输入；
- 异常堆栈在最终边界记录一次，并限制日志访问权限。

### 性能边界

- 每次方法调用都会增加代理、计时和日志判断成本；
- DEBUG关闭时仍应避免先序列化大对象；
- 高频getter、转换方法和Mapper细粒度方法不适合全部记录；
- 日志不能代替Micrometer指标和分布式追踪；需要延迟分位数、告警和跨服务链路时，应使用对应监控体系。

### Review清单

1. 为什么使用AOP，而不是Filter或显式业务日志？
2. 切点精确匹配哪些方法？新增方法会不会意外进入？
3. `proceed()` 是否恰好调用一次并原样返回？
4. 异常是否继续抛出，HTTP和事务语义是否保持？
5. 是否记录了参数、返回值或敏感数据？
6. 是否与Service、Filter、Advice重复记录？
7. Logger等级和慢调用阈值是否适合运行环境？
8. 自调用、手工new、private/final方法是否影响代理？
9. 与事务和权限Advisor的相对顺序是否经过验证？
10. Postman响应、requestId和日志是否形成可复核证据？

## 十五、实验完成状态与恢复

如果项目确有“只对显式标记方法记录耗时”的需求，可以保留：

```text
pom.xml
src/main/java/com/example/employee/logging/
├── MethodLog.java
└── MethodLoggingAspect.java
EmployeeServiceImpl.java中的目标注解
application.yml中的切面Logger等级
```

如果只是学习实验，应删除上述两个Java文件和Service上的 `@MethodLog`，移除临时Logger等级；若项目没有其他AOP用途，再删除 `spring-boot-starter-aspectj` 并更新Maven。之后重新启动，运行原CRUD并确认没有AOP日志、接口行为不变。

完成本附录的标准不是“所有方法都有进入退出日志”，而是能够选择精确切点、保持原返回和异常语义、避免敏感数据及重复堆栈，并解释代理边界造成的实际运行结果。

## 十六、官方参考资料

- [Spring Boot：Aspect-Oriented Programming](https://docs.spring.io/spring-boot/reference/features/aop.html)
- [Spring Boot：Starters](https://docs.spring.io/spring-boot/reference/using/build-systems.html#using.build-systems.starters)
- [Spring Framework：Aspect Oriented Programming](https://docs.spring.io/spring-framework/reference/core/aop.html)
- [Spring Framework：Proxying Mechanisms](https://docs.spring.io/spring-framework/reference/core/aop/proxying.html)
