package com.loopone.loopinbe.domain.loop.loop.dto.res;

import lombok.Builder;

import java.time.LocalDate;

@Builder
public record LoopSimpleResponse (
    Long id,
    String title,
    LocalDate loopDate,
    double progress
){}
