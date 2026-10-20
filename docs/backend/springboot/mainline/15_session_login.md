# 第15章 Spring Security：注册、认证与Session登录

第14章完成后，Employee Management的员工接口仍没有登录保护。本章在原工程中完成一条连续的路径：创建数据库账号表 → 注册普通账号 → 用这个账号提交JSON登录 → 在后续请求中恢复身份 → 退出。第16章才根据角色和员工关联决定“能做什么、能看谁”。本章使用Java 17、Spring Boot 4.1.1、MyBatis Starter 4.1.0和MySQL 8.0；在Eclipse启动，用Postman验证。最终文件可在完成各阶段后与[R15源码](../reference/R15_session_login_complete_source.md)核对；认证组件和Session机制的深入讨论分别见[A15-1](../appendix/A15_01_spring_security_authentication.md)与[A15-2](../appendix/A15_02_session_cookie_csrf.md)。

本章只有`POST /auth/login`这一条JSON登录入口，不启用内置表单登录或HTTP Basic。注册接口是本地教学项目的公开自助注册示例；实际项目是否开放注册，需要按业务规格决定。

## 15-01 Spring Security在项目中做什么

原来的请求直接进入业务代码：

```text
客户端 → EmployeeController → Service → Mapper → MySQL
```

只在前端隐藏“员工列表”按钮，并不能阻止别人直接发送`GET /employees`。Spring Security是Spring应用的安全框架。在本项目中，它提供四类能力：在Controller之前检查HTTP请求、验证账号密码并建立身份、按身份决定接口是否可访问、处理CSRF保护与退出。

Spring Security使用Servlet过滤器参与请求处理。过滤器可以继续传递请求，也可以在Controller之前拒绝它。为了先理解位置，把一次请求简化为：

```text
Postman或浏览器 → Spring Security过滤器 → Spring MVC Controller → 业务层
                                  └─ 拒绝时直接返回401/403
```

**现在需要理解：** 安全过滤器位于Controller之前，可以让请求继续，也可以拒绝请求；认证确认“是谁”，授权判断“能做什么、能看哪条数据”。本章先让项目知道请求是谁，第16章再按角色和本人数据限制操作。

**先认识名称，暂不展开内部机制：** `HttpSecurity`用于配置安全规则，`SecurityFilterChain`是构建后的过滤链；标有`@Bean`的方法在启动时创建它，不是每次请求都重新创建。第2章已介绍Bean与依赖注入，15-02会结合代码解释这三个名称。更底层的协作见[A15-1](../appendix/A15_01_spring_security_authentication.md)。

登录成功还要解决一个问题：**下一次独立请求怎样知道刚才是谁登录的？** 本项目会用服务器会话（`HttpSession`）和客户端Cookie（`JSESSIONID`）连接两次请求。这里先记住两者的名称；如何保存、恢复和验证身份留到15-06。Cookie不是密码，也不是完整用户对象。

本章将沿着同一条问题链推进：

```text
为什么需要登录？
       ↓
如何阻止未登录请求？
       ↓
账号数据从哪里来？
       ↓
如何验证密码？
       ↓
如何保存登录状态？
       ↓
下一次请求如何恢复？
       ↓
如何安全退出？
```

本节不修改文件。画出匿名员工请求进入Controller之前被拒绝的路径，并说出认证与授权分别回答什么问题；跨请求身份恢复将在15-06实际验证。

## 15-02 完整案例：先让匿名员工请求返回401

这一阶段只有一个目标：`GET /health`仍可访问，匿名`GET /employees/1001`在进入Controller之前返回401。先在`EmployeeController`详情方法首行打断点，记录加入Security前的结果。

先把要实现的访问规则写成业务语言，后面再找对应代码：

- `employees`相关请求必须先登录。
- 其他请求暂时允许匿名访问，例如`GET /health`；这只是当前阶段的规则。
- 未登录却访问员工接口时返回HTTP 401，不进入`EmployeeController`。

在根目录`pom.xml`的`<dependencies>`中加入以下两个依赖并在Eclipse刷新Maven项目；运行依赖参与应用，测试依赖只供后面的MockMvc测试使用。版本由Spring Boot 4.1.1管理：

```xml
<!-- 功能：为应用加入Spring Security的请求过滤与认证能力；版本由Spring Boot管理。 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
<!-- 功能：只在测试时提供Security与MockMvc的测试辅助工具。 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security-test</artifactId>
    <scope>test</scope>
</dependency>
```

**功能说明：** 两个`dependency`均由`groupId`和`artifactId`标识；`spring-boot-starter-security`在应用运行时加入安全能力，`spring-boot-starter-security-test`通过`scope=test`限定在测试代码中使用。

**理解要点：** 刷新Maven依赖后还需重启应用，安全过滤链才会参与请求；加入测试依赖不代表已经完成认证测试。

新建`src/main/java/com/example/employee/config/SecurityConfig.java`，这是本阶段的**完整文件**：

```java
package com.example.employee.config;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

// 功能：声明本类提供Spring容器使用的安全配置。
@Configuration
public class SecurityConfig {
    // 功能：把方法返回的过滤链注册为Bean。
    @Bean
    // 功能：接收Spring提供的配置对象，在应用启动时组装请求规则。
    public SecurityFilterChain securityFilterChain(HttpSecurity http)
            throws Exception {
        // 功能：先规定需要登录的路径，再允许其余路径匿名访问。
        http.authorizeHttpRequests(authorize -> authorize
                    .requestMatchers("/auth/me", "/employees", "/employees/**")
                    .authenticated()
                    .anyRequest().permitAll())
                // 功能：未登录访问受保护路径时发送HTTP 401。
                .exceptionHandling(exceptions -> exceptions
                    .authenticationEntryPoint((request, response, exception) ->
                        response.sendError(HttpServletResponse.SC_UNAUTHORIZED)))
                // 功能：关闭本项目不用的内置表单登录入口。
                .formLogin(AbstractHttpConfigurer::disable)
                // 功能：关闭本项目不用的HTTP Basic认证入口。
                .httpBasic(AbstractHttpConfigurer::disable);
        // 功能：把上面的规则构建成供请求使用的安全过滤链。
        return http.build();
    }
}
```

**功能说明：本例的类型、对象、注解和方法**

`@Configuration`告诉Spring这是配置类；`@Bean`使返回的`SecurityFilterChain`成为容器对象。方法名`securityFilterChain`只是Bean方法名称，不是HTTP接口。参数`HttpSecurity http`是Spring注入的配置对象；`throws Exception`让构建配置的错误导致启动失败；`http.build()`生成过滤链并返回。

再按代码顺序读每个配置调用：

- `authorizeHttpRequests(authorize -> ...)`接收一个配置回调；`authorize`是在这里添加访问规则的对象，不是登录用户。该方法让过滤链按照后面的规则检查请求。
- `requestMatchers(String... paths)`在此接收三个路径字符串。`/employees`匹配列表路径，`/employees/**`匹配其子路径，`/auth/me`是稍后新增的身份查询接口。此处没有限定HTTP方法，因此匹配路径上的GET、POST等请求；它只选择请求，后面的`authenticated()`才指定要求。
- `authenticated()`不接收参数，含义是“匹配请求必须已有认证身份”。它**不会自行读取本次JSON密码并执行登录**。目前还没有登录接口，所以匿名员工请求必然不能通过。
- `anyRequest().permitAll()`让前面没有匹配的请求暂时可匿名访问，例如`/health`。规则从上到下匹配；如果把兜底规则放在前面，员工路径就不能按预期受保护。`permitAll()`只是不要求登录，不表示跳过CSRF等其他过滤器。第16章会按最终授权规格收紧兜底规则。
- `exceptionHandling(exceptions -> ...)`配置安全异常如何变成HTTP响应。它不是业务层的`@RestControllerAdvice`。
- `authenticationEntryPoint(...)`接收一个`AuthenticationEntryPoint`实现。内层Lambda的`request`是当前`HttpServletRequest`，`response`是当前`HttpServletResponse`，`exception`是认证异常。当前只使用`response`；当匿名用户请求必须登录的资源时，`sendError(SC_UNAUTHORIZED)`发送401。常量`SC_UNAUTHORIZED`的整数值是401；这一行不验证密码，也不保证生成项目统一JSON。它明确选择API的401响应，而不是跳转HTML登录页面。
- `formLogin(AbstractHttpConfigurer::disable)`关闭框架内置的表单认证入口。方法引用`AbstractHttpConfigurer::disable`可先理解成`form -> form.disable()`；关闭的是这一种登录方式，不是整个Security。项目最终使用自己的JSON登录接口，本章不创建表单入口。
- `httpBasic(AbstractHttpConfigurer::disable)`关闭HTTP Basic入口。Basic让客户端在请求头提交账号密码，不是本项目的“登录一次、后续使用Session”流程。

**理解要点：配置时机与请求结果**

这份配置分别回答三个问题：**哪些路径要求身份**、**没有身份时返回什么**、**当前不开放哪两种内置登录入口**。Lambda用于启动时配置规则，不是每次请求都重新执行整个Bean方法。它没有关闭默认CSRF检查；缺令牌的写请求可能先得到403，不能把所有403都解释为“账号密码错”。

Eclipse重启后清理Postman本站点旧Cookie，验证下表；请求均无查询参数和请求体：

| HTTP方法与URL | 预期状态与响应 |
| --- | --- |
| `GET http://localhost:8080/health` | 200，正文`OK` |
| `GET http://localhost:8080/employees` | 401，不返回员工列表 |
| `GET http://localhost:8080/employees/1001` | 401，Controller断点不命中 |

若员工接口仍返回200，检查Maven依赖、配置类包扫描和是否重启；若出现HTML登录页，检查`formLogin`是否仍启用。本阶段不要求输入账号密码。

## 15-03 建立账号表：业务员工与登录账号不是一回事

员工记录回答“这名员工的业务资料是什么”，登录账号回答“谁可以进入系统”。两者不能简单合成一张表：员工可能尚未开通账号，公开注册的人也可能尚未关联员工。接下来保留原有`employees`表，另建`app_users`表；先在MySQL 8.0的**个人练习库**确认表尚不存在，再执行：

```sql
-- 功能：选择本地练习数据库，避免在其他数据库中建表。
USE employee_db;
-- 功能：建立登录账号表；员工业务资料仍留在employees表。
CREATE TABLE app_users (
    -- 功能：为每个账号生成独立主键。
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    -- 功能：限制登录名唯一，并为密码哈希预留存储空间。
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    -- 功能：关联员工记录可为空；角色与启用标志由服务器控制。
    employee_id BIGINT NULL UNIQUE,
    role VARCHAR(30) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE
);
```

**功能说明：表中的字段和约束**

- `id`是`BIGINT`主键，`AUTO_INCREMENT`由MySQL生成新值。
- `username`是登录名，`UNIQUE`防止重复；仅在Java里“先查再插”不能代替数据库约束。
- `password_hash`存单向BCrypt哈希，注册时生成、登录时核对，不存明文。
- `employee_id`允许为空并设唯一约束；`role`保存角色；`enabled`控制账号能否进行新的登录。

**理解要点：为什么分开设计？**

- `employees`继续保存姓名、部门等业务资料；`app_users`只保存认证和授权所需的账号资料。员工没有账号时仍能保留业务记录，账号停用也不必删除员工资料。
- 公开注册的新账号尚未确认对应哪名员工，所以`employee_id=NULL`。若允许请求体自行填`1001`，注册者就能冒充该员工；员工关联应由受信任的管理流程完成。
- 注册请求不能自行指定`ADMIN`或启用状态；服务端固定`role=USER`、`enabled=true`。否则任何人都可能注册管理员账号。
- 密码不能以明文或可逆形式存入数据库；只保存哈希，登录时用`matches`核对。`enabled=false`阻止新的登录，但不会自动撤销已经建立的Session。

为第16章的“本人”实验保留课程练习账号`tanaka`，并为本章的停用测试保留一行数据。它们是**数据库测试资料**，不是写死在Java认证代码里的账号。执行前先检查这些用户名和ID是否存在；已有内容时不要重复插入或覆盖：

```sql
-- 功能：仅在个人练习库中准备一个启用账号和一个停用账号。
INSERT INTO app_users (id, username, password_hash, employee_id, role, enabled)
VALUES
    -- 功能：为第16章的本人数据实验关联员工1001。
    (1, 'tanaka',
     '$2a$10$nljJFXRpHlZ2gPsH7lM12eIan96c0Y03Y/YCucFA/SA6z.1Lv7UTC',
     1001, 'USER', TRUE),
    -- 功能：保留一条可读取但不可登录的账号记录。
    (4, 'disabled-user',
     '$2a$10$nljJFXRpHlZ2gPsH7lM12eIan96c0Y03Y/YCucFA/SA6z.1Lv7UTC',
     NULL, 'USER', FALSE);
```

**功能说明：** `INSERT INTO ... VALUES`一次插入两条预置数据；`password_hash`是BCrypt哈希，不是明文。两行的本地练习密码均为`TrainingPass123!`。

**理解要点：** 这些密码只供本地练习，不得用于真实环境。个人练习库保留此表以进入第16章；第12章的`test-data.sql`含`DROP`，只可由`MySqlTestGuard`限制的**专用测试库**使用，绝不能导入个人或共享库。

新建`src/main/java/com/example/employee/entity/AppUser.java`，以下是完整文件：

```java
package com.example.employee.entity;

// 功能：承载app_users表的一行数据，不直接执行认证。
public class AppUser {
    // 功能：字段分别保存主键、登录名、密码哈希、员工关联、角色和启用状态。
    private Long id;
    private String username;
    private String passwordHash;
    private Long employeeId;
    private String role;
    private boolean enabled;

    // 功能：getter供业务代码和MyBatis读取字段，setter供其写入字段。
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

**功能说明：** `AppUser`承载`app_users`的一行数据；字段与表列对应。沿用第9章的下划线转驼峰与类型别名配置，`password_hash`映射到`passwordHash`。getter/setter仍是已学的普通Java方法。

**理解要点：** 这是数据库账号对象，不是“当前已登录用户”；不要把`employees`业务对象直接交给认证组件。没有映射配置时先补齐第9章设置，不要把查询得到的空哈希当成正确账号。

新建`src/main/java/com/example/employee/mapper/AppUserMapper.java`：

```java
package com.example.employee.mapper;

import com.example.employee.entity.AppUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

// 功能：让MyBatis为接口创建数据库访问实现。
@Mapper
public interface AppUserMapper {
    // 功能：把方法参数命名为username，供XML中的#{username}绑定。
    AppUser findByUsername(@Param("username") String username);
    // 功能：插入一条账号记录，返回受影响行数。
    int insert(AppUser account);
}
```

再新建`src/main/resources/mapper/AppUserMapper.xml`：

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "https://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.example.employee.mapper.AppUserMapper">
    <!-- 功能：按用户名查询账号；查询列供AppUser映射使用。 -->
    <select id="findByUsername" resultType="AppUser">
        SELECT id, username, password_hash, employee_id, role, enabled
        FROM app_users
        WHERE username = #{username}
    </select>
    <!-- 功能：插入账号并把MySQL生成的id回填到对象。 -->
    <insert id="insert" parameterType="AppUser"
            useGeneratedKeys="true" keyProperty="id">
        INSERT INTO app_users
            (username, password_hash, employee_id, role, enabled)
        VALUES
            (#{username}, #{passwordHash}, #{employeeId}, #{role}, #{enabled})
    </insert>
</mapper>
```

**功能说明：** `@Mapper`让MyBatis创建接口实现；`@Param("username")`把方法参数交给XML的`#{username}`。`namespace`、`id`分别对应接口和方法；`findByUsername`查不到时返回`null`，`insert`返回影响行数，并通过`useGeneratedKeys`把MySQL生成的ID写回对象。这些映射写法沿用第9章。

**理解要点：** 账号Mapper只负责从`app_users`读写资料，不验证密码，也不决定谁是管理员。用户名必须参数绑定，不能拼进SQL。此阶段只创建代码，暂不发送注册请求。

## 15-04 完成公开注册：JSON到数据库

### 15-04-1 先确定注册业务

`POST /auth/register`接收JSON中的`username`和`password`。成功时返回201和新账号的ID、用户名，但**不自动登录**；客户端之后仍需调用`POST /auth/login`。缺字段或格式不合适返回400，同名返回409。请求中的`role`、`employeeId`、`enabled`不能决定数据库值：公开注册由服务端固定`role=USER`、`employee_id=NULL`、`enabled=true`，不能让注册者自行获得ADMIN身份或冒充某名员工。

这一步的可检查结果是：`app_users`新增一行，`password_hash`不是明文，返回体没有密码；重复注册不新增第二行。Controller负责HTTP输入输出，Service负责账号规则与密码编码，15-03的Mapper负责写库。注册成功只代表账号写入，不代表已经建立登录Session。

### 15-04-2 注册请求怎样被处理

请求先经过Security过滤链的CSRF检查；通过后才进入`AuthController.register(...)`，由JSON绑定和`@Valid`检查用户名、密码，再交给`AccountRegistrationService.register(...)`。Service创建`AppUser`，只从请求读取用户名和原始密码；密码经编码后才写入`password_hash`，角色、员工关联和启用状态由服务端决定。`AppUserMapper.insert(...)`把账号存入`app_users`，生成的ID随响应返回。各失败出口按发生的位置分别处理：

```text
POST /auth/register
         ↓
Security过滤链：CSRF检查
    ├─ 失败 → 403（Controller尚未执行）
    └─ 通过
         ↓
JSON绑定＋@Valid
    ├─ 失败 → 400
    └─ 通过
         ↓
AccountRegistrationService
         ↓
PasswordEncoder.encode()
         ↓
AppUserMapper.insert()
    ├─ 用户名重复 → 409
    └─ 成功 → 201
```

图中只标出本节重点的请求结果；403、400、409和201分别对应不同检查点，不能统称为“注册失败”。

下面是即将创建的`AccountRegistrationService.register(...)`中的**关键语句片段**，完整方法和异常处理在后面的操作代码中：

```java
// 功能：对本次输入密码生成单向哈希，保存到账号对象。
account.setPasswordHash(encoder.encode(request.getPassword()));
// 功能：由服务端指定普通角色和暂未关联员工的状态。
account.setRole("USER");
account.setEmployeeId(null);
account.setEnabled(true);
// 功能：通过Mapper将账号写入app_users表；完整方法还要检查影响行数与重名异常。
mapper.insert(account);
```

**功能说明：** `encoder`是构造器注入的`PasswordEncoder` Bean，不是此处临时创建的对象；`request.getPassword()`取得本次原始输入，`encode(...)`返回可存储的BCrypt哈希，`setPasswordHash(...)`只把哈希放进`AppUser`。`setRole(...)`、`setEmployeeId(...)`和`setEnabled(...)`写入服务器确定的值，不读取客户端同名字段。`mapper.insert(account)`调用15-03的MyBatis接口执行INSERT，并将数据库生成的ID回填到对象。

**为什么要编码密码？** 原始密码只用于这次注册请求，不能存进账号表。`PasswordEncoder`提供`encode(原始密码)`和`matches(原始密码, 已保存的哈希)`两项不同操作：前者返回要保存的哈希字符串，后者返回是否匹配的`boolean`，不“解密”哈希。本项目的`BCryptPasswordEncoder`使用随机盐与可调工作因子，哈希文本包含以后核对所需的信息；相同密码多次编码，文本通常也不同。15-05配置的认证Provider会在15-06登录时使用`matches(...)`，不能重新`encode(...)`后比较两个字符串。密码哈希降低数据库内容泄露时直接暴露原始密码的风险，但不能替代访问控制、传输保护和妥善管理数据库。

**理解要点：** 密码哈希与角色赋值都必须在服务端完成；Mapper只负责保存，不负责决定注册者能否成为ADMIN。注册名的唯一约束由数据库最终保证；完整Service会把同名插入异常转换为409，不能只靠“先查询有没有”防重复。

### 15-04-3 编写完整注册代码

新建请求和响应类，均为完整文件：

```java
// src/main/java/com/example/employee/dto/request/RegisterRequest.java
package com.example.employee.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

// 功能：接收注册JSON中的用户名和密码，不接收角色或员工ID。
public class RegisterRequest {
    // 功能：拒绝空用户名、超长用户名和不符合本例字符规则的用户名。
    @NotBlank(message = "用户名不能为空")
    @Size(max = 50, message = "用户名不能超过50个字符")
    @Pattern(regexp = "[A-Za-z0-9._-]+", message = "用户名格式错误")
    private String username;

    // 功能：拒绝空密码和超出本例长度范围的密码。
    @NotBlank(message = "密码不能为空")
    @Size(min = 12, max = 64, message = "密码长度应为12到64个字符")
    private String password;

    // 功能：供JSON绑定和业务服务读写请求字段。
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
```

```java
// src/main/java/com/example/employee/dto/response/RegisteredUserResponse.java
package com.example.employee.dto.response;
// 功能：注册成功时只返回账号ID和用户名。
public record RegisteredUserResponse(Long id, String username) {}
```

`AuthController`还要返回CSRF令牌资料，先新增这个完整响应类；令牌怎样取得、怎样提交见本节“CSRF要求”：

```java
// src/main/java/com/example/employee/dto/response/CsrfResponse.java
package com.example.employee.dto.response;
// 功能：保存本次CSRF令牌的参数名、请求头名和提交值。
public record CsrfResponse(String parameterName, String headerName, String token) {}
```

**功能说明：** `RegisterRequest`只接收用户名和密码：用户名不能为空、最多50位且限于示例中的字符；密码不能为空且为12～64位。Controller中的`@Valid`触发这些规则，非法输入返回400。`RegisteredUserResponse`只返回ID和用户名，不包含密码或哈希。

**理解要点：** 请求DTO不包含`role`或`employeeId`，不能让客户端决定权限。这里的密码长度只是本地输入规则，不等于完整生产密码策略。响应不含密码哈希；第8章的400处理继续适用。

在`SecurityConfig.java`中再增加以下Bean及`BCryptPasswordEncoder`、`PasswordEncoder`导入，供注册和后续登录共用同一个编码方案：

```java
// 功能：把密码编码器注册为Spring Bean，供注册和认证共用。
@Bean
public PasswordEncoder passwordEncoder() {
    // 功能：创建工作因子为10的BCrypt实现。
    return new BCryptPasswordEncoder(10);
}
```

**功能说明：** `@Bean`注册返回对象；`PasswordEncoder`是编码与核对接口，`BCryptPasswordEncoder`是本项目的实现；构造器参数`10`是本地练习的BCrypt工作因子，允许范围为4～31。工作因子越高，编码和核对所需计算量通常越大；它不是“密码长度10位”或“加密10次”。注册时`encode(rawPassword)`产生哈希，登录时`matches(rawPassword, storedHash)`核对并返回布尔值；两次操作使用相同编码方案，但不要求使用同一个Java实例。真实项目应结合机器性能与安全要求测量后确定工作因子。

**理解要点：** 同一密码每次生成的哈希文本可能不同；不能在登录时重新`encode`后比较字符串。

新建`src/main/java/com/example/employee/security/AccountRegistrationService.java`：

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

// 功能：让Spring创建负责注册业务的服务对象。
@Service
public class AccountRegistrationService {
    private final AppUserMapper mapper;
    private final PasswordEncoder encoder;

    // 功能：接收数据库访问对象和密码编码对象，不在方法中临时创建它们。
    public AccountRegistrationService(AppUserMapper mapper, PasswordEncoder encoder) {
        this.mapper = mapper;
        this.encoder = encoder;
    }

    // 功能：把已校验的注册输入转换为固定USER角色的数据库账号。
    public RegisteredUserResponse register(RegisterRequest request) {
        // 功能：只从请求中读取用户名和密码；角色、员工ID、状态由服务器决定。
        AppUser account = new AppUser();
        account.setUsername(request.getUsername());
        account.setPasswordHash(encoder.encode(request.getPassword()));
        account.setRole("USER");
        account.setEmployeeId(null);
        account.setEnabled(true);
        // 功能：写入数据库并检查影响行数。
        try {
            if (mapper.insert(account) != 1) {
                throw new IllegalStateException("account insert affected no row");
            }
        // 功能：把同名账号的数据库约束冲突转换为业务异常。
        } catch (DuplicateKeyException exception) {
            throw new DuplicateUsernameException();
        }
        // 功能：只返回新账号ID和用户名，不返回哈希。
        return new RegisteredUserResponse(account.getId(), account.getUsername());
    }
}
```

**功能说明：** 构造器取得前面准备的Mapper与密码编码器。注册方法把哈希和服务端确定的账号属性写入数据库，检查影响行数，再返回不含密码的摘要；数据库唯一约束触发的`DuplicateKeyException`转换为`DuplicateUsernameException`，由HTTP处理器返回409。

**理解要点：** Service不从HTTP请求取得角色或员工ID；数据库唯一约束才是并发重名的最终保障。不能把SQL与堆栈返回客户端。此例只插入账号表一行，不需要额外引入多写事务；实际开放注册还需防滥用、账号验证或审批，不能把教学接口直接上线。

新建`src/main/java/com/example/employee/controller/AuthController.java`，本阶段的**完整文件**只有CSRF与注册两个接口：

```java
package com.example.employee.controller;

import com.example.employee.common.ApiResponse;
import com.example.employee.dto.request.RegisterRequest;
import com.example.employee.dto.response.CsrfResponse;
import com.example.employee.dto.response.RegisteredUserResponse;
import com.example.employee.security.AccountRegistrationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// 功能：把返回值写成HTTP响应体，并给两个端点统一加/auth前缀。
@RestController
@RequestMapping("/auth")
public class AuthController {
    private final AccountRegistrationService registrationService;

    // 功能：注入注册服务；Controller不直接写数据库。
    public AuthController(AccountRegistrationService registrationService) {
        this.registrationService = registrationService;
    }

    // 功能：向客户端返回当前请求可用的CSRF Header名和令牌值。
    @GetMapping("/csrf")
    public ResponseEntity<ApiResponse<CsrfResponse>> csrf(CsrfToken token) {
        return ResponseEntity.ok(ApiResponse.success(new CsrfResponse(
                token.getParameterName(), token.getHeaderName(), token.getToken())));
    }

    // 功能：校验注册JSON，调用服务并返回201与账号摘要。
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<RegisteredUserResponse>> register(
            @Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(registrationService.register(request)));
    }
}
```

**功能说明：** `csrf(CsrfToken token)`把当前请求的令牌资料交给客户端；`register(...)`接收并校验JSON，调用注册服务，把成功结果包装为201响应。`@RestController`、`@RequestBody`、`@Valid`和`ResponseEntity`的基础用法沿用第3、4、8章。

**理解要点：** Controller只负责HTTP输入输出并调用服务，不直接拼SQL或编码密码；匿名允许访问不等于POST免除CSRF检查。

### 15-04-4 看清注册代码的依赖关系

```text
AuthController
    └─ AccountRegistrationService
         ├─ AppUserMapper → app_users表
         └─ PasswordEncoder → BCryptPasswordEncoder
```

- `AppUserMapper`在15-03由MyBatis创建，负责执行INSERT与回填ID；它不知道原始密码怎样处理，也不决定角色。
- `PasswordEncoder`由本节`SecurityConfig.passwordEncoder()`注册为Spring Bean。Spring创建`AccountRegistrationService`时把这个对象传给构造器的`encoder`参数；`encoder.encode(request.getPassword())`返回哈希，再由Mapper保存。它不是Service每次收到请求时`new`出来的，也不是数据库自动提供的功能。
- Spring创建`AuthController`时注入`AccountRegistrationService`。Controller不持有Mapper或编码器，因此输入校验、业务规则与写库职责不会混在HTTP方法里。

这条链也是15-06登录的前置条件：本节必须先在`app_users`成功写入带BCrypt哈希的`learner15`，15-05才能从数据库读取其认证资料，15-06才能用同一编码方案验证密码。若只是复制账号表结构而没有完成注册，后面的登录请求没有可认证的练习账号。

### 15-04-5 处理重名与非法输入

新建`src/main/java/com/example/employee/exception/DuplicateUsernameException.java`：

```java
package com.example.employee.exception;

// 功能：把数据库唯一约束冲突转换成可被HTTP异常处理器识别的业务异常。
public class DuplicateUsernameException extends RuntimeException {
    // 功能：固定对外可理解的冲突消息，不暴露SQL细节。
    public DuplicateUsernameException() {
        super("用户名已存在");
    }
}
```

**功能说明：** `DuplicateUsernameException`表示注册名冲突；Service捕获数据库的`DuplicateKeyException`后抛出它。数据库唯一约束是并发注册同名时的最终保障，仅先查询用户名是否存在不能代替约束。

新建`src/main/java/com/example/employee/exception/AuthExceptionHandler.java`，本阶段先处理用户名冲突；15-06会加入认证失败处理：

```java
package com.example.employee.exception;

import com.example.employee.common.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

// 功能：集中把Controller中的业务异常转换为HTTP响应。
@RestControllerAdvice
public class AuthExceptionHandler {
    // 功能：捕获注册重名异常，返回409与统一失败响应体。
    @ExceptionHandler(DuplicateUsernameException.class)
    public ResponseEntity<ApiResponse<Void>> duplicateUsername(
            DuplicateUsernameException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.failure(exception.getMessage()));
    }
}
```

**功能说明：** `@ExceptionHandler(DuplicateUsernameException.class)`把注册重名转换为409与统一失败响应；`@Valid`发现请求字段不合法时沿用第8章已有的校验异常处理，返回400。不要把密码校验失败的401、输入错误的400与重名的409混为一谈。

**理解要点：** 这里不处理Security过滤器在进入Controller前给出的CSRF 403；认证失败的401处理要在15-06加入。

### 15-04-6 匿名注册为什么仍需CSRF令牌

15-02允许匿名访问注册接口，但“无需登录”不等于“写请求无需安全检查”。如果直接`POST /auth/register`而不提交有效CSRF令牌，请求会在进入Controller前得到403；此时数据库不应新增账号。这与`@Valid`产生的400、重名产生的409是不同原因。

本项目让客户端先`GET /auth/csrf`，读取响应体中的`data.headerName`与`data.token`，保留Cookie；再把令牌放进指定请求头发送注册POST。前面完整`AuthController`中的`csrf(CsrfToken token)`负责返回这些资料，`CsrfResponse`只承载输出值，不保存登录身份。CSRF为什么需要请求头、令牌如何更新，见[15-07](#15-07-csrf与退出区分令牌和登录会话)及[A15-2](../appendix/A15_02_session_cookie_csrf.md)。

在现有`SecurityConfig.java`顶部增加`CookieCsrfTokenRepository`和`CsrfTokenRepository`的导入，在类内追加以下Bean，后续阶段继续使用同一仓库：

```java
// 功能：把本项目选择的Cookie令牌仓库注册为Spring Bean。
@Bean
public CsrfTokenRepository csrfTokenRepository() {
    // 功能：创建负责保存与读取预期CSRF令牌的实现。
    return new CookieCsrfTokenRepository();
}
```

**功能说明：** `CsrfTokenRepository`是令牌仓库接口，`CookieCsrfTokenRepository`是本项目的实现。这个Bean只提供仓库对象；还须让过滤链使用它。保存CSRF令牌的Cookie与记录登录会话的`JSESSIONID`用途不同。

再用下面的**完整方法**替换15-02的`securityFilterChain`方法，类中其余内容不变：

```java
// 功能：保留原有访问规则，并为写请求指定CSRF令牌仓库。
@Bean
public SecurityFilterChain securityFilterChain(
        HttpSecurity http, CsrfTokenRepository csrfTokenRepository)
        throws Exception {
    // 功能：让过滤链用上面创建的仓库检查CSRF令牌。
    http.csrf(csrf -> csrf.csrfTokenRepository(csrfTokenRepository))
            // 功能：保留15-02的匿名与登录要求。
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers("/auth/me", "/employees", "/employees/**")
                    .authenticated()
                .anyRequest().permitAll())
            // 功能：匿名访问受保护资源时仍返回401。
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint((request, response, exception) ->
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED)))
            // 功能：继续关闭两种不用的内置登录入口。
            .formLogin(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable);
    // 功能：构建更新后的过滤链。
    return http.build();
}
```

**功能说明：** 方法参数`csrfTokenRepository`是Spring注入的上一个Bean；`http.csrf(...)`把它交给过滤链，`http.build()`完成配置。其余访问规则沿用15-02。配置在启动时生效，不是在调用`GET /auth/csrf`时才创建安全过滤链。缺少或提交错误的令牌会在Controller之前得到403。

### 15-04-7 用Postman和数据库验证注册

按顺序在Postman实验。先`GET http://localhost:8080/auth/csrf`（无参数、无请求体），保留Cookie并读出`data.headerName`和`data.token`；本阶段后续POST都把该值放入对应Header：

| HTTP方法与URL | JSON请求体或条件 | 预期状态、响应与数据库证据 |
| --- | --- | --- |
| `POST http://localhost:8080/auth/register` | `{"username":"learner15","password":"TrainingPass123!"}`，有效CSRF | 201；`data.username=learner15`且有ID；库中只有哈希、`role=USER`、`employee_id=NULL` |
| 再次`POST /auth/register` | 同一个用户名、有效CSRF | 409；账号仍只有一行 |
| `POST /auth/register` | `{"username":"","password":"short"}`，有效CSRF | 400；数据库不新增 |
| `POST /auth/register` | 正确JSON但没有CSRF Header | 403；数据库不新增 |

在个人练习库执行下面的**MySQL 8.0查询**，确认成功注册得到一行；重复注册、400及403场景之后仍只应有这一行。`password_hash`应有值且不等于请求中的明文密码，`role`必须是`USER`，`employee_id`必须是`NULL`：

```sql
SELECT id, username, password_hash, employee_id, role, enabled
FROM app_users
WHERE username = 'learner15';
```

每次重做成功场景换一个未使用的练习用户名，或只在个人练习库核对实际ID后删除本次临时账号；不要删除`tanaka`及`disabled-user`。不能把完整Cookie、明文密码或哈希放进共享测试证据。**注册成功并没有建立登录Session**：在没有旧登录Cookie的Postman会话中，注册后调用`GET /auth/me`应仍为401；如果Postman原来已登录，先清除旧Cookie再做这个检查，避免把旧身份误认为注册自动登录。

## 15-05 从数据库账号得到认证资料

现在数据库已有`learner15`，但Spring Security还不知道怎样读取它。本节先实现“按用户名取得账号资料”，再把这个读取对象与密码编码器交给认证组件。取得账号资料与验证本次密码是两个不同动作。

新建`src/main/java/com/example/employee/security/AppUserDetailsService.java`，这是完整文件：

```java
package com.example.employee.security;

import com.example.employee.entity.AppUser;
import com.example.employee.mapper.AppUserMapper;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

// 功能：将数据库账号转换为Spring Security需要的账号资料。
@Service
public class AppUserDetailsService implements UserDetailsService {
    private final AppUserMapper mapper;

    // 功能：注入按用户名查询账号的Mapper。
    public AppUserDetailsService(AppUserMapper mapper) {
        this.mapper = mapper;
    }

    // 功能：按用户名加载账号资料；此处不核对本次输入的密码。
    @Override
    public UserDetails loadUserByUsername(String username) {
        // 功能：查询账号；不存在时告诉认证器无法取得该用户。
        AppUser account = mapper.findByUsername(username);
        if (account == null) {
            throw new UsernameNotFoundException("login failed");
        }
        // 功能：把哈希、角色和启用状态转换为UserDetails。
        return User.withUsername(account.getUsername())
                .password(account.getPasswordHash())
                .roles(account.getRole())
                .disabled(!account.isEnabled())
                .build();
    }
}
```

**功能说明：** `@Service`让Spring创建对象，`UserDetailsService`是Security读取账号的接口，`@Override`表示这里实现其`loadUserByUsername(String)`方法。输入参数仅是用户名，返回`UserDetails`资料；查不到时抛`UsernameNotFoundException`。`User.withUsername(...)`开始构建资料，`password(...)`接收数据库哈希，`roles("USER")`产生`ROLE_USER`权限，`disabled(!enabled)`把启用标志取反为停用标志，`build()`返回`UserDetails`。

**理解要点：** `UserDetails`不表示“已登录”；密码核对发生在后面的Provider中。表里的`role`不存`ROLE_`前缀；第16章才实际用角色判断接口访问。

在`SecurityConfig.java`类中新增下面的认证Bean及对应导入，保留15-04的`PasswordEncoder`：

```java
// 功能：把方法返回的认证管理器交给Spring容器管理。
@Bean
// 功能：接收容器提供的账号读取对象和密码核对对象，组装认证入口。
public AuthenticationManager authenticationManager(
        AppUserDetailsService users, PasswordEncoder encoder) {
    // 功能：指定由哪个对象按用户名读取账号资料。
    DaoAuthenticationProvider provider = new DaoAuthenticationProvider(users);
    // 功能：指定用哪种密码编码方案核对本次输入与数据库哈希。
    provider.setPasswordEncoder(encoder);
    // 功能：让管理器把用户名／密码认证请求交给上面的Provider。
    return new ProviderManager(provider);
}
```

**功能说明：示例中出现的类型、对象、注解和方法**

- `@Bean`是Spring注解，标在配置类的方法上。Spring启动时调用该方法，将返回对象注册到容器，供其他Bean注入；它不会在每次登录时执行。
- `AuthenticationManager`是Spring Security的认证入口接口，定义`authenticate(...)`方法：传入待验证的`Authentication`，成功返回已认证的`Authentication`，失败抛认证异常。本方法声明返回这个接口类型，15-06的登录服务也依赖这个接口。
- `AppUserDetailsService`是本项目实现`UserDetailsService`的类；参数`users`是它的Bean对象。它通过Mapper按用户名读取数据库账号，并转换成`UserDetails`，不核对本次输入的密码。
- `PasswordEncoder`是密码编码与核对接口；参数`encoder`是15-04配置的`BCryptPasswordEncoder`对象。注册使用`encode(原始密码)`生成哈希；认证时使用`matches(本次原始密码, 数据库哈希)`返回是否匹配。它不查询账号。
- `DaoAuthenticationProvider`是Spring Security提供的用户名／密码认证器，实现`AuthenticationProvider`接口；局部变量`provider`指向这个新建对象。构造器`DaoAuthenticationProvider(users)`指定账号资料来源；`setPasswordEncoder(encoder)`指定密码核对方式。收到认证请求后，它读取`UserDetails`、检查账号状态并核对密码；它不直接编写SQL。
- `ProviderManager`是`AuthenticationManager`的实现类。`new ProviderManager(provider)`把这个Provider交给管理器；本例只有一种用户名／密码认证方式。管理器负责转交请求，本身不查数据库或核对密码。

把两个示例放在一起看，登录时的对象协作关系是：

```text
待验证的用户名和原始密码
           ↓
AuthenticationManager（统一入口接口）
           ↓
ProviderManager（本项目的实现）
           ↓
DaoAuthenticationProvider（执行用户名／密码认证）
     ├─ AppUserDetailsService（实现UserDetailsService）
     │   ├─ AppUserMapper → MySQL的app_users表
     │   │  ← 返回AppUser数据库对象
     │   └─ 转换成UserDetails：账号哈希、状态、角色
     └─ PasswordEncoder.matches(原始密码, 数据库哈希)
           ↓
认证成功：返回已认证的Authentication；失败：抛认证异常
```

**理解要点：为什么不能只保留Mapper或PasswordEncoder？**

- `AppUserMapper`只负责查询数据库，返回项目的`AppUser`，不知道Spring Security需要什么账号格式。
- `AppUserDetailsService`调用Mapper，再把`AppUser`转换成`UserDetails`。这一步只是**提供账号资料**；即使找到了用户，也不代表本次密码正确或已经登录。
- `PasswordEncoder`只会编码或比较密码，不负责查找用户、检查账号是否停用。`DaoAuthenticationProvider`把账号资料、状态检查和密码比较组合成一次认证；`ProviderManager`通过`AuthenticationManager`入口把认证请求交给它。只有认证成功返回的`Authentication`才表示本次身份已经验证。

**理解要点：区分组装与认证**

运行这段`@Bean`方法是在**应用启动时组装对象**：`users`和`encoder`由Spring按类型传入，方法创建Provider和Manager，并把Manager注册为Bean。15-06调用`authenticationManager.authenticate(...)`才是**处理一次登录**，顺序为`ProviderManager → DaoAuthenticationProvider → AppUserDetailsService／PasswordEncoder`。因此不能把`new ProviderManager(...)`理解为每次请求新建登录对象，也不能把返回Manager理解为已经登录成功。

认证失败在这里表现为异常，认证组件不会自行生成HTTP 401；15-06的异常处理器才将异常转换为对外的401，且不区分“用户名不存在”和“密码错误”。此时还没有HTTP登录接口；Mapper测试和密码匹配测试可在[R15的独立测试节](../reference/R15_session_login_complete_source.md)核对。

## 15-06 实现唯一JSON登录并保存Session

目标请求是`POST /auth/login`，请求体为`{"username":"learner15","password":"TrainingPass123!"}`。认证成功必须同时满足两件事：本次密码验证通过；**另一条请求**仍能识别当前用户名。只返回200而不保存身份不是完成登录。

一次成功登录的业务过程是：客户端提交JSON；`AuthController`把用户名、密码交给`SessionLoginService`；登录服务请Spring Security验证；验证成功后处理会话并保存身份；接口只返回用户名。客户端在下一次请求带回`JSESSIONID`，服务器才能找到先前保存的身份。密码不正确时不会进入保存身份的步骤，而是返回401。本项目只有这一条`POST /auth/login`登录入口，不另做一套表单登录。

先新增`src/main/java/com/example/employee/dto/request/LoginRequest.java`：

```java
package com.example.employee.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// 功能：接收一次登录请求提交的用户名和原始密码。
public class LoginRequest {
    // 功能：拒绝空白或过长的用户名。
    @NotBlank(message = "用户名不能为空")
    @Size(max = 50, message = "用户名不能超过50个字符")
    private String username;
    // 功能：拒绝空白或过长的密码；不修改密码内容。
    @NotBlank(message = "密码不能为空")
    @Size(max = 200, message = "密码不能超过200个字符")
    private String password;

    // 功能：供JSON绑定和登录服务读取字段。
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
```

再新增只返回用户名的完整响应类：

```java
// src/main/java/com/example/employee/dto/response/CurrentUserResponse.java
package com.example.employee.dto.response;
// 功能：成功响应只包含当前用户名。
public record CurrentUserResponse(String username) {}
```

**功能说明：** `LoginRequest`是JSON请求DTO，`CurrentUserResponse`是只含用户名的响应`record`。`@NotBlank`拒绝空白，`@Size(max = 50/200)`限制长度，getter/setter供JSON绑定和服务读取；15-06的Controller用`@Valid`触发校验，失败沿用已有400处理。

**理解要点：** 登录时不返回密码、哈希、完整认证对象或Session ID。`LoginRequest`只装本次输入；密码不做`trim()`，因为首尾空格可能是密码的一部分。

先看`SessionLoginService.login(...)`如何完成登录，再看它依赖哪些配置。下面三段均为**同一个方法中的关键片段**，不是三个可分别运行的登录版本；稍后会给出包含导入、构造器和完整方法的文件。

**第一步：验证本次输入。**

```java
// 这只是待验证的用户名密码，不代表已经登录。
Authentication input = UsernamePasswordAuthenticationToken.unauthenticated(
        username, password);
// 验证失败会抛出认证异常；成功才返回已认证结果。
Authentication result = authenticationManager.authenticate(input);
```

- `Authentication`是Spring Security表示一次认证输入或结果的接口。`input`尚未通过验证；`result`是认证成功后返回的身份，包含认证后的用户名等资料。
- `AuthenticationManager`是登录服务需要的认证入口。这里调用`authenticate(input)`，实际查账号、检查状态和核对密码的工作沿用15-05组装的Provider，而不是由Controller或Mapper单独完成。

**第二步：处理认证成功后的会话。**

```java
// 使用已验证的身份及当前HTTP请求、响应执行会话安全动作。
sessionStrategy.onAuthentication(result, request, response);
```

- `SessionAuthenticationStrategy`不是密码验证器；它在认证成功后处理会话安全事项，例如更换已有Session ID，避免沿用登录前的标识。
- `request`和`response`是当前的`HttpServletRequest`、`HttpServletResponse`。会话策略可能需要读取或更新本次会话，并让客户端获知更新后的Cookie，所以不能只传入用户名。此步骤**不等于**已经保存跨请求身份。

**第三步：设置本次身份，并保存供后续请求恢复。**

```java
// 建立当前请求可读取的安全上下文。
SecurityContext context = SecurityContextHolder.createEmptyContext();
context.setAuthentication(result);
SecurityContextHolder.setContext(context);
// 显式保存到Session仓库，供另一条请求恢复身份。
contextRepository.saveContext(context, request, response);
```

- `SecurityContext`装着已认证的`Authentication`；`SecurityContextHolder.setContext(...)`使本次请求能读取它，但不能代替跨请求保存。
- `SecurityContextRepository`负责保存和读取这个上下文。`saveContext(...)`接收上下文及当前请求、响应，将身份保存到服务器会话。下一次请求带回`JSESSIONID`时，安全过滤链从同一仓库恢复身份；少了这一步，就可能出现“登录200，随后`/auth/me`仍为401”。

这是一项**直接参与HTTP认证生命周期的服务**，因此`login(...)`接收Servlet请求和响应；普通业务Service不应照搬这种参数设计。三个依赖由Spring通过构造器提供，完整服务文件见下文。现在再查看它们由哪些Bean提供。

把`SecurityConfig.java`**完整替换**为下方本阶段快照。它保留注册与CSRF设置，为上述三个动作提供认证管理器、会话策略和身份仓库；第七阶段再增加Logout设置：

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

// 功能：集中提供本阶段所需的密码、CSRF、认证与Session相关Bean。
@Configuration
public class SecurityConfig {
    // 功能：注册与登录共用同一种BCrypt密码编码方案。
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }

    // 功能：保存和读取预期CSRF令牌。
    @Bean
    public CsrfTokenRepository csrfTokenRepository() {
        return new CookieCsrfTokenRepository();
    }

    // 功能：把认证身份存入服务器HttpSession，供后续请求恢复。
    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    // 功能：把数据库账号读取器和密码编码器组装成认证入口。
    @Bean
    public AuthenticationManager authenticationManager(
            AppUserDetailsService users, PasswordEncoder encoder) {
        // 功能：用户名密码Provider负责实际账号状态与密码检查。
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(users);
        provider.setPasswordEncoder(encoder);
        return new ProviderManager(provider);
    }

    // 功能：认证成功后依次更换已有Session ID并清除旧CSRF令牌。
    @Bean
    public SessionAuthenticationStrategy sessionAuthenticationStrategy(
            CsrfTokenRepository csrfTokenRepository) {
        return new CompositeSessionAuthenticationStrategy(List.of(
                new ChangeSessionIdAuthenticationStrategy(),
                new CsrfAuthenticationStrategy(csrfTokenRepository)));
    }

    // 功能：把CSRF、Session恢复、路径要求和401响应组装成过滤链。
    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http, SecurityContextRepository repository,
            CsrfTokenRepository csrfTokenRepository) throws Exception {
        // 功能：写请求检查令牌，并让过滤链使用与登录服务相同的身份仓库。
        http.csrf(csrf -> csrf.csrfTokenRepository(csrfTokenRepository))
                .securityContext(context -> context
                    .requireExplicitSave(true)
                    .securityContextRepository(repository))
                // 功能：公开注册与登录路径，保护身份查询和员工接口。
                .authorizeHttpRequests(authorize -> authorize
                    .requestMatchers("/auth/csrf", "/auth/register",
                            "/auth/login", "/health").permitAll()
                    .requestMatchers("/auth/me", "/employees", "/employees/**")
                        .authenticated()
                    .anyRequest().permitAll())
                // 功能：匿名访问受保护资源时返回401。
                .exceptionHandling(exceptions -> exceptions
                    .authenticationEntryPoint((request, response, exception) ->
                        response.sendError(HttpServletResponse.SC_UNAUTHORIZED)))
                // 功能：仍不开放表单登录和HTTP Basic两种入口。
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable);
        // 功能：创建供请求使用的过滤链。
        return http.build();
    }
}
```

**功能说明：本快照中的类型、对象和方法**

- `@Configuration`声明配置类；各`@Bean`方法在启动时提供容器对象。`PasswordEncoder`与`AuthenticationManager`的具体作用见15-04和15-05。
- `CsrfTokenRepository`仍是15-04的**令牌仓库**，不是登录状态仓库。`SecurityContextRepository`是另一个接口；这里的`HttpSessionSecurityContextRepository`实现负责在服务器Session中保存和加载`SecurityContext`。过滤链与下方登录服务使用同一个仓库Bean。
- `SessionAuthenticationStrategy`是登录成功后处理会话的接口。这里组合了更换已有Session ID与清除登录前CSRF令牌两个动作；`CompositeSessionAuthenticationStrategy(List.of(...))`按列表顺序调用它们。本节只需知道组合的顺序和可观察结果，内部更新过程见[A15-2](../appendix/A15_02_session_cookie_csrf.md)。
- `http.securityContext(...)`配置身份仓库；`requireExplicitSave(true)`要求自定义登录代码显式调用`saveContext`。`requestMatchers(...).permitAll()`只允许匿名访问列出的路径，`authenticated()`要求身份，`authenticationEntryPoint(...)`负责匿名401，`build()`返回过滤链。

**理解要点：** 令牌仓库与身份仓库不能互换；会话策略也不会验证密码或代替`saveContext`。公开的`/auth/register`和`/auth/login`虽允许匿名调用，仍必须通过CSRF写请求检查。

三个新依赖与登录服务的关系如下；箭头表示“提供给／由其使用”，不是每次请求重新创建Bean：

```text
SecurityConfig
├─ AuthenticationManager Bean ──────── 验证本次用户名和密码
├─ SessionAuthenticationStrategy Bean ─ 登录成功后处理会话安全动作
└─ SecurityContextRepository Bean ───── 保存并恢复跨请求身份
             └─ 以上三个Bean经构造器注入SessionLoginService
                                      ↓
                              AuthController调用登录服务
```

把图中的对象按创建时机再对照一次：

| 对象 | 在什么时候创建或取得 | 谁使用 |
| --- | --- | --- |
| `AuthenticationManager` | 应用启动时创建Bean | `SessionLoginService` |
| `SessionAuthenticationStrategy` | 应用启动时创建Bean | `SessionLoginService` |
| `SecurityContextRepository` | 应用启动时创建Bean | `SessionLoginService`及Security过滤链 |
| `Authentication input` | 每次登录请求中创建 | `AuthenticationManager` |
| `Authentication result` | 本次认证成功时返回 | `SessionLoginService` |
| `SecurityContext` | 登录成功后为本次身份创建并保存 | Security过滤链及读取当前身份的代码 |

前三行是Spring管理的依赖对象，后面三行是一次登录过程产生或处理的对象；不要把“Bean已创建”理解成“用户已经登录”。

`SecurityConfig`负责创建和配置对象；`SessionLoginService`的构造器接收Spring已有的Bean，不自行`new`出认证管理器、会话策略或身份仓库。`AuthController`只调用登录服务并返回结果。这里的`SecurityContextRepository`与15-04的`CsrfTokenRepository`都叫Repository，但前者保存**身份**，后者保存**CSRF令牌**，用途不同。

新建`src/main/java/com/example/employee/security/SessionLoginService.java`，完整文件如下：

```java
package com.example.employee.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;

// 功能：把一次成功的密码认证结果保存为可跨请求恢复的Session身份。
@Service
public class SessionLoginService {
    private final AuthenticationManager authenticationManager;
    private final SessionAuthenticationStrategy sessionStrategy;
    private final SecurityContextRepository contextRepository;

    // 功能：注入认证入口、登录成功后的会话策略和身份保存仓库。
    public SessionLoginService(AuthenticationManager authenticationManager,
            SessionAuthenticationStrategy sessionStrategy,
            SecurityContextRepository contextRepository) {
        this.authenticationManager = authenticationManager;
        this.sessionStrategy = sessionStrategy;
        this.contextRepository = contextRepository;
    }

    // 功能：完成验证密码、处理Session、保存身份，并返回当前用户名。
    public String login(String username, String password,
            HttpServletRequest request, HttpServletResponse response) {
        // 功能：仅封装本次输入，此时尚未证明密码正确。
        Authentication input = UsernamePasswordAuthenticationToken.unauthenticated(
                username, password);
        // 功能：交给认证管理器验证，失败时抛认证异常。
        Authentication result = authenticationManager.authenticate(input);
        // 功能：认证成功后应用Session ID与CSRF令牌更新策略。
        sessionStrategy.onAuthentication(result, request, response);
        // 功能：建立本次请求使用的身份上下文。
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(result);
        SecurityContextHolder.setContext(context);
        // 功能：显式保存上下文，使下一条携带会话Cookie的请求能恢复身份。
        contextRepository.saveContext(context, request, response);
        // 功能：只把认证后的用户名交给Controller作为响应数据。
        return result.getName();
    }
}
```

**功能说明：完整服务如何落实前面三个动作**

- `@Service`让Spring创建服务；构造器把上图三个Bean交给它。这里不另行创建登录入口。
- `authenticate(input)`负责验证；只有成功返回`result`后，才依次执行`onAuthentication(...)`、`SecurityContextHolder.setContext(...)`和`saveContext(...)`。异常会中断后续保存动作。
- `return result.getName()`只把认证后的用户名交给Controller；不返回密码或整个`Authentication`对象。

**理解要点：** Holder只供当前请求使用，不是跨请求存储。少了`saveContext`可能出现“登录200、下一请求`/auth/me`401”；后续请求须带回`JSESSIONID`，过滤链才会从同一仓库恢复身份。

把15-04的`AuthController.java`**完整替换**为下方文件；注册和CSRF方法保持原行为，只新增登录与身份查询：

```java
package com.example.employee.controller;

import com.example.employee.common.ApiResponse;
import com.example.employee.dto.request.LoginRequest;
import com.example.employee.dto.request.RegisterRequest;
import com.example.employee.dto.response.CsrfResponse;
import com.example.employee.dto.response.CurrentUserResponse;
import com.example.employee.dto.response.RegisteredUserResponse;
import com.example.employee.security.AccountRegistrationService;
import com.example.employee.security.SessionLoginService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// 功能：提供注册、CSRF令牌、登录和当前身份的JSON接口。
@RestController
@RequestMapping("/auth")
public class AuthController {
    private final AccountRegistrationService registrationService;
    private final SessionLoginService loginService;

    // 功能：注入注册与登录服务，Controller不直接访问账号表。
    public AuthController(AccountRegistrationService registrationService,
            SessionLoginService loginService) {
        this.registrationService = registrationService;
        this.loginService = loginService;
    }

    // 功能：把当前请求的CSRF令牌资料交给客户端。
    @GetMapping("/csrf")
    public ResponseEntity<ApiResponse<CsrfResponse>> csrf(CsrfToken token) {
        return ResponseEntity.ok(ApiResponse.success(new CsrfResponse(
                token.getParameterName(), token.getHeaderName(), token.getToken())));
    }

    // 功能：校验注册输入，返回201和新账号摘要。
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<RegisteredUserResponse>> register(
            @Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(registrationService.register(request)));
    }

    // 功能：接收登录JSON，调用服务保存Session，返回用户名。
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<CurrentUserResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        String username = loginService.login(request.getUsername(),
                request.getPassword(), httpRequest, httpResponse);
        return ResponseEntity.ok(ApiResponse.success(
                new CurrentUserResponse(username)));
    }

    // 功能：从当前请求已恢复的身份中读取用户名。
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<CurrentUserResponse>> me(
            Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(
                new CurrentUserResponse(authentication.getName())));
    }
}
```

**功能说明：** `@RestController`返回JSON，`@RequestMapping("/auth")`提供公共路径。`@PostMapping("/login")`限定POST登录；`@RequestBody`把JSON映射到`LoginRequest`，`@Valid`触发校验。`HttpServletRequest`和`HttpServletResponse`是本次HTTP请求、响应，传给登录服务处理Session；`ResponseEntity.ok(...)`返回200。`@GetMapping("/me")`限定GET；`Authentication`参数来自本次请求由过滤链恢复的身份，`getName()`取得用户名。注册与CSRF方法保持15-04的行为。

**理解要点：** Controller只调用登录服务并包装用户名，不在这里验证哈希或直接操作Session仓库。匿名`GET /auth/me`在进入方法前得到401；认证失败也不应分别暴露未知用户名、错误密码和停用账号的内部原因。

把15-04的`AuthExceptionHandler.java`**完整替换**为下方最终文件，保留注册冲突并新增认证失败处理：

```java
package com.example.employee.exception;

import com.example.employee.common.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

// 功能：把进入Controller后的注册冲突与认证异常转换为统一JSON错误。
@RestControllerAdvice
public class AuthExceptionHandler {
    // 功能：保留注册重名时的409处理。
    @ExceptionHandler(DuplicateUsernameException.class)
    public ResponseEntity<ApiResponse<Void>> duplicateUsername(
            DuplicateUsernameException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.failure(exception.getMessage()));
    }

    // 功能：对账号不存在、密码错误和停用等认证失败统一返回401。
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> authenticationFailed(
            AuthenticationException exception) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.failure("用户名或密码错误"));
    }
}
```

**功能说明：** `@RestControllerAdvice`集中处理Controller异常；`@ExceptionHandler(AuthenticationException.class)`匹配认证异常，`AuthenticationException`参数承载内部错误，但方法固定以`HttpStatus.UNAUTHORIZED`返回401及统一失败响应体。另一个方法继续处理`DuplicateUsernameException`并返回409。

**理解要点：** 过滤链中的匿名401、CSRF403不经过这个Advice。账号不存在、密码错误或停用都向调用方返回相同的401消息，避免暴露账号存在性。

在Postman保留同一Cookie Jar，先`GET /auth/csrf`取当前Header名和令牌，再`POST /auth/login`提交上面JSON。预期200且`data.username=learner15`；接受登录响应中的Cookie更新后，再发一条独立`GET /auth/me`，预期200且用户名仍是`learner15`。清掉`JSESSIONID`后重发`GET /auth/me`应401。登录后再`GET /auth/csrf`取得**新令牌**，不要沿用登录前的值。登录200而下一请求401时，依次查`authenticate`、`onAuthentication`、`saveContext`及Postman Cookie；不要关闭Security来让请求通过。

## 15-07 CSRF与退出：区分令牌和登录会话

浏览器在符合Cookie规则时可能自动带上`JSESSIONID`；恶意页面可能诱导用户浏览器发送写请求。CSRF令牌要求客户端除了自动发送的Cookie，还主动提交当前令牌。本章从注册开始保留CSRF，因此`POST /auth/register`、`POST /auth/login`、员工写请求和退出都需要有效令牌。Postman从`GET /auth/csrf`响应体读取`headerName`和`token`，再把值放进对应请求头。只带Cookie不够；`JSESSIONID`也不是CSRF令牌。浏览器的SameSite等因素会影响攻击是否可行，不能简单声称所有跨站请求都会成功。

登录成功时15-06的组合策略更换已有Session ID并清除登录前令牌。客户端必须接受服务器的Cookie更新，再次`GET /auth/csrf`取当前令牌。此处`CookieCsrfTokenRepository`保存预期令牌，默认请求处理器可能向响应体暴露带掩码的提交值；不要假设`XSRF-TOKEN` Cookie文本必然等于响应体的`token`。深入处理流程见[A15-2](../appendix/A15_02_session_cookie_csrf.md)。

界面显示“退出”不等于服务器Session失效。本项目用Spring Security的Logout过滤器处理`POST /auth/logout`。只修改15-06的`SecurityConfig.java`：在`.httpBasic(AbstractHttpConfigurer::disable)`后、`return http.build()`前，把原分号改成下面的链式调用与分号：

```java
// 功能：保留关闭HTTP Basic的设置，继续添加退出路径配置。
.httpBasic(AbstractHttpConfigurer::disable)
// 功能：让Security过滤器处理POST /auth/logout，并清除客户端会话Cookie。
.logout(logout -> logout.logoutUrl("/auth/logout")
    .deleteCookies("JSESSIONID")
    // 功能：退出成功时只返回204，不返回响应体。
    .logoutSuccessHandler((request, response, authentication) ->
        response.setStatus(204)));
```

**功能说明：** 这是`SecurityConfig.securityFilterChain(...)`中原调用链的**替换片段**，不是独立Java语句。`logoutUrl("/auth/logout")`设置退出路径；`deleteCookies("JSESSIONID")`让退出响应删除会话Cookie；`logoutSuccessHandler(...)`接收成功处理回调，Lambda的`request`、`response`、`authentication`依次是本次请求、响应和退出前身份。`response.setStatus(204)`使成功响应没有正文。

**理解要点：** CSRF仍启用，所以客户端须用POST并带当前令牌。删除客户端Cookie和使服务器Session失效不是同一件事，不能用“在Postman里手动删Cookie”代替退出验证。

按下表验证；员工新增体为`{"name":"Csrf Practice","department":"Sales","email":"csrf-practice@example.com"}`，先确认邮箱未使用。各POST均无查询参数：

| HTTP方法与URL | 条件和请求体 | 预期状态、响应与数据库证据 |
| --- | --- | --- |
| `POST http://localhost:8080/auth/login` | 登录JSON，缺CSRF Header | 403，不建立登录身份 |
| `POST /auth/login` | 登录JSON，当前令牌和Cookie | 200，返回用户名 |
| `POST /employees` | 员工JSON、登录Cookie，缺令牌 | 403，库中无该邮箱 |
| `GET /auth/csrf` | 登录后同一Cookie，无请求体 | 200，取得登录后令牌 |
| `POST /employees` | 员工JSON、登录Cookie、新令牌 | 201，库中新增该邮箱 |
| `POST /auth/logout` | 登录Cookie、新令牌、无请求体 | 204、无正文；重送旧会话标识访问`GET /auth/me`仍为401 |

再用登录前旧令牌配合当前Header名单独发送写请求，应为403。员工新增实验完成后，仅在个人练习库核对实际ID再清理本次数据；不要运行测试库的`DROP`。已登录仍403时，先核对CSRF令牌和Cookie，再判断是否为第16章角色授权拒绝。

## 15-08 从注册到退出的测试与交接

自动化测试与Postman承担不同职责。真实登录测试必须使用MySQL账号、BCrypt、JSON请求与另一次HTTP请求；`@WithMockUser`或`user(...)`只能模拟已登录身份，不能证明密码正确。测试数据库必须由第12章`MySqlTestGuard`限制为专用MySQL库；[R15的`test-data.sql`](../reference/R15_session_login_complete_source.md)含`DROP`，绝不可导入个人库。

下面是一个**完整的最小集成测试类**。它沿用第12章已有的`MySqlTestGuard`与R15测试数据；真实注册账号`registered-in-test`与初始化账号不同，避免“注册并未写入，登录却碰巧使用已有账号”的假通过：

```java
package com.example.employee.security;

import com.example.employee.support.MySqlTestGuard;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
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

// 功能：启动真实Spring上下文与MockMvc，选用test配置，并在每次测试前准备专用测试库。
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Sql(scripts = "/test-data.sql",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class RegistrationLoginTest extends MySqlTestGuard {
    // 功能：注入模拟HTTP请求工具与直接核对数据库的工具。
    @Autowired MockMvc mockMvc;
    @Autowired JdbcTemplate jdbcTemplate;

    // 功能：证明新注册账号写入MySQL后，能够真实登录且跨请求恢复身份。
    @Test
    void registeredAccountCanLoginOnNextRequest() throws Exception {
        // 功能：提交带有效CSRF的注册JSON，检查201与返回用户名。
        mockMvc.perform(post("/auth/register").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"registered-in-test\","
                                + "\"password\":\"TrainingPass123!\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.username").value("registered-in-test"));
        // 功能：直接查库，证明保存的是哈希而非明文密码。
        String hash = jdbcTemplate.queryForObject(
                "SELECT password_hash FROM app_users WHERE username = ?",
                String.class, "registered-in-test");
        assertThat(hash).isNotEqualTo("TrainingPass123!");

        // 功能：对刚注册的账号发送真实登录请求，取得本次响应结果。
        MvcResult login = mockMvc.perform(post("/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"registered-in-test\","
                                + "\"password\":\"TrainingPass123!\"}"))
                .andExpect(status().isOk())
                .andReturn();
        // 功能：从登录请求取得已存在的Session，不凭空创建一个。
        MockHttpSession session =
                (MockHttpSession) login.getRequest().getSession(false);
        assertThat(session).isNotNull();
        // 功能：在另一条请求携带该Session，检查身份能否恢复。
        mockMvc.perform(get("/auth/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("registered-in-test"));
    }

    // 功能：重复注册应返回409，数据库仍只有一条账号。
    @Test
    void duplicateRegistrationDoesNotCreateSecondAccount() throws Exception {
        String body = "{\"username\":\"duplicate-in-test\","
                + "\"password\":\"TrainingPass123!\"}";
        // 功能：先成功注册，再用相同请求体触发重名错误。
        mockMvc.perform(post("/auth/register").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/auth/register").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());
        // 功能：用COUNT核对没有产生第二行，不能只看HTTP状态。
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM app_users WHERE username = ?",
                Integer.class, "duplicate-in-test");
        assertThat(count).isEqualTo(1);
    }
}
```

**功能说明：本例的注解、对象、方法和断言**

- `@SpringBootTest`启动应用上下文，`@AutoConfigureMockMvc`配置MockMvc，`@ActiveProfiles("test")`选择测试配置；`@Sql(..., BEFORE_TEST_METHOD)`在每个`@Test`方法前执行初始化脚本。`MySqlTestGuard`限制只能连接专用测试库。
- `@Autowired`注入`MockMvc`与`JdbcTemplate`。`MockMvc.perform(...)`发送模拟HTTP请求；静态导入的`post(...)`、`get(...)`创建请求，`.with(csrf())`仅给该请求附上有效令牌，`.contentType(MediaType.APPLICATION_JSON)`声明JSON请求体，`.content(...)`设置内容。
- `.andExpect(status().isCreated()/isOk()/isConflict())`检查HTTP状态；`jsonPath("$.data.username").value(...)`检查响应字段。`.andReturn()`返回`MvcResult`，用于取得本次请求的`MockHttpSession`测试会话；`getSession(false)`在没有Session时返回`null`，不会创建假的会话。另一条请求的`.session(session)`显式携带它。
- `JdbcTemplate.queryForObject(sql, type, value)`在此执行带`?`参数的查询；`assertThat(hash).isNotEqualTo(...)`检查没有存明文，`assertThat(session).isNotNull()`检查会话存在，`assertThat(count).isEqualTo(1)`检查重复注册只留下单行。

**理解要点：** 第一个测试的账号与初始化账号不同，分别证明数据库有哈希、密码可用于真实登录、另一条请求能恢复身份；第二个测试同时检查409和数据库行数。`.with(csrf())`不能代替Postman的真实Cookie/令牌生命周期检查。更全面的停用账号、旧令牌和退出测试见[R15](../reference/R15_session_login_complete_source.md)，但完成本例不依赖先阅读R15。

人工验收按“取令牌→注册→数据库查哈希与固定USER角色→JSON登录→另一请求`/auth/me`→重新取令牌→退出→重送旧Cookie”的顺序进行。另验证错误密码、不存在账号、停用账号统一401；缺令牌POST为403；注册重复409；校验失败400；原员工CRUD、事务和分页测试仍通过。Eclipse单独运行`RegistrationLoginTest`与R15的`SessionLoginTest`，再运行受影响的旧业务测试；旧Controller切片测试可模拟身份，写请求加`.with(csrf())`，但保留原业务断言。日志与证据只记录状态、脱敏响应、必要数据库字段，不记录密码、哈希、完整Cookie或令牌。

本章结束时只有一个JSON登录入口；注册账号来自MySQL，角色由服务器固定为USER，关联员工ID为空。它能登录并访问`/auth/me`，但还没有“本人员工资料”资格。第16章再讲角色与员工关联如何影响授权；公开注册接口不能用于修改角色或停用账号。
