package com.loopone.loopinbe.domain.chat.chatMessage.dto;

import com.loopone.loopinbe.domain.chat.chatMessage.entity.ChatMessage;
import com.loopone.loopinbe.domain.loop.loop.dto.req.LoopCreateRequest;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageDto {
    private Long id;
    private Long chatRoomId;
    private Long memberId;
    private String nickname;
    private String profileImageUrl;
    private String content;
    ChatMessage.AuthorType authorType;
    private List<LoopCreateRequest> recommendations;
    private LocalDateTime createdAt;
}
