package com.example.attendance;

import java.time.temporal.ChronoUnit;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.stereotype.Component;

@Component
public class AttendanceProcessor implements ItemProcessor<AttendanceRecord, DailySummary> {
    @Override
    public DailySummary process(AttendanceRecord item) {
        long minutes = ChronoUnit.MINUTES.between(item.startAt(), item.endAt());
        if (minutes <= 0 || item.breakMinutes() < 0 || item.breakMinutes() >= minutes) {
            throw new InvalidAttendanceException(item.id(), "invalid time or break");
        }
        long worked = minutes - item.breakMinutes();
        return new DailySummary(item.employeeId(), item.businessDate(),
                worked, Math.max(0, worked - 480));
    }
}
