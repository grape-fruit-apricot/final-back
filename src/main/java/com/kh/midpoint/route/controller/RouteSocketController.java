package com.kh.midpoint.route.controller;


import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import com.kh.midpoint.chat.model.vo.ChatSession;
import com.kh.midpoint.common.response.SocketErrorResponseDto;
import com.kh.midpoint.participant.model.service.ParticipantService;
import com.kh.midpoint.route.model.dto.RouteResponseDto;
import com.kh.midpoint.route.model.service.RouteService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// 방장이 결과 확정을 트리거하는 소켓 컨트롤러. 실제 게임이 붙으면 이 트리거만 교체하면 되고,
// 확정·경로 계산은 그대로 RouteService 가 담당한다.
// 방장 여부는 클라이언트가 보낸 값이 아니라 연결 시 세션에 저장된 participantId 로 확인한다.
@Slf4j
@Controller
@RequiredArgsConstructor
public class RouteSocketController {

	private final RouteService routeService;
	private final ParticipantService participantService;
	private final SimpMessagingTemplate messagingTemplate;

	@MessageMapping("/result/find")
	public void findRoute(SimpMessageHeaderAccessor accessor) {
		ChatSession session = ChatSession.from(accessor);
		if (session == null) {
			log.warn("검증되지 않은 연결이라 결과 확정 요청을 무시합니다.");
			return;
		}

		participantService.validateHost(session.roomUuid(), session.participantId());

		RouteResponseDto result = routeService.findRoute(session.roomUuid());
		messagingTemplate.convertAndSend("/topic/room/" + session.roomUuid() + "/result", result);
	}

	@MessageExceptionHandler
	public void handleFindRouteException(Exception e, SimpMessageHeaderAccessor accessor) {
		ChatSession session = ChatSession.from(accessor);
		if (session == null) {
			return;
		}

		log.warn("결과 확정 실패 - {}", e.toString());

		String message = e.getMessage() == null ? "결과를 확정하지 못했습니다." : e.getMessage();
		messagingTemplate.convertAndSend("/topic/room/" + session.roomUuid() + "/result/error",
				new SocketErrorResponseDto(message));
	}


}
