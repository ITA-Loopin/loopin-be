package com.loopone.loopinbe.domain.notification.converter;

import com.loopone.loopinbe.domain.notification.dto.res.NotificationResponse;
import com.loopone.loopinbe.domain.notification.entity.Notification;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface NotificationConverter {
    // ---------- Notification -> NotificationResponse ----------
    @Mapping(target = "id", source = "id")
    @Mapping(target = "senderId", source = "senderId")
    @Mapping(target = "senderNickname", source = "senderNickname")
    @Mapping(target = "senderProfileUrl", source = "senderProfileUrl")
    @Mapping(target = "receiverId", source = "receiverId")
    @Mapping(target = "objectId", source = "objectId")
    @Mapping(target = "content", source = "content")
    @Mapping(target = "isRead", source = "isRead")
    @Mapping(target = "targetObject", source = "targetObject")
    @Mapping(target = "createdAt", source = "createdAt")
    NotificationResponse toNotificationResponse(Notification notification);
}
