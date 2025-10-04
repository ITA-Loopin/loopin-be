package com.loopone.loopinbe.domain.chat.chatMessage.dto;

import com.loopone.loopinbe.domain.chat.chatMessage.entity.ChatMessage;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ChatMessageDto {
    private Long id;
    private Long chatRoomId;
    private Long memberId;
    private String nickname;
    private String profileImageUrl;
    private String content;
    ChatMessage.AuthorType authorType;
    private LocalDateTime createdAt;
}
