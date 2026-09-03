package com.kh.midpoint.vote.model.vo;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ModeVote {

	Long participantId;
	String voteMode;

}
