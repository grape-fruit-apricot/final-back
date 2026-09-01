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
import com.kh.midpoint.common.exception.InvalidStateException;

import lombok.RequiredArgsConstructor;

/**
 * STOMP over WebSocket 설정.
 *
 *   연결 : /ws  (SockJS, CONNECT 헤더에 roomUuid / participantId)
 *   구독 : /topic/**
 *   발행 : /app/**
 */
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

	private final ChatService chatService;

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

	/**
	 * CONNECT 프레임을 가로채 방과 참가자를 검증하고, 결과를 세션에 보관한다.
	 *
	 * 연결이 맺어진 뒤에는 이 세션 값만 사용하므로,
	 * 클라이언트가 다른 사람의 participantId 를 실어 보내도 반영되지 않는다.
	 * 검증에 실패하면 예외가 STOMP ERROR 프레임으로 전달되고 연결은 끊긴다.
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
					throw new InvalidStateException("WebSocket 세션이 없습니다.");
				}

				String roomUuid = decode(accessor.getFirstNativeHeader("roomUuid"));
				Long participantId = parseId(accessor.getFirstNativeHeader("participantId"));

				ChatSession session = chatService.openSession(roomUuid, participantId);
				attributes.put(ChatSession.ATTR_KEY, session);

				return message;
			}
		});
	}

	/** 닉네임 등 한글이 섞일 수 있어 프론트에서 인코딩해 보내는 값을 되돌린다 */
	private String decode(String raw) {
		return raw == null ? null : URLDecoder.decode(raw, StandardCharsets.UTF_8);
	}

	private Long parseId(String raw) {
		if (raw == null || raw.isBlank()) {
			return null;
		}
		try {
			return Long.valueOf(raw.trim());
		} catch (NumberFormatException e) {
			throw new InvalidStateException("participantId 형식이 올바르지 않습니다: " + raw);
		}
	}

}
