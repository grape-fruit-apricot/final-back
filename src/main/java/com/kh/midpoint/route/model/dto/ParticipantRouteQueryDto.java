package com.kh.midpoint.route.model.dto;

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
public class ParticipantRouteQueryDto {

	private Long routeId;
	private Long participantId;
	private String nickname;
	private Integer timeMinutes;

}
