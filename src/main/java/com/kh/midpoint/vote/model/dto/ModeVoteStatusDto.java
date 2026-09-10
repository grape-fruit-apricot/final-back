package com.kh.midpoint.vote.model.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

// 투표 현황 전체. 한 번의 브로드캐스트로 화면을 그릴 수 있도록 진행 상황과 결과를 함께 담는다.
// decidedMode 는 전원이 투표를 마치기 전까지 null 이다.
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class ModeVoteStatusDto {

	private List<ModeVoteResponseDto> votes;
	private int totalCount;
	private String decidedMode;

}
