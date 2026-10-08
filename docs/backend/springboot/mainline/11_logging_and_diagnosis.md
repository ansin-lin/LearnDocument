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

## 二、完整示例

先阅读 `application.yml` 及其紧随其后的日志配置说明，再完成本节其他文件。这样在阅读Java日志代码前，已经知道日志会不会输出、输出到哪里，以及旧文件怎样滚动归档。

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

#### 1.1 先看懂logging配置的结构

`logging` 是Spring Boot的日志配置入口。本章使用Spring Boot Web Starter默认提供的SLF4J和Logback，因此可以直接在 `application.yml` 中设置日志等级、日志文件、滚动策略和输出格式，不需要另外创建 `logback-spring.xml`。

```text
logging
├── level           决定哪些日志允许输出
├── file            决定当前日志文件写到哪里
├── logback
│   └── rollingpolicy
│       └── ...     决定当前文件何时归档、归档怎样命名和保留多少
└── pattern         决定控制台和文件中的每一行长什么样
```

YAML通过缩进表示从属关系。例如 `max-file-size` 必须位于 `logging.logback.rollingpolicy` 下面；缩进层级错误时，它就不再是这一组滚动配置。修改配置后需要重新启动应用，不能只刷新浏览器。

#### 1.2 level：决定哪些日志可以输出

日志等级从详细到严重依次为：

```text
TRACE < DEBUG < INFO < WARN < ERROR
```

配置的等级是“最低输出等级”。例如设为 `INFO` 时，`INFO`、`WARN` 和 `ERROR` 会输出，`DEBUG` 和 `TRACE` 不会输出。Spring Boot配置还接受 `FATAL` 和 `OFF`：默认Logback会把FATAL映射为ERROR，SLF4J也没有 `fatal()` 方法，因此本项目统一使用ERROR；OFF表示关闭指定Logger的全部日志，它也不是业务代码中调用的日志方法。

| 等级 | 含义 | 本项目中的典型内容 | 生产环境通常是否长期输出 |
| --- | --- | --- | --- |
| `TRACE` | 最细的执行轨迹，比DEBUG更详细 | 框架内部或极细粒度跟踪 | 否 |
| `DEBUG` | 开发和排查时使用的调试信息 | 查询是否带筛选条件、返回数量 | 通常否，需要时临时开启 |
| `INFO` | 系统正常运行中的重要事实 | 应用启动、员工新增成功、一次请求完成 | 是 |
| `WARN` | 程序仍能处理，但需要关注的情况 | 员工不存在、重复邮箱等可预期失败 | 是 |
| `ERROR` | 当前操作失败，需要调查原因 | 数据库访问失败、未预期系统异常 | 是 |
| `FATAL` | Spring Boot可接受的配置等级；默认Logback按ERROR处理 | 本项目不单独使用 | 不适用 |
| `OFF` | 关闭指定范围的所有日志 | 临时屏蔽极端噪声Logger | 仅特殊场景 |

等级描述的是事件严重程度，不表示代码位于Controller、Service还是Mapper。查询结果为空不一定是错误；数据库无法连接也不能只记成DEBUG。

本章的四条等级配置按“全局 → 项目包 → 具体类或子包”逐步缩小范围：

| 配置 | 可接受的值 | 本章值 | 作用 |
| --- | --- | --- | --- |
| `logging.level.root` | `TRACE`、`DEBUG`、`INFO`、`WARN`、`ERROR`、`FATAL`、`OFF` | `INFO` | 所有Logger的默认最低等级；没有更具体配置时使用它 |
| `logging.level.com.example.employee` | 同一组等级值，也可以使用环境变量占位符 | `INFO` | 整个项目包使用INFO，明确项目代码的基础等级 |
| `logging.level.com.example.employee.service.impl.EmployeeServiceImpl` | 同一组等级值，也可以使用环境变量占位符 | `${APP_SERVICE_LOG_LEVEL:INFO}` | 只允许通过环境变量临时调整这个Service类；变量未设置时使用INFO |
| `logging.level.com.example.employee.mapper` | 同一组等级值，也可以使用环境变量占位符 | `INFO` | 明确保持Mapper为INFO，防止排查Service时误输出大量SQL细节 |

Logger名称通常是类的完整包名。多个配置同时匹配时，范围更具体的配置生效。因此把 `EmployeeServiceImpl` 调成DEBUG不会把整个项目或Mapper一起调成DEBUG。

`${APP_SERVICE_LOG_LEVEL:INFO}` 是Spring Boot占位符：先读取环境变量 `APP_SERVICE_LOG_LEVEL`，没有设置时使用冒号后的默认值 `INFO`。本地需要查看Service调试日志时，在启动应用的同一个PowerShell窗口执行：

```powershell
$env:APP_SERVICE_LOG_LEVEL = "DEBUG"
.\mvnw.cmd spring-boot:run
```

验证结束后停止应用，删除当前PowerShell进程中的变量，再重新启动：

```powershell
Remove-Item Env:APP_SERVICE_LOG_LEVEL
.\mvnw.cmd spring-boot:run
```

MyBatis通常使用Mapper接口名或XML的namespace作为Logger名称。Mapper达到DEBUG时可能输出SQL，进一步提高到TRACE时还可能出现更细的结果信息。因此不能为了查看一条Service调试日志而把 `root` 或整个项目包长期改成DEBUG。

#### 1.3 file：同时写入控制台和日志文件

```yaml
logging:
  file:
    name: logs/employee-api.log
```

`logging.file.name` 指定当前正在写入的日志文件。设置它以后，日志仍会显示在控制台，同时还会写入文件。

| 配置 | 可接受的值 | 本章值 | 作用 |
| --- | --- | --- | --- |
| `logging.file.name` | 应用进程有权写入的相对或绝对文件路径 | `logs/employee-api.log` | 指定当前活动日志文件，并启用文件输出 |

本章使用相对路径 `logs/employee-api.log`：

- `logs` 是目录；
- `employee-api.log` 是当前活动日志文件；
- 相对路径以启动应用时的工作目录为基准；
- 目录不存在时，默认日志系统会尝试创建；
- 部署环境应由运行目录、权限和磁盘规划决定最终路径，不能假设一定与本地相同。

只配置 `logging.file.path` 也能指定目录，但同时配置 `name` 和 `path` 时容易让新人误判实际文件位置。本章只使用更明确的 `logging.file.name`。

#### 1.4 rollingpolicy：限制活动文件和历史文件

如果一直向同一个文件追加日志，文件会持续增大。滚动（rolling）是指达到条件后，把当前活动文件归档，再建立新的活动文件继续写入。

本章的过程是：

```text
持续写入 logs/employee-api.log
  → 日期进入下一天，或同一天内文件达到10MB
  → 按日期和序号生成压缩归档文件
  → 新的 employee-api.log 继续接收日志
  → 超过历史数量或总容量时清理较旧归档
```

| 配置 | 可接受的值 | 本章值 | 作用 |
| --- | --- | --- | --- |
| `file-name-pattern` | 合法的Logback滚动文件名格式 | `${LOG_FILE}.%d{yyyy-MM-dd}.%i.gz` | 指定归档文件的名称；日期区分日期，序号区分同一天的多个文件，`.gz` 表示压缩 |
| `max-file-size` | 例如 `10MB`、`100MB` | `10MB` | 同一个日期周期内，当前活动文件达到该大小时触发滚动 |
| `max-history` | 非负整数 | `7` | 保留最近7个日期周期；本章按天滚动，因此约为7天，每天可以有多个序号归档 |
| `total-size-cap` | 例如 `100MB`、`1GB`；`0B` 表示不限制 | `100MB` | 所有归档文件合计超过该容量时清理较旧文件 |
| `clean-history-on-start` | `true` 或 `false` | `false` | 是否在应用启动时立即执行历史归档清理；false不代表永远不清理 |

`${LOG_FILE}` 代表前面 `logging.file.name` 确定的当前日志文件，所以归档文件仍生成在 `logs` 目录。`%d{yyyy-MM-dd}` 写入归档日期，`%i` 是同一日期内从0开始递增的序号。实际文件名可能类似：

```text
employee-api.log.2026-10-08.0.gz
employee-api.log.2026-10-08.1.gz
```

本章的 `%d{yyyy-MM-dd}` 表示按天划分周期，`%i` 和 `max-file-size` 又把同一天的大文件继续拆分。因此滚动既可能由日期变化后的第一条新日志触发，也可能由文件达到10MB触发。

清理时先应用 `max-history`，再应用 `total-size-cap`；即使仍在最近7个日期周期内，只要归档总量超过100MB，较旧归档仍会被删除。滚动只能控制应用日志文件，不能代替磁盘空间监控、备份或集中日志平台。

这些属性由Spring Boot交给默认Logback配置处理；属性入口可对照[Spring Boot官方日志说明](https://docs.spring.io/spring-boot/reference/features/logging.html)，日期周期、大小序号和清理顺序可对照[Logback滚动文件说明](https://logback.qos.ch/manual/appenders.html#SizeAndTimeBasedRollingPolicy)。

#### 1.5 pattern：决定每一行日志的格式

`logging.pattern.console` 控制控制台格式，`logging.pattern.file` 控制文件格式。本章让两处保持一致，方便用同一个requestId对照控制台和文件。

| 配置 | 可接受的值 | 本章值 | 作用 |
| --- | --- | --- | --- |
| `logging.pattern.console` | 合法的Logback日志格式字符串 | 本章所示格式 | 决定控制台中每一行日志的字段和顺序 |
| `logging.pattern.file` | 合法的Logback日志格式字符串 | 与控制台相同 | 决定日志文件中每一行日志的字段和顺序 |

```text
%d{yyyy-MM-dd'T'HH:mm:ss.SSSXXX} %-5level [%thread] [%X{requestId:-no-request}] %logger{36} - %msg%n
```

| 写法 | 输出内容 |
| --- | --- |
| `%d{yyyy-MM-dd'T'HH:mm:ss.SSSXXX}` | 带毫秒和UTC偏移的时间，例如 `2026-10-08T10:20:31.123+09:00` |
| `%-5level` | 左对齐、至少5个字符宽的日志等级 |
| `%thread` | 当前执行线程名 |
| `%X{requestId:-no-request}` | 读取MDC中的requestId；不存在时显示 `no-request` |
| `%logger{36}` | Logger名称，过长时缩短到约36个字符 |
| `%msg` | 代码传入的日志消息 |
| `%n` | 换行 |

UTC偏移表示该条日志使用的时间偏移量，但不包含完整的区域时区规则。Java Web时间类型和时区边界见[日期时间附录](../appendix/A11_java_web_datetime.md)。

此时学员应该先能回答四个问题：INFO配置会显示哪些等级、日志文件写在哪里、什么时候生成归档、requestId会出现在日志的什么位置。随后再编写产生这些日志的Java代码。

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

#### 2.1 Logger、SLF4J和Logback各自负责什么

这段代码第一次创建Logger：

```java
private static final Logger log =
        LoggerFactory.getLogger(RequestLoggingFilter.class);
```

- `SLF4J` 是Java日志接口规范，业务代码通过它记录日志。
- `Logger` 是SLF4J提供的日志记录接口，包含 `trace()`、`debug()`、`info()`、`warn()` 和 `error()` 等方法。
- `LoggerFactory.getLogger(RequestLoggingFilter.class)` 根据当前类取得Logger，Logger名称默认就是这个类的完整包名。
- `Logback` 是Spring Boot默认采用的日志实现，负责把SLF4J日志真正输出到控制台和文件。
- `private static final` 表示该类共享一个Logger引用，而且引用不会被重新赋值；不需要为每次请求创建Logger。

业务代码面向SLF4J编写，底层由Logback输出。Spring Boot Web Starter已经提供这套兼容组合，不要再手动加入另一套SLF4J实现，否则可能出现多个日志提供者冲突。接口定位和标准占位写法可参考[SLF4J官方手册](https://slf4j.org/manual.html)。

`log.info()` 表示按INFO等级记录消息。消息中的 `{}` 是SLF4J参数占位符，后面的参数按顺序填入：

```java
log.info(
        "request_complete method={} path={} status={} elapsedMs={}",
        request.getMethod(),
        request.getRequestURI(),
        response.getStatus(),
        elapsedMs);
```

这里四个 `{}` 分别对应method、path、status和elapsedMs。不要使用字符串拼接生成普通日志；占位写法更容易核对字段，并能在该等级关闭时避免不必要的字符串拼接。

#### 2.2 Filter为什么适合建立requestId

Filter位于Controller之前，可以包住一次HTTP请求的后续处理：

```text
请求进入
  → RequestLoggingFilter建立requestId并记录开始时间
  → Controller → Service → Mapper
  → 正常响应或异常处理结果
  → RequestLoggingFilter记录状态和耗时
  → 请求结束
```

因为requestId要在Controller、Service和异常处理器写日志之前建立，所以本章把它放在Filter，而不是某一个Controller方法中。

示例中第一次出现的主要注解、类型和方法如下：

| 类型或方法 | 参数与可接受值 | 返回值或运行效果 |
| --- | --- | --- |
| `@Component` | 无必填属性 | 让Spring扫描并管理这个Filter对象 |
| `@Order` | 任意 `int` 顺序值；数值越小越早 | 明确多个Filter之间的执行优先级 |
| `OncePerRequestFilter` | 当前类继承的Spring Web基类 | 提供每次请求执行一次过滤逻辑的入口 |
| `HttpServletRequest` | 由Servlet容器提供当前请求 | 可以读取HTTP方法和请求路径等信息 |
| `HttpServletResponse` | 由Servlet容器提供当前响应 | 可以写入响应头并读取当前响应状态 |
| `FilterChain` | 由Servlet容器组建的后续处理链 | 表示当前Filter之后还需要继续执行的处理 |
| `doFilterInternal()` | request、response、filterChain均由容器提供 | Spring Web调用的重写入口 |
| `filterChain.doFilter()` | 当前request和response，必填 | 把请求继续交给Controller等后续处理 |
| `UUID.randomUUID()` | 无参数 | 返回随机UUID，作为本次请求编号 |
| `System.nanoTime()` | 无参数 | 返回适合计算经过时间的 `long` 值，不用于显示日期 |
| `response.setHeader()` | 响应头名称和字符串值 | 设置或替换指定响应头 |
| `request.getMethod()` | 无参数 | 返回GET、POST等HTTP方法名 |
| `request.getRequestURI()` | 无参数 | 返回请求路径，不包含查询字符串 |
| `response.getStatus()` | 无参数 | 返回当前HTTP状态码整数 |

`@Order(Ordered.HIGHEST_PRECEDENCE)` 的完整属性写法是 `@Order(value = Ordered.HIGHEST_PRECEDENCE)`。`value` 接受整数，数值越小优先级越高；省略属性名后就是示例写法。这里使用最高优先级，让后续处理产生的日志都能取得requestId。以后加入Spring Security或其他Filter时，仍应通过配置和实际日志核对顺序，不能根据类名猜测。

`doFilterInternal()` 声明的 `ServletException` 表示Servlet处理失败，`IOException` 表示请求或响应读写失败。它们是后续处理链可能抛出的受检异常；当前Filter不改变异常含义，所以按重写方法签名继续声明。

#### 2.3 MDC怎样让同一次请求的日志带上同一个编号

多个请求可能同时执行，日志会互相穿插。如果只有时间和类名，很难判断若干行日志是否属于同一次调用。

`MDC` 的完整名称是Mapped Diagnostic Context（映射诊断上下文），类位于 `org.slf4j` 包。它保存当前执行上下文中的诊断键值，不是员工业务数据，也不是返回给前端的Map。

| 方法 | 参数与可接受值 | 返回值或运行效果 |
| --- | --- | --- |
| `MDC.put()` | 非空键和字符串值；本章为 `requestId` 和UUID | 把请求编号放入当前执行上下文 |
| `MDC.remove()` | 要删除的键；本章为 `requestId` | 请求结束时移除编号，防止线程复用造成串号 |

`MDC.put("requestId", requestId)` 执行后，前面日志格式中的 `%X{requestId:-no-request}` 会自动读取这个编号。`response.setHeader("X-Request-Id", requestId)` 又把相同编号返回给调用方，因此前端或测试人员可以把一次失败与服务端日志对应起来。

#### 2.4 try...finally为什么必须保留

`filterChain.doFilter()` 可能正常返回，也可能因为后续代码异常而提前退出。放在 `finally` 中的代码无论哪种情况都会执行，因此能够记录最终HTTP状态并清理MDC：

```text
startedAt = 开始时的单调时间
elapsedMs = (结束时间 - startedAt) ÷ 1,000,000
```

`System.nanoTime()` 适合计算时间间隔，不受系统时钟调整直接影响；除以1,000,000把纳秒换算成毫秒。它不能转换成日期时间，日志日期由Logback格式负责输出。

过滤器只记录HTTP方法、请求路径、状态和耗时，不记录查询字符串、请求体、Cookie或Authorization请求头，避免把密码、Token和个人信息写入日志。

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

### 5. 在.gitignore追加日志目录

本章运行后会在项目根目录生成 `logs`。它属于当前机器的运行产物，不进入源码版本管理。在项目根目录的 `.gitignore` 末尾追加：

```gitignore
# Local runtime logs
logs/
```

这里只追加两行，不要覆盖Spring Initializr已经生成的其他忽略规则。日志证据需要提交时，应摘取并脱敏后放入项目规定的证据目录，不能直接提交整个运行日志。

### 6. 完整替换GlobalExceptionHandler.java

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

## 三、运行并读取日志

设置数据库环境变量并启动应用，完成一次详情查询、一次不存在查询和一次新增：

```powershell
.\mvnw.cmd spring-boot:run
```

响应头中应出现 `X-Request-Id`。下面是格式示例，时间、线程、UUID、耗时和员工编号以实际结果为准：

```text
2026-09-14T10:20:31.123+09:00 INFO  [http-nio-8080-exec-1] [7f...c2] c.e.e.c.RequestLoggingFilter - request_complete method=GET path=/employees/1001 status=200 elapsedMs=28
2026-09-14T10:20:35.456+09:00 WARN  [http-nio-8080-exec-2] [a1...9d] c.e.e.e.GlobalExceptionHandler - employee_not_found path=/employees/999999 employeeId=999999
2026-09-14T10:20:35.457+09:00 INFO  [http-nio-8080-exec-2] [a1...9d] c.e.e.c.RequestLoggingFilter - request_complete method=GET path=/employees/999999 status=404 elapsedMs=7
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

## 四、四类故障的排查起点

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
| Service debug不出现 | `APP_SERVICE_LOG_LEVEL`和重启后的配置 | 变量设在其他终端，或修改后没有重启 | 在启动进程所在终端设置并重新启动 |
| SQL也大量输出 | Mapper Logger最终级别 | 打开了整个项目包或Mapper DEBUG | 保持Mapper为INFO，只打开目标Service |
| 同一异常出现多次 | Service与异常处理器 | 多层重复记录同一异常 | 确定一个最终记录位置 |
| 启动日志显示no-request | 日志发生时机 | 启动阶段没有HTTP请求上下文 | 属于正常现象，不伪造requestId |
| 日志无法写入 | 父目录、文件权限、磁盘空间 | 运行账号无写权限或磁盘已满 | 修正限定目录权限或按运维手顺处理容量 |
| 归档数量与预期不同 | 日期周期、文件大小、`max-history`和`total-size-cap` | 只看某一项限制，忽略每天可能生成多个序号归档 | 先按日期周期核对，再检查10MB拆分和100MB总容量清理 |
| 时间无法与其他系统对照 | 时间中的UTC偏移 | 不同环境使用不同默认时区 | 保留偏移，调查时统一换算时间范围 |

## 五、完成一次故障调查

### 1. 区分启动故障和请求期间故障

先做启动故障实验：停止应用，把当前PowerShell中的 `DB_PASSWORD` 临时改成错误值并重启。数据库连接池可能在启动阶段确认连接，也可能在第一次访问数据库时才取连接；因此先观察应用是否成功启动，不预先假定一定能取得HTTP响应。

- 如果应用启动失败：保存启动日志中的最底层数据库认证原因，不发送接口请求。
- 如果应用能够启动：只调用一次详情接口，记录状态、`X-Request-Id`和同一requestId下的数据库异常。

随后恢复正确环境变量并重启，确认应用正常启动且详情接口返回200。不要反复猜密码，也不要把错误或正确密码写进证据。

再做一次能够稳定产生requestId的运行时SQL故障实验：在个人练习分支中，把 `EmployeeMapper.xml` 的 `findById` 查询表名临时改为不存在的 `employees_log_lab_missing`，重启后调用 `GET /employees/1001`。预期得到500、`X-Request-Id`以及同一requestId下的 `database_access_error`和数据库“表不存在”原因。完成后立即恢复表名、重启，并确认同一请求重新返回200。不得在共享环境修改Mapper，也不要把错误表名保留到后续章节。

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
3. 临时把 `APP_SERVICE_LOG_LEVEL` 设为DEBUG，验证列表debug出现且Mapper SQL没有同时输出；恢复INFO并证明debug不再输出。
4. 对404、409和数据库500分别保存状态、requestId、关键日志和判定。
5. 使用断点核对一次PUT中路径id、Update DTO、Employee Entity和Mapper参数，确认日志不代替对象检查。

## 六、知道共通处理发生在哪一层

本章实际使用Servlet Filter建立requestId，第7章使用Controller Advice转换MVC异常。两者不是同一种机制：

```text
HTTP请求
  → RequestLoggingFilter：建立requestId、记录HTTP完成状态
  → DispatcherServlet与Controller
  → Service与Mapper
  → GlobalExceptionHandler：把Controller调用阶段的已知异常转换成响应
```

`RequestLoggingFilter` 使用 `@Order(Ordered.HIGHEST_PRECEDENCE)` 明确优先级，因此后续处理产生的日志能够取得requestId。Controller Advice不能替代Filter，也不能处理以后Spring Security在Controller之前拒绝的所有请求。

HandlerInterceptor、WebMvcConfigurer、自定义AOP和Spring Proxy属于进入既存项目后需要识读的共通机制，不在本章再建立第二套实验。需要比较它们的位置、回调和适用问题时，阅读[Spring共通处理机制附录](../appendix/A13_spring_common_processing_mechanisms.md)。


## 七、本章稳定状态

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
