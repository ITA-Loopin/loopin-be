package com.loopone.loopinbe.domain.loop.teamLoop.subGoal.controller;

import com.loopone.loopinbe.domain.account.auth.currentUser.CurrentUser;
import com.loopone.loopinbe.domain.account.auth.currentUser.CurrentUserDto;
import com.loopone.loopinbe.domain.loop.teamLoop.subGoal.dto.req.SubGoalRequest;
import com.loopone.loopinbe.domain.loop.teamLoop.subGoal.service.SubGoalService;
import com.loopone.loopinbe.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value="/rest-api/v1/subGoal")
@RequiredArgsConstructor
@Tag(name = "Sub Goal", description = "하위목표 API")
public class ApiV1SubGoalController {
    private final SubGoalService subGoalService;

    // 하위목표 생성
    @PostMapping
    @Operation(summary = "하위목표 생성")
    public ApiResponse<Void> addSubGoal(@RequestBody @Valid SubGoalRequest subGoalRequest, @CurrentUser CurrentUserDto currentUser){
        subGoalService.addSubGoal(subGoalRequest, currentUser);
        return ApiResponse.success();
    }

    // 하위목표 수정
    @PutMapping("/{subGoalId}")
    @Operation(summary = "하위목표 수정")
    public ApiResponse<Void> updateSubGoal(@PathVariable("subGoalId") Long subGoalId,
                                             @RequestBody @Valid SubGoalRequest subGoalRequest, @CurrentUser CurrentUserDto currentUser){
        subGoalService.updateSubGoal(subGoalId, subGoalRequest, currentUser);
        return ApiResponse.success();
    }

    // 하위목표 삭제
    @DeleteMapping("/{subGoalId}")
    @Operation(summary = "하위목표 삭제")
    public ApiResponse<Void> deleteSubGoal(@PathVariable("subGoalId") Long subGoalId, @CurrentUser CurrentUserDto currentUser){
        subGoalService.deleteSubGoal(subGoalId, currentUser);
        return ApiResponse.success();
    }
}
