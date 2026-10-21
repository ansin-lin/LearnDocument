package com.example.attendance;

import java.time.LocalDate;
import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.batch.infrastructure.item.ItemStreamReader;

public class AttendanceReader implements ItemStreamReader<AttendanceRecord> {
    private static final String LAST_ID = "attendanceReader.lastId";
    private final AttendanceMapper mapper;
    private final LocalDate businessDate;
    private long lastId;

    public AttendanceReader(AttendanceMapper mapper, LocalDate businessDate) {
        this.mapper = mapper;
        this.businessDate = businessDate;
    }

    @Override
    public void open(ExecutionContext executionContext) {
        lastId = executionContext.getLong(LAST_ID, 0);
    }

    @Override
    public AttendanceRecord read() {
        AttendanceRecord item = mapper.findNext(businessDate, lastId);
        if (item != null) {
            lastId = item.id();
        }
        return item;
    }

    @Override
    public void update(ExecutionContext executionContext) {
        executionContext.putLong(LAST_ID, lastId);
    }

    @Override
    public void close() {
        // No open cursor: each read is a keyset query.
    }
}
