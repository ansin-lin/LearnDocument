# Spring Bean 管理与生命周期注解

> 覆盖 Bean 注册、限定与选择、作用域、生命周期回调等。

---

## 🧩 @Component / @Service / @Repository / @Controller

**作用：**  
声明组件并交由容器管理。`@Service`、`@Repository`、`@Controller` 都是 `@Component` 的语义化派生，用于区分应用层次。

**定义位置：**  
类上。

```java
@Service
public class UserService {}
```

**详细说明：**

- `@Component` 是通用组件注解，适用于任何层。  
- `@Service` 用于业务逻辑层，支持事务、AOP。  
- `@Repository` 用于数据访问层，附带**异常转换机制**，自动将底层异常转换为 `DataAccessException`。  
- `@Controller` 用于 MVC 控制层，处理 Web 请求。

---

## 🧩 @RestController（复合注解）

**作用：**  
等价于 `@Controller` + `@ResponseBody`，用于构建 RESTful API。返回值会被自动序列化为 JSON 或 XML 并写入 HTTP 响应体。

**定义位置：**  
类上。

```java
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class HelloController {

    @GetMapping("/hello")
    public Map<String, String> hello() {
        return Map.of("msg", "ok");
    }
}
```

**补充说明：**

- 适用于前后端分离的 REST API。  
- 返回类型可为对象、集合、字符串等，自动使用 `HttpMessageConverter` 转换为 JSON。

---

## 🧩 @Bean

**作用：**  
在 `@Configuration` 类中声明方法，返回对象将作为 Bean 注册到 IoC 容器中。

**定义位置：**  
配置类的方法上。

```java
@Bean(initMethod = "init", destroyMethod = "close")
public OkHttpClient httpClient() {
    return new OkHttpClient();
}
```

**补充说明：**

- 支持生命周期方法（`initMethod` / `destroyMethod`）。  
- 方法名默认作为 Bean 名称，可通过 `@Bean("beanName")` 修改。  
- 可结合条件注解使用，如 `@ConditionalOnMissingBean`。

---

## 🧩 @Scope

**作用：**  
定义 Bean 的作用域（生命周期范围）。

**定义位置：**  
类或 `@Bean` 方法上。

**常见取值：**

| 作用域 | 说明 | 使用场景 |
|--------|------|----------|
| `singleton` | 单例（默认），全局共享实例 | 通用服务类 |
| `prototype` | 每次获取新实例 | 短生命周期对象 |
| `request` | 每个 HTTP 请求创建 | Web 请求作用域 |
| `session` | 每个 Session 创建 | Web 会话作用域 |
| `application` | ServletContext 范围 | Web 全局作用域 |

**示例：**

```java
@Scope("prototype")
@Bean
public Foo foo() { return new Foo(); }
```

---

## 🧩 @Primary / @Qualifier

**作用：**  
在存在多个同类型 Bean 时控制注入优先级。  
`@Primary` 标记默认首选 Bean，`@Qualifier("name")` 精确指定要注入的 Bean。

**定义位置：**  
类、方法或注入点。

```java
@Service @Primary
class AliSmsSender implements SmsSender {}

@Service @Qualifier("twilio")
class TwilioSmsSender implements SmsSender {}

@RequiredArgsConstructor
class NotifyService {
    private final SmsSender smsSender;                // 注入 @Primary
    private final @Qualifier("twilio") SmsSender twilioSender; // 指定名称
}
```

---

## 🧩 @Lazy（Bean 级）

**作用：**  
延迟初始化 Bean，直到第一次被使用时才创建实例。  
减少启动时间或避免循环依赖。

**定义位置：**  
类上或 `@Bean` 方法上。

```java
@Lazy
@Service
public class HeavyService {
    public HeavyService() {
        System.out.println("HeavyService 初始化");
    }
}
```

---

## 🧩 @DependsOn

**作用：**  
声明 Bean 初始化依赖关系，确保被依赖 Bean 先创建。

**定义位置：**  
类上或 `@Bean` 方法上。

```java
@DependsOn("cacheManager")
@Bean
public StatsService statsService() {
    return new StatsService();
}
```

**补充说明：**

- 仅控制初始化顺序，不影响依赖注入逻辑。

---

## 🧩 生命周期回调：@PostConstruct / @PreDestroy

**作用：**  
声明 Bean 初始化完成后与销毁前的回调方法。

**定义位置：**  
Bean 方法上。

```java
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

@Service
public class CacheService {

    @PostConstruct
    public void init() {
        System.out.println("Cache 初始化");
    }

    @PreDestroy
    public void cleanup() {
        System.out.println("Cache 销毁前清理资源");
    }
}
```

**执行顺序：**

1. 实例化 Bean  
2. 依赖注入完成  
3. 执行 `@PostConstruct`  
4. Bean 可被使用  
5. 容器关闭前执行 `@PreDestroy`

---

## 🧩 @Order

**作用：**  
定义 Bean、拦截器、过滤器、切面等的执行顺序。  
数值越小，优先级越高。

**定义位置：**  
类或方法上。

```java
@Component
@Order(1)
public class FirstInterceptor implements HandlerInterceptor {}

@Component
@Order(2)
public class SecondInterceptor implements HandlerInterceptor {}
```

---

## 🧩 @Lookup

**作用：**  
用于在单例 Bean 中动态获取原型 Bean（Prototype Bean）。  
Spring 会在运行时为该抽象方法生成实现，以完成依赖查找。

**定义位置：**  
抽象方法上。

```java
public abstract class SingleService {

    @Lookup
    protected abstract Task newTask();

    public void process() {
        Task t = newTask(); // 每次调用都创建新实例
        t.execute();
    }
}
```

**应用场景：**

- 单例 Bean 需要按需创建短生命周期对象。

---
