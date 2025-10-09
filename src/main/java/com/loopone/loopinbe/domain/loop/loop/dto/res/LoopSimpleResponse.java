package com.loopone.loopinbe.domain.loop.loop.dto.res;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDate;

@Builder
@Schema(description = "루프의 요약 정보를 담는 응답 DTO")
public record LoopSimpleResponse (
    Long id,
    String title,
    LocalDate loopDate,
    //double progress, //해당 루프의 진행률
    boolean completed,
    int totalChecklists, //전체 체크리스트 개수
    int compleatedChecklists //완료된 체크리스트 개수
){}
