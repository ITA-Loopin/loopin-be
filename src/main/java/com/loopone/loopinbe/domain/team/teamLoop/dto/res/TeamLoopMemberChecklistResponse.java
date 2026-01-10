package com.loopone.loopinbe.domain.team.teamLoop.dto.res;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.util.List;

@Builder
@Schema(description = "팀원 체크리스트 현황 응답 DTO")
public record TeamLoopMemberChecklistResponse(
        Long memberId,

        @Schema(description = "팀원 닉네임")
        String nickname,

        @Schema(description = "팀원의 진행률")
        double progress,

        @Schema(description = "체크리스트 목록")
        List<TeamLoopChecklistResponse> checklists
) {}