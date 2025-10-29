
# Spring Boot 配置与多环境 + 多数据源 + 打包发布（超详解）

> 适配 Spring Boot 3.x。涵盖配置优先级、Profiles（多环境）、外部化配置、`@Value` vs `@ConfigurationProperties`、**多数据源（JPA/MyBatis）** 实战、**打包与发布**（JAR、Docker、Buildpacks、Jib），并附带可运行示例与命令。

---

## 目录

1. 配置加载与优先级
2. Profiles 多环境管理（单环境、复合环境、Profile Group）
3. 外部化配置（命令行、环境变量、外部文件、`spring.config.import`）
4. 属性绑定：`@Value` 与 `@ConfigurationProperties`
5. 多数据源配置（JPA 版 / MyBatis 版 / 动态路由）
6. 打包与发布（Maven/Gradle JAR、Buildpacks、Dockerfile、Jib）
7. 一体化示例与启动指令（含本地/测试/生产）
8. 常见坑位与Checklist

---

## 1. 配置加载与优先级

Spring Boot 会按“高→低”的优先级解析属性：

1) **命令行参数**：`--server.port=9090`  
2) **JVM 系统属性**：`-Dserver.port=9091`  
3) **环境变量**：`SERVER_PORT=9092`（支持松散绑定）  
4) **外部文件**：工作目录 `./config/` 下的 `application[-{profile}].yml`  
5) **类路径文件**：`classpath:/application[-{profile}].yml`  
6) **默认值**：代码中定义的默认值

> 规则：同名属性**后解析覆盖前解析**（优先级越高越后生效）。

**松散绑定示例**：`app.sample-config.maxItems` 可由环境变量 `APP_SAMPLE_CONFIG_MAXITEMS` 覆盖。

---

## 2. Profiles 多环境管理

### 2.1 单环境（最常用）

文件结构：

```java
src/main/resources/
├── application.yml
├── application-dev.yml
├── application-test.yml
└── application-prod.yml
```

在 `application.yml` 激活：

```yaml
spring:
  profiles:
    active: dev
```

**切换方式**：

- 命令行：`--spring.profiles.active=test`
- 环境变量：`SPRING_PROFILES_ACTIVE=prod`
- IDE：运行配置添加 Program Arguments

### 2.2 复合环境（Boot 2.4+）

同一时间激活多个：

```yaml
spring:
  profiles:
    active:
      - dev
      - common
```

### 2.3 Profile Group（Boot 2.4+）

定义一个组名激活多个 Profile：

```yaml
spring:
  profiles:
    group:
      staging: [test, metrics, gray]
```

启动时只需：`--spring.profiles.active=staging`。

### 2.4 条件激活（配置文件内部细分）

使用 `spring.config.activate.on-profile`：

```yaml
# application.yml
app:
  name: demo

---
spring:
  config:
    activate:
      on-profile: dev
server:
  port: 8081

---
spring:
  config:
    activate:
      on-profile: prod
server:
  port: 8080
```

### 2.5 典型多环境拆分示例

`application.yml`（公共）：

```yaml
spring:
  application:
    name: demo-app
  profiles:
    active: dev
logging:
  level:
    root: INFO
```

`application-dev.yml`：

```yaml
server:
  port: 8081
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/demo_dev?useSSL=false&serverTimezone=UTC
    username: root
    password: root
```

`application-prod.yml`：

```yaml
server:
  port: 8080
spring:
  datasource:
    url: jdbc:mysql://db-prod:3306/demo_prod?useSSL=false&serverTimezone=UTC
    username: admin
    password: ${DB_PASS}     # 从环境变量注入
logging:
  level:
    root: WARN
```

---

## 3. 外部化配置

### 3.1 命令行 / 系统属性 / 环境变量

- 命令行：`java -jar app.jar --server.port=9000`
- 系统属性：`java -Dserver.port=9001 -jar app.jar`
- 环境变量：`export SERVER_PORT=9002 && java -jar app.jar`

### 3.2 外部文件

- 放置于工作目录 `./config/` 或 `./`：会**覆盖类路径**配置。  
- 指定路径：`--spring.config.location=/etc/app/` 或 `--spring.config.additional-location=/etc/app/`（追加，不覆盖默认搜索路径）。

### 3.3 `spring.config.import`（Boot 2.4+）

可引入额外文件或远程配置：

```yaml
spring:
  config:
    import: "optional:file:/etc/app/custom.yml"
```

### 3.4 Kubernetes / Docker 环境变量

- K8S ConfigMap/Secret → 容器环境变量 → 覆盖 Spring 配置
- Docker：`docker run -e SPRING_PROFILES_ACTIVE=prod -e DB_PASS=xxx demo:1.0`

---

## 4. 属性绑定：`@Value` vs `@ConfigurationProperties`

### 4.1 `@Value`（适合零散字段）

```java
@RestController
class InfoController {
  @Value("${spring.application.name}")
  String appName;
  @GetMapping("/info") String info() { return appName; }
}
```

### 4.2 `@ConfigurationProperties`（推荐结构化绑定）

```yaml
app:
  name: demo
  timeout: 5000
  security:
    enable: true
    level: high
```

```java
@Component
@ConfigurationProperties(prefix = "app")
@lombok.Data
class AppProps {
  String name;
  int timeout;
  Security security;
  static class Security { boolean enable; String level; }
}
```

**校验**：

```java
@Component
@Validated
@ConfigurationProperties(prefix = "app")
class AppProps { @NotBlank String name; @Min(1000) int timeout; }
```

---

## 5. 多数据源配置（三种方案）

### 5.1 方案A：JPA 多数据源（不同包、不同事务管理器）

**目标**：`primary` 连接业务库，`report` 连接报表库。

`application-dev.yml`：

```yaml
spring:
  datasource:
    primary:
      url: jdbc:mysql://localhost:3306/biz?useSSL=false&serverTimezone=UTC
      username: root
      password: root
    report:
      url: jdbc:mysql://localhost:3306/report?useSSL=false&serverTimezone=UTC
      username: root
      password: root
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
```

**配置类**：

```java
@Configuration
@EnableTransactionManagement
public class MultiJpaConfig { }

@Configuration
@EnableJpaRepositories(
  basePackages = "com.example.repo.biz",
  entityManagerFactoryRef = "bizEmf",
  transactionManagerRef = "bizTx"
)
class BizJpaConfig {
  @Bean @ConfigurationProperties("spring.datasource.primary")
  public DataSource bizDs() { return DataSourceBuilder.create().build(); }

  @Bean
  public LocalContainerEntityManagerFactoryBean bizEmf(
      @Qualifier("bizDs") DataSource ds) {
    var factory = new LocalContainerEntityManagerFactoryBean();
    factory.setDataSource(ds);
    factory.setPackagesToScan("com.example.entity.biz");
    factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
    return factory;
  }

  @Bean
  public PlatformTransactionManager bizTx(@Qualifier("bizEmf") EntityManagerFactory emf) {
    return new JpaTransactionManager(emf);
  }
}

@Configuration
@EnableJpaRepositories(
  basePackages = "com.example.repo.report",
  entityManagerFactoryRef = "reportEmf",
  transactionManagerRef = "reportTx"
)
class ReportJpaConfig {
  @Bean @ConfigurationProperties("spring.datasource.report")
  public DataSource reportDs() { return DataSourceBuilder.create().build(); }

  @Bean
  public LocalContainerEntityManagerFactoryBean reportEmf(
      @Qualifier("reportDs") DataSource ds) {
    var factory = new LocalContainerEntityManagerFactoryBean();
    factory.setDataSource(ds);
    factory.setPackagesToScan("com.example.entity.report");
    factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
    return factory;
  }

  @Bean
  public PlatformTransactionManager reportTx(@Qualifier("reportEmf") EntityManagerFactory emf) {
    return new JpaTransactionManager(emf);
  }
}
```

**使用**：

```java
@Service
@RequiredArgsConstructor
public class MixService {
  private final BizOrderRepository bizRepo;
  private final ReportSnapshotRepository reportRepo;

  @Transactional("bizTx")
  public void writeBiz(...) { bizRepo.save(...); }

  @Transactional("reportTx")
  public void writeReport(...) { reportRepo.save(...); }
}
```

---

### 5.2 方案B：MyBatis 多数据源（独立 `SqlSessionFactory` 与 `MapperScan`）

`application-dev.yml`（同上两套数据源）。

**配置**：

```java
@Configuration
public class MybatisMultiDsConfig { }

@Configuration
@MapperScan(basePackages = "com.example.mapper.biz", sqlSessionFactoryRef = "bizSqlSessionFactory")
class BizMybatisConfig {
  @Bean @ConfigurationProperties("spring.datasource.primary")
  public DataSource bizDs() { return DataSourceBuilder.create().build(); }

  @Bean
  public SqlSessionFactory bizSqlSessionFactory(@Qualifier("bizDs") DataSource ds) throws Exception {
    var factory = new SqlSessionFactoryBean();
    factory.setDataSource(ds);
    return factory.getObject();
  }
}

@Configuration
@MapperScan(basePackages = "com.example.mapper.report", sqlSessionFactoryRef = "reportSqlSessionFactory")
class ReportMybatisConfig {
  @Bean @ConfigurationProperties("spring.datasource.report")
  public DataSource reportDs() { return DataSourceBuilder.create().build(); }

  @Bean
  public SqlSessionFactory reportSqlSessionFactory(@Qualifier("reportDs") DataSource ds) throws Exception {
    var factory = new SqlSessionFactoryBean();
    factory.setDataSource(ds);
    return factory.getObject();
  }
}
```

**使用**：Mapper 各自放在对应包路径下，自动路由到相应数据源。

---

### 5.3 方案C：动态数据源（读写分离 / 按租户路由）

借助 `AbstractRoutingDataSource`：

```java
public class RoutingDs extends AbstractRoutingDataSource {
  @Override protected Object determineCurrentLookupKey() {
    return DsContext.get(); // 从 ThreadLocal/上下文获取标识：master / slave / tenantId
  }
}
```

**配置**：

```java
@Bean
public DataSource dataSource(
    @Qualifier("masterDs") DataSource master,
    @Qualifier("slaveDs") DataSource slave) {
  Map<Object, Object> map = new HashMap<>();
  map.put("master", master); map.put("slave", slave);
  RoutingDs routing = new RoutingDs();
  routing.setDefaultTargetDataSource(master);
  routing.setTargetDataSources(map);
  return routing;
}
```

**使用方式**：

- 在 AOP 切面根据方法名模式（如 `get/find/list`）切到 `slave`；写操作切 `master`；
- 或在租户上下文中设置 `tenantId`，实现多租户路由。

> 提示：结合连接池（HikariCP）与健康检测，避免单点失效。

---

## 6. 打包与发布

### 6.1 Maven 打包可执行 JAR（内嵌 Tomcat）

`pom.xml`：

```xml
<build>
  <plugins>
    <plugin>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-maven-plugin</artifactId>
      <executions>
        <execution>
          <goals><goal>repackage</goal></goals>
        </execution>
      </executions>
      <configuration>
        <layers><enabled>true</enabled></layers> <!-- 开启分层打包 -->
      </configuration>
    </plugin>
  </plugins>
</build>
```

打包：`mvn clean package -DskipTests`  
运行：`java -jar target/demo-1.0.0.jar --spring.profiles.active=prod`

### 6.2 Gradle 打包

`build.gradle`：

```gradle
plugins { id 'org.springframework.boot' version '3.3.2' }
tasks.named('test') { useJUnitPlatform() }
```

`./gradlew bootJar` 生成可执行 JAR。

### 6.3 Buildpacks（零 Dockerfile 构建镜像）

```bash
mvn spring-boot:build-image -Dspring-boot.build-image.imageName=demo:1.0
# 或 Gradle
./gradlew bootBuildImage --imageName=demo:1.0
```

> 使用 Paketo Buildpacks，自动选择 JDK 运行时层。

### 6.4 Dockerfile（多阶段构建）

`Dockerfile`：

```dockerfile
# 1) 构建阶段
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q -e -DskipTests dependency:go-offline
COPY src ./src
RUN mvn -q -DskipTests clean package

# 2) 运行阶段（小体积基础镜像）
FROM eclipse-temurin:21-jre
WORKDIR /opt/app
COPY --from=build /app/target/*.jar app.jar
ENV JAVA_OPTS="-XX:+UseContainerSupport -Xms256m -Xmx512m"
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar app.jar"]
```

构建：`docker build -t demo:1.0 .`  
运行：`docker run -p 8080:8080 -e SPRING_PROFILES_ACTIVE=prod demo:1.0`

### 6.5 Jib（无需 Docker 守护进程）

**Maven**：

```xml
<plugin>
  <groupId>com.google.cloud.tools</groupId>
  <artifactId>jib-maven-plugin</artifactId>
  <version>3.4.2</version>
  <configuration>
    <to><image>registry.example.com/demo:1.0</image></to>
  </configuration>
</plugin>
```

`mvn compile jib:build` 直接推送镜像到仓库。

---

## 7. 一体化示例与启动指令

### 7.1 `application.yml`（公共 + 激活 dev）

```yaml
spring:
  application:
    name: demo-app
  profiles:
    active: dev

app:
  sample-config:
    maxItems: 50
```

### 7.2 `application-dev.yml`（单库 + 控制台日志）

```yaml
server:
  port: 8081
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/demo_dev?serverTimezone=UTC
    username: root
    password: root
  jpa:
    show-sql: true
logging:
  level:
    root: INFO
```

### 7.3 `application-prod.yml`（多数据源 + 外部秘钥）

```yaml
server:
  port: 8080

spring:
  datasource:
    primary:
      url: jdbc:mysql://db-prod:3306/biz?serverTimezone=UTC
      username: admin
      password: ${BIZ_DB_PASS}
    report:
      url: jdbc:mysql://db-prod:3306/report?serverTimezone=UTC
      username: admin
      password: ${REPORT_DB_PASS}

  jpa:
    hibernate:
      ddl-auto: validate
    properties:
      hibernate.jdbc.batch_size: 50
      hibernate.order_inserts: true
      hibernate.order_updates: true

logging:
  level:
    root: WARN
```

### 7.4 启动命令全集

- 本地开发：`mvn spring-boot:run`  
- 切换环境：`java -jar app.jar --spring.profiles.active=test`  
- 指定外部配置：`java -jar app.jar --spring.config.additional-location=/etc/app/`  
- Docker：`docker run -e SPRING_PROFILES_ACTIVE=prod -e BIZ_DB_PASS=xxx -e REPORT_DB_PASS=yyy -p 8080:8080 demo:1.0`  
- K8S（片段）：

```yaml
env:
  - name: SPRING_PROFILES_ACTIVE
    value: prod
  - name: BIZ_DB_PASS
    valueFrom:
      secretKeyRef:
        name: db-secrets
        key: biz-pass
```

---

## 8. 常见坑位与 Checklist

**配置与环境**

- [ ] 同时存在 `.yml` 与 `.properties` 时，以**相同 key 的优先级覆盖**为准；避免重复定义。  
- [ ] 跨环境敏感信息使用 `${ENV_VAR:default}` 从环境变量注入。  
- [ ] Boot 2.4+ 使用 `spring.config.import` 与 Profile Group，避免老式 `spring.profiles.include` 混乱。

**多数据源**

- [ ] JPA 多数据源需**独立 `EntityManagerFactory` 与 `TransactionManager`**，并保证包扫描不交叉。  
- [ ] MyBatis 多数据源需**独立 `SqlSessionFactory`** 和 `@MapperScan`。  
- [ ] 动态路由时管理好线程上下文（使用 try/finally 清理）。

**打包与发布**

- [ ] 生产镜像建议：Buildpacks 或 Jib（更小、更快、更可重复）。  
- [ ] JVM 参数容器化优化：启用容器感知，设定合理 `-Xms/-Xmx`。  
- [ ] 开启分层 JAR 打包提升 Docker 缓存命中。

**调试与诊断**

- [ ] 开启 Actuator `/actuator/health`、`/actuator/env`（注意安全）。  
- [ ] 使用 `--debug` 查看自动配置报告。  
- [ ] 日志按包分级（`logging.level.com.example=DEBUG`）。

---

> 以上内容可直接用于团队内训或项目模板。你也可以把**多数据源**片段替换为**读写分离**或**多租户**方案，原理相同。
