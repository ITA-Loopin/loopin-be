package com.loopone.loopinbe.global.webSocket.auth;

import com.loopone.loopinbe.domain.account.auth.security.JwtTokenProvider;
import com.loopone.loopinbe.domain.account.member.repository.MemberRepository;
import com.loopone.loopinbe.domain.chat.chatRoom.repository.ChatRoomRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.net.URI;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtWsHandshakeInterceptor implements HandshakeInterceptor {
    private final JwtTokenProvider jwtTokenProvider; // 기존 사용
    private final MemberRepository memberRepository;
    private final ChatRoomRepository chatRoomRepository;

    @Override
    public boolean beforeHandshake(ServerHttpRequest req, ServerHttpResponse res,
                                   WebSocketHandler wsHandler, Map<String, Object> attrs) {

        try {
            HttpServletRequest servletReq = (req instanceof ServletServerHttpRequest s)
                    ? s.getServletRequest() : null;

            // 0) Origin 체크 (브라우저만)
            String origin = servletReq != null ? servletReq.getHeader("Origin") : null;
            if (!isAllowedOrigin(origin)) {
                log.warn("[WS] Forbidden origin: {}", origin);
                setStatus(res, HttpStatus.FORBIDDEN);
                return false;
            }

            // 1) 토큰 추출: Cookie -> Authorization -> ?token
            String token = extractFromCookie(servletReq, "access_token");
            if (token == null || !jwtTokenProvider.validateAccessToken(token)) {
                log.warn("[WS] invalid/missing token");
                setStatus(res, HttpStatus.UNAUTHORIZED);
                return false;
            }

            // 2) 이메일/subject 파싱 → memberId 조회
            String email = jwtTokenProvider.getEmailFromToken(token);
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

            // 3) chatRoomId 파라미터 & 참여 권한 확인
            Long chatRoomId = resolveLongQueryParam(req.getURI(), "chatRoomId");
            if (chatRoomId != null && !chatRoomRepository.existsMember(chatRoomId, memberId)) {
                log.warn("[WS] member={} not in room={}", memberId, chatRoomId);
                setStatus(res, HttpStatus.FORBIDDEN);
                return false;
            }

            // 4) 세션 Attribute 저장
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
                               WebSocketHandler wsHandler, Exception ex) { }

    // --- helpers ---

    private boolean isAllowedOrigin(String origin) {
        if (origin == null) return true; // 네이티브 클라 허용
        return switch (origin) {
            case "https://loopin.co.kr",
                 "https://develop.loopin.co.kr",
                 "http://local.loopin.co.kr",
                 "http://localhost:3000",
                 "http://localhost:8080" -> true;
            default -> false;
        };
    }

    private static void setStatus(ServerHttpResponse res, HttpStatus status) {
        res.setStatusCode(status);
        if (res instanceof org.springframework.http.server.ServletServerHttpResponse ssr) {
            ssr.getServletResponse().setStatus(status.value());
        }
    }

    private static String extractFromCookie(HttpServletRequest req, String name) {
        if (req == null || req.getCookies() == null) return null;
        for (Cookie c : req.getCookies()) if (name.equals(c.getName())) return c.getValue();
        return null;
    }

    private static Long resolveLongQueryParam(URI uri, String key) {
        String q = uri.getQuery();
        if (q == null) return null;
        for (String p : q.split("&")) {
            int i = p.indexOf('=');
            if (i > 0 && key.equals(p.substring(0, i))) {
                try { return Long.parseLong(p.substring(i + 1)); }
                catch (NumberFormatException ignored) {}
            }
        }
        return null;
    }
}
