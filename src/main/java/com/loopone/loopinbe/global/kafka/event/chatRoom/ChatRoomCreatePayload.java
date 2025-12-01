package com.loopone.loopinbe.global.kafka.event.chatRoom;

public record ChatRoomCreatePayload(
        String requestId,
        Long memberId
) {}
