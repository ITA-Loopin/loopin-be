package com.loopone.loopinbe.domain.fcm.controller;

import com.loopone.loopinbe.domain.account.auth.currentUser.CurrentUser;
import com.loopone.loopinbe.domain.account.auth.currentUser.CurrentUserDto;
import com.loopone.loopinbe.domain.fcm.dto.req.FcmTokenRequest;
import com.loopone.loopinbe.domain.fcm.service.FcmTokenService;
import com.loopone.loopinbe.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/rest-api/v1/fcm")
@RequiredArgsConstructor
@Tag(name = "FCM", description = "FCM API")
public class FcmController {
    private final FcmTokenService fcmTokenService;

    // FCM Token 저장
    @PostMapping
    @Operation(summary = "FCM Token 저장", description = "FCM Token을 저장합니다.")
    public ApiResponse<Void> saveFcmToken(@RequestBody @Valid FcmTokenRequest fcmTokenRequest, @CurrentUser CurrentUserDto currentUser) {
        fcmTokenService.saveFcmToken(currentUser.id(), fcmTokenRequest.fcmToken());
        return ApiResponse.success();
    }

    // FCM Token 삭제
    @DeleteMapping
    @Operation(summary = "FCM Token 삭제", description = "FCM Token을 삭제합니다.")
    public ApiResponse<Void> deleteFcmToken(@CurrentUser CurrentUserDto currentUser) {
        fcmTokenService.deleteFcmToken(currentUser.id());
        return ApiResponse.success();
    }
}
