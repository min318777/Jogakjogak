package com.zb.jogakjogak.notification.config;

import com.zb.jogakjogak.jobDescription.entity.JD;
import com.zb.jogakjogak.notification.dto.NotificationDto;
import com.zb.jogakjogak.notification.entity.Notification;
import com.zb.jogakjogak.notification.entity.NotificationStatus;
import com.zb.jogakjogak.notification.repository.NotificationRepository;
import com.zb.jogakjogak.notification.service.NotificationBatchService;
import com.zb.jogakjogak.notification.service.NotificationEmailSender;
import com.zb.jogakjogak.security.entity.Member;
import jakarta.mail.MessagingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.SkipListener;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.item.database.builder.JdbcCursorItemReaderBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Configuration
public class NotificationEmailFullJdbcBatchConfig {

    private static final int CHUNK_SIZE = 1000;
    private static final int MAX_ATTEMPTS = 3;

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final DataSource mainDBSource;
    private final NotificationEmailSender emailSender;
    private final NotificationBatchService batchService;
    private final NotificationRepository notificationRepository;

    public NotificationEmailFullJdbcBatchConfig(
            JobRepository jobRepository,
            @Qualifier("dataTransactionManager") PlatformTransactionManager transactionManager,
            @Qualifier("mainDBSource") DataSource mainDBSource,
            NotificationEmailSender emailSender,
            NotificationBatchService batchService,
            NotificationRepository notificationRepository) {
        this.jobRepository = jobRepository;
        this.transactionManager = transactionManager;
        this.mainDBSource = mainDBSource;
        this.emailSender = emailSender;
        this.batchService = batchService;
        this.notificationRepository = notificationRepository;
    }

    // Job

    @Bean
    public Job emailNotificationFullJdbcBatchJob() {
        return new JobBuilder("emailNotificationFullJdbcBatchJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(enqueueStepFullJdbc())
                    .on("FAILED").end()
                    .on("*").to(sendStepFullJdbc())
                .end()
                .build();
    }

    // Step 1: 알림 대상 JD 조회 → Notification batch INSERT (PENDING)

    @Bean
    public Step enqueueStepFullJdbc() {
        return new StepBuilder("enqueueStepFullJdbc", jobRepository)
                .<JD, Notification>chunk(CHUNK_SIZE, transactionManager)
                .reader(fullJdbcJdReader(null))
                .processor(fullJdbcJdToNotificationProcessor(null))
                .writer(fullJdbcNotificationInsertWriter())
                .build();
    }

    @Bean
    @StepScope
    public JdbcCursorItemReader<JD> fullJdbcJdReader(
            @Value("#{jobParameters['batchStartTime']}") LocalDateTime batchStartTime) {
        LocalDateTime threeDaysAgo = LocalDate.now().atStartOfDay().minusDays(3);
        LocalDateTime todayStart = batchStartTime.toLocalDate().atStartOfDay();

        String sql =
                "SELECT j.id, j.member_id, m.id AS mid, m.email, m.nickname " +
                "FROM job_description j " +
                "JOIN member m ON m.id = j.member_id " +
                "WHERE j.updated_at <= ? " +
                "AND j.is_alarm_on = 1 " +
                "AND j.notification_count < 3 " +
                "AND (j.last_notified_at IS NULL OR j.last_notified_at < ?) " +
                "AND (j.ended_at IS NULL OR j.ended_at >= ?) " +
                "ORDER BY j.id ASC";

        return new JdbcCursorItemReaderBuilder<JD>()
                .name("fullJdbcJdReader")
                .dataSource(mainDBSource)
                .sql(sql)
                .fetchSize(CHUNK_SIZE)
                .preparedStatementSetter(ps -> {
                    ps.setObject(1, threeDaysAgo);
                    ps.setObject(2, todayStart);
                    ps.setObject(3, batchStartTime);
                })
                .rowMapper((rs, rowNum) -> {
                    Member member = new Member();
                    member.setId(rs.getLong("mid"));
                    member.setEmail(rs.getString("email"));
                    member.setNickname(rs.getString("nickname"));

                    JD jd = new JD();
                    jd.setId(rs.getLong("id"));
                    jd.setMember(member);
                    return jd;
                })
                .build();
    }

    @Bean
    @StepScope
    public ItemProcessor<JD, Notification> fullJdbcJdToNotificationProcessor(
            @Value("#{jobParameters['batchStartTime']}") LocalDateTime batchStartTime) {
        return jd -> Notification.builder()
                .jd(jd)
                .member(jd.getMember())
                .status(NotificationStatus.PENDING)
                .createdAt(batchStartTime)
                .build();
    }

    @Bean
    public JdbcBatchItemWriter<Notification> fullJdbcNotificationInsertWriter() {
        return new JdbcBatchItemWriterBuilder<Notification>()
                .dataSource(mainDBSource)
                .sql("INSERT INTO notification (jd_id, member_id, status, created_at, attempt_count, sent) " +
                     "VALUES (:jd.id, :member.id, 'PENDING', :createdAt, 0, false)")
                .beanMapped()
                .build();
    }

    // Step 2: PENDING Notification 조회 → 이메일 발송(Processor) → batch UPDATE(Writer)

    @Bean
    public Step sendStepFullJdbc() {
        return new StepBuilder("sendStepFullJdbc", jobRepository)
                .<Notification, Notification>chunk(CHUNK_SIZE, transactionManager)
                .reader(fullJdbcNotificationReader())
                .processor(fullJdbcEmailSendProcessor())
                .writer(fullJdbcNotificationUpdateWriter())
                .faultTolerant()
                .retry(MessagingException.class)
                .retry(TransientDataAccessException.class)
                .retryLimit(3)
                .skip(MessagingException.class)
                .skipLimit(50)
                .listener(skipListener("[EmailFullJdbcBatch]"))
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
    public JdbcCursorItemReader<Notification> fullJdbcNotificationReader() {
        String sql =
                "SELECT n.id, n.attempt_count, n.status, " +
                "m.id AS mid, m.email, m.nickname, " +
                "j.id AS jid, j.title, j.company_name, j.ended_at " +
                "FROM notification n " +
                "JOIN member m ON m.id = n.member_id " +
                "JOIN job_description j ON j.id = n.jd_id " +
                "WHERE n.status = 'PENDING' " +
                "ORDER BY n.id ASC";

        return new JdbcCursorItemReaderBuilder<Notification>()
                .name("fullJdbcNotificationReader")
                .dataSource(mainDBSource)
                .sql(sql)
                .fetchSize(CHUNK_SIZE)
                .rowMapper((rs, rowNum) -> {
                    Member member = new Member();
                    member.setId(rs.getLong("mid"));
                    member.setEmail(rs.getString("email"));
                    member.setNickname(rs.getString("nickname"));

                    JD jd = new JD();
                    jd.setId(rs.getLong("jid"));
                    jd.setTitle(rs.getString("title"));
                    jd.setCompanyName(rs.getString("company_name"));
                    jd.setEndedAt(rs.getObject("ended_at", LocalDateTime.class));

                    return Notification.builder()
                            .id(rs.getLong("id"))
                            .attemptCount(rs.getInt("attempt_count"))
                            .status(NotificationStatus.valueOf(rs.getString("status")))
                            .member(member)
                            .jd(jd)
                            .build();
                })
                .build();
    }

    // 이메일 발송 후 status 변경 → Writer에서 batch UPDATE
    @Bean
    public ItemProcessor<Notification, Notification> fullJdbcEmailSendProcessor() {
        return notification -> {
            emailSender.sendNotificationEmail(NotificationDto.builder()
                    .member(notification.getMember())
                    .jdList(List.of(notification.getJd()))
                    .build());
            notification.markSent(LocalDateTime.now());
            return notification;
        };
    }

    // enum을 .name()으로 직접 변환해서 직렬화 문제 방지
    @Bean
    public JdbcBatchItemWriter<Notification> fullJdbcNotificationUpdateWriter() {
        return new JdbcBatchItemWriterBuilder<Notification>()
                .dataSource(mainDBSource)
                .sql("UPDATE notification " +
                     "SET status = :status, sent = :sent, sent_at = :sentAt, " +
                     "    attempt_count = :attemptCount, error_message = :errorMessage " +
                     "WHERE id = :id")
                .itemSqlParameterSourceProvider(notification -> new MapSqlParameterSource()
                        .addValue("id", notification.getId())
                        .addValue("status", notification.getStatus().name())
                        .addValue("sent", notification.isSent())
                        .addValue("sentAt", notification.getSentAt())
                        .addValue("attemptCount", notification.getAttemptCount())
                        .addValue("errorMessage", notification.getErrorMessage()))
                .build();
    }
}
