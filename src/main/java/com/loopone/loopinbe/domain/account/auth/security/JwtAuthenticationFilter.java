package com.loopone.loopinbe.domain.account.auth.security;

import com.loopone.loopinbe.domain.account.auth.service.RefreshTokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtTokenProvider jwtTokenProvider;
    private final UserDetailsService userDetailsService;
    private final RefreshTokenService refreshTokenService;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();
        String method = request.getMethod();

        // 인증이 필요 없는 URL 리스트
        return path.startsWith("/rest-api/v1/auth/signup-login")
                || path.startsWith("/rest-api/v1/auth/login")
                || path.startsWith("/rest-api/v1/member/available")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/rest-api/v1/oauth")
                || path.startsWith("/rest-api/v1/find-password")
                || path.startsWith("/api/v1/health-check");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String token = jwtTokenProvider.resolveToken(request);

        // JWT가 없을 경우
        if (!StringUtils.hasText(token)) {
            log.warn("JWT 토큰이 없습니다.");
            responseUnauthorized(response, "인증 실패");
            return;
        }
        try {
            // 토큰 유효성 검사
            if (!jwtTokenProvider.validateAccessToken(token)) {
                log.warn("유효하지 않은 Access Token");
                responseUnauthorized(response, "유효하지 않은 Access Token 입니다.");
                return;
            }
            // 이메일 추출
            String email = jwtTokenProvider.getEmailFromToken(token);
            log.info("정상적으로 사용자 정보를 토큰으로부터 가져왔습니다. Email: {}", email);
            UserDetails userDetails = userDetailsService.loadUserByUsername(email);

            // JWT 기반 인증 객체 생성 (비밀번호 없이)
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            // SecurityContextHolder에 직접 세팅
            SecurityContextHolder.getContext().setAuthentication(authentication);

        } catch (Exception e) {
            log.error("JWT 인증 처리 실패: {}", e.getMessage(), e);
            SecurityContextHolder.clearContext();
            try {
                String email = jwtTokenProvider.getEmailFromToken(token);
                refreshTokenService.deleteRefreshToken(email);
            } catch (Exception ignored) {
            }
        }
        filterChain.doFilter(request, response);
    }

    private void responseUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write("{\"message\": \"" + message + "\"}");
    }
}
