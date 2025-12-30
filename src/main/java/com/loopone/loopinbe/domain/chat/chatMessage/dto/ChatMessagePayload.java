package com.loopone.loopinbe.domain.chat.chatMessage.dto;

import com.loopone.loopinbe.domain.chat.chatMessage.entity.ChatMessage;
import com.loopone.loopinbe.domain.loop.loop.dto.req.LoopCreateRequest;

import java.time.Instant;
import java.util.List;

public record ChatMessagePayload(
        String id,   // UUID
        String clientMessageId, // UUID (멱등키, UNIQUE)
        Long chatRoomId,
        Long memberId,
        String content,
        List<String> imageUrls,
        List<LoopCreateRequest> recommendations,
        ChatMessage.AuthorType authorType,
        boolean isBotRoom,
        Instant createdAt,
        Instant modifiedAt
) {}
