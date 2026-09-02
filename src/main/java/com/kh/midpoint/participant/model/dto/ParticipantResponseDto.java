package com.kh.midpoint.participant.model.dto;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@ToString
public class ParticipantResponseDto {
	private Long participantId;
	private Long roomId;
	private String nickname;
	private String isHost;
	private String isReady;
	private Double prefLat;
	private Double prefLng;
	private LocalDateTime joinedAt;
	private LocalDateTime leftAt;
}