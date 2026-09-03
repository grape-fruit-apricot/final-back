package com.kh.midpoint.vote.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class ModeVoteRequestDto {

	// 'GAME' 또는 'RANDOM'. 실제 값 검증은 CK_MODE_VOTE_MODE 와 같은 기준으로 서비스에서 한다.
	@NotBlank
	private String voteMode;

}
