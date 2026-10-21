package com.example.attendance;

import java.time.LocalDate;

public record DailySummary(long employeeId, LocalDate businessDate,
                           long workedMinutes, long overtimeMinutes) {
}
