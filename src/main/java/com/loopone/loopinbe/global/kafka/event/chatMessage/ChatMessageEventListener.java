package com.loopone.loopinbe.global.kafka.event.chatMessage;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopone.loopinbe.domain.chat.chatMessage.dto.ChatInboundMessagePayload;
import com.loopone.loopinbe.domain.chat.chatMessage.dto.ChatMessageDto;
import com.loopone.loopinbe.domain.chat.chatMessage.dto.ChatMessageSavedResult;
import com.loopone.loopinbe.domain.chat.chatMessage.entity.ChatMessage;
import com.loopone.loopinbe.domain.chat.chatMessage.service.ChatMessageService;
import com.loopone.loopinbe.global.exception.ServiceException;
import com.loopone.loopinbe.global.kafka.event.ai.AiEventPublisher;
import com.loopone.loopinbe.global.kafka.event.ai.AiRequestPayload;
import com.loopone.loopinbe.global.webSocket.handler.ChatWebSocketHandler;
import com.loopone.loopinbe.global.webSocket.payload.ChatWebSocketPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import static com.loopone.loopinbe.global.constants.KafkaKey.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatMessageEventListener {
    private final ObjectMapper objectMapper;
    private final ChatMessageService chatMessageService;       // 저장/검증은 서비스에서
    private final ChatWebSocketHandler chatWebSocketHandler;   // 브로드캐스트
    private final AiEventPublisher aiEventPublisher;

    @KafkaListener(
            topics = SEND_MESSAGE_TOPIC,
            groupId = CHAT_GROUP_ID,
            containerFactory = KAFKA_LISTENER_CONTAINER
    )
    public void consume(ConsumerRecord<String, String> record) {
        try {
            ChatInboundMessagePayload in = objectMapper.readValue(record.value(), ChatInboundMessagePayload.class);

            // 1) 서비스에 위임 (트랜잭션/멱등/검증)
            ChatMessageSavedResult saved = chatMessageService.processInbound(in);

            // 2) 브로드캐스트 (DB 생성 시각/ID 사용)
            ChatMessageDto resp = ChatMessageDto.builder()
                    .id(saved.messageId())
                    .chatRoomId(saved.chatRoomId())
                    .memberId(saved.memberId())
                    .content(saved.content())
                    .authorType(saved.authorType())
                    .createdAt(saved.createdAt() != null
                            ? saved.createdAt()
                            : null)
                    .build();
            ChatWebSocketPayload out = ChatWebSocketPayload.builder()
                    .messageType(ChatWebSocketPayload.MessageType.MESSAGE)
                    .chatRoomId(saved.chatRoomId())
                    .chatMessageDto(resp)
                    .lastMessageCreatedAt(resp.getCreatedAt())
                    .build();
            chatWebSocketHandler.broadcastToRoom(saved.chatRoomId(), objectMapper.writeValueAsString(out));

            // 4) 분기: "사람 메시지" && "BOT 방"이면 AI 요청 발행
            if (saved.authorType() == ChatMessage.AuthorType.USER && saved.isBotRoom()) {
                AiRequestPayload req = new AiRequestPayload(
                        java.util.UUID.randomUUID().toString(),
                        saved.chatRoomId(),
                        saved.messageId(),
                        saved.memberId(),
                        saved.content(),
                        java.time.Instant.now()
                );
                aiEventPublisher.publishAiRequest(req);
            }
        } catch (ServiceException se) {
            // 비즈니스 예외 (권한 등) → 컨테이너에서 비재시도로 처리하도록 설정 권장
            log.warn("business error in consume: {}", se.getReturnCode(), se);
            throw se; // DefaultErrorHandler에서 not-retry 설정 필요
        } catch (Exception e) {
            log.error("consume failed", e);
            // 재시도/ DLT는 컨테이너 에러핸들러에 맡긴다
            throw new RuntimeException(e);
        }
    }
}
