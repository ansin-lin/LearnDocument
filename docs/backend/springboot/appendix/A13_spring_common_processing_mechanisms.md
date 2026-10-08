# A13 Spring共通处理机制：Filter、Interceptor、Controller Advice与AOP

本附录用于阅读既存Spring项目中的共通处理。开始前应完成第11章，能够使用 `RequestLoggingFilter`、requestId和全局异常处理器。本附录不改变Employee主线的最终代码；实验文件完成后要删除。

## 一、先按执行位置区分机制

```text
HTTP Request
  → Servlet Filter
  → Spring Security Filter Chain
  → DispatcherServlet
  → HandlerInterceptor.preHandle
  → Controller
  → Service上的Spring Proxy / AOP
  → Mapper
  ← HandlerInterceptor.postHandle / afterCompletion
  ← HTTP Response
```

这是用于定位的简化图，不是所有配置下绝对固定的源码调用栈。一个请求可能经过多个Filter、Interceptor和代理；实际先后还会受注册顺序、异常、错误分派和异步处理影响。

| 机制 | 主要位置 | 适合解决 | 不适合替代 |
| --- | --- | --- | --- |
| Servlet Filter | MVC之前的Servlet处理链 | requestId、HTTP访问日志、Security、请求包装 | Service业务规则 |
| HandlerInterceptor | Spring MVC的Handler调用前后 | Controller范围的计时、检查和清理 | 数据库事务、任意Bean方法 |
| Controller Advice | MVC异常解析与共通响应 | 业务异常到HTTP响应的转换 | Security在Controller前的拒绝 |
| AOP / Spring Proxy | Spring Bean方法调用 | 事务、方法权限、受控横切逻辑 | 原始HTTP报文处理 |

选择机制前先问：问题发生在HTTP入口、MVC Handler、异常转换，还是Bean方法调用。

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

运行员工详情、404和业务异常请求，比较Filter与Interceptor日志。完成后删除两个 `dilab` 文件并运行全部测试，主线不保留重复计时日志。

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
| `@Aspect` | 声明一个由Spring AOP使用的切面类 | 切点实际匹配哪些Bean |
| `@Before` | 匹配的方法调用前执行 | 是否改变参数或记录敏感值 |
| `@AfterReturning` | 方法正常返回后执行 | 是否误记录大量响应或个人信息 |
| `@Around` | 包围目标调用 | 是否正确调用且只调用一次 `proceed()` |

代理只拦截真正经过代理对象的调用。同一个类中使用 `this.someMethod()` 调用另一个带事务或方法安全注解的方法，通常不会再次经过外层代理。这一边界会在第13章事务和第16章方法权限中结合实际代码说明。

本课程不要求自行编写通用日志切面。看到记录所有方法参数的AOP时，应检查密码、Token、个人信息、大对象、异常语义和性能影响。

## 六、识读与Review任务

1. 为一次员工详情请求标出Filter、Security Filter Chain、Interceptor、Controller、Service代理和Mapper的位置。
2. Review“在Interceptor读取 `X-Role` 并授予ADMIN”的方案，说明为什么客户端请求头不是可信身份来源。
3. Review记录所有Controller参数的 `@Around` 切面，列出敏感信息和重复日志风险。
4. 制造Controller业务异常，比较Filter、Interceptor和Controller Advice分别能够观察到什么。
5. 删除实验文件后执行全部测试，确认没有留下重复日志或路径行为变化。

## 七、完成标准

完成后应能够根据问题发生位置选择调查入口，解释四种机制为什么不能互相替代，并在既存项目中检查注册范围、执行顺序、代理边界、异常变化和敏感信息风险。
