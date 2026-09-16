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

## 三、完整示例

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
        <version>3.5.16</version>
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
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
        <dependency>
            <groupId>org.mybatis.spring.boot</groupId>
            <artifactId>mybatis-spring-boot-starter</artifactId>
            <version>3.0.5</version>
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
            <groupId>com.h2database</groupId>
            <artifactId>h2</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.springframework.security</groupId>
            <artifactId>spring-security-test</artifactId>
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

第一项把Spring Security加入正式应用；第二项只给测试提供 `csrf()` 等安全请求工具。版本由Spring Boot 3.5.16统一管理，不单独填写。

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
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

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
    public SessionAuthenticationStrategy sessionAuthenticationStrategy() {
        return new ChangeSessionIdAuthenticationStrategy();
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
            SessionAuthenticationStrategy sessionStrategy)
            throws Exception {
        http
                .csrf(csrf -> csrf.csrfTokenRepository(
                        CookieCsrfTokenRepository.withHttpOnlyFalse()))
                .securityContext(context -> context
                        .requireExplicitSave(true)
                        .securityContextRepository(repository))
                .sessionManagement(session -> session
                        .sessionAuthenticationStrategy(sessionStrategy))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/auth/me").authenticated()
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

启用Spring Security后，原有POST、PUT、DELETE测试若没有CSRF令牌会得到403。两个既有测试类都增加静态import：

```java
import static org.springframework.security.test.web.servlet.request
        .SecurityMockMvcRequestPostProcessors.csrf;
```

然后给两个类中的每一个写请求加入 `.with(csrf())`。GET请求不修改：

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

这不是关闭安全检查，而是让自动化测试像合法客户端一样提供有效令牌。第13章的两条事务PUT测试也位于 `EmployeeCrudIntegrationTest` 中，必须一起修改。

`@WebMvcTest` 只加载指定的Web切片，不会按完整应用方式加载本章的 `SecurityConfig`，Spring Boot测试支持会为切片提供默认安全配置。因此 `EmployeeControllerWebTest` 还要增加下面的import和类注解，明确这些用例以已认证测试用户执行：

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

### 12. 新建SessionLoginTest.java

```java
package com.example.employee.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.http.MediaType;
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
class SessionLoginTest {

    @Autowired
    private MockMvc mockMvc;

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

        mockMvc.perform(post("/auth/logout")
                        .session(session)
                        .with(csrf()))
                .andExpect(status().isNoContent());
        assertThat(session.isInvalid()).isTrue();
    }
}
```

## 四、Filter与SecurityFilterChain

Filter是在请求到达Controller之前执行的Servlet组件。Spring Security把多个安全Filter按顺序组成过滤器链，用于加载Session中的认证、检查CSRF、判断访问规则和执行logout。

`SecurityFilterChain` 是这条链的配置结果。`HttpSecurity` 是构建配置的对象，`build()` 返回最终链。应用中不要在每个Controller复制“从Session取用户”的判断，否则容易漏接口、响应不一致，也无法统一处理退出和攻击防护。

## 五、@Configuration与@Bean怎样注册框架对象

`@Configuration` 标记配置类，Spring启动时读取其中的Bean定义。`@Bean` 写在方法上：Spring调用方法，把返回对象注册进容器，默认Bean名是方法名。

| 方法 | 参数来源 | 返回对象 | 用途 |
| --- | --- | --- | --- |
| `passwordEncoder()` | 无 | `PasswordEncoder` | BCrypt哈希与验证 |
| `authenticationManager(...)` | 容器注入两个Bean | `AuthenticationManager` | 组织账号加载和密码校验 |
| `securityFilterChain(...)` | 容器注入安全对象 | `SecurityFilterChain` | 生成Web安全过滤器链 |

这些对象确实由Spring Security运行时使用，不是为了展示注解而虚构的空组件。

## 六、密码如何验证

`BCryptPasswordEncoder(10)` 使用bcrypt和强度10。`encode()`生成单向哈希，`matches(raw, encoded)`验证原始输入是否对应已存哈希。密码不能解密，也不能明文保存或用Base64代替哈希。正式系统需要根据服务器性能测量强度，并规划哈希升级。参考[Spring Security密码存储](https://docs.spring.io/spring-security/reference/6.5/features/authentication/password-storage.html)。

登录过程由 `DaoAuthenticationProvider` 协作完成：调用 `UserDetailsService` 查账号、检查enabled、使用PasswordEncoder比较密码，成功后返回已认证的 `Authentication`。

## 七、Authentication、SecurityContext和Session

`Authentication` 登录前可以承载用户名和密码输入，登录后表示已经确认的用户；返回结果中的凭据会由框架按安全策略处理。`SecurityContext` 保存当前请求的Authentication，`SecurityContextHolder` 让同一请求中的后续代码取得它。

自定义Controller完成登录时，Spring Security 6要求显式调用 `SecurityContextRepository.saveContext()`，否则当前请求看似成功，下一请求却恢复不到用户。官方流程见[认证持久化与Session管理](https://docs.spring.io/spring-security/reference/6.5/servlet/authentication/session-management.html)。

`SessionAuthenticationStrategy` 在认证成功时更换Session ID，防止攻击者预先固定一个会话标识。Cookie只保存随机JSESSIONID，账号信息保存在服务器Session中；客户端不能从Cookie值推算密码。

## 八、CSRF为什么登录和退出也要检查

浏览器会自动携带目标站点Cookie。攻击者可能诱导已登录用户的浏览器提交修改请求，因此使用Session Cookie的应用必须保留CSRF防护。登录本身也可能遭受“把受害者登录到攻击者账号”的login CSRF，退出也会改变安全状态。

客户端先读取 `/auth/csrf`，再把返回token放入返回的headerName所指定请求头。`CookieCsrfTokenRepository.withHttpOnlyFalse()` 还会生成 `XSRF-TOKEN` Cookie，便于浏览器端JavaScript读取；真正的Session Cookie仍应保持HttpOnly。参考[Spring Security CSRF说明](https://docs.spring.io/spring-security/reference/6.5/servlet/exploits/csrf.html)。

## 九、Cookie和会话边界

本地HTTP练习时可观察JSESSIONID；生产环境必须使用HTTPS，并确认Cookie至少具备HttpOnly、Secure及适合业务的SameSite策略。SameSite能降低部分跨站请求风险，但不能替代CSRF令牌。

Session保存在单个应用进程内时，重启会使登录失效，多实例之间也不能自然共享。是否使用Redis等外部Session存储属于部署设计，不能仅修改Cookie解决。

## 十、运行与验证

```powershell
.\mvnw.cmd clean test
```

第14章累计15个测试，本章新增5个，预期20个全部通过。HTTP客户端必须启用Cookie保存；如果每次请求都新建无Cookie会话，`/auth/me` 必然返回401。

验证证据至少包括：取得CSRF、登录响应的Set-Cookie、同一Cookie访问me、错误密码401、缺少CSRF的403、logout 204，以及退出后me重新变为401。不要把完整Cookie或令牌粘贴到共享工单。

## 十一、常见失败

| 现象 | 原因 | 修正 |
| --- | --- | --- |
| 登录200但me仍401 | 只设置SecurityContext，未保存 | 调用SecurityContextRepository保存 |
| 正确密码也失败 | 数据库存了明文或哈希算法不匹配 | 用同一PasswordEncoder生成和matches |
| POST总是403 | 没先取得或携带CSRF令牌 | 保留Cookie并按headerName发送token |
| 每次请求产生新会话 | HTTP客户端没有保存Cookie | 启用Cookie jar并复用同一客户端 |
| 日志出现密码或Session ID | 记录了请求体、Cookie或安全对象 | 删除敏感字段，只记录结果和请求编号 |
| 登录后Session ID不变 | 自定义登录漏掉SessionAuthenticationStrategy | 在保存context前执行策略 |

## 十二、规格理解、影响调查与练习

1. 实现并验证停用账号登录失败，响应不能暴露“账号已停用”。
2. 用同一HTTP客户端完成csrf→login→me→logout→me，保存脱敏证据。
3. Review一个把密码写进日志、用MD5保存、关闭CSRF的实现，逐项说明风险和修正。
4. 调查“会话30分钟无操作后过期”的配置、测试、用户提示和多实例影响，先提交调查表。
5. 为新增账号功能写规格：谁能创建、密码何时hash、重复用户名、初始enabled及不得返回passwordHash。

## 十三、本章稳定状态

完成后，项目能够使用数据库账号和BCrypt真实登录，通过服务器Session识别当前用户，并安全退出。你应能够解释Filter、SecurityFilterChain、Bean注册、账号加载、密码验证、Authentication、SecurityContext、Session、Cookie和CSRF在同一流程中的职责。

员工接口本章仍保持permitAll，不能误认为已经完成权限控制。第16章将先制定权限矩阵，再限制不同角色能够访问的HTTP方法和数据。
