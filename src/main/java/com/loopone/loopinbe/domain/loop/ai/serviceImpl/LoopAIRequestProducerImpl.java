package com.loopone.loopinbe.domain.loop.ai.serviceImpl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopone.loopinbe.domain.loop.ai.service.LoopAIRequestProducer;
import com.loopone.loopinbe.global.exception.ReturnCode;
import com.loopone.loopinbe.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

import static com.loopone.loopinbe.global.constants.KafkaKey.OPEN_AI_TOPIC;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoopAIRequestProducerImpl implements LoopAIRequestProducer {
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Override
    public void sendRequest(String requestId, String prompt) {
        Map<String, String> payload = Map.of(
                "requestId", requestId,
                "prompt", prompt
        );
        try {
            kafkaTemplate.send(OPEN_AI_TOPIC, requestId, new ObjectMapper().writeValueAsString(payload));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new ServiceException(ReturnCode.KAFKA_SEND_ERROR, e.getMessage());
        }
    }
}
