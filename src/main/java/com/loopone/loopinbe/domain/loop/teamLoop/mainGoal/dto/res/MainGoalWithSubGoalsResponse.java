package com.loopone.loopinbe.domain.loop.teamLoop.mainGoal.dto.res;

import com.loopone.loopinbe.domain.loop.teamLoop.subGoal.dto.res.SubGoalResponse;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class MainGoalWithSubGoalsResponse {
    private MainGoalResponse mainGoal;
    private List<SubGoalResponse> subGoals;
}
