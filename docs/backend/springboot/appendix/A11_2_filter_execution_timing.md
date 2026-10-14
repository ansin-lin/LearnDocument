# A11-2 Filter、OncePerRequestFilter与RequestLoggingFilter的执行时机

完成[第11章：用日志定位接口故障](../mainline/11_logging_and_diagnosis.md)后，Employee工程已经有 `RequestLoggingFilter`。本附录不新增第二个日志过滤器；它从Servlet分派、过滤器注册、Spring Security、DispatcherServlet、异常处理和异步边界六个角度解释现有实现。完成后，你应能根据200、400、404、401、403和500的实际证据判断请求走到了哪里，而不是把Filter、Interceptor、Controller Advice和AOP当成同一层的不同写法。

## 一、三个名称不是三道过滤器

| 名称 | 是什么 | 谁提供 | 在本项目中做什么 |
| --- | --- | --- | --- |
| `jakarta.servlet.Filter` | Servlet规范定义的接口 | Jakarta Servlet | 规定过滤器接收请求、响应与过滤链的基本契约 |
| `OncePerRequestFilter` | 实现了Filter契约的Spring抽象基类 | Spring Web | 处理“已经过滤”标记等细节，把当前分派的处理交给子类的 `doFilterInternal` |
| `RequestLoggingFilter` | 继承上述基类的具体类 | 本课程项目 | 生成requestId、设置响应头与MDC，调用后续链，并记录完成日志 |

因此关系是 `Filter ← OncePerRequestFilter ← RequestLoggingFilter`，不是请求依次经过名为这三个名称的三个对象。Servlet容器调用已注册过滤器的 `doFilter`；基类决定本次是否进入 `doFilterInternal`；项目类在这个方法中实现日志行为。`OncePerRequestFilter` 的“一次”不能理解成“从浏览器发出请求到所有异步、错误分派结束，绝对只运行一次”。Servlet请求可能发生 `REQUEST`、`ASYNC`、`ERROR` 等分派，基类有相应的跳过和扩展规则；要调查这些场景，应核对实际分派类型与日志，而不套用普通同步请求的结论。详见[官方Javadoc](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/web/filter/OncePerRequestFilter.html)。

## 二、一次普通同步请求怎样经过它

以下是Employee项目中**普通同步请求**的简化路径；实际Filter顺序取决于注册与配置，不能仅凭图推断所有应用的精确顺序：

```text
客户端请求
  → Servlet容器中的过滤器链
      → RequestLoggingFilter.doFilterInternal：生成requestId、写入MDC
          → filterChain.doFilter：交给后续Filter（包含安全过滤链）与Spring MVC
              → DispatcherServlet → Controller → Service → Mapper
              ← MVC生成响应或处理业务异常
          ← 后续链返回
      → finally：记录状态与耗时、清理MDC
  ← 响应客户端
```

`filterChain.doFilter(request, response)` 是关键分界：调用前属于“进入”，它返回后属于“退出”；漏掉它通常会阻断后续处理。第11章的实现把结束日志和 `MDC.remove("requestId")` 放在 `finally`，所以正常返回与后续代码抛异常时都能清理当前线程的上下文。`@Order(Ordered.HIGHEST_PRECEDENCE)` 使该Filter在Spring管理的过滤器顺序中尽早执行，但其他容器注册方式或过滤器链仍需在实际项目核对。不要把 `RequestLoggingFilter` 的HTTP完成日志误当成“Service事务已提交”的证据。

对于MVC已经处理的业务错误，Filter通常能在退出时读到相应的404或409；如果异常继续传播到Servlet容器，`finally` 中的 `response.getStatus()` 未必等于客户端最后看到的状态。异步请求也可能在当前线程退出后才完成响应。本课程用同步员工API观察基本路径，不据此承诺异步最终耗时或状态。

## 三、其他四种机制分别何时执行

| 机制 | 进入条件和时机 | 返回或失败时的表现 |
| --- | --- | --- |
| Spring Security过滤器链 | 作为Servlet过滤器链的一部分，在Controller之前检查认证、CSRF和URL权限 | 可直接返回401/403，使Controller、MVC拦截器和Service不执行；本项目具体配置见[第15章](../mainline/15_session_login.md) |
| `HandlerInterceptor` | Spring MVC已经找到匹配的Handler后，`preHandle` 在Controller前执行 | 正常调用后可进入 `postHandle`；请求完成阶段执行 `afterCompletion`。它不覆盖所有被更早Filter拒绝的请求 |
| `@RestControllerAdvice`＋`@ExceptionHandler` | MVC处理Controller或Handler阶段的可匹配异常时参与异常解析 | 把业务异常转换为HTTP响应；不会自然接管Security过滤器在Controller之前作出的拒绝 |
| Spring AOP / 代理 | 对匹配的Spring Bean方法、且调用确实经过代理时执行；本项目事务与方法权限主要作用于Service调用 | Advice可在目标方法前后运行；同类对象内部 `this.method()` 调用通常绕过代理，不等同于Servlet Filter |

`HandlerInterceptor` 不适合作为本项目的主要安全边界：它运行在MVC Handler选择之后，Spring Security已经在更早的过滤器链处理认证授权。对于REST响应，`postHandle` 执行时响应内容可能已写出，不应把修改响应正文的关键逻辑放在那里。详细的拦截器注册和回调实验见[B06 Spring共通处理机制](B06_spring_common_processing_mechanisms.md)；全局异常转换见[第7章](../mainline/07_success_and_failure_response.md)，AOP与事务代理边界见[第13章](../mainline/13_transaction_and_consistency.md)。

## 四、Servlet分派类型决定Filter是否再次参与

一个浏览器动作可以在Servlet容器中出现多个分派。`HttpServletRequest.getDispatcherType()` 可以观察当前类型：

| `DispatcherType` | 何时出现 | 对日志Filter的影响 |
| --- | --- | --- |
| `REQUEST` | 客户端最初请求进入容器 | 主线同步接口首先出现的分派 |
| `ASYNC` | 请求启动异步处理后，由其他线程继续分派 | 可能发生线程切换；MDC不会自动跨线程继承 |
| `ERROR` | 容器进行错误页或错误处理分派 | 是否再次过滤取决于注册范围和Filter设计 |
| `FORWARD` | 服务器内部把请求转发给另一资源 | 地址栏不变，但处理目标发生变化 |
| `INCLUDE` | 一个资源把另一个资源输出包含进当前响应 | REST API主线很少使用 |

`OncePerRequestFilter` 的目标是让子类在一次分派中只执行一次，并提供控制ASYNC和ERROR分派的方法。它不是“一个客户端请求在所有线程和所有分派中永远只运行一次”。默认实现通常跳过后续ASYNC和ERROR分派；若项目覆盖 `shouldNotFilterAsyncDispatch()` 或 `shouldNotFilterErrorDispatch()`，行为会改变。

因此第11章的同步耗时是“当前REQUEST过滤链从进入到返回”的耗时。若Controller启动异步任务并提前返回，该数字不一定包含异步任务最终完成时间，也不能直接称为完整业务处理时间。

## 五、Filter怎样注册，顺序怎样决定

常见注册方式包括：

| 注册方式 | 谁创建Filter | 顺序来源 | Review重点 |
| --- | --- | --- | --- |
| `@Component` | Spring组件扫描 | `@Order`或Ordered | 是否扫描到、是否与其他方式重复注册 |
| `FilterRegistrationBean` | 配置类中的Bean方法 | `setOrder()` | URL范围、DispatcherType、是否启用、顺序 |
| Spring Security Filter Chain | Security配置 | Security内部链及外部代理位置 | 认证、CSRF、授权发生在Controller之前 |
| Servlet容器配置 | 容器或传统部署描述 | 容器配置 | 与Spring Bean的顺序及分派范围是否一致 |

同一个Filter不要既加 `@Component`，又通过 `FilterRegistrationBean` 注册第二次。否则一次请求可能输出两次日志、生成两个requestId或重复包装请求。

主线使用：

```java
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestLoggingFilter extends OncePerRequestFilter {
    // ...
}
```

### 本例注解与常量说明

| 写法 | 标注位置、处理者 | 常用属性或值 | 当前效果 |
| --- | --- | --- | --- |
| `@Component` | 类；Spring组件扫描 | 可选Bean名称，例如 `@Component("requestLoggingFilter")` | 使用默认Bean名称并注册项目Filter |
| `@Order` | 类；Spring排序机制 | 任意 `int`，越小优先级越高 | 使用 `Ordered.HIGHEST_PRECEDENCE` 尽早建立requestId |
| `@Override` | 方法；Java编译器 | 无属性 | 确认方法正确重写父类契约 |
| `Ordered.HIGHEST_PRECEDENCE` | Spring常量 | `Integer.MIN_VALUE` | 表示最高优先级起点，不表示一定早于所有容器级Filter |

“最高优先级”只在参与同一排序体系的组件之间比较。遇到Servlet容器Filter、Spring Security代理和错误分派时，仍要用断点或日志确认实际位置，不能只根据常量名称断言绝对顺序。

## 六、六类HTTP结果分别能说明什么

| 结果 | 典型最远位置 | 通常可以观察到 | 不能据此断定 |
| --- | --- | --- | --- |
| 200/201 | Controller→Service→Mapper正常返回 | Filter完成日志、业务成功日志、数据库结果 | 外部异步任务一定完成 |
| 400 JSON/校验失败 | DispatcherServlet参数解析或校验阶段 | Filter完成日志、统一400响应 | Controller方法体一定执行过 |
| 404业务不存在 | Controller/Service抛业务异常后由Advice转换 | Advice WARN、Filter完成日志 | URL没有匹配；业务404与路由404不是同一原因 |
| 404路由不存在 | DispatcherServlet找不到Handler或静态资源处理 | Filter完成日志 | Service一定执行过 |
| 401/403 Security拒绝 | Security Filter Chain | Security处理器响应、外层Filter日志 | Controller、Interceptor和Controller Advice参与过 |
| 500系统异常 | 可能来自MVC、Service、Mapper或更外层 | 最早异常、边界ERROR、Filter退出 | `response.getStatus()` 在所有传播路径都已是最终值 |

请求完成日志证明当前过滤链已经返回，并不证明事务一定提交。Service方法返回后，外层事务Advice还可能执行提交并失败；多个代理的实际嵌套顺序也会影响切面测得的范围。事务证据应结合第13章测试和数据库状态。

## 七、MDC为什么必须在 `finally` 清理

Servlet容器会复用工作线程。若请求A在线程T1写入 `requestId=A` 后没有清理，线程T1随后处理请求B时，B的早期日志可能错误携带A的编号。

```text
线程T1处理A：MDC.put(A)
  → A结束但没有remove
线程T1处理B
  → B的日志错误显示A
```

正确结构是：

```java
MDC.put("requestId", requestId);
try {
    filterChain.doFilter(request, response);
} finally {
    try {
        log.info("request_complete ...");
    } finally {
        MDC.remove("requestId");
    }
}
```

外层 `finally` 保证后续链抛异常时仍进入清理；内层 `finally` 保证即使记录完成日志本身失败也移除MDC。`MDC.clear()` 会删除当前线程的全部MDC键，可能误删其他组件放入的合法字段，所以主线只移除自己负责的 `requestId`。

异步任务切换到线程池后，不会因为原请求线程有MDC就自动安全继承。需要异步日志时，应使用项目批准的上下文传播机制，并在目标线程同样执行设置与清理；简单把整个MDC Map长期保存在线程池中会造成串号和数据泄露。

## 八、异常穿过不同边界时发生什么

### 1. MVC可以处理的业务异常

```text
Service抛EmployeeNotFoundException
  → Controller调用退出
  → HandlerExceptionResolver找到@RestControllerAdvice
  → @ExceptionHandler生成404响应
  → DispatcherServlet返回Filter链
  → Filter读取404并记录完成日志
```

### 2. Security在Controller之前拒绝

```text
请求进入外层Filter
  → Security判断未登录或无权限
  → AuthenticationEntryPoint / AccessDeniedHandler生成401/403
  → 不进入Controller
  → 外层Filter记录完成日志
```

### 3. 未被MVC处理的异常继续外抛

```text
后续链抛异常
  → Filter的finally仍执行
  → 异常可能继续交给容器ERROR分派
  → 客户端最终状态可能晚于当前finally确定
```

因此Filter不应自行捕获所有异常并统一改成500，否则可能绕过原有异常映射、重复写响应或掩盖Security语义。

## 九、用已有工程验证执行时机

开始状态：使用第11章完成后的Employee工程，保留原有 `RequestLoggingFilter`，在Eclipse启动服务。此练习不修改文件和数据库。用Postman依次发送：

1. `GET {{baseUrl}}/employees/1001`：记录HTTP状态、`X-Request-Id` 和对应的 `request_complete` 日志。预期请求到达Controller/Service，Filter在链返回后记录完成信息。
2. `GET {{baseUrl}}/employees/999999`：记录404响应和相同requestId的完成日志。指出业务异常由MVC全局异常处理转换，Filter仍会走退出清理。
3. 完成第15章登录内容后，清除Cookie并再次请求受保护的员工详情：预期未登录响应为401，Controller断点不命中；若最外层日志Filter已覆盖该分派，仍可观察请求完成日志。由此区分“Filter运行”与“Controller运行”。
4. 发送字段错误的POST：预期400；在Controller方法体设置断点，确认参数绑定失败时方法体不一定进入。
5. 使用不存在路径：预期路由404；与“员工不存在”的业务404比较Advice日志和Service断点差异。

如需观察更细顺序，可在 `RequestLoggingFilter.doFilterInternal` 的 `filterChain.doFilter` 前后、第7章异常处理方法以及Controller/Service各设一个调试断点。不要为了观察顺序再注册第二个日志Filter。记录一张“请求 → DispatcherType → 实际命中的位置 → HTTP状态 → requestId → 日志”的对照表，并解释为何401请求不会触发Controller中的业务异常处理。实验后移除临时断点，工程代码保持原状。

## 十、设计选择与常见误判

### 1. 该把处理放在哪里

| 需求 | 首选位置 | 原因 |
| --- | --- | --- |
| 为所有HTTP请求生成requestId | Filter | 在MVC和Security之前建立关联信息 |
| Controller路径范围的共通计时 | Interceptor | 能取得MVC Handler信息 |
| 业务成功事件 | Service显式Logger | 只有业务层知道何时真正满足业务规则 |
| 业务异常转HTTP状态 | Controller Advice | 位于MVC异常解析边界 |
| Service方法统一耗时 | AOP | 围绕匹配的Spring Bean方法；详见[A11-3](A11_3_aop_method_logging.md) |
| 登录、CSRF、角色权限 | Spring Security | 不能用自定义Header或Interceptor替代安全框架 |

- 看到 `OncePerRequestFilter` 就认为一个HTTP交换只可能有一次分派。异步和错误分派需要单独核对。
- 以为Filter、Interceptor和AOP是同一层的三种写法。它们分别围绕Servlet链、MVC Handler和Spring Bean方法。
- 以为 `@RestControllerAdvice` 能统一处理任何位置抛出的异常。过滤器链中的安全拒绝需要相应的Security处理入口。
- 以为 `finally` 中的日志一定表示响应已发送、数据库已提交。它只表示当前过滤链调用正在退出。
- 用Filter读取并记录完整请求Body。请求流通常只能直接读取一次，需要包装与大小限制，而且Body可能包含密码和个人信息；主线不这样做。
- 认为更早的Filter一定能读到最终响应内容。响应可能流式写出、异步完成或发生容器ERROR分派。

## 十一、Review清单与完成标准

Review自定义Filter时至少确认：

1. 注册方式是否唯一，顺序和URL范围是否明确；
2. 是否调用 `filterChain.doFilter()`，调用前后逻辑是否符合目的；
3. MDC、ThreadLocal、包装器等资源是否在 `finally` 清理；
4. 是否错误记录Authorization、Cookie、Session ID或完整Body；
5. 400、404、401、403、500是否经过预期边界；
6. 是否把Filter完成误写成事务提交成功；
7. 是否考虑ASYNC和ERROR分派，还是明确只支持同步REQUEST；
8. 是否出现第二个重复日志Filter或重复requestId；
9. 异常是否保持原有HTTP语义，没有被Filter统一吞掉；
10. 验证证据是否包含HTTP响应、requestId、断点位置和日志顺序。

完成后，应能从一次HTTP结果反推哪些组件可能执行，用DispatcherType和断点验证实际路径，并说明Filter、Interceptor、Advice、Security与AOP的责任边界。

参考：[Spring Web Filters](https://docs.spring.io/spring-framework/reference/web/webmvc/filters.html)、[Spring OncePerRequestFilter Javadoc](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/web/filter/OncePerRequestFilter.html)、[Spring MVC Interception](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-servlet/handlermapping-interceptor.html)、[Controller Advice](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-advice.html)、[Spring AOP代理](https://docs.spring.io/spring-framework/reference/core/aop/proxying.html)。
