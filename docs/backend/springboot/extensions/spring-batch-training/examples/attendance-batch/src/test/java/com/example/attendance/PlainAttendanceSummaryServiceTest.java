package com.example.attendance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class PlainAttendanceSummaryServiceTest {
    @Test
    void summarizesOneRecord() {
        AttendanceMapper mapper = mock(AttendanceMapper.class);
        LocalDate date = LocalDate.of(2026, 10, 19);
        when(mapper.findNext(date, 0)).thenReturn(new AttendanceRecord(
                1, 1001, date, date.atTime(9, 0), date.atTime(18, 0), 60));
        when(mapper.findNext(date, 1)).thenReturn(null);

        PlainAttendanceSummaryService.Result result =
                new PlainAttendanceSummaryService(mapper, new AttendanceProcessor())
                        .summarize(date);

        assertEquals(1, result.processed());
        verify(mapper).upsertSummary(any(DailySummary.class));
    }
}
