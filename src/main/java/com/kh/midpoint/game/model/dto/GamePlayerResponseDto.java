package com.kh.midpoint.game.model.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

// 게임에 참가한 사람 1명의 표시용 정보.
// nickname/isHost 는 이미 participants 토픽으로 공개된 값이라 함께 담아도 문제없다.
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class GamePlayerResponseDto {

	private Long participantId;
	private String nickname;
	private String isHost;
	private Integer turnOrder;
	private String isLeft;
	private String isWinner;

}
