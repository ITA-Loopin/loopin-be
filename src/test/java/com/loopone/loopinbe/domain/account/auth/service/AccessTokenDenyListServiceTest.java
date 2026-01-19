package com.loopone.loopinbe.domain.account.auth.service;

import com.loopone.loopinbe.domain.account.auth.serviceImpl.AccessTokenDenyListServiceImpl;
import com.loopone.loopinbe.domain.account.member.service.MemberService;
import com.loopone.loopinbe.domain.account.oauth.ticket.service.OAuthTicketService;
import com.loopone.loopinbe.domain.chat.chatRoom.service.ChatRoomService;
import com.loopone.loopinbe.domain.loop.loop.service.LoopService;
import com.loopone.loopinbe.domain.team.team.service.TeamService;
import com.loopone.loopinbe.global.kafka.event.auth.AuthEventPublisher;
import com.loopone.loopinbe.global.kafka.event.notification.NotificationEventPublisher;
import com.loopone.loopinbe.global.redis.config.RedisConfig;
import com.loopone.loopinbe.global.s3.S3Service;
import com.loopone.loopinbe.global.security.JwtTokenProvider;
import com.loopone.loopinbe.global.webSocket.util.WsSessionRegistry;
import com.loopone.loopinbe.support.TestContainersConfig;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.redis.DataRedisTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Duration;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

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
        AccessTokenDenyListServiceImpl.class
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class AccessTokenDenyListServiceTest {
    private static final String KEY_PREFIX = "jwt:deny:";

    // ===== Real (Redis) =====
    @Autowired
    StringRedisTemplate stringRedisTemplate;

    // ===== SUT =====
    @Autowired AccessTokenDenyListService accessTokenDenyListService;

    // ===== External boundaries (mock) =====
    @MockitoBean JwtTokenProvider jwtTokenProvider;
    @MockitoBean RefreshTokenService refreshTokenService;
    @MockitoBean WsSessionRegistry wsSessionRegistry;
    @MockitoBean OAuthTicketService oAuthTicketService;

    // 프로젝트에서 공통으로 물고 있는 외부 의존성들도 형식 통일용으로 mock
    @MockitoBean S3Service s3Service;
    @MockitoBean MemberService memberService;
    @MockitoBean ChatRoomService chatRoomService;
    @MockitoBean TeamService teamService;
    @MockitoBean LoopService loopService;
    @MockitoBean NotificationEventPublisher notificationEventPublisher;
    @MockitoBean AuthEventPublisher authEventPublisher;

    @AfterEach
    void cleanup() {
        // prefix 기반으로 이번 테스트에서 만든 deny 키들 삭제
        Set<String> keys = stringRedisTemplate.keys(KEY_PREFIX + "*");
        if (keys != null && !keys.isEmpty()) {
            stringRedisTemplate.delete(keys);
        }
    }

    // ===== Helpers =====
    private String key(String jti) {
        return KEY_PREFIX + jti;
    }

    // =========================================================
    // deny
    // =========================================================
    @Nested
    class Deny {

        @Test
        @DisplayName("성공: jti + ttl이 유효하면 Redis에 KEY_PREFIX+jti 로 저장되고, TTL이 설정된다")
        void success() {
            // given
            String jti = "jti-123";
            Duration ttl = Duration.ofSeconds(30);

            // when
            accessTokenDenyListService.deny(jti, ttl);

            // then
            assertThat(stringRedisTemplate.hasKey(key(jti))).isTrue();
            String val = stringRedisTemplate.opsForValue().get(key(jti));
            assertThat(val).isEqualTo("1");
            Long expireSeconds = stringRedisTemplate.getExpire(key(jti));
            // Redis 상황에 따라 약간 줄어들 수 있으니 범위로 체크
            assertThat(expireSeconds).isNotNull();
            assertThat(expireSeconds).isGreaterThan(0);
            assertThat(expireSeconds).isLessThanOrEqualTo(ttl.getSeconds());
        }

        @Test
        @DisplayName("무시: jti가 null이면 아무 것도 저장하지 않는다")
        void ignore_nullJti() {
            // when
            accessTokenDenyListService.deny(null, Duration.ofSeconds(10));

            // then
            Set<String> keys = stringRedisTemplate.keys(KEY_PREFIX + "*");
            assertThat(keys == null || keys.isEmpty()).isTrue();
        }

        @Test
        @DisplayName("무시: jti가 blank면 아무 것도 저장하지 않는다")
        void ignore_blankJti() {
            // when
            accessTokenDenyListService.deny("   ", Duration.ofSeconds(10));

            // then
            Set<String> keys = stringRedisTemplate.keys(KEY_PREFIX + "*");
            assertThat(keys == null || keys.isEmpty()).isTrue();
        }

        @Test
        @DisplayName("무시: ttl이 null이면 아무 것도 저장하지 않는다")
        void ignore_nullTtl() {
            // when
            accessTokenDenyListService.deny("jti-123", null);

            // then
            assertThat(stringRedisTemplate.hasKey(key("jti-123"))).isFalse();
        }

        @Test
        @DisplayName("무시: ttl이 0이면 아무 것도 저장하지 않는다")
        void ignore_zeroTtl() {
            // when
            accessTokenDenyListService.deny("jti-123", Duration.ZERO);

            // then
            assertThat(stringRedisTemplate.hasKey(key("jti-123"))).isFalse();
        }

        @Test
        @DisplayName("무시: ttl이 음수면 아무 것도 저장하지 않는다")
        void ignore_negativeTtl() {
            // when
            accessTokenDenyListService.deny("jti-123", Duration.ofSeconds(-1));

            // then
            assertThat(stringRedisTemplate.hasKey(key("jti-123"))).isFalse();
        }
    }

    // =========================================================
    // isDenied
    // =========================================================
    @Nested
    class IsDenied {

        @Test
        @DisplayName("true: deny된 jti는 isDenied가 true를 반환한다")
        void denied_true() {
            // given
            String jti = "jti-777";
            accessTokenDenyListService.deny(jti, Duration.ofMinutes(1));

            // when
            boolean denied = accessTokenDenyListService.isDenied(jti);

            // then
            assertThat(denied).isTrue();
        }

        @Test
        @DisplayName("false: 없는 jti는 isDenied가 false를 반환한다")
        void denied_false_whenNotExists() {
            // when
            boolean denied = accessTokenDenyListService.isDenied("not-exists");

            // then
            assertThat(denied).isFalse();
        }

        @Test
        @DisplayName("false: jti가 null이면 false를 반환한다")
        void denied_false_whenNull() {
            assertThat(accessTokenDenyListService.isDenied(null)).isFalse();
        }

        @Test
        @DisplayName("false: jti가 blank면 false를 반환한다")
        void denied_false_whenBlank() {
            assertThat(accessTokenDenyListService.isDenied("   ")).isFalse();
        }

        @Test
        @DisplayName("TTL 만료 후: 시간이 지나면 키가 사라져 isDenied가 false가 된다")
        void denied_false_afterExpire() throws InterruptedException {
            // given
            String jti = "jti-expire";
            accessTokenDenyListService.deny(jti, Duration.ofSeconds(1));
            assertThat(accessTokenDenyListService.isDenied(jti)).isTrue();

            // when
            Thread.sleep(1200);

            // then
            assertThat(accessTokenDenyListService.isDenied(jti)).isFalse();
        }
    }
}
