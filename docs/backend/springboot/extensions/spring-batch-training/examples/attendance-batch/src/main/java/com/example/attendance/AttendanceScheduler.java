package com.example.attendance;

import java.time.LocalDate;
import java.time.ZoneId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.launch.JobInstanceAlreadyCompleteException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
@ConditionalOnProperty(prefix = "app.schedule", name = "enabled", havingValue = "true")
public class AttendanceScheduler {
    private static final Logger log = LoggerFactory.getLogger(AttendanceScheduler.class);
    private final BatchTrigger trigger;

    public AttendanceScheduler(BatchTrigger trigger) {
        this.trigger = trigger;
    }

    @Scheduled(cron = "${app.schedule.cron:0 0 2 * * *}", zone = "Asia/Tokyo")
    public void launchYesterday() {
        LocalDate businessDate = LocalDate.now(ZoneId.of("Asia/Tokyo")).minusDays(1);
        try {
            trigger.launch(businessDate);
        } catch (JobInstanceAlreadyCompleteException ex) {
            log.info("scheduled batch already completed for businessDate={}", businessDate);
        } catch (Exception ex) {
            log.error("scheduled batch failed for businessDate={}", businessDate, ex);
        }
    }
}
