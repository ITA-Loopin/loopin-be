package com.loopone.loopinbe.domain.account.oauth2.dto;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

@ConfigurationProperties(prefix = "app.oauth2")
public record OAuth2WebPropertiesDto(
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
