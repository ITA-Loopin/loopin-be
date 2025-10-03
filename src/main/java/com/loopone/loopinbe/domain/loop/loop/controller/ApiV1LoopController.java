package com.loopone.loopinbe.domain.loop.loop.controller;

import com.loopone.loopinbe.domain.account.auth.currentUser.CurrentUser;
import com.loopone.loopinbe.domain.account.auth.currentUser.CurrentUserDto;
import com.loopone.loopinbe.domain.loop.loop.dto.req.LoopCreateRequest;
import com.loopone.loopinbe.domain.loop.loop.dto.req.LoopUpdateRequest;
import com.loopone.loopinbe.domain.loop.loop.dto.res.LoopSimpleResponse;
import com.loopone.loopinbe.domain.loop.loop.entity.LoopPage;
import com.loopone.loopinbe.domain.loop.loop.service.LoopService;
import com.loopone.loopinbe.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(value="/rest-api/v1")
@RequiredArgsConstructor
@Tag(name = "Loop", description = "루프 API")
public class ApiV1LoopController {
    private final LoopService loopService;

    //루프 생성
    @PostMapping("/loops")
    @Operation(summary = "루프 생성")
    public ApiResponse<Void> addLoop(
            @RequestBody @Valid LoopCreateRequest loopCreateRequest,
            @Parameter(hidden = true) @CurrentUser CurrentUserDto currentUser
    ){
        loopService.addLoop(loopCreateRequest, currentUser);
        return ApiResponse.success();
    }

    //TODO: 루프 상세 조회 API 구현

    //TODO: 루프 날짜별 리스트 조회 API 구현

    //루프 전체 리스트 조회
    @GetMapping("/loops")
    @Operation(summary = "루프 리스트 조회")
    public ApiResponse<List<LoopSimpleResponse>> getAllLoop(
            @ModelAttribute LoopPage loopPage,
            @Parameter(hidden = true) @CurrentUser CurrentUserDto currentUser
    ){
        Pageable pageable = PageRequest.of(loopPage.getPage(), loopPage.getSize());
        return ApiResponse.success(loopService.getAllLoop(pageable, currentUser));
    }

    //루프 수정
    @PutMapping("/loops/{loopId}")
    @Operation(summary = "루프 수정")
    public ApiResponse<Void> updateLoop(
            @PathVariable("loopId") Long loopId,
            @RequestBody @Valid LoopUpdateRequest loopUpdateRequest,
            @Parameter(hidden = true) @CurrentUser CurrentUserDto currentUser
    ){
        loopService.updateLoop(loopId, loopUpdateRequest, currentUser);
        return ApiResponse.success();
    }

    //루프 삭제
    @DeleteMapping("/loops/{loopId}")
    @Operation(summary = "루프 삭제")
    public ApiResponse<Void> deleteLoop(
            @PathVariable("loopId") Long loopId,
            @Parameter(hidden = true) @CurrentUser CurrentUserDto currentUser
    ){
        loopService.deleteLoop(loopId, currentUser);
        return ApiResponse.success();
    }
}
