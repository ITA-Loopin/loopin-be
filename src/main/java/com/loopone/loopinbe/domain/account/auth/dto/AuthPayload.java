package com.loopone.loopinbe.domain.account.auth.dto;

public record AuthPayload(
        String requestId,
        Long memberId,
        String accessToken
) {}
