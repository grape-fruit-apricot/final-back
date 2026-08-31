package com.kh.midpoint.chat.model.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * 프론트 -> 서버 (STOMP 발행 payload)
 *
 * roomUuid 와 participantId 는 CONNECT 헤더로 받아 세션에 보관하므로
 * 본문에는 내용만 담는다. (사칭 방지)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class ChatSendRequest {

    private String content;
}
