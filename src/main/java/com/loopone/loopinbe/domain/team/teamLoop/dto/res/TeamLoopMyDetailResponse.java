package com.loopone.loopinbe.domain.team.teamLoop.dto.res;

import com.loopone.loopinbe.domain.team.teamLoop.enums.TeamLoopImportance;
import com.loopone.loopinbe.domain.team.teamLoop.enums.TeamLoopStatus;
import com.loopone.loopinbe.domain.team.teamLoop.enums.TeamLoopType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDate;
import java.util.List;

@Builder
@Schema(description = "팀 루프 상세 조회 응답 DTO")
public record TeamLoopMyDetailResponse(
        Long id,

        @Schema(description = "루프 제목")
        String title,

        @Schema(description = "루프 날짜")
        LocalDate loopDate,

        @Schema(description = "루프 유형 (COMMON/INDIVIDUAL)")
        TeamLoopType type,

        @Schema(description = "반복 주기 문자열 (예: '매주 월금', '매월 1일', '없음')")
        String repeatCycle,

        @Schema(description = "루프 중요도")
        TeamLoopImportance importance,

        @Schema(description = "나의 루프 상태 (시작전/진행중/완료됨)")
        TeamLoopStatus status,

        @Schema(description = "나의 진행률")
        double personalProgress,

        @Schema(description = "전체 체크리스트 개수")
        int totalChecklistCount,

        @Schema(description = "체크리스트 목록")
        List<ChecklistItem> checklists
) {
    @Builder
    @Schema(description = "체크리스트 항목")
    public record ChecklistItem(
            Long checklistId,

            @Schema(description = "체크리스트 내용")
            String content,

            @Schema(description = "체크 완료 여부")
            boolean isCompleted
    ) {}
}