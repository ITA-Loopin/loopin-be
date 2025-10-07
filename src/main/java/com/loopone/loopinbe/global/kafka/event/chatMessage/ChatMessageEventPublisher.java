package com.loopone.loopinbe.global.kafka.event.chatMessage;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopone.loopinbe.domain.chat.chatMessage.dto.ChatInboundMessagePayload;
import com.loopone.loopinbe.global.webSocket.payload.ChatWebSocketPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import static com.loopone.loopinbe.global.constants.KafkaKey.SEND_MESSAGE_TOPIC;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatMessageEventPublisher {
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public void publishInbound(ChatInboundMessagePayload payload) {
        try {
            String json = objectMapper.writeValueAsString(payload);
            kafkaTemplate.send(SEND_MESSAGE_TOPIC, String.valueOf(payload.chatRoomId()), json); // key=roomId(순서보장)
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize inbound payload", e);
        }
    }
}
