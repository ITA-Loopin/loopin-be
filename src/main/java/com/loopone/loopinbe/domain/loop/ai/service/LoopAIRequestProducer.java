package com.loopone.loopinbe.domain.loop.ai.service;

public interface LoopAIRequestProducer {
    void sendRequest(String requestId, String prompt);
}
