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
// 지도에 찍는 좌표 한 점. 현재 위치와 궤적이 같은 모양을 쓴다.
public class TrackingPointResponseDto {

	private Double lat;
	private Double lng;

}
