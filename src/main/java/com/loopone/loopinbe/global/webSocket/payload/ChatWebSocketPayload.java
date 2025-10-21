package com.loopone.loopinbe.global.webSocket.payload;

import com.loopone.loopinbe.domain.chat.chatMessage.dto.ChatMessageDto;
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
    @Column(length = 7)
    private MessageType messageType;
    public enum MessageType {
        MESSAGE,
        READ,
        READALL,
        PING
    }

    private Long memberId;

    private Long chatRoomId;

    // MESSAGE일 때만 존재
    private ChatMessageDto chatMessageDto;
    private String content;
    private LocalDateTime lastMessageCreatedAt;

    // READ일 때만 존재
    private Long messageId;

    // READALL일 때만 존재
    private List<Long> readMessageIdList;
}
