# 第15章 用Session识别当前登录用户

> 本章目标：在员工管理API中完成真实的账号密码登录，使后续请求能够通过Session识别当前用户，并能验证登录失败、CSRF、退出和会话失效。

## 一、登录要解决的是“后续请求还是谁”

HTTP请求彼此独立。第一次请求提交正确密码，不代表第二次请求天然知道同一个用户。本章建立下面的闭环：

```text
GET /auth/csrf          → 取得CSRF令牌
POST /auth/login        → 验证账号密码，服务器建立Session
                         ← Set-Cookie: JSESSIONID=随机会话标识
GET /auth/me            → 客户端携带Cookie，服务器恢复当前用户
POST /auth/logout       → 清除认证和Session
GET /auth/me            → 再次访问得到401
```

本章只回答“用户是谁”，不判断普通用户是否能修改或删除员工。角色和数据范围授权在第16章实现。

## 二、接口规格

| 编号 | 接口 | 输入 | 成功 | 失败 |
| --- | --- | --- | --- | --- |
| AUTH-CSRF-01 | `GET /auth/csrf` | 无 | 200，返回令牌名、值和请求头名 | — |
| AUTH-LOGIN-01 | `POST /auth/login` | JSON账号密码＋CSRF令牌 | 200，返回用户名；建立Session | 400字段错误、401统一登录失败、403缺少CSRF |
| AUTH-ME-01 | `GET /auth/me` | Session Cookie | 200，返回当前用户名 | 401未登录或会话过期 |
| AUTH-LOGOUT-01 | `POST /auth/logout` | Session Cookie＋CSRF令牌 | 204，无正文；Session失效 | 403缺少CSRF |

登录失败只返回“用户名或密码错误”，不能分别暴露“用户不存在”和“密码错误”。响应、日志和异常堆栈都不能记录原始密码、密码哈希、JSESSIONID或CSRF令牌。

## 三、按案例建立可跨请求恢复的登录

从第14章的员工项目继续，不重建业务系统。每个案例都保留前一案例已完成的文件；本节在首次操作位置给出所需代码，章末最终文件用于完成后核对。只在个人练习数据库创建账号并生成BCrypt哈希，不使用真实密码或共享账号。

先看组件如何连接；箭头表示调用或注入，不表示所有组件都由Controller手工创建：

```text
POST /auth/login → SecurityFilterChain（先检查CSRF）→ AuthController
                                                    │ 注入AuthenticationManager
                                                    ▼
                                             DaoAuthenticationProvider
                                              ├─ AppUserDetailsService → AppUserMapper → app_users
                                              └─ PasswordEncoder（比较原始密码和哈希）
成功的Authentication → SessionAuthenticationStrategy（更换Session ID并清理旧CSRF令牌）
                    → SecurityContextRepository（保存到Session）
后续请求携带JSESSIONID → SecurityFilterChain从Session恢复身份 → /auth/me或员工接口
```

`@Configuration` 中的 `@Bean` 方法把PasswordEncoder、AuthenticationManager、SecurityContextRepository、CsrfTokenRepository和SessionAuthenticationStrategy注册进Spring容器；构造器参数和 `@Bean` 方法参数由容器按类型注入。账号Mapper由MyBatis创建，`AppUserDetailsService` 由 `@Service` 注册。依赖关系不是“Controller自己new所有对象”，缺少其中一个Bean会在启动时暴露，而不是等登录时才随机失效。

### 案例15-01：未登录访问员工接口

先在第14章 `pom.xml` 的 `<dependencies>` 中追加以下两项，其他原有依赖不删；版本继续交给Spring Boot 4.1.x管理。第一项进入正式应用，第二项只供测试使用：

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security-test</artifactId>
    <scope>test</scope>
</dependency>
```

刷新Eclipse Maven项目依赖，再新建 `config/SecurityConfig.java`，建立只保护员工接口的最小过滤链。此时尚无登录接口，先观察匿名请求不能到达Controller：

```java
package com.example.employee.config;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http)
            throws Exception {
        http.authorizeHttpRequests(authorize -> authorize
                    .requestMatchers("/employees", "/employees/**")
                    .authenticated()
                    .anyRequest().permitAll())
                .exceptionHandling(exceptions -> exceptions
                    .authenticationEntryPoint((request, response, exception) ->
                        response.sendError(HttpServletResponse.SC_UNAUTHORIZED)))
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable);
        return http.build();
    }
}
```

`SecurityFilterChain` 是Spring管理的安全过滤链配置；请求先经过过滤器，再可能到达Spring MVC。这里显式关闭表单登录和Basic认证，给REST请求设置401入口，所以未认证的GET `/employees/1001` 预期401，而不是依赖框架默认登录页可能产生的302。401表示尚未认证；403可能表示CSRF失败或已认证但无权限，不能只凭403判断角色。认证回答“是谁”，授权回答“能访问什么”；本章只做基本登录保护。Postman清除旧Cookie后请求员工详情，确认401且Controller断点未命中。

### 案例15-02：让认证组件能查账号并验证密码

按下面四步建立账号加载能力；此阶段还没有登录入口，员工接口继续返回401，但工程应能启动，数据库查询应能找到练习账号。

1. 仅在个人MySQL 8.0练习库执行账号表DDL，确认连接不是共享或生产库：

```sql
USE employee_db;
CREATE TABLE app_users (
    id BIGINT NOT NULL AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    employee_id BIGINT NULL,
    role VARCHAR(30) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT pk_app_users PRIMARY KEY (id),
    CONSTRAINT uk_app_users_username UNIQUE (username),
    CONSTRAINT uk_app_users_employee_id UNIQUE (employee_id)
);
```

`username` 唯一，`password_hash` 只存单向哈希，`employee_id` 是与员工的可空关联，`role` 暂存 `USER`/`ADMIN`，`enabled` 控制是否允许登录；当前物理删除员工规则下不贸然加外键。新建 `src/main/java/com/example/employee/entity/AppUser.java`：

```java
package com.example.employee.entity;

public class AppUser {
    private Long id;
    private String username;
    private String passwordHash;
    private Long employeeId;
    private String role;
    private boolean enabled;
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public Long getEmployeeId() { return employeeId; }
    public void setEmployeeId(Long employeeId) { this.employeeId = employeeId; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
}
```

2. 新建 `src/main/java/com/example/employee/mapper/AppUserMapper.java` 和 `src/main/resources/mapper/AppUserMapper.xml`：

```java
package com.example.employee.mapper;
import com.example.employee.entity.AppUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AppUserMapper {
    AppUser findByUsername(@Param("username") String username);
}
```

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "https://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.example.employee.mapper.AppUserMapper">
    <select id="findByUsername" resultType="AppUser">
        SELECT id, username, password_hash, employee_id, role, enabled
        FROM app_users WHERE username = #{username}
    </select>
</mapper>
```

`#{username}` 是绑定参数，不是拼接SQL；MyBatis按原有下划线转驼峰配置把 `password_hash` 映射到 `passwordHash`。用数据库客户端确认用户名查询恰好一行，未找到时Mapper返回null。

3. 新建 `src/main/java/com/example/employee/security/AppUserDetailsService.java`：

```java
package com.example.employee.security;
import com.example.employee.entity.AppUser;
import com.example.employee.mapper.AppUserMapper;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class AppUserDetailsService implements UserDetailsService {
    private final AppUserMapper appUserMapper;
    public AppUserDetailsService(AppUserMapper appUserMapper) {
        this.appUserMapper = appUserMapper;
    }
    @Override
    public UserDetails loadUserByUsername(String username) {
        AppUser user = appUserMapper.findByUsername(username);
        if (user == null) {
            throw new UsernameNotFoundException("login failed");
        }
        return User.withUsername(user.getUsername())
                .password(user.getPasswordHash())
                .roles(user.getRole())
                .disabled(!user.isEnabled())
                .build();
    }
}
```

`loadUserByUsername` 由认证Provider调用；它只把数据库账号转成Security的 `UserDetails`，不接收HTTP、不比较密码。`.roles("USER")` 会生成 `ROLE_USER` 权限；数据库值不要带 `ROLE_` 前缀。不存在或停用账号对外都应统一为登录失败。

4. 在案例15-01的 `SecurityConfig.java` 增加两个导入，并在类中追加Bean方法，不删除原过滤链：

```java
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(10);
}
```

在本机临时新建 `src/test/java/com/example/employee/security/TrainingHashTest.java`，用Eclipse作为JUnit Test运行一次，复制控制台的课程专用哈希；完成插入后删除这个临时测试文件，不把哈希生成输出保存进提交记录：

```java
package com.example.employee.security;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class TrainingHashTest {
    @Test
    void printTrainingHash() {
        System.out.println(new BCryptPasswordEncoder(10)
                .encode("TrainingPass123!"));
    }
}
```

该测试用与Bean相同的算法和强度生成**练习密码**的哈希；正式登录仍由Spring注入的Bean验证，不从控制台读取。把刚生成的哈希填进个人练习库下面一条INSERT，不要原样插入占位文字：

```sql
INSERT INTO app_users (username, password_hash, employee_id, role, enabled)
VALUES ('tanaka', '这里替换为刚生成的BCrypt哈希', 1001, 'USER', TRUE);
```

`matches(raw, encoded)` 才是认证时的比较方式，重复 `encode()` 得到不同字符串属正常现象。不要在日志、截图或Git中保存真实密码。检查 `app_users` 有一行哈希、哈希不等于明文，并重新启动应用确认无Mapper或Bean创建错误；此时仍不能发起登录，下一案例才接入认证调用。

### 案例15-03：完成JSON登录并保存SecurityContext

在前一案例基础上，完整替换临时 `SecurityConfig.java`；然后新建请求/响应DTO、`AuthController` 和认证失败处理器。本案例完成时，JSON登录、跨请求恢复和退出已经可用，不必先跳到章末拼接文件。

`src/main/java/com/example/employee/config/SecurityConfig.java` 完整内容：

```java
package com.example.employee.config;

import com.example.employee.security.AppUserDetailsService;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import org.springframework.security.web.authentication.session.CompositeSessionAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfAuthenticationStrategy;
import org.springframework.security.web.csrf.CsrfTokenRepository;

@Configuration
public class SecurityConfig {
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }
    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }
    @Bean
    public CsrfTokenRepository csrfTokenRepository() {
        return new CookieCsrfTokenRepository();
    }
    @Bean
    public SessionAuthenticationStrategy sessionAuthenticationStrategy(
            CsrfTokenRepository csrfTokenRepository) {
        return new CompositeSessionAuthenticationStrategy(List.of(
                new ChangeSessionIdAuthenticationStrategy(),
                new CsrfAuthenticationStrategy(csrfTokenRepository)));
    }
    @Bean
    public AuthenticationManager authenticationManager(
            AppUserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider =
                new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(provider);
    }
    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http, SecurityContextRepository repository,
            CsrfTokenRepository csrfTokenRepository)
            throws Exception {
        http.csrf(csrf -> csrf.csrfTokenRepository(csrfTokenRepository))
                .securityContext(context -> context
                    .requireExplicitSave(true)
                    .securityContextRepository(repository))
                .authorizeHttpRequests(authorize -> authorize
                    .requestMatchers("/auth/me", "/employees", "/employees/**")
                    .authenticated()
                    .anyRequest().permitAll())
                .exceptionHandling(exceptions -> exceptions
                    .authenticationEntryPoint((request, response, exception) ->
                        response.sendError(HttpServletResponse.SC_UNAUTHORIZED)))
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(logout -> logout.logoutUrl("/auth/logout")
                    .deleteCookies("JSESSIONID")
                    .logoutSuccessHandler((request, response, authentication) ->
                        response.setStatus(204)));
        return http.build();
    }
}
```

过滤链保留CSRF默认防护并明确使用Cookie令牌仓库；`AuthenticationManager` 注入账号加载服务与密码编码器，`SecurityContextRepository` 保存认证。这里把更换Session ID与清理旧CSRF令牌两个策略组合成一个Bean，由自定义登录Controller在认证成功后主动调用；只调用更换ID的策略，不会自动清理旧令牌。`List.of(...)` 提供按顺序执行的两个策略；`CookieCsrfTokenRepository` 负责Cookie存取，`CsrfAuthenticationStrategy` 通过同一个仓库发出清除旧Cookie的响应。过滤链也使用这个仓库。`/auth/login` 和 `/auth/csrf` 由当前 `anyRequest().permitAll()` 放行，但POST登录仍需通过CSRF检查；员工接口及 `/auth/me` 需要登录。

新建 `dto/request/LoginRequest.java`、`dto/response/CurrentUserResponse.java` 和 `dto/response/CsrfResponse.java`：

```java
package com.example.employee.dto.request;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class LoginRequest {
    @NotBlank(message = "用户名不能为空")
    @Size(max = 50, message = "用户名不能超过50个字符")
    private String username;
    @NotBlank(message = "密码不能为空")
    @Size(max = 200, message = "密码不能超过200个字符")
    private String password;
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
```

```java
package com.example.employee.dto.response;
public record CurrentUserResponse(String username) {}
```

```java
package com.example.employee.dto.response;
public record CsrfResponse(
        String parameterName, String headerName, String token) {}
```

每个代码块是独立文件；`record` 适合这里的只读响应，JSON请求对象仍使用可绑定的setter。新建 `src/main/java/com/example/employee/controller/AuthController.java`：

```java
package com.example.employee.controller;

import com.example.employee.common.ApiResponse;
import com.example.employee.dto.request.LoginRequest;
import com.example.employee.dto.response.CsrfResponse;
import com.example.employee.dto.response.CurrentUserResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository contextRepository;
    private final SessionAuthenticationStrategy sessionStrategy;
    public AuthController(AuthenticationManager authenticationManager,
            SecurityContextRepository contextRepository,
            SessionAuthenticationStrategy sessionStrategy) {
        this.authenticationManager = authenticationManager;
        this.contextRepository = contextRepository;
        this.sessionStrategy = sessionStrategy;
    }
    @GetMapping("/csrf")
    public ResponseEntity<ApiResponse<CsrfResponse>> csrf(CsrfToken token) {
        return ResponseEntity.ok(ApiResponse.success(new CsrfResponse(
                token.getParameterName(), token.getHeaderName(), token.getToken())));
    }
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<CurrentUserResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        Authentication input = UsernamePasswordAuthenticationToken.unauthenticated(
                request.getUsername().trim(), request.getPassword());
        Authentication result = authenticationManager.authenticate(input);
        sessionStrategy.onAuthentication(result, httpRequest, httpResponse);
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(result);
        SecurityContextHolder.setContext(context);
        contextRepository.saveContext(context, httpRequest, httpResponse);
        return ResponseEntity.ok(ApiResponse.success(
                new CurrentUserResponse(result.getName())));
    }
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<CurrentUserResponse>> me(
            Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(
                new CurrentUserResponse(authentication.getName())));
    }
}
```

登录先验证密码，再通过组合策略更换Session ID、清理登录前CSRF令牌，最后设置本次请求的认证并显式保存上下文；仅返回200而未调用 `saveContext()`，下一请求仍会是匿名。退出无需另建Controller方法，交由过滤链处理。最后新建 `src/main/java/com/example/employee/exception/AuthExceptionHandler.java`：

```java
package com.example.employee.exception;
import com.example.employee.common.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class AuthExceptionHandler {
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthentication(
            AuthenticationException exception) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.failure("用户名或密码错误"));
    }
}
```

该处理器只处理进入Controller后的认证异常；用户名不存在、密码错误、停用账号都给同一401消息。未登录的401和缺CSRF的403发生在过滤链，不由此Advice处理。现在用 `GET /auth/csrf` → 带令牌 `POST /auth/login` → 携带同一Cookie的 `GET /auth/me` 检查200、200、200；错误密码应是401，缺令牌的登录请求应是403。

登录Controller中的关键顺序可单独读为：

```java
Authentication input =
        UsernamePasswordAuthenticationToken.unauthenticated(
                request.getUsername().trim(), request.getPassword());
Authentication result = authenticationManager.authenticate(input);
sessionStrategy.onAuthentication(result, httpRequest, httpResponse);
SecurityContext context = SecurityContextHolder.createEmptyContext();
context.setAuthentication(result);
SecurityContextHolder.setContext(context);
contextRepository.saveContext(context, httpRequest, httpResponse);
```

`AuthenticationManager` 协调认证，当前 `DaoAuthenticationProvider` 调用UserDetailsService查用户并让PasswordEncoder验证密码。成功返回的 `Authentication` 放入本次请求的 `SecurityContext`；`saveContext()` 才把它持久化到Session，供下一次请求恢复。这里只拿到认证结果但不保存，不能称为Session登录。认证失败统一返回401“用户名或密码错误”，不暴露账号是否存在。登录前先GET `/auth/csrf`，再带令牌POST登录；成功后GET `/auth/me` 和员工详情均应返回200。

### 案例15-04：观察Session和Cookie如何维持身份

HTTP请求本身不记住上一个请求。服务器Session保存认证上下文，客户端Cookie中的JSESSIONID只是定位Session的随机标识，不是密码。Postman的Cookie Jar会按域名自动保存 `Set-Cookie` 并在后续请求中发送，因此同一客户端可能无需手填Cookie就保持登录。按照后文Postman步骤检查登录后的Cookie、`/auth/me` 成功、清除Cookie后401、重新登录后恢复。这里必须证明跨请求恢复，不能仅用登录接口200证明。不要把完整Cookie值放进截图或工单。

### 案例15-05：用受保护写请求验证CSRF

当前Cookie会被浏览器自动附带，因此恶意页面可能借用户已登录状态发起写请求。Spring Security的CSRF过滤器对当前POST、PUT、DELETE进行令牌检查；Cookie不是CSRF令牌。先在登录会话中不给员工POST附令牌，预期403且业务代码不执行；再GET `/auth/csrf` 取当前令牌，用响应中的headerName发送token并携带同一会话Cookie，预期201且数据库出现练习员工。本例登录和退出都会通知客户端清除旧CSRF Cookie；写请求前按当前会话重新获取令牌。本章不关闭CSRF；具体请求、清理和回归测试见后文。

### 案例15-06：退出并验证旧会话失效

保留最终Security配置中的 `logoutUrl("/auth/logout")`；退出由Security过滤器处理，不写一个只修改自定义布尔值的假接口。以当前会话令牌POST退出，预期204；随后GET `/auth/me` 和员工详情都为401，即使重送旧JSESSIONID也不能恢复原认证。正常退出还需清理服务器端SecurityContext和Session，客户端删除本地练习Cookie。案例完成后运行章末 `SessionLoginTest` 并按后文Postman表保存脱敏证据。

## 四、Filter与SecurityFilterChain

Filter是在请求到达Controller之前执行的Servlet组件。Spring Security把多个安全Filter按顺序组成过滤器链，用于加载Session中的认证、检查CSRF、判断访问规则和执行logout。

`SecurityFilterChain` 是这条链的配置结果。`HttpSecurity` 是构建配置的对象，`build()` 返回最终链。应用中不要在每个Controller复制“从Session取用户”的判断，否则容易漏接口、响应不一致，也无法统一处理退出和攻击防护。

## 五、@Configuration与@Bean怎样注册框架对象

`@Configuration` 标记配置类，Spring启动时读取其中的Bean定义。`@Bean` 写在方法上：Spring调用方法，把返回对象注册进容器，默认Bean名是方法名。

| 方法 | 参数来源 | 返回对象 | 用途 |
| --- | --- | --- | --- |
| `passwordEncoder()` | 无 | `PasswordEncoder` | BCrypt哈希与验证 |
| `authenticationManager(...)` | 容器注入两个Bean | `AuthenticationManager` | 组织账号加载和密码校验 |
| `csrfTokenRepository()` | 无 | `CsrfTokenRepository` | 保存和读取Cookie令牌；策略通过它清除Cookie |
| `sessionAuthenticationStrategy(...)` | 容器注入令牌仓库 | `SessionAuthenticationStrategy` | 登录成功时依次更换Session ID、清理旧CSRF令牌 |
| `securityFilterChain(...)` | 容器注入安全对象 | `SecurityFilterChain` | 生成Web安全过滤器链 |

这些对象确实由Spring Security运行时使用，不是为了展示注解而虚构的空组件。

## 六、密码如何验证

`BCryptPasswordEncoder(10)` 使用bcrypt和强度10。`encode()`生成单向哈希，`matches(raw, encoded)`验证原始输入是否对应已存哈希。密码不能解密，也不能明文保存或用Base64代替哈希。正式系统需要根据服务器性能测量强度，并规划哈希升级。参考[Spring Security密码存储](https://docs.spring.io/spring-security/reference/features/authentication/password-storage.html)。

登录过程由 `DaoAuthenticationProvider` 协作完成：调用 `UserDetailsService` 查账号、检查enabled、使用PasswordEncoder比较密码，成功后返回已认证的 `Authentication`。

## 七、Authentication、SecurityContext和Session

`Authentication` 登录前可以承载用户名和密码输入，登录后表示已经确认的用户；返回结果中的凭据会由框架按安全策略处理。`SecurityContext` 保存当前请求的Authentication，`SecurityContextHolder` 让同一请求中的后续代码取得它。

本课程采用Spring Boot 4.1.1管理的Spring Security 7.1.1。自定义Controller完成登录时，需要显式调用 `SecurityContextRepository.saveContext()`，否则当前请求看似成功，下一请求却恢复不到用户。官方流程见[认证持久化与Session管理](https://docs.spring.io/spring-security/reference/servlet/authentication/session-management.html)。

组合的 `SessionAuthenticationStrategy` 在认证成功时先更换Session ID，防止攻击者预先固定会话标识，然后通知客户端清除登录前的CSRF Cookie。JSESSIONID只保存随机会话标识，账号信息保存在服务器Session中；客户端不能从Cookie值推算密码。

由于这是自定义登录Controller，`AuthController.login()` 会主动调用组合策略：先由 `ChangeSessionIdAuthenticationStrategy` 更换ID，再由 `CsrfAuthenticationStrategy` 清除登录前令牌；本例的 `SecurityFilterChain` 不再重复配置 `sessionManagement().sessionAuthenticationStrategy(...)`。自定义认证机制不能假设过滤器会替它调用策略。

## 八、CSRF为什么登录和退出也要检查

浏览器会自动携带目标站点Cookie。攻击者可能诱导已登录用户的浏览器提交修改请求，因此使用Session Cookie的应用必须保留CSRF防护。登录本身也可能遭受“把受害者登录到攻击者账号”的login CSRF，退出也会改变安全状态。

Spring Security对当前Servlet应用默认启用CSRF防护；这里配置 `CookieCsrfTokenRepository` 是选择令牌**存储方式**，不是“打开CSRF”的开关。未配置仓库时默认使用基于HttpSession的令牌仓库；本例改为把令牌存在 `XSRF-TOKEN` Cookie中，而登录认证信息仍通过服务器Session与 `JSESSIONID` 恢复，两种Cookie用途不同。客户端先读取 `/auth/csrf`，从响应Body取 `token`，再按 `headerName` 指定的名称放进写请求头；仅自动携带 `XSRF-TOKEN` Cookie不等于已提交令牌。当前课程不需要浏览器JavaScript直接读取令牌Cookie，因此 `new CookieCsrfTokenRepository()` 保持该Cookie的HttpOnly保护。

本例的自定义登录Controller通过上面的组合策略发出清除旧令牌Cookie的响应；退出则由框架的Logout Filter清理。客户端必须接受这个 `Set-Cookie` 更新，不能故意重送旧Cookie；之后应在**新Session/Cookie状态**下再GET `/auth/csrf`，使用新响应的令牌做POST/PUT/DELETE。缺少、错误或过期令牌时，CSRF过滤器在Controller和URL角色判断之前就可能返回403；这与“已经登录但角色不足”也返回403是两种不同来源。要定位，先核对本次请求是否带有效CSRF，再检查是否已经登录以及是否有角色权限。若将来由前端JavaScript直接读取Cookie，才需评估 `withHttpOnlyFalse()` 与SPA令牌处理；这不意味着可以关闭JSESSIONID的HttpOnly。参考[Spring Security CSRF说明](https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html)和[登出时清理CSRF令牌](https://docs.spring.io/spring-security/reference/servlet/authentication/logout.html)。

## 九、Cookie和会话边界

本地HTTP练习时可观察JSESSIONID；生产环境必须使用HTTPS，并确认Cookie至少具备HttpOnly、Secure及适合业务的SameSite策略。SameSite能降低部分跨站请求风险，但不能替代CSRF令牌。

Session保存在单个应用进程内时，重启会使登录失效，多实例之间也不能自然共享。是否使用Redis等外部Session存储属于部署设计，不能仅修改Cookie解决。

## 十、运行与验证

先执行本章自动化回归：

```powershell
.\mvnw.cmd clean test
```

第14章累计15个测试，本章新增7个，预期22个全部通过。这个数字是按文档中的测试方法计算的预期，不是实际执行结果。自动化测试完成后，使用已配置数据库环境变量的Eclipse Run Configuration启动应用，再用Postman观察真实Cookie和Session流程；Postman不能替代上述JUnit和MockMvc回归。

先清除Postman在 `localhost` 的旧练习Cookie，发送未登录的 `GET {{baseUrl}}/employees/1001`（无参数、无请求体），按本章安全配置预期401。第14章以前的员工查询现在都需要登录；员工POST、PUT、DELETE还要携带当前会话的CSRF令牌。这是引入Security后的接口访问前提，不是原CRUD业务响应规则被删除。

### 1. 取得CSRF令牌

| 项目 | 内容 |
| --- | --- |
| HTTP方法 | GET |
| URL | `{{baseUrl}}/auth/csrf` |
| 查询参数 | 无 |
| 请求体 | 无 |
| 预期状态码 | 200 |
| 预期响应 | `data.headerName` 和 `data.token` 有值 |

发送后Postman会在当前域名的Cookie jar中保存 `XSRF-TOKEN`。从响应Body临时复制 `headerName` 和 `token`；不要把完整令牌保存到共享文档或Git。

### 2. 登录并确认Session

新建登录请求：

| 项目 | 内容 |
| --- | --- |
| HTTP方法 | POST |
| URL | `{{baseUrl}}/auth/login` |
| 查询参数 | 无 |
| Headers | 使用上一步返回的 `headerName` 作为名称、`token` 作为值 |
| Body | **raw → JSON**，见下方 |
| 预期状态码 | 200 |
| 预期响应 | `data.username=tanaka`，响应产生 `JSESSIONID` Cookie |

```json
{
  "username": "tanaka",
  "password": "TrainingPass123!"
}
```

选择请求 **Send** 按钮下方的 **Cookies**，在Cookie管理器中确认 `localhost` 下存在 `JSESSIONID`，但不要复制其值。Postman会为同一域名自动保存并在后续请求中携带Cookie。

然后发送 `GET {{baseUrl}}/auth/me`，无查询参数、无请求体，预期200且 `data.username=tanaka`。如果得到401，先检查是否仍使用同一个Postman环境、域名和Cookie jar。

再发送 `GET {{baseUrl}}/employees/1001`，无参数和请求体，预期200且 `data.id=1001`；在Postman Cookies管理器临时清除 `JSESSIONID` 后重发，预期401。接着重新GET `/auth/csrf`、带新令牌POST `/auth/login`，重新建立登录会话。本例登录成功会清除登录前的CSRF Cookie；确认Postman已接受该响应后，再GET `/auth/csrf`，使用新响应中的 `headerName` 与 `token` 做后续写请求，不能长期复用登录前复制的令牌。

### 3. 验证错误密码和缺少CSRF

| 场景 | HTTP方法与URL | Headers | 请求体 | 预期状态与响应 |
| --- | --- | --- | --- | --- |
| 错误密码 | `POST {{baseUrl}}/auth/login` | 携带有效CSRF请求头 | `{"username":"tanaka","password":"WrongPass123!"}` | 401，统一登录失败消息 |
| 缺少CSRF | `POST {{baseUrl}}/auth/login` | 临时取消勾选CSRF请求头 | 正确账号密码JSON | 403，不进入登录Controller |

两个请求都使用 **raw → JSON** 与 `Content-Type: application/json`。完成缺少CSRF场景后重新启用请求头，避免影响退出请求。

在当前登录会话下，对受保护的员工写接口发送一次合法练习请求，例如 `POST {{baseUrl}}/employees`，Body为 `{"name":"Sato","department":"Sales","email":"sato-session@example.com"}`。先不带CSRF请求头，预期403且数据库不新增记录；再用刚从 `/auth/csrf` 取得的令牌及同一Cookie重发，预期201、返回新编号和Location，数据库新增一行。完成后在个人练习库按新编号删除该员工；不要对共享库操作。403表示请求在CSRF检查处被拒绝，不等于用户名或密码错误。

### 4. 退出并确认Session失效

退出前再次GET `/auth/csrf` 取得当前会话有效令牌。发送 `POST {{baseUrl}}/auth/logout`，无查询参数、无请求体，携带该CSRF请求头和Postman自动保存的Cookie，预期204且响应Body为空。随后再次发送 `GET {{baseUrl}}/auth/me` 和 `GET {{baseUrl}}/employees/1001`，均预期401；即使手动重送退出前的旧JSESSIONID，也不应恢复原认证身份。

验证证据至少包括：取得CSRF、登录响应的Set-Cookie、同一Cookie访问me、错误密码401、缺少CSRF的403、logout 204，以及退出后me重新变为401。只记录Cookie名称和存在性，不要把完整Cookie、密码或令牌粘贴到共享工单。完成后在Postman Cookies管理器中删除本地练习Cookie，并在Eclipse中停止应用。

## 十一、常见失败

| 现象 | 原因 | 修正 |
| --- | --- | --- |
| 登录200但me仍401 | 只设置SecurityContext，未保存 | 调用SecurityContextRepository保存 |
| 正确密码也失败 | 数据库存了明文或哈希算法不匹配 | 用同一PasswordEncoder生成和matches |
| POST总是403 | 没先取得或携带CSRF令牌 | 保留Cookie并按headerName发送token |
| 每次请求产生新会话 | Postman没有保存Cookie，或请求混用了 `localhost` 与 `127.0.0.1` | 统一使用 `{{baseUrl}}` 并检查Cookies管理器 |
| 日志出现密码或Session ID | 记录了请求体、Cookie或安全对象 | 删除敏感字段，只记录结果和请求编号 |
| 登录后Session ID不变 | 自定义登录漏掉SessionAuthenticationStrategy | 在保存context前执行策略 |

## 十二、规格理解、影响调查与练习

1. 实现并验证停用账号登录失败，响应不能暴露“账号已停用”。
2. 用同一个Postman Cookie会话完成csrf→login→me→logout→me，保存脱敏证据。
3. Review一个把密码写进日志、用MD5保存、关闭CSRF的实现，逐项说明风险和修正。
4. 调查“会话30分钟无操作后过期”的配置、测试、用户提示和多实例影响，先提交调查表。
5. 为新增账号功能写规格：谁能创建、密码何时hash、重复用户名、初始enabled及不得返回passwordHash。

## 十三、章末完整代码

### 最终文件清单与完整实现

先在代码前分清这次要建立的关系。HTTP本身不记住用户；服务器Session保存认证状态，客户端Cookie只带会话标识。每个请求先经过Spring Security过滤链，链中的过滤器可在Controller之前检查CSRF并恢复安全上下文。登录时，`AuthController` 把用户名和密码交给 `AuthenticationManager`；`DaoAuthenticationProvider` 调用 `UserDetailsService` 取出 `UserDetails`，再用 `PasswordEncoder` 比对密码哈希。认证成功后，Controller主动执行Session ID更换并把 `SecurityContext` 存进Session；下一次请求由安全过滤链恢复它。数据库账号对象与Security的 `UserDetails` 分开，是因为前者表示表数据，后者是认证组件要求的契约。

本章采用自定义JSON登录Controller，因此认证成功时由Controller显式调用 `SessionAuthenticationStrategy` 和 `SecurityContextRepository.saveContext()`，不依赖 `SessionManagementFilter` 猜测本次是否刚完成认证。CSRF令牌由 `/auth/csrf` 的响应体交给Postman，令牌Cookie保持HttpOnly；认证Cookie `JSESSIONID` 也不能为方便调试而关闭HttpOnly。读完这一条处理链后，再按“账号表与Mapper→账号加载→安全配置→登录接口→CSRF与测试”的顺序写入以下完整文件。

从第14章稳定状态继续，新增安全依赖、账号表和认证代码：

```text
pom.xml                                                     ← 完整替换
src/main/java/com/example/employee/
├── config/SecurityConfig.java                              ← 新建
├── controller/AuthController.java                          ← 新建
├── dto/request/LoginRequest.java                           ← 新建
├── dto/response/CurrentUserResponse.java                   ← 新建
├── dto/response/CsrfResponse.java                          ← 新建
├── entity/AppUser.java                                     ← 新建
├── exception/AuthExceptionHandler.java                    ← 新建
├── mapper/AppUserMapper.java                               ← 新建
└── security/AppUserDetailsService.java                    ← 新建
src/main/resources/mapper/AppUserMapper.xml                 ← 新建
src/test/java/com/example/employee/
├── controller/EmployeeControllerWebTest.java              ← 修改写请求
├── integration/EmployeeCrudIntegrationTest.java            ← 修改写请求
src/test/java/com/example/employee/security/SessionLoginTest.java ← 新建
src/test/resources/test-data.sql                            ← 完整替换
```

### 1. 完整替换pom.xml

完整内容如下：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>4.1.1</version>
        <relativePath/>
    </parent>
    <groupId>com.example</groupId>
    <artifactId>employee-management-api</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <name>employee-management-api</name>
    <description>Employee management REST API</description>
    <properties>
        <java.version>17</java.version>
    </properties>
    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-webmvc</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
        <dependency>
            <groupId>org.mybatis.spring.boot</groupId>
            <artifactId>mybatis-spring-boot-starter</artifactId>
            <version>4.1.0</version>
        </dependency>
        <dependency>
            <groupId>com.mysql</groupId>
            <artifactId>mysql-connector-j</artifactId>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-security</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-webmvc-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-security-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

第一项把Spring Security加入正式应用；第二项是Boot 4的Security测试Starter，给测试提供 `csrf()` 等安全请求工具。版本由Spring Boot 4.1.1统一管理，不单独填写。

### 2. 在MySQL 8.0创建账号表

只在个人练习数据库执行。`password_hash` 保存单向哈希，不保存原始密码：

```sql
USE employee_db;

CREATE TABLE app_users (
    id BIGINT NOT NULL AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    employee_id BIGINT NULL,
    role VARCHAR(30) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT pk_app_users PRIMARY KEY (id),
    CONSTRAINT uk_app_users_username UNIQUE (username),
    CONSTRAINT uk_app_users_employee_id UNIQUE (employee_id)
);
```

账号是“能否登录系统”的安全数据，员工是业务数据。账号可以暂时不关联员工，员工离职也不应直接等同于删除账号；停用账号由 `enabled` 表达。当前员工采用物理删除，因此本章不虚构外键删除规则。

先在本机临时工具中用课程项目的 `PasswordEncoder` 生成哈希，再把输出写入练习库。不要把命令中的练习密码替换成真实密码：

```java
String hash = passwordEncoder.encode("TrainingPass123!");
System.out.println(hash);
```

```sql
INSERT INTO app_users (
    username, password_hash, employee_id, role, enabled
) VALUES (
    'tanaka',
    '把上一步生成的bcrypt哈希粘贴到这里',
    1001,
    'USER',
    TRUE
);
```

BCrypt每次使用随机salt，同一密码产生不同哈希是正常现象；验证应调用 `matches()`，不能重新encode后比较两个字符串。

### 3. 新建AppUser.java

```java
package com.example.employee.entity;

public class AppUser {
    private Long id;
    private String username;
    private String passwordHash;
    private Long employeeId;
    private String role;
    private boolean enabled;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public Long getEmployeeId() { return employeeId; }
    public void setEmployeeId(Long employeeId) { this.employeeId = employeeId; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
}
```

### 4. 新建AppUserMapper.java和XML

```java
package com.example.employee.mapper;

import com.example.employee.entity.AppUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AppUserMapper {
    AppUser findByUsername(@Param("username") String username);
}
```

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "https://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.example.employee.mapper.AppUserMapper">
    <select id="findByUsername" resultType="AppUser">
        SELECT id, username, password_hash, employee_id, role, enabled
        FROM app_users
        WHERE username = #{username}
    </select>
</mapper>
```

用户名使用 `#{username}` 参数绑定。查询只允许返回唯一的一行，唯一约束同时阻止两个账号使用同一用户名。

### 5. 新建LoginRequest.java和响应类

```java
package com.example.employee.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class LoginRequest {
    @NotBlank(message = "用户名不能为空")
    @Size(max = 50, message = "用户名不能超过50个字符")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(max = 200, message = "密码不能超过200个字符")
    private String password;

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
```

```java
package com.example.employee.dto.response;

public record CurrentUserResponse(String username) {
}
```

```java
package com.example.employee.dto.response;

public record CsrfResponse(
        String parameterName,
        String headerName,
        String token) {
}
```

`record` 是Java 17的数据载体写法，构造参数同时定义不可重新赋值的组件和同名访问方法。本章响应只有少量只读字段，因此使用record；请求对象仍保留普通类和setter供JSON绑定。

### 6. 新建AppUserDetailsService.java

```java
package com.example.employee.security;

import com.example.employee.entity.AppUser;
import com.example.employee.mapper.AppUserMapper;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class AppUserDetailsService implements UserDetailsService {
    private final AppUserMapper appUserMapper;

    public AppUserDetailsService(AppUserMapper appUserMapper) {
        this.appUserMapper = appUserMapper;
    }

    @Override
    public UserDetails loadUserByUsername(String username) {
        AppUser appUser = appUserMapper.findByUsername(username);
        if (appUser == null) {
            throw new UsernameNotFoundException("login failed");
        }
        return User.withUsername(appUser.getUsername())
                .password(appUser.getPasswordHash())
                .roles(appUser.getRole())
                .disabled(!appUser.isEnabled())
                .build();
    }
}
```

`UserDetailsService` 只负责按用户名加载账号；它不接收HTTP请求，也不自己比较密码。返回的 `UserDetails` 交给认证组件，异常消息使用统一文字，避免泄露账号是否存在。

### 7. 新建SecurityConfig.java

```java
package com.example.employee.config;

import com.example.employee.security.AppUserDetailsService;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import org.springframework.security.web.authentication.session.CompositeSessionAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfAuthenticationStrategy;
import org.springframework.security.web.csrf.CsrfTokenRepository;

@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }

    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    public CsrfTokenRepository csrfTokenRepository() {
        return new CookieCsrfTokenRepository();
    }

    @Bean
    public SessionAuthenticationStrategy sessionAuthenticationStrategy(
            CsrfTokenRepository csrfTokenRepository) {
        return new CompositeSessionAuthenticationStrategy(List.of(
                new ChangeSessionIdAuthenticationStrategy(),
                new CsrfAuthenticationStrategy(csrfTokenRepository)));
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AppUserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider =
                new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(provider);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            SecurityContextRepository repository,
            CsrfTokenRepository csrfTokenRepository)
            throws Exception {
        http
                .csrf(csrf -> csrf.csrfTokenRepository(csrfTokenRepository))
                .securityContext(context -> context
                        .requireExplicitSave(true)
                        .securityContextRepository(repository))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/auth/me", "/employees", "/employees/**")
                        .authenticated()
                        .anyRequest().permitAll())
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, exception) ->
                                response.sendError(
                                        HttpServletResponse.SC_UNAUTHORIZED)))
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(logout -> logout
                        .logoutUrl("/auth/logout")
                        .deleteCookies("JSESSIONID")
                        .logoutSuccessHandler((request, response, authentication) ->
                                response.setStatus(204)));
        return http.build();
    }
}
```

### 8. 新建AuthController.java

```java
package com.example.employee.controller;

import com.example.employee.common.ApiResponse;
import com.example.employee.dto.request.LoginRequest;
import com.example.employee.dto.response.CsrfResponse;
import com.example.employee.dto.response.CurrentUserResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository contextRepository;
    private final SessionAuthenticationStrategy sessionStrategy;

    public AuthController(
            AuthenticationManager authenticationManager,
            SecurityContextRepository contextRepository,
            SessionAuthenticationStrategy sessionStrategy) {
        this.authenticationManager = authenticationManager;
        this.contextRepository = contextRepository;
        this.sessionStrategy = sessionStrategy;
    }

    @GetMapping("/csrf")
    public ResponseEntity<ApiResponse<CsrfResponse>> csrf(CsrfToken token) {
        return ResponseEntity.ok(ApiResponse.success(new CsrfResponse(
                token.getParameterName(), token.getHeaderName(), token.getToken())));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<CurrentUserResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        Authentication input =
                UsernamePasswordAuthenticationToken.unauthenticated(
                        request.getUsername().trim(), request.getPassword());
        Authentication result = authenticationManager.authenticate(input);
        sessionStrategy.onAuthentication(result, httpRequest, httpResponse);

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(result);
        SecurityContextHolder.setContext(context);
        contextRepository.saveContext(context, httpRequest, httpResponse);

        return ResponseEntity.ok(ApiResponse.success(
                new CurrentUserResponse(result.getName())));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<CurrentUserResponse>> me(
            Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(
                new CurrentUserResponse(authentication.getName())));
    }
}
```

退出由Security过滤器处理，不再写一个只删除自定义属性的假logout方法。

### 9. 新建AuthExceptionHandler.java

```java
package com.example.employee.exception;

import com.example.employee.common.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class AuthExceptionHandler {
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthentication(
            AuthenticationException exception) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.failure("用户名或密码错误"));
    }
}
```

该处理器只能处理已经进入Controller的登录异常。未登录访问 `/auth/me` 和CSRF失败发生在过滤器链中，不经过ControllerAdvice；它们由Spring Security直接返回401或403。

### 10. 完整替换test-data.sql

下面哈希对应测试专用密码 `TrainingPass123!`，不得用于真实账号。完整内容如下：

```sql
DROP TABLE IF EXISTS app_users;
DROP TABLE IF EXISTS employee_change_logs;
DROP TABLE IF EXISTS employees;

CREATE TABLE employees (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    department VARCHAR(50) NOT NULL,
    email VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_employees_email UNIQUE (email)
);

CREATE TABLE employee_change_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    action VARCHAR(30) NOT NULL,
    detail VARCHAR(200) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE app_users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    employee_id BIGINT UNIQUE,
    role VARCHAR(30) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE
);

INSERT INTO employees (
    id, name, department, email, status
) VALUES
    (1001, 'Tanaka', 'Sales', 'tanaka@example.com', 'ACTIVE'),
    (1002, 'Sato', 'Development', 'sato@example.com', 'ACTIVE');

INSERT INTO app_users (
    id, username, password_hash, employee_id, role, enabled
) VALUES (
    1,
    'tanaka',
    '$2a$10$nljJFXRpHlZ2gPsH7lM12eIan96c0Y03Y/YCucFA/SA6z.1Lv7UTC',
    1001,
    'USER',
    TRUE
);
```

### 11. 为既有写请求测试加入CSRF

启用Spring Security后，`GET /employees`、`GET /employees/{id}` 及员工写接口都需要认证；原有POST、PUT、DELETE测试若没有CSRF令牌还会得到403。`EmployeeControllerWebTest`、`EmployeeCrudIntegrationTest` 和 `EmployeeSearchIntegrationTest` 的原有业务断言仍应保留，但须明确测试身份。三个类都增加 `WithMockUser` 导入及类级 `@WithMockUser(username = "employee-test-user")`；这只供旧业务回归测试使用，不代替本章的真实登录测试。两个包含员工写请求的类还须增加静态import：

```java
import static org.springframework.security.test.web.servlet.request
        .SecurityMockMvcRequestPostProcessors.csrf;
```

然后给两个类中的每一个写请求加入 `.with(csrf())`。GET请求不用CSRF，但仍需要上述测试身份：

```java
mockMvc.perform(post("/employees")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson));

mockMvc.perform(put("/employees/{id}", employeeId)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateJson));

mockMvc.perform(delete("/employees/{id}", employeeId)
                .with(csrf()));
```

这不是关闭安全检查，而是让自动化测试具备已认证身份和有效令牌。第13章的两条事务PUT测试也位于 `EmployeeCrudIntegrationTest` 中，必须一起修改。第14章的 `EmployeeSearchIntegrationTest` 只有GET，增加测试身份即可。若把这些修改实际写入测试工程，应逐个运行旧测试，不能仅凭文档声称回归通过。

`@WebMvcTest` 只加载指定的Web切片，不会按完整应用方式加载本章的 `SecurityConfig`，Spring Boot测试支持会为切片提供默认安全配置。因此 `EmployeeControllerWebTest` 的类注解可写成：

```java
import org.springframework.security.test.context.support.WithMockUser;

@WebMvcTest(EmployeeController.class)
@ActiveProfiles("test")
@WithMockUser(username = "web-test-user")
class EmployeeControllerWebTest {
    // 保留原有三个测试
}
```

`@WithMockUser` 只建立测试用SecurityContext，不执行数据库登录；它适合本来只验证Controller输入输出的切片测试。`SessionLoginTest` 必须走真实AuthenticationManager，不能用它代替登录验证。

两个集成测试类也分别在文件顶部导入 `org.springframework.security.test.context.support.WithMockUser`，并在现有类声明前追加 `@WithMockUser(username = "employee-test-user")`；不要删除原有的 `@SpringBootTest`、`@AutoConfigureMockMvc`、`@ActiveProfiles` 和 `@Sql`。这样第12～14章的业务测试仍验证原契约，而下方 `SessionLoginTest` 另行验证真实账号查询、密码和Session恢复。

### 12. 新建SessionLoginTest.java

```java
package com.example.employee.security;

import com.jayway.jsonpath.JsonPath;
import com.example.employee.support.MySqlTestGuard;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Sql(scripts = "/test-data.sql",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class SessionLoginTest extends MySqlTestGuard {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void loginSessionIdentifiesCurrentUser() throws Exception {
        MvcResult login = mockMvc.perform(post("/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"tanaka\","
                                + "\"password\":\"TrainingPass123!\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("tanaka"))
                .andReturn();

        MockHttpSession session =
                (MockHttpSession) login.getRequest().getSession(false);
        assertThat(session).isNotNull();

        mockMvc.perform(get("/auth/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("tanaka"));
        mockMvc.perform(get("/employees/1001").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1001));
    }

    @Test
    void loginClearsPreLoginCsrfCookieAndRequiresFreshToken()
            throws Exception {
        MvcResult beforeLogin = mockMvc.perform(get("/auth/csrf"))
                .andExpect(status().isOk())
                .andReturn();
        Cookie oldCookie = beforeLogin.getResponse().getCookie("XSRF-TOKEN");
        assertThat(oldCookie).isNotNull();
        String oldToken = JsonPath.read(
                beforeLogin.getResponse().getContentAsString(),
                "$.data.token");

        MvcResult login = mockMvc.perform(post("/auth/login")
                        .cookie(oldCookie)
                        .header("X-XSRF-TOKEN", oldToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"tanaka\","
                                + "\"password\":\"TrainingPass123!\"}"))
                .andExpect(status().isOk())
                .andReturn();
        Cookie clearedCookie = login.getResponse().getCookie("XSRF-TOKEN");
        assertThat(clearedCookie).isNotNull();
        assertThat(clearedCookie.getMaxAge()).isZero();
        MockHttpSession session =
                (MockHttpSession) login.getRequest().getSession(false);
        assertThat(session).isNotNull();

        // 模拟客户端接受了清除Cookie的响应：不再发送oldCookie。
        mockMvc.perform(post("/auth/logout")
                        .session(session)
                        .header("X-XSRF-TOKEN", oldToken))
                .andExpect(status().isForbidden());
        assertThat(session.isInvalid()).isFalse();

        MvcResult refreshed = mockMvc.perform(get("/auth/csrf")
                        .session(session))
                .andExpect(status().isOk())
                .andReturn();
        Cookie freshCookie = refreshed.getResponse().getCookie("XSRF-TOKEN");
        assertThat(freshCookie).isNotNull();
        String freshToken = JsonPath.read(
                refreshed.getResponse().getContentAsString(),
                "$.data.token");
        mockMvc.perform(post("/auth/logout")
                        .session(session)
                        .cookie(freshCookie)
                        .header("X-XSRF-TOKEN", freshToken))
                .andExpect(status().isNoContent());
        assertThat(session.isInvalid()).isTrue();
    }

    @Test
    void wrongPasswordReturnsSame401Message() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"tanaka\","
                                + "\"password\":\"wrong-password\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message")
                        .value("用户名或密码错误"));
    }

    @Test
    void currentUserWithoutSessionReturns401() throws Exception {
        mockMvc.perform(get("/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void employeeApiRequiresLoginAndWriteRequiresCsrf() throws Exception {
        mockMvc.perform(get("/employees/1001"))
                .andExpect(status().isUnauthorized());

        MvcResult login = mockMvc.perform(post("/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"tanaka\","
                                + "\"password\":\"TrainingPass123!\"}"))
                .andExpect(status().isOk())
                .andReturn();
        MockHttpSession session =
                (MockHttpSession) login.getRequest().getSession(false);
        assertThat(session).isNotNull();

        mockMvc.perform(get("/employees/1001").session(session))
                .andExpect(status().isOk());

        String createJson = "{\"name\":\"Sato\","
                + "\"department\":\"Sales\","
                + "\"email\":\"session-new-employee@example.com\"}";
        assertThat(countByEmail("session-new-employee@example.com"))
                .isZero();
        mockMvc.perform(post("/employees")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createJson))
                .andExpect(status().isForbidden());
        assertThat(countByEmail("session-new-employee@example.com"))
                .isZero();
        mockMvc.perform(post("/employees")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                .content(createJson))
                .andExpect(status().isCreated());
        assertThat(countByEmail("session-new-employee@example.com"))
                .isEqualTo(1);
    }

    @Test
    void loginWithoutCsrfReturns403() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"tanaka\","
                                + "\"password\":\"TrainingPass123!\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void logoutInvalidatesAuthenticatedSession() throws Exception {
        MvcResult login = mockMvc.perform(post("/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"tanaka\","
                                + "\"password\":\"TrainingPass123!\"}"))
                .andExpect(status().isOk())
                .andReturn();
        MockHttpSession session =
                (MockHttpSession) login.getRequest().getSession(false);
        assertThat(session).isNotNull();

        mockMvc.perform(get("/auth/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("tanaka"));

        mockMvc.perform(post("/auth/logout")
                        .session(session)
                        .with(csrf()))
                .andExpect(status().isNoContent());
        assertThat(session.isInvalid()).isTrue();
        mockMvc.perform(get("/auth/me"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/employees/1001"))
                .andExpect(status().isUnauthorized());
    }

    private int countByEmail(String email) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM employees WHERE email = ?",
                Integer.class, email);
        return count == null ? 0 : count;
    }
}
```

上述测试的创建邮箱不在 `test-data.sql` 的两条员工记录中。`JdbcTemplate` 在请求前、缺CSRF后、成功创建后分别读取数据库件数，避免只看HTTP状态而漏掉持久化结果。令牌更新测试先取得登录前的CSRF Cookie与响应体令牌；登录响应应清除旧Cookie。模拟客户端接受清除响应后，旧请求头单独发起logout会403，重新GET令牌并携带新Cookie与请求头才会204。`JsonPath.read(..., "$.data.token")` 只从测试响应中取令牌，不把值打印或保存到日志。`MockMvc` 中的 `MockHttpSession` 是测试对象；退出后检查它已失效，并让新的受保护请求得到401。旧 `JSESSIONID` 在真实Servlet容器中的重送效果还需按前面的Postman退出步骤验证，不能把MockMvc新请求冒充为旧Cookie重送。

## 十四、本章稳定状态

完成后，项目能够使用数据库账号和BCrypt真实登录，通过服务器Session识别当前用户，并安全退出。你应能够解释Filter、SecurityFilterChain、Bean注册、账号加载、密码验证、Authentication、SecurityContext、Session、Cookie和CSRF在同一流程中的职责。

员工接口本章要求登录，但尚未区分USER与ADMIN的操作权限或本人数据范围。第16章将按权限矩阵限制不同角色能够访问的HTTP方法和数据。
