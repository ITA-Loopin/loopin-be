package com.loopone.loopinbe.domain.chat.chatMessage.repository;

import com.loopone.loopinbe.domain.loop.loop.dto.req.LoopCreateRequest;

import java.util.List;

public interface MessageContentRepositoryCustom {
    void upsert(String id, String content, List<LoopCreateRequest> recommendations);
}
