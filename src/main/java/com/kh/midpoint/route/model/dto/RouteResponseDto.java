package com.kh.midpoint.route.model.dto;

import com.kh.midpoint.restaurant.model.dto.RestaurantResponseDto;
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
public class RouteResponseDto {

	private RestaurantResponseDto restaurant;
	private List<ParticipantRouteResponseDto> participantRoutes;

}
