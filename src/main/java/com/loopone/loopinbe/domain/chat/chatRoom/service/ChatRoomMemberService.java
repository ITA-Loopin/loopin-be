package com.loopone.loopinbe.domain.chat.chatRoom.service;

import java.time.Instant;

public interface ChatRoomMemberService {
    // 메시지 읽은 시각 업데이트
    Instant updateLastReadAt(Long chatRoomId, Long memberId, Instant lastReadAt);

    // 방 멤버십 검증 - isBotRoom=false + membership를 동시에 검증
    boolean canConnectNonBotRoom(Long chatRoomId, Long memberId);
}
