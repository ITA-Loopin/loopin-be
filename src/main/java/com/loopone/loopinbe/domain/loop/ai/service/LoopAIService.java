package com.loopone.loopinbe.domain.loop.ai.service;

import com.loopone.loopinbe.domain.loop.ai.dto.res.RecommendationsLoop;
import com.loopone.loopinbe.global.kafka.event.ai.AiRequestPayload;

public interface LoopAIService {
    RecommendationsLoop chat(AiRequestPayload message);
}
