package com.zb.jogakjogak.notification.controller;

import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Hidden
@RestController
@RequestMapping("/biz/batch")
@RequiredArgsConstructor
public class TestController {

    private final JobLauncher jobLauncher;
    private final JobRegistry jobRegistry;

    @PostMapping("/email-paging")
    public Map<String, Object> runEmailBatch() throws Exception {
        JobParameters params = new JobParametersBuilder()
                .addLocalDateTime("batchStartTime", LocalDateTime.now())
                .addLong("run.id", System.currentTimeMillis())
                .toJobParameters();

        JobExecution execution = jobLauncher.run(jobRegistry.getJob("emailNotificationJob"), params);
        long elapsedMs = Duration.between(execution.getStartTime(), execution.getEndTime()).toMillis();

        log.info("[EmailBatch-Paging] status={}, elapsedMs={}ms", execution.getStatus(), elapsedMs);
        return Map.of(
                "method", "JpaPagingItemReader",
                "status", execution.getStatus().toString(),
                "elapsedMs", elapsedMs
        );
    }

    @PostMapping("/email-cursor")
    public Map<String, Object> runEmailCursorBatch() throws Exception {
        JobParameters params = new JobParametersBuilder()
                .addLocalDateTime("batchStartTime", LocalDateTime.now())
                .addLong("run.id", System.currentTimeMillis())
                .toJobParameters();

        JobExecution execution = jobLauncher.run(jobRegistry.getJob("emailNotificationCursorJob"), params);
        long elapsedMs = Duration.between(execution.getStartTime(), execution.getEndTime()).toMillis();

        log.info("[EmailBatch-Cursor] status={}, elapsedMs={}ms", execution.getStatus(), elapsedMs);
        return Map.of(
                "method", "JpaCursorItemReader",
                "status", execution.getStatus().toString(),
                "elapsedMs", elapsedMs
        );
    }

    @PostMapping("/email-jdbc-cursor")
    public Map<String, Object> runEmailJdbcCursorBatch() throws Exception {
        JobParameters params = new JobParametersBuilder()
                .addLocalDateTime("batchStartTime", LocalDateTime.now())
                .addLong("run.id", System.currentTimeMillis())
                .toJobParameters();

        JobExecution execution = jobLauncher.run(jobRegistry.getJob("emailNotificationJdbcCursorJob"), params);
        long elapsedMs = Duration.between(execution.getStartTime(), execution.getEndTime()).toMillis();

        log.info("[EmailBatch-JdbcCursor] status={}, elapsedMs={}ms", execution.getStatus(), elapsedMs);
        return Map.of(
                "method", "JdbcCursorItemReader",
                "status", execution.getStatus().toString(),
                "elapsedMs", elapsedMs
        );
    }

    @PostMapping("/email-jdbc-batch-writer")
    public Map<String, Object> runEmailJdbcBatchWriterBatch() throws Exception {
        JobParameters params = new JobParametersBuilder()
                .addLocalDateTime("batchStartTime", LocalDateTime.now())
                .addLong("run.id", System.currentTimeMillis())
                .toJobParameters();

        JobExecution execution = jobLauncher.run(jobRegistry.getJob("emailNotificationJdbcBatchWriterJob"), params);
        long elapsedMs = Duration.between(execution.getStartTime(), execution.getEndTime()).toMillis();

        log.info("[EmailBatch-JdbcBatchWriter] status={}, elapsedMs={}ms", execution.getStatus(), elapsedMs);
        return Map.of(
                "method", "JdbcBatchItemWriter",
                "status", execution.getStatus().toString(),
                "elapsedMs", elapsedMs
        );
    }

@PostMapping("/email-full-jdbc-batch")
    public Map<String, Object> runEmailFullJdbcBatch() throws Exception {
        JobParameters params = new JobParametersBuilder()
                .addLocalDateTime("batchStartTime", LocalDateTime.now())
                .addLong("run.id", System.currentTimeMillis())
                .toJobParameters();

        JobExecution execution = jobLauncher.run(jobRegistry.getJob("emailNotificationFullJdbcBatchJob"), params);
        long elapsedMs = Duration.between(execution.getStartTime(), execution.getEndTime()).toMillis();

        log.info("[EmailBatch-FullJdbcBatch] status={}, elapsedMs={}ms", execution.getStatus(), elapsedMs);
        return Map.of(
                "method", "FullJdbcBatch (INSERT + UPDATE)",
                "status", execution.getStatus().toString(),
                "elapsedMs", elapsedMs
        );
    }
}
