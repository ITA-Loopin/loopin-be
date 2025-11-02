package com.loopone.loopinbe.domain.account.auth.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;

@Slf4j
@Component
public class JwtTokenProvider {
    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String BEARER_PREFIX = "Bearer ";
    private final SecretKey key;

    // SecretKey 초기화 (한 번만 생성)
    public JwtTokenProvider(@Value("${custom.jwt.secretKey}") String secretKey) {
        byte[] keyBytes = Base64.getDecoder().decode(secretKey);
        this.key = Keys.hmacShaKeyFor(keyBytes);
    }

    // JWT 생성
    public String generateToken(String email, String type, Duration expiration) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(email)
                .claim("tokenType", type)
                .issuedAt(new Date())
                .expiration(Date.from(now.plus(expiration)))
                .signWith(key, Jwts.SIG.HS256)  // 필드 key 사용
                .compact();
    }

    // JWT 디코딩
    public void decodeToken(String token) {
        Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token);
    }

    // ACCESS Token 검증
    public boolean validateAccessToken(String token) {
        return validateToken(token, "ACCESS");
    }

    // REFRESH Token 검증
    public boolean validateRefreshToken(String token) {
        return validateToken(token, "REFRESH");
    }

    // JWT 검증
    private boolean validateToken(String token, String expectedType) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            String tokenType = claims.get("tokenType", String.class);
            if (!expectedType.equals(tokenType)) {
                log.warn("토큰 타입 불일치: expected={}, actual={}", expectedType, tokenType);
                return false;
            }
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("토큰이 만료되었습니다: {}", e.getMessage());
        }
        return false;
    }

    // 이메일 추출
    public String getEmailFromToken(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    // 요청 헤더에서 JWT 추출
    public String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
        if (bearerToken != null && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length());
        }
        return null;
    }
}
