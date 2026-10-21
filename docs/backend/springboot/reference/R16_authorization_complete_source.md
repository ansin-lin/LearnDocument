# R16 第16章JWT角色授权与本人范围最终参考源码

本文件以[R15最终状态](R15_session_login_complete_source.md)为起点，对照[第16章正文](../mainline/16_authorization_and_data_scope.md)完成角色与本人数据授权。按下文新建两个JSON安全异常处理器和`EmployeeAccess`，完整替换`EmployeeService`接口与**唯一的**`SecurityConfig`；保留R15的`JwtConfig`、`JwtLoginService`、`JwtTokenService`、`AuthController`、`AuthExceptionHandler`及原员工业务实现。登录Controller内部的认证失败仍由R15的`AuthExceptionHandler`返回401；无效Bearer或授权失败分别由本文件的过滤链处理器返回401/403。方法级权限拒绝发生在Service代理处：当前第8章`GlobalExceptionHandler`及`AuthExceptionHandler`均不匹配`AccessDeniedException`，异常从MVC向外传播后才交由Security的`ExceptionTranslationFilter`和`RestAccessDeniedHandler`写统一403；若以后增加MVC兜底异常处理，必须重新验证此路径。

从[R15](R15_session_login_complete_source.md)的最终状态继续：注册、JSON登录和JWT签发文件不变。本章仅替换安全访问规则、新增JWT角色转换与本人范围判断，并保留第13、14章全部员工业务。不要复制旧Session/CSRF版本。

## 1. 新建RestAuthenticationEntryPoint.java

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
        response.setHeader("WWW-Authenticate", "Bearer");
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(
                response.getOutputStream(),
                ApiResponse.failure("请先登录"));
    }
}
```

## 2. 新建RestAccessDeniedHandler.java

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

## 3. 新建EmployeeAccess.java

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
                        "ROLE_ADMIN".equals(authority.getAuthority()));
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

## 4. 完整替换EmployeeService.java

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


## 5. 替换SecurityConfig.java为第16章最终状态

保留第15章`JwtConfig`、注册、数据库认证、JWT签发以及员工CRUD全部文件。只替换下方`SecurityConfig`。`roles`声明来自第15章登录时查出的数据库角色；资源服务器默认会把scope映射为`SCOPE_`，因此这里显式指定`roles`并加`ROLE_`前缀，让`hasRole("ADMIN")`匹配`ROLE_ADMIN`。

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
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
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
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter roles = new JwtGrantedAuthoritiesConverter();
        roles.setAuthoritiesClaimName("roles");
        roles.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(roles);
        return converter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationConverter converter,
            RestAuthenticationEntryPoint unauthorized,
            RestAccessDeniedHandler forbidden) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .formLogin(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)
            .logout(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(a -> a
                .requestMatchers("/health", "/error",
                        "/auth/register", "/auth/login").permitAll()
                .requestMatchers("/auth/me").authenticated()
                .requestMatchers(HttpMethod.GET, "/employees").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/employees/*").authenticated()
                .requestMatchers(HttpMethod.POST, "/employees").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/employees/*").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/employees/*").hasRole("ADMIN")
                .anyRequest().denyAll())
            .exceptionHandling(e -> e
                .authenticationEntryPoint(unauthorized)
                .accessDeniedHandler(forbidden))
            .oauth2ResourceServer(o -> o
                .authenticationEntryPoint(unauthorized)
                .accessDeniedHandler(forbidden)
                .jwt(j -> j.jwtAuthenticationConverter(converter)));
        return http.build();
    }
}
```

Bearer验证先由`JwtDecoder`检查签名、HS256、iss、aud、nbf、exp，再由Converter把`roles=["USER"]`转成`ROLE_USER`。JWT中的角色是签发时快照，数据库改角色或停用账号不会立即撤销旧Token；本课15分钟后到期，敏感系统还要设计撤销或短期令牌策略。

## 6. 本地验证数据

在个人练习库准备员工1001/Tanaka、1002/Sato及账号tanaka(USER→1001)、sato(USER→1002)、admin(ADMIN→NULL)、disabled-user(停用)。账号密码哈希通过第15章`PasswordEncoder.encode`生成，不能存明文。不要对已有练习库执行`DROP TABLE`。如果需要完整的专用测试库初始化脚本，应另建隔离数据库，先核对库名再执行；第16章主线不要求JUnit/MockMvc。

## 7. Postman权限矩阵

分别用tanaka、sato、admin登录取得各自Bearer Token；切换身份时替换`Authorization`头，不能只换JSON用户名。下表相对URL均以`http://localhost:8080`为前缀；每次受保护请求都写`Authorization: Bearer <对应Token>`，无请求体的GET也不例外。没有标明的查询参数均不提交。

| 身份 | 方法、URL | 请求参数与请求体 | 预期状态/响应 |
| --- | --- | --- | --- |
| 无Token | GET `/employees/1001` | 无 | 401 JSON |
| tanaka | GET `/employees/1001` | 无 | 200，data.id=1001 |
| tanaka | GET `/employees/1002` | 无 | 403 JSON |
| sato | GET `/employees/1002` | 无 | 200，data.id=1002 |
| 注册后未绑定员工USER | GET `/employees/1001` | 无 | 403 JSON |
| admin | GET `/employees?page=1&pageSize=20&sortBy=id&sortDirection=asc` | 四个查询参数；无请求体 | 200，`data.items`、`page=1`、`pageSize=20`、`total`、`totalPages` |
| tanaka | GET `/employees` | 无 | 403 JSON |
| tanaka | POST `/employees` | JSON员工资料 | 403 JSON，数据库无新增 |
| tanaka | PUT `/employees/1002` | JSON员工资料 | 403 JSON，数据库原值不变 |
| tanaka | DELETE `/employees/1002` | 无 | 403 JSON，数据库原值不变 |
| admin | POST `/employees` | JSON员工资料 | 201，新员工入库 |
| admin | PUT `/employees/1002` | 下方完整修改JSON | 200，`data.id=1002`及修改后的字段；测试后恢复练习数据 |
| admin | GET `/employees/999999` | 无 | 404 JSON |
| admin | DELETE `/employees/1002` | 无；最后执行 | 204，数据库1002已删除 |
| 修改过的Token | GET `/auth/me` | 无 | 401 JSON |
| 过期Token | GET `/employees` | 无 | 401 JSON |

写请求Body具体使用：USER被拒绝的POST为`{"name":"Blocked","department":"Sales","email":"blocked@example.com"}`；ADMIN成功POST为`{"name":"Allowed","department":"Sales","email":"allowed@example.com"}`；越权和ADMIN成功PUT为`{"name":"Sato Edited","department":"Development","email":"sato@example.com"}`。新增和修改都必须包含`name`、`department`、`email`，部门只能是Sales、Development或Support。POST/PUT均设置`Content-Type: application/json`；DELETE无请求体。原CRUD契约为详情200、新增201及`Location`、修改200、删除204且无正文、不存在404；列表返回`data.items/page/pageSize/total/totalPages`，可选查询参数为`name`、`department`、`status`、`page`、`pageSize`、`sortBy`、`sortDirection`。

POST/PUT/DELETE均无CSRF Header，本工程认证只取Bearer请求头；缺Token写请求预期401。任何被拒绝的写请求应查询数据库验证无变化。先做详情再删除测试数据，避免后续404被误认为权限错误。旧Token在客户端“退出”后仍有效至到期，不声称服务端已吊销。
