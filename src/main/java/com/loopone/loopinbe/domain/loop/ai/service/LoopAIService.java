package com.loopone.loopinbe.domain.loop.ai.service;

import com.loopone.loopinbe.global.kafka.event.ai.AiRequestPayload;

public interface LoopAIService {
    String chat(AiRequestPayload message);
}
