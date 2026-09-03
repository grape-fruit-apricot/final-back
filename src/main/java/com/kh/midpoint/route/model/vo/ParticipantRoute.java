package com.kh.midpoint.route.model.vo;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ParticipantRoute {

	Long roomId;
	Long participantId;
	Integer timeMinutes;

}
