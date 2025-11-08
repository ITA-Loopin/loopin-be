package com.loopone.loopinbe.domain.chat.chatRoom.repository;

import com.loopone.loopinbe.domain.chat.chatRoom.entity.ChatRoomMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChatRoomMemberRepository extends JpaRepository<ChatRoomMember, Long> {
    // memberId, chatRoomID로 ChatRoomMember 찾기
    ChatRoomMember findByMemberIdAndChatRoomId(Long currentMemberId, Long chatRoomId);

    // 특정 채팅방의 멤버 수 조회
    Long countByChatRoomId(Long chatRoomId);
}
