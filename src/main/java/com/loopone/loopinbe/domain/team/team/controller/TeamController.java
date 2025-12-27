package com.loopone.loopinbe.domain.team.team.controller;

import com.loopone.loopinbe.domain.account.auth.currentUser.CurrentUser;
import com.loopone.loopinbe.domain.account.auth.currentUser.CurrentUserDto;
import com.loopone.loopinbe.domain.team.team.dto.req.TeamCreateRequest;
import com.loopone.loopinbe.domain.team.team.dto.res.MyTeamResponse;
import com.loopone.loopinbe.domain.team.team.dto.res.RecruitingTeamResponse;
import com.loopone.loopinbe.domain.team.team.enums.TeamState;
import com.loopone.loopinbe.domain.team.team.service.TeamService;
import com.loopone.loopinbe.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/rest-api/v1")
@RequiredArgsConstructor
@Tag(name = "Team", description = "팀 API")
public class TeamController {

    private final TeamService teamService;

    @PostMapping("/teams")
    @Operation(summary = "팀 루프 생성", description = "새로운 팀 루프를 생성하고 팀원을 초대합니다.")
    public ApiResponse<Long> createTeam(
            @Valid @RequestBody TeamCreateRequest request,
            @Parameter(hidden = true) @CurrentUser CurrentUserDto currentUser
    ) {
        Long teamId = teamService.createTeam(request, currentUser);
        return ApiResponse.success(teamId);
    }

    @GetMapping("/teams/my")
    @Operation(summary = "나의 팀 루프 조회", description = "내가 참여 중인 팀 루프 목록과 진행률을 조회합니다.")
    public ApiResponse<List<MyTeamResponse>> getMyTeams(
            @Parameter(hidden = true) @CurrentUser CurrentUserDto currentUser,
            @RequestParam(required = false, defaultValue = "IN_PROGRESS") TeamState teamState
            ) {
        List<MyTeamResponse> response = teamService.getMyTeams(currentUser, teamState);
        return ApiResponse.success(response);
    }

    @GetMapping("/teams/recruiting")
    @Operation(summary = "모집 중인 팀 루프 조회", description = "참여 가능한 다른 팀 루프 목록을 조회합니다.")
    public ApiResponse<List<RecruitingTeamResponse>> getRecruitingTeams(
            @Parameter(hidden = true) @CurrentUser CurrentUserDto currentUser
    ) {
        List<RecruitingTeamResponse> response = teamService.getRecruitingTeams(currentUser);
        return ApiResponse.success(response);
    }
}