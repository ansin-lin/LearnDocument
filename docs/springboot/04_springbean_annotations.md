
# Spring Bean 管理与生命周期注解

> 覆盖 Bean 注册、限定与选择、作用域、生命周期回调等。

---

## @Component / @Service / @Repository / @Controller

**作用**：声明组件并交由容器管理。`@Service`/`@Repository`/`@Controller` 是 `@Component` 的语义化派生。  
**定义位置**：类上。

```java
@Service
public class UserService {}
```

- `@Repository` 额外启用持久层异常转换（将底层异常转换为 `DataAccessException`）。

---

## @RestController（复合注解）

**作用**：`@Controller` + `@ResponseBody`，控制器返回值直接写入 HTTP 响应体（默认序列化为 JSON）。  
**定义位置**：类上。

```java
@RestController
@RequestMapping("/api")
public class HelloController {
  @GetMapping("/hello")
  public Map<String, String> hello() { return Map.of("msg","ok"); }
}
```

---

## @Bean

**作用**：在 `@Configuration` 中声明方法，返回值作为 Bean 放入容器。可结合 `initMethod`/`destroyMethod`。  
**定义位置**：配置类的方法上。

```java
@Bean(initMethod = "init", destroyMethod = "close")
public OkHttpClient http() { return new OkHttpClient(); }
```

---

## @Scope

**作用**：指定 Bean 作用域：`singleton`（默认）、`prototype`、`request`、`session`、`application`（Web）。  
**定义位置**：类或 `@Bean` 方法。

```java
@Scope("prototype")
@Bean
public Foo foo() { return new Foo(); }
```

---

## @Primary / @Qualifier

**作用**：多候选 Bean 注入时的选择策略。`@Primary` 标记默认首选；`@Qualifier("name")` 精确指定。  
**定义位置**：类/方法定义处或注入点。

```java
@Service @Primary class AliSmsSender implements SmsSender {}
@Service @Qualifier("twilio") class TwilioSmsSender implements SmsSender {}

@RequiredArgsConstructor
class Notify {
  private final SmsSender smsSender;           // 注入 @Primary
  private final @Qualifier("twilio") SmsSender twilioSender; // 指定
}
```

---

## @Lazy（Bean 级）

**作用**：延迟创建 Bean，减少启动时间或避免循环依赖初始化链。  
**定义位置**：类或 `@Bean` 方法。

---

## @DependsOn

**作用**：声明初始化依赖顺序。  
**定义位置**：类或 `@Bean` 方法。

```java
@DependsOn("cacheManager")
@Bean
StatsService statsService() { return new StatsService(); }
```

---

## 生命周期回调：@PostConstruct / @PreDestroy

**作用**：Bean 初始化后与销毁前的回调。  
**定义位置**：Bean 方法上。

```java
@PostConstruct
public void init(){}
@PreDestroy
public void cleanup(){}
```

---

## @Order

**作用**：定义有序集合注入或切面优先级。  
**定义位置**：类或方法上。

---

## @Lookup

**作用**：在单例中注入原型 Bean 的查找方法注入。  
**定义位置**：抽象方法上，由容器生成实现。

```java
public abstract class SingleService {
  @Lookup protected abstract Task newTask();
}
```

---

