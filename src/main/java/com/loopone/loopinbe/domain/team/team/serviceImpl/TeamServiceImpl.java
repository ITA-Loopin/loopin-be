package com.loopone.loopinbe.domain.team.team.serviceImpl;

import com.loopone.loopinbe.domain.account.auth.currentUser.CurrentUserDto;
import com.loopone.loopinbe.domain.account.member.entity.Member;
import com.loopone.loopinbe.domain.account.member.repository.MemberRepository;
import com.loopone.loopinbe.domain.team.team.dto.req.TeamCreateRequest;
import com.loopone.loopinbe.domain.team.team.dto.res.MyTeamResponse;
import com.loopone.loopinbe.domain.team.team.dto.res.RecruitingTeamResponse;
import com.loopone.loopinbe.domain.team.team.dto.res.TeamDetailResponse;
import com.loopone.loopinbe.domain.team.team.entity.Team;
import com.loopone.loopinbe.domain.team.team.entity.TeamMember;
import com.loopone.loopinbe.domain.team.team.mapper.TeamMapper;
import com.loopone.loopinbe.domain.team.team.repository.TeamMemberRepository;
import com.loopone.loopinbe.domain.team.team.repository.TeamRepository;
import com.loopone.loopinbe.domain.team.team.service.TeamService;
import com.loopone.loopinbe.domain.team.teamLoop.entity.TeamLoop;
import com.loopone.loopinbe.domain.team.teamLoop.entity.TeamLoopMemberCheck;
import com.loopone.loopinbe.domain.team.teamLoop.entity.TeamLoopMemberProgress;
import com.loopone.loopinbe.domain.team.teamLoop.repository.TeamLoopRepository;
import com.loopone.loopinbe.global.exception.ReturnCode;
import com.loopone.loopinbe.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TeamServiceImpl implements TeamService {

    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final MemberRepository memberRepository;
    private final TeamMapper teamMapper;
    private final TeamLoopRepository teamLoopRepository;

    @Override
    @Transactional
    public Long createTeam(TeamCreateRequest request, CurrentUserDto currentUser) {
        Member leader = getMemberOrThrow(currentUser.id());

        //팀 엔티티 생성 및 저장
        Team team = saveTeam(request, leader);

        //팀장을 팀원으로 등록
        saveLeaderAsMember(team, leader);

        //초대된 멤버들 등록
        inviteMembers(team, request.invitedNicknames());

        return team.getId();
    }

    @Override
    public List<MyTeamResponse> getMyTeams(CurrentUserDto currentUser) {
        Member member = getMemberOrThrow(currentUser.id());

        //내가 속한 팀 멤버 정보 조회
        List<TeamMember> myTeamMembers = teamMemberRepository.findAllByMember(member);

        LocalDate today = LocalDate.now();

        //DTO 변환
        return myTeamMembers.stream()
                .map(teamMapper::toMyTeamResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<RecruitingTeamResponse> getRecruitingTeams(CurrentUserDto currentUser) {
        //모든 팀 조회
        List<Team> allTeams = teamRepository.findAll();

        //내가 이미 속한 팀 ID 목록 조회
        List<Long> myTeamIds = getMyTeamIds(currentUser.id());

        //내가 속하지 않은 팀만 필터링하여 반환
        return allTeams.stream()
                .filter(team -> !myTeamIds.contains(team.getId()))
                .map(teamMapper::toRecruitingTeamResponse)
                .collect(Collectors.toList());
    }

    @Override
    public TeamDetailResponse getTeamDetails(Long teamId, LocalDate targetDate, CurrentUserDto currentUser) {
        Team team = getTeamOrThrow(teamId);

        //해당 날짜의 팀 전체 루프 조회
        List<TeamLoop> todayLoops = teamLoopRepository.findByTeamAndLoopDate(team, targetDate);

        //팀 루프 통계 계산
        int totalLoopCount = todayLoops.size();
        double teamTotalProgress = todayLoops.isEmpty() ? 0.0 :
                todayLoops.stream()
                        .mapToDouble(TeamLoop::calculateTeamProgress)
                        .average().orElse(0.0);

        //내 루프 통계 계산
        Long myId = currentUser.id();
        List<TeamLoop> myTeamLoops = todayLoops.stream()
                .filter(loop -> loop.isParticipating(myId))
                .toList();
        int myTeamLoopCount = myTeamLoops.size();
        double myTotalProgress = myTeamLoops.isEmpty() ? 0.0 :
                myTeamLoops.stream()
                        .mapToDouble(loop -> loop.calculatePersonalProgress(myId))
                        .average().orElse(0.0);

        return TeamDetailResponse.builder()
                .teamId(team.getId())
                .currentDate(targetDate)
                .name(team.getName())
                .goal(team.getGoal())
                .category(team.getCategory())
                .leaderId(team.getLeader().getId())
                .totalLoopCount(totalLoopCount)
                .teamTotalProgress(teamTotalProgress)
                .myLoopCount(myTeamLoopCount)
                .myTotalProgress(myTotalProgress)
                .build();
    }

    // ========== 비즈니스 로직 메서드 ==========
    // 팀 저장
    private Team saveTeam(TeamCreateRequest request, Member leader) {
        Team team = Team.builder()
                .name(request.name())
                .goal(request.goal())
                .category(request.category())
                .leader(leader)
                .build();
        return teamRepository.save(team);
    }

    // 팀장 멤버 등록
    private void saveLeaderAsMember(Team team, Member leader) {
        TeamMember leaderMember = TeamMember.builder()
                .team(team)
                .member(leader)
                .build();
        teamMemberRepository.save(leaderMember);
    }

    // 멤버 초대 및 등록
    private void inviteMembers(Team team, List<String> invitedNicknames) {
        if (invitedNicknames == null || invitedNicknames.isEmpty()) {
            return;
        }

        // 닉네임 리스트로 멤버 한 번에 조회
        List<Member> invitedMembers = memberRepository.findAllByNicknameIn(invitedNicknames);

        // TeamMember 리스트 생성
        List<TeamMember> teamMembers = invitedMembers.stream()
                .map(member -> TeamMember.builder()
                        .team(team)
                        .member(member)
                        .build())
                .collect(Collectors.toList());

        teamMemberRepository.saveAll(teamMembers);
    }

    // ========== 조회 메서드 ==========
    // 회원 조회
    private Member getMemberOrThrow(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new ServiceException(ReturnCode.USER_NOT_FOUND));
    }

    // 내가 속한 팀 ID 목록 조회
    private List<Long> getMyTeamIds(Long memberId) {
        return teamMemberRepository.findAllByMemberId(memberId).stream()
                .map(tm -> tm.getTeam().getId())
                .toList();
    }

    //팀 조회
    private Team getTeamOrThrow(Long teamId) {
        return teamRepository.findById(teamId)
                .orElseThrow(() -> new ServiceException(ReturnCode.TEAM_NOT_FOUND));
    }
}
