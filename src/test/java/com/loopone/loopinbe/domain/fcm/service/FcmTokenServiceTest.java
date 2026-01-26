package com.loopone.loopinbe.domain.fcm.service;

import com.loopone.loopinbe.domain.fcm.serviceImpl.FcmTokenServiceImpl;
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
        "custom.refreshToken.expiration=2s",
        "testcontainers.redis.enabled=true"
})
@Import({
        TestContainersConfig.class,
        RedisConfig.class,
        FcmTokenServiceImpl.class
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class FcmTokenServiceTest {

    private static final String KEY_PREFIX = "fcm:";

    // ===== Real (Redis) =====
    @Autowired
    StringRedisTemplate stringRedisTemplate;

    // ===== SUT =====
    @Autowired
    FcmTokenService fcmTokenService;

    @AfterEach
    void cleanup() {
        Set<String> keys = stringRedisTemplate.keys(KEY_PREFIX + "*");
        if (keys != null && !keys.isEmpty()) {
            stringRedisTemplate.delete(keys);
        }
    }

    // ===== Helpers =====
    private String key(Long memberId) {
        return KEY_PREFIX + memberId;
    }

    // =========================================================
    // saveFcmToken
    // =========================================================
    @Nested
    class SaveFcmToken {

        @Test
        @DisplayName("성공: 저장되고 TTL이 설정된다")
        void success_saveAndTtl() {
            // given
            Long memberId = 1L;
            String token = "fcm-token-123";

            // when
            fcmTokenService.saveFcmToken(memberId, token);

            // then
            assertThat(stringRedisTemplate.hasKey(key(memberId))).isTrue();
            Long expireSeconds = stringRedisTemplate.getExpire(key(memberId));
            assertThat(expireSeconds).isNotNull();
            assertThat(expireSeconds).isGreaterThan(0);
            assertThat(expireSeconds).isLessThanOrEqualTo(Duration.ofSeconds(2).getSeconds());
        }
    }

    // =========================================================
    // getFcmToken
    // =========================================================
    @Nested
    class GetFcmToken {

        @Test
        @DisplayName("성공: 저장된 token은 조회된다")
        void success_get() {
            // given
            Long memberId = 2L;
            String token = "fcm-token-abc";
            fcmTokenService.saveFcmToken(memberId, token);

            // when
            String found = fcmTokenService.getFcmToken(memberId);

            // then
            assertThat(found).isEqualTo(token);
        }

        @Test
        @DisplayName("null: 없는 키는 null을 반환한다")
        void null_whenNotExists() {
            assertThat(fcmTokenService.getFcmToken(999L)).isNull();
        }
    }

    // =========================================================
    // deleteFcmToken
    // =========================================================
    @Nested
    class DeleteFcmToken {

        @Test
        @DisplayName("성공: 삭제하면 키가 제거되어 조회가 null이 된다")
        void success_delete() {
            // given
            Long memberId = 3L;
            fcmTokenService.saveFcmToken(memberId, "fcm-token-del");
            assertThat(stringRedisTemplate.hasKey(key(memberId))).isTrue();

            // when
            fcmTokenService.deleteFcmToken(memberId);

            // then
            assertThat(stringRedisTemplate.hasKey(key(memberId))).isFalse();
            assertThat(fcmTokenService.getFcmToken(memberId)).isNull();
        }
    }
}
