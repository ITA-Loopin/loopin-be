package com.loopone.loopinbe.global.oauth.handler;

import com.loopone.loopinbe.global.oauth.enums.FrontendEnv;
import com.loopone.loopinbe.global.config.properties.FrontendRedirectProperties;
import com.loopone.loopinbe.global.oauth.authorization.HttpCookieOAuth2AuthorizationRequestRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class WebOAuth2FailureHandler implements AuthenticationFailureHandler {
    private final FrontendRedirectProperties frontendRedirectProperties;
    private final HttpCookieOAuth2AuthorizationRequestRepository authRequestRepo;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException, ServletException {
        OAuth2AuthorizationRequest authReq = authRequestRepo.removeAuthorizationRequest(request, response);
        FrontendEnv env = extractEnv(authReq);
        String url = UriComponentsBuilder.fromUriString(frontendRedirectProperties.urlFor(env))
                .queryParam("status", "OAUTH_FAILED")
                .build()
                .toUriString();
        response.setStatus(HttpServletResponse.SC_FOUND);
        response.setHeader("Location", url);
        response.flushBuffer();
    }

    private FrontendEnv extractEnv(OAuth2AuthorizationRequest authReq) {
        if (authReq == null) return FrontendEnv.PROD;
        Object v = authReq.getAttribute("frontend_env");
        if (v == null) return FrontendEnv.PROD;
        try { return FrontendEnv.valueOf(String.valueOf(v)); }
        catch (Exception e) { return FrontendEnv.PROD; }
    }
}
