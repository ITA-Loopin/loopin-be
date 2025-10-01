package com.loopone.loopinbe.domain.loop.checkList.dto.req;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoopCheckListRequest {
    private Long loopId;
    private String content;
    private Boolean completed;
}
