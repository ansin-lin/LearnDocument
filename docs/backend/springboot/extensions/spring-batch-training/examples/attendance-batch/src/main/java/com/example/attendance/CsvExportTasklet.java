package com.example.attendance;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.batch.core.step.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class CsvExportTasklet implements Tasklet {
    private final AttendanceMapper mapper;
    private final Path outputDirectory;

    public CsvExportTasklet(AttendanceMapper mapper,
                            @Value("${app.batch.output-dir:./output}") String outputDirectory) {
        this.mapper = mapper;
        this.outputDirectory = Path.of(outputDirectory).toAbsolutePath().normalize();
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext context) throws Exception {
        LocalDate date = LocalDate.parse((String) context.getStepContext()
                .getJobParameters().get("businessDate"));
        Files.createDirectories(outputDirectory);
        Path target = outputDirectory.resolve("attendance-" + date + ".csv");
        Path temporary = Files.createTempFile(outputDirectory, "attendance-" + date + "-", ".tmp");
        try {
            List<String> lines = new ArrayList<>();
            lines.add("employee_id,business_date,worked_minutes,overtime_minutes");
            for (DailySummary row : mapper.findSummaries(date)) {
                lines.add(row.employeeId() + "," + row.businessDate() + ","
                        + row.workedMinutes() + "," + row.overtimeMinutes());
            }
            Files.write(temporary, lines, StandardCharsets.UTF_8);
            Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
            return RepeatStatus.FINISHED;
        } finally {
            Files.deleteIfExists(temporary);
        }
    }
}
