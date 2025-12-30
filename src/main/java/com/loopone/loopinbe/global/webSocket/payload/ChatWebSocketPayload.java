package com.loopone.loopinbe.global.webSocket.payload;

import com.loopone.loopinbe.domain.chat.chatMessage.dto.res.ChatMessageResponse;
import com.loopone.loopinbe.domain.chat.chatMessage.entity.type.MessageType;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatWebSocketPayload {
    @Enumerated(EnumType.STRING)
    @Column(length = 12)
    private MessageType messageType;

    private Long memberId;

    private Long chatRoomId;

    // MESSAGE일 때만 존재
    private ChatMessageResponse chatMessageResponse;
    private String content;
    private LocalDateTime lastMessageCreatedAt;

    // READ일 때만 존재
    private Long messageId;

    // READALL일 때만 존재
    private List<Long> readMessageIdList;
}
