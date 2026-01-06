package com.loopone.loopinbe.domain.loop.loop.service;

import com.loopone.loopinbe.domain.account.auth.currentUser.CurrentUserDto;
import com.loopone.loopinbe.domain.loop.loop.dto.req.LoopCompletionUpdateRequest;
import com.loopone.loopinbe.domain.loop.loop.dto.req.LoopCreateRequest;
import com.loopone.loopinbe.domain.loop.loop.dto.req.LoopGroupUpdateRequest;
import com.loopone.loopinbe.domain.loop.loop.dto.req.LoopUpdateRequest;
import com.loopone.loopinbe.domain.loop.loop.dto.res.DailyLoopsResponse;
import com.loopone.loopinbe.domain.loop.loop.dto.res.LoopDetailResponse;

import java.time.LocalDate;

public interface LoopService {
    // 루프 생성
    Long createLoop(LoopCreateRequest loopCreateRequest, CurrentUserDto currentUser);

    // 루프 상세 조회
    LoopDetailResponse getDetailLoop(Long loopId, CurrentUserDto currentUser);

    // 날짜별 루프 리스트 조회
    DailyLoopsResponse getDailyLoops(LocalDate date, CurrentUserDto currentUser);

    //루프 전체 리스트 조회
    //PageResponse<LoopSimpleResponse> getAllLoop(Pageable pageable, CurrentUserDto currentUser);

    //루프 완료 처리
    void updateLoopCompletion(Long loopId, LoopCompletionUpdateRequest loopCompletionUpdateRequest, CurrentUserDto currentUser);

    //루프 수정
    void updateLoop(Long loopId, LoopUpdateRequest loopUpdateRequest, CurrentUserDto currentUser);

    // 루프 그룹 전체 수정
    void updateLoopGroup(Long loopRuleId, LoopGroupUpdateRequest loopGroupUpdateRequest, CurrentUserDto currentUser);

    // 루프 삭제
    void deleteLoop(Long loopId, CurrentUserDto currentUser);

    // 루프 그룹 전체 삭제
    void deleteLoopGroup(Long loopRuleId, CurrentUserDto currentUser);

    // 사용자가 생성한 루프 전체 삭제
    void deleteMyLoops(Long memberId);
}
