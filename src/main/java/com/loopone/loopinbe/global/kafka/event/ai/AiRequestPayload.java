package com.loopone.loopinbe.global.kafka.event.ai;

// AI 요청 페이로드 (필요 최소)
public record AiRequestPayload(
        String requestId,
        Long chatRoomId,
        Long userMessageId,
        Long userId,
        String userContent,
        java.time.Instant requestedAt
) {}
