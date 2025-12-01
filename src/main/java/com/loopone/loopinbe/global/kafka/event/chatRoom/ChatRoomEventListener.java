package com.loopone.loopinbe.global.kafka.event.chatRoom;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopone.loopinbe.domain.account.member.entity.Member;
import com.loopone.loopinbe.domain.account.member.repository.MemberRepository;
import com.loopone.loopinbe.domain.chat.chatRoom.dto.req.ChatRoomRequest;
import com.loopone.loopinbe.domain.chat.chatRoom.dto.res.ChatRoomResponse;
import com.loopone.loopinbe.domain.chat.chatRoom.service.ChatRoomService;
import com.loopone.loopinbe.global.exception.ReturnCode;
import com.loopone.loopinbe.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import static com.loopone.loopinbe.global.constants.KafkaKey.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatRoomEventListener {
    private final ObjectMapper objectMapper;
    private final ChatRoomService chatRoomService;
    private final MemberRepository memberRepository;

    @KafkaListener(
            topics = CREATE_AI_CHATROOM_TOPIC,
            groupId = AI_CHATROOM_GROUP_ID,
            containerFactory = KAFKA_LISTENER_CONTAINER
    )
    @Transactional
    public void consumeChatRoomCreate(ConsumerRecord<String, String> rec) {
        try {
            ChatRoomCreatePayload payload =
                    objectMapper.readValue(rec.value(), ChatRoomCreatePayload.class);
            Long memberId = payload.memberId();
            log.info("Consume ChatRoom create event. memberId={}, requestId={}",
                    memberId, payload.requestId());
            Member member = memberRepository.findById(memberId)
                    .orElseThrow(() -> new ServiceException(ReturnCode.USER_NOT_FOUND));

            // 기존 createAiChatRoom 로직 재사용
            ChatRoomRequest chatRoomRequest = ChatRoomRequest.builder().build();
            ChatRoomResponse chatRoomResponse = chatRoomService.createAiChatRoom(chatRoomRequest, member);

            // member 에 chatRoomId 세팅 후 저장
            member.setChatRoomId(chatRoomResponse.getId());
            memberRepository.save(member);
            log.info("AI chat room created. chatRoomId={} for memberId={}, requestId={}",
                    chatRoomResponse.getId(), member.getId(), payload.requestId());
        } catch (Exception e) {
            log.error("Failed to handle ChatRoom create event", e);
            // 재시도/데드레터 큐 전략에 따라 처리
            throw new RuntimeException(e);
        }
    }
}
