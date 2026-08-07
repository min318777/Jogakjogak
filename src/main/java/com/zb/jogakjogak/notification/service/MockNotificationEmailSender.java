package com.zb.jogakjogak.notification.service;

import com.zb.jogakjogak.notification.dto.NotificationDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Slf4j
@Profile("perf-test")
@Primary
@Component
public class MockNotificationEmailSender implements NotificationEmailSender {

    @Value("${notification.mock.latency-ms:0}")
    private long latencyMs;

    @Override
    public void sendNotificationEmail(NotificationDto notificationDto) {
        if (latencyMs > 0) {
            try {
                Thread.sleep(latencyMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        if (log.isDebugEnabled()) {
            log.debug("MOCK 이메일: memberId={}, JD {}건",
                    notificationDto.getMember().getId(),
                    notificationDto.getJdList().size());
        }
    }
}
