package com.loopone.loopinbe.domain.loop.loop.controller;

import com.loopone.loopinbe.domain.account.auth.currentUser.CurrentUser;
import com.loopone.loopinbe.domain.account.auth.currentUser.CurrentUserDto;
import com.loopone.loopinbe.domain.loop.loop.dto.req.LoopRequest;
import com.loopone.loopinbe.domain.loop.loop.dto.res.LoopWithCheckListResponse;
import com.loopone.loopinbe.domain.loop.loop.entity.LoopPage;
import com.loopone.loopinbe.domain.loop.loop.service.LoopService;
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
@RequestMapping(value="/rest-api/v1/loop")
@RequiredArgsConstructor
@Tag(name = "Loop", description = "루프 API")
public class ApiV1LoopController {
    private final LoopService loopService;

    // 루프 생성
    @PostMapping
    @Operation(summary = "루프 생성")
    public ApiResponse<Void> addLoop(@RequestBody @Valid LoopRequest loopRequest, @CurrentUser CurrentUserDto currentUser){
        loopService.addLoop(loopRequest, currentUser);
        return ApiResponse.success();
    }

    // 루프 전체 리스트 조회
    @GetMapping
    @Operation(summary = "루프 리스트 조회")
    public ApiResponse<List<LoopWithCheckListResponse>> getAllLoop(@ModelAttribute LoopPage loopPage, @CurrentUser CurrentUserDto currentUser){
        Pageable pageable = PageRequest.of(loopPage.getPage(), loopPage.getSize());
        return ApiResponse.success(loopService.getAllLoop(pageable, currentUser));
    }

    // 루프 수정
    @PutMapping("/{loopId}")
    @Operation(summary = "루프 수정")
    public ApiResponse<Void> updateLoop(@PathVariable("loopId") Long loopId,
                                        @RequestBody @Valid LoopRequest loopRequest, @CurrentUser CurrentUserDto currentUser){
        loopService.updateLoop(loopId, loopRequest, currentUser);
        return ApiResponse.success();
    }

    // 루프 삭제
    @DeleteMapping("/{loopId}")
    @Operation(summary = "루프 삭제")
    public ApiResponse<Void> deleteLoop(@PathVariable("loopId") Long loopId, @CurrentUser CurrentUserDto currentUser){
        loopService.deleteLoop(loopId, currentUser);
        return ApiResponse.success();
    }
}
