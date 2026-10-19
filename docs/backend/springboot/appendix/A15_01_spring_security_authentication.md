# A15-1 Spring Security认证机制与核心Bean

本附录以[第15章](../mainline/15_session_login.md)的员工账号登录为对象。主线只需会配置、调用和验证；这里解释过滤器、认证管理器与账号加载组件为何能协作。最终实现见[R15源码参考](../reference/R15_session_login_complete_source.md)，不需要另建一套登录系统。

## 一、先分清请求处理与账号认证

Servlet容器先把HTTP请求交给Filter，再进入Spring MVC的Controller。Spring Security通过过滤器链完成CSRF、从Session恢复身份和URL访问检查；它可以在Controller执行前直接拒绝请求。`SecurityFilterChain` 是Spring管理的过滤器链配置结果，`HttpSecurity` 是在`@Bean`方法中构造该链的配置对象。

```text
HTTP请求 → Servlet Filter → SecurityFilterChain
                                ├─ CSRF检查
                                ├─ 从Session恢复SecurityContext
                                └─ URL访问检查
                         → DispatcherServlet → AuthController
```

`@Configuration` 标记配置类；`@Bean` 标记的方法由Spring调用，返回值注册为容器对象。`SecurityConfig` 中的`PasswordEncoder`、`AuthenticationManager`、`SecurityContextRepository`等不是Controller自行`new`出来的。构造器或Bean方法参数按类型注入；缺少或出现多个不明确候选时，应用通常在启动阶段报错。

## 二、认证组件关系

```text
AuthController.login()
  → AuthenticationManager（项目中为ProviderManager）
      → DaoAuthenticationProvider
          ├─ AppUserDetailsService.loadUserByUsername()
          │    → AppUserMapper.findByUsername() → app_users
          └─ PasswordEncoder.matches(原始输入, 数据库哈希)
  ← 已认证Authentication，或AuthenticationException
```

| 对象或方法 | 类型和创建者 | 输入与结果 | 本项目用途 |
| --- | --- | --- | --- |
| `AuthenticationManager.authenticate(...)` | Security接口；`SecurityConfig`提供Bean | 未认证`Authentication` → 已认证结果或异常 | 登录入口只调用统一认证入口 |
| `ProviderManager` | Security实现；Bean方法创建 | 调用合适的Provider | 当前只注册一个`DaoAuthenticationProvider` |
| `DaoAuthenticationProvider` | Security实现；Bean方法创建 | 用户名、密码、加载器、编码器 | 查账号并验证密码 |
| `UserDetailsService.loadUserByUsername(...)` | Security接口；项目`@Service`实现 | 用户名 → `UserDetails`；不存在抛异常 | 从MySQL账号表取认证资料 |
| `PasswordEncoder.matches(...)` | Security接口；`BCryptPasswordEncoder`实现 | 原密码与存储哈希 → 布尔值 | 不重新`encode`后比较字符串 |
| `Authentication` | Security接口；由认证过程返回 | 用户身份及权限 | 保存进`SecurityContext`，供后续授权 |

数据库`AppUser`表示表中的一行，`UserDetails`表示认证组件需要的账号资料。`AppUserDetailsService`负责转换二者，不自行校验密码。表中角色`USER`经过`User.withUsername(...).roles("USER")`成为权限`ROLE_USER`；不要把`ROLE_`前缀重复存入本课程的`app_users.role`。

从登录请求到后续授权，几个名称相近的对象依次转换，不能把数据库行直接当作“已登录身份”：

| 对象 | 创建者或来源 | 主要作用 |
| --- | --- | --- |
| `LoginRequest` | `AuthController`从JSON请求体绑定 | 只接收用户名和本次提交的原始密码，不持久化 |
| `AppUser` | `AppUserMapper`从`app_users`读取 | 保存账号哈希、角色、启用状态及员工关联 |
| `UserDetails` | `AppUserDetailsService`把`AppUser`转换后返回 | 向Security提供账号状态、密码哈希和权限 |
| 未认证`Authentication` | 登录入口调用`UsernamePasswordAuthenticationToken.unauthenticated(...)` | 把待验证的用户名和原始密码交给认证入口，尚不能用于授权 |
| 已认证`Authentication` | `DaoAuthenticationProvider`经`ProviderManager`返回 | 表示验证成功的主体与权限；失败则抛认证异常 |
| `SecurityContext` | `SecurityContextHolder.createEmptyContext()`创建，并由Repository跨请求保存／加载 | 容纳当前认证结果，让后续请求恢复身份 |

`AuthenticationManager`是统一入口接口；当前`SecurityConfig`用`ProviderManager`实现它，并只注册一个`DaoAuthenticationProvider`。`ProviderManager`可以按认证类型委托多个`AuthenticationProvider`，但本项目没有配置第二种认证方式。`DaoAuthenticationProvider`专门处理本课程的用户名／密码：调用`UserDetailsService`取账号，用`PasswordEncoder.matches(...)`比较原始输入与数据库哈希，再检查账号状态。Manager负责调度，Provider负责具体认证，Mapper只读数据库；职责不能混为一层。

## 三、从账号数据到认证结果

第15章的最小实现只需要一个账号加载方法和一个认证Bean：

```java
// AppUserDetailsService.loadUserByUsername()中的关键片段
AppUser appUser = appUserMapper.findByUsername(username);
if (appUser == null) {
    throw new UsernameNotFoundException("login failed");
}
return User.withUsername(appUser.getUsername())
        .password(appUser.getPasswordHash())
        .roles(appUser.getRole())
        .disabled(!appUser.isEnabled())
        .build();
```

这里每个调用都在**构造认证资料**，不是验证本次输入：`findByUsername(username)`把登录名作为MyBatis绑定参数，查到返回数据库对象、查不到返回`null`；`UsernameNotFoundException`告诉Provider账号加载失败，Controller不应把内部查找结果原样回显。`User.withUsername(...)`返回构建器，后续`password(...)`接收数据库的**编码后哈希**，`roles(...)`接收不带`ROLE_`前缀的角色名并生成权限，`disabled(...)`接收“是否停用”的布尔值，因此传入`!appUser.isEnabled()`；`build()`才返回实现`UserDetails`的对象。加载方法的返回值不是“已经认证成功”，密码核对发生在下面的Provider中。

```java
// SecurityConfig中的Bean方法；项目还需第15章已创建的类与导入
@Bean
public AuthenticationManager authenticationManager(
        AppUserDetailsService users, PasswordEncoder encoder) {
    DaoAuthenticationProvider provider = new DaoAuthenticationProvider(users);
    provider.setPasswordEncoder(encoder);
    return new ProviderManager(provider);
}
```

`@Bean`使方法返回的`ProviderManager`注册成`AuthenticationManager` Bean；`users`和`encoder`是容器按类型注入的现有Bean，不是登录时才临时创建。`DaoAuthenticationProvider(users)`指定到哪里加载账号；`setPasswordEncoder(encoder)`指定如何比较原密码与数据库哈希；`ProviderManager(provider)`把它列为可处理用户名密码Token的Provider。调用`authenticate(input)`时，Manager委托Provider，Provider调用`loadUserByUsername`、检查`UserDetails`状态并用`matches(raw, encoded)`校验密码；成功返回已认证`Authentication`，失败抛`AuthenticationException`。`matches`的两个参数不能交换，也不能每次先对原密码`encode`后用字符串相等比较。

两个代码块是**对应文件中的方法片段**，完整导入和文件内容见R15。`enabled=false`应造成认证失败。用户名不存在、密码错误和停用账号对外统一返回401，不能泄露哪个账号存在。不要把原密码、哈希或认证异常堆栈写进对外响应。

BCrypt哈希包含随机salt，`encode("同一密码")`两次结果不同属正常现象；验证用`matches(raw, encoded)`。哈希不是加密后可解开的密码。本课程练习强度为10，真实项目应结合性能与安全要求评估。

## 四、自定义JSON登录与框架内建入口

`formLogin`对应框架提供的表单登录流程，`httpBasic`对应每次请求携带Basic凭据；本项目是JSON登录，所以关闭二者，由`AuthController`接收`LoginRequest`并调用同一个`AuthenticationManager`。这不表示绕过Spring Security：请求仍先经过安全过滤器与CSRF检查，认证仍由Provider完成。

```text
POST /auth/login（JSON＋有效CSRF）
  → Filter检查
  → AuthController建立未认证UsernamePasswordAuthenticationToken
  → AuthenticationManager.authenticate()
  ├─ 成功：返回Authentication，再保存到Session（A15-2）
  └─ 失败：抛AuthenticationException，由AuthExceptionHandler统一返回401
```

`UsernamePasswordAuthenticationToken.unauthenticated(...)`只是把输入封装起来，不能自己证明密码正确。`AuthExceptionHandler`只能处理进入Controller后的认证异常；过滤器链的未登录或CSRF拒绝不经过该Advice。

把一次`POST /auth/login`按实际代码追踪：请求先经过`SecurityFilterChain`的CSRF检查，再由`AuthController`把`LoginRequest`转换成未认证Token；`AuthenticationManager`（实际为`ProviderManager`）把它交给`DaoAuthenticationProvider`；Provider调用`AppUserDetailsService`，后者通过MyBatis的`AppUserMapper`查`app_users`并构建`UserDetails`；随后`PasswordEncoder`比较密码。正确密码且账号启用时返回已认证`Authentication`，Controller再调用Session策略并保存`SecurityContext`。错误密码在Provider校验环节失败；不存在账号在`loadUserByUsername`环节失败；停用账号从Mapper查得到，但账号状态检查拒绝认证。三者对外都由第15章约定为同样的401信息，不能暴露账号是否存在。`AppUser`、`UserDetails`、`Authentication`和`SecurityContext`在这条链中不是同一个对象。

## 五、如何调查认证失败

| 现象 | 首先检查 | 不应采取的捷径 |
| --- | --- | --- |
| 启动时报缺少`AuthenticationManager`或`PasswordEncoder` | `@Bean`、导入、包扫描与构造器参数 | 在Controller里临时`new`一个假对象 |
| 用户名能查到但密码错误 | 哈希是否由BCrypt生成、`matches`是否用对 | 保存明文或把两次`encode`结果直接比较 |
| 数据库有用户仍返回401 | `enabled`、`role`、Mapper映射、密码哈希 | 把“账号不存在”详细返回客户端 |
| 登录返回200但下一请求401 | 本次认证结果是否保存到`SecurityContextRepository` | 只在本次请求设置Java变量 |
| 预期401却得到403 | 先检查写请求CSRF令牌 | 关闭CSRF来让测试通过 |

用Eclipse在`loadUserByUsername`、`authenticationManager.authenticate`后设置断点，依次记录请求是否通过过滤器、Mapper返回什么、认证是否抛异常；不要在调试截图中暴露密码或完整令牌。

## 六、最小检查任务

在专用MySQL测试库准备一个启用账号和一个停用账号。运行第15章的Mapper、`PasswordEncoder`测试，再分别提交正确密码、错误密码、不存在用户名和停用账号；保存HTTP状态与脱敏日志。若密码验证通过但`/auth/me`仍401，继续查[A15-2](A15_02_session_cookie_csrf.md)的Session保存链，而不是修改账号表。

参考：[Spring Security DaoAuthenticationProvider](https://docs.spring.io/spring-security/reference/servlet/authentication/passwords/dao-authentication-provider.html)、[认证持久化](https://docs.spring.io/spring-security/reference/servlet/authentication/session-management.html)。
