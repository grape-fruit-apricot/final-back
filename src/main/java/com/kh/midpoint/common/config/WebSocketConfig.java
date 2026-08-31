package com.kh.midpoint.common.config;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import com.kh.midpoint.chat.model.service.ChatService;
import com.kh.midpoint.chat.model.vo.ChatSession;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final ChatService chatService;

    /**
     * 프론트가 접속할 STOMP 엔드포인트.
     * WebSocket 은 WebConfig 의 CORS 설정이 적용되지 않으므로 여기서 따로 열어준다.
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // 구독(subscribe) : /topic/room/{roomUuid}
        registry.enableSimpleBroker("/topic");
        // 발행(send)      : /app/chat.enter, /app/chat.send
        registry.setApplicationDestinationPrefixes("/app");
    }

    /**
     * CONNECT 시점에 roomUuid / participantId 를 검증하고 세션에 보관한다.
     * 이후 메시지 처리에서는 클라이언트가 보낸 값 대신 이 세션 값을 쓰기 때문에
     * 다른 사람 행세를 하며 메시지를 보낼 수 없다.
     */
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor =
                        MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

                if (accessor == null || !StompCommand.CONNECT.equals(accessor.getCommand())) {
                    return message;
                }

                Map<String, Object> attributes = accessor.getSessionAttributes();
                if (attributes == null) {
                    throw new IllegalStateException("WebSocket 세션이 없습니다.");
                }

                String roomUuid = decode(accessor.getFirstNativeHeader("roomUuid"));
                Long participantId = parseId(accessor.getFirstNativeHeader("participantId"));

                // 검증 실패 시 예외를 던지면 클라이언트의 onStompError 로 전달된다
                ChatSession session = chatService.openSession(roomUuid, participantId);
                attributes.put(ChatSession.ATTR_KEY, session);

                return message;
            }
        });
    }

    private String decode(String raw) {
        return raw == null ? null : URLDecoder.decode(raw, StandardCharsets.UTF_8);
    }

    private Long parseId(String raw) {
        try {
            return raw == null ? null : Long.valueOf(raw.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("participantId 형식이 올바르지 않습니다: " + raw);
        }
    }
}
