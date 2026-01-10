package com.loopone.loopinbe.domain.team.teamLoop.dto.res;

import com.loopone.loopinbe.domain.team.teamLoop.enums.TeamLoopImportance;
import com.loopone.loopinbe.domain.team.teamLoop.enums.TeamLoopStatus;
import com.loopone.loopinbe.domain.team.teamLoop.enums.TeamLoopType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDate;
import java.util.List;

@Builder
@Schema(description = "팀 전체 팀 루프 상세 조회 응답 DTO")
public record TeamLoopAllDetailResponse(
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

        @Schema(description = "팀의 루프 상태 (시작전/진행중/완료됨)")
        TeamLoopStatus status,

        @Schema(description = "팀 전체 진행률")
        double teamProgress,

        @Schema(description = "전체 체크리스트 개수")
        int totalChecklistCount,

        @Schema(description = "체크리스트 목록")
        List<ChecklistInfo> checklists,

        @Schema(description = "팀원 진행 상황")
        List<MemberProgress> memberProgresses
) {
    @Builder
    @Schema(description = "체크리스트 정보")
    public record ChecklistInfo(
            Long checklistId,

            @Schema(description = "체크리스트 내용")
            String content
    ) {}

    @Builder
    @Schema(description = "팀원 진행 상황")
    public record MemberProgress(
            Long memberId,

            @Schema(description = "팀원 닉네임")
            String nickname,

            @Schema(description = "팀원의 루프 상태 (시작전/진행중/완료됨)")
            TeamLoopStatus status,

            @Schema(description = "팀원의 진행률")
            double progress
    ) {}
}