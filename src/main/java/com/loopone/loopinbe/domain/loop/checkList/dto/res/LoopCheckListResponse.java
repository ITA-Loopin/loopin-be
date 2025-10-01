package com.loopone.loopinbe.domain.loop.checkList.dto.res;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class LoopCheckListResponse {
    private Long id;
    private Long loopId;
    private String content;
    private String dDay;
    private Boolean checked;
    private LocalDateTime createdAt;
}
