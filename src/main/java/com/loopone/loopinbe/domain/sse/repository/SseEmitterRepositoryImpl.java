package com.loopone.loopinbe.domain.sse.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class SseEmitterRepositoryImpl implements SseEmitterRepository {

    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();
    private final Map<String, Object> eventCache = new ConcurrentHashMap<>();

    @Override
    public SseEmitter save(String emitterId, SseEmitter sseEmitter) {
        emitters.put(emitterId, sseEmitter);
        return sseEmitter;
    }

    @Override
    public void saveEventCache(String eventCacheId, Object event) {
        eventCache.put(eventCacheId, event);
    }

    @Override
    public void deleteById(String emitterId) {
        emitters.remove(emitterId);
    }

    @Override
    public Map<String, SseEmitter> findAllByChatRoomId(Long chatRoomId) {
        return emitters.entrySet().stream()
                .filter(entry -> entry.getKey().startsWith(chatRoomId + "_"))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    @Override
    public Map<String, Object> findAllEventCacheStartWithByChatRoomId(String chatRoomId) {
        return eventCache.entrySet().stream()
                .filter(entry -> entry.getKey().startsWith(chatRoomId + "_"))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    @Override
    public void deleteAllEventCacheStartWithId(String chatRoomId) {
        eventCache.keySet().removeIf(key -> key.startsWith(chatRoomId + "_"));
    }
}
