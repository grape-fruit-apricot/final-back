package com.kh.midpoint.vote.model.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

// 누가 무엇에 투표했는지 한 건. 프론트에서 참가자별 투표 여부를 표시하는 데 쓴다.
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class ModeVoteResponseDto {

	private Long participantId;
	private String voteMode;

}
