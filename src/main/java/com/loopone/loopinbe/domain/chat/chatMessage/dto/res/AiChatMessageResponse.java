package com.loopone.loopinbe.domain.chat.chatMessage.dto.res;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.loopone.loopinbe.domain.chat.chatMessage.entity.ChatMessage;
import com.loopone.loopinbe.domain.loop.loop.dto.req.LoopCreateRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiChatMessageResponse {
    @Schema(description = "메시지 ID")
    private String id;
    @Schema(description = "멤버 ID")
    private Long memberId;
    @Schema(description = "멤버 닉네임")
    private String nickname;
    @Schema(description = "멤버 프로필 이미지 URL")
    private String profileImageUrl;
    @Schema(description = "메시지 내용")
    private String content;
    @Schema(description = "첨부물 리스트")
    private List<LoopCreateRequest> recommendations;
    @Schema(description = "루프 규칙 ID")
    private Long loopRuleId;
    @Schema(description = "삭제할 메시지 ID")
    private String deleteMessageId;
    @Schema(description = "메시지 작성자 타입")
    private ChatMessage.AuthorType authorType;
    @Schema(description = "루프 수정 완료하기 버튼 여부(false인 경우 루프 수정하기 버튼)")
    private Boolean callUpdateLoop;
    @Schema(description = "메시지 생성 시각")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant createdAt;
}
