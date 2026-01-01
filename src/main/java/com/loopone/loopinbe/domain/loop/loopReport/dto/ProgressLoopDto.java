package com.loopone.loopinbe.domain.loop.loopReport.dto;

import com.loopone.loopinbe.domain.loop.loop.dto.res.LoopDetailResponse;

public record ProgressLoopDto(
    String loopTitle,
    LoopDetailResponse.LoopRuleDTO loopRule,
    Long loopAchievePercent,
    String message
) {}
