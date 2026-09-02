package com.kh.midpoint.chat.model.vo;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ChatMessage {

	Long messageId;
	Long roomId;
	Long participantId;
	String content;
	MsgType msgType;

}
