# 第14章 多环境配置与打包

> 本章目标：理解开发环境和生产环境配置的区别，能打包 Spring Boot 项目并指定配置运行。

## 一、为什么需要多环境配置

开发环境和生产环境通常不同：

- 数据库地址不同
- 日志级别不同
- 端口不同
- 外部接口地址不同
- 密码和密钥来源不同

所以项目需要分环境配置。

## 二、配置文件

```text
src/main/resources/application.yml
src/main/resources/application-dev.yml
src/main/resources/application-prod.yml
```

`application.yml`：

```yaml
spring:
  profiles:
    active: dev # 默认使用开发环境
```

`application-dev.yml`：

```yaml
server:
  port: 8080 # 开发环境端口

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/employee_db # 本地 MySQL 地址
    username: root # 本地数据库用户名
    password: password # 本地数据库密码，实际项目不要提交真实密码
```

`application-prod.yml`：

```yaml
server:
  port: 8080 # 生产环境端口

spring:
  datasource:
    url: ${DB_URL} # 从环境变量读取数据库地址
    username: ${DB_USERNAME} # 从环境变量读取数据库用户名
    password: ${DB_PASSWORD} # 从环境变量读取数据库密码
```

`${DB_URL}` 表示从操作系统环境变量读取值。

生产环境不要把真实密码直接写进 Git 仓库。

## 三、指定环境启动项目

开发环境：

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

生产环境 jar 启动：

```bash
java -jar target/employee-management-api-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

如果命令行指定了 `--spring.profiles.active=prod`，会优先使用生产配置。

## 四、打包命令

```bash
mvn clean package
```

生成文件通常位于：

```text
target/employee-management-api-0.0.1-SNAPSHOT.jar
```

`mvn clean package` 的含义：

| 命令片段 | 作用 |
| --- | --- |
| `mvn` | 执行 Maven |
| `clean` | 删除旧的编译和打包结果 |
| `package` | 编译、测试并生成 jar |

如果只想跳过测试打包，可以使用：

```bash
mvn clean package -DskipTests
```

但正式交付前不建议跳过测试。

## 五、确认 jar 是否生成

打包后检查：

```bash
dir target
```

Windows PowerShell 或 Linux/macOS 都可以使用：

```bash
ls target
```

应该能看到：

```text
employee-management-api-0.0.1-SNAPSHOT.jar
```

## 六、常见错误

| 错误 | 原因 | 修正 |
| --- | --- | --- |
| 启动后连接错数据库 | profile 没切换 | 检查 `spring.profiles.active` |
| 生产密码提交到 Git | 密码写死在配置文件 | 改为环境变量 |
| 打包失败 | 测试失败或依赖下载失败 | 先看 Maven 错误日志 |
| jar 名称找不到 | artifactId 或 version 不一致 | 检查 `pom.xml` 和 `target` 目录 |

## 七、本章练习

请完成：

1. 创建 `application-dev.yml` 和 `application-prod.yml`。
2. 使用 Maven 打包项目。
3. 使用 `prod` 配置启动 jar。
4. 说明生产环境为什么不应该提交真实数据库密码。

## 八、本章总结

- 多环境配置用于区分开发、测试、生产环境。
- `application-dev.yml` 和 `application-prod.yml` 保存不同环境配置。
- 生产环境密码应通过环境变量等方式注入。
- `mvn clean package` 用于生成可部署 jar。
