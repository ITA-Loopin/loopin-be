package com.loopone.loopinbe.domain.account.oauth.service;

import com.loopone.loopinbe.domain.account.member.dto.SocialUserDto;
import com.loopone.loopinbe.domain.account.oauth.dto.res.OAuthRedirectResponse;
import com.loopone.loopinbe.domain.account.oauth.enums.FrontendEnv;
import jakarta.servlet.http.HttpServletRequest;

public interface OAuthService {
    // 소셜 로그인 리디렉션 URL 생성
    String getAuthUrl(String provider, FrontendEnv env);

    // 소셜 유저 정보 조회
    SocialUserDto getUserInfo(String provider, String code);

    // 리디렉션 URL 생성
    OAuthRedirectResponse buildRedirectResponse(SocialUserDto socialUser, FrontendEnv env);

    // OAuth state 토큰 검증
    FrontendEnv resolveEnvFromState(String state);
}
