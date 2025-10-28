package com.loopone.loopinbe.domain.chat.chatMessage.dto;

import com.loopone.loopinbe.domain.chat.chatMessage.entity.ChatMessage;
import com.loopone.loopinbe.domain.loop.loop.dto.req.LoopCreateRequest;

import java.time.LocalDateTime;
import java.util.List;

public record ChatMessageSavedResult(
        Long chatRoomId,
        Long memberId,
        Long messageId,
        String content,
        List<LoopCreateRequest> recommendations,
        ChatMessage.AuthorType authorType,
        LocalDateTime createdAt,
        boolean isBotRoom
) {
}
