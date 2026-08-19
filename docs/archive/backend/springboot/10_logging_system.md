
# Spring Boot 日志体系与 Logback 配置详解

> 适配 Spring Boot 3.x  
> 本讲义覆盖：**日志架构原理 → Logback 标签详解 → 基础配置与文件滚动策略 → 多环境策略 → JSON 输出 → 企业级实践模板与常见错误排查**。

---

## 一、Spring Boot 日志体系结构

Spring Boot 默认集成 **SLF4J + Logback**，实现了统一的日志门面机制。

```java
应用 → SLF4J(日志门面) → Logback(日志实现)
                       ↳ Log4j2 (可替换)
                       ↳ JUL (桥接)
```

| 层级 | 说明 |
|------|------|
| SLF4J | 日志抽象层，提供统一 API |
| Logback | Spring Boot 默认日志实现 |
| Log4j2 | 可替换为更强大的异步日志实现 |
| JUL | Java 原生日志，启动时会被桥接到 SLF4J |

---

## 二、日志配置加载优先级

Spring Boot 会自动识别以下配置文件（按优先级顺序）：

| 优先级 | 文件名 | 说明 |
|:--:|:--|:--|
| ① | `logback-spring.xml` | 推荐使用，支持 `${}` 与 `springProfile` |
| ② | `logback.xml` | 标准 Logback 文件 |
| ③ | `log4j2-spring.xml` | Log4j2 版本 |
| ④ | `log4j2.xml` | 标准 Log4j2 文件 |

---

## 三、Logback 核心结构与标签详解

### 1️⃣ 基本结构

```xml
<configuration scan="true" scanPeriod="60 seconds">
    <property name="LOG_PATH" value="./logs"/>

    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{HH:mm:ss.SSS} %-5level [%thread] %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>

    <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>${LOG_PATH}/app.log</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>${LOG_PATH}/app.%d{yyyy-MM-dd}.log</fileNamePattern>
            <maxHistory>15</maxHistory>
        </rollingPolicy>
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>

    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
        <appender-ref ref="FILE"/>
    </root>
</configuration>
```

### 2️⃣ 标签详解

| 标签 | 作用说明 |
|------|-----------|
| `<configuration>` | 根元素，控制整个日志系统。<br>属性：`scan`=是否自动监控变更；`scanPeriod`=检测间隔；`debug`=输出Logback内部日志。 |
| `<property>` | 定义全局变量，使用 `${}` 引用。 |
| `<appender>` | 定义日志输出目标，如控制台、文件、网络等。 |
| `<encoder>` | 定义日志格式编码，如 `PatternLayoutEncoder`。 |
| `<pattern>` | 定义日志格式，占位符 `%d`(时间)、`%p`(级别)、`%c`(类名)、`%m`(消息)。 |
| `<rollingPolicy>` | 文件滚动策略，如时间/大小切割。 |
| `<fileNamePattern>` | 滚动文件命名规则。 |
| `<maxHistory>` | 保留多少天的日志文件。 |
| `<appender-ref>` | 引用一个已有的 appender。 |
| `<logger>` | 指定某个包的独立日志级别。 |
| `<root>` | 全局日志级别。 |
| `<springProfile>` | 结合 Spring 多环境配置。 |

---

## 四、日志输出与格式

### 1️⃣ 控制台输出

```yaml
logging:
  level:
    root: INFO
    com.example: DEBUG
```

### 2️⃣ 文件输出

```yaml
logging:
  file:
    name: logs/app.log
  level:
    root: INFO
```

### 3️⃣ 控制台日志格式

```properties
logging.pattern.console=%d{HH:mm:ss.SSS} %-5level [%thread] %logger{36} - %msg%n
```

---

## 五、进阶配置讲解

### 1️⃣ 多环境配置（Spring Profile）

```xml
<springProfile name="dev">
    <root level="DEBUG">
        <appender-ref ref="CONSOLE"/>
    </root>
</springProfile>

<springProfile name="prod">
    <root level="INFO">
        <appender-ref ref="FILE"/>
    </root>
</springProfile>
```

### 2️⃣ 异步日志

```xml
<appender name="ASYNC" class="ch.qos.logback.classic.AsyncAppender">
    <queueSize>10000</queueSize>
    <discardingThreshold>0</discardingThreshold>
    <appender-ref ref="FILE"/>
</appender>
<root level="INFO">
    <appender-ref ref="ASYNC"/>
</root>
```

### 3️⃣ 按大小滚动

```xml
<rollingPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy">
    <fileNamePattern>${LOG_PATH}/app.%d{yyyy-MM-dd}.%i.log</fileNamePattern>
    <maxFileSize>10MB</maxFileSize>
    <maxHistory>30</maxHistory>
</rollingPolicy>
```

### 4️⃣ JSON 结构化日志

```xml
<encoder class="net.logstash.logback.encoder.LoggingEventCompositeJsonEncoder">
  <providers>
    <timestamp/>
    <level/>
    <threadName/>
    <loggerName/>
    <message/>
    <stackTrace/>
  </providers>
</encoder>
```

依赖：

```xml
<dependency>
  <groupId>net.logstash.logback</groupId>
  <artifactId>logstash-logback-encoder</artifactId>
  <version>7.4</version>
</dependency>
```

---

## 六、常用占位符与模式

| 占位符 | 含义 |
|--------|------|
| `%d{yyyy-MM-dd HH:mm:ss}` | 时间 |
| `%p` | 级别 |
| `%t` | 线程 |
| `%c{36}` | 类名 |
| `%msg` | 日志消息 |
| `%n` | 换行符 |
| `%file:%line` | 文件与行号 |

---

## 七、最佳实践

| 环境 | 推荐方案 |
|------|-----------|
| 开发 | 控制台输出 + 彩色日志 |
| 测试 | 控制台 + 文件滚动（7天） |
| 生产 | 异步日志 + JSON 输出 + ELK 收集 |
| Docker | 仅 STDOUT + JSON结构化 |

---

## 八、验证与调试

```java
@RestController
public class LogTestController {
    private static final Logger log = LoggerFactory.getLogger(LogTestController.class);
    @GetMapping("/log/test")
    public String test() {
        log.trace("trace log");
        log.debug("debug log");
        log.info("info log");
        log.warn("warn log");
        log.error("error log");
        return "ok";
    }
}
```

访问：`http://localhost:8080/log/test` 验证控制台与文件输出。

---

## 九、常见错误排查

| 问题 | 原因 | 解决方案 |
|------|------|-----------|
| 日志重复输出 | 多重 Starter 引入 `spring-boot-starter-logging` | 排除重复依赖 |
| 文件未生成 | 未配置 `<file>` 或无写入权限 | 检查路径和权限 |
| 日志过大 | 未配置滚动策略 | 添加 TimeBasedRollingPolicy |
| JSON 日志乱码 | 编码未设置 UTF-8 | 加上 `charset=UTF-8` |
| 性能低 | 同步写入阻塞 | 启用 AsyncAppender |

---

## 🔚 十、总结

- Spring Boot 默认使用 Logback，无需额外依赖即可使用。  
- 推荐使用 `logback-spring.xml`，支持 Profile 与变量引用。  
- 理解 `<appender>` + `<encoder>` 是日志体系核心。  
- 多环境配置与异步日志能显著提升性能。  
- 企业部署时建议统一结构化日志（JSON + ELK/Loki 收集）。

