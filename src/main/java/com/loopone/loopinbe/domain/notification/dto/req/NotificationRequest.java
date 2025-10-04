package com.loopone.loopinbe.domain.notification.dto.req;

import jakarta.persistence.Column;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

public record NotificationRequest(
        @Column(columnDefinition = "jsonb")
        List<Long> notificationIdList
) {}
