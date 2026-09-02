package com.kh.midpoint.chat.controller;

import java.util.Map;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import com.kh.midpoint.chat.model.dto.ChatMessageResponseDto;
import com.kh.midpoint.chat.model.dto.ChatSendRequestDto;
import com.kh.midpoint.chat.model.service.ChatService;
import com.kh.midpoint.chat.model.vo.ChatSession;
import com.kh.midpoint.chat.model.vo.MsgType;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * STOMP 전용 컨트롤러.
 *
 *   발행 : /app/chat.enter, /app/chat.send, /app/chat.leave
 *   구독 : /topic/room/{roomUuid}
 *
 * 방과 참가자는 CONNECT 때 검증해 세션에 담아뒀으므로, 여기서는 세션 값만 사용한다.
 * 클라이언트가 본문에 다른 사람의 정보를 실어 보내도 반영되지 않는다.
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatController {

	private final ChatService chatService;
	private final SimpMessagingTemplate messagingTemplate;

	@MessageMapping("/chat.enter")
	public void enter(SimpMessageHeaderAccessor accessor) {
		ChatSession session = findSession(accessor);
		if (session == null) {
			return;
		}
		broadcast(session, MsgType.ENTER, session.nickname() + "님이 입장하셨습니다.");
	}

	@MessageMapping("/chat.send")
	public void send(ChatSendRequestDto request, SimpMessageHeaderAccessor accessor) {
		ChatSession session = findSession(accessor);
		if (session == null) {
			return;
		}

		String content = (request == null || request.getContent() == null)
				? "" : request.getContent().trim();
		if (content.isEmpty()) {
			return;
		}

		broadcast(session, MsgType.TALK, content);
	}

	@MessageMapping("/chat.leave")
	public void leave(SimpMessageHeaderAccessor accessor) {
		ChatSession session = findSession(accessor);
		if (session == null) {
			return;
		}
		broadcast(session, MsgType.LEAVE, session.nickname() + "님이 퇴장하셨습니다.");
	}

	private void broadcast(ChatSession session, String msgType, String content) {
		ChatMessageResponseDto saved = chatService.saveMessage(session, msgType, content);
		messagingTemplate.convertAndSend("/topic/room/" + session.roomUuid(), saved);
	}

	/** CONNECT 때 저장해둔 세션 정보를 꺼낸다 */
	private ChatSession findSession(SimpMessageHeaderAccessor accessor) {
		Map<String, Object> attributes = accessor.getSessionAttributes();
		if (attributes == null) {
			log.warn("세션 attribute 가 없어 메시지를 무시합니다.");
			return null;
		}

		Object value = attributes.get(ChatSession.ATTR_KEY);
		if (value == null) {
			log.warn("검증되지 않은 연결이라 메시지를 무시합니다.");
			return null;
		}

		return (ChatSession) value;
	}

}
