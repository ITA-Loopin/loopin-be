package com.loopone.loopinbe.global.kafka.event.ai;

import com.loopone.loopinbe.domain.loop.loop.dto.res.LoopDetailResponse;

// AI 요청 페이로드 (필요 최소)
public record AiRequestPayload(
        String requestId,
        Long chatRoomId,
        Long userMessageId,
        Long userId,
        String userContent,
        LoopDetailResponse loopDetailResponse,
        java.time.Instant requestedAt) {
}
