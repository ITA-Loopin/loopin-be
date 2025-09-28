package com.loopone.loopinbe.domain.notification.service;

import com.letzgo.LetzgoBe.domain.account.auth.currentUser.CurrentUserDto;
import com.letzgo.LetzgoBe.domain.notification.dto.req.NotificationRequest;
import com.letzgo.LetzgoBe.domain.notification.dto.res.NotificationResponse;
import com.letzgo.LetzgoBe.domain.notification.entity.Notification;
import com.letzgo.LetzgoBe.global.common.response.PageResponse;
import org.springframework.data.domain.Pageable;

public interface NotificationService {
    // 알림 생성
    void createNotification(Notification notification);

    // 알림 목록 조회
    PageResponse<NotificationResponse> getNotifications(Pageable pageable, CurrentUserDto currentUser);

    // 알림 읽음 처리
    void markAsRead(NotificationRequest notificationRequest, CurrentUserDto currentUser);

    // Notification을 NotificationDto로 변환
    NotificationResponse convertToNotificationDto(Notification notification);
}
