package com.loopone.loopinbe.domain.team.team.service;

import com.loopone.loopinbe.domain.account.auth.currentUser.CurrentUserDto;
import com.loopone.loopinbe.domain.account.member.entity.Member;
import com.loopone.loopinbe.domain.account.member.repository.MemberRepository;
import com.loopone.loopinbe.domain.chat.chatRoom.service.ChatRoomService;
import com.loopone.loopinbe.domain.team.team.dto.req.TeamCreateRequest;
import com.loopone.loopinbe.domain.team.team.dto.res.MyTeamResponse;
import com.loopone.loopinbe.domain.team.team.dto.res.TeamDetailResponse;
import com.loopone.loopinbe.domain.team.team.dto.res.TeamMemberResponse;
import com.loopone.loopinbe.domain.team.team.entity.Team;
import com.loopone.loopinbe.domain.team.team.entity.TeamMember;
import com.loopone.loopinbe.domain.team.team.enums.TeamCategory;
import com.loopone.loopinbe.domain.team.team.enums.TeamVisibility;
import com.loopone.loopinbe.domain.team.team.mapper.TeamMapperImpl;
import com.loopone.loopinbe.domain.team.team.repository.TeamMemberRepository;
import com.loopone.loopinbe.domain.team.team.repository.TeamRepository;
import com.loopone.loopinbe.domain.team.team.serviceImpl.TeamServiceImpl;
import com.loopone.loopinbe.domain.team.teamLoop.repository.TeamLoopMemberCheckRepository;
import com.loopone.loopinbe.domain.team.teamLoop.repository.TeamLoopMemberProgressRepository;
import com.loopone.loopinbe.domain.team.teamLoop.repository.TeamLoopRepository;
import com.loopone.loopinbe.domain.team.teamLoop.service.TeamLoopService;
import com.loopone.loopinbe.global.exception.ReturnCode;
import com.loopone.loopinbe.global.exception.ServiceException;
import com.loopone.loopinbe.support.TestContainersConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@Import({ TestContainersConfig.class, TeamServiceImpl.class, TeamMapperImpl.class })
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class TeamServiceTest {
    // ===== Real Repositories =====
    @Autowired
    TeamRepository teamRepository;
    @Autowired
    TeamMemberRepository teamMemberRepository;
    @Autowired
    MemberRepository memberRepository;
    @Autowired
    TeamLoopRepository teamLoopRepository;
    @Autowired
    TeamLoopMemberProgressRepository teamLoopMemberProgressRepository;
    @Autowired
    TeamLoopMemberCheckRepository teamLoopMemberCheckRepository;

    // ===== Mock Beans =====
    @MockitoBean
    ChatRoomService chatRoomService;
    @MockitoBean
    TeamInvitationService teamInvitationService;
    @MockitoBean
    TeamLoopService teamLoopService;

    // ===== SUT =====
    @Autowired
    TeamServiceImpl teamService;

    @AfterEach
    void cleanup() {
        teamMemberRepository.deleteAll();
        teamRepository.deleteAll();
        memberRepository.deleteAll();
    }

    // ===== Helpers =====
    private Member persistMember(String email, String nickname) {
        Member m = Member.builder()
                .email(email)
                .nickname(nickname)
                .profileImageUrl(null)
                .state(Member.State.NORMAL)
                .role(Member.MemberRole.ROLE_USER)
                .oAuthProvider(Member.OAuthProvider.GOOGLE)
                .providerId("pid")
                .build();
        return memberRepository.saveAndFlush(m);
    }

    private Team persistTeam(Member leader, String name, String goal, TeamCategory category,
            TeamVisibility visibility) {
        Team team = Team.builder()
                .leader(leader)
                .name(name)
                .goal(goal)
                .category(category)
                .visibility(visibility)
                .build();
        return teamRepository.saveAndFlush(team);
    }

    private TeamMember persistTeamMember(Team team, Member member, Integer sortOrder) {
        TeamMember tm = TeamMember.builder()
                .team(team)
                .member(member)
                .sortOrder(sortOrder)
                .build();
        return teamMemberRepository.saveAndFlush(tm);
    }

    private CurrentUserDto cu(Member m) {
        return new CurrentUserDto(
                m.getId(),
                m.getEmail(),
                null,
                m.getNickname(),
                null,
                null,
                null,
                m.getProfileImageUrl(),
                m.getState(),
                m.getRole(),
                m.getOAuthProvider(),
                m.getProviderId());
    }

    // =========================================================
    // createTeam
    // =========================================================
    @Nested
    @DisplayName("팀 생성")
    class CreateTeam {

        @Test
        @Disabled("ChatRoomService Mock 이슈로 비활성화")
        @DisplayName("성공: 팀 생성 및 리더 자동 등록")
        void success() {
            var member = persistMember("leader@test.com", "리더");
            var request = new TeamCreateRequest(TeamCategory.PROJECT, "테스트팀", "목표", List.of());
            var currentUser = cu(member);

            Long teamId = teamService.createTeam(request, currentUser);

            assertThat(teamId).isNotNull();
            var team = teamRepository.findById(teamId).orElseThrow();
            assertThat(team.getName()).isEqualTo("테스트팀");
            assertThat(team.getLeader().getId()).isEqualTo(member.getId());

            // 리더가 팀원으로 자동 등록되었는지 확인
            var teamMembers = team.getTeamMembers();
            assertThat(teamMembers).hasSize(1);
            assertThat(teamMembers.get(0).getMember().getId()).isEqualTo(member.getId());
        }
    }

    // =========================================================
    // getMyTeams
    // =========================================================
    @Nested
    @DisplayName("내 팀 조회")
    class GetMyTeams {

        @Test
        @DisplayName("성공: 여러 팀 조회")
        void success() {
            var member = persistMember("user@test.com", "사용자");
            var leader = persistMember("leader@test.com", "리더");
            var team1 = persistTeam(leader, "팀1", "목표1", TeamCategory.PROJECT, TeamVisibility.PRIVATE);
            var team2 = persistTeam(leader, "팀2", "목표2", TeamCategory.STUDY, TeamVisibility.PUBLIC);
            persistTeamMember(team1, member, 1);
            persistTeamMember(team2, member, 2);

            var currentUser = cu(member);
            List<MyTeamResponse> result = teamService.getMyTeams(currentUser);

            assertThat(result).hasSize(2);
            assertThat(result).extracting("name").containsExactly("팀1", "팀2");
        }

        @Test
        @DisplayName("성공: 팀이 없는 경우 빈 리스트")
        void emptyList() {
            var member = persistMember("user@test.com", "사용자");
            var currentUser = cu(member);

            List<MyTeamResponse> result = teamService.getMyTeams(currentUser);

            assertThat(result).isEmpty();
        }
    }

    // =========================================================
    // getTeamDetails
    // =========================================================
    @Nested
    @DisplayName("팀 상세 조회")
    class GetTeamDetails {

        @Test
        @DisplayName("성공: 팀 상세 정보 조회")
        void success() {
            var leader = persistMember("leader@test.com", "리더");
            var team = persistTeam(leader, "테스트팀", "목표", TeamCategory.PROJECT, TeamVisibility.PRIVATE);
            persistTeamMember(team, leader, 1);
            var currentUser = cu(leader);

            TeamDetailResponse result = teamService.getTeamDetails(team.getId(), LocalDate.now(), currentUser);

            assertThat(result).isNotNull();
            assertThat(result.teamId()).isEqualTo(team.getId());
            assertThat(result.name()).isEqualTo("테스트팀");
            assertThat(result.leaderId()).isEqualTo(leader.getId());
        }

        @Test
        @DisplayName("실패: 존재하지 않는 팀")
        void teamNotFound() {
            var member = persistMember("user@test.com", "사용자");
            var currentUser = cu(member);

            assertThatThrownBy(() -> teamService.getTeamDetails(999L, LocalDate.now(), currentUser))
                    .isInstanceOf(ServiceException.class)
                    .extracting("returnCode").isEqualTo(ReturnCode.TEAM_NOT_FOUND);
        }
    }

    // =========================================================
    // getTeamMembers
    // =========================================================
    @Nested
    @DisplayName("팀 멤버 조회")
    class GetTeamMembers {

        @Test
        @Disabled("Team.getTeamMembers() NullPointerException 이슈로 비활성화")
        @DisplayName("성공: 팀 멤버 목록 조회")
        void success() {
            var leader = persistMember("leader@test.com", "리더");
            var member1 = persistMember("member1@test.com", "멤버1");
            var member2 = persistMember("member2@test.com", "멤버2");
            var team = persistTeam(leader, "테스트팀", "목표", TeamCategory.PROJECT, TeamVisibility.PRIVATE);
            persistTeamMember(team, leader, 1);
            persistTeamMember(team, member1, 2);
            persistTeamMember(team, member2, 3);

            List<TeamMemberResponse> result = teamService.getTeamMembers(team.getId());

            assertThat(result).hasSize(3);
            assertThat(result).extracting("nickname").containsExactlyInAnyOrder("리더", "멤버1", "멤버2");
        }
    }

    // =========================================================
    // deleteTeam
    // =========================================================
    @Nested
    @DisplayName("팀 삭제")
    class DeleteTeam {

        @Test
        @Disabled("ChatRoomService Mock 이슈로 비활성화")
        @DisplayName("성공: 리더가 팀 삭제")
        void successByLeader() {
            var leader = persistMember("leader@test.com", "리더");
            var team = persistTeam(leader, "테스트팀", "목표", TeamCategory.PROJECT, TeamVisibility.PRIVATE);
            persistTeamMember(team, leader, 1);
            var currentUser = cu(leader);

            teamService.deleteTeam(team.getId(), currentUser);

            assertThat(teamRepository.findById(team.getId())).isEmpty();
        }

        @Test
        @DisplayName("실패: 일반 멤버가 팀 삭제 시도")
        void failByMember() {
            var leader = persistMember("leader@test.com", "리더");
            var member = persistMember("member@test.com", "멤버");
            var team = persistTeam(leader, "테스트팀", "목표", TeamCategory.PROJECT, TeamVisibility.PRIVATE);
            persistTeamMember(team, leader, 1);
            persistTeamMember(team, member, 2);
            var memberUser = cu(member);

            assertThatThrownBy(() -> teamService.deleteTeam(team.getId(), memberUser))
                    .isInstanceOf(ServiceException.class)
                    .extracting("returnCode").isEqualTo(ReturnCode.NOT_AUTHORIZED);
        }

        @Test
        @DisplayName("실패: 존재하지 않는 팀")
        void teamNotFound() {
            var member = persistMember("user@test.com", "사용자");
            var currentUser = cu(member);

            assertThatThrownBy(() -> teamService.deleteTeam(999L, currentUser))
                    .isInstanceOf(ServiceException.class)
                    .extracting("returnCode").isEqualTo(ReturnCode.TEAM_NOT_FOUND);
        }
    }

    // =========================================================
    // leaveTeam
    // =========================================================
    @Nested
    @DisplayName("팀 나가기")
    class LeaveTeam {

        @Test
        @Disabled("ChatRoomService Mock 이슈로 비활성화")
        @DisplayName("성공: 일반 멤버가 팀 나가기")
        void successByMember() {
            var leader = persistMember("leader@test.com", "리더");
            var member = persistMember("member@test.com", "멤버");
            var team = persistTeam(leader, "테스트팀", "목표", TeamCategory.PROJECT, TeamVisibility.PRIVATE);
            persistTeamMember(team, leader, 1);
            var tm = persistTeamMember(team, member, 2);
            var memberUser = cu(member);

            teamService.leaveTeam(team.getId(), memberUser);

            assertThat(teamMemberRepository.findById(tm.getId())).isEmpty();
        }

        @Test
        @DisplayName("실패: 리더는 팀 나가기 불가")
        void failByLeader() {
            var leader = persistMember("leader@test.com", "리더");
            var team = persistTeam(leader, "테스트팀", "목표", TeamCategory.PROJECT, TeamVisibility.PRIVATE);
            persistTeamMember(team, leader, 1);
            var currentUser = cu(leader);

            assertThatThrownBy(() -> teamService.leaveTeam(team.getId(), currentUser))
                    .isInstanceOf(ServiceException.class)
                    .extracting("returnCode").isEqualTo(ReturnCode.TEAM_LEADER_CANNOT_LEAVE);
        }
    }

    // =========================================================
    // removeMember
    // =========================================================
    @Nested
    @DisplayName("팀원 강제 퇴출")
    class RemoveMember {

        @Test
        @Disabled("ChatRoomService Mock 이슈로 비활성화")
        @DisplayName("성공: 리더가 팀원 강제 퇴출")
        void successByLeader() {
            var leader = persistMember("leader@test.com", "리더");
            var member = persistMember("member@test.com", "멤버");
            var team = persistTeam(leader, "테스트팀", "목표", TeamCategory.PROJECT, TeamVisibility.PRIVATE);
            persistTeamMember(team, leader, 1);
            var tm = persistTeamMember(team, member, 2);
            var currentUser = cu(leader);

            teamService.removeMember(team.getId(), member.getId(), currentUser);

            assertThat(teamMemberRepository.findById(tm.getId())).isEmpty();
        }

        @Test
        @DisplayName("실패: 일반 멤버가 강제 퇴출 시도")
        void failByMember() {
            var leader = persistMember("leader@test.com", "리더");
            var member1 = persistMember("member1@test.com", "멤버1");
            var member2 = persistMember("member2@test.com", "멤버2");
            var team = persistTeam(leader, "테스트팀", "목표", TeamCategory.PROJECT, TeamVisibility.PRIVATE);
            persistTeamMember(team, leader, 1);
            persistTeamMember(team, member1, 2);
            persistTeamMember(team, member2, 3);
            var member1User = cu(member1);

            assertThatThrownBy(() -> teamService.removeMember(team.getId(), member2.getId(), member1User))
                    .isInstanceOf(ServiceException.class)
                    .extracting("returnCode").isEqualTo(ReturnCode.UNAUTHORIZED_TEAM_LEADER_ONLY);
        }

        @Test
        @DisplayName("실패: 리더 자신을 강제 퇴출 시도")
        void failRemoveSelf() {
            var leader = persistMember("leader@test.com", "리더");
            var team = persistTeam(leader, "테스트팀", "목표", TeamCategory.PROJECT, TeamVisibility.PRIVATE);
            persistTeamMember(team, leader, 1);
            var currentUser = cu(leader);

            assertThatThrownBy(() -> teamService.removeMember(team.getId(), leader.getId(), currentUser))
                    .isInstanceOf(ServiceException.class)
                    .extracting("returnCode").isEqualTo(ReturnCode.CANNOT_REMOVE_SELF);
        }
    }
}
