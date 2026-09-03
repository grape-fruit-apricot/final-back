package com.kh.midpoint.chat.model.vo;

import org.springframework.messaging.simp.SimpMessageHeaderAccessor;

import java.util.Map;

public record ChatSession(
	String roomUuid,
	Long roomId,
	Long participantId,
	String nickname
) {

	public static final String ATTR_KEY = "chatSession";

	// 소켓 메시지의 작성자는 클라이언트가 보낸 값이 아니라 연결 시 세션에 저장해 둔 이 값으로 판단한다.
	// 검증되지 않은 연결이면 null 을 돌려주고, 호출한 쪽에서 요청을 무시한다.
	public static ChatSession from(SimpMessageHeaderAccessor accessor) {
		Map<String, Object> attributes = accessor.getSessionAttributes();
		if (attributes == null) {
			return null;
		}

		return (ChatSession) attributes.get(ATTR_KEY);
	}

}
