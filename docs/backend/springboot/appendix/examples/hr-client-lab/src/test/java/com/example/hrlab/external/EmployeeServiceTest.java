package com.example.hrlab.external;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EmployeeServiceTest {
    private final EmployeeMapper mapper = mock(EmployeeMapper.class);
    private final HrApiClient client = mock(HrApiClient.class);
    private final EmployeeService service = new EmployeeService(mapper, client);

    @Test void localEmployeeAndHrSuccess() {
        when(mapper.findById(1001L)).thenReturn(new Employee(1001L, "Aki", "IT"));
        HrStatusResponse response = new HrStatusResponse();
        response.setEmployeeId(1001L); response.setStatus("ACTIVE");
        when(client.findStatus(1001L, "r1")).thenReturn(response);
        assertEquals(new EmployeeHrView(1001L, "Aki", "IT", "ACTIVE"),
                service.findWithHrStatus(1001L, "r1"));
        verify(mapper, never()).findById(999L);
    }
    @Test void localMissingSkipsHr() {
        assertThrows(EmployeeNotFoundException.class, () -> service.findWithHrStatus(999L, "r2"));
        verifyNoInteractions(client);
    }
    @Test void external404UsesDependencyPolicy() {
        when(mapper.findById(1001L)).thenReturn(new Employee(1001L, "Aki", "IT"));
        when(client.findStatus(1001L, "r3")).thenThrow(new HrApiClientException("HR 404"));
        assertThrows(HrApiClientException.class, () -> service.findWithHrStatus(1001L, "r3"));
        verify(mapper).findById(1001L);
        verifyNoMoreInteractions(mapper);
    }
    @Test void external500PropagatesWithoutLocalWrite() {
        when(mapper.findById(1001L)).thenReturn(new Employee(1001L, "Aki", "IT"));
        when(client.findStatus(1001L, "r4")).thenThrow(new HrApiServerException("HR 500"));
        assertThrows(HrApiServerException.class, () -> service.findWithHrStatus(1001L, "r4"));
        verify(mapper).findById(1001L);
        verifyNoMoreInteractions(mapper);
    }
    @Test void failedConfirmationDoesNotWriteLocalState() {
        when(mapper.findById(1001L)).thenReturn(new Employee(1001L, "Aki", "IT"));
        doThrow(new HrApiServerException("HR 500")).when(client)
                .confirmStatus(any(HrStatusConfirmationRequest.class), eq("r5"));
        assertThrows(HrApiServerException.class, () -> service.confirmStatus(1001L, "ACTIVE", "r5"));
        verify(mapper).findById(1001L);
        verifyNoMoreInteractions(mapper);
    }
}
