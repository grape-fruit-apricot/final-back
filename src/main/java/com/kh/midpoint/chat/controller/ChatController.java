package com.kh.midpoint.chat.controller;

import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import com.kh.midpoint.chat.model.dto.ChatMessageResponseDto;
import com.kh.midpoint.chat.model.dto.ChatSendRequestDto;
import com.kh.midpoint.chat.model.service.ChatService;
import com.kh.midpoint.chat.model.vo.ChatSession;
import com.kh.midpoint.chat.model.vo.MsgType;
import com.kh.midpoint.common.response.SocketErrorResponseDto;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatController {

	private final ChatService chatService;
	private final SimpMessagingTemplate messagingTemplate;

	@MessageMapping("/chat/enter")
	public void enter(SimpMessageHeaderAccessor accessor) {
		ChatSession session = ChatSession.from(accessor);
		if (session == null) {
			log.warn("검증되지 않은 연결이라 입장 요청을 무시합니다.");
			return;
		}

		broadcast(session, MsgType.ENTER, session.nickname() + "님이 입장하셨습니다.");
	}

	@MessageMapping("/chat/send")
	public void send(ChatSendRequestDto requestDto, SimpMessageHeaderAccessor accessor) {
		ChatSession session = ChatSession.from(accessor);
		if (session == null) {
			log.warn("검증되지 않은 연결이라 메시지를 무시합니다.");
			return;
		}

		String content = (requestDto == null || requestDto.getContent() == null)
				? "" : requestDto.getContent().trim();
		if (content.isEmpty()) {
			return;
		}

		broadcast(session, MsgType.TALK, content);
	}

	@MessageMapping("/chat/leave")
	public void leave(SimpMessageHeaderAccessor accessor) {
		ChatSession session = ChatSession.from(accessor);
		if (session == null) {
			log.warn("검증되지 않은 연결이라 퇴장 요청을 무시합니다.");
			return;
		}

		broadcast(session, MsgType.LEAVE, session.nickname() + "님이 퇴장하셨습니다.");
	}

	@MessageExceptionHandler
	public void handleChatException(Exception e, SimpMessageHeaderAccessor accessor) {
		ChatSession session = ChatSession.from(accessor);
		if (session == null) {
			return;
		}

		log.warn("채팅 처리 실패 - {}", e.toString());

		String message = e.getMessage() == null ? "메시지를 처리하지 못했습니다." : e.getMessage();
		messagingTemplate.convertAndSend("/topic/room/" + session.roomUuid() + "/chat/error",
				new SocketErrorResponseDto(message));
	}

	private void broadcast(ChatSession session, MsgType msgType, String content) {
		ChatMessageResponseDto saved = chatService.saveMessage(session, msgType, content);
		messagingTemplate.convertAndSend("/topic/room/" + session.roomUuid(), saved);
	}

}
