package com.kh.midpoint.tracking.model.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

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
// 디바이스 1대가 참가자 여러 명을 대신 움직이므로 좌표를 묶어서 받는다.
// 한 요청 = 한 트랜잭션 = 한 번의 방송이라, 인원이 늘어도 DB 커넥션을 더 잡지 않는다.
public class LocationRequestDto {

	@NotBlank(message = "디바이스 키가 필요합니다.")
	private String deviceKey;

	@Valid
	@NotEmpty(message = "좌표가 최소 1건 필요합니다.")
	private List<LocationPointRequestDto> points;

}
