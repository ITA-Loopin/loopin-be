package com.loopone.loopinbe.domain.chat.chatRoom.repository;

import com.loopone.loopinbe.domain.chat.chatRoom.entity.ChatRoomMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface ChatRoomMemberRepository extends JpaRepository<ChatRoomMember, Long> {
    // 특정 채팅방의 멤버 수 조회
    Long countByChatRoomId(Long chatRoomId);

    boolean existsByChatRoom_IdAndMember_Id(Long chatRoomId, Long memberId);

    Optional<ChatRoomMember> findByChatRoom_IdAndMember_Id(Long chatRoomId, Long memberId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update ChatRoomMember crm
           set crm.lastReadAt = :lastReadAt
         where crm.chatRoom.id = :chatRoomId
           and crm.member.id   = :memberId
           and (crm.lastReadAt is null or crm.lastReadAt < :lastReadAt)
    """)
    int updateLastReadAtIfGreater(
            @Param("chatRoomId") Long chatRoomId,
            @Param("memberId") Long memberId,
            @Param("lastReadAt") Instant lastReadAt
    );

    @Query("""
        SELECT (COUNT(crm) > 0)
        FROM ChatRoomMember crm
        JOIN crm.chatRoom cr
        WHERE cr.id = :chatRoomId
          AND crm.member.id = :memberId
          AND cr.isBotRoom = false
    """)
    boolean existsConnectableMember(@Param("chatRoomId") Long chatRoomId, @Param("memberId") Long memberId);
}
