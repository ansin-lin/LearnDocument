
# Spring Boot 基础与核心注解

> 本章覆盖 Spring Boot 启动与核心配置相关注解。每个注解包含：作用、定义位置、示例、补充说明（若为复合注解会拆解包含的注解）。

---

## @SpringBootApplication

**作用**：应用入口复合注解，等价于 `@Configuration` + `@EnableAutoConfiguration` + `@ComponentScan`。用于声明配置类、启用自动配置、启用组件扫描（默认扫描启动类所在包及其子包）。  
**定义位置**：应用主启动类上。

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

**复合注解拆解**：

- `@Configuration`：声明当前类是配置类，注册 `@Bean` 定义。
- `@EnableAutoConfiguration`：根据 classpath 依赖与 `spring.factories`/`AutoConfiguration.imports` 加载自动配置。
- `@ComponentScan`：扫描并注册组件（`@Component`、`@Service`、`@Controller`、`@Repository` 等）。

---

## @Configuration

**作用**：标识配置类，使其中 `@Bean` 方法生效（默认使用 CGLIB 代理，保证 `@Bean` 单例间方法调用仍返回容器中的同一实例）。  
**定义位置**：任意 Java 类上。

```java
@Configuration
public class AppConfig {
  @Bean
  public ObjectMapper objectMapper() { return new ObjectMapper(); }
}
```

---

## @EnableAutoConfiguration

**作用**：开启 Spring Boot 自动配置机制，按条件装配（`@Conditional*`）将常用组件注册到容器。  
**定义位置**：配置类上（通常由 `@SpringBootApplication` 隐式启用）。

```java
@Configuration
@EnableAutoConfiguration
public class AutoConfigEnabled {}
```

---

## @ComponentScan

**作用**：控制组件扫描的基本包、过滤器等。  
**定义位置**：配置类上。

```java
@ComponentScan(basePackages = "com.example.demo")
@Configuration
public class ScanConfig {}
```

**补充**：默认以注解所在包为根进行扫描；可用 include/exclude filters 精准控制。

---

## @Import

**作用**：将指定的配置类或 `ImportSelector`/`ImportBeanDefinitionRegistrar` 产生的定义导入容器。  
**定义位置**：配置类上。

```java
@Import(AppConfig.class)
@Configuration
public class RootConfig {}
```

---

## @PropertySource / @PropertySources

**作用**：显式加载外部属性文件到 Environment。  
**定义位置**：配置类上。

```java
@PropertySource("classpath:app.properties")
@Configuration
public class PropConfig {}
```

---

## @SpringBootConfiguration

**作用**：`@Configuration` 的派生注解，标识 Spring Boot 配置类。  
**定义位置**：通常由 `@SpringBootApplication` 间接使用。

---

## @ConditionalOnClass / @ConditionalOnMissingClass / @ConditionalOnBean / @ConditionalOnMissingBean / @ConditionalOnProperty / @ConditionalOnWebApplication / @ConditionalOnExpression 等（条件装配家族）

**作用**：基于条件决定是否装配 `@Bean` 或配置类。广泛用于自动配置模块。  
**定义位置**：`@Configuration` 类或 `@Bean` 方法上。

```java
@Configuration
@ConditionalOnClass(name = "com.fasterxml.jackson.databind.ObjectMapper")
public class JacksonAutoConfig {
  @Bean @ConditionalOnMissingBean
  ObjectMapper objectMapper() { return new ObjectMapper(); }
}
```

---

## @EnableConfigurationProperties / @ConfigurationProperties

**作用**：将配置文件中的属性映射到 POJO；前者启用后者的注册绑定。  
**定义位置**：

- `@ConfigurationProperties`：放在普通类上；
- `@EnableConfigurationProperties`：放在配置类上（Boot 2.2+ 支持通过 `@ConfigurationProperties` + `@ConstructorBinding` 直接被容器拾取）。

```java
@ConfigurationProperties(prefix = "app")
public record AppProps(String name, int timeout) {}

@EnableConfigurationProperties(AppProps.class)
@Configuration
class PropsConfig {}
```

---

## @Profile

**作用**：基于环境激活不同的 Bean/配置。  
**定义位置**：类或方法上。

```java
@Profile("dev")
@Bean
DataSource devDataSource() { /*...*/ return ds; }
```

---

## @Lazy

**作用**：延迟初始化 Bean，直到首次使用时创建。  
**定义位置**：类或 `@Bean` 方法。

```java
@Lazy
@Bean
ExpensiveService exp() { return new ExpensiveService(); }
```

---

## @ImportResource

**作用**：导入 XML 配置，便于与老项目混合。  
**定义位置**：配置类上。

```java
@ImportResource("classpath:/legacy.xml")
@Configuration
class LegacyConfig {}
```

---

## @SpringBootTest（测试）

**作用**：在测试中启动完整的 Spring 应用上下文，可配合 `webEnvironment`、`properties` 自定义。  
**定义位置**：测试类上。

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class DemoTests {}
```
