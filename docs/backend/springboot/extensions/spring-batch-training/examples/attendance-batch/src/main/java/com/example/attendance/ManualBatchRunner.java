package com.example.attendance;

import java.time.LocalDate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.batch", name = "business-date")
public class ManualBatchRunner implements ApplicationRunner {
    private final BatchTrigger trigger;
    private final String businessDate;

    public ManualBatchRunner(BatchTrigger trigger,
                             @Value("${app.batch.business-date}") String businessDate) {
        this.trigger = trigger;
        this.businessDate = businessDate;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        trigger.launch(LocalDate.parse(businessDate));
    }
}
