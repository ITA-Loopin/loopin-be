package com.loopone.loopinbe.domain.team.team.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
@Schema(description = "팀 순서 변경 요청 DTO")
public record TeamOrderUpdateRequest(
        @Schema(description = "이동할 팀 ID")
        Long teamId,

        @Schema(description = "이동할 위치 (0,1,2,3 ... 순서)")
        Integer newPosition
) {}