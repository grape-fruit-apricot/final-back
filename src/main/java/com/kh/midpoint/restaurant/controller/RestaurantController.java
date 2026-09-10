package com.kh.midpoint.restaurant.controller;

import com.kh.midpoint.common.response.ApiResponse;
import com.kh.midpoint.restaurant.model.dto.KakaoRestaurantResponseDto;
import com.kh.midpoint.restaurant.model.dto.RestaurantCreateRequestDto;
import com.kh.midpoint.restaurant.model.dto.RestaurantResponseDto;
import com.kh.midpoint.restaurant.model.service.RestaurantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/rooms/{roomUuid}/restaurants")
@RequiredArgsConstructor
public class RestaurantController {

	private final RestaurantService restaurantService;
	private final SimpMessagingTemplate messagingTemplate;

	@PostMapping
	public ResponseEntity<ApiResponse<Void>> insertRestaurant(
			@PathVariable("roomUuid") String roomUuid,
			@Valid @RequestBody RestaurantCreateRequestDto requestDto) {
		restaurantService.insertRestaurant(roomUuid, requestDto);

		// 등록 응답에는 생성된 식당을 담지 않으므로(생성 ID 미노출 규칙), 갱신된 목록 전체를
		// 방에 브로드캐스트해 등록한 사람과 나머지 참가자가 같은 목록을 보게 한다.
		messagingTemplate.convertAndSend("/topic/room/" + roomUuid + "/restaurants",
				restaurantService.findRestaurantList(roomUuid));

		return ResponseEntity.status(HttpStatus.CREATED)
			.body(ApiResponse.created("식당이 등록되었습니다.", null));
	}

	@GetMapping
	public ResponseEntity<ApiResponse<List<RestaurantResponseDto>>> findRestaurantList(@PathVariable("roomUuid") String roomUuid) {
		List<RestaurantResponseDto> responseDto = restaurantService.findRestaurantList(roomUuid);
		return ResponseEntity.ok(ApiResponse.ok("식당 목록 조회에 성공했습니다.", responseDto));
	}

	@GetMapping("/nearby")
	public ResponseEntity<ApiResponse<List<KakaoRestaurantResponseDto>>> findNearbyRestaurantList(@PathVariable("roomUuid") String roomUuid,
																								  @RequestParam Double lat,
																								  @RequestParam Double lng) {
		List<KakaoRestaurantResponseDto> responseDto = restaurantService.findNearbyRestaurantList(roomUuid, lat, lng);
		return ResponseEntity.ok(ApiResponse.ok("주변 식당 조회에 성공했습니다.", responseDto));
	}

}
