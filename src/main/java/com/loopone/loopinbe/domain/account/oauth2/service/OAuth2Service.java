package com.loopone.loopinbe.domain.account.oauth2.service;

import com.loopone.loopinbe.domain.account.member.dto.SocialUserDto;

public interface OAuth2Service {
    // 소셜 로그인 리디렉션 URL 생성
    String getAuthUrl(String provider);

    // 소셜 유저 정보 조회
    SocialUserDto getUserInfo(String provider, String code);
}
