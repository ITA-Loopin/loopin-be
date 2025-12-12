package com.loopone.loopinbe.domain.account.chat.chatmessage.service;

import com.loopone.loopinbe.domain.account.auth.currentUser.CurrentUserDto;
import com.loopone.loopinbe.domain.account.member.entity.Member;
import com.loopone.loopinbe.domain.account.member.repository.MemberRepository;
import com.loopone.loopinbe.domain.chat.chatMessage.converter.ChatMessageConverter;
import com.loopone.loopinbe.domain.chat.chatMessage.dto.ChatMessagePayload;
import com.loopone.loopinbe.domain.chat.chatMessage.dto.ChatMessageDto;
import com.loopone.loopinbe.domain.chat.chatMessage.dto.ChatMessageSavedResult;
import com.loopone.loopinbe.domain.chat.chatMessage.entity.ChatMessage;
import com.loopone.loopinbe.domain.chat.chatMessage.entity.MessageContent;
import com.loopone.loopinbe.domain.chat.chatMessage.repository.ChatMessageRepository;
import com.loopone.loopinbe.domain.chat.chatMessage.repository.MessageContentRepository;
import com.loopone.loopinbe.domain.chat.chatMessage.serviceImpl.ChatMessageServiceImpl;
import com.loopone.loopinbe.domain.chat.chatRoom.entity.ChatRoom;
import com.loopone.loopinbe.domain.chat.chatRoom.entity.ChatRoomMember;
import com.loopone.loopinbe.domain.chat.chatRoom.repository.ChatRoomMemberRepository;
import com.loopone.loopinbe.domain.chat.chatRoom.repository.ChatRoomRepository;
import com.loopone.loopinbe.global.common.response.PageResponse;
import com.loopone.loopinbe.global.exception.ServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;

@ExtendWith(SpringExtension.class)
public class ChatMessageServiceTest {
    @InjectMocks
    ChatMessageServiceImpl chatMessageService;

    @Mock
    ChatMessageRepository chatMessageRepository;
    @Mock
    ChatRoomRepository chatRoomRepository;
    @Mock
    MemberRepository memberRepository;
    @Mock
    MessageContentRepository messageContentRepository;
    @Mock
    ChatRoomMemberRepository chatRoomMemberRepository;
    @Mock
    ChatMessageConverter chatMessageConverter;

    CurrentUserDto user;

    @Test
    @DisplayName("findByChatRoomId - 참여자 아님 → 예외 발생")
    void findByChatRoomId_notMember_throwException() {
        given(chatRoomMemberRepository.findByMemberIdAndChatRoomId(1L, 10L))
                .willReturn(null);

        assertThrows(ServiceException.class, () ->
                chatMessageService.findByChatRoomId(10L, PageRequest.of(0, 10), user)
        );
    }

    @Test
    @DisplayName("findByChatRoomId - 정상 조회 시 DTO 변환 성공")
    void findByChatRoomId_success() {

        given(chatRoomMemberRepository.findByMemberIdAndChatRoomId(1L, 10L))
                .willReturn(new ChatRoomMember());

        ChatMessage msg = ChatMessage.builder()
                .id(100L)
                .messageKey("m1")
                .createdAt(LocalDateTime.now())
                .build();

        Pageable pageable = PageRequest.of(0, 10);
        var page = new PageImpl<>(List.of(msg), pageable, 1);

        given(chatMessageRepository.findByChatRoomId(eq(10L), any()))
                .willReturn(page);

        MessageContent content = new MessageContent("m1", "hi", List.of());
        given(messageContentRepository.findByIdIn(List.of("m1")))
                .willReturn(List.of(content));

        ChatMessageDto dto = new ChatMessageDto();
        given(chatMessageConverter.toChatMessageDto(eq(msg), eq("hi"), eq(List.of())))
                .willReturn(dto);

        PageResponse<ChatMessageDto> result =
                chatMessageService.findByChatRoomId(10L, PageRequest.of(0, 20), user);

        assertThat(result.getContent().size()).isEqualTo(1);
    }

    @Test
    @DisplayName("processInbound - 중복 messageKey 기존 메시지 재사용")
    void processInbound_idempotent() {

        ChatMessagePayload in = new ChatMessagePayload(
                "m1",
                10L,
                1L,
                "hello",
                List.of(),
                ChatMessage.AuthorType.USER,
                LocalDateTime.now()
        );

        given(chatRoomRepository.existsMember(10L, 1L))
                .willReturn(true);

        ChatMessage existing = ChatMessage.builder()
                .id(500L)
                .messageKey("m1")
                .build();

        given(chatMessageRepository.findByMessageKey("m1"))
                .willReturn(Optional.of(existing));

        given(chatRoomRepository.getReferenceById(10L))
                .willReturn(ChatRoom.builder()
                        .id(10L)
                        .isBotRoom(true)
                        .build());

        given(memberRepository.getReferenceById(1L))
                .willReturn(Member.builder().id(1L).build());

        ChatMessageSavedResult result = chatMessageService.processInbound(in);

        assertThat(result.messageId()).isEqualTo(500L);
        verify(chatMessageRepository, never()).save(any());
    }

    @Test
    @DisplayName("processInbound - 신규 메시지 저장 성공")
    void processInbound_newMessage() {

        ChatMessagePayload in = new ChatMessagePayload(
                "m1",
                10L,
                1L,
                "hello",
                List.of(),
                ChatMessage.AuthorType.USER,
                LocalDateTime.now()
        );

        given(chatRoomRepository.existsMember(10L, 1L)).willReturn(true);

        given(chatMessageRepository.findByMessageKey("m1"))
                .willReturn(Optional.empty());

        given(chatRoomRepository.getReferenceById(10L))
                .willReturn(ChatRoom.builder()
                        .id(10L)
                        .isBotRoom(true)
                        .build());

        given(memberRepository.getReferenceById(1L))
                .willReturn(Member.builder().id(1L).build());

        ChatMessage saved = ChatMessage.builder()
                .id(700L)
                .messageKey("m1")
                .build();

        given(chatMessageRepository.save(any()))
                .willReturn(saved);

        ChatMessageSavedResult result = chatMessageService.processInbound(in);

        assertThat(result.messageId()).isEqualTo(700L);
        verify(messageContentRepository).upsert("m1", "hello", List.of());
    }

    @Test
    @DisplayName("deleteAllChatMessages - 메시지 모두 삭제")
    void deleteAllChatMessages() {

        ChatRoom room = ChatRoom.builder().id(10L).build();

        given(chatRoomRepository.findById(10L))
                .willReturn(Optional.of(room));

        ChatMessage m1 = ChatMessage.builder().id(1L).build();
        ChatMessage m2 = ChatMessage.builder().id(2L).build();

        given(chatMessageRepository.findByChatRoom(room))
                .willReturn(List.of(m1, m2));

        chatMessageService.deleteAllChatMessages(10L);

        verify(messageContentRepository).deleteById("1");
        verify(messageContentRepository).deleteById("2");

        verify(chatMessageRepository).deleteAll(List.of(m1, m2));
    }

    @BeforeEach
    void setUp() {
        user = dummyUser(1L);
    }

    private CurrentUserDto dummyUser(Long id) {
        return new CurrentUserDto(
                id,
                "test" + id + "@test.com",
                "pw",
                "user" + id,
                "010-0000-0000",
                Member.Gender.MALE,
                LocalDate.of(1999, 1, 1),
                null,
                Member.State.NORMAL,
                Member.MemberRole.ROLE_USER,
                Member.OAuthProvider.KAKAO,
                null
        );
    }
}
