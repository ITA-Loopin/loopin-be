//package com.loopone.loopinbe.global.webSocket.config;
//
//import com.loopone.loopinbe.global.webSocket.auth.JwtWsHandshakeInterceptor;
//import com.loopone.loopinbe.global.webSocket.handler.ChatWebSocketHandler;
//import lombok.RequiredArgsConstructor;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.web.socket.config.annotation.EnableWebSocket;
//import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
//import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
//
//@Configuration
//@EnableWebSocket
//@RequiredArgsConstructor
//public class WebSocketConfig implements WebSocketConfigurer {
//    private final ChatWebSocketHandler chatWebSocketHandler;
//    private final JwtWsHandshakeInterceptor jwtWsHandshakeInterceptor;
//
//    @Override
//    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
//        registry.addHandler(chatWebSocketHandler, "/ws/chat")
//                .addInterceptors(jwtWsHandshakeInterceptor)
//                .setAllowedOrigins(
//                        "http://localhost:8080",
//                        "http://localhost:3000",
//                        "http://local.loopin.co.kr",
//                        "https://local.loopin.co.kr",
//                        "https://loopin.co.kr",
//                        "https://develop.loopin.co.kr"
//                );
//    }
//}
