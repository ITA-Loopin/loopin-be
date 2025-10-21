package com.loopone.loopinbe.domain.loop.loop.controller;

import com.loopone.loopinbe.domain.account.auth.currentUser.CurrentUser;
import com.loopone.loopinbe.domain.account.auth.currentUser.CurrentUserDto;
import com.loopone.loopinbe.domain.loop.loop.dto.req.LoopCreateRequest;
import com.loopone.loopinbe.domain.loop.loop.dto.req.LoopUpdateRequest;
import com.loopone.loopinbe.domain.loop.loop.dto.res.DailyLoopsResponse;
import com.loopone.loopinbe.domain.loop.loop.dto.res.LoopDetailResponse;
import com.loopone.loopinbe.domain.loop.loop.dto.res.LoopSimpleResponse;
import com.loopone.loopinbe.domain.loop.loop.entity.LoopPage;
import com.loopone.loopinbe.domain.loop.loop.service.LoopService;
import com.loopone.loopinbe.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping(value="/rest-api/v1")
@RequiredArgsConstructor
@Tag(name = "Loop", description = "루프 API")
public class ApiV1LoopController {
    private final LoopService loopService;

    //루프 생성
    @PostMapping("/loops")
    @Operation(summary = "루프 생성", description = "새로운 루프를 생성합니다.")
    public ApiResponse<Void> addLoop(
            @RequestBody @Valid LoopCreateRequest loopCreateRequest,
            @Parameter(hidden = true) @CurrentUser CurrentUserDto currentUser
    ){
        loopService.createLoop(loopCreateRequest, currentUser);
        return ApiResponse.success();
    }

    //루프 상세 조회
    @GetMapping("/loops/{loopId}")
    @Operation(summary = "루프 상세 조회", description = "해당 루프의 상세 정보를 조회합니다.")
    public ApiResponse<LoopDetailResponse> getDetailLoop(
            @PathVariable Long loopId,
            @Parameter(hidden = true) @CurrentUser CurrentUserDto currentUser
    ){
        LoopDetailResponse detailLoop = loopService.getDetailLoop(loopId, currentUser);
        return ApiResponse.success(detailLoop);
    }


    //날짜별 루프 리스트 조회
    @GetMapping("/loops/date/{loopDate}")
    @Operation(summary = "날짜별 루프 리스트 조회", description = "해당 날짜의 루프 리스트를 조회합니다.")
    public ApiResponse<DailyLoopsResponse> getDailyLoops(
            @PathVariable LocalDate loopDate,
            @Parameter(hidden = true) @CurrentUser CurrentUserDto currentUser
    ){
        DailyLoopsResponse dailyLoops = loopService.getDailyLoops(loopDate, currentUser);
        return ApiResponse.success(dailyLoops);
    }

/*    //루프 전체 리스트 조회
    @GetMapping("/loops")
    @Operation(summary = "루프 리스트 조회", description = "사용자가 생성한 모든 루프를 조회합니다.")
    public ApiResponse<List<LoopSimpleResponse>> getAllLoop(
            @ModelAttribute LoopPage loopPage,
            @Parameter(hidden = true) @CurrentUser CurrentUserDto currentUser
    ){
        Pageable pageable = PageRequest.of(loopPage.getPage(), loopPage.getSize());
        return ApiResponse.success(loopService.getAllLoop(pageable, currentUser));
    }*/

    //단일 루프 수정
    @PutMapping("/loops/{loopId}")
    @Operation(summary = "단일 루프 수정", description = "해당 루프의 정보를 수정합니다. (그룹에서 제외됨)")
    public ApiResponse<Void> updateLoop(
            @PathVariable Long loopId,
            @RequestBody @Valid LoopUpdateRequest loopUpdateRequest,
            @Parameter(hidden = true) @CurrentUser CurrentUserDto currentUser
    ){
        loopService.updateLoop(loopId, loopUpdateRequest, currentUser);
        return ApiResponse.success();
    }

    //TODO: 그룹 전체 루프 수정 API 구현

    //루프 삭제
    @DeleteMapping("/loops/{loopId}")
    @Operation(summary = "루프 삭제", description = "해당 루프를 삭제합니다.")
    public ApiResponse<Void> deleteLoop(
            @PathVariable Long loopId,
            @Parameter(hidden = true) @CurrentUser CurrentUserDto currentUser
    ){
        loopService.deleteLoop(loopId, currentUser);
        return ApiResponse.success();
    }

    //TODO: 그룹 전체 루프 삭제 API 구현
}
