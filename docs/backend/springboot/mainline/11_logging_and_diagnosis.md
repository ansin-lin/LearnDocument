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
.gitignore                                           ← 追加运行日志目录
```

SLF4J API和默认Logback实现已经由Spring Boot Web Starter提供，本章不新增Maven依赖，也不在 `pom.xml` 中另外指定日志库版本。

## 二、项目日志设计基础：先决定记录什么、放在哪里

员工管理接口返回状态码，是给调用方的结果；日志则是供开发、运维按时间和请求编号调查的服务端证据。一次新增员工值得留下“新增成功及员工编号”，而普通DTO转换、getter或每一步循环不值得长期输出INFO。日志并非越多越好：过量日志会淹没故障线索、增加保存成本，也可能泄露个人信息。密码、Token、Cookie、Authorization、完整请求体和无必要的姓名、邮箱均不写入本章日志。

| 员工系统场景 | 推荐位置 | 记录内容 | 建议等级 |
| --- | --- | --- | --- |
| 新增、修改、删除确认成功 | Service | 稳定事件名、员工编号、必要业务字段 | INFO |
| 列表查询的筛选及结果数 | Service | 是否有筛选、数量，不记录全部结果 | 排查时DEBUG |
| 每次HTTP请求结束 | Filter | method、path、status、耗时、requestId | INFO或按项目策略 |
| 员工不存在、邮箱冲突 | 全局异常处理器 | 路径、员工编号或冲突类型 | WARN或按项目策略 |
| 数据库不可用 | 全局异常处理器 | requestId、异常堆栈，不向客户端暴露内部详情 | ERROR |
| 统一方法耗时 | AOP或监控组件 | 方法名、耗时 | 按监控要求 |
| SQL诊断 | MyBatis日志配置 | 必要SQL诊断信息，限时开启 | DEBUG |

这只是教学项目的设计示例，不是所有企业统一的日志等级规范。记录位置取决于“谁能确认这个事实”：Service知道业务操作是否成功；Filter知道请求进入、退出及耗时，却不能仅凭HTTP返回猜测事务是否提交；异常处理器统一决定异常等级，避免Controller、Service、Advice重复输出同一堆栈。

| 方式 | 覆盖范围与适用日志 | 不能替代什么 |
| --- | --- | --- |
| Service内Logger | 特定业务方法的成功事件、关键状态变化 | 全部HTTP请求统一记录 |
| Servlet Filter | Servlet请求入口、响应、耗时、requestId | 员工业务语义 |
| Spring MVC HandlerInterceptor | MVC Handler调用前后，可用于Controller公共处理；在MVC链中，范围与Filter不同 | 非MVC的全部Servlet流量 |
| Spring AOP | Spring管理Bean的被代理方法；适合统一方法耗时 | HTTP底层细节、全部业务事实 |

本章稳定方案是“Service业务事件＋Filter请求信息＋全局异常日志＋MDC关联”。Interceptor只作选型比较；AOP独立实验放在[A13附录](../appendix/A13_spring_common_processing_mechanisms.md)，不进入本章最终工程。同一个事件只设一个主要记录位置。即使某个Service方法返回成功，也不应仅靠AOP推断外层事务已经提交。

## 三、案例一：记录第一条业务日志

### 1. 目标和涉及文件

在 `EmployeeServiceImpl` 的新增成功路径记录员工编号，在Eclipse Console中观察INFO日志。局部修改Service，并保留第10章CRUD行为。

### 2. 案例代码

文件为 `src/main/java/com/example/employee/service/impl/EmployeeServiceImpl.java`。先在类的字段区增加Logger，随后用下面完整方法替换原 `create()`；其余方法保持第10章状态。例中 `DuplicateKeyException` 等导入沿用原文件，新增两个SLF4J导入。

```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// EmployeeServiceImpl类的字段区：
private static final Logger log =
        LoggerFactory.getLogger(EmployeeServiceImpl.class);

@Override
public EmployeeResponse create(EmployeeCreateRequest request) {
    String department = request.getDepartment().trim();
    validateDepartment(department);
    Employee employee = new Employee();
    employee.setName(request.getName().trim());
    employee.setDepartment(department);
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
    log.info("employee_created employeeId={} department={}",
            employee.getId(), employee.getDepartment());
    return findById(employee.getId());
}
```

原方法的插入、影响行数检查、重复邮箱异常转换和再次查询都保留；唯一新机制是Logger与成功后的一条业务事件。若在INSERT前输出 `employee_created`，插入失败也会留下假成功记录。`employeeMapper.insert` 返回1且生成id之后才输出；更严格的“事务已提交”语义需要事务提交后机制，本章不把这条方法内日志称为提交证明。

SLF4J是应用调用的日志门面/API，`Logger` 是实际调用对象；Logback是Spring Boot默认使用的输出实现，决定Console、文件和格式。`LoggerFactory.getLogger(EmployeeServiceImpl.class)` 以当前类作为Logger名称，便于按类配置等级。`private` 防止外部随意调用，`static` 让该类共享Logger，`final` 避免重新赋值。`log.info("employeeId={}", id)` 中 `{}` 按顺序绑定参数；相比字符串拼接，未启用等级时可避免不必要的字符串构造，也便于保持稳定格式。`System.out.println()` 缺少统一等级、来源、格式、滚动文件与运行配置，不适合作为项目日志方案。配置的INFO门槛会显示INFO、WARN、ERROR，过滤DEBUG、TRACE。

| 等级 | 当前项目适用例 | 不应滥用为 |
| --- | --- | --- |
| TRACE | 临时追踪极细的框架/流程细节 | 日常业务事件；数据量可能极大 |
| DEBUG | 排查列表筛选条件是否存在、结果条数 | 长期开启的个人数据输出 |
| INFO | 确认新增、修改、删除等重要业务结果 | 每个getter、DTO转换的“进入方法” |
| WARN | 员工不存在、重复邮箱等可预期但需关注的失败 | 数据库不可用等系统故障的唯一告警 |
| ERROR | 数据库访问失败等需要排查的系统异常，并在边界保留堆栈 | 所有404、409或用户输入错误 |

### 3. 基础日志配置

在第10章的 `application.yml` 末尾追加以下最小配置，不改动原有数据库与MyBatis部分。后面的requestId案例再加入日志格式，滚动文件配置及细节见[日志配置进阶附录](../appendix/A14_logging_configuration_advanced.md)。

```yaml
logging:
  level:
    root: INFO
    com.example.employee: INFO
    com.example.employee.service.impl.EmployeeServiceImpl: ${APP_SERVICE_LOG_LEVEL:INFO}
    com.example.employee.mapper: INFO
  file:
    name: logs/employee-api.log
```

`logging.level.root` 是默认输出门槛，具体类的设置更优先；`APP_SERVICE_LOG_LEVEL` 未设置时使用INFO。需要观察DEBUG时，在Eclipse当前启动配置的Environment页设置 `APP_SERVICE_LOG_LEVEL=DEBUG` 并重启，观察后恢复INFO。`logging.file.name` 使日志同时写入Console与文件；相对路径以应用启动工作目录为基准，写入目录需要权限。此时没有requestId，日志也不显示MDC字段。

### 4. 验证和完成状态

在Eclipse运行启动类；Postman发送 `POST {{baseUrl}}/employees`，Body为 `{"name":"Suzuki","department":"Sales","email":"suzuki@example.com"}`，预期201、Location指向生成id、响应 `data.id` 与Console及 `logs/employee-api.log` 中的 `employee_created employeeId=...` 一致；再用数据库客户端核对该id。临时在Eclipse启动配置中设 `APP_SERVICE_LOG_LEVEL=DEBUG` 重启，观察DEBUG后恢复INFO。恢复测试数据后保留日志代码。

## 四、案例二：在Service和异常边界记录有效信息

### 1. 目标

为查询、列表、新增、修改和删除记录必要业务事实；业务失败记WARN，系统故障由统一异常处理器记一次ERROR。不要在Controller、Service和Advice重复打印同一个异常堆栈。

### 2. 先判断哪些方法值得记录

`findById` 的正常查询通常由请求日志覆盖，不必长期再写INFO；查不到员工由Advice决定是否记录WARN，不是系统ERROR。`findList` 只在排查时用DEBUG记录筛选是否存在及数量。`create`、`update`、`delete` 是业务状态变化，确认结果后各留一条INFO。若失败，Service抛出业务或系统异常，由异常边界统一记录，不要一层一条堆栈。第一个案例已给出完整 `create()`；以下是另外两个完整方法，放在同一 `EmployeeServiceImpl` 类中替换原同名方法，不另建Service。

```java
@Override
public EmployeeResponse update(Long id, EmployeeUpdateRequest request) {
    String department = request.getDepartment().trim();
    validateDepartment(department);
    Employee employee = new Employee();
    employee.setId(id);
    employee.setName(request.getName().trim());
    employee.setDepartment(department);
    employee.setEmail(normalizeEmail(request.getEmail()));
    int affectedRows;
    try {
        affectedRows = employeeMapper.update(employee);
    } catch (DuplicateKeyException exception) {
        throw new DuplicateEmailException(employee.getEmail());
    }
    if (affectedRows > 1) {
        throw new EmployeeSystemException("修改员工影响多行");
    }
    if (affectedRows == 0) {
        Employee existing = employeeMapper.findById(id);
        if (existing == null) {
            throw new EmployeeNotFoundException(id);
        }
        log.debug("employee_update_no_change employeeId={}", id);
        return toResponse(existing);
    }
    log.info("employee_updated employeeId={} department={}",
            id, employee.getDepartment());
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
```

`update()` 的0行不能直接等同404：值未变化时也可能是0，因此再查一次；查不到才抛404，查得到只记DEBUG并返回当前值。`delete()` 只在恰好删除一行后记INFO。两段代码与章末稳定版一致。查询方法仅按下表增量修改。

### 3. 在现有Service方法中增量记录日志

第10章的业务方法、Mapper调用和异常转换保持不变。在 `EmployeeServiceImpl.java` 导入 `org.slf4j.Logger`、`org.slf4j.LoggerFactory`，并保留案例一已经加入的类级Logger。以下均为局部增量，位置按方法名标出；章末提供累积后的完整文件。

| 方法与位置 | 新增或替换的日志 | 观察目标 |
| --- | --- | --- |
| `findList()`：转换完列表、return前 | `log.debug("employee_list_completed filterApplied={} resultCount={}", filter != null, responses.size());` | 只在DEBUG下观察筛选与数量 |
| `create()`：INSERT成功且生成id后 | `log.info("employee_created employeeId={} department={}", employee.getId(), employee.getDepartment());` | 将案例一的id日志替换为稳定的业务事件，不能在INSERT前记录 |
| `update()`：影响行数0且再次查询发现记录存在时 | `log.debug("employee_update_no_change employeeId={}", id);` | 说明未改变值的更新，不将0行直接判为404 |
| `update()`：影响行数1后、再次读取前 | `log.info("employee_updated employeeId={} department={}", id, employee.getDepartment());` | 记录成功修改的编号与部门 |
| `delete()`：确认只删除1行后 | `log.info("employee_deleted employeeId={}", id);` | 只在数据库成功删除后记录 |

这些日志代码放入第10章已有的同名方法，不改变返回值、异常分支或Mapper SQL。业务异常的WARN和系统故障的ERROR由后面的Advice案例统一记录。

#### 4.1 Service为什么只记录业务结果

第10章已经完成查询、新增、修改、删除、字段标准化和异常转换。本节保留这些业务行为，只在能够确认结果的位置增加日志：

```text
读取或校验输入
  → 调用Mapper
  → 检查影响行数或返回结果
  → 确认操作结果
  → 记录一条有业务意义的日志
```

不能在调用Mapper之前就记录“新增成功”或“删除成功”，因为数据库操作仍可能失败。日志必须描述已经发生的事实，而不是准备执行的动作。

`EmployeeServiceImpl.class` 取得以当前Service类命名的Logger。它与第2节的Filter Logger写法相同，但配置可以按完整类名把这个Service单独调成DEBUG。

#### 4.2 本节每条业务日志表示什么

| 代码位置 | 等级 | 事件名和字段 | 为什么这样记录 |
| --- | --- | --- | --- |
| `findList()` 返回前 | DEBUG | `employee_list_completed`、是否筛选、结果数量 | 排查查询条件和结果规模；正常运行不必长期输出 |
| `create()` 确认插入成功后 | INFO | `employee_created`、员工编号、部门 | 留下新增成功的业务事实 |
| `update()` 影响行数为0但员工存在 | DEBUG | `employee_update_no_change`、员工编号 | 说明请求内容与既有数据相同，不误报为404 |
| `update()` 确认修改成功后 | INFO | `employee_updated`、员工编号、部门 | 留下修改成功的业务事实 |
| `delete()` 确认只删除一行后 | INFO | `employee_deleted`、员工编号 | 留下删除成功的业务事实 |

事件名采用稳定的小写单词和下划线，例如 `employee_created`；字段采用 `key={}`，例如 `employeeId={}`。这种写法比“新增完成了”更容易检索，也能让Review人员快速核对日志中保存了哪些数据。

`log.debug()` 和 `log.info()` 的参数规则相同：消息中的 `{}` 按顺序接收后续参数。区别在于DEBUG只有当前Logger的有效等级达到DEBUG或更低时才输出；INFO在本章默认配置下会输出。

#### 4.3 为什么catch中转换异常但不立即写日志

新增和修改捕获 `DuplicateKeyException` 后，将数据库唯一约束异常转换成 `DuplicateEmailException`：

```java
} catch (DuplicateKeyException exception) {
    throw new DuplicateEmailException(employee.getEmail());
}
```

这里不记录一次ERROR，因为重复邮箱是可以预期的业务冲突，稍后的全局异常处理器会统一记录WARN并返回409。如果Service和全局异常处理器各记录一次，同一事件就会产生重复日志。

`EmployeeNotFoundException` 和 `EmployeeSystemException` 也采用相同原则：Service负责判断业务结果并抛出含义明确的异常，全局异常处理器负责选择日志等级和HTTP响应。

#### 4.4 哪些数据不能写入业务日志

本章只记录完成排查所需的操作名、员工编号、已校验部门、筛选是否存在和结果数量。不要记录：

- 数据库密码、Token、Cookie和Authorization请求头；
- 完整请求体；
- 员工姓名、邮箱等没有排查必要的个人信息；
- 拼接后的完整SQL和敏感参数；
- “进入方法”“执行下一行”等没有诊断价值的逐行轨迹。

日志需要同时满足“能够定位”和“不过度收集”。即使员工编号本身不是密码，也应遵守项目的日志访问权限和保存期限。


各层职责是：Filter记录一次HTTP完成信息，Service记录业务动作和关键编号，Mapper通常通过受控等级输出SQL，Advice记录最终异常分类。Controller没有新的业务判断时不重复记录Service已经记录的事实。

### 4. 验证和完成状态

分别执行查询成功、员工不存在、重复邮箱和新增成功，确认日志包含足以定位的id、结果或影响行数，但不包含密码和完整请求体。业务异常应为WARN或由请求完成日志体现，不能全部作为ERROR。

## 五、案例三：Filter记录HTTP请求

### 1. 目标与文件

新建 `src/main/java/com/example/employee/config/RequestLoggingFilter.java`。先只记录HTTP方法、请求路径、当前响应状态和耗时；此时不生成requestId，也不改变响应Header。

如果把 `log.info("request_complete ...")` 分别写入GET、POST、PUT、DELETE的Controller方法，每增加一个接口都要复制代码，且异常提前返回时容易漏记。Filter在Servlet请求链的入口/出口统一记录。简化流程（实际还可能有其他Filter）为 `Postman → Filter → Spring MVC → Controller → Service → Mapper → 数据库 → 响应返回Filter → 记录完成日志`。Filter观察的是请求层事实，不推断业务提交。

### 2. 新建最小Filter完整文件

```java
package com.example.employee.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger log =
            LoggerFactory.getLogger(RequestLoggingFilter.class);

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {
        long startedAt = System.nanoTime();
        try {
            filterChain.doFilter(request, response);
        } finally {
            long elapsedMs = (System.nanoTime() - startedAt) / 1_000_000;
            log.info("request_complete method={} path={} status={} elapsedMs={}",
                    request.getMethod(), request.getRequestURI(),
                    response.getStatus(), elapsedMs);
        }
    }
}
```

`@Component` 使Spring注册Filter，`@Order` 指定顺序；`OncePerRequestFilter` 提供一次请求的过滤入口。请求进入时记录单调时间，`filterChain.doFilter()` 把请求交给后续Filter和Controller；缺少它会阻断正常处理。`finally` 在后续调用返回或抛出异常时仍执行，用结束时间减开始时间得到毫秒耗时。`getMethod()` 和 `getRequestURI()` 分别取得方法与路径，路径不含查询参数；这里只记录必要的请求信息，不记录Body或Cookie。

`response.getStatus()` 通常能取得Spring MVC已处理完成的200、400、404等状态；如果异常继续向Servlet容器传播，此时读到的状态不一定是客户端最终收到的状态。Filter本身也不会自动处理所有异常。

### 3. 验证与完成状态

在Eclipse启动服务，用Postman分别请求 `GET {{baseUrl}}/employees/1001`（无Body，预期200）、`GET {{baseUrl}}/employees/999999`（无Body，预期404），以及 `POST {{baseUrl}}/employees`（raw JSON：`{"name":"","department":"Sales","email":"bad"}`，预期400）。逐条比对原HTTP响应和 `request_complete` 的method、path、status、elapsedMs；业务响应保持第10章规格。此时日志与响应Header均没有requestId，下一案例在这个Filter上增量添加。

## 六、案例四：为每个请求加入requestId与MDC

在案例三的Filter上增加UUID、响应Header和MDC；保留已有的method、path、status与耗时日志。先在 `RequestLoggingFilter.java` 导入 `java.util.UUID` 和 `org.slf4j.MDC`，然后完整替换该类的 `doFilterInternal()` 方法；类、注解、Logger和其他import保持不变。

假设A查询、B新增、C删除的日志在Console交错，仅靠时间难以判断归属。requestId是每次请求的关联编号；UUID使本地生成的编号足够不易碰撞，响应Header让调用方也能提供同一线索。MDC是当前线程的诊断上下文：Filter设置后，同步调用链上的Service和Advice日志都可通过pattern读取。异步线程不自动继承此上下文，需要另行传播；容器线程会复用，所以必须在请求结束时清理。

```java
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
            long elapsedMs = (System.nanoTime() - startedAt) / 1_000_000;
            try {
                log.info("request_complete method={} path={} status={} elapsedMs={}",
                        request.getMethod(), request.getRequestURI(),
                        response.getStatus(), elapsedMs);
            } finally {
                MDC.remove("requestId");
            }
        }
    }
```

修改后的 `RequestLoggingFilter.java` 完整文件如下；它替换案例三的旧版，不能把两版类同时保留：

```java
package com.example.employee.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
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
            try {
                log.info(
                        "request_complete method={} path={} status={} elapsedMs={}",
                        request.getMethod(),
                        request.getRequestURI(),
                        response.getStatus(),
                        elapsedMs);
            } finally {
                MDC.remove("requestId");
            }
        }
    }
}
```

同时在 `application.yml` 的 `logging` 下新增pattern与基本滚动配置；保留案例一的level和file设置。下方是新增片段，章末有完整配置。

```yaml
  logback:
    rollingpolicy:
      file-name-pattern: "${LOG_FILE}.%d{yyyy-MM-dd}.%i.gz"
      max-file-size: 10MB
      max-history: 7
      total-size-cap: 100MB
      clean-history-on-start: false
  pattern:
    console: "%d{yyyy-MM-dd'T'HH:mm:ss.SSSXXX} %-5level [%thread] [%X{requestId:-no-request}] %logger{36} - %msg%n"
    file: "%d{yyyy-MM-dd'T'HH:mm:ss.SSSXXX} %-5level [%thread] [%X{requestId:-no-request}] %logger{36} - %msg%n"
```

`UUID.randomUUID()` 为这次请求生成标识；`X-Request-Id` 将同一值返回客户端。`MDC.put` 把 `requestId` 放入当前线程的日志上下文，pattern中的 `%X{requestId:-no-request}` 读取它；没有上下文的启动日志显示 `no-request`。Service和Advice在同一同步请求线程上记录日志时，不必手动传入编号。线程会被复用，所以在 `finally` 中清理MDC；内层 `finally` 还保证即使写完成日志本身失败也执行清理。异步线程的传播不属于本案例。

滚动配置限制单文件大小、按天归档并限制保留时间和归档容量；当前项目最终配置仍使用它们，参数与文件名语法的详细说明见[日志配置进阶附录](../appendix/A14_logging_configuration_advanced.md)。

验证A：发送一次 `GET {{baseUrl}}/employees/1001`，在Postman响应Header记录 `X-Request-Id`，对照Filter和Service日志中的编号一致。验证B：连续发送两次请求，两个Header值应不同，各自日志不得混用。再发送404请求，确认Advice WARN与Filter完成日志共用同一编号。案例完成后保留Filter和pattern，供故障调查使用。

## 七、案例五：用requestId完成一次故障调查

### 1. 异常类型和统一记录

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

#### 3.1 为什么异常对象还要保存employeeId

`EmployeeNotFoundException extends RuntimeException` 表示这是一个运行时业务异常。Service发现员工不存在时主动抛出它，Controller不需要在每个方法上声明 `throws`，之后由全局异常处理器统一转换为404响应。

构造方法同时保存两类信息：

```java
super("员工不存在：" + employeeId); // 交给RuntimeException保存公开错误消息
this.employeeId = employeeId;       // 保存结构明确的业务编号
```

| 成员 | 类型或参数 | 作用 |
| --- | --- | --- |
| `super(...)` | 非空错误消息字符串 | 调用父类构造方法，使 `getMessage()` 可以取得错误消息 |
| `private final Long employeeId` | 当前请求中的员工编号 | 创建异常后不再改变，并且只允许通过方法读取 |
| `getEmployeeId()` | 无参数，返回 `Long` | 让异常处理器直接取得编号并写入结构化日志 |

如果异常只保存字符串，异常处理器就只能从“员工不存在：1001”中截取编号。这种做法容易受文字变化影响，也不利于Review。把业务字段独立保存后，响应消息可以调整，日志字段仍保持稳定。

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

#### 6.1 全局异常处理器同时面对两类读者

异常发生后，服务端排查人员和接口调用方需要的信息不同：

```text
异常对象
├── 服务端日志：记录事件名、路径、必要业务编号；系统异常保留受控堆栈
└── HTTP响应：返回稳定状态码和允许公开的消息，不暴露SQL和内部调用栈
```

因此，日志不能直接等同于响应正文。数据库异常可以在服务端保存堆栈，但客户端只得到500和“服务器内部错误”。

#### 6.2 每个处理方法怎样选择日志和响应

`@RestControllerAdvice` 让Spring管理这个全局异常处理器，并把返回对象写入响应体。每个 `@ExceptionHandler(异常类型.class)` 只处理声明的异常类型；处理方法通过 `ResponseEntity` 明确返回状态码和统一响应对象。

| 异常类型 | 日志等级和事件 | HTTP状态 | 对外响应重点 |
| --- | --- | --- | --- |
| `MethodArgumentNotValidException` | WARN、`request_rejected reason=validation` | 400 | 返回字段校验错误 |
| `HttpMessageNotReadableException` | WARN、`request_rejected reason=unreadable_json` | 400 | 只说明JSON格式错误 |
| `InvalidDepartmentException` | WARN、`request_rejected reason=invalid_department` | 400 | 返回允许公开的业务消息 |
| `EmployeeNotFoundException` | WARN、`employee_not_found` | 404 | 返回不存在消息，并在日志中记录员工编号 |
| `DuplicateEmailException` | WARN、`employee_conflict reason=duplicate_email` | 409 | 返回重复邮箱业务消息，不记录邮箱值 |
| `EmployeeSystemException` | ERROR、`employee_system_error` | 500 | 响应隐藏内部原因，日志保留异常堆栈 |
| `DataAccessException` | ERROR、`database_access_error` | 500 | 响应隐藏数据库细节，日志保留异常堆栈 |

400、404和409是应用已经预期并能转换的失败，因此使用WARN而不是ERROR；数据库故障和内部状态异常会阻止当前操作完成，需要调查根因，因此使用ERROR。

#### 6.3 HttpServletRequest和DataAccessException的作用

`HttpServletRequest` 来自 `jakarta.servlet.http`，由Spring为当前失败请求传入处理方法。`request.getRequestURI()` 返回请求路径，使日志能说明哪个接口发生问题；本章仍不读取请求体、查询字符串或敏感请求头。

`DataAccessException` 是Spring统一的数据访问异常父类。数据库连接失败、SQL执行错误等底层异常可以转换为它的子类，因此处理器不必依赖某一种数据库驱动异常。第10章已经转成 `DuplicateEmailException` 的唯一约束冲突会优先进入更具体的业务处理方法，不会在这里统一变成500。

#### 6.4 怎样记录异常堆栈

记录系统异常时，把异常对象作为日志方法的最后一个参数：

```java
log.error(
        "database_access_error path={}",
        request.getRequestURI(),
        exception);
```

第一个 `{}` 只接收请求路径，最后的 `exception` 由SLF4J识别为需要输出的异常。这样会保留异常类型、调用位置和原因链。只记录 `exception.getMessage()` 会丢失这些信息。

堆栈中的 `Caused by` 表示下一层原因。排查时通常从最底部具体的数据库、SQL或Java异常开始，再向上核对它经过Mapper、Service和异常处理器的调用路径。

业务性的400、404和409通常不输出整段堆栈，否则大量可预期请求会制造噪声。系统异常才使用ERROR并保留堆栈。

#### 6.5 为什么不添加Exception.class兜底处理

本章仍不添加 `@ExceptionHandler(Exception.class)`。笼统捕获可能把HTTP方法不支持、媒体类型不支持等框架异常也改成统一500，破坏原有405和415语义。只有项目明确规定统一兜底响应，并为各类框架错误建立回归测试后，才应增加这样的处理。

同一个异常也不要在Mapper、Service和全局处理器连续记录。Service负责转换已知业务含义，最终处理器负责记录一次；否则一次故障会产生多条相似ERROR，让排查人员误以为故障发生了多次。


### 2. Postman和日志调查

#### 2.1 发送请求并读取日志

确认Eclipse Run Configuration中已有数据库环境变量，然后从启动类运行应用。使用Postman依次发送：

| HTTP方法 | URL | Params | 请求体 | 预期状态与响应 |
| --- | --- | --- | --- | --- |
| GET | `{{baseUrl}}/employees/1001` | 无 | 无 | 200，员工详情 |
| GET | `{{baseUrl}}/employees/999999` | 无 | 无 | 404，员工不存在响应 |
| POST | `{{baseUrl}}/employees` | 无 | `{"name":"Log Sample","department":"Support","email":"log.sample@example.com"}` | 201，返回新增员工 |

POST请求的Body选择 **raw → JSON**，确认 `Content-Type: application/json`。记录响应Headers中的 `X-Request-Id`。完成日志观察后，在数据库客户端先按邮箱确认目标，再只删除本次新增记录：

```sql
SELECT id, email
FROM employees
WHERE email = 'log.sample@example.com';

DELETE FROM employees
WHERE email = 'log.sample@example.com';
```

确认连接的是个人练习库，并且SELECT只返回本次创建的目标后才能执行DELETE。

响应头中应出现 `X-Request-Id`。下面是格式示例，时间、线程、UUID、耗时和员工编号以实际结果为准：

```text
2026-09-14T10:20:31.123+09:00 INFO  [http-nio-8080-exec-1] [7f...c2] c.e.e.c.RequestLoggingFilter - request_complete method=GET path=/employees/1001 status=200 elapsedMs=28
2026-09-14T10:20:35.456+09:00 WARN  [http-nio-8080-exec-2] [a1...9d] c.e.e.e.GlobalExceptionHandler - employee_not_found path=/employees/999999 employeeId=999999
2026-09-14T10:20:35.457+09:00 INFO  [http-nio-8080-exec-2] [a1...9d] c.e.e.c.RequestLoggingFilter - request_complete method=GET path=/employees/999999 status=404 elapsedMs=7
```

Eclipse Console可以直接观察当前运行日志。需要查看文件日志时，在Project Explorer中刷新项目，打开 `logs/employee-api.log`；如果日志目录没有显示，确认Eclipse工作目录和 `logging.file.name` 的相对路径。使用编辑器的 **Find** 查找 `employeeId=1001`、`status=500` 或记录下来的requestId。

查找时先缩小时间范围，再用requestId串起同一次请求，最后沿类名和异常原因进入代码。不要只看到最后一条500就猜测原因。日志文件较大时可以使用IDE支持的日志查看插件，但课程主线只要求Console、文件和文本查找。

#### 2.2 四类故障的排查起点

| 现象 | 第一检查点 | 后续证据 |
| --- | --- | --- |
| 应用无法启动 | 启动日志最底部原因 | 配置名、端口、Bean、数据库连接 |
| 请求格式错误 | HTTP状态和全局处理日志 | 路径、方法、Content-Type、字段错误 |
| 业务失败 | warn日志和业务编号 | employeeId、规则、数据库当前状态 |
| 系统异常 | error日志和完整原因链 | requestId、堆栈、SQL/连接根因、复现条件 |

日志指出运行到了哪里，断点可以检查当时对象里有什么值。需要确认跨层传值时，可依次在Controller参数、Service方法、Mapper调用前设置断点；不要为了调试把完整DTO长期打印到日志。

日志功能本身出现问题时，按下面顺序检查：

| 现象 | 先检查 | 常见原因 | 修正方向 |
| --- | --- | --- | --- |
| 找不到日志文件 | 启动工作目录、`logging.file.name` | 相对路径基准与预想不同 | 确认启动目录；部署时使用受控绝对路径 |
| Service debug不出现 | `APP_SERVICE_LOG_LEVEL`和重启后的配置 | 变量未加入当前Eclipse运行配置，或修改后没有重启 | 修改当前Run Configuration并重新启动 |
| SQL也大量输出 | Mapper Logger最终级别 | 打开了整个项目包或Mapper DEBUG | 保持Mapper为INFO，只打开目标Service |
| 同一异常出现多次 | Service与异常处理器 | 多层重复记录同一异常 | 确定一个最终记录位置 |
| 启动日志显示no-request | 日志发生时机 | 启动阶段没有HTTP请求上下文 | 属于正常现象，不伪造requestId |
| 日志无法写入 | 父目录、文件权限、磁盘空间 | 运行账号无写权限或磁盘已满 | 修正限定目录权限或按运维手顺处理容量 |
| 归档数量与预期不同 | 日期周期、文件大小、`max-history`和`total-size-cap` | 只看某一项限制，忽略每天可能生成多个序号归档 | 先按日期周期核对，再检查10MB拆分和100MB总容量清理 |
| 时间无法与其他系统对照 | 时间中的UTC偏移 | 不同环境使用不同默认时区 | 保留偏移，调查时统一换算时间范围 |

#### 2.3 完成调查闭环

##### （1）区分启动故障和请求期间故障

先做启动故障实验：停止应用，在当前Eclipse Run Configuration中把 `DB_PASSWORD` 临时改成错误值并重启。数据库连接池可能在启动阶段确认连接，也可能在第一次访问数据库时才取连接；因此先观察应用是否成功启动，不预先假定一定能取得HTTP响应。

- 如果应用启动失败：保存启动日志中的最底层数据库认证原因，不发送接口请求。
- 如果应用能够启动：只在Postman调用一次 `GET {{baseUrl}}/employees/1001`，无参数、无请求体；记录实际状态、`X-Request-Id`和同一requestId下的数据库异常。

随后恢复正确环境变量并重启，确认应用正常启动且详情接口返回200。不要反复猜密码，也不要把错误或正确密码写进证据。

再做一次能够稳定产生requestId的运行时SQL故障实验：在个人练习分支中，把 `EmployeeMapper.xml` 的 `findById` 查询表名临时改为不存在的 `employees_log_lab_missing`，从Eclipse重启后在Postman调用 `GET {{baseUrl}}/employees/1001`，无参数、无请求体。预期得到500、`X-Request-Id`以及同一requestId下的 `database_access_error`和数据库“表不存在”原因。完成后立即恢复表名、重启，并确认同一请求重新返回200。不得在共享环境修改Mapper，也不要把错误表名保留到后续章节。

##### （2）问题记录必须形成闭环

使用下面结构整理：

| 项目 | 应记录的内容 |
| --- | --- |
| 现象 | 哪个接口、什么状态、何时发生 |
| 复现 | 前置条件和最短操作步骤 |
| 证据 | requestId、必要日志、响应和数据库状态；隐藏敏感信息 |
| 原因 | 最底层异常与错误配置或代码位置 |
| 修复 | 修改了什么，为什么能够消除原因 |
| 确认 | 原场景通过，相关CRUD回归，临时状态已恢复 |

##### （3）Review与自测任务

1. Review `log.info("request={}", request)`，指出可能泄露的字段并改成必要的业务事实。
2. Review“Service catch异常后打印error再原样抛出、全局处理器再次打印error”，说明重复日志的影响并选择唯一记录位置。
3. 临时把 `APP_SERVICE_LOG_LEVEL` 设为DEBUG，验证列表debug出现且Mapper SQL没有同时输出；恢复INFO并证明debug不再输出。
4. 对404、409和数据库500分别保存状态、requestId、关键日志和判定。
5. 使用断点核对一次PUT中路径id、Update DTO、Employee Entity和Mapper参数，确认日志不代替对象检查。


故障调查顺序是：取得响应头requestId，按编号检索日志，确认最后一个正常组件和第一个异常组件，再结合异常类型、SQL或堆栈判断根因。HTTP 500只表示服务端未完成请求，不等于根因一定在Controller，也不能说明一定是数据库故障。

## 八、日本项目日志设计、Review与故障调查手顺

以下是本教学项目可采用的日志设计表，不代表所有日本企业使用同一模板。Review时逐项确认“事件是否必要、在哪层唯一输出、等级是否恰当、是否含敏感字段、是否能用requestId检索”。

| 事件 | 输出层 | 等级 | 保留字段 | 不记录字段 |
| --- | --- | --- | --- | --- |
| `employee_created` | Service | INFO | employeeId、部门 | 姓名、邮箱、请求体 |
| `request_complete` | Filter | INFO | requestId、method、path、status、elapsedMs | Cookie、Authorization、Body |
| 员工不存在 | Advice | WARN | requestId、path、employeeId | 完整请求体 |
| 数据库访问失败 | Advice | ERROR | requestId、异常堆栈（受控服务端） | 密码、对外内部堆栈 |

配置Review还要检查INFO/DEBUG门槛、滚动文件大小与保留期限、日志目录权限，以及Service、Filter、Advice是否重复记录同一异常。异常响应只给客户端既定状态与安全消息；堆栈留在受权限保护的服务端日志。404、409通常属于预期业务失败，不必一律ERROR；数据库故障通常需要ERROR。请求日志与业务日志可各有一条，它们表达的事实不同。

故障调查手顺：在Postman复现404、409、数据库500或错误密码场景，记录HTTP方法、URL、参数/Body、预期和实际状态及响应；取得响应Header的 `X-Request-Id`；在Console/日志文件按该编号筛选；比较Filter、Service、Advice记录，判断故障发生层；定位代码或配置并修复；重新执行原请求和相关正常系；保存Postman响应、日志片段、数据库确认与Review记录。错误密码实验不得把密码值写进证据。启动日志没有requestId正常；两个请求编号必须不同。

## 九、日志文件与版本库边界

本章运行后会在项目根目录生成 `logs`。它属于当前机器的运行产物，不进入源码版本管理。在项目根目录的 `.gitignore` 末尾追加：

```gitignore
# Local runtime logs
logs/
```

这里只追加两行，不要覆盖Spring Initializr已经生成的其他忽略规则。日志证据需要提交时，应摘取并脱敏后放入项目规定的证据目录，不能直接提交整个运行日志。


日志文件属于运行产物，可能包含业务编号和故障信息，不应提交到Git。提交前确认只有 `.gitignore` 规则变化，而不是把现有日志文件加入版本管理。

## 十、章末最终代码

下面汇总本章最终文件中的完整内容。案例完成后用它核对，不要跳过前面的运行与调查步骤。

### 1. application.yml

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
  configuration:
    map-underscore-to-camel-case: true
    default-statement-timeout: 10

logging:
  level:
    root: INFO
    com.example.employee: INFO
    com.example.employee.service.impl.EmployeeServiceImpl: ${APP_SERVICE_LOG_LEVEL:INFO}
    com.example.employee.mapper: INFO
  file:
    name: logs/employee-api.log
  logback:
    rollingpolicy:
      file-name-pattern: "${LOG_FILE}.%d{yyyy-MM-dd}.%i.gz"
      max-file-size: 10MB
      max-history: 7
      total-size-cap: 100MB
      clean-history-on-start: false
  pattern:
    console: "%d{yyyy-MM-dd'T'HH:mm:ss.SSSXXX} %-5level [%thread] [%X{requestId:-no-request}] %logger{36} - %msg%n"
    file: "%d{yyyy-MM-dd'T'HH:mm:ss.SSSXXX} %-5level [%thread] [%X{requestId:-no-request}] %logger{36} - %msg%n"
```

### 2. RequestLoggingFilter.java

文件位置：`src/main/java/com/example/employee/config/RequestLoggingFilter.java`。完整内容：

```java
package com.example.employee.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
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
            try {
                log.info(
                        "request_complete method={} path={} status={} elapsedMs={}",
                        request.getMethod(),
                        request.getRequestURI(),
                        response.getStatus(),
                        elapsedMs);
            } finally {
                MDC.remove("requestId");
            }
        }
    }
}
```

### 3. EmployeeNotFoundException.java

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

### 4. EmployeeServiceImpl.java

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
        String department = request.getDepartment().trim();
        validateDepartment(department);

        Employee employee = new Employee();
        employee.setName(request.getName().trim());
        employee.setDepartment(department);
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
        String department = request.getDepartment().trim();
        validateDepartment(department);

        Employee employee = new Employee();
        employee.setId(id);
        employee.setName(request.getName().trim());
        employee.setDepartment(department);
        employee.setEmail(normalizeEmail(request.getEmail()));

        int affectedRows;
        try {
            affectedRows = employeeMapper.update(employee);
        } catch (DuplicateKeyException exception) {
            throw new DuplicateEmailException(employee.getEmail());
        }

        if (affectedRows > 1) {
            throw new EmployeeSystemException("修改员工影响多行");
        }
        if (affectedRows == 0) {
            Employee existing = employeeMapper.findById(id);
            if (existing == null) {
                throw new EmployeeNotFoundException(id);
            }
            log.debug("employee_update_no_change employeeId={}", id);
            return toResponse(existing);
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

### 5. .gitignore追加内容

```gitignore
# Local runtime logs
logs/
```

### 6. GlobalExceptionHandler.java

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

## 十一、本章稳定状态

完成故障实验并恢复正确数据库变量、INFO级别后，工程新增或修改状态为：

```text
src/main/java/com/example/employee/
├── config/RequestLoggingFilter.java
├── exception/
│   ├── EmployeeNotFoundException.java
│   └── GlobalExceptionHandler.java
└── service/impl/EmployeeServiceImpl.java

src/main/resources/application.yml
.gitignore
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
9. 区分当前使用的Filter和Controller Advice的位置与责任；
10. 在需要阅读既存项目时，知道从附录继续比较Interceptor和AOP。

下一章会把第10章的手工验证整理成可重复执行的自动化测试。日志用于诊断失败原因，测试用于自动判断结果，两者不能互相替代。

## 十二、附录：日志扩展方向

以下内容不属于当前本地故障调查主线：

- **高级滚动策略**：多环境路径、按级别拆分、特殊归档和更复杂的保留策略应结合部署与磁盘规划。
- **异步日志**：可以降低调用线程写日志的等待，但要评估队列溢出、停机刷新和故障时丢失风险。
- **集中式日志平台**：Elasticsearch、OpenSearch或云日志服务用于跨实例检索，不替代应用内正确记录字段。
- **分布式链路追踪**：traceId和spanId用于跨服务调用，范围大于本章单应用requestId。
- **异步线程中的MDC传播**：线程切换后需要显式上下文传播和清理，不能假设MDC自动继承。
- **复杂日志配置**：需要按环境、部署平台和运维规范决定，不在新人主线同时引入多套配置模型。
