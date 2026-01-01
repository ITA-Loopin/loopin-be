package com.loopone.loopinbe.global.kafka.event.chatMessage;

import com.fasterxml.jackson.databind.ObjectMapper;
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
    private final ChatWebSocketHandler chatWebSocketHandler;

    @KafkaListener(topics = {CHAT_MESSAGE_TOPIC, CHAT_READ_UP_TO_TOPIC},
            groupId = CHAT_GROUP_ID, containerFactory = KAFKA_LISTENER_CONTAINER)
    public void consumeWsEvent(ConsumerRecord<String, String> rec) {
        try {
            ChatWebSocketPayload payload = objectMapper.readValue(rec.value(), ChatWebSocketPayload.class);
            Long chatRoomId = payload.getChatRoomId();
            if (chatRoomId == null) {
                log.warn("WS event missing chatRoomId. topic={}, key={}", rec.topic(), rec.key());
                return;
            }
            chatWebSocketHandler.broadcastToRoom(chatRoomId, rec.value());
            log.debug("Consumed WS event & broadcasted. roomId={}, type={}", chatRoomId, payload.getMessageType());
        } catch (Exception e) {
            log.error("Failed to handle WS event. topic={}, key={}", rec.topic(), rec.key(), e);
            throw new RuntimeException(e);
        }
    }
}
