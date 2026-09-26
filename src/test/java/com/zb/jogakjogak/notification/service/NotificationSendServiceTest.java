package com.zb.jogakjogak.notification.service;

import com.zb.jogakjogak.ga.service.GaMeasurementProtocolService;
import com.zb.jogakjogak.jobdescription.entity.JD;
import com.zb.jogakjogak.jobdescription.repository.JDRepository;
import com.zb.jogakjogak.member.entity.Member;
import com.zb.jogakjogak.notification.dto.NotificationDto;
import com.zb.jogakjogak.notification.repository.NotificationRepository;
import jakarta.mail.MessagingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class NotificationSendServiceTest {

    @Mock
    private JDRepository jdRepository;

    @Mock
    private NotificationEmailSender emailSender;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private GaMeasurementProtocolService gaService;

    private NotificationSendService notificationSendService;

    private Member member;
    private List<JD> jds;
    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        notificationSendService = new NotificationSendService(jdRepository, emailSender, notificationRepository, gaService);

        member = Member.builder()
                .id(1L)
                .email("test@example.com")
                .nickname("테스터")
                .build();

        JD jd = JD.builder()
                .id(1L)
                .title("백엔드 개발자")
                .companyName("테스트 회사")
                .endedAt(LocalDateTime.now().plusDays(7))
                .build();

        jds = List.of(jd);
        now = LocalDateTime.now();
    }

    @Test
    @DisplayName("메일 발송 최종 실패 시 GA 실패 이벤트를 한 번만 전송한다")
    void sendToMember_fail_sendsGaFailureEventOnce() throws MessagingException {
        // given
        given(gaService.sendGaEvent(any(), any(), any(), any())).willReturn(Mono.empty());
        willThrow(new MessagingException("이메일 전송 실패")).given(emailSender).sendNotificationEmail(any(NotificationDto.class));

        // when
        notificationSendService.sendToMember(member, jds, now);

        // then
        ArgumentCaptor<String> eventNameCaptor = ArgumentCaptor.forClass(String.class);
        then(gaService).should().sendGaEvent(any(), eq("1"), eventNameCaptor.capture(), any());
        assert eventNameCaptor.getValue().equals("email_send_failed");
    }

    @Test
    @DisplayName("메일 발송 성공 시 GA 실패 이벤트를 전송하지 않는다")
    void sendToMember_success_doesNotSendGaFailureEvent() throws MessagingException {
        // when
        notificationSendService.sendToMember(member, jds, now);

        // then
        then(gaService).should(never()).sendGaEvent(any(), any(), any(), any());
    }
}
