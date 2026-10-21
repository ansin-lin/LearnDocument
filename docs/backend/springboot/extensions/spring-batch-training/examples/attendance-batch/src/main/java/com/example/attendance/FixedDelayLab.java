package com.example.attendance;

import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
@ConditionalOnProperty(prefix = "app.schedule", name = "lab-mode", havingValue = "delay")
public class FixedDelayLab {
    private static final Logger log = LoggerFactory.getLogger(FixedDelayLab.class);
    private final boolean failOnce;
    private final AtomicBoolean first = new AtomicBoolean(true);

    public FixedDelayLab(@Value("${app.schedule.lab-fail-once:false}") boolean failOnce) {
        this.failOnce = failOnce;
    }

    @Scheduled(fixedDelay = 2000, initialDelay = 1000)
    public void tick() {
        log.info("fixedDelay start thread={}", Thread.currentThread().getName());
        if (failOnce && first.getAndSet(false)) {
            throw new IllegalStateException("simulated scheduling error");
        }
        try {
            Thread.sleep(3000);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return;
        }
        log.info("fixedDelay end");
    }
}
