package com.kh.midpoint.chat.model.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * 프론트 -> 서버 (STOMP 발행 payload)
 *
 * 방과 참가자는 CONNECT 때 검증해 세션에 보관하므로 본문에는 내용만 담는다.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class ChatSendRequestDto {

	private String content;

}
