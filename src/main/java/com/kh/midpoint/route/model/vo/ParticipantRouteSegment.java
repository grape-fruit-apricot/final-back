package com.kh.midpoint.route.model.vo;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ParticipantRouteSegment {

	Integer segmentOrder;
	String segmentType;
	Integer timeMinutes;
	String guidance;
	String vehicles;

}
