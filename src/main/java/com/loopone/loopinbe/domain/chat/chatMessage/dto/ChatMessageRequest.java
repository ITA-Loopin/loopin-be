package com.loopone.loopinbe.domain.chat.chatMessage.dto;

import com.loopone.loopinbe.domain.chat.chatMessage.entity.type.MessageType;

public record ChatMessageRequest(
        String message,
        MessageType type
) {
}
