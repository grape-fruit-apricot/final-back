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
public class ParticipantDto {
	private Long participantId;
	private Long roomId;
	private String nickname;
	private String isHost; // 'Y' / 'N'
	private String isReady; // 'Y' / 'N'
	private Double prefLat;
	private Double prefLng;
	private LocalDateTime joinedAt;
	private LocalDateTime leftAt;

	public boolean isHost() {
		return "Y".equals(isHost);
	}

	public boolean isReady() {
		return "Y".equals(isReady);
	}
}