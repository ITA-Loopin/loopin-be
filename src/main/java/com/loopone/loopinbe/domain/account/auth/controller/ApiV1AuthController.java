package com.loopone.loopinbe.domain.account.auth.controller;

import com.letzgo.LetzgoBe.domain.account.auth.currentUser.CurrentUser;
import com.letzgo.LetzgoBe.domain.account.auth.currentUser.CurrentUserDto;
import com.letzgo.LetzgoBe.domain.account.auth.dto.req.LoginRequest;
import com.letzgo.LetzgoBe.domain.account.auth.dto.res.LoginResponse;
import com.letzgo.LetzgoBe.domain.account.auth.service.AuthService;
import com.letzgo.LetzgoBe.global.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/rest-api/v1/auth")
@RequiredArgsConstructor
public class ApiV1AuthController {
    private final AuthService authService;

    // 로그인
    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@RequestBody @Valid LoginRequest loginRequest) {
        return ApiResponse.success(authService.login(loginRequest, false));
    }

    // 로그아웃
    @PostMapping("/logout")
    public ApiResponse<Void> logout(@CurrentUser CurrentUserDto currentUser) {
        authService.logout(currentUser);
        return ApiResponse.success();
    }

    // accessToken 재발급
    @GetMapping("/refresh-token")
    public ApiResponse<LoginResponse> refreshToken(@RequestHeader("Authorization") String refreshToken, @CurrentUser CurrentUserDto currentUser) {
        return ApiResponse.success(authService.refreshToken(refreshToken, currentUser));
    }
}
