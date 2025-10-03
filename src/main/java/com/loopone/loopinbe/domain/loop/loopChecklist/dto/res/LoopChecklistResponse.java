package com.loopone.loopinbe.domain.loop.loopChecklist.dto.res;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LoopChecklistResponse {
    private Long id;
    private Long loopId;
    private String content;
    private Boolean completed;
}
