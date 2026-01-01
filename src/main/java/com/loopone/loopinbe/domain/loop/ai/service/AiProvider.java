package com.loopone.loopinbe.domain.loop.ai.service;

import com.loopone.loopinbe.domain.loop.ai.dto.AiPayload;
import com.loopone.loopinbe.domain.loop.ai.dto.res.RecommendationsLoop;

public interface AiProvider {
    RecommendationsLoop callOpenAi(AiPayload payload);

    String getName();
}
