package com.loopone.loopinbe.domain.team.teamLoop.service;

import com.loopone.loopinbe.domain.account.auth.currentUser.CurrentUserDto;
import com.loopone.loopinbe.domain.team.teamLoop.dto.req.TeamLoopChecklistCreateRequest;
import com.loopone.loopinbe.domain.team.teamLoop.dto.req.TeamLoopChecklistUpdateRequest;
import com.loopone.loopinbe.domain.team.teamLoop.dto.res.TeamLoopChecklistResponse;

import java.util.List;

public interface TeamLoopChecklistService {
    TeamLoopChecklistResponse createChecklist(Long loopId, TeamLoopChecklistCreateRequest request, CurrentUserDto currentUser);
    TeamLoopChecklistResponse updateChecklist(Long checklistId, TeamLoopChecklistUpdateRequest request, CurrentUserDto currentUser);
    void deleteChecklist(Long checklistId, CurrentUserDto currentUser);
    TeamLoopChecklistResponse toggleCheck(Long checklistId, CurrentUserDto currentUser);
    List<TeamLoopChecklistResponse> getChecklistStatus(Long loopId, Long memberId, CurrentUserDto currentUser);
}
