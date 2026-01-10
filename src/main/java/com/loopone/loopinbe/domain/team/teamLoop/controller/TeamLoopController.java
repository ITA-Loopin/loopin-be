package com.loopone.loopinbe.domain.team.teamLoop.controller;

import com.loopone.loopinbe.domain.account.auth.currentUser.CurrentUser;
import com.loopone.loopinbe.domain.account.auth.currentUser.CurrentUserDto;
import com.loopone.loopinbe.domain.team.teamLoop.dto.req.TeamLoopCreateRequest;
import com.loopone.loopinbe.domain.team.teamLoop.dto.res.TeamLoopDetailResponse;
import com.loopone.loopinbe.domain.team.teamLoop.dto.res.TeamLoopListResponse;
import com.loopone.loopinbe.domain.team.teamLoop.service.TeamLoopService;
import com.loopone.loopinbe.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/rest-api/v1/teams")
@RequiredArgsConstructor
@Tag(name = "Team", description = "팀 루프 API")
public class TeamLoopController {

    private final TeamLoopService teamLoopService;

    @GetMapping("/{teamId}/loops")
    @Operation(summary = "팀 루프 리스트 조회", description = "특정 날짜의 팀 루프 리스트를 조회합니다. (파라메터 없으면 오늘 기준)")
    public ApiResponse<List<TeamLoopListResponse>> getTeamLoops(
            @PathVariable Long teamId,
            @RequestParam(required = false) LocalDate date,
            @Parameter(hidden = true) @CurrentUser CurrentUserDto currentUser
    ) {
        LocalDate targetDate = (date != null) ? date : LocalDate.now();
        List<TeamLoopListResponse> response = teamLoopService.getTeamLoops(teamId, targetDate, currentUser);
        return ApiResponse.success(response);
    }

    @PostMapping("/{teamId}/loops")
    @Operation(summary = "팀 루프 생성", description = "팀 루프를 생성하고 팀원들에게 할당합니다.")
    public ApiResponse<Long> createTeamLoop(
            @PathVariable Long teamId,
            @RequestBody @Valid TeamLoopCreateRequest request,
            @Parameter(hidden = true) @CurrentUser CurrentUserDto currentUser
    ) {
        Long loopRuleId = teamLoopService.createTeamLoop(teamId, request, currentUser);
        return ApiResponse.success(loopRuleId);
    }

    @GetMapping("/{teamId}/my/loops/{loopId}")
    @Operation(summary = "내 팀 루프 상세 조회", description = "내가 참여 중인 팀 루프의 상세 정보를 조회합니다.")
    public ApiResponse<TeamLoopDetailResponse> getMyTeamLoopDetail(
            @PathVariable Long teamId,
            @PathVariable Long loopId,
            @Parameter(hidden = true) @CurrentUser CurrentUserDto currentUser
    ) {
        TeamLoopDetailResponse response = teamLoopService.getMyTeamLoopDetail(teamId, loopId, currentUser);
        return ApiResponse.success(response);
    }
}
