package com.loopone.loopinbe.domain.loop.loop.dto.res;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.util.List;

@Builder
@Schema(description = "특정 날짜의 루프 요약 정보를 담는 응답 DTO")
public record DailyLoopsResponse(
        @Schema(description = "해당 날짜의 전체 루프 진행률 (완료된 루프 수 / 전체 루프 수 * 100)")
        double totalProgress,
        @Schema(description = "해당 날짜의 루프 리스트")
        List<LoopSimpleResponse> loops
) {}