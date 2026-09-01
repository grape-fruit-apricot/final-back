package com.kh.midpoint.restaurant.model.vo;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class Restaurant {

	String roomUuid;
	Long participantId;
	String name;
	String address;
	Double lat;
	Double lng;

}
