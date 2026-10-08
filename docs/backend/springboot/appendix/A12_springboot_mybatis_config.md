# A12 Spring Boot怎样读取独立MyBatis配置

第9章主线直接在 `application.yml` 的 `mybatis.configuration` 下填写MyBatis核心设置。既存项目也可能使用独立的 `mybatis-config.xml`。本附录通过一次可恢复实验，说明Spring Boot怎样找到该文件，以及它与Mapper XML为什么不是同一种配置。

开始前应完成第9章，确保主线的 `GET /employees/{id}` 可以查询数据库。实验结束后必须恢复纯YAML主线配置。

## 一、两类XML解决不同问题

```text
mybatis/mybatis-config.xml
  → MyBatis全局设置，例如超时、自动映射和缓存策略

mapper/EmployeeMapper.xml
  → 某个Mapper的SQL、参数和结果映射
```

Spring Boot不会因为文件名是 `mybatis-config.xml` 就自动读取它。必须在 `application.yml` 中通过 `mybatis.config-location` 明确指定资源位置。

## 二、创建独立核心配置文件

新建：

```text
src/main/resources/mybatis/mybatis-config.xml
```

完整内容：

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE configuration
        PUBLIC "-//mybatis.org//DTD Config 3.0//EN"
        "https://mybatis.org/dtd/mybatis-3-config.dtd">
<configuration>
    <settings>
        <setting name="mapUnderscoreToCamelCase" value="true"/>
        <setting name="defaultStatementTimeout" value="10"/>
    </settings>
</configuration>
```

`<settings>` 保存MyBatis核心运行设置。这里的驼峰名称对应第9章YAML中的短横线名称：

| XML设置 | YAML配置 | 当前值 |
| --- | --- | --- |
| `mapUnderscoreToCamelCase` | `mybatis.configuration.map-underscore-to-camel-case` | `true` |
| `defaultStatementTimeout` | `mybatis.configuration.default-statement-timeout` | `10`秒 |

## 三、让Starter读取该文件

把 `application.yml` 中原来的整个 `mybatis` 区块替换为：

```yaml
mybatis:
  config-location: classpath:mybatis/mybatis-config.xml
  check-config-location: true
  mapper-locations: classpath:mapper/*.xml
  type-aliases-package: com.example.employee.entity
```

| 配置项 | 可接受的值 | 默认值或必填性 | 当前作用 |
| --- | --- | --- | --- |
| `config-location` | 一个可读取的Spring资源路径 | 默认不指定 | 指向MyBatis核心XML配置文件 |
| `check-config-location` | `true`、`false` | 默认 `false`，实验设置 `true` | 启动时确认核心配置文件存在 |
| `mapper-locations` | 一个或多个Mapper XML资源模式 | 本实验继续明确设置 | 加载保存SQL的 `EmployeeMapper.xml` |
| `type-aliases-package` | 一个或多个Java包名 | 本实验继续明确设置 | 扫描 `Employee` 等类型别名 |

Starter先读取 `application.yml`，把 `config-location` 指向的核心XML交给 `SqlSessionFactory`，同时按照 `mapper-locations` 加载SQL映射文件。`@Mapper` 接口由Mapper扫描注册成Spring Bean，三部分最终在同一个 `SqlSessionFactory` 中建立联系。

## 四、两种核心设置来源不能叠加

```text
方式A：mybatis.configuration.*
  → 直接在application.yml中配置核心设置

方式B：mybatis.config-location
  → 从独立mybatis-config.xml读取核心设置
```

两种方式不能同时使用。切换到独立XML时，必须删除整个 `configuration:` 区块；否则Starter会在创建 `SqlSessionFactory` 时启动失败。`mapper-locations` 和 `type-aliases-package` 属于Starter集成配置，可以继续保留。

官方属性表也明确说明 `configuration.*` 不能与 `config-location` 同时使用，参见[MyBatis Spring Boot Starter配置说明](https://mybatis.org/spring-boot-starter/mybatis-spring-boot-autoconfigure/)。

## 五、为什么不复制普通MyBatis的environments

普通Java项目的 `mybatis-config.xml` 可能包含：

```xml
<environments>
    <!-- dataSource和transactionManager -->
</environments>
```

当前Spring Boot项目已经通过 `spring.datasource` 创建 `DataSource`，事务基础设施也由Spring集成。独立MyBatis配置只保存MyBatis自身设置，不再复制账号、连接池和事务管理器，否则会形成两套数据源配置。

“由Spring集成”只表示Mapper能够加入已经开启的Spring事务，不表示每个Service方法会自动成为事务。没有 `@Transactional` 时，事务外的Mapper调用仍分别提交；第10章说明当前限制，第13章建立正式事务边界。

MyBatis基础课程的[MyBatis配置文件](../../mybatis/03_mybatis_config_file.md)展示普通Java项目怎样独立准备配置；本附录说明接入Spring Boot Starter后怎样由Spring装配。

## 六、启动、制造故障并恢复

先在已经设置数据库环境变量的PowerShell中执行：

```powershell
.\mvnw.cmd clean test
.\mvnw.cmd spring-boot:run
```

确认 `GET /employees/{id}` 能返回数据库记录。随后依次完成两个故障实验，每次只改变一个条件：

1. 临时写错 `config-location`，记录启动失败发生在哪个阶段，然后恢复路径；
2. 临时同时保留 `config-location` 和 `configuration`，记录冲突信息，然后删除其中一套。

最后恢复第9章主线：

1. 删除 `src/main/resources/mybatis/mybatis-config.xml`；
2. 删除已经变空的 `src/main/resources/mybatis` 目录；
3. 把 `application.yml` 的 `mybatis` 区块恢复为第9章的 `configuration:` 写法；
4. 重新构建、启动并确认 `GET /employees/{id}` 返回200。

保存配置差异、启动结果、错误根因和恢复后的请求结果，不能只写“测试成功”。完成后主线工程中不应保留本附录的实验文件。
