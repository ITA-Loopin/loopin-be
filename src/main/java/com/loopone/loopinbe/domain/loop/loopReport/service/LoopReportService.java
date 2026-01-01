package com.loopone.loopinbe.domain.loop.loopReport.service;

import com.loopone.loopinbe.domain.account.auth.currentUser.CurrentUserDto;
import com.loopone.loopinbe.domain.loop.loopReport.dto.res.LoopReportResponse;

public interface LoopReportService {
    // 루프 리포트 조회
    LoopReportResponse getLoopReport(CurrentUserDto currentUser);
}
