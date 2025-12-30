package com.loopone.loopinbe.domain.team.team.dto.res;

import com.loopone.loopinbe.domain.team.team.enums.TeamCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
@Schema(description = "내 팀 조회를 위한 응답 DTO")
public record MyTeamResponse(
        @Schema(description = "팀 ID")
        Long teamId,

        @Schema(description = "팀 카테고리")
        TeamCategory category,

        @Schema(description = "팀 이름")
        String name,

        @Schema(description = "팀 목표")
        String goal,

        @Schema(description = "팀 전체 평균 진행률 (0~100)")
        int totalProgress
) {}