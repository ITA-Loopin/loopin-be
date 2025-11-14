package com.loopone.loopinbe.domain.chat.chatroom.service;

import com.loopone.loopinbe.domain.account.auth.currentUser.CurrentUserDto;
import com.loopone.loopinbe.domain.account.member.converter.MemberConverter;
import com.loopone.loopinbe.domain.account.member.entity.Member;
import com.loopone.loopinbe.domain.account.member.repository.MemberRepository;
import com.loopone.loopinbe.domain.chat.chatMessage.service.ChatMessageService;
import com.loopone.loopinbe.domain.chat.chatRoom.converter.ChatRoomConverter;
import com.loopone.loopinbe.domain.chat.chatRoom.dto.req.ChatRoomRequest;
import com.loopone.loopinbe.domain.chat.chatRoom.dto.res.ChatRoomResponse;
import com.loopone.loopinbe.domain.chat.chatRoom.entity.ChatRoom;
import com.loopone.loopinbe.domain.chat.chatRoom.entity.ChatRoomMember;
import com.loopone.loopinbe.domain.chat.chatRoom.repository.ChatRoomMemberRepository;
import com.loopone.loopinbe.domain.chat.chatRoom.repository.ChatRoomRepository;
import com.loopone.loopinbe.domain.chat.chatRoom.serviceImpl.ChatRoomServiceImpl;
import com.loopone.loopinbe.global.exception.ServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
public class ChatRoomServiceTest {
    @Mock private ChatRoomRepository chatRoomRepository;
    @Mock private ChatMessageService chatMessageService;
    @Mock private MemberRepository memberRepository;
    @Mock private ChatRoomMemberRepository chatRoomMemberRepository;
    @Mock private MemberConverter memberConverter;
    @Mock private ChatRoomConverter chatRoomConverter;

    @InjectMocks
    private ChatRoomServiceImpl chatRoomService;

    private CurrentUserDto currentUser;

    @Test
    void addChatRoom_shouldThrowException_WhenOneOnOneRoomAlreadyExists() {
        // given
        ChatRoomRequest request = new ChatRoomRequest();
        Member target = Member.builder().id(2L).build();

        ChatRoomMember chatRoomMember = ChatRoomMember.builder().member(target).build();
        request.setChatRoomMembers(List.of(chatRoomMember));

        when(chatRoomRepository.existsOneOnOneChatRoom(1L, 2L)).thenReturn(true);

        // when + then
        assertThrows(ServiceException.class, () ->
                chatRoomService.addChatRoom(request, currentUser)
        );
        verify(chatRoomRepository, never()).save(any());
    }

    @Test
    void createAiChatRoom_shouldCreateRoomWithSingleMember() {
        // given
        Member member = Member.builder().id(1L).build();

        ChatRoomRequest request = new ChatRoomRequest();

        ChatRoom chatRoom = ChatRoom.builder()
                .id(10L)
                .member(member)
                .chatRoomMembers(new ArrayList<>())
                .build();

        ChatRoomResponse expected = new ChatRoomResponse();
        when(chatRoomRepository.save(any(ChatRoom.class))).thenReturn(chatRoom);
        when(chatRoomConverter.toChatRoomResponse(any(), any())).thenReturn(expected);

        // when
        ChatRoomResponse response = chatRoomService.createAiChatRoom(request, member);

        // then
        assertNotNull(response);
        verify(chatRoomRepository, times(1)).save(any(ChatRoom.class));
        verify(chatRoomConverter, times(1)).toChatRoomResponse(any(), any());
    }

    @Test
    void leaveAllChatRooms_shouldDeleteRoom_WhenLastMemberLeaves() {
        // given
        Member owner = Member.builder().id(1L).build();
        ChatRoom chatRoom = ChatRoom.builder().id(200L).member(owner).build();
        ChatRoomMember onlyMember = ChatRoomMember.builder().member(owner).chatRoom(chatRoom).build();
        chatRoom.setChatRoomMembers(new ArrayList<>(List.of(onlyMember)));

        when(chatRoomRepository.findByMemberId(1L)).thenReturn(List.of(chatRoom));

        // when
        chatRoomService.leaveAllChatRooms(1L);

        // then
        verify(chatMessageService).deleteAllChatMessages(200L);
        verify(chatRoomRepository).delete(chatRoom);
    }

    @BeforeEach
    void setUp() {
        currentUser = dummyUser(1L);
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
