package com.loopone.loopinbe.global.kafka.event.chatRoom;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopone.loopinbe.domain.chat.chatRoom.dto.ChatRoomPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import static com.loopone.loopinbe.global.constants.KafkaKey.CREATE_AI_CHATROOM_TOPIC;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatRoomEventPublisher {
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public void publishChatRoomRequest(ChatRoomPayload payload) {
        try {
            String json = objectMapper.writeValueAsString(payload);
            kafkaTemplate.send(CREATE_AI_CHATROOM_TOPIC, String.valueOf(payload.memberId()), json);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to publish chat room create request", e);
        }
    }
}
