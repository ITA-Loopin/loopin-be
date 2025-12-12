package com.loopone.loopinbe.global.webSocket.auth;

import com.loopone.loopinbe.global.security.JwtTokenProvider;
import com.loopone.loopinbe.domain.account.auth.service.AccessTokenDenyListService;
import com.loopone.loopinbe.domain.account.member.repository.MemberRepository;
import com.loopone.loopinbe.domain.chat.chatRoom.repository.ChatRoomRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.net.URI;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtWsHandshakeInterceptor implements HandshakeInterceptor {
    private final JwtTokenProvider jwtTokenProvider;
    private final MemberRepository memberRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final AccessTokenDenyListService accessTokenDenyListService;

    @Override
    public boolean beforeHandshake(ServerHttpRequest req, ServerHttpResponse res,
                                   WebSocketHandler wsHandler, Map<String, Object> attrs) {
        try {
            HttpServletRequest servletReq = (req instanceof ServletServerHttpRequest s)
                    ? s.getServletRequest() : null;

            // 1) 토큰 추출: Cookie only
            String accessToken = resolveAccessTokenFromCookieOnly(servletReq);
            if (!hasText(accessToken) || !jwtTokenProvider.validateAccessToken(accessToken)) {
                log.warn("[WS] invalid/missing token (cookie only)");
                setStatus(res, HttpStatus.UNAUTHORIZED);
                return false;
            }

            // 2) deny-list (즉시 무효화) 체크 ← 추가 포인트
            String jti = null;
            try { jti = jwtTokenProvider.getJti(accessToken); } catch (Exception ignore) {}
            if (jti != null && accessTokenDenyListService.isDenied(jti)) {
                log.info("[WS] denied by logout. jti={}", jti);
                setStatus(res, HttpStatus.UNAUTHORIZED);
                return false;
            }

            // 3) 이메일/subject 파싱 → memberId 조회
            String email = jwtTokenProvider.getEmailFromToken(accessToken);
            if (email == null) {
                log.warn("[WS] email not found in token");
                setStatus(res, HttpStatus.UNAUTHORIZED);
                return false;
            }
            Long memberId = memberRepository.findIdByEmail(email).orElse(null);
            if (memberId == null) {
                log.warn("[WS] member not found by email={}", email);
                setStatus(res, HttpStatus.UNAUTHORIZED);
                return false;
            }

            // 4) chatRoomId 파라미터 & 참여 권한 확인
            Long chatRoomId = resolveLongQueryParam(req.getURI(), "chatRoomId");
            if (chatRoomId != null && !chatRoomRepository.existsMember(chatRoomId, memberId)) {
                log.warn("[WS] member={} not in room={}", memberId, chatRoomId);
                setStatus(res, HttpStatus.FORBIDDEN);
                return false;
            }

            // 5) 세션 Attribute 저장
            attrs.put("memberId", memberId);
            attrs.put("email", email);
            if (chatRoomId != null) attrs.put("chatRoomId", chatRoomId);

            log.info("[WS] handshake authorized. memberId={}, email={}", memberId, email);
            return true;
        } catch (Exception e) {
            log.error("[WS] handshake error: {}", e.getMessage(), e);
            setStatus(res, HttpStatus.UNAUTHORIZED);
            return false;
        }
    }

    @Override
    public void afterHandshake(ServerHttpRequest req, ServerHttpResponse res,
                               WebSocketHandler wsHandler, Exception ex) {}

    // ----------------- 헬퍼 메서드 -----------------

    private static void setStatus(ServerHttpResponse res, HttpStatus status) {
        res.setStatusCode(status);
        if (res instanceof org.springframework.http.server.ServletServerHttpResponse ssr) {
            ssr.getServletResponse().setStatus(status.value());
        }
    }

    private String resolveAccessTokenFromCookieOnly(HttpServletRequest servletReq) {
        return extractFromCookie(servletReq, "access_token");
    }

    private static String extractFromCookie(HttpServletRequest req, String name) {
        if (req == null || req.getCookies() == null) return null;
        for (Cookie c : req.getCookies()) {
            if (name.equals(c.getName())) return c.getValue();
        }
        return null;
    }

    private static Long resolveLongQueryParam(URI uri, String key) {
        String v = resolveQueryParam(uri, key);
        if (v == null) return null;
        try { return Long.parseLong(v); }
        catch (NumberFormatException ignored) { return null; }
    }

    private static String resolveQueryParam(URI uri, String key) {
        String q = (uri != null ? uri.getQuery() : null);
        if (q == null) return null;
        for (String p : q.split("&")) {
            int i = p.indexOf('=');
            if (i > 0 && key.equals(p.substring(0, i))) {
                return urlDecodeSafe(p.substring(i + 1));
            }
        }
        return null;
    }

    private static boolean hasText(String s) {
        return s != null && !s.isBlank();
    }

    private static String urlDecodeSafe(String s) {
        try { return java.net.URLDecoder.decode(s, java.nio.charset.StandardCharsets.UTF_8); }
        catch (Exception ignored) { return s; }
    }
}
