package com.zb.jogakjogak.notification.service;

import com.zb.jogakjogak.jobDescription.entity.JD;
import com.zb.jogakjogak.jobDescription.repository.JDRepository;
import com.zb.jogakjogak.notification.dto.NotificationDto;
import com.zb.jogakjogak.notification.entity.Notification;
import com.zb.jogakjogak.notification.entity.NotificationStatus;
import com.zb.jogakjogak.notification.repository.NotificationRepository;
import com.zb.jogakjogak.security.entity.Member;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
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
            log.error("[DailyNotification] 발송 실패: memberId={}, reason={}", member.getId(), e.getMessage());
        }
    }
}
