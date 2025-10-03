package com.loopone.loopinbe.domain.loop.loop.dto.res;

import com.loopone.loopinbe.domain.loop.checkList.dto.res.LoopCheckListResponse;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Builder
public record LoopDetailResponse (
    Long id,
    String title,
    String content,
    LocalDate loopDate,
    double progress,
    List<LoopCheckListResponse> checklists //체크리스트 목록
){}
