package com.loopone.loopinbe.domain.sse.service;

import com.loopone.loopinbe.domain.chat.chatMessage.entity.type.MessageType;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface SseEmitterService {
    SseEmitter subscribe(Long chatRoomId, String lastEventId);

    void sendToClient(Long chatRoomId, MessageType eventName, Object data);
}
