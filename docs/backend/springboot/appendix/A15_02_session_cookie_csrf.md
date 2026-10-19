# A15-2 Session、Cookie、SecurityContext与CSRF

本附录解释[第15章](../mainline/15_session_login.md)的真实Session登录。请先分开三个对象：服务器的`HttpSession`保存认证状态；客户端的`JSESSIONID` Cookie只保存查找该Session的标识；`XSRF-TOKEN`及请求头里的CSRF令牌用于防止跨站伪造写请求，**不是登录身份**。完整代码见[R15](../reference/R15_session_login_complete_source.md)。

## 一、HTTP请求为何要恢复身份

HTTP无状态是指单次请求本身不会继承上一次请求的Java局部变量，并不是说服务器绝对不能保存状态。登录成功后服务器保存`SecurityContext`，响应给客户端`Set-Cookie: JSESSIONID=...`。后续请求携带同一Cookie，服务器才能找到相应Session，并由安全过滤器恢复认证。

```text
登录请求 → 认证成功 → SecurityContext(Authentication)
                           ↓ saveContext()
                         HttpSession（服务器）
                           ↑ JSESSIONID定位
下一次HTTP请求 ← Cookie: JSESSIONID=随机标识（客户端）
```

`Authentication`表示认证后的身份，`SecurityContext`装载它；`SecurityContextHolder`让当前请求的处理代码取得上下文。`SecurityContextRepository`负责跨请求保存与读取；本项目Bean为`HttpSessionSecurityContextRepository`。自定义`AuthController.login()`若只调用`authenticate()`，即使本次返回200，也不会自然使下一次请求有身份；必须显式`saveContext(context, request, response)`。

## 二、项目登录调用顺序

```text
AuthController.login()
  1. AuthenticationManager.authenticate(input)
  2. SessionAuthenticationStrategy.onAuthentication(result, request, response)
  3. SecurityContextHolder.createEmptyContext()
  4. context.setAuthentication(result)
  5. SecurityContextHolder.setContext(context)
  6. SecurityContextRepository.saveContext(context, request, response)
  7. 返回用户名，不返回密码、哈希、Session ID
```

本项目的`SessionAuthenticationStrategy`组合了`ChangeSessionIdAuthenticationStrategy`与`CsrfAuthenticationStrategy`。前者在认证成功后更换会话ID，降低“攻击者预先固定受害者会话ID”的风险；后者清除登录前CSRF令牌，要求客户端随后重新取得令牌。自定义Controller主动调用组合策略；不能假设过滤器会替Controller自动调用。

逐项核对第15章登录方法依赖的类型与调用结果：

- `AuthenticationManager.authenticate(input)`：`input`是含用户名和原密码的未认证Token；成功返回已认证`Authentication`，错误密码、未知账号或停用账号抛认证异常。它本身不创建浏览器登录态。
- `SessionAuthenticationStrategy.onAuthentication(result, request, response)`：三个参数分别是认证结果、当前Servlet请求和响应；返回`void`，组合策略按配置顺序更换现有Session ID并清除旧CSRF令牌。前者降低会话固定风险，后者要求下一次写请求先取新令牌。
- `SecurityContextHolder.createEmptyContext()`返回新的空上下文；`setAuthentication(result)`把身份放进去；`SecurityContextHolder.setContext(context)`使当前请求线程可以读取。Holder不是数据库或跨请求存储。
- `SecurityContextRepository.saveContext(context, request, response)`返回`void`，把上下文保存到本项目的HttpSession仓库，并让响应带回必要的会话标识。`requireExplicitSave(true)`要求自定义登录代码确实调用这一句；只设置Holder而不保存，下一请求仍可能401。
- 后续请求由过滤链通过同一`HttpSessionSecurityContextRepository`加载上下文。`JSESSIONID`只负责定位服务器Session；它不直接存储完整`Authentication`，也不是CSRF令牌。

更换Session ID与清除CSRF令牌是不同动作。先前尚无Session时，容器会按需要创建；已有Session时，策略更新ID。下一次请求能否恢复身份，要用另一条请求携带Cookie访问`GET /auth/me`证明，而不能只看登录接口的200。

## 三、Cookie属性及浏览器、Postman的区别

| 属性 | 常见取值或状态 | 对本项目的意义 |
| --- | --- | --- |
| `HttpOnly` | 有／无 | 有时浏览器脚本不能直接读取Cookie；不要为调试JSESSIONID而关闭 |
| `Secure` | 有／无 | 有时仅经HTTPS发送；生产HTTPS环境应检查 |
| `SameSite` | `Lax`、`Strict`、`None` | 影响跨站发送Cookie；不能单独替代CSRF令牌 |
| `Path`、`Domain` | 路径、域名 | 决定哪些请求会附带Cookie；Cookie本身不以端口隔离，同一主机不同端口联调也要核对实际请求 |
| `Max-Age`／过期时间 | 秒数或会话Cookie | 客户端保存时间不等于服务器Session一定有效 |

Postman的Cookie Jar会按域名保存和发送Cookie，适合观察请求与响应；真实浏览器还受同源、SameSite、Secure及跨源凭据策略影响。Postman能调用成功，不能单独证明浏览器跨源联调正确；扩展实验见[A16 跨源Session与CORS](A16_cross_origin_session_cors.md)。

上表列的是需要检查的Cookie属性，**不是说本章的Java代码逐项设置了这些值**。`JSESSIONID`由Servlet容器/Session配置发出，`XSRF-TOKEN`由`CookieCsrfTokenRepository`发出；应在浏览器开发工具或Postman响应的`Set-Cookie`里核对实际属性。`HttpOnly`只限制浏览器脚本读取，不阻止浏览器随符合规则的请求发送Cookie；`Secure`和`SameSite`也不能替代服务端的认证或CSRF校验。

Session通常保存在应用进程内；应用重启或Session超时后，旧Cookie可能仍在客户端，但服务器找不到有效认证，受保护接口应返回401。多实例部署若要共享Session，必须另设计集中存储或会话路由；复制JSESSIONID字符串不能解决。

## 四、CSRF保护针对什么

浏览器在满足Cookie规则时可能自动附带目标站点的JSESSIONID；恶意页面可诱导用户浏览器发起写请求。CSRF令牌要求请求方除了自动Cookie，还提供从本应用获得的令牌值。本项目保留Spring Security默认启用的CSRF检查，并选择`CookieCsrfTokenRepository`保存令牌。

```text
GET /auth/csrf → 响应体返回headerName与token，设置XSRF-TOKEN Cookie
POST /auth/login → 带同一Cookie及指定请求头中的token
登录成功 → Session ID更新，旧CSRF Cookie被清除
再次GET /auth/csrf → 取得当前令牌
POST/PUT/DELETE → 同一会话Cookie＋当前令牌请求头
```

`JSESSIONID`用于找到认证Session；`XSRF-TOKEN`用于保存CSRF令牌；请求头如`X-XSRF-TOKEN`是本次写请求主动提交的令牌值。仅自动带上XSRF-TOKEN Cookie不等于提交了正确请求头。项目的`new CookieCsrfTokenRepository()`保留Cookie的HttpOnly属性，Postman从`/auth/csrf`响应体取得令牌，不要求浏览器脚本读取Cookie。若将来由前端JavaScript直接读Cookie，需要另行评审`withHttpOnlyFalse()`与SPA流程，不能因此关闭JSESSIONID的HttpOnly。

`CsrfTokenRepository`接口负责生成、保存和读取令牌，`CookieCsrfTokenRepository`是Cookie实现；`CsrfAuthenticationStrategy`在认证成功时通过相同仓库清除旧令牌。退出由Logout Filter清理认证和Session，客户端仍要遵守当前CSRF检查。没有、错误或过期令牌通常在Controller前得到403；这与第16章“角色不足的403”来源不同。

这些类名相似，但每个方法服务的时机不同：`CookieCsrfTokenRepository`作为Bean交给`.csrf(csrf -> csrf.csrfTokenRepository(repository))`，供`CsrfFilter`在需要保护的请求上加载预期令牌；同一个仓库也交给`new CsrfAuthenticationStrategy(repository)`，在登录成功时清除旧令牌。`new CompositeSessionAuthenticationStrategy(List.of(...))`按列表顺序调用策略，`List.of`的元素是策略对象而非字符串属性。`AuthController.csrf(CsrfToken token)`由当前请求注入令牌，`getHeaderName()`给客户端请求头名称，`getToken()`给应提交的值。Filter负责验证，Controller只负责把令牌交给客户端；仅有仓库Bean或仅有令牌Cookie都不表示已经通过验证。

### Spring Security的CSRF令牌处理流程

本项目把预期令牌保存在`XSRF-TOKEN` Cookie中，同时由`GET /auth/csrf`把可提交的令牌放进响应体。两者的职责不同：Cookie会随符合规则的浏览器请求自动发送，单凭它与`JSESSIONID`不能证明请求是本应用页面主动发出的；客户端还必须把从响应体取得的令牌放入`X-XSRF-TOKEN`请求头。这里使用的是第15章现有的`new CookieCsrfTokenRepository()`和`AuthController.csrf(CsrfToken token)`，不新增自定义处理器。

| 组件（`org.springframework.security.web.csrf`） | 在本项目中的职责 |
| --- | --- |
| `CsrfTokenRepository` | 生成、保存、加载**预期的原始令牌**；只是接口，不规定必须存在哪里 |
| `CookieCsrfTokenRepository` | 将原始令牌保存在`XSRF-TOKEN` Cookie；当前构造方式保持HttpOnly，前端不直接读此Cookie |
| `CsrfTokenRequestHandler` | 把令牌暴露给请求处理代码，并从Header或请求参数解析客户端提交值 |
| `XorCsrfTokenRequestAttributeHandler` | 当前默认处理器；给暴露的令牌加入随机掩码，并在校验前解析还原，降低令牌在压缩响应中反复以相同文本出现的风险 |
| `CsrfFilter` | 对需要保护的请求取得预期令牌、解析提交值并比较；缺失或不匹配时通常在Controller前返回403 |
| `CsrfAuthenticationStrategy` | 本项目JSON认证成功时由`SessionAuthenticationStrategy`调用，清除登录前令牌 |

```text
客户端 GET /auth/csrf
    → CsrfFilter准备延迟加载的令牌
    → CsrfTokenRequestHandler把令牌暴露给AuthController
    → Controller读取CsrfToken.getToken()，触发生成/加载
    → CookieCsrfTokenRepository保存原始值到XSRF-TOKEN Cookie
    ← 响应体给客户端可提交的token和headerName

客户端 POST /auth/login（Cookie＋响应体取得的CSRF Header）
    → CsrfFilter经RequestHandler解析Header，与仓库中的原始值比较
    → AuthController调用AuthenticationManager认证
    → 成功时CsrfAuthenticationStrategy清除旧令牌
    ← 客户端接受登录响应中的Cookie更新

客户端重新 GET /auth/csrf → 用新响应体令牌发送后续写请求
```

默认的`XorCsrfTokenRequestAttributeHandler`暴露给Controller的值可能是**带随机掩码的值**；Cookie仓库中保存的是原始值，所以不要假定`XSRF-TOKEN` Cookie文本必然等于响应体的`token`。本项目客户端提交的是**响应体**中的值，默认处理器再从Header解析、还原并与仓库值比较；不要改成“读Cookie原值后直接写Header”的另一套SPA方案。令牌默认延迟加载：安全的GET若不使用令牌，不必每次生成或读取；`/auth/csrf`调用`getToken()`，因此明确取得并返回它。

登录成功时旧令牌被清除，后续写请求必须重新取得；退出成功时CSRF退出处理也会清理原令牌，若继续使用匿名写入口，应重新取得新令牌。清除旧令牌并不等于停用CSRF：仓库解决保存位置，过滤器仍负责验证。若出现403，依次核对客户端是否接受了Cookie更新、Header是否来自**当前**`/auth/csrf`响应、请求是否仍在同一Cookie上下文；不要只比较Cookie与Header的字符串是否相等。

## 五、退出与旧Session失效

`POST /auth/logout`由`SecurityFilterChain`中的Logout Filter处理。成功时返回204无正文，清除`SecurityContext`、使服务器Session失效并要求客户端删除JSESSIONID。验证要保留退出前的旧会话标识：先登录并确认`/auth/me`为200，再携带有效CSRF令牌退出，最后重送旧Cookie访问`/auth/me`应为401。只在Postman里手动删Cookie，不能证明服务器真的使旧Session失效。

第15章过滤链中的`.logout(...)`配置了三个不同动作：`logoutUrl("/auth/logout")`匹配退出路径，因本项目保留CSRF，客户端以带当前令牌的POST调用；`deleteCookies("JSESSIONID")`要求浏览器清除同名Cookie，但**服务器Session失效**仍由Logout处理，单靠删Cookie不足以证明它；`logoutSuccessHandler((request, response, authentication) -> response.setStatus(204))`在退出处理完成后设置状态，三个参数分别是本次请求、响应与退出前身份（可能为空），不返回业务JSON。`204 No Content`的响应体应为空，不能再要求`ApiResponse.success(...)`。旧Cookie重送仍应401，才是服务器不再接受旧会话的证据。

## 六、排障顺序

| 现象 | 首先检查 | 可能责任位置 |
| --- | --- | --- |
| 登录200、`/auth/me`401 | `saveContext`是否执行；请求是否带相同JSESSIONID | Controller保存、Cookie Jar、Session |
| 登录前令牌可用、登录后写请求403 | 是否接受清除旧Cookie的`Set-Cookie`并重新GET令牌 | CSRF令牌生命周期 |
| 有JSESSIONID仍401 | Session是否超时、重启或存储不共享 | 服务器Session |
| 写请求403 | 先核对CSRF，再核对登录身份与角色 | CSRF Filter或授权规则 |
| 浏览器失败、Postman成功 | Origin、Cookie凭据、SameSite/Secure与CORS | 浏览器与部署配置 |

在Eclipse调试时可观察`AuthController.login`中`authenticate`、策略调用、`saveContext`三个位置，并用Postman检查脱敏后的`Set-Cookie`、Cookie Jar和响应状态。不要记录完整JSESSIONID、CSRF令牌、明文密码或密码哈希。

参考：[Spring Security认证持久化与Session管理](https://docs.spring.io/spring-security/reference/servlet/authentication/session-management.html)、[CSRF说明](https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html)、[Logout说明](https://docs.spring.io/spring-security/reference/servlet/authentication/logout.html)。
