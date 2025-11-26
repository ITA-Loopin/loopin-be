package com.loopone.loopinbe.global.kafka.event.chatMessage;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopone.loopinbe.domain.chat.chatMessage.dto.ChatInboundMessagePayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatMessageEventPublisher {
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public void publishInbound(ChatInboundMessagePayload payload, String topic) {
        try {
            String json = objectMapper.writeValueAsString(payload);
            kafkaTemplate.send(topic, String.valueOf(payload.chatRoomId()), json); // key=roomId(순서보장)
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize inbound payload", e);
        }
    }
}
