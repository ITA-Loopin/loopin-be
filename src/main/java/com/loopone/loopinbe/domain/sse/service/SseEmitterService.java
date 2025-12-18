package com.loopone.loopinbe.domain.sse.service;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface SseEmitterService {
    SseEmitter subscribe(Long chatRoomId, String lastEventId);

    void sendToClient(Long chatRoomId, String eventName, Object data);
}
