package com.loopone.loopinbe.domain.fcm.dto.req;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

public record FcmTokenRequest(
        @NotNull(message = "FCM Token은 필수입니다.")
        String fcmToken
) {}
