package com.loopone.loopinbe.domain.loop.loop.serviceImpl;

import com.loopone.loopinbe.domain.account.auth.currentUser.CurrentUserDto;
import com.loopone.loopinbe.domain.loop.loop.dto.req.LoopCreateRequest;
import com.loopone.loopinbe.domain.loop.loop.dto.req.LoopUpdateRequest;
import com.loopone.loopinbe.domain.loop.loop.dto.res.DailyLoopsResponse;
import com.loopone.loopinbe.domain.loop.loop.dto.res.LoopDetailResponse;
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
    public void createLoop(LoopCreateRequest requestDTO, CurrentUserDto currentUser){

        switch (requestDTO.scheduleType()) {
            case NONE:
                createSingleLoop(requestDTO, currentUser);
                break;
            case WEEKLY:
                createWeeklyLoops(requestDTO, currentUser);
                break;
            case MONTHLY:
                createMonthlyLoops(requestDTO, currentUser);
                break;
            case YEARLY:
                createYearlyLoops(requestDTO, currentUser);
                break;
        }
    }



    //루프 상세 조회
    @Override
    public LoopDetailResponse getDetailLoop(Long loopId, CurrentUserDto currentUser) {
        //루프 조회
        Loop loop = loopRepository.findById(loopId).orElseThrow(() -> new ServiceException(ReturnCode.LOOP_NOT_FOUND));

        //루프의 소유자가 현재 사용자인지 확인
        validateLoopOwner(loop, currentUser);

        return loopMapper.toDetailResponse(loop);
    }

    //날짜별 루프 리스트 조회
    @Override
    public DailyLoopsResponse getDailyLoops(LocalDate date, CurrentUserDto currentUser) {
        //루프 리스트 조회
        List<Loop> DailyLoops = loopRepository.findByMemberIdAndLoopDate(currentUser.id(), date);

        return loopMapper.toDailyLoopsResponse(DailyLoops);
    }

/*    //루프 전체 리스트 조회
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
    }*/

    //단일 루프 수정
    @Override
    @Transactional
    public void updateLoop(Long loopId, LoopUpdateRequest requestDTO, CurrentUserDto currentUser){
        //루프 조회
        Loop loop = loopRepository.findById(loopId).orElseThrow(() -> new ServiceException(ReturnCode.LOOP_NOT_FOUND));

        //루프의 소유자가 현재 사용자인지 확인
        validateLoopOwner(loop, currentUser);

        //그룹 연결 해제 (단일 수정을 하는 경우, 독립적인 루프가 되기에)
        loop.setLoopGroup(null);

        //루프 정보 수정
        if (requestDTO.title() != null) loop.setTitle(requestDTO.title());
        if (requestDTO.content() != null) loop.setContent(requestDTO.content());
        if (requestDTO.specificDate() != null) loop.setLoopDate(requestDTO.specificDate());

        //체크리스트 수정
        if(requestDTO.checklists() != null){
            for(String cl : requestDTO.checklists()){
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

    // ========== 비즈니스 로직 메서드 ==========
    //단일 루프 (반복x)
    private void createSingleLoop(LoopCreateRequest requestDTO, CurrentUserDto currentUser) {
        LocalDate date = (requestDTO.specificDate() == null) ? LocalDate.now() : requestDTO.specificDate(); //null이면 당일로 설정
        Loop loop = buildLoop(requestDTO, currentUser, date, null);
        loopRepository.save(loop);
    }

    //매주 반복 루프
    private void createWeeklyLoops(LoopCreateRequest requestDTO, CurrentUserDto currentUser) {
        LocalDate start = (requestDTO.startDate() == null) ? LocalDate.now() : requestDTO.startDate();
        LocalDate end = (requestDTO.endDate() == null) ? start.plusYears(1) : requestDTO.endDate();

        //고유 그룹id UUID로 생성
        String groupId = UUID.randomUUID().toString();

        List<Loop> loopsToCreate = new ArrayList<>();
        for (LocalDate currentDate = start; !currentDate.isAfter(end); currentDate = currentDate.plusDays(1)) {
            if (requestDTO.daysOfWeek().contains(currentDate.getDayOfWeek())) {
                loopsToCreate.add(buildLoop(requestDTO, currentUser, currentDate, groupId));
            }
        }

        //만들어진 루프 DB에 저장
        if (!loopsToCreate.isEmpty()) {
            loopRepository.saveAll(loopsToCreate);
        }
    }

    //매월 반복 루프
    private void createMonthlyLoops(LoopCreateRequest requestDTO, CurrentUserDto currentUser) {
        LocalDate start = (requestDTO.startDate() == null) ? LocalDate.now() : requestDTO.startDate();
        LocalDate end = (requestDTO.endDate() == null) ? start.plusYears(1) : requestDTO.endDate();

        String groupId = UUID.randomUUID().toString();

        List<Loop> loopsToCreate = new ArrayList<>();
        LocalDate currentDate = start;
        int monthsToAdd = 0; //plusMonths에 넣어줄 값을 저장할 변수
        while (!currentDate.isAfter(end)) {
            loopsToCreate.add(buildLoop(requestDTO, currentUser, currentDate, groupId));
            monthsToAdd++;
            //plusMonths로 인해 생기는 보정 문제: 윤년 또는 말일이 유효한 날짜가 아닌 경우, 자동으로 보정을 해주는데 그 다음 계산에서 원복을 하지 않음.
            //ex)3월31일->4월30일(보정)->5월30일(문제발생)
            currentDate = start.plusMonths(monthsToAdd); //시작일을 기준으로 증가하도록 구현하여 보정으로 인해 생기는 문제를 해결
        }
        if (!loopsToCreate.isEmpty()) {
            loopRepository.saveAll(loopsToCreate);
        }
    }

    //매년 반복 루프
    private void createYearlyLoops(LoopCreateRequest requestDTO, CurrentUserDto currentUser) {
        LocalDate start = (requestDTO.startDate() == null) ? LocalDate.now() : requestDTO.startDate();
        LocalDate end = (requestDTO.endDate() == null) ? start.plusYears(5) : requestDTO.endDate();

        String groupId = UUID.randomUUID().toString();

        List<Loop> loopsToCreate = new ArrayList<>();
        LocalDate currentDate = start;
        int yearsToAdd = 0;
        while (!currentDate.isAfter(end)) {
            loopsToCreate.add(buildLoop(requestDTO, currentUser, currentDate, groupId));
            yearsToAdd++;
            currentDate = start.plusYears(yearsToAdd);
        }
        if (!loopsToCreate.isEmpty()) {
            loopRepository.saveAll(loopsToCreate);
        }
    }

    //루프 생성
    private Loop buildLoop(LoopCreateRequest requestDTO, CurrentUserDto currentUser, LocalDate date, String loopGroup) {
        Loop loop = Loop.builder()
                .member(memberConverter.toMember(currentUser))
                .title(requestDTO.title())
                .content(requestDTO.content())
                .loopDate(date)
                .loopGroup(loopGroup)
                .build();

        //입력한 체크리스트가 있다면 해당 루프에 추가
        if (requestDTO.checklists() != null && !requestDTO.checklists().isEmpty()){
            for (String checklistContent : requestDTO.checklists()) {
                loop.addChecklist(LoopChecklist.builder().content(checklistContent).build());
            }
        }
        return loop;
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
