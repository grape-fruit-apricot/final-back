package com.kh.midpoint.game.model.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

// 승자를 ROOM_RESULT 에 연결할 때 쓰는 내부 조회 결과.
// 승자가 식당을 고르지 않았을 수 있어 restaurantId 는 null 일 수 있다.
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class GameWinnerQueryDto {

	private Long gameParticipantId;
	private Long participantId;
	private Long restaurantId;

}
