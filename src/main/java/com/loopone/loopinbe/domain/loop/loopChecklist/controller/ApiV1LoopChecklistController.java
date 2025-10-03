package com.loopone.loopinbe.domain.loop.loopChecklist.controller;

import com.loopone.loopinbe.domain.account.auth.currentUser.CurrentUser;
import com.loopone.loopinbe.domain.account.auth.currentUser.CurrentUserDto;
import com.loopone.loopinbe.domain.loop.loopChecklist.dto.req.LoopChecklistCreateRequest;
import com.loopone.loopinbe.domain.loop.loopChecklist.dto.req.LoopChecklistUpdateRequest;
import com.loopone.loopinbe.domain.loop.loopChecklist.service.LoopChecklistService;
import com.loopone.loopinbe.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value="/rest-api/v1")
@RequiredArgsConstructor
@Tag(name = "LoopChecklist", description = "루프 체크리스트 API")
public class ApiV1LoopChecklistController {
    private final LoopChecklistService loopChecklistService;

    //체크리스트 생성
    @PostMapping("/loops/{loopId}/checklists")
    @Operation(summary = "체크리스트 생성")
    public ApiResponse<Void> addLoopChecklist(
            @PathVariable Long loopId,
            @RequestBody @Valid LoopChecklistCreateRequest loopChecklistCreateRequest,
            @Parameter(hidden = true) @CurrentUser CurrentUserDto currentUser
    ){
        loopChecklistService.addLoopChecklist(loopId, loopChecklistCreateRequest, currentUser);
        return ApiResponse.success();
    }

    //TODO: 체크리스트 단일 조회

    //체크리스트 수정
    @PutMapping("/checklists/{checklistId}")
    @Operation(summary = "체크리스트 수정")
    public ApiResponse<Void> updateLoopChecklist(
            @PathVariable("loopChecklistId") Long loopChecklistId,
            @RequestBody @Valid LoopChecklistUpdateRequest loopChecklistUpdateRequest,
            @Parameter(hidden = true) @CurrentUser CurrentUserDto currentUser
    ){
        loopChecklistService.updateLoopChecklist(loopChecklistId, loopChecklistUpdateRequest, currentUser);
        return ApiResponse.success();
    }

    //체크리스트 삭제
    @DeleteMapping("/checklists/{checklistId}")
    @Operation(summary = "체크리스트 삭제")
    public ApiResponse<Void> deleteLoopChecklist(
            @PathVariable("loopChecklistId") Long loopChecklistId,
            @Parameter(hidden = true) @CurrentUser CurrentUserDto currentUser
    ){
        loopChecklistService.deleteLoopChecklist(loopChecklistId, currentUser);
        return ApiResponse.success();
    }
}
