package com.kh.midpoint.common.config;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import jakarta.annotation.PreDestroy;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import com.kh.midpoint.chat.model.service.ChatService;
import com.kh.midpoint.chat.model.vo.ChatSession;
import com.kh.midpoint.common.exception.InvalidStateException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

	@Value("${chat.heartbeat-interval}")
	private long heartbeatInterval;

	private final ThreadPoolTaskScheduler heartbeatScheduler = createHeartbeatScheduler();

	private final ChatService chatService;

	@Override
	public void registerStompEndpoints(StompEndpointRegistry registry) {
		registry.addEndpoint("/ws")
				.setAllowedOriginPatterns("*")
				.withSockJS();
	}

	@Override
	public void configureMessageBroker(MessageBrokerRegistry registry) {
		registry.enableSimpleBroker("/topic")
				.setHeartbeatValue(new long[] { heartbeatInterval, heartbeatInterval })
				.setTaskScheduler(heartbeatScheduler);
		registry.setApplicationDestinationPrefixes("/app");
	}

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

				try {
					String roomUuid = decode(accessor.getFirstNativeHeader("roomUuid"));
					Long participantId = parseId(accessor.getFirstNativeHeader("participantId"));

					ChatSession session = chatService.openSession(roomUuid, participantId);
					attributes.put(ChatSession.ATTR_KEY, session);

					return message;

				} catch (Exception e) {
					log.warn("STOMP CONNECT 거부 - {}", e.toString());
					throw new MessageDeliveryException(message, e.getMessage(), e);
				}
			}
		});
	}

	private static ThreadPoolTaskScheduler createHeartbeatScheduler() {
		ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
		scheduler.setPoolSize(1);
		scheduler.setThreadNamePrefix("ws-heartbeat-");
		scheduler.initialize();
		return scheduler;
	}

	@PreDestroy
	public void shutdownHeartbeatScheduler() {
		heartbeatScheduler.shutdown();
	}

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
