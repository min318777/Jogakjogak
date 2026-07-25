package com.zb.jogakjogak.notification.service;

import com.zb.jogakjogak.notification.dto.NotificationDto;
import jakarta.mail.MessagingException;

public interface NotificationEmailSender {
    void sendNotificationEmail(NotificationDto notificationDto) throws MessagingException;
}
