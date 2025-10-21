package com.loopone.loopinbe.domain.loop.loop.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

@Schema(description = "루프 생성을 위한 요청 DTO")
public record LoopCreateRequest (
    @NotBlank String title,
    String content,
    @NotEmpty List<DayOfWeek> daysOfWeek,
    @NotNull(message = "시작일은 반드시 지정해야 합니다.") LocalDate startDate,
    LocalDate endDate,
    List<String> checklists //추가할 체크리스트
){}
