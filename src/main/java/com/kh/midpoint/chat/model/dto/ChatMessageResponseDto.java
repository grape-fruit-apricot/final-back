package com.kh.midpoint.chat.model.dto;

import java.time.LocalDateTime;

import com.kh.midpoint.chat.model.vo.MsgType;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class ChatMessageResponseDto {

	private Long messageId;
	private Long participantId;
	private String nickname;
	private String content;
	private MsgType msgType;
	private LocalDateTime createdAt;

}
