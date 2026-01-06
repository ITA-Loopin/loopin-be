package com.loopone.loopinbe.domain.team.teamLoop.controller;

import com.loopone.loopinbe.domain.account.auth.currentUser.CurrentUser;
import com.loopone.loopinbe.domain.account.auth.currentUser.CurrentUserDto;
import com.loopone.loopinbe.domain.team.teamLoop.dto.req.TeamLoopChecklistCreateRequest;
import com.loopone.loopinbe.domain.team.teamLoop.dto.req.TeamLoopChecklistUpdateRequest;
import com.loopone.loopinbe.domain.team.teamLoop.dto.res.TeamLoopChecklistResponse;
import com.loopone.loopinbe.domain.team.teamLoop.service.TeamLoopChecklistService;
import com.loopone.loopinbe.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/rest-api/v1/teams/loops")
@RequiredArgsConstructor
@Tag(name = "TeamLoopChecklist", description = "팀 루프 체크리스트 API")
public class TeamLoopChecklistController {

    private final TeamLoopChecklistService teamLoopChecklistService;

    @PostMapping("/{loopId}/checklists")
    @Operation(summary = "체크리스트 생성", description = "팀 루프에 새로운 체크리스트를 추가합니다. (공통: 팀장만 / 개인: 본인만)")
    public ApiResponse<TeamLoopChecklistResponse> createChecklist(
            @PathVariable Long loopId,
            @RequestBody @Valid TeamLoopChecklistCreateRequest request,
            @Parameter(hidden = true) @CurrentUser CurrentUserDto currentUser
    ) {
        TeamLoopChecklistResponse response = teamLoopChecklistService.createChecklist(loopId, request, currentUser);
        return ApiResponse.success(response);
    }

    @PatchMapping("/checklists/{checklistId}")
    @Operation(summary = "체크리스트 내용 수정", description = "체크리스트의 내용을 수정합니다.")
    public ApiResponse<TeamLoopChecklistResponse> updateChecklist(
            @PathVariable Long checklistId,
            @RequestBody @Valid TeamLoopChecklistUpdateRequest request,
            @Parameter(hidden = true) @CurrentUser CurrentUserDto currentUser
    ) {
        TeamLoopChecklistResponse response = teamLoopChecklistService.updateChecklist(checklistId, request, currentUser);
        return ApiResponse.success(response);
    }

    @DeleteMapping("/checklists/{checklistId}")
    @Operation(summary = "체크리스트 삭제", description = "체크리스트 항목을 삭제합니다.")
    public ApiResponse<Void> deleteChecklist(
            @PathVariable Long checklistId,
            @Parameter(hidden = true) @CurrentUser CurrentUserDto currentUser
    ) {
        teamLoopChecklistService.deleteChecklist(checklistId, currentUser);
        return ApiResponse.success();
    }

    @PatchMapping("/checklists/{checklistId}/check")
    @Operation(summary = "체크리스트 체크/해제", description = "나의 체크리스트 수행 여부를 토글합니다.")
    public ApiResponse<TeamLoopChecklistResponse> toggleCheck(
            @PathVariable Long checklistId,
            @Parameter(hidden = true) @CurrentUser CurrentUserDto currentUser
    ) {
        TeamLoopChecklistResponse response = teamLoopChecklistService.toggleCheck(checklistId, currentUser);
        return ApiResponse.success(response);
    }

    @GetMapping("/{loopId}/checklists")
    @Operation(summary = "체크리스트 현황 조회", description = "특정 루프의 체크리스트 목록과 해당 멤버의 수행 여부를 조회합니다. (memberId 생략 시 현재 사용자)")
    public ApiResponse<List<TeamLoopChecklistResponse>> getChecklistStatus(
            @PathVariable Long loopId,
            @RequestParam(required = false) Long memberId,
            @Parameter(hidden = true) @CurrentUser CurrentUserDto currentUser
    ) {
        List<TeamLoopChecklistResponse> response = teamLoopChecklistService.getChecklistStatus(loopId, memberId, currentUser);
        return ApiResponse.success(response);
    }
}