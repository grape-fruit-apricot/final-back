package com.kh.midpoint.route.model.vo;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ParticipantRoutePoint {

	Long roomId;
	Long participantId;
	String travelMode;
	Integer pointOrder;
	Double lat;
	Double lng;

}
