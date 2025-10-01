package com.loopone.loopinbe.domain.loop.loop.controller;

import com.loopone.loopinbe.domain.account.auth.currentUser.CurrentUser;
import com.loopone.loopinbe.domain.account.auth.currentUser.CurrentUserDto;
import com.loopone.loopinbe.domain.loop.loop.dto.req.MainGoalRequest;
import com.loopone.loopinbe.domain.loop.loop.dto.res.MainGoalWithSubGoalsResponse;
import com.loopone.loopinbe.domain.loop.loop.entity.MainGoalPage;
import com.loopone.loopinbe.domain.loop.loop.service.MainGoalService;
import com.loopone.loopinbe.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(value="/rest-api/v1/mainGoal")
@RequiredArgsConstructor
@Tag(name = "Main Goal", description = "상위목표 API")
public class ApiV1MainGoalController {
    private final MainGoalService mainGoalService;

    // 상위목표 생성
    @PostMapping
    @Operation(summary = "상위목표 생성")
    public ApiResponse<Void> addMainGoal(@RequestBody @Valid MainGoalRequest mainGoalRequest, @CurrentUser CurrentUserDto currentUser){
        mainGoalService.addMainGoal(mainGoalRequest, currentUser);
        return ApiResponse.success();
    }

    // 목표 전체 리스트 조회
    @GetMapping
    @Operation(summary = "목표 리스트 조회")
    public ApiResponse<List<MainGoalWithSubGoalsResponse>> getAllGoal(@ModelAttribute MainGoalPage mainGoalPage, @CurrentUser CurrentUserDto currentUser){
        Pageable pageable = PageRequest.of(mainGoalPage.getPage(), mainGoalPage.getSize());
        return ApiResponse.success(mainGoalService.getAllGoal(pageable, currentUser));
    }

    // 상위목표 수정
    @PutMapping("/{mainGoalId}")
    @Operation(summary = "상위목표 수정")
    public ApiResponse<Void> updateMainGoal(@PathVariable("mainGoalId") Long mainGoalId,
                                          @RequestBody @Valid MainGoalRequest mainGoalRequest, @CurrentUser CurrentUserDto currentUser){
        mainGoalService.updateMainGoal(mainGoalId, mainGoalRequest, currentUser);
        return ApiResponse.success();
    }

    // 상위목표 삭제
    @DeleteMapping("/{mainGoalId}")
    @Operation(summary = "상위목표 삭제")
    public ApiResponse<Void> deleteMainGoal(@PathVariable("mainGoalId") Long mainGoalId, @CurrentUser CurrentUserDto currentUser){
        mainGoalService.deleteMainGoal(mainGoalId, currentUser);
        return ApiResponse.success();
    }
}
