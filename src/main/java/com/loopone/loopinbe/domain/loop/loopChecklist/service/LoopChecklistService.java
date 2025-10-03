package com.loopone.loopinbe.domain.loop.loopChecklist.service;

import com.loopone.loopinbe.domain.account.auth.currentUser.CurrentUserDto;
import com.loopone.loopinbe.domain.loop.loopChecklist.dto.req.LoopChecklistRequest;

public interface LoopChecklistService {
    // 체크리스트 생성
    void addLoopChecklist(LoopChecklistRequest loopChecklistRequest, CurrentUserDto currentUser);

    // 체크리스트 수정
    void updateLoopChecklist(Long checkListId, LoopChecklistRequest loopChecklistRequest, CurrentUserDto currentUser);

    // 체크리스트 삭제
    void deleteLoopChecklist(Long checkListId, CurrentUserDto currentUser);

    // 루프 내의 체크리스트 전체 삭제
    void deleteAllChecklist(Long loopId);
}
