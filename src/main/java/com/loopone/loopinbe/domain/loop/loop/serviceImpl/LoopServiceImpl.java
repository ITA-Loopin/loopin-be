package com.loopone.loopinbe.domain.loop.loop.serviceImpl;

import com.loopone.loopinbe.domain.account.auth.currentUser.CurrentUserDto;
import com.loopone.loopinbe.domain.loop.loop.dto.req.LoopCreateRequest;
import com.loopone.loopinbe.domain.loop.loop.dto.req.LoopUpdateRequest;
import com.loopone.loopinbe.domain.loop.loop.dto.res.LoopSimpleResponse;
import com.loopone.loopinbe.domain.account.member.converter.MemberConverter;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoopServiceImpl implements LoopService {
    private final LoopRepository loopRepository;
    private final LoopChecklistRepository loopChecklistRepository;
    private final LoopMapper loopMapper;
    private final MemberConverter memberConverter;

    //루프 생성
    @Override
    @Transactional
    public void createLoop(LoopCreateRequest loopCreateRequest, CurrentUserDto currentUser){
        //시작일, 종료일 설정 (종료일 미입력 시, 시작일의 1년 후)
        LocalDate start = loopCreateRequest.startDate();
        LocalDate end = (loopCreateRequest.endDate() == null) ? start.plusYears(1) : loopCreateRequest.endDate();

        //고유 그룹id UUID로 생성
        String groupId = UUID.randomUUID().toString();

        List<Loop> loopToCreate = new ArrayList<>();

        //오늘부터 종료일까지 반복
        for(LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)){
            //입력한 요일 목록에 포함되어있다면 현재 date에 루프 생성
            if(loopCreateRequest.daysOfWeek().contains(date.getDayOfWeek())){
                Loop loop = Loop.builder()
                        .member(memberConverter.toMember(currentUser))
                        .title(loopCreateRequest.title())
                        .content(loopCreateRequest.content())
                        .loopDate(date)
                        .loopGroup(groupId)
                        .build();

                //입력한 체크리스트가 있다면 해당 루프에 추가
                if(loopCreateRequest.checklists() != null && !loopCreateRequest.checklists().isEmpty()){
                    for(String cl : loopCreateRequest.checklists()){
                        loop.addChecklist(LoopChecklist.builder().content(cl).build());
                    }
                }
                loopToCreate.add(loop);
            }
        }
        //만들어진 루프 DB에 저장
        if(!loopToCreate.isEmpty()){
            loopRepository.saveAll(loopToCreate);
        }
    }

    //루프 전체 리스트 조회
    @Override
    @Transactional(readOnly = true)
    public PageResponse<LoopSimpleResponse> getAllLoop(Pageable pageable, CurrentUserDto currentUser) {
        checkPageSize(pageable.getPageSize());

        //Loop 엔티티 페이지를 DB에서 조회
        Page<Loop> loopPage = loopRepository.findByMemberIdWithOrder(currentUser.id(), pageable);
        List<Long> loopIds = loopPage.stream().map(Loop::getId).toList();

        //모든 체크리스트를 한 번에 조회해서 Map으로 그룹핑
        Map<Long, List<LoopChecklist>> checklistsMap = loopChecklistRepository.findByLoopIdIn(loopIds)
                .stream()
                .collect(Collectors.groupingBy(cl -> cl.getLoop().getId())); // Stream의 groupingBy를 사용해 한 줄로 그룹핑

        //엔티티 페이지를 DTO 페이지로 직접 변환
        Page<LoopSimpleResponse> simpleDtoPage = loopPage.map(loopMapper::toSimpleResponse);

        return PageResponse.of(simpleDtoPage);
    }

    //단일 루프 수정
    @Override
    @Transactional
    public void updateLoop(Long loopId, LoopUpdateRequest loopUpdateRequest, CurrentUserDto currentUser){
        //루프 조회
        Loop loop = loopRepository.findById(loopId).orElseThrow(() -> new ServiceException(ReturnCode.LOOP_NOT_FOUND));

        //루프의 소유자가 현재 사용자인지 확인
        validateLoopOwner(loop, currentUser);

        //그룹 연결 해제 (단일 수정을 하는 경우, 독립적인 루프가 되기에)
        loop.setLoopGroup(null);

        //루프 정보 수정
        if (loopUpdateRequest.title() != null) loop.setTitle(loopUpdateRequest.title());
        if (loopUpdateRequest.content() != null) loop.setContent(loopUpdateRequest.content());
        if (loopUpdateRequest.loopDate() != null) loop.setLoopDate(loopUpdateRequest.loopDate());

        //체크리스트 수정
        if(loopUpdateRequest.checklists() != null){
            for(String cl : loopUpdateRequest.checklists()){
                loop.addChecklist(LoopChecklist.builder().content(cl).build());
            }
        }

        loopRepository.save(loop);
    }

    //루프 삭제
    @Override
    @Transactional
    public void deleteLoop(Long loopId, CurrentUserDto currentUser) {
        Loop loop = loopRepository.findById(loopId).orElseThrow(() -> new ServiceException(ReturnCode.LOOP_NOT_FOUND));

        //루프의 소유자가 현재 사용자인지 확인
        validateLoopOwner(loop, currentUser);

        loopRepository.delete(loop);
    }

    // ========== 헬퍼 메서드 ==========
    //요청 페이지 수 제한
    private void checkPageSize(int pageSize) {
        int maxPageSize = LoopPage.getMaxPageSize();
        if (pageSize > maxPageSize) {
            throw new ServiceException(ReturnCode.PAGE_REQUEST_FAIL);
        }
    }

    // ========== 검증 메서드 ==========
    //루프 사용자 검증
    public static void validateLoopOwner(Loop loop, CurrentUserDto currentUser) {
        if (!loop.getMember().getId().equals(currentUser.id())) {
            throw new ServiceException(ReturnCode.NOT_AUTHORIZED);
        }
    }
}
