package com.loopone.loopinbe.domain.loop.loopChecklist.dto.req;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoopChecklistRequest {
    private Long loopId;
    private String content;
    private Boolean completed;
}
