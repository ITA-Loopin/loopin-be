package com.loopone.loopinbe.domain.team.teamLoop.service;

import com.loopone.loopinbe.domain.account.auth.currentUser.CurrentUserDto;
import com.loopone.loopinbe.domain.team.teamLoop.dto.req.TeamLoopCreateRequest;
import com.loopone.loopinbe.domain.team.teamLoop.dto.res.TeamLoopListResponse;

import java.time.LocalDate;
import java.util.List;

public interface TeamLoopService {
    List<TeamLoopListResponse> getTeamLoops(Long teamId, LocalDate targetDate, CurrentUserDto currentUser);
    Long createTeamLoop(Long teamId, TeamLoopCreateRequest request, CurrentUserDto currentUser);
}
