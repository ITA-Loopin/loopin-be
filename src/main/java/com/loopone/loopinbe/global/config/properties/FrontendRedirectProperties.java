package com.loopone.loopinbe.global.config.properties;

import com.loopone.loopinbe.domain.account.oauth.enums.FrontendEnv;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "frontend.redirect")
public record FrontendRedirectProperties(
        String local,
        String dev,
        String prod
) {
    public String urlFor(FrontendEnv env) {
        return switch (env) {
            case LOCAL   -> local;
            case DEVELOP -> dev;
            case PROD    -> prod;
        };
    }
}
