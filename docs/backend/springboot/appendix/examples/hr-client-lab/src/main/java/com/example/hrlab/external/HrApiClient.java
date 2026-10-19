package com.example.hrlab.external;

import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/** B05 的独立实验组件，不属于 Employee 主线。 */
@Component
public class HrApiClient {
    private final RestClient restClient;

    public HrApiClient(RestClient restClient) {
        this.restClient = restClient;
    }

    public HrStatusResponse findStatus(Long employeeId, String requestId) {
        try {
            HrStatusResponse response = restClient.get()
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
            throw new HrApiConnectionException("Could not connect to HR API", exception);
        } catch (RestClientException exception) {
            throw new HrApiProtocolException("HR API response contract failed", exception);
        }
    }

    public void confirmStatus(HrStatusConfirmationRequest body, String requestId) {
        try {
            restClient.post()
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
            throw new HrApiConnectionException("Could not connect to HR API", exception);
        } catch (RestClientException exception) {
            throw new HrApiProtocolException("HR API response contract failed", exception);
        }
    }
}

class HrStatusResponse {
    private Long employeeId;
    private String status;
    public HrStatusResponse() { }
    public Long getEmployeeId() { return employeeId; }
    public void setEmployeeId(Long employeeId) { this.employeeId = employeeId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}

class HrStatusConfirmationRequest {
    private final Long employeeId;
    private final String confirmedStatus;
    HrStatusConfirmationRequest(Long employeeId, String confirmedStatus) {
        this.employeeId = employeeId;
        this.confirmedStatus = confirmedStatus;
    }
    public Long getEmployeeId() { return employeeId; }
    public String getConfirmedStatus() { return confirmedStatus; }
}

class HrApiClientException extends RuntimeException {
    HrApiClientException(String message) { super(message); }
}

class HrApiServerException extends RuntimeException {
    HrApiServerException(String message) { super(message); }
}

class HrApiConnectionException extends RuntimeException {
    HrApiConnectionException(String message, Throwable cause) { super(message, cause); }
}

class HrApiProtocolException extends RuntimeException {
    HrApiProtocolException(String message) { super(message); }
    HrApiProtocolException(String message, Throwable cause) { super(message, cause); }
}
