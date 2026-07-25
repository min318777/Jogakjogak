package com.zb.jogakjogak.notification.controller;

import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Hidden
@RestController
@RequestMapping("/biz/batch/restart")
@RequiredArgsConstructor
public class BatchRestartController {

    private final JobLauncher jobLauncher;
    private final JobRegistry jobRegistry;

    // 실패한 배치 재실행: batchStartTime은 실패한 실행의 원래 시간 입력
    // 예: /biz/batch/restart/jdbc?batchStartTime=2026-06-07T10:00:00
    // batchStartTime 미입력 시 오늘 기준으로 재실행
    @PostMapping("/jdbc")
    public Map<String, Object> restartJdbcBatch(
            @RequestParam(required = false) String batchStartTime) throws Exception {

        LocalDateTime targetTime = batchStartTime != null
                ? LocalDateTime.parse(batchStartTime)
                : LocalDateTime.now().toLocalDate().atTime(10, 0);

        JobParameters params = new JobParametersBuilder()
                .addString("date", LocalDate.from(targetTime).toString())
                .addString("batchStartTime", targetTime.toString())
                .toJobParameters();

        JobExecution execution = jobLauncher.run(jobRegistry.getJob("sendNotificationTwoStepJdbc"), params);
        log.info("[JDBC 배치 재실행] batchStartTime={}, 상태={}", targetTime, execution.getStatus());

        return Map.of(
                "batchStartTime", targetTime.toString(),
                "status", execution.getStatus().toString(),
                "exitCode", execution.getExitStatus().getExitCode()
        );
    }
}