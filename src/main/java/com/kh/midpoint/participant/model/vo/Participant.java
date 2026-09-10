package com.kh.midpoint.participant.model.vo;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class Participant {
	Long participantId;
	Long roomId;
	String nickname;
	String isHost;
	String isReady;
	Double prefLat;
	Double prefLng;
	LocalDateTime joinedAt;
}