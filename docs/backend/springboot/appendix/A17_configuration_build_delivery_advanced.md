# A17 配置、构建与交付进阶

本附录承接[第17章](../mainline/17_profiles_configuration_packaging.md)中不属于新人主线的配置与交付内容。只有项目手顺要求时再使用，不把示例直接当成生产标准。

## 一、配置来源与覆盖调查

Spring Boot可以从JAR内配置、Profile文件、环境变量、命令行参数和外部配置文件读取值。后提供的高优先级来源可能覆盖低优先级来源，但完整顺序会受启动方式与版本影响。调查时不要背一张简化表下结论，应记录：

1. 当前激活的Profile；
2. JAR内有哪些配置；
3. 运行目录及 `config/` 是否有外部配置；
4. 服务管理器提供了哪些环境变量；
5. 启动命令是否带参数；
6. 最终生效值来自哪里。

命令行覆盖示例仅用于个人练习：

```bash
java -jar employee-api.jar --server.port=8081 --spring.profiles.active=prod
```

命令历史可能被保存，不要在参数中写密码。

## 二、配置对象的扩展

相关配置可以用嵌套 `@ConfigurationProperties` 集中管理，并用Bean Validation在启动时检查。`@ConfigurationProperties` 属于Spring Boot，写在配置属性类上；应用上下文启动时，配置绑定器按注解的prefix把一组外部配置转换并写入类型安全对象。它负责成组绑定，不会自行隐藏秘密，也需要通过配置属性扫描或显式启用方式注册。配置校验失败属于启动失败，应让日志指出属性名和约束，但不能打印秘密值。

启动确认日志可以记录版本、Profile和非敏感功能开关：

```java
log.info("application_started releaseName={} activeProfiles={}",
        properties.releaseName(), environment.getActiveProfiles());
```

不要记录数据库密码、Session ID、CSRF令牌或完整连接串。`ApplicationRunner` 可在容器就绪后执行一次确认逻辑，但不应用它承载长期业务任务。

## 三、构建产物检查

正式交付通常还会核对：

- `artifactId`、版本和文件名；
- 自动化测试结果；
- JAR是否可由目标Java版本运行；
- 构建来源和提交编号；
- 文件大小与摘要；
- 外部配置项清单；
- 已知限制和恢复入口。

摘要用于确认传输前后文件是否一致，不证明代码安全。常见示例：

```powershell
Get-FileHash .\target\employee-management-api-0.0.1-SNAPSHOT.jar -Algorithm SHA256
```

```bash
sha256sum employee-management-api-0.0.1-SNAPSHOT.jar
```

Windows PowerShell与Linux Bash命令不能混用。摘要应与同一发布版本的受控记录比较。

## 四、外部配置模板

仓库可保存不含秘密的模板：

```yaml
server:
  port: ${SERVER_PORT:8080}
spring:
  datasource:
    url: ${DB_URL}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
```

模板说明必需键，不提供真实值。实际秘密由组织批准的方式注入，访问权限、更新和轮换也要受控。

## 五、交付调查清单

| 检查项 | 要回答的问题 |
| --- | --- |
| 构建 | 使用哪个Java、Maven和源码版本 |
| 测试 | 哪些测试执行并通过 |
| 产物 | JAR名称、版本、大小与摘要 |
| 配置 | Profile、必需变量、默认值与秘密来源 |
| 兼容 | 数据库结构与旧版本是否兼容 |
| 启停 | 谁有权限、怎样启动与正常停止 |
| 验证 | 健康、登录、权限和数据库怎样确认 |
| 恢复 | 失败条件、旧版本与数据恢复方式 |

复杂发布编号、制品仓库、签名和流水线由实际项目规定，本课程不建立第二套标准。
