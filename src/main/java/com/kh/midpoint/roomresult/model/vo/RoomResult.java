package com.kh.midpoint.roomresult.model.vo;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class RoomResult {

	Long roomId;
	Long restaurantId;

}
