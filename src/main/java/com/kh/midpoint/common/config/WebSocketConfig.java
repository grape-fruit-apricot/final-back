package com.kh.midpoint.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * STOMP over WebSocket 기본 설정.
 *
 *   연결   : /ws  (SockJS)
 *   구독   : /topic/**
 *   발행   : /app/**
 *
 * 이 단계에서는 연결과 브로커 설정까지만 다룬다.
 * 방 단위 세션 관리와 메시지 처리는 다음 PR에서 추가한다.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    /**
     * WebSocket 핸드셰이크는 WebMvc 의 CORS 설정을 타지 않으므로
     * 허용 오리진을 여기서 별도로 지정한다.
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // 서버 -> 클라이언트 (구독 경로)
        registry.enableSimpleBroker("/topic");
        // 클라이언트 -> 서버 (@MessageMapping 경로 접두사)
        registry.setApplicationDestinationPrefixes("/app");
    }
}
