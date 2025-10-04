package com.loopone.loopinbe.domain.notification.entity;

import lombok.Data;
import lombok.Getter;

@Data
public class NotificationPage {
    // 기본 page, size
    private int page = 0;
    private int size = 20;
    @Getter
    private static final int maxPageSize = 20;
}
