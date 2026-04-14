package com.loopone.loopinbe.domain.account.oauth.ticket.service;

import com.loopone.loopinbe.domain.account.member.entity.Member;
import com.loopone.loopinbe.domain.account.oauth.ticket.dto.OAuthTicketPayload;
import com.loopone.loopinbe.domain.account.oauth.ticket.serviceImpl.OAuthTicketServiceImpl;
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

import static org.assertj.core.api.Assertions.*;

@Slf4j
@DataRedisTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "testcontainers.redis.enabled=true"
})
@Import({
        TestContainersConfig.class,
        RedisConfig.class,
        OAuthTicketServiceImpl.class
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class OAuthTicketServiceTest {

    private static final String KEY_PREFIX = "oauth:ticket:";

    // ===== Real (Redis) =====
    @Autowired
    StringRedisTemplate stringRedisTemplate;

    // ===== SUT =====
    @Autowired
    OAuthTicketService oAuthTicketService;

    @AfterEach
    void cleanup() {
        Set<String> keys = stringRedisTemplate.keys(KEY_PREFIX + "*");
        if (keys != null && !keys.isEmpty()) {
            stringRedisTemplate.delete(keys);
        }
    }

    // ===== Helpers =====
    private OAuthTicketPayload payload(String email) {
        return new OAuthTicketPayload(email, Member.OAuthProvider.GOOGLE, "pid");
    }

    // =========================================================
    // issue
    // =========================================================
    @Nested
    class Issue {

        @Test
        @DisplayName("성공: 티켓 발급 시 Redis에 저장되고 TTL이 설정된다")
        void success_issueAndTtl() {
            // given
            OAuthTicketPayload payload = payload("jun@loop.in");
            Duration ttl = Duration.ofSeconds(30);

            // when
            String ticket = oAuthTicketService.issue(payload, ttl);

            // then
            assertThat(ticket).isNotBlank();
            String key = KEY_PREFIX + ticket;
            assertThat(stringRedisTemplate.hasKey(key)).isTrue();
            Long expireSeconds = stringRedisTemplate.getExpire(key);
            assertThat(expireSeconds).isNotNull();
            assertThat(expireSeconds).isGreaterThan(0);
            assertThat(expireSeconds).isLessThanOrEqualTo(ttl.getSeconds());
        }
    }

    // =========================================================
    // consume
    // =========================================================
    @Nested
    class Consume {

        @Test
        @DisplayName("성공: 티켓 소비 시 payload 반환 + Redis 키 삭제(1회성)")
        void success_consumeAndDelete() {
            // given
            OAuthTicketPayload payload = payload("jun@loop.in");
            String ticket = oAuthTicketService.issue(payload, Duration.ofSeconds(30));
            String key = KEY_PREFIX + ticket;
            assertThat(stringRedisTemplate.hasKey(key)).isTrue();

            // when
            OAuthTicketPayload found = oAuthTicketService.consume(ticket);

            // then
            assertThat(found.email()).isEqualTo(payload.email());
            assertThat(found.provider()).isEqualTo(payload.provider());
            assertThat(found.providerId()).isEqualTo(payload.providerId());
            assertThat(stringRedisTemplate.hasKey(key)).isFalse();
        }

        @Test
        @DisplayName("실패: 없는 티켓은 예외")
        void fail_whenNotExists() {
            assertThatThrownBy(() -> oAuthTicketService.consume("no-ticket"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("유효하지 않거나 만료된 ticket");
        }
    }
}
