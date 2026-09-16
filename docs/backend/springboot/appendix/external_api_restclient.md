# Appendix A：Spring Boot调用外部REST API

> 本专题目标：理解Service除了Mapper还可能依赖外部系统，并使用Spring Framework 6.2的 `RestClient` 完成同步GET、POST、Header、JSON、超时和分层异常处理。

## 一、业务场景与边界

Employee系统需要向HR System查询员工状态，但员工基本信息仍来自本地数据库：

```text
EmployeeController
  → EmployeeService
      ├── EmployeeMapper → MySQL
      └── HrApiClient    → HR System REST API
```

Service后面不一定只有Mapper。`HrApiClient` 负责HTTP通信和外部错误转换，Service负责决定“查不到HR状态时当前业务怎样处理”。Controller不直接拼外部URL，也不接触HR Token。

本附录是独立识读与实验，不要求把HrApiClient永久加入第20章的Employee主线。真实项目常见目录可能是：

```text
service/EmployeeService.java
client/HrApiClient.java
dto/external/HrEmployeeResponse.java
```

本专题使用同步客户端 `RestClient`，适合当前Servlet MVC主线。Spring官方将其定义为带流式API的同步HTTP客户端，参见[REST Clients](https://docs.spring.io/spring-framework/reference/6.2/integration/rest-clients.html)。老项目中仍可能看到 `RestTemplate`，识读时要继续追踪其Bean、超时和异常处理；本课程新增代码使用RestClient，不把WebClient、Reactive或WebFlux扩展成另一条主线。

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

`URI` 保留协议、主机和路径语义；`Duration` 让 `2s`、`3s` 直接绑定为时间长度。生产启动前应像第17章一样校验非空、HTTPS和正数超时，本示例省略重复的Validation代码。

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

            if (response == null) {
                throw new HrApiServerException("HR API returned an empty body");
            }
            return response;
        } catch (ResourceAccessException exception) {
            throw new HrApiConnectionException(
                    "Could not connect to HR API", exception);
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
        }
    }
}
```

调用链依次是 `get()`/`post()` 选择HTTP方法，`uri(...)` 组合路径和变量，`header(...)` 添加本次requestId，`body(...)` 由Jackson写JSON，`retrieve()` 执行并进入响应处理，最后 `body(Class)` 读取JSON或 `toBodilessEntity()` 接受无正文成功响应。

Spring默认会把4xx、5xx转换成 `RestClientException` 子类；本例使用 `onStatus` 先转换成项目能区分的异常。`ResourceAccessException` 表示连接、DNS、连接超时或读取阶段的I/O失败。JSON无法解析等其他 `RestClientException` 还应作为外部协议/响应错误调查，不能全部伪装成“员工不存在”。

## 三、错误分类与当前API响应

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

## 四、日志与安全

可以记录：外部系统代号、HTTP方法、脱敏路径模板、状态码、耗时、requestId和异常类型。禁止记录：Bearer Token、Authorization Header、密码、Cookie、完整个人数据和未经限制的外部响应正文。

请求与响应日志即使是DEBUG也要脱敏。外部响应可能包含HR系统的内部错误、账号或个人信息，不能直接拼到对外错误消息。

## 五、测试与影响调查

自动化测试应使用模拟HTTP服务器或Spring提供的Client测试支持，不依赖真实HR环境。至少覆盖：

1. GET成功并把JSON映射为 `HrStatusResponse`；
2. POST成功且Header、URI和JSON正文正确；
3. 4xx和5xx转换成不同异常；
4. 连接失败和读取超时；
5. 空正文或不符合契约的JSON；
6. 日志和异常消息不包含Token。

改修外部接口时调查配置、Client、外部DTO、Service调用方、全局异常、超时、日志、测试、部署环境变量和运维监控。发布证据中使用模拟值或脱敏结果。

## 六、练习

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
