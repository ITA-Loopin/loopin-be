package com.loopone.loopinbe.global.webSocket.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@Component
public class WsSessionRegistry {
    // memberId -> sessions
    private final ConcurrentMap<Long, CopyOnWriteArrayList<WebSocketSession>> byMember = new ConcurrentHashMap<>();

    public void add(Long memberId, WebSocketSession s) {
        byMember.computeIfAbsent(memberId, k -> new CopyOnWriteArrayList<>()).add(s);
    }

    public void remove(Long memberId, WebSocketSession s) {
        var list = byMember.get(memberId);
        if (list != null) {
            list.remove(s);
            if (list.isEmpty()) byMember.remove(memberId);
        }
    }

    public void closeAll(Long memberId, CloseStatus status) {
        var list = byMember.remove(memberId);
        if (list == null) return;
        for (WebSocketSession s : list) {
            try {
                s.close(status);
            } catch (Exception e) {
                log.debug("WS close ignore: {}", e.getMessage());
            }
        }
    }

    public int count(Long memberId) {
        var list = byMember.get(memberId);
        return list == null ? 0 : list.size();
    }
}
