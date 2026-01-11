package com.loopone.loopinbe.domain.team.team.service;


import com.loopone.loopinbe.domain.account.auth.currentUser.CurrentUserDto;
import com.loopone.loopinbe.domain.account.member.entity.Member;
import com.loopone.loopinbe.domain.team.team.dto.req.TeamCreateRequest;
import com.loopone.loopinbe.domain.team.team.dto.req.TeamOrderUpdateRequest;
import com.loopone.loopinbe.domain.team.team.dto.res.MyTeamResponse;
import com.loopone.loopinbe.domain.team.team.dto.res.RecruitingTeamResponse;
import com.loopone.loopinbe.domain.team.team.dto.res.TeamDetailResponse;
import com.loopone.loopinbe.domain.team.team.dto.res.TeamMemberResponse;

import java.time.LocalDate;
import java.util.List;

public interface TeamService {
    Long createTeam(TeamCreateRequest request, CurrentUserDto currentUser);
    List<MyTeamResponse> getMyTeams(CurrentUserDto currentUser);
    List<RecruitingTeamResponse> getRecruitingTeams(CurrentUserDto currentUser);
    TeamDetailResponse getTeamDetails(Long teamId, LocalDate targetDate, CurrentUserDto currentUser);
    List<TeamMemberResponse> getTeamMembers(Long teamId);
    //팀 순서 변경
    void updateTeamOrder(TeamOrderUpdateRequest request, CurrentUserDto currentUser);

    // 사용자가 참여중인 모든 팀 나가기/관련 엔티티 삭제
    void deleteMyTeams(Member member);

    // 팀 삭제
    void deleteTeam(Long teamId, CurrentUserDto currentUser);
}
