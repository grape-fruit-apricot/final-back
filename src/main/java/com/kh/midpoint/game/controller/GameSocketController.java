package com.kh.midpoint.game.controller;

import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import com.kh.midpoint.chat.model.vo.ChatSession;
import com.kh.midpoint.common.response.SocketErrorResponseDto;
import com.kh.midpoint.game.model.dto.GameExpireRequestDto;
import com.kh.midpoint.game.model.dto.GamePickRequestDto;
import com.kh.midpoint.game.model.dto.GameStatusDto;
import com.kh.midpoint.game.model.service.GameService;
import com.kh.midpoint.route.model.dto.RouteResponseDto;
import com.kh.midpoint.route.model.service.RouteService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// 보물 주머니 게임. 주머니를 연 사람은 클라이언트가 보낸 값이 아니라 연결 시 세션에 저장된
// participantId 로 판단하므로, 남의 차례를 대신 쓸 수 없다.
@Slf4j
@Controller
@RequiredArgsConstructor
public class GameSocketController {

	private final GameService gameService;
	private final RouteService routeService;
	private final SimpMessagingTemplate messagingTemplate;

	@MessageMapping("/game/start")
	public void insertGame(SimpMessageHeaderAccessor accessor) {
		ChatSession session = ChatSession.from(accessor);
		if (session == null) {
			log.warn("검증되지 않은 연결이라 요청을 무시합니다.");
			return;
		}

		sendStatus(session.roomUuid(), gameService.insertGame(session.roomUuid(), session.participantId()));
	}

	@MessageMapping("/game/pick")
	public void insertGamePick(@Payload GamePickRequestDto requestDto, SimpMessageHeaderAccessor accessor) {
		ChatSession session = ChatSession.from(accessor);
		if (session == null) {
			log.warn("검증되지 않은 연결이라 요청을 무시합니다.");
			return;
		}

		GameStatusDto status = gameService.insertGamePick(
				session.roomUuid(), session.participantId(), requestDto);

		// 승자를 먼저 알린다. 경로 계산에 몇 초가 걸려서 이걸 나중에 보내면
		// 그동안 참가자들이 빈 화면을 보게 된다.
		sendStatus(session.roomUuid(), status);

		if (GameService.STATUS_FINISHED.equals(status.getStatus())) {
			// 승자가 고른 식당을 결과로 먼저 박아둔다. 그러면 이어지는 findRoute 가
			// 무작위 추첨을 건너뛰고 그 식당으로 경로를 만든다.
			gameService.insertRoomResult(session.roomUuid());
			RouteResponseDto result = routeService.findRoute(session.roomUuid());
			messagingTemplate.convertAndSend("/topic/room/" + session.roomUuid() + "/result", result);
		}
	}

	// 마감된 차례를 넘겨달라는 알림. 서버에 타이머를 두지 않고 클라이언트가 알려주는 구조라
	// 누구나 보낼 수 있지만, 조건이 맞지 않으면 서비스에서 아무 일도 일어나지 않는다.
	@MessageMapping("/game/expire")
	public void updateTurnExpired(@Payload GameExpireRequestDto requestDto, SimpMessageHeaderAccessor accessor) {
		ChatSession session = ChatSession.from(accessor);
		if (session == null) {
			log.warn("검증되지 않은 연결이라 요청을 무시합니다.");
			return;
		}

		sendStatus(session.roomUuid(), gameService.updateTurnExpired(session.roomUuid(), requestDto.getTurnSeq()));
	}

	@MessageMapping("/game/leave")
	public void updateGameParticipantLeft(SimpMessageHeaderAccessor accessor) {
		ChatSession session = ChatSession.from(accessor);
		if (session == null) {
			log.warn("검증되지 않은 연결이라 요청을 무시합니다.");
			return;
		}

		sendStatus(session.roomUuid(),
				gameService.updateGameParticipantLeft(session.roomUuid(), session.participantId()));
	}

	@MessageExceptionHandler
	public void handleGameException(Exception e, SimpMessageHeaderAccessor accessor) {
		ChatSession session = ChatSession.from(accessor);
		if (session == null) {
			return;
		}

		log.warn("게임 처리 실패 - {}", e.toString());

		String message = e.getMessage() == null ? "게임을 처리하지 못했습니다." : e.getMessage();
		messagingTemplate.convertAndSend("/topic/room/" + session.roomUuid() + "/game/error",
				new SocketErrorResponseDto(message));
	}

	private void sendStatus(String roomUuid, GameStatusDto status) {
		messagingTemplate.convertAndSend("/topic/room/" + roomUuid + "/game", status);
	}

}
