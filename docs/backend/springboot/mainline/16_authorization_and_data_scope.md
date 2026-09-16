# 第16章 权限与访问范围

> 本章目标：在第15章Session登录基础上，为员工API加入角色权限和本人数据范围校验，并用自动化测试证明匿名用户、普通用户和管理员只能执行规格允许的操作。

## 一、登录成功不等于可以操作全部数据

第15章解决了认证问题：服务器能够确认“当前用户是谁”。本章解决授权问题：服务器要继续判断“这个用户能执行什么操作、能访问哪些记录”。

```text
请求携带JSESSIONID
        ↓
从Session恢复Authentication              ← 认证：当前是tanaka
        ↓
按HTTP方法和路径检查角色                  ← 操作权限：USER不能修改
        ↓
按当前账号与employeeId检查记录归属         ← 数据范围：USER只能查看本人
        ↓
Controller → Service → Mapper → 数据库
```

前端隐藏“删除”按钮只能改善画面体验。用户仍可直接发送HTTP请求，所以决定是否放行的规则必须在后端执行。

## 二、先把权限规格写成矩阵

本章先固定规格，再写代码。`USER` 表示普通用户，`ADMIN` 表示管理员：

| 请求 | 未登录 | USER | ADMIN | 判断位置 |
| --- | --- | --- | --- | --- |
| `GET /health` | 允许 | 允许 | 允许 | URL规则 |
| `GET /auth/csrf`、`POST /auth/login` | 允许 | 允许 | 允许 | URL规则 |
| `GET /auth/me` | 401 | 允许 | 允许 | URL规则 |
| `POST /auth/logout` | 有效CSRF时204 | 204 | 204 | Logout Filter＋CSRF |
| `GET /employees` | 401 | 403 | 允许 | URL＋角色 |
| `GET /employees/{id}` | 401 | 仅本人；他人403 | 允许任意员工 | URL＋Service方法 |
| `POST /employees` | 401 | 403 | 允许 | URL＋HTTP方法＋角色 |
| `PUT /employees/{id}` | 401 | 403，即使修改本人也不允许 | 允许 | URL＋HTTP方法＋角色 |
| `DELETE /employees/{id}` | 401 | 403 | 允许 | URL＋HTTP方法＋角色 |
| 未列入规格的其他请求 | 拒绝 | 拒绝 | 拒绝 | 兜底规则 |

这里包含三种不同概念：

- **角色**：账号在系统中的职责分类，例如USER、ADMIN。
- **操作权限**：某个角色能否执行查询、新增、修改或删除。
- **数据范围**：操作本身允许后，还能访问哪些记录，例如“只能查看本人”。

401表示当前请求没有可用的登录身份；403表示身份已经确认，但权限不足。找不到员工仍由第7章的统一异常处理返回404。

## 三、完整示例

从第15章稳定状态继续。本章不改数据库表结构和业务字段，只增加授权代码、测试账号和授权测试：

```text
src/main/java/com/example/employee/
├── config/SecurityConfig.java                         ← 完整替换
├── exception/RestAccessDeniedHandler.java             ← 新建
├── exception/RestAuthenticationEntryPoint.java        ← 新建
├── security/EmployeeAccess.java                       ← 新建
└── service/EmployeeService.java                       ← 完整替换
src/test/java/com/example/employee/
├── controller/EmployeeControllerWebTest.java          ← 修改测试角色
├── integration/EmployeeCrudIntegrationTest.java       ← 增加管理员身份
├── integration/EmployeeSearchIntegrationTest.java     ← 增加管理员身份
└── security/AuthorizationIntegrationTest.java          ← 新建
src/test/resources/test-data.sql                       ← 完整替换
```

### 1. 新建RestAuthenticationEntryPoint.java

```java
package com.example.employee.exception;

import com.example.employee.common.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class RestAuthenticationEntryPoint
        implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    public RestAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception)
            throws IOException, ServletException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(
                response.getOutputStream(),
                ApiResponse.failure("请先登录"));
    }
}
```

### 2. 新建RestAccessDeniedHandler.java

```java
package com.example.employee.exception;

import com.example.employee.common.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    public RestAccessDeniedHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException exception)
            throws IOException, ServletException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(
                response.getOutputStream(),
                ApiResponse.failure("没有该操作的权限"));
    }
}
```

### 3. 新建EmployeeAccess.java

```java
package com.example.employee.security;

import com.example.employee.entity.AppUser;
import com.example.employee.mapper.AppUserMapper;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component("employeeAccess")
public class EmployeeAccess {

    private final AppUserMapper appUserMapper;

    public EmployeeAccess(AppUserMapper appUserMapper) {
        this.appUserMapper = appUserMapper;
    }

    public boolean canRead(
            Authentication authentication, Long employeeId) {
        if (authentication == null
                || !authentication.isAuthenticated()
                || employeeId == null) {
            return false;
        }
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(authority ->
                        authority.getAuthority().equals("ROLE_ADMIN"));
        if (isAdmin) {
            return true;
        }
        AppUser currentUser =
                appUserMapper.findByUsername(authentication.getName());
        return currentUser != null
                && employeeId.equals(currentUser.getEmployeeId());
    }
}
```

### 4. 完整替换EmployeeService.java

```java
package com.example.employee.service;

import com.example.employee.dto.request.EmployeeCreateRequest;
import com.example.employee.dto.request.EmployeeSearchRequest;
import com.example.employee.dto.request.EmployeeUpdateRequest;
import com.example.employee.dto.response.EmployeeListItemResponse;
import com.example.employee.dto.response.EmployeeResponse;
import com.example.employee.dto.response.PageResponse;
import org.springframework.security.access.prepost.PreAuthorize;

public interface EmployeeService {

    @PreAuthorize("@employeeAccess.canRead(authentication, #p0)")
    EmployeeResponse findById(Long id);

    PageResponse<EmployeeListItemResponse> search(
            EmployeeSearchRequest request);

    EmployeeResponse create(EmployeeCreateRequest request);

    EmployeeResponse update(Long id, EmployeeUpdateRequest request);

    void delete(Long id);
}
```

只给 `findById()` 增加方法级数据范围检查。其他四个方法由URL级角色规则保护；不要删除第14章 `EmployeeServiceImpl` 中的查询、事务和履历代码。

### 5. 完整替换SecurityConfig.java

```java
package com.example.employee.config;

import com.example.employee.exception.RestAccessDeniedHandler;
import com.example.employee.exception.RestAuthenticationEntryPoint;
import com.example.employee.security.AppUserDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
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
@EnableMethodSecurity
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
            SessionAuthenticationStrategy sessionStrategy,
            RestAuthenticationEntryPoint authenticationEntryPoint,
            RestAccessDeniedHandler accessDeniedHandler)
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
                        .requestMatchers(
                                "/health", "/error",
                                "/auth/csrf", "/auth/login")
                        .permitAll()
                        .requestMatchers("/auth/me")
                        .authenticated()
                        .requestMatchers(HttpMethod.GET, "/employees")
                        .hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/employees/*")
                        .authenticated()
                        .requestMatchers(HttpMethod.POST, "/employees")
                        .hasRole("ADMIN")
                        .requestMatchers(
                                HttpMethod.PUT, "/employees/*")
                        .hasRole("ADMIN")
                        .requestMatchers(
                                HttpMethod.DELETE, "/employees/*")
                        .hasRole("ADMIN")
                        .anyRequest().denyAll())
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(logout -> logout
                        .logoutUrl("/auth/logout")
                        .deleteCookies("JSESSIONID")
                        .logoutSuccessHandler(
                                (request, response, authentication) ->
                                        response.setStatus(204)));
        return http.build();
    }
}
```

### 6. 完整替换test-data.sql

以下SQL使用H2测试数据库；每条测试前会重新建立已知状态。两个USER分别关联1001和1002，ADMIN不需要绑定某一名员工：

```sql
DROP TABLE IF EXISTS employee_change_logs;
DROP TABLE IF EXISTS app_users;
DROP TABLE IF EXISTS employees;

CREATE TABLE employees (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    department VARCHAR(100) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    status VARCHAR(20) NOT NULL
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
) VALUES
    (1, 'tanaka',
     '$2a$10$nljJFXRpHlZ2gPsH7lM12eIan96c0Y03Y/YCucFA/SA6z.1Lv7UTC',
     1001, 'USER', TRUE),
    (2, 'sato',
     '$2a$10$nljJFXRpHlZ2gPsH7lM12eIan96c0Y03Y/YCucFA/SA6z.1Lv7UTC',
     1002, 'USER', TRUE),
    (3, 'admin',
     '$2a$10$nljJFXRpHlZ2gPsH7lM12eIan96c0Y03Y/YCucFA/SA6z.1Lv7UTC',
     NULL, 'ADMIN', TRUE);
```

课程哈希对应第15章的练习密码，只能用于本地教学数据，不能复制到真实环境。

### 7. 调整三个既有测试类的身份

第12～14章的测试调用了现在已经受保护的员工接口。测试目标仍是Controller、CRUD或分页时，应给它们固定的管理员身份，不要关闭安全过滤器。

`EmployeeControllerWebTest` 把原类注解改成：

```java
@WebMvcTest(EmployeeController.class)
@ActiveProfiles("test")
@WithMockUser(username = "web-test-admin", roles = "ADMIN")
class EmployeeControllerWebTest {
    // 原有测试保持不变
}
```

`EmployeeCrudIntegrationTest` 和 `EmployeeSearchIntegrationTest` 各自增加相同的import与类注解：

```java
import org.springframework.security.test.context.support.WithMockUser;

@WithMockUser(username = "integration-admin", roles = "ADMIN")
```

写请求原有的 `.with(csrf())` 必须保留。身份验证和CSRF是两项不同检查，具备ADMIN角色不等于可以省略CSRF令牌。

### 8. 新建AuthorizationIntegrationTest.java

```java
package com.example.employee.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Sql(scripts = "/test-data.sql",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class AuthorizationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void anonymousCannotReadEmployee() throws Exception {
        mockMvc.perform(get("/employees/{id}", 1001))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("请先登录"));
    }

    @Test
    void userCanReadOwnEmployee() throws Exception {
        mockMvc.perform(get("/employees/{id}", 1001)
                        .with(user("tanaka").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1001));
    }

    @Test
    void userCannotReadAnotherEmployee() throws Exception {
        mockMvc.perform(get("/employees/{id}", 1002)
                        .with(user("tanaka").roles("USER")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message")
                        .value("没有该操作的权限"));
    }

    @Test
    void userCannotSearchEmployees() throws Exception {
        mockMvc.perform(get("/employees")
                        .with(user("tanaka").roles("USER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void userCannotUpdateEvenOwnEmployee() throws Exception {
        String body = """
                {
                  "name": "Tanaka Updated",
                  "department": "Sales",
                  "email": "tanaka.updated@example.com"
                }
                """;
        mockMvc.perform(put("/employees/{id}", 1001)
                        .with(user("tanaka").roles("USER"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    void userCannotChangePathIdToUpdateAnotherEmployee()
            throws Exception {
        String body = """
                {
                  "name": "Sato Updated",
                  "department": "Development",
                  "email": "sato.updated@example.com"
                }
                """;
        mockMvc.perform(put("/employees/{id}", 1002)
                        .with(user("tanaka").roles("USER"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanSearchAndReadAnyEmployee() throws Exception {
        mockMvc.perform(get("/employees")
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk());
        mockMvc.perform(get("/employees/{id}", 1002)
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1002));
    }

    @Test
    void logoutThenProtectedRequestReturns401() throws Exception {
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

        mockMvc.perform(get("/employees/{id}", 1001))
                .andExpect(status().isUnauthorized());
    }
}
```

`user("tanaka").roles("USER")` 是Spring Security测试库提供的请求处理器，只为当前MockMvc请求建立测试身份；它不提交密码，也不执行第15章登录流程。这里用它隔离验证权限规则，而真实账号密码和Session仍由 `SessionLoginTest` 负责。`roles("USER")` 会建立 `ROLE_USER`，不要写成 `roles("ROLE_USER")`。

本章的 `EmployeeAccess` 还会按测试用户名查询 `app_users`，所以tanaka和sato必须存在于 `test-data.sql`。ADMIN在检查authority后直接通过，不依赖某一个employeeId。`.andExpect(...)` 延续第12章写法，分别断言HTTP状态和统一JSON字段。

## 四、URL级规则怎样工作

`authorizeHttpRequests()` 接收按顺序排列的“请求匹配条件＋授权要求”。Spring Security采用第一条匹配规则，所以具体规则写在前面，最后再写兜底规则。

| 本章写法 | 可接受的值 | 作用与结果 |
| --- | --- | --- |
| `requestMatchers("/health")` | 一个或多个应用内绝对路径 | 选择要判断的请求 |
| `requestMatchers(HttpMethod.GET, "/employees")` | HTTP方法枚举＋路径 | 同一路径按请求方法区别授权 |
| `permitAll()` | 无参数 | 不要求登录 |
| `authenticated()` | 无参数 | 必须有已认证身份，不限制具体角色 |
| `hasRole("ADMIN")` | 不带`ROLE_`前缀的角色名 | 实际检查`ROLE_ADMIN`权限 |
| `denyAll()` | 无参数 | 无条件拒绝未在规格中放行的请求 |

`/employees` 和 `/employees/*` 是不同范围：前者是列表及新增路径，后者匹配带一个路径片段的详情、修改和删除路径。仅按路径写一条规则会把GET和DELETE混在一起，所以本章同时匹配 `HttpMethod`。

`anyRequest().denyAll()` 是安全兜底。以后新增接口时，开发者必须主动决定权限；遗漏规则不会意外变成公开接口。Spring Security 6.5官方说明也强调匹配顺序和兜底授权规则，参见[请求级授权](https://docs.spring.io/spring-security/reference/6.5/servlet/authorization/authorize-http-requests.html)。

`POST /auth/logout` 由第15章已配置的Logout Filter在URL授权之前处理，并继续受CSRF保护。它采用可重复执行的退出语义：只要CSRF有效，即使当前已经没有登录Session也返回204；调用方随后访问受保护接口时，才会得到401。不要误以为给 `authorizeHttpRequests` 增加logout规则就一定能改变过滤器链中更早的logout处理。

## 五、方法级检查解决数据范围

URL规则能识别 `GET /employees/*`，却不知道1001是否属于tanaka。数据范围依赖方法参数和数据库账号关系，因此放在Service入口检查：

```text
GET /employees/1002，当前用户tanaka
        ↓ URL规则：已登录，暂时通过
EmployeeService.findById(1002)
        ↓ @PreAuthorize调用employeeAccess.canRead(...)
账号tanaka关联employee_id=1001
        ↓ 1001 != 1002
拒绝调用Service方法 → 403
```

`@EnableMethodSecurity` 来自 `org.springframework.security.config.annotation.method.configuration`，写在配置类上。Spring启动时为方法授权建立拦截能力；仅加入Security依赖不会自动启用它。

`@PreAuthorize` 来自 `org.springframework.security.access.prepost`，可写在Spring管理的类、接口或方法上，在目标方法执行前计算表达式。`@employeeAccess` 引用名称为 `employeeAccess` 的Bean，`authentication` 是当前认证对象，`#p0` 是被调用方法的第一个参数，也就是 `id`。

方法检查依赖Spring代理，只有通过容器中的 `EmployeeService` Bean调用才会触发；在测试中直接 `new EmployeeServiceImpl(...)` 不会经过代理。因此第12章纯单元测试不能代替本章的Spring集成测试。方法授权的启用方式和接口注解支持可参考[Spring Security方法级授权](https://docs.spring.io/spring-security/reference/6.5/servlet/authorization/method-security.html)。

## 六、当前用户信息必须来自可信位置

`EmployeeAccess` 的两个输入来源不同：

- `authentication.getName()` 来自第15章已验证并保存在Session中的身份。
- `employeeId` 来自当前即将访问的Service方法参数。

方法再根据认证用户名查询 `app_users.employee_id`。不能让客户端提交下面这样的字段并直接相信它们：

```json
{
  "currentUserId": 1,
  "role": "ADMIN"
}
```

请求体和查询参数都能被客户端修改。若需要前端显示当前角色，可以由后端根据Session返回，但授权仍必须使用服务器SecurityContext和数据库中的权限数据。

`getAuthorities()` 返回当前身份的权限集合；`stream()` 按集合元素建立顺序处理；`anyMatch(...)` 在任一权限满足条件时返回true；`authority -> ...` 是接收单个权限对象的lambda表达式。管理员直接通过，其余账号继续核对记录归属。

## 七、为什么401和403不交给ControllerAdvice

第7章的 `@RestControllerAdvice` 处理Controller调用过程中抛出的业务异常。URL权限检查发生在Controller之前的Spring Security过滤器链中，所以这些拒绝不会自然进入既有全局异常处理器。

```text
请求 → Security Filter拒绝 → AuthenticationEntryPoint或AccessDeniedHandler
请求 → Controller → Service抛业务异常 → GlobalExceptionHandler
```

`AuthenticationEntryPoint.commence()` 处理“需要身份但当前未登录”，写入401；`AccessDeniedHandler.handle()` 处理“已经登录但权限不足”，写入403。两个接口都由Spring Security在适当时机调用，不由Controller手动调用。

`ObjectMapper` 是第6章已经使用过的JSON转换器。本章用 `writeValue(OutputStream, Object)` 把统一的 `ApiResponse` 直接写入Servlet响应流；因为此时还未进入Controller，不能使用 `ResponseEntity` 返回。`SC_UNAUTHORIZED` 和 `SC_FORBIDDEN` 分别是Servlet提供的401、403常量。

异常响应只说明需要登录或权限不足，不向客户端暴露内部规则、数据库账号关系、表达式或异常堆栈。

## 八、Session、同源与CORS的边界

浏览器页面和API的协议、主机、端口全部相同时属于同源，浏览器可按正常Cookie规则携带JSESSIONID。任一项不同就是跨源，浏览器会执行CORS检查。

当前课程默认前端与API同源，因此主线不添加CORS配置。下面用“前端 `http://localhost:3000`、API `http://localhost:8080`”做独立实验，完成后恢复同源主线。

浏览器会在跨源请求中发送 `Origin`。对于使用PUT、DELETE、自定义请求头或某些Content-Type的请求，浏览器通常先发OPTIONS预检，询问后端是否允许这个来源、方法和请求头。后端返回的 `Access-Control-Allow-Origin` 等响应头满足规则后，浏览器才会继续实际请求。预检成功不等于登录成功。

### 1. 两种配置位置

`@CrossOrigin` 可以加在单个Controller或方法上，适合识别现有代码或非常局部的规则：

```java
@CrossOrigin(origins = "http://localhost:3000")
@RestController
@RequestMapping("/employees")
public class EmployeeController {
    // 原有方法保持不变
}
```

企业项目通常需要统一审查允许来源，因而更适合集中配置。新建 `CorsConfig.java`：

```java
package com.example.employee.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/auth/**")
                .allowedOrigins("http://localhost:3000")
                .allowedMethods("GET", "POST", "OPTIONS")
                .allowedHeaders("Content-Type", "X-XSRF-TOKEN", "X-Request-Id")
                .allowCredentials(true)
                .maxAge(3600);

        registry.addMapping("/employees/**")
                .allowedOrigins("http://localhost:3000")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("Content-Type", "X-XSRF-TOKEN", "X-Request-Id")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
```

认证路径与员工路径分开配置：`/auth/**` 只开放登录流程需要的GET、POST和OPTIONS，`/employees/**` 再开放CRUD使用的PUT与DELETE。若本地实验为了观察其他路径而临时使用 `addMapping("/**")`，必须明确它只是本地配置；真实项目应按前端需要访问的API、环境和安全设计缩小范围，不能为了方便永久覆盖全部路径。

第一次出现的配置含义如下：

| 配置 | 含义 |
| --- | --- |
| `addMapping` | 哪些后端路径应用该CORS规则 |
| `allowedOrigins` | 明确允许哪些页面来源；来源包含协议、主机和端口 |
| `allowedMethods` | 跨源实际请求可使用的方法 |
| `allowedHeaders` | 浏览器可以随请求发送哪些请求头 |
| `allowCredentials` | 是否允许跨源携带Cookie等凭据 |
| `maxAge` | 浏览器可缓存预检结果的秒数，不是Session有效期 |

使用Session Cookie时，前端还需明确发送凭据：

```javascript
fetch("http://localhost:8080/employees/1001", {
  credentials: "include"
});
```

允许凭据时不能把允许来源配置成 `*`。来源要按dev、test、prod环境分别管理，不能把临时本机来源直接带入生产。

完整的跨域Session流程如下。每一步请求都要使用 `credentials: "include"`；POST还要按第15章取得并发送CSRF请求头：

```text
Frontend http://localhost:3000
  ↓ GET /auth/csrf
  ↓ POST /auth/login
      credentials: include
      CSRF Header
  ↓ Server验证账号并建立Session
  ↓ Set-Cookie: JSESSIONID
  ↓ Browser保存Cookie
  ↓ GET /auth/me 或 GET /employees/1001
      credentials: include
  ↓ Spring Security从Session恢复Authentication
  ↓ Authorization检查角色和数据范围
  ↓ Controller
```

`GET /auth/csrf`、`POST /auth/login`、`GET /auth/me`、`POST /auth/logout` 和员工接口都必须命中CORS规则，否则不能完成真实的前后端分离登录验证。CORS成功不等于登录成功，登录成功不等于拥有目标权限，拥有权限也不等于写请求已经通过CSRF检查。

### 2. Spring Security也要启用CORS集成

本项目使用Spring Security。在第15章的 `SecurityConfig` 中保留原有规则，只给 `SecurityFilterChain` 增加以下配置和静态导入：

```java
import static org.springframework.security.config.Customizer.withDefaults;

http.cors(withDefaults());
```

Spring Security需要在认证授权前处理CORS，因为OPTIONS预检通常没有JSESSIONID。若只配置MVC却没有正确接入安全过滤链，预检可能先被401或403拒绝。不要通过把员工接口改成 `permitAll()` 来“修复”跨域。

### 3. 四个经常混淆的边界

| 机制 | 回答的问题 | 失败时常见现象 |
| --- | --- | --- |
| CORS | 这个浏览器页面来源能否读取响应 | 浏览器拦截，或预检失败 |
| 认证 | 当前请求是谁 | 401 |
| 授权 | 这个身份能否操作该资源 | 403 |
| CSRF | 携带用户凭据的写请求是否来自预期页面操作 | 403或CSRF错误 |

CORS只决定浏览器是否允许某个来源读取响应，不证明用户身份，也不授予USER管理员权限。命令行客户端不受浏览器同源限制，但仍会受到后端认证、授权和CSRF规则约束。

实验时先发送OPTIONS预检，确认允许来源不是 `*`、方法和请求头正确；再分别验证未登录401、USER越权403、ADMIN成功以及写请求仍需要CSRF令牌。完成后删除 `CorsConfig` 和 `http.cors(...)` 增量，重跑本章测试，恢复同源稳定状态。官方配置边界可对照[Spring MVC CORS](https://docs.spring.io/spring-framework/reference/web/webmvc-cors.html)和[Spring Security CORS集成](https://docs.spring.io/spring-security/reference/servlet/integrations/cors.html)。

## 九、运行与验证

在项目根目录执行：

```powershell
.\mvnw.cmd clean test
```

除了第15章已有登录测试，本章新增8个授权场景。必须确认所有旧测试和新测试都通过；只运行新测试不能证明加入权限后没有破坏CRUD、事务和分页。

手工验证时使用两个独立的Cookie会话分别登录 `tanaka` 和 `admin`，不要混用Cookie。证据至少包含：

1. 未登录读取员工返回401和统一JSON。
2. tanaka读取1001返回200。
3. tanaka读取1002返回403。
4. tanaka查询列表或修改1001返回403。
5. admin查询列表、读取1002和修改员工成功。
6. logout后不再携带旧会话，再访问受保护接口返回401。

测试证据记录请求方法、脱敏路径、预期状态、实际状态和测试时间即可。不要粘贴JSESSIONID、CSRF令牌、密码或完整安全日志。

## 十、常见失败与定位顺序

| 现象 | 判断位置 | 常见原因 | 修正 |
| --- | --- | --- | --- |
| 未登录返回HTML或空401 | 安全过滤器 | 未配置AuthenticationEntryPoint | 接入统一401处理器 |
| USER读取他人数据却200 | Service方法 | 漏写或未启用方法授权 | 检查两个注解和Spring代理调用 |
| ADMIN也被403 | 账号加载 | role已带`ROLE_`又传给`roles()` | 数据库存`ADMIN`，由框架增加前缀 |
| 所有写请求都403 | CSRF | 有角色但无令牌 | 登录身份和CSRF都要提供 |
| 旧CRUD测试突然401/403 | 测试准备 | 测试未指定管理员身份 | 为原测试补`@WithMockUser(roles="ADMIN")` |
| ControllerAdvice断点不进入 | 执行阶段 | 请求在Controller前被拒绝 | 检查安全异常处理器 |
| 加新接口后ADMIN也不能访问 | URL兜底 | 新路径未加入权限矩阵 | 先评审规格，再添加精确规则 |

排查顺序固定为：请求方法与路径 → 是否有Session → 当前authority → 命中的URL规则 → 方法授权表达式 → 数据库账号关联。不要为了快速通过而改成 `permitAll()` 或关闭CSRF。

## 十一、规格理解、影响调查、Review与练习

### 练习1：补充退出后的回归证据

使用真实登录流程取得Session，访问本人记录成功，执行logout，再以不带有效会话的请求访问本人记录。验收结果必须依次是200、204、401。

### 练习2：新增“上司可查看本部门”规格调查

先不要写代码。提交一份影响调查，至少回答：角色是否足够、账号怎样关联部门、部门变化何时生效、列表与详情是否都受影响、需要哪些401/403/200测试、是否会泄露其他部门总件数。

### 练习3：Review越权代码

找出下面实现的问题并给出修改方向：

```java
@GetMapping("/{id}")
public EmployeeResponse findById(
        @PathVariable Long id,
        @RequestParam String role) {
    if ("ADMIN".equals(role)) {
        return employeeService.findById(id);
    }
    return employeeService.findById(id);
}
```

Review结论至少包括：信任客户端角色、两个分支都放行、Controller重复安全判断、没有记录归属校验、没有无权测试。

### 练习4：整理权限自测表

根据本章权限矩阵，把每个接口的未登录、USER、ADMIN用例列成表格，标注测试类和方法名。若某个规格格子没有自动化测试，要说明手工证据位置或补测计划。

## 十二、本章稳定状态

完成后，公开接口、登录接口和员工接口都有明确的URL级规则；普通用户只能读取与账号关联的本人记录，管理员可以执行员工管理操作；安全层的401和403继续使用统一JSON响应；旧CRUD、事务、分页和登录测试仍可回归。

你应能够根据权限矩阵解释认证、授权、角色、操作权限和数据范围的区别，并能从URL规则、Service方法检查、当前用户来源和测试证据四个位置判断一次访问为什么被允许或拒绝。

下一章将使用这一稳定安全状态进入多环境配置与打包，不在本章提前修改生产配置。
