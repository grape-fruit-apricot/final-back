package com.kh.midpoint.external.kakao;

import java.util.List;

import com.kh.midpoint.route.model.dto.RoutePointDto;
import com.kh.midpoint.route.model.dto.RouteSegmentDto;

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
	private List<RouteSegmentDto> segments;

}
