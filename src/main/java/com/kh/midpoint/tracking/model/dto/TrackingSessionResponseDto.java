package com.kh.midpoint.tracking.model.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
// 추적 세션 한 행. 서비스 내부 계산용이라 그대로 응답에 나가지 않는다.
public class TrackingSessionResponseDto {

	private Long sessionId;

	// 내부 식별자라 응답에 나가면 안 된다(BR-27). 방 단위 조회에는 필요해 필드는 남긴다.
	@JsonIgnore
	private Long roomId;
	private String roomUuid;
	private Long participantId;
	private String nickname;
	private String deviceKey;
	private Double destLat;
	private Double destLng;
	private String status;
	private Double lastDistanceM;
	private LocalDateTime startedAt;
	private LocalDateTime arrivedAt;

}
