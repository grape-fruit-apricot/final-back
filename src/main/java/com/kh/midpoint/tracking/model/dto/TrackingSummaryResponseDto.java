package com.kh.midpoint.tracking.model.dto;

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
// 도착한 뒤에만 채운다. 저장해 둔 좌표 이력을 되짚어 계산하는 값이라,
// 원본을 남기지 않았다면 나올 수 없는 숫자다.
//
// totalDistanceM 은 목적지까지의 직선거리(lastDistanceM)가 아니라
// 실제로 지나온 점들을 이어붙인 누적 이동거리다.
public class TrackingSummaryResponseDto {

	private Double totalDistanceM;
	private Long durationSeconds;
	private Double averageSpeedKmh;

}
