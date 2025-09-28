package com.loopone.loopinbe.domain.account.auth.service;

import com.letzgo.LetzgoBe.domain.account.auth.currentUser.CurrentUserDto;
import com.letzgo.LetzgoBe.domain.account.auth.dto.req.LoginRequest;
import com.letzgo.LetzgoBe.domain.account.auth.dto.res.LoginResponse;

public interface AuthService {
    // 로그인
    LoginResponse login(LoginRequest loginRequest, boolean isSocialLogin);

    // 로그아웃
    void logout(CurrentUserDto currentUser);

    // accessToken 재발급
    LoginResponse refreshToken(String refreshToken, CurrentUserDto currentUser);
}
