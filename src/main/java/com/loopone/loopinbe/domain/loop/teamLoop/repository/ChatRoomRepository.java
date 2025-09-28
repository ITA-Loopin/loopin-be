package com.loopone.loopinbe.domain.loop.teamLoop.repository;

import com.letzgo.LetzgoBe.domain.account.member.entity.Member;
import com.letzgo.LetzgoBe.domain.chat.chatRoom.entity.ChatRoom;
import com.letzgo.LetzgoBe.domain.chat.chatRoom.entity.ChatRoomMember;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {
    // 사용자가 참여 중인 채팅방 목록을 최신 메시지 순으로 조회
    @EntityGraph(attributePaths = {"chatRoomMembers.member"})
    @Query("""
        SELECT cr
        FROM ChatRoom cr
        JOIN cr.chatRoomMembers crm
        WHERE crm.member = :member
        ORDER BY COALESCE(
            (SELECT MAX(cm.createdAt) FROM ChatMessage cm WHERE cm.chatRoom = cr),
            cr.createdAt
        ) DESC
    """)
    Page<ChatRoom> findChatRoomsByMemberOrderByLatestMessage(Pageable pageable, @Param("member") Member member);

    // 현재 사용자가 otherMember와 이미 1대1 채팅방이 존재하는지 확인
    @Query("SELECT EXISTS (" +
            "    SELECT 1 FROM ChatRoom c " +
            "    JOIN ChatRoomMember m1 ON c.id = m1.chatRoom.id " +
            "    JOIN ChatRoomMember m2 ON c.id = m2.chatRoom.id " +
            "    WHERE m1.member.id = :memberId1 " +
            "    AND m2.member.id = :memberId2 " +
            "    AND c.id IN (" +
            "        SELECT cm.chatRoom.id FROM ChatRoomMember cm " +
            "        GROUP BY cm.chatRoom.id " +
            "        HAVING COUNT(cm.chatRoom.id) = 2" +
            "    )" +
            ")")
    boolean existsOneOnOneChatRoom(@Param("memberId1") Long memberId1, @Param("memberId2") Long memberId2);

    // 해당 채팅방의 참여자 리스트 가져오기
    @Query("SELECT crm FROM ChatRoomMember crm JOIN FETCH crm.member WHERE crm.chatRoom.id = :chatRoomId")
    List<ChatRoomMember> findChatRoomMembersWithMember(@Param("chatRoomId") Long chatRoomId);

    // 기존 findById 대체용 (채팅방 + 참여 멤버 + 멤버 정보까지 한번에)
    @Query("""
        SELECT cr
        FROM ChatRoom cr
        JOIN FETCH cr.chatRoomMembers crm
        JOIN FETCH crm.member
        WHERE cr.id = :chatRoomId
    """)
    Optional<ChatRoom> findByIdWithMembers(@Param("chatRoomId") Long chatRoomId);

    // 멤버가 참여중인 모든 채팅방 조회 (N+1 방지)
    @Query("""
        SELECT DISTINCT cr
        FROM ChatRoom cr
        JOIN FETCH cr.chatRoomMembers crm
        JOIN FETCH crm.member m
        WHERE m.id = :memberId
    """)
    List<ChatRoom> findByMemberId(@Param("memberId") Long memberId);

    // 해당 채팅방의 참여자id 리스트 가져오기
    @Query("SELECT crm.member.id FROM ChatRoomMember crm WHERE crm.chatRoom.id = :chatRoomId")
    List<Long> findParticipantMemberIds(Long chatRoomId);
}
