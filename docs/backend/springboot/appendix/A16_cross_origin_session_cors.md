# A16 跨源Session与CORS实验

> 前置：完成第15章的Session登录和第16章的角色、本人数据授权。本附录只处理“浏览器页面与API不在同一Origin”时的访问边界，不改变业务权限。

## 一、先区分四道检查

浏览器页面与API的协议、主机、端口都相同，才是同源。本实验假设页面位于 `http://localhost:3000`，API位于 `http://localhost:8080`；端口不同，所以浏览器会执行CORS检查。先确认 `localhost:3000` 上确实有可打开的本地前端页面；若你的前端使用其他端口，必须把下面的允许来源改成浏览器地址栏中的实际Origin。不要用 `file://` 页面代替这个前提。

| 机制 | 回答的问题 | 常见失败 |
| --- | --- | --- |
| CORS | 这个Origin的浏览器脚本能否读取跨源响应 | 浏览器拦截或OPTIONS预检失败 |
| 认证 | 当前请求是否有有效登录身份 | 401 |
| 授权 | 已登录身份能否操作该接口和记录 | 403 |
| CSRF | 使用Cookie的写请求是否提交了有效令牌 | 403 |

CORS通过不等于登录成功；Postman不受浏览器同源限制，因而Postman成功也不能证明CORS配置正确。前端隐藏按钮同样不能代替后端授权。

### Same-Origin与Same-Site不是一回事

Origin由**协议、主机、端口**共同决定；Site主要看站点域名及协议，端口不参与同站判断。下表假设浏览器以所写地址打开页面并访问API；`example.com`只是概念示例，不是本实验允许来源。

| 页面 → API | 同源（Same-Origin） | 同站（Same-Site） |
| --- | --- | --- |
| `http://localhost:3000` → `http://localhost:8080` | 否，端口不同 | 通常是，同为HTTP localhost |
| `https://app.example.com` → `https://api.example.com` | 否，主机不同 | 通常是，同协议且同一站点域 |
| `https://app.example.com` → `https://other.example.net` | 否 | 否，站点域不同 |

CORS处理跨**源**响应是否可供页面脚本使用；Cookie的`SameSite`约束跨**站**发送，两者不能互相代替。`credentials: "include"`只表示这次跨源fetch允许浏览器携带和接受凭据，并不强制浏览器发送Cookie：还要检查`SameSite`、`Secure`、`Path`、`Domain`、是否已过期及浏览器隐私策略。同站跨源也仍需CORS；本地两个localhost端口的实验不能直接推断不同站点的生产Cookie设置。

## 二、浏览器请求为什么可能先发OPTIONS

浏览器跨源发送JSON POST、自定义 `X-XSRF-TOKEN` 请求头或PUT、DELETE时，通常会先发送OPTIONS预检。预检询问服务器是否允许当前 `Origin`、方法和请求头；它通常不携带 `JSESSIONID`。预检通过后，浏览器才发送实际请求。预检是浏览器行为，不是后端登录接口，也不会授予ADMIN角色。

本实验的Session流程是：

```text
页面 localhost:3000 → GET /auth/csrf（取得响应体中的令牌）
                  → POST /auth/login（JSON、CSRF请求头、credentials: include）
                  ← Set-Cookie: JSESSIONID；登录前CSRF Cookie被清除
                  → GET /auth/csrf（取得登录后的新令牌）
                  → GET /auth/me 或 GET /employees/1001
                  → 写请求还要同时携带新CSRF请求头
```

`credentials: "include"` 要用于每一步跨源请求，让浏览器接收和发送Session Cookie。第15章的 `XSRF-TOKEN` Cookie保持HttpOnly，页面脚本从 `/auth/csrf` 的**响应体**读取令牌，不读取Cookie。浏览器存储 `JSESSIONID` 不表示请求已经通过授权。

## 三、在工程副本中增加集中CORS配置

从第16章稳定工程的副本开始，新建 `src/main/java/com/example/employee/config/CorsConfig.java`。本实验只允许一个明确的本地Origin，不使用通配符：

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

`@Configuration` 让Spring读取这个配置类；`WebMvcConfigurer` 是MVC扩展接口，`@Override` 表示这里实现它的 `addCorsMappings` 方法。`CorsRegistry` 收集路径对应的跨源规则。`/auth/**` 只开放认证流程所需方法；`/employees/**` 覆盖列表和单条员工API。不要把两个规则误写成允许所有路径和所有来源。

| 方法 | 本例值 | 可接受值或边界 | 作用 |
| --- | --- | --- | --- |
| `addMapping` | `/auth/**`、`/employees/**` | 需要开放的应用路径模式 | 指定规则适用的API路径 |
| `allowedOrigins` | `http://localhost:3000` | 明确的协议、主机和端口；携带凭据时不用 `*` | 指定允许访问的页面来源 |
| `allowedMethods` | GET、POST等 | 该路径实际使用的HTTP方法 | 控制跨源实际请求的方法 |
| `allowedHeaders` | Content-Type等 | 页面实际发送的请求头名称 | 允许JSON和CSRF请求头通过预检 |
| `allowCredentials` | `true` | `true` 或 `false`；默认不为跨源凭据放行 | 允许浏览器发送与接收Cookie |
| `maxAge` | `3600` | 非负秒数 | 预检结果的缓存时间，不是Session有效期 |

`@CrossOrigin(origins = "http://localhost:3000")` 也可以写在单个Controller或方法上，处理局部跨源规则；它由Spring MVC读取，`origins` 要写允许的页面Origin。若项目已经集中配置，不要再随意叠加局部配置，让权限来源难以调查。例如下面仅是注解位置片段，不是替换整个Controller的完整文件：

```java
@CrossOrigin(origins = "http://localhost:3000")
@RestController
@RequestMapping("/employees")
public class EmployeeController {
    // 原有方法保留；实际使用时需导入CrossOrigin等注解。
}
```

`@RestController` 与 `@RequestMapping` 的作用沿用第3～4章；这里新增的仅是CORS允许来源。不要把 `@CrossOrigin` 误认为角色授权注解。

## 四、让Spring Security在认证前处理预检

在第16章的 `SecurityConfig.java` 增加静态导入，并在现有 `http` 配置链中追加 `.cors(withDefaults())`；其余CSRF、Session、URL权限和logout配置保持不变：

```java
import static org.springframework.security.config.Customizer.withDefaults;

// securityFilterChain(...) 方法内，现有 http 配置链的一部分：
http.cors(withDefaults());
```

`withDefaults()` 让Security使用已有的MVC CORS规则；`cors(...)` 使预检先于认证授权处理。OPTIONS通常没有Session Cookie，不能靠把员工接口改成 `permitAll()` 来让它通过。允许凭据时，不能把 `allowedOrigins` 改成 `*`；正式环境必须按实际前端Origin逐一评审。

Spring Security 7.x官方的MVC集成方式支持这种组合：在工程具有Spring MVC且**没有另行提供`CorsConfigurationSource`**时，安全链的`.cors(withDefaults())`使用MVC的`addCorsMappings`规则。本实验只使用上面的一个`CorsConfig`，不再叠加Controller的`@CrossOrigin`、第二个`CorsConfigurationSource`或另一条冲突的Security过滤链。预检由CORS处理，不要求登录Session，也不改变实际员工接口的角色规则。当前`/auth/**`覆盖GET取令牌、POST登录／退出；`/employees/**`覆盖GET、POST、PUT、DELETE；`Content-Type`与`X-XSRF-TOKEN`覆盖示例JSON登录及写请求。若浏览器额外发送其他**非简单请求头**，必须先在Network看`Access-Control-Request-Headers`，再按实际需求评审`allowedHeaders`，不要直接改成任意来源或放宽业务授权。

## 五、用真实浏览器验证，而不只看Postman

在Eclipse启动API，并打开 `http://localhost:3000` 的本地页面。打开浏览器开发者工具的Network和Console；下面代码只在本地实验页面的Console执行，输入课程专用密码，不输入真实账号密码。浏览器地址栏的Origin必须与配置完全一致。

```javascript
(async () => {
  const api = "http://localhost:8080";
  const csrfBefore = await fetch(`${api}/auth/csrf`, {
    credentials: "include"
  });
  const before = (await csrfBefore.json()).data;
  const login = await fetch(`${api}/auth/login`, {
    method: "POST",
    credentials: "include",
    headers: {
      "Content-Type": "application/json",
      [before.headerName]: before.token
    },
    body: JSON.stringify({
      username: "tanaka",
      password: prompt("输入本地课程练习密码")
    })
  });
  const me = await fetch(`${api}/auth/me`, {
    credentials: "include"
  });
  console.log({ loginStatus: login.status, meStatus: me.status });
})().catch(error => console.error("浏览器跨源请求失败", error));
```

这段代码只打印状态码，不打印密码、Cookie或令牌。预期登录和 `/auth/me` 都是200；Network中应能看到登录请求的OPTIONS预检、允许的Origin、允许凭据的响应头，以及后续请求携带Cookie。若浏览器报CORS错误，先检查页面Origin、预检响应和安全链；若请求能到达后端但返回401或403，再按认证、授权和CSRF顺序排查。`prompt()` 被取消时不会得到有效练习密码，需重新执行。

继续从同一页面按第15章流程再GET `/auth/csrf` 取得登录后的令牌，并用 `credentials: "include"` 与对应请求头发送写请求。使用tanaka写员工应按第16章角色规则得到403；改用admin的独立会话并携带有效CSRF，才应允许写操作。Network证据记录方法、URL、Origin、预检结果、实际状态码和脱敏后的响应，不保存 `JSESSIONID`、密码或完整CSRF令牌。Postman可辅助检查后端401/403，但不能代替这一步浏览器验证。

### 排障案例：Postman登录成功，浏览器跨源登录失败

按浏览器Network中的**预检请求、实际请求、后续`/auth/me`**三条记录依次定位，不只看Console一句“CORS error”：

1. 确认页面地址栏的Origin与`allowedOrigins`完全一致；`localhost`与`127.0.0.1`不是相同主机。
2. 找到`POST /auth/login`之前的OPTIONS：检查`Origin`、`Access-Control-Request-Method`和`Access-Control-Request-Headers`，以及预检状态。预检通常没有`JSESSIONID`；如果在此处被认证拒绝，先检查`.cors(withDefaults())`和是否存在冲突配置，不把员工接口改成`permitAll()`。
3. 在预检及实际响应中核对`Access-Control-Allow-Origin`是否为页面的**确切**Origin，`Access-Control-Allow-Credentials`是否为`true`；携带凭据时不能用`*`代替明确来源。
4. 如果实际登录为200，检查浏览器是否接受`Set-Cookie`：在Network的Cookies/Issues中看拒收原因，并核对`SameSite`、`Secure`、`Path`、`Domain`和`credentials: "include"`。不要把完整Cookie值贴进票据。
5. 检查下一条`GET /auth/me`是否带`JSESSIONID`且返回200。若为401，优先查Session保存、Cookie发送或会话过期，而不是先改角色。
6. 写请求若为403，检查登录后是否重新`GET /auth/csrf`、Header是否来自新响应体、Cookie是否同一会话；CSRF无误后再查URL角色和`@PreAuthorize`。保存脱敏的响应状态与数据库未变化证据。

本案例的浏览器结果必须在真实页面和真实API启动后观察；只读到配置代码不能声称OPTIONS、Cookie或Session已经联调成功。

## 六、实验后恢复

删除副本中的 `CorsConfig.java`，移除 `SecurityConfig.java` 里本实验加入的 `.cors(withDefaults())` 和静态导入；不要删除第15～16章原有的CSRF、Session、权限规则。在Eclipse重新启动并运行第16章测试，确认同源主线恢复。生产跨源部署若确有需要，应另行确定前端Origin、Cookie的Secure/SameSite、HTTPS与测试证据，不能直接复制本机地址。

参考：[Spring MVC CORS](https://docs.spring.io/spring-framework/reference/web/webmvc-cors.html)、[Spring Security CORS集成](https://docs.spring.io/spring-security/reference/servlet/integrations/cors.html)、[Spring Security CSRF](https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html)。
