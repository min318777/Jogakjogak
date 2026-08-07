package com.zb.jogakjogak.notification.schedule;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.LocalDateTime;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class NotificationScheduler {

    private final JobLauncher jobLauncher;
    private final JobRegistry jobRegistry;

    @Scheduled(cron = "0 0 10 * * *", zone = "Asia/Seoul")
    public void runEmailNotificationJob() throws Exception {
        JobParameters params = new JobParametersBuilder()
                .addLocalDateTime("batchStartTime", LocalDateTime.now())
                .addLong("run.id", System.currentTimeMillis())
                .toJobParameters();
        jobLauncher.run(jobRegistry.getJob("emailNotificationJob"), params);
    }
}
