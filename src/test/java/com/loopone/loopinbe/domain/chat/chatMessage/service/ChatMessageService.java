package com.loopone.loopinbe.domain.chat.chatMessage.service;

import com.loopone.loopinbe.domain.account.auth.currentUser.CurrentUserDto;
import com.loopone.loopinbe.domain.chat.chatMessage.dto.ChatMessagePayload;
import com.loopone.loopinbe.domain.chat.chatMessage.dto.res.AiChatMessageResponse;
import com.loopone.loopinbe.global.common.response.PageResponse;
import org.springframework.data.domain.Pageable;

public interface ChatMessageService {
    // 채팅방 과거 메시지 조회 [참여자 권한]
    PageResponse<AiChatMessageResponse> findByChatRoomId(Long chatRoomId, Pageable pageable, CurrentUserDto currentUser);

    // 채팅방 메시지 검색(내용) [참여자 권한]
    PageResponse<AiChatMessageResponse> searchByKeyword(Long chatRoomId, String keyword, Pageable pageable, CurrentUserDto currentUser);

    // Kafka 인바운드 메시지 처리(권한검증 + 멱등 저장 + Mongo 업서트)
    ChatMessagePayload processInbound(ChatMessagePayload in);

    // 채팅방의 모든 메시지 삭제
    void deleteAllChatMessages(Long chatRoomId);
}
