package com.loopone.loopinbe.domain.loop.loop.serviceImpl;

import com.loopone.loopinbe.domain.account.auth.currentUser.CurrentUserDto;
import com.loopone.loopinbe.domain.account.member.mapper.MemberMapper;
import com.loopone.loopinbe.domain.loop.loop.dto.req.LoopCreateRequest;
import com.loopone.loopinbe.domain.loop.loop.dto.req.LoopUpdateRequest;
import com.loopone.loopinbe.domain.loop.loop.dto.res.LoopSimpleResponse;
import com.loopone.loopinbe.domain.loop.loop.entity.Loop;
import com.loopone.loopinbe.domain.loop.loop.entity.LoopPage;
import com.loopone.loopinbe.domain.loop.loop.mapper.LoopMapper;
import com.loopone.loopinbe.domain.loop.loop.repository.LoopRepository;
import com.loopone.loopinbe.domain.loop.loop.service.LoopService;
import com.loopone.loopinbe.domain.loop.loopChecklist.dto.res.LoopChecklistResponse;
import com.loopone.loopinbe.domain.loop.loopChecklist.entity.LoopChecklist;
import com.loopone.loopinbe.domain.loop.loopChecklist.repository.LoopChecklistRepository;
import com.loopone.loopinbe.domain.loop.loopChecklist.service.LoopChecklistService;
import com.loopone.loopinbe.global.common.response.PageResponse;
import com.loopone.loopinbe.global.exception.ReturnCode;
import com.loopone.loopinbe.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoopServiceImpl implements LoopService {
    private final LoopRepository loopRepository;
    private final LoopChecklistRepository loopChecklistRepository;
    private final MemberMapper memberMapper;
    private final LoopMapper loopMapper;

    // 루프 생성
    @Override
    @Transactional
    public void addLoop(LoopCreateRequest loopCreateRequest, CurrentUserDto currentUser){
        Loop loop = Loop.builder()
                .member(memberMapper.toMember(currentUser))
                .title(loopCreateRequest.title())
                .content(loopCreateRequest.content())
                .loopDate(loopCreateRequest.loopDate())
                .build();

/*        // 요청받은 checklist 내용으로 LoopChecklist 엔티티 생성 및 연관관계 설정
        if (loopCreateRequest.getChecklists() != null) {
            for (String checklistContent : loopCreateRequest.getChecklists()) {
                LoopChecklist checklist = LoopChecklist.builder()
                        .content(checklistContent)
                        .completed(false) // 생성 시 기본값은 false
                        .build();
                loop.addChecklist(checklist); // 연관관계 편의 메서드 사용
            }
        }*/

        loopRepository.save(loop);
    }

    // 루프 전체 리스트 조회
    @Override
    @Transactional(readOnly = true)
    public PageResponse<LoopSimpleResponse> getAllLoop(Pageable pageable, CurrentUserDto currentUser) {
        checkPageSize(pageable.getPageSize());

        // 1. Loop 엔티티 페이지를 DB에서 조회
        Page<Loop> loopPage = loopRepository.findByMemberIdWithOrder(currentUser.getId(), pageable);
        List<Long> loopIds = loopPage.stream().map(Loop::getId).toList();

        // 2. 모든 체크리스트를 한 번에 조회해서 Map으로 그룹핑
        Map<Long, List<LoopChecklist>> checklistsMap = loopChecklistRepository.findByLoopIdIn(loopIds)
                .stream()
                .collect(Collectors.groupingBy(cl -> cl.getLoop().getId())); // Stream의 groupingBy를 사용해 한 줄로 그룹핑

        // 3. 엔티티 페이지를 DTO 페이지로 직접 변환
        Page<LoopSimpleResponse> simpleDtoPage = loopPage.map(loopMapper::toSimpleResponse);

        return PageResponse.of(simpleDtoPage);
    }

    // 루프 수정
    @Override
    @Transactional
    public void updateLoop(Long loopId, LoopUpdateRequest loopUpdateRequest, CurrentUserDto currentUser){
        Loop loop = loopRepository.findById(loopId).orElseThrow(() -> new ServiceException(ReturnCode.LOOP_NOT_FOUND));

        // 작성자 검증 아닌 경우 예외 처리
        validateLoopOwner(loop, currentUser);

        if (loopUpdateRequest.title() != null) loop.setTitle(loopUpdateRequest.title());
        if (loopUpdateRequest.content() != null) loop.setContent(loopUpdateRequest.content());
        if (loopUpdateRequest.loopDate() != null) loop.setLoopDate(loopUpdateRequest.loopDate());

        //TODO: 체크리스트 업데이트 로직

        loopRepository.save(loop);
    }

    // 루프 삭제
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

    // 루프 작성자 검증
    public static void validateLoopOwner(Loop loop, CurrentUserDto currentUser) {
        if (!loop.getMember().getId().equals(currentUser.getId())) {
            throw new ServiceException(ReturnCode.NOT_AUTHORIZED);
        }
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
