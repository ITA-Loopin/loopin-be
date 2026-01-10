package com.loopone.loopinbe.domain.team.team.serviceImpl;

import com.loopone.loopinbe.domain.account.auth.currentUser.CurrentUserDto;
import com.loopone.loopinbe.domain.account.member.entity.Member;
import com.loopone.loopinbe.domain.account.member.repository.MemberRepository;
import com.loopone.loopinbe.domain.chat.chatRoom.service.ChatRoomService;
import com.loopone.loopinbe.domain.team.team.dto.req.TeamCreateRequest;
import com.loopone.loopinbe.domain.team.team.dto.req.TeamOrderUpdateRequest;
import com.loopone.loopinbe.domain.team.team.dto.res.MyTeamResponse;
import com.loopone.loopinbe.domain.team.team.dto.res.RecruitingTeamResponse;
import com.loopone.loopinbe.domain.team.team.dto.res.TeamDetailResponse;
import com.loopone.loopinbe.domain.team.team.dto.res.TeamMemberResponse;
import com.loopone.loopinbe.domain.team.team.entity.Team;
import com.loopone.loopinbe.domain.team.team.entity.TeamMember;
import com.loopone.loopinbe.domain.team.team.mapper.TeamMapper;
import com.loopone.loopinbe.domain.team.team.repository.TeamMemberRepository;
import com.loopone.loopinbe.domain.team.team.repository.TeamRepository;
import com.loopone.loopinbe.domain.team.team.service.TeamService;
import com.loopone.loopinbe.domain.team.teamLoop.entity.TeamLoop;
import com.loopone.loopinbe.domain.team.teamLoop.entity.TeamLoopMemberCheck;
import com.loopone.loopinbe.domain.team.teamLoop.entity.TeamLoopMemberProgress;
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
import java.util.Map;
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
    private final TeamLoopService teamLoopService;
    private final ChatRoomService chatRoomService;

    @Override
    @Transactional
    public Long createTeam(TeamCreateRequest request, CurrentUserDto currentUser) {
        Member leader = getMemberOrThrow(currentUser.id());

        //팀 엔티티 생성 및 저장
        Team team = saveTeam(request, leader);

        //팀장을 팀원으로 등록
        saveLeaderAsMember(team, leader);

        //초대된 멤버들 등록
        List<Member> members = inviteMembers(team, request.invitedNicknames());

        chatRoomService.createTeamChatRoom(currentUser.id(), team, members);

        return team.getId();
    }

    @Override
    public List<MyTeamResponse> getMyTeams(CurrentUserDto currentUser) {
        Member member = getMemberOrThrow(currentUser.id());

        //팀을 정렬한 TeamMember 조회 (sortOrder 우선, null이면 createdAt DESC)
        List<TeamMember> myTeamMembers = teamMemberRepository.findAllByMemberOrderBySortOrder(member);

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

    @Override
    public List<TeamMemberResponse> getTeamMembers(Long teamId) {
        Team team = getTeamOrThrow(teamId);

        return team.getTeamMembers().stream()
                .map(tm -> TeamMemberResponse.builder()
                        .memberId(tm.getMember().getId())
                        .nickname(tm.getMember().getNickname())
                        .profileImage(tm.getMember().getProfileImageUrl())
                        .build())
                .collect(Collectors.toList());
    }

    //팀 순서 변경
    @Override
    @Transactional
    public void updateTeamOrder(TeamOrderUpdateRequest request, CurrentUserDto currentUser) {
        Member member = getMemberOrThrow(currentUser.id());
        List<Long> orderedTeamIds = request.teamIds();

        // 내가 속한 팀 멤버 정보 조회
        List<TeamMember> myTeamMembers = teamMemberRepository.findAllByMember(member);

        // 요청된 팀 ID로 매핑
        Map<Long, TeamMember> teamMemberMap = myTeamMembers.stream()
                .collect(Collectors.toMap(
                        tm -> tm.getTeam().getId(),
                        tm -> tm
                ));

        // 순서 업데이트
        for (int i = 0; i < orderedTeamIds.size(); i++) {
            Long teamId = orderedTeamIds.get(i);
            TeamMember teamMember = teamMemberMap.get(teamId);

            if (teamMember == null) {
                throw new ServiceException(ReturnCode.USER_NOT_IN_TEAM);
            }

            teamMember.setSortOrder(i);
        }

        // 일괄 저장
        teamMemberRepository.saveAll(myTeamMembers);
    }

    // 사용자가 참여중인 모든 팀 나가기/관련 엔티티 삭제
    @Override
    @Transactional
    public void deleteMyTeams(Member member) {
        List<TeamMember> myMemberships = teamMemberRepository.findAllByMember(member);
        if (myMemberships.isEmpty()) return;
        List<Long> myTeamIds = myMemberships.stream()
                .map(tm -> tm.getTeam().getId())
                .distinct()
                .toList();
        // 내가 리더인 팀들 중, (다른 팀원이 없어서) 팀 자체를 삭제해야 하는 팀들
        List<Team> myLeaderTeams = teamRepository.findAllByLeaderId(member.getId());
        List<Long> teamsToDelete = new ArrayList<>();
        for (Team team : myLeaderTeams) {
            if (!myTeamIds.contains(team.getId())) continue;
            teamMemberRepository.findFirstMemberByTeamIdAndMemberIdNot(team.getId(), member.getId())
                    .ifPresentOrElse(
                            nextLeader -> {
                                team.setLeader(nextLeader); // 1) 팀 리더 위임
                                // 2) TeamLoop에 연결된 LoopRule의 member도 새 리더로 위임
                                teamLoopService.transferTeamLoopRuleOwner(team.getId(), member.getId(), nextLeader);
                            },
                            () -> teamsToDelete.add(team.getId())
                    );
        }
        // 팀은 남고, 나는 탈퇴만 하는 팀들
        List<Long> remainingTeamIds = myTeamIds.stream()
                .filter(id -> !teamsToDelete.contains(id))
                .toList();
        // 루프 관련 삭제는 TeamLoopService가 담당
        teamLoopService.deleteMyTeamLoops(member.getId(), teamsToDelete, remainingTeamIds);
        // (A) 팀 전체 삭제: TeamMember + Team만 삭제 (루프쪽은 이미 위에서 삭제됨)
        if (!teamsToDelete.isEmpty()) {
            teamMemberRepository.deleteByTeamIds(teamsToDelete);
            teamRepository.deleteAllByIdInBatch(teamsToDelete);
        }
        // (B) 팀은 남음: TeamMember만 삭제(탈퇴)
        if (!remainingTeamIds.isEmpty()) {
            teamMemberRepository.deleteByMemberAndTeamIds(member.getId(), remainingTeamIds);
        }
    }

    @Override
    @Transactional
    public void deleteTeam(Long teamId, CurrentUserDto currentUser) {
        Team team = getTeamOrThrow(teamId);

        if (!team.getLeader().getId().equals(currentUser.id())) {
            throw new ServiceException(ReturnCode.NOT_AUTHORIZED);
        }

        chatRoomService.deleteTeamChatRoom(currentUser.id(), teamId);

        List<TeamLoop> loops = teamLoopRepository.findAllByTeamId(teamId);
        teamLoopRepository.deleteAll(loops);

        // 팀 삭제
        teamRepository.delete(team);
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
    private List<Member> inviteMembers(Team team, List<String> invitedNicknames) {
        if (invitedNicknames == null || invitedNicknames.isEmpty()) {
            return null;
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

        return invitedMembers;
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

    // ========== 검증 메서드 ==========
    // 팀원 검증
    private void validateTeamMember(Long teamId, Long memberId) {
        if (!teamMemberRepository.existsByTeamIdAndMemberId(teamId, memberId)) {
            throw new ServiceException(ReturnCode.USER_NOT_IN_TEAM);
        }
    }
}
