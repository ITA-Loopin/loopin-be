package com.loopone.loopinbe.domain.account.oauth.service;

import com.loopone.loopinbe.domain.account.oauth.enums.FrontendEnv;

import java.time.Duration;

public interface OAuthStateService {
    // OAuth state 토큰 발급
    String issue(FrontendEnv env, Duration ttl);

    // OAuth state 토큰 검증
    FrontendEnv consume(String state);
}
