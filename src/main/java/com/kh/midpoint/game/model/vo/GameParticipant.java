package com.kh.midpoint.game.model.vo;

import lombok.Builder;
import lombok.Value;

// 게임에 참가한 사람 1명. 방장 + 준비를 마친 참가자만 들어간다.
// GAME_ID 는 ROOM_ID 로 찾아 넣으므로 생성 ID 를 자바가 받지 않는다.
@Value
@Builder
public class GameParticipant {

	Long roomId;
	Long participantId;
	// 차례 순번(0-based). 방장 0, 나머지는 participantId 오름차순.
	Integer turnOrder;

}
