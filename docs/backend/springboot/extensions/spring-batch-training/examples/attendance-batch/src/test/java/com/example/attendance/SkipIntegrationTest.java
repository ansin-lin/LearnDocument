package com.example.attendance;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(properties = {
        "app.batch.skip-invalid=true",
        "spring.datasource.url=jdbc:h2:mem:attendance_skip_test;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "app.batch.output-dir=./target/skip-output"
})
@ActiveProfiles("test")
class SkipIntegrationTest {
    @Autowired JobLauncher launcher;
    @Autowired Job attendanceSummaryJob;
    @Autowired AttendanceMapper mapper;
    @Autowired JdbcTemplate jdbc;

    @Test
    void skipsOnlyTheAllowedInvalidRecord() throws Exception {
        LocalDate date = LocalDate.of(2026, 10, 19);
        jdbc.update("UPDATE attendance_records SET break_minutes=600 WHERE id=13");
        JobExecution execution = launcher.run(attendanceSummaryJob,
                new JobParametersBuilder().addString("businessDate", date.toString(), true)
                        .toJobParameters());
        assertEquals(BatchStatus.COMPLETED, execution.getStatus());
        assertEquals(24, mapper.countSummaries(date));
        var step = execution.getStepExecutions().stream()
                .filter(s -> s.getStepName().equals("summarizeStep"))
                .findFirst().orElseThrow();
        assertEquals(1, step.getProcessSkipCount());
        assertEquals(BatchStatus.COMPLETED, step.getStatus());
        assertEquals(25, step.getReadCount());
        assertEquals(24, step.getWriteCount());
        assertEquals(25L, step.getExecutionContext().getLong("attendanceReader.lastId", -1));
    }
}
