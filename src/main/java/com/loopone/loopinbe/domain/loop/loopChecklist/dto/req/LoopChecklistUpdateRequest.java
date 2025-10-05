package com.loopone.loopinbe.domain.loop.loopChecklist.dto.req;

public record LoopChecklistUpdateRequest(
        String content,
        Boolean completed
){}
