package com.loopone.loopinbe.domain.chat.chatMessage.dto.req;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AttachmentRequest(
        @NotNull(message = "clientMessageId는 필수입니다.")
        UUID clientMessageId
) {}
