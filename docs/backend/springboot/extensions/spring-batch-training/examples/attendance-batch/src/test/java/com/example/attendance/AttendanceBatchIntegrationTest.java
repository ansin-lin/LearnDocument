package com.example.attendance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.JobInstanceAlreadyCompleteException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class AttendanceBatchIntegrationTest {
    @Autowired JobLauncher launcher;
    @Autowired Job attendanceSummaryJob;
    @Autowired AttendanceMapper mapper;
    @Autowired JdbcTemplate jdbc;

    private JobParameters parameters(LocalDate date) {
        return new JobParametersBuilder()
                .addString("businessDate", date.toString(), true).toJobParameters();
    }

    @Test
    void runsChunksRejectsDuplicateAndRestartsAfterFailure() throws Exception {
        LocalDate normalDate = LocalDate.of(2026, 10, 19);
        JobExecution normal = launcher.run(attendanceSummaryJob, parameters(normalDate));
        assertEquals(BatchStatus.COMPLETED, normal.getStatus());
        assertEquals(25, mapper.countSummaries(normalDate));
        assertEquals(1L, jdbc.queryForObject(
                "SELECT COUNT(*) FROM BATCH_JOB_EXECUTION", Long.class));
        assertEquals(25, normal.getStepExecutions().stream()
                .filter(s -> s.getStepName().equals("summarizeStep"))
                .findFirst().orElseThrow().getWriteCount());
        assertEquals(26, Files.readAllLines(
                Path.of("target/test-output/attendance-2026-10-19.csv")).size());
        assertThrows(JobInstanceAlreadyCompleteException.class,
                () -> launcher.run(attendanceSummaryJob, parameters(normalDate)));

        LocalDate failureDate = LocalDate.of(2026, 10, 20);
        jdbc.update("""
                INSERT INTO attendance_records
                    (id, employee_id, business_date, start_at, end_at, break_minutes)
                SELECT id + 100, employee_id, ?, start_at + INTERVAL '1' DAY,
                       end_at + INTERVAL '1' DAY, break_minutes
                FROM attendance_records WHERE business_date = ?
                """, failureDate, normalDate);
        jdbc.update("UPDATE attendance_records SET break_minutes=600 WHERE id=113");
        JobExecution failed = launcher.run(attendanceSummaryJob, parameters(failureDate));
        assertEquals(BatchStatus.FAILED, failed.getStatus());
        assertEquals(10, mapper.countSummaries(failureDate));
        var failedStep = failed.getStepExecutions().stream()
                .filter(s -> s.getStepName().equals("summarizeStep"))
                .findFirst().orElseThrow();
        assertEquals(110L, failedStep.getExecutionContext()
                .getLong("attendanceReader.lastId", -1));

        jdbc.update("UPDATE attendance_records SET break_minutes=60 WHERE id=113");
        JobExecution restarted = launcher.run(attendanceSummaryJob, parameters(failureDate));
        assertEquals(BatchStatus.COMPLETED, restarted.getStatus());
        assertEquals(failed.getJobInstance().getInstanceId(),
                restarted.getJobInstance().getInstanceId());
        assertEquals(25, mapper.countSummaries(failureDate));
        assertTrue(restarted.getStepExecutions().stream()
                .noneMatch(s -> s.getStepName().equals("validateStep")));
        assertTrue(Files.exists(Path.of("target/test-output/attendance-2026-10-20.csv")));

        LocalDate emptyDate = LocalDate.of(2026, 10, 21);
        JobExecution empty = launcher.run(attendanceSummaryJob, parameters(emptyDate));
        assertEquals(BatchStatus.COMPLETED, empty.getStatus());
        assertEquals(0, mapper.countSummaries(emptyDate));
        assertEquals(1, Files.readAllLines(
                Path.of("target/test-output/attendance-2026-10-21.csv")).size());

        JobExecution missing = launcher.run(attendanceSummaryJob, new JobParameters());
        assertEquals(BatchStatus.FAILED, missing.getStatus());
        JobExecution malformed = launcher.run(attendanceSummaryJob,
                new JobParametersBuilder().addString("businessDate", "not-a-date", true)
                        .toJobParameters());
        assertEquals(BatchStatus.FAILED, malformed.getStatus());
    }
}
