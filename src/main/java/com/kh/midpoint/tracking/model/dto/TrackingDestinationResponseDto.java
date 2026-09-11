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
// 확정된 목적지. 세션을 만들 때 좌표를 스냅샷으로 떠 두므로(BR-34),
// 결과가 나중에 바뀌어도 이동 중인 판정 기준은 흔들리지 않는다.
public class TrackingDestinationResponseDto {

	private String name;
	private Double lat;
	private Double lng;

}
