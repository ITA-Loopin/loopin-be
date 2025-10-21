package com.loopone.loopinbe.domain.loop.loopChecklist.dto.res;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
@Schema(description = "체크리스트 정보를 담는 응답 DTO")
public record LoopChecklistResponse (
    Long id,
    String content,
    Boolean completed
){}
