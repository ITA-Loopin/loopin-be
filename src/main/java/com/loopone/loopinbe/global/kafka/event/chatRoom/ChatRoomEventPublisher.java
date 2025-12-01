package com.loopone.loopinbe.global.kafka.event.chatRoom;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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

    public void publishChatRoomRequest(ChatRoomCreatePayload payload) {
        try {
            String json = objectMapper.writeValueAsString(payload);
            // key 는 memberId 로 지정 (파티셔닝/트레이싱 용도)
            kafkaTemplate.send(CREATE_AI_CHATROOM_TOPIC, String.valueOf(payload.memberId()), json);
            log.info("ChatRoom create event published. memberId={}, requestId={}",
                    payload.memberId(), payload.requestId());
        } catch (JsonProcessingException e) {
            log.error("Failed to publish chat room create request", e);
            throw new RuntimeException("Failed to publish chat room create request", e);
        }
    }
}
