package com.loopone.loopinbe.domain.loop.loop.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "루프 완료 처리를 위한 요청 DTO")
public record LoopCompletionUpdateRequest(
        @Schema(description = "루프 완료 여부 (true: 완료, false: 미완료")
        @NotNull
        boolean completed
) {}
