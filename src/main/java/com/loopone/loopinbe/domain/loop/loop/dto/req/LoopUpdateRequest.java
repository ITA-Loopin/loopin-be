package com.loopone.loopinbe.domain.loop.loop.dto.req;

import java.time.LocalDate;

public record LoopUpdateRequest(
        String title,
        LocalDate loopDate,
        String content
) {}