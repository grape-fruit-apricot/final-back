package com.kh.midpoint.chat.listener;

import java.util.Map;

import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import com.kh.midpoint.chat.model.dto.ChatMessageResponse;
import com.kh.midpoint.chat.model.service.ChatService;
import com.kh.midpoint.chat.model.vo.ChatSession;
import com.kh.midpoint.chat.model.vo.MsgType;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 브라우저를 닫거나 연결이 끊기면 퇴장 메시지를 남긴다.
 * 나가기 버튼을 따로 누르지 않아도 동작한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketEventListener {

    private final ChatService chatService;
    private final SimpMessagingTemplate template;

    @EventListener
    public void handleDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());

        Map<String, Object> attributes = accessor.getSessionAttributes();
        if (attributes == null) {
            return;
        }

        Object value = attributes.get(ChatSession.ATTR_KEY);
        if (value == null) {
            return;
        }

        ChatSession session = (ChatSession) value;
        chatService.leaveRoom(session.roomUuid());

        try {
            ChatMessageResponse saved = chatService.save(
                    session, MsgType.LEAVE, session.nickname() + "님이 퇴장하셨습니다.");
            template.convertAndSend("/topic/room/" + session.roomUuid(), saved);
        } catch (Exception e) {
            log.warn("퇴장 메시지 처리 실패 - {}", e.getMessage());
        }
    }
}
