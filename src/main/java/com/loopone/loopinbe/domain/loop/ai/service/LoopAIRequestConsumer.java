package com.loopone.loopinbe.domain.loop.ai.service;

import com.fasterxml.jackson.core.JsonProcessingException;

public interface LoopAIRequestConsumer {
    void consume(String message) throws JsonProcessingException;
}
