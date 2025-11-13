package com.loopone.loopinbe.domain.loop.loop.dto.res;

import com.loopone.loopinbe.domain.loop.loop.enums.RepeatType;
import com.loopone.loopinbe.domain.loop.loopChecklist.dto.res.LoopChecklistResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

@Builder
@Schema(description = "루프의 상세 정보를 담는 응답 DTO")
public record LoopDetailResponse (
        @Schema(description = "루프 ID")
        Long id,

        @Schema(description = "루프 제목")
        String title,

        @Schema(description = "루프 내용")
        String content,

        @Schema(description = "루프 날짜")
        LocalDate loopDate,

        @Schema(description = "해당 루프의 체크리스트 진행률")
        double progress,

        @Schema(description = "해당 루프에 속한 체크리스트 목록")
        List<LoopChecklistResponse> checklists,

        @Schema(description = "이 루프가 속한 반복 규칙 정보 (단일 루프일 경우 null)")
        LoopRuleDTO loopRule

){
    @Builder
    @Schema(description = "반복 규칙 정보")
    public record LoopRuleDTO(
            Long ruleId,
            RepeatType scheduleType,
            List<DayOfWeek> daysOfWeek,
            LocalDate startDate,
            LocalDate endDate
    ) {}
}
