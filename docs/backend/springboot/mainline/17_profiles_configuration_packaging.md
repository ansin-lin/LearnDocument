# 第17章 多环境配置与打包

> 本章目标：把第16章完成的员工管理API拆分为公共、开发、测试和生产配置，校验项目自己的配置项，通过Maven测试并生成可执行JAR，最后在不依赖IDE的情况下启动和核对交付物。

## 一、为什么同一份代码需要多套配置

开发、测试和生产环境运行同一套业务代码，但外部条件不同：

| 项目 | dev开发环境 | test自动化测试 | prod生产环境 |
| --- | --- | --- | --- |
| 数据库 | 本机MySQL 8.0 | H2内存数据库 | 受控MySQL地址 |
| 端口 | 本机默认8080 | MockMvc通常不监听真实端口 | 由部署环境决定 |
| 日志 | INFO，项目内练习日志 | WARN，写入target | INFO，写入受控目录 |
| 发布标识 | 本地快照 | 自动化测试 | 实际发布编号 |
| 凭据 | 当前终端的练习变量 | 测试专用值 | 密钥管理或受控环境变量 |

如果把这些差异写进Java代码，每次部署都要重新改代码、编译和测试。多环境配置的目标是：**业务代码和JAR保持一致，只替换运行环境提供的配置值**。

```text
同一个可执行JAR
├── dev配置  → 本机开发验证
├── test配置 → 自动化回归
└── prod配置 → 受控生产运行
```

Profile只是选择一组配置，不是权限控制，也不能把生产密码藏进JAR。第16章的角色权限仍由Spring Security执行。

## 二、本章开始状态与完成结果

开始前应满足：

- 第16章全部自动化测试通过。
- 项目根目录包含 `pom.xml` 和Maven Wrapper。
- Java目标版本仍为17，Spring Boot仍为3.5.16。
- `pom.xml` 已包含Validation、MySQL驱动、H2测试依赖和Spring Boot Maven Plugin。

完成后，配置和新增Java文件如下：

```text
src/main/java/com/example/employee/
├── EmployeeManagementApiApplication.java         ← 完整替换
├── config/DeploymentProperties.java               ← 新建
└── config/StartupConfigurationLogger.java         ← 新建
src/main/resources/
├── application.yml                                ← 完整替换
├── application-dev.yml                            ← 新建
└── application-prod.yml                           ← 新建
src/test/java/com/example/employee/config/
└── DeploymentPropertiesTest.java                  ← 新建
src/test/resources/
└── application-test.yml                           ← 完整替换
deploy/
└── application-prod.yml.example                   ← 新建交付模板
```

员工Controller、Service、Mapper、认证、授权和SQL文件保持第16章状态，不要重写。

## 三、完整示例

### 1. 完整替换application.yml

文件位置：`src/main/resources/application.yml`

```yaml
spring:
  application:
    name: employee-management-api

mybatis:
  mapper-locations: classpath:mapper/*.xml
  type-aliases-package: com.example.employee.entity

logging:
  logback:
    rollingpolicy:
      max-file-size: 10MB
      max-history: 7
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss.SSS} %-5level [%X{requestId:-no-request}] %logger{36} - %msg%n"
    file: "%d{yyyy-MM-dd HH:mm:ss.SSS} %-5level [%X{requestId:-no-request}] %logger{36} - %msg%n"
```

这份文件只保存所有环境共同使用的应用名、MyBatis位置和日志格式，不再包含数据库账号、密码、端口和日志文件路径。

### 2. 新建application-dev.yml

文件位置：`src/main/resources/application-dev.yml`

```yaml
spring:
  config:
    activate:
      on-profile: dev
  datasource:
    url: ${DB_URL:jdbc:mysql://localhost:3306/employee_db?useUnicode=true&characterEncoding=UTF-8&connectionTimeZone=Asia/Tokyo}
    username: ${DB_USERNAME:employee_app}
    password: ${DB_PASSWORD}
    driver-class-name: com.mysql.cj.jdbc.Driver

server:
  port: ${SERVER_PORT:8080}

logging:
  level:
    root: INFO
    com.example.employee: ${APP_LOG_LEVEL:INFO}
  file:
    name: ${APP_LOG_FILE:logs/employee-api.log}

app:
  deployment:
    environment-name: ${APP_ENVIRONMENT_NAME:local-development}
    release-id: ${APP_RELEASE_ID:local-snapshot}
```

开发环境允许数据库地址、用户名、端口和非敏感发布信息使用本地默认值；数据库密码没有默认值，必须由启动进程所在环境提供。

### 3. 新建application-prod.yml

文件位置：`src/main/resources/application-prod.yml`

```yaml
spring:
  config:
    activate:
      on-profile: prod
  datasource:
    url: ${DB_URL}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
    driver-class-name: com.mysql.cj.jdbc.Driver

server:
  port: ${SERVER_PORT:8080}

logging:
  level:
    root: ${ROOT_LOG_LEVEL:INFO}
    com.example.employee: ${APP_LOG_LEVEL:INFO}
  file:
    name: ${APP_LOG_FILE}

app:
  deployment:
    environment-name: ${APP_ENVIRONMENT_NAME}
    release-id: ${APP_RELEASE_ID}
```

生产配置不给数据库连接、日志文件、环境名和发布编号设置默认值。缺少这些值时让应用启动失败，比静默连接到错误数据库更安全。

### 4. 完整替换application-test.yml

文件位置：`src/test/resources/application-test.yml`

```yaml
spring:
  config:
    activate:
      on-profile: test
  datasource:
    url: jdbc:h2:mem:employee_test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1
    username: sa
    password: ""
    driver-class-name: org.h2.Driver
  sql:
    init:
      mode: never

logging:
  file:
    name: target/test-logs/employee-api.log
  level:
    root: WARN
    com.example.employee: WARN

app:
  deployment:
    environment-name: automated-test
    release-id: test-run
```

原有测试都通过 `@ActiveProfiles("test")` 激活这份配置。H2、测试日志位置和MySQL兼容模式保持第12章状态，本章只补上Profile声明和结构化配置值。

### 5. 新建DeploymentProperties.java

文件位置：`src/main/java/com/example/employee/config/DeploymentProperties.java`

```java
package com.example.employee.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.deployment")
public class DeploymentProperties {

    @NotBlank(message = "environment-name不能为空")
    private String environmentName;

    @NotBlank(message = "release-id不能为空")
    private String releaseId;

    public String getEnvironmentName() {
        return environmentName;
    }

    public void setEnvironmentName(String environmentName) {
        this.environmentName = environmentName;
    }

    public String getReleaseId() {
        return releaseId;
    }

    public void setReleaseId(String releaseId) {
        this.releaseId = releaseId;
    }
}
```

### 6. 新建StartupConfigurationLogger.java

文件位置：`src/main/java/com/example/employee/config/StartupConfigurationLogger.java`

```java
package com.example.employee.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class StartupConfigurationLogger implements ApplicationRunner {

    private static final Logger log =
            LoggerFactory.getLogger(StartupConfigurationLogger.class);

    private final DeploymentProperties deploymentProperties;

    public StartupConfigurationLogger(
            DeploymentProperties deploymentProperties) {
        this.deploymentProperties = deploymentProperties;
    }

    @Override
    public void run(ApplicationArguments arguments) {
        log.info(
                "application_configuration_loaded "
                        + "environmentName={} releaseId={}",
                deploymentProperties.getEnvironmentName(),
                deploymentProperties.getReleaseId());
    }
}
```

启动日志只记录非敏感的环境名和发布编号，不能追加数据库密码、Session ID、CSRF令牌或完整数据库URL。

### 7. 完整替换启动类

文件位置：`src/main/java/com/example/employee/EmployeeManagementApiApplication.java`

```java
package com.example.employee;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class EmployeeManagementApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(
                EmployeeManagementApiApplication.class, args);
    }
}
```

### 8. 新建DeploymentPropertiesTest.java

文件位置：`src/test/java/com/example/employee/config/DeploymentPropertiesTest.java`

```java
package com.example.employee.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class DeploymentPropertiesTest {

    @Autowired
    private DeploymentProperties deploymentProperties;

    @Test
    void testProfileBindsDeploymentProperties() {
        assertThat(deploymentProperties.getEnvironmentName())
                .isEqualTo("automated-test");
        assertThat(deploymentProperties.getReleaseId())
                .isEqualTo("test-run");
    }
}
```

### 9. 新建生产外部配置模板

文件位置：`deploy/application-prod.yml.example`

```yaml
# 复制为运行目录下的config/application-prod.yml后才会被Spring Boot读取。
# 本文件只保存非秘密配置和环境变量占位符，不填写真实值。
server:
  port: ${SERVER_PORT:8080}

logging:
  file:
    name: ${APP_LOG_FILE}

app:
  deployment:
    environment-name: ${APP_ENVIRONMENT_NAME}
    release-id: ${APP_RELEASE_ID}
```

模板不重复数据库设置，未写的属性继续使用JAR内部 `application-prod.yml`。实际运行文件和环境变量由部署环境管理，不要把填写真实值后的文件提交到Git。

## 四、application.yml和Profile文件怎样合并

启动 `dev` Profile时，Spring Boot先读取 `application.yml`，再读取 `application-dev.yml`。相同键由Profile文件覆盖，不同键合并：

```text
application.yml
  mybatis.mapper-locations = classpath:mapper/*.xml
        +
application-dev.yml
  spring.datasource.url = 本机MySQL
  server.port = 8080
        ↓
dev环境最终配置同时包含以上三项
```

`spring.config.activate.on-profile` 声明当前文档只在指定Profile激活时生效。

`spring.application.name` 是应用的逻辑名称，可供日志、监控和其他Spring组件识别当前应用；它不会修改Maven坐标或JAR文件名。`app.deployment` 则是本项目自定义的配置前缀，`app` 不是Spring Boot保留关键字。

| 配置键 | 当前值 | 可接受的值 | 默认值与作用 |
| --- | --- | --- | --- |
| `spring.config.activate.on-profile` | `dev`、`test`或`prod` | 合法Profile名或表达式 | 无默认值；条件满足时加载当前文档 |
| `spring.profiles.active` | 不写入配置文件 | 一个或多个Profile名 | 未指定时没有本课程Profile被激活 |

本课程不在 `application.yml` 固定激活dev，防止JAR在服务器上忘记指定环境时自动连接开发配置。`spring.profiles.active` 也不能写在Profile专用文件或由 `on-profile` 激活的文档中；应由环境变量或启动参数选择。参考[Spring Boot Profiles](https://docs.spring.io/spring-boot/reference/features/profiles.html)。

只改文件名但不激活对应Profile，不会产生环境切换。启动日志应出现：

```text
The following 1 profile is active: "dev"
application_configuration_loaded environmentName=local-development releaseId=local-snapshot
```

第二行是明确标注的预期日志示例，具体时间、线程和Logger名称会因运行环境不同而变化。

## 五、配置来源和覆盖顺序

Spring Boot能从JAR内部文件、JAR外部文件、操作系统环境变量和命令行参数读取配置。同一个键出现多次时，当前示例中可按下面的低到高顺序判断：

```text
JAR内application.yml
        ↓ 被覆盖
JAR内application-{profile}.yml
        ↓ 被覆盖
运行目录外部application.yml / application-{profile}.yml
        ↓ 被覆盖
操作系统环境变量
        ↓ 被覆盖
命令行 --key=value
```

例如dev文件给出端口默认值8080，当前PowerShell设置 `SERVER_PORT=8081` 后变成8081；再追加 `--server.port=8082`，最终使用8082。

这个顺序只概括本章实际使用的来源。Spring Boot还有Java系统属性、测试属性等来源，完整顺序参见[Spring Boot外部化配置](https://docs.spring.io/spring-boot/reference/features/external-config.html)。排错时不能只打开一份YAML就认定最终值，还要检查激活Profile、运行目录、环境变量和启动参数。

生产环境通常用同一个JAR配合外部配置和受控变量。外部文件应只覆盖当前环境确实不同的键，不必复制JAR内全部公共配置。

## 六、占位符、默认值和缺失配置

下面两种写法结果不同：

```yaml
port: ${SERVER_PORT:8080}
password: ${DB_PASSWORD}
```

| 写法 | 可接受的值 | 变量存在时 | 变量缺失时 |
| --- | --- | --- | --- |
| `${SERVER_PORT:8080}` | 可转换为端口整数的文本 | 使用环境变量 | 使用默认值8080 |
| `${DB_PASSWORD}` | 数据库接受的密码文本 | 使用环境变量 | 占位符无法解析，启动失败 |

是否提供默认值由风险决定。开发端口缺失时回到8080通常可以接受；生产数据库、日志路径和发布编号缺失时不应猜值。

不要通过下面的命令传递生产密码：

```text
java -jar app.jar --spring.datasource.password=真实密码
```

命令行可能进入终端历史、进程信息或作业日志。生产秘密应来自部署平台密钥管理、权限受控的服务环境或同等机制；普通环境变量只是配置注入方式，不自动等于完善的秘密管理。

## 七、@ConfigurationProperties怎样形成Java对象

YAML中的层级：

```yaml
app:
  deployment:
    environment-name: local-development
    release-id: local-snapshot
```

会绑定到 `DeploymentProperties`：

```text
app.deployment.environment-name → setEnvironmentName(...)
app.deployment.release-id       → setReleaseId(...)
```

`@ConfigurationProperties(prefix = "app.deployment")` 来自Spring Boot，把指定前缀下的一组配置绑定为结构化对象。`prefix` 必填，本例接受规范的小写点分名称；YAML使用短横线，Java字段使用camelCase，Spring Boot会进行宽松名称绑定。

`@ConfigurationPropertiesScan` 写在启动类上，从启动类所在包向下扫描配置属性类并注册Bean。它不会替代 `@SpringBootApplication`，两个注解职责不同。

与在多个类中分别读取字符串相比，结构化对象具备明确字段、类型和统一校验位置。数据库连接仍交给Spring Boot自带的DataSource配置类，本章只为项目自己的 `app.deployment` 创建对象。

## 八、配置校验为什么在启动时执行

`@Validated` 来自Spring Framework，要求Spring在绑定后执行Jakarta Validation。两个 `@NotBlank` 来自第8章：值不能是 `null`、空字符串或纯空白。

```text
读取配置 → 创建DeploymentProperties → 调用setter绑定
        → 执行@NotBlank → 全部通过后注册并注入
```

如果prod缺少 `APP_RELEASE_ID`，绑定或占位符解析阶段会让应用启动失败，而不是运行到某次请求才发现。`spring-boot-starter-validation` 已在第8章和第15章的完整 `pom.xml` 中存在，本章不新增依赖。官方绑定校验说明见[`@ConfigurationProperties` Validation](https://docs.spring.io/spring-boot/reference/features/external-config.html#features.external-config.typesafe-configuration-properties.validation)。

`DeploymentPropertiesTest` 使用第12章已学的 `@SpringBootTest` 和 `@ActiveProfiles("test")`。它从 `application-test.yml` 取得两个值，断言配置文件、扫描、绑定和Bean注入已经连接成功。

## 九、ApplicationRunner与启动确认日志

`ApplicationRunner` 是Spring Boot提供的启动回调接口。应用上下文创建成功后，框架调用一次 `run(ApplicationArguments)`。本章用它记录最终绑定的环境名和发布编号：

| 接口或方法 | 当前参数 | 可接受的值 | 返回或效果 |
| --- | --- | --- | --- |
| `implements ApplicationRunner` | 无显式参数 | 实现该接口的Spring Bean | 注册应用启动回调 |
| `run(arguments)` | `ApplicationArguments` | 框架提供的启动参数对象 | 无返回值；执行一次当前日志代码 |
| `getEnvironmentName()` | 无 | 无 | 返回绑定后的环境名 |
| `getReleaseId()` | 无 | 无 | 返回绑定后的发布编号 |

`ApplicationArguments` 封装传给应用的启动参数，本例不读取它，但实现接口时仍必须保留方法参数。这里的日志证明“配置已经加载”，不等于数据库、外部服务和全部业务接口都健康；完整运行检查在第18章完成。

## 十、先测试，再生成可执行JAR

在Windows PowerShell中进入包含 `pom.xml` 的项目根目录，确认环境：

```powershell
java -version
.\mvnw.cmd -version
```

两条命令都应显示Java 17。然后执行：

```powershell
.\mvnw.cmd clean package
```

| 命令部分 | 可接受的值 | 作用 | 结果 |
| --- | --- | --- | --- |
| `.\mvnw.cmd` | 项目内Windows Maven Wrapper | 使用项目约定的Maven运行构建 | 不依赖IDE内置按钮 |
| `clean` | Maven生命周期阶段 | 清理旧target内容 | 避免误交付旧产物 |
| `package` | Maven生命周期阶段 | 编译主代码和测试、运行测试并打包 | 成功后生成JAR |

构建必须以 `BUILD SUCCESS` 结束，并确认第12～17章的旧测试和新增配置测试全部通过。不要只看到target中存在旧JAR就认为本次构建成功。

本项目继承 `spring-boot-starter-parent`，并已在 `pom.xml` 声明：

```xml
<plugin>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-maven-plugin</artifactId>
</plugin>
```

父项目已经配置 `repackage` 执行，Maven Plugin会把项目类和运行依赖重新组织成可以用 `java -jar` 启动的归档。官方说明见[Spring Boot可执行归档打包](https://docs.spring.io/spring-boot/maven-plugin/packaging.html)。

预期主要产物：

```text
target/employee-management-api-0.0.1-SNAPSHOT.jar
```

名称来自 `pom.xml` 的 `artifactId` 和 `version`。`SNAPSHOT` 表示开发中的快照版本；正式项目的发布版本应由团队发布规则或CI确定，不能手工给不同文件随意改名。

## 十一、检查JAR而不是只检查文件名

先确认文件时间和大小：

```powershell
Get-Item target\employee-management-api-0.0.1-SNAPSHOT.jar |
    Select-Object Name, Length, LastWriteTime
```

再检查归档结构：

```powershell
jar tf target\employee-management-api-0.0.1-SNAPSHOT.jar |
    Select-String 'BOOT-INF/classes|BOOT-INF/lib'
```

`jar` 是JDK提供的归档工具，`tf` 中的 `t` 表示列出目录，`f` 表示后面给出文件名。Spring Boot可执行JAR通常把应用类和资源放在 `BOOT-INF/classes`，依赖放在 `BOOT-INF/lib`。普通JAR只有项目类而没有运行依赖时，常会在脱离IDE后出现找不到类。

最后计算本次产物摘要：

```powershell
Get-FileHash `
    target\employee-management-api-0.0.1-SNAPSHOT.jar `
    -Algorithm SHA256
```

SHA-256可用于确认交付前后是否为相同字节内容，但普通摘要不是数字签名，不能独自证明发布者身份。

| PowerShell/JDK命令 | 当前参数 | 可接受的值 | 返回结果 |
| --- | --- | --- | --- |
| `Get-Item` | JAR路径 | 已存在的单个路径 | 返回文件对象，包含大小和修改时间 |
| `Select-Object` | `Name, Length, LastWriteTime` | 文件对象具有的属性名 | 只显示选择的属性 |
| `Select-String` | 两个JAR内部路径模式 | 普通文本或正则表达式 | 只保留匹配的归档目录行 |
| `Get-FileHash -Algorithm SHA256` | JAR路径 | 已存在文件；算法使用系统支持值 | 返回算法、摘要和文件路径 |

## 十二、脱离IDE启动dev配置

在将要执行 `java -jar` 的同一个PowerShell窗口设置练习变量：

```powershell
$env:SPRING_PROFILES_ACTIVE = "dev"
$env:DB_USERNAME = "employee_app"
$env:DB_PASSWORD = "填写本机练习数据库密码"
$env:APP_RELEASE_ID = "chapter17-check"
```

启动JAR：

```powershell
java -jar target\employee-management-api-0.0.1-SNAPSHOT.jar
```

`java -jar` 让JVM读取JAR清单并启动Spring Boot入口，不使用IDE的运行配置。成功标准至少包括：

- 日志显示激活dev Profile。
- 启动配置日志显示 `releaseId=chapter17-check`。
- Tomcat监听8080。
- 使用第15章流程登录后，第16章权限测试对应的HTTP请求结果不变。

验证结束后按 `Ctrl+C` 正常停止。然后只清理本次PowerShell会话中的练习变量：

```powershell
Remove-Item Env:SPRING_PROFILES_ACTIVE
Remove-Item Env:DB_USERNAME
Remove-Item Env:DB_PASSWORD
Remove-Item Env:APP_RELEASE_ID
```

不要删除系统级环境变量，也不要把上述练习占位文字当作实际密码。

## 十三、使用外部配置模拟交付运行

本练习只使用本机练习数据库，不连接共享测试库或真实生产库。先创建独立交付目录：

```powershell
New-Item -ItemType Directory -Force delivery\config
Copy-Item `
    target\employee-management-api-0.0.1-SNAPSHOT.jar `
    delivery\
Copy-Item `
    deploy\application-prod.yml.example `
    delivery\config\application-prod.yml
```

Spring Boot默认检查运行目录及其 `config` 子目录。切换到交付目录后，外部 `config/application-prod.yml` 会覆盖JAR内同名Profile配置中的相同键。

| PowerShell命令 | 当前参数 | 可接受的值 | 结果 |
| --- | --- | --- | --- |
| `New-Item -ItemType Directory` | `delivery\config` | 当前练习项目内明确的目录路径 | 创建外部配置目录；已存在时由`-Force`继续使用 |
| `Copy-Item` | 源文件和目标目录 | 存在的文件及有写权限的目标 | 复制JAR或配置模板，不修改源文件 |
| `Set-Location` | `delivery`或`..` | 存在且可进入的目录 | 改变后续命令的当前工作目录 |

在当前PowerShell设置模拟值。数据库URL仍指向个人练习库：

```powershell
$env:SPRING_PROFILES_ACTIVE = "prod"
$env:DB_URL = "jdbc:mysql://localhost:3306/employee_db?useUnicode=true&characterEncoding=UTF-8&connectionTimeZone=Asia/Tokyo"
$env:DB_USERNAME = "employee_app"
$env:DB_PASSWORD = "填写本机练习数据库密码"
$env:APP_LOG_FILE = "logs/employee-api.log"
$env:APP_ENVIRONMENT_NAME = "local-prod-simulation"
$env:APP_RELEASE_ID = "chapter17-delivery-check"
$env:SERVER_PORT = "8081"
Set-Location delivery
java -jar .\employee-management-api-0.0.1-SNAPSHOT.jar
```

看到8081监听和正确环境名后完成接口验证，按 `Ctrl+C` 停止，再回到项目根目录：

仍在 `delivery` 目录时，也可以用命令行临时覆盖非敏感配置：

```powershell
java -jar .\employee-management-api-0.0.1-SNAPSHOT.jar `
    --spring.profiles.active=prod `
    --server.port=8082
```

验证完8082后再次按 `Ctrl+C` 停止，然后回到项目根目录并清理本次模拟变量：

```powershell
Set-Location ..
Remove-Item Env:SPRING_PROFILES_ACTIVE
Remove-Item Env:DB_URL
Remove-Item Env:DB_USERNAME
Remove-Item Env:DB_PASSWORD
Remove-Item Env:APP_LOG_FILE
Remove-Item Env:APP_ENVIRONMENT_NAME
Remove-Item Env:APP_RELEASE_ID
Remove-Item Env:SERVER_PORT
```

`Set-Location` 改变当前目录，外部配置的搜索位置和相对日志路径也会随之改变。交付测试结束后删除 `delivery` 练习目录前，先确认其中没有需要保留的证据或人工配置；真实发布目录不能用本练习清理方式处理。

`Remove-Item Env:变量名` 只删除当前PowerShell进程中的对应环境变量，不删除配置文件，也不会修改其他已经启动进程取得的值。配置变化必须通过停止并重新启动应用生效。

命令行参数适合一次性、非敏感覆盖。密码等秘密不要写在命令行。

## 十四、IDE能运行但JAR失败怎样排查

按构建、配置、运行三个阶段定位：

| 现象 | 检查位置 | 常见原因 | 修正 |
| --- | --- | --- | --- |
| `Unable to access jarfile` | 当前目录和文件名 | 路径错误或JAR未复制 | 用Get-Item确认实际路径 |
| `no main manifest attribute` | JAR结构和插件 | 运行了未repackage的普通JAR | 检查Maven Plugin和package结果 |
| 提示没有激活Profile | 启动日志 | IDE保存了Profile，终端没有 | 在终端明确设置或传参 |
| 无法解析 `DB_PASSWORD` | 运行进程环境 | 变量只配置在IDE或另一个终端 | 在同一PowerShell设置后重启 |
| 配置校验失败 | DeploymentProperties字段 | 环境名或发布编号缺失/空白 | 补齐受控配置，不删除校验 |
| 端口被占用 | 实际最终端口 | 环境变量或外部文件覆盖了预期 | 查四类配置来源和监听进程 |
| 日志写到意外目录 | 当前工作目录 | 使用了相对日志路径 | 明确运行目录或提供绝对受控路径 |
| 数据库认证失败 | URL、账号和权限 | 指向错误环境或凭据无效 | 停止重试，核对配置来源和账号范围 |
| 运行的是旧功能 | JAR时间、版本和摘要 | 复制了上次构建产物 | clean package并重新计算SHA-256 |

不要通过关闭测试、删除配置校验、硬编码密码或切回 `permitAll()` 来让JAR“先启动”。这些做法会掩盖真正的交付问题。

## 十五、交付记录应包含什么

一次可复查的交付至少记录：

```text
项目：employee-management-api
产物：employee-management-api-0.0.1-SNAPSHOT.jar
Java：17
Spring Boot：3.5.16
支持Profile：dev、prod
发布编号：chapter17-delivery-check
SHA-256：填写本次Get-FileHash结果
测试：mvnw clean package成功
配置项：DB_URL、DB_USERNAME、DB_PASSWORD、APP_LOG_FILE、
         APP_ENVIRONMENT_NAME、APP_RELEASE_ID、SERVER_PORT
启动验证：Profile、端口、登录、权限接口结果
已知限制：本章仅完成本机脱离IDE验证，尚未部署为系统服务
```

发布记录中的摘要、构建时间和测试结果必须来自同一次产物。配置清单记录键名、用途、是否必填和提供方，不记录秘密值。

## 十六、规格理解、影响调查、Review与练习

### 练习1：完成dev打包和JAR验证

从干净target执行 `mvnw clean package`，保存测试结果、JAR文件信息、归档结构、SHA-256和dev启动日志。验收时必须能说明为什么它不依赖IDE。

### 练习2：调查新增外部API地址

规格要求prod调用一个外部人事系统，dev调用本机模拟服务。先提交影响调查：配置键名、数据类型、是否必填、默认值风险、超时配置、两个Profile的取值来源、结构化配置类、不得记录的内容和所需测试。不要只新增一个URL字符串。

### 练习3：Review错误的生产配置

指出下面配置的问题并给出修正：

```yaml
spring:
  profiles:
    active: prod
  datasource:
    url: jdbc:mysql://prod-db.example:3306/employee
    username: root
    password: real-password
logging:
  level:
    root: DEBUG
```

Review至少覆盖：JAR内固定激活prod、内部地址与真实凭据入库、高权限账号、长期DEBUG日志、没有发布编号和缺失配置不能及时失败。

### 练习4：制作配置项清单

为第17章全部环境变量建立表格，至少包含配置键、对应环境变量、dev/prod是否必填、是否敏感、提供方和验证方式。密码列只能写“已提供/未提供”，不能写值。

### 练习5：排查一次陈旧JAR

记录源码改修时间、构建时间、JAR修改时间和SHA-256，判断服务器样本是否为最新产物。提交结论、证据和重新交付步骤，不在服务器直接修改class或源码。

## 十七、本章稳定状态

完成后，同一套员工管理代码可以通过dev、test和prod Profile取得不同配置；项目自己的发布信息会被结构化绑定并在启动时校验；完整测试通过后可生成Spring Boot可执行JAR；学员能够用环境变量、外部配置和非敏感启动参数脱离IDE运行，并交付可核对的版本、配置清单和摘要。

下一章将在这个JAR和配置清单基础上完成Linux部署、启停、日志、健康检查和回滚，不在本章把一次前台启动当作正式部署完成。
