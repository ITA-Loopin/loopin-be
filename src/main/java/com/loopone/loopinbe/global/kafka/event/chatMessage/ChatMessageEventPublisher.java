package com.loopone.loopinbe.global.kafka.event.chatMessage;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopone.loopinbe.domain.chat.chatMessage.entity.type.MessageType;
import com.loopone.loopinbe.global.webSocket.payload.ChatWebSocketPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import static com.loopone.loopinbe.global.constants.KafkaKey.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatMessageEventPublisher {
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public void publishWsEvent(ChatWebSocketPayload payload) {
        try {
            String topic = resolveTopic(payload.getMessageType());
            String key = String.valueOf(payload.getChatRoomId());
            String json = objectMapper.writeValueAsString(payload);
            kafkaTemplate.send(topic, key, json);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to publish chat message WS event", e);
        }
    }

    private String resolveTopic(MessageType type) {
        return switch (type) {
            case MESSAGE -> CHAT_MESSAGE_TOPIC;
            case READ_UP_TO -> CHAT_READ_UP_TO_TOPIC;
            case DELETE -> CHAT_DELETE_TOPIC;
            default -> throw new IllegalArgumentException("Unsupported WS messageType: " + type);
        };
    }
}
