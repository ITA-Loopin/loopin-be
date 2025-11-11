package com.loopone.loopinbe.global.security;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class AuthCookieFactory {
    @Value("${app.cookie.secure}")  // dev=false, prod=true
    private boolean secure;

    @Value("${app.cookie.domain}")  // ".loopin.co.kr"
    private String cookieDomain;

    @Value("${custom.accessToken.expiration}")
    private Duration accessTokenExpiration;

    @Value("${custom.refreshToken.expiration}")
    private Duration refreshTokenExpiration;

    private String sameSite() { return secure ? "None" : "Lax"; }

    private ResponseCookie build(String name, String value, Duration maxAge, String path, boolean httpOnly) {
        ResponseCookie.ResponseCookieBuilder b = ResponseCookie.from(name, value)
                .httpOnly(httpOnly)
                .secure(secure)
                .sameSite(sameSite())
                .path(path)
                .maxAge(maxAge)
                .domain(cookieDomain);
        return b.build();
    }

    // API/WS 공용 Access Token (Path=/)
    public ResponseCookie issueAccess(String accessToken) {
        return build("access_token", accessToken, accessTokenExpiration, "/", true);
    }

    // API/WS 공용 Refresh Token (Path=/)
    public ResponseCookie issueRefresh(String refreshToken) {
        return build("refresh_token", refreshToken, refreshTokenExpiration, "/", true);
    }

    public ResponseCookie expireAccess()  { return build("access_token",  "", Duration.ZERO, "/", true); }
    public ResponseCookie expireRefresh() { return build("refresh_token", "", Duration.ZERO, "/", true); }
}
