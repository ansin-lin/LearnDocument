# A11-1 Spring Boot日志配置进阶

第11章主线已经建立Logger、请求日志和requestId。本附录专门解释该项目最终 `application.yml` 中的日志等级、文件滚动与格式设置。学习前先完成第11章案例，备份本地配置；修改后在Eclipse重启应用，用Postman验证CRUD状态保持不变，并检查Console与日志文件。

完成本附录后，应能从“代码调用、有效等级、输出目标、格式、滚动、环境差异和运行调查”七个角度解释一条日志为什么出现或没有出现，并能在不泄露业务数据的前提下完成一次配置改修和验证。集中日志平台的安装不属于本附录范围，但会说明结构化日志和平台对接时需要识别的边界。

下面只列出 `application.yml` 的 `logging` 部分；数据库和MyBatis设置继续沿用第11章完整配置。

```yaml
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

## 一、先看懂logging配置的结构

`logging` 是Spring Boot的日志配置入口。第11章使用Spring Boot Web Starter默认提供的SLF4J和Logback，因此可以直接在 `application.yml` 中设置日志等级、日志文件、滚动策略和输出格式，不需要另外创建 `logback-spring.xml`。

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

### 1. 一条日志从代码到文件经过什么

```text
业务代码调用SLF4J Logger
  → 根据Logger名称计算有效等级
  → 低于门槛的事件被过滤
  → Logback把保留事件交给Console/File Appender
  → Encoder或Pattern把事件转换成一行文本或JSON
  → 写入Eclipse Console或活动日志文件
  → Rolling Policy在满足条件时归档旧文件
```

这里有三个容易混淆的角色：

| 角色 | 当前项目中的实现 | 责任 |
| --- | --- | --- |
| 日志API | SLF4J的 `Logger` | 让Java代码使用 `info()`、`warn()`、`error()` 等统一接口 |
| 日志实现 | Logback | 接收事件，执行等级过滤、格式化和输出 |
| Spring Boot配置 | `logging.*` | 把常见Logback能力映射成YAML配置，并提供默认Console/File输出 |

因此 `log.info(...)` 没有出现在文件中，不一定是代码没有执行，也可能是有效等级过滤、文件路径不同、目录不可写或当前配置没有启用文件输出。调查时应沿上面的顺序确认，不要一开始就修改业务代码。

## 二、level：决定哪些日志可以输出

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

第11章的四条等级配置按“全局 → 项目包 → 具体类或子包”逐步缩小范围：

| 配置 | 可接受的值 | 第11章值 | 作用 |
| --- | --- | --- | --- |
| `logging.level.root` | `TRACE`、`DEBUG`、`INFO`、`WARN`、`ERROR`、`FATAL`、`OFF` | `INFO` | 所有Logger的默认最低等级；没有更具体配置时使用它 |
| `logging.level.com.example.employee` | 同一组等级值，也可以使用环境变量占位符 | `INFO` | 整个项目包使用INFO，明确项目代码的基础等级 |
| `logging.level.com.example.employee.service.impl.EmployeeServiceImpl` | 同一组等级值，也可以使用环境变量占位符 | `${APP_SERVICE_LOG_LEVEL:INFO}` | 只允许通过环境变量临时调整这个Service类；变量未设置时使用INFO |
| `logging.level.com.example.employee.mapper` | 同一组等级值，也可以使用环境变量占位符 | `INFO` | 明确保持Mapper为INFO，防止排查Service时误输出大量SQL细节 |

Logger名称通常是类的完整包名。多个配置同时匹配时，范围更具体的配置生效。因此把 `EmployeeServiceImpl` 调成DEBUG不会把整个项目或Mapper一起调成DEBUG。

`${APP_SERVICE_LOG_LEVEL:INFO}` 是Spring Boot占位符：先读取环境变量 `APP_SERVICE_LOG_LEVEL`，没有设置时使用冒号后的默认值 `INFO`。本地需要查看Service调试日志时，打开Eclipse的 **Run → Run Configurations... → Environment**，在当前启动配置中增加 `APP_SERVICE_LOG_LEVEL=DEBUG`，点击 **Apply** 后重新启动应用。

验证结束后停止应用，从同一个运行配置中删除该变量或把值恢复为 `INFO`，再重新启动。环境变量在进程启动时读取；只修改配置但不重启，不会改变已经运行的Logger等级。

MyBatis通常使用Mapper接口名或XML的namespace作为Logger名称。Mapper达到DEBUG时可能输出SQL，进一步提高到TRACE时还可能出现更细的结果信息。因此不能为了查看一条Service调试日志而把 `root` 或整个项目包长期改成DEBUG。

## 三、file：同时写入控制台和日志文件

```yaml
logging:
  file:
    name: logs/employee-api.log
```

`logging.file.name` 指定当前正在写入的日志文件。设置它以后，日志仍会显示在控制台，同时还会写入文件。

| 配置 | 可接受的值 | 第11章值 | 作用 |
| --- | --- | --- | --- |
| `logging.file.name` | 应用进程有权写入的相对或绝对文件路径 | `logs/employee-api.log` | 指定当前活动日志文件，并启用文件输出 |

第11章使用相对路径 `logs/employee-api.log`：

- `logs` 是目录；
- `employee-api.log` 是当前活动日志文件；
- 相对路径以启动应用时的工作目录为基准；
- 目录不存在时，默认日志系统会尝试创建；
- 部署环境应由运行目录、权限和磁盘规划决定最终路径，不能假设一定与本地相同。

只配置 `logging.file.path` 也能指定目录，但同时配置 `name` 和 `path` 时容易让新人误判实际文件位置。第11章只使用更明确的 `logging.file.name`。

## 四、rollingpolicy：限制活动文件和历史文件

如果一直向同一个文件追加日志，文件会持续增大。滚动（rolling）是指达到条件后，把当前活动文件归档，再建立新的活动文件继续写入。

第11章的过程是：

```text
持续写入 logs/employee-api.log
  → 日期进入下一天，或同一天内文件达到10MB
  → 按日期和序号生成压缩归档文件
  → 新的 employee-api.log 继续接收日志
  → 超过历史数量或总容量时清理较旧归档
```

| 配置 | 可接受的值 | 第11章值 | 作用 |
| --- | --- | --- | --- |
| `file-name-pattern` | 合法的Logback滚动文件名格式 | `${LOG_FILE}.%d{yyyy-MM-dd}.%i.gz` | 指定归档文件的名称；日期区分日期，序号区分同一天的多个文件，`.gz` 表示压缩 |
| `max-file-size` | 例如 `10MB`、`100MB` | `10MB` | 同一个日期周期内，当前活动文件达到该大小时触发滚动 |
| `max-history` | 非负整数 | `7` | 保留最近7个日期周期；第11章按天滚动，因此约为7天，每天可以有多个序号归档 |
| `total-size-cap` | 例如 `100MB`、`1GB`；`0B` 表示不限制 | `100MB` | 所有归档文件合计超过该容量时清理较旧文件 |
| `clean-history-on-start` | `true` 或 `false` | `false` | 是否在应用启动时立即执行历史归档清理；false不代表永远不清理 |

`${LOG_FILE}` 代表前面 `logging.file.name` 确定的当前日志文件，所以归档文件仍生成在 `logs` 目录。`%d{yyyy-MM-dd}` 写入归档日期，`%i` 是同一日期内从0开始递增的序号。实际文件名可能类似：

```text
employee-api.log.2026-10-08.0.gz
employee-api.log.2026-10-08.1.gz
```

第11章的 `%d{yyyy-MM-dd}` 表示按天划分周期，`%i` 和 `max-file-size` 又把同一天的大文件继续拆分。因此滚动既可能由日期变化后的第一条新日志触发，也可能由文件达到10MB触发。

清理时先应用 `max-history`，再应用 `total-size-cap`；即使仍在最近7个日期周期内，只要归档总量超过100MB，较旧归档仍会被删除。滚动只能控制应用日志文件，不能代替磁盘空间监控、备份或集中日志平台。

这些属性由Spring Boot交给默认Logback配置处理；属性入口可对照[Spring Boot官方日志说明](https://docs.spring.io/spring-boot/reference/features/logging.html)，日期周期、大小序号和清理顺序可对照[Logback滚动文件说明](https://logback.qos.ch/manual/appenders.html#SizeAndTimeBasedRollingPolicy)。

## 五、pattern：决定每一行日志的格式

`logging.pattern.console` 控制控制台格式，`logging.pattern.file` 控制文件格式。第11章让两处保持一致，方便用同一个requestId对照控制台和文件。

| 配置 | 可接受的值 | 第11章值 | 作用 |
| --- | --- | --- | --- |
| `logging.pattern.console` | 合法的Logback日志格式字符串 | 第11章所示格式 | 决定控制台中每一行日志的字段和顺序 |
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

UTC偏移表示该条日志使用的时间偏移量，但不包含完整的区域时区规则。Java Web时间类型和时区边界见[A09-1日期时间附录](A09_01_java_web_datetime.md)。

完成配置后，分别检查输出等级、文件位置、归档条件和requestId字段。

## 六、有效等级怎样由Logger名称决定

Logger通常使用类的完整名称，例如：

```text
com.example.employee.service.impl.EmployeeServiceImpl
```

配置会从最具体名称逐级向父名称查找等级。假设配置为：

```yaml
logging:
  level:
    root: WARN
    com.example.employee: INFO
    com.example.employee.service: DEBUG
    com.example.employee.service.impl.EmployeeServiceImpl: TRACE
```

则有效结果是：

| Logger | 有效门槛 | 原因 |
| --- | --- | --- |
| `EmployeeServiceImpl` | TRACE | 精确类名最具体 |
| 同包的其他Service实现 | DEBUG | 命中 `com.example.employee.service` |
| Controller和项目其他类 | INFO | 命中 `com.example.employee` |
| 第三方库 | WARN | 没有更具体配置，回到root |

等级继承回答“事件是否允许输出”。Logback的Appender继承与additivity回答“事件交给哪些输出目标”。主线只使用Spring Boot默认Appender，不需要手写additivity；既存项目若在 `logback-spring.xml` 中给子Logger配置独立Appender又保持 `additivity="true"`，同一事件可能同时交给子Logger和父Logger的Appender，造成重复日志。看到完全相同的日志行重复出现时，应同时检查代码是否重复调用以及Logger/Appender继承，而不是只搜索两次 `log.info()`。

## 七、不同环境怎样设置路径和等级

本地开发可以使用相对路径，服务器通常需要由部署手顺确定的受控目录。不要把本机绝对路径写死在公共配置中。可以让环境变量覆盖：

```yaml
logging:
  level:
    root: ${APP_ROOT_LOG_LEVEL:INFO}
    com.example.employee: ${APP_LOG_LEVEL:INFO}
  file:
    name: ${APP_LOG_FILE:logs/employee-api.log}
```

| 占位符 | 可接受的值 | 未设置时 | 用途 |
| --- | --- | --- | --- |
| `APP_ROOT_LOG_LEVEL` | 合法日志等级 | `INFO` | 控制全局门槛，生产环境不应无审批长期设为DEBUG/TRACE |
| `APP_LOG_LEVEL` | 合法日志等级 | `INFO` | 单独调整项目代码范围 |
| `APP_LOG_FILE` | 当前进程有权限写入的文件路径 | `logs/employee-api.log` | 让部署环境决定实际目录 |

环境差异不是“开发用一套源码、生产改另一套源码”。相同构建产物通过外部配置获得环境值。修改前要记录当前Profile、启动目录、最终属性来源和运行账号权限；修改后重启并确认实际输出位置。

不能把数据库密码、Session ID、Authorization、Cookie或Token放进日志路径、pattern、MDC或启动参数证据。环境变量能外部化配置，但不代表其内容可以安全输出。

## 八、什么时候才需要 `logback-spring.xml`

当前YAML已经能完成等级、单一文件、滚动和pattern，因此不应为了“更专业”立即新增XML。遇到下面需求时，才评估自定义Logback配置：

- 不同包写入不同文件；
- 为某个Appender单独设置过滤器；
- 使用AsyncAppender；
- 根据Profile组合复杂Appender；
- 接入项目批准的特殊日志组件。

Spring Boot扩展应写在 `logback-spring.xml`，而不是普通 `logback.xml`。普通文件加载得更早，不能使用Spring Boot提供的 `<springProfile>` 和 `<springProperty>` 扩展。自定义XML还会接管较多默认行为，必须同时验证Console、文件、滚动、编码、异常堆栈和停机刷新，不能只确认应用启动。

下面只用于识读配置来源，不要求加入主线：

```xml
<springProperty
        scope="context"
        name="appName"
        source="spring.application.name"
        defaultValue="employee-api"/>

<springProfile name="dev | test">
    <!-- 仅在dev或test Profile生效的Appender/Logger -->
</springProfile>
```

| XML元素/属性 | 当前值可接受范围 | 作用 |
| --- | --- | --- |
| `source` | Spring Environment中的kebab-case属性名 | 读取外部配置 |
| `defaultValue` | 属性不存在时使用的字符串 | 防止变量没有定义 |
| `scope` | `local`、`context`、`system` | 决定变量可见范围；示例使用当前日志上下文 |
| `<springProfile name>` | Profile名称或表达式 | 限定一段Logback配置的生效环境 |

## 九、结构化日志怎样与文本日志选择

固定文本格式适合新人在Console直接阅读；日志平台更适合接收字段明确的JSON。Spring Boot 4可以通过以下配置启用内置结构化格式：

```yaml
logging:
  structured:
    format:
      console: ecs
      file: ecs
```

`console` 和 `file` 可接受Spring Boot支持的格式ID，例如 `ecs`、`gelf`、`logstash`，也可在高级项目中指定自定义格式类。启用后，输出不再使用本附录前面的普通pattern；每行成为结构化JSON。MDC中的 `requestId` 会作为结构化字段加入输出。

不要同时把“人类阅读pattern”和“JSON结构化格式”描述为同一输出目标的最终格式。项目应根据采集平台决定：本地Console可以保留易读文本，服务器文件或标准输出可以采用平台要求的JSON。格式变化属于接口变化，日志采集规则、告警和测试证据都要同步确认。

结构化不等于安全。把完整DTO、邮箱或Token作为键值加入JSON，仍然是敏感信息泄露，而且更容易被集中检索和长期保存。

## 十、异步日志、磁盘和异常堆栈的边界

### 1. 异步日志

AsyncAppender可以让业务线程先把事件放入队列，再由后台线程写出，降低同步I/O等待。但它引入队列容量、事件丢弃、内存占用和停机刷新问题。没有性能数据时，不要仅凭“异步更快”修改主线。

### 2. 磁盘容量

`max-history` 和 `total-size-cap` 只管理该滚动策略控制的归档文件。它们不监控整个磁盘，也不能清理其他应用文件。磁盘已满时日志可能写不进去，应用是否继续运行取决于故障位置和配置；运维监控与容量告警仍不可缺少。

### 3. 异常堆栈

```java
log.error("database_access_error path={}", path, exception);
```

异常对象应作为最后一个参数传入，Logback才能输出堆栈。下面写法只输出异常的字符串，不包含完整调用链：

```java
log.error("database_access_error exception={}", exception.toString());
```

同一异常通常只在能够决定最终处理结果的边界记录一次堆栈。Service记录后原样抛出、全局处理器再记录，会产生重复ERROR并干扰告警计数。

## 十一、配置改修验证矩阵

不要只确认“看到了某一行日志”。一次日志配置改修至少验证：

| 场景 | 操作 | 预期证据 |
| --- | --- | --- |
| INFO门槛 | 正常GET | Console和文件均有请求完成日志 |
| DEBUG局部开启 | 只调整Service Logger | Service DEBUG出现，Mapper和全局第三方日志没有大量增加 |
| requestId | Postman发送两次请求 | 每次Header与日志一致，两次编号不同 |
| 业务异常 | 请求不存在员工 | WARN及404共享同一requestId，不输出多份堆栈 |
| 系统异常 | 仅在专用环境制造数据库失败 | ERROR保留原因链，响应不泄露数据库信息 |
| 文件路径 | 从Eclipse启动 | 实际文件位置与启动工作目录、配置值一致 |
| 重启恢复 | 移除临时DEBUG变量后重启 | 有效门槛恢复INFO |

## 十二、常见故障与调查顺序

| 症状 | 先确认 | 常见原因 |
| --- | --- | --- |
| Console有、文件没有 | `logging.file.name`和实际工作目录 | 文件输出未启用、看错目录、权限不足 |
| DEBUG仍不出现 | Logger名称、最具体等级、是否重启 | 调整了错误包名、环境变量覆盖、旧进程仍在运行 |
| 同一日志出现两次 | 代码记录位置、Appender和additivity | 多层重复记录或自定义XML重复Appender |
| requestId显示no-request | 日志发生线程和时机 | 启动日志、异步线程或Filter尚未设置MDC |
| 归档未立即生成 | 日期、活动文件大小、是否有新事件 | 尚未达到滚动条件，不是配置一定失效 |
| 文件持续变大 | pattern、max-file-size、滚动配置层级 | YAML缩进错误或自定义配置覆盖默认策略 |
| 日志乱码 | 文件编码、查看工具编码 | 写入与读取编码不一致 |

调查时先保存启动时实际Profile、相关配置和最早错误，再检查文件系统。不要为了验证滚动在共享环境制造大量无意义日志。

## 十三、验证与恢复

1. 在Eclipse用INFO启动，发送一次GET和POST，确认Console与 `logs/employee-api.log` 中有日志；同一请求的 `X-Request-Id` 与日志编号一致。
2. 只把当前启动配置中的 `APP_SERVICE_LOG_LEVEL` 临时设为DEBUG并重启，观察Service调试行；确认Mapper并未因这个局部设置大量输出SQL。
3. 恢复INFO并重启。滚动归档依赖日期或文件大小，不要为了验证而在共享环境制造大量日志；可在个人练习目录观察已有归档，核对日期、序号与容量限制。
4. 若目录不可写、启动失败或日志格式异常，恢复备份的 `logging` 段再启动，确认原CRUD请求与日志输出恢复。

更复杂的多环境路径、异步日志、集中式日志平台和跨线程MDC传播需要独立的运维设计，不直接复制到本地教学工程。

## 十四、完成标准与官方参考

完成后，应能根据Logger名称算出有效等级，解释Console/File/Pattern/Rolling Policy之间的关系，判断何时继续用YAML、何时需要 `logback-spring.xml`，并用验证矩阵证明配置修改没有扩大敏感信息和日志量。

- [Spring Boot Logging](https://docs.spring.io/spring-boot/reference/features/logging.html)
- [Spring Boot Common Application Properties](https://docs.spring.io/spring-boot/appendix/application-properties/)
- [Logback Manual](https://logback.qos.ch/manual/index.html)
