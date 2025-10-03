package com.loopone.loopinbe.domain.loop.ai.serviceImpl;

import com.loopone.loopinbe.domain.loop.ai.service.LoopAIService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import static com.loopone.loopinbe.global.constants.RedisKey.OPEN_AI_RESULT_KEY;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoopAIServiceImpl implements LoopAIService {
    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public String getAIResult(String requestId) {
        return stringRedisTemplate.opsForValue().get(OPEN_AI_RESULT_KEY + requestId);
    }
}
