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
import org.springframework.dao.TransientDataAccessException;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.database.JpaItemWriter;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.batch.item.database.builder.JdbcCursorItemReaderBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Configuration
public class NotificationEmailJdbcCursorBatchConfig {

    private static final int CHUNK_SIZE = 1000;
    private static final int MAX_ATTEMPTS = 3;

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final DataSource mainDBSource;
    private final EntityManagerFactory entityManagerFactory;
    private final NotificationRepository notificationRepository;
    private final NotificationEmailSender emailSender;
    private final NotificationBatchService batchService;

    public NotificationEmailJdbcCursorBatchConfig(
            JobRepository jobRepository,
            @Qualifier("dataTransactionManager") PlatformTransactionManager transactionManager,
            @Qualifier("mainDBSource") DataSource mainDBSource,
            @Qualifier("dataEntityManager") EntityManagerFactory entityManagerFactory,
            NotificationRepository notificationRepository,
            NotificationEmailSender emailSender,
            NotificationBatchService batchService) {
        this.jobRepository = jobRepository;
        this.transactionManager = transactionManager;
        this.mainDBSource = mainDBSource;
        this.entityManagerFactory = entityManagerFactory;
        this.notificationRepository = notificationRepository;
        this.emailSender = emailSender;
        this.batchService = batchService;
    }

    // Job

    @Bean
    public Job emailNotificationJdbcCursorJob() {
        return new JobBuilder("emailNotificationJdbcCursorJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(enqueueStepJdbc())
                    .on("FAILED").end()
                    .on("*").to(sendStepJdbc())
                .end()
                .build();
    }

    // Step 1: 알림 대상 JD 조회 → Notification 저장 (PENDING)

    @Bean
    public Step enqueueStepJdbc() {
        return new StepBuilder("enqueueStepJdbc", jobRepository)
                .<JD, Notification>chunk(CHUNK_SIZE, transactionManager)
                .reader(jdbcJdReader(null))
                .processor(jdbcJdToNotificationProcessor(null))
                .writer(jdbcNotificationSaveWriter())
                .build();
    }

    @Bean
    @StepScope
    public JdbcCursorItemReader<JD> jdbcJdReader(
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
                .name("jdbcJdReader")
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
    public ItemProcessor<JD, Notification> jdbcJdToNotificationProcessor(
            @Value("#{jobParameters['batchStartTime']}") LocalDateTime batchStartTime) {
        return jd -> Notification.builder()
                .jd(jd)
                .member(jd.getMember())
                .status(NotificationStatus.PENDING)
                .createdAt(batchStartTime)
                .build();
    }

    @Bean
    public JpaItemWriter<Notification> jdbcNotificationSaveWriter() {
        JpaItemWriter<Notification> writer = new JpaItemWriter<>();
        writer.setEntityManagerFactory(entityManagerFactory);
        return writer;
    }

    // Step 2: PENDING Notification 조회 → 이메일 발송 → SENT 업데이트

    @Bean
    public Step sendStepJdbc() {
        return new StepBuilder("sendStepJdbc", jobRepository)
                .<Notification, Notification>chunk(CHUNK_SIZE, transactionManager)
                .reader(jdbcNotificationReader())
                .processor(jdbcEmailSendProcessor())
                .writer(jdbcEmailSendWriter())
                .faultTolerant()
                .retry(MessagingException.class)
                .retry(TransientDataAccessException.class)
                .retryLimit(3)
                .skip(MessagingException.class)
                .skipLimit(50)
                .listener(skipListener("[EmailJdbcCursorBatch]"))
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
    public JdbcCursorItemReader<Notification> jdbcNotificationReader() {
        String sql =
                "SELECT n.id, n.attempt_count, n.created_at, n.error_message, n.sent, n.sent_at, n.status, " +
                "m.id AS mid, m.email, m.nickname, " +
                "j.id AS jid, j.title, j.company_name, j.ended_at " +
                "FROM notification n " +
                "JOIN member m ON m.id = n.member_id " +
                "JOIN job_description j ON j.id = n.jd_id " +
                "WHERE n.status = 'PENDING' " +
                "ORDER BY n.id ASC";

        return new JdbcCursorItemReaderBuilder<Notification>()
                .name("jdbcNotificationReader")
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

    @Bean
    public ItemProcessor<Notification, Notification> jdbcEmailSendProcessor() {
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
    public JpaItemWriter<Notification> jdbcEmailSendWriter() {
        JpaItemWriter<Notification> writer = new JpaItemWriter<>();
        writer.setEntityManagerFactory(entityManagerFactory);
        return writer;
    }
}
