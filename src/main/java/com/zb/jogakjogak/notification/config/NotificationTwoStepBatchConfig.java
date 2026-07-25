package com.zb.jogakjogak.notification.config;

import com.zb.jogakjogak.jobDescription.entity.JD;
import com.zb.jogakjogak.jobDescription.repository.JDRepository;
import com.zb.jogakjogak.notification.dto.NotificationDto;
import com.zb.jogakjogak.notification.entity.Notification;
import com.zb.jogakjogak.notification.repository.NotificationRepository;
import com.zb.jogakjogak.notification.service.NotificationEmailSender;
import com.zb.jogakjogak.security.entity.Member;
import jakarta.mail.MessagingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.SkipListener;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.batch.item.database.builder.JdbcCursorItemReaderBuilder;
import org.springframework.batch.item.support.CompositeItemWriter;
import org.springframework.batch.item.support.builder.CompositeItemWriterBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Configuration
public class NotificationTwoStepBatchConfig {

    private static final int CHUNK_SIZE = 1000;
    private static final int RETRY_LIMIT = 3;
    private static final int SKIP_SIZE = 10;

    private final NotificationRepository notificationRepository;
    private final JobRepository jobRepository;
    private final JDRepository jdRepository;
    private final PlatformTransactionManager platformTransactionManager;
    private final NotificationEmailSender notificationEmailSender;
    private final DataSource mainDBSource;

    public NotificationTwoStepBatchConfig(
            NotificationRepository notificationRepository,
            JobRepository jobRepository,
            JDRepository jdRepository,
            @Qualifier("dataTransactionManager") PlatformTransactionManager platformTransactionManager,
            NotificationEmailSender notificationEmailSender,
            @Qualifier("mainDBSource") DataSource mainDBSource) {
        this.notificationRepository = notificationRepository;
        this.jobRepository = jobRepository;
        this.jdRepository = jdRepository;
        this.platformTransactionManager = platformTransactionManager;
        this.notificationEmailSender = notificationEmailSender;
        this.mainDBSource = mainDBSource;
    }

    @Bean
    public Job twoStepNotificationJob() {
        return new JobBuilder("sendNotificationTwoStep", jobRepository)
                .start(twoStepSaveNotificationStep())
                    .on("FAILED").end()
                    .on("*").to(twoStepSendEmailStep())
                .end()
                .build();
    }

    // ===== Step 1: JD 읽기 → Notification 저장 =====

    @Bean
    public Step twoStepSaveNotificationStep() {
        return new StepBuilder("twoStepSaveNotificationStep", jobRepository)
                .<JD, JD>chunk(CHUNK_SIZE, platformTransactionManager)
                .reader(twoStepJdReader(null))
                .writer(twoStepSaveNotificationWriter())
                .faultTolerant()
                .retryLimit(RETRY_LIMIT)
                .retry(TransientDataAccessException.class)
                .skipLimit(SKIP_SIZE)
                .skip(TransientDataAccessException.class)
                .listener(jdSkipListener())
                .build();
    }

    @Bean
    @StepScope
    public JdbcCursorItemReader<JD> twoStepJdReader(
            @Value("#{jobParameters['batchStartTime']}") String batchStartTimeStr) {
        LocalDateTime now = LocalDateTime.parse(batchStartTimeStr);
        LocalDateTime todayStart = now.toLocalDate().atStartOfDay();
        LocalDateTime threeDaysAgo = LocalDate.now().atStartOfDay().minusDays(3);

        return new JdbcCursorItemReaderBuilder<JD>()
                .name("twoStepJdReader")
                .dataSource(mainDBSource)
                .fetchSize(CHUNK_SIZE)
                .sql("SELECT j.id, j.member_id FROM job_description j " +
                        "WHERE j.updated_at <= ? " +
                        "AND j.is_alarm_on = true " +
                        "AND j.notification_count < 3 " +
                        "AND (j.last_notified_at IS NULL OR j.last_notified_at < ?) " +
                        "AND (j.ended_at IS NULL OR j.ended_at >= ?) " +
                        "ORDER BY j.id ASC")
                .preparedStatementSetter(ps -> {
                    ps.setTimestamp(1, Timestamp.valueOf(threeDaysAgo));
                    ps.setTimestamp(2, Timestamp.valueOf(todayStart));
                    ps.setTimestamp(3, Timestamp.valueOf(now));
                })
                .rowMapper((rs, rowNum) -> {
                    Member member = Member.builder().id(rs.getLong("member_id")).build();
                    return JD.builder().id(rs.getLong("id")).member(member).build();
                })
                .build();
    }

    @Bean
    public CompositeItemWriter<JD> twoStepSaveNotificationWriter() {
        return new CompositeItemWriterBuilder<JD>()
                .delegates(List.of(jpaJdUpdateWriter(null), jpaNotificationInsertWriter(null)))
                .build();
    }

    @Bean
    @StepScope
    public ItemWriter<JD> jpaJdUpdateWriter(
            @Value("#{jobParameters['batchStartTime']}") String batchStartTimeStr) {
        LocalDateTime batchStartTime = LocalDateTime.parse(batchStartTimeStr);
        return chunk -> {
            List<Long> ids = chunk.getItems().stream().map(JD::getId).toList();
            jdRepository.updateNotificationFields(ids, batchStartTime);
        };
    }

    @Bean
    @StepScope
    public ItemWriter<JD> jpaNotificationInsertWriter(
            @Value("#{jobParameters['batchStartTime']}") String batchStartTimeStr) {
        LocalDateTime batchStartTime = LocalDateTime.parse(batchStartTimeStr);
        return chunk -> {
            List<Notification> notifications = chunk.getItems().stream()
                    .map(jd -> Notification.builder()
                            .jd(jd)
                            .member(jd.getMember())
                            .createdAt(batchStartTime)
                            .build())
                    .toList();
            notificationRepository.saveAll(notifications);
        };
    }

    // ===== Step 2: Notification 읽기 → 이메일 발송 =====

    @Bean
    public Step twoStepSendEmailStep() {
        return new StepBuilder("twoStepSendEmailStep", jobRepository)
                .<Notification, Notification>chunk(CHUNK_SIZE, platformTransactionManager)
                .reader(twoStepNotificationReader(null))
                .writer(twoStepEmailSendingWriter())
                .faultTolerant()
                .skipLimit(SKIP_SIZE)
                .skip(MessagingException.class)
                .listener(notificationSkipListener())
                .build();
    }

    @Bean
    @StepScope
    public JdbcCursorItemReader<Notification> twoStepNotificationReader(
            @Value("#{jobParameters['batchStartTime']}") String batchStartTimeStr) {
        LocalDateTime now = LocalDateTime.parse(batchStartTimeStr);
        log.info("TwoStep Notification Reader: now={}", now);

        return new JdbcCursorItemReaderBuilder<Notification>()
                .name("twoStepNotificationReader")
                .dataSource(mainDBSource)
                .fetchSize(CHUNK_SIZE)
                .sql("SELECT n.id, n.member_id, n.jd_id FROM notification n " +
                        "JOIN job_description j ON n.jd_id = j.id " +
                        "WHERE n.sent = false AND j.ended_at >= ? " +
                        "ORDER BY n.id ASC")
                .preparedStatementSetter(ps -> ps.setTimestamp(1, Timestamp.valueOf(now)))
                .rowMapper((rs, rowNum) -> Notification.builder()
                        .id(rs.getLong("id"))
                        .member(Member.builder().id(rs.getLong("member_id")).build())
                        .jd(JD.builder().id(rs.getLong("jd_id")).build())
                        .build())
                .build();
    }

    @Bean
    public ItemWriter<Notification> twoStepEmailSendingWriter() {
        return chunk -> {
            Map<Long, List<Notification>> grouped = chunk.getItems().stream()
                    .collect(Collectors.groupingBy(n -> n.getMember().getId()));

            for (Map.Entry<Long, List<Notification>> entry : grouped.entrySet()) {
                List<Notification> notifications = entry.getValue();
                Member member = notifications.get(0).getMember();
                List<JD> jdList = notifications.stream().map(Notification::getJd).toList();

                notificationEmailSender.sendNotificationEmail(
                        NotificationDto.builder().member(member).jdList(jdList).build());
                notifications.forEach(n -> n.setSent(true));
            }

            notificationRepository.saveAll(chunk.getItems());
        };
    }

    @Bean
    public SkipListener<JD, JD> jdSkipListener() {
        return new SkipListener<>() {
            @Override
            public void onSkipInRead(Throwable t) {
                log.warn("[SKIP] JD read skipped: {}", t.getMessage());
            }
            @Override
            public void onSkipInWrite(JD item, Throwable t) {
                log.warn("[SKIP] JD id={} skipped: {}", item.getId(), t.getMessage());
            }
        };
    }

    @Bean
    public SkipListener<Notification, Notification> notificationSkipListener() {
        return new SkipListener<>() {
            @Override
            public void onSkipInWrite(Notification item, Throwable t) {
                log.warn("[SKIP] Notification id={} memberId={} skipped: {}",
                        item.getId(), item.getMember().getId(), t.getMessage());
            }
        };
    }
}