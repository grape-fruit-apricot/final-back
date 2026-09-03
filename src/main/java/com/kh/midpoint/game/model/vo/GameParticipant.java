package com.kh.midpoint.game.model.vo;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Value;

/** GAME_PARTICIPANT 테이블에 저장하거나 갱신할 참가자별 승패 결과 값 객체다. */
@Value
@Builder
public class GameParticipant {
	 Long gameParticipantId;
	 Long participantId;
	 String isWinner;
	 LocalDateTime joinedAt;
	 LocalDateTime leftAt;
}
