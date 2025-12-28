package com.loopone.loopinbe.domain.notification.serviceImpl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopone.loopinbe.domain.account.auth.currentUser.CurrentUserDto;
import com.loopone.loopinbe.domain.account.member.entity.MemberPage;
import com.loopone.loopinbe.domain.fcm.dto.res.FcmMessageResponse;
import com.loopone.loopinbe.domain.fcm.service.FcmService;
import com.loopone.loopinbe.domain.fcm.service.FcmTokenService;
import com.loopone.loopinbe.domain.notification.converter.NotificationConverter;
import com.loopone.loopinbe.domain.notification.dto.NotificationPayload;
import com.loopone.loopinbe.domain.notification.dto.req.NotificationRequest;
import com.loopone.loopinbe.domain.notification.dto.res.NotificationResponse;
import com.loopone.loopinbe.domain.notification.entity.Notification;
import com.loopone.loopinbe.domain.notification.entity.NotificationPage;
import com.loopone.loopinbe.domain.notification.repository.NotificationRepository;
import com.loopone.loopinbe.domain.notification.service.NotificationService;
import com.loopone.loopinbe.global.common.response.PageResponse;
import com.loopone.loopinbe.global.exception.ReturnCode;
import com.loopone.loopinbe.global.exception.ServiceException;
import com.loopone.loopinbe.global.kafka.event.fcm.FcmEventPublisher;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {
    private final NotificationRepository notificationRepository;
    private final NotificationConverter notificationConverter;
    private final ObjectMapper objectMapper;
    private final FcmTokenService fcmTokenService;
    private final FcmEventPublisher fcmEventPublisher;

    // Kafka 수신 메시지 저장 & FCM 전송
    @Override
    @Transactional
    public void createAndNotifyFromMessage(String message, String title) {
        Notification notification;
        try {
            NotificationPayload payload = objectMapper.readValue(message, NotificationPayload.class);
            notification = notificationConverter.toNotification(payload, title);
        } catch (Exception e) {
            // 역직렬화 실패는 재시도해도 의미 없음 → 비재시도 예외로 래핑해도 좋음
            log.error("Failed to deserialize notification message: {}", message, e);
            throw new IllegalArgumentException("Invalid notification payload", e);
        }
        // 2) 저장
        notificationRepository.save(notification);

        // 3) 커밋 이후 FCM 전송 (DB 롤백 시 푸시 발송 방지)
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    String fcmToken = fcmTokenService.getFcmToken(notification.getReceiverId());
                    if (fcmToken == null) {
                        log.warn("FCM token not found for receiverId: {}", notification.getReceiverId());
                        return;
                    }
                    NotificationResponse notificationResponse = notificationConverter.toNotificationResponse(notification);
                    String bodyJson = objectMapper.writeValueAsString(notificationResponse);
                    String eventId = "notif:" + notification.getId();
                    FcmMessageResponse fcmMessageResponse = FcmMessageResponse.builder()
                            .eventId(eventId)
                            .targetToken(fcmToken)
                            .title(title)
                            .body(bodyJson)
                            .build();
                    fcmEventPublisher.publishFcm(fcmMessageResponse);
                } catch (Exception ex) {
                    log.error("Failed to send FCM after commit. notificationId={}", notification.getId(), ex);
                }
            }
        });
    }

    // 알림 목록 조회
    @Override
    @Transactional
    public PageResponse<NotificationResponse> getNotifications(Pageable pageable, CurrentUserDto currentUser) {
        checkPageSize(pageable.getPageSize());
        Page<Notification> notificationPage = notificationRepository.findByReceiverIdOrderByCreatedAtDesc(currentUser.id(), pageable);
        return PageResponse.of(notificationPage.map(notificationConverter::toNotificationResponse));
    }

    // 알림 읽음 처리
    @Override
    @Transactional
    public void markAsRead(NotificationRequest notificationRequest, CurrentUserDto currentUser) {
        // 본인 알림인지 확인
        List<Long> ids = notificationRequest.notificationIdList();
        List<Notification> notifications = notificationRepository.findAllByIdInAndReceiverId(ids, currentUser.id());
        if (notifications.size() != ids.size()) {
            throw new ServiceException(ReturnCode.NOTIFICATION_NOT_FOUND);
        }
        notifications.forEach(n -> n.setIsRead(true));
        notificationRepository.saveAll(notifications);
    }

    // ----------------- 헬퍼 메서드 -----------------

    // 요청 페이지 수 제한
    private void checkPageSize(int pageSize) {
        int maxPageSize = NotificationPage.getMaxPageSize();
        if (pageSize > maxPageSize) {
            throw new ServiceException(ReturnCode.PAGE_REQUEST_FAIL);
        }
    }
}
