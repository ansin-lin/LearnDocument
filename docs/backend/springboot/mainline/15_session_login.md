# 第15章 Spring Security认证与Session登录

> 从匿名请求到可跨HTTP请求恢复的登录状态。本章在第14章的Employee Management工程上增加数据库账号、JSON登录、Session、CSRF和退出；第16章才收紧USER/ADMIN权限与本人数据范围。技术基线为Java 17、Spring Boot 4.1.1、MyBatis Starter 4.1.0、MySQL 8.0。

本章每个阶段沿用上一阶段的文件，不重新建工程。详细认证机制见[A15-1](../appendix/A15_01_spring_security_authentication.md)，Session与CSRF原理见[A15-2](../appendix/A15_02_session_cookie_csrf.md)；二者是深入阅读，不是完成当前操作的前置条件。最终文件核对见[R15](../reference/R15_session_login_complete_source.md)。

## 一、阶段15-01：为什么一次登录不能自动延续到下一次请求

**本阶段要解决的问题：** 用户提交一次正确密码后，第二次HTTP请求为什么还需要识别身份？

认证（Authentication）回答“你是谁”；授权（Authorization）回答“你能做什么”。本章先实现前者，只要求员工接口已登录，第16章再决定USER和ADMIN的操作范围。HTTP请求本身不会保留上一次Controller方法里的Java变量：

```text
客户端发送请求 → 服务器处理 → 返回响应
下一次请求重新到达服务器，不会自动继承前一次的Java局部变量
```

服务器的Session保存登录后的认证状态，客户端Cookie保存会话标识。`JSESSIONID`是随机会话标识，不是用户名、密码或完整用户对象：

```text
POST登录 → 服务器验证账号密码 → 把认证状态保存到Session
     → 响应Set-Cookie: JSESSIONID=随机标识
后续请求带Cookie → 服务器找到Session → 恢复当前用户
```

本阶段不创建`AuthenticationManager`或Session策略。先在Postman连续发两次现有`GET /health`（无参数、无请求体，预期200与`OK`），观察它们是两个独立请求；员工接口暂时仍按第14章行为工作。

**教师讲解重点：** 认证和授权是两个问题；Cookie在客户端、Session在服务器；JSESSIONID不含密码。

**学员必须完成的操作：** 画出上面两条请求链，指出第二次请求要带回什么。

**常见错误：** 把Controller的局部变量当作可跨请求保存的登录状态。

**本阶段完成标准：** 能解释为何还需要Session与Cookie；工程无改动。

## 二、阶段15-02：加入Security并观察匿名401

**本阶段要解决的问题：** 员工详情请求怎样在进入Controller前要求登录？

`spring-boot-starter-security`提供安全过滤链。`SecurityFilterChain`是过滤链配置结果；`HttpSecurity`在Bean方法中构造它。`@Configuration`把类标记为Spring配置，`@Bean`把方法返回对象交给容器。`authenticated()`要求已有认证身份，`permitAll()`放行匹配请求。401表示当前请求尚无可用身份；关闭默认表单和HTTP Basic后，REST请求不会被引导到登录页面。

**文件变化：**

- 修改：项目根`pom.xml`的`<dependencies>`中追加下列两项，其他依赖和第13～14章业务代码不删。
- 新增：`src/main/java/com/example/employee/config/SecurityConfig.java`。
- 保持不变：员工Controller、Service、Mapper、事务和分页。

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

版本由Boot 4.1.1管理。Eclipse刷新Maven依赖，再新建完整配置类：

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

`requestMatchers`选择路径；员工请求先经Security过滤器，再可能进入Controller。`anyRequest().permitAll()`暂放行健康接口；POST/PUT/DELETE仍受框架默认CSRF检查，放行URL不等于关闭CSRF。

Eclipse用“Run As → Spring Boot App”启动类；若菜单没有该项，用“Run As → Java Application”。Postman清空旧Cookie，发送`GET http://localhost:8080/employees/1001`（无参数、无请求体），预期401；`GET /health`预期200与`OK`。在员工Controller首行打断点，匿名详情请求不应命中。

**教师讲解重点：** Filter先于Controller；`@Bean`由Spring调用；401不是404。

**学员必须完成的操作：** 写配置类、运行项目、保存两条请求的状态证据。

**常见错误：** 依赖未刷新；期待框架默认表单登录返回JSON。

**本阶段完成标准：** 员工详情匿名401、健康接口200；此时还没有自定义登录入口。

## 三、阶段15-03：建立数据库账号与密码验证能力

**本阶段要解决的问题：** 系统从哪里找到账号，怎样确认密码而不保存明文？

`app_users`是MySQL 8.0账号表；`AppUser`是表数据对象。MyBatis的`AppUserMapper`按用户名查询。Spring Security的`UserDetailsService`负责把表对象转换成认证资料`UserDetails`，不自行比较密码；`PasswordEncoder`接口的BCrypt实现执行哈希与`matches(raw, encoded)`。`DaoAuthenticationProvider`在下一阶段把查询与密码比较连起来。数据库的`role`暂存`USER`，第16章才用来授权。

**文件变化：**

- 新增：个人库`app_users`表、`entity/AppUser.java`、`mapper/AppUserMapper.java`及XML、`security/AppUserDetailsService.java`。
- 修改：`SecurityConfig.java`追加`PasswordEncoder` Bean；专用测试库的`src/test/resources/test-data.sql`增加账号表与测试账号。
- 保持不变：第14章员工表、事务、搜索SQL及当前Security过滤链。

仅在个人练习库确认`employee_db`后执行：

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

`username`唯一；`password_hash`只存单向哈希；`employee_id`与业务员工关联但本章不做本人授权；`enabled=false`表示不可登录。当前员工允许物理删除，本章不擅自加外键删除规则。新建完整实体：

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

新增Mapper接口及独立XML；`#{username}`是参数绑定，沿用第9章下划线转驼峰设置：

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

新增账号加载服务：

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

`@Service`使Spring创建加载器；认证Provider以后调用`loadUserByUsername`。`roles("USER")`生成`ROLE_USER`，数据库值不带`ROLE_`前缀。`SecurityConfig.java`顶部新增以下导入，并在类内追加Bean，不替换15-02的`securityFilterChain`：

```java
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(10);
}
```

这段是**导入与Bean增量**：两个`import`放在`package`后，Bean方法放在类大括号内。个人练习库先插入本章账号（只用于本地教学）：

```sql
INSERT INTO app_users
    (username, password_hash, employee_id, role, enabled)
VALUES
    ('tanaka',
     '$2a$10$nljJFXRpHlZ2gPsH7lM12eIan96c0Y03Y/YCucFA/SA6z.1Lv7UTC',
     1001, 'USER', TRUE);
```

执行前先确认1001员工存在、`tanaka`账号尚不存在。专用测试库的`src/test/resources/test-data.sql`中，先于既有`DROP`加入`DROP TABLE IF EXISTS app_users;`；在员工表建立后追加下面完整表定义，在既有员工初始化后追加上面的`INSERT`。**这些DROP只允许在第12章指定的专用MySQL测试库执行，绝不在个人练习库或共享库执行。**

```sql
CREATE TABLE app_users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    employee_id BIGINT UNIQUE,
    role VARCHAR(30) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE
);
```

先在数据库客户端核对`SELECT username, employee_id, role, enabled FROM app_users WHERE username = 'tanaka';`应只有一行。然后在Eclipse分别运行下面两个测试：第一个沿用第12章专用MySQL测试库与`MySqlTestGuard`，真正执行Mapper SQL；第二个不启动Spring上下文，只检查样例哈希。此处不需要提前复制R15终版文件。

```java
package com.example.employee.security;

import static org.assertj.core.api.Assertions.assertThat;
import com.example.employee.entity.AppUser;
import com.example.employee.mapper.AppUserMapper;
import com.example.employee.support.MySqlTestGuard;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

@SpringBootTest
@ActiveProfiles("test")
@Sql(scripts = "/test-data.sql",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class AppUserMapperTest extends MySqlTestGuard {
    @Autowired AppUserMapper appUserMapper;

    @Test
    void findsKnownAccountAndNotUnknownAccount() {
        AppUser user = appUserMapper.findByUsername("tanaka");
        assertThat(user).isNotNull();
        assertThat(user.getEmployeeId()).isEqualTo(1001L);
        assertThat(appUserMapper.findByUsername("missing")).isNull();
    }
}
```

```java
package com.example.employee.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class PasswordHashTest {
    private static final String HASH =
            "$2a$10$nljJFXRpHlZ2gPsH7lM12eIan96c0Y03Y/YCucFA/SA6z.1Lv7UTC";

    @Test
    void sampleHashMatchesOnlyCorrectPassword() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        assertTrue(encoder.matches("TrainingPass123!", HASH));
        assertFalse(encoder.matches("wrong-password", HASH));
    }
}
```

生成其他练习账号的哈希可用Eclipse临时JUnit方法调用`encode(...)`；插入后删掉临时打印代码，不记录真实密码或哈希到截图。第15章最终完整测试脚本仍可在[R15](../reference/R15_session_login_complete_source.md)核对。

**教师讲解重点：** `AppUser`与`UserDetails`职责不同；BCrypt不能“解密”；加载器不自行验证密码。

**学员必须完成的操作：** 建表、编写四个文件及Bean、在Eclipse运行Mapper和密码匹配测试。

**常见错误：** 把明文写进`password_hash`；把`ROLE_`重复存入`role`。

**本阶段完成标准：** 可查练习账号，正确/错误密码匹配结果不同；此时尚无登录接口，员工匿名请求仍401。

## 四、阶段15-04：先完成一次JSON认证

**本阶段要解决的问题：** 正确密码能否被认证组件确认？“认证成功”是否已经等于“后续请求已登录”？

`Authentication`表示认证输入或成功结果；`AuthenticationManager.authenticate`负责交给合适Provider，当前`ProviderManager`使用`DaoAuthenticationProvider`。`UsernamePasswordAuthenticationToken.unauthenticated(...)`只是装载输入，不能自行证明身份。失败会抛`AuthenticationException`，对外统一401。此阶段**故意不保存SecurityContext**，用下一次`/auth/me`的401观察缺口；不是第15章交付状态。

**文件变化：**

- 修改：`SecurityConfig.java`追加认证Bean，保留15-03已有的密码Bean和过滤链；过滤链中把`/auth/me`也加入`authenticated()`匹配。
- 新增：`LoginRequest.java`、`CurrentUserResponse.java`、`CsrfResponse.java`、`AuthController.java`、`AuthExceptionHandler.java`。
- 保持不变：账号表、Mapper、Service、员工业务代码。

`SecurityConfig.java`顶部增加下列导入，类内追加Bean，并把原过滤链`requestMatchers("/employees", "/employees/**")`改为`requestMatchers("/auth/me", "/employees", "/employees/**")`：

```java
import com.example.employee.security.AppUserDetailsService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;

@Bean
public AuthenticationManager authenticationManager(
        AppUserDetailsService users, PasswordEncoder encoder) {
    DaoAuthenticationProvider provider = new DaoAuthenticationProvider(users);
    provider.setPasswordEncoder(encoder);
    return new ProviderManager(provider);
}
```

以下每个代码块分别是一个**完整新文件**。请求对象用`@NotBlank`与`@Size`校验，`@Valid`在Controller调用前触发；响应`record`只公开用户名或令牌资料：

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
public record CsrfResponse(String parameterName, String headerName, String token) {}
```

```java
package com.example.employee.controller;

import com.example.employee.common.ApiResponse;
import com.example.employee.dto.request.LoginRequest;
import com.example.employee.dto.response.CsrfResponse;
import com.example.employee.dto.response.CurrentUserResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
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
    public AuthController(AuthenticationManager authenticationManager) {
        this.authenticationManager = authenticationManager;
    }
    @GetMapping("/csrf")
    public ResponseEntity<ApiResponse<CsrfResponse>> csrf(CsrfToken token) {
        return ResponseEntity.ok(ApiResponse.success(new CsrfResponse(
                token.getParameterName(), token.getHeaderName(), token.getToken())));
    }
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<CurrentUserResponse>> login(
            @Valid @RequestBody LoginRequest request) {
        Authentication input = UsernamePasswordAuthenticationToken.unauthenticated(
                request.getUsername().trim(), request.getPassword());
        Authentication result = authenticationManager.authenticate(input);
        // 临时实验：尚未把result保存到Session；15-05完整替换此方法。
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

`/auth/csrf`读取Spring Security提供的`CsrfToken`；POST登录仍受默认CSRF保护。本阶段默认令牌仓库存于HttpSession；15-06才改为Cookie仓库。新增统一登录异常处理器：

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

在Eclipse重启。Postman先`GET /auth/csrf`（无参数/请求体，预期200，保存令牌及Cookie），再`POST /auth/login`（JSON `{"username":"tanaka","password":"TrainingPass123!"}`；以响应`headerName`为请求头名、`token`为值，保留Cookie；预期200、`data.username=tanaka`）。错误密码用同样CSRF流程预期401统一消息；缺令牌登录预期403。紧接着`GET /auth/me`（无请求体）预期401：**这正是本阶段要观察的受控失败**。测试后不用恢复文件，15-05直接增量改进。

**教师讲解重点：** `authenticate`只确认这次输入；401与403来源不同；200并不证明Session登录。

**学员必须完成的操作：** 编写文件，在Eclipse调试认证结果，保存200/401/403和下一请求401证据。

**常见错误：** 为了临时实验关掉CSRF；将密码或完整`Authentication`返回JSON。

**本阶段完成标准：** 正确账号认证200、错误密码401、缺令牌403；`/auth/me`仍401。

## 五、阶段15-05：保存SecurityContext，让下一请求恢复身份

**本阶段要解决的问题：** 如何把15-04的一次性认证结果变成真实Session登录？

`SecurityContext`装载已认证的`Authentication`；`SecurityContextHolder`保存当前请求上下文；`SecurityContextRepository`跨请求保存和恢复。本项目使用`HttpSessionSecurityContextRepository`。自定义登录Controller必须显式`saveContext(...)`，并在成功时通过`ChangeSessionIdAuthenticationStrategy`更换会话ID。

```text
AuthController.login → authenticate → 更换Session ID
                     → 创建SecurityContext并设置Authentication
                     → saveContext到HttpSession → 响应Set-Cookie: JSESSIONID
下一请求带JSESSIONID → Security过滤器恢复Authentication
                     → GET /auth/me 返回用户名
```

**文件变化：** 只修改`SecurityConfig.java`与`AuthController.java`；账号、员工业务和15-04 DTO/异常处理器保持不变。

在`SecurityConfig.java`顶部新增以下导入，在类内追加两个Bean；原`PasswordEncoder`和`AuthenticationManager`不删除：

```java
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;

@Bean
public SecurityContextRepository securityContextRepository() {
    return new HttpSessionSecurityContextRepository();
}
@Bean
public SessionAuthenticationStrategy sessionAuthenticationStrategy() {
    return new ChangeSessionIdAuthenticationStrategy();
}
```

把`securityFilterChain`原参数`(HttpSecurity http)`改为`(HttpSecurity http, SecurityContextRepository repository)`，在原链`.authorizeHttpRequests(...)`之前追加：

```java
.securityContext(context -> context
    .requireExplicitSave(true)
    .securityContextRepository(repository))
```

它让过滤器与Controller使用同一Repository；加入时保证调用链仍是`http.securityContext(...).authorizeHttpRequests(...)`，不创建第二个过滤链。`AuthController.java`顶部新增导入：

```java
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
```

将原仅接收`AuthenticationManager`的字段和构造器**完整替换**为：

```java
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
```

再把15-04的`login(...)`方法**完整替换**为下方方法；`csrf(...)`和`me(...)`保留：

```java
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
```

注意`onAuthentication`先处理会话ID，再`saveContext`；只设置Holder、不保存Repository，下一请求仍401。Eclipse重启后按15-04方法取得CSRF并登录，检查登录响应的`Set-Cookie`；同一Postman Cookie Jar发送`GET /auth/me`（无请求体），预期200与`data.username=tanaka`；清除JSESSIONID后再发预期401；重新登录后再次200。当前仍使用默认CSRF仓库，下一阶段换Cookie仓库。

**教师讲解重点：** 当前请求与跨请求不同；`saveContext`是关键；更换ID不是把密码写入Cookie。

**学员必须完成的操作：** 修改两文件、分别测试同Cookie和无Cookie的`/auth/me`。

**常见错误：** 只得到`Authentication`就认为已登录；配置Bean后忘记在Controller调用策略。

**本阶段完成标准：** 登录200，同Cookie的下一请求`/auth/me`200，移除Cookie后401。

## 六、阶段15-06：保留CSRF并验证受保护的写请求

**本阶段要解决的问题：** 已登录浏览器会自动带Cookie，怎样阻止其他页面借这个Cookie发起写请求？

CSRF令牌与JSESSIONID职责不同。`CookieCsrfTokenRepository`把令牌存在`XSRF-TOKEN` Cookie；`GET /auth/csrf`把当前令牌及请求头名返回给Postman。仅自动带令牌Cookie还不够，写请求必须主动在指定Header提交值。GET通常不改状态，本项目POST/PUT/DELETE要检查令牌。登录成功会清除登录前令牌，客户端必须再取新的令牌。

```text
GET /auth/csrf → XSRF-TOKEN Cookie＋响应体token
POST /auth/login → 同一Cookie＋CSRF请求头
登录成功 → 更换Session ID＋清除旧CSRF Cookie
再次GET /auth/csrf → 新令牌
员工POST/PUT/DELETE → 登录Session＋当前令牌请求头
```

**文件变化：** 只修改`SecurityConfig.java`；15-05的登录Controller不变。先在文件顶部加导入：

```java
import java.util.List;
import org.springframework.security.web.authentication.session.CompositeSessionAuthenticationStrategy;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfAuthenticationStrategy;
import org.springframework.security.web.csrf.CsrfTokenRepository;
```

类内追加`csrfTokenRepository()`，并**完整替换**15-05的`sessionAuthenticationStrategy()`，仍保留同一个Bean名称：

```java
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
```

把`securityFilterChain`参数扩为`(HttpSecurity http, SecurityContextRepository repository, CsrfTokenRepository csrfTokenRepository)`，在现有`.securityContext(...)`之前增加`.csrf(csrf -> csrf.csrfTokenRepository(csrfTokenRepository))`。不要删除`.securityContext(...)`、认证Bean或现有URL规则，也不要同时保留旧的单策略Bean。`CsrfAuthenticationStrategy`使用**同一个仓库**清除旧令牌；组合策略由`AuthController.login`的`onAuthentication`调用。

Eclipse运行后，用Postman按下表顺序验证；URL均以`http://localhost:8080`为前缀，未注明参数的请求无查询参数。

| 方法与URL | Cookie、Header和请求体 | 预期状态及响应/数据 |
| --- | --- | --- |
| `POST /auth/login` | 无CSRF；JSON `{"username":"tanaka","password":"TrainingPass123!"}` | 403，未登录 |
| `GET /auth/csrf` | 无请求体 | 200，响应含当前`token`与Header名称，设置令牌Cookie |
| `POST /auth/login` | 同一Cookie Jar、正确CSRF请求头；同上JSON | 200，返回用户名并建立Session |
| `POST /employees` | 登录Session但无CSRF请求头；JSON `{"name":"Csrf Practice","department":"Sales","email":"csrf-practice@example.com"}` | 403，数据库无该邮箱 |
| `GET /auth/csrf` | 登录后的同一Session；无请求体 | 200，取得新令牌 |
| `POST /employees` | 同一Session、当前令牌Cookie与`X-XSRF-TOKEN`请求头；同上员工JSON | 201，数据库新增该邮箱 |

接受登录响应对旧Cookie的清除后再取新令牌；另外用旧令牌单独发写请求应为403。测试邮箱须未被占用，实验后在个人练习库清理这条新增记录；不要把完整令牌保存到证据中。`CookieCsrfTokenRepository`的HttpOnly边界和浏览器差异见[A15-2](../appendix/A15_02_session_cookie_csrf.md)。

**教师讲解重点：** CSRF与角色不是一回事；令牌Cookie与请求头是两处；登录后旧令牌不可长期复用。

**学员必须完成的操作：** 修改一个配置文件，保存403、201及数据库新增/未新增的对照。

**常见错误：** 只发送XSRF-TOKEN Cookie不送Header；用15-05旧令牌测试新配置。

**本阶段完成标准：** 登录与员工写请求都受CSRF保护；当前有效令牌允许合法写入。

## 七、阶段15-07：退出并证明旧Session失效

**本阶段要解决的问题：** 退出是否真的让服务器不再认可旧身份？

Logout由Spring Security的Logout Filter处理，`POST /auth/logout`成功时清除`SecurityContext`、失效`HttpSession`并返回204无正文。不能只在前端清一个“已登录”布尔值。写请求退出仍需当前CSRF令牌。

**文件变化：** 只修改`SecurityConfig.java`的过滤链；AuthController不新建logout方法。在现有`.httpBasic(...)`之后、`return http.build()`之前追加：

```java
.logout(logout -> logout.logoutUrl("/auth/logout")
    .deleteCookies("JSESSIONID")
    .logoutSuccessHandler((request, response, authentication) ->
        response.setStatus(204)))
```

连在原`http`链上，不能另写第二个`SecurityFilterChain` Bean。Eclipse重启并登录，`GET /auth/me`预期200；登录后重新GET CSRF令牌，用同一Session＋当前令牌`POST /auth/logout`（无请求体）预期204；随后`GET /auth/me`预期401。记录退出前旧JSESSIONID，在受控本地实验重送旧Cookie仍应401；仅在Postman删除Cookie不能证明服务器Session失效。令牌和会话标识不要粘贴到共享资料。

**教师讲解重点：** Logout Filter不经过AuthController；204无响应体；必须证明旧Session失效。

**学员必须完成的操作：** 添加配置、执行登录→退出→旧Cookie访问三步。

**常见错误：** 忘记退出请求的CSRF；把Postman删Cookie当成服务器端失效证据。

**本阶段完成标准：** 退出204，旧会话和新的无身份请求访问`/auth/me`均401。

## 八、阶段15-08：JUnit与Postman完成验收

**本阶段要解决的问题：** 如何证明真实账号认证、Session持久化、CSRF和退出都保持可用？

**文件变化：** 新建`src/test/java/com/example/employee/security/SessionLoginTest.java`；为`EmployeeControllerWebTest`、`EmployeeCrudIntegrationTest`、`EmployeeSearchIntegrationTest`提供模拟身份，写请求增加`.with(csrf())`；保留原业务断言与事务、分页测试。专用测试库的最终`test-data.sql`和测试类完整文件在[R15](../reference/R15_session_login_complete_source.md)，此时才用来核对最终状态，不能在15-02先复制其最终SecurityConfig。

本阶段的停用账号用例要求专用测试脚本中确实有`disabled-user`且`enabled=FALSE`。按R15的最终`test-data.sql`核对这条记录，再运行测试；测试方法先查询数据库断言账号存在且停用，然后才以真实JSON登录路径断言401。否则“用户名不存在也返回401”会掩盖停用逻辑未被验证的问题。第16章替换测试脚本时仍须保留这条账号。

`SessionLoginTest`使用`@SpringBootTest`、`@AutoConfigureMockMvc`、`@ActiveProfiles("test")`及`@Sql("/test-data.sql")`，继承第12章的`MySqlTestGuard`。前几项分别加载应用、配置MockMvc、选择专用测试配置、每个方法前准备已知MySQL数据。**测试类和测试方法都不要加`@Transactional`**，以免掩盖真实业务提交或回滚。常用请求处理器`csrf()`仅为MockMvc当前请求提交有效令牌，不证明Postman的真实Cookie生命周期。

先在Eclipse“Run As → JUnit Test”运行一个真实登录方法，再运行整个测试类。至少保留下面这段**测试方法片段**（加入R15给出的完整类、导入和字段中）：

```java
@Test
void loginSessionIdentifiesCurrentUser() throws Exception {
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
}
```

完整类还应分别测试：匿名员工接口401、错误密码401、不存在账号401、停用账号401、缺CSRF的登录403、登录后CSRF令牌更新、写请求缺令牌时数据库不变、Logout后Session失效。`@WithMockUser`只能模拟测试身份，不能替代上述真实登录。运行顺序为“单个方法 → 整个SessionLoginTest → 受影响的旧Controller/CRUD/分页与事务测试”；Maven打包命令仅作为补充，不要求每次手工验证前先运行。

Postman真实验收按下列顺序，用当前Eclipse启动的服务：

| 步骤 | HTTP方法与URL | 参数、请求体和Cookie | 预期状态与响应 |
| --- | --- | --- | --- |
| 1 | `GET http://localhost:8080/auth/csrf` | 无参数/请求体，保留Cookie | 200，`data.headerName`、`data.token`有值 |
| 2 | `POST http://localhost:8080/auth/login` | JSON账号密码、步骤1令牌请求头及Cookie | 200，`data.username=tanaka`、产生JSESSIONID |
| 3 | `GET http://localhost:8080/auth/me` | 无请求体，同一Cookie Jar | 200，`data.username=tanaka` |
| 4 | `GET http://localhost:8080/employees/1001` | 无请求体，同一Cookie | 200，`data.id=1001` |
| 5 | `GET /auth/csrf`后`POST /auth/logout` | 新令牌请求头、同一Cookie，无请求体 | 204，无正文 |
| 6 | `GET http://localhost:8080/auth/me` | 重送旧JSESSIONID或不带Cookie | 401 |

错误密码`POST /auth/login`（同一URL、JSON密码改为错误值、带有效CSRF）预期401；缺令牌的POST登录预期403；已登录但员工POST不带令牌预期403且数据库不变。每次保存脱敏的状态、响应与必要数据库证据。

**教师讲解重点：** 模拟身份测试与真实登录测试分工；HTTP成功要结合Session和数据库；新权限在下一章。

**学员必须完成的操作：** 在Eclipse运行JUnit，再按表用Postman验收并整理证据。

**常见错误：** 用`@WithMockUser`证明密码正确；只看登录200不测下一请求。

**本阶段完成标准：** 真实账号登录、跨请求恢复、CSRF、退出及旧业务回归全部通过；USER/ADMIN角色矩阵仍留给第16章。
