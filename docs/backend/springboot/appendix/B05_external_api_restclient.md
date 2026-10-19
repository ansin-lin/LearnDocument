# B05 Spring Boot调用外部REST API

> 本专题目标：理解Service除了Mapper还可能依赖外部系统，并使用Spring Framework 7的 `RestClient` 完成同步GET、POST、Header、JSON、超时和分层异常处理。

## 一、业务场景与边界

Employee系统需要向HR System查询员工状态，但员工基本信息仍来自本地数据库：

```text
EmployeeController
  → EmployeeService
      ├── EmployeeMapper → MySQL
      └── HrApiClient    → HR System REST API
```

Service后面不一定只有Mapper。`HrApiClient` 负责HTTP通信和外部错误转换，Service负责决定“查不到HR状态时当前业务怎样处理”。Controller不直接拼外部URL，也不接触HR Token。

本附录在**已完成主线第12章的工程副本**中实验；仓库中的[员工管理参考工程](../sample/employee-management-api/README.md)也可作为核对结构的起点，但它已包含第15～16章Security，操作时必须保留认证与CSRF。复用主线 `EmployeeMapper.findById` 和 `Employee`，不要求把HrApiClient永久加入第18章的Employee主线。实验新类放在单独的 `hrlab` 与 `external.hr` 包，接口路径使用 `/lab/employees/**`，避免覆盖现有员工接口。真实项目常见目录可能是：

```text
service/EmployeeService.java
client/HrApiClient.java
dto/external/HrEmployeeResponse.java
```

本专题使用同步客户端 `RestClient`，适合当前Servlet MVC主线。Spring官方将其定义为带流式API的同步HTTP客户端，参见[REST Clients](https://docs.spring.io/spring-framework/reference/integration/rest-clients.html)。老项目中仍可能看到 `RestTemplate`，识读时要继续追踪其Bean、超时和异常处理；本课程新增代码使用RestClient，不把WebClient、Reactive或WebFlux扩展成另一条主线。

## 二、完整最小示例

### 1. 外部接口规格

| 用途 | 方法与路径 | 输入 | 成功响应 |
| --- | --- | --- | --- |
| 查询状态 | `GET /api/employees/{id}/status` | 路径id、Bearer Token、X-Request-Id | `{"employeeId":1001,"status":"ACTIVE"}` |
| 通知确认 | `POST /api/status-confirmations` | Header和JSON | `204 No Content` |

超时：连接2秒，读取3秒。4xx、5xx、网络失败必须转换成不同的本系统异常；示例不自动重试写请求。

### 2. 外部API配置

`application.yml` 只保存非秘密默认值和环境变量占位符：

```yaml
external:
  hr:
    base-url: ${HR_API_BASE_URL}
    token: ${HR_API_TOKEN}
    connect-timeout: 2s
    read-timeout: 3s
```

真实Token由环境或获批的秘密管理方式提供，不能提交到Git、日志或测试证据。

下面的 `HrApiProperties` 是完整配置对象：

```java
package com.example.employee.external.hr;

import java.net.URI;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("external.hr")
public class HrApiProperties {

    private URI baseUrl;
    private String token;
    private Duration connectTimeout;
    private Duration readTimeout;

    public URI getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(URI baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public Duration getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(Duration connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public Duration getReadTimeout() {
        return readTimeout;
    }

    public void setReadTimeout(Duration readTimeout) {
        this.readTimeout = readTimeout;
    }
}
```

`@ConfigurationProperties` 属于Spring Boot，写在配置属性类上。应用上下文启动时，Spring Boot把前缀 `external.hr` 下的外部配置按属性名绑定到这个对象；本例中的 `base-url`、`connect-timeout` 会分别绑定到 `baseUrl`、`connectTimeout`。它只负责绑定，不会单独把该类注册为Bean，本例稍后使用 `@EnableConfigurationProperties` 完成注册。

`URI` 保留协议、主机和路径语义；`Duration` 让 `2s`、`3s` 直接绑定为时间长度。生产启动前应像第17章一样校验非空、HTTPS和正数超时；本练习只使用本机模拟HR地址。真实项目不得把明文HTTP地址和演示Token用于生产。

### 3. 创建RestClient Bean并设置超时

```java
package com.example.employee.external.hr;

import java.net.http.HttpClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(HrApiProperties.class)
public class HrApiConfig {

    @Bean
    public RestClient hrRestClient(HrApiProperties properties) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(properties.getConnectTimeout())
                .build();

        JdkClientHttpRequestFactory requestFactory =
                new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(properties.getReadTimeout());

        return RestClient.builder()
                .baseUrl(properties.getBaseUrl().toString())
                .requestFactory(requestFactory)
                .defaultHeader(
                        HttpHeaders.AUTHORIZATION,
                        "Bearer " + properties.getToken())
                .build();
    }
}
```

`java.net.http.HttpClient` 设置建立连接的最长等待；`JdkClientHttpRequestFactory` 把JDK客户端接入Spring并设置读取响应的最长等待。`RestClient.Builder` 统一保存base URL、请求工厂和默认Authorization Header。

本段首次出现的注解：

| 注解 | 所属与位置 | 处理时机和本例效果 |
| --- | --- | --- |
| `@Configuration` | Spring；配置类 | 容器启动扫描配置时，把该类作为Bean定义来源处理 |
| `@EnableConfigurationProperties(HrApiProperties.class)` | Spring Boot；配置类 | 容器启动时注册并绑定 `HrApiProperties`，使其可以注入 `@Bean` 方法 |
| `@Bean` | Spring；配置类的方法 | 容器创建配置时调用该方法，并把返回的 `RestClient` 注册为Bean；默认Bean名是方法名 `hrRestClient` |

连接超时和读取超时解决不同阶段：前者限制连接建立，后者限制已发送请求后等待响应。两者都必须是正数；无限等待会长期占用处理线程。Token虽然由配置提供，也不能在启动日志中打印。

### 4. JSON请求和响应对象

```java
package com.example.employee.external.hr;

public class HrStatusResponse {

    private Long employeeId;
    private String status;

    public HrStatusResponse() {
    }

    public Long getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(Long employeeId) {
        this.employeeId = employeeId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
```

```java
package com.example.employee.external.hr;

public class HrStatusConfirmationRequest {

    private final Long employeeId;
    private final String confirmedStatus;

    public HrStatusConfirmationRequest(
            Long employeeId,
            String confirmedStatus) {
        this.employeeId = employeeId;
        this.confirmedStatus = confirmedStatus;
    }

    public Long getEmployeeId() {
        return employeeId;
    }

    public String getConfirmedStatus() {
        return confirmedStatus;
    }
}
```

它们表示外部接口契约，不应直接复用本系统的数据库Entity。HR接口字段变化时，影响调查应从这些对象和Client开始，再判断Service及本系统响应是否受影响。

### 5. 区分外部4xx、5xx和网络失败

先准备三种本系统异常：

```java
package com.example.employee.external.hr;

public class HrApiClientException extends RuntimeException {
    public HrApiClientException(String message) {
        super(message);
    }
}
```

```java
package com.example.employee.external.hr;

public class HrApiServerException extends RuntimeException {
    public HrApiServerException(String message) {
        super(message);
    }
}
```

```java
package com.example.employee.external.hr;

public class HrApiConnectionException extends RuntimeException {
    public HrApiConnectionException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

完整Client：

```java
package com.example.employee.external.hr;

import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class HrApiClient {

    private final RestClient hrRestClient;

    public HrApiClient(RestClient hrRestClient) {
        this.hrRestClient = hrRestClient;
    }

    public HrStatusResponse findStatus(Long employeeId, String requestId) {
        try {
            HrStatusResponse response = hrRestClient.get()
                    .uri("/api/employees/{id}/status", employeeId)
                    .header("X-Request-Id", requestId)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError,
                            (request, externalResponse) -> {
                                throw new HrApiClientException(
                                        "HR API rejected status request: "
                                                + externalResponse.getStatusCode());
                            })
                    .onStatus(HttpStatusCode::is5xxServerError,
                            (request, externalResponse) -> {
                                throw new HrApiServerException(
                                        "HR API failed status request: "
                                                + externalResponse.getStatusCode());
                            })
                    .body(HrStatusResponse.class);

            if (response == null || response.getEmployeeId() == null
                    || response.getStatus() == null
                    || response.getStatus().isBlank()
                    || !employeeId.equals(response.getEmployeeId())) {
                throw new HrApiProtocolException("HR API returned an invalid status body");
            }
            return response;
        } catch (ResourceAccessException exception) {
            throw new HrApiConnectionException(
                    "Could not connect to HR API", exception);
        } catch (RestClientException exception) {
            throw new HrApiProtocolException("HR API response contract failed", exception);
        }
    }

    public void confirmStatus(
            HrStatusConfirmationRequest body,
            String requestId) {
        try {
            hrRestClient.post()
                    .uri("/api/status-confirmations")
                    .header("X-Request-Id", requestId)
                    .body(body)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError,
                            (request, externalResponse) -> {
                                throw new HrApiClientException(
                                        "HR API rejected confirmation: "
                                                + externalResponse.getStatusCode());
                            })
                    .onStatus(HttpStatusCode::is5xxServerError,
                            (request, externalResponse) -> {
                                throw new HrApiServerException(
                                        "HR API failed confirmation: "
                                                + externalResponse.getStatusCode());
                            })
                    .toBodilessEntity();
        } catch (ResourceAccessException exception) {
            throw new HrApiConnectionException(
                    "Could not connect to HR API", exception);
        } catch (RestClientException exception) {
            throw new HrApiProtocolException("HR API response contract failed", exception);
        }
    }
}
```

`@Component` 属于Spring，写在类上。组件扫描在应用启动时发现 `HrApiClient` 并把它注册为Bean，随后Spring通过构造器注入前面创建的 `RestClient`；它不会改变类中HTTP调用方法本身的语义。

调用链依次是 `get()`/`post()` 选择HTTP方法，`uri(...)` 组合路径和变量，`header(...)` 添加本次requestId，`body(...)` 由Jackson写JSON，`retrieve()` 执行并进入响应处理，最后 `body(Class)` 读取JSON或 `toBodilessEntity()` 接受无正文成功响应。

再新建 `src/main/java/com/example/employee/external/hr/HrApiProtocolException.java`：

```java
package com.example.employee.external.hr;

public class HrApiProtocolException extends RuntimeException {
    public HrApiProtocolException(String message) { super(message); }
    public HrApiProtocolException(String message, Throwable cause) { super(message, cause); }
}
```

Spring默认会把4xx、5xx转换成 `RestClientException` 子类；本例使用 `onStatus` 先转换成项目能区分的异常。`ResourceAccessException` 表示连接、DNS、连接超时或读取阶段的I/O失败；需沿 `cause` 判断具体阶段，不能仅凭异常类断言。JSON无法解析和字段契约不符由 `HrApiProtocolException` 表示，不能伪装成“员工不存在”。

## 三、把本地员工与HR状态组合成可访问功能

本节的四个文件是**在主线工程副本中新增**，不是替换原有 `EmployeeController` / `EmployeeService`。开始状态：第10～12章的 `com.example.employee.mapper.EmployeeMapper#findById`、`com.example.employee.entity.Employee` 和第11章的 `RequestLoggingFilter` 已存在；主线MySQL测试库已有员工1001。若主线包名被项目改过，先统一调整下列import。`/lab` 路径仍受现有Security规则约束，不为了实验放开正式权限。

`src/main/java/com/example/employee/hrlab/EmployeeHrView.java`：

```java
package com.example.employee.hrlab;

public record EmployeeHrView(Long id, String name, String department,
                             String hrStatus) { }
```

`record` 是Java 17可用的数据载体；只向客户端返回这四个字段，不能直接把本地Entity或外部HR完整响应泄露出去。`src/main/java/com/example/employee/hrlab/ConfirmStatusRequest.java`：

```java
package com.example.employee.hrlab;

public record ConfirmStatusRequest(String confirmedStatus) { }
```

`src/main/java/com/example/employee/hrlab/EmployeeService.java`：

```java
package com.example.employee.hrlab;

import com.example.employee.entity.Employee;
import com.example.employee.exception.EmployeeNotFoundException;
import com.example.employee.external.hr.HrApiClient;
import com.example.employee.external.hr.HrStatusConfirmationRequest;
import com.example.employee.external.hr.HrStatusResponse;
import com.example.employee.mapper.EmployeeMapper;
import org.springframework.stereotype.Service;

@Service
public class EmployeeService {
    private final EmployeeMapper employeeMapper;
    private final HrApiClient hrApiClient;

    public EmployeeService(EmployeeMapper employeeMapper, HrApiClient hrApiClient) {
        this.employeeMapper = employeeMapper;
        this.hrApiClient = hrApiClient;
    }

    public EmployeeHrView findWithHrStatus(Long id, String requestId) {
        Employee employee = findLocal(id);
        HrStatusResponse hr = hrApiClient.findStatus(id, requestId);
        return new EmployeeHrView(employee.getId(), employee.getName(),
                employee.getDepartment(), hr.getStatus());
    }

    public void confirmStatus(Long id, String status, String requestId) {
        findLocal(id);
        if (status == null || status.isBlank()) {
            throw new IllegalArgumentException("confirmedStatus is required");
        }
        hrApiClient.confirmStatus(
                new HrStatusConfirmationRequest(id, status), requestId);
    }

    private Employee findLocal(Long id) {
        Employee employee = employeeMapper.findById(id);
        if (employee == null) {
            throw new EmployeeNotFoundException(id);
        }
        return employee;
    }
}
```

本地员工不存在时不调用HR；HR 404则表示依赖系统与本地不同步，本例不改成本系统员工404，而由下方Advice返回502。Service不标本地数据库写事务，因为两个方法只读本地库并发出外部请求；实际增加本地写入时需重新设计事务边界。`src/main/java/com/example/employee/hrlab/EmployeeController.java`：

```java
package com.example.employee.hrlab;

import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.UUID;

@RestController
@RequestMapping("/lab/employees")
public class EmployeeController {
    private final EmployeeService employeeService;

    public EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    @GetMapping("/{id}/hr-status")
    public EmployeeHrView status(@PathVariable Long id) {
        return employeeService.findWithHrStatus(id, requestId());
    }

    @PostMapping("/{id}/status-confirmations")
    public ResponseEntity<Void> confirm(@PathVariable Long id,
                                        @RequestBody ConfirmStatusRequest request) {
        employeeService.confirmStatus(id, request.confirmedStatus(), requestId());
        return ResponseEntity.noContent().build();
    }

    private String requestId() {
        String value = MDC.get("requestId");
        return value == null ? UUID.randomUUID().toString() : value;
    }
}
```

在普通同步HTTP请求中，第11章 `RequestLoggingFilter` 先生成requestId，写入MDC并返回 `X-Request-Id`；Controller从同一线程的MDC读取并传给Client。`UUID` 分支仅供不经过该Filter的直接方法调用/测试，不能替代正式请求链的统一编号；异步跨线程时要另行传播上下文。`POST` 使用本地请求DTO，不让前端决定外部URL、Token或员工ID。

`src/main/java/com/example/employee/hrlab/HrApiAdvice.java`：

```java
package com.example.employee.hrlab;

import com.example.employee.external.hr.HrApiClientException;
import com.example.employee.external.hr.HrApiConnectionException;
import com.example.employee.external.hr.HrApiProtocolException;
import com.example.employee.external.hr.HrApiServerException;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = EmployeeController.class)
public class HrApiAdvice {
    public record ErrorBody(String code, String message, String requestId) { }

    @ExceptionHandler({HrApiClientException.class, HrApiServerException.class,
                       HrApiProtocolException.class})
    public ResponseEntity<ErrorBody> badGateway(RuntimeException exception) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(
                new ErrorBody("HR_DEPENDENCY_ERROR", "HR status unavailable",
                        MDC.get("requestId")));
    }

    @ExceptionHandler(HrApiConnectionException.class)
    public ResponseEntity<ErrorBody> unavailable(HrApiConnectionException exception) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(
                new ErrorBody("HR_CONNECTION_ERROR", "HR status unavailable",
                        MDC.get("requestId")));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorBody> invalidInput(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(
                new ErrorBody("INVALID_STATUS", "confirmedStatus is required",
                        MDC.get("requestId")));
    }
}
```

Advice只处理这个练习Controller的外部依赖异常，不覆盖主线全局异常处理。生产方案还应按第7章统一错误结构，并记录脱敏后的异常原因链；不把外部响应正文返回客户端。外部401/403常提示本系统HR凭据或权限配置问题，不能映射成当前用户未登录；外部404也不自动成为本地404。

## 四、错误分类与当前API响应

| 情况 | 含义 | Client层 | Service/Controller层考虑 |
| --- | --- | --- | --- |
| 本系统业务错误 | 本地规格拒绝操作 | 不调用HR或正常返回后判断 | 按本系统400/404/409规格 |
| 外部4xx | HR拒绝本系统请求 | `HrApiClientException` | 调查请求契约；不原样透传内部响应 |
| 外部5xx | HR已接收但处理失败 | `HrApiServerException` | 按依赖系统故障处理，通常是502/503类策略 |
| DNS/连接失败 | 未建立可用通信 | `HrApiConnectionException` | 记录目标系统代号、requestId和原因链 |
| Connection Refused | 地址可达但目标端口拒绝连接 | connection异常 | 核对目标进程、端口、防火墙和部署状态 |
| 连接超时 | 建连超过2秒 | connection异常 | 区分网络、地址、防火墙 |
| 读取超时 | 建连后3秒未完成响应 | read异常 | 区分HR性能和超时规格 |
| JSON反序列化失败 | 已收到响应但正文不符合DTO契约 | 协议/响应异常 | 保存脱敏后的契约差异，不伪装成404 |

“外部返回404”不一定等于本系统员工404；它可能表示HR中未同步。必须由Service根据已确认业务规格转换。不要 `catch (Exception)` 后全部返回同一消息，否则会失去重试判断、告警优先级和障害调查线索。

## 五、日志与安全

可以记录：外部系统代号、HTTP方法、脱敏路径模板、状态码、耗时、requestId和异常类型。禁止记录：Bearer Token、Authorization Header、密码、Cookie、完整个人数据和未经限制的外部响应正文。

请求与响应日志即使是DEBUG也要脱敏。外部响应可能包含HR系统的内部错误、账号或个人信息，不能直接拼到对外错误消息。

## 六、自动化测试与影响调查

自动化测试应使用模拟HTTP服务器或Spring提供的Client测试支持，不依赖真实HR环境。当前Boot 4.1.1主线已有 `spring-boot-starter-test` 和 `spring-boot-starter-webmvc-test`；核对POM中存在Spring Test后可使用 `MockRestServiceServer`。它在内存中替换HTTP请求工厂，不需要启动真正的HR进程，但**不能**证明DNS/连接/读取超时的真实网络行为；后两类还要用本机网络模拟服务做集成测试。[Spring官方测试说明](https://docs.spring.io/spring-framework/reference/testing/spring-mvc-test-client.html)也建议以真实监听端口的mock server验证传输层问题。

附录提供可编译的[HR隔离实验工程](examples/hr-client-lab/README.md)、[Client测试](examples/hr-client-lab/src/test/java/com/example/hrlab/external/HrApiClientTest.java)、[Service测试](examples/hr-client-lab/src/test/java/com/example/hrlab/external/EmployeeServiceTest.java)和[Controller/Security测试](examples/hr-client-lab/src/test/java/com/example/hrlab/external/EmployeeControllerSecurityTest.java)。在 `examples/hr-client-lab` 目录执行 `mvn test`；它以Java 17为编译目标、Boot 4.1.1为依赖，不访问MySQL，也不改变主线工程。教学正文使用正式包名 `com.example.employee.external.hr`；实验工程为避免依赖主线数据库，把类组织在 `com.example.hrlab.external`，并以只读内存Mapper模拟员工1001。正式集成时仍须按前文拆成各自Java文件、沿用主线MyBatis Mapper和Security策略。

2026-10-19 实际执行：Maven运行JDK 21.0.10、编译参数 `--release 17`，3个测试类、22例全部通过（0失败、0跳过）；其中 Client 10、Service 5、Controller/Security 7。`mvn test` 不等于已验证真实HR、真实MySQL和主线整合。各层方法名与关键断言以链接的测试源码为准：Service断言本地缺失时零HR调用、HR 404/500不触发本地写入；MockMvc断言200 JSON、401、403、502及CSRF与授权的不同拦截位置。

真实延迟响应实验 `realDelayedResponseTriggersReadTimeoutAndPreservesCause` 在本机随机端口启动HTTP Server，先建立连接、延迟600ms再返回正文；JDK请求工厂读取超时100ms。实际断言异常链为 `HrApiConnectionException → ResourceAccessException → HttpTimeoutException`，实验结束关闭服务。`simulatedReadTimeoutClassification` 只验证模拟异常的分类，不冒充网络实测。连接超时取决于目标IP、路由和系统网络策略，未稳定复现；不能以HTTP 500代替。Connection Refused是建连阶段被拒绝，读取超时是建连后等待响应，HTTP 500/404是已收到状态码，非法JSON是已收到响应但契约不符。POST确认请求没有自动重试：请求可能已经被HR执行而只是响应丢失，盲目重试会造成重复写入；若业务要重试，先设计幂等键和有限次数/退避策略。

新建 `src/test/java/com/example/employee/external/hr/HrApiClientTest.java`。以下完整类覆盖GET/POST、Header、JSON、外部404/500、I/O异常、空响应、非法JSON和异常消息不含Token；运行前确认项目的Boot 4测试依赖已下载。为了让请求工厂受模拟器控制，测试直接构造 `RestClient.Builder`，**不调用**生产 `HrApiConfig`。

```java
package com.example.employee.external.hr;

import java.io.IOException;
import java.net.SocketTimeoutException;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class HrApiClientTest {
    private MockRestServiceServer server;
    private HrApiClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder()
                .baseUrl("http://localhost:18081")
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer demo-token");
        server = MockRestServiceServer.bindTo(builder).build();
        client = new HrApiClient(builder.build());
    }

    @Test
    void getSuccessAndHeaders() {
        server.expect(requestTo("http://localhost:18081/api/employees/1001/status"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("X-Request-Id", "req-1"))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer demo-token"))
                .andRespond(withSuccess(
                        "{\"employeeId\":1001,\"status\":\"ACTIVE\"}",
                        MediaType.APPLICATION_JSON));
        assertEquals("ACTIVE", client.findStatus(1001L, "req-1").getStatus());
        server.verify();
    }

    @Test
    void postJsonAndHeaders() {
        server.expect(requestTo("http://localhost:18081/api/status-confirmations"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-Request-Id", "req-2"))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer demo-token"))
                .andExpect(content().json(
                        "{\"employeeId\":1001,\"confirmedStatus\":\"ACTIVE\"}"))
                .andRespond(withNoContent());
        client.confirmStatus(new HrStatusConfirmationRequest(1001L, "ACTIVE"),
                "req-2");
        server.verify();
    }

    @Test
    void external404IsNotLocal404() {
        server.expect(requestTo("http://localhost:18081/api/employees/1001/status"))
                .andRespond(withResourceNotFound());
        assertThrows(HrApiClientException.class,
                () -> client.findStatus(1001L, "req-3"));
        server.verify();
    }

    @Test
    void external500() {
        server.expect(requestTo("http://localhost:18081/api/employees/1001/status"))
                .andRespond(withServerError());
        assertThrows(HrApiServerException.class,
                () -> client.findStatus(1001L, "req-4"));
        server.verify();
    }

    @Test
    void connectionFailure() {
        server.expect(requestTo("http://localhost:18081/api/employees/1001/status"))
                .andRespond(withException(new IOException("connection refused")));
        assertThrows(HrApiConnectionException.class,
                () -> client.findStatus(1001L, "req-5"));
        server.verify();
    }

    @Test
    void simulatedReadTimeoutClassification() {
        server.expect(requestTo("http://localhost:18081/api/employees/1001/status"))
                .andRespond(withException(new SocketTimeoutException("read timed out")));
        assertThrows(HrApiConnectionException.class,
                () -> client.findStatus(1001L, "req-6"));
        server.verify();
    }

    @Test
    void emptyBody() {
        server.expect(requestTo("http://localhost:18081/api/employees/1001/status"))
                .andRespond(withSuccess("", MediaType.APPLICATION_JSON));
        assertThrows(HrApiProtocolException.class,
                () -> client.findStatus(1001L, "req-7"));
        server.verify();
    }

    @Test
    void invalidJson() {
        server.expect(requestTo("http://localhost:18081/api/employees/1001/status"))
                .andRespond(withSuccess("{invalid", MediaType.APPLICATION_JSON));
        assertThrows(HrApiProtocolException.class,
                () -> client.findStatus(1001L, "req-8"));
        server.verify();
    }

    @Test
    void tokenIsNotInErrorMessageOrCapturedLogs() {
        server.expect(requestTo("http://localhost:18081/api/employees/1001/status"))
                .andRespond(withServerError());
        Logger root = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        ListAppender<ILoggingEvent> captured = new ListAppender<>();
        captured.start();
        root.addAppender(captured);
        try {
            RuntimeException error = assertThrows(HrApiServerException.class,
                    () -> client.findStatus(1001L, "req-9"));
            assertFalse(error.getMessage().contains("demo-token"));
            assertTrue(captured.list.stream().noneMatch(event ->
                    event.getFormattedMessage().contains("demo-token")));
        } finally {
            root.detachAppender(captured);
            captured.stop();
        }
        server.verify();
    }
}
```

最后一项捕获本测试进程的Logback日志并检查演示Token；它只覆盖这一条异常路径，正式Review还应发送正常请求、检查HTTP拦截器、代理日志及部署平台日志，逐条断言无 `Authorization`、`Bearer` 和Token。网络异常测试里的 `SocketTimeoutException` 只验证分类逻辑，不等于验证生产请求工厂真的在3秒触发读取超时。以下两类证据仍须单独提供：①本地无监听端口或无法解析域名以验证连接/DNS失败；②本机mock服务先接受连接、故意延迟超过3秒以验证读取超时。测试要记录实际耗时、异常根因和是否有HR侧处理，避免把“响应超时”误认为“请求没到达”。

还要补Service与Web层测试：Mock `EmployeeMapper` 和 `HrApiClient`，验证本地员工不存在时Client调用次数为0、正常组合字段、外部异常不被吞掉；用MockMvc验证 `/lab/employees/1001/hr-status` 的响应字段、外部404对应502、连接失败对应503、无权限请求在Controller前被Security拒绝。测试类可按[第12章](../mainline/12_automated_testing_and_regression.md)的Mockito/MockMvc方式写，不能把上述Client测试当作完整端到端证明。

建议的验收矩阵：GET成功、POST成功、授权Header与requestId、JSON入/出、外部404、外部500、连接异常、读取超时、空正文、非法JSON、Token不进入日志，共11项。上述Client类覆盖这些场景的单元级行为；真实网络超时、完整日志链和服务组合仍需追加集成证据，未执行前标记“未验证”。

原有检查要点至少覆盖：

1. GET成功并把JSON映射为 `HrStatusResponse`；
2. POST成功且Header、URI和JSON正文正确；
3. 4xx和5xx转换成不同异常；
4. 连接失败和读取超时；
5. 空正文或不符合契约的JSON；
6. 日志和异常消息不包含Token。

改修外部接口时调查配置、Client、外部DTO、Service调用方、全局异常、超时、日志、测试、部署环境变量和运维监控。发布证据中使用模拟值或脱敏结果。

## 七、运行、事务边界与练习

本章代码只对本地库**读取**，HR POST可能写入外部系统。若未来一个Service方法同时更新本地员工并调用HR，`@Transactional` 只能控制本地数据库；外部POST已经成功后，本地事务回滚并不会撤销HR动作。相同POST自动重试可能重复确认，应由业务规格定义幂等键（例如一次操作ID）并由HR按键去重；已发生的跨系统不一致需要补偿或人工对账。没有HR确认的幂等契约前，本例不自动重试POST。

手动联调可在工程副本之外新建 `HrMockServer.java`，使用Java 17自带的轻量HTTP服务器，不引入第三方依赖。它只监听本机，不用于生产：

```java
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

public class HrMockServer {
    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(
                new InetSocketAddress("127.0.0.1", 18081), 0);
        server.createContext("/api/employees/1001/status", exchange -> {
            if (!"GET".equals(exchange.getRequestMethod())) {
                reply(exchange, 405, "");
                return;
            }
            reply(exchange, 200, "{\"employeeId\":1001,\"status\":\"ACTIVE\"}");
        });
        server.createContext("/api/status-confirmations", exchange -> {
            if (!"POST".equals(exchange.getRequestMethod())) {
                reply(exchange, 405, "");
                return;
            }
            exchange.getRequestBody().readAllBytes(); // 仅消费演示请求；正式服务必须校验JSON
            reply(exchange, 204, "");
        });
        server.start();
        System.out.println("HR mock listening on 127.0.0.1:18081");
    }

    private static void reply(HttpExchange exchange, int code, String body)
            throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        if (code != 204) {
            exchange.getResponseHeaders().set("Content-Type", "application/json");
        }
        exchange.sendResponseHeaders(code, code == 204 ? -1 : bytes.length);
        if (code != 204) {
            exchange.getResponseBody().write(bytes);
        }
        exchange.close();
    }
}
```

在保存该文件的目录执行 `javac HrMockServer.java`、`java HrMockServer`；另开PowerShell设置 `$env:HR_API_BASE_URL='http://127.0.0.1:18081'` 与 `$env:HR_API_TOKEN='demo-token'`。这个mock只验证成功路径，**不校验Bearer Token，也不输出/保存它**；不能用它证明Header正确、权限安全、真实超时或错误映射，这些应由上方测试和专项集成测试提供。停止时在mock终端按 Ctrl+C，删除临时 `.java`/`.class` 文件。

运行顺序：在主线副本中新增上述文件和 `application.yml` 的 `external.hr` 配置，把 `HR_API_BASE_URL` 指向本机mock、`HR_API_TOKEN` 设为演示值；确认**独立MySQL测试库**有员工1001，切到副本工程根目录运行 `./mvnw test`（Windows PowerShell用 `./mvnw.cmd test`）。参考工程测试会重建专用测试表，必须先按其README核对 `TEST_DB_URL`，禁止对开发或生产库运行。测试运行时 `MockRestServiceServer` 在测试进程内模拟HTTP，不需要真实HR。手动联调先启动上述本地服务，再启动应用；发送 `GET /lab/employees/1001/hr-status` 应得到本地 `id/name/department` 与HR `hrStatus`；发送 `POST /lab/employees/1001/status-confirmations`、JSON `{"confirmedStatus":"ACTIVE"}` 预期204。若当前主线启用了Session/CSRF，按第15～16章使用测试身份和CSRF Token，不关闭安全配置。检查响应 `X-Request-Id` 与应用日志一致，同时确认日志没有Token；本mock没有记录入站requestId，端到端Header传播还需由自动化测试或增强mock验证。完成后删除 `hrlab`、`external.hr` 实验包和新增配置，运行主线测试确认恢复。

### 练习1：Review请求头日志

```java
log.info("request headers={}", headers);
```

指出Authorization、API Key、Cookie和个人数据泄露风险，改为只记录外部系统代号、方法、脱敏路径、状态、耗时和requestId。

### 练习2：Review吞掉异常

```java
catch (Exception exception) {
    return null;
}
```

分析调用方空指针、错误分类丢失、监控无法告警和障害调查缺少原因链的问题；提出按4xx、5xx、连接和响应契约分类转换的方向。

### 练习3：调查没有Timeout的调用

说明外部系统迟迟不响应时，应用线程、连接和后续请求会受到什么影响；补出连接超时、读取超时、配置来源和对应测试规格。

### 练习4：Review无限重试

分析外部500后立即无限重试造成的线程占用、故障放大和重复POST风险。Retry必须有次数、间隔、总超时、幂等性和规格依据；本附录不要求实现复杂重试机制。

### 练习5：完成影响调查

为HR查询增加 `X-Request-Id` 传播测试；再假设HR把status改名为employmentStatus，画出Controller→Service→Mapper/Client两条分支，并提交DTO、Client、Service、异常、日志和回归测试影响表，不直接修改Employee主线。
