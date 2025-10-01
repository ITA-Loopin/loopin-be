package com.loopone.loopinbe.domain.loop.loop.serviceImpl;

import com.loopone.loopinbe.domain.account.auth.currentUser.CurrentUserDto;
import com.loopone.loopinbe.domain.account.member.mapper.MemberMapper;
import com.loopone.loopinbe.domain.loop.loop.dto.req.LoopRequest;
import com.loopone.loopinbe.domain.loop.loop.dto.res.LoopResponse;
import com.loopone.loopinbe.domain.loop.loop.dto.res.LoopWithCheckListResponse;
import com.loopone.loopinbe.domain.loop.loop.entity.Loop;
import com.loopone.loopinbe.domain.loop.loop.entity.LoopPage;
import com.loopone.loopinbe.domain.loop.loop.repository.LoopRepository;
import com.loopone.loopinbe.domain.loop.loop.service.LoopService;
import com.loopone.loopinbe.domain.loop.subGoal.dto.res.SubGoalResponse;
import com.loopone.loopinbe.domain.loop.subGoal.entity.SubGoal;
import com.loopone.loopinbe.domain.loop.subGoal.repository.SubGoalRepository;
import com.loopone.loopinbe.domain.loop.subGoal.service.SubGoalService;
import com.loopone.loopinbe.global.common.response.PageResponse;
import com.loopone.loopinbe.global.exception.ReturnCode;
import com.loopone.loopinbe.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoopServiceImpl implements LoopService {
    private final LoopRepository loopRepository;
    private final SubGoalRepository subGoalRepository;
    private final SubGoalService subGoalService;
    private final MemberMapper memberMapper;

    // 목표 생성
    @Override
    @Transactional
    public void addLoop(LoopRequest loopRequest, CurrentUserDto currentUser){
        Loop loop = Loop.builder()
                .member(memberMapper.toMember(currentUser))
                .content(loopRequest.getContent())
                .deadline(loopRequest.getDeadline())
                .checked(loopRequest.getChecked())
                .build();
        loopRepository.save(loop);
    }

    // 목표 전체 리스트 조회
    @Override
    @Transactional(readOnly = true)
    public PageResponse<LoopWithCheckListResponse> getAllLoop(Pageable pageable, CurrentUserDto currentUser) {
        checkPageSize(pageable.getPageSize());

        // 1) 메인 목표 페이지 조회 (DB에서 NULLS LAST 처리)
        Page<Loop> mainPage = loopRepository.findByMemberIdWithOrder(currentUser.getId(), pageable);

        // 2) 해당 페이지에 포함된 메인 목표 ID 추출
        List<Long> mainIds = mainPage.getContent().stream()
                .map(Loop::getId)
                .toList();
        // 3) 하위 목표 벌크 조회 (DB에서 loop.id ASC, deadline ASC NULLS LAST 처리)
        List<SubGoal> subGoals = mainIds.isEmpty()
                ? List.of()
                : subGoalRepository.findByLoopIdInWithOrder(mainIds);

        // 4) 하위 목표들을 loopId 기준으로 그룹핑
        Map<Long, List<SubGoal>> subByMainId = new LinkedHashMap<>();
        for (SubGoal sg : subGoals) {
            Long mid = sg.getLoop().getId();
            subByMainId.computeIfAbsent(mid, k -> new ArrayList<>()).add(sg);
        }
        // 5) DTO 변환
        List<LoopWithCheckListResponse> content = new ArrayList<>(mainPage.getNumberOfElements());
        for (Loop mg : mainPage.getContent()) {
            LoopResponse mainDto = convertToLoopResponse(mg);
            List<SubGoalResponse> subDtos = subByMainId.getOrDefault(mg.getId(), List.of())
                    .stream()
                    .map(this::convertToSubGoalResponse)
                    .toList();
            content.add(LoopWithCheckListResponse.builder()
                    .loop(mainDto)
                    .subGoals(subDtos)
                    .build());
        }
        // 6) Page로 감싸서 반환
        return PageResponse.of(new PageImpl<>(content, mainPage.getPageable(), mainPage.getTotalElements()));
    }

    // 목표 수정
    @Override
    @Transactional
    public void updateLoop(Long loopId, LoopRequest loopRequest, CurrentUserDto currentUser){
        Loop loop = loopRepository.findById(loopId).orElseThrow(() -> new ServiceException(ReturnCode.MAIN_GOAL_NOT_FOUND));
        // 작성자 검증 아닌 경우 예외 처리
        validateLoopOwner(loop, currentUser);
        if (loopRequest.getContent() != null) {
            loop.setContent(loopRequest.getContent());
        }
        if (loopRequest.getDeadline() != null) {
            loop.setDeadline(loopRequest.getDeadline());
        }
        if (loopRequest.getChecked() != null) {
            loop.setChecked(loopRequest.getChecked());
        }
        loopRepository.save(loop);
    }

    // 목표 삭제
    @Override
    @Transactional
    public void deleteLoop(Long loopId, CurrentUserDto currentUser) {
        Loop loop = loopRepository.findById(loopId).orElseThrow(() -> new ServiceException(ReturnCode.MAIN_GOAL_NOT_FOUND));
        // 작성자 검증 아닌 경우 예외 처리
        validateLoopOwner(loop, currentUser);
        deleteSubGoals(loopId);
        loopRepository.delete(loop);
    }

    // 해당 목표의 모든 하위 목표 삭제
    private void deleteSubGoals(Long loopId) {
        subGoalService.deleteAllSubGoal(loopId);
    }

    // ----------------- 헬퍼 메서드 -----------------

    // 요청 페이지 수 제한
    private void checkPageSize(int pageSize) {
        int maxPageSize = LoopPage.getMaxPageSize();
        if (pageSize > maxPageSize) {
            throw new ServiceException(ReturnCode.PAGE_REQUEST_FAIL);
        }
    }

    // 목표 작성자 검증
    public static void validateLoopOwner(Loop loop, CurrentUserDto currentUser) {
        if (!loop.getMember().getId().equals(currentUser.getId())) {
            throw new ServiceException(ReturnCode.NOT_AUTHORIZED);
        }
    }

    // Loop를 LoopResponse로 변환
    private LoopResponse convertToLoopResponse(Loop loop) {
        return LoopResponse.builder()
                .id(loop.getId())
                .content(loop.getContent())
                .dDay(calculateDDay(loop.getDeadline()))
                .checked(loop.getChecked())
                .createdAt(loop.getCreatedAt())
                .build();
    }

    // SubGoal를 SubGoalResponse로 변환
    private SubGoalResponse convertToSubGoalResponse(SubGoal subGoal) {
        return SubGoalResponse.builder()
                .id(subGoal.getId())
                .loopId(subGoal.getLoop().getId())
                .content(subGoal.getContent())
                .dDay(calculateDDay(subGoal.getDeadline()))
                .checked(subGoal.getChecked())
                .createdAt(subGoal.getCreatedAt())
                .build();
    }

    // D-Day 계산 함수
    private String calculateDDay(LocalDate deadline) {
        if (deadline == null) {
            return null;
        }
        long daysBetween = ChronoUnit.DAYS.between(LocalDate.now(), deadline);
        if (daysBetween > 0) {
            return "D-" + daysBetween;   // 마감일이 미래
        } else if (daysBetween == 0) {
            return "D-Day";              // 오늘이 마감일
        } else {
            return "D+" + Math.abs(daysBetween); // 마감일이 지남
        }
    }

    private static String safe(String s) {
        return s == null ? "" : s.replaceAll("\\s+", " ").trim();
    }
}
