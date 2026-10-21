package com.example.attendance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class AttendanceProcessorTest {
    private final AttendanceProcessor processor = new AttendanceProcessor();

    @Test
    void calculatesWorkedAndOvertimeMinutes() {
        LocalDate date = LocalDate.of(2026, 10, 19);
        DailySummary result = processor.process(new AttendanceRecord(
                1, 1001, date, date.atTime(9, 0), date.atTime(19, 0), 60));
        assertEquals(540, result.workedMinutes());
        assertEquals(60, result.overtimeMinutes());
    }

    @Test
    void rejectsImpossibleBreak() {
        LocalDate date = LocalDate.of(2026, 10, 19);
        LocalDateTime start = date.atTime(9, 0);
        assertThrows(InvalidAttendanceException.class, () ->
                processor.process(new AttendanceRecord(13, 1013, date,
                        start, date.atTime(18, 0), 600)));
    }
}
