package com.example.attendance;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record AttendanceRecord(long id, long employeeId, LocalDate businessDate,
                               LocalDateTime startAt, LocalDateTime endAt, int breakMinutes) {
}
