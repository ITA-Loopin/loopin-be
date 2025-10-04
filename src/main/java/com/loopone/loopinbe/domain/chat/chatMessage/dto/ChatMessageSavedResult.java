package com.loopone.loopinbe.domain.chat.chatMessage.dto;

import com.loopone.loopinbe.domain.chat.chatMessage.entity.ChatMessage;

import java.time.LocalDateTime;

public record ChatMessageSavedResult(
        Long chatRoomId,
        Long memberId,
        Long messageId,
        String content,
        ChatMessage.AuthorType authorType,
        LocalDateTime createdAt,
        boolean isBotRoom
) {}
