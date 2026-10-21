# R15 第15章JWT Bearer认证最终参考源码

本文件对应第15章稳定状态。沿用第14章员工CRUD、事务和分页；账号来自MySQL，注册固定USER；登录签发15分钟JWT，后续请求以Bearer头访问。第16章再加入角色与本人范围规则。所有片段按路径放入同一工程，不复制第二份同名类。

这是[第15章正文](../mainline/15_session_login.md)完成15-08后的**最终代码快照**，不是15-02临时过滤链或15-04仅注册Controller的阶段代码。正文首次出现的Bean、方法及请求流程应在对应小节学习；这里按文件路径核对最终状态。第16章会完整替换本文件中的`SecurityConfig`，其他注册、登录、密钥和JWT签发类继续使用。

## 1. 完整替换pom.xml

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
            <artifactId>spring-boot-starter-security-oauth2-resource-server</artifactId>
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
        <dependency>
            <groupId>org.mybatis.spring.boot</groupId>
            <artifactId>mybatis-spring-boot-starter-test</artifactId>
            <version>4.1.0</version>
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

`spring-boot-starter-security`提供核心安全功能；`spring-boot-starter-security-oauth2-resource-server`提供Bearer JWT验证及JOSE实现。测试Starter保留第12章自动化测试所需依赖，本章主线只做Postman手工验证。Spring Boot依赖由4.1.1统一管理，MyBatis Starter及其测试Starter沿用4.1.0。

## 2. 在MySQL 8.0创建账号表

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

## 3. 新建AppUser.java

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

## 4. 新建AppUserMapper.java和XML

```java
package com.example.employee.mapper;

import com.example.employee.entity.AppUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AppUserMapper {
    AppUser findByUsername(@Param("username") String username);
    int insert(AppUser account);
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
    <insert id="insert" parameterType="AppUser"
            useGeneratedKeys="true" keyProperty="id">
        INSERT INTO app_users
            (username, password_hash, employee_id, role, enabled)
        VALUES
            (#{username}, #{passwordHash}, #{employeeId}, #{role}, #{enabled})
    </insert>
</mapper>
```

用户名使用 `#{username}` 参数绑定。查询只允许返回唯一的一行；注册时`insert`返回影响行数，并通过`useGeneratedKeys`把新ID写回`AppUser.id`。唯一约束阻止两个账号使用同一用户名。

## 5. 新建注册、登录请求和响应类

```java
package com.example.employee.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class RegisterRequest {
    @NotBlank(message = "用户名不能为空")
    @Size(max = 50, message = "用户名不能超过50个字符")
    @Pattern(regexp = "[A-Za-z0-9._-]+", message = "用户名格式错误")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 12, max = 64, message = "密码长度应为12到64个字符")
    private String password;

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
```

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

public record RegisteredUserResponse(Long id, String username) {
}
```

```java
package com.example.employee.dto.response;

public record CurrentUserResponse(String username) {
}
```

`record` 是Java 17的数据载体写法，构造参数同时定义不可重新赋值的组件和同名访问方法。本章响应只有少量只读字段，因此使用record；请求对象仍保留普通类和setter供JSON绑定。注册请求不包含角色、员工ID和启用状态。

## 5.5 新建注册服务和用户名冲突异常

`src/main/java/com/example/employee/exception/DuplicateUsernameException.java`：

```java
package com.example.employee.exception;

public class DuplicateUsernameException extends RuntimeException {
    public DuplicateUsernameException() {
        super("用户名已存在");
    }
}
```

`src/main/java/com/example/employee/security/AccountRegistrationService.java`：

```java
package com.example.employee.security;

import com.example.employee.dto.request.RegisterRequest;
import com.example.employee.dto.response.RegisteredUserResponse;
import com.example.employee.entity.AppUser;
import com.example.employee.exception.DuplicateUsernameException;
import com.example.employee.mapper.AppUserMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AccountRegistrationService {
    private final AppUserMapper mapper;
    private final PasswordEncoder encoder;

    public AccountRegistrationService(AppUserMapper mapper, PasswordEncoder encoder) {
        this.mapper = mapper;
        this.encoder = encoder;
    }

    public RegisteredUserResponse register(RegisterRequest request) {
        AppUser account = new AppUser();
        account.setUsername(request.getUsername());
        account.setPasswordHash(encoder.encode(request.getPassword()));
        account.setRole("USER");
        account.setEmployeeId(null);
        account.setEnabled(true);
        try {
            if (mapper.insert(account) != 1) {
                throw new IllegalStateException("account insert affected no row");
            }
        } catch (DuplicateKeyException exception) {
            throw new DuplicateUsernameException();
        }
        return new RegisteredUserResponse(account.getId(), account.getUsername());
    }
}
```

角色与员工关联只由服务器赋值。MySQL用户名唯一约束是并发注册的最终防线；Service把冲突转换为业务异常，不对外返回数据库细节。注册成功只返回ID与用户名，不自动签发令牌。

## 6. 新建AppUserDetailsService.java

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
    private final AppUserMapper mapper;

    public AppUserDetailsService(AppUserMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public UserDetails loadUserByUsername(String username) {
        AppUser account = mapper.findByUsername(username);
        if (account == null) {
            throw new UsernameNotFoundException("login failed");
        }
        return User.withUsername(account.getUsername())
                .password(account.getPasswordHash())
                .roles(account.getRole())
                .disabled(!account.isEnabled())
                .build();
    }
}
```

`UserDetailsService` 只负责按用户名加载账号；它不接收HTTP请求，也不自己比较密码。返回的 `UserDetails` 交给认证组件，异常消息使用统一文字，避免泄露账号是否存在。


## 7. 配置签名密钥与JWT编解码

`application.yml`保留第14章的数据库、MyBatis等配置，只新增以下字段。运行前在Eclipse的Run Configuration中设置`EMPLOYEE_JWT_SECRET_BASE64`；用本地Java代码`byte[] bytes = new byte[32]; new java.security.SecureRandom().nextBytes(bytes); System.out.println(java.util.Base64.getEncoder().encodeToString(bytes));`生成随机值。不要把真实密钥提交仓库，也不要将示例值用于实际环境。更换密钥会令旧Token无法验证。

```yaml
app:
  security:
    jwt:
      secret-base64: ${EMPLOYEE_JWT_SECRET_BASE64}
      issuer: employee-management
      audience: employee-api
```

`src/main/java/com/example/employee/config/JwtConfig.java`：

```java
package com.example.employee.config;

import java.util.Base64;
import java.util.List;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;

@Configuration
public class JwtConfig {
    @Bean
    public SecretKey jwtSecretKey(
            @Value("${app.security.jwt.secret-base64}") String encoded) {
        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(encoded);
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException("JWT secret must be Base64", ex);
        }
        if (bytes.length < 32) {
            throw new IllegalStateException("JWT HS256 secret must be at least 32 bytes");
        }
        return new SecretKeySpec(bytes, "HmacSHA256");
    }

    @Bean
    public JwtEncoder jwtEncoder(SecretKey jwtSecretKey) {
        return NimbusJwtEncoder.withSecretKey(jwtSecretKey)
                .algorithm(MacAlgorithm.HS256).build();
    }

    @Bean
    public JwtDecoder jwtDecoder(SecretKey jwtSecretKey,
            @Value("${app.security.jwt.issuer}") String issuer,
            @Value("${app.security.jwt.audience}") String audience) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(jwtSecretKey)
                .macAlgorithm(MacAlgorithm.HS256).build();
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(issuer),
                new JwtClaimValidator<List<String>>(JwtClaimNames.AUD,
                        values -> values != null && values.contains(audience))));
        return decoder;
    }
}
```

`JwtEncoder`签名，`JwtDecoder`验签并检查时间、issuer与audience。`JwtValidators.createDefaultWithIssuer`包含标准时间校验；audience另加。密钥只从环境配置读取，启动时长度不足直接失败。生产环境还须经HTTPS传输并由密钥管理系统保管。

## 8. 配置认证组件和Bearer过滤链

`src/main/java/com/example/employee/config/SecurityConfig.java`：

```java
package com.example.employee.config;

import com.example.employee.security.AppUserDetailsService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AppUserDetailsService users, PasswordEncoder encoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(users);
        provider.setPasswordEncoder(encoder);
        return new ProviderManager(provider);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http)
            throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .formLogin(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)
            .logout(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(a -> a
                .requestMatchers("/health", "/error",
                        "/auth/register", "/auth/login").permitAll()
                .requestMatchers("/auth/me", "/employees", "/employees/**")
                    .authenticated()
                .anyRequest().denyAll())
            .exceptionHandling(e -> e.authenticationEntryPoint((request, response, ex) ->
                writeUnauthorized(response)))
            .oauth2ResourceServer(o -> o
                .authenticationEntryPoint((request, response, ex) ->
                    writeUnauthorized(response))
                .jwt(Customizer.withDefaults()));
        return http.build();
    }

    private static void writeUnauthorized(HttpServletResponse response)
            throws java.io.IOException {
        response.setStatus(401);
        response.setHeader("WWW-Authenticate", "Bearer");
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"success\":false,\"message\":\"请先登录\",\"data\":null}");
    }
}
```

此配置属于第15章阶段状态。`AuthenticationManager`只在登录时用数据库账号验证；`oauth2ResourceServer().jwt()`在后续请求验证Bearer签名、时间和声明。`STATELESS`禁止用Session保存SecurityContext。因为本工程**只**使用Authorization请求头承载Token，不靠浏览器自动附加认证Cookie，所以此处关闭CSRF；不能将这条结论套用于仍使用Cookie认证的项目。第16章将替换此文件的访问规则与错误处理。

## 9. 签发JWT与唯一JSON登录

`src/main/java/com/example/employee/dto/response/LoginResponse.java`：

```java
package com.example.employee.dto.response;
public record LoginResponse(String accessToken, String tokenType, long expiresIn) {}
```

`src/main/java/com/example/employee/security/JwtTokenService.java`：

```java
package com.example.employee.security;

import com.example.employee.dto.response.LoginResponse;
import java.time.Instant;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

@Service
public class JwtTokenService {
    private static final long LIFETIME_SECONDS = 900;
    private final JwtEncoder encoder;
    private final String issuer;
    private final String audience;

    public JwtTokenService(JwtEncoder encoder,
            @Value("${app.security.jwt.issuer}") String issuer,
            @Value("${app.security.jwt.audience}") String audience) {
        this.encoder = encoder;
        this.issuer = issuer;
        this.audience = audience;
    }

    public LoginResponse issue(Authentication authenticated) {
        Instant now = Instant.now();
        List<String> roles = authenticated.getAuthorities().stream()
                .map(a -> a.getAuthority())
                .filter(a -> a.startsWith("ROLE_"))
                .map(a -> a.substring("ROLE_".length()))
                .toList();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .subject(authenticated.getName())
                .audience(List.of(audience))
                .issuedAt(now)
                .notBefore(now)
                .expiresAt(now.plusSeconds(LIFETIME_SECONDS))
                .claim("roles", roles)
                .build();
        String token = encoder.encode(JwtEncoderParameters.from(claims))
                .getTokenValue();
        return new LoginResponse(token, "Bearer", LIFETIME_SECONDS);
    }
}
```

`src/main/java/com/example/employee/security/JwtLoginService.java`：

```java
package com.example.employee.security;

import com.example.employee.dto.request.LoginRequest;
import com.example.employee.dto.response.LoginResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class JwtLoginService {
    private final AuthenticationManager authenticationManager;
    private final JwtTokenService tokens;

    public JwtLoginService(AuthenticationManager authenticationManager,
            JwtTokenService tokens) {
        this.authenticationManager = authenticationManager;
        this.tokens = tokens;
    }

    public LoginResponse login(LoginRequest request) {
        Authentication input = UsernamePasswordAuthenticationToken.unauthenticated(
                request.getUsername(), request.getPassword());
        Authentication result = authenticationManager.authenticate(input);
        return tokens.issue(result);
    }
}
```

`src/main/java/com/example/employee/controller/AuthController.java`：

```java
package com.example.employee.controller;

import com.example.employee.dto.request.LoginRequest;
import com.example.employee.dto.request.RegisterRequest;
import com.example.employee.dto.response.CurrentUserResponse;
import com.example.employee.dto.response.LoginResponse;
import com.example.employee.dto.response.RegisteredUserResponse;
import com.example.employee.common.ApiResponse;
import com.example.employee.security.AccountRegistrationService;
import com.example.employee.security.JwtLoginService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final AccountRegistrationService registration;
    private final JwtLoginService login;

    public AuthController(AccountRegistrationService registration,
            JwtLoginService login) {
        this.registration = registration;
        this.login = login;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<RegisteredUserResponse>> register(
            @Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(registration.register(request)));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(ApiResponse.success(login.login(request)));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<CurrentUserResponse>> me(
            Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(
                new CurrentUserResponse(authentication.getName())));
    }
}
```

`ApiResponse.success`沿用第7章`common`包中的定义，不创建第二份包装类。`Authentication`在`/auth/me`来自已验证的Bearer Token，而不是Session。注册不签发令牌。不要增加服务器端`/auth/logout`；前端退出只删除本地Token。

## 10. 注册和登录异常

`src/main/java/com/example/employee/exception/AuthExceptionHandler.java`：

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
    @ExceptionHandler(DuplicateUsernameException.class)
    public ResponseEntity<ApiResponse<Void>> duplicate() {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.failure("用户名已存在"));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> loginFailed() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.failure("用户名或密码错误"));
    }
}
```

与第8章现有全局异常处理并存，参数校验400继续沿用原有映射；本类只处理重名409与登录失败401。JWT过滤链中的401由`SecurityConfig`处理，不会进入MVC的`@RestControllerAdvice`。

## 11. 手工验收

先在Eclipse启动，再用Postman：
- `POST /auth/register`，JSON用户名/密码，成功201；MySQL的`password_hash`不等于明文，`role=USER`且`employee_id=NULL`。同名409，非法输入400。
- `POST /auth/login`，正确密码200且`data.accessToken`、`data.tokenType=Bearer`、`data.expiresIn=900`；错误密码或停用账号401。
- 新请求`GET /auth/me`加`Authorization: Bearer <accessToken>`，预期200；不带令牌401，改动令牌任一字符401，过期401。`GET /employees/1001`带令牌，未进入第16章前只要求认证通过，业务结果依员工数据。
- 本地退出删除Postman变量中的Token；重送已签发且未过期的旧Token仍可能成功，这是无状态方案的边界，不能声称服务器已吊销。
