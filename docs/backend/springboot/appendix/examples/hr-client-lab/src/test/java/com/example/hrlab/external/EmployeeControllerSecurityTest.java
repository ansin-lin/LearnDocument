package com.example.hrlab.external;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class EmployeeControllerSecurityTest {
    @Autowired MockMvc mvc;
    @MockitoBean EmployeeService service;

    @Test void authorizedGetReturnsJson() throws Exception {
        when(service.findWithHrStatus(eq(1001L), anyString()))
                .thenReturn(new EmployeeHrView(1001L, "Aki", "IT", "ACTIVE"));
        mvc.perform(get("/lab/employees/1001/hr-status").with(user("reader").roles("HR_READER")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.hrStatus").value("ACTIVE"));
    }
    @Test void unauthenticatedGetIs401() throws Exception {
        mvc.perform(get("/lab/employees/1001/hr-status")).andExpect(status().isUnauthorized());
        verifyNoInteractions(service);
    }
    @Test void unauthorizedGetIs403() throws Exception {
        mvc.perform(get("/lab/employees/1001/hr-status").with(user("other").roles("USER")))
                .andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }
    @Test void dependency404MapsTo502NotLocal404() throws Exception {
        when(service.findWithHrStatus(eq(1001L), anyString()))
                .thenThrow(new HrApiClientException("HR 404"));
        mvc.perform(get("/lab/employees/1001/hr-status").with(user("reader").roles("HR_READER")))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.code").value("HR_DEPENDENCY_ERROR"));
    }
    @Test void noCsrfIs403EvenWithRole() throws Exception {
        mvc.perform(post("/lab/employees/1001/status-confirmations")
                        .with(user("reader").roles("HR_READER"))
                        .contentType("application/json").content("{\"confirmedStatus\":\"ACTIVE\"}"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }
    @Test void validCsrfAndRoleReturns204() throws Exception {
        mvc.perform(post("/lab/employees/1001/status-confirmations")
                        .with(user("reader").roles("HR_READER")).with(csrf())
                        .contentType("application/json").content("{\"confirmedStatus\":\"ACTIVE\"}"))
                .andExpect(status().isNoContent());
        verify(service).confirmStatus(eq(1001L), eq("ACTIVE"), anyString());
    }
    @Test void csrfDoesNotReplaceAuthorization() throws Exception {
        mvc.perform(post("/lab/employees/1001/status-confirmations")
                        .with(user("other").roles("USER")).with(csrf())
                        .contentType("application/json").content("{\"confirmedStatus\":\"ACTIVE\"}"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }
}
