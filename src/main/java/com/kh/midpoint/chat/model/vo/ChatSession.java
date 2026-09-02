package com.kh.midpoint.chat.model.vo;

public record ChatSession(
	String roomUuid,
	Long roomId,
	Long participantId,
	String nickname
) {

	public static final String ATTR_KEY = "chatSession";

}
