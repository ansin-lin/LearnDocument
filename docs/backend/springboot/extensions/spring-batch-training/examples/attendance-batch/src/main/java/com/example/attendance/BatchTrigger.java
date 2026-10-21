package com.example.attendance;

import java.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class BatchTrigger {
    private static final Logger log = LoggerFactory.getLogger(BatchTrigger.class);
    private final JobLauncher launcher;
    private final Job job;

    public BatchTrigger(JobLauncher launcher, @Qualifier("attendanceSummaryJob") Job attendanceSummaryJob) {
        this.launcher = launcher;
        this.job = attendanceSummaryJob;
    }

    public JobExecution launch(LocalDate businessDate) throws Exception {
        JobParameters parameters = new JobParametersBuilder()
                .addString("businessDate", businessDate.toString(), true)
                .toJobParameters();
        JobExecution execution = launcher.run(job, parameters);
        log.info("jobExecutionId={} businessDate={} status={}",
                execution.getId(), businessDate, execution.getStatus());
        if (execution.getStatus() != BatchStatus.COMPLETED) {
            throw new IllegalStateException("batch failed: jobExecutionId=" + execution.getId());
        }
        return execution;
    }
}
