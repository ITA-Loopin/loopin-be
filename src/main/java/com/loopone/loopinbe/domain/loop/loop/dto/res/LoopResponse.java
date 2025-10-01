package com.loopone.loopinbe.domain.loop.loop.dto.res;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class LoopResponse {
    private Long id;
    private String content;
    private String dDay;
    private Boolean checked;
    private LocalDateTime createdAt;
}
