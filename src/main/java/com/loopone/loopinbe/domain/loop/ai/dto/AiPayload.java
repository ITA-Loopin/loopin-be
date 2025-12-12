package com.loopone.loopinbe.domain.loop.ai.dto;

import com.loopone.loopinbe.domain.loop.loop.dto.res.LoopDetailResponse;

// AI 요청 페이로드 (필요 최소)
public record AiPayload(
        String requestId,
        Long chatRoomId,
        Long userMessageId,
        Long userId,
        String userContent,
        LoopDetailResponse loopDetailResponse,
        java.time.Instant requestedAt
) {}
