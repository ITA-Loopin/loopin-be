package com.loopone.loopinbe.domain.team.team.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.util.List;

@Builder
@Schema(description = "팀 순서 변경 요청 DTO")
public record TeamOrderUpdateRequest(
        @Schema(description = "변경할 팀 ID 목록 (순서대로 정렬)", example = "[1, 3, 2, 5]")
        List<Long> teamIds
) {}