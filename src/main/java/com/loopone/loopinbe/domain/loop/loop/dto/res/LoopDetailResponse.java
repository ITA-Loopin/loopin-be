package com.loopone.loopinbe.domain.loop.loop.dto.res;

import com.loopone.loopinbe.domain.loop.loopChecklist.dto.res.LoopChecklistResponse;
import lombok.Builder;

import java.time.LocalDate;
import java.util.List;

@Builder
public record LoopDetailResponse (
        Long id,
        String title,
        String content,
        LocalDate loopDate,
        double progress,
        List<LoopChecklistResponse> checklists //체크리스트 목록
){}
