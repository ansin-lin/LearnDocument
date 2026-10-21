package com.example.attendance;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

class AttendanceSchedulerTest {
    @Test
    void triggersYesterdayInTokyo() throws Exception {
        BatchTrigger trigger = mock(BatchTrigger.class);
        new AttendanceScheduler(trigger).launchYesterday();
        verify(trigger).launch(LocalDate.now(ZoneId.of("Asia/Tokyo")).minusDays(1));
    }
}
