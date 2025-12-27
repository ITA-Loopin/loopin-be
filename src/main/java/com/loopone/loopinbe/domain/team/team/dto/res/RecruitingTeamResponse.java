package com.loopone.loopinbe.domain.team.team.dto.res;

import com.loopone.loopinbe.domain.team.team.enums.TeamCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDate;

@Builder
@Schema(description = "모집 중인 팀 루프 목록 응답 DTO")
public record RecruitingTeamResponse(
        @Schema(description = "팀 ID")
        Long teamId,

        @Schema(description = "팀 카테고리")
        TeamCategory category,

        @Schema(description = "팀 이름")
        String name,

        @Schema(description = "팀 목표")
        String goal,

        @Schema(description = "현재 참여 인원 수")
        int currentMemberCount
) {}