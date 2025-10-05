package com.loopone.loopinbe.domain.loop.loopChecklist.dto.res;

import lombok.Builder;

@Builder
public record LoopChecklistResponse (
    Long id,
    String content,
    Boolean completed
){}
