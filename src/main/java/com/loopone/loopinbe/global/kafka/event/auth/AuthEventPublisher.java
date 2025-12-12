package com.loopone.loopinbe.global.kafka.event.auth;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopone.loopinbe.domain.account.auth.dto.AuthPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import static com.loopone.loopinbe.global.constants.KafkaKey.LOGOUT_TOPIC;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthEventPublisher {
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    // 트랜잭션이 있으면 "커밋 후"에 보내고, 없으면 즉시 보냄
    public void publishLogoutAfterCommit(AuthPayload payload) {
        if (org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive()) {
            org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
                    new org.springframework.transaction.support.TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            publishAuthRequest(payload);
                        }
                    }
            );
        } else {
            publishAuthRequest(payload);
        }
    }

    public void publishAuthRequest(AuthPayload payload) {
        try {
            String json = objectMapper.writeValueAsString(payload);
            kafkaTemplate.send(LOGOUT_TOPIC, String.valueOf(payload.memberId()), json);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to publish auth logout event", e);
        }
    }
}
