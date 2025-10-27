package com.loopone.loopinbe.domain.account.oauth.serviceImpl;

import com.loopone.loopinbe.domain.account.oauth.enums.FrontendEnv;
import com.loopone.loopinbe.domain.account.oauth.service.OAuthStateService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OAuthStateServiceImpl implements OAuthStateService {
    private final StringRedisTemplate redis;
    @Value("${oauth.state.secret}")
    private String secret; // 최소 256bit 이상 권장

    private SecretKey key() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    // OAuth state 토큰 발급
    @Override
    public String issue(FrontendEnv env, Duration ttl) {
        String jti = UUID.randomUUID().toString();
        Instant now = Instant.now();

        String token = Jwts.builder()
                .id(jti)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(ttl)))
                .claim("env", env.name())
                .signWith(key(), Jwts.SIG.HS256)
                .compact();

        // 재사용 방지용 마커 (5분)
        redis.opsForValue().set("oauth:state:" + jti, "1", ttl);
        return token;
    }

    // OAuth state 토큰 검증
    @Override
    public FrontendEnv consume(String state) {
        try {
            Jws<Claims> jws = Jwts.parser().verifyWith(key()).build().parseSignedClaims(state);
            Claims c = jws.getPayload();
            String jti = c.getId();
            String redisKey = "oauth:state:" + jti;

            // Redis 존재 확인
            Boolean exists = redis.hasKey(redisKey);
            if (Boolean.FALSE.equals(exists)) {
                log.warn("OAuth state 검증 실패 - Redis key 없음 (만료 또는 재사용): stateId={}, env={}", jti, c.get("env"));
                throw new IllegalArgumentException("Invalid or reused state");
            }

            // 삭제 시도
            Long deletedCount = redis.delete(redisKey) ? 1L : 0L;
            if (deletedCount == 0) {
                log.warn("OAuth state 재사용 감지: stateId={}", jti);
                throw new IllegalArgumentException("Invalid or reused state");
            }

            String env = c.get("env", String.class);
            return FrontendEnv.valueOf(env);

        } catch (JwtException e) {
            log.error("OAuth state JWT 파싱 실패: {}", e.getMessage());
            throw new IllegalArgumentException("Invalid state token", e);
        }
    }
}
