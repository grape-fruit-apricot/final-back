package com.kh.midpoint.room.model.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class RoomResponseDto {

	private String roomUuid;
	private int maxParticipants;
	private Double midpointLat;
	private Double midpointLng;
	private String midpointSource;
	private String stage;
	private LocalDateTime createdAt;
	private LocalDateTime expiresAt;

}
