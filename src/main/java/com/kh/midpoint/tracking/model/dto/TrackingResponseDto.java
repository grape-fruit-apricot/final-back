package com.kh.midpoint.tracking.model.dto;

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
// 추적 화면이 보는 전부. 소켓 방송과 REST 조회가 같은 모양을 쓴다.
// 새로고침으로 복원할 때와 실시간으로 받을 때 화면이 다르게 동작할 이유가 없다.
public class TrackingResponseDto {

	private TrackingDestinationResponseDto destination;
	private List<TrackingParticipantResponseDto> participants;

}
