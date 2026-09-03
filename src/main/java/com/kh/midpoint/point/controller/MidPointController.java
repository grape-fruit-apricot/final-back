package com.kh.midpoint.point.controller;

import java.util.Map;

import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import com.kh.midpoint.chat.model.vo.ChatSession;
import com.kh.midpoint.external.kakao.NearbyStationDto;
import com.kh.midpoint.point.model.dto.MidPointErrorResponseDto;
import com.kh.midpoint.point.model.service.MidPointService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequiredArgsConstructor
public class MidPointController {

	private final MidPointService midPointService;
	private final SimpMessagingTemplate messagingTemplate;

	@MessageMapping("/midpoint/find")
	public void findMidpoint(SimpMessageHeaderAccessor accessor) {
		ChatSession session = findSession(accessor);
		if (session == null) {
			return;
		}

		NearbyStationDto midpoint = midPointService.findMidpoint(session.roomUuid(), session.participantId());
		messagingTemplate.convertAndSend("/topic/room/" + session.roomUuid() + "/midpoint", midpoint);
	}

	@MessageExceptionHandler
	public void handleFindMidpointException(Exception e, SimpMessageHeaderAccessor accessor) {
		ChatSession session = findSession(accessor);
		if (session == null) {
			return;
		}

		log.warn("중간지점 찾기 실패 - {}", e.toString());

		String message = e.getMessage() == null ? "중간지점을 찾지 못했습니다." : e.getMessage();
		messagingTemplate.convertAndSend("/topic/room/" + session.roomUuid() + "/midpoint/error",
				new MidPointErrorResponseDto(message));
	}

	private ChatSession findSession(SimpMessageHeaderAccessor accessor) {
		Map<String, Object> attributes = accessor.getSessionAttributes();
		if (attributes == null) {
			log.warn("세션 attribute 가 없어 요청을 무시합니다.");
			return null;
		}

		Object value = attributes.get(ChatSession.ATTR_KEY);
		if (value == null) {
			log.warn("검증되지 않은 연결이라 요청을 무시합니다.");
			return null;
		}

		return (ChatSession) value;
	}

}
