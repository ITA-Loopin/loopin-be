package com.loopone.loopinbe.domain.account.auth.controller;

import com.loopone.loopinbe.domain.account.auth.currentUser.CurrentUser;
import com.loopone.loopinbe.domain.account.auth.currentUser.CurrentUserDto;
import com.loopone.loopinbe.domain.account.auth.dto.req.LoginRequest;
import com.loopone.loopinbe.domain.account.auth.dto.res.LoginResponse;
import com.loopone.loopinbe.domain.account.auth.service.AuthService;
import com.loopone.loopinbe.domain.account.member.dto.req.MemberCreateRequest;
import com.loopone.loopinbe.global.common.response.ApiResponse;
import com.loopone.loopinbe.global.webSocket.util.WebSocketCookieFactory;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/rest-api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "일반 인증 API")
public class AuthController {
    private final AuthService authService;
    private final WebSocketCookieFactory webSocketCookieFactory;

    // 회원가입 후 로그인 처리
    @Operation(summary = "회원가입 후 로그인 처리", description = "신규 사용자 회원가입 후 로그인 처리합니다.")
    @PostMapping("/signup-login")
    public ResponseEntity<ApiResponse<LoginResponse>> signUpAndLogin(@Valid @RequestBody MemberCreateRequest memberCreateRequest) {
        LoginResponse login = authService.signUpAndLogin(memberCreateRequest);

        // WS 전용 access 쿠키 발급
        ResponseCookie wsCookie = webSocketCookieFactory.createWsAccessCookie(login.getAccessToken());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, wsCookie.toString())
                .body(ApiResponse.success(login));
    }

    // 로그인
    @Operation(summary = "로그인", description = "이메일로 로그인합니다.")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@RequestBody @Valid LoginRequest loginRequest) {
        LoginResponse login = authService.login(loginRequest);

        // WS 전용 access 쿠키 발급
        ResponseCookie wsCookie = webSocketCookieFactory.createWsAccessCookie(login.getAccessToken());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, wsCookie.toString())
                .body(ApiResponse.success(login));
    }

    // 로그아웃
    @Operation(summary = "로그아웃", description = "현재 로그인된 사용자가 로그아웃합니다.")
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@CurrentUser CurrentUserDto currentUser,
                                                    @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader
    ) {
        String accessToken = (authHeader != null && authHeader.startsWith("Bearer "))
                ? authHeader.substring(7) : null;
        authService.logout(currentUser, accessToken);

        // WS 전용 access 쿠키 만료
        ResponseCookie expire = webSocketCookieFactory.expireWsAccessCookie();
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, expire.toString())
                .body(ApiResponse.success());
    }

    // accessToken 재발급
    @Operation(summary = "accessToken 재발급", description = "refresh 토큰을 사용하여 access 토큰을 재발급합니다.")
    @GetMapping("/refresh-token")
    public ResponseEntity<ApiResponse<LoginResponse>> refreshToken(
            @RequestHeader("Authorization-Refresh") String refreshToken,
            @CurrentUser CurrentUserDto currentUser) {
        LoginResponse refreshed = authService.refreshToken(refreshToken, currentUser);

        // 새 access 토큰으로 WS 전용 쿠키 갱신
        ResponseCookie wsCookie = webSocketCookieFactory.createWsAccessCookie(refreshed.getAccessToken());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, wsCookie.toString())
                .body(ApiResponse.success(refreshed));
    }
}
