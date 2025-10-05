package com.loopone.loopinbe.domain.loop.loop.dto.res;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDate;

@Builder
@Schema(description = "루프의 간략한 정보를 담는 응답 DTO")
public record LoopSimpleResponse (
    Long id,
    String title,
    LocalDate loopDate,
    double progress
){}
