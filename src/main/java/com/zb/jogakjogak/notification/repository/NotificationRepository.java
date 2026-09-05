package com.zb.jogakjogak.notification.repository;

import com.zb.jogakjogak.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    @Modifying(clearAutomatically = true)
    @Query(value = "UPDATE notification SET status = 'SENDING' WHERE id = :id AND status = 'PENDING'",
            nativeQuery = true)
    int markAsSending(@Param("id") Long id);
}
