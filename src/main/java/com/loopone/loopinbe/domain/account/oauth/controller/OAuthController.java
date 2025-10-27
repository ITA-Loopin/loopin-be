package com.loopone.loopinbe.domain.account.oauth.controller;

import com.loopone.loopinbe.domain.account.member.dto.SocialUserDto;
import com.loopone.loopinbe.domain.account.oauth.enums.FrontendEnv;
import com.loopone.loopinbe.domain.account.oauth.service.OAuthService;
import com.loopone.loopinbe.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/rest-api/v1/oauth")
@RequiredArgsConstructor
@Tag(name = "OAuth", description = "소셜 인증 API")
public class OAuthController {
    private final OAuthService oAuthService;

    // 소셜 로그인 리디렉션 URL
    @Operation(summary = "소셜 로그인 리디렉션 URL", description = "소셜 로그인 리디렉션 URL을 제공합니다.(provider=google|kakao|naver)")
    @GetMapping("/redirect-url/{provider}")
    public ApiResponse<String> redirectToProvider(@PathVariable("provider") String provider, HttpServletRequest request) {
        FrontendEnv env = FrontendEnv.fromRequest(request);
        return ApiResponse.success(oAuthService.getAuthUrl(provider, env));
    }

    // 소셜 로그인 콜백
    @Operation(summary = "소셜 로그인 콜백", description = "소셜인증 제공자가 자동 호출합니다.(완료시 AccessToken, RefreshToken 제공)")
    @GetMapping("/{provider}")
    public ResponseEntity<Void> socialLogin(
            @PathVariable("provider") String provider,
            @RequestParam("code") String code,
            @RequestParam(value = "state") String state) {
        // state 검증 및 소비 → env 복원
        FrontendEnv env = oAuthService.resolveEnvFromState(state);
        // 소셜 유저 정보 조회
        SocialUserDto socialUser = oAuthService.getUserInfo(provider, code);
        // 리디렉션 URL 생성
        String redirectUrl = oAuthService.getRedirectUrl(socialUser, env);
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(redirectUrl))
                .build();
    }
}
