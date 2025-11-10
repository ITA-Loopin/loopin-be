package com.loopone.loopinbe.domain.account.oauth.controller.web;

import com.loopone.loopinbe.domain.account.member.dto.SocialUserDto;
import com.loopone.loopinbe.domain.account.oauth.dto.res.OAuthRedirectResponse;
import com.loopone.loopinbe.domain.account.oauth.enums.FrontendEnv;
import com.loopone.loopinbe.domain.account.oauth.service.OAuthService;
import com.loopone.loopinbe.global.common.response.ApiResponse;
import com.loopone.loopinbe.global.security.AuthCookieFactory;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/rest-api/v1/oauth")
@RequiredArgsConstructor
@Tag(name = "WebOAuth", description = "웹 소셜 인증 API")
public class WebOAuthController {
    private final OAuthService oAuthService;
    private final AuthCookieFactory authCookieFactory;

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
        // 리디렉션 URL 생성 + 내부 로그인 처리
        OAuthRedirectResponse response = oAuthService.buildRedirectResponse(socialUser, env);
        ResponseEntity.BodyBuilder res = ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(response.getRedirectUrl()));

        // 로그인 성공인 경우에만 쿠키 셋업 (경로: /, HttpOnly)
        if (response.isLoginSuccess()) {
            // buildRedirectResponse에서 내부 로그인 처리 후 토큰 반환한다고 가정
            if (response.getAccessToken() != null) {
                res.header(HttpHeaders.SET_COOKIE, authCookieFactory.issueAccess(response.getAccessToken()).toString());
            }
            if (response.getRefreshToken() != null) {
                res.header(HttpHeaders.SET_COOKIE, authCookieFactory.issueRefresh(response.getRefreshToken()).toString());
            }
        }
        return res.build();
    }
}
