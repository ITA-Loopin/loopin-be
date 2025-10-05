package com.loopone.loopinbe.domain.loop.loopChecklist.dto.req;

import jakarta.validation.constraints.NotBlank;

public record LoopChecklistCreateRequest(
        @NotBlank
        String content
){}
