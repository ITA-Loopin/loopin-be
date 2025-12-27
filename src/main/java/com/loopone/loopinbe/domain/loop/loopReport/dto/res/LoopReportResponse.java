package com.loopone.loopinbe.domain.loop.loopReport.dto.res;

import com.loopone.loopinbe.domain.loop.loopReport.dto.MonthReportDto;
import com.loopone.loopinbe.domain.loop.loopReport.dto.WeekReportDto;
import com.loopone.loopinbe.domain.loop.loopReport.enums.ReportState;

import java.util.List;

public record LoopReportResponse(
    ReportState loopReportState,
    String reportStateMessage,
    Long sevenDayDoneCount,
    Long sevenDayTotalCount,
    Long tenDayAvgPercent,
    WeekReportDto weekReportDto,
    MonthReportDto monthReportDto
) {}
