package com.zb.jogakjogak.notification.service;

import com.zb.jogakjogak.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationBatchService {

    private final NotificationRepository notificationRepository;

    // 청크 트랜잭션과 독립적으로 즉시 커밋 → 재실행 시 중복 발송 방지
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markAsSending(Long notificationId) {
        notificationRepository.markAsSending(notificationId);
    }
}
