# 全局异常处理与事务注解（实践详解）

> 覆盖异常切面、异常映射与响应体定制，以及声明式事务的边界、传播、隔离、回滚规则与常见陷阱。所有示例均可直接用于 Spring Boot 3.x。

---

## 一、全局异常处理

### 1.1 `@ControllerAdvice` / `@RestControllerAdvice`（复合注解）

**作用**：定义**控制器层切面**，集中做异常处理、数据绑定、自定义模型属性。  

- `@RestControllerAdvice` = `@ControllerAdvice` + `@ResponseBody`（返回 JSON 等序列化数据）。  
- 支持按**包、注解、类型**进行作用范围限定。

**定义位置**：类上。

**关键属性**：

- `basePackages`：仅匹配这些包下的控制器。
- `assignableTypes`：仅匹配这些类型及其子类。
- `annotations`：仅匹配带指定注解的控制器。

```java
@RestControllerAdvice(basePackages = "com.example.demo.api")
public class GlobalExceptionAdvice {
  // 1) 请求体校验失败（@Valid/@Validated + @RequestBody）
  @ExceptionHandler(MethodArgumentNotValidException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public Map<String, Object> handleBodyBind(MethodArgumentNotValidException e) {
    var errors = e.getBindingResult().getFieldErrors().stream()
      .map(f -> Map.of("field", f.getField(), "msg", f.getDefaultMessage()))
      .toList();
    return Map.of("message", "参数校验失败", "errors", errors);
  }

  // 2) 查询参数/路径变量校验失败（@Validated 放在类上）
  @ExceptionHandler(ConstraintViolationException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public Map<String, Object> handleConstraint(ConstraintViolationException e) {
    var errors = e.getConstraintViolations().stream()
      .map(v -> Map.of("field", v.getPropertyPath().toString(), "msg", v.getMessage()))
      .toList();
    return Map.of("message", "参数非法", "errors", errors);
  }

  // 3) 业务异常统一返回
  @ExceptionHandler(BizException.class)
  public ResponseEntity<Map<String, Object>> handleBiz(BizException e) {
    return ResponseEntity.status(e.getStatus())
      .body(Map.of("message", e.getMessage(), "code", e.getCode()));
  }

  // 4) 兜底异常（最后一道防线）
  @ExceptionHandler(Exception.class)
  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  public Map<String, Object> handleOther(Exception e) {
    return Map.of("message", "服务端异常", "detail", e.getClass().getSimpleName());
  }
}
```

**进阶**：

- 继承 `ResponseEntityExceptionHandler` 可获得对常见 MVC 异常的默认处理，然后按需覆写。
- 使用 `@Order` 控制多个 Advice 的优先级（值越小优先级越高）。

---

### 1.2 `@ExceptionHandler`

**作用**：声明**局部（当前控制器）**或**全局（Advice）**的异常处理方法。

**方法签名**（部分可选参数）：

- 异常类型参数（如 `MethodArgumentNotValidException e`）
- `HttpServletRequest` / `WebRequest`
- `BindingResult`
- 返回 `ResponseEntity<T>` / 对象（配合 `@ResponseBody`）/ 视图名（`String`）

**支持多异常**：

```java
@ExceptionHandler({IOException.class, TimeoutException.class})
public ResponseEntity<?> handleIO(Exception e) { ... }
```

**选择规则**：优先匹配**最具体**的异常类型；若有重叠，按方法签名最精确者生效。

---

### 1.3 `@ResponseStatus` 与 `ResponseStatusException`

**作用**：在**异常类**或**处理方法**上声明固定的 HTTP 状态码。

```java
@ResponseStatus(HttpStatus.NOT_FOUND)
public class UserNotFoundException extends RuntimeException {}
```

若需要**动态**设置状态码或提供原因：

```java
throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "参数不合法");
```

---

### 1.4 绑定与数据增强

- `@InitBinder`：注册自定义 `WebDataBinder`（类型转换、日期格式等）。
- `@ModelAttribute`（方法级）：在每个控制器方法前添加固定模型数据（返回视图时常用）。
- `Problem+JSON`：若需要标准错误格式，可自定义统一结构或采用 RFC7807 规范。

---

### 1.5 Spring Boot 默认错误机制

- 未捕获异常由 `BasicErrorController` 生成默认响应，`/error` 端点承载错误信息。
- 可通过自定义 `ErrorAttributes` 或 `ErrorController` 改写默认结构。
- 生产环境建议关闭 `server.error.include-stacktrace=never`。

---

## 二、事务管理

### 2.1 `@Transactional`

**作用**：声明**事务边界**，由 Spring AOP 代理拦截方法调用，统一开启/提交/回滚。  
**定义位置**：类或方法（**方法优先**覆盖类上的设置）。

```java
@Service
public class OrderService {
  // 业务方法：任何异常都回滚
  @Transactional(rollbackFor = Exception.class)
  public void placeOrder(Order o) {
    // 1) 写主单
    // 2) 扣库存
    // 3) 记账/积分
  }
}
```

**常用属性**：

- `propagation`（传播行为）：见 2.2
- `isolation`（隔离级别）：见 2.3
- `readOnly`：优化只读事务（提示底层引擎不做脏写检测等）
- `timeout`：超时秒数，超过触发回滚
- `rollbackFor` / `noRollbackFor`：指定哪些异常触发/不触发回滚
- `transactionManager`：指定使用的事务管理器（多数据源场景）

---

### 2.2 传播行为（Propagation）

| 值 | 语义 | 说明 |
|---|---|---|
| `REQUIRED`（默认） | 有则加入，无则新建 | 最常用 |
| `REQUIRES_NEW` | **挂起**当前事务，开启新事务 | 适合写日志/独立操作 |
| `NESTED` | 嵌套事务（保存点） | 依赖底层数据源支持（如 JDBC） |
| `SUPPORTS` | 有则加入，无则非事务执行 | 读操作可用 |
| `MANDATORY` | 必须存在外层事务，否则异常 | |
| `NOT_SUPPORTED` | 始终以非事务方式执行 | |
| `NEVER` | 必须无事务，否则异常 | |

**示例**：主流程失败但希望日志仍提交：

```java
@Service
public class LoggingService {
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void writeAudit(Audit a) { ... }
}
```

---

### 2.3 隔离级别（Isolation）

| 级别 | 脏读 | 不可重复读 | 幻读 | 说明 |
|---|---|---|---|---|
| `DEFAULT` | 取决于数据库 | | | 推荐 |
| `READ_UNCOMMITTED` | 可能 | 可能 | 可能 | 几乎不用 |
| `READ_COMMITTED` | 否 | 可能 | 可能 | 常见于 Oracle |
| `REPEATABLE_READ` | 否 | 否 | 可能* | MySQL 默认（基于 MVCC，通常也避免幻读） |
| `SERIALIZABLE` | 否 | 否 | 否 | 最严格，吞吐低 |

> 选择隔离级别需结合数据库类型与性能权衡。

---

### 2.4 回滚规则

- **默认**：仅**运行时异常**（`RuntimeException`）与 `Error` 自动回滚。  
- 若要**受检异常**也回滚：`@Transactional(rollbackFor = Exception.class)`。
- 对于**特定异常不回滚**：`noRollbackFor = SpecificException.class`。

---

### 2.5 常见陷阱与最佳实践

1) **自调用失效**：同类内一个 `@Transactional` 方法调用另一个，不经过代理 → 事务不生效。  
   - 解决：通过外部 Bean 调用，或注入自身代理 `AopContext.currentProxy()`，或使用 AspectJ。

2) **只读误解**：`readOnly = true` 不是强制禁止写入，只是**提示**优化。底层仍可能允许 `INSERT/UPDATE`。

3) **异常被吞**：捕获异常后未重新抛出 → 事务无法感知失败而提交。  
   - 解决：捕获后**转抛**运行时异常或 `TransactionSystemException`。

4) **异步方法**：`@Async` 新线程不在原事务上下文内。需要单独在异步方法上声明事务。

5) **多数据源**：指定 `transactionManager`，或使用分布式事务/柔性事务（如 Saga/消息表）。

6) **测试回滚**：`@Transactional` 放在测试类/方法上，默认用例执行完**自动回滚**。

7) **AOP 顺序**：与日志、权限等切面叠加时，用 `@Order` 控制拦截顺序（事务应尽量靠里层）。

---

### 2.6 `@EnableTransactionManagement`

**作用**：开启基于代理的注解驱动事务管理（Boot 默认已启用）。

关键属性：

- `mode`：`PROXY`（默认）或 `ASPECTJ`（需要编织支持）。
- `proxyTargetClass`：`true` 强制使用 CGLIB 代理（对类代理），否则优先 JDK 动态代理（对接口）。

```java
@Configuration
@EnableTransactionManagement(proxyTargetClass = true)
public class TxConfig {}
```

---

### 2.7 事务事件监听：`@TransactionalEventListener`

**作用**：监听在**事务生命周期**某个阶段发布的事件（如提交后发送消息）。

```java
@Component
public class DomainEventHandler {

  // 事务成功提交后再执行
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handle(OrderCreatedEvent evt) {
    // 发送消息、清缓存等
  }

  // 回滚后执行
  @TransactionalEventListener(phase = TransactionPhase.AFTER_ROLLBACK)
  public void onRollback(SomeEvent evt) { ... }
}
```

**触发方式**：在事务方法中 `ApplicationEventPublisher#publishEvent(event)`。

---

## 三、综合示例：统一错误响应 + 事务边界

```java
@RestController
@RequestMapping("/orders")
public class OrderApi {

  private final OrderService service;
  public OrderApi(OrderService service) { this.service = service; }

  @PostMapping
  public ResponseEntity<?> create(@Valid @RequestBody OrderDTO dto) {
    service.createOrder(dto); // 若抛异常由全局异常器接管
    return ResponseEntity.status(HttpStatus.CREATED).build();
  }
}

@Service
public class OrderService {

  private final LoggingService logging;
  private final OrderRepository repo;

  public OrderService(LoggingService logging, OrderRepository repo) {
    this.logging = logging; this.repo = repo;
  }

  @Transactional(rollbackFor = Exception.class)
  public void createOrder(OrderDTO dto) {
    var order = repo.save(mapper.toEntity(dto));
    try {
      // 业务副作用：写审计日志，独立新事务确保即使主事务失败也留下记录
      logging.writeAudit(Audit.from(order));
    } catch (Exception ex) {
      // 审计失败不影响主流程，可记录警告
    }
  }
}

@Service
public class LoggingService {
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void writeAudit(Audit a) { /* 单独提交 */ }
}
```

---

## 四、Checklist

- [ ] 控制器异常是否全部被 Advice 覆盖？是否有兜底 `Exception` 处理？  
- [ ] 使用 `ResponseEntityExceptionHandler` 或统一响应体？  
- [ ] 事务边界是否在 **Service 层**？是否避免长事务？  
- [ ] 是否存在**自调用**导致事务失效？  
- [ ] 是否根据场景正确选择传播行为与隔离级别？  
- [ ] 日志/审计是否用 `REQUIRES_NEW` 独立提交？  
- [ ] 校验异常（`MethodArgumentNotValidException` / `ConstraintViolationException`）是否已覆盖？  
- [ ] 测试用例是否用 `@Transactional` 自动回滚保持数据库清洁？

---
