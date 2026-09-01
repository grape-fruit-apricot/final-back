package com.kh.midpoint.restaurant.controller;

import com.kh.midpoint.common.response.ApiResponse;
import com.kh.midpoint.restaurant.model.dto.RestaurantCreateRequestDto;
import com.kh.midpoint.restaurant.model.dto.RestaurantResponseDto;
import com.kh.midpoint.restaurant.model.service.RestaurantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/rooms/{roomUuid}/restaurants")
@RequiredArgsConstructor
public class RestaurantController {

	private final RestaurantService restaurantService;

	@PostMapping
	public ResponseEntity<ApiResponse<Void>> insertRestaurant(
			@PathVariable String roomUuid,
			@Valid @RequestBody RestaurantCreateRequestDto requestDto) {
		restaurantService.insertRestaurant(roomUuid, requestDto);
		return ResponseEntity.status(HttpStatus.CREATED)
			.body(ApiResponse.created("식당이 등록되었습니다.", null));
	}

	@GetMapping
	public ResponseEntity<ApiResponse<List<RestaurantResponseDto>>> findRestaurantList(@PathVariable String roomUuid) {
		List<RestaurantResponseDto> responseDto = restaurantService.findRestaurantList(roomUuid);
		return ResponseEntity.ok(ApiResponse.ok("식당 목록 조회에 성공했습니다.", responseDto));
	}

}
