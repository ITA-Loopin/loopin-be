package com.loopone.loopinbe.domain.team.teamLoop.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "체크리스트 생성 요청 DTO")
public record TeamLoopChecklistCreateRequest(
        @Schema(description = "체크리스트 내용")
        @NotBlank(message = "내용은 필수입니다.")
        String content
) {}