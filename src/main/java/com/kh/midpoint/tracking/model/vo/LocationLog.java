package com.kh.midpoint.tracking.model.vo;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class LocationLog {

	Long sessionId;
	Integer seq;
	Double lat;
	Double lng;
	Double distanceM;

}
