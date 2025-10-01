package com.loopone.loopinbe.domain.loop.checkList.serviceImpl;

import com.loopone.loopinbe.domain.account.auth.currentUser.CurrentUserDto;
import com.loopone.loopinbe.domain.account.member.mapper.MemberMapper;
import com.loopone.loopinbe.domain.loop.loop.entity.Loop;
import com.loopone.loopinbe.domain.loop.loop.repository.LoopRepository;
import com.loopone.loopinbe.domain.loop.checkList.dto.req.LoopCheckListRequest;
import com.loopone.loopinbe.domain.loop.checkList.entity.LoopCheckList;
import com.loopone.loopinbe.domain.loop.checkList.repository.LoopCheckListRepository;
import com.loopone.loopinbe.domain.loop.checkList.service.LoopCheckListService;
import com.loopone.loopinbe.global.exception.ReturnCode;
import com.loopone.loopinbe.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoopCheckListServiceImpl implements LoopCheckListService {
    private final LoopCheckListRepository LoopCheckListRepository;
    private final LoopRepository loopRepository;
    private final MemberMapper memberMapper;

    // 체크리스트 생성
    @Override
    @Transactional
    public void addLoopCheckList(LoopCheckListRequest loopCheckListRequest, CurrentUserDto currentUser){
        Loop loop = loopRepository.findById(loopCheckListRequest.getLoopId())
                .orElseThrow(() -> new ServiceException(ReturnCode.LOOP_NOT_FOUND));
        LoopCheckList loopChecklist = LoopCheckList.builder()
                .member(memberMapper.toMember(currentUser))
                .loop(loop)
                .content(loopCheckListRequest.getContent())
                .completed(loopCheckListRequest.getCompleted())
                .build();
        LoopCheckListRepository.save(loopChecklist);
    }

    // 체크리스트 수정
    @Override
    @Transactional
    public void updateLoopCheckList(Long checkListId, LoopCheckListRequest loopCheckListRequest, CurrentUserDto currentUser){
        LoopCheckList loopChecklist = LoopCheckListRepository.findById(checkListId)
                .orElseThrow(() -> new ServiceException(ReturnCode.CHECK_LIST_NOT_FOUND));
        // 작성자 검증 아닌 경우 예외 처리
        validateLoopCheckListOwner(loopChecklist, currentUser);
        if (loopCheckListRequest.getContent() != null) {
            loopChecklist.setContent(loopCheckListRequest.getContent());
        }
        if (loopCheckListRequest.getCompleted() != null) { // Boolean 래퍼 타입이어야 null 체크 가능
            loopChecklist.setCompleted(loopCheckListRequest.getCompleted());
        }
        LoopCheckListRepository.save(loopChecklist);
    }

    // 하위목표 삭제
    @Override
    @Transactional
    public void deleteLoopCheckList(Long checkListId, CurrentUserDto currentUser) {
        LoopCheckList loopChecklist = LoopCheckListRepository.findById(checkListId)
                .orElseThrow(() -> new ServiceException(ReturnCode.CHECK_LIST_NOT_FOUND));

        // 작성자 검증 아닌 경우 예외 처리
        validateLoopCheckListOwner(loopChecklist, currentUser);

        LoopCheckListRepository.delete(loopChecklist);
    }

    // 루프 내의 체크리스트 전체 삭제
    @Override
    @Transactional
    public void deleteAllCheckList(Long loopId){
        List<LoopCheckList> loopCheckLists = LoopCheckListRepository.findByLoopId(loopId);
        LoopCheckListRepository.deleteAll(loopCheckLists);
    }

    // ----------------- 헬퍼 메서드 -----------------
    // 목표 작성자 검증
    public static void validateLoopCheckListOwner(LoopCheckList loopChecklist, CurrentUserDto currentUser) {
        if (!loopChecklist.getMember().getId().equals(currentUser.getId())) {
            throw new ServiceException(ReturnCode.NOT_AUTHORIZED);
        }
    }
}
