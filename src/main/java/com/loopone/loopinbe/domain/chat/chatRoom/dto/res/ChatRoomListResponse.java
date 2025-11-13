package com.loopone.loopinbe.domain.chat.chatRoom.dto.res;

import lombok.Builder;

import java.util.List;

@Builder
public record ChatRoomListResponse(
        List<ChatRoomResponse> chatRooms
) {
}
