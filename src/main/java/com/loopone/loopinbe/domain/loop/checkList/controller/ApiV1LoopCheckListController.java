package com.loopone.loopinbe.domain.loop.checkList.controller;

import com.loopone.loopinbe.domain.account.auth.currentUser.CurrentUser;
import com.loopone.loopinbe.domain.account.auth.currentUser.CurrentUserDto;
import com.loopone.loopinbe.domain.loop.checkList.dto.req.LoopCheckListRequest;
import com.loopone.loopinbe.domain.loop.checkList.service.LoopCheckListService;
import com.loopone.loopinbe.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value="/rest-api/v1/loopCheckList")
@RequiredArgsConstructor
@Tag(name = "LoopCheckList", description = "루프 체크리스트 API")
public class ApiV1LoopCheckListController {
    private final LoopCheckListService loopCheckListService;

    // 체크리스트 생성
    @PostMapping
    @Operation(summary = "체크리스트 생성")
    public ApiResponse<Void> addLoopCheckList(@RequestBody @Valid LoopCheckListRequest loopCheckListRequest, @CurrentUser CurrentUserDto currentUser){
        loopCheckListService.addLoopCheckList(loopCheckListRequest, currentUser);
        return ApiResponse.success();
    }

    // 하위목표 수정
    @PutMapping("/{loopCheckListId}")
    @Operation(summary = "체크리스트 수정")
    public ApiResponse<Void> updateLoopCheckList(@PathVariable("loopCheckListId") Long loopCheckListId,
                                           @RequestBody @Valid LoopCheckListRequest loopCheckListRequest, @CurrentUser CurrentUserDto currentUser){
        loopCheckListService.updateLoopCheckList(loopCheckListId, loopCheckListRequest, currentUser);
        return ApiResponse.success();
    }

    // 하위목표 삭제
    @DeleteMapping("/{loopCheckListId}")
    @Operation(summary = "체크리스트 삭제")
    public ApiResponse<Void> deleteLoopCheckList(@PathVariable("loopCheckListId") Long loopCheckListId, @CurrentUser CurrentUserDto currentUser){
        loopCheckListService.deleteLoopCheckList(loopCheckListId, currentUser);
        return ApiResponse.success();
    }
}
