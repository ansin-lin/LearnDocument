# B07 Spring Boot 跨域问题与解决方案

前置：了解 [HTTP 请求与响应](../../../web_basics/01_http_rest_cookie_cors.md)，并完成 Spring Boot 第 3～4 章的 Controller 基础。本章用一个**不登录的独立实验接口**认识 CORS；若实际项目使用 Session、Cookie 和 CSRF，必须另行评审浏览器凭据、Cookie属性和安全链配置，不要直接套用本章的无凭据配置。

完成本章后，你应能从浏览器 Network 判断问题是否真的属于跨域，给指定前端来源开放必要的 API，并证明允许与未允许的来源得到不同结果。

## 一、为什么同一个接口，浏览器报错而接口工具成功

假设前端页面地址是 `http://localhost:5173`，API 地址是 `http://localhost:8080`。浏览器判断“同源”时比较**协议、主机、端口**，端口不同就是跨源。页面脚本发起跨源请求时，浏览器会检查服务器的 CORS 响应头；这项检查不会因为 API 本身返回 200 就自动通过。

```text
页面 Origin：http://localhost:5173
请求目标：http://localhost:8080/api/cors-demo
API 响应：Access-Control-Allow-Origin: http://localhost:5173
浏览器：允许该页面的脚本读取响应
```

`Origin` 是发起请求的页面来源，不是 API 地址。`localhost` 与 `127.0.0.1` 也不是同一个 Origin。Postman 和 `curl` 不执行浏览器的同源限制，因此“Postman 成功”只能证明接口可访问，不能证明浏览器能读取响应。

CORS 不是登录、授权，也不是阻止所有外部请求的防火墙。服务器仍要独立检查身份、角色、数据范围和必要的 CSRF 令牌。不要为了消除浏览器报错而关闭这些检查。

## 二、先用一个 GET 接口解决最小问题

在第 4 章尚未加入 Spring Security 的 Spring Boot Web 工程**副本**中，新建 `src/main/java/com/example/employee/web/CorsDemoController.java`。如果你的启动类不在 `com.example.employee` 包及其上层，按现有项目的组件扫描范围调整包名。以下是完整的独立 Controller，不需要数据库：

```java
package com.example.employee.web;

import java.util.Map;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CorsDemoController {

    @CrossOrigin(origins = "http://localhost:5173")
    @GetMapping("/api/cors-demo")
    public Map<String, String> message() {
        return Map.of("message", "CORS OK");
    }

    @PostMapping("/api/cors-demo")
    public Map<String, String> submit() {
        return Map.of("message", "POST OK");
    }
}
```

`@GetMapping` 把 GET `/api/cors-demo` 交给 `message()`；`@PostMapping` 提供后面观察预检的独立实验入口，不写数据库。`@RestController` 把返回值写成 JSON。`@CrossOrigin` 由 Spring MVC 读取，这里只标在 GET 方法上，仅允许来源为 `http://localhost:5173` 的页面脚本读取 GET 响应；POST 暂未开放跨源。`origins` 的值是**前端页面**的完整 Origin，不包含路径。这个注解可以放在方法或 Controller 类上：一个接口临时联调可放在方法上，多个接口需要同一规则时优先考虑集中配置。不要只写无参数的 `@CrossOrigin`，它会允许所有来源访问标注的接口。

启动 API 后，先在 PowerShell 中检查接口本身。下面的命令只模拟请求头，不代替浏览器验证：

```powershell
curl.exe -i -H "Origin: http://localhost:5173" http://localhost:8080/api/cors-demo
```

预期可看到 200、JSON `{"message":"CORS OK"}`，以及 `Access-Control-Allow-Origin: http://localhost:5173`。如果没有 200，先查服务是否启动、路径是否正确、Controller 是否进入扫描范围；不要把 404 或连接失败当成 CORS 配置问题。

再在**确实运行于** `http://localhost:5173` 的本地页面打开开发者工具 Console，执行：

```javascript
fetch("http://localhost:8080/api/cors-demo")
  .then(response => response.json())
  .then(data => console.log(data.message))
  .catch(error => console.error("请求失败", error));
```

预期打印 `CORS OK`。在 Network 中核对 Request Headers 的 `Origin` 与 Response Headers 的 `Access-Control-Allow-Origin`。若前端实际运行在其他端口，先确认地址栏，再相应修改允许来源并重启或重新加载服务端配置。

## 三、多个接口如何统一配置

当同一组 API 都允许同一前端访问，可用集中配置代替逐个添加注解。**这是上节注解方案的替代方案，不要与其同时叠加做本实验。**先移除 `CorsDemoController.message()` 上的 `@CrossOrigin`，再新建 `src/main/java/com/example/employee/config/CorsConfig.java`：

```java
package com.example.employee.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins("http://localhost:5173")
                .allowedMethods("GET")
                .allowedHeaders("Content-Type")
                .allowCredentials(false);
    }
}
```

`@Configuration` 使该类进入 Spring 配置；`WebMvcConfigurer.addCorsMappings` 在 MVC 建立路由规则时登记 CORS 规则。`addMapping` 只覆盖 `/api/**`；`allowedOrigins` 指定可访问的页面来源；`allowedMethods` 限制方法；`allowedHeaders` 约束预检时允许的请求头；`allowCredentials(false)` 表示本实验不允许跨源携带 Cookie 等凭据。实际项目应按接口路径、方法和请求头评审，不把所有路径与所有来源一并开放。

重启后，重复上一节的命令和浏览器验证。然后从另一个来源（例如 `http://localhost:3000` 的页面）请求同一接口，在 Network 中观察响应未获该来源的 CORS 许可，页面脚本不能读取结果。该页面必须真实存在；也可用 `curl.exe` 改变 `Origin` 请求头辅助观察响应头，但不能单靠 `curl.exe` 宣称浏览器已验证。

## 四、什么时候会先出现 OPTIONS

上面的普通 GET 通常直接发送实际请求。跨源的 JSON POST、PUT、DELETE 或携带自定义请求头等请求，浏览器通常先发 **OPTIONS 预检**，询问服务器是否允许当前 Origin、方法和请求头；预检通过后才发送实际请求。例如请求 JSON POST 时，Network 可看到：

```text
OPTIONS /api/employees
Origin: http://localhost:5173
Access-Control-Request-Method: POST
Access-Control-Request-Headers: content-type
```

这里 `Access-Control-Request-Method` 是浏览器要发送的**实际方法**，不是说业务接口应改成 OPTIONS。服务端规则若只允许 GET，POST 预检就不能通过。要开放 POST，应先确认业务接口确实存在，再把该路径的 `allowedMethods` 加入 `POST`，根据实际预检头审查 `allowedHeaders`。预检成功也不代表实际请求一定成功：后续仍可能因为参数、认证或业务规则返回 400、401、403 等。

若工程启用了 Spring Security，预检可能在 MVC 处理前被安全过滤器链拒绝。此时需要使 Security 的 CORS 处理与 MVC 规则协作；不能用 `permitAll()` 开放业务接口来“修复跨域”。带 Session 的跨源请求还需单独核对`http.cors(...)`、浏览器Cookie策略和CSRF，本基础实验不提供可直接套用的配置。

## 五、按证据排查，而不是先加通配符

| 浏览器中看到的现象 | 先检查 | 合理处理 |
| --- | --- | --- |
| `Failed to fetch`，Network 无响应 | API 是否启动、地址和端口、HTTPS/HTTP 是否匹配 | 先修连接或协议问题 |
| 有 OPTIONS，实际请求未发送 | 预检状态、`Origin`、请求方法和 `Access-Control-Request-Headers` | 对照路径与 CORS 规则；启用 Security 时再查安全链 |
| 实际响应有 200，Console 仍提示 CORS | 响应是否有匹配的 `Access-Control-Allow-Origin` | 校正前端 Origin 与服务端允许列表 |
| 浏览器可读响应，但状态为 401/403 | Cookie/登录、授权或 CSRF；区分实际响应与预检 | 按第 15～16 章及 A16 排查，不放宽 CORS 代替权限 |
| 本地成功、上线失败 | 正式前端 Origin、反向代理、HTTPS、不同环境配置 | 按获批部署地址配置并在真实浏览器复测 |

跨源携带 Cookie 时，服务端需要明确的允许来源与凭据配置，前端也要正确请求凭据；`allowedOrigins("*")` 不能与 `allowCredentials(true)` 组合。Cookie 的 `SameSite`、`Secure` 和 CSRF 另有独立规则，本章的无登录示例**不能证明**跨源登录可用。

## 六、动手练习与恢复

在工程副本上完成三项检查：

1. 让 `http://localhost:5173` 页面成功读取 `/api/cors-demo`，保留脱敏的 Network 记录，指出实际 `Origin` 和允许来源响应头。
2. 从另一个真实本地 Origin 请求，记录页面为何不能读取；不要把 `curl.exe` 的状态码当作浏览器结果。
3. 在采用集中配置的方案下，从允许来源页面执行下面的 JSON POST。先确认只允许 GET 时预检不通过；再把 `allowedMethods("GET")` 改成 `allowedMethods("GET", "POST")` 并重启，预期预检通过、实际响应为 `POST OK`。分别记录 OPTIONS 与 POST 的结果。

   ```javascript
   fetch("http://localhost:8080/api/cors-demo", {
     method: "POST",
     headers: { "Content-Type": "application/json" },
     body: "{}"
   })
     .then(response => response.json())
     .then(data => console.log(data.message))
     .catch(error => console.error("请求失败", error));
   ```

实验结束后删除副本中的 `CorsDemoController.java` 与 `CorsConfig.java`（若只采用注解方案，移除注解和实验 Controller 即可），重启并确认原有接口行为不变。不要在主线项目或生产配置里保留教学用 `localhost` 来源。

参考：[Spring MVC CORS 官方文档](https://docs.spring.io/spring-framework/reference/web/webmvc-cors.html)、[Spring Security CORS 官方文档](https://docs.spring.io/spring-security/reference/servlet/integrations/cors.html)。
