package com.loopone.loopinbe.domain.loop.loopChecklist.service;

import com.loopone.loopinbe.domain.account.auth.currentUser.CurrentUserDto;
import com.loopone.loopinbe.domain.loop.loopChecklist.dto.req.LoopChecklistCreateRequest;
import com.loopone.loopinbe.domain.loop.loopChecklist.dto.req.LoopChecklistUpdateRequest;
import com.loopone.loopinbe.domain.loop.loopChecklist.dto.res.LoopChecklistResponse;

public interface LoopChecklistService {
    //체크리스트 생성
    LoopChecklistResponse addLoopChecklist(Long loopId, LoopChecklistCreateRequest loopChecklistCreateRequest, CurrentUserDto currentUser);

    //체크리스트 수정
    void updateLoopChecklist(Long checkListId, LoopChecklistUpdateRequest loopChecklistUpdateRequest, CurrentUserDto currentUser);

    //체크리스트 삭제
    void deleteLoopChecklist(Long checkListId, CurrentUserDto currentUser);

    //루프 내의 체크리스트 전체 삭제
    void deleteAllChecklist(Long loopId);
}
