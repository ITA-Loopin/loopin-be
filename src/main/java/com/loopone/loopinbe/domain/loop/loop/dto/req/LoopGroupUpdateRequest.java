package com.loopone.loopinbe.domain.loop.loop.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

@Schema(description = "루프 그룹 전체의 수정을 위한 요청 DTO")
public record LoopGroupUpdateRequest(
        String title,
        String content,
        List<DayOfWeek> daysOfWeek,
        LocalDate startDate,
        LocalDate endDate,
        List<String> checklists
) {}