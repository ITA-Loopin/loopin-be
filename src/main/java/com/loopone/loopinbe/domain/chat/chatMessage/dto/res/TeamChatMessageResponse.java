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
public class TeamChatMessageResponse {
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
    private List<ChatAttachmentResponse> attachments;
    @Schema(description = "본인 메시지 여부")
    private Boolean isMine;
    @Schema(description = "메시지 생성 시각")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant createdAt;
}
