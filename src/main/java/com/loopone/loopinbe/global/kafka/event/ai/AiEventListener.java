package com.loopone.loopinbe.global.kafka.event.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopone.loopinbe.domain.chat.chatMessage.dto.ChatInboundMessagePayload;
import com.loopone.loopinbe.domain.chat.chatMessage.dto.ChatMessageDto;
import com.loopone.loopinbe.domain.chat.chatMessage.dto.ChatMessageSavedResult;
import com.loopone.loopinbe.domain.chat.chatMessage.entity.ChatMessage;
import com.loopone.loopinbe.domain.chat.chatMessage.service.ChatMessageService;
import com.loopone.loopinbe.domain.loop.ai.dto.res.RecommendationsLoop;
import com.loopone.loopinbe.domain.loop.ai.service.LoopAIService;
import com.loopone.loopinbe.global.webSocket.handler.ChatWebSocketHandler;
import com.loopone.loopinbe.global.webSocket.payload.ChatWebSocketPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

import static com.loopone.loopinbe.global.constants.Constant.AI_CREATE_MESSAGE;
import static com.loopone.loopinbe.global.constants.Constant.AI_UPDATE_MESSAGE;
import static com.loopone.loopinbe.global.constants.KafkaKey.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class AiEventListener {

    private final ObjectMapper objectMapper;
    private final LoopAIService loopAIService;
    private final ChatWebSocketHandler chatWebSocketHandler;
    private final ChatMessageService chatMessageService;

    @KafkaListener(topics = OPEN_AI_CREATE_TOPIC, groupId = OPEN_AI_GROUP_ID, containerFactory = KAFKA_LISTENER_CONTAINER)
    public void consumeAiCreateLoop(ConsumerRecord<String, String> rec) {
        handleAiEvent(rec, AI_CREATE_MESSAGE);
    }

    @KafkaListener(topics = OPEN_AI_UPDATE_TOPIC, groupId = OPEN_AI_GROUP_ID, containerFactory = KAFKA_LISTENER_CONTAINER)
    public void consumeAiUpdateLoop(ConsumerRecord<String, String> rec) {
        handleAiEvent(rec, AI_UPDATE_MESSAGE);
    }

    /**
     * 공통 흐름 처리
     */
    private void handleAiEvent(
            ConsumerRecord<String, String> rec, String message) {
        try {
            AiRequestPayload req = objectMapper.readValue(rec.value(), AiRequestPayload.class);

            loopAIService.chat(req).thenAccept(loopRecommend -> {

                // 1) AI 결과 기반 Inbound 메시지 생성
                ChatInboundMessagePayload inbound = botInboundMessage(req, loopRecommend, message);

                // 2) 저장 (멱등 처리 포함)
                ChatMessageSavedResult saved = chatMessageService.processInbound(inbound);

                // 3) 브로드캐스트
                sendWebSocket(saved);

            }).exceptionally(ex -> {
                log.error("AI 이벤트 처리 중 비동기 오류: {}", ex.getMessage());
                return null;
            });

        } catch (Exception e) {
            log.error("AI 이벤트 처리 실패", e);
            throw new RuntimeException(e);
        }
    }

    private ChatInboundMessagePayload botInboundMessage(AiRequestPayload req, RecommendationsLoop recommendationsLoop,
                                                        String message) {
        return new ChatInboundMessagePayload(
                deterministicMessageKey(req),
                req.chatRoomId(),
                null,
                message,
                recommendationsLoop.recommendations(),
                ChatMessage.AuthorType.BOT,
                LocalDateTime.now());
    }

    /**
     * 멱등 키 생성
     */
    private String deterministicMessageKey(AiRequestPayload req) {
        // 예시 1) 요청ID 기반 or 사용자 메시지ID 기반: "ai-reply:"+req.userMessageId()
        return "ai:" + req.requestId();
    }

    /**
     * WebSocket 전송만 수행
     */
    private void sendWebSocket(ChatMessageSavedResult saved) {
        try {
            ChatMessageDto resp = ChatMessageDto.builder()
                    .id(saved.messageId())
                    .chatRoomId(saved.chatRoomId())
                    .memberId(saved.memberId())
                    .content(saved.content())
                    .recommendations(saved.recommendations())
                    .authorType(saved.authorType())
                    .createdAt(saved.createdAt())
                    .build();

            ChatWebSocketPayload out = ChatWebSocketPayload.builder()
                    .messageType(ChatWebSocketPayload.MessageType.MESSAGE)
                    .chatRoomId(saved.chatRoomId())
                    .chatMessageDto(resp)
                    .lastMessageCreatedAt(resp.getCreatedAt())
                    .build();

            chatWebSocketHandler.broadcastToRoom(
                    saved.chatRoomId(),
                    objectMapper.writeValueAsString(out));

        } catch (Exception e) {
            log.error("WebSocket 브로드캐스트 실패: {}", e.getMessage());
        }
    }
}