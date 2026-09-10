package com.kh.midpoint.restaurant.model.dto;

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
public class KakaoRestaurantResponseDto {

	private String kakaoPlaceId;
	private String name;
	private String category;
	private String address;
	private String roadAddress;
	private String phone;
	private String placeUrl;
	private Double lat;
	private Double lng;
	private int distanceMeters;

}
