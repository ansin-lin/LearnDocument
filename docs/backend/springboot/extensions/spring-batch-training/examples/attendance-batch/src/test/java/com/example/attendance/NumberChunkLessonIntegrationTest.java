package com.example.attendance;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
        "app.lesson.chunk-enabled=true",
        "spring.datasource.url=jdbc:h2:mem:number_chunk_lesson_test;MODE=MySQL;DB_CLOSE_DELAY=-1"
})
@ActiveProfiles("test")
class NumberChunkLessonIntegrationTest {
    @Autowired JobLauncher launcher;
    @Autowired @Qualifier("numberChunkJob") Job numberChunkJob;
    @Autowired @Qualifier("numberChunkFiveJob") Job numberChunkFiveJob;
    @Autowired @Qualifier("numberFilterJob") Job numberFilterJob;
    @Autowired NumberChunkLessonConfig.NumberChunkRecorder recorder;
    @Autowired JdbcTemplate jdbc;

    @Test
    void observesGroupsEndOfInputAndFilteringWithoutBusinessWrites() throws Exception {
        JobExecution sizeTwo = launcher.run(numberChunkJob, new JobParameters());
        assertEquals(BatchStatus.COMPLETED, sizeTwo.getStatus());
        assertEquals(List.of(List.of(10, 20), List.of(30, 40), List.of(50)),
                recorder.groups("numberChunkStep"));
        assertEquals(5, sizeTwo.getStepExecutions().iterator().next().getReadCount());
        assertEquals(5, sizeTwo.getStepExecutions().iterator().next().getWriteCount());

        JobExecution independentRun = launcher.run(numberChunkJob,
                new JobParametersBuilder().addString("lessonRun", "second", true).toJobParameters());
        assertEquals(BatchStatus.COMPLETED, independentRun.getStatus());
        assertEquals(5, independentRun.getStepExecutions().iterator().next().getReadCount());
        assertEquals(5, independentRun.getStepExecutions().iterator().next().getWriteCount());
        assertEquals(List.of(List.of(10, 20), List.of(30, 40), List.of(50),
                List.of(10, 20), List.of(30, 40), List.of(50)), recorder.groups("numberChunkStep"));

        JobExecution sizeFive = launcher.run(numberChunkFiveJob, new JobParameters());
        assertEquals(BatchStatus.COMPLETED, sizeFive.getStatus());
        assertEquals(List.of(List.of(10, 20, 30, 40, 50)),
                recorder.groups("numberChunkFiveStep"));

        JobExecution filtered = launcher.run(numberFilterJob, new JobParameters());
        assertEquals(BatchStatus.COMPLETED, filtered.getStatus());
        assertEquals(List.of(List.of(10, 20), List.of(40), List.of(50)),
                recorder.groups("numberFilterStep"));
        assertEquals(5, filtered.getStepExecutions().iterator().next().getReadCount());
        assertEquals(4, filtered.getStepExecutions().iterator().next().getWriteCount());
        assertEquals(0L, jdbc.queryForObject(
                "SELECT COUNT(*) FROM attendance_daily_summary", Long.class));
    }
}
