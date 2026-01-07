package com.loopone.loopinbe.domain.team.teamLoop.dto.res;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
@Schema(description = "팀 루프 체크리스트 응답 DTO")
public record TeamLoopChecklistResponse(
        @Schema(description = "체크리스트 ID")
        Long id,

        @Schema(description = "체크리스트 내용")
        String content,

        @Schema(description = "체크 여부 (true: 완료, false: 미완료)")
        boolean isChecked
) {}