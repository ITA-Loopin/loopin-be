package com.loopone.loopinbe.global.oauth.authorization;

import com.loopone.loopinbe.global.oauth.enums.FrontendEnv;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CustomOAuth2AuthorizationRequestResolver implements OAuth2AuthorizationRequestResolver {
    private final ClientRegistrationRepository clientRegistrationRepository;

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request) {
        return customize(delegate().resolve(request), request);
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request, String clientRegistrationId) {
        return customize(delegate().resolve(request, clientRegistrationId), request);
    }

    private DefaultOAuth2AuthorizationRequestResolver delegate() {
        // 기본 엔드포인트: /oauth2/authorization/{registrationId}
        return new DefaultOAuth2AuthorizationRequestResolver(clientRegistrationRepository, "/oauth2/authorization");
    }

    private OAuth2AuthorizationRequest customize(OAuth2AuthorizationRequest req, HttpServletRequest request) {
        if (req == null) return null;

        FrontendEnv env = FrontendEnv.fromRequest(request);
        return OAuth2AuthorizationRequest.from(req)
                .attributes(attrs -> attrs.put("frontend_env", env.name()))
                .build();
    }
}
