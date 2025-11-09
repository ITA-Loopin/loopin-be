package com.loopone.loopinbe.domain.chat.chatMessage.repository;

import com.loopone.loopinbe.domain.chat.chatMessage.entity.ChatMessage;
import com.loopone.loopinbe.domain.chat.chatRoom.entity.ChatRoom;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    // 해당 채팅방의 메시지 페이지 조회
    @Query("""
        SELECT m FROM ChatMessage m
        LEFT JOIN FETCH m.member
        JOIN FETCH m.chatRoom r
        WHERE r.id = :chatRoomId
    """)
    Page<ChatMessage> findByChatRoomId(@Param("chatRoomId") Long chatRoomId, Pageable pageable);

    // 해당 채팅방의 메시지 리스트 조회
    List<ChatMessage> findByChatRoom(ChatRoom chatRoom);

    // 가장 최근 메시지의 ID를 가져오는 메서드
    @Query("""
        SELECT cm.id
        FROM ChatMessage cm
        WHERE cm.chatRoom.id = :chatRoomId
        ORDER BY cm.createdAt DESC
    """)
    List<Long> findLatestMessageIdsByChatRoomId(@Param("chatRoomId") Long chatRoomId, Pageable pageable);

    @Query("""
           SELECT m FROM ChatMessage m
           JOIN FETCH m.member mem
           JOIN FETCH m.chatRoom r
           LEFT JOIN FETCH r.chatRoomMembers
           WHERE r.id = :chatRoomId
           """)
    Page<ChatMessage> findByChatRoomIdWithMembers(@Param("chatRoomId") Long chatRoomId, Pageable pageable);

    Optional<ChatMessage> findByMessageKey(String messageKey);
}
