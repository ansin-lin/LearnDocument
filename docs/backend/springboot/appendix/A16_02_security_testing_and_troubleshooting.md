# A16-2 Spring Security测试、回归与故障调查

本附录结合[第15章真实登录测试](../mainline/15_session_login.md)和[第16章授权测试](../mainline/16_authorization_and_data_scope.md)。完整测试源码分别见[R15](../reference/R15_session_login_complete_source.md)和[R16](../reference/R16_authorization_complete_source.md)。本附录不新增另一套认证系统，也不通过关闭过滤器或CSRF“修复”失败测试。

## 一、先决定要证明哪一层

| 测试方式 | 准备身份的方法 | 能证明什么 | 不能证明什么 |
| --- | --- | --- | --- |
| `SessionLoginTest` | JSON登录、真实数据库账号与BCrypt | 账号验证、Session保存、跨请求恢复、退出 | 所有角色与本人规则 |
| `AuthorizationIntegrationTest` | `user(...)`或`@WithMockUser` | URL规则、方法授权、CSRF对照、数据库未改变 | 真实密码验证与登录Controller |
| `EmployeeControllerWebTest` | `@WithMockUser` | Controller绑定、响应与异常转换 | Mapper、真实数据库和完整登录 |
| `EmployeeCrudIntegrationTest` | 模拟ADMIN身份＋CSRF | CRUD、事务、数据库结果在安全改造后仍正确 | 用户密码是否正确 |
| `EmployeeSearchIntegrationTest` | 模拟ADMIN身份 | 筛选、分页和COUNT在授权后仍正确 | USER是否可查看列表 |

`@SpringBootTest`加载完整应用上下文，`@AutoConfigureMockMvc`创建MockMvc；`@WebMvcTest`是Web切片，只聚焦指定Controller及相关Web组件，不等同于完整项目。`@WithMockUser`在测试请求前提供模拟`SecurityContext`；`user("tanaka").roles("USER")`只给**当前请求**建立身份。二者不执行`/auth/login`，因此不能替代第15章的真实Session测试。

## 二、写请求同时控制身份和CSRF

```java
// AuthorizationIntegrationTest中的请求片段
mockMvc.perform(put("/employees/{id}", 1001)
        .with(user("tanaka").roles("USER"))
        .with(csrf())
        .contentType(MediaType.APPLICATION_JSON)
        .content(updateJson))
    .andExpect(status().isForbidden());
```

上面是**原测试方法内的片段**，所需静态导入为`SecurityMockMvcRequestPostProcessors.user`、`SecurityMockMvcRequestPostProcessors.csrf`、`MockMvcRequestBuilders.put`及`MockMvcResultMatchers.status`。先给有效CSRF，才能把403归因于USER角色不足；没有`with(csrf())`的403可能由CSRF过滤器在URL角色判断前产生。GET通常不需CSRF，但仍可能需要认证和权限。

`@WithMockUser(username = "integration-admin", roles = "ADMIN")`适合原有CRUD、分页测试只想保证业务逻辑可继续执行；类级注解会作用于该类测试，单个用例若要验证匿名或USER，应换独立测试类或明确覆盖。`roles("ADMIN")`会产生`ROLE_ADMIN`，不要写成`roles("ROLE_ADMIN")`。

## 三、按拒绝来源调查401、403与404

| 现象 | 首先核对 | 可能停止在哪层 | 证据 |
| --- | --- | --- | --- |
| 未登录GET详情401 | Cookie／测试身份是否存在 | 认证入口，Controller之前 | Controller断点未命中 |
| USER GET列表403 | 已登录且角色为USER | URL授权 | Mapper未执行 |
| USER GET他人详情403 | URL规则已通过，目标id与账号关联不同 | `@PreAuthorize`方法代理 | Service主体未执行 |
| 写请求缺CSRF得403 | 是否带当前令牌请求头及配套Cookie | CSRF Filter | 业务代码和数据库均未改变 |
| ADMIN带有效CSRF仍403 | URL规则、角色前缀、Session和令牌更新 | 需要进一步定位 | 用同一请求条件逐项对照 |
| 已授权查询不存在员工404 | 业务异常 | Controller／Service | 无越权数据泄露 |

`AuthenticationEntryPoint`处理需要登录的401，`AccessDeniedHandler`处理权限拒绝的403；`@RestControllerAdvice`负责进入Controller后的业务异常，不能代替过滤器链中的两个处理器。CSRF失败也可能表现为403，不能只凭状态码判定由`hasRole`触发。使用Eclipse Debug观察Filter之后Controller、Service代理和`EmployeeAccess`是否被执行，配合脱敏日志与数据库状态判断。

```text
HTTP请求失败
  ├─ Controller断点未命中
  │    ├─ 写请求：CSRF Cookie与Header是否对应当前令牌？
  │    ├─ JSESSIONID对应的Session是否有效、身份能否恢复？
  │    └─ SecurityConfig的URL角色规则是否允许？
  └─ Controller已命中
       ├─ Service方法主体未命中
       │    ├─ @PreAuthorize是否经过Spring代理执行？
       │    └─ EmployeeAccess是否拒绝目标id？
       └─ Service主体已命中
            ├─ 检查业务校验与异常映射
            ├─ 检查Mapper、SQL及影响行数
            └─ 检查数据库最终状态
```

401不只表示密码错误：未带Cookie、Session过期或退出后的受保护请求也可能得到401。403要依次区分CSRF、URL角色和方法级数据范围；相同状态码不能证明是同一机制。404也要先确认请求已通过授权，再判断是否真的不存在，不要让越权者仅凭404/403差异探测他人数据。断点和临时安全日志只记录请求阶段、角色及业务键等必要信息；不得打印原始密码、完整Session ID、完整CSRF令牌或密码哈希。

## 四、权限失败时检查数据没有变

对越权PUT，除了断言403，还要用`JdbcTemplate`在请求后查询员工姓名仍为旧值。对缺CSRF的DELETE，断言403及记录仍存在；对同一ADMIN请求加入有效令牌后，再断言204与记录消失。这样才排除了“接口本身坏了”或“测试只由CSRF拒绝”的误判。测试数据只在第12章定义的专用MySQL测试库初始化，`@Sql`每个方法前准备确定状态；不要在共享库执行破坏性脚本。

## 五、权限规格改变后的回归

从第15章到第16章，旧员工接口的业务语义未删，但调用前提改变了：列表及写接口要求ADMIN，写接口另需CSRF；USER详情还需本人范围。Review已有测试时记录“原先证明什么、现在多了什么前提、断言是否仍适用”。不能删除失败测试，也不能全局`addFilters = false`来让业务测试变绿。

建议在Eclipse依次运行：`AuthorizationIntegrationTest`、`SessionLoginTest`、`EmployeeControllerWebTest`、`EmployeeCrudIntegrationTest`、`EmployeeSearchIntegrationTest`，再执行第13章事务相关测试和完整回归。每一项记录测试类、方法、前置数据、预期/实际状态、数据库结果及修复后再测试；截图需遮盖Cookie、CSRF令牌和密码。

## 六、跨源请求是另一条调查线

浏览器页面与API的协议、主机或端口不同，可能先受CORS和预检限制；即使CORS允许，Session Cookie仍受`SameSite`、`Secure`及凭据发送规则影响，CSRF也继续生效。CORS决定跨源页面脚本是否能读取响应；需要预检的请求还须先通过预检才会发送实际请求。它不授予业务权限，不可拿`permitAll()`或关闭CSRF替代。需要实验时查[A16 跨源Session与CORS](A16_cross_origin_session_cors.md)，结束后恢复主线同源配置。

参考：[Spring Security MockMvc测试](https://docs.spring.io/spring-security/reference/servlet/test/mockmvc/index.html)、[CSRF测试](https://docs.spring.io/spring-security/reference/servlet/test/mockmvc/csrf.html)、[方法安全](https://docs.spring.io/spring-security/reference/servlet/authorization/method-security.html)。

## 七、Eclipse调试顺序与自测证据

收到“登录后更新接口403”的问题，不要先改权限代码。先在Postman保存**脱敏后的**HTTP方法、URL、请求体字段、响应状态和Cookie/Header是否存在，再用同一账号复现。随后在Eclipse依次观察：`AuthController.login`是否成功、`/auth/me`是否恢复身份、写请求是否有当前CSRF、`SecurityConfig`的URL角色是否匹配、`EmployeeAccess.canRead`是否被调用。若Controller断点未命中，优先查过滤链；若Service主体未命中但`EmployeeAccess`命中，优先查数据范围。只在本地临时提高`org.springframework.security`日志级别，避免生产环境长期记录过多安全细节。

| 证据项 | 应记录 | 不应记录 |
| --- | --- | --- |
| 请求 | 方法、路径、参数名、请求体字段、账号角色 | 明文密码、完整JSESSIONID、完整CSRF令牌 |
| 响应 | 状态码、统一错误信息、是否进入业务层 | 内部堆栈、账号是否存在的差异细节 |
| 测试 | 类名、方法名、预期/实际、运行时间与结果 | 仅写“已测试”而无具体场景 |
| 数据库 | 目标记录的事前/事后值或COUNT | 共享库中的无关个人资料 |

对每个403至少写清“已登录谁、CSRF是否有效、角色要求、数据范围要求、数据库是否改变”。这样Review者可以判断失败来自CSRF、URL授权还是方法授权，而不需要猜测。遇到浏览器与Postman结果不同，再单独增加Origin、预检、Cookie属性证据，转入跨源附录；不要把CORS当成角色权限修复。
