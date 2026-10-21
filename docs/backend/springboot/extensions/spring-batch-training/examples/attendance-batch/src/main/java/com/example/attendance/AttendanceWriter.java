package com.example.attendance;

import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.stereotype.Component;

@Component
public class AttendanceWriter implements ItemWriter<DailySummary> {
    private final AttendanceMapper mapper;

    public AttendanceWriter(AttendanceMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void write(Chunk<? extends DailySummary> chunk) {
        for (DailySummary summary : chunk) {
            mapper.upsertSummary(summary);
        }
    }
}
