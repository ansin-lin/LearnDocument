package com.example.attendance;

import java.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

/** Chapter 02 only: small jobs for learning Tasklet before the full four-step job. */
@Configuration
@ConditionalOnProperty(prefix = "app.lesson", name = "enabled", havingValue = "true")
public class TaskletLessonConfig {
    private static final Logger log = LoggerFactory.getLogger(TaskletLessonConfig.class);

    @Bean
    Job simpleJob(JobRepository repository, @Qualifier("helloStep") Step helloStep) {
        return new JobBuilder("simpleJob", repository).start(helloStep).build();
    }

    @Bean
    Step helloStep(JobRepository repository, PlatformTransactionManager transactionManager) {
        Tasklet tasklet = (contribution, context) -> {
            log.info("helloStep executed");
            return RepeatStatus.FINISHED;
        };
        return new StepBuilder("helloStep", repository)
                .tasklet(tasklet, transactionManager).build();
    }

    @Bean
    Job attendanceTaskletJob(JobRepository repository,
                             @Qualifier("lessonValidateStep") Step validateStep,
                             @Qualifier("lessonResultStep") Step resultStep) {
        return new JobBuilder("attendanceTaskletJob", repository)
                .start(validateStep).next(resultStep).build();
    }

    @Bean
    Step lessonValidateStep(JobRepository repository,
                            PlatformTransactionManager transactionManager,
                            AttendanceMapper mapper) {
        Tasklet tasklet = (contribution, context) -> {
            String raw = (String) context.getStepContext()
                    .getJobParameters().get("businessDate");
            if (raw == null) {
                throw new IllegalArgumentException("businessDate is required");
            }
            LocalDate date = LocalDate.parse(raw);
            log.info("lessonValidateStep businessDate={} sourceCount={}",
                    date, mapper.countRecords(date));
            return RepeatStatus.FINISHED;
        };
        return new StepBuilder("lessonValidateStep", repository)
                .tasklet(tasklet, transactionManager).build();
    }

    @Bean
    Step lessonResultStep(JobRepository repository,
                          PlatformTransactionManager transactionManager,
                          AttendanceMapper mapper) {
        Tasklet tasklet = (contribution, context) -> {
            LocalDate date = LocalDate.parse((String) context.getStepContext()
                    .getJobParameters().get("businessDate"));
            log.info("lessonResultStep businessDate={} summaryCount={}",
                    date, mapper.countSummaries(date));
            return RepeatStatus.FINISHED;
        };
        return new StepBuilder("lessonResultStep", repository)
                .tasklet(tasklet, transactionManager).build();
    }
}
