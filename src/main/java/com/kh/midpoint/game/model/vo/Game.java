package com.kh.midpoint.game.model.vo;

import lombok.Builder;
import lombok.Value;

// 보물 주머니 게임 1건. 방당 1건이라 ROOM_ID 가 자연키 역할을 한다.
// winningIndex 는 INSERT 할 때만 자바가 들고 있고, 이후로는 절대 다시 읽지 않는다.
// 당첨 판정은 updateGameFinished 의 WHERE 절에서만 이뤄진다.
@Value
@Builder
public class Game {

	Long roomId;
	Integer bagCount;
	Integer winningIndex;
	Long currentParticipantId;
	// 첫 차례의 마감 시각을 SQL 에서 계산하기 위해 넘긴다(초).
	Integer turnSeconds;

}
