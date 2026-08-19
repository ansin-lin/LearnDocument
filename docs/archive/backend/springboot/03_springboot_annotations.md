# Spring Boot 基础与核心注解

> 本章讲解 Spring Boot 启动阶段与核心配置注解。每个注解配合简明解释与实战示例，帮助理解自动装配与 IoC 机制。

---

## @SpringBootApplication

**作用：**  
Spring Boot 应用的入口注解，是一个复合注解，整合了：

- `@Configuration`
- `@EnableAutoConfiguration`
- `@ComponentScan`

它表示当前类是整个应用的 **启动配置类**，负责自动配置与组件扫描。

**定义位置：**  
通常在应用主类上（包含 `main()` 方法）。

```java
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class DemoApplication {
    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }
}
```

**详细说明：**

1. `@Configuration` 使类成为配置类；
2. `@EnableAutoConfiguration` 根据 classpath 依赖自动配置组件；
3. `@ComponentScan` 扫描该包及子包下的所有 `@Component`、`@Service`、`@Controller`、`@Repository`。

---

### @Configuration

**作用：**  
声明该类为配置类，可在其中定义多个 `@Bean` 方法，Spring 会将其注册到容器中。

**定义位置：**  
任意 Java 类上，一般用于管理 `@Bean`

```java
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.fasterxml.jackson.databind.ObjectMapper;

@Configuration
public class AppConfig {
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}
```

**补充说明：**

- 默认使用 CGLIB 代理来保证 `@Bean` 方法间的单例性。  
- 若指定 `@Configuration(proxyBeanMethods = false)`，则禁用代理以提升性能（但跨方法调用时会产生新对象）。

---

### @EnableAutoConfiguration

**作用：**  
开启 Spring Boot 的自动装配机制。  
根据 `classpath` 下存在的依赖（如 `spring-boot-starter-web`）自动配置相关 Bean。

**定义位置：**  
配置类上。通常由 `@SpringBootApplication` 隐式启用。

```java
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableAutoConfiguration
public class AutoConfigEnabled {}
```

**工作原理：**

1. 从 `META-INF/spring.factories`（Spring Boot 2.x）或 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`（Boot 3.x）读取配置；
2. 扫描并加载标注了 `@Configuration` 的自动配置类；
3. 使用 `@ConditionalOn*` 条件判断是否需要注册到容器。

---

### @ComponentScan

**作用：**  
指定扫描哪些包中的组件（如 `@Component`、`@Service`、`@Repository` 等），并将其注册为 Bean。

**定义位置：**  
配置类上。

```java
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(basePackages = "com.example.demo")
public class ScanConfig {}
```

**补充说明：**

- 默认扫描当前类所在包及其子包。  
- 可通过 `excludeFilters` 和 `includeFilters` 精确控制扫描范围。

---

## @Import

**作用：**  
将指定的类、配置类或选择器的 Bean 导入到 Spring 容器中。

**定义位置：**
配置类上。

```java
@Configuration
@Import(AppConfig.class)
public class RootConfig {}
```

**常见用法：**

- 导入配置类；
- 搭配 `ImportSelector` 实现动态加载；
- 搭配 `ImportBeanDefinitionRegistrar` 手动注册 Bean。

---

## @PropertySource / @PropertySources

**作用：**  
显式加载外部 `.properties` 文件，将其内容注入到 Spring 的 `Environment` 环境中。

**定义位置：**  
配置类上。

```java
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@PropertySource("classpath:application.properties")
public class PropConfig {}
```

**补充说明：**

- 支持多个文件：

  ```java
  @PropertySources({
      @PropertySource("classpath:app.properties"),
      @PropertySource("classpath:db.properties")
  })
  ```

- 优先级低于 `application.yml`。

---

## @SpringBootConfiguration

**作用：**  
`@Configuration` 的派生注解，是 Spring Boot 应用的配置核心标识。  
实际上 `@SpringBootApplication` 就使用了它。

**定义位置：**  
一般在主类中（无需单独使用）。

```java
import org.springframework.boot.SpringBootConfiguration;

@SpringBootConfiguration
public class MainConfig {}
```

---

## 条件装配系列（@ConditionalOn*）

**作用：**  
根据条件是否成立来决定是否注册 Bean。  
常用于自动配置类。

**常见成员：**

- `@ConditionalOnClass`：类存在时生效；
- `@ConditionalOnMissingClass`：类不存在时生效；
- `@ConditionalOnBean` / `@ConditionalOnMissingBean`：Bean 存在/不存在时；
- `@ConditionalOnProperty`：指定属性存在或满足值时；
- `@ConditionalOnWebApplication`：仅在 Web 环境中；
- `@ConditionalOnExpression`：SpEL 表达式计算结果为 true 时。

```java
@Configuration
@ConditionalOnClass(name = "com.fasterxml.jackson.databind.ObjectMapper")
public class JacksonAutoConfig {

    @Bean
    @ConditionalOnMissingBean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}
```

---

## @EnableConfigurationProperties / @ConfigurationProperties

**作用：**  
将配置文件（application.yml / properties）中的属性映射为 Java 对象。  
前者用于启用绑定功能，后者用于声明属性类。

**定义位置：**  

- `@ConfigurationProperties`：放在普通类上；  
- `@EnableConfigurationProperties`：放在配置类上。

```java
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@ConfigurationProperties(prefix = "app")
public class AppProperties {
    private String name;
    private int timeout;
    // getter/setter
}

@EnableConfigurationProperties(AppProperties.class)
@Configuration
public class PropsConfig {}
```

**补充说明：**

- 也可使用构造绑定：

  ```java
  @ConfigurationProperties(prefix = "app")
  public record AppProps(String name, int timeout) {}
  ```

- Boot 2.2+ 支持自动注册（无需显式启用）。

---

## @Profile

**作用：**  
基于环境（profile）激活不同的 Bean 或配置，常用于区分开发 / 测试 / 生产环境。

**定义位置：**  
类或方法上。

```java
@Configuration
@Profile("dev")
public class DevConfig {

    @Bean
    public DataSource dataSource() {
        // 开发环境数据源
        return new HikariDataSource();
    }
}
```

**使用方式：**
在配置文件中激活：

```yaml
spring:
  profiles:
    active: dev
```

---

## @Lazy

**作用：**  
延迟初始化 Bean，直到第一次被使用时才实例化。

**定义位置：**  
类级或方法级。

```java
@Configuration
public class LazyConfig {

    @Bean
    @Lazy
    public ExpensiveService expensiveService() {
        System.out.println("ExpensiveService 初始化");
        return new ExpensiveService();
    }
}
```

应用启动时不会立即创建该 Bean，只有调用时才会创建。

---

## @ImportResource

**作用：**  
导入传统 XML 配置文件，便于与旧项目混用。

**定义位置：**  
配置类上。

```java
@Configuration
@ImportResource("classpath:/legacy-beans.xml")
public class LegacyConfig {}
```

---

## @SpringBootTest（测试）

**作用：**  
在测试中启动完整的 Spring Boot 应用上下文。  
常用于集成测试。

**定义位置：**  
测试类上。

```java
import org.springframework.boot.test.context.SpringBootTest;
import org.junit.jupiter.api.Test;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class DemoTests {

    @Test
    void contextLoads() {
        System.out.println("上下文加载成功");
    }
}
```

**补充说明：**

- 可通过 `properties` 指定临时属性；
- 可配合 `@MockBean` 模拟依赖；
- `webEnvironment` 选项：
    - `NONE`：不启动 Web 环境；
    - `MOCK`：使用 Mock 环境；
    - `RANDOM_PORT`：随机端口启动；
    - `DEFINED_PORT`：固定端口。
