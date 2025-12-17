package com.loopone.loopinbe.global.kafka.event.chatMessage;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopone.loopinbe.domain.chat.chatMessage.dto.ChatMessagePayload;
import com.loopone.loopinbe.domain.chat.chatMessage.dto.ChatMessageDto;
import com.loopone.loopinbe.domain.chat.chatMessage.dto.ChatMessageSavedResult;
import com.loopone.loopinbe.domain.chat.chatMessage.entity.ChatMessage;
import com.loopone.loopinbe.domain.chat.chatMessage.entity.type.MessageType;
import com.loopone.loopinbe.domain.chat.chatMessage.service.ChatMessageService;
import com.loopone.loopinbe.domain.chat.chatRoom.service.ChatRoomService;
import com.loopone.loopinbe.domain.loop.loop.dto.res.LoopDetailResponse;
import com.loopone.loopinbe.global.kafka.event.ai.AiEventPublisher;
import com.loopone.loopinbe.domain.loop.ai.dto.AiPayload;
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
public class ChatMessageEventConsumer {
    private final ObjectMapper objectMapper;
    private final ChatMessageService chatMessageService; // 저장/검증은 서비스에서
    private final ChatWebSocketHandler chatWebSocketHandler; // 브로드캐스트
    private final AiEventPublisher aiEventPublisher;
    private final ChatRoomService chatRoomService;

    // 루프 생성
    @KafkaListener(topics = CREATE_LOOP_TOPIC, groupId = CHAT_GROUP_ID, containerFactory = KAFKA_LISTENER_CONTAINER)
    public void consumeCreateLoop(ConsumerRecord<String, String> record) {
        log.info("Consume create loop: {}", record.value());
        handleEvent(record, OPEN_AI_CREATE_TOPIC);
        log.info("Consume create loop end");
    }

    // 루프 업데이트
    @KafkaListener(topics = UPDATE_LOOP_TOPIC, groupId = CHAT_GROUP_ID, containerFactory = KAFKA_LISTENER_CONTAINER)
    public void consumeUpdateLoop(ConsumerRecord<String, String> record) {
        log.info("Consume update loop: {}", record.value());
        handleEvent(record, OPEN_AI_UPDATE_TOPIC);
        log.info("Consume update loop end");
    }

    private void handleEvent(ConsumerRecord<String, String> record, String aiTopic) {
        ChatMessagePayload payload = deserialize(record.value());

        ChatMessageSavedResult saved = chatMessageService.processInbound(payload);

        ChatWebSocketPayload out = buildWebSocketPayload(saved);

        LoopDetailResponse loopDetailResponse = chatRoomService.findLoopDetailResponse(saved.chatRoomId());

        log.info("Loop detail response: {}", loopDetailResponse);

        sendWebSocket(saved.chatRoomId(), out);

        if (shouldTriggerAI(saved)) {
            publishAI(saved, loopDetailResponse, aiTopic);
        }
    }

    // JSON을 payload로 변경
    private ChatMessagePayload deserialize(String json) {
        try {
            return objectMapper.readValue(json, ChatMessagePayload.class);
        } catch (Exception e) {
            throw new RuntimeException("Invalid JSON: " + json, e);
        }
    }

    // ChatWebSocketPayload 생성
    private ChatWebSocketPayload buildWebSocketPayload(ChatMessageSavedResult saved) {
        ChatMessageDto resp = ChatMessageDto.builder()
                .id(saved.messageId())
                .chatRoomId(saved.chatRoomId())
                .memberId(saved.memberId())
                .content(saved.content())
                .recommendations(saved.recommendations())
                .authorType(saved.authorType())
                .createdAt(saved.createdAt())
                .build();

        return ChatWebSocketPayload.builder()
                .messageType(MessageType.MESSAGE)
                .chatRoomId(saved.chatRoomId())
                .chatMessageDto(resp)
                .lastMessageCreatedAt(resp.getCreatedAt())
                .build();
    }

    // webSocket 전송
    private void sendWebSocket(Long roomId, ChatWebSocketPayload out) {
        try {
            chatWebSocketHandler.broadcastToRoom(roomId, objectMapper.writeValueAsString(out));
        } catch (Exception e) {
            log.error("WebSocket send failed (ignored)", e);
        }
    }

    // AI 호출 여부 판단
    private boolean shouldTriggerAI(ChatMessageSavedResult saved) {
        return saved.authorType() == ChatMessage.AuthorType.USER
                && saved.isBotRoom();
    }

    // AI 호출
    private void publishAI(ChatMessageSavedResult saved, LoopDetailResponse loopDetailResponse, String topic) {
        AiPayload req = new AiPayload(
                java.util.UUID.randomUUID().toString(),
                saved.chatRoomId(),
                saved.messageId(),
                saved.memberId(),
                saved.content(),
                loopDetailResponse,
                java.time.Instant.now());
        aiEventPublisher.publishAiRequest(req, topic);
    }
}
