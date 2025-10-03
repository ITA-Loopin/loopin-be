package com.loopone.loopinbe.domain.loop.loopChecklist.dto.req;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public record LoopChecklistCreateRequest(
        @NotBlank
        String content
){}
