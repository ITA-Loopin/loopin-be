package com.loopone.loopinbe.domain.loop.loop.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;

@Schema(description = "단일 루프의 수정을 위한 요청 DTO")
public record LoopUpdateRequest(
        String title,
        String content,
        LocalDate loopDate,
        List<String> checklists
) {}

