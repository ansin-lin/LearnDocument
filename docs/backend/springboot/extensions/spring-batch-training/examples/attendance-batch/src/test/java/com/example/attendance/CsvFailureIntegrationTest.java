package com.example.attendance;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:attendance_csv_test;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "app.batch.output-dir=./target/csv-blocker"
})
@ActiveProfiles("test")
class CsvFailureIntegrationTest {
    @Autowired JobLauncher launcher;
    @Autowired Job attendanceSummaryJob;
    @Autowired AttendanceMapper mapper;

    @Test
    void restartsCsvStepAfterOutputPathFailure() throws Exception {
        Path blockedPath = Path.of("target/csv-blocker");
        Files.createDirectories(blockedPath.getParent());
        if (Files.isDirectory(blockedPath)) {
            Files.deleteIfExists(blockedPath.resolve("attendance-2026-10-19.csv"));
            Files.delete(blockedPath);
        }
        Files.writeString(blockedPath, "blocking file");
        LocalDate date = LocalDate.of(2026, 10, 19);
        JobParameters parameters = new JobParametersBuilder()
                .addString("businessDate", date.toString(), true).toJobParameters();
        try {
            JobExecution failed = launcher.run(attendanceSummaryJob, parameters);
            assertEquals(BatchStatus.FAILED, failed.getStatus());
            assertEquals(25, mapper.countSummaries(date));
        } finally {
            Files.deleteIfExists(blockedPath);
        }
        JobExecution restarted = launcher.run(attendanceSummaryJob, parameters);
        assertEquals(BatchStatus.COMPLETED, restarted.getStatus());
        assertTrue(Files.exists(blockedPath.resolve("attendance-2026-10-19.csv")));
    }
}
