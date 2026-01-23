package com.loopone.loopinbe.domain.chat.chatRoom.dto.res;

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
    @Schema(description = "루프 수정 완료하기 버튼 여부(false인 경우 루프 수정하기 버튼)")
    private boolean isCallUpdateLoop;
    @Schema(description = "공지 메시지 내용")
    private String noticeMessageContent;
    @Schema(description = "최근 메시지 생성 시각")
    private Instant lastMessageAt;
    @Schema(description = "마지막으로 메시지 읽은 시각")
    private Instant lastReadAt;
}
