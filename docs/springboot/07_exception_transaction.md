
# 全局异常处理与事务注解

---

## 全局异常处理

### @ControllerAdvice / @RestControllerAdvice（复合注解）

**作用**：控制器层切面，集中处理异常、数据绑定、模型属性。`@RestControllerAdvice` = `@ControllerAdvice` + `@ResponseBody`。  
**定义位置**：类上。

```java
@RestControllerAdvice
public class GlobalExceptionAdvice {

  @ExceptionHandler(MethodArgumentNotValidException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public Map<String, Object> handleBind(MethodArgumentNotValidException e) {
    var fieldErrors = e.getBindingResult().getFieldErrors();
    return Map.of(
      "message", "参数校验失败",
      "errors", fieldErrors.stream().map(f -> Map.of("field", f.getField(), "msg", f.getDefaultMessage())).toList()
    );
  }

  @ExceptionHandler(ConstraintViolationException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public Map<String, Object> handleConstraint(ConstraintViolationException e) {
    return Map.of("message", "参数非法", "errors", e.getMessage());
  }
}
```

### @ExceptionHandler

**作用**：声明本控制器或全局的异常处理方法（方法签名可接收异常对象、`WebRequest` 等）。

### @ResponseStatus

**作用**：在异常类或处理方法上声明固定状态码。

---

## 事务管理

### @Transactional

**作用**：声明式事务边界；支持传播行为（`REQUIRED`/`REQUIRES_NEW`/`NESTED` 等）、隔离级别、只读、超时、回滚规则。  
**定义位置**：类或方法上（优先以方法为准）。

```java
@Service
public class OrderService {
  @Transactional(rollbackFor = Exception.class)
  public void placeOrder(Order o) { /* 更新多表、远端调用等 */ }
}
```

**说明**：

- 默认仅在运行时异常触发回滚；`rollbackFor` 可扩展到受检异常。
- 代理生效前提：从容器外部调用代理方法（同类内部自调用无法触发，除非使用 `AspectJ` 或注入自身代理）。
- 建议作用在 **Service 层**；控制器层避免开启长事务。

### @EnableTransactionManagement

**作用**：启用基于代理的注解驱动事务管理（Boot 自动配置通常已开启）。

### @TransactionalEventListener

**作用**：基于事务事件回调（如在事务提交后发布领域事件）。

```java
@Component
class DomainEventHandler {
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handle(OrderCreatedEvent evt){ /*...*/ }
}
```
