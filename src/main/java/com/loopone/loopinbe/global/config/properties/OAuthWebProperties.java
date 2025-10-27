package com.loopone.loopinbe.global.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

@ConfigurationProperties(prefix = "app.oauth")
public record OAuthWebProperties(
        Map<String, ProviderProperties> providers
) {
    public record ProviderProperties(
            String clientId,
            String clientSecret,
            String redirectUri,
            String tokenUri,
            String userInfoUri
    ) {}
}
