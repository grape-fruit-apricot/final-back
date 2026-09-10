package com.kh.midpoint.tracking.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import com.kh.midpoint.chat.model.vo.ChatSession;
import com.kh.midpoint.common.response.SocketErrorResponseDto;
import com.kh.midpoint.participant.model.service.ParticipantService;
import com.kh.midpoint.tracking.model.dto.TrackingResponseDto;
import com.kh.midpoint.tracking.model.service.TrackingService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// 방장이 이동 추적을 여는 트리거. 방장 여부는 클라이언트가 보낸 값이 아니라
// 연결 시 세션에 저장해 둔 participantId 로 확인한다.
@Slf4j
@Controller
@RequiredArgsConstructor
public class TrackingSocketController {

	@Value("${chat.session-attribute-key}")
	private String sessionAttributeKey;

	private final TrackingService trackingService;
	private final ParticipantService participantService;
	private final SimpMessagingTemplate messagingTemplate;

	@MessageMapping("/tracking/start")
	public void insertTrackingSessionList(SimpMessageHeaderAccessor accessor) {
		ChatSession session = ChatSession.from(accessor, sessionAttributeKey);
		if (session == null) {
			log.warn("검증되지 않은 연결이라 이동 추적 시작 요청을 무시합니다.");
			return;
		}

		participantService.validateHost(session.roomUuid(), session.participantId());

		TrackingResponseDto tracking = trackingService.insertTrackingSessionList(session.roomUuid());
		messagingTemplate.convertAndSend("/topic/room/" + session.roomUuid() + "/tracking", tracking);
	}

	@MessageExceptionHandler
	public void handleTrackingException(Exception e, SimpMessageHeaderAccessor accessor) {
		ChatSession session = ChatSession.from(accessor, sessionAttributeKey);
		if (session == null) {
			return;
		}

		log.warn("이동 추적 시작 실패 - {}", e.toString());

		String message = e.getMessage() == null ? "이동 추적을 시작하지 못했습니다." : e.getMessage();
		messagingTemplate.convertAndSend("/topic/room/" + session.roomUuid() + "/tracking/error",
				new SocketErrorResponseDto(message));
	}

}
