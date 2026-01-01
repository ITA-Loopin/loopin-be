package com.loopone.loopinbe.domain.chat.chatMessage.dto.req;

import com.loopone.loopinbe.domain.chat.chatMessage.entity.type.MessageType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ChatMessageRequest(
        @NotBlank(message = "메세지 내용은 필수입니다.")
        String content,

        @NotNull(message = "clientMessageId는 필수입니다.")
        UUID clientMessageId,

        @NotNull(message = "messageType 필수입니다.")
        MessageType messageType
) {}
