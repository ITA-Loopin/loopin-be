package com.loopone.loopinbe.domain.chat.chatMessage.repository;

public interface MessageContentRepositoryCustom {
    void upsert(String id, String content);
}
