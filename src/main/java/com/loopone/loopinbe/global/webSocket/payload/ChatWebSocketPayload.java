package com.loopone.loopinbe.global.webSocket.payload;

import com.loopone.loopinbe.domain.chat.chatMessage.dto.res.AiChatMessageResponse;
import com.loopone.loopinbe.domain.chat.chatMessage.dto.res.TeamChatMessageResponse;
import com.loopone.loopinbe.domain.chat.chatMessage.entity.type.MessageType;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatWebSocketPayload {
    private MessageType messageType;
    // 어떤 채팅방에 브로드캐스트할지 식별용 (MESSAGE(파일) / READ_UP_TO 공통)
    private Long chatRoomId;

    // MESSAGE일 때만 존재
    private UUID clientMessageId;     // UUID (멱등키, UNIQUE)
    private TeamChatMessageResponse teamChatMessageResponse;

    // READ_UP_TO일 때만 존재
    private Long memberId;
    private Instant lastReadAt;

    // SET_NOTICE일 때만 존재
    private String noticeMessageId;
    private String noticeMessageContent;

    // DELETE일 때만 존재
    private String deleteId;
}
