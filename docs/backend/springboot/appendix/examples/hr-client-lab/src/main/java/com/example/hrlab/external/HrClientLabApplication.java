package com.example.hrlab.external;

import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.RestClient;

@SpringBootApplication
@EnableConfigurationProperties(HrApiProperties.class)
public class HrClientLabApplication {
    public static void main(String[] args) { SpringApplication.run(HrClientLabApplication.class, args); }

    @Bean
    RestClient hrRestClient(HrApiProperties properties) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(properties.getConnectTimeout()).build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(properties.getReadTimeout());
        return RestClient.builder().baseUrl(properties.getBaseUrl().toString())
                .requestFactory(factory)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getToken())
                .build();
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http.authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/lab/employees/**").hasRole("HR_READER")
                        .anyRequest().denyAll())
                .httpBasic(Customizer.withDefaults()).build();
    }

    @Bean
    EmployeeMapper employeeMapper() {
        // 隔离实验只模拟已存在的本地员工；正式 Employee 工程仍使用 MyBatis。
        return id -> id.equals(1001L) ? new Employee(1001L, "Aki", "IT") : null;
    }
}

@ConfigurationProperties("external.hr")
class HrApiProperties {
    private URI baseUrl;
    private String token;
    private Duration connectTimeout;
    private Duration readTimeout;
    public URI getBaseUrl() { return baseUrl; }
    public void setBaseUrl(URI baseUrl) { this.baseUrl = baseUrl; }
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public Duration getConnectTimeout() { return connectTimeout; }
    public void setConnectTimeout(Duration connectTimeout) { this.connectTimeout = connectTimeout; }
    public Duration getReadTimeout() { return readTimeout; }
    public void setReadTimeout(Duration readTimeout) { this.readTimeout = readTimeout; }
}

record Employee(Long id, String name, String department) { }
interface EmployeeMapper { Employee findById(Long id); }
record EmployeeHrView(Long id, String name, String department, String hrStatus) { }
record ConfirmStatusRequest(String confirmedStatus) { }
class EmployeeNotFoundException extends RuntimeException {
    EmployeeNotFoundException(Long id) { super("Employee not found: " + id); }
}

@Service
class EmployeeService {
    private final EmployeeMapper employeeMapper;
    private final HrApiClient hrApiClient;
    EmployeeService(EmployeeMapper employeeMapper, HrApiClient hrApiClient) {
        this.employeeMapper = employeeMapper;
        this.hrApiClient = hrApiClient;
    }
    EmployeeHrView findWithHrStatus(Long id, String requestId) {
        Employee employee = findLocal(id);
        HrStatusResponse hr = hrApiClient.findStatus(id, requestId);
        return new EmployeeHrView(employee.id(), employee.name(), employee.department(), hr.getStatus());
    }
    void confirmStatus(Long id, String status, String requestId) {
        findLocal(id);
        if (status == null || status.isBlank()) throw new IllegalArgumentException("confirmedStatus is required");
        hrApiClient.confirmStatus(new HrStatusConfirmationRequest(id, status), requestId);
    }
    private Employee findLocal(Long id) {
        Employee employee = employeeMapper.findById(id);
        if (employee == null) throw new EmployeeNotFoundException(id);
        return employee;
    }
}

@RestController
@RequestMapping("/lab/employees")
class EmployeeController {
    private final EmployeeService service;
    EmployeeController(EmployeeService service) { this.service = service; }
    @GetMapping("/{id}/hr-status")
    EmployeeHrView status(@PathVariable Long id) { return service.findWithHrStatus(id, requestId()); }
    @PostMapping("/{id}/status-confirmations")
    ResponseEntity<Void> confirm(@PathVariable Long id, @RequestBody ConfirmStatusRequest request) {
        service.confirmStatus(id, request.confirmedStatus(), requestId());
        return ResponseEntity.noContent().build();
    }
    private String requestId() {
        String value = MDC.get("requestId");
        return value == null ? UUID.randomUUID().toString() : value;
    }
}

@RestControllerAdvice(assignableTypes = EmployeeController.class)
class HrApiAdvice {
    record ErrorBody(String code, String message, String requestId) { }
    @ExceptionHandler({HrApiClientException.class, HrApiServerException.class, HrApiProtocolException.class})
    ResponseEntity<ErrorBody> badGateway(RuntimeException exception) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(new ErrorBody("HR_DEPENDENCY_ERROR", "HR status unavailable", MDC.get("requestId")));
    }
    @ExceptionHandler(HrApiConnectionException.class)
    ResponseEntity<ErrorBody> unavailable(HrApiConnectionException exception) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new ErrorBody("HR_CONNECTION_ERROR", "HR status unavailable", MDC.get("requestId")));
    }
    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<ErrorBody> invalidInput(IllegalArgumentException exception) {
        return ResponseEntity.badRequest()
                .body(new ErrorBody("INVALID_STATUS", "confirmedStatus is required", MDC.get("requestId")));
    }
    @ExceptionHandler(EmployeeNotFoundException.class)
    ResponseEntity<ErrorBody> notFound(EmployeeNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorBody("EMPLOYEE_NOT_FOUND", "Employee not found", MDC.get("requestId")));
    }
}
