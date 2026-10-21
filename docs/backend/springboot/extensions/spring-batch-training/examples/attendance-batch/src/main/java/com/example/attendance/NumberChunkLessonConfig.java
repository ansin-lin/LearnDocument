package com.example.attendance;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.batch.infrastructure.item.ItemReader;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

/** Chapter 03 only: in-memory Chunk examples, never part of attendanceSummaryJob. */
@Configuration
@ConditionalOnProperty(prefix = "app.lesson", name = "chunk-enabled", havingValue = "true")
public class NumberChunkLessonConfig {
    @Bean
    NumberChunkRecorder numberChunkRecorder() {
        return new NumberChunkRecorder();
    }

    @Bean
    Job numberChunkJob(JobRepository repository, @Qualifier("numberChunkStep") Step step) {
        return new JobBuilder("numberChunkJob", repository).start(step).build();
    }

    @Bean
    Job numberChunkFiveJob(JobRepository repository, @Qualifier("numberChunkFiveStep") Step step) {
        return new JobBuilder("numberChunkFiveJob", repository).start(step).build();
    }

    @Bean
    Job numberFilterJob(JobRepository repository, @Qualifier("numberFilterStep") Step step) {
        return new JobBuilder("numberFilterJob", repository).start(step).build();
    }

    @Bean
    Step numberChunkStep(JobRepository repository, PlatformTransactionManager transactionManager,
                         NumberChunkRecorder recorder, @Qualifier("numberChunkReader") ItemReader<Integer> reader) {
        return numberStep("numberChunkStep", 2, false, repository, transactionManager, recorder, reader);
    }

    @Bean
    Step numberChunkFiveStep(JobRepository repository, PlatformTransactionManager transactionManager,
                             NumberChunkRecorder recorder, @Qualifier("numberChunkFiveReader") ItemReader<Integer> reader) {
        return numberStep("numberChunkFiveStep", 5, false, repository, transactionManager, recorder, reader);
    }

    @Bean
    Step numberFilterStep(JobRepository repository, PlatformTransactionManager transactionManager,
                          NumberChunkRecorder recorder, @Qualifier("numberFilterReader") ItemReader<Integer> reader) {
        return numberStep("numberFilterStep", 2, true, repository, transactionManager, recorder, reader);
    }

    @Bean
    @StepScope
    ItemReader<Integer> numberChunkReader() {
        return newNumberReader();
    }

    @Bean
    @StepScope
    ItemReader<Integer> numberChunkFiveReader() {
        return newNumberReader();
    }

    @Bean
    @StepScope
    ItemReader<Integer> numberFilterReader() {
        return newNumberReader();
    }

    private ItemReader<Integer> newNumberReader() {
        AtomicInteger next = new AtomicInteger(1);
        return () -> {
            int value = next.getAndIncrement();
            return value <= 5 ? value : null;
        };
    }

    private Step numberStep(String name, int chunkSize, boolean filterThree,
                            JobRepository repository, PlatformTransactionManager transactionManager,
                            NumberChunkRecorder recorder, ItemReader<Integer> reader) {
        ItemProcessor<Integer, Integer> processor =
                value -> filterThree && value == 3 ? null : value * 10;
        ItemWriter<Integer> writer = chunk -> recorder.record(name, chunk.getItems());
        return new StepBuilder(name, repository)
                .<Integer, Integer>chunk(chunkSize)
                .reader(reader).processor(processor).writer(writer)
                .transactionManager(transactionManager).build();
    }

    public static class NumberChunkRecorder {
        private final Map<String, List<List<Integer>>> groups = new ConcurrentHashMap<>();

        void record(String step, List<? extends Integer> values) {
            groups.computeIfAbsent(step, ignored -> new ArrayList<>())
                    .add(List.copyOf(values));
        }

        public List<List<Integer>> groups(String step) {
            return List.copyOf(groups.getOrDefault(step, List.of()));
        }
    }
}
