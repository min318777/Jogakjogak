package com.zb.jogakjogak.notification.repository;

import com.zb.jogakjogak.notification.entity.Notification;
import com.zb.jogakjogak.notification.entity.NotificationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Page<Notification> findByCreatedAtAfter(LocalDateTime since, Pageable pageable);

    @Query("SELECT n FROM Notification n JOIN FETCH n.member JOIN FETCH n.jd WHERE n.sent = false AND n.jd.endedAt >= :now")
    Page<Notification> findUnsentWithValidJd(@Param("now") LocalDateTime now, Pageable pageable);

    @Modifying(clearAutomatically = true)
    @Query(value = "UPDATE notification SET status = 'SENT', sent = 1, sent_at = :now, " +
            "attempt_count = attempt_count + 1, error_message = NULL WHERE id = :id",
            nativeQuery = true)
    void markSent(@Param("id") Long id, @Param("now") LocalDateTime now);

    @Modifying(clearAutomatically = true)
    @Query(value = "UPDATE notification SET " +
            "status = IF(attempt_count + 1 >= :maxAttempts, 'DEAD_LETTER', 'FAILED'), " +
            "attempt_count = attempt_count + 1, " +
            "error_message = :errorMessage WHERE id = :id",
            nativeQuery = true)
    void markFailed(@Param("id") Long id,
                    @Param("errorMessage") String errorMessage,
                    @Param("maxAttempts") int maxAttempts);

    @Query("SELECT COUNT(n) FROM Notification n WHERE n.status IN :statuses")
    long countByStatusIn(@Param("statuses") List<NotificationStatus> statuses);

    @Query(value = "SELECT id FROM notification " +
            "WHERE status IN ('PENDING','FAILED') AND attempt_count < :maxAttempts " +
            "ORDER BY id LIMIT :limit FOR UPDATE SKIP LOCKED",
            nativeQuery = true)
    List<Long> findClaimableIds(@Param("maxAttempts") int maxAttempts, @Param("limit") int limit);

    @Modifying(clearAutomatically = true)
    @Query(value = "UPDATE notification SET status = 'SENDING' " +
            "WHERE id IN :ids AND status IN ('PENDING','FAILED')",
            nativeQuery = true)
    int claim(@Param("ids") List<Long> ids);

    @Modifying(clearAutomatically = true)
    @Query(value = "UPDATE notification SET status = 'SENDING' WHERE id = :id AND status = 'PENDING'",
            nativeQuery = true)
    int markAsSending(@Param("id") Long id);

    @Query("SELECT n FROM Notification n JOIN FETCH n.member JOIN FETCH n.jd WHERE n.id IN :ids")
    List<Notification> findWithMemberAndJdByIds(@Param("ids") List<Long> ids);
}
