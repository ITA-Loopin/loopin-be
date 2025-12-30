package com.loopone.loopinbe.domain.team.teamLoop.service;

import com.loopone.loopinbe.domain.account.auth.currentUser.CurrentUserDto;
import com.loopone.loopinbe.domain.team.teamLoop.dto.res.TeamLoopListResponse;

import java.util.List;

public interface TeamLoopService {
    List<TeamLoopListResponse> getTeamLoops(Long teamId, CurrentUserDto currentUser);
}
