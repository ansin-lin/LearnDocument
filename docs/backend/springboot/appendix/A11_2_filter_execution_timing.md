# A11-2 Filter、OncePerRequestFilter与RequestLoggingFilter的执行时机

完成[第11章：用日志定位接口故障](../mainline/11_logging_and_diagnosis.md)后，Employee工程已经有 `RequestLoggingFilter`。本附录不新增过滤器，也不改变主线代码；通过现有实现观察一次HTTP请求进入和离开应用的过程。完成后，你应能区分接口、框架基类和项目类，并指出拦截器、全局异常处理和AOP各在什么时候参与。

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

## 四、用已有工程验证执行时机

开始状态：使用第11章完成后的Employee工程，保留原有 `RequestLoggingFilter`，在Eclipse启动服务。此练习不修改文件和数据库。用Postman依次发送：

1. `GET {{baseUrl}}/employees/1001`：记录HTTP状态、`X-Request-Id` 和对应的 `request_complete` 日志。预期请求到达Controller/Service，Filter在链返回后记录完成信息。
2. `GET {{baseUrl}}/employees/999999`：记录404响应和相同requestId的完成日志。指出业务异常由MVC全局异常处理转换，Filter仍会走退出清理。
3. 完成第15章登录内容后，清除Cookie并再次请求受保护的员工详情：预期未登录响应为401，Controller断点不命中；若最外层日志Filter已覆盖该分派，仍可观察请求完成日志。由此区分“Filter运行”与“Controller运行”。

如需观察更细顺序，可在 `RequestLoggingFilter.doFilterInternal` 的 `filterChain.doFilter` 前后、第7章异常处理方法以及Controller/Service各设一个调试断点。不要为了观察顺序再注册第二个日志Filter。记录一张“请求 → 实际命中的位置 → HTTP状态 → 日志”的对照表，并解释为何401请求不会触发Controller中的业务异常处理。实验后移除临时断点，工程代码保持原状。

## 五、常见误判

- 看到 `OncePerRequestFilter` 就认为一个HTTP交换只可能有一次分派。异步和错误分派需要单独核对。
- 以为Filter、Interceptor和AOP是同一层的三种写法。它们分别围绕Servlet链、MVC Handler和Spring Bean方法。
- 以为 `@RestControllerAdvice` 能统一处理任何位置抛出的异常。过滤器链中的安全拒绝需要相应的Security处理入口。
- 以为 `finally` 中的日志一定表示响应已发送、数据库已提交。它只表示当前过滤链调用正在退出。

参考：[Spring OncePerRequestFilter Javadoc](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/web/filter/OncePerRequestFilter.html)、[Spring MVC Interception](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-servlet/handlermapping-interceptor.html)、[Controller Advice](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-advice.html)、[Spring AOP代理](https://docs.spring.io/spring-framework/reference/core/aop/proxying.html)。
