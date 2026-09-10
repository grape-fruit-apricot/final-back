package com.kh.midpoint.game.model.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

// 열린 주머니 1건. isWinner 는 당첨 주머니가 열린 뒤에만 'Y' 가 될 수 있고,
// 그 시점에는 이미 게임이 끝나 있으므로 당첨 위치가 미리 새지 않는다.
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class GamePickResponseDto {

	private Integer bagIndex;
	private Long participantId;
	private String isWinner;

}
