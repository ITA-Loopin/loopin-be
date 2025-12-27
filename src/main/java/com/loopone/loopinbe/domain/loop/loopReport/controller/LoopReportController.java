package com.loopone.loopinbe.domain.loop.loopReport.controller;

import com.loopone.loopinbe.domain.account.auth.currentUser.CurrentUser;
import com.loopone.loopinbe.domain.account.auth.currentUser.CurrentUserDto;
import com.loopone.loopinbe.domain.loop.loop.dto.res.LoopDetailResponse;
import com.loopone.loopinbe.domain.loop.loopReport.dto.res.LoopReportResponse;
import com.loopone.loopinbe.domain.loop.loopReport.service.LoopReportService;
import com.loopone.loopinbe.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value="/rest-api/v1/report")
@RequiredArgsConstructor
@Tag(name = "LoopReport", description = "루프 리포트 API")
public class LoopReportController {
    private final LoopReportService loopReportService;

    // 루프 리포트 조회
    @GetMapping
    @Operation(summary = "루프 리포트 조회", description = "루프 리포트를 조회합니다.")
    public ApiResponse<LoopReportResponse> getLoopReport(@CurrentUser CurrentUserDto currentUser) {
        return ApiResponse.success(loopReportService.getLoopReport(currentUser));
    }
}
