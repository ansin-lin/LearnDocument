# A14 Spring Boot日志配置进阶

第11章主线已经建立Logger、请求日志和requestId。本附录专门解释该项目最终 `application.yml` 中的日志等级、文件滚动与格式设置。学习前先完成第11章案例，备份本地配置；修改后在Eclipse重启应用，用Postman验证CRUD状态保持不变，并检查Console与日志文件。

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

## 先看懂logging配置的结构

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

## level：决定哪些日志可以输出

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

## file：同时写入控制台和日志文件

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

## rollingpolicy：限制活动文件和历史文件

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

## pattern：决定每一行日志的格式

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

UTC偏移表示该条日志使用的时间偏移量，但不包含完整的区域时区规则。Java Web时间类型和时区边界见[日期时间附录](../appendix/A11_java_web_datetime.md)。

完成配置后，分别检查输出等级、文件位置、归档条件和requestId字段。



## 验证与恢复

1. 在Eclipse用INFO启动，发送一次GET和POST，确认Console与 `logs/employee-api.log` 中有日志；同一请求的 `X-Request-Id` 与日志编号一致。
2. 只把当前启动配置中的 `APP_SERVICE_LOG_LEVEL` 临时设为DEBUG并重启，观察Service调试行；确认Mapper并未因这个局部设置大量输出SQL。
3. 恢复INFO并重启。滚动归档依赖日期或文件大小，不要为了验证而在共享环境制造大量日志；可在个人练习目录观察已有归档，核对日期、序号与容量限制。
4. 若目录不可写、启动失败或日志格式异常，恢复备份的 `logging` 段再启动，确认原CRUD请求与日志输出恢复。

更复杂的多环境路径、异步日志、集中式日志平台和跨线程MDC传播需要独立的运维设计，不直接复制到本地教学工程。
