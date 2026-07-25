package com.zb.jogakjogak.notification.config;

import com.zb.jogakjogak.jobDescription.entity.JD;
import com.zb.jogakjogak.notification.dto.NotificationDto;
import com.zb.jogakjogak.notification.entity.Notification;
import com.zb.jogakjogak.notification.service.NotificationEmailSender;
import com.zb.jogakjogak.security.config.EmailEncryptor;
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
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.item.database.builder.JdbcCursorItemReaderBuilder;
import org.springframework.batch.item.support.CompositeItemWriter;
import org.springframework.batch.item.support.builder.CompositeItemWriterBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Configuration
public class NotificationTwoStepJdbcBatchConfig {

    private static final int CHUNK_SIZE = 1000;
    private static final int RETRY_LIMIT = 3;
    private static final int SKIP_SIZE = 10;

    private final JobRepository jobRepository;
    private final PlatformTransactionManager dataTransactionManager;
    private final NotificationEmailSender notificationEmailSender;
    private final DataSource mainDBSource;
    private final JdbcTemplate jdbcTemplate;

    public NotificationTwoStepJdbcBatchConfig(
            JobRepository jobRepository,
            @Qualifier("dataTransactionManager") PlatformTransactionManager dataTransactionManager,
            @Qualifier("mainDBSource") DataSource mainDBSource,
            NotificationEmailSender notificationEmailSender) {
        this.jobRepository = jobRepository;
        this.dataTransactionManager = dataTransactionManager;
        this.mainDBSource = mainDBSource;
        this.notificationEmailSender = notificationEmailSender;
        this.jdbcTemplate = new JdbcTemplate(mainDBSource);
    }

    @Bean
    public Job jdbcTwoStepNotificationJob() {
        return new JobBuilder("sendNotificationTwoStepJdbc", jobRepository)
                .start(jdbcTwoStepSaveNotificationStep())
                    .on("FAILED").end()
                    .on("*").to(jdbcTwoStepSendEmailStep())
                .end()
                .build();
    }

    // ===== Step 1: JD 읽기 → JD UPDATE + Notification INSERT (JDBC Bulk) =====

    @Bean
    public Step jdbcTwoStepSaveNotificationStep() {
        return new StepBuilder("jdbcTwoStepSaveNotificationStep", jobRepository)
                .<JD, JD>chunk(CHUNK_SIZE, dataTransactionManager)
                .reader(jdbcTwoStepJdReader(null))
                .writer(jdbcTwoStepNotificationBulkWriter())
                .faultTolerant()
                .retryLimit(RETRY_LIMIT)
                .retry(TransientDataAccessException.class)
                .skipLimit(SKIP_SIZE)
                .skip(TransientDataAccessException.class)
                .listener(jdbcJdSkipListener())
                .build();
    }

    @Bean
    @StepScope
    public JdbcCursorItemReader<JD> jdbcTwoStepJdReader(
            @Value("#{jobParameters['batchStartTime']}") String batchStartTimeStr) {
        LocalDateTime now = LocalDateTime.parse(batchStartTimeStr);
        LocalDateTime todayStart = now.toLocalDate().atStartOfDay();
        LocalDateTime threeDaysAgo = LocalDate.now().atStartOfDay().minusDays(3);
        log.info("JDBC TwoStep JD Reader: now={}, todayStart={}", now, todayStart);

        return new JdbcCursorItemReaderBuilder<JD>()
                .name("jdbcTwoStepJdReader")
                .dataSource(mainDBSource)
                .sql("SELECT j.id, j.member_id " +
                        "FROM job_description j " +
                        "WHERE j.updated_at <= ? " +
                        "AND j.is_alarm_on = 1 " +
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
                    Member member = Member.builder()
                            .id(rs.getLong("member_id"))
                            .build();
                    return JD.builder()
                            .id(rs.getLong("id"))
                            .member(member)
                            .build();
                })
                .build();
    }

    @Bean
    public CompositeItemWriter<JD> jdbcTwoStepNotificationBulkWriter() {
        return new CompositeItemWriterBuilder<JD>()
                .delegates(List.of(jdbcJdUpdateWriter(null), jdbcNotificationInsertWriter(null)))
                .build();
    }

    @Bean
    @StepScope
    public JdbcBatchItemWriter<JD> jdbcJdUpdateWriter(
            @Value("#{jobParameters['batchStartTime']}") String batchStartTimeStr) {
        LocalDateTime batchStartTime = LocalDateTime.parse(batchStartTimeStr);
        return new JdbcBatchItemWriterBuilder<JD>()
                .dataSource(mainDBSource)
                .sql("UPDATE job_description SET notification_count = notification_count + 1, last_notified_at = :batchStartTime WHERE id = :id")
                .itemSqlParameterSourceProvider(jd -> new MapSqlParameterSource()
                        .addValue("batchStartTime", batchStartTime)
                        .addValue("id", jd.getId()))
                .build();
    }

    @Bean
    @StepScope
    public JdbcBatchItemWriter<JD> jdbcNotificationInsertWriter(
            @Value("#{jobParameters['batchStartTime']}") String batchStartTimeStr) {
        LocalDateTime batchStartTime = LocalDateTime.parse(batchStartTimeStr);
        return new JdbcBatchItemWriterBuilder<JD>()
                .dataSource(mainDBSource)
                .sql("INSERT INTO notification (member_id, jd_id, created_at, sent) VALUES (:memberId, :jdId, :createdAt, false)")
                .itemSqlParameterSourceProvider(jd -> new MapSqlParameterSource()
                        .addValue("memberId", jd.getMember().getId())
                        .addValue("jdId", jd.getId())
                        .addValue("createdAt", batchStartTime))
                .build();
    }

    // ===== Step 2: Notification 읽기 → 이메일 발송 + sent UPDATE (JDBC Bulk) =====

    @Bean
    public Step jdbcTwoStepSendEmailStep() {
        return new StepBuilder("jdbcTwoStepSendEmailStep", jobRepository)
                .<Notification, Notification>chunk(CHUNK_SIZE, dataTransactionManager)
                .reader(jdbcTwoStepNotificationReader(null))
                .writer(jdbcTwoStepEmailSendingWriter())
                .faultTolerant()
                .retryLimit(RETRY_LIMIT)
                .retry(MessagingException.class)
                .skipLimit(SKIP_SIZE)
                .skip(MessagingException.class)
                .listener(jdbcNotificationSkipListener())
                .build();
    }

    @Bean
    @StepScope
    public JdbcCursorItemReader<Notification> jdbcTwoStepNotificationReader(
            @Value("#{jobParameters['batchStartTime']}") String batchStartTimeStr) {
        LocalDateTime now = LocalDateTime.parse(batchStartTimeStr);
        EmailEncryptor emailEncryptor = new EmailEncryptor();

        return new JdbcCursorItemReaderBuilder<Notification>()
                .name("jdbcTwoStepNotificationReader")
                .dataSource(mainDBSource)
                .sql("SELECT n.id AS n_id, n.sent, n.created_at, " +
                        "m.id AS m_id, m.email, m.nickname, " +
                        "j.id AS j_id, j.title, j.company_name, j.ended_at " +
                        "FROM notification n " +
                        "JOIN member m ON n.member_id = m.id " +
                        "JOIN job_description j ON n.jd_id = j.id " +
                        "WHERE n.sent = false " +
                        "AND (j.ended_at IS NULL OR j.ended_at >= ?) " +
                        "ORDER BY n.id ASC")
                .preparedStatementSetter(ps -> ps.setTimestamp(1, Timestamp.valueOf(now)))
                .rowMapper((rs, rowNum) -> {
                    Timestamp endedAtTs = rs.getTimestamp("ended_at");
                    Member member = Member.builder()
                            .id(rs.getLong("m_id"))
                            .email(emailEncryptor.convertToEntityAttribute(rs.getString("email")))
                            .nickname(rs.getString("nickname"))
                            .build();
                    JD jd = JD.builder()
                            .id(rs.getLong("j_id"))
                            .title(rs.getString("title"))
                            .companyName(rs.getString("company_name"))
                            .endedAt(endedAtTs != null ? endedAtTs.toLocalDateTime() : null)
                            .build();
                    return Notification.builder()
                            .id(rs.getLong("n_id"))
                            .sent(rs.getBoolean("sent"))
                            .createdAt(rs.getTimestamp("created_at").toLocalDateTime())
                            .member(member)
                            .jd(jd)
                            .build();
                })
                .build();
    }

    @Bean
    public ItemWriter<Notification> jdbcTwoStepEmailSendingWriter() {
        return chunk -> {
            Map<Long, List<Notification>> grouped = chunk.getItems().stream()
                    .collect(Collectors.groupingBy(n -> n.getMember().getId()));

            List<Long> sentIds = new ArrayList<>();
            for (Map.Entry<Long, List<Notification>> entry : grouped.entrySet()) {
                List<Notification> notifications = entry.getValue();
                Member member = notifications.get(0).getMember();
                List<JD> jdList = notifications.stream().map(Notification::getJd).toList();

                notificationEmailSender.sendNotificationEmail(
                        NotificationDto.builder().member(member).jdList(jdList).build());
                notifications.stream().map(Notification::getId).forEach(sentIds::add);
            }

            if (!sentIds.isEmpty()) {
                jdbcTemplate.batchUpdate("UPDATE notification SET sent = 1 WHERE id = ?",
                        sentIds, sentIds.size(), (ps, id) -> ps.setLong(1, id));
            }
        };
    }

    @Bean
    public SkipListener<JD, JD> jdbcJdSkipListener() {
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
    public SkipListener<Notification, Notification> jdbcNotificationSkipListener() {
        return new SkipListener<>() {
            @Override
            public void onSkipInWrite(Notification item, Throwable t) {
                log.warn("[SKIP] Notification id={} memberId={} skipped: {}",
                        item.getId(), item.getMember().getId(), t.getMessage());
            }
        };
    }
}
