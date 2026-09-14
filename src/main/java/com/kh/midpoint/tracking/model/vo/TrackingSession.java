package com.kh.midpoint.tracking.model.vo;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class TrackingSession {

	Long sessionId;
	Long roomId;
	Long participantId;
	String deviceKey;
	Double destLat;
	Double destLng;
	Double lastDistanceM;

}
