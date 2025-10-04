package com.loopone.loopinbe.domain.notification.service;

import com.loopone.loopinbe.domain.account.auth.currentUser.CurrentUserDto;
import com.loopone.loopinbe.domain.notification.dto.req.NotificationRequest;
import com.loopone.loopinbe.domain.notification.dto.res.NotificationResponse;
import com.loopone.loopinbe.global.common.response.PageResponse;
import org.springframework.data.domain.Pageable;

public interface NotificationService {
    // Kafka 수신 메시지 저장 & FCM 전송
    void createAndNotifyFromMessage(String message, String title);

    // 알림 목록 조회
    PageResponse<NotificationResponse> getNotifications(Pageable pageable, CurrentUserDto currentUser);

    // 알림 읽음 처리
    void markAsRead(NotificationRequest notificationRequest, CurrentUserDto currentUser);
}
