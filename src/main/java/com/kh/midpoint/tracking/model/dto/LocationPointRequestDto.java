package com.kh.midpoint.tracking.model.dto;

import jakarta.validation.constraints.NotNull;

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
public class LocationPointRequestDto {

	@NotNull(message = "세션 번호가 필요합니다.")
	private Long sessionId;

	// 디바이스가 매긴 전송 순번. 같은 값이 두 번 오면 DB 의 유일 제약이 막는다.
	@NotNull(message = "전송 순번이 필요합니다.")
	private Integer seq;

	@NotNull(message = "위도가 필요합니다.")
	private Double lat;

	@NotNull(message = "경도가 필요합니다.")
	private Double lng;

}
