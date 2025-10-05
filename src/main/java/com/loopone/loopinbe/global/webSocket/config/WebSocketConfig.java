package com.loopone.loopinbe.global.webSocket.config;

import com.loopone.loopinbe.global.webSocket.auth.JwtWsHandshakeInterceptor;
import com.loopone.loopinbe.global.webSocket.handler.ChatRoomWebSocketHandler;
import com.loopone.loopinbe.global.webSocket.handler.ChatWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {
    private final ChatWebSocketHandler chatWebSocketHandler;
    private final ChatRoomWebSocketHandler chatRoomWebSocketHandler;
    private final JwtWsHandshakeInterceptor jwtWsHandshakeInterceptor;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(chatWebSocketHandler, "/ws/chat")
                .addInterceptors(jwtWsHandshakeInterceptor)
                .setAllowedOrigins(
                        "http://localhost:8080", "https://www.letzgo.site","https://letzgo.site"
                );

        registry.addHandler(chatRoomWebSocketHandler, "/ws/chat-room")
                .addInterceptors(jwtWsHandshakeInterceptor)
                .setAllowedOrigins(
                        "http://localhost:8080", "https://www.letzgo.site","https://letzgo.site"
                );
    }
}
