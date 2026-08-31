package com.kh.midpoint.chat.controller;

import java.util.Map;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import com.kh.midpoint.chat.model.dto.ChatMessageResponse;
import com.kh.midpoint.chat.model.dto.ChatSendRequest;
import com.kh.midpoint.chat.model.service.ChatService;
import com.kh.midpoint.chat.model.vo.ChatSession;
import com.kh.midpoint.chat.model.vo.MsgType;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * STOMP 전용 컨트롤러.
 *
 *   연결 : /ws  (CONNECT 헤더에 roomUuid, participantId)
 *   발행 : /app/chat.enter, /app/chat.send
 *   구독 : /topic/room/{roomUuid}
 *
 * 방/참가자 정보는 CONNECT 때 검증해서 세션에 담아뒀으므로
 * 클라이언트가 보낸 본문에서는 내용만 읽는다.
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final SimpMessagingTemplate template;

    /** 입장 알림 */
    @MessageMapping("/chat.enter")
    public void enter(SimpMessageHeaderAccessor accessor) {
        ChatSession session = session(accessor);
        if (session == null) {
            return;
        }
 
        chatService.enterRoom(session.roomUuid());

        ChatMessageResponse saved = chatService.save(
                session, MsgType.ENTER, session.nickname() + "님이 입장하셨습니다.");

        broadcast(session, saved);
    }

    /** 일반 대화 */
    @MessageMapping("/chat.send")
    public void send(ChatSendRequest request, SimpMessageHeaderAccessor accessor) {
        ChatSession session = session(accessor);
        if (session == null) {
            return;
        }

        String content = request == null || request.getContent() == null
                ? "" : request.getContent().trim();
        if (content.isEmpty()) {
            return;
        }

        ChatMessageResponse saved = chatService.save(session, MsgType.TALK, content);

        broadcast(session, saved);
    }

    private void broadcast(ChatSession session, ChatMessageResponse message) {
        template.convertAndSend("/topic/room/" + session.roomUuid(), message);
    }

    /** CONNECT 때 저장해둔 세션 정보 꺼내기 */
    private ChatSession session(SimpMessageHeaderAccessor accessor) {
        Map<String, Object> attributes = accessor.getSessionAttributes();
        if (attributes == null) {
            log.warn("세션 attribute 가 없습니다. 메시지를 무시합니다.");
            return null;
        }
        Object value = attributes.get(ChatSession.ATTR_KEY);
        if (value == null) {
            log.warn("검증되지 않은 연결입니다. 메시지를 무시합니다.");
            return null;
        }
        return (ChatSession) value;
    }
}
