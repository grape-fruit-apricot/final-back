package com.kh.midpoint.tracking.model.dto;

import java.time.LocalDateTime;
import java.util.List;

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
// 참가자 한 명의 이동 상황. sessionId 와 roomId 는 넣지 않는다(BR-27).
// 화면이 참가자를 가리키는 값은 participantId 하나로 충분하다.
public class TrackingParticipantResponseDto {

	private Long participantId;
	private String nickname;
	private String status;
	private Double distanceM;
	private TrackingPointResponseDto current;
	private List<TrackingPointResponseDto> trail;
	private LocalDateTime arrivedAt;
	private TrackingSummaryResponseDto summary;

}
