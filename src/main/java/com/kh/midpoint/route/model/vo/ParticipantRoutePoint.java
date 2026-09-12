package com.kh.midpoint.route.model.vo;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ParticipantRoutePoint {

	Integer pointOrder;
	Double lat;
	Double lng;

}
