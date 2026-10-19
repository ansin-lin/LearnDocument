# R16 第16章授权最终源码

本文件对应第16章完成后的稳定状态，建立在R15的登录与CSRF实现之上。第13章事务和第14章分页逻辑保持不变；下文仅列第16章新增或修改的文件。

## 最终源码与变更核对

先把矩阵中的规则按执行层次读一遍：安全过滤链在Controller之前按HTTP方法、路径和角色拒绝操作，例如USER不能DELETE；只有通过URL规则的详情请求才会进入Service，方法安全代理在调用 `findById(id)` 前执行 `@PreAuthorize`，比较可信的当前 `authentication` 与方法参数 `id`。`@EnableMethodSecurity` 在配置类上启用这一代理检查；表达式中的 `@employeeAccess` 是Spring Bean名称，`#p0` 是第一个方法参数。客户端提交的角色、用户名或“这是我的记录”不能替代服务器Session中的认证信息和数据库账号关联。

未登录而需要认证时由 `RestAuthenticationEntryPoint` 写401；已有身份但被URL或方法规则拒绝时由 `RestAccessDeniedHandler` 写403；通过授权却找不到员工时仍由业务异常处理器写404。过滤链中发生的401/403通常没有进入Controller，不能指望普通 `@RestControllerAdvice` 统一捕获。`authorizeHttpRequests` 按声明顺序寻找首个匹配规则，因此具体路径与方法规则必须在兜底 `denyAll` 前。下方完整代码保留匿名、USER本人、USER他人和ADMIN的测试；CORS只作为跨域部署扩展阅读，不参与本地同源授权判断。

下面是完成第16章七个阶段后的最终文件清单；已在阶段中创建的文件不要重复新建，按这里的完整版本核对导入和未列出的配置。本章不改数据库表结构和业务字段：

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
├── security/SessionLoginTest.java                      ← 调整创建员工的回归场景
└── security/AuthorizationIntegrationTest.java          ← 新建
src/test/resources/test-data.sql                       ← 完整替换
```

### 1. 新建RestAuthenticationEntryPoint.java

```java
package com.example.employee.exception;

import com.example.employee.common.ApiResponse;
import tools.jackson.databind.ObjectMapper;
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
import tools.jackson.databind.ObjectMapper;
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
import java.util.List;
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
import org.springframework.security.web.authentication.session.CompositeSessionAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfAuthenticationStrategy;
import org.springframework.security.web.csrf.CsrfTokenRepository;

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
            CsrfTokenRepository csrfTokenRepository,
            RestAuthenticationEntryPoint authenticationEntryPoint,
            RestAccessDeniedHandler accessDeniedHandler)
            throws Exception {
        http
                .csrf(csrf -> csrf.csrfTokenRepository(csrfTokenRepository))
                .securityContext(context -> context
                        .requireExplicitSave(true)
                        .securityContextRepository(repository))
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

以下SQL使用第12章的专用MySQL测试数据库；每条测试前会重新建立已知状态。两个启用的USER分别关联1001和1002，ADMIN不需要绑定某一名员工；停用账号保留给第15章登录回归测试：

```sql
DROP TABLE IF EXISTS employee_change_logs;
DROP TABLE IF EXISTS app_users;
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
) VALUES
    (1, 'tanaka',
     '$2a$10$nljJFXRpHlZ2gPsH7lM12eIan96c0Y03Y/YCucFA/SA6z.1Lv7UTC',
     1001, 'USER', TRUE),
    (2, 'sato',
     '$2a$10$nljJFXRpHlZ2gPsH7lM12eIan96c0Y03Y/YCucFA/SA6z.1Lv7UTC',
     1002, 'USER', TRUE),
    (3, 'admin',
     '$2a$10$nljJFXRpHlZ2gPsH7lM12eIan96c0Y03Y/YCucFA/SA6z.1Lv7UTC',
     NULL, 'ADMIN', TRUE),
    (4, 'disabled-user',
     '$2a$10$nljJFXRpHlZ2gPsH7lM12eIan96c0Y03Y/YCucFA/SA6z.1Lv7UTC',
     NULL, 'USER', FALSE);
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

### 8. 调整第15章的SessionLoginTest.java

第15章的 `employeeApiRequiresLoginAndWriteRequiresCsrf()` 用tanaka登录后创建员工，原本用于证明“同一登录会话缺少CSRF会403、带有效令牌会201”。本章把新增员工限定为ADMIN；若原样保留，带令牌的USER请求也必须是403，因此不能把它继续断言为201。只替换该测试方法，其他真实登录、令牌刷新、退出和Session测试保留。特别是`disabledAccountReturnsSame401Message()`：本文件第6节的脚本保留`disabled-user`且`enabled=FALSE`，R15测试会先查询确认该记录，再经过真实登录路径验证401，不能用“不存在账号也返回401”代替停用账号回归。

替换方法如下：

```java
@Test
void employeeApiRequiresLoginAndWriteRequiresCsrf() throws Exception {
    mockMvc.perform(get("/employees/1001"))
            .andExpect(status().isUnauthorized());

    MvcResult userLogin = mockMvc.perform(post("/auth/login")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"username\":\"tanaka\","
                            + "\"password\":\"TrainingPass123!\"}"))
            .andExpect(status().isOk())
            .andReturn();
    MockHttpSession userSession =
            (MockHttpSession) userLogin.getRequest().getSession(false);
    assertThat(userSession).isNotNull();
    mockMvc.perform(get("/employees/1001").session(userSession))
            .andExpect(status().isOk());

    MvcResult adminLogin = mockMvc.perform(post("/auth/login")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"username\":\"admin\","
                            + "\"password\":\"TrainingPass123!\"}"))
            .andExpect(status().isOk())
            .andReturn();
    MockHttpSession adminSession =
            (MockHttpSession) adminLogin.getRequest().getSession(false);
    assertThat(adminSession).isNotNull();

    String createJson = "{\"name\":\"Sato\","
            + "\"department\":\"Sales\","
            + "\"email\":\"session-new-employee@example.com\"}";
    assertThat(countByEmail("session-new-employee@example.com")).isZero();
    mockMvc.perform(post("/employees")
                    .session(adminSession)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(createJson))
            .andExpect(status().isForbidden());
    assertThat(countByEmail("session-new-employee@example.com")).isZero();
    mockMvc.perform(post("/employees")
                    .session(adminSession)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(createJson))
            .andExpect(status().isCreated());
    assertThat(countByEmail("session-new-employee@example.com"))
            .isEqualTo(1);
}
```

此方法已位于第15章完整测试类中，所需的 `MockMvc`、`MockHttpSession`、`MediaType`、`MvcResult`、`csrf()`、`assertThat(...)` 和 `countByEmail(...)` 均沿用原文件。USER只验证本人可读；ADMIN的同一写接口先缺CSRF得403且数据库不变，再带有效令牌得201且新增一行。这样保留了第15章的CSRF教学目的，也符合本章的新权限规格。

### 9. 新建AuthorizationIntegrationTest.java

```java
package com.example.employee.security;

import com.example.employee.support.MySqlTestGuard;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Sql(scripts = "/test-data.sql",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class AuthorizationIntegrationTest extends MySqlTestGuard {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void anonymousCannotReadEmployee() throws Exception {
        mockMvc.perform(get("/employees/{id}", 1001))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("请先登录"));
    }

    @Test
    void anonymousWriteDistinguishesMissingCsrfFromMissingLogin()
            throws Exception {
        mockMvc.perform(delete("/employees/{id}", 1002))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/employees/{id}", 1002)
                        .with(csrf()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("请先登录"));
        assertThat(employeeName(1002L)).isEqualTo("Sato");
    }

    @Test
    void userCanReadOwnEmployee() throws Exception {
        mockMvc.perform(get("/employees/{id}", 1001)
                        .with(user("tanaka").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1001));
    }

    @Test
    void secondUserCanReadOwnEmployee() throws Exception {
        mockMvc.perform(get("/employees/{id}", 1002)
                        .with(user("sato").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1002));
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
    void userCannotCreateOrDeleteEvenWithValidCsrf()
            throws Exception {
        String createBody = """
                {"name":"Denied Employee","department":"Sales",
                 "email":"denied@example.com"}
                """;
        mockMvc.perform(post("/employees")
                        .with(user("tanaka").roles("USER"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/employees/{id}", 1002)
                        .with(user("tanaka").roles("USER"))
                        .with(csrf()))
                .andExpect(status().isForbidden());
        Integer created = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM employees WHERE email = ?",
                Integer.class, "denied@example.com");
        assertThat(created).isZero();
        assertThat(employeeName(1002L)).isEqualTo("Sato");
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
        assertThat(employeeName(1001L)).isEqualTo("Tanaka");
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
        assertThat(employeeName(1002L)).isEqualTo("Sato");
    }

    @Test
    void csrfFailureIsNotEvidenceOfRoleDenial() throws Exception {
        mockMvc.perform(delete("/employees/{id}", 1002)
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isForbidden());
        assertThat(employeeName(1002L)).isEqualTo("Sato");
    }

    @Test
    void adminCanDeleteWithCsrf() throws Exception {
        mockMvc.perform(delete("/employees/{id}", 1002)
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().isNoContent());
        Integer remaining = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM employees WHERE id = ?",
                Integer.class, 1002L);
        assertThat(remaining).isZero();
    }

    @Test
    void adminCanCreateAndUpdateWithCsrf() throws Exception {
        String createBody = """
                {"name":"New Employee","department":"Sales",
                 "email":"admin-created@example.com"}
                """;
        mockMvc.perform(post("/employees")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isCreated());
        Integer created = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM employees WHERE email = ?",
                Integer.class, "admin-created@example.com");
        assertThat(created).isEqualTo(1);

        String updateBody = """
                {"name":"Tanaka Admin Updated","department":"Sales",
                 "email":"tanaka@example.com"}
                """;
        mockMvc.perform(put("/employees/{id}", 1001)
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().isOk());
        assertThat(employeeName(1001L)).isEqualTo("Tanaka Admin Updated");
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

    private String employeeName(Long id) {
        return jdbcTemplate.queryForObject(
                "SELECT name FROM employees WHERE id = ?",
                String.class, id);
    }
}
```

`user("tanaka").roles("USER")` 是Spring Security测试库提供的请求处理器，只为当前MockMvc请求建立测试身份；它不提交密码，也不执行第15章登录流程。这里用它隔离验证权限规则，而真实账号密码和Session仍由 `SessionLoginTest` 负责。`roles("USER")` 会建立 `ROLE_USER`，不要写成 `roles("ROLE_USER")`。

本章的 `EmployeeAccess` 还会按测试用户名查询 `app_users`，所以tanaka和sato必须存在于 `test-data.sql`。ADMIN在检查authority后直接通过，不依赖某一个employeeId。`.andExpect(...)` 延续第12章写法，分别断言HTTP状态和统一JSON字段。匿名DELETE不带令牌先得403，带有效令牌才进入身份判断并得401；两次都不应删行。越权PUT再用 `JdbcTemplate` 核对原姓名未改变；缺CSRF的ADMIN删除也会403，带有效CSRF的ADMIN删除才应204且数据库记录消失。这两种403原因不同，不能只凭状态码判断角色规则是否正确。

## 最终权限矩阵与回归清单

| 接口 | 匿名 | USER | ADMIN |
| --- | --- | --- | --- |
| `GET /auth/me` | 401 | 200 | 200 |
| `GET /employees` | 401 | 403 | 200 |
| `GET /employees/{id}` | 401 | 本人200、他人403 | 任意员工200，未找到404 |
| `POST /employees`、`PUT /employees/{id}`、`DELETE /employees/{id}` | 有效CSRF时401 | 有效CSRF时403 | 有效CSRF时按业务规格成功 |

无CSRF的写请求可能先得到403，不能拿它证明角色规则。逐类运行`AuthorizationIntegrationTest`、`SessionLoginTest`、`EmployeeControllerWebTest`、`EmployeeCrudIntegrationTest`、`EmployeeSearchIntegrationTest`及第13章事务测试；核对原业务断言仍然存在，且没有关闭Security过滤器。R16的旧测试部分记录的是从R15及第12～14章稳定文件出发的**精确变更位置**，未列出的原有测试方法和断言保持不变；不要把片段误当作全新空测试类覆盖旧文件。
