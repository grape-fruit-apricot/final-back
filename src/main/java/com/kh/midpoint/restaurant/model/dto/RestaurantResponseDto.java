package com.kh.midpoint.restaurant.model.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class RestaurantResponseDto {

	private Long restaurantId;
	private String name;
	private String source;
	private String address;
	private Double lat;
	private Double lng;
	private Long addedBy;
	private LocalDateTime createdAt;

}
