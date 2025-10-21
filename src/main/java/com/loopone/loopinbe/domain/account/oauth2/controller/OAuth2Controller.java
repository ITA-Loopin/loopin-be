package com.loopone.loopinbe.domain.account.oauth2.controller;

import com.loopone.loopinbe.domain.account.member.dto.SocialUserDto;
import com.loopone.loopinbe.domain.account.oauth2.service.OAuth2Service;
import com.loopone.loopinbe.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/rest-api/v1/oauth2")
@RequiredArgsConstructor
@Tag(name = "OAuth2", description = "소셜 인증 API")
public class OAuth2Controller {
    private final OAuth2Service oAuth2Service;

    // 소셜 로그인 리디렉션 URL
    @Operation(summary = "소셜 로그인 리디렉션 URL", description = "소셜 로그인 리디렉션 URL을 제공합니다.(provider=google|kakao|naver)")
    @GetMapping("/redirect-url/{provider}")
    public ApiResponse<String> redirectToProvider(@PathVariable("provider") String provider) {
        return ApiResponse.success(oAuth2Service.getAuthUrl(provider));
    }

    // 소셜 로그인 콜백
    @Operation(summary = "소셜 로그인 콜백", description = "소셜인증 제공자가 자동 호출합니다.(완료시 AccessToken, RefreshToken 제공)")
    @GetMapping("/{provider}")
    public ResponseEntity<Void> socialLogin(
            @PathVariable("provider") String provider,
            @RequestParam("code") String code) {
        // 소셜 유저 정보 조회
        SocialUserDto socialUser = oAuth2Service.getUserInfo(provider, code);
        // 리디렉션 URL 생성
        String redirectUrl = oAuth2Service.getRedirectUrl(socialUser);
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(redirectUrl))
                .build();
    }
}
