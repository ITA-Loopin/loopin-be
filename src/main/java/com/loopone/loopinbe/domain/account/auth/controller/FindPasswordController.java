package com.loopone.loopinbe.domain.account.auth.controller;

import com.loopone.loopinbe.domain.account.auth.serviceImpl.FindPasswordServiceImpl;
import com.loopone.loopinbe.global.common.response.ApiResponse;
import com.loopone.loopinbe.global.exception.ReturnCode;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/find-password")
public class FindPasswordController {
    private final FindPasswordServiceImpl findPasswordService;

    // 이메일 인증코드 전송
    @GetMapping("/send-code/email")
    public ApiResponse<Void> sendCodeToEmail(@RequestParam("email") String email) {
        boolean result = findPasswordService.sendEmailVerificationCode(email);
        if (result) //해당 이메일의 유저 존재하지 않을경우
            return ApiResponse.success();
        return ApiResponse.failure(ReturnCode.USER_NOT_FOUND);
    }

    // 이메일 인증코드 인증
    @GetMapping("/verify-code")
    public ApiResponse<String> verifyCodeByEmail(@RequestParam("email") String email, @RequestParam("code") Integer code) {
        String token = findPasswordService.verifyEmailVerificationCode(email, code);
        if(token.equals("INVALID_CODE")) //인증코드 불일치할 경우
            return ApiResponse.failure(ReturnCode.INVALID_VERIFICATION_CODE);
        return ApiResponse.success(token);
    }

    // 비밀번호 초기화
    @PostMapping("/reset-password")
    public ApiResponse<Void> resetPassword(@RequestParam("email") String email,
                                             @RequestParam("token") String token,
                                             @RequestParam("password") String newPassword) {
        boolean result = findPasswordService.resetPassword(email, token, newPassword);
        if (result) //재설정 토큰 불일치 할경우
            return ApiResponse.success();
        return ApiResponse.failure(ReturnCode.INVALID_RESET_TOKEN);
    }
}
