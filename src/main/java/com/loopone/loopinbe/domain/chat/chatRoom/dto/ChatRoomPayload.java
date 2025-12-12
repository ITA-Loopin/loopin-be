package com.loopone.loopinbe.domain.chat.chatRoom.dto;

public record ChatRoomPayload(
        String requestId,
        Long memberId
) {}
