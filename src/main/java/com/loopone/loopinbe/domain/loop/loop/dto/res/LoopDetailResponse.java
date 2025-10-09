package com.loopone.loopinbe.domain.loop.loop.dto.res;

import com.loopone.loopinbe.domain.loop.loopChecklist.dto.res.LoopChecklistResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDate;
import java.util.List;

@Builder
@Schema(description = "루프의 상세 정보를 담는 응답 DTO")
public record LoopDetailResponse (
        Long id,
        String title,
        String content,
        LocalDate loopDate,
        double progress, //해당 루프의 체크리스트 진행률
        List<LoopChecklistResponse> checklists //체크리스트 목록
){}
