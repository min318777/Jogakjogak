package com.zb.jogakjogak.notification.service;

import com.zb.jogakjogak.ga.service.GaMeasurementProtocolService;
import com.zb.jogakjogak.global.util.HashingUtil;
import com.zb.jogakjogak.jobdescription.entity.JD;
import com.zb.jogakjogak.jobdescription.repository.JDRepository;
import com.zb.jogakjogak.notification.dto.NotificationDto;
import com.zb.jogakjogak.notification.entity.Notification;
import com.zb.jogakjogak.notification.entity.NotificationStatus;
import com.zb.jogakjogak.notification.repository.NotificationRepository;
import com.zb.jogakjogak.member.entity.Member;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationSendService {

    private static final int MAX_ATTEMPTS = 3;
    private static final int PAGE_SIZE = 1000;

    private final JDRepository jdRepository;
    private final NotificationEmailSender emailSender;
    private final NotificationRepository notificationRepository;
    private final GaMeasurementProtocolService gaService;

    public void sendDailyNotifications() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime threeDaysAgo = LocalDate.now().atStartOfDay().minusDays(3);
        LocalDateTime todayStart = now.toLocalDate().atStartOfDay();
        Pageable pageable = PageRequest.of(0, PAGE_SIZE);

        List<JD> targetJds = jdRepository.findNotUpdatedJdByQueryDsl(threeDaysAgo, todayStart, pageable).getContent();

        Map<Member, List<JD>> jdsByMember = targetJds.stream()
                .collect(Collectors.groupingBy(JD::getMember));

        for (Map.Entry<Member, List<JD>> entry : jdsByMember.entrySet()) {
            sendToMember(entry.getKey(), entry.getValue(), now);
        }
    }

    @Transactional
    public void sendToMember(Member member, List<JD> jds, LocalDateTime now) {
        try {
            emailSender.sendNotificationEmail(NotificationDto.builder()
                    .member(member)
                    .jdList(jds)
                    .build());

            jdRepository.updateNotificationFields(jds.stream().map(JD::getId).toList(), now);
            for (JD jd : jds) {
                Notification notification = Notification.builder()
                        .jd(jd)
                        .member(member)
                        .createdAt(now)
                        .build();
                notification.markSent(now);
                notificationRepository.save(notification);
            }
            log.info("[DailyNotification] 발송 성공: memberId={}, jdCount={}", member.getId(), jds.size());
        } catch (MessagingException e) {
            for (JD jd : jds) {
                Notification notification = Notification.builder()
                        .jd(jd)
                        .member(member)
                        .status(NotificationStatus.PENDING)
                        .createdAt(now)
                        .build();
                notification.markFailed(e.getMessage(), MAX_ATTEMPTS);
                notificationRepository.save(notification);
            }
            sendGaFailureEvent(member, e.getMessage());
            log.error("[DailyNotification] 발송 실패: memberId={}, reason={}", member.getId(), e.getMessage());
        }
    }

    private void sendGaFailureEvent(Member member, String errorMessage) {
        String userId = member.getId().toString();
        String hashedEmail = HashingUtil.sha256(member.getEmail());

        Map<String, Object> eventParams = new HashMap<>();
        eventParams.put("email_type", "notification_jd_reminder");
        eventParams.put("campaign_name", "jd_deadline_reminder");
        eventParams.put("send_status", "failure");
        eventParams.put("recipient_email", hashedEmail);
        eventParams.put("recipient_user_id", userId);
        eventParams.put("error_message_summary",
                errorMessage != null ? errorMessage.substring(0, Math.min(errorMessage.length(), 250)) : "Unknown email error");
        eventParams.put("error_code_custom", "EMAIL_SEND_FAILED");

        String gaClientId = "backend_notification_error_" + UUID.randomUUID();
        gaService.sendGaEvent(gaClientId, userId, "email_send_failed", eventParams).subscribe();
    }
}
