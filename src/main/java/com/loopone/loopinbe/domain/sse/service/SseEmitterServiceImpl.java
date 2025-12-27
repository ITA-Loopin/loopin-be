package com.loopone.loopinbe.domain.sse.service;

import com.loopone.loopinbe.domain.chat.chatMessage.entity.type.MessageType;
import com.loopone.loopinbe.domain.sse.repository.SseEmitterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class SseEmitterServiceImpl implements SseEmitterService {

    private static final Long DEFAULT_TIMEOUT = 60L * 1000 * 60; // 1시간
    private final SseEmitterRepository sseEmitterRepository;

    @Override
    public SseEmitter subscribe(Long chatRoomId, String lastEventId) {
        String emitterId = makeTimeIncludeId(chatRoomId);
        SseEmitter emitter = sseEmitterRepository.save(emitterId, new SseEmitter(DEFAULT_TIMEOUT));

        emitter.onCompletion(() -> sseEmitterRepository.deleteById(emitterId));
        emitter.onTimeout(() -> sseEmitterRepository.deleteById(emitterId));

        // 503 에러를 방지하기 위한 더미 이벤트 전송
        String eventId = makeTimeIncludeId(chatRoomId);
        sendNotification(emitter, eventId, emitterId, MessageType.CONNECT, "connected!");

        // 클라이언트가 미수신한 Event 목록이 존재할 경우 전송하여 유실 방지
        if (!lastEventId.isEmpty()) {
            Map<String, Object> events = sseEmitterRepository.findAllEventCacheStartWithByChatRoomId(String.valueOf(chatRoomId));
            events.entrySet().stream()
                    .filter(entry -> lastEventId.compareTo(entry.getKey()) < 0)
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(entry -> sendNotification(emitter, entry.getKey(), emitterId, MessageType.MESSAGE, entry.getValue()));
        }

        return emitter;
    }

    @Override
    public void sendToClient(Long chatRoomId, MessageType eventName, Object data) {
        String eventId = makeTimeIncludeId(chatRoomId);
        sseEmitterRepository.saveEventCache(eventId, data); // 데이터 유실 방지를 위해 캐시 저장

        Map<String, SseEmitter> emitters = sseEmitterRepository.findAllByChatRoomId(chatRoomId);
        emitters.forEach((emitterId, emitter) -> {
            sendNotification(emitter, eventId, emitterId, eventName, data);
        });
    }

    private void sendNotification(SseEmitter emitter, String eventId, String emitterId, MessageType eventName, Object data) {
        try {
            emitter.send(SseEmitter.event()
                    .id(eventId)
                    .name(eventName.name())
                    .data(data));
        } catch (IOException e) {
            sseEmitterRepository.deleteById(emitterId);
            log.error("SSE 연결 오류 발생, emitterId 삭제: {}", emitterId);
        }
    }

    private String makeTimeIncludeId(Long chatRoomId) {
        return chatRoomId + "_" + System.currentTimeMillis();
    }
}
