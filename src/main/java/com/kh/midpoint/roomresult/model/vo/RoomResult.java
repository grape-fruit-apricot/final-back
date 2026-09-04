package com.kh.midpoint.roomresult.model.vo;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class RoomResult {

	Long roomId;
	Long restaurantId;
	// 게임 승자의 GAME_PARTICIPANT_ID. 게임 없이 확정된 방은 null 이다(BR-14/BR-15).
	Long gameParticipantId;

}
