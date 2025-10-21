package com.loopone.loopinbe.domain.chat.chatRoom.converter;

import com.loopone.loopinbe.domain.account.member.converter.SimpleMemberMapper;
import com.loopone.loopinbe.domain.account.member.dto.res.SimpleMemberResponse;
import com.loopone.loopinbe.domain.chat.chatRoom.dto.res.ChatRoomResponse;
import com.loopone.loopinbe.domain.chat.chatRoom.entity.ChatRoom;
import com.loopone.loopinbe.domain.chat.chatRoom.entity.ChatRoomMember;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring", uses = SimpleMemberMapper.class, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ChatRoomConverter {
    // ---------- ChatRoom -> ChatRoomResponse ----------
    @Mapping(target = "memberId", source = "chatRoom.member.id")
    @Mapping(target = "chatRoomMembers", source = "chatRoomMembers")
    ChatRoomResponse toChatRoomResponse(ChatRoom chatRoom, List<ChatRoomMember> chatRoomMembers);
}
