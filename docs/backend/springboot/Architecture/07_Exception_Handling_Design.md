# 07 异常处理设计（@ControllerAdvice 教学版）

> 本章节用于 **Spring Boot 项目中的异常处理教学 / 面试准备 / 架构规范说明**  
> 目标：讲清楚 **为什么要统一异常处理、如何设计异常体系、@ControllerAdvice 的正确用法**

---

## 一、为什么需要统一异常处理？

如果不做统一异常处理，系统通常会出现以下问题：

- Controller 中大量 try-catch
- 返回给前端的错误格式不统一
- 异常信息暴露内部实现（安全风险）
- 难以维护和扩展

👉 因此，企业级项目必须进行 **统一异常处理设计**。

---

## 二、异常处理的总体设计目标

- 接口返回结构统一
- 内部异常不直接暴露
- 业务异常与系统异常区分
- 方便日志记录与问题定位

---

## 三、Spring Boot 异常处理的核心机制

Spring Boot 提供了三种主要异常处理方式：

1. Controller 内 try-catch（❌ 不推荐）
2. @ExceptionHandler（局部）
3. **@ControllerAdvice（全局，推荐）**

---

## 四、@ControllerAdvice 的作用

### 1. 定义

`@ControllerAdvice` 用于定义**全局异常处理器**，  
可以拦截 Controller 抛出的异常并统一处理。

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
}
```

---

### 2. 优点

- 与业务代码解耦
- 所有 Controller 共用
- 返回格式统一

---

## 五、异常分类设计（教学重点）

### 1. 系统异常（System Exception）
- NullPointerException
- SQLException
- 系统运行错误

特点：
- 不可预期
- 通常记录日志
- 返回通用错误信息

---

### 2. 业务异常（Business Exception）
- 参数不合法
- 状态不允许操作
- 业务规则校验失败

特点：
- 可预期
- 提示用户友好信息

---

## 六、自定义业务异常设计

### 1. 定义业务异常类

```java
public class BusinessException extends RuntimeException {

    private final String errorCode;

    public BusinessException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
```

---

## 七、全局异常处理器示例

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ErrorResponse handleBusinessException(BusinessException ex) {
        return new ErrorResponse(ex.getErrorCode(), ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ErrorResponse handleException(Exception ex) {
        return new ErrorResponse("SYSTEM_ERROR", "系统异常，请稍后重试");
    }
}
```

---

## 八、统一错误返回结构设计

### 示例：ErrorResponse

```java
public class ErrorResponse {
    private String code;
    private String message;
}
```

推荐返回结构：

```json
{
  "code": "ORDER_NOT_FOUND",
  "message": "订单不存在"
}
```

---

## 九、异常处理与日志的配合

设计原则：
- 业务异常：warn 日志
- 系统异常：error 日志
- 不重复打印日志

---

## 十、异常处理常见误区（面试高频）

### 1. 捕获异常但不处理
```java
try {
} catch (Exception e) {
}
```

❌ 严重问题

---

### 2. 直接返回异常信息给前端

```json
"message": "NullPointerException at xxx"
```

❌ 安全风险

---

## 十一、异常处理与事务的关系

- 抛出 RuntimeException → 事务回滚
- 捕获异常但不抛出 → 事务不回滚

👉 异常设计必须与事务设计配合

---

## 十二、推荐包结构

```text
com.example.demo
 ├─ exception
 │   ├─ BusinessException.java
 │   ├─ GlobalExceptionHandler.java
 │   └─ ErrorResponse.java
```

---

## 十三、面试总结用语（可直接背）

> 在 Spring Boot 项目中，通常通过 @ControllerAdvice 实现全局异常处理，  
> 将业务异常与系统异常分离，统一接口返回格式，  
> 既提高了系统可维护性，也避免了内部异常信息泄露。
