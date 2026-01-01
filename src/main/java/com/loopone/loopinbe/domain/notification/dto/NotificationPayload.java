package com.loopone.loopinbe.domain.notification.dto;

import com.loopone.loopinbe.domain.notification.entity.Notification;

public record NotificationPayload(
        Long senderId,
        String senderNickname,
        String senderProfileUrl,
        Long receiverId,
        Long objectId,
        String content,
        Notification.TargetObject targetObject
) {}
