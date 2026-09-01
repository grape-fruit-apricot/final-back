package com.kh.midpoint.room.model.vo;

import lombok.Builder;
import lombok.Value;

import java.util.UUID;

@Value
@Builder
public class Room {

	String roomUuid;
	Integer maxParticipants;

	public static Room create(Integer maxParticipants) {
		return Room.builder()
			.roomUuid(UUID.randomUUID().toString())
			.maxParticipants(maxParticipants)
			.build();
	}

}
