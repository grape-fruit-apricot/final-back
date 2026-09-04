package com.kh.midpoint.external.kakao;

import java.util.List;

import com.kh.midpoint.external.tmap.RoutePointDto;

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
public class TransitRouteResponseDto {

	private Integer timeMinutes;
	private List<RoutePointDto> points;

}
