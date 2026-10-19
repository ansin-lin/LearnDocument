# 第17章 多环境配置与打包

> 本章目标：理解开发、测试和生产环境为什么需要不同配置，掌握Profile与环境变量的基础用法，了解Maven构建过程，并把项目打包成可以脱离Eclipse运行的JAR。

从第16章稳定工程继续。本章不改变员工API、Session登录和权限规则。生产交付的深入做法见附录 [A17 配置、构建与交付进阶](../appendix/A17_configuration_build_delivery_advanced.md)。

## 一、为什么不同环境需要不同配置

本章处理的是“同一份代码怎样在不同环境运行并交付”。先看各知识点的关系，再逐项配置和实际打包：

```text
同一份源代码与pom.xml
    ↓ Maven执行测试、打包
同一份可执行JAR
    ↓ 启动时选择Profile，并从运行环境读取配置与秘密
开发／测试／生产中的应用进程
    ↓ 用启动日志和受保护接口确认实际运行状态
```

- **配置与Profile**：`application.yml` 放共同设置，`application-dev.yml` 等文件放环境差异。Profile决定加载哪组环境配置；它不代表当前登录用户的角色。
- **环境变量与敏感信息**：数据库地址、账号和密码是运行条件，不应写进Java业务代码；密码由外部环境提供，不提交到Git。配置文件可用 `${变量名}` 读取它们。
- **配置绑定**：单个配置值可用 `@Value`，同一组相关值可用 `@ConfigurationProperties` 绑定到对象；它们读取运行配置，不负责业务逻辑或数据库记录。
- **构建与运行产物**：Maven根据 `pom.xml` 管理依赖和构建流程，测试通过后生成可执行JAR。第17章先在本机脱离Eclipse运行，下一章再讨论把JAR放到服务器后怎样持续运行。

本章检查的不是“文件存在即可”：要确认实际启用的Profile、必需变量、测试结果、JAR路径，以及启动后健康接口和受保护员工接口的响应。

| 环境 | 典型用途 | 数据库与日志特点 |
| --- | --- | --- |
| dev | 开发者本机调试 | 本机MySQL，日志较详细 |
| test | 自动化测试 | 专用MySQL测试库，数据可重复准备并有库名保护 |
| prod | 正式运行 | 受控数据库，秘密由运行环境提供 |

如果把数据库地址、密码和端口写死在Java代码中，每次换环境都要改代码。Spring Boot把差异放入配置，使同一个JAR可以在启动时选择环境。

```yaml
server:
  port: 8080
```

配置描述运行条件，业务代码继续负责业务规则。

## 二、application.yml与Profile

Spring Boot默认读取 `src/main/resources/application.yml`。Profile表示一组运行环境配置：

```text
application.yml       公共配置
application-dev.yml   开发环境差异
application-test.yml  自动化测试差异
application-prod.yml  生产环境差异
```

### 1. 公共配置

`application.yml`：

```yaml
spring:
  application:
    name: employee-management-api
mybatis:
  mapper-locations: classpath:mapper/*.xml
  type-aliases-package: com.example.employee.entity
  configuration:
    map-underscore-to-camel-case: true
logging:
  level:
    root: INFO
```

这里不固定Profile，也不保存数据库密码。

### 2. 环境专用配置

`application-dev.yml`：

```yaml
server:
  port: 8080
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/employee_db
    username: ${DB_USERNAME:employee_app}
    password: ${DB_PASSWORD}
    driver-class-name: com.mysql.cj.jdbc.Driver
logging:
  level:
    com.example.employee: DEBUG
```

测试继续使用第12章的 `src/test/resources/application-test.yml`：

```yaml
spring:
  datasource:
    url: ${TEST_DB_URL:jdbc:mysql://localhost:3306/employee_test_db?useUnicode=true&characterEncoding=UTF-8&connectionTimeZone=Asia/Tokyo}
    username: ${DB_USERNAME:employee_app}
    password: ${DB_PASSWORD}
    driver-class-name: com.mysql.cj.jdbc.Driver
logging:
  level:
    root: WARN
```

测试配置仍与第12章一致：数据库名必须以 `_test` 或 `_test_db` 结尾，账号和密码沿用第9章的本地环境变量，执行建表脚本的测试继续继承 `MySqlTestGuard`。H2只用于A12-2的独立比较实验，不属于test主线配置。

`application-prod.yml`只写结构和外部值占位符：

```yaml
server:
  port: ${SERVER_PORT:8080}
spring:
  datasource:
    url: ${DB_URL}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
    driver-class-name: com.mysql.cj.jdbc.Driver
logging:
  level:
    root: INFO
```

生产数据库值没有默认值，缺少时应启动失败，避免连接错误数据库。

### 3. 激活Profile

Eclipse可在 **Run Configurations → Spring Boot App → Profile** 选择 `dev`，也可设置：

```text
SPRING_PROFILES_ACTIVE=dev
```

运行JAR时可以指定：

```bash
java -jar target/employee-management-api-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev
```

Profile只选择运行配置，不是账号权限，也不是秘密存储机制。

## 三、环境变量和敏感信息

```yaml
username: ${DB_USERNAME:employee_app}
password: ${DB_PASSWORD}
```

- `${DB_USERNAME:employee_app}`：优先读取 `DB_USERNAME`，没有时使用 `employee_app`。
- `${DB_PASSWORD}`：必须读取 `DB_PASSWORD`，没有默认值。

Eclipse可在 **Run Configurations → Environment** 添加练习变量。命令行写法因Shell不同而不同：

```powershell
# Windows PowerShell，仅对当前窗口及其子进程有效
$env:DB_USERNAME = "employee_app"
$env:DB_PASSWORD = "本机练习密码"
```

```bash
# Linux Bash，仅对当前Shell及其子进程有效
export DB_USERNAME='employee_app'
export DB_PASSWORD='本机练习密码'
```

不要把真实密码写进截图、源码、教学文档或Git。生产环境通常由受控部署工具或秘密管理机制提供。

## 四、自定义配置读取基础

单个值可以用 `@Value`：

```java
@Value("${app.release-name:local}")
private String releaseName;
```

多个相关值更适合绑定为对象：

```yaml
app:
  release-name: ${APP_RELEASE_NAME:local}
  support-contact: ${APP_SUPPORT_CONTACT:training@example.invalid}
```

```java
package com.example.employee.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app")
public record DeploymentProperties(
        @NotBlank String releaseName,
        @NotBlank String supportContact) {
}
```

启动类增加 `@ConfigurationPropertiesScan` 后，Spring会把 `app.*` 绑定到对象。`@Validated` 在启动时检查约束。配置对象描述运行参数，不是数据库Entity。复杂绑定与覆盖实验见A17附录。

## 五、Maven构建基础

Maven读取项目根目录的 `pom.xml`，解析依赖并按生命周期执行任务。

| 名称 | 作用 |
| --- | --- |
| dependency | 编译或运行需要的库 |
| plugin | 构建期间工作的工具 |
| compile | 编译主代码 |
| test | 编译并运行测试 |
| package | 生成JAR，通常先执行compile和test |
| clean | 删除上一次构建生成的 `target` |

Spring Boot Maven Plugin把应用及运行依赖组织成可执行JAR，使 `java -jar` 能找到启动类和所需库。

## 六、生成可执行JAR

先停止Eclipse中的应用。在包含 `pom.xml` 和Maven Wrapper的项目根目录执行：

```powershell
# Windows PowerShell
.\mvnw.cmd clean package
```

```bash
# Linux或macOS Bash
./mvnw clean package
```

没有Wrapper但已安装Maven时可运行 `mvn clean package`。Maven会编译并执行测试；测试失败通常会阻止打包，应修复原因，不默认跳过测试。

成功时看到 `BUILD SUCCESS`，并在 `target` 找到类似：

```text
target/employee-management-api-0.0.1-SNAPSHOT.jar
```

实际文件名以 `pom.xml` 的 `artifactId` 和 `version` 为准。

## 七、脱离Eclipse运行JAR

### 1. 启动

```powershell
java -version
$env:DB_USERNAME = "employee_app"
$env:DB_PASSWORD = "本机练习密码"
java -jar target/employee-management-api-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev
```

命令在项目根目录执行。启动日志应显示 `dev` Profile、应用端口和启动完成信息，不应打印数据库密码。

### 2. 用Postman验证

第15、16章已启用Spring Security，不能匿名访问员工接口并期待200。先验证公开健康接口：

| 项目 | 内容 |
| --- | --- |
| HTTP方法 | GET |
| URL | `http://localhost:8080/health` |
| 请求参数、请求体 | 无 |
| 预期状态码 | 200 |
| 预期响应 | `OK` |

再按第15章执行 `GET /auth/csrf`、`POST /auth/login`，保留Cookie后访问 `GET /employees/1001`。tanaka读取本人应按第16章规格返回200。

### 3. 正常停止

回到运行JAR的终端按 `Ctrl+C`，等待停止日志和命令提示符返回。

## 八、常见启动问题

| 现象 | 常见原因 | 检查方法 |
| --- | --- | --- |
| 找不到JAR | 文件名或目录错误 | 查看 `target` 实际文件名 |
| UnsupportedClassVersionError | Java版本过低 | 比较 `java -version` 与项目版本 |
| 端口占用 | Eclipse旧进程仍运行 | 停止旧应用 |
| 缺少DB_PASSWORD | 必填环境变量未提供 | 检查Run Configuration或终端 |
| 数据库连接失败 | 地址、账号、密码或MySQL状态错误 | 查看启动日志 |
| 员工接口401 | 尚未登录 | 按第15章建立Session |
| 员工列表403 | USER没有列表权限 | 按第16章核对身份 |

## 九、练习与完成标准

1. 说明dev、test、prod使用不同数据库的原因，并指出公共配置位置。
2. 临时移除 `DB_PASSWORD`，记录启动失败，再恢复变量。
3. 执行包含测试的打包，保存 `BUILD SUCCESS` 和JAR路径。
4. 脱离Eclipse启动JAR，完成健康检查、登录、本人详情查询和正常停止。
5. Review把生产密码写入 `application-prod.yml` 的改动并改为占位符。

完成后，你应能解释Profile、环境变量、Maven、`pom.xml`、dependency、plugin和可执行JAR，并完成“测试→打包→启动→验证→停止”。下一章将说明服务器运行需要准备什么、怎样检查以及如何调查故障。
