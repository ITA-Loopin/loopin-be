package com.loopone.loopinbe.domain.loop.ai.serviceImpl;

import static com.loopone.loopinbe.global.constants.Constant.CREATE_LOOP_PROMPT;
import static com.loopone.loopinbe.global.constants.Constant.UPDATE_LOOP_PROMPT;
import static com.loopone.loopinbe.global.constants.RedisKey.OPEN_AI_RESULT_KEY;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopone.loopinbe.domain.loop.ai.dto.AiPayload;
import com.loopone.loopinbe.domain.loop.ai.dto.res.RecommendationsLoop;
import com.loopone.loopinbe.domain.loop.ai.service.LoopAIService;
import com.loopone.loopinbe.domain.loop.loop.dto.res.LoopDetailResponse;
import com.loopone.loopinbe.global.exception.ReturnCode;
import com.loopone.loopinbe.global.exception.ServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Slf4j
@Service
public class LoopAIServiceImpl implements LoopAIService {
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    private final ChatClient chatClient;

    public LoopAIServiceImpl(RedisTemplate<String, Object> redisTemplate,
                             ObjectMapper objectMapper,
                             ChatClient.Builder chatClientBuilder) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.chatClient = chatClientBuilder.build();
    }

    @Override
    public CompletableFuture<RecommendationsLoop> chat(AiPayload request) {
        log.info("OpenAI 요청 처리 시작: requestId={}", request.requestId());
        String prompt;

        if (request.loopDetailResponse() != null) {
            prompt = updatePrompt(request.userContent(), request.loopDetailResponse());
        } else {
            prompt = createPrompt(request.userContent());
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                String result = chatClient.prompt()
                        .user(prompt)
                        .call()
                        .content();

                RecommendationsLoop parsed = parseToRecommendationsLoop(result);

                redisTemplate.opsForValue().set(OPEN_AI_RESULT_KEY + request.requestId(), parsed,
                        Duration.ofMinutes(10));
                log.info("OpenAI 결과 Redis에 캐시 완료 : {}", request.requestId());

                return parsed;

            } catch (Exception e) {
                log.error("OpenAI 호출 중 오류 발생: {}", e.getMessage());
                throw new ServiceException(ReturnCode.OPEN_AI_INTERNAL_ERROR, "OpenAI 호출 실패");
            }
        });
    }

    private RecommendationsLoop parseToRecommendationsLoop(String result) {
        try {
            objectMapper.readTree(result); // JSON 형식 검증
            return objectMapper.readValue(result, RecommendationsLoop.class);
        } catch (JsonProcessingException e) {
            log.warn("GPT 응답이 JSON 형식이 아닙니다. 원문 저장으로 대체합니다: {}", e.getMessage());
            return new RecommendationsLoop(Collections.emptyList());
        }
    }

    private String createPrompt(String message) {
        String today = LocalDate.now().toString();
        return CREATE_LOOP_PROMPT.formatted(today) + message;
    }

    private String updatePrompt(String message, LoopDetailResponse loopDetailResponse) {
        String checklistText = loopDetailResponse.checklists().stream()
                .map(c -> "- " + c.content() + " (완료 여부: " + c.completed() + ")")
                .collect(Collectors.joining("\n"));

        return UPDATE_LOOP_PROMPT.formatted(loopDetailResponse.id(), loopDetailResponse.title(),
                loopDetailResponse.content(), loopDetailResponse.loopDate(), loopDetailResponse.progress(),
                checklistText, formatLoopRule(loopDetailResponse.loopRule()), message);
    }

    private String formatLoopRule(LoopDetailResponse.LoopRuleDTO rule) {
        if (rule == null) {
            return "반복 규칙 없음";
        }

        return String.format("""
                - Rule ID: %d
                - Schedule Type: %s
                - Days of Week: %s
                - Start: %s
                - End: %s
                """,
                rule.ruleId(),
                rule.scheduleType(),
                rule.daysOfWeek(),
                rule.startDate(),
                rule.endDate());
    }
}
