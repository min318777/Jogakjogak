package com.zb.jogakjogak.notification.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationTest {

    @Test
    @DisplayName("markSent - 상태가 SENT로 변경되고 발송 정보가 기록된다")
    void markSent_success() {
        // given
        Notification notification = Notification.builder().build();
        LocalDateTime sentAt = LocalDateTime.of(2025, 8, 1, 10, 0);

        // when
        notification.markSent(sentAt);

        // then
        assertThat(notification.getStatus()).isEqualTo(NotificationStatus.SENT);
        assertThat(notification.isSent()).isTrue();
        assertThat(notification.getSentAt()).isEqualTo(sentAt);
        assertThat(notification.getAttemptCount()).isEqualTo(1);
        assertThat(notification.getErrorMessage()).isNull();
    }

    @Test
    @DisplayName("markFailed - 시도 횟수가 maxAttempts 미만이면 FAILED 상태")
    void markFailed_below_max_attempts() {
        // given
        Notification notification = Notification.builder().build(); // attemptCount = 0
        int maxAttempts = 3;

        // when
        notification.markFailed("메일 전송 실패", maxAttempts);

        // then
        assertThat(notification.getStatus()).isEqualTo(NotificationStatus.FAILED);
        assertThat(notification.getAttemptCount()).isEqualTo(1);
        assertThat(notification.getErrorMessage()).isEqualTo("메일 전송 실패");
    }

    @Test
    @DisplayName("markFailed - 시도 횟수가 maxAttempts 이상이면 DEAD_LETTER 상태")
    void markFailed_at_max_attempts() {
        // given
        Notification notification = Notification.builder()
                .attemptCount(2)
                .build();
        int maxAttempts = 3;

        // when
        notification.markFailed("메일 전송 실패", maxAttempts);

        // then
        assertThat(notification.getStatus()).isEqualTo(NotificationStatus.DEAD_LETTER);
        assertThat(notification.getAttemptCount()).isEqualTo(3);
    }

    @Test
    @DisplayName("markFailed - 에러 메시지가 500자를 초과하면 500자로 잘린다")
    void markFailed_truncates_long_error_message() {
        // given
        Notification notification = Notification.builder().build();
        String longMessage = "a".repeat(600);

        // when
        notification.markFailed(longMessage, 3);

        // then
        assertThat(notification.getErrorMessage()).hasSize(500);
    }

    @Test
    @DisplayName("markFailed - 에러 메시지가 null이면 null로 저장된다")
    void markFailed_null_error_message() {
        // given
        Notification notification = Notification.builder().build();

        // when
        notification.markFailed(null, 3);

        // then
        assertThat(notification.getErrorMessage()).isNull();
    }
}
