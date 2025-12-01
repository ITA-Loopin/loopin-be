package com.loopone.loopinbe.global.kafka.event.notification;

import com.loopone.loopinbe.domain.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventConsumer {

    private final NotificationService notificationService;

    private final Map<String, NotificationMeta> notificationMetaMap = Map.of(
            "follow-topic",  new NotificationMeta("팔로우 알림")
    );

    @KafkaListener(topics = {"follow-topic"}, groupId = "1")
    public void consume(ConsumerRecord<String, String> record) {
        String topic = record.topic();
        String message = record.value();
        NotificationMeta meta = notificationMetaMap.get(topic);

        if (meta == null) {
            // 알 수 없는 토픽이면 처리 불가 → 예외를 던져서 DLT로 유도하는 것도 방법
            log.error("Unknown topic received: {}", topic);
            throw new IllegalArgumentException("Unknown topic: " + topic);
        }

        // 비즈니스는 서비스로 위임
        notificationService.createAndNotifyFromMessage(message, meta.title());
    }
    private record NotificationMeta(String title) {}
}
