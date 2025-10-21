package com.loopone.loopinbe.domain.loop.ai.serviceImpl;

import com.fasterxml.jackson.databind.JsonNode;
import com.loopone.loopinbe.domain.loop.ai.service.LoopAIService;
import com.loopone.loopinbe.global.exception.ReturnCode;
import com.loopone.loopinbe.global.exception.ServiceException;
import com.loopone.loopinbe.global.kafka.event.ai.AiRequestPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static com.loopone.loopinbe.global.constants.RedisKey.OPEN_AI_RESULT_KEY;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoopAIServiceImpl implements LoopAIService {
    private final StringRedisTemplate stringRedisTemplate;
    private final WebClient openAiWebClient;

    @Override
    public String chat(AiRequestPayload request) {
        log.info("OpenAI 요청 처리 시작: requestId={}", request.requestId());

        String result = openAiWebClient
                .post()
                .uri("/chat/completions")
                .bodyValue(Map.of(
                        "model", "gpt-4o-mini",
                        "messages", List.of(Map.of("role", "user", "content", toPrompt(request.userContent())))
                ))
                .retrieve()
                .onStatus(status -> status.value() == 401,
                        clientResponse -> handleError(clientResponse, ReturnCode.OPEN_AI_UNAUTHORIZED, "OpenAI Unauthorized Error")
                )
                .onStatus(status -> status.value() == 429,
                        clientResponse -> handleError(clientResponse, ReturnCode.OPEN_AI_RATE_LIMIT, "OpenAI Rate Limit Error")
                )
                .onStatus(HttpStatusCode::is5xxServerError,
                        clientResponse -> handleError(clientResponse, ReturnCode.OPEN_AI_INTERNAL_ERROR, "OpenAI Internal Error")
                )
                .bodyToMono(JsonNode.class)
                .map(node -> node.get("choices").get(0).get("message").get("content").asText())
                .block();

        stringRedisTemplate.opsForValue().set(OPEN_AI_RESULT_KEY + request.requestId(), result, Duration.ofMinutes(10));

        log.info("OpenAI 요청 처리 완료 : requestId={}", request.requestId());
        return result;
    }

    private String toPrompt(String message) {
        return message;
    }

    private Mono<? extends Throwable> handleError(ClientResponse response, ReturnCode returnCode, String logPrefix) {
        return response.bodyToMono(String.class)
                .flatMap(errorBody -> {
                    log.error("{} : {}", logPrefix, errorBody);
                    return Mono.error(new ServiceException(returnCode, errorBody));
                });
    }
}
