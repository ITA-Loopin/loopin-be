package com.loopone.loopinbe.domain.chat.chatRoom.serviceImpl;

import com.loopone.loopinbe.domain.chat.chatRoom.entity.ChatRoomMember;
import com.loopone.loopinbe.domain.chat.chatRoom.repository.ChatRoomMemberRepository;
import com.loopone.loopinbe.domain.chat.chatRoom.service.ChatRoomMemberService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatRoomMemberServiceImpl implements ChatRoomMemberService {
    private final ChatRoomMemberRepository chatRoomMemberRepository;

    // 메시지 읽은 시각 업데이트
    @Override
    @Transactional
    public Instant updateLastReadAt(Long chatRoomId, Long memberId, Instant lastReadAt) {
        if (lastReadAt == null) {
            throw new IllegalArgumentException("lastReadAt is required");
        }
        // (1) 존재 여부 체크 (없으면 권한/참여자 아님)
        if (!chatRoomMemberRepository.existsByChatRoom_IdAndMember_Id(chatRoomId, memberId)) {
            throw new IllegalStateException("Not a member of this chatRoom");
        }
        // (2) 증가하는 경우에만 업데이트
        chatRoomMemberRepository.updateLastReadAtIfGreater(chatRoomId, memberId, lastReadAt);

        // (3) 최종 값 조회해서 반환 (업데이트 안 됐으면 기존 값 그대로)
        return chatRoomMemberRepository.findByChatRoom_IdAndMember_Id(chatRoomId, memberId)
                .map(ChatRoomMember::getLastReadAt)
                .orElse(lastReadAt);
    }

    // 방 멤버십 검증 - isBotRoom=false + membership를 동시에 검증
    @Override
    @Transactional(readOnly = true)
    public boolean canConnectNonBotRoom(Long chatRoomId, Long memberId) {
        return chatRoomMemberRepository.existsConnectableMember(chatRoomId, memberId);
    }
}
