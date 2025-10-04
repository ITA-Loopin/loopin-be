package com.loopone.loopinbe.global.webSocket.auth;

import com.loopone.loopinbe.domain.account.auth.security.JwtTokenProvider;
import com.loopone.loopinbe.domain.account.member.repository.MemberRepository;
import com.loopone.loopinbe.domain.chat.chatRoom.repository.ChatRoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
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
    private final JwtTokenProvider jwtTokenProvider;     // 기존 필터에서 사용하던 것과 동일
    private final MemberRepository memberRepository;     // email -> memberId 매핑용
    private final ChatRoomRepository chatRoomRepository;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request,
                                   ServerHttpResponse response,
                                   WebSocketHandler wsHandler,
                                   Map<String, Object> attributes) {

        try {
            // 1) 토큰 추출 (Authorization: Bearer, or ?token=)
            String token = resolveToken(request);
            if (token == null || !jwtTokenProvider.validateToken(token)) {
                log.warn("[WS] invalid or missing token");
                setUnauthorized(response);
                return false;
            }

            // 2) 이메일 파싱 (필요 시 jwtTokenProvider.decodeToken(token) 먼저 호출)
            String email = jwtTokenProvider.getEmailFromToken(token);
            if (email == null) {
                log.warn("[WS] email not found in token");
                setUnauthorized(response);
                return false;
            }

            // 3) memberId 조회 (성능 위해 캐시를 두어도 좋음)
            Long memberId = memberRepository.findIdByEmail(email)
                    .orElse(null);
            if (memberId == null) {
                log.warn("[WS] member not found by email={}", email);
                setUnauthorized(response);
                return false;
            }

            // 4) chatRoomId 파라미터 파싱 & 채팅방 참여 권한 확인
            Long chatRoomId = resolveLongQueryParam(request.getURI(), "chatRoomId");
            if (chatRoomId != null && !chatRoomRepository.existsMember(chatRoomId, memberId)) {
                log.warn("[WS] member={} not in room={}", memberId, chatRoomId);
                setUnauthorized(response);
                return false;
            }

            // 5) 세션 Attribute에 인증 컨텍스트 저장
            attributes.put("memberId", memberId);
            attributes.put("email", email);
            if (chatRoomId != null) attributes.put("chatRoomId", chatRoomId);
            log.info("[WS] handshake authorized. memberId={}, email={}", memberId, email);
            return true;

        } catch (Exception e) {
            log.error("[WS] handshake error: {}", e.getMessage(), e);
            setUnauthorized(response);
            return false;
        }
    }

    @Override
    public void afterHandshake(ServerHttpRequest request,
                               ServerHttpResponse response,
                               WebSocketHandler wsHandler,
                               Exception exception) {
        // no-op
    }

    private static String resolveToken(ServerHttpRequest request) {
        // 1) Authorization: Bearer xxx
        List<String> auths = request.getHeaders().get(HttpHeaders.AUTHORIZATION);
        if (auths != null && !auths.isEmpty()) {
            String v = auths.get(0);
            if (v != null && v.toLowerCase().startsWith("bearer ")) {
                return v.substring(7).trim();
            }
        }
        // 2) ?token=xxx (모바일/웹 소켓 클라이언트 편의)
        String q = request.getURI().getQuery();
        if (q != null) {
            for (String p : q.split("&")) {
                int i = p.indexOf('=');
                if (i > 0) {
                    String k = p.substring(0, i);
                    String val = p.substring(i + 1);
                    if ("token".equals(k)) return val;
                }
            }
        }
        return null;
    }

    private static Long resolveLongQueryParam(URI uri, String key) {
        if (uri.getQuery() == null) return null;
        for (String p : uri.getQuery().split("&")) {
            int i = p.indexOf('=');
            if (i > 0 && key.equals(p.substring(0, i))) {
                try { return Long.parseLong(p.substring(i + 1)); }
                catch (NumberFormatException ignored) {}
            }
        }
        return null;
    }

    private static void setUnauthorized(ServerHttpResponse response) {
        if (response instanceof org.springframework.http.server.ServletServerHttpResponse ssr) {
            ssr.getServletResponse().setStatus(401);
        }
    }
}
