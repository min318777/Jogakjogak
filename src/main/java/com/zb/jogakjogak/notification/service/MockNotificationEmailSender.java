package com.zb.jogakjogak.notification.service;

import com.zb.jogakjogak.notification.dto.NotificationDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Slf4j
@Profile("perf-test")
@Primary
@Component
public class MockNotificationEmailSender implements NotificationEmailSender {

    @Override
    public void sendNotificationEmail(NotificationDto notificationDto) {
        log.info("MOCK 이메일: memberId={}, JD {}건",
                notificationDto.getMember().getId(),
                notificationDto.getJdList().size());
    }
}
