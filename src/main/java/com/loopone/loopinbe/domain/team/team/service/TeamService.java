package com.loopone.loopinbe.domain.team.team.service;


import com.loopone.loopinbe.domain.account.auth.currentUser.CurrentUserDto;
import com.loopone.loopinbe.domain.team.team.dto.req.TeamCreateRequest;
import com.loopone.loopinbe.domain.team.team.dto.res.MyTeamResponse;
import com.loopone.loopinbe.domain.team.team.dto.res.RecruitingTeamResponse;
import com.loopone.loopinbe.domain.team.team.dto.res.TeamDetailResponse;

import java.util.List;

public interface TeamService {
    Long createTeam(TeamCreateRequest request, CurrentUserDto currentUser);
    List<MyTeamResponse> getMyTeams(CurrentUserDto currentUser);
    List<RecruitingTeamResponse> getRecruitingTeams(CurrentUserDto currentUser);
    TeamDetailResponse getTeamDetails(Long teamId, CurrentUserDto currentUser);
}