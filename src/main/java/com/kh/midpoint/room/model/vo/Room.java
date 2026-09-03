package com.kh.midpoint.room.model.vo;

import lombok.Builder;
import lombok.Value;

import java.util.UUID;

@Value
@Builder
public class Room {

	Long roomId;
	String roomUuid;
	Integer maxParticipants;
	Double midpointLat;
	Double midpointLng;
	String midpointSource;
	String stage;

	public static Room create(Integer maxParticipants) {
		return Room.builder()
			.roomUuid(UUID.randomUUID().toString())
			.maxParticipants(maxParticipants)
			.build();
	}

}
