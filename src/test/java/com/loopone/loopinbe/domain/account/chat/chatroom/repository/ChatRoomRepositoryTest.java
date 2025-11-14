package com.loopone.loopinbe.domain.account.chat.chatroom.repository;

import com.loopone.loopinbe.domain.account.member.entity.Member;
import com.loopone.loopinbe.domain.account.member.repository.MemberRepository;
import com.loopone.loopinbe.domain.chat.chatMessage.entity.ChatMessage;
import com.loopone.loopinbe.domain.chat.chatMessage.repository.ChatMessageRepository;
import com.loopone.loopinbe.domain.chat.chatRoom.entity.ChatRoom;
import com.loopone.loopinbe.domain.chat.chatRoom.entity.ChatRoomMember;
import com.loopone.loopinbe.domain.chat.chatRoom.repository.ChatRoomMemberRepository;
import com.loopone.loopinbe.domain.chat.chatRoom.repository.ChatRoomRepository;
import com.loopone.loopinbe.support.TestContainersConfig;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@DataJpaTest
@Import(TestContainersConfig.class) // 아래에 제공
@ActiveProfiles("test")
public class ChatRoomRepositoryTest {
    @PersistenceContext
    EntityManager em;
    @Autowired
    ChatRoomRepository chatRoomRepository;
    @Autowired
    MemberRepository memberRepository;
    @Autowired
    ChatRoomMemberRepository chatRoomMemberRepository;
    @Autowired
    ChatMessageRepository chatMessageRepository;

    Member owner, other;
    ChatRoom room;

    private void flushAndClear() {
        em.flush();
        em.clear();
    }

    @Test
    @DisplayName("findChatRoomsByMemberOrderByLatestMessage 동작 확인")
    void findChatRoomsByMemberOrderByLatestMessage() {

        // room1, room2, room3 생성 (chatRoomMembers = new ArrayList<>() 는 builder가 이미 초기값 줌)
        ChatRoom room1 = chatRoomRepository.save(ChatRoom.builder().title("room1").member(owner).chatRoomMembers(new ArrayList<>()).build());
        ChatRoom room2 = chatRoomRepository.save(ChatRoom.builder().title("room2").member(owner).chatRoomMembers(new ArrayList<>()).build());

        // 반드시 양방향 모두 설정해야 함
        ChatRoomMember crm1 = chatRoomMemberRepository.save(ChatRoomMember.builder().chatRoom(room1).member(owner).build());
        room1.getChatRoomMembers().add(crm1);

        ChatRoomMember crm2 = chatRoomMemberRepository.save(ChatRoomMember.builder().chatRoom(room2).member(owner).build());
        room2.getChatRoomMembers().add(crm2);

        flushAndClear();

        // 메시지 생성 시간 : room1 -> room2
        chatMessageRepository.save(ChatMessage.builder()
                .chatRoom(room1).member(owner)
                .createdAt(LocalDateTime.now().minusMinutes(10))
                .authorType(ChatMessage.AuthorType.USER)
                .messageKey("m1").build());

        chatMessageRepository.save(ChatMessage.builder()
                .chatRoom(room2).member(owner)
                .createdAt(LocalDateTime.now().minusMinutes(1))
                .authorType(ChatMessage.AuthorType.USER)
                .messageKey("m2").build());

        Page<ChatRoom> result = chatRoomRepository
                .findChatRoomsByMemberOrderByLatestMessage(PageRequest.of(0, 10), owner);

        List<ChatRoom> list = result.getContent();

        // 기존 room 포함 3개
        assertThat(list.size()).isEqualTo(3);
        // 아무 메세지도 없는 room이 1번, 더 최근 room2, 마지막 room1 순서
        assertThat(list.get(0).getId()).isEqualTo(room.getId());
        assertThat(list.get(1).getId()).isEqualTo(room2.getId());
        assertThat(list.get(2).getId()).isEqualTo(room1.getId());
    }

    @Test
    @DisplayName("existsOneOnOneChatRoom 동작 확인")
    void existsOneOnOneChatRoom_returnTrue() {
        boolean result = chatRoomRepository.existsOneOnOneChatRoom(owner.getId(), other.getId());
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("findChatRoomMembersWithMember 동작 확인")
    void findChatRoomMembersWithMember() {
        List<ChatRoomMember> result = chatRoomRepository.findChatRoomMembersWithMember(room.getId());
        // then
        assertThat(result.size()).isEqualTo(2);

        // Fetch Join이 되었는지 검증 (LazyInitializationException 발생하면 실패)
        assertDoesNotThrow(() -> result.get(0).getMember().getNickname());
    }

    @Test
    @DisplayName("findByIdWithMembers 동작 확인")
    void findByIdWithMembers() {
        ChatRoom found = chatRoomRepository.findByIdWithMembers(room.getId())
                .orElseThrow();

        assertThat(found.getMember().getId()).isEqualTo(owner.getId());
        assertThat(found.getMember().getNickname()).isEqualTo("owner");
    }

    @Test
    @DisplayName("findByMemberId 동작 확인")
    void findByMemberId() {
        List<ChatRoom> result = chatRoomRepository.findByMemberId(owner.getId());

        // then
        assertThat(result.size()).isEqualTo(1);

        // Fetch Join 검증
        result.forEach(room -> {
            assertDoesNotThrow(() -> room.getChatRoomMembers().get(0).getMember().getNickname());
        });
    }

    @Test
    @DisplayName("findParticipantMemberIds 동작 확인")
    void findParticipantMemberIds() {
        List<Long> result = chatRoomRepository.findParticipantMemberIds(room.getId());
        assertThat(result.size()).isEqualTo(2);
        assertThat(result).containsExactlyInAnyOrder(owner.getId(), other.getId());
    }

    @Test
    @DisplayName("existsMember 동작 확인")
    void existsMember() {
        // 참여하는 경우
        boolean exists = chatRoomRepository.existsMember(room.getId(), owner.getId());
        // 참여하지 않는 경우
        Member outsider = memberRepository.save(
                Member.builder().email("out@test.com").nickname("out").build()
        );

        boolean notExists = chatRoomRepository.existsMember(room.getId(), outsider.getId());

        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
    }

    @BeforeEach
    void setup() {
        owner = memberRepository.save(Member.builder().email("owner@test.com").nickname("owner").build());
        other = memberRepository.save(Member.builder().email("other@test.com").nickname("other").build());

        room = ChatRoom.builder()
                .title("테스트방")
                .chatRoomMembers(new ArrayList<>())
                .member(owner)
                .build();

        ChatRoomMember crmOwner = ChatRoomMember.builder().chatRoom(room).member(owner).build();
        ChatRoomMember crmOther = ChatRoomMember.builder().chatRoom(room).member(other).build();

        room.getChatRoomMembers().add(crmOwner);
        room.getChatRoomMembers().add(crmOther);

        chatRoomRepository.save(room);
    }
}
