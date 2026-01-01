package com.loopone.loopinbe.global.kafka.event.fcm;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopone.loopinbe.domain.fcm.dto.res.FcmMessageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import static com.loopone.loopinbe.global.constants.KafkaKey.FCM_SEND_TOPIC;

@Slf4j
@Component
@RequiredArgsConstructor
public class FcmEventPublisher {
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public void publishFcm(FcmMessageResponse message) {
        try {
            String json = objectMapper.writeValueAsString(message);
            kafkaTemplate.send(FCM_SEND_TOPIC, message.getTargetToken(), json);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize FCM message", e);
        }
    }
}
