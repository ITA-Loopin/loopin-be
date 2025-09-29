package com.loopone.loopinbe.domain.loop.teamLoop.mainGoal.dto.req;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MainGoalRequest {
    private String content;
    private LocalDate deadline;
    private Boolean checked;
}
