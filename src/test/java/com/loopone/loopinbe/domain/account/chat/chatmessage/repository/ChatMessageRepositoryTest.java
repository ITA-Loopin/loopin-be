package com.loopone.loopinbe.domain.account.chat.chatmessage.repository;

import com.loopone.loopinbe.domain.account.member.entity.Member;
import com.loopone.loopinbe.domain.account.member.repository.MemberRepository;
import com.loopone.loopinbe.domain.chat.chatMessage.entity.ChatMessage;
import com.loopone.loopinbe.domain.chat.chatMessage.repository.ChatMessageRepository;
import com.loopone.loopinbe.domain.chat.chatRoom.entity.ChatRoom;
import com.loopone.loopinbe.domain.chat.chatRoom.entity.ChatRoomMember;
import com.loopone.loopinbe.domain.chat.chatRoom.repository.ChatRoomRepository;
import com.loopone.loopinbe.support.TestContainersConfig;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@DataJpaTest
@Import(TestContainersConfig.class)
@ActiveProfiles("test")
class ChatMessageRepositoryTest {

    @Autowired
    EntityManager em;

    @Autowired
    ChatMessageRepository chatMessageRepository;
    @Autowired
    ChatRoomRepository chatRoomRepository;
    @Autowired
    MemberRepository memberRepository;

    private Member user;
    private ChatRoom room;

    @Test
    @DisplayName("findByChatRoomId")
    void findByChatRoomId() {

        // 메시지 3개 저장
        ChatMessage m1 = chatMessageRepository.save(
                ChatMessage.builder()
                        .chatRoom(room)
                        .member(user)
                        .messageKey("k1")
                        .createdAt(LocalDateTime.now().minusMinutes(3))
                        .build()
        );

        ChatMessage m2 = chatMessageRepository.save(
                ChatMessage.builder()
                        .chatRoom(room)
                        .member(user)
                        .messageKey("k2")
                        .createdAt(LocalDateTime.now().minusMinutes(2))
                        .build()
        );

        ChatMessage m3 = chatMessageRepository.save(
                ChatMessage.builder()
                        .chatRoom(room)
                        .member(user)
                        .messageKey("k3")
                        .createdAt(LocalDateTime.now().minusMinutes(1))
                        .build()
        );

        flushAndClear();

        Page<ChatMessage> result = chatMessageRepository.findByChatRoomId(
                room.getId(),
                PageRequest.of(0, 2, Sort.by("createdAt").descending())
        );

        // 검증
        assertThat(result.getContent().size()).isEqualTo(2);
        assertThat(result.getContent().get(0).getMessageKey()).isEqualTo("k3");
        assertThat(result.getContent().get(1).getMessageKey()).isEqualTo("k2");

        // fetch join이므로 Lazy Loading 필요 없음
        assertThat(result.getContent().get(0).getMember().getNickname()).isEqualTo("a");
    }

    @Test
    @DisplayName("findByChatRoom")
    void findByChatRoom() {

        chatMessageRepository.save(ChatMessage.builder()
                .chatRoom(room)
                .member(user)
                .messageKey("k1")
                .createdAt(LocalDateTime.now())
                .build()
        );

        flushAndClear();

        List<ChatMessage> list = chatMessageRepository.findByChatRoom(room);
        assertThat(list.size()).isEqualTo(1);
    }

    @Test
    @DisplayName("findLatestMessageIdsByChatRoomId")
    void findLatestMessageIdsByChatRoomId() {

        ChatMessage m1 = chatMessageRepository.save(
                ChatMessage.builder().chatRoom(room).member(user)
                        .messageKey("k1").createdAt(LocalDateTime.now().minusMinutes(3)).build());

        ChatMessage m2 = chatMessageRepository.save(
                ChatMessage.builder().chatRoom(room).member(user)
                        .messageKey("k2").createdAt(LocalDateTime.now().minusMinutes(1)).build());

        ChatMessage m3 = chatMessageRepository.save(
                ChatMessage.builder().chatRoom(room).member(user)
                        .messageKey("k3").createdAt(LocalDateTime.now().minusMinutes(2)).build());

        flushAndClear();

        List<Long> ids = chatMessageRepository.findLatestMessageIdsByChatRoomId(
                room.getId(),
                PageRequest.of(0, 1)
        );

        assertThat(ids.get(0)).isEqualTo(m2.getId());
    }

    @Test
    @DisplayName("findByChatRoomIdWithMembers")
    void findByChatRoomIdWithMembers() {

        ChatRoomMember crm = ChatRoomMember.builder()
                .member(user)
                .chatRoom(room)
                .build();
        room.getChatRoomMembers().add(crm);
        em.persist(crm);

        ChatMessage msg = chatMessageRepository.save(
                ChatMessage.builder()
                        .chatRoom(room)
                        .member(user)
                        .messageKey("k1")
                        .createdAt(LocalDateTime.now())
                        .build()
        );

        flushAndClear();

        Page<ChatMessage> result = chatMessageRepository.findByChatRoomIdWithMembers(
                room.getId(), PageRequest.of(0, 10)
        );

        ChatMessage found = result.getContent().get(0);

        // fetch join 되어 있어야 Lazy 문제 없이 접근 가능함
        assertThat(found.getMember().getId()).isNotNull();
        assertThat(found.getChatRoom().getChatRoomMembers().size()).isEqualTo(1);
    }

    @BeforeEach
    void setup() {
        user = memberRepository.save(
                Member.builder().email("a@test.com").nickname("a").build()
        );

        room = chatRoomRepository.save(
                ChatRoom.builder().title("room").member(user).chatRoomMembers(new ArrayList<>()).build()
        );
    }

    private void flushAndClear() {
        em.flush();
        em.clear();
    }
}