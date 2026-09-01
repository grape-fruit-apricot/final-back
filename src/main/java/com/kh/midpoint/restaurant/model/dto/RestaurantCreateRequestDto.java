package com.kh.midpoint.restaurant.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class RestaurantCreateRequestDto {

	@NotNull
	private Long participantId;

	@NotBlank
	private String name;

	@NotBlank
	private String address;

	@NotNull
	private Double lat;

	@NotNull
	private Double lng;

}
