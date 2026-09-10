package com.kh.midpoint.restaurant.model.vo;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class Restaurant {

	String roomUuid;
	Long participantId;
	String source;
	Long kakaoPlaceId;
	String name;
	String category;
	String address;
	String roadAddress;
	String phone;
	String placeUrl;
	Double lat;
	Double lng;

}
