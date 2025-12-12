package com.loopone.loopinbe.global.webSocket.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopone.loopinbe.domain.chat.chatMessage.dto.ChatMessagePayload;
import com.loopone.loopinbe.domain.chat.chatMessage.entity.ChatMessage;
import com.loopone.loopinbe.global.kafka.event.chatMessage.ChatMessageEventPublisher;
import com.loopone.loopinbe.global.webSocket.payload.ChatWebSocketPayload;
import com.loopone.loopinbe.global.webSocket.util.WsSessionRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import static com.loopone.loopinbe.global.constants.KafkaKey.CREATE_LOOP_TOPIC;
import static com.loopone.loopinbe.global.constants.KafkaKey.UPDATE_LOOP_TOPIC;
import static com.loopone.loopinbe.global.webSocket.payload.ChatWebSocketPayload.MessageType.CREATE_LOOP;
import static com.loopone.loopinbe.global.webSocket.payload.ChatWebSocketPayload.MessageType.UPDATE_LOOP;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatWebSocketHandler extends TextWebSocketHandler {
    private final ChatMessageEventPublisher publisher;
    private final ObjectMapper objectMapper;
    private final Map<Long, CopyOnWriteArrayList<WebSocketSession>> chatRoomSessions = new ConcurrentHashMap<>();
    private final Map<WebSocketSession, Long> sessionRoomMap = new ConcurrentHashMap<>(); // 세션 -> 방 매핑
    private final WsSessionRegistry wsSessionRegistry;
    private final Map<WebSocketSession, Long> sessionMemberMap = new ConcurrentHashMap<>(); // 세션 → 멤버 매핑

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        // 인터셉터가 넣어준 memberId 필수
        Long memberId = (Long) session.getAttributes().get("memberId");
        if (memberId == null) {
            log.warn("WS connected without memberId: {}", session.getId());
            session.close(new CloseStatus(4401, "UNAUTHENTICATED"));
            return;
        }
        // 인터셉터가 넣어준 값 우선 사용
        Long chatRoomId = (Long) session.getAttributes().get("chatRoomId");
        if (chatRoomId == null) {
            // 쿼리에서 보조 파싱 (예: ?chatRoomId=123)
            String q = session.getUri() != null ? session.getUri().getQuery() : null;
            if (q != null) {
                for (String p : q.split("&")) {
                    int i = p.indexOf('=');
                    if (i > 0 && "chatRoomId".equals(p.substring(0, i))) {
                        try {
                            chatRoomId = Long.parseLong(p.substring(i + 1));
                        } catch (NumberFormatException ignored) {
                        }
                    }
                }
            }
        }
        if (chatRoomId == null) {
            log.warn("WebSocket connected without chatRoomId: {}", session.getId());
            session.close(CloseStatus.BAD_DATA);
            return;
        }
        chatRoomSessions
                .computeIfAbsent(chatRoomId, k -> new CopyOnWriteArrayList<>())
                .add(session);
        sessionRoomMap.put(session, chatRoomId);
        sessionMemberMap.put(session, memberId);
        wsSessionRegistry.add(memberId, session);
        log.info("WS connected: {} memberId={} chatRoomId={}", session.getId(), memberId, chatRoomId);
    }

    private Long resolveMemberId(WebSocketSession session) {
        // 레지스트리 매핑에서 즉시 조회
        Long memberId = sessionMemberMap.get(session);
        if (memberId == null)
            throw new IllegalStateException("Unauthenticated WS session");
        return memberId;
    }

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            // 1) JSON 파싱
            ChatWebSocketPayload in = objectMapper.readValue(message.getPayload(), ChatWebSocketPayload.class);

            // 2) 필수 검증 (필요 최소)
            if (in.getMessageType() != CREATE_LOOP && in.getMessageType() != UPDATE_LOOP) {
                sendWsError(session, "INVALID_TYPE", "messageType must be CREATE_LOOP or UPDATE_LOOP");
                return;
            }
            Long roomId = in.getChatRoomId();
            if (roomId == null) {
                sendWsError(session, "INVALID_ROOM", "chatRoomId is required");
                return;
            }
            String content = (in.getChatMessageDto() != null) ? in.getChatMessageDto().getContent() : null;
            if (content == null || content.isBlank()) {
                sendWsError(session, "EMPTY_CONTENT", "content must not be blank");
                return;
            }

            // 3) 인증(세션) 확인
            Long memberId = resolveMemberId(session); // 없으면 IllegalStateException

            ChatMessagePayload payload = new ChatMessagePayload(
                    java.util.UUID.randomUUID().toString(),
                    roomId,
                    memberId,
                    content,
                    null,
                    ChatMessage.AuthorType.USER,
                    java.time.LocalDateTime.now());

            if (in.getMessageType() == CREATE_LOOP) {
                // 4) 퍼블리시 (멱등키 포함)
                publisher.publishChatMessageRequest(payload, CREATE_LOOP_TOPIC);
            } else if (in.getMessageType() == UPDATE_LOOP) {
                // 4) 퍼블리시 (멱등키 포함)
                publisher.publishChatMessageRequest(payload, UPDATE_LOOP_TOPIC);
            }
        } catch (com.fasterxml.jackson.core.JsonProcessingException jpe) {
            // 잘못된 JSON
            log.warn("WS BAD_JSON: {}", jpe.getOriginalMessage());
            sendWsError(session, "BAD_JSON", "Malformed JSON");
        } catch (IllegalStateException unauth) {
            // 미인증 세션
            log.warn("WS UNAUTHENTICATED: {}", unauth.getMessage());
            sendWsErrorAndClose(session, "UNAUTHENTICATED", "Login required");
        } catch (Exception e) {
            // 그 외 모든 예외
            log.error("WS INTERNAL_ERROR", e);
            sendWsError(session, "INTERNAL_ERROR", "Unexpected server error");
        }
    }

    private void sendWsError(WebSocketSession s, String code, String msg) {
        try {
            var err = java.util.Map.of("type", "ERROR", "code", code, "message", msg);
            s.sendMessage(new TextMessage(objectMapper.writeValueAsString(err)));
        } catch (IOException ignore) {
        }
    }

    private void sendWsErrorAndClose(WebSocketSession s, String code, String msg) {
        sendWsError(s, code, msg);
        try {
            s.close(CloseStatus.POLICY_VIOLATION);
        } catch (IOException ignore) {
        }
    }

    // 실시간 메시지 채팅방에 브로드캐스트
    public void broadcastToRoom(Long chatRoomId, String payload) {
        CopyOnWriteArrayList<WebSocketSession> sessions = chatRoomSessions.get(chatRoomId);
        if (sessions == null)
            return;
        for (WebSocketSession s : sessions) {
            try {
                if (s.isOpen()) {
                    s.sendMessage(new TextMessage(payload));
                } else {
                    sessions.remove(s); // COW 리스트라 안전
                }
            } catch (IOException e) {
                log.error("Failed to send message", e);
                sessions.remove(s);
            }
        }
        if (sessions.isEmpty())
            chatRoomSessions.remove(chatRoomId);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Long chatRoomId = sessionRoomMap.remove(session);
        if (chatRoomId != null) {
            var sessions = chatRoomSessions.get(chatRoomId);
            if (sessions != null) {
                sessions.remove(session);
                if (sessions.isEmpty())
                    chatRoomSessions.remove(chatRoomId);
            }
        }
        // 멤버 세션 해제
        Long memberId = sessionMemberMap.remove(session);
        if (memberId != null) {
            wsSessionRegistry.remove(memberId, session);
            log.info("WS disconnected: {} memberId={} room={}", session.getId(), memberId, chatRoomId);
        } else {
            log.info("WS disconnected: {} (no member mapping) room={}", session.getId(), chatRoomId);
        }
    }
}
