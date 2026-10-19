# R15 第15章Session登录最终源码

本文件对应第15章完成后的稳定状态，不包含第16章的角色限制与本人数据授权。按第15章分阶段完成后，用本文件核对全部文件；它不是第一个阶段的起步代码。

## 最终源码与变更核对

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
    id, username, password_hash, employee_id, role, enabled
) VALUES (
    1, 'tanaka',
    '$2a$10$nljJFXRpHlZ2gPsH7lM12eIan96c0Y03Y/YCucFA/SA6z.1Lv7UTC',
    1001,
    'USER',
    TRUE
),
(
    4,
    'disabled-user',
    '$2a$10$nljJFXRpHlZ2gPsH7lM12eIan96c0Y03Y/YCucFA/SA6z.1Lv7UTC',
    NULL,
    'USER',
    FALSE
);
```

这里把停用账号固定为id=4，使第16章新增的sato、admin可使用id=2、3；若个人练习库已存在tanaka，不要再次执行整段INSERT，应只补尚不存在的停用账号。

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
), (
    4,
    'disabled-user',
    '$2a$10$nljJFXRpHlZ2gPsH7lM12eIan96c0Y03Y/YCucFA/SA6z.1Lv7UTC',
    NULL,
    'USER',
    FALSE
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
    void nonexistentAccountReturnsSame401Message() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"nobody\","
                                + "\"password\":\"TrainingPass123!\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message")
                        .value("用户名或密码错误"));
    }

    @Test
    void disabledAccountReturnsSame401Message() throws Exception {
        Integer accountCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM app_users WHERE username = ?",
                Integer.class, "disabled-user");
        Integer disabledCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM app_users WHERE username = ? AND enabled = FALSE",
                Integer.class, "disabled-user");
        assertThat(accountCount).isEqualTo(1);
        assertThat(disabledCount).isEqualTo(1);

        mockMvc.perform(post("/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"disabled-user\","
                                + "\"password\":\"TrainingPass123!\"}"))
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

### 13. 配置文件与第15章完成核对

本章沿用第14章的`application.yml`、`application-test.yml`、数据源、MyBatis Mapper路径和日志设置，不为登录另建一份重复配置。测试库仍须由第12章的`MySqlTestGuard`限制为专用MySQL库；不得让`test-data.sql`的`DROP TABLE`落到个人或共享库。会话超时、Cookie的Secure/SameSite等配置不是本章新建登录所必需，按部署环境评审，原理见[A15-2](../appendix/A15_02_session_cookie_csrf.md)。

| 核对点 | 第15章末应观察到 |
| --- | --- |
| 匿名员工详情 | 401；未进入Controller业务方法 |
| 正确/错误/不存在/停用账号 | 正确200；其余统一401，不泄露账号是否存在 |
| Session恢复 | 登录响应设置JSESSIONID；同Cookie访问`/auth/me`为200，无Cookie为401 |
| CSRF | 无令牌写请求403；登录后旧令牌失效，新令牌可用 |
| Logout | 204；旧Session ID不能再恢复认证 |
| 员工业务 | 第13章事务、第14章搜索分页和既有CRUD测试保留 |

R15仅是第15章末状态；第16章才加入USER/ADMIN的URL角色矩阵、`@EnableMethodSecurity`与`EmployeeAccess`。
