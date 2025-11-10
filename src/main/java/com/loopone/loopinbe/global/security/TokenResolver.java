package com.loopone.loopinbe.global.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class TokenResolver {
    public String resolveAccess(HttpServletRequest req) {
        // 1) 쿠키 우선
        String byCookie = cookie(req, "access_token");
        if (StringUtils.hasText(byCookie)) return byCookie;

        // 2) Authorization: Bearer
        String auth = req.getHeader(HttpHeaders.AUTHORIZATION);
        if (StringUtils.hasText(auth) && auth.startsWith("Bearer ")) {
            return auth.substring(7);
        }
        return null;
    }

    public String resolveRefresh(HttpServletRequest req) {
        // 1) 쿠키 우선
        String byCookie = cookie(req, "refresh_token");
        if (StringUtils.hasText(byCookie)) return byCookie;

        // 2) (하위호환) Authorization-Refresh 헤더
        String h = req.getHeader("Authorization-Refresh");
        return (StringUtils.hasText(h) ? h : null);
    }

    private String cookie(HttpServletRequest req, String name) {
        Cookie[] cookies = req.getCookies();
        if (cookies == null) return null;
        for (Cookie c : cookies) {
            if (name.equals(c.getName())) return c.getValue();
        }
        return null;
    }
}
