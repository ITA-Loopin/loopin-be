package com.loopone.loopinbe.global.kafka.event.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import static com.loopone.loopinbe.global.constants.KafkaKey.OPEN_AI_TOPIC;

@Slf4j
@Component
@RequiredArgsConstructor
public class AiEventPublisher {
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public void publishAiRequest(AiRequestPayload payload) {
        try {
            String json = objectMapper.writeValueAsString(payload);
            kafkaTemplate.send(OPEN_AI_TOPIC, String.valueOf(payload.chatRoomId()), json);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to publish AI request", e);
        }
    }
}
