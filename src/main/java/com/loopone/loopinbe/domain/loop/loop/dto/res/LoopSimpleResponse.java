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
    //double progress, //해당 루프의 체크리스트 진행률
    boolean completed, //완료 여부
    int totalChecklists, //전체 체크리스트 개수
    int completedChecklists //완료된 체크리스트 개수
){}
