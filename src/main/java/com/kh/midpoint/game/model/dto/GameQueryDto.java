package com.kh.midpoint.game.model.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

// 서비스가 규칙을 판단할 때만 쓰는 내부 조회 결과. 컨트롤러 밖으로 나가지 않는다.
// winningIndex 필드를 일부러 두지 않았다. 자바가 당첨 위치를 들고 있지 않아야
// 실수로 응답에 섞여 나갈 수 없다.
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class GameQueryDto {

	private String status;
	private Integer bagCount;
	private Integer turnSeq;
	private Long currentParticipantId;
	// 마감 시각이 지났는지를 DB 시계로 판단해 받아온다.
	private String isExpired;
	private Integer consecutiveTimeouts;

}
