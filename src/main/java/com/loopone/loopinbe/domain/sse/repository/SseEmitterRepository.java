package com.loopone.loopinbe.domain.sse.repository;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

public interface SseEmitterRepository {
    SseEmitter save(String emitterId, SseEmitter sseEmitter);

    void saveEventCache(String eventCacheId, Object event);

    void deleteById(String emitterId);

    Map<String, SseEmitter> findAllByChatRoomId(Long chatRoomId);

    Map<String, Object> findAllEventCacheStartWithByChatRoomId(String chatRoomId);

    void deleteAllEventCacheStartWithId(String chatRoomId);
}
