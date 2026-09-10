package com.kh.midpoint.route.model.dto;

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
public class RouteSegmentDto {

	private Integer segmentOrder;
	private String segmentType;
	private Integer timeMinutes;
	private String guidance;
	private List<String> vehicles;
	private List<RoutePointDto> points;

}
