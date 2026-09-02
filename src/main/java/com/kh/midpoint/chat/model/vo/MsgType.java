package com.kh.midpoint.chat.model.vo;

/**
 * CHAT_MESSAGE.MSG_TYPE 값.
 * 프론트가 이 값으로 말풍선과 시스템 메시지를 구분하므로 문자열을 직접 쓰지 않는다.
 */
public final class MsgType {

	public static final String ENTER = "ENTER";
	public static final String TALK  = "TALK";
	public static final String LEAVE = "LEAVE";

	private MsgType() {
	}

}
