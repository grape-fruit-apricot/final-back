package com.kh.midpoint.chat.model.vo;

/**
 * STOMP CONNECT 시점에 검증이 끝난 접속 정보.
 * WebSocket 세션 attribute 에 담아두고 메시지 처리 때 꺼내 쓴다.
 * 클라이언트가 보낸 값을 그대로 믿지 않기 위한 장치다.
 */
public record ChatSession(
        String roomUuid,
        Long roomId,
        Long participantId,
        String nickname
) {
    /** 세션 attribute 키 */
    public static final String ATTR_KEY = "chatSession";
}
