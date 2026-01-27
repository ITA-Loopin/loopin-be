package com.loopone.loopinbe.domain.chat.chatRoom.dto.res;

import com.loopone.loopinbe.domain.chat.chatRoom.enums.ChatRoomStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ChatRoomResponse {
    @Schema(description = "채팅방 ID")
    private Long id;
    @Schema(description = "팀장 ID")
    private Long ownerId;
    @Schema(description = "채팅방 제목")
    private String title;
    @Schema(description = "루프 선택 여부")
    private boolean loopSelect;
    @Schema(description = """
            채팅방의 상태를 나타냅니다.<br>
            기본은 DEFAULT입니다. (팀 채팅방의 경우 항상 DEFAULT, AI 채팅방의 경우 채팅방 처음 진입 시, RECREATE_LOOP 호출 후)<br>
            CREATE_LOOP 호출 후에 AFTER_CREATE_LOOP가 됩니다.<br>
            AI가 추천해준 루프로 생성, 업데이트 시 BEFORE_CLICK_UPDATE_LOOP가 됩니다.<br>
            BEFORE_UPDATE_LOOP 호출 후 AFTER_CLICK_UPDATE_LOOP가 됩니다.<br>
            UPDATE_LOOP 호출 후 AFTER_CREATE_UPDATE_LOOP가 됩니다.<br>
            """)
    private ChatRoomStatus chatRoomStatus;
    @Schema(description = "공지 메시지 내용")
    private String noticeMessageContent;
    @Schema(description = "최근 메시지 생성 시각")
    private Instant lastMessageAt;
    @Schema(description = "마지막으로 메시지 읽은 시각")
    private Instant lastReadAt;
}
