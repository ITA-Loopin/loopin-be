package com.loopone.loopinbe.domain.chat.chatRoom.service;

import com.loopone.loopinbe.domain.account.auth.currentUser.CurrentUserDto;
import com.loopone.loopinbe.domain.account.member.entity.Member;
import com.loopone.loopinbe.domain.chat.chatRoom.dto.req.ChatRoomRequest;
import com.loopone.loopinbe.domain.chat.chatRoom.dto.res.ChatRoomResponse;
import com.loopone.loopinbe.global.common.response.PageResponse;
import org.springframework.data.domain.Pageable;

public interface ChatRoomService {
    // 채팅방 생성(DM/그룹)
    ChatRoomResponse addChatRoom(ChatRoomRequest chatRoomRequest, CurrentUserDto currentUser);

    // AI 채팅방 생성
    ChatRoomResponse createAiChatRoom(ChatRoomRequest chatRoomRequest, Member member);

    // 멤버가 참여중인 모든 채팅방 나가기(DM/그룹)
    void leaveAllChatRooms(Long memberId);
}
