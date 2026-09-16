# 第11章 用日志定位接口故障

> 本章目标：为第10章员工CRUD增加请求追踪、关键业务日志和异常日志，并完成一次“复现→定位→修复→确认”的故障调查。

接口返回400、404、409或500只告诉调用方处理结果。开发人员还需要回答：请求何时到达、访问了什么路径、员工编号是什么、在哪一层失败、修复后是否恢复。日志就是程序运行时留下的可检索证据。

本章不改变CRUD业务规格，只增加诊断能力：

```text
HTTP请求
  → 分配requestId
  → 记录关键业务事实
  → 成功或异常响应
  → 记录method、path、status、elapsedMs
```

## 一、开始状态与改动范围

第10章的Controller、DTO、Mapper、XML和数据库表保持不变。本章修改或新建：

```text
src/main/java/com/example/employee/
├── config/RequestLoggingFilter.java                 ← 新建
├── exception/
│   ├── EmployeeNotFoundException.java               ← 完整替换
│   └── GlobalExceptionHandler.java                  ← 完整替换
└── service/impl/EmployeeServiceImpl.java            ← 完整替换

src/main/resources/application.yml                   ← 完整替换
```

SLF4J API和默认Logback实现已经由Spring Boot Web Starter提供，本章不新增Maven依赖，也不在 `pom.xml` 中另外指定日志库版本。

## 二、完整示例

先完成本节全部文件，再从第三节开始逐项理解第一次出现的日志对象、级别、占位符、MDC和滚动配置。

### 1. 完整替换application.yml

```yaml
server:
  port: 8080

spring:
  datasource:
    url: ${DB_URL:jdbc:mysql://localhost:3306/employee_db?useUnicode=true&characterEncoding=UTF-8&connectionTimeZone=Asia/Tokyo}
    username: ${DB_USERNAME:employee_app}
    password: ${DB_PASSWORD}
    driver-class-name: com.mysql.cj.jdbc.Driver

mybatis:
  mapper-locations: classpath:mapper/*.xml
  type-aliases-package: com.example.employee.entity

logging:
  level:
    root: INFO
    com.example.employee: ${APP_LOG_LEVEL:INFO}
  file:
    name: logs/employee-api.log
  logback:
    rollingpolicy:
      max-file-size: 10MB
      max-history: 7
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss.SSS} %-5level [%X{requestId:-no-request}] %logger{36} - %msg%n"
    file: "%d{yyyy-MM-dd HH:mm:ss.SSS} %-5level [%X{requestId:-no-request}] %logger{36} - %msg%n"
```

### 2. 新建RequestLoggingFilter.java

文件位置：

```text
src/main/java/com/example/employee/config/RequestLoggingFilter.java
```

完整内容：

```java
package com.example.employee.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger log =
            LoggerFactory.getLogger(RequestLoggingFilter.class);

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {
        String requestId = UUID.randomUUID().toString();
        long startedAt = System.nanoTime();

        MDC.put("requestId", requestId);
        response.setHeader("X-Request-Id", requestId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            long elapsedMs =
                    (System.nanoTime() - startedAt) / 1_000_000;
            log.info(
                    "request_complete method={} path={} status={} elapsedMs={}",
                    request.getMethod(),
                    request.getRequestURI(),
                    response.getStatus(),
                    elapsedMs);
            MDC.remove("requestId");
        }
    }
}
```

### 3. 完整替换EmployeeNotFoundException.java

```java
package com.example.employee.exception;

public class EmployeeNotFoundException extends RuntimeException {

    private final Long employeeId;

    public EmployeeNotFoundException(Long employeeId) {
        super("员工不存在：" + employeeId);
        this.employeeId = employeeId;
    }

    public Long getEmployeeId() {
        return employeeId;
    }
}
```

异常继续保存对外业务消息，同时单独保存结构明确的员工编号。日志不需要从错误消息文本中截取编号。

### 4. 完整替换EmployeeServiceImpl.java

```java
package com.example.employee.service.impl;

import com.example.employee.dto.request.EmployeeCreateRequest;
import com.example.employee.dto.request.EmployeeUpdateRequest;
import com.example.employee.dto.response.EmployeeListItemResponse;
import com.example.employee.dto.response.EmployeeResponse;
import com.example.employee.entity.Employee;
import com.example.employee.exception.DuplicateEmailException;
import com.example.employee.exception.EmployeeNotFoundException;
import com.example.employee.exception.EmployeeSystemException;
import com.example.employee.exception.InvalidDepartmentException;
import com.example.employee.mapper.EmployeeMapper;
import com.example.employee.service.EmployeeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class EmployeeServiceImpl implements EmployeeService {

    private static final Logger log =
            LoggerFactory.getLogger(EmployeeServiceImpl.class);

    private final EmployeeMapper employeeMapper;

    public EmployeeServiceImpl(EmployeeMapper employeeMapper) {
        this.employeeMapper = employeeMapper;
    }

    @Override
    public EmployeeResponse findById(Long id) {
        return toResponse(findEmployeeOrThrow(id));
    }

    @Override
    public List<EmployeeListItemResponse> findList(String department) {
        String filter = normalizeDepartmentFilter(department);
        List<Employee> employees = employeeMapper.findList(filter);
        List<EmployeeListItemResponse> responses = new ArrayList<>();

        for (Employee employee : employees) {
            responses.add(toListItemResponse(employee));
        }

        log.debug(
                "employee_list_completed filterApplied={} resultCount={}",
                filter != null,
                responses.size());
        return responses;
    }

    @Override
    public EmployeeResponse create(EmployeeCreateRequest request) {
        validateDepartment(request.getDepartment());

        Employee employee = new Employee();
        employee.setName(request.getName().trim());
        employee.setDepartment(request.getDepartment().trim());
        employee.setEmail(normalizeEmail(request.getEmail()));
        employee.setStatus("ACTIVE");

        try {
            int affectedRows = employeeMapper.insert(employee);
            if (affectedRows != 1 || employee.getId() == null) {
                throw new EmployeeSystemException("新增员工结果异常");
            }
        } catch (DuplicateKeyException exception) {
            throw new DuplicateEmailException(employee.getEmail());
        }

        log.info(
                "employee_created employeeId={} department={}",
                employee.getId(),
                employee.getDepartment());
        return findById(employee.getId());
    }

    @Override
    public EmployeeResponse update(
            Long id,
            EmployeeUpdateRequest request) {
        validateDepartment(request.getDepartment());
        findEmployeeOrThrow(id);

        Employee employee = new Employee();
        employee.setId(id);
        employee.setName(request.getName().trim());
        employee.setDepartment(request.getDepartment().trim());
        employee.setEmail(normalizeEmail(request.getEmail()));

        try {
            int affectedRows = employeeMapper.update(employee);
            if (affectedRows > 1) {
                throw new EmployeeSystemException("修改员工影响多行");
            }
            if (affectedRows == 0
                    && employeeMapper.findById(id) == null) {
                throw new EmployeeNotFoundException(id);
            }
        } catch (DuplicateKeyException exception) {
            throw new DuplicateEmailException(employee.getEmail());
        }

        log.info(
                "employee_updated employeeId={} department={}",
                id,
                employee.getDepartment());
        return findById(id);
    }

    @Override
    public void delete(Long id) {
        int affectedRows = employeeMapper.deleteById(id);

        if (affectedRows == 0) {
            throw new EmployeeNotFoundException(id);
        }
        if (affectedRows != 1) {
            throw new EmployeeSystemException("删除员工影响多行");
        }

        log.info("employee_deleted employeeId={}", id);
    }

    private Employee findEmployeeOrThrow(Long id) {
        Employee employee = employeeMapper.findById(id);
        if (employee == null) {
            throw new EmployeeNotFoundException(id);
        }
        return employee;
    }

    private EmployeeResponse toResponse(Employee employee) {
        return new EmployeeResponse(
                employee.getId(),
                employee.getName(),
                employee.getDepartment(),
                employee.getEmail());
    }

    private EmployeeListItemResponse toListItemResponse(
            Employee employee) {
        return new EmployeeListItemResponse(
                employee.getId(),
                employee.getName(),
                employee.getDepartment());
    }

    private void validateDepartment(String department) {
        if (!"Sales".equals(department)
                && !"Development".equals(department)
                && !"Support".equals(department)) {
            throw new InvalidDepartmentException(department);
        }
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeDepartmentFilter(String department) {
        if (department == null || department.isBlank()) {
            return null;
        }
        return department.trim();
    }
}
```

### 5. 完整替换GlobalExceptionHandler.java

```java
package com.example.employee.exception;

import com.example.employee.common.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log =
            LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidation(
            MethodArgumentNotValidException exception,
            HttpServletRequest request) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();

        for (FieldError fieldError
                : exception.getBindingResult().getFieldErrors()) {
            fieldErrors.putIfAbsent(
                    fieldError.getField(),
                    fieldError.getDefaultMessage());
        }

        log.warn(
                "request_rejected reason=validation path={} fields={}",
                request.getRequestURI(),
                fieldErrors.keySet());
        return ResponseEntity
                .badRequest()
                .body(ApiResponse.failure(
                        "参数校验失败",
                        fieldErrors));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnreadableJson(
            HttpMessageNotReadableException exception,
            HttpServletRequest request) {
        log.warn(
                "request_rejected reason=unreadable_json path={}",
                request.getRequestURI());
        return ResponseEntity
                .badRequest()
                .body(ApiResponse.failure("请求JSON格式错误"));
    }

    @ExceptionHandler(InvalidDepartmentException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidDepartment(
            InvalidDepartmentException exception,
            HttpServletRequest request) {
        log.warn(
                "request_rejected reason=invalid_department path={}",
                request.getRequestURI());
        return ResponseEntity
                .badRequest()
                .body(ApiResponse.failure(exception.getMessage()));
    }

    @ExceptionHandler(EmployeeNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleEmployeeNotFound(
            EmployeeNotFoundException exception,
            HttpServletRequest request) {
        log.warn(
                "employee_not_found path={} employeeId={}",
                request.getRequestURI(),
                exception.getEmployeeId());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.failure(exception.getMessage()));
    }

    @ExceptionHandler(DuplicateEmailException.class)
    public ResponseEntity<ApiResponse<Void>> handleDuplicateEmail(
            DuplicateEmailException exception,
            HttpServletRequest request) {
        log.warn(
                "employee_conflict reason=duplicate_email path={}",
                request.getRequestURI());
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ApiResponse.failure(exception.getMessage()));
    }

    @ExceptionHandler(EmployeeSystemException.class)
    public ResponseEntity<ApiResponse<Void>> handleEmployeeSystem(
            EmployeeSystemException exception,
            HttpServletRequest request) {
        log.error(
                "employee_system_error path={}",
                request.getRequestURI(),
                exception);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.failure("服务器内部错误"));
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataAccess(
            DataAccessException exception,
            HttpServletRequest request) {
        log.error(
                "database_access_error path={}",
                request.getRequestURI(),
                exception);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.failure("服务器内部错误"));
    }
}
```

## 三、SLF4J、Logger和LoggerFactory是什么

SLF4J是一套Java日志接口，业务代码面向它写日志；Spring Boot默认使用Logback执行实际输出。这样代码不必直接依赖某一个日志实现。

- `Logger` 是记录日志的接口。
- `LoggerFactory.getLogger(CurrentClass.class)` 接收当前类的 `Class`对象，返回以该类名命名的Logger。
- `private static final` 表示同一类共享一个不可重新赋值的Logger引用，不需要为每个Service对象重复取得。

Spring Boot Starter已经提供兼容组合，不要再手动加入另一套SLF4J实现，否则可能出现多个日志提供者冲突。SLF4J的接口定位和典型用法可参考[SLF4J官方手册](https://slf4j.org/manual.html)。

## 四、日志级别怎样选择

| 级别 | 当前项目中的用途 | 默认INFO时是否输出 |
| --- | --- | --- |
| `debug` | 查询条件是否存在、结果数量等调试细节 | 否 |
| `info` | 员工新增、修改、删除成功和请求完成 | 是 |
| `warn` | 可预期的400、404、409业务失败 | 是 |
| `error` | 数据库故障或内部系统异常，并记录堆栈 | 是 |

级别表示事件严重性，不表示代码所在层。正常的查询结果为空不是错误；数据库无法连接也不能只记成debug。

本地临时查看debug日志时，在启动应用的同一PowerShell设置：

```powershell
$env:APP_LOG_LEVEL = "DEBUG"
```

验证完成后删除当前进程变量并重启应用：

```powershell
Remove-Item Env:APP_LOG_LEVEL
```

生产环境长期打开大量debug会增加存储、检索和信息暴露风险，应遵循项目运行方针。

## 五、占位符和异常堆栈

下面的 `{}` 是SLF4J参数占位符，参数按顺序填入：

```java
log.info(
        "employee_updated employeeId={} department={}",
        id,
        employee.getDepartment());
```

不要用字符串拼接构造普通日志。占位写法更清楚，而且日志级别关闭时可以避免不必要的字符串拼接。

记录异常堆栈时，把异常对象放在最后一个参数：

```java
log.error("database_access_error path={}", path, exception);
```

只写 `exception.getMessage()` 会丢失异常类型、调用位置和原因链。堆栈中的 `Caused by` 表示下一层原因；定位时通常从最底部的具体数据库、SQL或Java异常向上核对调用链。

业务性的404和409通常不需要整段堆栈，否则正常业务分支会制造大量噪声。系统异常才使用 `error`并保留堆栈。

## 六、一次请求为什么需要requestId

多个用户可能同时调用接口，日志会交错。`MDC.put("requestId", value)` 把请求编号放入当前执行上下文，日志格式中的 `%X{requestId:-no-request}` 会自动输出它。

`MDC` 的完整名称是Mapped Diagnostic Context（映射诊断上下文），类位于 `org.slf4j` 包。它保存的是当前执行上下文中的诊断键值，不是员工业务数据，也不是返回给前端的Map。

`RequestLoggingFilter` 中第一次出现的类型和方法如下：

| 类型或方法 | 参数与可接受值 | 返回值或运行效果 |
| --- | --- | --- |
| `OncePerRequestFilter` | 项目继承的Spring Web基类 | 为请求提供一次过滤处理入口 |
| `HttpServletRequest` | 由Servlet容器为当前HTTP请求提供 | 可读取方法、路径等请求信息 |
| `HttpServletResponse` | 由Servlet容器为当前HTTP响应提供 | 可写入响应头并读取当前状态 |
| `FilterChain` | 由容器组建的后续处理链 | 决定当前过滤器之后还要继续执行哪些处理 |
| `doFilterInternal()` | request、response、filterChain均由容器提供 | Spring Web调用的重写入口，执行本项目的请求日志逻辑 |
| `filterChain.doFilter()` | 当前请求和响应，必填 | 继续进入Controller等后续处理 |
| `UUID.randomUUID()` | 无参数 | 返回随机UUID作为本次请求编号 |
| `MDC.put()` | 非空键和字符串值 | 让后续同一执行上下文的日志带requestId |
| `MDC.remove()` | 要删除的键 | 请求结束时清理，防止线程复用造成串号 |
| `response.setHeader()` | 响应头名与字符串值 | 设置或替换指定响应头，本章写入requestId |
| `request.getMethod()` | 无参数 | 返回GET、POST等HTTP方法名 |
| `request.getRequestURI()` | 无参数 | 返回请求路径，不包含查询字符串 |
| `response.getStatus()` | 无参数 | 返回当前HTTP响应状态整数 |

`doFilterInternal()` 声明的 `ServletException` 表示Servlet处理失败，`IOException` 表示请求或响应读写失败。它们都是后续处理链可能抛出的受检异常，本过滤器不擅自改变其含义，因此按重写方法签名继续声明。

`try...finally` 保证正常和异常路径都会记录耗时并清理MDC。`System.nanoTime()` 无参数，返回只适合计算经过时间的 `long`值，不用来显示日期；两次结果相减后除以1,000,000得到毫秒。

过滤器只记录 `getRequestURI()`，不记录查询字符串、请求体或请求头。响应中的 `X-Request-Id` 让调用方可以把一次失败与服务端日志对应起来。

## 七、应该记录哪些业务事实

本章只在写操作成功后记录：操作名称、员工编号和已验证的部门。列表debug日志只记录是否使用筛选和结果数量，不记录原始用户输入。

不要记录：

- 数据库密码、Token、Cookie、Authorization请求头；
- 完整请求体；
- 员工姓名、邮箱等没有排查必要的个人信息；
- SQL拼接后的敏感数据；
- “进入方法”“执行下一行”之类没有诊断价值的逐行轨迹。

日志需要同时满足“能定位”和“不过度收集”。即使员工编号本身不是密码，也应受访问权限、保存期限和项目规则约束。

## 八、日志配置和滚动文件

`logging.file.name` 同时启用控制台和文件输出；相对路径 `logs/employee-api.log` 以应用启动工作目录为基准。目录不存在时，日志系统会尝试创建。

| 配置 | 可接受的值 | 默认值或本章值 | 作用 |
| --- | --- | --- | --- |
| `logging.level.root` | TRACE、DEBUG、INFO、WARN、ERROR、OFF等 | 本章INFO | 设置全局最低输出级别 |
| `logging.level.com.example.employee` | 同上，也可来自环境变量 | 默认INFO | 单独控制项目包日志 |
| `logging.file.name` | 可写文件路径 | `logs/employee-api.log` | 设置当前日志文件 |
| `max-file-size` | 合法数据大小 | 10MB | 当前文件达到大小后滚动归档 |
| `max-history` | 非负整数 | 7 | 限制保留的历史归档周期 |
| `logging.pattern.console/file` | Logback格式字符串 | 本章固定格式 | 决定时间、级别、requestId、类名和消息顺序 |

日志滚动控制单个文件和历史数量，但不能替代磁盘监控、备份和集中日志平台。Spring Boot支持的文件输出方式可参考[Spring Boot日志说明](https://docs.spring.io/spring-boot/how-to/logging.html)。

## 九、异常日志与对外响应怎样分工

全局异常处理器做两件不同的事：

```text
服务端日志：给开发和运维排查，可保存受控堆栈
HTTP响应：给调用方判断，只返回稳定状态和公开消息
```

`HttpServletRequest` 来自 `jakarta.servlet.http`，由Spring提供给异常处理方法。`getRequestURI()` 返回当前路径，使失败日志可以定位接口。

`DataAccessException` 是Spring统一的数据访问异常父类。无法连接数据库、SQL执行失败等异常会记录完整堆栈并返回通用500；第10章已经转换过的重复邮箱仍由更具体的业务异常返回409。

本章仍不添加 `@ExceptionHandler(Exception.class)`。笼统捕获会把方法不支持、媒体类型不支持等框架异常也改成统一500，破坏已有405和415语义。未预期编程错误由Spring Boot记录；只有项目明确设计并回归全部框架错误后，才应加入项目级兜底。

同一异常不要在Mapper、Service和全局处理器连续记录三次。Service负责转换已知业务含义，最终处理器负责记录一次；重复日志会让一次故障看起来像发生了多次。

## 十、运行并读取日志

设置数据库环境变量并启动应用，完成一次详情查询、一次不存在查询和一次新增：

```powershell
.\mvnw.cmd spring-boot:run
```

响应头中应出现 `X-Request-Id`。下面是格式示例，时间、线程、UUID、耗时和员工编号以实际结果为准：

```text
2026-09-14 10:20:31.123 INFO  [7f...c2] c.e.e.c.RequestLoggingFilter - request_complete method=GET path=/employees/1001 status=200 elapsedMs=28
2026-09-14 10:20:35.456 WARN  [a1...9d] c.e.e.e.GlobalExceptionHandler - employee_not_found path=/employees/999999 employeeId=999999
2026-09-14 10:20:35.457 INFO  [a1...9d] c.e.e.c.RequestLoggingFilter - request_complete method=GET path=/employees/999999 status=404 elapsedMs=7
```

读取文件最后100行：

```powershell
Get-Content -LiteralPath ".\logs\employee-api.log" -Tail 100
```

按员工编号、状态或请求编号检索：

```powershell
Select-String `
    -LiteralPath ".\logs\employee-api.log" `
    -Pattern "employeeId=1001", "status=500", "7f...c2"
```

查找时先缩小时间范围，再用requestId串起同一次请求，最后沿类名和异常原因进入代码。不要只看到最后一条500就猜测原因。

## 十一、四类故障的排查起点

| 现象 | 第一检查点 | 后续证据 |
| --- | --- | --- |
| 应用无法启动 | 启动日志最底部原因 | 配置名、端口、Bean、数据库连接 |
| 请求格式错误 | HTTP状态和全局处理日志 | 路径、方法、Content-Type、字段错误 |
| 业务失败 | warn日志和业务编号 | employeeId、规则、数据库当前状态 |
| 系统异常 | error日志和完整原因链 | requestId、堆栈、SQL/连接根因、复现条件 |

日志指出运行到了哪里，断点可以检查当时对象里有什么值。需要确认跨层传值时，可依次在Controller参数、Service方法、Mapper调用前设置断点；不要为了调试把完整DTO长期打印到日志。

## 十二、完成一次故障调查

### 1. 安全制造连接故障

先停止应用，把当前PowerShell中的 `DB_PASSWORD` 临时改成错误值并重启。调用详情接口，记录响应状态、`X-Request-Id`和日志最底部数据库认证原因。不要反复重试，不要修改数据库账号，也不要把错误或正确密码写进证据。

随后恢复正确的环境变量并重启，再次调用同一接口确认200。恢复动作是本次实验的一部分。

### 2. 问题记录必须形成闭环

使用下面结构整理：

| 项目 | 应记录的内容 |
| --- | --- |
| 现象 | 哪个接口、什么状态、何时发生 |
| 复现 | 前置条件和最短操作步骤 |
| 证据 | requestId、必要日志、响应和数据库状态；隐藏敏感信息 |
| 原因 | 最底层异常与错误配置或代码位置 |
| 修复 | 修改了什么，为什么能够消除原因 |
| 确认 | 原场景通过，相关CRUD回归，临时状态已恢复 |

### 3. Review与自测任务

1. Review `log.info("request={}", request)`，指出可能泄露的字段并改成必要的业务事实。
2. Review“Service catch异常后打印error再原样抛出、全局处理器再次打印error”，说明重复日志的影响并选择唯一记录位置。
3. 临时把项目包级别设为DEBUG，验证列表debug出现；恢复INFO并证明它不再输出。
4. 对404、409和数据库500分别保存状态、requestId、关键日志和判定。
5. 使用断点核对一次PUT中路径id、Update DTO、Employee Entity和Mapper参数，确认日志不代替对象检查。

## 十三、Spring共通处理机制的位置

本章已经实际使用Servlet Filter，第7章使用ControllerAdvice，第13章和第16章还会使用事务代理与方法安全代理。它们都能执行“共通处理”，但作用位置和适用问题不同。

### 1. 先建立课程级简化流程

```text
HTTP Request
  → Servlet Filter
  → Spring Security Filter Chain
  → DispatcherServlet
  → HandlerInterceptor preHandle
  → Controller
  → Service上的Spring Proxy / AOP
  → Mapper
  ← HandlerInterceptor postHandle / afterCompletion
  ← HTTP Response
```

这是一张帮助定位的简化图，不是所有配置下绝对固定的源码调用栈。一个请求可能经过多个Filter、多个Interceptor和多层代理；异常、异步请求及响应已经提交等情况也会改变可执行的回调。调查真实项目时仍要查看注册顺序和日志证据。

Spring Security本身由一组Servlet Filter组成，因此它位于进入Controller之前。Filter顺序配置错误可能让日志、CORS或安全行为变化，不能只根据类名推断执行次序。

### 2. Filter适合HTTP入口级处理

当前 `RequestLoggingFilter extends OncePerRequestFilter` 属于Servlet层。它面对的是请求和响应对象，不依赖某个Controller方法，适合：

- 建立requestId和HTTP访问日志；
- 统一字符编码或请求/响应包装；
- Spring Security等入口安全处理；
- 在非常早的阶段拒绝不合规请求。

`OncePerRequestFilter` 的目标是让一次请求分派按其规则执行一次过滤逻辑，但异步和错误分派仍有专门行为。它不是“每个业务方法只执行一次”的AOP工具。

### 3. 完整的HandlerInterceptor独立实验

下面实验观察Controller前后时机，不替换RequestLoggingFilter。新建 `dilab/RequestTimingInterceptor.java`：

```java
package com.example.employee.dilab;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

@Component
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
        log.debug("controller returned status={}", response.getStatus());
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
            log.info("mvc completed status={} elapsedMs={}",
                    response.getStatus(), elapsedNanos / 1_000_000);
        }
    }
}
```

再新建 `dilab/InterceptorLabConfig.java` 完成注册：

```java
package com.example.employee.dilab;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class InterceptorLabConfig implements WebMvcConfigurer {

    private final RequestTimingInterceptor requestTimingInterceptor;

    public InterceptorLabConfig(
            RequestTimingInterceptor requestTimingInterceptor) {
        this.requestTimingInterceptor = requestTimingInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(requestTimingInterceptor)
                .addPathPatterns("/employees/**");
    }
}
```

`HandlerInterceptor` 来自 `org.springframework.web.servlet`。`WebMvcConfigurer` 是Spring MVC配置回调接口；实现 `addInterceptors` 后，通过 `InterceptorRegistry` 注册实例及路径范围。`addPathPatterns("/employees/**")` 只匹配员工路径，避免实验影响health。

三个回调的意义：

| 方法 | 时机 | 返回或参数 | 适合 |
| --- | --- | --- | --- |
| `preHandle` | Controller执行前 | 返回true继续，false停止链 | 计时开始、MVC上下文检查 |
| `postHandle` | Controller正常执行后 | 可看到ModelAndView | 传统视图模型后处理 |
| `afterCompletion` | 请求完成后的清理阶段 | 可接收处理异常 | 计时结束、清理资源、最终记录 |

对于 `@ResponseBody`、`@RestController` 和 `ResponseEntity`，响应可能在 `postHandle` 前已经写出，因此不要把“修改REST响应正文或响应头”的关键逻辑放在 `postHandle`。Spring官方也明确说明这一边界，参见[HandlerInterceptor说明](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-servlet/handlermapping-interceptor.html)。

`System.nanoTime()` 适合计算同一进程内的经过时间，不代表日期时间；相减后除以1,000,000得到近似毫秒。日志不输出请求体、Cookie或Authorization。

运行员工详情、404和业务异常请求，观察preHandle与afterCompletion证据。完成后删除两个 `dilab` 文件并执行全部测试，主线不保留第二套重复访问日志。

### 4. ControllerAdvice处理MVC异常和共通响应

`@RestControllerAdvice` 让第7章的全局异常处理器参与Controller调用阶段的异常转换。它更接近MVC异常解析：业务异常进入后，根据 `@ExceptionHandler` 选择处理方法并生成HTTP响应。

它不替代Servlet Filter，也不能自然处理Spring Security在Controller之前拒绝的401/403。第16章因此使用 `AuthenticationEntryPoint` 和 `AccessDeniedHandler`。

### 5. AOP和Spring Proxy只做基础识读

Spring AOP可以通过代理在Bean方法调用前后加入处理。课程已经使用两个典型能力：

- `@Transactional`：代理在Service方法前开启或加入事务，正常返回时提交，符合条件的异常时回滚；
- `@PreAuthorize`：方法安全代理在目标方法执行前检查授权表达式。

既存项目自定义AOP时常见：

| 注解 | 所属 | 识读含义 |
| --- | --- | --- |
| `@Aspect` | AspectJ注解、由Spring AOP使用 | 声明一个切面类 |
| `@Before` | AspectJ注解 | 匹配的方法调用前执行 |
| `@AfterReturning` | AspectJ注解 | 匹配的方法正常返回后执行 |
| `@Around` | AspectJ注解 | 包围方法调用，必须正确调用 `proceed()` 才会继续目标方法 |

本课程不创建自定义切面。看到这些注解时先调查切点表达式匹配哪些Bean方法、是否记录敏感参数、异常是否被改变，以及调用是否真正经过Spring代理。Spring AOP是代理式AOP，`@Transactional` 的同类内部调用问题正来自这一边界。官方概念见[Spring AOP代理](https://docs.spring.io/spring-framework/reference/core/aop/introduction-proxies.html)和[Advice说明](https://docs.spring.io/spring-framework/reference/core/aop/ataspectj/advice.html)。

### 6. 四种机制不是同一个问题的四种写法

| 机制 | 常见位置 | 适合做什么 | 不适合替代 |
| --- | --- | --- | --- |
| Filter | Servlet层、MVC之前 | HTTP日志、Security、编码、请求包装 | 具体Service业务规则 |
| Interceptor | Spring MVC、Controller前后 | Handler相关计时、MVC共通处理 | 主要安全边界、任意Bean方法 |
| ControllerAdvice | MVC异常/响应处理 | 全局业务异常、Controller共通转换 | Security过滤器拒绝、数据库事务 |
| AOP / Spring Proxy | Spring Bean方法调用 | Transaction、方法权限、横切逻辑 | 原始HTTP报文处理 |

排查共通逻辑时先问“问题发生在HTTP入口、MVC Handler、异常转换还是Bean方法调用”，再找对应机制。不要为了统一而把所有逻辑都塞进Filter或AOP。

### 7. 识读与Review任务

1. 为一次员工详情请求标出RequestLoggingFilter、Security、Interceptor、Controller、Service事务代理和Mapper的大致位置。
2. Review“在Interceptor读取 `X-Role` 并授予ADMIN”的设计，说明为什么应交给Spring Security和可信身份来源。
3. Review一个 `@Around` 切面记录所有方法参数的方案，列出密码、Token、个人数据和大对象风险。
4. 制造Controller异常，比较Filter日志、Interceptor afterCompletion和ControllerAdvice各自能看到的证据。
5. 删除实验文件后执行全部测试，确认没有留下重复日志或路径行为变化。

## 十四、本章稳定状态

完成故障实验并恢复正确数据库变量、INFO级别后，工程新增或修改状态为：

```text
src/main/java/com/example/employee/
├── config/RequestLoggingFilter.java
├── exception/
│   ├── EmployeeNotFoundException.java
│   └── GlobalExceptionHandler.java
└── service/impl/EmployeeServiceImpl.java

src/main/resources/application.yml
logs/employee-api.log                            ← 运行时生成，不提交Git
```

此时你应能够：

1. 区分启动故障、请求错误、业务失败和系统异常；
2. 使用Logger、级别和占位符记录必要事实；
3. 用requestId连接同一次请求的多条日志；
4. 正确记录异常堆栈并沿原因链定位；
5. 配置控制台、文件、级别和基础滚动策略；
6. 避免日志泄露凭据、请求体和个人信息；
7. 避免多层重复记录同一异常；
8. 结合日志、响应、数据库状态和断点形成完整故障记录；
9. 区分Filter、Interceptor、ControllerAdvice与AOP/Proxy的位置和责任；
10. 识别 `preHandle`、`postHandle`、`afterCompletion` 以及常见AOP注解。

下一章会把第10章的手工验证整理成可重复执行的自动化测试。日志用于诊断失败原因，测试用于自动判断结果，两者不能互相替代。
