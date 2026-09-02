package com.kh.midpoint.chat.model.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * 서버 -> 프론트 (STOMP 브로드캐스트 · 이전 대화 조회)
 *
 * 닉네임은 PARTICIPANT 와 LEFT JOIN 해서 채운다.
 * 이미 나간 참가자는 행이 삭제되어 있으므로 '알 수 없음' 이 들어온다.
 */
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
	private String msgType;
	private LocalDateTime createdAt;

}
