package com.kh.midpoint.game.model.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

// 게임 현황 전체. 브로드캐스트할 때마다 이걸 통째로 보낸다(기존 mode/selections 와 같은 방식).
// 부분 갱신을 보내면 클라이언트가 놓친 메시지를 스스로 메워야 해서 상태가 어긋난다.
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class GameStatusDto {

	private String status;
	private int bagCount;
	// 소비된 차례 수. 프론트가 만료를 알릴 때 이 값을 실어 보내 늦은 알림을 걸러낸다.
	private int turnSeq;
	private Long currentParticipantId;
	// 남은 시간은 DB 시계로 계산해 내려준다. 브라우저 시계로 빼면 기기별 오차가 그대로 게임에 반영된다.
	private int remainingSeconds;
	// 게임이 끝나기 전에는 항상 null 이다. SQL 의 CASE 가 보장하므로 자바에서 지울 일이 없다.
	private Integer winningIndex;
	private Long winnerParticipantId;
	private List<GamePlayerResponseDto> players;
	private List<GamePickResponseDto> picks;

}
