package com.loopone.loopinbe.domain.loop.ai.service;

import com.loopone.loopinbe.domain.loop.ai.dto.res.RecommendationsLoop;
import com.loopone.loopinbe.domain.loop.ai.dto.AiPayload;

import java.util.concurrent.CompletableFuture;

public interface LoopAIService {
    CompletableFuture<RecommendationsLoop> chat(AiPayload message);
}
