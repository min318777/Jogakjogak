package com.zb.jogakjogak.notification.service;

import com.zb.jogakjogak.ga.service.GaMeasurementProtocolService;
import com.zb.jogakjogak.jobDescription.entity.JD;
import com.zb.jogakjogak.jobDescription.repository.ToDoListRepository;
import com.zb.jogakjogak.notification.dto.NotificationDto;
import com.zb.jogakjogak.security.entity.Member;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private JavaMailSender javaMailSender;

    @Mock
    private SpringTemplateEngine templateEngine;

    @Mock
    private ToDoListRepository toDoListRepository;

    @Mock
    private GaMeasurementProtocolService gaService;

    @InjectMocks
    private NotificationService notificationService;

    private NotificationDto notificationDto;

    @BeforeEach
    void setUp() {
        Member member = Member.builder()
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

        notificationDto = NotificationDto.builder()
                .member(member)
                .jdList(new ArrayList<>(List.of(jd)))
                .build();
    }

    @Test
    @DisplayName("이메일 발송 성공 - 메일 전송 및 GA 성공 이벤트 호출")
    void sendNotificationEmail_success() throws MessagingException {
        // given
        MimeMessage mimeMessage = new MimeMessage((Session) null);
        given(javaMailSender.createMimeMessage()).willReturn(mimeMessage);
        given(templateEngine.process(any(String.class), any(Context.class))).willReturn("<html>test</html>");
        given(toDoListRepository.countByIsDoneTrueAndJd_Id(1L)).willReturn(3);
        given(toDoListRepository.countByJd_Id(1L)).willReturn(5);
        given(gaService.sendGaEvent(any(), any(), any(), any())).willReturn(Mono.empty());

        // when
        notificationService.sendNotificationEmail(notificationDto);

        // then
        then(javaMailSender).should().send(any(MimeMessage.class));
        then(gaService).should().sendGaEvent(any(), eq("1"), eq("email_sent"), any());
    }

    @Test
    @DisplayName("이메일 발송 실패 - GA 실패 이벤트 호출 후 MessagingException 발생")
    void sendNotificationEmail_fail() throws MessagingException {
        // given
        MimeMessage mimeMessage = new MimeMessage((Session) null);
        given(javaMailSender.createMimeMessage()).willReturn(mimeMessage);
        given(templateEngine.process(any(String.class), any(Context.class))).willReturn("<html>test</html>");
        given(toDoListRepository.countByIsDoneTrueAndJd_Id(1L)).willReturn(0);
        given(toDoListRepository.countByJd_Id(1L)).willReturn(0);
        given(gaService.sendGaEvent(any(), any(), any(), any())).willReturn(Mono.empty());
        willThrow(new MailSendException("SMTP 연결 실패")).given(javaMailSender).send(any(MimeMessage.class));

        // when & then
        assertThatThrownBy(() -> notificationService.sendNotificationEmail(notificationDto))
                .isInstanceOf(MessagingException.class)
                .hasMessageContaining("이메일 전송 실패");

        then(gaService).should().sendGaEvent(any(), eq("1"), eq("email_send_failed"), any());
    }
}
