package com.loopone.loopinbe.domain.loop.teamLoop.subGoal.service;

import com.loopone.loopinbe.domain.account.auth.currentUser.CurrentUserDto;
import com.loopone.loopinbe.domain.loop.teamLoop.subGoal.dto.req.SubGoalRequest;

public interface SubGoalService {
    // 하위목표 생성
    void addSubGoal(SubGoalRequest subGoalRequest, CurrentUserDto currentUser);

    // 하위목표 수정
    void updateSubGoal(Long subGoalId, SubGoalRequest subGoalRequest, CurrentUserDto currentUser);

    // 하위목표 삭제
    void deleteSubGoal(Long subGoalId, CurrentUserDto currentUser);

    // 상위목표 내의 하위목표 전체 삭제
    void deleteAllSubGoal(Long mainGoalId);
}
