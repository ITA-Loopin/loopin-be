package com.loopone.loopinbe.domain.fcm.controller;

import com.loopone.loopinbe.domain.account.auth.currentUser.CurrentUser;
import com.loopone.loopinbe.domain.account.auth.currentUser.CurrentUserDto;
import com.loopone.loopinbe.domain.fcm.dto.req.FcmTokenRequest;
import com.loopone.loopinbe.domain.fcm.service.FcmTokenService;
import com.loopone.loopinbe.global.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/rest-api/v1/fcm")
@RequiredArgsConstructor
public class ApiV1FcmController {
    private final FcmTokenService fcmTokenService;

    // FCM Token 저장
    @PostMapping
    public ApiResponse<Void> saveFcmToken(@RequestBody @Valid FcmTokenRequest fcmTokenRequest, @CurrentUser CurrentUserDto currentUser) {
        fcmTokenService.saveFcmToken(currentUser.getId(), fcmTokenRequest.getFcmToken());
        return ApiResponse.success();
    }

    // FCM Token 삭제
    @DeleteMapping
    public ApiResponse<Void> deleteFcmToken(@CurrentUser CurrentUserDto currentUser) {
        fcmTokenService.deleteFcmToken(currentUser.getId());
        return ApiResponse.success();
    }
}
