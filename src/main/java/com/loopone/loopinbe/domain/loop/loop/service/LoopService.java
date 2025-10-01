package com.loopone.loopinbe.domain.loop.loop.service;

import com.loopone.loopinbe.domain.account.auth.currentUser.CurrentUserDto;
import com.loopone.loopinbe.domain.loop.loop.dto.req.LoopRequest;
import com.loopone.loopinbe.domain.loop.loop.dto.res.LoopWithCheckListResponse;
import com.loopone.loopinbe.global.common.response.PageResponse;
import org.springframework.data.domain.Pageable;

public interface LoopService {
    // 루프 생성
    void addLoop(LoopRequest loopRequest, CurrentUserDto currentUser);

    // 루프 전체 리스트 조회
    PageResponse<LoopWithCheckListResponse> getAllLoop(Pageable pageable, CurrentUserDto currentUser);

    // 루프 수정
    void updateLoop(Long loopId, LoopRequest loopRequest, CurrentUserDto currentUser);

    // 루프 삭제
    void deleteLoop(Long loopId, CurrentUserDto currentUser);
}
