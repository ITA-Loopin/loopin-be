package com.loopone.loopinbe.domain.loop.loopReport.dto;

import com.loopone.loopinbe.domain.loop.loopReport.enums.DetailReportState;
import com.loopone.loopinbe.domain.loop.loopReport.enums.ReportState;

import java.time.LocalDate;
import java.util.Map;

public record MonthReportDto(
    DetailReportState detailReportState,
    Map<LocalDate, Long> monthCard,
    ProgressLoopDto goodProgressLoopDto,
    ProgressLoopDto badProgressLoopDto
) {}
