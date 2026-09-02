package com.kh.midpoint.route.model.dto;

import com.kh.midpoint.external.tmap.RoutePointDto;
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
	private int timeMinutes;
	private List<RoutePointDto> points;

}
