package com.loopone.loopinbe.domain.loop.ai.serviceImpl;

import com.loopone.loopinbe.domain.loop.ai.dto.AiPayload;
import com.loopone.loopinbe.domain.loop.ai.dto.res.RecommendationsLoop;
import com.loopone.loopinbe.domain.loop.ai.service.LoopAIService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Slf4j
@Service
public class LoopAIServiceImpl implements LoopAIService {
    private final AiRoute aiRoute;
    @Qualifier("openAiExecutor")
    private final Executor executor;

    public LoopAIServiceImpl(
            AiRoute aiRoute,
            @Qualifier("openAiExecutor") Executor executor
    ) {
        this.aiRoute = aiRoute;
        this.executor = executor;
    }

    @Override
    public CompletableFuture<RecommendationsLoop> chat(AiPayload request) {
        log.info("OpenAI 요청 처리 시작: requestId={}", request.clientMessageId());
        return CompletableFuture.supplyAsync(() -> aiRoute.route(request), executor);
    }
}
