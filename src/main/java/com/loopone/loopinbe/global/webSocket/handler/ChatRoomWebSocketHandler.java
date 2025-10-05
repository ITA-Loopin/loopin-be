package com.loopone.loopinbe.global.webSocket.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopone.loopinbe.domain.chat.chatRoom.repository.ChatRoomRepository;
import com.loopone.loopinbe.global.webSocket.payload.ChatWebSocketPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatRoomWebSocketHandler extends TextWebSocketHandler {
    private final Map<Long, List<WebSocketSession>> memberSessions = new ConcurrentHashMap<>();
    private final ChatRoomRepository chatRoomRepository;
    private final ObjectMapper objectMapper;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String query = session.getUri().getQuery(); // e.g., "memberId=1"
        if (query != null && query.startsWith("memberId=")) {
            Long memberId = Long.parseLong(query.split("=")[1]);
            memberSessions.computeIfAbsent(memberId, k -> Collections.synchronizedList(new ArrayList<>())).add(session);
            log.info("ChatRoomWebSocket connected for memberId={}", memberId);
        } else {
            session.close(CloseStatus.BAD_DATA);
            log.warn("WebSocket connection without memberId");
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        memberSessions.values().forEach(list -> list.remove(session));
        log.info("ChatRoomWebSocket disconnected: {}", session.getId());
    }

    public void sendLatestMessageToMember(Long memberId, ChatWebSocketPayload payload) {
        List<WebSocketSession> sessions = memberSessions.getOrDefault(memberId, Collections.emptyList());
        String jsonPayload;
        try {
            jsonPayload = objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize ChatRoomWebSocketPayload", e);
            return;
        }
        for (WebSocketSession session : sessions) {
            try {
                session.sendMessage(new TextMessage(jsonPayload));
            } catch (IOException e) {
                log.error("Failed to send latest message to memberId={}", memberId, e);
            }
        }
    }

    public void sendLatestMessageToAllMembers(Long chatRoomId, ChatWebSocketPayload payload) {
        List<Long> participantIds = chatRoomRepository.findParticipantMemberIds(chatRoomId);
        for (Long memberId : participantIds) {
            sendLatestMessageToMember(memberId, payload);
        }
    }
}
