package com.kh.midpoint.room.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;

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

	// 내부 식별자라 응답에 나가면 안 된다(BR-27). 서비스 간 위임에는 계속 쓰이므로 필드는 남긴다.
	@JsonIgnore
	private Long roomId;

	private String roomUuid;
	private int maxParticipants;
	private Double midpointLat;
	private Double midpointLng;
	private String midpointSource;
	private String stage;
	private LocalDateTime createdAt;
	private LocalDateTime expiresAt;

}
