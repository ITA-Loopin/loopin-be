package com.loopone.loopinbe.global.webSocket.util;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.net.http.WebSocket;
import java.time.Duration;

@Component
@RequiredArgsConstructor
public class WebSocketCookieFactory {
    @Value("${app.cookie.secure}") // dev=false, prod=true
    private boolean secure;

    @Value("${custom.accessToken.expiration}")
    private Duration accessTokenExpiration;

    private String sameSite() {
        return secure ? "None" : "Lax";
    }

    private ResponseCookie buildCookie(String value, Duration maxAge) {
        return ResponseCookie.from("ws_access", value)
                .httpOnly(true)
                .secure(secure)
                .sameSite(sameSite())
                .path("/ws")
                .maxAge(maxAge)
                .build();
    }

    // 발급
    public ResponseCookie createWsAccessCookie(String accessToken) {
        return buildCookie(accessToken, accessTokenExpiration);
    }

    // 만료
    public ResponseCookie expireWsAccessCookie() {
        return buildCookie("", Duration.ZERO);
    }
}
