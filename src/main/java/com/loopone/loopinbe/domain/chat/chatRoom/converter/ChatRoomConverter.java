package com.loopone.loopinbe.domain.chat.chatRoom.converter;

import com.loopone.loopinbe.domain.account.member.converter.SimpleMemberMapper;
import com.loopone.loopinbe.domain.chat.chatRoom.dto.res.ChatRoomListResponse;
import com.loopone.loopinbe.domain.chat.chatRoom.dto.res.ChatRoomResponse;
import com.loopone.loopinbe.domain.chat.chatRoom.entity.ChatRoom;
import com.loopone.loopinbe.domain.chat.chatRoom.entity.ChatRoomMember;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", uses = SimpleMemberMapper.class, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ChatRoomConverter {
    // ---------- ChatRoom -> ChatRoomResponse ----------
    @Mapping(target = "memberId", source = "chatRoom.member.id")
    @Mapping(target = "chatRoomMembers", source = "chatRoomMembers")
    ChatRoomResponse toChatRoomResponse(ChatRoom chatRoom, List<ChatRoomMember> chatRoomMembers);

    // ---------- ChatRoomList -> ChatRoomListResponse ----------
    List<ChatRoomResponse> toChatRoomResponses(List<ChatRoom> chatRooms);

    default ChatRoomListResponse toChatRoomListResponse(List<ChatRoom> chatRooms) {
        return new ChatRoomListResponse(toChatRoomResponses(chatRooms));
    }
}
