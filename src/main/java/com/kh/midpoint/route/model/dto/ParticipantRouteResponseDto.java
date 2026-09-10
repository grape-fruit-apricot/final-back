package com.kh.midpoint.route.model.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class ParticipantRouteResponseDto {

	private Long participantId;
	private String nickname;
	private String travelMode;
	private int timeMinutes;
	private List<RoutePointDto> points;
	private List<RouteSegmentDto> segments;

}
