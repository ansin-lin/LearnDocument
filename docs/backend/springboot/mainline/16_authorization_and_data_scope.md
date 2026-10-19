# 第16章 Spring Security权限控制与数据访问范围

> 本章目标：在第15章Session登录基础上，按阶段实现URL角色、可信账号关联、本人数据范围和统一拒绝响应，并用真实登录与自动化测试证明规则。

开始状态：第15章员工CRUD、`app_users`、Session登录、CSRF、退出均能运行。完整终版放在 [R16最终源码](../reference/R16_authorization_complete_source.md)，仅供每阶段完成后核对，不是前期操作的前置要求。原理延伸见 [A16-01授权与数据范围](../appendix/A16_01_method_security_data_scope.md)、[A16-02安全测试与排障](../appendix/A16_02_security_testing_and_troubleshooting.md)。

## 一、阶段16-01：区分认证、操作权限与数据范围

第15章解决了认证问题：服务器能够确认“当前用户是谁”。本章解决授权问题：服务器要继续判断“这个用户能执行什么操作、能访问哪些记录”。

本章依次完成四个判断：

- **权限规格**：先写清匿名、USER、ADMIN分别能调用哪些接口，哪些记录可以看；不能只凭前端按钮是否显示来决定权限。
- **URL角色授权**：安全过滤链在Controller之前，按HTTP方法、路径和角色判断。例如USER不能调用员工列表和写接口；`hasRole("ADMIN")` 检查的是账号的管理员权限，不检查记录归属。
- **本人数据授权**：详情GET对USER开放，但路径中的员工id只是目标，不是用户身份。进入业务层时，服务器再用已认证的用户名及`app_users.employee_id`判断目标是否属于本人；具体注解和代码在第四阶段讲解。
- **拒绝响应与验证**：未登录通常是401，已登录但越权通常是403；写请求若缺少CSRF令牌，也可能在角色判断前得到403。测试必须同时控制登录身份和CSRF，才能证明403究竟由哪条规则产生。

判断顺序是“先有可信身份，再看能否调用该接口，最后看能否访问这条数据”。URL角色规则与Service数据范围规则解决不同问题，不能任选一层来代替另一层。

```text
请求携带JSESSIONID
        ↓
从Session恢复Authentication              ← 认证：当前是tanaka
        ↓
按HTTP方法和路径检查角色                  ← 操作权限：USER不能修改
        ↓
Controller → Service入口
        ↓
按当前账号与employeeId检查记录归属         ← 数据范围：USER只能查看本人
        ↓
Service方法主体 → Mapper → 数据库
```

前端隐藏“删除”按钮只能改善画面体验。用户仍可直接发送HTTP请求，所以决定是否放行的规则必须在后端执行。

| 请求场景 | 认证：是谁 | 功能授权：能否调用API | 数据范围：能否访问该记录 |
| --- | --- | --- | --- |
| 未登录查询1001 | 无有效身份，拒绝 | 不继续判断 | 不继续判断 |
| tanaka查询本人1001 | USER身份有效 | 详情GET允许 | `app_users.employee_id=1001`，允许 |
| tanaka查询他人1002 | USER身份有效 | 详情GET允许 | `1001 ≠ 1002`，拒绝 |
| admin查询1002 | ADMIN身份有效 | 详情GET允许 | 管理员按本章规格可查看全部 |

登录成功并不自动获得所有API权限；能够调用某个详情API，也不表示能读取任意编号的数据。前端发送的路径id是“目标记录”，不是“当前用户身份”。

### 把判断写成可测试的权限矩阵

本章先固定规格，再写代码。`USER` 表示普通用户，`ADMIN` 表示管理员：

表中POST、PUT、DELETE的匿名401以**已携带有效CSRF令牌**为前提。若写请求缺少或带错令牌，CSRF过滤器可能先返回403；这不是“匿名用户已通过认证”。GET不需要CSRF令牌。第15章的登录与退出流程也保持CSRF检查。

| 请求 | 未登录 | USER | ADMIN | 判断位置 |
| --- | --- | --- | --- | --- |
| `GET /health` | 允许 | 允许 | 允许 | URL规则 |
| `GET /auth/csrf`、`POST /auth/login` | 允许 | 允许 | 允许 | URL规则 |
| `GET /auth/me` | 401 | 允许 | 允许 | URL规则 |
| `POST /auth/logout` | 有效CSRF时204 | 204 | 204 | Logout Filter＋CSRF |
| `GET /employees` | 401 | 403 | 允许 | URL＋角色 |
| `GET /employees/{id}` | 401 | 仅本人；他人403 | 允许任意员工 | URL＋Service方法 |
| `POST /employees` | 有效CSRF时401 | 403 | 允许 | CSRF通过后按URL＋HTTP方法＋角色 |
| `PUT /employees/{id}` | 有效CSRF时401 | 403，即使修改本人也不允许 | 允许 | CSRF通过后按URL＋HTTP方法＋角色 |
| `DELETE /employees/{id}` | 有效CSRF时401 | 403 | 允许 | CSRF通过后按URL＋HTTP方法＋角色 |
| 未列入规格的其他请求 | 拒绝 | 拒绝 | 拒绝 | 兜底规则 |

这里包含三种不同概念：

- **角色**：账号在系统中的职责分类，例如USER、ADMIN。
- **操作权限**：某个角色能否执行查询、新增、修改或删除。
- **数据范围**：操作本身允许后，还能访问哪些记录，例如“只能查看本人”。

401表示当前请求没有可用的登录身份；403可能表示已认证但权限不足，也可能是写请求尚未通过CSRF检查。找不到员工仍由第7章的统一异常处理返回404。

本章列表规格是“仅ADMIN可查询”，所以不在第14章的分页SQL里加入新条件。将来若允许USER按部门或本人查询列表，列表与COUNT都必须使用同一可信范围；这是新的业务规格和进阶设计，详见[A16-01](../appendix/A16_01_method_security_data_scope.md)，本章不实现。

- 教师讲解重点：让学员先口述“谁、做什么、哪条记录”三个问题。
- 学员操作：对照矩阵写出tanaka查本人、查他人和改本人的预期状态。
- 常见错误：把前端隐藏按钮当成后端授权，或把路径ID当成当前用户身份。
- 完成标准：能解释每条规则在URL层还是Service层判断。

## 二、阶段16-02：URL角色授权

在第15章 `SecurityConfig.java` 的 `securityFilterChain(...)` 中，仅把原 `.authorizeHttpRequests(...)` 整块替换为以下代码，并新增 `import org.springframework.http.HttpMethod;`；其余CSRF、Session、认证和logout配置保留。此阶段详情GET只检查是否登录，下一案例才加入本人范围。

```java
.authorizeHttpRequests(authorize -> authorize
    .requestMatchers("/health", "/error", "/auth/csrf", "/auth/login")
        .permitAll()
    .requestMatchers("/auth/me").authenticated()
    .requestMatchers(HttpMethod.GET, "/employees").hasRole("ADMIN")
    .requestMatchers(HttpMethod.GET, "/employees/*").authenticated()
    .requestMatchers(HttpMethod.POST, "/employees").hasRole("ADMIN")
    .requestMatchers(HttpMethod.PUT, "/employees/*").hasRole("ADMIN")
    .requestMatchers(HttpMethod.DELETE, "/employees/*").hasRole("ADMIN")
    .anyRequest().denyAll())
```

`requestMatchers` 按顺序匹配HTTP方法与路径，`hasRole("ADMIN")` 查找 `ROLE_ADMIN`，不要传入 `ROLE_` 前缀；`hasAuthority("ROLE_ADMIN")` 则要写完整权限字符串。本章账号加载代码把数据库 `ADMIN` 经 `.roles(...)` 转成 `ROLE_ADMIN`。`authenticated()` 只要求有身份，不限制角色；`denyAll()` 拒绝未列明请求。匿名GET员工详情应为401，USER GET列表应为403，ADMIN GET列表应为200；需要先在第三阶段建立admin账号，届时再回测此项。此时USER仍可读任意详情是**临时阶段状态**，不要作为交付状态，也不要把数据范围测试当作已通过。

此阶段先用第15章tanaka真实登录，Postman验证 `GET http://localhost:8080/employees`：无请求体、USER应403。匿名请求应401。待下一阶段建好admin，再回测ADMIN列表200。代码中只改一块URL规则，`/auth/me` 和退出仍应正常。

- 教师讲解重点：`hasRole("ADMIN")` 检查 `ROLE_ADMIN`，`authenticated()` 仅检查登录。
- 学员操作：替换授权配置并运行Postman，记录状态码和响应。
- 常见错误：把 `ROLE_ADMIN` 直接传给 `hasRole`；拿缺CSRF的写请求证明角色规则。
- 完成标准：USER列表403、匿名员工详情401；ADMIN项在下一阶段补测。

## 三、阶段16-03：账号与员工关系

第15章的 `app_users.employee_id` 已存在，无需再改表结构。只在个人练习库或专用MySQL测试库的 `test-data.sql` 中，为现有1001、1002员工准备两个启用的USER账号、一个ADMIN账号，并保留停用账号供登录回归。完整测试初始化脚本在下方；如果从第15章脚本增量修改，保留原有 `tanaka`，只追加尚不存在的 `sato`、`admin`。先查询`disabled-user`是否存在且`enabled=FALSE`；若第15章个人练习库只有tanaka，再单独补它。已有账号不要重复插入：

```sql
INSERT INTO app_users (id, username, password_hash, employee_id, role, enabled)
VALUES
    (2, 'sato',
     '$2a$10$nljJFXRpHlZ2gPsH7lM12eIan96c0Y03Y/YCucFA/SA6z.1Lv7UTC',
     1002, 'USER', TRUE),
    (3, 'admin',
     '$2a$10$nljJFXRpHlZ2gPsH7lM12eIan96c0Y03Y/YCucFA/SA6z.1Lv7UTC',
     NULL, 'ADMIN', TRUE);
```

仅当`SELECT username, enabled FROM app_users WHERE username = 'disabled-user';`没有返回行时，才在**个人练习库**执行以下补充；已有行但`enabled`不符合预期，应先调查数据来源，不盲目重复插入：

```sql
INSERT INTO app_users (id, username, password_hash, employee_id, role, enabled)
VALUES (4, 'disabled-user',
        '$2a$10$nljJFXRpHlZ2gPsH7lM12eIan96c0Y03Y/YCucFA/SA6z.1Lv7UTC',
        NULL, 'USER', FALSE);
```

此哈希只用于本地教学，两个USER分别关联1001/1002；ADMIN不绑定员工。不要把账号表中 `role` 或 `employee_id` 交给客户端声明。用数据库客户端确认用户名唯一、两个关联各有一行；分别真实登录后 `/auth/me` 返回各自用户名，此阶段角色URL规则已能区分USER列表403与ADMIN列表200。若已按下方完整 `test-data.sql` 替换，就**不要再次执行**这段INSERT。

`src/test/resources/test-data.sql` 用于第12章的专用MySQL测试库。需要整文件替换时使用下面版本；在个人练习库只执行新增账号语句，**不得运行下面的DROP脚本**。

### 专用测试库的完整初始化脚本

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

在Postman中分别登录tanaka、sato、admin，调用 `GET http://localhost:8080/auth/me`（无参数/请求体）核对身份；再以admin调用 `GET http://localhost:8080/employees`（无参数/请求体），应200且返回列表。继续用当前有效CSRF令牌对比同一新增接口：

| 方法与URL | 身份、请求体 | 预期状态与数据库 |
| --- | --- | --- |
| `POST http://localhost:8080/employees` | tanaka；`{"name":"Denied","department":"Sales","email":"denied-stage16@example.com"}` | 403；数据库无该邮箱 |
| `POST http://localhost:8080/employees` | admin；`{"name":"Allowed","department":"Sales","email":"allowed-stage16@example.com"}` | 201；数据库新增该邮箱 |

两个请求都要带当前Session的Cookie、令牌Cookie和`X-XSRF-TOKEN`请求头；若管理员请求仍403，先检查CSRF而不是改权限配置。验证后在个人练习库清理本次新增的练习记录。数据库客户端还要检查三个启用账号与1001/1002的关联。本阶段还没有本人范围限制，不以他人详情200作为最终结果。

- 教师讲解重点：角色列定义功能类别，`employee_id` 定义数据关联，客户端不能声明二者。
- 学员操作：准备账号、检查唯一性和关系，补测ADMIN列表。
- 常见错误：在个人练习库执行测试脚本的DROP；重复INSERT已有账号。
- 完成标准：三个账号都能登录，ADMIN列表200，账号关联正确。

## 四、阶段16-04：方法级本人数据授权

URL只能识别 `/employees/1002`，无法知道它是否属于当前账号。在 `SecurityConfig.java` 类上新增 `@EnableMethodSecurity`，导入 `org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity`；Spring Boot的Security Starter不会自动启用方法授权。`@PreAuthorize` 在目标Service方法执行前检查表达式，必须通过Spring管理的Service Bean调用才生效。

```text
GET /employees/1002
  → SecurityFilterChain：是否已登录，详情URL是否允许
  → EmployeeController：接收目标ID 1002
  → EmployeeService的Spring代理：执行@PreAuthorize
  → EmployeeAccess.canRead()：从认证身份取用户名，再查app_users.employee_id
  → 允许：进入Service方法主体；拒绝：不执行方法主体并返回403
```

新建 `src/main/java/com/example/employee/security/EmployeeAccess.java`，完整内容如下：

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
    public boolean canRead(Authentication authentication, Long employeeId) {
        if (authentication == null || !authentication.isAuthenticated()
                || employeeId == null) {
            return false;
        }
        boolean admin = authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        if (admin) {
            return true;
        }
        AppUser current = appUserMapper.findByUsername(authentication.getName());
        return current != null
                && employeeId.equals(current.getEmployeeId());
    }
}
```

`@Component("employeeAccess")` 注册Bean供表达式引用；`authentication.getName()` 是第15章验证过的用户名，`employeeId` 是Service方法的目标参数。管理员按角色通过，USER用数据库中可信的 `employee_id` 比较，不从请求体取“我的编号”。接着在原 `EmployeeService.java` 增加 `import org.springframework.security.access.prepost.PreAuthorize;`，只给原有 `findById(Long id)` 声明加注解，其余方法与实现不变：

```java
@PreAuthorize("@employeeAccess.canRead(authentication, #p0)")
EmployeeResponse findById(Long id);
```

`#p0` 是第一个方法参数id；检查失败时方法主体不会执行。此阶段通过Spring上下文测试或真实登录请求验证：tanaka读1001为200，读1002为403，admin读任一记录为200。直接 `new EmployeeServiceImpl(...)` 的单元测试不经过安全代理，不能用来证明此规则。前一案例的“USER可读任意详情”到此结束。

Postman依次验证 `GET http://localhost:8080/employees/1001`（tanaka，200）、`GET http://localhost:8080/employees/1002`（tanaka，403）、同一URL（sato，200；admin，200）；四个请求均无请求体。也要检查403响应未包含他人资料。

- 教师讲解重点：`@EnableMethodSecurity`、`@PreAuthorize`、`#p0` 和Spring代理的作用，先讲再看测试。
- 学员操作：新增判断Bean、启用方法授权并给Service接口添加注解。
- 常见错误：遗漏启用注解，或用前端传入的“本人ID”代替可信账号关系。
- 完成标准：USER本人200、他人403，ADMIN任意详情200。

## 五、阶段16-05：统一401/403的JSON响应

过滤链的拒绝可能发生在Controller之前，第7章的 `@RestControllerAdvice` 不能处理这些路径。新建下方两个完整文件；它们只写统一JSON，不暴露账号关联或表达式细节。

`src/main/java/com/example/employee/exception/RestAuthenticationEntryPoint.java`：

```java
package com.example.employee.exception;
import com.example.employee.common.ApiResponse;
import tools.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {
    private final ObjectMapper objectMapper;
    public RestAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException exception) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getOutputStream(),
                ApiResponse.failure("请先登录"));
    }
}
```

`src/main/java/com/example/employee/exception/RestAccessDeniedHandler.java`：

```java
package com.example.employee.exception;
import com.example.employee.common.ApiResponse;
import tools.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

@Component
public class RestAccessDeniedHandler implements AccessDeniedHandler {
    private final ObjectMapper objectMapper;
    public RestAccessDeniedHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
            AccessDeniedException exception) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getOutputStream(),
                ApiResponse.failure("没有该操作的权限"));
    }
}
```

在 `SecurityConfig.java` 的 `securityFilterChain(...)` 方法参数中增加 `RestAuthenticationEntryPoint authenticationEntryPoint` 与 `RestAccessDeniedHandler accessDeniedHandler`，导入这两个类；把原 `.exceptionHandling(...)` 整块替换为：

```java
.exceptionHandling(exceptions -> exceptions
    .authenticationEntryPoint(authenticationEntryPoint)
    .accessDeniedHandler(accessDeniedHandler))
```

不要改变案例16-01的URL规则、案例16-03的 `@EnableMethodSecurity`、第15章的CSRF和Session配置。此时匿名401、USER越权403、ADMIN成功响应都应具有规定格式。第六阶段测试使用 `user(...)` 注入当前请求身份以隔离授权规则；第15章真实账号密码与Session恢复由 `SessionLoginTest` 单独验证。测试写接口时**同时加 `.with(csrf())`**，否则403可能仅来自CSRF，不能证明角色拒绝。对USER的PUT还要查询数据库确认姓名未改变；ADMIN同一写接口携带CSRF应成功，以排除接口本身损坏。

用Postman验证匿名 `GET http://localhost:8080/employees/1001` 返回401 JSON，tanaka读取1002返回403 JSON，admin读取不存在的9999进入业务层返回404。方法均为GET，无请求体；若写请求不带CSRF，403可能先由CSRF产生。

- 教师讲解重点：安全过滤链在Controller之前，和业务异常处理的位置不同。
- 学员操作：新增两个处理类，连接到过滤链，再记录401/403/404的响应。
- 常见错误：以为`@RestControllerAdvice`会处理过滤器里的全部拒绝。
- 完成标准：安全拒绝有统一JSON，业务404未被覆盖。

## 六、阶段16-06：权限测试与数据库不变断言

沿用第三阶段的专用测试库数据。在IDE中直接运行下方测试类；本阶段开始前不需要创建它。`user(...)` 模拟当前请求身份以隔离授权规则；它不代替第15章真实登录和Session恢复测试。写请求同时带 `csrf()`，否则403不能证明是角色拒绝。

### 可运行的授权集成测试

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

- 教师讲解重点：让学员解释每个`with(user(...))`、`with(csrf())`、响应断言和数据库不变断言分别证明什么。
- 学员操作：先跑USER本人/他人和匿名，再跑写请求与ADMIN；保存测试结果。
- 常见错误：用缺CSRF的403当成角色授权证据；只看响应不查拒绝后的数据库。
- 完成标准：矩阵关键行均有自动化证据，拒绝写入的数据未改变。

## 七、阶段16-07：既有测试影响调查与回归

**本阶段要解决的问题：** 权限改修让原先匿名访问员工接口的测试失效；哪些是规格变化，哪些是业务功能回归？不能靠删除旧测试或关闭Security让结果变绿。

第六阶段的`AuthorizationIntegrationTest`证明新权限规则。第15章`SessionLoginTest`仍负责真实账号密码、跨请求Session、令牌更新与Logout。这两类证据互相补充，`user(...)`模拟身份不能证明真实登录。

先调查旧测试的目标，再修改测试前提：

| 旧测试类 | 本次影响 | 调整后仍须证明 |
| --- | --- | --- |
| `EmployeeControllerWebTest` | 原MockMvc请求可能先被安全过滤链拒绝 | 保留Controller输入/输出断言；给适合的模拟身份，写请求加`csrf()` |
| `EmployeeCrudIntegrationTest` | CRUD写接口改为仅ADMIN，且仍要求CSRF | 类级或请求级ADMIN身份，写请求带`csrf()`；保留事务和数据库断言 |
| `EmployeeSearchIntegrationTest` | 列表仅ADMIN | 使用ADMIN身份，保留筛选、排序、COUNT、分页断言 |
| `SessionLoginTest` | USER不再能新增、修改或删除员工 | tanaka只验证本人详情；写请求用admin验证缺CSRF的403与带令牌成功；真实登录和退出测试不删除 |
| `AuthorizationIntegrationTest` | 本章新增 | 匿名、USER、ADMIN、CSRF、本人范围和拒绝后的数据不变 |

R16的[第7～8节](../reference/R16_authorization_complete_source.md)列出旧类需更改的精确注解与`SessionLoginTest`完整替换方法；只在本阶段对照，不要把R16的最终`SecurityConfig`提前覆盖前六阶段的增量配置。`@WebMvcTest`切片加载的Bean范围与`@SpringBootTest`不同，若Controller测试启动失败，先核对测试切片依赖，不要关闭过滤器。

在Eclipse按下面顺序逐类“Run As → JUnit Test”：新权限测试 → `SessionLoginTest` → Controller测试 → CRUD集成测试 → 搜索分页测试 → 第13章事务测试。失败时先看请求身份和CSRF，再看业务异常与数据库，不要先改预期状态码。

Postman真实验收先调用`GET http://localhost:8080/auth/csrf`取得令牌，再`POST http://localhost:8080/auth/login`提交`{"username":"tanaka","password":"TrainingPass123!"}`；记录JSESSIONID但不写入共享证据。换账号前清空旧Cookie，再按第15章流程登录。以下表格中的“有效CSRF”指当前Session下的令牌Cookie与`X-XSRF-TOKEN`请求头同时正确；未注明请求体者无请求体。

| HTTP方法与URL | 身份、请求体/参数 | 预期状态与响应 |
| --- | --- | --- |
| `GET http://localhost:8080/employees/1001` | 无Session | 401，统一JSON`请先登录` |
| `GET http://localhost:8080/employees/1001` | tanaka | 200，员工ID 1001 |
| `GET http://localhost:8080/employees/1002` | tanaka | 403，统一JSON，不含1002资料 |
| `GET http://localhost:8080/employees/1002` | sato | 200，员工ID 1002 |
| `GET http://localhost:8080/employees` | tanaka，无查询参数 | 403，统一JSON |
| `GET http://localhost:8080/employees` | admin，无查询参数 | 200，列表JSON |
| `POST http://localhost:8080/employees` | tanaka、有效CSRF；`{"name":"Denied","department":"Sales","email":"denied@example.com"}` | 403，数据库无该邮箱 |
| `POST http://localhost:8080/employees` | admin、有效CSRF；`{"name":"New Employee","department":"Sales","email":"admin-created@example.com"}` | 201，数据库有该邮箱 |
| `PUT http://localhost:8080/employees/1001` | tanaka、有效CSRF；`{"name":"Changed","department":"Sales","email":"tanaka@example.com"}` | 403，数据库姓名不变 |
| `DELETE http://localhost:8080/employees/1002` | admin、不带CSRF；无请求体 | 403，数据库1002仍存在 |
| `POST http://localhost:8080/auth/logout` | tanaka、有效CSRF；无请求体 | 204，无响应体；旧Session后续读员工得401 |

查询列表若使用第14章筛选/分页参数，也必须继续验证COUNT与列表条件一致。若有权限而员工不存在，业务层返回404；写请求缺CSRF的403并不能证明角色权限。跨Origin浏览器联调只作为[A16跨源实验](../appendix/A16_cross_origin_session_cors.md)，不强制加入本章同源工程。排查顺序与扩展设计见[A16-02](../appendix/A16_02_security_testing_and_troubleshooting.md)。

**影响调查练习：** 接到“主管可读本部门员工”的新规格时，先列出账号与部门关系、跨部门调动、列表与COUNT共用范围条件、详情403/404策略、现有分页和索引受影响位置；本章不实现该需求。另做一次Review：如果代码仅在Controller中比较客户端传来的`employeeId`，指出越权路径并给出应使用的可信来源。

- 教师讲解重点：规格改变后旧断言为何要调整；真实登录测试和模拟权限测试各自证明什么；同一个403的来源怎样调查。
- 学员必须完成的操作：逐类运行回归，保存请求、状态、响应、数据库前后对照与失败修正记录。
- 常见错误：一律加ADMIN使旧测试“通过”；关闭CSRF或Security过滤器；只看HTTP状态不核对数据库。
- 本阶段完成标准：新旧测试均有运行结果，Session、CRUD、事务、搜索分页和权限矩阵均保持有效；遗留失败有明确原因与处理记录。

## 本章稳定状态

完成后，公开接口、登录接口和员工接口都有明确的URL级规则；普通用户只能读取与账号关联的本人记录，管理员可以执行员工管理操作；安全层的401和403继续使用统一JSON响应；旧CRUD、事务、分页和登录测试仍可回归。

你应能够根据权限矩阵解释认证、授权、角色、操作权限和数据范围的区别，并能从URL规则、Service方法检查、当前用户来源和测试证据四个位置判断一次访问为什么被允许或拒绝。

下一章将使用这一稳定安全状态进入多环境配置与打包，不在本章提前修改生产配置。
