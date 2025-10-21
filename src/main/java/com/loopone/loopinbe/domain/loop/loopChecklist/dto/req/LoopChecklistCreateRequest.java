package com.loopone.loopinbe.domain.loop.loopChecklist.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "체크리스트 생성을 위한 요청 DTO")
public record LoopChecklistCreateRequest(
        @NotBlank
        String content
){}
