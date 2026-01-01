package com.loopone.loopinbe.domain.team.teamLoop.serviceImpl;

import com.loopone.loopinbe.domain.account.auth.currentUser.CurrentUserDto;
import com.loopone.loopinbe.domain.account.member.entity.Member;
import com.loopone.loopinbe.domain.account.member.repository.MemberRepository;
import com.loopone.loopinbe.domain.loop.loop.entity.LoopRule;
import com.loopone.loopinbe.domain.loop.loop.repository.LoopRuleRepository;
import com.loopone.loopinbe.domain.team.team.entity.Team;
import com.loopone.loopinbe.domain.team.team.entity.TeamMember;
import com.loopone.loopinbe.domain.team.team.repository.TeamRepository;
import com.loopone.loopinbe.domain.team.teamLoop.dto.req.TeamLoopCreateRequest;
import com.loopone.loopinbe.domain.team.teamLoop.dto.res.TeamLoopListResponse;
import com.loopone.loopinbe.domain.team.teamLoop.entity.TeamLoop;
import com.loopone.loopinbe.domain.team.teamLoop.entity.TeamLoopChecklist;
import com.loopone.loopinbe.domain.team.teamLoop.entity.TeamLoopMemberCheck;
import com.loopone.loopinbe.domain.team.teamLoop.entity.TeamLoopMemberProgress;
import com.loopone.loopinbe.domain.team.teamLoop.enums.TeamLoopType;
import com.loopone.loopinbe.domain.team.teamLoop.repository.TeamLoopChecklistRepository;
import com.loopone.loopinbe.domain.team.teamLoop.repository.TeamLoopMemberCheckRepository;
import com.loopone.loopinbe.domain.team.teamLoop.repository.TeamLoopMemberProgressRepository;
import com.loopone.loopinbe.domain.team.teamLoop.repository.TeamLoopRepository;
import com.loopone.loopinbe.domain.team.teamLoop.service.TeamLoopService;
import com.loopone.loopinbe.global.exception.ReturnCode;
import com.loopone.loopinbe.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TeamLoopServiceImpl implements TeamLoopService {
    private final TeamRepository teamRepository;
    private final MemberRepository memberRepository;
    private final LoopRuleRepository loopRuleRepository;
    private final TeamLoopRepository teamLoopRepository;
    private final TeamLoopChecklistRepository teamLoopChecklistRepository;
    private final TeamLoopMemberProgressRepository teamLoopMemberProgressRepository;
    private final TeamLoopMemberCheckRepository teamLoopMemberCheckRepository;

    //팀 루프 리스트 조회
    @Override
    public List<TeamLoopListResponse> getTeamLoops(Long teamId, LocalDate targetDate, CurrentUserDto currentUser) {
        List<TeamLoop> teamLoops = teamLoopRepository.findAllByTeamIdAndDate(teamId, targetDate);
        Long myId = currentUser.id();

        return teamLoops.stream()
                .map(loop -> {
                    //참여 여부
                    boolean isParticipating = loop.isParticipating(myId);
                    //해당 루프의 내 진행률
                    double myProgress = isParticipating ?
                            loop.calculatePersonalProgress(myId) : 0.0;
                    //해당 루프의 팀 진행률
                    double teamProgress = loop.calculateTeamProgress();

                    return TeamLoopListResponse.builder()
                            .id(loop.getId())
                            .title(loop.getTitle())
                            .loopDate(loop.getLoopDate())
                            .type(loop.getType())
                            .importance(loop.getImportance())
                            .teamProgress(teamProgress)
                            .personalProgress(myProgress)
                            .isParticipating(isParticipating)
                            .build();
                })
                .collect(Collectors.toList());
    }

    //팀 루프 생성
    @Override
    @Transactional
    public Long createTeamLoop(Long teamId, TeamLoopCreateRequest requestDTO, CurrentUserDto currentUser) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new ServiceException(ReturnCode.TEAM_NOT_FOUND));

        Member creator = memberRepository.findById(currentUser.id())
                .orElseThrow(() -> new ServiceException(ReturnCode.USER_NOT_FOUND));

        LoopRule loopRule;
        switch (requestDTO.scheduleType()) {
            case NONE -> {
                return createSingleTeamLoop(team, requestDTO);
            }
            case WEEKLY -> {
                loopRule = createLoopRule(requestDTO, creator);
                return createWeeklyTeamLoops(team, requestDTO, loopRule);
            }
            case MONTHLY -> {
                loopRule = createLoopRule(requestDTO, creator);
                return createMonthlyTeamLoops(team, requestDTO, loopRule);
            }
            case YEARLY -> {
                loopRule = createLoopRule(requestDTO, creator);
                return createYearlyTeamLoops(team, requestDTO, loopRule);
            }
            default -> throw new ServiceException(ReturnCode.UNKNOWN_SCHEDULE_TYPE);
        }
    }

        // ========== 비즈니스 로직 메서드 ==========
    private Long createSingleTeamLoop(Team team, TeamLoopCreateRequest requestDTO) {
        LocalDate date = (requestDTO.specificDate() == null) ? LocalDate.now() : requestDTO.specificDate();

        TeamLoop teamLoop = buildTeamLoop(team, requestDTO, date, null);
        teamLoopRepository.save(teamLoop);

        // 하위 엔티티(체크리스트, 참여자 진행판) 생성
        createSubEntitiesForLoop(teamLoop, team, requestDTO);

        return teamLoop.getId();
    }

    // 매주 반복 루프
    private Long createWeeklyTeamLoops(Team team, TeamLoopCreateRequest requestDTO, LoopRule loopRule) {
        List<TeamLoop> loopsToCreate = new ArrayList<>();

        // LoopServiceImpl과 동일한 날짜 순회 로직
        for (LocalDate currentDate = loopRule.getStartDate();
             !currentDate.isAfter(loopRule.getEndDate());
             currentDate = currentDate.plusDays(1)) {

            if (loopRule.getDaysOfWeek().contains(currentDate.getDayOfWeek())) {
                loopsToCreate.add(buildTeamLoop(team, requestDTO, currentDate, loopRule));
            }
        }

        return saveTeamLoopsAndSubEntities(loopsToCreate, team, requestDTO);
    }

    // 매월 반복 루프
    private Long createMonthlyTeamLoops(Team team, TeamLoopCreateRequest requestDTO, LoopRule loopRule) {
        List<TeamLoop> loopsToCreate = new ArrayList<>();
        LocalDate currentDate = loopRule.getStartDate();
        int monthsToAdd = 0;

        // 시작일이 과거라면 오늘 이후로 보정
        while (currentDate.isBefore(LocalDate.now())) {
            currentDate = loopRule.getStartDate().plusMonths(++monthsToAdd);
        }

        while (!currentDate.isAfter(loopRule.getEndDate())) {
            loopsToCreate.add(buildTeamLoop(team, requestDTO, currentDate, loopRule));
            monthsToAdd++;
            currentDate = loopRule.getStartDate().plusMonths(monthsToAdd);
        }

        return saveTeamLoopsAndSubEntities(loopsToCreate, team, requestDTO);
    }

    // 매년 반복 루프
    private Long createYearlyTeamLoops(Team team, TeamLoopCreateRequest requestDTO, LoopRule loopRule) {
        List<TeamLoop> loopsToCreate = new ArrayList<>();
        LocalDate currentDate = loopRule.getStartDate();
        int yearsToAdd = 0;

        while (currentDate.isBefore(LocalDate.now())) {
            currentDate = loopRule.getStartDate().plusYears(++yearsToAdd);
        }

        while (!currentDate.isAfter(loopRule.getEndDate())) {
            loopsToCreate.add(buildTeamLoop(team, requestDTO, currentDate, loopRule));
            yearsToAdd++;
            currentDate = loopRule.getStartDate().plusYears(yearsToAdd);
        }

        return saveTeamLoopsAndSubEntities(loopsToCreate, team, requestDTO);
    }

    // LoopRule(규칙) 생성
    private LoopRule createLoopRule(TeamLoopCreateRequest requestDTO, Member creator) {
        LocalDate start = (requestDTO.startDate() == null) ? LocalDate.now() : requestDTO.startDate();
        LocalDate end = (requestDTO.endDate() == null) ? start.plusYears(5) : requestDTO.endDate();

        LoopRule loopRule = LoopRule.builder()
                .member(creator)
                .scheduleType(requestDTO.scheduleType())
                .daysOfWeek(requestDTO.daysOfWeek())
                .startDate(start)
                .endDate(end)
                .build();

        loopRuleRepository.save(loopRule);
        return loopRule;
    }

    // TeamLoop 객체 빌드 (저장 전 메모리 객체)
    private TeamLoop buildTeamLoop(Team team, TeamLoopCreateRequest requestDTO, LocalDate date, LoopRule loopRule) {
        return TeamLoop.builder()
                .team(team)
                .loopRule(loopRule)
                .title(requestDTO.title())
                .content(requestDTO.content())
                .loopDate(date)
                .type(requestDTO.type())
                .importance(requestDTO.importance())
                .build();
    }

    // 리스트 일괄 저장 및 하위 엔티티 처리 헬퍼
    private Long saveTeamLoopsAndSubEntities(List<TeamLoop> loops, Team team, TeamLoopCreateRequest requestDTO) {
        if (!loops.isEmpty()) {
            teamLoopRepository.saveAll(loops); // Batch Insert

            // 각 루프에 대해 체크리스트 및 참여자 생성
            for (TeamLoop loop : loops) {
                createSubEntitiesForLoop(loop, team, requestDTO);
            }
            return loops.get(0).getId(); // 첫 번째 루프 ID 반환 (LoopServiceImpl 패턴 유지)
        }
        return null;
    }

    // 체크리스트, 참여자 Progress/Check 생성 로직
    private void createSubEntitiesForLoop(TeamLoop teamLoop, Team team, TeamLoopCreateRequest requestDTO) {
        // 체크리스트 생성
        List<TeamLoopChecklist> checklists = new ArrayList<>();
        if (requestDTO.checklists() != null && !requestDTO.checklists().isEmpty()) {
            checklists = requestDTO.checklists().stream()
                    .map(content -> TeamLoopChecklist.builder()
                            .teamLoop(teamLoop)
                            .content(content)
                            .build())
                    .collect(Collectors.toList());
            teamLoopChecklistRepository.saveAll(checklists);
        }

        // 참여자 결정 (공통/개인)
        List<Member> participants = getParticipants(team, requestDTO);

        // 참여자별 Progress, Check 생성
        for (Member member : participants) {
            // Progress 생성
            TeamLoopMemberProgress progress = TeamLoopMemberProgress.builder()
                    .teamLoop(teamLoop)
                    .member(member)
                    .build();
            teamLoopMemberProgressRepository.save(progress);

            // Check 생성
            if (!checklists.isEmpty()) {
                List<TeamLoopMemberCheck> checks = checklists.stream()
                        .map(checklist -> TeamLoopMemberCheck.builder()
                                .memberProgress(progress) // Progress와 연결
                                .checklist(checklist)     // Checklist와 연결
                                .isChecked(false)
                                .build())
                        .collect(Collectors.toList());
                teamLoopMemberCheckRepository.saveAll(checks);
            }
        }
    }

    // 참여자 목록 필터링
    private List<Member> getParticipants(Team team, TeamLoopCreateRequest requestDTO) {
        //팀원들의 객체 리스트
        List<Member> TeamMembers = team.getTeamMembers().stream()
                .map(TeamMember::getMember)
                .collect(Collectors.toList());

        if (requestDTO.type() == TeamLoopType.COMMON) {
            //공통인 경우 팀원 전체 반환
            return TeamMembers;
        } else {
            //개인인 경우 해당하는 팀원만 반환
            List<Long> targetIds = requestDTO.targetMemberIds();

            if (targetIds == null || targetIds.isEmpty()) {
                throw new ServiceException(ReturnCode.INVALID_REQUEST_TEAM);
            }

            //실제 팀 맴버의 ID
            List<Long> actualMemberIds = TeamMembers.stream()
                    .map(Member::getId)
                    .toList();

            //요청 ID 중 팀원이 아닌 ID가 있는지 검사
            boolean allMatch = actualMemberIds.containsAll(targetIds);
            if (!allMatch) {
                throw new ServiceException(ReturnCode.USER_NOT_IN_TEAM);
            }

            return TeamMembers.stream()
                    .filter(m -> targetIds.contains(m.getId()))
                    .collect(Collectors.toList());
        }
    }
}
