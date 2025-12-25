package com.loopone.loopinbe.domain.loop.loopReport.dto;

import com.loopone.loopinbe.domain.loop.loopReport.enums.DetailReportState;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record WeekReportDto(
    DetailReportState detailReportState,
    Long weekAvgPercent,
    Map<LocalDate, Long> weekCard,
    ProgressLoopDto goodProgressLoopDto,
    ProgressLoopDto badProgressLoopDto
) {}
