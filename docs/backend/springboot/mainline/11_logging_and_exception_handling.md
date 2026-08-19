# 第11章 日志与统一异常

> 本章目标：建立统一异常返回和基础日志输出，能够定位常见后端错误。

## 一、为什么需要统一异常

如果每个 Controller 自己处理异常，返回格式会不一致。

统一异常处理可以保证：

- 前端收到稳定错误格式
- 后端日志有可排查信息
- 业务异常和系统异常分开处理
- Code Review 更容易确认异常边界

## 二、项目中的异常分类

| 类型 | 示例 | 返回方式 |
| --- | --- | --- |
| 参数错误 | 姓名为空、邮箱格式错误 | 返回 400 |
| 业务错误 | 员工不存在、邮箱重复 | 返回业务错误码和消息 |
| 系统错误 | 数据库连接失败、空指针异常 | 记录日志，返回统一系统错误 |

不要把所有异常都直接返回给前端。

前端需要稳定的错误格式，后端需要完整日志用于排查。

## 三、自定义业务异常

修改文件：

```text
src/main/java/com/example/employee/exception/BusinessException.java
```

```java
package com.example.employee.exception; // 异常包

public class BusinessException extends RuntimeException { // 业务异常

    private final String code; // 业务错误码

    public BusinessException(String message) { // 只传错误消息时使用
        this("BUSINESS_ERROR", message); // 使用默认业务错误码
    }

    public BusinessException(String code, String message) { // 同时传错误码和错误消息
        super(message); // 保存错误消息
        this.code = code; // 保存错误码
    }

    public String getCode() { // 获取错误码
        return code; // 返回错误码
    }
}
```

## 四、在业务代码中抛出异常

修改文件：

```text
src/main/java/com/example/employee/service/EmployeeService.java
```

示例：

```java
public EmployeeResponse findById(Long id) { // 根据 ID 查询员工
    Employee employee = employeeMapper.selectById(id); // 查询数据库
    if (employee == null) { // 员工不存在时
        throw new BusinessException("EMPLOYEE_NOT_FOUND", "员工不存在"); // 抛出业务异常
    }
    return EmployeeResponse.from(employee); // 返回员工响应对象
}
```

这里不要返回 `null`。

返回 `null` 会让 Controller 或前端继续猜测错误原因。

抛出明确的业务异常，再由全局异常处理器统一转换为响应。

## 五、全局异常处理

修改文件：

```text
src/main/java/com/example/employee/exception/GlobalExceptionHandler.java
```

```java
package com.example.employee.exception; // 异常包

import com.example.employee.common.ApiResponse; // 统一响应
import org.slf4j.Logger; // 日志接口
import org.slf4j.LoggerFactory; // 日志工厂
import org.springframework.web.bind.annotation.ExceptionHandler; // 异常处理方法
import org.springframework.web.bind.annotation.RestControllerAdvice; // 全局 REST 异常处理

@RestControllerAdvice // 对所有 REST Controller 生效
public class GlobalExceptionHandler { // 全局异常处理器

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class); // 创建日志对象

    @ExceptionHandler(BusinessException.class) // 捕获业务异常
    public ApiResponse<Void> handleBusinessException(BusinessException ex) { // 处理业务异常
        log.warn("business exception, code={}, message={}", ex.getCode(), ex.getMessage()); // 记录业务异常
        return ApiResponse.fail(ex.getCode(), ex.getMessage()); // 返回业务错误码和消息
    }

    @ExceptionHandler(Exception.class) // 捕获未预期异常
    public ApiResponse<Void> handleException(Exception ex) { // 处理系统异常
        log.error("system exception", ex); // 记录完整异常堆栈
        return ApiResponse.fail("SYSTEM_ERROR", "系统错误，请联系管理员"); // 不把内部异常细节直接返回前端
    }
}
```

`@RestControllerAdvice` 表示这个类用于处理所有 REST Controller 抛出的异常。

`@ExceptionHandler(BusinessException.class)` 表示这个方法只处理 `BusinessException`。

`@ExceptionHandler(Exception.class)` 用于兜底处理未预期异常。

## 六、日志基础

```java
import org.slf4j.Logger; // 日志接口
import org.slf4j.LoggerFactory; // 日志工厂

private static final Logger log = LoggerFactory.getLogger(EmployeeService.class); // 创建当前类日志对象

log.info("create employee, name={}", request.getName()); // 输出业务处理日志
```

日志不要输出密码、密钥、完整个人敏感信息。

常用日志级别：

| 级别 | 使用场景 |
| --- | --- |
| `debug` | 开发调试信息，生产环境通常不打开 |
| `info` | 正常业务流程关键节点 |
| `warn` | 可恢复或需要关注的问题 |
| `error` | 系统异常、不可恢复错误 |

## 七、员工新增中的日志示例

```java
@Transactional // 新增员工需要事务
public EmployeeResponse create(EmployeeCreateRequest request) { // 新增员工
    log.info("create employee start, name={}, departmentId={}", request.getName(), request.getDepartmentId()); // 记录开始日志

    Employee employee = new Employee(); // 创建员工实体
    employee.setName(request.getName()); // 设置姓名
    employee.setDepartmentId(request.getDepartmentId()); // 设置部门 ID
    employee.setEmail(request.getEmail()); // 设置邮箱
    employee.setStatus("ACTIVE"); // 设置默认状态

    int count = employeeMapper.insert(employee); // 执行新增
    if (count != 1) { // 判断影响行数
        log.warn("create employee failed, name={}", request.getName()); // 记录失败日志
        throw new BusinessException("EMPLOYEE_CREATE_FAILED", "员工新增失败"); // 抛出业务异常
    }

    log.info("create employee success, id={}", employee.getId()); // 记录成功日志
    return EmployeeResponse.from(employee); // 返回响应
}
```

## 八、常见错误

| 错误 | 原因 | 修正 |
| --- | --- | --- |
| Controller 到处写 try-catch | 缺少统一异常处理 | 使用 `@RestControllerAdvice` |
| 把异常堆栈返回前端 | 泄露内部实现 | 前端只返回统一错误信息 |
| 日志没有关键参数 | 无法排查具体数据 | 记录 ID、状态、业务关键值 |
| 日志输出密码 | 泄露敏感信息 | 密码、Token、密钥不写日志 |

## 九、本章练习

请完成：

1. 创建 `BusinessException`。
2. 创建 `GlobalExceptionHandler`。
3. 员工不存在时返回统一错误结构。
4. 在新增员工时输出一条业务日志。
5. 模拟一个系统异常，确认日志中能看到异常堆栈。

## 十、本章总结

- 业务异常和系统异常要分开处理。
- `@RestControllerAdvice` 用于统一处理 Controller 抛出的异常。
- 日志用于排查问题，不是给前端看的错误信息。
- 生产日志不能输出密码、密钥、Token 等敏感信息。
