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
import com.loopone.loopinbe.domain.loop.checkList.dto.res.LoopCheckListResponse;
import com.loopone.loopinbe.domain.loop.checkList.entity.LoopCheckList;
import com.loopone.loopinbe.domain.loop.checkList.repository.LoopCheckListRepository;
import com.loopone.loopinbe.domain.loop.checkList.service.LoopCheckListService;
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
    private final LoopCheckListRepository loopCheckListRepository;
    private final LoopCheckListService loopCheckListService;
    private final MemberMapper memberMapper;

    // 목표 생성
    @Override
    @Transactional
    public void addLoop(LoopRequest loopRequest, CurrentUserDto currentUser){
        Loop loop = Loop.builder()
                .member(memberMapper.toMember(currentUser))
                .title(loopRequest.getTitle())
                .content(loopRequest.getContent())
                .loopDate(loopRequest.getLoopDate())
                .build();

/*        // 요청받은 checklist 내용으로 LoopChecklist 엔티티 생성 및 연관관계 설정
        if (loopRequest.getChecklists() != null) {
            for (String checklistContent : loopRequest.getChecklists()) {
                LoopChecklist checklist = LoopChecklist.builder()
                        .content(checklistContent)
                        .completed(false) // 생성 시 기본값은 false
                        .build();
                loop.addChecklist(checklist); // 연관관계 편의 메서드 사용
            }
        }*/

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
        List<LoopCheckList> loopCheckLists = mainIds.isEmpty()
                ? List.of()
                : loopCheckListRepository.findByLoopIdInWithOrder(mainIds);

        // 4) 하위 목표들을 loopId 기준으로 그룹핑
        Map<Long, List<LoopCheckList>> subByMainId = new LinkedHashMap<>();
        for (LoopCheckList sg : loopCheckLists) {
            Long mid = sg.getLoop().getId();
            subByMainId.computeIfAbsent(mid, k -> new ArrayList<>()).add(sg);
        }
        // 5) DTO 변환
        List<LoopWithCheckListResponse> content = new ArrayList<>(mainPage.getNumberOfElements());
        for (Loop mg : mainPage.getContent()) {
            LoopResponse mainDto = convertToLoopResponse(mg);
            List<LoopCheckListResponse> subDtos = subByMainId.getOrDefault(mg.getId(), List.of())
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
        Loop loop = loopRepository.findById(loopId).orElseThrow(() -> new ServiceException(ReturnCode.LOOP_NOT_FOUND));

        // 작성자 검증 아닌 경우 예외 처리
        validateLoopOwner(loop, currentUser);

        if (loopRequest.getTitle() != null) loop.setTitle(loopRequest.getTitle());
        if (loopRequest.getContent() != null) loop.setContent(loopRequest.getContent());
        if (loopRequest.getLoopDate() != null) loop.setLoopDate(loopRequest.getLoopDate());

        //TODO: 체크리스트 업데이트 로직

        loopRepository.save(loop);
    }

    // 목표 삭제
    @Override
    @Transactional
    public void deleteLoop(Long loopId, CurrentUserDto currentUser) {
        Loop loop = loopRepository.findById(loopId).orElseThrow(() -> new ServiceException(ReturnCode.LOOP_NOT_FOUND));

        // 작성자 검증 아닌 경우 예외 처리
        validateLoopOwner(loop, currentUser);

        loopRepository.delete(loop);
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
        //TODO: 진행률 계산
        //TODO: 체크리스트 DTO로 변환

        return LoopResponse.builder()
                .id(loop.getId())
                .title(loop.getTitle())
                .content(loop.getContent())
                .loopDate(loop.getLoopDate())
                .build();
    }

    // SubGoal를 SubGoalResponse로 변환
    private LoopCheckListResponse convertToSubGoalResponse(LoopCheckList loopChecklist) {
        return LoopCheckListResponse.builder()
                .id(loopChecklist.getId())
                .loopId(loopChecklist.getLoop().getId())
                .content(loopChecklist.getContent())
                .dDay(calculateDDay(loopChecklist.getDeadline()))
                .checked(loopChecklist.getCompleted())
                .createdAt(loopChecklist.getCreatedAt())
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
