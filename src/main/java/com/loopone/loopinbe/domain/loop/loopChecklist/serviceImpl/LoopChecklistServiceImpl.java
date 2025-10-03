package com.loopone.loopinbe.domain.loop.loopChecklist.serviceImpl;

import com.loopone.loopinbe.domain.account.auth.currentUser.CurrentUserDto;
import com.loopone.loopinbe.domain.account.member.mapper.MemberMapper;
import com.loopone.loopinbe.domain.loop.loop.entity.Loop;
import com.loopone.loopinbe.domain.loop.loop.repository.LoopRepository;
import com.loopone.loopinbe.domain.loop.loopChecklist.dto.req.LoopChecklistRequest;
import com.loopone.loopinbe.domain.loop.loopChecklist.entity.LoopChecklist;
import com.loopone.loopinbe.domain.loop.loopChecklist.repository.LoopChecklistRepository;
import com.loopone.loopinbe.domain.loop.loopChecklist.service.LoopChecklistService;
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
public class LoopChecklistServiceImpl implements LoopChecklistService {
    private final LoopChecklistRepository LoopChecklistRepository;
    private final LoopRepository loopRepository;
    private final MemberMapper memberMapper;

    // 체크리스트 생성
    @Override
    @Transactional
    public void addLoopChecklist(LoopChecklistRequest loopChecklistRequest, CurrentUserDto currentUser){
        Loop loop = loopRepository.findById(loopChecklistRequest.getLoopId())
                .orElseThrow(() -> new ServiceException(ReturnCode.LOOP_NOT_FOUND));
        LoopChecklist loopChecklist = LoopChecklist.builder()
                .member(memberMapper.toMember(currentUser))
                .loop(loop)
                .content(loopChecklistRequest.getContent())
                .completed(loopChecklistRequest.getCompleted())
                .build();
        LoopChecklistRepository.save(loopChecklist);
    }

    // 체크리스트 수정
    @Override
    @Transactional
    public void updateLoopChecklist(Long checkListId, LoopChecklistRequest loopChecklistRequest, CurrentUserDto currentUser){
        LoopChecklist loopChecklist = LoopChecklistRepository.findById(checkListId)
                .orElseThrow(() -> new ServiceException(ReturnCode.CHECK_LIST_NOT_FOUND));
        // 작성자 검증 아닌 경우 예외 처리
        validateLoopChecklistOwner(loopChecklist, currentUser);
        if (loopChecklistRequest.getContent() != null) {
            loopChecklist.setContent(loopChecklistRequest.getContent());
        }
        if (loopChecklistRequest.getCompleted() != null) { // Boolean 래퍼 타입이어야 null 체크 가능
            loopChecklist.setCompleted(loopChecklistRequest.getCompleted());
        }
        LoopChecklistRepository.save(loopChecklist);
    }

    // 체크리스트 삭제
    @Override
    @Transactional
    public void deleteLoopChecklist(Long checkListId, CurrentUserDto currentUser) {
        LoopChecklist loopChecklist = LoopChecklistRepository.findById(checkListId)
                .orElseThrow(() -> new ServiceException(ReturnCode.CHECK_LIST_NOT_FOUND));

        // 작성자 검증 아닌 경우 예외 처리
        validateLoopChecklistOwner(loopChecklist, currentUser);

        LoopChecklistRepository.delete(loopChecklist);
    }

    // 루프 내의 체크리스트 전체 삭제
    @Override
    @Transactional
    public void deleteAllChecklist(Long loopId){
        List<LoopChecklist> loopChecklists = LoopChecklistRepository.findByLoopId(loopId);
        LoopChecklistRepository.deleteAll(loopChecklists);
    }

    // ----------------- 헬퍼 메서드 -----------------
    // 체크리스트 작성자 검증
    public static void validateLoopChecklistOwner(LoopChecklist loopChecklist, CurrentUserDto currentUser) {
        if (!loopChecklist.getMember().getId().equals(currentUser.getId())) {
            throw new ServiceException(ReturnCode.NOT_AUTHORIZED);
        }
    }
}
