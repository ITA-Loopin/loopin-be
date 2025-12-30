package com.loopone.loopinbe.domain.loop.ai.dto;

import com.loopone.loopinbe.domain.loop.loop.dto.res.LoopDetailResponse;

import com.loopone.loopinbe.domain.loop.loop.dto.res.LoopDetailResponse;

// AI 요청 페이로드 (필요 최소)
public record AiPayload(
        String clientMessageId,
        Long chatRoomId,
        String userMessageId,
        Long userId,
        String userContent,
        LoopDetailResponse loopDetailResponse,
        java.time.Instant requestedAt
) {}
