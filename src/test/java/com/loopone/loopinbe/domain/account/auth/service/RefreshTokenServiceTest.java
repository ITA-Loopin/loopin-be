package com.loopone.loopinbe.domain.account.auth.service;

import com.loopone.loopinbe.domain.account.auth.serviceImpl.RefreshTokenServiceImpl;
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
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.time.Duration;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

@Slf4j
@DataRedisTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "custom.accessToken.expiration=30m",
        "custom.refreshToken.expiration=30d",
        "testcontainers.redis.enabled=true"
})
@Import({
        TestContainersConfig.class,
        RedisConfig.class,
        RefreshTokenServiceImpl.class
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class RefreshTokenServiceTest {

    private static final String KEY_PREFIX = "jwt:refresh:test:";

    // ===== Real (Redis) =====
    @Autowired
    StringRedisTemplate stringRedisTemplate;
    @Autowired
    RedisTemplate<String, Object> redisTemplate;

    // ===== SUT =====
    @Autowired RefreshTokenService refreshTokenService;

    @AfterEach
    void cleanup() {
        Set<String> keys = stringRedisTemplate.keys(KEY_PREFIX + "*");
        if (keys != null && !keys.isEmpty()) {
            stringRedisTemplate.delete(keys);
        }
    }

    // ===== Helpers =====
    private String key(String email) {
        return KEY_PREFIX + email;
    }

    // =========================================================
    // saveRefreshToken
    // =========================================================
    @Nested
    class SaveRefreshToken {

        @Test
        @DisplayName("성공: email 키로 refreshToken이 저장되고 TTL이 설정된다")
        void success_saveAndTtl() {
            // given
            String emailKey = key("jun@loop.in");
            String refreshToken = "refresh-token-123";
            Duration ttl = Duration.ofSeconds(30);

            // when
            refreshTokenService.saveRefreshToken(emailKey, refreshToken, ttl);

            // then
            assertThat(stringRedisTemplate.hasKey(emailKey)).isTrue();
            Object stored = redisTemplate.opsForValue().get(emailKey);
            assertThat(stored).isNotNull();
            assertThat(stored.toString()).isEqualTo(refreshToken);
            Long expireSeconds = stringRedisTemplate.getExpire(emailKey);
            assertThat(expireSeconds).isNotNull();
            assertThat(expireSeconds).isGreaterThan(0);
            assertThat(expireSeconds).isLessThanOrEqualTo(ttl.getSeconds());
        }
    }

    // =========================================================
    // getRefreshToken
    // =========================================================
    @Nested
    class GetRefreshToken {

        @Test
        @DisplayName("성공: 저장된 refreshToken은 조회된다")
        void success_get() {
            // given
            String emailKey = key("jun@loop.in");
            String refreshToken = "refresh-token-abc";
            refreshTokenService.saveRefreshToken(emailKey, refreshToken, Duration.ofMinutes(1));

            // when
            String found = refreshTokenService.getRefreshToken(emailKey);

            // then
            assertThat(found).isEqualTo(refreshToken);
        }

        @Test
        @DisplayName("null: 없는 키는 null을 반환한다")
        void null_whenNotExists() {
            // when
            String found = refreshTokenService.getRefreshToken(key("not-exists@loop.in"));

            // then
            assertThat(found).isNull();
        }

        @Test
        @DisplayName("TTL 만료 후: 시간이 지나면 키가 사라져 조회 결과가 null이 된다")
        void null_afterExpire() throws InterruptedException {
            // given
            String emailKey = key("expire@loop.in");
            refreshTokenService.saveRefreshToken(emailKey, "refresh-token-expire", Duration.ofSeconds(1));
            assertThat(refreshTokenService.getRefreshToken(emailKey)).isNotNull();

            // when
            Thread.sleep(1200);

            // then
            assertThat(refreshTokenService.getRefreshToken(emailKey)).isNull();
            assertThat(stringRedisTemplate.hasKey(emailKey)).isFalse();
        }
    }

    // =========================================================
    // deleteRefreshToken
    // =========================================================
    @Nested
    class DeleteRefreshToken {

        @Test
        @DisplayName("성공: 삭제하면 키가 제거되어 조회가 null이 된다")
        void success_delete() {
            // given
            String emailKey = key("delete@loop.in");
            refreshTokenService.saveRefreshToken(emailKey, "refresh-token-del", Duration.ofMinutes(5));
            assertThat(stringRedisTemplate.hasKey(emailKey)).isTrue();

            // when
            refreshTokenService.deleteRefreshToken(emailKey);

            // then
            assertThat(stringRedisTemplate.hasKey(emailKey)).isFalse();
            assertThat(refreshTokenService.getRefreshToken(emailKey)).isNull();
        }

        @Test
        @DisplayName("무해: 없는 키를 삭제해도 예외 없이 동작한다")
        void ok_deleteNotExists() {
            // given
            String emailKey = key("no@loop.in");
            assertThat(stringRedisTemplate.hasKey(emailKey)).isFalse();

            // when / then
            assertThatCode(() -> refreshTokenService.deleteRefreshToken(emailKey)).doesNotThrowAnyException();
        }
    }
}
