package com.loopone.loopinbe.domain.loop.loopChecklist.dto.res;

import lombok.Builder;
import lombok.Data;

@Builder
public record LoopChecklistResponse (
    Long id,
    String content,
    Boolean completed
){}
