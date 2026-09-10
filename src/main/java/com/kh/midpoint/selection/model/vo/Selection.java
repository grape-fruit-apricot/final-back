package com.kh.midpoint.selection.model.vo;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class Selection {

	Long participantId;
	Long restaurantId;

}
