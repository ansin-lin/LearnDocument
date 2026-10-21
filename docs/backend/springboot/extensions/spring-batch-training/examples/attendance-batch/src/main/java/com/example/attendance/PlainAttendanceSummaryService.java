package com.example.attendance;

import java.time.Instant;
import java.time.LocalDate;
import org.springframework.stereotype.Service;

/** Chapter 01 comparison only; the final job uses Spring Batch instead. */
@Service
public class PlainAttendanceSummaryService {
    private final AttendanceMapper mapper;
    private final AttendanceProcessor processor;

    public PlainAttendanceSummaryService(AttendanceMapper mapper, AttendanceProcessor processor) {
        this.mapper = mapper;
        this.processor = processor;
    }

    public Result summarize(LocalDate businessDate) {
        Instant started = Instant.now();
        long lastId = 0;
        int processed = 0;
        AttendanceRecord record;
        while ((record = mapper.findNext(businessDate, lastId)) != null) {
            mapper.upsertSummary(processor.process(record));
            lastId = record.id();
            processed++;
        }
        return new Result(processed, started, Instant.now());
    }

    public record Result(int processed, Instant started, Instant finished) {
    }
}
