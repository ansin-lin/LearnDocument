# B01 Spring共通处理机制：Filter、Interceptor、Controller Advice与AOP

本附录用于阅读既存Spring项目中的共通处理。开始前应完成第11章，能够使用 `RequestLoggingFilter`、requestId和全局异常处理器。本附录不改变Employee主线的最终代码；实验文件完成后要删除。

需要核对具体实现时，分别回到[第11章请求日志Filter](../mainline/11_logging_and_diagnosis.md)、[A11-2 Filter执行时机](A11_2_filter_execution_timing.md)、[第13章事务](../mainline/13_transaction_and_consistency.md)、[第16章权限](../mainline/16_authorization_and_data_scope.md)；本章只把这些机制放到同一次请求的观察图中。

## 一、先按执行位置区分机制

```text
HTTP Request
  → Servlet Filter（可能包围Security代理）
  → Spring Security Filter Chain（本身由Filter进入）
  → DispatcherServlet
  → HandlerInterceptor.preHandle
  → Controller
  → Service上的Spring Proxy / AOP
  → Mapper
  ← HandlerInterceptor.postHandle（正常返回）/ afterCompletion（完成阶段）
  ← MVC异常解析：匹配时由Controller Advice转换异常
  ← HTTP Response
```

这是用于定位的简化图，不是所有配置下绝对固定的源码调用栈。一个请求可能经过多个Filter、Interceptor和代理；实际先后还会受注册顺序、异常、错误分派和异步处理影响。

| 机制 | 主要位置 | 适合解决 | 不适合替代 |
| --- | --- | --- | --- |
| Servlet Filter | MVC之前的Servlet处理链 | requestId、HTTP访问日志、Security、请求包装 | Service业务规则 |
| HandlerInterceptor | Spring MVC的Handler调用前后 | Controller范围的计时、检查和清理 | 数据库事务、任意Bean方法 |
| Controller Advice | MVC异常解析与共通响应 | 业务异常到HTTP响应的转换 | Security在Controller前的拒绝 |
| AOP / Spring Proxy | Spring Bean方法调用 | 事务、方法权限、受控横切逻辑 | 原始HTTP报文处理 |

| 机制 | 作用范围与执行位置 | 依赖Controller | 需要Spring Bean | 常见配置 |
| --- | --- | --- | --- | --- |
| Filter（含Security链） | Servlet请求分派；进入MVC前，也包围下游返回 | 否 | Servlet Filter本身不必是Bean，Boot中常由Bean或注册器管理 | `FilterRegistrationBean`、组件注册、`SecurityFilterChain` |
| Interceptor | 匹配到的MVC Handler前后 | 是，准确说依赖MVC Handler | 实现对象可由配置手动创建，不必单独为Bean | `WebMvcConfigurer#addInterceptors` |
| Controller Advice | MVC异常解析等阶段，受适用范围限制 | 通常处理MVC链异常 | 是 | `@RestControllerAdvice`、`@ExceptionHandler` |
| AOP/Proxy | 命中切点且经过代理的Bean方法 | 否 | 是，目标需受Spring管理 | `@Transactional`、`@PreAuthorize`、`@Aspect` |

Filter适合请求入口日志但不适合业务事务；Interceptor适合已匹配Handler的计时但不适合认证主边界；Advice适合把MVC业务异常转为响应但不负责过滤器链提前拒绝；AOP适合方法横切处理但不直接处理原始HTTP。图省略Servlet容器、异常解析器的全部分支，不能按箭头推断所有项目中Filter的绝对顺序。

选择机制前先问：问题发生在HTTP入口、MVC Handler、异常转换，还是Bean方法调用。

### 本附录出现的注解由谁处理

| 注解 | 所属与标注位置 | 处理时机和作用 |
| --- | --- | --- |
| `@Order` | Spring Core；Bean类或Bean定义 | 容器组织有顺序的组件时提供优先级；本例让Filter尽早执行，数值越小优先级越高 |
| `@Configuration` | Spring；配置类 | 应用上下文启动时把类作为Bean定义和框架配置的来源 |
| `@Component` | Spring；组件类 | 组件扫描时把类注册为Bean；本附录的AOP切面使用它，Interceptor实验则故意不用 |
| `@RestController` | Spring MVC；Controller类 | 启动时注册请求处理器，并让返回值默认写入响应正文 |
| `@RestControllerAdvice` | Spring MVC；共通异常处理类 | 启动时注册面向REST Controller的Advice，异常解析阶段返回响应正文 |
| `@ExceptionHandler` | Spring MVC；Advice或Controller方法 | Controller调用抛出匹配异常时选择处理方法并生成响应 |
| `@Transactional` | Spring Transaction；Bean类或方法 | 调用经过Spring代理时，在目标方法前后建立事务边界并决定提交或回滚 |
| `@PreAuthorize` | Spring Security；Bean类或方法 | 启用方法安全后，代理在目标方法调用前计算授权表达式 |
| `@Aspect` | AspectJ注解，由Spring AOP读取；切面类 | 容器建立AOP代理时把该类识别为切面定义 |
| `@Before` | AspectJ注解，由Spring AOP读取；切面方法 | 命中切点的目标方法执行前运行Advice |
| `@AfterReturning` | AspectJ注解，由Spring AOP读取；切面方法 | 命中方法正常返回后运行Advice；抛出异常时不执行 |
| `@Around` | AspectJ注解，由Spring AOP读取；切面方法 | 包围目标调用，必须正确调用 `proceed()` 才会继续执行目标方法 |
| `@Override` | Java；重写方法 | 编译器确认方法确实重写接口或父类声明；它不是Spring运行时注解 |

同一注解是否真正生效还取决于组件是否成为Spring Bean、相关能力是否启用，以及调用是否经过对应处理链或代理。下文再结合实验说明这些边界。

## 二、Filter：覆盖最外层HTTP入口

第11章的 `RequestLoggingFilter extends OncePerRequestFilter` 面对 `HttpServletRequest` 和 `HttpServletResponse`，不依赖某个Controller方法。它适合在请求早期建立requestId，并在 `finally` 中记录最终状态和清理MDC。

`@Order(Ordered.HIGHEST_PRECEDENCE)` 明确该Filter的注册优先级。整数越小越早执行；多个Filter有先后要求时必须显式配置并验证，不能根据类名判断顺序。

`OncePerRequestFilter` 的“一次”按Servlet请求分派规则理解。异步分派和错误分派有专门行为，它不表示“每个业务方法只执行一次”。

## 三、HandlerInterceptor：围绕MVC Handler执行

下面是独立实验。它观察Controller前后时机，不替换第11章的请求日志Filter。

新建：

```text
src/main/java/com/example/employee/dilab/RequestTimingInterceptor.java
```

```java
package com.example.employee.dilab;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

public class RequestTimingInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(
            RequestTimingInterceptor.class);
    private static final String START_NANOS =
            RequestTimingInterceptor.class.getName() + ".startNanos";

    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler) {
        request.setAttribute(START_NANOS, System.nanoTime());
        return true;
    }

    @Override
    public void postHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler,
            ModelAndView modelAndView) {
        log.debug("controller_returned status={}", response.getStatus());
    }

    @Override
    public void afterCompletion(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler,
            Exception exception) {
        Long startNanos = (Long) request.getAttribute(START_NANOS);
        if (startNanos != null) {
            long elapsedNanos = System.nanoTime() - startNanos;
            log.info("mvc_completed status={} elapsedMs={}",
                    response.getStatus(), elapsedNanos / 1_000_000);
        }
    }
}
```

再新建注册配置：

```text
src/main/java/com/example/employee/dilab/InterceptorLabConfig.java
```

```java
package com.example.employee.dilab;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class InterceptorLabConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new RequestTimingInterceptor())
                .addPathPatterns("/employees/**");
    }
}
```

这里由配置类直接创建并注册Interceptor，因此不再给 `RequestTimingInterceptor` 添加 `@Component`，避免同一个对象的创建方式不清楚。

| 接口或方法 | 当前参数与可接受值 | 返回或效果 |
| --- | --- | --- |
| `HandlerInterceptor` | 由自定义类实现 | 提供MVC Handler前后的回调入口 |
| `preHandle()` | request、response、handler由Spring MVC提供 | 返回true继续；返回false停止后续Handler调用 |
| `postHandle()` | 增加可为null的ModelAndView | Controller正常返回后执行 |
| `afterCompletion()` | 增加本次处理异常，可为null | 请求完成阶段执行，适合清理和最终记录 |
| `WebMvcConfigurer` | 由配置类实现 | 提供扩展Spring MVC配置的回调 |
| `addInterceptors()` | Spring提供的InterceptorRegistry | 注册Interceptor和匹配路径 |
| `addPathPatterns()` | 一个或多个MVC路径模式 | 本例只应用于 `/employees/**` |

对于 `@RestController` 和 `ResponseEntity`，响应可能在 `postHandle` 前已经写出，因此不要把修改REST响应正文的关键逻辑放在 `postHandle`。

运行员工详情、404和业务异常请求，比较Filter与Interceptor日志。这个计时示例只针对普通同步MVC请求：异步Handler可能先退出原线程再发生异步分派，`postHandle` 不保证按同步案例出现，`afterCompletion` 也可能在异步处理真正结束后触发。需要覆盖异步请求时另查 `AsyncHandlerInterceptor`、分派类型与测试证据，不直接照搬这里的耗时结论。完成后删除两个 `dilab` 文件并运行全部测试，主线不保留重复计时日志。

## 四、Controller Advice：集中转换MVC异常

第7章的 `@RestControllerAdvice` 会让异常处理Bean参与Controller调用阶段的异常解析； `@ExceptionHandler` 再按异常类型选择方法并生成HTTP响应。

它适合把 `EmployeeNotFoundException` 转换为404，把 `DuplicateEmailException` 转换为409。它不会自然处理Spring Security过滤器链在Controller之前生成的401和403；第16章因此使用 `AuthenticationEntryPoint` 和 `AccessDeniedHandler`。

不要为了“统一”添加没有边界的 `@ExceptionHandler(Exception.class)`，否则可能把405、415等框架已经具有明确含义的错误改成500。

## 五、AOP与Spring Proxy：围绕Bean方法调用

Spring AOP常通过代理在Spring Bean方法前后加入处理。主线已经使用两种典型能力：

- `@Transactional`：代理在Service方法前开始或加入事务，并根据正常返回或异常决定提交和回滚；
- `@PreAuthorize`：方法安全代理在调用目标方法前计算授权规则。

既存项目中还可能看到：

| 注解或方法 | 作用 | Review重点 |
| --- | --- | --- |
| `@Aspect` | 在类上声明一个由Spring AOP使用的切面定义 | 切点实际匹配哪些Bean |
| `@Before` | 在Advice方法上声明：匹配的方法调用前执行 | 是否改变参数或记录敏感值 |
| `@AfterReturning` | 在Advice方法上声明：目标方法正常返回后执行 | 是否误记录大量响应或个人信息 |
| `@Around` | 在Advice方法上声明：包围目标调用 | 是否正确调用且只调用一次 `proceed()` |

代理只拦截真正经过代理对象的调用。同一个类中使用 `this.someMethod()` 调用另一个带事务或方法安全注解的方法，通常不会再次经过外层代理。这一边界会在第13章事务和第16章方法权限中结合实际代码说明。

识读例：`new EmployeeService(mapper).updateWithHistory()` 创建的对象不由Spring管理，不会得到事务代理；Spring注入的 `employeeService.updateWithHistory()` 才可能经过代理。即便是受管Bean，若其 `update()` 内部用 `this.updateWithHistory()` 调用同类方法，内部调用不会再次穿过代理，标在内部方法上的新事务设置可能不生效。修正应把事务边界放在外部实际调用的公开Service入口，或按设计拆成另一个受管Bean，再用事务测试观察两次写入是否同回滚；不要只检查方法上有没有注解。

### 同步请求观察矩阵

在主线工程的可恢复副本中，先保留第11章 `RequestLoggingFilter`，再添加上面的Interceptor；按第7章查看Advice、按第16章查看Security。以下是**预期观察点**，不是所有项目绝对结果。用每层第一行日志或断点记录实际命中情况，并标记配置和请求。

| 请求 | Filter | Security | Interceptor | Controller/Advice | Service代理 |
| --- | --- | --- | --- | --- | --- |
| 已授权的 `GET /employees/1001` | 进入并最终清理MDC | 放行 | 匹配时pre/post/after | Controller进入，通常无Advice | 调用受代理方法时命中 |
| Controller抛业务异常 | 进入并最终清理 | 放行 | pre、通常after；post不保证 | Controller进入，匹配异常时Advice转换 | 仅实际调用过的Service命中 |
| 未认证时Security提前拒绝 | 取决于Filter注册顺序，入口日志通常可见 | 拒绝 | 通常不进入 | 不进入；MVC Advice通常不处理 | 不进入 |
| 不匹配的URL | 入口Filter仍可见 | 视URL规则而定 | 当前 `/employees/**` 配置是否进入取决于MVC Handler匹配 | 业务Controller不进入，404可由框架处理 | 不进入 |
| 已授权Controller调用被代理的Service | 进入 | 放行 | 匹配时进入 | Controller进入 | AOP/事务/方法安全按各自切点和顺序命中 |

实验时分别请求存在员工、由主线规则引起的业务异常、无Cookie受保护URL、不存在URL；在 `RequestLoggingFilter`、`preHandle`、Controller、Advice、Service首行设置断点。URL不匹配仍可能被静态资源Handler接收，因此不能只凭Interceptor有无日志断定“没匹配”。记录HTTP状态、requestId与断点顺序，最后删除实验文件并运行回归测试。

下面是**教学模拟日志**，不是本仓库实际运行结果。前置条件：测试副本中请求日志Filter在Security链外层、Interceptor只匹配 `/employees/**`，GET需要登录且有读取权限；Service入口有事务代理或方法日志切面。不同Filter注册顺序、Security配置和异常处理会改变细节，先记录实际配置再比较。

| 请求与条件 | 模拟日志先后顺序 | 未进入的组件及原因 |
| --- | --- | --- |
| `GET /employees/1001`，已登录且授权，员工存在 → 200 | `Filter enter → Security allow → Interceptor pre → Controller → Service proxy enter/transaction → Mapper → Service return/commit → Interceptor post → Interceptor after → Filter exit` | Advice未进入：没有待转换的异常 |
| `GET /employees/9999`，已授权，Controller/Service抛业务异常 → 404 | `Filter enter → Security allow → Interceptor pre → Controller → Service proxy → exception → Advice maps 404 → Interceptor after → Filter exit` | `postHandle` 通常不执行：Handler未正常完成；具体仍看MVC异常解析路径 |
| `GET /employees/1001`，无认证 → 401 | `Filter enter → Security reject 401 → Filter exit` | Interceptor、Controller、MVC Advice、Service代理未进入：Security在MVC前终止；具体401依赖认证入口配置 |
| `GET /employees/1001`，已认证但无读取权限 → 403 | `Filter enter → Security reject 403 → Filter exit` | 同上；与401的差别是身份已确认但不具所需权限 |
| `GET /employees/1001`，授权，Service调用被代理方法 → 200 | `Filter enter → Security allow → Interceptor pre → Controller → AOP before/transaction begin → Service → transaction commit/AOP after → Controller return → Interceptor post/after → Filter exit` | 若同类 `this.method()` 自调用，被调用方法上的新AOP/事务Advice不会再次进入代理 |

操作：在专用测试副本给上述组件加不含个人信息的单行日志，以同一个requestId关联五次请求，记录实际HTTP状态和每层是否命中；业务异常、权限条件须按项目现有测试数据准备。提交“实际顺序 vs 模拟顺序”差异及配置依据。清理时移除临时日志/断点、恢复测试数据并执行受影响测试，不能把模拟表当成实测证据。

编写方法日志切面时，应检查密码、Token、个人信息、大对象、异常语义、切点范围和性能影响。完整的递进示例、依赖、自定义 `@MethodLog`、`@Around`、成功/失败/慢调用、自调用边界和验证方式见[A11-3 使用AOP统一记录方法日志](A11_3_aop_method_logging.md)。本附录只负责比较共通机制的位置，不再维护第二份AOP日志实现。

## 六、识读与Review任务

1. 为一次员工详情请求标出Filter、Security Filter Chain、Interceptor、Controller、Service代理和Mapper的位置。
2. Review“在Interceptor读取 `X-Role` 并授予ADMIN”的方案，说明为什么客户端请求头不是可信身份来源。
3. Review记录所有Controller参数的 `@Around` 切面，列出敏感信息和重复日志风险。
4. 制造Controller业务异常，比较Filter、Interceptor和Controller Advice分别能够观察到什么。
5. 删除实验文件后执行全部测试，确认没有留下重复日志或路径行为变化。

## 七、完成标准

完成后应能够根据问题发生位置选择调查入口，解释四种机制为什么不能互相替代，并在既存项目中检查注册范围、执行顺序、代理边界、异常变化和敏感信息风险。
