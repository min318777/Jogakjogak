package com.zb.jogakjogak.notification.service;

import com.zb.jogakjogak.ga.service.GaMeasurementProtocolService;
import com.zb.jogakjogak.jobdescription.entity.JD;
import com.zb.jogakjogak.jobdescription.repository.ToDoListRepository;
import com.zb.jogakjogak.member.entity.Member;
import com.zb.jogakjogak.notification.dto.NotificationDto;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;

/**
 * @Retryable이 인터페이스(NotificationEmailSender) 참조로 호출되어도
 * 실제 프록시를 통해 재시도가 발생하는지 검증한다.
 */
@SpringJUnitConfig
@ContextConfiguration(classes = NotificationServiceRetryTest.RetryTestConfig.class)
class NotificationServiceRetryTest {

    @EnableRetry
    @Configuration
    static class RetryTestConfig {
        @Bean
        NotificationService notificationService(JavaMailSender javaMailSender,
                                                  SpringTemplateEngine templateEngine,
                                                  ToDoListRepository toDoListRepository,
                                                  GaMeasurementProtocolService gaService) {
            return new NotificationService(javaMailSender, templateEngine, toDoListRepository, gaService);
        }
    }

    @Autowired
    private NotificationEmailSender emailSender;

    @MockBean
    private JavaMailSender javaMailSender;

    @MockBean
    private SpringTemplateEngine templateEngine;

    @MockBean
    private ToDoListRepository toDoListRepository;

    @MockBean
    private GaMeasurementProtocolService gaService;

    @Test
    @DisplayName("메일 전송이 계속 실패하면 3번 재시도 후 MessagingException을 던진다")
    void sendNotificationEmail_retriesThreeTimesThenFails() throws MessagingException {
        // given
        Member member = Member.builder().id(1L).email("test@example.com").nickname("테스터").build();
        JD jd = JD.builder().id(1L).title("백엔드 개발자").companyName("테스트 회사")
                .endedAt(LocalDateTime.now().plusDays(7)).build();
        NotificationDto notificationDto = NotificationDto.builder()
                .member(member)
                .jdList(new java.util.ArrayList<>(List.of(jd)))
                .build();

        given(javaMailSender.createMimeMessage()).willReturn(new MimeMessage((Session) null));
        given(templateEngine.process(any(String.class), any(Context.class))).willReturn("<html>test</html>");
        given(gaService.sendGaEvent(any(), any(), any(), any())).willReturn(Mono.empty());
        willThrow(new MailSendException("SMTP 연결 실패")).given(javaMailSender).send(any(MimeMessage.class));

        // when & then
        assertThatThrownBy(() -> emailSender.sendNotificationEmail(notificationDto))
                .isInstanceOf(MessagingException.class);

        then(javaMailSender).should(org.mockito.Mockito.times(3)).send(any(MimeMessage.class));
    }
}
