package com.example.hrlab.external;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.time.Duration;
import com.sun.net.httpserver.HttpServer;
import org.springframework.http.client.JdkClientHttpRequestFactory;
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
    void realDelayedResponseTriggersReadTimeoutAndPreservesCause() throws Exception {
        HttpServer delayed = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        delayed.createContext("/api/employees/1001/status", exchange -> {
            try {
                Thread.sleep(600);
                byte[] body = "{\"employeeId\":1001,\"status\":\"ACTIVE\"}".getBytes();
                exchange.sendResponseHeaders(200, body.length);
                exchange.getResponseBody().write(body);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
            } finally {
                exchange.close();
            }
        });
        delayed.start();
        try {
            JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(
                    HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build());
            factory.setReadTimeout(Duration.ofMillis(100));
            HrApiClient realClient = new HrApiClient(RestClient.builder()
                    .baseUrl("http://127.0.0.1:" + delayed.getAddress().getPort())
                    .requestFactory(factory).build());
            HrApiConnectionException error = assertThrows(HrApiConnectionException.class,
                    () -> realClient.findStatus(1001L, "timeout-1"));
            assertNotNull(error.getCause());
            assertTrue(error.getCause() instanceof org.springframework.web.client.ResourceAccessException);
            assertTrue(error.getCause().getCause() instanceof java.net.http.HttpTimeoutException,
                    () -> "actual cause: " + error.getCause().getCause());
        } finally {
            delayed.stop(0);
        }
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
