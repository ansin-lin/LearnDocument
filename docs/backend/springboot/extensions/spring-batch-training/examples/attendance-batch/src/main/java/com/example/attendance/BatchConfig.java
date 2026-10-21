package com.example.attendance;

import java.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.ChunkOrientedStepBuilder;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class BatchConfig {
    private static final Logger log = LoggerFactory.getLogger(BatchConfig.class);

    @Bean
    Job attendanceSummaryJob(JobRepository repository,
                             @Qualifier("validateStep") Step validateStep,
                             @Qualifier("summarizeStep") Step summarizeStep,
                             @Qualifier("csvStep") Step csvStep,
                             @Qualifier("resultStep") Step resultStep) {
        return new JobBuilder("attendanceSummaryJob", repository)
                .start(validateStep).next(summarizeStep).next(csvStep).next(resultStep)
                .build();
    }

    @Bean
    Step validateStep(JobRepository repository, PlatformTransactionManager transactionManager,
                      AttendanceMapper mapper) {
        Tasklet tasklet = (contribution, context) -> {
            String raw = (String) context.getStepContext().getJobParameters().get("businessDate");
            if (raw == null) {
                throw new IllegalArgumentException("businessDate is required");
            }
            LocalDate date = LocalDate.parse(raw);
            log.info("validate businessDate={} sourceCount={}", date, mapper.countRecords(date));
            return org.springframework.batch.infrastructure.repeat.RepeatStatus.FINISHED;
        };
        return new StepBuilder("validateStep", repository)
                .tasklet(tasklet, transactionManager).build();
    }

    @Bean
    @StepScope
    AttendanceReader attendanceReader(AttendanceMapper mapper,
                                      @Value("#{jobParameters['businessDate']}") String rawDate) {
        return new AttendanceReader(mapper, LocalDate.parse(rawDate));
    }

    @Bean
    Step summarizeStep(JobRepository repository, PlatformTransactionManager transactionManager,
                       AttendanceReader reader, AttendanceProcessor processor,
                       AttendanceWriter writer,
                       @Value("${app.batch.skip-invalid:false}") boolean skipInvalid) {
        ChunkOrientedStepBuilder<AttendanceRecord, DailySummary> builder =
                new StepBuilder("summarizeStep", repository)
                        .<AttendanceRecord, DailySummary>chunk(10)
                        .reader(reader).processor(processor).writer(writer)
                        .transactionManager(transactionManager);
        if (skipInvalid) {
            builder.faultTolerant()
                    .skip(InvalidAttendanceException.class).skipLimit(2)
                    .retry(TransientDataAccessException.class).retryLimit(3);
        }
        return builder.build();
    }

    @Bean
    Step csvStep(JobRepository repository, PlatformTransactionManager transactionManager,
                 CsvExportTasklet csvExportTasklet) {
        return new StepBuilder("csvStep", repository)
                .tasklet(csvExportTasklet, transactionManager).build();
    }

    @Bean
    Step resultStep(JobRepository repository, PlatformTransactionManager transactionManager,
                    AttendanceMapper mapper) {
        Tasklet tasklet = (contribution, context) -> {
            LocalDate date = LocalDate.parse((String) context.getStepContext()
                    .getJobParameters().get("businessDate"));
            log.info("result businessDate={} summaryCount={}", date, mapper.countSummaries(date));
            return org.springframework.batch.infrastructure.repeat.RepeatStatus.FINISHED;
        };
        return new StepBuilder("resultStep", repository)
                .tasklet(tasklet, transactionManager).build();
    }
}
