package com.loopone.loopinbe.domain.fcm.service;

import com.loopone.loopinbe.domain.fcm.serviceImpl.FcmIdempotencyServiceImpl;
import com.loopone.loopinbe.global.redis.config.RedisConfig;
import com.loopone.loopinbe.support.TestContainersConfig;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.redis.DataRedisTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.time.Duration;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@DataRedisTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "testcontainers.redis.enabled=true"
})
@Import({
        TestContainersConfig.class,
        RedisConfig.class,
        FcmIdempotencyServiceImpl.class
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class FcmIdempotencyServiceTest {

    private static final String KEY_PREFIX = "fcm:dedup:";
    private static final Duration PROCESSING_TTL = Duration.ofMinutes(3);
    private static final Duration SENT_TTL = Duration.ofDays(2);

    // ===== Real (Redis) =====
    @Autowired
    StringRedisTemplate stringRedisTemplate;

    // ===== SUT =====
    @Autowired
    FcmIdempotencyService fcmIdempotencyService;

    @AfterEach
    void cleanup() {
        Set<String> keys = stringRedisTemplate.keys(KEY_PREFIX + "*");
        if (keys != null && !keys.isEmpty()) {
            stringRedisTemplate.delete(keys);
        }
    }

    // ===== Helpers =====
    private String key(String eventId) {
        return KEY_PREFIX + eventId;
    }

    // =========================================================
    // tryAcquire
    // =========================================================
    @Nested
    class TryAcquire {

        @Test
        @DisplayName("성공: 첫 tryAcquire는 true, 재호출은 false")
        void success_firstTrue_secondFalse() {
            // given
            String eventId = "evt-1";

            // when
            boolean first = fcmIdempotencyService.tryAcquire(eventId);
            boolean second = fcmIdempotencyService.tryAcquire(eventId);

            // then
            assertThat(first).isTrue();
            assertThat(second).isFalse();
            assertThat(stringRedisTemplate.opsForValue().get(key(eventId))).isEqualTo("PROCESSING");
            Long expireSeconds = stringRedisTemplate.getExpire(key(eventId));
            assertThat(expireSeconds).isNotNull();
            assertThat(expireSeconds).isGreaterThan(0);
            assertThat(expireSeconds).isLessThanOrEqualTo(PROCESSING_TTL.getSeconds());
        }
    }

    // =========================================================
    // markSuccess
    // =========================================================
    @Nested
    class MarkSuccess {

        @Test
        @DisplayName("성공: SENT 상태로 저장되고 TTL이 연장된다")
        void success_markSuccess() {
            // given
            String eventId = "evt-2";
            fcmIdempotencyService.tryAcquire(eventId);

            // when
            fcmIdempotencyService.markSuccess(eventId);

            // then
            assertThat(stringRedisTemplate.opsForValue().get(key(eventId))).isEqualTo("SENT");
            Long expireSeconds = stringRedisTemplate.getExpire(key(eventId));
            assertThat(expireSeconds).isNotNull();
            assertThat(expireSeconds).isGreaterThan(0);
            assertThat(expireSeconds).isLessThanOrEqualTo(SENT_TTL.getSeconds());
        }
    }

    // =========================================================
    // release
    // =========================================================
    @Nested
    class Release {

        @Test
        @DisplayName("성공: release하면 키가 삭제되어 재획득 가능")
        void success_release() {
            // given
            String eventId = "evt-3";
            assertThat(fcmIdempotencyService.tryAcquire(eventId)).isTrue();
            assertThat(stringRedisTemplate.hasKey(key(eventId))).isTrue();

            // when
            fcmIdempotencyService.release(eventId);

            // then
            assertThat(stringRedisTemplate.hasKey(key(eventId))).isFalse();
            assertThat(fcmIdempotencyService.tryAcquire(eventId)).isTrue();
        }
    }
}
