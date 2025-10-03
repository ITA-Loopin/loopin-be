package com.loopone.loopinbe.domain.loop.loop.dto.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

public record LoopCreateRequest (
    @NotBlank
    String title,

    @NotNull
    LocalDate loopDate,

    String content,

    List<String> checklists //추가할 체크리스트
){}
