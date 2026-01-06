package com.loopone.loopinbe.domain.team.teamLoop.serviceImpl;

import com.loopone.loopinbe.domain.account.auth.currentUser.CurrentUserDto;
import com.loopone.loopinbe.domain.account.member.entity.Member;
import com.loopone.loopinbe.domain.account.member.repository.MemberRepository;
import com.loopone.loopinbe.domain.loop.loop.entity.LoopPage;
import com.loopone.loopinbe.domain.loop.loop.entity.LoopRule;
import com.loopone.loopinbe.domain.loop.loop.enums.RepeatType;
import com.loopone.loopinbe.domain.loop.loop.repository.LoopRuleRepository;
import com.loopone.loopinbe.domain.team.team.entity.Team;
import com.loopone.loopinbe.domain.team.team.entity.TeamMember;
import com.loopone.loopinbe.domain.team.team.repository.TeamMemberRepository;
import com.loopone.loopinbe.domain.team.team.repository.TeamRepository;
import com.loopone.loopinbe.domain.team.teamLoop.dto.req.TeamLoopChecklistCreateRequest;
import com.loopone.loopinbe.domain.team.teamLoop.dto.req.TeamLoopCreateRequest;
import com.loopone.loopinbe.domain.team.teamLoop.dto.res.TeamLoopChecklistResponse;
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
import org.checkerframework.checker.units.qual.A;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
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
    private final TeamMemberRepository teamMemberRepository;

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
        validateTeamMember(teamId, currentUser.id());

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

    // (A) teamsToDelete: 팀 자체가 삭제될 팀들 -> 팀에 속한 루프/체크/진행률/체크리스트 "전체 삭제"
    // (B) remainingTeamIds: 팀은 남고 내가 탈퇴하는 팀들 -> 내 체크/내 진행률만 삭제
    @Override
    public void deleteMyTeamLoops(Long memberId, List<Long> teamsToDelete, List<Long> remainingTeamIds) {
        // (A) 팀 삭제 대상: teamId 기준으로 전부 삭제 + 연결된 LoopRule(고아만) 삭제
        if (teamsToDelete != null && !teamsToDelete.isEmpty()) {
            // 1) TeamLoop 삭제 전에, 해당 팀들의 loopRuleId를 미리 수집 (FK 때문에 필수)
            List<Long> loopRuleIds = teamLoopRepository.findDistinctLoopRuleIdsByTeamIds(teamsToDelete);

            // FK 삭제 순서: check -> progress -> checklist -> teamLoop
            teamLoopMemberCheckRepository.deleteByTeamIds(teamsToDelete);
            teamLoopMemberProgressRepository.deleteByTeamIds(teamsToDelete);
            teamLoopChecklistRepository.deleteByTeamIds(teamsToDelete);
            teamLoopRepository.deleteByTeamIds(teamsToDelete);

            // 2) 이제 TeamLoop가 삭제됐으니, 연결됐던 LoopRule 중 "어디에서도 참조되지 않는 것"만 삭제
            if (loopRuleIds != null && !loopRuleIds.isEmpty()) {
                loopRuleRepository.deleteOrphanByIds(loopRuleIds);
            }
        }
        // (B) 팀은 남음: 내 흔적만 삭제
        if (remainingTeamIds != null && !remainingTeamIds.isEmpty()) {
            teamLoopMemberCheckRepository.deleteByMemberAndTeamIds(memberId, remainingTeamIds);
            teamLoopMemberProgressRepository.deleteByMemberAndTeamIds(memberId, remainingTeamIds);
        }
    }

    @Override
    @Transactional
    public void transferTeamLoopRuleOwner(Long teamId, Long oldLeaderId, Member newLeader) {
        loopRuleRepository.transferOwnerByTeamId(teamId, oldLeaderId, newLeader);
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
                .daysOfWeek(requestDTO.scheduleType() == RepeatType.WEEKLY
                        ? toDayOfWeekSet(requestDTO.daysOfWeek())
                        : null)
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
        // 참여자 결정 (공통/개인)
        List<Member> participants = getParticipants(team, requestDTO);
        List<TeamLoopMemberProgress> savedProgresses = new ArrayList<>();

        // 참여자별 데이터 생성
        for (Member member : participants) {
            // Progress 생성
            TeamLoopMemberProgress progress = TeamLoopMemberProgress.builder()
                    .teamLoop(teamLoop)
                    .member(member)
                    .build();
            savedProgresses.add(teamLoopMemberProgressRepository.save(progress));

            // 체크리스트 및 체크 현황 생성
            if (requestDTO.checklists() != null && !requestDTO.checklists().isEmpty()) {
                if (teamLoop.getType() == TeamLoopType.COMMON) {
                    // 공통 루프인 경우 -> 체크리스트를 한 번만 만들고 공유
                    // 공통 루프는 별도로 처리
                } else {
                    // 개인 루프인 경우 -> 각 멤버마다 전용 체크리스트 생성
                    List<TeamLoopChecklist> myChecklists = requestDTO.checklists().stream()
                            .map(content -> TeamLoopChecklist.builder()
                                    .teamLoop(teamLoop)
                                    .content(content)
                                    .owner(member) // 체크리스트 주인 지정
                                    .build())
                            .collect(Collectors.toList());
                    teamLoopChecklistRepository.saveAll(myChecklists);

                    // 각 멤버에 대한 체크 현황 생성
                    List<TeamLoopMemberCheck> myChecks = myChecklists.stream()
                            .map(cl -> TeamLoopMemberCheck.builder()
                                    .memberProgress(progress)
                                    .checklist(cl)
                                    .isChecked(false)
                                    .build())
                            .collect(Collectors.toList());
                    teamLoopMemberCheckRepository.saveAll(myChecks);
                }
            }
        }

        // 공통 루프인 경우 체크리스트 생성
        if (teamLoop.getType() == TeamLoopType.COMMON && requestDTO.checklists() != null) {
            List<TeamLoopChecklist> commonChecklists = requestDTO.checklists().stream()
                    .map(content -> TeamLoopChecklist.builder()
                            .teamLoop(teamLoop)
                            .content(content)
                            .owner(null) // 주인 없음 (공용)
                            .build())
                    .collect(Collectors.toList());
            teamLoopChecklistRepository.saveAll(commonChecklists);

            // 모든 참여자의 체크 현황 생성
            for (TeamLoopMemberProgress progress : savedProgresses) {
                List<TeamLoopMemberCheck> checks = commonChecklists.stream()
                        .map(cl -> TeamLoopMemberCheck.builder()
                                .memberProgress(progress)
                                .checklist(cl)
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

    // ========== 검증 메서드 ==========
    // 팀원 검증
    private void validateTeamMember(Long teamId, Long memberId) {
        if (!teamMemberRepository.existsByTeamIdAndMemberId(teamId, memberId)) {
            throw new ServiceException(ReturnCode.USER_NOT_IN_TEAM);
        }
    }

    // ========== 헬퍼 메서드 ==========
    // List -> Set 변환
    private Set<DayOfWeek> toDayOfWeekSet(List<DayOfWeek> days) {
        if (days == null || days.isEmpty()) return null; // WEEKLY 아니면 null 저장하려는 의도 유지
        return EnumSet.copyOf(days); // 중복 제거 + Enum 최적화 Set
    }
}
