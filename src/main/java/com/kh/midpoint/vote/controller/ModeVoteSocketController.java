package com.kh.midpoint.vote.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import com.kh.midpoint.chat.model.vo.ChatSession;
import com.kh.midpoint.route.model.dto.RouteResponseDto;
import com.kh.midpoint.vote.model.dto.ModeVoteRequestDto;
import com.kh.midpoint.vote.model.dto.ModeVoteStatusDto;
import com.kh.midpoint.vote.model.service.ModeVoteService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// 진행 방식(게임/무작위) 투표. 투표자는 클라이언트가 보낸 값이 아니라 연결 시 세션에 저장된
// participantId 로 판단하므로, 남의 표를 대신 던질 수 없다.
@Slf4j
@Controller
@RequiredArgsConstructor
public class ModeVoteSocketController {

	@Value("${chat.session-attribute-key}")
	private String sessionAttributeKey;


	private final ModeVoteService modeVoteService;
	private final SimpMessagingTemplate messagingTemplate;

	@MessageMapping("/mode/start")
	public void startModeVote(SimpMessageHeaderAccessor accessor) {
		ChatSession session = ChatSession.from(accessor, sessionAttributeKey);
		if (session == null) {
			log.warn("검증되지 않은 연결이라 요청을 무시합니다.");
			return;
		}

		ModeVoteStatusDto status = modeVoteService.startModeVote(session.roomUuid(), session.participantId());
		sendStatus(session.roomUuid(), status);
	}

	@MessageMapping("/mode/vote")
	public void insertModeVote(@Payload ModeVoteRequestDto requestDto, SimpMessageHeaderAccessor accessor) {
		ChatSession session = ChatSession.from(accessor, sessionAttributeKey);
		if (session == null) {
			log.warn("검증되지 않은 연결이라 요청을 무시합니다.");
			return;
		}

		ModeVoteStatusDto status = modeVoteService.insertModeVote(
				session.roomUuid(), session.participantId(), requestDto);

		// 결정된 방식을 먼저 알린다. 무작위는 경로 계산에 몇 초가 걸려서,
		// 이걸 나중에 보내면 그동안 참가자들이 빈 화면을 보게 된다.
		sendStatus(session.roomUuid(), status);

		// 확정할 것이 있는지는 서비스가 판단한다. 여기서는 받은 것만 알린다.
		RouteResponseDto result = modeVoteService.insertRouteResultIfRandom(session.roomUuid(), status);
		if (result != null) {
			messagingTemplate.convertAndSend("/topic/room/" + session.roomUuid() + "/result", result);
		}
	}

	private void sendStatus(String roomUuid, ModeVoteStatusDto status) {
		messagingTemplate.convertAndSend("/topic/room/" + roomUuid + "/mode", status);
	}

}
