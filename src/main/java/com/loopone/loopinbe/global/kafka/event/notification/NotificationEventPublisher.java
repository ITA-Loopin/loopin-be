package com.loopone.loopinbe.global.kafka.event.notification;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopone.loopinbe.domain.notification.entity.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventPublisher {
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public void publishFollowRequest(Notification notification) {
        try {
            String json = objectMapper.writeValueAsString(notification);
            kafkaTemplate.send("follow-topic", String.valueOf(notification.getReceiverId()), json);  // key = receiverId
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize follow notification", e);
        }
    }
}
