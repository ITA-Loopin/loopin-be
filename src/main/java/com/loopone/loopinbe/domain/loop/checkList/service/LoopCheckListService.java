package com.loopone.loopinbe.domain.loop.checkList.service;

import com.loopone.loopinbe.domain.account.auth.currentUser.CurrentUserDto;
import com.loopone.loopinbe.domain.loop.checkList.dto.req.LoopCheckListRequest;

public interface LoopCheckListService {
    // 체크리스트 생성
    void addLoopCheckList(LoopCheckListRequest loopCheckListRequest, CurrentUserDto currentUser);

    // 체크리스트 수정
    void updateLoopCheckList(Long checkListId, LoopCheckListRequest loopCheckListRequest, CurrentUserDto currentUser);

    // 체크리스트 삭제
    void deleteLoopCheckList(Long checkListId, CurrentUserDto currentUser);

    // 루프 내의 체크리스트 전체 삭제
    void deleteAllCheckList(Long loopId);
}
