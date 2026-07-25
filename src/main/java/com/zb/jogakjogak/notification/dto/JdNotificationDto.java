package com.zb.jogakjogak.notification.dto;

import com.zb.jogakjogak.jobDescription.entity.JD;
import com.zb.jogakjogak.notification.entity.Notification;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class JdNotificationDto {
    private final JD jd;
    private final Notification notification;
}
