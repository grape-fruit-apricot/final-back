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
// 좌표 이력 한 행. 방 단위로 한 번에 읽어 자바에서 세션별로 나눈다.
public class LocationLogResponseDto {

	private Long sessionId;
	private Integer seq;
	private Double lat;
	private Double lng;
	private Double distanceM;

}
