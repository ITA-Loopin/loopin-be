package com.loopone.loopinbe.domain.loop.loopChecklist.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "체크리스트 수정을 위한 요청 DTO")
public record LoopChecklistUpdateRequest(
        String content,
        Boolean completed
){}
