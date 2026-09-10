package com.kh.midpoint.tracking.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kh.midpoint.common.response.ApiResponse;
import com.kh.midpoint.tracking.model.dto.TrackingResponseDto;
import com.kh.midpoint.tracking.model.service.TrackingService;

import lombok.RequiredArgsConstructor;

// 화면이 추적 상황을 읽는 자리. 갱신은 소켓으로 받지만, 새로고침하거나 뒤늦게 들어온 사람은
// 그동안 지나간 방송을 받을 수 없으므로 지금 상태를 통째로 읽을 방법이 따로 있어야 한다.
@RestController
@RequestMapping("/api/rooms/{roomUuid}")
@RequiredArgsConstructor
public class TrackingController {

	private final TrackingService trackingService;

	@GetMapping("/tracking")
	public ResponseEntity<ApiResponse<TrackingResponseDto>> findTracking(
			@PathVariable("roomUuid") String roomUuid) {
		TrackingResponseDto responseDto = trackingService.findTracking(roomUuid);
		return ResponseEntity.ok(ApiResponse.ok("이동 현황 조회에 성공했습니다.", responseDto));
	}

}
