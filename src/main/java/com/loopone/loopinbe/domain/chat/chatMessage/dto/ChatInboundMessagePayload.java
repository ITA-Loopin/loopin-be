package com.loopone.loopinbe.domain.chat.chatMessage.dto;

import com.loopone.loopinbe.domain.chat.chatMessage.entity.ChatMessage;
import com.loopone.loopinbe.domain.loop.loop.dto.req.LoopCreateRequest;

import java.time.LocalDateTime;
import java.util.List;

public record ChatInboundMessagePayload(
        String messageKey,   // UUID/ULID (멱등키, UNIQUE)
        Long chatRoomId,
        Long memberId,
        String content,
        List<LoopCreateRequest> recommendations,
        ChatMessage.AuthorType authorType,
        LocalDateTime createdAt
) {
}
