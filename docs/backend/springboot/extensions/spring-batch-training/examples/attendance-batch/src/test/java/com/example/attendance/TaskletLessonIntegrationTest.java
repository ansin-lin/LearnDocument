package com.example.attendance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(properties = {
        "app.lesson.enabled=true",
        "spring.datasource.url=jdbc:h2:mem:attendance_tasklet_lesson_test;MODE=MySQL;DB_CLOSE_DELAY=-1"
})
@ActiveProfiles("test")
class TaskletLessonIntegrationTest {
    @Autowired JobLauncher launcher;
    @Autowired @Qualifier("simpleJob") Job simpleJob;
    @Autowired @Qualifier("attendanceTaskletJob") Job attendanceTaskletJob;
    @Autowired JdbcTemplate jdbc;

    @Test
    void runsOneStepThenTwoTaskletStepsAndRecordsFailure() throws Exception {
        JobExecution hello = launcher.run(simpleJob, new JobParameters());
        assertEquals(BatchStatus.COMPLETED, hello.getStatus());
        assertEquals(List.of("helloStep"), hello.getStepExecutions().stream()
                .map(step -> step.getStepName()).toList());

        JobParameters date = new JobParametersBuilder()
                .addString("businessDate", "2026-10-19", true).toJobParameters();
        JobExecution twoSteps = launcher.run(attendanceTaskletJob, date);
        assertEquals(BatchStatus.COMPLETED, twoSteps.getStatus());
        assertEquals(List.of("lessonValidateStep", "lessonResultStep"),
                twoSteps.getStepExecutions().stream().map(step -> step.getStepName()).toList());
        assertEquals(0L, jdbc.queryForObject(
                "SELECT COUNT(*) FROM attendance_daily_summary", Long.class));

        JobExecution failed = launcher.run(attendanceTaskletJob, new JobParameters());
        assertEquals(BatchStatus.FAILED, failed.getStatus());
        assertTrue(failed.getStepExecutions().stream()
                .anyMatch(step -> step.getStepName().equals("lessonValidateStep")
                        && step.getStatus() == BatchStatus.FAILED));
        assertEquals(3L, jdbc.queryForObject(
                "SELECT COUNT(*) FROM BATCH_JOB_EXECUTION", Long.class));
    }
}
