package com.kh.midpoint.participant.model.dto;

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
public class ParticipantResponseDto {
	private Long participantId;

	// 내부 식별자라 응답·소켓 페이로드에 나가면 안 된다(BR-27).
	// 서비스 내부에서는 방 소속 확인에 쓰이므로 필드 자체는 남긴다.
	@JsonIgnore
	private Long roomId;
	private String nickname;
	private String isHost;
	private String isReady;
	private Double prefLat;
	private Double prefLng;
	private LocalDateTime joinedAt;
	private LocalDateTime leftAt;
}