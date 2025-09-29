package com.loopone.loopinbe.domain.loop.teamLoop.mainGoal.service;

import com.loopone.loopinbe.domain.account.auth.currentUser.CurrentUserDto;
import com.loopone.loopinbe.domain.loop.teamLoop.mainGoal.dto.req.MainGoalRequest;
import com.loopone.loopinbe.domain.loop.teamLoop.mainGoal.dto.res.MainGoalWithSubGoalsResponse;
import com.loopone.loopinbe.global.common.response.PageResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface MainGoalService {
    // 목표 생성
    void addMainGoal(MainGoalRequest mainGoalRequest, CurrentUserDto currentUser);

    // 목표 전체 리스트 조회
    PageResponse<MainGoalWithSubGoalsResponse> getAllGoal(Pageable pageable, CurrentUserDto currentUser);

    // 목표 수정
    void updateMainGoal(Long mainGoalId, MainGoalRequest mainGoalRequest, CurrentUserDto currentUser);

    // 목표 삭제
    void deleteMainGoal(Long mainGoalId, CurrentUserDto currentUser);
}
