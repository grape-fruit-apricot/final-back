package com.kh.midpoint.game.model.vo;

import lombok.Builder;
import lombok.Value;

// 열린 주머니 1건. 당첨 여부는 저장하지 않는다.
// GAME.WINNING_INDEX 와 비교해 조회 시점에 계산하면 되고, 두 군데에 두면 당첨 위치가 샐 구멍이 늘어난다.
@Value
@Builder
public class GamePick {

	Long roomId;
	Long participantId;
	Integer bagIndex;

}
