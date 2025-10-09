package com.loopone.loopinbe.domain.loop.loop.service;

import com.loopone.loopinbe.domain.account.auth.currentUser.CurrentUserDto;
import com.loopone.loopinbe.domain.loop.loop.dto.req.LoopCreateRequest;
import com.loopone.loopinbe.domain.loop.loop.dto.req.LoopUpdateRequest;
import com.loopone.loopinbe.domain.loop.loop.dto.res.DailyLoopsResponse;
import com.loopone.loopinbe.domain.loop.loop.dto.res.LoopDetailResponse;
import com.loopone.loopinbe.domain.loop.loop.dto.res.LoopSimpleResponse;
import com.loopone.loopinbe.global.common.response.PageResponse;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

public interface LoopService {
    //루프 생성
    void createLoop(LoopCreateRequest loopCreateRequest, CurrentUserDto currentUser);

    //루프 상세 조회
    LoopDetailResponse getDetailLoop(Long loopId, CurrentUserDto currentUserDto);

    //날짜별 루프 리스트 조회
    DailyLoopsResponse getDailyLoops(LocalDate date, CurrentUserDto currentUserDto);

    //루프 전체 리스트 조회
    //PageResponse<LoopSimpleResponse> getAllLoop(Pageable pageable, CurrentUserDto currentUser);

    //루프 수정
    void updateLoop(Long loopId, LoopUpdateRequest loopUpdateRequest, CurrentUserDto currentUser);

    //루프 삭제
    void deleteLoop(Long loopId, CurrentUserDto currentUser);
}
