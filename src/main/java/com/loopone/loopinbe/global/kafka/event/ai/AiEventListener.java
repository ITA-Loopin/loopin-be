package com.loopone.loopinbe.global.kafka.event.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopone.loopinbe.domain.chat.chatMessage.dto.ChatInboundMessagePayload;
import com.loopone.loopinbe.domain.chat.chatMessage.dto.ChatMessageDto;
import com.loopone.loopinbe.domain.chat.chatMessage.entity.ChatMessage;
import com.loopone.loopinbe.domain.chat.chatMessage.service.ChatMessageService;
import com.loopone.loopinbe.domain.loop.ai.dto.res.RecommendationsLoop;
import com.loopone.loopinbe.domain.loop.ai.service.LoopAIService;
import com.loopone.loopinbe.global.exception.ServiceException;
import com.loopone.loopinbe.global.kafka.event.chatMessage.ChatMessageEventPublisher;
import com.loopone.loopinbe.global.webSocket.handler.ChatWebSocketHandler;
import com.loopone.loopinbe.global.webSocket.payload.ChatWebSocketPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import static com.loopone.loopinbe.global.constants.Constant.AI_RESPONSE_MESSAGE;
import static com.loopone.loopinbe.global.constants.KafkaKey.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class AiEventListener {
    private final ObjectMapper objectMapper;
    private final ChatMessageEventPublisher chatMessageEventPublisher; // send-message-topic 발행기
    private final LoopAIService loopAIService;
    private final ChatWebSocketHandler chatWebSocketHandler;
    private final ChatMessageService chatMessageService;

    @KafkaListener(
            topics = OPEN_AI_TOPIC,
            groupId = OPEN_AI_GROUP_ID,
            containerFactory = KAFKA_LISTENER_CONTAINER
    )
    public void onAiRequest(ConsumerRecord<String, String> rec) {
        try {
            AiRequestPayload req = objectMapper.readValue(rec.value(), AiRequestPayload.class);

            // 1) 프롬프트 구성 + LLM 호출
            RecommendationsLoop loopRecommend = loopAIService.chat(req);

            // 2) 여기서 AI 답변용 ChatInboundMessagePayload 생성
            ChatInboundMessagePayload botInbound = new ChatInboundMessagePayload(
                    deterministicMessageKey(req),       // 멱등키 (아래 참고)
                    req.chatRoomId(),
                    null,
                    AI_RESPONSE_MESSAGE,
                    loopRecommend.recommendations(),
                    ChatMessage.AuthorType.BOT,
                    java.time.LocalDateTime.now()
            );

            // 3) 브로드캐스트
            ChatMessageDto resp = ChatMessageDto.builder()
                    .tempId(botInbound.messageKey())
                    .chatRoomId(botInbound.chatRoomId())
                    .memberId(botInbound.memberId())
                    .content(botInbound.content())
                    .recommendations(botInbound.recommendations())
                    .authorType(botInbound.authorType())
                    .createdAt(botInbound.createdAt() != null
                            ? botInbound.createdAt()
                            : null)
                    .build();

            ChatWebSocketPayload out = ChatWebSocketPayload.builder()
                    .messageType(ChatWebSocketPayload.MessageType.MESSAGE)
                    .chatRoomId(botInbound.chatRoomId())
                    .chatMessageDto(resp)
                    .lastMessageCreatedAt(resp.getCreatedAt())
                    .build();
            chatWebSocketHandler.broadcastToRoom(botInbound.chatRoomId(), objectMapper.writeValueAsString(out));

            // 4) RDB 저장
            chatMessageService.processInbound(botInbound);
        } catch (ServiceException se) {
            log.warn("AI biz error: {}", se.getReturnCode(), se);
            throw se; // not-retry → DLT
        } catch (Exception e) {
            log.error("AI worker failed", e);
            throw new RuntimeException(e); // retry → 실패 시 DLT
        }
    }

    // 재시도에도 중복 저장 안 되도록 멱등키를 결정적으로 만든다
    private String deterministicMessageKey(AiRequestPayload req) {
        // 예시 1) 요청ID 기반 or 사용자 메시지ID 기반: "ai-reply:"+req.userMessageId()
        return "ai:" + req.requestId();
    }
}
