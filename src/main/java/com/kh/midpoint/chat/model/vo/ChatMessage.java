package com.kh.midpoint.chat.model.vo;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessage {
	private Long messageId;
    private Long roomId;
    private Long participantId;
    private String content;
    private String msgType;
    private LocalDateTime createdAt;
}
