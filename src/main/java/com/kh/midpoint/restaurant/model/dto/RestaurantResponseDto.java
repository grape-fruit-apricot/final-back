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
	private String category;
	private String address;
	private String roadAddress;
	private String phone;
	private String placeUrl;
	private Double lat;
	private Double lng;
	private Long addedBy;
	private LocalDateTime createdAt;

}
