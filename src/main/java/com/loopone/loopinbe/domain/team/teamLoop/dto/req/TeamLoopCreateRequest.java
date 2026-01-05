package com.loopone.loopinbe.domain.team.teamLoop.dto.req;

import com.loopone.loopinbe.domain.loop.loop.enums.RepeatType;
import com.loopone.loopinbe.domain.team.teamLoop.enums.TeamLoopImportance;
import com.loopone.loopinbe.domain.team.teamLoop.enums.TeamLoopType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@Schema(description = "팀 루프 생성을 위한 요청 DTO")
public record TeamLoopCreateRequest(
        @Schema(description = "루프 제목")
        @NotBlank(message = "제목은 필수입니다.")
        String title,

        @Schema(description = "루프 내용")
        String content,

        @Schema(description = "반복 규칙 (NONE, WEEKLY, MONTHLY, YEARLY)")
        @NotNull(message = "반복 유형은 필수입니다.")
        RepeatType scheduleType,

        @Schema(description = "scheduleType이 NONE일 때 사용할 특정 날짜")
        LocalDate specificDate,

        @Schema(description = "scheduleType이 WEEKLY일 때 사용할 요일 목록")
        List<DayOfWeek> daysOfWeek,

        @Schema(description = "반복 시작일 (WEEKLY, MONTHLY, YEARLY일 때 사용, 기본값은 당일로 설정)")
        LocalDate startDate,

        @Schema(description = "반복 종료일 (WEEKLY, MONTHLY, YEARLY일 때 사용, null이면 5년 후로 설정)")
        LocalDate endDate,

        @Schema(description = "각 루프에 포함될 체크리스트 내용 목록")
        List<String> checklists,

        @Schema(description = "루프 유형 (COMMON: 공통, INDIVIDUAL: 개인)")
        @NotNull(message = "루프 유형은 필수입니다.")
        TeamLoopType type,

        @Schema(description = "중요도 (HIGH, MEDIUM, LOW)")
        @NotNull(message = "중요도는 필수입니다.")
        TeamLoopImportance importance,

        @Schema(description = "개인 루프일 때 할당할 팀원 ID 목록")
        List<Long> targetMemberIds
) {}
