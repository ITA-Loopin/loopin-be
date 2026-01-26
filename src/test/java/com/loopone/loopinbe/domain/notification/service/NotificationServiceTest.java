package com.loopone.loopinbe.domain.notification.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.loopone.loopinbe.domain.account.auth.currentUser.CurrentUserDto;
import com.loopone.loopinbe.domain.account.member.entity.Member;
import com.loopone.loopinbe.domain.fcm.dto.res.FcmMessageResponse;
import com.loopone.loopinbe.domain.fcm.service.FcmTokenService;
import com.loopone.loopinbe.domain.notification.mapper.NotificationMapperImpl;
import com.loopone.loopinbe.domain.notification.dto.NotificationPayload;
import com.loopone.loopinbe.domain.notification.dto.req.NotificationRequest;
import com.loopone.loopinbe.domain.notification.entity.Notification;
import com.loopone.loopinbe.domain.notification.repository.NotificationRepository;
import com.loopone.loopinbe.domain.notification.serviceImpl.NotificationServiceImpl;
import com.loopone.loopinbe.global.exception.ReturnCode;
import com.loopone.loopinbe.global.exception.ServiceException;
import com.loopone.loopinbe.global.kafka.event.fcm.FcmEventPublisher;
import com.loopone.loopinbe.support.TestContainersConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.transaction.TestTransaction;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.clearInvocations;

@DataJpaTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "testcontainers.redis.enabled=true"
})
@Import({
        TestContainersConfig.class,
        NotificationServiceImpl.class,
        NotificationMapperImpl.class,
        NotificationServiceTest.TestConfig.class
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class NotificationServiceTest {

    // ===== Real Repositories =====
    @Autowired NotificationRepository notificationRepository;

    // ===== SUT =====
    @Autowired NotificationServiceImpl notificationService;

    @Autowired ObjectMapper objectMapper;

    // ===== External boundaries (mock) =====
    @MockitoBean FcmTokenService fcmTokenService;
    @MockitoBean FcmEventPublisher fcmEventPublisher;

    @AfterEach
    void cleanup() {
        notificationRepository.deleteAll();
    }

    @BeforeEach
    void resetMocks() {
        clearInvocations(fcmEventPublisher, fcmTokenService);
    }

    // ===== Helpers =====
    private Notification persistNotification(Long receiverId, String content, Notification.TargetObject targetObject) {
        Notification n = Notification.builder()
                .senderId(2L)
                .senderNickname("sender")
                .senderProfileUrl("http://img")
                .receiverId(receiverId)
                .objectId(10L)
                .title("title")
                .content(content)
                .isRead(false)
                .targetObject(targetObject)
                .build();
        return notificationRepository.saveAndFlush(n);
    }

    private CurrentUserDto currentUser(Long id) {
        return new CurrentUserDto(
                id,
                "jun@loop.in",
                null,
                "jun",
                "010-0000-0000",
                Member.Gender.MALE,
                java.time.LocalDate.of(2000, 1, 1),
                null,
                Member.State.NORMAL,
                Member.MemberRole.ROLE_USER,
                Member.OAuthProvider.GOOGLE,
                "provider-id"
        );
    }

    @org.springframework.boot.test.context.TestConfiguration
    static class TestConfig {
        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper().registerModule(new JavaTimeModule());
        }
    }

    // =========================================================
    // createAndNotifyFromMessage
    // =========================================================
    @Nested
    class CreateAndNotifyFromMessage {

        @Test
        @DisplayName("성공: 알림 저장 후 커밋 이후 FCM 이벤트 발행")
        void success_saveAndPublishAfterCommit() throws Exception {
            // given
            NotificationPayload payload = new NotificationPayload(
                    2L, "sender", "http://img", 1L,
                    10L, "content-1", Notification.TargetObject.Follow
            );
            String message = objectMapper.writeValueAsString(payload);
            String title = "title-1";
            given(fcmTokenService.getFcmToken(1L)).willReturn("fcm-token-123");

            // when
            notificationService.createAndNotifyFromMessage(message, title);
            assertThat(notificationRepository.findAll()).hasSize(1);

            // commit to trigger afterCommit
            TestTransaction.flagForCommit();
            TestTransaction.end();

            // then
            then(fcmEventPublisher).should().publishFcm(any(FcmMessageResponse.class));
        }

        @Test
        @DisplayName("FCM 토큰이 없으면 publish 하지 않는다")
        void skipPublishWhenTokenMissing() throws Exception {
            // given
            NotificationPayload payload = new NotificationPayload(
                    2L, "sender", "http://img", 1L,
                    10L, "content-2", Notification.TargetObject.Invite
            );
            String message = objectMapper.writeValueAsString(payload);
            given(fcmTokenService.getFcmToken(1L)).willReturn(null);

            // when
            notificationService.createAndNotifyFromMessage(message, "title-2");
            TestTransaction.flagForCommit();
            TestTransaction.end();

            // then
            then(fcmEventPublisher).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("실패: 잘못된 payload면 IllegalArgumentException")
        void fail_invalidPayload() {
            assertThatThrownBy(() -> notificationService.createAndNotifyFromMessage("not-json", "title"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid notification payload");
        }
    }

    // =========================================================
    // getNotifications
    // =========================================================
    @Nested
    class GetNotifications {

        @Test
        @DisplayName("성공: receiverId 기준으로 목록을 조회한다")
        void success_get() {
            // given
            persistNotification(1L, "a", Notification.TargetObject.Follow);
            persistNotification(1L, "b", Notification.TargetObject.Invite);
            persistNotification(2L, "c", Notification.TargetObject.Invite);

            // when
            var result = notificationService.getNotifications(PageRequest.of(0, 20), currentUser(1L));

            // then
            assertThat(result.getContent()).hasSize(2);
        }

        @Test
        @DisplayName("실패: 페이지 사이즈가 최대값을 초과하면 PAGE_REQUEST_FAIL")
        void fail_pageSizeTooLarge() {
            assertThatThrownBy(() -> notificationService.getNotifications(PageRequest.of(0, 21), currentUser(1L)))
                    .isInstanceOf(ServiceException.class)
                    .extracting("returnCode").isEqualTo(ReturnCode.PAGE_REQUEST_FAIL);
        }
    }

    // =========================================================
    // markAsRead
    // =========================================================
    @Nested
    class MarkAsRead {

        @Test
        @DisplayName("성공: 본인 알림은 읽음 처리된다")
        void success_markAsRead() {
            // given
            Notification n1 = persistNotification(1L, "a", Notification.TargetObject.Follow);
            Notification n2 = persistNotification(1L, "b", Notification.TargetObject.Invite);

            // when
            notificationService.markAsRead(new NotificationRequest(List.of(n1.getId(), n2.getId())), currentUser(1L));

            // then
            Notification reloaded1 = notificationRepository.findById(n1.getId()).orElseThrow();
            Notification reloaded2 = notificationRepository.findById(n2.getId()).orElseThrow();
            assertThat(reloaded1.getIsRead()).isTrue();
            assertThat(reloaded2.getIsRead()).isTrue();
        }

        @Test
        @DisplayName("실패: 다른 사용자 알림이 섞이면 NOTIFICATION_NOT_FOUND")
        void fail_whenNotOwned() {
            // given
            Notification mine = persistNotification(1L, "a", Notification.TargetObject.Follow);
            Notification others = persistNotification(2L, "b", Notification.TargetObject.Invite);
            NotificationRequest req = new NotificationRequest(List.of(mine.getId(), others.getId()));

            // when & then
            assertThatThrownBy(() -> notificationService.markAsRead(req, currentUser(1L)))
                    .isInstanceOf(ServiceException.class)
                    .extracting("returnCode").isEqualTo(ReturnCode.NOTIFICATION_NOT_FOUND);
        }
    }
}
