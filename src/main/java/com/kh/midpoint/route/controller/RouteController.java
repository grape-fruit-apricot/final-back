package com.kh.midpoint.route.controller;

import com.kh.midpoint.common.response.ApiResponse;
import com.kh.midpoint.route.model.dto.RouteResponseDto;
import com.kh.midpoint.route.model.service.RouteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rooms/{roomUuid}/routes")
@RequiredArgsConstructor
public class RouteController {

	private final RouteService routeService;

	@PostMapping
	public ResponseEntity<ApiResponse<RouteResponseDto>> findRoute(@PathVariable String roomUuid) {
		RouteResponseDto responseDto = routeService.findRoute(roomUuid);
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(ApiResponse.created("참가자별 도보 경로를 조회했습니다.", responseDto));
	}

	@GetMapping
	public ResponseEntity<ApiResponse<RouteResponseDto>> findRouteResult(@PathVariable String roomUuid) {
		RouteResponseDto responseDto = routeService.findRouteResult(roomUuid);
		return ResponseEntity.ok(ApiResponse.ok("확정된 결과를 조회했습니다.", responseDto));
	}

}
