
# Spring Boot 常用模块详解

> 本课程面向前后端分离企业项目，讲解 Redis、Spring Security、Spring Cache 与定时任务在高并发系统中的整合应用。
> 案例背景：模拟电商系统的“用户登录认证 + 缓存 + 定时任务清理机制”。

---

## 🎯 学习目标

- 掌握四大常用模块的原理与企业场景用法。
- 理解配置项、参数与运行机制。
- 能够设计登录认证、缓存与异步任务联动系统。

---

## 一、Redis 模块（高性能缓存与分布式会话）

### 1️⃣ 模块作用

Redis 是内存型数据库，广泛用于：

- 登录 Token 存储与黑名单
- 验证码缓存、防暴力破解
- 热点数据缓存（商品详情、库存）
- 分布式锁与异步消息处理

### 2️⃣ 前后端分离架构中的角色

```mermaid
flowchart LR
Frontend -->|REST API| Backend
Backend -->|操作| Redis[(Redis Server)]
Backend --> DB[(MySQL)]
```

> 前端请求 -> 后端验证 -> 缓存查询 -> 减少数据库访问

### 3️⃣ 配置讲解（application.yml）

```yaml
spring:
  data:
    redis:
      host: localhost          # Redis 服务器地址
      port: 6379               # Redis 端口
      database: 0              # 使用的逻辑数据库索引
      lettuce:
        pool:
          max-active: 8        # 最大连接数
          max-idle: 8          # 最大空闲连接数
          min-idle: 0          # 最小空闲连接
          max-wait: 1ms        # 等待连接最大时间
```

💡 **讲解要点：**

- `max-active`: 控制最大并发连接数，防止连接爆满。  
- `max-wait`: 避免线程长时间等待连接。  
- 建议搭配 `spring.cache.type=redis` 启用缓存整合。

### 4️⃣ 配置类逐行讲解

```java
@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>(); // 创建模板
        template.setConnectionFactory(factory);                        // 绑定连接工厂
        template.setKeySerializer(new StringRedisSerializer());         // Key 序列化为字符串
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer()); // Value 转为 JSON
        template.afterPropertiesSet();                                  // 初始化
        return template;
    }
}
```

### 5️⃣ 实战案例：Token 管理

```java
@Service
@RequiredArgsConstructor
public class TokenService {
    private final RedisTemplate<String, Object> redis;

    // 保存 Token 并设置过期时间
    public void saveToken(String userId, String token) {
        redis.opsForValue().set("auth:token:" + userId, token, 1, TimeUnit.HOURS); // 过期 1 小时
    }

    // 校验 Token
    public boolean isValid(String userId, String token) {
        String key = "auth:token:" + userId;
        Object saved = redis.opsForValue().get(key);
        return token.equals(saved);
    }
}
```

---

## 二、Spring Security 模块（JWT 无状态认证）

### 1️⃣ 模块作用

提供认证与授权机制；结合 JWT 可实现无状态登录。

### 2️⃣ 登录流程

```mermaid
sequenceDiagram
Frontend->>Backend: POST /auth/login
Backend->>AuthService: 校验用户信息
AuthService->>JWT: 生成 Token
JWT-->>Frontend: 返回 Token
Frontend->>Backend: 请求携带 Authorization 头
Backend->>JwtFilter: 校验 Token
JwtFilter->>SecurityContext: 注入认证信息
```

### 3️⃣ 配置逐行说明

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
            .csrf(AbstractHttpConfigurer::disable)                        // 关闭 CSRF，前后端分离用 JWT
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/auth/**").permitAll()                  // 登录接口放行
                .anyRequest().authenticated())                            // 其他接口需登录
            .addFilterBefore(new JwtAuthFilter(), UsernamePasswordAuthenticationFilter.class)
            .build();
    }
}
```

### 4️⃣ JWT 鉴权过滤器

```java
@Component
public class JwtAuthFilter extends OncePerRequestFilter {
    @Autowired private JwtUtil jwt;

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String header = req.getHeader("Authorization");                   // 从请求头取 Token
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            String username = jwt.extractUsername(token);                 // 解析 Token
            if (username != null && jwt.validateToken(token)) {
                UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(username, null, List.of());
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }
        chain.doFilter(req, res);                                         // 放行请求
    }
}
```

---

## 三、Spring Cache 模块

### 1️⃣ 模块作用

缓存查询结果，减少数据库压力。

### 2️⃣ 配置与注解说明

```java
@EnableCaching
@Service
public class ProductService {

    @Cacheable(value="products", key="#id")     // 第一次查询后缓存结果
    public Product find(Long id) {
        System.out.println("查询数据库...");
        return repo.findById(id).orElseThrow();
    }

    @CacheEvict(value="products", key="#id")    // 删除缓存
    public void delete(Long id) { repo.deleteById(id); }
}
```

### 3️⃣ 使用场景

- 商品、用户资料、配置项缓存
- 查询频繁但变动少的数据

---

## 四、Spring Boot 定时任务

### 1. 什么是定时任务

定时任务（Scheduled Task）是指程序在运行过程中，按照**固定时间**或**固定周期**自动执行的方法。  
在后端系统中，常用于数据同步、定时统计、日志清理等场景。

Spring Boot 通过 `@Scheduled` 注解提供了非常简单的定时任务支持。

---

### 2. 启用定时任务功能

在 Spring Boot 中，使用定时任务前，需要在启动类上开启调度功能。

```java
@SpringBootApplication
@EnableScheduling
public class DemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }
}
```

说明：  
如果没有添加 `@EnableScheduling`，所有 `@Scheduled` 注解都不会生效。

---

### 3. 定义定时任务类

定时任务类需要交给 Spring 管理，通常使用 `@Component` 注解。

```java
@Component
public class SimpleJob {
}
```

---

### 4. 使用 @Scheduled 定义定时任务

#### 4.1 fixedRate（固定频率执行）

含义：  
每隔固定时间执行一次（从**方法开始执行**算间隔）。

```java
@Component
public class RateJob {

    @Scheduled(fixedRate = 5000)
    public void run() {
        System.out.println("fixedRate 执行：" + System.currentTimeMillis());
    }
}
```

说明：  

- 时间单位为毫秒  
- `5000` 表示每 5 秒执行一次

---

#### 4.2 fixedDelay（固定延迟执行）

含义：  
上一次方法执行完成后，延迟指定时间再执行。

```java
@Component
public class DelayJob {

    @Scheduled(fixedDelay = 5000)
    public void run() throws InterruptedException {
        Thread.sleep(2000);
        System.out.println("fixedDelay 执行：" + System.currentTimeMillis());
    }
}
```

说明：  

- 会等待方法执行完毕  
- 再延迟 5 秒执行下一次

---

#### 4.3 cron 表达式（按时间点执行）

```java
@Component
public class CronJob {

    @Scheduled(cron = "0 0 2 * * *")
    public void run() {
        System.out.println("cron 执行：" + System.currentTimeMillis());
    }
}
```

含义：  
每天凌晨 2 点执行一次。

---

### 5. cron 表达式基础说明

Spring Boot 使用 6 位 cron 表达式：

```
秒 分 时 日 月 星期
```

常见示例：

| 表达式 | 说明 |
|------|------|
| 0 */5* ** * | 每 5 分钟执行 |
| 0 0 **** | 每小时执行 |
| 0 0 9 ** * | 每天 9 点执行 |
| 0 0 0 ** * | 每天凌晨执行 |

---

### 6. 基本使用规则

1. 定时任务方法 **不能有参数**
2. 定时任务方法 **返回值必须是 void**
3. 定时任务随 Spring Boot 启动而启动

错误示例：

```java
@Scheduled(fixedRate = 1000)
public String run(String name) {
    return "error";
}
```

---

### 7. 总结

- 使用 `@EnableScheduling` 开启定时任务功能  
- 使用 `@Scheduled` 定义执行规则  
- 三种基础方式：`fixedRate`、`fixedDelay`、`cron`  
- 适合入门学习和简单业务场景

---

## 五、综合实战案例：用户登录与缓存清理系统

```mermaid
flowchart LR
A[Frontend] --> B[Login API]
B --> C[JWT + Redis 存储 Token]
C --> D[SecurityFilter 验证请求]
D --> E[Controller -> Service -> Cache]
F[定时任务] -->|清理过期 Token| C
```

💡 **业务逻辑说明：**

1. 用户登录后生成 JWT 并存储在 Redis。  
2. Redis 缓存 Token + 用户信息。  
3. Spring Cache 缓存业务查询结果。  
4. 定时任务定期清理 Redis 无效 Token。

---

