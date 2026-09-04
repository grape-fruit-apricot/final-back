package com.kh.midpoint.game.controller;

import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import com.kh.midpoint.chat.model.vo.ChatSession;
import com.kh.midpoint.game.model.dto.GameStatusDto;
import com.kh.midpoint.game.model.service.GameService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// 연결이 끊긴 사람의 차례를 즉시 넘긴다.
//
// 끊겼다고 게임에서 빼지는 않는다. 새로고침은 물론이고 채팅 탭으로 이동하기만 해도
// (ChatPage 가 자기 소켓을 따로 연다) 연결이 끊겼다 다시 붙기 때문에,
// 끊김을 이탈로 처리하면 채팅을 한 번 보고 온 사람이 탈락한다.
// 진짜 나가기는 /app/game/leave 로만 처리한다.
//
// 브로드캐스트를 하므로 컨트롤러와 같은 자리에 둔다.
@Slf4j
@Component
@RequiredArgsConstructor
public class GameDisconnectListener {

	private final GameService gameService;
	private final SimpMessagingTemplate messagingTemplate;

	@EventListener
	public void handleSessionDisconnect(SessionDisconnectEvent event) {
		ChatSession session = ChatSession.from(StompHeaderAccessor.wrap(event.getMessage()));
		if (session == null) {
			return;
		}

		// 이 메서드는 컨트롤러가 아니라 브로커 스레드에서 돌기 때문에 예외를 밖으로 던지면
		// 애플리케이션 전체의 연결 종료 처리가 깨진다. 여기서만은 직접 잡는다.
		try {
			GameStatusDto status = gameService.updateTurnBySkip(session.roomUuid(), session.participantId());
			if (status == null) {
				return;
			}
			messagingTemplate.convertAndSend("/topic/room/" + session.roomUuid() + "/game", status);
		} catch (Exception e) {
			log.warn("연결 종료 처리 실패 - roomUuid={}, {}", session.roomUuid(), e.toString());
		}
	}

}
