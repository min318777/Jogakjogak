package com.zb.jogakjogak.notification.config;

import com.zb.jogakjogak.jobDescription.entity.JD;
import com.zb.jogakjogak.notification.dto.NotificationDto;
import com.zb.jogakjogak.notification.entity.Notification;
import com.zb.jogakjogak.notification.entity.NotificationStatus;
import com.zb.jogakjogak.notification.repository.NotificationRepository;
import com.zb.jogakjogak.notification.service.NotificationBatchService;
import com.zb.jogakjogak.notification.service.NotificationEmailSender;
import jakarta.mail.MessagingException;
import jakarta.persistence.EntityManagerFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.SkipListener;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.database.JpaItemWriter;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Configuration
public class NotificationEmailBatchConfig {

    private static final int CHUNK_SIZE = 1000;
    private static final int MAX_ATTEMPTS = 3;

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final EntityManagerFactory entityManagerFactory;
    private final NotificationRepository notificationRepository;
    private final NotificationEmailSender emailSender;
    private final NotificationBatchService batchService;

    public NotificationEmailBatchConfig(
            JobRepository jobRepository,
            @Qualifier("dataTransactionManager") PlatformTransactionManager transactionManager,
            @Qualifier("dataEntityManager") EntityManagerFactory entityManagerFactory,
            NotificationRepository notificationRepository,
            NotificationEmailSender emailSender,
            NotificationBatchService batchService) {
        this.jobRepository = jobRepository;
        this.transactionManager = transactionManager;
        this.entityManagerFactory = entityManagerFactory;
        this.notificationRepository = notificationRepository;
        this.emailSender = emailSender;
        this.batchService = batchService;
    }

    // Job

    @Bean
    public Job emailNotificationJob() {
        return new JobBuilder("emailNotificationJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(enqueueStep())
                    .on("FAILED").end()
                    .on("*").to(sendStep())
                .end()
                .build();
    }

    // Step 1: 알림 대상 JD 조회 → Notification 저장 (PENDING)

    @Bean
    public Step enqueueStep() {
        return new StepBuilder("enqueueStep", jobRepository)
                .<JD, Notification>chunk(CHUNK_SIZE, transactionManager)
                .reader(emailJdReader(null))
                .processor(jdToNotificationProcessor(null))
                .writer(notificationSaveWriter())
                .build();
    }

    @Bean
    @StepScope
    public JpaPagingItemReader<JD> emailJdReader(
            @Value("#{jobParameters['batchStartTime']}") LocalDateTime batchStartTime) {
        LocalDateTime threeDaysAgo = LocalDate.now().atStartOfDay().minusDays(3);
        LocalDateTime todayStart = batchStartTime.toLocalDate().atStartOfDay();

        return new JpaPagingItemReaderBuilder<JD>()
                .name("jdReader")
                .entityManagerFactory(entityManagerFactory)
                .pageSize(CHUNK_SIZE)
                .queryString(
                        "SELECT j FROM JD j JOIN FETCH j.member " +
                        "WHERE j.updatedAt <= :threeDaysAgo " +
                        "AND j.isAlarmOn = true " +
                        "AND j.notificationCount < 3 " +
                        "AND (j.lastNotifiedAt IS NULL OR j.lastNotifiedAt < :todayStart) " +
                        "AND (j.endedAt IS NULL OR j.endedAt >= :now) " +
                        "ORDER BY j.id ASC")
                .parameterValues(Map.of(
                        "threeDaysAgo", threeDaysAgo,
                        "todayStart", todayStart,
                        "now", batchStartTime))
                .build();
    }

    @Bean
    @StepScope
    public ItemProcessor<JD, Notification> jdToNotificationProcessor(
            @Value("#{jobParameters['batchStartTime']}") LocalDateTime batchStartTime) {
        return jd -> Notification.builder()
                .jd(jd)
                .member(jd.getMember())
                .status(NotificationStatus.PENDING)
                .createdAt(batchStartTime)
                .build();
    }

    @Bean
    public JpaItemWriter<Notification> notificationSaveWriter() {
        JpaItemWriter<Notification> writer = new JpaItemWriter<>();
        writer.setEntityManagerFactory(entityManagerFactory);
        return writer;
    }

    // Step 2: PENDING Notification 조회 → 이메일 발송 → SENT 업데이트

    @Bean
    public Step sendStep() {
        return new StepBuilder("sendStep", jobRepository)
                .<Notification, Notification>chunk(CHUNK_SIZE, transactionManager)
                .reader(notificationReader(null))
                .processor(emailSendProcessor())
                .writer(emailSendWriter())
                .faultTolerant()
                .retry(MessagingException.class)
                .retry(TransientDataAccessException.class)
                .retryLimit(3)
                .skip(MessagingException.class)
                .skipLimit(50)
                .listener(skipListener("[EmailBatch]"))
                .build();
    }

    private SkipListener<Notification, Notification> skipListener(String prefix) {
        return new SkipListener<>() {
            @Override
            public void onSkipInProcess(Notification item, Throwable t) {
                item.markFailed(t.getMessage(), MAX_ATTEMPTS);
                notificationRepository.save(item);
                log.error("{} [Skip] notificationId={}, reason={}", prefix, item.getId(), t.getMessage());
            }
        };
    }

    @Bean
    @StepScope
    public JpaPagingItemReader<Notification> notificationReader(
            @Value("#{jobParameters['batchStartTime']}") LocalDateTime batchStartTime) {
        return new JpaPagingItemReaderBuilder<Notification>()
                .name("notificationReader")
                .entityManagerFactory(entityManagerFactory)
                .pageSize(CHUNK_SIZE)
                .queryString(
                        "SELECT n FROM Notification n " +
                        "JOIN FETCH n.member " +
                        "JOIN FETCH n.jd " +
                        "WHERE n.status = :status " +
                        "ORDER BY n.id ASC")
                .parameterValues(Map.of("status", NotificationStatus.PENDING))
                .build();
    }

    @Bean
    public ItemProcessor<Notification, Notification> emailSendProcessor() {
        return notification -> {
            emailSender.sendNotificationEmail(NotificationDto.builder()
                    .member(notification.getMember())
                    .jdList(List.of(notification.getJd()))
                    .build());
            notification.markSent(LocalDateTime.now());
            return notification;
        };
    }

    @Bean
    public JpaItemWriter<Notification> emailSendWriter() {
        JpaItemWriter<Notification> writer = new JpaItemWriter<>();
        writer.setEntityManagerFactory(entityManagerFactory);
        return writer;
    }
}
