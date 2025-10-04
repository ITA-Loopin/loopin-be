package com.loopone.loopinbe.domain.chat.chatMessage.dto;

import com.loopone.loopinbe.domain.chat.chatMessage.entity.ChatMessage;

import java.time.Instant;
import java.time.LocalDateTime;

public record ChatInboundMessagePayload(
        String messageKey,   // UUID/ULID (멱등키, UNIQUE)
        Long chatRoomId,
        Long memberId,
        String content,
        ChatMessage.AuthorType authorType,
        LocalDateTime createdAt
) {}
