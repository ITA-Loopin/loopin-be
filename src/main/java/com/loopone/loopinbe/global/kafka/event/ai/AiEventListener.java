package com.loopone.loopinbe.global.kafka.event.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopone.loopinbe.domain.chat.chatMessage.dto.ChatInboundMessagePayload;
import com.loopone.loopinbe.domain.chat.chatMessage.entity.ChatMessage;
import com.loopone.loopinbe.global.exception.ServiceException;
import com.loopone.loopinbe.global.kafka.event.chatMessage.ChatMessageEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AiEventListener {
    private final ObjectMapper objectMapper;
    private final ChatMessageEventPublisher chatMessageEventPublisher; // send-message-topic 발행기

    @KafkaListener(
            topics = "ai-request-topic",
            groupId = "ai-worker",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onAiRequest(ConsumerRecord<String, String> rec) {
        try {
            AiRequestPayload req = objectMapper.readValue(rec.value(), AiRequestPayload.class);

            // 1) 프롬프트 구성 + LLM 호출
            String loopRecommend = "---------- NeedToCallService -----------";

            // 2) 여기서 AI 답변용 ChatInboundMessagePayload 생성
            ChatInboundMessagePayload botInbound = new ChatInboundMessagePayload(
                    deterministicMessageKey(req),       // 멱등키 (아래 참고)
                    req.chatRoomId(),
                    null,
                    loopRecommend,
                    ChatMessage.AuthorType.BOT,
                    java.time.LocalDateTime.now()
            );

            // 3) 기존 파이프라인 재사용: send-message-topic 으로 재주입
            chatMessageEventPublisher.publishInbound(botInbound);
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
