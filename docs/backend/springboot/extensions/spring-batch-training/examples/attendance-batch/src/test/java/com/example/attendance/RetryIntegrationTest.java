package com.example.attendance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.TransientDataAccessResourceException;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(properties = {
        "app.batch.skip-invalid=true",
        "spring.datasource.url=jdbc:h2:mem:attendance_retry_test;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "app.batch.output-dir=./target/retry-output"
})
@ActiveProfiles("test")
@Import(RetryIntegrationTest.RetryConfig.class)
class RetryIntegrationTest {
    @Autowired JobLauncher launcher;
    @Autowired Job attendanceSummaryJob;
    @Autowired AttendanceMapper mapper;
    @Autowired AtomicInteger writerAttempts;

    @Test
    void retriesOneTransientWriteWithoutSkippingData() throws Exception {
        LocalDate date = LocalDate.of(2026, 10, 19);
        JobExecution execution = launcher.run(attendanceSummaryJob,
                new JobParametersBuilder().addString("businessDate", date.toString(), true)
                        .toJobParameters());
        assertEquals(BatchStatus.COMPLETED, execution.getStatus());
        assertEquals(25, mapper.countSummaries(date));
        var step = execution.getStepExecutions().stream()
                .filter(s -> s.getStepName().equals("summarizeStep"))
                .findFirst().orElseThrow();
        assertEquals(0, step.getProcessSkipCount());
        assertEquals(BatchStatus.COMPLETED, step.getStatus());
        assertEquals(25, step.getWriteCount());
        assertEquals(25L, step.getExecutionContext().getLong("attendanceReader.lastId", -1));
        assertTrue(writerAttempts.get() >= 2);
    }

    static class RetryConfig {
        @Bean
        AtomicInteger writerAttempts() {
            return new AtomicInteger();
        }

        @Bean
        @Primary
        AttendanceWriter failOnceWriter(AttendanceMapper mapper, AtomicInteger writerAttempts) {
            return new AttendanceWriter(mapper) {
                @Override
                public void write(Chunk<? extends DailySummary> chunk) {
                    if (writerAttempts.incrementAndGet() == 1) {
                        throw new TransientDataAccessResourceException("simulated one-time outage");
                    }
                    super.write(chunk);
                }
            };
        }
    }
}
