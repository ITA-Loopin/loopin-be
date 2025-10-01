package com.loopone.loopinbe.domain.loop.subGoal.serviceImpl;

import com.loopone.loopinbe.domain.account.auth.currentUser.CurrentUserDto;
import com.loopone.loopinbe.domain.account.member.mapper.MemberMapper;
import com.loopone.loopinbe.domain.loop.loop.entity.Loop;
import com.loopone.loopinbe.domain.loop.loop.repository.LoopRepository;
import com.loopone.loopinbe.domain.loop.subGoal.dto.req.SubGoalRequest;
import com.loopone.loopinbe.domain.loop.subGoal.entity.LoopChecklist;
import com.loopone.loopinbe.domain.loop.subGoal.repository.SubGoalRepository;
import com.loopone.loopinbe.domain.loop.subGoal.service.SubGoalService;
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
public class SubGoalServiceImpl implements SubGoalService {
    private final SubGoalRepository subGoalRepository;
    private final LoopRepository loopRepository;
    private final MemberMapper memberMapper;

    // 하위목표 생성
    @Override
    @Transactional
    public void addSubGoal(SubGoalRequest subGoalRequest, CurrentUserDto currentUser){
        Loop loop = loopRepository.findById(subGoalRequest.getLoopId())
                .orElseThrow(() -> new ServiceException(ReturnCode.MAIN_GOAL_NOT_FOUND));
        LoopChecklist loopChecklist = LoopChecklist.builder()
                .member(memberMapper.toMember(currentUser))
                .loop(loop)
                .content(subGoalRequest.getContent())
                .deadline(subGoalRequest.getDeadline())
                .checked(subGoalRequest.getChecked())
                .build();
        subGoalRepository.save(loopChecklist);
    }

    // 하위목표 수정
    @Override
    @Transactional
    public void updateSubGoal(Long subGoalId, SubGoalRequest subGoalRequest, CurrentUserDto currentUser){
        LoopChecklist loopChecklist = subGoalRepository.findById(subGoalId)
                .orElseThrow(() -> new ServiceException(ReturnCode.SUB_GOAL_NOT_FOUND));
        // 작성자 검증 아닌 경우 예외 처리
        validateSubGoalOwner(loopChecklist, currentUser);
        if (subGoalRequest.getContent() != null) {
            loopChecklist.setContent(subGoalRequest.getContent());
        }
        if (subGoalRequest.getDeadline() != null) {
            loopChecklist.setDeadline(subGoalRequest.getDeadline());
        }
        if (subGoalRequest.getChecked() != null) { // Boolean 래퍼 타입이어야 null 체크 가능
            loopChecklist.setChecked(subGoalRequest.getChecked());
        }
        subGoalRepository.save(loopChecklist);
    }

    // 하위목표 삭제
    @Override
    @Transactional
    public void deleteSubGoal(Long subGoalId, CurrentUserDto currentUser) {
        LoopChecklist loopChecklist = subGoalRepository.findById(subGoalId)
                .orElseThrow(() -> new ServiceException(ReturnCode.SUB_GOAL_NOT_FOUND));
        // 작성자 검증 아닌 경우 예외 처리
        validateSubGoalOwner(loopChecklist, currentUser);
        subGoalRepository.delete(loopChecklist);
    }

    // 상위목표 내의 하위목표 전체 삭제
    @Override
    @Transactional
    public void deleteAllSubGoal(Long loopId){
        List<LoopChecklist> loopChecklists = subGoalRepository.findByLoopId(loopId);
        subGoalRepository.deleteAll(loopChecklists);
    }

    // ----------------- 헬퍼 메서드 -----------------

    // 목표 작성자 검증
    public static void validateSubGoalOwner(LoopChecklist loopChecklist, CurrentUserDto currentUser) {
        if (!loopChecklist.getMember().getId().equals(currentUser.getId())) {
            throw new ServiceException(ReturnCode.NOT_AUTHORIZED);
        }
    }
}
