package com.example.attendance;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(properties = {
        "app.schedule.enabled=true",
        "app.schedule.cron=*/1 * * * * *",
        "spring.datasource.url=jdbc:h2:mem:attendance_schedule_test;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "app.batch.output-dir=./target/schedule-output"
})
@ActiveProfiles("test")
class ScheduledLaunchIntegrationTest {
    @Autowired JdbcTemplate jdbc;

    @Test
    void schedulerCreatesJobExecution() throws InterruptedException {
        long deadline = System.currentTimeMillis() + 6000;
        long count = 0;
        while (System.currentTimeMillis() < deadline) {
            count = jdbc.queryForObject("SELECT COUNT(*) FROM BATCH_JOB_EXECUTION", Long.class);
            if (count > 0) {
                break;
            }
            Thread.sleep(200);
        }
        assertTrue(count > 0, "the scheduled trigger did not create a JobExecution");
    }
}
