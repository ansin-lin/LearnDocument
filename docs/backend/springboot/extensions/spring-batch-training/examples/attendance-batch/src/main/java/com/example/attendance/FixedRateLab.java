package com.example.attendance;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
@ConditionalOnProperty(prefix = "app.schedule", name = "lab-mode", havingValue = "rate")
public class FixedRateLab {
    private static final Logger log = LoggerFactory.getLogger(FixedRateLab.class);

    @Scheduled(fixedRate = 2000, initialDelay = 1000)
    public void tick() {
        log.info("fixedRate start thread={}", Thread.currentThread().getName());
        try {
            Thread.sleep(3000);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return;
        }
        log.info("fixedRate end");
    }
}
