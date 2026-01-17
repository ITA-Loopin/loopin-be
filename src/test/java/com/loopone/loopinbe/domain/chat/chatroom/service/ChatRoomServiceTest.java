package com.loopone.loopinbe.domain.chat.chatRoom.service;

import com.loopone.loopinbe.domain.account.auth.currentUser.CurrentUserDto;
import com.loopone.loopinbe.domain.account.member.converter.MemberConverterImpl;
import com.loopone.loopinbe.domain.account.member.converter.SimpleMemberConverterImpl;
import com.loopone.loopinbe.domain.account.member.entity.Member;
import com.loopone.loopinbe.domain.account.member.repository.MemberRepository;
import com.loopone.loopinbe.domain.chat.chatMessage.service.ChatMessageService;
import com.loopone.loopinbe.domain.chat.chatRoom.converter.ChatRoomConverterImpl;
import com.loopone.loopinbe.domain.chat.chatRoom.dto.req.ChatRoomRequest;
import com.loopone.loopinbe.domain.chat.chatRoom.dto.res.ChatRoomListResponse;
import com.loopone.loopinbe.domain.chat.chatRoom.dto.res.ChatRoomResponse;
import com.loopone.loopinbe.domain.chat.chatRoom.entity.ChatRoom;
import com.loopone.loopinbe.domain.chat.chatRoom.entity.ChatRoomMember;
import com.loopone.loopinbe.domain.chat.chatRoom.enums.ChatRoomType;
import com.loopone.loopinbe.domain.chat.chatRoom.repository.ChatRoomMemberRepository;
import com.loopone.loopinbe.domain.chat.chatRoom.repository.ChatRoomRepository;
import com.loopone.loopinbe.domain.chat.chatRoom.serviceImpl.ChatRoomServiceImpl;
import com.loopone.loopinbe.domain.team.team.entity.Team;
import com.loopone.loopinbe.domain.team.team.enums.TeamCategory;
import com.loopone.loopinbe.domain.team.team.repository.TeamMemberRepository;
import com.loopone.loopinbe.domain.team.team.repository.TeamRepository;
import com.loopone.loopinbe.global.exception.ReturnCode;
import com.loopone.loopinbe.global.exception.ServiceException;
import com.loopone.loopinbe.support.TestContainersConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@DataJpaTest
@ActiveProfiles("test")
@Import({
        TestContainersConfig.class,
        ChatRoomServiceImpl.class,
        ChatRoomConverterImpl.class,
        MemberConverterImpl.class,
        SimpleMemberConverterImpl.class
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional
class ChatRoomServiceTest {

    @Autowired
    ChatRoomServiceImpl chatRoomService;
    @Autowired
    ChatRoomRepository chatRoomRepository;
    @Autowired
    MemberRepository memberRepository;
    @Autowired
    TeamRepository teamRepository;
    @Autowired
    ChatRoomMemberRepository chatRoomMemberRepository;
    @Autowired
    TeamMemberRepository teamMemberRepository;

    @MockitoBean
    ChatMessageService chatMessageService;

    private Member member;
    private Member otherMember;
    private CurrentUserDto currentUser;
    private Team team;

    @BeforeEach
    void setUp() {
        member = Member.builder()
                .email("user@loop.in")
                .nickname("user")
                .build();
        memberRepository.save(member);

        otherMember = Member.builder()
                .email("other@loop.in")
                .nickname("other")
                .build();
        memberRepository.save(otherMember);

        currentUser = new CurrentUserDto(
                member.getId(), member.getEmail(), null, member.getNickname(), null,
                null, null, null, member.getState(), member.getRole(), member.getOAuthProvider(), member.getProviderId()
        );

        team = Team.builder()
                .name("Test Team")
                .goal("Test Goal")
                .category(TeamCategory.PROJECT)
                .leader(member)
                .build();
        teamRepository.save(team);
    }

    @AfterEach
    void tearDown() {
        chatRoomMemberRepository.deleteAll();
        chatRoomRepository.deleteAll();
        teamRepository.deleteAll();
        memberRepository.deleteAll();
    }

    @Test
    @DisplayName("AI 채팅방 생성")
    void createAiChatRoom() {
        // when
        ChatRoomResponse response = chatRoomService.createAiChatRoom(currentUser);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getTitle()).isEqualTo("새 채팅");

        ChatRoom saved = chatRoomRepository.findById(response.getId()).orElseThrow();
        assertThat(saved.getMember().getId()).isEqualTo(member.getId());

        verify(chatMessageService).sendChatMessage(any(), any(), any());
    }

    @Test
    @DisplayName("채팅방 제목 수정 - 새 채팅일 경우만")
    void updateChatRoomTitle() {
        // given
        ChatRoom chatRoom = ChatRoom.builder()
                .title("새 채팅")
                .member(member)
                .build();
        chatRoomRepository.save(chatRoom);

        // when
        chatRoomService.updateChatRoomTitle(chatRoom.getId(), "변경된 제목");

        // then
        ChatRoom updated = chatRoomRepository.findById(chatRoom.getId()).orElseThrow();
        assertThat(updated.getTitle()).isEqualTo("변경된 제목");
    }

    @Test
    @DisplayName("채팅방 목록 조회 - AI 채팅방 필터링")
    void getChatRooms_Success() {
        // given
        // 1. AI 채팅방 생성
        chatRoomService.createAiChatRoom(currentUser);
        // 2. 일반 팀 채팅방 생성 (수동 생성)
        ChatRoom teamRoom = ChatRoom.builder()
                .title("Team Room")
                .member(member)
                .isBotRoom(false)
                .teamId(team.getId())
                .build();
        chatRoomRepository.save(teamRoom);

        ChatRoomMember crm = ChatRoomMember.builder().chatRoom(teamRoom).member(member).build();
        chatRoomMemberRepository.save(crm);

        // when
        ChatRoomListResponse aiRooms = chatRoomService.getChatRooms(member.getId(), ChatRoomType.AI);
        ChatRoomListResponse teamRooms = chatRoomService.getChatRooms(member.getId(), ChatRoomType.TEAM);

        // then
        assertThat(aiRooms.chatRooms()).hasSize(1);
        assertThat(aiRooms.chatRooms().get(0).getTitle()).isEqualTo("새 채팅");

        assertThat(teamRooms.chatRooms()).hasSize(1);
        assertThat(teamRooms.chatRooms().get(0).getTitle()).isEqualTo("Team Room");
    }

    @Test
    @DisplayName("팀 채팅방 삭제 - 리더 성공")
    void deleteTeamChatRoom_Success() {
        // given
        chatRoomService.createTeamChatRoom(member.getId(), team); // 리더가 생성

        // when
        chatRoomService.deleteTeamChatRoom(member.getId(), team.getId());

        // then
        assertThat(chatRoomRepository.findByTeamId(team.getId())).isEmpty();
    }

    @Test
    @DisplayName("팀 채팅방 삭제 - 권한 없음 실패")
    void deleteTeamChatRoom_Fail_NotLeader() {
        // given
        chatRoomService.createTeamChatRoom(member.getId(), team);

        // when & then
        assertThatThrownBy(() -> chatRoomService.deleteTeamChatRoom(otherMember.getId(), team.getId()))
                .isInstanceOf(ServiceException.class)
                .hasFieldOrPropertyWithValue("returnCode", ReturnCode.NOT_AUTHORIZED);
    }

    @Test
    @DisplayName("채팅방 나가기 - 방장 위임")
    void leaveAllChatRooms_OwnerDelegation() {
        // given
        ChatRoom chatRoom = ChatRoom.builder()
                .title("General Room")
                .member(member) // 현재 방장
                .build();
        chatRoomRepository.save(chatRoom);

        ChatRoomMember member1 = ChatRoomMember.builder().chatRoom(chatRoom).member(member).build();
        ChatRoomMember member2 = ChatRoomMember.builder().chatRoom(chatRoom).member(otherMember).build();
        chatRoomMemberRepository.saveAll(List.of(member1, member2));

        // when
        chatRoomService.leaveAllChatRooms(member.getId());

        // then
        ChatRoom updatedRoom = chatRoomRepository.findById(chatRoom.getId()).orElseThrow();
        assertThat(updatedRoom.getMember().getId()).isEqualTo(otherMember.getId()); // 방장이 otherMember로 변경됨
        assertThat(chatRoomMemberRepository.existsByChatRoomIdAndMemberId(chatRoom.getId(), member.getId())).isFalse();
    }

    @Test
    @DisplayName("1:1 채팅방 중복 생성 실패")
    void addChatRoom_Fail_DuplicateOneOnOne() {
        // given
        // 1. 이미 1:1 채팅방 존재 상황 생성
        ChatRoom chatRoom = ChatRoom.builder().title("1:1").member(member).build();
        chatRoomRepository.save(chatRoom);
        ChatRoomMember m1 = ChatRoomMember.builder().chatRoom(chatRoom).member(member).build();
        ChatRoomMember m2 = ChatRoomMember.builder().chatRoom(chatRoom).member(otherMember).build();
        chatRoomMemberRepository.saveAll(List.of(m1, m2));

        // 요청 객체 생성 (otherMember와 1:1 채팅 요청)
        ChatRoomMember reqMemberDto = ChatRoomMember.builder().member(otherMember).build();
        ChatRoomRequest request = new ChatRoomRequest("New 1:1", List.of(reqMemberDto));

        // when & then
        assertThatThrownBy(() -> chatRoomService.addChatRoom(request, currentUser))
                .isInstanceOf(ServiceException.class)
                .hasFieldOrPropertyWithValue("returnCode", ReturnCode.CHATROOM_ALREADY_EXISTS);
    }

    @Test
    @DisplayName("팀 채팅방 참여 성공")
    void participateChatRoom_Success() {
        // given
        ChatRoom teamRoom = ChatRoom.builder()
                .title(team.getName())
                .member(member)
                .isBotRoom(false)
                .teamId(team.getId())
                .build();
        chatRoomRepository.save(teamRoom);

        // when
        chatRoomService.participateChatRoom(team.getId(), otherMember.getId());

        // then
        boolean exists = chatRoomMemberRepository.existsByChatRoomIdAndMemberId(teamRoom.getId(), otherMember.getId());
        assertThat(exists).isTrue();
    }
}
