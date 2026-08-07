package com.zb.jogakjogak.notification.entity;


import com.zb.jogakjogak.jobDescription.entity.JD;
import com.zb.jogakjogak.security.entity.Member;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDateTime createdAt;

    @Builder.Default
    private boolean sent = false;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    @Builder.Default
    private NotificationStatus status = NotificationStatus.PENDING;

    @Builder.Default
    private int attemptCount = 0;

    private LocalDateTime sentAt;

    @Column(length = 500)
    private String errorMessage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "jd_id")
    private JD jd;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    public void markSent(LocalDateTime sentAt) {
        this.status = NotificationStatus.SENT;
        this.sent = true;
        this.sentAt = sentAt;
        this.attemptCount += 1;
        this.errorMessage = null;
    }

    public void markFailed(String errorMessage, int maxAttempts) {
        this.attemptCount += 1;
        this.errorMessage = truncate(errorMessage);
        this.status = this.attemptCount >= maxAttempts
                ? NotificationStatus.DEAD_LETTER
                : NotificationStatus.FAILED;
    }

    private String truncate(String s) {
        if (s == null) return null;
        return s.length() > 500 ? s.substring(0, 500) : s;
    }
}
