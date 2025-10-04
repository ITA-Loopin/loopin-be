package com.loopone.loopinbe.domain.account.oauth2.controller;

import com.loopone.loopinbe.domain.account.auth.dto.req.LoginRequest;
import com.loopone.loopinbe.domain.account.auth.dto.res.LoginResponse;
import com.loopone.loopinbe.domain.account.auth.service.AuthService;
import com.loopone.loopinbe.domain.account.member.dto.SocialUserDto;
import com.loopone.loopinbe.domain.account.member.dto.req.MemberCreateRequest;
import com.loopone.loopinbe.domain.account.member.entity.Member;
import com.loopone.loopinbe.domain.account.member.repository.MemberRepository;
import com.loopone.loopinbe.domain.account.member.service.MemberService;
import com.loopone.loopinbe.domain.account.oauth2.service.OAuth2Service;
import com.loopone.loopinbe.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/rest-api/v1/oauth2")
@RequiredArgsConstructor
@Tag(name = "OAuth2", description = "소셜인증 API")
public class OAuth2Controller {
    private final AuthService authService;
    private final OAuth2Service oAuth2Service;
    private final MemberService memberService;
    private final MemberRepository memberRepository;

    @Value("${frontend.oauth-redirect}")
    private String frontendRedirect;

    // 소셜 로그인 리디렉션 URL
    @Operation(summary = "소셜 로그인 리디렉션 URL", description = "소셜 로그인 리디렉션 URL을 제공합니다.")
    @GetMapping("/redirect-url/{provider}")
    public ApiResponse<String> redirectToProvider(@PathVariable("provider") String provider) {
        String authUrl = oAuth2Service.getAuthUrl(provider);
        return ApiResponse.success(authUrl);
    }

    // 소셜 로그인 콜백
    @Operation(summary = "소셜 로그인 콜백", description = "소셜인증 제공자가 자동 호출합니다.(완료시 AccessToken, RefreshToken 제공)")
    @GetMapping("/{provider}")
    public ResponseEntity<Void> socialLogin(
            @PathVariable("provider") String provider,
            @RequestParam("code") String code) {
        // 소셜 유저 정보 조회
        SocialUserDto socialUser = oAuth2Service.getUserInfo(provider, code);
        String email = socialUser.email();

        // 로그인 또는 회원가입 처리
        LoginRequest socialLoginRequest;
        if (memberRepository.existsByEmail(email)) {
            socialLoginRequest = LoginRequest.builder().email(email).build();
        } else {
            MemberCreateRequest memberCreateRequest = MemberCreateRequest.builder()
                    .email(email)
                    .provider(socialUser.provider())
                    .providerId(socialUser.providerId())
                    .build();
            Member newMember = memberService.socialSignUp(memberCreateRequest);
            socialLoginRequest = LoginRequest.builder().email(newMember.getEmail()).build();
        }
        LoginResponse loginResponse = authService.login(socialLoginRequest, true);
        String accessToken = loginResponse.getAccessToken();
        String refreshToken = loginResponse.getRefreshToken();

        // 프론트엔드 리디렉션 주소에 토큰을 쿼리로 포함
        String redirectUrl = UriComponentsBuilder.fromUriString(frontendRedirect)
                .queryParam("accessToken", accessToken)
                .queryParam("refreshToken", refreshToken)
                .build()
                .toUriString();
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(redirectUrl))
                .build();
    }
}
