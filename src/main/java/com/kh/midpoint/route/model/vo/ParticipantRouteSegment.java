package com.kh.midpoint.route.model.vo;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ParticipantRouteSegment {

	Long routeSegmentId;
	Long routeId;
	Integer segmentOrder;
	String segmentType;
	Integer timeMinutes;
	String guidance;
	String vehicles;

}
